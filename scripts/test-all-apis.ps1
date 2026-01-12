# ============================================
# 测试所有24个后端API接口
# ============================================

$baseUrl = "http://localhost:8080/api"
$testUsername = "testuser_api"
$testPassword = "123456"
$token = $null

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "后端API接口测试" -ForegroundColor Cyan
Write-Host "============================================`n" -ForegroundColor Cyan

# 测试计数
$totalTests = 0
$passedTests = 0
$failedTests = 0

function Test-API {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [bool]$NeedsAuth = $true,
        [int]$ExpectedStatus = 200
    )
    
    $script:totalTests++
    
    try {
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        if ($NeedsAuth -and $script:token) {
            $headers["Authorization"] = "Bearer $script:token"
        }
        
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            UseBasicParsing = $true
        }
        
        if ($Body) {
            $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
        }
        
        $response = Invoke-WebRequest @params
        
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "✓ PASS: $Name" -ForegroundColor Green
            $script:passedTests++
            return $response.Content | ConvertFrom-Json
        } else {
            Write-Host "✗ FAIL: $Name (Expected $ExpectedStatus, Got $($response.StatusCode))" -ForegroundColor Red
            $script:failedTests++
            return $null
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "✓ PASS: $Name (Expected error $ExpectedStatus)" -ForegroundColor Green
            $script:passedTests++
        } else {
            Write-Host "✗ FAIL: $Name - Error: $($_.Exception.Message)" -ForegroundColor Red
            $script:failedTests++
        }
        return $null
    }
}

# ============================================
# 1. 注册测试账号
# ============================================
Write-Host "`n[1/24] 测试用户注册..." -ForegroundColor Yellow
$registerResult = Test-API `
    -Name "用户注册" `
    -Method "POST" `
    -Url "$baseUrl/auth/register" `
    -Body @{
        username = $testUsername
        password = $testPassword
        nickname = "API测试用户"
        phone = "13900000999"
    } `
    -NeedsAuth $false

# ============================================
# 2. 登录获取Token
# ============================================
Write-Host "`n[2/24] 测试用户登录..." -ForegroundColor Yellow
$loginResult = Test-API `
    -Name "用户登录" `
    -Method "POST" `
    -Url "$baseUrl/auth/login" `
    -Body @{
        username = $testUsername
        password = $testPassword
    } `
    -NeedsAuth $false

if ($loginResult -and $loginResult.data -and $loginResult.data.token) {
    $script:token = $loginResult.data.token
    Write-Host "Token获取成功！" -ForegroundColor Green
} else {
    Write-Host "登录失败，无法继续测试！" -ForegroundColor Red
    exit 1
}

# ============================================
# 情绪日记接口测试（6个）
# ============================================
Write-Host "`n[情绪日记接口]" -ForegroundColor Magenta

Write-Host "`n[3/24] 添加日记..." -ForegroundColor Yellow
$diary1 = Test-API `
    -Name "添加日记 #1" `
    -Method "POST" `
    -Url "$baseUrl/patient/mood-diary/add" `
    -Body @{
        moodScore = 7
        moodEmoji = "😊"
        title = "今天心情不错"
        content = "天气很好，出去散步了"
        energyLevel = 8
        sleepQuality = 7
        stressLevel = 3
    }

$diary2 = Test-API `
    -Name "添加日记 #2" `
    -Method "POST" `
    -Url "$baseUrl/patient/mood-diary/add" `
    -Body @{
        moodScore = 4
        moodEmoji = "😔"
        title = "有点低落"
        content = "工作压力有点大"
        energyLevel = 5
        sleepQuality = 5
        stressLevel = 7
    }

