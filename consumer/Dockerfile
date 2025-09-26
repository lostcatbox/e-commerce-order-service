# Consumer 서버 Dockerfile
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
COPY consumer consumer

# 실행 권한 부여
RUN chmod +x ./gradlew

# Consumer 모듈 빌드 (Core에 의존)
RUN ./gradlew :consumer:bootJar

# 생성된 JAR 파일 복사
RUN cp consumer/build/libs/coupon-issue-consumer.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# Consumer는 웹 서버가 아니므로 포트 노출 불필요
# 하지만 actuator를 위해 8081 포트 사용
EXPOSE 8081

# 애플리케이션 실행
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
