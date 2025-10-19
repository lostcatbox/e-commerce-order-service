#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수들
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 프로젝트 루트 디렉토리로 이동
cd "$(dirname "$0")"

log_info "=== 서버 시작 스크립트 ==="
log_info "프로젝트 디렉토리: $(pwd)"

# 1. Docker Compose 실행
log_info "1. Docker Compose 서비스 시작 중..."
docker-compose up -d

if [ $? -ne 0 ]; then
    log_error "Docker Compose 실행 실패"
    exit 1
fi

log_success "Docker Compose 서비스가 성공적으로 시작되었습니다"

# 2. 30초 대기
log_info "2. 인프라 서비스 초기화를 위해 30초 대기 중..."
for i in {30..1}; do
    echo -ne "\r${YELLOW}대기 중... ${i}초${NC}"
    sleep 1
done
echo ""

log_success "대기 완료"

# 3. 서비스 상태 확인
log_info "3. 서비스 상태 확인 중..."
docker-compose ps

# 4. Coupon 서버 로컬 배포
log_info "4. Coupon 서버 로컬 배포 중..."
cd coupon

# Coupon 서버 빌드 및 실행
log_info "Coupon 서버 빌드 중..."
../gradlew bootJar

if [ $? -ne 0 ]; then
    log_error "Coupon 서버 빌드 실패"
    exit 1
fi

log_info "Coupon 서버 실행 중... (포트: 8081)"
nohup java -jar build/libs/*.jar > ../logs/coupon/coupon.log 2>&1 &
COUPON_PID=$!

# Coupon 서버 시작 확인
sleep 5
if ps -p $COUPON_PID > /dev/null; then
    log_success "Coupon 서버가 성공적으로 시작되었습니다 (PID: $COUPON_PID)"
    echo $COUPON_PID > ../logs/coupon/coupon.pid
else
    log_error "Coupon 서버 시작 실패"
    exit 1
fi

# 5. Core 서버 로컬 배포
log_info "5. Core 서버 로컬 배포 중..."
cd ../core

# Core 서버 빌드 및 실행
log_info "Core 서버 빌드 중..."
../gradlew bootJar

if [ $? -ne 0 ]; then
    log_error "Core 서버 빌드 실패"
    exit 1
fi

log_info "Core 서버 실행 중... (포트: 8080)"
nohup java -jar build/libs/*.jar > ../logs/core/core.log 2>&1 &
CORE_PID=$!

# Core 서버 시작 확인
sleep 5
if ps -p $CORE_PID > /dev/null; then
    log_success "Core 서버가 성공적으로 시작되었습니다 (PID: $CORE_PID)"
    echo $CORE_PID > ../logs/core/core.pid
else
    log_error "Core 서버 시작 실패"
    exit 1
fi

# 6. 서비스 헬스 체크
log_info "6. 서비스 헬스 체크 중..."

# Coupon 서버 헬스 체크
log_info "Coupon 서버 헬스 체크..."
for i in {1..10}; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        log_success "Coupon 서버 헬스 체크 성공"
        break
    fi
    if [ $i -eq 10 ]; then
        log_warning "Coupon 서버 헬스 체크 실패 (서버가 아직 시작 중일 수 있습니다)"
    else
        sleep 2
    fi
done

# Core 서버 헬스 체크
log_info "Core 서버 헬스 체크..."
for i in {1..10}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_success "Core 서버 헬스 체크 성공"
        break
    fi
    if [ $i -eq 10 ]; then
        log_warning "Core 서버 헬스 체크 실패 (서버가 아직 시작 중일 수 있습니다)"
    else
        sleep 2
    fi
done

# 7. 최종 상태 출력
log_success "=== 모든 서비스가 성공적으로 시작되었습니다 ==="
echo ""
log_info "서비스 정보:"
echo "  - MySQL: localhost:3406"
echo "  - Redis: localhost:6379"
echo "  - Kafka: localhost:9092"
echo "  - Kafka UI: http://localhost:9090"
echo "  - Coupon 서버: http://localhost:8081"
echo "  - Core 서버: http://localhost:8080"
echo ""
log_info "로그 파일 위치:"
echo "  - Coupon 서버: logs/coupon/coupon.log"
echo "  - Core 서버: logs/core/core.log"
echo ""
log_info "서비스 중지 방법:"
echo "  - 스크립트: ./stop.sh"
echo "  - 수동: docker-compose down"
echo ""

log_success "start.sh 실행 완료!"
