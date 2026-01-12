# 医生端深度API测试脚本
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

Write-Host "========== Doctor Deep Test ==========" -ForegroundColor Cyan

# Login
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="doctor001";password="123456"}
$token = $res.data.token
if (-not $token) { Write-Host "Login failed!" -ForegroundColor Red; exit }
Write-Host "[OK] Doctor logged in" -ForegroundColor Green

# 1. Dashboard
Write-Host "`n--- 1. Dashboard ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/dashboard/statistics" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] Dashboard stats" -ForegroundColor Green; $Pass++
    Write-Host "  Patients: $($res.data.totalPatients), Reports: $($res.data.totalReports)"
} else { Write-Host "[FAIL] Dashboard" -ForegroundColor Red; $Fail++; $Issues += "Dashboard failed" }

# 2. Patients CRUD
Write-Host "`n--- 2. Patients ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/patients?pageNum=1&pageSize=10" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] List patients (Total: $($res.data.total))" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] List patients" -ForegroundColor Red; $Fail++; $Issues += "List patients failed" }

# 3. Patient Pool
Write-Host "`n--- 3. Patient Pool ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/patient-pool?pageNum=1&pageSize=10" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] Patient pool (Total: $($res.data.total))" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] Patient pool" -ForegroundColor Red; $Fail++; $Issues += "Patient pool failed" }

# 4. Appointments
Write-Host "`n--- 4. Appointments ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/appointments?pageNum=1&pageSize=10" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] List appointments" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] List appointments" -ForegroundColor Red; $Fail++; $Issues += "List appointments failed" }

$res = Test-Api "GET" "$BaseUrl/doctor/appointments/recent" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] Recent appointments" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] Recent appointments" -ForegroundColor Red; $Fail++; $Issues += "Recent appointments failed" }

# 5. Reports CRUD
Write-Host "`n--- 5. Reports ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/reports?pageNum=1&pageSize=10" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] List reports" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] List reports" -ForegroundColor Red; $Fail++; $Issues += "List reports failed" }

# 6. Consultations
Write-Host "`n--- 6. Consultations ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/consultations?pageNum=1&pageSize=10" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] List consultations" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] List consultations" -ForegroundColor Red; $Fail++; $Issues += "List consultations failed" }

# 7. Requests
Write-Host "`n--- 7. Requests ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/requests?pageNum=1&pageSize=10" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] List requests" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] List requests" -ForegroundColor Red; $Fail++; $Issues += "List requests failed" }

# 8. Profile
Write-Host "`n--- 8. Profile ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/user/profile" $null $token
if ($res.code -eq 200) {
    Write-Host "[PASS] Get profile" -ForegroundColor Green; $Pass++
} else { Write-Host "[FAIL] Get profile" -ForegroundColor Red; $Fail++; $Issues += "Get profile failed" }

# Results
Write-Host "`n========== Results ==========" -ForegroundColor Cyan
Write-Host "Pass: $Pass | Fail: $Fail"
if ($Issues.Count -gt 0) {
    Write-Host "Issues:" -ForegroundColor Red
    $Issues | ForEach-Object { Write-Host "  - $_" }
} else {
    Write-Host "All tests passed!" -ForegroundColor Green
}
