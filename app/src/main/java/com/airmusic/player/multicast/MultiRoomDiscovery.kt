package com.airmusic.player.multicast

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Phone-side discovery for the existing BukaMusic multi-room protocol.
 *
 * The device build piggybacks on the JmDNS instances owned by its AirPlay
 * receiver. The mobile client has no AirPlay server, so it advertises and
 * discovers `_airmusic-sync._tcp.` directly with Android NsdManager.
 */
class MultiRoomDiscovery {
    fun interface Listener {
        fun onDevicesChanged()
    }

    class DeviceInfo(
        @JvmField val name: String,
        @JvmField val addresses: Array<String>,
        @JvmField val port: Int,
    )

    private val devices = ConcurrentHashMap<String, DeviceInfo>()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val nsdManager: NsdManager? =
        MobileServiceContext.get()?.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val multicastLock: WifiManager.MulticastLock? =
        (MobileServiceContext.get()?.applicationContext
            ?.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createMulticastLock("buka-mobile-discovery")
            ?.apply { setReferenceCounted(false) }

    @Volatile
    private var requestedName: String = ""
    @Volatile
    private var registeredName: String = ""
    @Volatile
    private var discovering: Boolean = false
    @Volatile
    private var stopped: Boolean = true

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val pendingNames: MutableSet<String> =
        java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @Synchronized
    fun register(deviceName: String) {
        requestedName = deviceName.trim().ifEmpty { defaultDeviceName() }
        stopped = false
        runCatching { multicastLock?.acquire() }
        if (registeredName.isNotEmpty() &&
            !registeredName.equals(requestedName, ignoreCase = true)
        ) {
            registrationListener?.let { listener ->
                runCatching { nsdManager?.unregisterService(listener) }
            }
            registrationListener = null
            registeredName = ""
        }
        ensureRegistered()
        startDiscovery()
    }

    @Synchronized
    fun ensureRegistered() {
        if (stopped || registeredName.isNotEmpty()) return
        val manager = nsdManager ?: return
        val info = NsdServiceInfo().apply {
            serviceName = requestedName.ifEmpty { defaultDeviceName() }
            serviceType = ANDROID_SERVICE_TYPE
            port = MultiRoomProtocol.PORT
            setAttribute("name", requestedName)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                registeredName = serviceInfo.serviceName
                Log.i(TAG, "registered ${serviceInfo.serviceName} on ${MultiRoomProtocol.PORT}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registeredName = ""
                Log.w(TAG, "registration failed: $errorCode")
                if (!stopped) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        ensureRegistered()
                    }, 1_500)
                }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                registeredName = ""
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registeredName = ""
            }
        }
        registrationListener = listener
        runCatching { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { Log.w(TAG, "registerService failed", it) }
    }

    @Synchronized
    fun startDiscovery() {
        if (stopped || discovering) return
        val manager = nsdManager ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                discovering = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                val name = normalizeName(service.serviceName)
                if (name.isEmpty() || isLocalServiceName(name)) return
                if (!pendingNames.add(name)) return
                runCatching {
                    manager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            pendingNames.remove(name)
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            pendingNames.remove(name)
                            addResolved(serviceInfo)
                        }
                    })
                }.onFailure { pendingNames.remove(name) }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val name = normalizeName(service.serviceName)
                if (devices.remove(name) != null) notifyChanged()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                discovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                discovering = false
                Log.w(TAG, "start discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                discovering = false
            }
        }
        discoveryListener = listener
        runCatching {
            manager.discoverServices(ANDROID_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            discovering = false
            Log.w(TAG, "discoverServices failed", it)
        }
    }

    fun rescan() {
        val manager = nsdManager ?: return
        discoveryListener?.let { listener ->
            runCatching { manager.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
        discovering = false
        devices.clear()
        notifyChanged()
        startDiscovery()
    }

    fun getDevices(): List<DeviceInfo> =
        devices.values.sortedBy { it.name.lowercase() }

    fun isHealthy(): Boolean = registeredName.isNotEmpty()

    @Synchronized
    fun stop() {
        stopped = true
        val manager = nsdManager
        discoveryListener?.let { listener ->
            runCatching { manager?.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
        registrationListener?.let { listener ->
            runCatching { manager?.unregisterService(listener) }
        }
        registrationListener = null
        discovering = false
        registeredName = ""
        pendingNames.clear()
        devices.clear()
        runCatching {
            if (multicastLock?.isHeld == true) multicastLock.release()
        }
    }

    private fun addResolved(info: NsdServiceInfo) {
        val name = normalizeName(info.serviceName)
        if (name.isEmpty() || isLocalServiceName(name)) return
        val addresses = when {
            Build.VERSION.SDK_INT >= 34 && info.hostAddresses.isNotEmpty() ->
                info.hostAddresses
                    .map { it.hostAddress ?: it.hostName }
                    .filter(String::isNotBlank)
            !info.host.hostAddress.isNullOrBlank() -> listOf(info.host.hostAddress.orEmpty())
            else -> emptyList()
        }.map(::toTransferAddress).distinct()
        if (addresses.isEmpty() || addresses.any(::isLocalAddress)) return
        devices[name] = DeviceInfo(
            name = name,
            addresses = addresses.toTypedArray(),
            port = info.port.takeIf { it > 0 } ?: MultiRoomProtocol.PORT,
        )
        notifyChanged()
    }

    private fun notifyChanged() {
        listeners.toList().forEach { listener ->
            runCatching { listener.onDevicesChanged() }
        }
    }

    private fun defaultDeviceName(): String {
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isEmpty()) "BukaMusic 手机端" else "BukaMusic · $model"
    }

    private fun normalizeName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim().replace(Regex("\\s+\\(\\d+\\)$"), "")
    }

    private fun isLocalServiceName(name: String): Boolean {
        val normalized = normalizeName(name)
        return normalized.equals(normalizeName(requestedName), ignoreCase = true) ||
            normalized.equals(normalizeName(registeredName), ignoreCase = true)
    }

    /**
     * NsdManager can publish this phone's own record before the registration
     * callback stores the final service name. Comparing resolved IPs is the
     * authoritative fallback and mirrors the device client's self filtering.
     */
    private fun isLocalAddress(address: String): Boolean {
        val candidate = normalizeAddress(address) ?: return false
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching false
            var found = false
            for (network in java.util.Collections.list(interfaces)) {
                if (!network.isUp || network.isLoopback) continue
                for (inet in java.util.Collections.list(network.inetAddresses)) {
                    if (normalizeAddress(inet) == candidate) {
                        found = true
                        break
                    }
                }
                if (found) break
            }
            found
        }.getOrDefault(false)
    }

    private fun normalizeAddress(address: InetAddress): String? =
        normalizeAddress(address.hostAddress)

    /**
     * Android NSD can resolve a peer to an unscoped IPv6 link-local address.
     * Such an address cannot open a TCP socket until Android knows which local
     * interface to use. Add the Wi-Fi/interface scope while preserving any
     * global IPv6 or IPv4 addresses returned by newer NSD implementations.
     */
    private fun toTransferAddress(raw: String): String {
        val address = raw.trim()
            .removePrefix("[")
            .removeSuffix("]")
        if (address.isEmpty() || address.contains('%') || !isIpv6LinkLocal(address)) {
            return address
        }
        val scope = preferredNetworkInterfaceName() ?: return address
        return "$address%$scope"
    }

    private fun isIpv6LinkLocal(address: String): Boolean {
        val firstGroup = address.substringBefore(':').toIntOrNull(16) ?: return false
        return firstGroup in 0xFE80..0xFEBF
    }

    private fun preferredNetworkInterfaceName(): String? = runCatching {
        val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan", ignoreCase = true)) 0 else 1 }
        interfaces.firstOrNull { network ->
            java.util.Collections.list(network.inetAddresses).any { address ->
                address is java.net.Inet6Address && address.isLinkLocalAddress
            }
        }?.name
    }.getOrNull()

    private fun normalizeAddress(address: String?): String? =
        address
            ?.trim()
            ?.substringBefore('%')
            ?.removePrefix("[")
            ?.removeSuffix("]")
            ?.lowercase()
            ?.takeIf(String::isNotEmpty)

    private companion object {
        const val TAG = "MultiRoomDiscovery"
        val ANDROID_SERVICE_TYPE = MultiRoomProtocol.SERVICE_TYPE.removeSuffix("local.")
    }
}

/**
 * The Java manager constructors are kept context-free. This holder is
 * initialized by the application before the playback service starts.
 */
object MobileServiceContext {
    @Volatile
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun get(): Context? = context
}
