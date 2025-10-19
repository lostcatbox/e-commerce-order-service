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

log_info "=== 서버 중지 스크립트 ==="

# 1. 로컬 서버 중지
log_info "1. 로컬 서버 중지 중..."

# PID 파일에서 프로세스 ID 읽기
if [ -f .server_pids ]; then
    source .server_pids
    
    # Coupon 서버 중지
    if [ ! -z "$COUPON_PID" ] && ps -p $COUPON_PID > /dev/null; then
        log_info "Coupon 서버 중지 중... (PID: $COUPON_PID)"
        kill $COUPON_PID
        sleep 2
        
        # 강제 종료가 필요한 경우
        if ps -p $COUPON_PID > /dev/null; then
            log_warning "Coupon 서버 강제 종료 중..."
            kill -9 $COUPON_PID
        fi
        log_success "Coupon 서버가 중지되었습니다"
    else
        log_warning "Coupon 서버가 실행 중이 아닙니다"
    fi
    
    # Core 서버 중지
    if [ ! -z "$CORE_PID" ] && ps -p $CORE_PID > /dev/null; then
        log_info "Core 서버 중지 중... (PID: $CORE_PID)"
        kill $CORE_PID
        sleep 2
        
        # 강제 종료가 필요한 경우
        if ps -p $CORE_PID > /dev/null; then
            log_warning "Core 서버 강제 종료 중..."
            kill -9 $CORE_PID
        fi
        log_success "Core 서버가 중지되었습니다"
    else
        log_warning "Core 서버가 실행 중이 아닙니다"
    fi
    
    # PID 파일 삭제
    rm -f .server_pids
else
    log_warning "PID 파일을 찾을 수 없습니다. 수동으로 프로세스를 확인해주세요."
fi

# 2. Docker Compose 서비스 중지
log_info "2. Docker Compose 서비스 중지 중..."
docker-compose down

if [ $? -eq 0 ]; then
    log_success "Docker Compose 서비스가 중지되었습니다"
else
    log_warning "Docker Compose 서비스 중지 중 오류가 발생했습니다"
fi

# 3. 포트 사용 확인
log_info "3. 포트 사용 상태 확인..."
echo "포트 8080 (Core 서버):"
lsof -i :8080 2>/dev/null || echo "  사용 중인 프로세스 없음"
echo "포트 8081 (Coupon 서버):"
lsof -i :8080 2>/dev/null || echo "  사용 중인 프로세스 없음"

log_success "=== 모든 서비스가 중지되었습니다 ==="
