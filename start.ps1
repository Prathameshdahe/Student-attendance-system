# KIWI Smart Attendance - Startup Script
Write-Host "Starting KIWI Backend Server..." -ForegroundColor Green

# Start the FastAPI server in a new PowerShell window
$backendCmd = "-NoExit -Command `"cd backend; .\venv\Scripts\activate; uvicorn app.main:app --host 0.0.0.0 --port 8000`""
Start-Process powershell -ArgumentList $backendCmd

Write-Host "Opening KIWI Admin Portal..." -ForegroundColor Green
Start-Sleep -Seconds 3

# Open the HTML file in the default web browser (Chrome/Edge)
$htmlPath = Join-Path (Get-Location) "admin-portal\index.html"
Start-Process $htmlPath

Write-Host "Done! The server is running in the background." -ForegroundColor Cyan
