# 启动订单和支付服务

Write-Host "=== 启动订单和支付服务 ===" -ForegroundColor Green

# 检查是否已初始化数据库
Write-Host "`n检查数据库..." -ForegroundColor Yellow
$dbCheck = mysql -uroot -p123456 -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='apartment_db' AND TABLE_NAME='rental_order';" 2>$null

if ($LASTEXITCODE -ne 0) {
    Write-Host "错误：无法连接到数据库，请确保MySQL正在运行" -ForegroundColor Red
    exit 1
}

Write-Host "数据库连接成功！" -ForegroundColor Green

# 启动订单服务
Write-Host "`n启动订单服务（端口8088）..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\apartment-order-service'; mvn spring-boot:run"

# 等待3秒
Start-Sleep -Seconds 3

# 启动支付服务
Write-Host "启动支付服务（端口8087）..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\apartment-payment-service'; mvn spring-boot:run"

Write-Host "`n等待服务启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 测试服务
Write-Host "`n测试服务健康状态..." -ForegroundColor Yellow

try {
    $orderHealth = Invoke-RestMethod -Uri "http://localhost:8088/order/health" -ErrorAction Stop
    Write-Host "✓ 订单服务运行正常" -ForegroundColor Green
}
catch {
    Write-Host "✗ 订单服务未响应" -ForegroundColor Red
}

try {
    $paymentHealth = Invoke-RestMethod -Uri "http://localhost:8087/payment/health" -ErrorAction Stop
    Write-Host "✓ 支付服务运行正常" -ForegroundColor Green
}
catch {
    Write-Host "✗ 支付服务未响应" -ForegroundColor Red
}

Write-Host "`n=== 服务启动完成 ===" -ForegroundColor Green
Write-Host "订单服务：http://localhost:8088" -ForegroundColor Cyan
Write-Host "支付服务：http://localhost:8087" -ForegroundColor Cyan
