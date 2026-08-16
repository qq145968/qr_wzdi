<?php
require_once __DIR__ . '/config.php';

$conn = new mysqli(DB_HOST, DB_USER, DB_PASS, '', DB_PORT);
if ($conn->connect_error) {
    die('Connection failed: ' . $conn->connect_error);
}
$conn->set_charset('utf8mb4');

$dbName = DB_NAME;
$conn->query("CREATE DATABASE IF NOT EXISTS `$dbName` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$conn->select_db($dbName);

$conn->query("CREATE TABLE IF NOT EXISTS `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) DEFAULT '',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$conn->query("CREATE TABLE IF NOT EXISTS `password_resets` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `token` VARCHAR(64) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `used` TINYINT DEFAULT 0,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$conn->query("CREATE TABLE IF NOT EXISTS `scan_records` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `code` VARCHAR(500) NOT NULL,
    `type` VARCHAR(20) DEFAULT 'qrcode',
    `scan_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$conn->query("CREATE TABLE IF NOT EXISTS `captcha_codes` (
    `id` VARCHAR(32) PRIMARY KEY,
    `code` VARCHAR(10) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `used` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$conn->query("CREATE TABLE IF NOT EXISTS `app_settings` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `setting_key` VARCHAR(100) NOT NULL UNIQUE,
    `setting_value` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$conn->query("CREATE TABLE IF NOT EXISTS `app_versions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `version_code` INT NOT NULL,
    `version_name` VARCHAR(20) NOT NULL,
    `download_url` TEXT,
    `update_content` TEXT,
    `force_update` TINYINT DEFAULT 0,
    `file_size` BIGINT DEFAULT 0,
    `is_release` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$conn->query("CREATE TABLE IF NOT EXISTS `app_messages` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `message_type` VARCHAR(20) DEFAULT 'system',
    `status` VARCHAR(20) DEFAULT 'sent',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

$defaultSettings = [
    ['announcement', '欢迎使用扫码机器人'],
    ['maintenance_mode', '0'],
    ['registration_required', '1'],
    ['captcha_enabled', '1'],
    ['splash_screen_url', ''],
    ['app_name', '扫码机器人'],
    ['app_description', '让手机变成扫码枪']
];
$checkStmt = $conn->prepare("SELECT id FROM app_settings WHERE setting_key = ? LIMIT 1");
$insertStmt = $conn->prepare("INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)");
foreach ($defaultSettings as $setting) {
    $checkStmt->bind_param('s', $setting[0]);
    $checkStmt->execute();
    $checkStmt->store_result();
    if ($checkStmt->num_rows === 0) {
        $insertStmt->bind_param('ss', $setting[0], $setting[1]);
        $insertStmt->execute();
    }
    $checkStmt->free_result();
}
$checkStmt->close();
$insertStmt->close();

$conn->query("DELETE FROM captcha_codes WHERE created_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)");

echo json_encode(['success' => true, 'message' => 'Database initialized successfully']);
$conn->close();
