<?php
/**
 * APP 图形验证码接口
 * GET https://qr.wzdi.cn/api/captcha.php
 * 返回: { success, message, data:{captcha_id, captcha_image} }
 *   captcha_image = base64 图片 data:image/png;base64,....
 *   验证码文本以 captcha_id 为 key 存入 captcha_codes 表，10 分钟有效
 */
require_once __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

if (!function_exists('imagecreate')) {
    // GD 不可用时 fallback: 返回简单随机码文字占位
    $code = strtoupper(bin2hex(random_bytes(2)));
    $captchaId = bin2hex(random_bytes(8));
    try {
        $db->query("INSERT INTO captcha_codes (id, code, created_at) VALUES (?, ?, NOW())", [$captchaId, $code]);
    } catch (Exception $e) {
        // 表结构不一致时兜底：仅写入核心列
        try { $db->query("INSERT INTO captcha_codes (id, code) VALUES (?, ?)", [$captchaId, $code]); } catch (Exception $_) {}
    }
    $img = '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="40" viewBox="0 0 120 40">'
         . '<rect width="120" height="40" fill="#eef2ff"/>'
         . '<text x="60" y="28" text-anchor="middle" font-family="sans-serif" font-size="22" fill="#4338ca" font-weight="bold">' . htmlspecialchars($code) . '</text>'
         . '</svg>';
    $b64 = base64_encode($img);
    echo json_encode([
        'success' => true,
        'message' => 'ok',
        'data' => [
            'captcha_id'    => $captchaId,
            'captcha_image' => 'data:image/svg+xml;base64,' . $b64,
        ],
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. 生成随机码
$len = 4;
$chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
$code = '';
for ($i = 0; $i < $len; $i++) {
    $code .= $chars[random_int(0, strlen($chars) - 1)];
}

// 2. 存入 captcha_codes（列名兼容：优先全量列，失败则只写核心列）
$captchaId = bin2hex(random_bytes(16));
try {
    $db->query(
        "INSERT INTO captcha_codes (id, code, created_at) VALUES (?, ?, NOW())",
        [$captchaId, $code]
    );
} catch (Exception $e) {
    // 兼容不同表结构
    try { $db->query("INSERT INTO captcha_codes (id, code) VALUES (?, ?)", [$captchaId, $code]); } catch (Exception $_) {}
}

// 3. 绘制图形验证码
$w = 130; $h = 44;
$img = imagecreate($w, $h);
$bg  = imagecolorallocate($img, 238, 242, 255);
$textColor = imagecolorallocate($img, 67, 56, 202);
$noise     = imagecolorallocate($img, 165, 180, 252);
imagefill($img, 0, 0, $bg);

// 干扰线
for ($i = 0; $i < 5; $i++) {
    imageline($img, random_int(0, $w), random_int(0, $h), random_int(0, $w), random_int(0, $h), $noise);
}
// 干扰点
for ($i = 0; $i < 60; $i++) {
    imagesetpixel($img, random_int(0, $w), random_int(0, $h), $noise);
}

// 写入文字（加少量扰动）
$font = __DIR__ . '/../assets/fonts/captcha.ttf';
if (!is_file($font)) $font = 5;
for ($i = 0; $i < $len; $i++) {
    $x = 14 + $i * 28;
    $y = random_int(28, 36);
    $angle = random_int(-12, 12);
    if (is_string($font)) {
        @imagettftext($img, 20, $angle, $x, $y, $textColor, $font, $code[$i]);
    } else {
        imagestring($img, 5, $x, $y - 10, $code[$i], $textColor);
    }
}

ob_start();
imagepng($img);
$pngBin = ob_get_clean();
imagedestroy($img);

echo json_encode([
    'success' => true,
    'message' => 'ok',
    'data' => [
        'captcha_id'    => $captchaId,
        'captcha_image' => 'data:image/png;base64,' . base64_encode($pngBin),
    ],
], JSON_UNESCAPED_UNICODE);
