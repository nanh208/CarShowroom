# register_task.ps1 - Registers daily automated push into Windows Task Scheduler
param (
    [string]$Time = "09:00",
    [switch]$Remove,
    [switch]$Status
)

$taskName = "CarShowroom_AutoPush_Minh"
$scriptPath = "c:\Users\koreka\CarShowroom\scripts\auto_push_schedule.ps1"

if ($Status) {
    Write-Host "Kiem tra tac vu trong Windows Task Scheduler..."
    schtasks.exe /Query /TN $taskName /FO LIST /V
    return
}

if ($Remove) {
    Write-Host "Dang xoa tac vu $taskName..."
    schtasks.exe /Delete /TN $taskName /F
    Write-Host "Da xoa tac vu thanh cong."
    return
}

$action = "powershell.exe -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$scriptPath`" -RunNow"

Write-Host "Dang dang ky tac vu tu dong day hang ngay vao Windows Task Scheduler..."
Write-Host "Ten tac vu: $taskName"
Write-Host "Lich chay: Hang ngay luc $Time"

schtasks.exe /Create /SC DAILY /TN "$taskName" /TR "$action" /ST $Time /F

if ($LASTEXITCODE -eq 0) {
    Write-Host ">>> DANG KY LICH HEN TU DONG THANH CONG!"
    Write-Host "Moi ngay vao luc $Time, Windows se tu dong chay script day 1 commit len nhanh minh tren GitHub."
} else {
    Write-Host "schtasks ket thuc voi ma loi: $LASTEXITCODE"
}
