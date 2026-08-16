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

$announcement = getSetting('announcement', '欢迎使用扫码机器人');
$maintenanceMode = getSetting('maintenance_mode', '0') === '1';
$registrationRequired = getSetting('registration_required', '1') === '1';
$appName = getSetting('app_name', '扫码机器人');
$appDescription = getSetting('app_description', '让手机变成扫码枪');

$conn = getDb();

// Get latest published version
$versionResult = $conn->query("SELECT * FROM app_versions WHERE is_release = 1 ORDER BY version_code DESC LIMIT 1");
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

// Get sent messages
$msgResult = $conn->query("SELECT * FROM app_messages WHERE status = 'sent' ORDER BY created_at DESC LIMIT 10");
$messages = [];
if ($msgResult) {
    while ($row = $msgResult->fetch_assoc()) {
        $messages[] = [
            'id' => (int)$row['id'],
            'title' => $row['title'],
            'content' => $row['content'] ?? '',
            'type' => $row['message_type'] ?? 'system',
            'created_at' => $row['created_at'] ?? ''
        ];
    }
}

jsonResponse(true, 'ok', [
    'announcement' => $announcement,
    'maintenance_mode' => $maintenanceMode,
    'registration_required' => $registrationRequired,
    'app_name' => $appName,
    'app_description' => $appDescription,
    'latest_version' => $latestVersion,
    'messages' => $messages,
    'unread_count' => count($messages)
]);
