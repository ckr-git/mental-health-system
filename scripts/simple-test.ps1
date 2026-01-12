# Simple chat test
$res = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/auth/login" -Headers @{"Content-Type"="application/json"} -Body '{"username":"patient001","password":"123456"}'
$token = $res.data.token
Write-Host "Token obtained: $($token.Substring(0,30))..."

# Test chat list
$headers = @{"Content-Type"="application/json"; "Authorization"="Bearer $token"}
try {
    $chatRes = Invoke-RestMethod -Method GET -Uri "http://localhost:8080/api/patient/chat/list" -Headers $headers
    Write-Host "Chat list: $($chatRes | ConvertTo-Json -Compress)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Status: $($_.Exception.Response.StatusCode)"
}
