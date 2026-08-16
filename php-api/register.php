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
$email = trim($data['email'] ?? '');
$captchaId = trim($data['captcha_id'] ?? '');
$captchaCode = trim($data['captcha_code'] ?? '');

$registrationRequired = getSetting('registration_required', getSetting('app_registration_enabled', '1')) === '1';
if (!$registrationRequired) {
    jsonResponse(false, '当前已关闭注册，请联系管理员');
}

if (empty($username) || empty($password)) {
    jsonResponse(false, '用户名和密码不能为空');
}

if (strlen($username) < 3 || strlen($username) > 20) {
    jsonResponse(false, '用户名长度需3-20个字符');
}

if (strlen($password) < 6) {
    jsonResponse(false, '密码长度至少6位');
}

if (!empty($email) && !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    jsonResponse(false, '邮箱格式不正确');
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

$check = $db->prepare('SELECT id FROM users WHERE username = ?');
$check->bind_param('s', $username);
$check->execute();
$check->store_result();
if ($check->num_rows > 0) {
    $check->close();
    jsonResponse(false, '用户名已存在');
}
$check->close();

$hashedPassword = password_hash($password, PASSWORD_DEFAULT);

$stmt = $db->prepare('INSERT INTO users (username, password, email) VALUES (?, ?, ?)');
$stmt->bind_param('sss', $username, $hashedPassword, $email);

if ($stmt->execute()) {
    $userId = $db->insert_id;
    $token = generateToken($userId);
    jsonResponse(true, '注册成功', [
        'user_id' => $userId,
        'username' => $username,
        'token' => $token
    ]);
} else {
    jsonResponse(false, '注册失败: ' . $stmt->error);
}

$stmt->close();
$db->close();
