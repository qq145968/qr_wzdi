<?php
/**
 * APP 找回密码接口 - 发送重置码
 * POST https://qr.wzdi.cn/api/forgot_password.php
 * 参数: email, username
 * 返回: { success, message, data:{token} }
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

$email    = trim(strtolower(req('email') ?? ''));
$username = trim(req('username') ?? '');

if (empty($email) && empty($username)) {
    echo json_encode(['success' => false, 'message' => '请输入邮箱或用户名'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 查找用户（跨来源查找，优先 app 来源）
try {
    if ($email) {
        $user = $db->fetchOne(
            "SELECT id, username, email, register_source, status FROM users WHERE email = ? AND status = 1 ORDER BY FIELD(register_source, 'app', 'website', '') DESC LIMIT 1",
            [$email]
        );
    } else {
        $user = $db->fetchOne(
            "SELECT id, username, email, register_source, status FROM users WHERE username = ? AND status = 1 ORDER BY FIELD(register_source, 'app', 'website', '') DESC LIMIT 1",
            [$username]
        );
    }
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '数据库错误: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

if (!$user) {
    echo json_encode(['success' => false, 'message' => '该账号不存在或已被禁用'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 生成 6 位数字重置码
$resetCode = str_pad(random_int(0, 999999), 6, '0', STR_PAD_LEFT);
$expires = date('Y-m-d H:i:s', time() + 600); // 10 分钟有效

// 存储重置码到用户记录
try {
    $db->update('users', [
        'reset_token' => $resetCode,
        'reset_expires' => $expires,
    ], ['id' => $user['id']]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '重置码生成失败: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

// 发送重置码邮件
$mailConfigured = !empty($config['mail']['host']) && !empty($config['mail']['username']);
$mailSent = false;
$mailError = '';
if ($mailConfigured) {
    require_once $rootDir . '/includes/MailHelper.php';
    $mailer = new MailHelper($config['mail']);
    $siteName = $config['site']['name'] ?? '二维码管理系统';
    $subject = "密码重置码 - {$siteName}";
    $html = "<div style='font-family:Arial;max-width:500px;margin:0 auto;padding:20px;'>"
        . "<h2 style='color:#333;'>密码重置</h2>"
        . "<p>您正在重置 {$siteName} 的账户密码。</p>"
        . "<p>您的重置码是：</p>"
        . "<div style='text-align:center;margin:20px 0;'>"
        . "<span style='font-size:32px;font-weight:bold;letter-spacing:8px;color:#6366f1;background:#f5f5f5;padding:10px 20px;border-radius:8px;'>{$resetCode}</span>"
        . "</div>"
        . "<p style='color:#999;font-size:14px;'>重置码10分钟内有效，请尽快使用。如非本人操作请忽略此邮件。</p>"
        . "</div>";
    $mailSent = $mailer->send($user['email'], $subject, $html, "您的密码重置码是：{$resetCode}，10分钟内有效。");
    if (!$mailSent) {
        $mailError = $mailer->getError();
    }
}

if ($mailSent) {
    $message = "重置码已发送至邮箱 {$user['email']}，10分钟内有效";
} elseif ($mailConfigured) {
    $message = "邮件发送失败（{$mailError}），请联系管理员";
    echo json_encode(['success' => false, 'message' => $message], JSON_UNESCAPED_UNICODE);
    exit;
} else {
    $message = "邮件服务未配置，请联系管理员";
    echo json_encode(['success' => false, 'message' => $message], JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode([
    'success' => true,
    'message' => $message,
    'data' => [
        'username' => $user['username'],
    ],
], JSON_UNESCAPED_UNICODE);
