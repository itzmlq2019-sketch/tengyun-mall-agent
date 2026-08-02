param(
    [string]$ComposeFile = "compose.infrastructure.yml"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$composePath = Join-Path $root $ComposeFile

if (-not (Test-Path $composePath)) {
    throw "Compose file not found: $composePath"
}

docker compose --env-file (Join-Path $root ".env") -f $composePath up -d
docker compose --env-file (Join-Path $root ".env") -f $composePath ps
