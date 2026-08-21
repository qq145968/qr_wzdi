<?php
/**
 * APP 注册接口
 * POST https://qr.wzdi.cn/api/register.php
 * 参数: username, password, email, phone?, captcha_id?, captcha_code?
 * 返回: { success, message, user:{user_id, username, email, token, source} }
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

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => '仅支持 POST 请求'], JSON_UNESCAPED_UNICODE);
    exit;
}

$username    = trim(req('username') ?? '');
$password    = req('password') ?? '';
$email       = trim(strtolower(req('email') ?? ''));
$phone       = trim(req('phone') ?? '');
$captchaId   = trim(req('captcha_id') ?? '');
$captchaCode = trim(req('captcha_code') ?? '');

// 1. 读取 APP 后台开关
try {
    $rows = $db->fetchAll("SELECT setting_key, setting_value FROM app_settings");
    $settings = [];
    foreach ($rows as $r) $settings[$r['setting_key']] = $r['setting_value'];
    $regEnabled     = !isset($settings['app_registration_enabled']) || $settings['app_registration_enabled'] === '1';
    // 后台 APP 设置页使用 code_login_enabled / code_register_enabled；兼容旧键做兜底
    $regCaptcha = $settings['code_register_enabled'] ?? null;
    if ($regCaptcha === null || $regCaptcha === '') {
        $regCaptcha = $settings['app_verification_enabled'] ?? $settings['captcha_enabled'] ?? '0';
    }
    $captchaEnabled = $regCaptcha === '1';
} catch (Exception $e) {
    $regEnabled = true;
    $captchaEnabled = false;
}
if (!$regEnabled) {
    echo json_encode(['success' => false, 'message' => '当前已关闭 APP 用户注册'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 2. 参数校验
if (mb_strlen($username) < 3 || mb_strlen($username) > 20) {
    echo json_encode(['success' => false, 'message' => '用户名长度需 3-20 个字符'], JSON_UNESCAPED_UNICODE);
    exit;
}
if (strlen($password) < 6) {
    echo json_encode(['success' => false, 'message' => '密码至少 6 位'], JSON_UNESCAPED_UNICODE);
    exit;
}
if ($email !== '' && !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode(['success' => false, 'message' => '邮箱格式不正确'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 3. 验证码校验
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

// 4. 查重（账号互通：全局查重，不区分来源，精准提示）
try {
    $u = $db->fetchOne("SELECT id FROM users WHERE username = ? LIMIT 1", [$username]);
    if ($u) {
        echo json_encode(['success' => false, 'message' => '用户名已存在'], JSON_UNESCAPED_UNICODE);
        exit;
    }
    if ($email !== '') {
        $e = $db->fetchOne("SELECT id FROM users WHERE email = ? LIMIT 1", [$email]);
        if ($e) {
            echo json_encode(['success' => false, 'message' => '邮箱已存在'], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }
    if ($phone !== '') {
        $p = $db->fetchOne("SELECT id FROM users WHERE phone = ? LIMIT 1", [$phone]);
        if ($p) {
            echo json_encode(['success' => false, 'message' => '手机号已存在'], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }
} catch (Exception $e) {
    // 忽略
}

// 5. 写入用户（固定 source = 'app'）
$hash = password_hash($password, PASSWORD_DEFAULT);
try {
    $db->query(
        "INSERT INTO users (username, password, email, phone, register_source, status, created_at) VALUES (?, ?, ?, ?, 'app', 1, NOW())",
        [$username, $hash, $email, $phone]
    );
    $userId = (int)$db->fetchOne("SELECT LAST_INSERT_ID() as c")['c'];
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '写入数据库失败: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}
$token = sha1($userId . '|' . $username . '|' . microtime(true) . '|' . rand(1000, 9999));

// 把 token 写回 users，方便 app_info.php 反查用户ID（single 类消息精准分发）
try {
    $db->query("UPDATE users SET token = ? WHERE id = ?", [$token, $userId]);
} catch (Exception $e) {
    try {
        $col = $db->fetchOne("SHOW COLUMNS FROM `users` LIKE 'token'");
        if (empty($col)) {
            $db->query("ALTER TABLE `users` ADD COLUMN `token` VARCHAR(128) DEFAULT NULL COMMENT '会话Token' AFTER `status`");
        }
        $db->query("UPDATE users SET token = ? WHERE id = ?", [$token, $userId]);
    } catch (Exception $_) {}
}

echo json_encode([
    'success' => true,
    'message' => '注册成功',
    'data' => [
        'user_id'  => $userId,
        'username' => $username,
        'email'    => $email,
        'token'    => $token,
        'source'   => 'app',
    ],
    'user' => [
        'user_id'  => $userId,
        'username' => $username,
        'email'    => $email,
        'token'    => $token,
        'source'   => 'app',
    ],
], JSON_UNESCAPED_UNICODE);
