# 项目说明

## 当前已经完成

- 第一版产品需求文档：`docs/PRODUCT_REQUIREMENTS.md`
- Android 开发环境说明：`docs/DEVELOPMENT_SETUP.md`
- Kotlin + Jetpack Compose 安卓项目骨架
- 按日期记录健身数据
- 身高、体重、训练、三餐文本记录
- 身材照片上传和本地保存
- MiMo API Key 设置
- AI 分析当天记录

## 当前代码结构

```text
app/src/main/java/com/humsong/app
├── MainActivity.kt
├── ai
│   └── FitnessAiClient.kt
├── fitness
│   ├── FitnessModels.kt
│   └── FitnessRepository.kt
└── ui
    ├── FitnessApp.kt
    └── FitnessViewModel.kt
```

## 第一版运行方式

1. 安装 Android Studio
2. 用 Android Studio 打开当前文件夹
3. 等待 Gradle Sync
4. 连接安卓手机或启动模拟器
5. 运行 `app`

## 下一步建议

下一步建议：

- 试用一周，确认每天记录字段是否够用
- 增加体重趋势图
- 增加周报/月报
- 后续再做照片 AI 对比
