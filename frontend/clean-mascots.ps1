Add-Type -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Collections.Generic;

public class MascotProcessor {
    public static void ProcessMascots(string sheetPath, string outDir) {
        using (Bitmap sheet = new Bitmap(sheetPath)) {
            // 1. Chef: x=230, y=125, w=290, h=420
            ProcessPose(sheet, 230, 125, 290, 420, System.IO.Path.Combine(outDir, "mascot-chef.png"), false, 1.5);
            
            // 2. Cooking: x=580, y=275, w=200, h=265
            ProcessPose(sheet, 580, 275, 200, 265, System.IO.Path.Combine(outDir, "mascot-cooking.png"), false, 1.5);
            
            // 3. Delivery: x=770, y=275, w=220, h=265
            ProcessPose(sheet, 770, 275, 220, 265, System.IO.Path.Combine(outDir, "mascot-delivery.png"), false, 1.5);
            
            // 4. Circular Badge / Avatar: x=38, y=360, w=182, h=182
            ProcessPose(sheet, 38, 360, 182, 182, System.IO.Path.Combine(outDir, "mascot-badge.png"), true, 2.0);
            ProcessPose(sheet, 38, 360, 182, 182, System.IO.Path.Combine(outDir, "peditto-mascot-avatar.png"), true, 2.0);
            
            // 5. Generate crisp 64x64 favicon from badge
            using (Bitmap badge = new Bitmap(System.IO.Path.Combine(outDir, "peditto-mascot-avatar.png"))) {
                Bitmap fav = new Bitmap(64, 64, PixelFormat.Format32bppArgb);
                using (Graphics g = Graphics.FromImage(fav)) {
                    g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                    g.SmoothingMode = SmoothingMode.HighQuality;
                    g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                    g.DrawImage(badge, 0, 0, 64, 64);
                }
                fav.Save(System.IO.Path.Combine(outDir, "favicon.png"), ImageFormat.Png);
                fav.Dispose();
            }
        }
    }
    
