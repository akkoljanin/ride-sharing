FROM eclipse-temurin:21-jre
EXPOSE 8761
ADD target/naming-server-1.0.0.jar /app/naming-server.jar
ENTRYPOINT ["java","-jar","/app/naming-server.jar"]
