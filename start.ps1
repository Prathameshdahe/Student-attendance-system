# KIWI Smart Attendance - Admin Portal Launcher
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "    KIWI Smart Attendance Dashboard       " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting local web server for Admin Portal..." -ForegroundColor Yellow
Write-Host "(This fixes browser security restrictions on file:// URLs)" -ForegroundColor DarkGray
Write-Host "Configured Railway backend: https://fabulous-gratitude-production-9d95.up.railway.app" -ForegroundColor Cyan
Write-Host ""

# Kill any existing Python server on port 8080
$existing = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($existing) {
    $pid = $existing.OwningProcess | Select-Object -First 1
    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# Start a local HTTP server in the admin-portal directory in the background
$serverCmd = "-NoExit -Command `"cd '$PSScriptRoot\admin-portal'; python -m http.server 8080`""
Start-Process powershell -ArgumentList $serverCmd -WindowStyle Minimized

Write-Host "Server starting on http://localhost:8080 ..." -ForegroundColor White
Start-Sleep -Seconds 2

# Open the portal in the default browser via HTTP (not file://)
Start-Process "http://localhost:8080"

Write-Host ""
Write-Host "Admin portal is live at: http://localhost:8080" -ForegroundColor Green
Write-Host ""
Write-Host "Login credentials:" -ForegroundColor White
Write-Host "  Email:    admin@bvucoep.edu.in" -ForegroundColor Gray
Write-Host "  Password: Admin@1234" -ForegroundColor Gray
Write-Host ""
Write-Host "To stop the server, close the minimized PowerShell window." -ForegroundColor DarkGray
