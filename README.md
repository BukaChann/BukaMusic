<h1 align="center">BukaMusic（布卡流音）</h1>

<p align="center"><img src="docs/icon.png" width="112" alt="BukaMusic"></p>

<p align="center"><strong>布卡流音</strong> · 布卡系列手机端主应用：本地音乐 / 歌单 / 动态歌词 / USB 音频 / 多房间同步</p>

<p align="center">
  互联端：<a href="https://github.com/BukaChann/BukaMusic-Connect">BukaMusic Connect</a>
  ·
  桌面端：<a href="https://github.com/BukaChann/BukaMusic-Desktop">BukaMusic Desktop</a>
</p>

<p align="center">手动下载：<a href="https://github.com/BukaChann/BukaMusic/releases">GitHub Releases</a> · <a href="https://gitee.com/hanhan2048/BukaMusic-Releases/releases">Gitee Releases</a></p>

<p align="center">应用内更新：通过 Gitee 检测新版本并下载安装包</p>

<p align="center">Android 6.0+ · AGPL-3.0</p>

## 客户端关系

| 客户端 | 中文名称 | 定位 |
| :--- | :--- | :--- |
| **BukaMusic** | 布卡流音 | 手机端主应用，负责曲库、歌单、歌词与多房间主控 |
| **BukaMusic Connect** | 布卡聚音 | 互联终端设备端，负责本地播放、AirPlay 接收与局域网控制 |
| **BukaMusic Desktop** | 布卡音台 | Windows 桌面控制端，发现并控制局域网设备 |

## 功能

### 曲库与播放

- 读取 Android 媒体库，并从系统文件选择器导入本地音频和目录授权；
- 歌曲、专辑、歌手、歌单均支持列表与卡片网格显示，专辑和歌手可进入独立曲目页；
- 本地播放、队列、拖动进度、睡眠定时、淡入淡出、播放模式和会话恢复；
- 元数据与封面/歌词写回、重复歌曲识别、歌单备份和 `.msco` 曲库归档。

### 歌词与视觉

- 普通逐句歌词、内嵌歌词、同名 LRC/YRC/TTML，支持翻译与音译；
- Folia Sonnet 动态歌词，包含逐字时间轴、镜头运动与 OpenGL 后处理；
- 明亮/黑暗模式、自定义背景、界面缩放、页面转场与高刷新率支持；
- 全局配色从当前封面动态提取，按钮、图标、导航、卡片和歌词界面同步取色。

### 音频与输出

- 10 段均衡器、AutoEQ 导入、响度均衡、声道平衡、倍速与音调；
- 32-bit 高解析输出和系统 Bit-Perfect 通道；
- UAC2 原生独占输出，支持 16/24/32-bit PCM；异常断开时自动回退系统输出；
- 普通播放使用双播放器等功率交叉淡化，USB 独占与 EQ 输出保持单路处理。

### 多房间与更新

- 自动发现、选择、主控和接收布卡聚音多房间音频；
- 时钟校准、延迟补偿、主控/接收端双向播放控制；
- 设置 → 软件更新：验证签名清单、HTTPS 域名、SHA-256 和应用证书后安装；
- 更新源未发布正式清单时安全失败，不会回退到无签名下载。

## 使用方法

1. 首次启动授予通知和音乐音频读取权限。
2. 在首页或曲库选择歌曲；长曲库可使用搜索、列表/网格切换和歌单管理。
3. 点击专辑或歌手卡片进入对应曲目页，再选择单曲或播放全部。
4. 多房间页选择同一局域网内的布卡聚音设备，即可作为主控同步播放。
5. 设置 → 软件更新可手动检查新版本。

## 构建

环境：JDK 21、Android SDK 36、Gradle Wrapper。

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

当前环境如果出现 Gradle `Unable to establish loopback connection`，把
`TEMP` / `TMP` 指向纯 ASCII 路径后重新运行。

## 工程结构

```text
.
├─ app/src/main/java/com/buka/mobile/
│  ├─ data/        曲库、歌单、设置与归档
│  ├─ playback/    播放服务、USB 输出与队列状态
│  ├─ ui/          Compose 页面、动态主题与歌词
│  └─ update/      签名更新校验与安装
├─ usb-audio-driver/
└─ tools/
```

## 开源归属

- 界面基础：MusicOne，MIT License。
- 播放与多房间逻辑：BukaMusic Connect（布卡聚音）。
- Folia Sonnet 动态歌词：Folia for Android，AGPL-3.0。

本项目包含 AGPL-3.0 衍生代码，因此整体按 AGPL-3.0 发布。MusicOne 原始 MIT
许可证保留在 `LICENSES/MusicOne-MIT.txt`，详细归属见 `NOTICE.md`。
