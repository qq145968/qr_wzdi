<?php
require_once __DIR__ . '/config.php';

function getSetting($key, $default = '') {
    $conn = getDb();
    $stmt = $conn->prepare("SELECT setting_value FROM app_settings WHERE setting_key = ? LIMIT 1");
    $stmt->bind_param("s", $key);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($row = $result->fetch_assoc()) {
        $stmt->close();
        return $row['setting_value'];
    }
    $stmt->close();
    return $default;
}

$data = getPostData();
$username = trim($data['username'] ?? '');
$password = $data['password'] ?? '';
$captchaId = trim($data['captcha_id'] ?? '');
$captchaCode = trim($data['captcha_code'] ?? '');

if (empty($username) || empty($password)) {
    jsonResponse(false, '用户名和密码不能为空');
}

$captchaEnabled = getSetting('app_verification_enabled', getSetting('captcha_enabled', '0')) === '1';
if ($captchaEnabled) {
    if (empty($captchaId) || empty($captchaCode)) {
        jsonResponse(false, '请输入验证码');
    }
    $db = getDb();
    $stmt = $db->prepare("SELECT code, used FROM captcha_codes WHERE id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE) LIMIT 1");
    $stmt->bind_param('s', $captchaId);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($result->num_rows === 0) {
        $stmt->close();
        jsonResponse(false, '验证码已过期，请刷新');
    }
    $row = $result->fetch_assoc();
    $stmt->close();
    if ($row['used'] == 1) {
        jsonResponse(false, '验证码已被使用，请刷新');
    }
    if (strcasecmp($row['code'], $captchaCode) !== 0) {
        jsonResponse(false, '验证码错误');
    }
    $updateStmt = $db->prepare("UPDATE captcha_codes SET used = 1 WHERE id = ?");
    $updateStmt->bind_param('s', $captchaId);
    $updateStmt->execute();
    $updateStmt->close();
}

$db = getDb();

$stmt = $db->prepare('SELECT id, username, password, email FROM users WHERE username = ?');
$stmt->bind_param('s', $username);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    $stmt->close();
    jsonResponse(false, '用户不存在');
}

$user = $result->fetch_assoc();
$stmt->close();

if (!password_verify($password, $user['password'])) {
    jsonResponse(false, '密码错误');
}

$token = generateToken($user['id']);

jsonResponse(true, '登录成功', [
    'user_id' => (int)$user['id'],
    'username' => $user['username'],
    'email' => $user['email'],
    'token' => $token
]);

$db->close();
