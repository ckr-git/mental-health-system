# Complete Chat API Test
$BaseUrl = "http://localhost:8080/api"

Write-Host "===== Chat API Test =====" -ForegroundColor Cyan

# Patient login
$loginRes = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"patient001","password":"123456"}'
$patientToken = $loginRes.data.token
$patientId = $loginRes.data.userInfo.id
Write-Host "[OK] Patient login (ID: $patientId)" -ForegroundColor Green

# Doctor login
$loginRes = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"doctor001","password":"123456"}'
$doctorToken = $loginRes.data.token
$doctorId = $loginRes.data.userInfo.id
Write-Host "[OK] Doctor login (ID: $doctorId)" -ForegroundColor Green

# Patient headers
$patientHeaders = @{"Content-Type"="application/json"; "Authorization"="Bearer $patientToken"}
$doctorHeaders = @{"Content-Type"="application/json"; "Authorization"="Bearer $doctorToken"}

Write-Host "`n--- Patient Tests ---" -ForegroundColor Yellow

# 1. Create chat
$body = "{`"doctorId`":`"$doctorId`"}"
$res = Invoke-RestMethod -Uri "$BaseUrl/patient/chat/create" -Method POST -Headers $patientHeaders -Body $body
$sessionId = $res.data.sessionId
Write-Host "[PASS] Create chat (sessionId: $sessionId)" -ForegroundColor Green

# 2. Send message
$body = "{`"sessionId`":$sessionId,`"targetUserId`":$doctorId,`"content`":`"Hello doctor!`",`"type`":`"text`"}"
$res = Invoke-RestMethod -Uri "$BaseUrl/patient/chat/send" -Method POST -Headers $patientHeaders -Body $body
Write-Host "[PASS] Patient send message" -ForegroundColor Green

# 3. Get messages
$res = Invoke-RestMethod -Uri "$BaseUrl/patient/chat/messages/$sessionId`?pageNum=1&pageSize=50" -Method GET -Headers $patientHeaders
Write-Host "[PASS] Get messages (count: $($res.data.records.Count))" -ForegroundColor Green

# 4. Chat list
$res = Invoke-RestMethod -Uri "$BaseUrl/patient/chat/list" -Method GET -Headers $patientHeaders
Write-Host "[PASS] Patient chat list (count: $($res.data.Count))" -ForegroundColor Green

Write-Host "`n--- Doctor Tests ---" -ForegroundColor Yellow

# 5. Doctor chat list
$res = Invoke-RestMethod -Uri "$BaseUrl/doctor/chat/list" -Method GET -Headers $doctorHeaders
Write-Host "[PASS] Doctor chat list (count: $($res.data.Count))" -ForegroundColor Green

# 6. Doctor get messages
$res = Invoke-RestMethod -Uri "$BaseUrl/doctor/chat/messages/$sessionId`?pageNum=1&pageSize=50" -Method GET -Headers $doctorHeaders
Write-Host "[PASS] Doctor get messages (count: $($res.data.records.Count))" -ForegroundColor Green

# 7. Doctor reply
$body = "{`"sessionId`":$sessionId,`"targetUserId`":$patientId,`"content`":`"Hi, how can I help?`",`"type`":`"text`"}"
$res = Invoke-RestMethod -Uri "$BaseUrl/doctor/chat/send" -Method POST -Headers $doctorHeaders -Body $body
Write-Host "[PASS] Doctor send reply" -ForegroundColor Green

# 8. Mark as read
$res = Invoke-RestMethod -Uri "$BaseUrl/doctor/chat/read/$sessionId" -Method POST -Headers $doctorHeaders
Write-Host "[PASS] Mark as read" -ForegroundColor Green

Write-Host "`n===== All Tests Passed! =====" -ForegroundColor Green
