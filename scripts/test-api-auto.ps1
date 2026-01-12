# API自动化测试脚本
$BaseUrl = "http://localhost:8080/api"
$Pass = 0
$Fail = 0

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

Write-Host "========== API Test Start ==========" -ForegroundColor Cyan

# 1. Register
$user = "test_" + (Get-Random)
$res = Test-Api "POST" "$BaseUrl/auth/register" @{username=$user;password="123456";nickname="Test";role="PATIENT"}
if ($res.code -eq 200) { Write-Host "[PASS] Register" -ForegroundColor Green; $Pass++ }
else { Write-Host "[FAIL] Register: $($res.message)" -ForegroundColor Red; $Fail++ }

# 2. Login
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username=$user;password="123456"}
$token = $null
if ($res.code -eq 200 -and $res.data.token) {
    Write-Host "[PASS] Login" -ForegroundColor Green; $Pass++
    $token = $res.data.token
} else { Write-Host "[FAIL] Login" -ForegroundColor Red; $Fail++ }

# 3. Wrong password
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username=$user;password="wrong"}
if ($res.code -eq 500) { Write-Host "[PASS] Wrong password rejected" -ForegroundColor Green; $Pass++ }
else { Write-Host "[FAIL] Wrong password not rejected" -ForegroundColor Red; $Fail++ }

# 4. Get profile with token
if ($token) {
    $res = Test-Api "GET" "$BaseUrl/user/profile" $null $token
    if ($res.code -eq 200) { Write-Host "[PASS] Get profile" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Get profile" -ForegroundColor Red; $Fail++ }
}

# 5. Admin login
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="admin";password="123456"}
$adminToken = $null
if ($res.code -eq 200 -and $res.data.token) {
    Write-Host "[PASS] Admin login" -ForegroundColor Green; $Pass++
    $adminToken = $res.data.token
} else { Write-Host "[FAIL] Admin login" -ForegroundColor Red; $Fail++ }

# 6. Admin get users
if ($adminToken) {
    $res = Test-Api "GET" "$BaseUrl/admin/users?pageNum=1&pageSize=10" $null $adminToken
    if ($res.code -eq 200) { Write-Host "[PASS] Admin get users" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Admin get users" -ForegroundColor Red; $Fail++ }
}

# 7. Admin get resources
if ($adminToken) {
    $res = Test-Api "GET" "$BaseUrl/admin/resources/statistics" $null $adminToken
    if ($res.code -eq 200) { Write-Host "[PASS] Admin get resources stats" -ForegroundColor Green; $Pass++ }
    else { Write-Host "[FAIL] Admin get resources stats" -ForegroundColor Red; $Fail++ }
}

Write-Host "`n========== Results ==========" -ForegroundColor Cyan
Write-Host "Pass: $Pass | Fail: $Fail | Total: $($Pass+$Fail)"
if ($Fail -eq 0) { Write-Host "All tests passed!" -ForegroundColor Green }
else { Write-Host "Some tests failed!" -ForegroundColor Red }
