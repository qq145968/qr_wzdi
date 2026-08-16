# 部署脚本 - 请在 TRAE 终端或 PowerShell 中运行
# 此脚本将代码推送到 GitHub 仓库

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  扫码机器人 v1.6.0 部署脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 设置项目目录
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

# 检查 git 是否可用
$gitExe = "C:\Program Files\Git\cmd\git.exe"
if (-not (Test-Path $gitExe)) {
    $gitExe = "git"
}

# 检查 .git 目录是否存在
if (-not (Test-Path ".git")) {
    Write-Host "[1/5] 初始化 Git 仓库..." -ForegroundColor Yellow
    & $gitExe init
    & $gitExe remote add origin https://github.com/qq145968/qr_wzdi.git
    & $gitExe fetch origin
    & $gitExe checkout -b main
} else {
    Write-Host "[1/5] Git 仓库已存在" -ForegroundColor Green
}

Write-Host ""
Write-Host "[2/5] 添加文件到暂存区..." -ForegroundColor Yellow
& $gitExe add app/ php-api/ .github/ build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat .gitignore debug.keystore

Write-Host ""
Write-Host "[3/5] 创建提交..." -ForegroundColor Yellow
& $gitExe commit -m "feat: v1.6.0 - 退出登录、下载进度条、消息已读、验证码、启动图

- 个人中心右上角添加退出账户按钮
- 更新对话框添加下载进度条和实际APK下载功能
- 消息列表添加已读/未读状态显示和全部已读功能
- 登录和注册添加图形验证码支持(后台可开关)
- 添加Android启动图(Splash Screen)
- 注册开关控制(后台registration_required设置)
- 后端添加captcha_codes/app_settings/app_versions/app_messages表
- 后端login.php/register.php添加验证码校验
- 版本号更新至 v1.6.0 (versionCode=7)"

Write-Host ""
Write-Host "[4/5] 推送到 GitHub..." -ForegroundColor Yellow
& $gitExe push -u origin main --force

Write-Host ""
Write-Host "[5/5] 完成!" -ForegroundColor Green
Write-Host ""
Write-Host "代码已推送到 GitHub: https://github.com/qq145968/qr_wzdi" -ForegroundColor Cyan
Write-Host "GitHub Actions 将自动编译 APK" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  PHP 后端部署说明" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "请将以下 PHP 文件上传到服务器 qr.wzdi.cn 的 API 目录:" -ForegroundColor White
Write-Host "  1. php-api/captcha.php     (新文件 - 验证码生成)" -ForegroundColor White
Write-Host "  2. php-api/app_info.php    (修改 - 增加验证码和启动图设置)" -ForegroundColor White
Write-Host "  3. php-api/login.php       (修改 - 增加验证码校验)" -ForegroundColor White
Write-Host "  4. php-api/register.php    (修改 - 增加验证码校验和注册开关)" -ForegroundColor White
Write-Host "  5. php-api/init.php        (修改 - 增加新表创建)" -ForegroundColor White
Write-Host ""
Write-Host "上传完成后，访问以下地址初始化数据库:" -ForegroundColor Yellow
Write-Host "  https://qr.wzdi.cn/api/init.php" -ForegroundColor White
Write-Host ""
Write-Host "然后在宝塔面板的数据库中设置:" -ForegroundColor Yellow
Write-Host "  app_settings 表中:" -ForegroundColor White
Write-Host "    captcha_enabled = 1       (开启验证码)" -ForegroundColor White
Write-Host "    registration_required = 1  (开启注册)" -ForegroundColor White
Write-Host "    splash_screen_url = (启动图URL，留空使用默认)" -ForegroundColor White
Write-Host ""
Write-Host "  app_versions 表中添加新版本:" -ForegroundColor White
Write-Host "    version_code = 7" -ForegroundColor White
Write-Host "    version_name = 1.6.0" -ForegroundColor White
Write-Host "    download_url = (APK下载地址)" -ForegroundColor White
Write-Host "    is_release = 1" -ForegroundColor White
Write-Host "    force_update = 0" -ForegroundColor White
Write-Host ""
