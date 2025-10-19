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

log_info "=== Coupon Consumer 스케일 아웃 ==="

# 설정 변수
CONSUMER_COUNT=2  # Consumer 인스턴스 수
CONSUMER_GROUP_ID="coupon-issue-consumer-group"

# 1. 서비스 상태 확인
log_info "1. 서비스 상태 확인 중..."

# Coupon 서버 확인
if ! curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    log_error "Coupon 서버가 실행 중이 아닙니다. start.sh를 먼저 실행해주세요."
    exit 1
fi

log_success "Coupon 서버가 정상적으로 실행 중입니다"

# 2. Consumer 스케일 아웃
log_info "2. Consumer 스케일 아웃 시작..."

# Consumer 프로세스 ID 저장용 배열
declare -a CONSUMER_PIDS=()

# Consumer 인스턴스들 시작
for i in $(seq 1 $CONSUMER_COUNT); do
    log_info "Consumer 인스턴스 $i 시작 중..."

    # 각 Consumer를 별도 포트로 실행
    nohup java -jar coupon/build/libs/*.jar \
        --server.port=$((8081 + i)) \
        --spring.kafka.consumer.group-id=$CONSUMER_GROUP_ID \
        > logs/coupon/consumer-$i.log 2>&1 &

    CONSUMER_PID=$!
    CONSUMER_PIDS+=($CONSUMER_PID)

    # Consumer 시작 확인
    sleep 3
    if ps -p $CONSUMER_PID > /dev/null; then
        log_success "Consumer 인스턴스 $i 시작됨 (PID: $CONSUMER_PID, 포트: $((8081 + i)))"
    else
        log_error "Consumer 인스턴스 $i 시작 실패"
    fi
done

# 잠시 대기 (Consumer들이 등록될 시간)
sleep 5

# 4. 결과 요약
log_success "=== Consumer 스케일 아웃 완료 ==="
echo ""
log_info "실행된 Consumer 정보:"
echo "  - Consumer Group: $CONSUMER_GROUP_ID"
echo "  - Consumer 인스턴스 수: $CONSUMER_COUNT"
echo "  - 각 Consumer 포트: 8082, 8083"
echo "  - 로그 파일 위치: logs/coupon/consumer-*.log"
echo "  - Kafka UI: http://localhost:9090"
echo ""

# 5. Consumer 정리 옵션
echo "Consumer 인스턴스들을 정리하시겠습니까? (y/n)"
read -r response
if [[ "$response" =~ ^[Yy]$ ]]; then
    log_info "Consumer 인스턴스 정리 중..."
    for pid in "${CONSUMER_PIDS[@]}"; do
        if ps -p $pid > /dev/null; then
            kill $pid
            log_info "Consumer PID $pid 종료됨"
        fi
    done
    log_success "모든 Consumer 인스턴스가 정리되었습니다"
else
    log_info "Consumer 인스턴스들이 계속 실행 중입니다"
    echo "수동으로 종료하려면: kill ${CONSUMER_PIDS[*]}"
fi

log_success "스케일 아웃 스크립트 완료!"
