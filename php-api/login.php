<?php
/**
 * APP 登录接口
 * POST https://qr.wzdi.cn/api/login.php
 * 参数: username, password, captcha_id?, captcha_code?
 * 返回: { success, message, user:{id, username, email, phone, avatar, avatar_url, token, source, status} }
 */
require_once __DIR__ . '/config.php';

$raw = file_get_contents('php://input');
$jsonReq = [];
if ($raw) {
    $j = json_decode($raw, true);
    if (is_array($j)) $jsonReq = $j;
}
function req($key, $default = null) {
    global $jsonReq;
    if (isset($_POST[$key]) && $_POST[$key] !== '') return $_POST[$key];
    if (isset($jsonReq[$key]) && $jsonReq[$key] !== '') return $jsonReq[$key];
    return $default;
}

$username    = trim(req('username') ?? req('login') ?? '');
$password    = req('password') ?? '';
$captchaId   = trim(req('captcha_id') ?? '');
$captchaCode = trim(req('captcha_code') ?? '');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => '仅支持 POST 请求'], JSON_UNESCAPED_UNICODE);
    exit;
}
if (empty($username) || empty($password)) {
    echo json_encode(['success' => false, 'message' => '请输入账号和密码'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 验证码开关（后台 APP 设置页使用 code_login_enabled / code_register_enabled；兼容旧键做兜底）
try {
    $rows = $db->fetchAll("SELECT setting_key, setting_value FROM app_settings");
    $settings = [];
    foreach ($rows as $r) $settings[$r['setting_key']] = $r['setting_value'];
    $loginCaptcha = $settings['code_login_enabled'] ?? null;
    if ($loginCaptcha === null || $loginCaptcha === '') {
        $loginCaptcha = $settings['app_verification_enabled'] ?? $settings['captcha_enabled'] ?? '0';
    }
    $captchaEnabled = $loginCaptcha === '1';
} catch (Exception $e) {
    $captchaEnabled = false;
}

if ($captchaEnabled) {
    if (empty($captchaId) || empty($captchaCode)) {
        echo json_encode(['success' => false, 'message' => '请输入验证码'], JSON_UNESCAPED_UNICODE);
        exit;
    }
    $row = $db->fetchOne("SELECT id, code, used FROM captcha_codes WHERE id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE) LIMIT 1", [$captchaId]);
    if (!$row) {
        echo json_encode(['success' => false, 'message' => '验证码已过期，请刷新'], JSON_UNESCAPED_UNICODE);
        exit;
    }
    if ((int)$row['used'] === 1) {
        echo json_encode(['success' => false, 'message' => '验证码已被使用，请刷新'], JSON_UNESCAPED_UNICODE);
        exit;
    }
    if (strcasecmp((string)$row['code'], $captchaCode) !== 0) {
        echo json_encode(['success' => false, 'message' => '验证码错误'], JSON_UNESCAPED_UNICODE);
        exit;
    }
    $db->query("UPDATE captcha_codes SET used = 1 WHERE id = ?", [$captchaId]);
}

// 登录验证
try {
    $found = $db->fetchOne("SELECT id, username, password, email, phone, avatar, avatarUrl, status, register_source, login_count FROM users WHERE (username = ? OR email = ?) AND register_source = 'app' LIMIT 1", [$username, $username]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '数据库错误: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

// 账号互通：app没找到，查website用户，找到则自动复制一份到app
if (!$found) {
    try {
        $webUser = $db->fetchOne("SELECT id, username, password, email, phone, avatar, avatarUrl, status, register_source FROM users WHERE (username = ? OR email = ?) AND status = 1 AND (register_source = 'website' OR register_source IS NULL OR register_source = '') LIMIT 1", [$username, $username]);
        if ($webUser && password_verify($password, $webUser['password'])) {
            // 复制到app
            $db->query(
                "INSERT INTO users (username, password, email, phone, register_source, status, created_at) VALUES (?, ?, ?, ?, 'app', 1, NOW())",
                [$webUser['username'], $webUser['password'], $webUser['email'], $webUser['phone'] ?? '']
            );
            $newId = (int)$db->fetchOne("SELECT LAST_INSERT_ID() as c")['c'];
            $found = $db->fetchOne("SELECT id, username, password, email, phone, avatar, avatarUrl, status, register_source, login_count FROM users WHERE id = ?", [$newId]);
        }
    } catch (Exception $e) {
        // 忽略
    }
}

if (!$found || !password_verify($password, $found['password'])) {
    echo json_encode(['success' => false, 'message' => '用户名或密码错误'], JSON_UNESCAPED_UNICODE);
    exit;
}
if ((int)$found['status'] !== 1) {
    echo json_encode(['success' => false, 'message' => '账号已被禁用，请联系管理员'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 生成简单 token
$token = sha1($found['id'] . '|' . $found['username'] . '|' . microtime(true) . '|' . rand(1000, 9999));

// 更新登录信息 + 写入 token（用于 app_info.php 按当前用户拉取 single 类消息/通知）
try {
    $db->query("UPDATE users SET login_count = login_count + 1, last_login = NOW(), token = ? WHERE id = ?", [$token, (int)$found['id']]);
} catch (Exception $e) {
    // token 列可能不存在（老库），补齐并重试
    try {
        $col = $db->fetchOne("SHOW COLUMNS FROM `users` LIKE 'token'");
        if (empty($col)) {
            $db->query("ALTER TABLE `users` ADD COLUMN `token` VARCHAR(128) DEFAULT NULL COMMENT '会话Token' AFTER `status`");
        }
        $db->query("UPDATE users SET login_count = login_count + 1, last_login = NOW(), token = ? WHERE id = ?", [$token, (int)$found['id']]);
    } catch (Exception $_) {
        // 实在失败就只更新基础字段（不影响主流程）
        try { $db->query("UPDATE users SET login_count = login_count + 1, last_login = NOW() WHERE id = ?", [(int)$found['id']]); } catch (Exception $__) {}
    }
}

$avatar = !empty($found['avatar']) ? $found['avatar'] : strtoupper(mb_substr($found['username'], 0, 1, 'UTF-8'));
$avatarUrl = '';
if (!empty($found['avatarUrl'])) {
    $avatarUrl = $found['avatarUrl'];
} elseif (!empty($found['avatar']) && strpos($found['avatar'], 'http') !== 0) {
    $avatarUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https://' : 'http://') . $_SERVER['HTTP_HOST'] . $found['avatar'];
} elseif (!empty($found['avatar'])) {
    $avatarUrl = $found['avatar'];
}

echo json_encode([
    'success' => true,
    'message' => '登录成功',
    'data' => [
        'user_id'  => (int)$found['id'],
        'username' => $found['username'],
        'email'    => $found['email'] ?? '',
        'phone'    => $found['phone'] ?? '',
        'avatar'   => $avatar,
        'avatar_url' => $avatarUrl,
        'token'    => $token,
        'status'   => (int)$found['status'],
        'source'   => $found['register_source'] ?? 'website',
        'login_count' => (int)$found['login_count'],
        'last_login'  => $found['last_login'] ?? date('Y-m-d H:i:s'),
    ],
    'user' => [
        'id'          => (int)$found['id'],
        'username'    => $found['username'],
        'email'       => $found['email'] ?? '',
        'phone'       => $found['phone'] ?? '',
        'avatar'      => $avatar,
        'avatar_url'  => $avatarUrl,
        'token'       => $token,
        'status'      => (int)$found['status'],
        'source'      => $found['register_source'] ?? 'website',
        'login_count' => (int)$found['login_count'],
        'last_login'  => $found['last_login'] ?? date('Y-m-d H:i:s'),
    ],
], JSON_UNESCAPED_UNICODE);
