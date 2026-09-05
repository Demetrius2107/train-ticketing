# 网关镜像：先在根目录执行 mvn -DskipTests package 生成 jar，再 docker compose build
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
