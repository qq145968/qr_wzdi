<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

define('DB_HOST', '127.0.0.1');
define('DB_NAME', 'qr_wzdi_cn');
define('DB_USER', 'qr_wzdi_cn');
define('DB_PASS', 'nFhKHWxriPK257hh');
define('DB_PORT', 3306);

define('JWT_SECRET', 'scan_robot_jwt_secret_2026');

function jsonResponse($success, $message, $data = null) {
    echo json_encode([
        'success' => $success,
        'message' => $message,
        'data' => $data
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

function getPostData() {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);
    if ($data === null) {
        $data = $_POST;
    }
    return $data;
}

function getDb() {
    static $conn = null;
    if ($conn === null) {
        $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME, DB_PORT);
        if ($conn->connect_error) {
            jsonResponse(false, '数据库连接失败: ' . $conn->connect_error);
        }
        $conn->set_charset('utf8mb4');
    }
    return $conn;
}

function generateToken($userId) {
    $header = base64_encode(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
    $payload = base64_encode(json_encode([
        'user_id' => $userId,
        'iat' => time(),
        'exp' => time() + 86400 * 30
    ]));
    $signature = hash_hmac('sha256', $header . '.' . $payload, JWT_SECRET, true);
    $sig = base64_encode($signature);
    return $header . '.' . $payload . '.' . $sig;
}

function verifyToken($token) {
    if (!$token) return false;
    $parts = explode('.', $token);
    if (count($parts) !== 3) return false;
    $signature = base64_encode(hash_hmac('sha256', $parts[0] . '.' . $parts[1], JWT_SECRET, true));
    if ($signature !== $parts[2]) return false;
    $payload = json_decode(base64_decode($parts[1]), true);
    if (!$payload || $payload['exp'] < time()) return false;
    return $payload;
}
