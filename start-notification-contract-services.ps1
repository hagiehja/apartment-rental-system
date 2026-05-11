Write-Host "Starting Notification and Contract Services..."

# Notification Service (8091)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-notice-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

# Contract Service (8092)
Start-Process mvn -ArgumentList "spring-boot:run -pl apartment-contract-service" -WorkingDirectory "c:\Users\Zz\IdeaProjects\apartment-rental-system"

Write-Host "Services launch commands sent. Check new windows."
