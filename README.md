# 扫码机器人 (ScanRobot)

基于 Android + Jetpack Compose 的智能扫码工具应用，支持多种扫码模式、批量管理、数据导出、用户认证等功能。

## 功能特性

### 扫码功能
- **4种扫码模式**：全屏 / 新全屏 / 微信风格 / 半屏，适应不同场景
- **实时识别**：基于 CameraX + ML Kit，支持多种条码/二维码格式
- **批量扫码**：连续扫码自动归档，支持批次管理与详情查看
- **闪光灯控制**：低光环境下一键开启手电筒
- **扫码提示音**：AudioTrack PCM 播放，无需系统权限
- **震动反馈**：扫码成功震动提示

### 数据管理
- **历史记录**：扫码记录本地持久化，按批次归档
- **数据导出**：复制到剪贴板或导出为 CSV 文件
- **清空确认**：清空前弹出确认对话框，防止误操作
- **一键清除数据**：带进度对话框和自动重启

### 用户系统
- **用户认证**：登录 / 注册 / 忘记密码完整流程
- **滑动验证码**：登录页面滑动验证
- **个人中心**：自定义头像上传、个人资料管理
- **退出登录**：消息已读状态、会话清理
- **强制下线**：后台删除用户后 APP 自动退出登录

### 界面与交互
- **公告跑马灯**：基于实际文本宽度的无缝滚动
- **消息通知**：弹窗展示、清除按钮、已读管理
- **版本检查**：应用内更新提示
- **Material 3**：明暗主题自适应，现代化 UI 设计
- **再按一次退出**：美观的退出提示样式

## 技术栈

| 类别 | 技术 |
|------|------|
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM（ViewModel + StateFlow） |
| 相机 | CameraX |
| 扫码 | ML Kit Barcode Scanning |
| 数据持久化 | SharedPreferences（JSON 序列化） |
| 网络 | OkHttp + Gson |
| 图片加载 | BitmapFactory + Compose Image |
| 构建 | Gradle 8.7 + AGP 8.5.0 + Kotlin 1.9.24 |
| CI/CD | GitHub Actions（自动构建 Release APK） |

## 项目结构

```
app/src/main/java/com/scanrobot/app/
├── MainActivity.kt          # 入口 Activity，导航与状态管理
├── ScanApp.kt               # Application 初始化
├── data/
│   ├── Models.kt            # 数据模型（ScanBatch / AppInfo / ScanSettings）
│   └── ScanStore.kt         # 本地存储（SharedPreferences）
├── network/
│   └── ApiClient.kt         # 网络请求封装
├── ui/
│   ├── theme/               # 主题（Color / Theme）
│   ├── AuthScreen.kt        # 登录 / 注册 / 忘记密码
│   ├── HomeScreen.kt        # 首页（公告栏 + 管理 + 个人中心）
│   ├── ScannerScreen.kt     # 扫码页（相机预览 + 结果列表）
│   ├── DetailScreen.kt      # 批次详情页
│   └── CameraPreview.kt     # 相机预览组件
├── util/
│   └── BeepManager.kt       # 扫码提示音（PCM）
└── viewmodel/
    └── ScanViewModel.kt     # 全局状态管理
```

## 构建与运行

### 环境要求

- Android Studio Hedgehog 或更高
- JDK 17
- Android SDK 34（compileSdk / targetSdk）
- Gradle 8.7

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/qq145968/qr_wzdi.git
cd qr_wzdi

# Release 构建（已配置 R8 压缩 + 资源缩减）
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/release/`。

### CI 自动构建

仓库内置 GitHub Actions 工作流（`.github/workflows/build-apk.yml`），每次推送到 `main` 分支自动触发：

1. 构建 Release APK（R8 代码压缩 + 资源缩减）
2. 上传为 Artifacts 供下载（保留 30 天）

## 版本历史

| 版本 | 说明 |
|------|------|
| 1.0.0 | 首个正式版本：扫码 / 批次管理 / 导出 / 公告滚动 / 头像上传 |
| 1.0.5 | 4种扫码模式 / 用户认证系统 / 滑动验证码 / 消息通知 / 一键清除数据 / 公告跑马灯 / 退出样式美化 / Release 构建优化 |

## 权限说明

| 权限 | 用途 |
|------|------|
| CAMERA | 扫码功能 |
| VIBRATE | 扫码成功震动反馈 |
| INTERNET | 网络请求（登录 / 公告 / 消息） |
| ACCESS_NETWORK_STATE | 检测网络状态 |
| REQUEST_INSTALL_PACKAGES | 应用内更新（预留） |

## License

本项目仅供学习交流使用。
