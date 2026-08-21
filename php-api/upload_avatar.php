<?php
/**
 * APP 头像上传接口
 * POST https://qr.wzdi.cn/api/upload_avatar.php
 * 参数: user_id, token?, avatar_base64
 * 返回: { success, message, data:{avatar_url} }
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

$userId        = (int)req('user_id', 0);
$token         = trim(req('token') ?? '');
$avatarBase64  = trim(req('avatar_base64') ?? '');

if ($userId <= 0) {
    echo json_encode(['success' => false, 'message' => '用户 ID 不能为空'], JSON_UNESCAPED_UNICODE);
    exit;
}
if (empty($avatarBase64)) {
    echo json_encode(['success' => false, 'message' => '请上传头像图片'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 解码 base64（兼容 data:image/xxx;base64, 前缀）
if (strpos($avatarBase64, 'data:image') === 0) {
    $avatarBase64 = preg_replace('/^data:image[^;]+;base64,/', '', $avatarBase64);
}
$avatarBase64 = str_replace(' ', '+', $avatarBase64);
$bin = base64_decode($avatarBase64, true);
if ($bin === false || strlen($bin) < 200) {
    echo json_encode(['success' => false, 'message' => '头像图片格式错误'], JSON_UNESCAPED_UNICODE);
    exit;
}
if (strlen($bin) > 2 * 1024 * 1024) {
    echo json_encode(['success' => false, 'message' => '图片大小不能超过 2MB'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 验证头像格式（前几个字节判真）
$finfo = new finfo(FILEINFO_MIME_TYPE);
$mime = $finfo->buffer($bin);
$allowed = ['image/jpeg','image/png','image/webp','image/gif','image/bmp'];
if (!in_array($mime, $allowed, true)) {
    echo json_encode(['success' => false, 'message' => '仅支持 JPG/PNG/WebP/GIF/BMP 图片'], JSON_UNESCAPED_UNICODE);
    exit;
}
$extMap = ['image/jpeg'=>'jpg','image/png'=>'png','image/webp'=>'webp','image/gif'=>'gif','image/bmp'=>'bmp'];
$ext = $extMap[$mime] ?? 'png';

// 写入 uploads/avatars/
$uploadRoot = dirname(__DIR__) . '/uploads';
$avatarDir  = $uploadRoot . '/avatars';
if (!is_dir($avatarDir)) @mkdir($avatarDir, 0755, true);
$fileName = 'u' . $userId . '_' . time() . '.' . $ext;
$filePath = $avatarDir . '/' . $fileName;
@file_put_contents($filePath, $bin);

$relPath = '/uploads/avatars/' . $fileName;
$absUrl  = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https://' : 'http://') . $_SERVER['HTTP_HOST'] . $relPath;

try {
    $db->query("UPDATE users SET avatar = ?, avatarUrl = ? WHERE id = ?", [$relPath, $absUrl, $userId]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '数据库写入失败: ' . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode([
    'success' => true,
    'message' => '上传成功',
    'data' => [
        'avatar'     => $relPath,
        'avatar_url' => $absUrl,
    ],
], JSON_UNESCAPED_UNICODE);
