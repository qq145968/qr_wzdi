# 扫码机器人 (ScanRobot)

一款基于 Android + Jetpack Compose 的智能扫码工具应用，支持批量扫码、历史管理、数据导出等功能。

## 功能特性

- **实时扫码**：基于 CameraX + ML Kit，支持快速识别多种条码/二维码
- **批量扫码**：连续扫码自动归档到批次，支持批次管理与详情查看
- **闪光灯控制**：低光环境下一键开启手电筒
- **数据导出**：扫码记录可复制到剪贴板或导出为 CSV 文件
- **清空确认**：清空扫码列表前弹出确认对话框，防止误操作
- **公告滚动**：首页公告栏无缝滚动展示，文字完整不裁剪
- **个人中心**：支持自定义头像上传（从相册选择并持久化保存）
- **主题切换**：Material 3 设计语言，明暗主题自适应
- **登录鉴权**：内置登录页面，数据本地持久化

## 技术栈

| 类别 | 技术 |
|------|------|
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM（ViewModel + StateFlow） |
| 相机 | CameraX |
| 扫码 | ML Kit Barcode Scanning |
| 数据持久化 | SharedPreferences（JSON 序列化） |
| 网络 | Retrofit + OkHttp |
| 图片加载 | BitmapFactory + Compose Image |
| 构建 | Gradle 8.7 + AGP 8.2 + Kotlin 1.9 |
| CI/CD | GitHub Actions（自动构建 APK / AAB） |

## 项目结构

```
app/src/main/java/com/scanrobot/app/
├── MainActivity.kt          # 入口 Activity
├── ScanApp.kt               # Application 初始化
├── data/
│   ├── Models.kt            # 数据模型（ScanBatch / ScanListItem / ScanSettings）
│   └── ScanStore.kt         # 本地存储（SharedPreferences）
├── network/
│   └── ApiClient.kt         # 网络请求封装
├── ui/
│   ├── theme/               # 主题（Color / Theme）
│   ├── AuthScreen.kt        # 登录页
│   ├── HomeScreen.kt        # 首页（公告栏 + 管理 + 个人中心）
│   ├── ScannerScreen.kt     # 扫码页（相机预览 + 结果列表）
│   ├── DetailScreen.kt      # 批次详情页
│   └── CameraPreview.kt     # 相机预览组件
├── util/
│   └── BeepManager.kt       # 扫码提示音
└── viewmodel/
    └── ScanViewModel.kt     # 全局状态管理
```

## 构建与运行

### 环境要求

- Android Studio Hedgehog 或更高
- JDK 17
- Android SDK 34
- Gradle 8.7

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/qq145968/qr_wzdi.git
cd qr_wzdi

# Debug 构建
./gradlew assembleDebug

# Release 构建（需配置签名）
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/`。

### CI 自动构建

仓库内置 GitHub Actions 工作流（`.github/workflows/build.yml`），每次推送到 `main` 分支会自动触发：

1. 构建 Debug + Release APK
2. 构建 Release AAB
3. 上传为 Artifacts 供下载

## 版本历史

| 版本 | 说明 |
|------|------|
| 1.0.0 | 首个正式版本：扫码 / 批次管理 / 导出 / 公告滚动 / 头像上传 / 清空确认 |

## 权限说明

| 权限 | 用途 |
|------|------|
| CAMERA | 扫码功能 |
| VIBRATE | 扫码成功震动反馈 |
| INTERNET | 网络请求（登录 / 公告） |
| ACCESS_NETWORK_STATE | 检测网络状态 |
| REQUEST_INSTALL_PACKAGES | 应用内更新（预留） |

## License

本项目仅供学习交流使用。