    private static void ProcessPose(Bitmap sheet, int rx, int ry, int rw, int rh, string destFile, bool isCircular, double scale) {
        Rectangle cropRect = new Rectangle(rx, ry, Math.Min(rw, sheet.Width - rx), Math.Min(rh, sheet.Height - ry));
        using (Bitmap cropped = sheet.Clone(cropRect, PixelFormat.Format24bppRgb)) {
            int w = cropped.Width;
            int h = cropped.Height;
            
            int sw = (int)(w * scale);
            int sh = (int)(h * scale);
            Bitmap scaled = new Bitmap(sw, sh, PixelFormat.Format32bppArgb);
            using (Graphics g = Graphics.FromImage(scaled)) {
                g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                g.SmoothingMode = SmoothingMode.HighQuality;
                g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                g.DrawImage(cropped, 0, 0, sw, sh);
            }
            
            BitmapData bData = scaled.LockBits(new Rectangle(0, 0, sw, sh), ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
            int[] pixels = new int[sw * sh];
            System.Runtime.InteropServices.Marshal.Copy(bData.Scan0, pixels, 0, sw * sh);
            
            // 1. Mark background paper candidates
            bool[] isBg = new bool[sw * sh];
            for (int i = 0; i < sw * sh; i++) {
                int c = pixels[i];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                
                if (r > 200 && g > 190 && b > 175 && Math.Abs(r - g) <= 28 && Math.Abs(g - b) <= 32 && (r - b) >= -5) {
                    isBg[i] = true;
                } else if (r > 235 && g > 228 && b > 218) {
                    isBg[i] = true;
                }
            }
            
            // 2. Flood fill from exterior borders
            bool[] isExterior = new bool[sw * sh];
            Queue<int> q = new Queue<int>();
            
            for (int x = 0; x < sw; x++) {
                if (isBg[x]) { isExterior[x] = true; q.Enqueue(x); }
                int bIdx = (sh - 1) * sw + x;
                if (isBg[bIdx]) { isExterior[bIdx] = true; q.Enqueue(bIdx); }
            }
            for (int y = 0; y < sh; y++) {
                int lIdx = y * sw;
                if (isBg[lIdx]) { isExterior[lIdx] = true; q.Enqueue(lIdx); }
                int rIdx = y * sw + (sw - 1);
                if (isBg[rIdx]) { isExterior[rIdx] = true; q.Enqueue(rIdx); }
            }
            
            int[] dx = { 1, -1, 0, 0 };
            int[] dy = { 0, 0, 1, -1 };
            
            while (q.Count > 0) {
                int curr = q.Dequeue();
                int cx = curr % sw;
                int cy = curr / sw;
                for (int d = 0; d < 4; d++) {
                    int nx = cx + dx[d];
                    int ny = cy + dy[d];
                    if (nx >= 0 && nx < sw && ny >= 0 && ny < sh) {
                        int ni = ny * sw + nx;
                        if (!isExterior[ni] && isBg[ni]) {
                            isExterior[ni] = true;
                            q.Enqueue(ni);
                        }
                    }
                }
            }
            
            // 3. Clear enclosed pockets of background (between legs, under arms, inside pan handles)
            if (!isCircular) {
                bool[] visited = new bool[sw * sh];
                for (int i = 0; i < sw * sh; i++) {
                    if (!isExterior[i] && isBg[i] && !visited[i]) {
                        List<int> comp = new List<int>();
                        Queue<int> cq = new Queue<int>();
                        cq.Enqueue(i);
                        visited[i] = true;
                        
                        while (cq.Count > 0) {
                            int curr = cq.Dequeue();
                            comp.Add(curr);
                            int cx = curr % sw;
                            int cy = curr / sw;
                            for (int d = 0; d < 4; d++) {
                                int nx = cx + dx[d];
                                int ny = cy + dy[d];
                                if (nx >= 0 && nx < sw && ny >= 0 && ny < sh) {
                                    int ni = ny * sw + nx;
                                    if (!visited[ni] && isBg[ni]) {
                                        visited[ni] = true;
                                        cq.Enqueue(ni);
                                    }
                                }
                            }
                        }
                        
                        if (comp.Count > 25) {
                            long sumR = 0, sumG = 0, sumB = 0;
                            foreach (int idx in comp) {
                                int col = pixels[idx];
                                sumR += (col >> 16) & 0xFF;
                                sumG += (col >> 8) & 0xFF;
                                sumB += col & 0xFF;
                            }
                            double avgR = sumR / (double)comp.Count;
                            double avgG = sumG / (double)comp.Count;
                            double avgB = sumB / (double)comp.Count;
                            
                            if (avgR > 208 && avgG > 198 && avgB > 182 && Math.Abs(avgR - avgG) < 25 && Math.Abs(avgG - avgB) < 28) {
                                foreach (int idx in comp) {
                                    isExterior[idx] = true;
                                }
                            }
                        }
                    }
                }
            }
            
            // 4. Circle mask for circular badge
            double centerX = sw / 2.0;
            double centerY = sh / 2.0;
            double radius = Math.Min(sw, sh) / 2.0 - 4.0;
            
            // 5. Finalize alpha, antialiasing and edge defringing
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    int idx = y * sw + x;
                    int c = pixels[idx];
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    
                    if (isCircular) {
                        double dist = Math.Sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
                        if (dist > radius + 1.5) {
                            pixels[idx] = 0;
                            continue;
                        } else if (dist > radius - 1.5) {
                            double frac = (radius + 1.5 - dist) / 3.0;
                            int fa = Math.Max(0, Math.Min(255, (int)(255 * frac)));
                            pixels[idx] = (fa << 24) | (r << 16) | (g << 8) | b;
                            continue;
                        }
                    }
                    
                    if (isExterior[idx]) {
                        pixels[idx] = 0; // 100% transparent
                    } else {
                        // Check if it's on the boundary next to transparent
                        bool nearExt = false;
                        for (int d = 0; d < 4; d++) {
                            int nx = x + dx[d];
                            int ny = y + dy[d];
                            if (nx >= 0 && nx < sw && ny >= 0 && ny < sh) {
                                if (isExterior[ny * sw + nx]) { nearExt = true; break; }
                            }
                        }
                        
                        if (nearExt) {
                            if (r > 205 && g > 195 && b > 180) {
                                pixels[idx] = 0; // Remove halo
                            } else {
                                double brightness = (r * 0.299 + g * 0.587 + b * 0.114);
                                if (brightness > 130) {
                                    int edgeAlpha = (int)(255 * (1.0 - (brightness - 130) / 95.0));
                                    edgeAlpha = Math.Max(0, Math.Min(255, edgeAlpha));
                                    pixels[idx] = (edgeAlpha << 24) | (r << 16) | (g << 8) | b;
                                } else {
                                    pixels[idx] = (255 << 24) | (r << 16) | (g << 8) | b;
                                }
                            }
                        } else {
                            pixels[idx] = (255 << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                }
            }
            
            System.Runtime.InteropServices.Marshal.Copy(pixels, 0, bData.Scan0, sw * sh);
            scaled.UnlockBits(bData);
            
            scaled.Save(destFile, ImageFormat.Png);
            scaled.Dispose();
        }
    }
}
"@ -ReferencedAssemblies "System.Drawing"

$sheet = "C:\Users\wilso\.gemini\antigravity\brain\1a914506-51a2-487d-a430-5bed6ee2025f\.user_uploaded\media_1791998689886_edc1f340.jpg"
$out = "D:\Projetos\Peditto\frontend\public"
[MascotProcessor]::ProcessMascots($sheet, $out)
Write-Host "Mascots processed successfully!"
