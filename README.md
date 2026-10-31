<p align="center"><img src="docs/icon.png" width="112" alt="BukaMusic"></p>

# BukaMusic

<p align="center"><strong>布卡流音</strong> · 手机端主应用</p>

<p align="center">配套互联端：<a href="https://github.com/HAN-BAK/BukaMusic-Connect">BukaMusic Connect</a> · 桌面端：<a href="https://github.com/HAN-BAK/BukaMusic-Desktop">BukaMusic Desktop</a></p>

这是布卡系列的主应用，界面设计以
[MusicOne](https://github.com/HGODLK/MusicOne) 为基础，播放与多房间同步采用
BukaMusic Connect（布卡聚音）已有逻辑。

## 功能范围

保留：

- 读取 Android 系统媒体库中的本地音乐；
- 从系统文件选择器导入本地音频并持久保存访问授权；
- 本地播放、队列、上一首/下一首、拖动进度与播放模式；
- 设置内手动检查更新，验证签名清单、下载摘要和应用证书后再安装；
- 歌曲、专辑、歌手和歌单均支持列表与卡片网格两种显示方式；
- 本地封面读取与全屏播放器；
- MusicOne 风格的实时毛玻璃悬浮迷你播放器与底部导航胶囊；
- 根据当前封面自动生成全局调色板，按钮、卡片、导航、播放器和歌词控件随歌变色；
- 设置中可切换并持久保存明亮模式 / 黑暗模式；明亮模式不使用黑色背景遮罩；
- 明暗模式分别使用太阳与月亮图标；设置页的关于入口进入独立页面；
- 支持自定义本机名称；未设置时读取 Android 系统设备名称；
- 状态栏和系统导航栏使用透明沉浸式布局，并根据明暗模式切换图标颜色；
- 普通逐句歌词、内嵌歌词、同名 LRC/YRC/TTML，并支持 TTML 原文、翻译与音译分离；
- 普通歌词页使用封面模糊背景、逐字高亮、未唱句缩放/透明与毛玻璃播放控制栏，
  标题和歌词统一使用 Source Han Serif 展示字体；
- 歌词使用逐帧平滑时钟、弹簧滚动跟词和逐字透明度推进；支持时间偏移、
  逐字点击跳转、触感反馈与蓝牙/车载歌词；
- 支持“我喜欢”收藏、自定义歌单创建/重命名/删除、添加与移除曲目；
- 首页以歌单卡片展示内容，不再直接列出本地曲目；
- “每日推荐”根据累计播放频率生成，同一天固定缓存，次日后重新生成；
- 歌单卡片可进入 MusicOne 风格详情页，包含大封面、播放、随机和曲目列表；
- 无封面歌曲复用设备端占位图，并以占位图提取主色和生成模糊背景；
- 封面使用进程内 LRU 缓存，返回曲库、歌单和播放器时不再重复解码；
- Folia Sonnet 动态歌词：逐字时间轴、七种分镜、镜头轨迹和 OpenGL 后处理；
- 手机竖屏动态歌词布局，超长句自动拆分为更多镜头并保持可读字号；
- 自动发现、选择、主控和接收布卡聚音多房间音频；
- 多房间自动排除本机地址，设备列表不展示 IP，接收端播放页可直接断开连接；
- 普通歌词与 Sonnet 动态歌词共用 MusicOne 的低分辨率柔化封面背景；
- 双语歌词会在进入动态导演前拆分原文与翻译，并校正每个字的最终方向；
- 横屏播放器采用 MusicOne 风格双栏：左侧封面、曲目信息与播放控制，右侧同步歌词；歌单详情横屏使用封面和曲目双栏；
- 页面切换使用统一的缓动和缩放转场；
- 设置按“播放体验、音效与输出、歌词、本地曲库、外观、存储、诊断”分组；
- 睡眠定时、淡入淡出、会话恢复、蓝牙断开暂停、音频焦点和空闲退出；
- 10 段均衡器、预设与 AutoEQ 导入、响度均衡、左右声道平衡、倍速与音调；
- 32-bit 高解析请求可在当前播放位置切换，普通与高解析输出会自动重建播放器；
- UAC2 原生独占输出：直接通过 USB 等时传输写入 DAC，支持 16/24/32-bit 整数 PCM；
- UAC1 与兼容设备使用系统 USB 直连；Android 14 及以上优先尝试系统 Bit-Perfect 通道；
- 拔线、授权失败或设备拒绝采样率时自动回到系统音频输出，不会静默吞掉播放；
- 普通输出的切歌使用双播放器等功率交叉淡化；USB 独占或启用 EQ/响度时保持单路，避免混音破坏位完美；
- 本地曲库提供聆听统计、元数据与封面/歌词写回、重复歌曲识别、目录扫描、
  同名 LRC/TXT/封面配对、歌单备份与 `.msco` 归档导入导出；
- 设置、歌单和历史记录可导出配置，支持可选密码加密导入；
- 隐藏开发者入口、崩溃/ANR 记录、日志导出与安全模式；
- 设备端原有的 PCM 分块、时钟校准、延迟补偿与传输控制。

不包含：

- AirPlay / RAOP 接收；
- 设备 HTTP 控制接口；
- 扫码或网页上传；
- WebDAV、SMB、FTP 网络音乐路径；
- 在线音源、账号、歌单同步和云端服务；
- 后台自动更新与桌面 Home 模式。

- 播放页底部提供队列、睡眠定时、音量、全屏歌词和加入歌单快捷入口。

## 工程结构

```text
mobile/phone/
├─ app/src/main/java/com/buka/mobile/
│  ├─ data/        本地曲库与曲目模型
│  ├─ playback/    播放服务、状态仓库
│  └─ ui/          Compose 页面、主题与封面
└─ app/src/main/java/com/airmusic/player/
   ├─ playback/    设备端 LocalPlayer 与 Media3 音频链
   ├─ multicast/   多房间协议、音频流与时钟校准
   └─ library/     设备端曲目模型
```

`MultiRoomDiscovery.kt` 是手机端专用发现层：它直接使用 Android
`NsdManager` 广播 `_airmusic-sync._tcp.`，不再依赖 AirPlay 服务的 JmDNS 实例。

## 构建

环境：JDK 21、Android SDK 36。

```powershell
.\gradlew.bat :app:assembleDebug
```

APK：

```text
mobile/phone/app/build/outputs/apk/debug/app-debug.apk
```

Lint：

```powershell
.\gradlew.bat :app:lintDebug
```

当前版本已通过 `assembleDebug`、`testDebugUnitTest` 与 `lintDebug`，并安装验证：

- Android 36 x86_64 模拟器：`emulator-5554`；
- USB 平板：`531a5c99`，小米平板 5 Pro 5G（`M2105K81AC`）。

当前环境如果出现 Gradle `Unable to establish loopback connection`，把
`TEMP` / `TMP` 指向纯 ASCII 路径后重新运行即可。

## Android 模拟器

本机已创建：

- AVD：`BukaMusic_Phone_API36`
- 系统镜像：Android 36 Google APIs x86_64
- 设备模板：Pixel 7
- AVD 数据目录：`D:\APP\.android\avd`

启动：

```powershell
.\tools\start-emulator.ps1
```

也可以直接双击：

```text
tools/Start-Android-Emulator.bat
```

需要清空快速启动快照时：

```powershell
.\tools\start-emulator.ps1 -ColdBoot
```

运行截图：

- `docs/emulator-home.png`
- `docs/home-daily-recommendations.png`：无本地曲目列表的每日推荐首页
- `docs/emulator-multiroom.png`
- `docs/emulator-settings.png`
- `docs/folia-long-readable.png`：超长歌词的竖屏可读字号
- `docs/interlude-test.png`：间奏三点与竖排分镜
- `docs/musicone-glass-playing.png`：悬浮迷你播放器与导航胶囊
- `docs/musicone-glass-player.png`：全屏播放器
- `docs/musicone-glass-lyrics.png`：MusicOne 风格普通歌词页与播放控制栏
- `docs/musicone-lyrics-final.png`：特殊字体、歌词滑动与毛玻璃控制栏最终效果
- `docs/theme-light-settings.png`：明亮模式与设置入口
- `docs/theme-dark-settings.png`：黑暗模式
- `docs/theme-light-lyrics.png`：明亮模式歌词页
- `docs/theme-track1.png`、`docs/theme-track2.png`：不同歌曲触发不同动态主题色

## 开源归属

- 前端基础：MusicOne，MIT License。
- 播放与多房间逻辑：BukaMusic Connect（布卡聚音）。
- Folia Sonnet 动态歌词：Folia for Android，AGPL-3.0。

`mobile/phone` 客户端包含 AGPL-3.0 衍生代码，因此整体按 AGPL-3.0 发布。
MusicOne 原始 MIT 许可证保留在 `LICENSES/MusicOne-MIT.txt`。相关归属见
`NOTICE.md`。
