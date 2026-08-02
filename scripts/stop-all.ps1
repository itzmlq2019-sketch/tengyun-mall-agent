$ports = 8080, 8081, 8082, 8083, 8084, 8085

foreach ($port in $ports) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($null -ne $conn) {
        try {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction Stop
            Write-Host "Stopped process $($conn.OwningProcess) on port $port"
        } catch {
            Write-Host "Failed to stop process $($conn.OwningProcess) on port $($port): $($_.Exception.Message)"
        }
    } else {
        Write-Host "No process listening on port $port"
    }
}
