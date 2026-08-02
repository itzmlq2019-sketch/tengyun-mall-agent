param(
    [string]$ComposeFile = "compose.infrastructure.yml"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$composePath = Join-Path $root $ComposeFile

docker compose --env-file (Join-Path $root ".env") -f $composePath down
