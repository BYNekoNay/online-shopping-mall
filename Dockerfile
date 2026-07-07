FROM maven:3.9.6-eclipse-temurin-17-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY backend/src ./backend/src
RUN mvn -B -f backend/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/backend/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动参数
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
