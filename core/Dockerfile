# Core 서버 (Producer) Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 시간대 설정
RUN apt-get update && apt-get install -y tzdata && \
    ln -fs /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

# Gradle Wrapper와 소스 코드 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY core core

# 실행 권한 부여
RUN chmod +x ./gradlew

# Core 모듈 빌드
RUN ./gradlew :core:bootJar

# 생성된 JAR 파일 복사
RUN cp core/build/libs/coupon-issue-core.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# 8080 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
