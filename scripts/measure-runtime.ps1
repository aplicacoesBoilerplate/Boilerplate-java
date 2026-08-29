param(
    [string]$BaseUrl = 'http://localhost:8080',
    [Parameter(Mandatory = $true)][string]$AdminToken,
    [ValidateRange(10, 1000)][int]$Iterations = 100
)

$ErrorActionPreference = 'Stop'
$headers = @{ Authorization = "Bearer $AdminToken" }
$durations = [System.Collections.Generic.List[double]]::new()

1..5 | ForEach-Object { Invoke-RestMethod -Uri "$BaseUrl/api/v1/actuator/health-check" -Headers $headers -TimeoutSec 10 | Out-Null }
1..$Iterations | ForEach-Object {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/me" -Headers $headers -TimeoutSec 10 | Out-Null
    $watch.Stop()
    $durations.Add($watch.Elapsed.TotalMilliseconds)
}

$metricNames = @('jvm.memory.used', 'jvm.gc.pause', 'jvm.threads.live', 'hikaricp.connections.pending', 'http.server.requests')
$metrics = @{}
foreach ($name in $metricNames) {
    $metrics[$name] = Invoke-RestMethod -Uri "$BaseUrl/api/v1/actuator/metrics/$name" -Headers $headers -TimeoutSec 10
}

$ordered = $durations | Sort-Object
$p95Index = [Math]::Min($ordered.Count - 1, [Math]::Ceiling($ordered.Count * 0.95) - 1)
$pending = $metrics['hikaricp.connections.pending'].measurements |
    Where-Object statistic -eq 'VALUE' |
    Select-Object -ExpandProperty value -First 1
if ($null -ne $pending -and $pending -gt 0) {
    throw "Há conexões Hikari pendentes após a amostra: $pending"
}

[pscustomobject]@{
    Requests = $Iterations
    P95Milliseconds = [Math]::Round($ordered[$p95Index], 2)
    HikariPending = $pending
    LiveThreads = ($metrics['jvm.threads.live'].measurements | Where-Object statistic -eq 'VALUE' | Select-Object -ExpandProperty value -First 1)
    MemoryMeasurements = $metrics['jvm.memory.used'].measurements.Count
}
