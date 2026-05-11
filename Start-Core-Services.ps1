$rootPath = "c:\Users\Zz\IdeaProjects\apartment-rental-system"

Write-Host "正在启动核心服务..."

# 启动 Gateway (8080)
Write-Host "启动 Gateway (端口 8080)..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -pl apartment-gateway" -WorkingDirectory $rootPath -WindowStyle Normal

# 启动 User Service (8081)
Write-Host "启动 User Service (端口 8081)..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -pl apartment-user-service" -WorkingDirectory $rootPath -WindowStyle Normal

# 启动 House Service (8083)
Write-Host "启动 House Service (端口 8083)..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -pl apartment-house-service" -WorkingDirectory $rootPath -WindowStyle Normal

Write-Host "服务启动请求已发送，请等待新窗口中的服务启动完成。"
