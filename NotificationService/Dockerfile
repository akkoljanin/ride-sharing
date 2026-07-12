FROM eclipse-temurin:21-jre
EXPOSE 8774
ADD target/notification-service-1.0.0.jar /app/notification-service.jar
ENTRYPOINT ["java","-jar","/app/notification-service.jar"]
