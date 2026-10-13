# ==========================================
# Multi-stage Dockerfile for CampusHub Backend
# Tối ưu hóa cho Render Free Tier (512MB RAM, tự động nhận $PORT)
# ==========================================

# ------------------------------------------
# Stage 1: Build JAR với Maven
# ------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy mã nguồn và đóng gói JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------
# Stage 2: Runtime image siêu nhẹ với JRE 17 Alpine
# ------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo non-root user spring để đảm bảo an toàn
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file jar từ build stage
COPY --from=builder /app/target/*.jar app.jar

# Render tự động inject biến môi trường PORT (mặc định 10000 hoặc 8080)
ENV PORT=8080
EXPOSE ${PORT}

# Tinh chỉnh JVM phù hợp với giới hạn 512MB RAM của Render Free Tier:
# - Xms256m / Xmx384m: Giới hạn heap trong mức an toàn tránh Render OOM 137
# - UseG1GC: Bộ thu gom rác tối ưu độ trễ
# - ExitOnOutOfMemoryError: Thoát sạch để Render restart thay vì treo container
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
