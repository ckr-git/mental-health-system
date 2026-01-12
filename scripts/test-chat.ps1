# Chat API Test
$BaseUrl = "http://localhost:8080/api"

# Login as patient
$loginRes = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"patient001","password":"123456"}'
$token = $loginRes.data.token
Write-Host "Token: $($token.Substring(0,30))..."

# Test chat list
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

try {
    $res = Invoke-RestMethod -Uri "$BaseUrl/patient/chat/list" -Method GET -Headers $headers
    Write-Host "[PASS] Chat list: $($res | ConvertTo-Json -Compress)"
} catch {
    Write-Host "[FAIL] Chat list: $($_.Exception.Message)"
}

# Create chat with doctor (ID=4)
try {
    $body = '{"doctorId":"4"}'
    $res = Invoke-RestMethod -Uri "$BaseUrl/patient/chat/create" -Method POST -Headers $headers -Body $body
    Write-Host "[PASS] Create chat: sessionId=$($res.data.sessionId)"
    $sessionId = $res.data.sessionId
    $targetUserId = $res.data.userId
} catch {
    Write-Host "[FAIL] Create chat: $($_.Exception.Message)"
}
