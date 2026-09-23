$env:JAVA_OPTS = "-Dfile.encoding=UTF-8"
java -jar target/apartment-notice-service-0.0.1-SNAPSHOT.jar "--spring.datasource.url=jdbc:mysql://localhost:3306/apartment_notification?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
