<?php
require_once __DIR__ . '/config.php';

$captchaId = bin2hex(random_bytes(16));
$chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
$code = '';
for ($i = 0; $i < 4; $i++) {
    $code .= $chars[random_int(0, strlen($chars) - 1)];
}

$db = getDb();
$stmt = $db->prepare("INSERT INTO captcha_codes (id, code) VALUES (?, ?)");
$stmt->bind_param('ss', $captchaId, $code);
$stmt->execute();
$stmt->close();

$db->query("DELETE FROM captcha_codes WHERE created_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)");

$width = 120;
$height = 40;
$image = imagecreatetruecolor($width, $height);
$bgColor = imagecolorallocate($image, 245, 246, 248);
imagefill($image, 0, 0, $bgColor);

$colors = [
    imagecolorallocate($image, 26, 115, 232),
    imagecolorallocate($image, 0, 184, 164),
    imagecolorallocate($image, 255, 77, 79),
    imagecolorallocate($image, 212, 136, 6)
];

for ($i = 0; $i < 80; $i++) {
    $noiseColor = imagecolorallocate($image, random_int(200, 240), random_int(200, 240), random_int(200, 240));
    imagesetpixel($image, random_int(0, $width), random_int(0, $height), $noiseColor);
}

for ($i = 0; $i < 4; $i++) {
    $lineColor = imagecolorallocate($image, random_int(180, 220), random_int(180, 220), random_int(180, 220));
    imageline($image, random_int(0, $width), random_int(0, $height), random_int(0, $width), random_int(0, $height), $lineColor);
}

for ($i = 0; $i < 4; $i++) {
    $angle = random_int(-15, 15);
    $x = 15 + $i * 26;
    $y = 28;
    $textColor = $colors[$i % count($colors)];
    imagestring($image, 5, $x, $y - 10, $code[$i], $textColor);
}

ob_start();
imagepng($image);
$imageData = ob_get_clean();
imagedestroy($image);

$base64 = base64_encode($imageData);

jsonResponse(true, 'ok', [
    'captcha_id' => $captchaId,
    'captcha_image' => 'data:image/png;base64,' . $base64
]);
