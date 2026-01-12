# 在线咨询API测试脚本
$BaseUrl = "http://localhost:8080/api"

function Test-Api($Method, $Url, $Body, $Token) {
    $headers = @{"Content-Type"="application/json"}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        if ($Body) {
            $json = $Body | ConvertTo-Json -Compress -Depth 10
            $res = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body $json
        } else {
            $res = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
        }
        return $res
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        return @{code=$statusCode; message=$_.Exception.Message}
    }
}

Write-Host "===== Chat API Test =====" -ForegroundColor Cyan

# 1. Patient login
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="patient001";password="123456"}
if ($res.code -eq 200) {
    $patientToken = $res.data.token
    $patientId = $res.data.userInfo.id
    Write-Host "[OK] Patient logged in (ID: $patientId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Patient login failed" -ForegroundColor Red
    exit
}

# 2. Doctor login
$res = Test-Api "POST" "$BaseUrl/auth/login" @{username="doctor001";password="123456"}
if ($res.code -eq 200) {
    $doctorToken = $res.data.token
    $doctorId = $res.data.userInfo.id
    Write-Host "[OK] Doctor logged in (ID: $doctorId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Doctor login failed" -ForegroundColor Red
    exit
}

# 3. Patient creates chat with doctor
Write-Host "`n--- Patient Chat ---" -ForegroundColor Yellow
$res = Test-Api "POST" "$BaseUrl/patient/chat/create" @{doctorId=$doctorId} $patientToken
if ($res.code -eq 200) {
    $sessionId = $res.data.sessionId
    $targetUserId = $res.data.userId
    Write-Host "[PASS] Create chat session (ID: $sessionId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Create chat: $($res.message)" -ForegroundColor Red
}

# 4. Patient gets chat list
$res = Test-Api "GET" "$BaseUrl/patient/chat/list" $null $patientToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Patient chat list (Count: $($res.data.Count))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Patient chat list: $($res.message)" -ForegroundColor Red
}

# 5. Patient sends message
$res = Test-Api "POST" "$BaseUrl/patient/chat/send" @{sessionId=$sessionId;targetUserId=$targetUserId;content="Hello doctor!";type="text"} $patientToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Patient send message" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Patient send: $($res.message)" -ForegroundColor Red
}

# 6. Patient gets messages
$res = Test-Api "GET" "$BaseUrl/patient/chat/messages/$sessionId`?pageNum=1&pageSize=50" $null $patientToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Patient get messages (Count: $($res.data.records.Count))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Patient get messages: $($res.message)" -ForegroundColor Red
}

# 7. Doctor gets chat list
Write-Host "`n--- Doctor Chat ---" -ForegroundColor Yellow
$res = Test-Api "GET" "$BaseUrl/doctor/chat/list" $null $doctorToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Doctor chat list (Count: $($res.data.Count))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Doctor chat list: $($res.message)" -ForegroundColor Red
}

# 8. Doctor gets messages
$res = Test-Api "GET" "$BaseUrl/doctor/chat/messages/$sessionId`?pageNum=1&pageSize=50" $null $doctorToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Doctor get messages (Count: $($res.data.records.Count))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Doctor get messages: $($res.message)" -ForegroundColor Red
}

# 9. Doctor sends reply
$patientId = $res.data.records[0].senderId
$res = Test-Api "POST" "$BaseUrl/doctor/chat/send" @{sessionId=$sessionId;targetUserId=2;content="Hi patient!";type="text"} $doctorToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Doctor send message" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Doctor send: $($res.message)" -ForegroundColor Red
}

# 10. Mark as read
$res = Test-Api "POST" "$BaseUrl/doctor/chat/read/$sessionId" $null $doctorToken
if ($res.code -eq 200) {
    Write-Host "[PASS] Mark as read" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Mark as read: $($res.message)" -ForegroundColor Red
}

Write-Host "`n===== Test Complete =====" -ForegroundColor Cyan
