<?php
require_once __DIR__ . '/config.php';

$data = getPostData();
$resetToken = trim($data['token'] ?? '');
$newPassword = $data['new_password'] ?? '';

if (empty($resetToken) || empty($newPassword)) {
    jsonResponse(false, '重置码和新密码不能为空');
}

if (strlen($newPassword) < 6) {
    jsonResponse(false, '密码长度至少6位');
}

$db = getDb();

$stmt = $db->prepare('SELECT user_id, expires_at, used FROM password_resets WHERE token = ?');
$stmt->bind_param('s', $resetToken);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    $stmt->close();
    jsonResponse(false, '重置码无效');
}

$row = $result->fetch_assoc();
$stmt->close();

if ($row['used'] == 1) {
    jsonResponse(false, '该重置码已被使用');
}

if (strtotime($row['expires_at']) < time()) {
    jsonResponse(false, '重置码已过期');
}

$hashedPassword = password_hash($newPassword, PASSWORD_DEFAULT);

$stmt = $db->prepare('UPDATE users SET password = ? WHERE id = ?');
$stmt->bind_param('si', $hashedPassword, $row['user_id']);
$stmt->execute();
$stmt->close();

$stmt = $db->prepare('UPDATE password_resets SET used = 1 WHERE token = ?');
$stmt->bind_param('s', $resetToken);
$stmt->execute();
$stmt->close();

jsonResponse(true, '密码重置成功');

$db->close();
