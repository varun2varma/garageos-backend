<#
GarageST Master Release Test — root orchestrator.

Runs Suite A (backend, Python, e2e/) and/or Suite B (Android, Flutter
integration_test/) against the already-running backend and however many
Android devices are currently connected via ADB — discovered dynamically
every run (see Get-ConnectedDevices), never a fixed count or a hardcoded
serial. One device runs the full journey by cycling personas on it via
real logout/login; several devices let DevicePool spread personas across
them. Does not start the backend itself (see e2e/run.py's own readiness
check) — this matches "verify readiness, don't silently paper over an
environment that isn't up."

Usage:
    .\master-release-test.ps1 -Full
    .\master-release-test.ps1 -Backend
    .\master-release-test.ps1 -Android
    .\master-release-test.ps1 -Backend -Android
#>
param(
    [switch]$Full,
    [switch]$Backend,
    [switch]$Android,
    [switch]$Media,       # alias: runs Suite A phases 1-5 (through media/Drive) only
    [switch]$Navigation,  # alias: runs Suite A phase 8 (driver/STOMP) only
    [switch]$Security,    # alias: runs only test_99_security_regressions.py
    [string]$DevHost = "192.168.1.24:8080",
    [string]$BackendBaseUrl = "http://localhost:8080/api/v1"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$flutterRepo = Join-Path (Split-Path $root -Parent) "garagest_flutter"
$exitCode = 0

function Write-Section($title) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

function Test-BackendUp {
    try {
        Invoke-WebRequest -Uri "$BackendBaseUrl/master/employee-roles" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue | Out-Null
        return $true
    } catch {
        # Any HTTP response (even 401/403) means the server answered.
        if ($_.Exception.Response) { return $true }
        return $false
    }
}

function Get-ConnectedDevices {
    <#
    Real, dynamic device discovery — every serial connected in ADB state
    "device" (not offline/unauthorized), never a hardcoded count or a
    specific expected serial. This is what feeds DevicePool: 1 device
    connected runs the single-device fallback for real; N connected would
    spread personas across up to N of them; extra devices beyond what the
    journey's 4 personas need are simply never touched.
    #>
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adb) {
        $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        if (-not (Test-Path $adbPath)) { return @() }
    } else {
        $adbPath = $adb.Source
    }
    $lines = & $adbPath devices 2>&1
    $serials = @()
    foreach ($line in $lines) {
        if ($line -match '^(\S+)\s+device(\s|$)') {
            $serials += $matches[1]
        }
    }
    # The unary comma forces this to stay an array even with exactly one
    # (or zero) elements — without it, PowerShell silently unwraps a
    # single-element array to a bare scalar string on return, and the
    # caller's $devices[0] would then index into that STRING's characters
    # instead of the device list (confirmed: this was a real bug here,
    # not a hypothetical one — Detected devices: 1 / [1] 1).
    return ,$serials
}

function Invoke-SuiteA([string]$PhaseArg = "9") {
    Write-Section "Suite A — Backend (Python, real Postgres, real Google Drive)"
    if (-not (Test-BackendUp)) {
        Write-Host "ENVIRONMENT FAILURE: backend not reachable at $BackendBaseUrl. Start it first (see garagest-e2e.env)." -ForegroundColor Red
        $script:exitCode = 2
        return
    }
    Push-Location (Join-Path $root "e2e")
    try {
        python run.py --base-url $BackendBaseUrl --phase $PhaseArg
        if ($LASTEXITCODE -ne 0) { $script:exitCode = $LASTEXITCODE }
    } finally {
        Pop-Location
    }
}

function Invoke-SuiteASecurity {
    Write-Section "Suite A — Security regressions only"
    if (-not (Test-BackendUp)) {
        Write-Host "ENVIRONMENT FAILURE: backend not reachable at $BackendBaseUrl." -ForegroundColor Red
        $script:exitCode = 2
        return
    }
    Push-Location (Join-Path $root "e2e")
    try {
        $env:MRT_BASE_URL = $BackendBaseUrl
        python -m pytest tests/test_99_security_regressions.py -v
        if ($LASTEXITCODE -ne 0) { $script:exitCode = $LASTEXITCODE }
    } finally {
        Pop-Location
    }
}

