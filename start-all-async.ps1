Write-Host "Starting All Backend Services..."

# Gateway (8080)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-gateway" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# User Service (8081)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-user-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# House Service (8083)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-house-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# Order Service (8088)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-order-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# Payment Service (8087)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-payment-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# Contract Service (8092)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-contract-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# Notification Service (8091)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-notification-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

Write-Host "Services launch commands sent. Check new windows for startup progress."
