$body = @{
    username = "patient001"
    password = "123456"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}

Write-Host "测试登录接口..." -ForegroundColor Yellow
Write-Host "用户名: patient001" -ForegroundColor Cyan
Write-Host "密码: 123456" -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -Headers $headers
    Write-Host "登录成功！" -ForegroundColor Green
    Write-Host "响应数据:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10
}
catch {
    Write-Host "登录失败！" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "详细错误: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}
