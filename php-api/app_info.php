<?php
require_once __DIR__ . '/config.php';

function ensureAppTables() {
    $conn = getDb();
    $conn->query("CREATE TABLE IF NOT EXISTS app_settings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        setting_key VARCHAR(100) UNIQUE NOT NULL,
        setting_value TEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    $conn->query("CREATE TABLE IF NOT EXISTS app_messages (
        id INT AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(200) NOT NULL,
        content TEXT NOT NULL,
        msg_type VARCHAR(50) DEFAULT 'system',
        target_users VARCHAR(50) DEFAULT 'all',
        status VARCHAR(20) DEFAULT 'sent',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    $conn->query("CREATE TABLE IF NOT EXISTS app_versions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        version_code INT NOT NULL,
        version_name VARCHAR(50) NOT NULL,
        platform VARCHAR(20) DEFAULT 'all',
        download_url TEXT,
        update_content TEXT,
        force_update TINYINT DEFAULT 0,
        is_published TINYINT DEFAULT 1,
        file_size INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    $defaults = [
        'announcement' => '欢迎使用扫码机器人，让手机变成扫码枪',
        'maintenance_mode' => '0',
        'registration_required' => '1',
        'app_name' => '扫码机器人',
        'app_description' => '让手机变成扫码枪',
        'auto_check_update' => '1',
        'push_notification' => '1',
        'min_version_code' => '1',
        'check_frequency' => 'on_launch',
    ];
    foreach ($defaults as $key => $value) {
        $conn->query("INSERT IGNORE INTO app_settings (setting_key, setting_value) VALUES ('$key', '" . $conn->real_escape_string($value) . "')");
    }
}

function getSetting($key, $default = '') {
    $conn = getDb();
    $result = $conn->query("SELECT setting_value FROM app_settings WHERE setting_key = '" . $conn->real_escape_string($key) . "' LIMIT 1");
    if ($result && $row = $result->fetch_assoc()) {
        return $row['setting_value'];
    }
    return $default;
}

function getSettings($keys) {
    $conn = getDb();
    $settings = [];
    $keyList = implode("','", array_map([$conn, 'real_escape_string'], $keys));
    $result = $conn->query("SELECT setting_key, setting_value FROM app_settings WHERE setting_key IN ('$keyList')");
    if ($result) {
        while ($row = $result->fetch_assoc()) {
            $settings[$row['setting_key']] = $row['setting_value'];
        }
    }
    return $settings;
}

ensureAppTables();

$settings = getSettings([
    'announcement', 'maintenance_mode', 'registration_required',
    'app_name', 'app_description', 'auto_check_update',
    'min_version_code', 'check_frequency'
]);

$announcement = $settings['announcement'] ?? '欢迎使用扫码机器人';
$maintenanceMode = ($settings['maintenance_mode'] ?? '0') === '1';
$registrationRequired = ($settings['registration_required'] ?? '1') === '1';

$conn = getDb();

$versionResult = $conn->query("SELECT * FROM app_versions WHERE is_published = 1 ORDER BY version_code DESC LIMIT 1");
$latestVersion = null;
if ($versionResult && $row = $versionResult->fetch_assoc()) {
    $latestVersion = [
        'version_code' => (int)$row['version_code'],
        'version_name' => $row['version_name'],
        'download_url' => $row['download_url'] ?? '',
        'update_content' => $row['update_content'] ?? '',
        'force_update' => (int)$row['force_update'] === 1,
        'file_size' => (int)($row['file_size'] ?? 0)
    ];
}

$msgResult = $conn->query("SELECT * FROM app_messages WHERE status = 'sent' ORDER BY created_at DESC LIMIT 10");
$messages = [];
if ($msgResult) {
    while ($row = $msgResult->fetch_assoc()) {
        $messages[] = [
            'id' => (int)$row['id'],
            'title' => $row['title'],
            'content' => $row['content'],
            'type' => $row['msg_type'],
            'created_at' => $row['created_at']
        ];
    }
}

jsonResponse(true, 'ok', [
    'announcement' => $announcement,
    'maintenance_mode' => $maintenanceMode,
    'registration_required' => $registrationRequired,
    'app_name' => $settings['app_name'] ?? '扫码机器人',
    'app_description' => $settings['app_description'] ?? '让手机变成扫码枪',
    'latest_version' => $latestVersion,
    'messages' => $messages,
    'unread_count' => count($messages)
]);
