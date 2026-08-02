param(
    [string]$GatewayBase = "http://127.0.0.1:8080",
    [string]$UserBase = "http://127.0.0.1:8081",
    [string]$OrderBase = "http://127.0.0.1:8082",
    [string]$ProductBase = "http://127.0.0.1:8083",
    [string]$CartBase = "http://127.0.0.1:8084",
    [string]$AgentBase = "http://127.0.0.1:8085"
)

$ErrorActionPreference = "Stop"

function Test-Http {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [int]$ExpectedStatus,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )

    try {
        if ([string]::IsNullOrEmpty($Body)) {
            $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $Url -Headers $Headers -TimeoutSec 8
        } else {
            $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $Url -Headers $Headers -Body $Body -TimeoutSec 8
        }
        $actualStatus = [int]$response.StatusCode
        $actualBody = $response.Content
    } catch {
        if ($_.Exception.Response -eq $null) {
            throw "[$Name] request failed before HTTP response: $($_.Exception.Message)"
        }
        $actualStatus = [int]$_.Exception.Response.StatusCode.value__
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $actualBody = $reader.ReadToEnd()
    }

    if ($actualStatus -ne $ExpectedStatus) {
        throw "[$Name] expected $ExpectedStatus but got $actualStatus. body=$actualBody"
    }

    Write-Host "[PASS] $Name -> $actualStatus"
}

function Test-Port {
    param(
        [string]$Name,
        [int]$Port
    )
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $conn) {
        throw "[$Name] port $Port is not listening"
    }
    Write-Host "[PASS] $Name port $Port listening (pid=$($conn.OwningProcess))"
}

Write-Host "=== Port readiness ==="
Test-Port -Name "gateway" -Port 8080
Test-Port -Name "user" -Port 8081
Test-Port -Name "order" -Port 8082
Test-Port -Name "product" -Port 8083
Test-Port -Name "cart" -Port 8084
Test-Port -Name "agent" -Port 8085

Write-Host "=== OpenAPI readiness ==="
Test-Http -Name "gateway api-docs" -Method "GET" -Url "$GatewayBase/v3/api-docs" -ExpectedStatus 200
Test-Http -Name "user api-docs" -Method "GET" -Url "$UserBase/v3/api-docs" -ExpectedStatus 200
Test-Http -Name "order api-docs" -Method "GET" -Url "$OrderBase/v3/api-docs" -ExpectedStatus 200
Test-Http -Name "product api-docs" -Method "GET" -Url "$ProductBase/v3/api-docs" -ExpectedStatus 200
Test-Http -Name "cart api-docs" -Method "GET" -Url "$CartBase/v3/api-docs" -ExpectedStatus 200
Test-Http -Name "agent api-docs" -Method "GET" -Url "$AgentBase/v3/api-docs" -ExpectedStatus 200

Write-Host "=== Input validation contract ==="
Test-Http -Name "user login validation" -Method "POST" -Url "$UserBase/user/login?username=&password=" -ExpectedStatus 400
Test-Http -Name "product deduct validation" -Method "POST" -Url "$ProductBase/product/deduct?productId=0&num=1" -ExpectedStatus 400
Test-Http -Name "cart header validation" -Method "GET" -Url "$CartBase/cart/list" -ExpectedStatus 400
Test-Http -Name "order body validation" -Method "POST" -Url "$OrderBase/order/checkout" -ExpectedStatus 400 -Headers @{ "X-User-Id" = "1"; "Content-Type" = "application/json" } -Body '{"productId":"abc","quantity":1}'
Test-Http -Name "agent header validation" -Method "GET" -Url "$AgentBase/agent/chat/stream?message=hello" -ExpectedStatus 400

Write-Host "Smoke test passed."
