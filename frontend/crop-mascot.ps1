Add-Type -AssemblyName System.Drawing
$src = "C:/Users/wilso/.gemini/antigravity/brain/1a914506-51a2-487d-a430-5bed6ee2025f/.user_uploaded/media_1791998689886_edc1f340.jpg"
$img = [System.Drawing.Bitmap]::FromFile($src)

function Crop($x, $y, $w, $h, $outFile) {
    $rect = New-Object System.Drawing.Rectangle([int]$x, [int]$y, [int]$w, [int]$h)
    $cropped = $img.Clone($rect, $img.PixelFormat)
    $cropped.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    $cropped.Dispose()
    Write-Host "Wrote: $outFile"
}

# 1. Main pose (chef standing with bag) - perfectly framed
Crop 230 130 286 415 "D:\Projetos\Peditto\frontend\public\mascot-chef.png"

# 2. Cooking / Preparo - perfectly framed
Crop 600 286 172 245 "D:\Projetos\Peditto\frontend\public\mascot-cooking.png"

# 3. Delivering / Saiu para entrega - perfectly framed
Crop 785 286 195 245 "D:\Projetos\Peditto\frontend\public\mascot-delivery.png"

# 4. Avatar badge / logo - perfectly framed
Crop 45 366 170 170 "D:\Projetos\Peditto\frontend\public\mascot-badge.png"
Crop 45 366 170 170 "D:\Projetos\Peditto\frontend\public\peditto-mascot-avatar.png"

# 5. Favicon generation (square 64x64 and 32x32 based on the badge)
$badgeRect = New-Object System.Drawing.Rectangle(45, 366, 170, 170)
$badgeCropped = $img.Clone($badgeRect, $img.PixelFormat)
$fav64 = New-Object System.Drawing.Bitmap 64, 64
$g = [System.Drawing.Graphics]::FromImage($fav64)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($badgeCropped, 0, 0, 64, 64)
$g.Dispose()
$fav64.Save("D:\Projetos\Peditto\frontend\public\favicon.png", [System.Drawing.Imaging.ImageFormat]::Png)
$fav64.Dispose()
$badgeCropped.Dispose()
Write-Host "Wrote: favicon.png"

$img.Dispose()

# Create favicon.ico
$bmp = [System.Drawing.Bitmap]::FromFile('D:\Projetos\Peditto\frontend\public\favicon.png')
$hIcon = $bmp.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($hIcon)
$fs = New-Object System.IO.FileStream('D:\Projetos\Peditto\frontend\public\favicon.ico', [System.IO.FileMode]::Create)
$icon.Save($fs)
$fs.Close()
$icon.Dispose()
$bmp.Dispose()
Write-Host "Created real favicon.ico"
