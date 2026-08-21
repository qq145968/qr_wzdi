<?php
/**
 * api/config.php — 供 api/* 入口共用：引入根目录 includes/ 并定义 getDb/jsonResponse/getPostData/generateToken
 * 用法：api/ 内文件头部 require_once __DIR__ . '/config.php';
 */

$rootDir = dirname(__DIR__);
require_once $rootDir . '/includes/db.php';
require_once $rootDir . '/includes/functions.php';
require_once $rootDir . '/config.php';

// 所有 API 默认 header
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, X-Requested-With');

// ===== 自动补齐 APP 所需表（首次调用API时自动创建，防止报错）=====
try {
    // 1. users 表补齐 avatarUrl 字段
    $col = $db->fetchOne("SHOW COLUMNS FROM `users` LIKE 'avatarUrl'");
    if (empty($col)) {
        try { $db->query("ALTER TABLE `users` ADD COLUMN `avatarUrl` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(外部)' AFTER `avatar`"); } catch (Exception $e) {}
    }
    // 2. captcha_codes 验证码表
    $db->query("CREATE TABLE IF NOT EXISTS `captcha_codes` (
        `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '验证码ID',
        `code` VARCHAR(10) NOT NULL COMMENT '验证码内容',
        `used` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用',
        `image_data` MEDIUMTEXT COMMENT '图片base64',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        INDEX `idx_created` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表'");
    // 3. app_settings APP设置表
    $db->query("CREATE TABLE IF NOT EXISTS `app_settings` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `setting_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
        `setting_value` TEXT COMMENT '设置值',
        `type` VARCHAR(20) DEFAULT 'text' COMMENT '值类型:text/bool/number/image/json',
        `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        INDEX `idx_key` (`setting_key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='APP设置表'");
    // 写入默认 APP 设置（INSERT IGNORE）
    $defaultAppSettings = [
        ['app_name',                '扫码机器人',                                                    'text',   '应用名称'],
        ['app_description',         '让手机变成扫码枪，高效便捷。',                                   'text',   '应用描述'],
        ['app_registration_enabled','1',                                                             'bool',   'APP注册是否开启'],
        ['maintenance_mode',        '0',                                                             'bool',   '维护模式开关'],
        ['app_verification_enabled','0',                                                             'bool',   'APP注册是否需要验证码(旧键,兼容用)'],
        ['code_login_enabled',      '1',                                                             'bool',   'APP登录是否需要验证码'],
        ['code_register_enabled',   '1',                                                             'bool',   'APP注册是否需要验证码'],
        ['announcement',            '欢迎使用扫码机器人 让手机变成扫码枪，高效便捷。',                   'text',   '公告文本（顶部跑马灯）'],
        ['splash_screen_url',       '',                                                              'image',  '启动页全屏广告图URL'],
        ['splash_duration',         '2',                                                             'number', '启动页时长（秒）'],
        ['splash_app_name',         '二维码管理系统',                                                 'text',   '启动页大标题'],
        ['splash_app_description',  '专业的二维码管理工具',                                           'text',   '启动页副标题'],
        ['splash_bg_color',         '#1677ff',                                                       'text',   '启动页背景色'],
        ['splash_icon_url',         '',                                                              'image',  '启动页中间圆形图标URL'],
        ['home_app_name',           '扫码机器人',                                                    'text',   '首页顶部大标题'],
        ['home_app_description',    '让手机变成扫码枪',                                               'text',   '首页顶部副标题'],
        ['home_icon_url',           '',                                                              'image',  '首页顶部圆形图标URL']
    ];
    foreach ($defaultAppSettings as $row) {
        $exists = $db->fetchOne("SELECT id FROM app_settings WHERE setting_key = ?", [$row[0]]);
        if (!$exists) {
            $db->insert('app_settings', [
                'setting_key'   => $row[0],
                'setting_value' => $row[1],
                'type'          => $row[2],
                'description'   => $row[3]
            ]);
        }
    }
    // 4. app_versions APP版本表
    $db->query("CREATE TABLE IF NOT EXISTS `app_versions` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `version_code` INT UNSIGNED NOT NULL COMMENT '构建版本号',
        `version_name` VARCHAR(20) NOT NULL COMMENT '显示版本名',
        `platform` VARCHAR(20) NOT NULL DEFAULT 'android' COMMENT '平台:android/ios/all',
        `download_url` VARCHAR(500) DEFAULT NULL COMMENT '下载地址',
        `update_content` TEXT COMMENT '更新说明',
        `force_update` TINYINT(1) DEFAULT 0 COMMENT '是否强制更新',
        `file_size` BIGINT UNSIGNED DEFAULT 0 COMMENT '文件大小(字节)',
        `is_release` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
        UNIQUE KEY `uniq_version_platform` (`version_code`, `platform`),
        INDEX `idx_release` (`is_release`),
        INDEX `idx_platform` (`platform`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='APP版本更新表'");
    // 5. app_messages APP消息表
    $db->query("CREATE TABLE IF NOT EXISTS `app_messages` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
        `content` TEXT COMMENT '消息内容',
        `target_type` VARCHAR(20) DEFAULT 'all' COMMENT '目标:all/single/topic',
        `target_value` VARCHAR(100) DEFAULT NULL COMMENT '目标值:用户ID/话题',
        `message_type` VARCHAR(20) DEFAULT 'system' COMMENT '类型:system/announcement/activity',
        `send_type` VARCHAR(20) DEFAULT 'both' COMMENT '发送方式:push/inapp/both',
        `status` VARCHAR(20) DEFAULT 'draft' COMMENT '状态:draft/sending/sent/failed',
        `scheduled_at` DATETIME DEFAULT NULL COMMENT '定时发送时间',
        `sent_at` DATETIME DEFAULT NULL COMMENT '实际发送时间',
        `send_count` INT UNSIGNED DEFAULT 0 COMMENT '发送数',
        `read_count` INT UNSIGNED DEFAULT 0 COMMENT '已读数',
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        INDEX `idx_status` (`status`),
        INDEX `idx_message_type` (`message_type`),
        INDEX `idx_created` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='APP消息推送表'");
    // 6. access_logs 访问日志表（统计页面需要）
    $db->query("CREATE TABLE IF NOT EXISTS `access_logs` (
        `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
        `user_id` INT UNSIGNED DEFAULT NULL COMMENT '用户ID',
        `ip` VARCHAR(45) NOT NULL COMMENT 'IP地址',
        `ip_location` VARCHAR(200) DEFAULT NULL COMMENT 'IP归属地',
        `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
        `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
        `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
        `os_version` VARCHAR(50) DEFAULT NULL COMMENT '系统版本',
        `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
        `browser_version` VARCHAR(50) DEFAULT NULL COMMENT '浏览器版本',
        `device_type` VARCHAR(50) DEFAULT NULL COMMENT '设备类型',
        `device_brand` VARCHAR(50) DEFAULT NULL COMMENT '设备品牌',
        `device_model` VARCHAR(100) DEFAULT NULL COMMENT '设备型号',
        `referer` VARCHAR(500) DEFAULT NULL COMMENT '来源页面',
        `referer_domain` VARCHAR(100) DEFAULT NULL COMMENT '来源域名',
        `source_type` VARCHAR(50) DEFAULT 'direct' COMMENT '来源类型:direct/search/social/referer/qrcode',
        `page_url` VARCHAR(500) NOT NULL COMMENT '访问页面',
        `page_title` VARCHAR(200) DEFAULT NULL COMMENT '页面标题',
        `qrcode_type` VARCHAR(20) DEFAULT NULL COMMENT '活码类型',
        `qrcode_short_code` VARCHAR(20) DEFAULT NULL COMMENT '活码短码',
        `stay_duration` INT UNSIGNED DEFAULT 0 COMMENT '停留时长(秒)',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
        INDEX `idx_qrcode` (`qrcode_type`, `qrcode_short_code`),
        INDEX `idx_ip` (`ip`),
        INDEX `idx_session` (`session_id`),
        INDEX `idx_created` (`created_at`),
        INDEX `idx_os` (`os`),
        INDEX `idx_browser` (`browser`),
        INDEX `idx_device` (`device_brand`),
        INDEX `idx_province` (`province`),
        INDEX `idx_source` (`source_type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表'");
    // 7. dynamic_qrcodes / image_qrcodes / image_qrcode_items / text_qrcodes 活码表（前台活码页面需要）
    $db->query("CREATE TABLE IF NOT EXISTS `dynamic_qrcodes` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `short_code` VARCHAR(16) NOT NULL UNIQUE,
        `title` VARCHAR(200) DEFAULT '',
        `target_url` TEXT NOT NULL,
        `qr_image` VARCHAR(255) DEFAULT '',
        `scan_count` INT UNSIGNED DEFAULT 0,
        `status` TINYINT(1) DEFAULT 1,
        `last_scan` DATETIME DEFAULT NULL,
        `last_scan_ip` VARCHAR(45) DEFAULT NULL,
        `user_id` INT UNSIGNED DEFAULT NULL,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='链接活码表'");
    $db->query("CREATE TABLE IF NOT EXISTS `image_qrcodes` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `short_code` VARCHAR(16) NOT NULL UNIQUE,
        `title` VARCHAR(200) DEFAULT '',
        `qr_image` VARCHAR(255) DEFAULT '',
        `scan_count` INT UNSIGNED DEFAULT 0,
        `status` TINYINT(1) DEFAULT 1,
        `last_scan` DATETIME DEFAULT NULL,
        `last_scan_ip` VARCHAR(45) DEFAULT NULL,
        `user_id` INT UNSIGNED DEFAULT NULL,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片活码表'");
    $db->query("CREATE TABLE IF NOT EXISTS `image_qrcode_items` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `qrcode_id` INT UNSIGNED NOT NULL,
        `image_path` VARCHAR(255) NOT NULL,
        `image_url` VARCHAR(255) NOT NULL,
        `sort_order` INT DEFAULT 0,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX `idx_qrcode_id` (`qrcode_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片活码子图表'");
    $db->query("CREATE TABLE IF NOT EXISTS `text_qrcodes` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `short_code` VARCHAR(16) NOT NULL UNIQUE,
        `title` VARCHAR(200) DEFAULT '',
        `content` TEXT NOT NULL,
        `qr_image` VARCHAR(255) DEFAULT '',
        `scan_count` INT UNSIGNED DEFAULT 0,
        `status` TINYINT(1) DEFAULT 1,
        `last_scan` DATETIME DEFAULT NULL,
        `last_scan_ip` VARCHAR(45) DEFAULT NULL,
        `user_id` INT UNSIGNED DEFAULT NULL,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文本活码表'");
    // 补活码表 user_id 字段
    foreach (['dynamic_qrcodes','image_qrcodes','text_qrcodes'] as $tbl) {
        $col = $db->fetchOne("SHOW COLUMNS FROM `{$tbl}` LIKE 'user_id'");
        if (empty($col)) {
            try { $db->query("ALTER TABLE `{$tbl}` ADD COLUMN `user_id` INT UNSIGNED DEFAULT NULL AFTER `last_scan_ip`"); } catch (Exception $e) {}
        }
    }
} catch (Exception $e) {
    // 自动建表失败不影响API主流程，静默即可
    error_log('[api/config] auto_init_tables failed: ' . $e->getMessage());
}
