param(
    [string]$MvnPath = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root ".run-logs"

if ([string]::IsNullOrWhiteSpace($MvnPath)) {
    $mavenCommand = Get-Command mvn.cmd, mvn -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $mavenCommand) {
        throw "Maven not found in PATH. Install Maven or pass -MvnPath with an explicit mvn.cmd path."
    }
    $MvnPath = $mavenCommand.Source
} elseif (-not (Test-Path -LiteralPath $MvnPath)) {
    throw "Maven not found: $MvnPath"
}
if (-not (Test-Path (Join-Path $root ".env"))) {
    throw "Missing .env in project root: $root"
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

$modules = @(
    "tengyun-user",
    "tengyun-product",
    "tengyun-cart",
    "tengyun-order",
    "tengyun-agent",
    "tengyun-gateway"
)

foreach ($module in $modules) {
    $outLog = Join-Path $logDir "$module.out.log"
    $errLog = Join-Path $logDir "$module.err.log"
    Start-Process -FilePath $MvnPath `
        -ArgumentList "-pl", $module, "spring-boot:run", "-Dspring-boot.run.fork=false" `
        -WorkingDirectory $root `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog | Out-Null
    Start-Sleep -Milliseconds 800
}

Write-Host "Started modules. Logs: $logDir"
