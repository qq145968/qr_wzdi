<?php
/**
 * APP 重置密码接口
 * POST https://qr.wzdi.cn/api/reset_password.php
 * 参数: token(重置码), new_password
 * 返回: { success, message }
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

$token       = trim(req('token') ?? '');
$newPassword = req('new_password') ?? '';

if (empty($token)) {
    echo json_encode(['success' => false, 'message' => '请输入重置码'], JSON_UNESCAPED_UNICODE);
    exit;
}
if (strlen($newPassword) < 6) {
    echo json_encode(['success' => false, 'message' => '新密码至少6位'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 验证重置码
try {
    $user = $db->fetchOne(
        "SELECT id, username, email, register_source FROM users WHERE reset_token = ? AND reset_expires > NOW() AND status = 1 LIMIT 1",
        [$token]
    );
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '数据库错误: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

if (!$user) {
    echo json_encode(['success' => false, 'message' => '重置码无效或已过期'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 重置密码：同步更新同一用户名/邮箱的所有记录（跨来源同步密码）
try {
    $newHash = password_hash($newPassword, PASSWORD_DEFAULT);

    // 更新匹配重置码的用户
    $db->update('users', [
        'password' => $newHash,
        'reset_token' => null,
        'reset_expires' => null,
    ], ['id' => $user['id']]);

    // 同步更新同用户名其他来源的密码
    $db->query(
        "UPDATE users SET password = ? WHERE username = ? AND id != ?",
        [$newHash, $user['username'], $user['id']]
    );
    // 同步更新同邮箱其他来源的密码
    $db->query(
        "UPDATE users SET password = ? WHERE email = ? AND id != ?",
        [$newHash, $user['email'], $user['id']]
    );
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '密码重置失败: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode([
    'success' => true,
    'message' => '密码重置成功，请重新登录',
    'data' => [
        'username' => $user['username'],
    ],
], JSON_UNESCAPED_UNICODE);
