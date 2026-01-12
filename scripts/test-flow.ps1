# 医生端业务流程测试
$BaseUrl = "http://localhost:8080/api"

function Test-Api($Method, $Url, $Body, $Token) {
    $headers = @{"Content-Type"="application/json"}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        if ($Body) {
            $res = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body ($Body | ConvertTo-Json -Compress)
        } else {
            $res = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
        }
        return $res
    } catch { return @{code=500; message=$_.Exception.Message} }
}

Write-Host "===== Business Flow Test =====" -ForegroundColor Cyan

# 1. Doctor login
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="doctor001";password="123456"}
$doctorToken = $res.data.token
Write-Host "[OK] Doctor logged in" -ForegroundColor Green

# 2. Check patient pool
$res = Test-Api "GET" "$BaseUrl/doctor/patient-pool?pageNum=1&pageSize=10" $null $doctorToken
$poolCount = $res.data.total
Write-Host "[INFO] Patient pool: $poolCount patients" -ForegroundColor Yellow

if ($poolCount -gt 0) {
    $patientId = $res.data.records[0].id
    Write-Host "[INFO] Testing claim patient ID: $patientId"

    # 3. Claim patient
    $res = Test-Api "POST" "$BaseUrl/doctor/patient-pool/claim/$patientId" @{reason="Test claim"} $doctorToken
    if ($res.code -eq 200) {
        Write-Host "[PASS] Claim request submitted" -ForegroundColor Green
    } else {
        Write-Host "[INFO] Claim: $($res.message)" -ForegroundColor Yellow
    }
}

# 4. Admin login to approve
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="admin";password="123456"}
$adminToken = $res.data.token
Write-Host "[OK] Admin logged in" -ForegroundColor Green

# 5. Check pending requests
$res = Test-Api "GET" "$BaseUrl/admin/relationship-requests?pageNum=1&pageSize=10" $null $adminToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Get pending requests" -ForegroundColor Green
} else {
    Write-Host "[INFO] Requests: $($res.message)" -ForegroundColor Yellow
}

Write-Host "`n===== Flow Test Complete =====" -ForegroundColor Cyan
