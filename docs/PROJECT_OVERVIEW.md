# 项目说明

## 当前已经完成

- 第一版产品需求文档：`docs/PRODUCT_REQUIREMENTS.md`
- Android 开发环境说明：`docs/DEVELOPMENT_SETUP.md`
- Kotlin + Jetpack Compose 安卓项目骨架
- 录音入口
- 原始哼唱 WAV 保存和回放
- 本地音高检测骨架
- 调式标记
- MIDI 音高到简谱的转换
- 基础和弦推荐
- 面向音乐小白的创作建议文案

## 当前代码结构

```text
app/src/main/java/com/humsong/app
├── MainActivity.kt
├── audio
│   └── HumAudioRecorder.kt
├── music
│   ├── ChordRecommender.kt
│   ├── JianpuConverter.kt
│   ├── MelodyAnalyzer.kt
│   ├── MelodyModels.kt
│   └── PitchDetector.kt
└── ui
    ├── HumSongApp.kt
    └── HumSongViewModel.kt
```

## 第一版运行方式

1. 安装 Android Studio
2. 用 Android Studio 打开当前文件夹
3. 等待 Gradle Sync
4. 连接安卓手机或启动模拟器
5. 运行 `app`

## 下一步建议

最重要的下一步不是马上做更多页面，而是拿真实哼唱测试识别效果。

建议你录 10 段 5 到 10 秒的旋律，分别测试：

- 音高是否接近
- 简谱是否大致可读
- 和弦听起来是否有基本方向
- 哪些情况下识别最差

这些测试结果会决定后续是先优化本地算法，还是直接接入更强的 AI 识别方案。