function Invoke-SuiteB {
    <#
    Dynamic multi-device Suite B: discovers every connected Android device
    (never a fixed count or a hardcoded serial), then delegates persona-to-
    device allocation and execution to run_suite_b_devices.py, which builds
    ONE canonical DevicePool.plan_allocation() over the journey's 4 personas
    and runs `flutter test -d <serial>` once per device the plan actually
    uses — 1 process (all 4 personas cycling via logout/login) when only 1
    device is connected, up to 4 concurrent processes when 4+ are connected.
    This script's only jobs are: discover devices, confirm the Flutter repo
    is there, find the most recent Suite A run context (so Suite B operates
    on the SAME real accounts/Job Card Suite A just created), and report
    the result — installing the APK, verifying it, and launching the app
    happens naturally as part of each `flutter test -d <serial>` call,
    exactly as it already does for a single device.
    #>
    Write-Section "Suite B — Android (dynamic multi-device DevicePool orchestration)"

    $devices = Get-ConnectedDevices
    Write-Host "Detected devices: $($devices.Count)"
    for ($i = 0; $i -lt $devices.Count; $i++) { Write-Host "  [$($i + 1)] $($devices[$i])" }

    if ($devices.Count -eq 0) {
        Write-Host "ENVIRONMENT FAILURE: no Android device found in 'adb devices' state 'device'. Connect at least one (wireless or USB ADB)." -ForegroundColor Red
        $script:exitCode = 2
        return
    }
    if (-not (Test-Path $flutterRepo)) {
        Write-Host "ENVIRONMENT FAILURE: expected garagest_flutter checkout at $flutterRepo, not found." -ForegroundColor Red
        $script:exitCode = 2
        return
    }

    $runContext = Get-ChildItem (Join-Path $root "e2e\reports") -Filter "*.run_context.json" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $runContext) {
        Write-Host "No Suite A run context found under e2e/reports/*.run_context.json — run Suite A first (-Backend) so Suite B has real accounts and a real Job Card to use; it never registers its own." -ForegroundColor Yellow
        $script:exitCode = 2
        return
    }
    Write-Host "Using run context: $($runContext.Name)"

    Push-Location (Join-Path $root "e2e")
    try {
        python run_suite_b_devices.py --run-context $runContext.FullName --flutter-repo $flutterRepo --dev-host $DevHost --base-url $BackendBaseUrl
        if ($LASTEXITCODE -ne 0) { $script:exitCode = $LASTEXITCODE }
    } finally {
        Pop-Location
    }
}

if ($Full) { $Backend = $true; $Android = $true }
if (-not ($Backend -or $Android -or $Media -or $Navigation -or $Security)) {
    Write-Host "Usage: .\master-release-test.ps1 [-Full] [-Backend] [-Android] [-Media] [-Navigation] [-Security]"
    Write-Host "  -Full        Run Suite A (all 9 phases) + Suite B"
    Write-Host "  -Backend     Suite A only, all 9 phases"
    Write-Host "  -Android     Suite B only"
    Write-Host "  -Media       Suite A, phases 1-5 (through repair-task media / Google Drive)"
    Write-Host "  -Navigation  Suite A, phases 1-8 (through the STOMP/dummy-GPS phase)"
    Write-Host "  -Security    Suite A, test_99_security_regressions.py only"
    exit 1
}

if ($Media) { Invoke-SuiteA -PhaseArg "5" }
if ($Navigation) { Invoke-SuiteA -PhaseArg "8" }
if ($Security) { Invoke-SuiteASecurity }
if ($Backend) { Invoke-SuiteA -PhaseArg "9" }
if ($Android) { Invoke-SuiteB }

Write-Section "Master Release Test — done"
Write-Host "See garageos-backend/e2e/reports/ for Suite A's JSON/HTML/Markdown reports."
Write-Host "See MASTER_E2E_COVERAGE.md for the living coverage matrix and known issues."
exit $exitCode
