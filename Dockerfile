# 1단계: 빌드 (JDK로 jar 생성)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 캐시 최적화: gradle 설정 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌드 산출물만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 비root 사용자로 실행 (컨테이너 탈취 시 권한 최소화)
RUN useradd -r -u 1001 appuser && chown appuser:appuser /app/app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
