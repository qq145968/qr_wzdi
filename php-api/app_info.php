<?php
/**
 * APP 信息接口（启动页/首页配置 + 最新版本 + 消息）
 * GET https://qr.wzdi.cn/api/app_info.php
 * 返回: { success, message, data:{...} }
 */
require_once __DIR__ . '/config.php';

function getSetting($key, $default = '') {
    global $db;
    try {
        $row = $db->fetchOne("SELECT setting_value FROM app_settings WHERE setting_key = ? LIMIT 1", [$key]);
        if ($row) return $row['setting_value'];
        // 查询不到时自动插入默认值，确保设置项存在
        try {
            $db->query("INSERT INTO app_settings (setting_key, setting_value, created_at, updated_at) VALUES (?, ?, NOW(), NOW())", [$key, $default]);
        } catch (Exception $insertEx) {}
    } catch (Exception $e) {}
    return $default;
}
function toUrl($val) {
    if ($val === '' || $val === null) return '';
    if (strpos($val, 'http') === 0) return $val;
    $host = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https://' : 'http://') . $_SERVER['HTTP_HOST'];
    return $host . (strpos($val, '/') === 0 ? $val : '/' . $val);
}

// —— 通用开关 ——
$announcement   = getSetting('announcement', getSetting('app_main_title', '欢迎使用扫码机器人'));
$maintenance    = getSetting('maintenance_mode', '0') === '1';
$regEnabled     = getSetting('app_registration_enabled', getSetting('registration_required', '1')) === '1';
// 后台 APP 设置页实际使用 code_login_enabled / code_register_enabled（默认开启）
$loginCaptchaKey = getSetting('code_login_enabled', '');
$regCaptchaKey   = getSetting('code_register_enabled', '');
$captchaLoginEnabled    = $loginCaptchaKey === '' ? (getSetting('app_verification_enabled', getSetting('captcha_enabled', '0')) === '1') : ($loginCaptchaKey === '1');
$captchaRegisterEnabled = $regCaptchaKey   === '' ? (getSetting('app_verification_enabled', getSetting('captcha_enabled', '0')) === '1') : ($regCaptchaKey   === '1');
// 兼容旧版 APP：任一开启就返回 captcha_enabled=true（旧版只看这个）
$captchaEnabled = $captchaLoginEnabled || $captchaRegisterEnabled;
// 滑动验证码开关（与图形验证码互斥）
$slidingLoginEnabled    = getSetting('sliding_login_enabled', '1') === '1';
$slidingRegisterEnabled = getSetting('sliding_register_enabled', '1') === '1';
$slidingForgotEnabled   = getSetting('sliding_forgot_enabled', '1') === '1';

// —— 检查用户是否存在且未禁用（后台删除/禁用用户后，APP 端立即失效）——
$authInvalid = false;
$reqUserId = (int)($_GET['user_id'] ?? $_POST['user_id'] ?? 0);
$authToken = trim($_GET['token'] ?? $_POST['token'] ?? '');
if ($reqUserId > 0) {
    try {
        // 先检查封禁是否过期，过期则自动解封
        $banRow = $db->fetchOne("SELECT id, status, banned_until FROM users WHERE id = ? LIMIT 1", [$reqUserId]);
        if (!$banRow) {
            $authInvalid = true;
        } elseif ((int)$banRow['status'] === 0 && !empty($banRow['banned_until'])) {
            $expireRow = $db->fetchOne("SELECT TIMESTAMPDIFF(SECOND, NOW(), ?) as diff", [$banRow['banned_until']]);
            if ($expireRow && (int)$expireRow['diff'] <= 0) {
                $db->update('users', ['status' => 1, 'banned_at' => null, 'banned_until' => null], ['id' => $reqUserId]);
                $banRow['status'] = 1;
            }
        }
        if (!$authInvalid && (int)$banRow['status'] !== 1) {
            $authInvalid = true;
        }
    } catch (Exception $e) {
        $authInvalid = true;
    }
}
$splashImage    = getSetting('splash_image', getSetting('splash_screen_url', ''));

// —— 启动页 (Splash Screen) ——
$splashAppName        = getSetting('splash_app_name',        getSetting('app_name', '二维码管理系统'));
$splashAppDescription = getSetting('splash_app_description', getSetting('app_description', '专业的二维码管理工具'));
$splashBgColor        = getSetting('splash_bg_color', '#1677ff');
$splashIconUrlRaw     = getSetting('splash_icon_url', getSetting('app_logo', ''));
$splashDuration       = (int)getSetting('splash_duration', '2');

// —— 首页顶部 (Home Header) ——
$homeAppName          = getSetting('home_app_name',          getSetting('app_name', '扫码机器人'));
$homeAppDescription   = getSetting('home_app_description',   getSetting('app_description', '让手机变成扫码枪'));
$homeIconUrlRaw       = getSetting('home_icon_url', '');

$appName        = getSetting('app_name', $homeAppName);
$appDescription = getSetting('app_description', $homeAppDescription);

// —— 最新版本 ——
$latestVersion = null;
try {
    $row = $db->fetchOne("SELECT * FROM app_versions WHERE is_release = 1 ORDER BY version_code DESC LIMIT 1");
    if ($row) {
        $latestVersion = [
            'version_code'   => (int)($row['version_code'] ?? 0),
            'version_name'   => $row['version_name'] ?? '',
            'download_url'   => $row['download_url'] ?? '',
            'update_content' => $row['changelog']     ?? $row['update_content'] ?? $row['release_notes'] ?? '',
            'force_update'   => (int)($row['force_update'] ?? 0) === 1,
            'file_size'      => (int)($row['file_size'] ?? 0),
        ];
    }
} catch (Exception $e) {}

