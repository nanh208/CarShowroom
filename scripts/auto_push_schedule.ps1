# auto_push_schedule.ps1 - Automated 20-day push engine for CarShowroom (branch: minh)
param (
    [int]$Day = 0,
    [switch]$RunNow,
    [switch]$Status,
    [switch]$OpenPR
)

$repoDir = "c:\Users\koreka\CarShowroom"
$scriptsDir = Join-Path $repoDir "scripts"
$planFile = Join-Path $scriptsDir "schedule_plan.json"
$stateFile = Join-Path $scriptsDir "schedule_state.json"
$logFile = Join-Path $scriptsDir "push_history.log"
$payloadsDir = Join-Path $scriptsDir "payloads"

function Log-Message([string]$msg) {
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $line = "[$timestamp] $msg"
    Write-Host $line
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

if ($OpenPR) {
    $prUrl = "https://github.com/nanh208/CarShowroom/compare/master...minh?expand=1"
    Log-Message "Mo trinh duyet kiem tra Pull Request tren GitHub: $prUrl"
    Start-Process $prUrl
    return
}

if (!(Test-Path $planFile) -or !(Test-Path $stateFile)) {
    Write-Error "Khong tim thay tap tin schedule_plan.json hoac schedule_state.json"
    return
}

$plan = Get-Content $planFile -Raw -Encoding UTF8 | ConvertFrom-Json
$state = Get-Content $stateFile -Raw -Encoding UTF8 | ConvertFrom-Json

if ($Status) {
    Write-Host "================ TIEN DO DAY 20 NGAY (NHANH MINH) ================" -ForegroundColor Cyan
    Write-Host "Tong so ngay: $($state.totalDays)"
    Write-Host "Ngay hien tai da day: Ngay $($state.currentDay) / $($state.totalDays)"
    Write-Host "Nhanh Git: $($state.branch) -> $($state.remote)/$($state.branch)"
    Write-Host "Lan day gan nhat: $($state.lastRunTimestamp)"
    Write-Host "------------------------------------------------------------------"
    foreach ($p in $plan) {
        $st = if ($p.day -le $state.currentDay) { "[DA DAY]" } else { "[CHO LICH]" }
        $color = if ($p.day -le $state.currentDay) { "Green" } else { "DarkGray" }
        Write-Host ("{0} Ngay {1:D2} [{2}]: {3}" -f $st, $p.day, $p.module, $p.commitMessage) -ForegroundColor $color
    }
    Write-Host "==================================================================" -ForegroundColor Cyan
    return
}

# Determine target day to push
$targetDay = $Day
if ($targetDay -le 0) {
    $targetDay = [int]$state.currentDay + 1
}

if ($targetDay -gt [int]$state.totalDays) {
    Log-Message "Da hoan thanh toan bo 20 ngay day ma nguon len GitHub!"
    Write-Host "Lo trinh 20 ngay da hoan tat. Ban co the mo Pull Request vao master." -ForegroundColor Green
    return
}

$dayInfo = $null
foreach ($item in $plan) {
    if ($item.day -eq $targetDay) {
        $dayInfo = $item
        break
    }
}

if (-not $dayInfo) {
    Log-Message "Khong tim thay cau hinh cho Ngay $targetDay trong ke hoach."
    return
}

$dayTag = ("day{0:D2}" -f $targetDay)
$sourcePayload = Join-Path $payloadsDir $dayTag

if (!(Test-Path $sourcePayload)) {
    Log-Message "Loi: Khong tim thay thu muc payload $sourcePayload"
    return
}

Log-Message ">>> BAT DAU DAY NGAY $targetDay / $($state.totalDays): [$($dayInfo.module)]"
Log-Message "Thong diep commit: $($dayInfo.commitMessage)"

# Copy files from payload to repo root
Copy-Item -Path "$sourcePayload\*" -Destination $repoDir -Recurse -Force
Log-Message "Da sao chep cac tep tin cua $dayTag vao ma nguon du an."

Set-Location -Path $repoDir

# Git stage, commit & push
& git add -A
$statusOutput = & git status --porcelain
if ([string]::IsNullOrWhiteSpace($statusOutput)) {
    Log-Message "Khong co thay doi moi can commit cho Ngay $targetDay."
} else {
    $commitMsg = $dayInfo.commitMessage
    & git commit -m "$commitMsg"
    Log-Message "Da tao commit thanh cong: $commitMsg"
}

Log-Message "Dang day (git push) len nhanh minh tren GitHub..."
$pushOutput = & git push origin minh 2>&1
Log-Message "Ket qua push: $pushOutput"

# Update state
$state.currentDay = $targetDay
$state.lastRunTimestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
$historyItem = [PSCustomObject]@{
    day = $targetDay
    module = $dayInfo.module
    commit = $dayInfo.commitMessage
    timestamp = $state.lastRunTimestamp
}
$state.history += $historyItem

$state | ConvertTo-Json -Depth 5 | Set-Content $stateFile -Encoding UTF8
Log-Message ">>> HOAN THANH DAY NGAY $targetDay LEN GITHUB."

if ($targetDay -eq 1 -or $targetDay -eq [int]$state.totalDays) {
    Log-Message "Meo: Mo giao dien Pull Request bang lenh: .\auto_push_schedule.ps1 -OpenPR"
}
