<?php
session_start();
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/db.php';
require_once __DIR__ . '/../includes/functions.php';

date_default_timezone_set($config['site']['timezone'] ?? 'Asia/Shanghai');

header('Content-Type: application/json');
header('Cache-Control: no-store, no-cache, must-revalidate');

$userId = $_SESSION['bottle_qr_user']['id'] ?? ($_SESSION['admin_id'] ?? 0);

$type = $_GET['type'] ?? 'qr';

if ($type === 'dyn') {
    // 活码扫码次数
    $result = [];
    $tables = ['link' => 'dynamic_qrcodes', 'image' => 'image_qrcodes', 'text' => 'text_qrcodes'];
    foreach ($tables as $t => $table) {
        $rawIds = $_GET['ids_' . $t] ?? [];
        if (!is_array($rawIds)) {
            $rawIds = explode(',', (string)$rawIds);
        }
        $ids = array_filter(array_map('intval', $rawIds));
        if (!empty($ids)) {
            $idList = implode(',', $ids);
            try {
                $rows = $db->fetchAll("SELECT id, scan_count, last_scan FROM `{$table}` WHERE id IN ($idList)");
                foreach ($rows as $r) {
                    $result[$t . '_' . $r['id']] = [
                        'count' => (int)$r['scan_count'],
                        'last_scan' => $r['last_scan']
                    ];
                }
            } catch (Exception $e) {}
        }
    }
    echo json_encode($result);
    exit;
}

// 二维码扫码次数
$rawIds = $_GET['ids'] ?? [];
if (!is_array($rawIds)) {
    $rawIds = explode(',', (string)$rawIds);
}
$ids = array_filter(array_map('intval', $rawIds));
if (empty($ids)) { echo json_encode([]); exit; }

$idList = implode(',', $ids);
try {
    $rows = $db->fetchAll("SELECT id, scan_count, last_scan_time FROM qr_codes WHERE id IN ($idList)");
} catch (Exception $e) {
    $rows = $db->fetchAll("SELECT id, scan_count FROM qr_codes WHERE id IN ($idList)");
}

$result = [];
foreach ($rows as $r) {
    $result[$r['id']] = [
        'count' => (int)$r['scan_count'],
        'last_scan' => $r['last_scan_time'] ?? null
    ];
}
echo json_encode($result);
