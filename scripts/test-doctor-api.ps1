# 医生端API自动化测试脚本
$BaseUrl = "http://localhost:8080/api"
$Pass = 0
$Fail = 0
$Issues = @()

function Test-Api($Method, $Url, $Body, $Token) {
    $headers = @{"Content-Type"="application/json"}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        if ($Body) {
            $json = $Body | ConvertTo-Json -Compress
            $res = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body $json
        } else {
            $res = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
        }
        return $res
    } catch {
        return @{code=500; message=$_.Exception.Message}
    }
}

Write-Host "========== Doctor API Test ==========" -ForegroundColor Cyan

# Login as doctor
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="doctor001";password="123456"}
$token = $null
if ($res.code -eq 200 -and $res.data.token) {
    Write-Host "[PASS] Doctor login" -ForegroundColor Green; $Pass++
    $token = $res.data.token
} else {
    Write-Host "[FAIL] Doctor login: $($res.message)" -ForegroundColor Red; $Fail++
    $Issues += "Doctor login failed"
}

if ($token) {
    # 1. Dashboard Statistics
    Write-Host "`n--- Dashboard ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/dashboard/statistics" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Dashboard stats" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Dashboard stats: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Dashboard stats failed" }

    # 2. Patients
    Write-Host "`n--- Patients ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/patients?pageNum=1&pageSize=10" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get patients" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get patients: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Get patients failed" }

    # 3. Patient Pool
    Write-Host "`n--- Patient Pool ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/patient-pool?pageNum=1&pageSize=10" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Patient pool" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Patient pool: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Patient pool failed" }

    # 4. Appointments
    Write-Host "`n--- Appointments ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/appointments?pageNum=1&pageSize=10" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get appointments" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get appointments: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Get appointments failed" }

    $res = Test-Api "GET" "$BaseUrl/doctor/appointments/recent" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Recent appointments" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Recent appointments: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Recent appointments failed" }

    # 5. Reports
    Write-Host "`n--- Reports ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/reports?pageNum=1&pageSize=10" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get reports" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get reports: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Get reports failed" }

    # 6. Consultations
    Write-Host "`n--- Consultations ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/consultations?pageNum=1&pageSize=10" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get consultations" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get consultations: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Get consultations failed" }

    # 7. Requests
    Write-Host "`n--- Requests ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/doctor/requests?pageNum=1&pageSize=10" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get requests" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get requests: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Get requests failed" }

    # 8. Profile (via user API)
    Write-Host "`n--- Profile ---" -ForegroundColor Yellow
    $res = Test-Api "GET" "$BaseUrl/user/profile" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get profile" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get profile: $($res.message)" -ForegroundColor Red; $Fail++; $Issues += "Get profile failed" }
}

Write-Host "`n========== Results ==========" -ForegroundColor Cyan
Write-Host "Pass: $Pass | Fail: $Fail"
if ($Issues.Count -gt 0) {
    Write-Host "`nIssues found:" -ForegroundColor Red
    $Issues | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
