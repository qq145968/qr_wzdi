<?php
require_once __DIR__ . '/config.php';

$data = getPostData();
$email = trim($data['email'] ?? '');
$username = trim($data['username'] ?? '');

if (empty($email)) {
    jsonResponse(false, '请输入注册邮箱');
}

$db = getDb();

if (!empty($username)) {
    $stmt = $db->prepare('SELECT id, email FROM users WHERE username = ? AND email = ?');
    $stmt->bind_param('ss', $username, $email);
} else {
    $stmt = $db->prepare('SELECT id, email FROM users WHERE email = ?');
    $stmt->bind_param('s', $email);
}

$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    $stmt->close();
    jsonResponse(false, '该邮箱未注册或与用户名不匹配');
}

$user = $result->fetch_assoc();
$stmt->close();

$resetToken = bin2hex(random_bytes(32));
$expiresAt = date('Y-m-d H:i:s', time() + 3600);

$stmt = $db->prepare('INSERT INTO password_resets (user_id, token, expires_at) VALUES (?, ?, ?)');
$stmt->bind_param('iss', $user['id'], $resetToken, $expiresAt);
$stmt->execute();
$stmt->close();

$subject = '=?B?'.base64_encode('扫码机器人 - 密码重置').'?=';
$message = "您正在重置扫码机器人密码。\n\n";
$message .= "重置码: $resetToken\n\n";
$message .= "该重置码1小时内有效。\n";
$message .= "如果这不是您本人的操作，请忽略此邮件。";

$headers = 'From: noreply@qr.wzdi.cn' . "\r\n";
$headers .= 'Content-Type: text/plain; charset=utf-8' . "\r\n";

$mailSent = @mail($user['email'], $subject, $message, $headers);

if ($mailSent) {
    jsonResponse(true, '重置码已发送到您的邮箱');
} else {
    jsonResponse(true, '重置码已生成', ['reset_token' => $resetToken]);
}

$db->close();
