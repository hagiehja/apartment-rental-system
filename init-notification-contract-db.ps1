# 初始化通知服务和合同服务数据库的PowerShell脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "初始化通知服务和合同服务数据库" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$mysqlUser = "root"
$mysqlPassword = "123456"
$sqlFile = "init-notification-contract-db.sql"

# 检查SQL文件是否存在
if (-Not (Test-Path $sqlFile)) {
    Write-Host "错误: SQL文件不存在 - $sqlFile" -ForegroundColor Red
    exit 1
}

Write-Host "开始执行数据库初始化..." -ForegroundColor Yellow
Write-Host ""

# 读取SQL文件内容
$sqlContent = Get-Content -Path $sqlFile -Encoding UTF8 -Raw

# 执行SQL
try {
    # 使用临时文件避免编码问题
    $tempFile = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($tempFile, $sqlContent, [System.Text.Encoding]::UTF8)
    
    $process = Start-Process -FilePath "mysql" `
        -ArgumentList "-u$mysqlUser", "-p$mysqlPassword", "--default-character-set=utf8mb4" `
        -RedirectStandardInput $tempFile `
        -RedirectStandardOutput "db-init-output.log" `
        -RedirectStandardError "db-init-error.log" `
        -NoNewWindow `
        -Wait `
        -PassThru
    
    Remove-Item $tempFile -Force
    
    if ($process.ExitCode -eq 0) {
        Write-Host "✓ 数据库初始化成功!" -ForegroundColor Green
        Write-Host ""
        Write-Host "已创建以下数据库:" -ForegroundColor Yellow
        Write-Host "  • apartment_notification (通知服务)" -ForegroundColor Green
        Write-Host "  • apartment_contract (合同服务)" -ForegroundColor Green
        Write-Host ""
        
        # 显示输出日志
        if (Test-Path "db-init-output.log") {
            $output = Get-Content "db-init-output.log" -Encoding UTF8
            if ($output) {
                Write-Host "执行日志:" -ForegroundColor Yellow
                $output | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
            }
        }
    }
    else {
        Write-Host "✗ 数据库初始化失败!" -ForegroundColor Red
        Write-Host ""
        
        if (Test-Path "db-init-error.log") {
            $errors = Get-Content "db-init-error.log" -Encoding UTF8
            Write-Host "错误信息:" -ForegroundColor Red
            $errors | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
        }
        exit 1
    }
}
catch {
    Write-Host "✗ 执行失败: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "初始化完成!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