// —— 已发布消息（APP公告）——
// 状态兼容：后台发送写 status='sent' 字符串，历史上有 status=1 的写法；两者都视为"已发布可见"
// target_type 兼容：all=全体 / single=指定用户 / topic=话题组
// 如带了 token 或 user_id，则额外把 single + target_value=对应用户的消息合进来
$messages = [];
try {
    // 解析可选登录态（APP 请求时可能通过 Header 或 GET 传 token / user_id）
    $headers = function_exists('getallheaders') ? (getallheaders() ?: []) : [];
    $authToken = '';
    foreach ($headers as $hk => $hv) {
        if (strcasecmp($hk, 'Authorization') === 0 && stripos($hv, 'Bearer ') === 0) {
            $authToken = trim(substr($hv, 7));
        }
    }
    if ($authToken === '') $authToken = trim($_GET['token'] ?? $_POST['token'] ?? '');
    $reqUserId = (int)($_GET['user_id'] ?? $_POST['user_id'] ?? 0);
    // 用 token 反查 user_id（只要 users.token 列存在，或直接走兼容）
    if ($reqUserId <= 0 && $authToken !== '') {
        try {
            $u = $db->fetchOne("SELECT id FROM users WHERE token = ? LIMIT 1", [$authToken]);
            if ($u) $reqUserId = (int)$u['id'];
        } catch (Exception $e) {
            // token 列可能不存在，静默
            $colExists = false;
            try {
                $col = $db->fetchOne("SHOW COLUMNS FROM `users` LIKE 'token'");
                $colExists = !empty($col);
            } catch (Exception $_) {}
            if (!$colExists) {
                try { $db->query("ALTER TABLE `users` ADD COLUMN `token` VARCHAR(128) DEFAULT NULL COMMENT '会话Token' AFTER `status`"); } catch (Exception $_) {}
            }
        }
    }

    $sql = "SELECT * FROM app_messages WHERE status IN ('sent','sending','1',1)";
    $params = [];
    // 目标范围：全体消息永远返回；指定用户消息在当前 user_id 匹配时返回；话题组暂按全体兼容
    $sql .= " AND (";
    $sql .= " target_type IN ('all','topic','') OR target_type IS NULL";
    if ($reqUserId > 0) {
        $sql .= " OR (target_type = 'single' AND target_value = ?)";
        $params[] = (string)$reqUserId;
    } else {
        // 未登录态不返回 single 消息，防止用户间私信外泄
        $sql .= " OR 1=0";
    }
    $sql .= ")";
    // 排序列兼容：优先按发布时间倒序（sent_at / created_at），历史表有 priority 时也能用
    $sql .= " ORDER BY COALESCE(sent_at, created_at, id) DESC, id DESC LIMIT 20";

    $rows = $db->fetchAll($sql, $params);
    foreach ($rows as $r) {
        $messages[] = [
            'id'         => (int)$r['id'],
            'title'      => $r['title'] ?? '',
            'content'    => $r['content'] ?? '',
            'type'       => $r['type'] ?? $r['message_type'] ?? 'system',
            'created_at' => $r['sent_at'] ?? $r['created_at'] ?? '',
        ];
    }
} catch (Exception $e) {}
// 兜底：如果因为 priority 列报错（老表结构），再用最小字段重查一次
if (empty($messages)) {
    try {
        $rows = $db->fetchAll("SELECT * FROM app_messages WHERE status IN ('sent','1') ORDER BY id DESC LIMIT 20");
        foreach ($rows as $r) {
            $messages[] = [
                'id'         => (int)$r['id'],
                'title'      => $r['title'] ?? '',
                'content'    => $r['content'] ?? '',
                'type'       => $r['type'] ?? $r['message_type'] ?? 'system',
                'created_at' => $r['sent_at'] ?? $r['created_at'] ?? '',
            ];
        }
    } catch (Exception $_) {}
}

echo json_encode([
    'success' => true,
    'message' => 'ok',
    'data' => [
        'auth_invalid'            => $authInvalid,
        'announcement'            => $announcement,
        'maintenance_mode'        => $maintenance,
        'registration_required'   => $regEnabled,
        'captcha_enabled'         => $captchaEnabled,
        'captcha_login_enabled'   => $captchaLoginEnabled,
        'captcha_register_enabled'=> $captchaRegisterEnabled,
        'sliding_login_enabled'   => $slidingLoginEnabled,
        'sliding_register_enabled'=> $slidingRegisterEnabled,
        'sliding_forgot_enabled'  => $slidingForgotEnabled,
        'splash_screen_url'       => toUrl($splashImage),
        'splash_duration'         => $splashDuration,
        'app_name'                => $appName,
        'app_description'         => $appDescription,
        // 启动页
        'splash_app_name'         => $splashAppName,
        'splash_app_description'  => $splashAppDescription,
        'splash_bg_color'         => $splashBgColor,
        'splash_icon_url'         => toUrl($splashIconUrlRaw),
        // 首页顶部
        'home_app_name'           => $homeAppName,
        'home_app_description'    => $homeAppDescription,
        'home_icon_url'           => toUrl($homeIconUrlRaw),
        // 版本 & 消息
        'latest_version'          => $latestVersion,
        'messages'                => $messages,
        'unread_count'            => count($messages),
    ],
], JSON_UNESCAPED_UNICODE);
