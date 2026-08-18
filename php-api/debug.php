<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

echo json_encode([
    'method' => $_SERVER['REQUEST_METHOD'],
    'content_type' => $_SERVER['CONTENT_TYPE'] ?? '',
    'content_length' => $_SERVER['CONTENT_LENGTH'] ?? 0,
    'raw_input' => file_get_contents('php://input'),
    'post_data' => $_POST,
    'http_raw_post_data' => $HTTP_RAW_POST_DATA ?? null,
    'server_software' => $_SERVER['SERVER_SOFTWARE'] ?? '',
    'php_version' => PHP_VERSION,
    'all_headers' => getallheaders(),
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
