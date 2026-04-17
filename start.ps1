Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "    KIWI Smart Attendance Dashboard       " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Cloud backend target: Railway production" -ForegroundColor Magenta
Write-Host "Admin portal client type: web-portal" -ForegroundColor Yellow
Write-Host ""
Write-Host "If admin login is restricted, use the Android admin app or allowlist this network in Railway." -ForegroundColor White
Write-Host ""
Write-Host "Opening the local admin portal..." -ForegroundColor Green

$htmlPath = Join-Path (Get-Location) "admin-portal\index.html"
Start-Process $htmlPath

Write-Host ""
Write-Host "Testing reminders:" -ForegroundColor White
Write-Host "1. Student phone needs fine + background location permissions." -ForegroundColor Gray
Write-Host "2. Admin phone shows the Trusted Admin Device Key inside the app." -ForegroundColor Gray
Write-Host "3. Geofence alerts appear in the admin app and portal after refresh." -ForegroundColor Gray
Write-Host ""
Write-Host "Closing window in 10 seconds..." -ForegroundColor DarkGray

Start-Sleep -Seconds 10
