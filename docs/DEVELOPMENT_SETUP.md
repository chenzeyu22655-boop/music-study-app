# Android 开发环境准备

## 我可以完成什么

我可以帮你完成：

- 产品文档
- 安卓项目结构
- Kotlin / Jetpack Compose 代码
- 录音、音高检测、简谱转换、和弦推荐逻辑
- 后续 AI 接口设计和接入
- 报错排查和代码修改

## 你需要安装什么

要在你的电脑上真正编译和运行 APK，需要安装：

1. Android Studio
2. Android SDK
3. JDK，Android Studio 通常自带
4. 安卓手机或 Android 模拟器

安装 Android Studio 后，用它打开当前文件夹：

```text
C:\Users\Administrator\桌面\music
```

等待 Gradle Sync 完成，然后运行 `app`。

## 首次运行建议

- 使用真机比模拟器更适合测试麦克风
- 手机打开开发者选项和 USB 调试
- 第一次打开 App 时允许麦克风权限
- 先哼 5 到 10 秒单旋律，不要加伴奏

## 当前版本限制

当前第一版代码是 MVP 骨架，音高识别使用轻量本地算法，主要用于打通流程。后续需要通过真实哼唱样本继续调优，或者替换成更强的模型。
