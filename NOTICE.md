# Third-party notices

## AndroidX / Jetpack Compose / Media3

- AndroidX, Jetpack Compose and AndroidX Media3
- https://github.com/androidx/androidx
- Apache License 2.0

## TagLib for Android

- TagLib Android binding by Kyant
- https://github.com/Kyant0/taglib
- Apache License 2.0

## MusicOne

本项目的 Compose 视觉基线与部分字体资源来自：

- MusicOne
- https://github.com/HGODLK/MusicOne
- Copyright (c) 2026 HGODLK
- MIT License

手机端只保留本地音乐相关界面，并已替换播放与多房间实现。

## USB Audio Driver

`usb-audio-driver` 模块移植自 MusicOne 使用的 DecentPlayer USB Audio
驱动，包含 UAC2 描述符解析、等时传输与 JNI 输出实现。原始许可证保留在
`usb-audio-driver/LICENSE`。

- DecentPlayer USB Audio Driver
- Copyright (c) 2026 Marcelo Silva
- MIT License

## BukaMusic

本地播放适配、多房间协议、PCM 音频同步与时钟校准逻辑来自本项目
`AirplayBackgroundMusic` 的互联终端设备端代码。

## Folia / Sonnet

动态歌词的 Sonnet 时间线、镜头运动、排版角色及 OpenGL 后处理移植自：

- Folia for Android
- https://github.com/Timingyyds/folia-android
- GNU Affero General Public License v3.0

手机端对该实现做了竖屏画布、长句拆分和可读字号适配。因为包含该
AGPL-3.0 衍生代码，`mobile/phone` 客户端整体按 AGPL-3.0 发布，对应源码
位于同一工程目录。