Write-Host "`n[4/24] 获取日记列表..." -ForegroundColor Yellow
$diaryList = Test-API `
    -Name "日记列表（分页）" `
    -Method "GET" `
    -Url "$baseUrl/patient/mood-diary/list?pageNum=1&pageSize=10"

Write-Host "`n[5/24] 获取日记详情..." -ForegroundColor Yellow
if ($diary1 -and $diary1.data -and $diary1.data.id) {
    $diaryDetail = Test-API `
        -Name "日记详情" `
        -Method "GET" `
        -Url "$baseUrl/patient/mood-diary/detail/$($diary1.data.id)"
}

Write-Host "`n[6/24] 更新日记状态..." -ForegroundColor Yellow
if ($diary1 -and $diary1.data -and $diary1.data.id) {
    Test-API `
        -Name "更新状态为better" `
        -Method "PUT" `
        -Url "$baseUrl/patient/mood-diary/status/$($diary1.data.id)" `
        -Body @{
            status = "better"
        }
}

Write-Host "`n[7/24] 获取统计数据..." -ForegroundColor Yellow
Test-API `
    -Name "用户统计数据" `
    -Method "GET" `
    -Url "$baseUrl/patient/mood-diary/stats"

Write-Host "`n[8/24] 获取最近日记..." -ForegroundColor Yellow
Test-API `
    -Name "最近日记" `
    -Method "GET" `
    -Url "$baseUrl/patient/mood-diary/recent?limit=5"

# ============================================
# 心情留言接口测试（4个）
# ============================================
Write-Host "`n[心情留言接口]" -ForegroundColor Magenta

Write-Host "`n[9/24] 添加留言..." -ForegroundColor Yellow
if ($diary1 -and $diary1.data -and $diary1.data.id) {
    $comment1 = Test-API `
        -Name "添加留言 #1" `
        -Method "POST" `
        -Url "$baseUrl/patient/mood-comment/add" `
        -Body @{
            diaryId = $diary1.data.id
            content = "现在回看，其实还好"
            commentType = "thought"
            moodAtComment = 8
            interactions = @("agree", "encourage")
        }
}

Write-Host "`n[10/24] 获取留言列表..." -ForegroundColor Yellow
if ($diary1 -and $diary1.data -and $diary1.data.id) {
    Test-API `
        -Name "留言列表" `
        -Method "GET" `
        -Url "$baseUrl/patient/mood-comment/list/$($diary1.data.id)"
}

Write-Host "`n[11/24] 更新互动标记..." -ForegroundColor Yellow
if ($comment1 -and $comment1.data -and $comment1.data.id) {
    Test-API `
        -Name "更新互动" `
        -Method "PUT" `
        -Url "$baseUrl/patient/mood-comment/interaction/$($comment1.data.id)" `
        -Body @{
            interactions = @("agree", "heartache", "encourage")
        }
}

Write-Host "`n[12/24] 删除留言..." -ForegroundColor Yellow
# 先添加一条用于测试删除
if ($diary1 -and $diary1.data -and $diary1.data.id) {
    $tempComment = Test-API `
        -Name "添加临时留言" `
        -Method "POST" `
        -Url "$baseUrl/patient/mood-comment/add" `
        -Body @{
            diaryId = $diary1.data.id
            content = "临时留言，用于测试删除"
            commentType = "random"
            moodAtComment = 6
        }
    
    if ($tempComment -and $tempComment.data -and $tempComment.data.id) {
        Test-API `
            -Name "删除留言" `
            -Method "DELETE" `
            -Url "$baseUrl/patient/mood-comment/$($tempComment.data.id)"
    }
}

# ============================================
# 时光信箱接口测试（7个）
# ============================================
Write-Host "`n[时光信箱接口]" -ForegroundColor Magenta

Write-Host "`n[13/24] 写信..." -ForegroundColor Yellow
$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$letter1 = Test-API `
    -Name "写信 #1" `
    -Method "POST" `
    -Url "$baseUrl/patient/time-capsule/write" `
    -Body @{
        letterType = "praise"
        title = "给未来的表扬信"
        content = "你最近做得很好，继续加油！"
        unlockDate = $tomorrow
    }

Write-Host "`n[14/24] 获取信箱列表..." -ForegroundColor Yellow
Test-API `
    -Name "信箱列表" `
    -Method "GET" `
    -Url "$baseUrl/patient/time-capsule/list"

Write-Host "`n[15/24] 检查可解锁信件..." -ForegroundColor Yellow
Test-API `
    -Name "检查解锁" `
    -Method "GET" `
    -Url "$baseUrl/patient/time-capsule/check-unlock"

Write-Host "`n[16/24] 解锁信件（需要到期）..." -ForegroundColor Yellow
# 创建一个今天就到期的信件
$today = (Get-Date).ToString("yyyy-MM-dd")
$letter2 = Test-API `
    -Name "写今天到期的信" `
    -Method "POST" `
    -Url "$baseUrl/patient/time-capsule/write" `
    -Body @{
        letterType = "thanks"
        title = "测试解锁"
        content = "这封信今天就可以打开"
        unlockDate = $today
    }

if ($letter2 -and $letter2.data -and $letter2.data.id) {
    # 检查解锁
    $unlockCheck = Test-API `
        -Name "检查解锁（应该有）" `
        -Method "GET" `
        -Url "$baseUrl/patient/time-capsule/check-unlock"
    
    # 解锁
    Test-API `
        -Name "解锁信件" `
        -Method "GET" `
        -Url "$baseUrl/patient/time-capsule/unlock/$($letter2.data.id)"
}

Write-Host "`n[17/24] 阅读信件..." -ForegroundColor Yellow
if ($letter2 -and $letter2.data -and $letter2.data.id) {
    Test-API `
        -Name "阅读信件" `
        -Method "POST" `
        -Url "$baseUrl/patient/time-capsule/read/$($letter2.data.id)"
}

Write-Host "`n[18/24] 回复信件..." -ForegroundColor Yellow
if ($letter2 -and $letter2.data -and $letter2.data.id) {
    Test-API `
        -Name "回复信件" `
        -Method "POST" `
        -Url "$baseUrl/patient/time-capsule/reply/$($letter2.data.id)" `
        -Body @{
            replyContent = "收到了，谢谢过去的自己！"
        }
}

Write-Host "`n[19/24] 获取信件详情..." -ForegroundColor Yellow
if ($letter1 -and $letter1.data -and $letter1.data.id) {
    Test-API `
        -Name "信件详情" `
        -Method "GET" `
        -Url "$baseUrl/patient/time-capsule/detail/$($letter1.data.id)"
}

# ============================================
# 主题配置接口测试（5个）
# ============================================
Write-Host "`n[主题配置接口]" -ForegroundColor Magenta

Write-Host "`n[20/24] 获取主题配置..." -ForegroundColor Yellow
Test-API `
    -Name "获取配置" `
    -Method "GET" `
    -Url "$baseUrl/patient/theme/config"

Write-Host "`n[21/24] 切换灯光模式..." -ForegroundColor Yellow
Test-API `
    -Name "切换灯光" `
    -Method "POST" `
    -Url "$baseUrl/patient/theme/toggle-light"

Write-Host "`n[22/24] 更新主题设置..." -ForegroundColor Yellow
Test-API `
    -Name "更新设置" `
    -Method "PUT" `
    -Url "$baseUrl/patient/theme/settings" `
    -Body @{
        weatherEnabled = 1
        particleEnabled = 1
        animationEnabled = 1
        soundEnabled = 1
        volume = 70
    }

Write-Host "`n[23/24] 获取所有天气配置..." -ForegroundColor Yellow
Test-API `
    -Name "天气配置列表" `
    -Method "GET" `
    -Url "$baseUrl/patient/theme/weather"

Write-Host "`n[24/24] 根据心情获取天气..." -ForegroundColor Yellow
Test-API `
    -Name "心情天气映射" `
    -Method "GET" `
    -Url "$baseUrl/patient/theme/weather/7"

# ============================================
# 测试结果统计
# ============================================
Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "测试结果" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "总测试数: $totalTests" -ForegroundColor White
Write-Host "通过: $passedTests" -ForegroundColor Green
Write-Host "失败: $failedTests" -ForegroundColor Red
Write-Host "通过率: $([math]::Round(($passedTests / $totalTests) * 100, 2))%" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Yellow" })
Write-Host "============================================`n" -ForegroundColor Cyan

if ($failedTests -eq 0) {
    Write-Host "🎉 所有接口测试通过！" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️ 有 $failedTests 个接口测试失败，请检查！" -ForegroundColor Red
    exit 1
}
