#!/bin/bash

# 쿠폰 발급 시스템 테스트 스크립트
# Kafka 기반 비동기 쿠폰 발급 시스템의 전체 플로우를 테스트합니다.

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
COUPON_SERVICE_URL="http://localhost:8081"
MYSQL_DB="e_commerce_db"
MYSQL_USER="application"
MYSQL_PASSWORD="application"

echo -e "${BLUE}🚀 쿠폰 발급 시스템 테스트 시작${NC}"
echo "================================================"

# 함수 정의
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# MySQL 연결 테스트
test_mysql_connection() {
    log_info "MySQL 연결 테스트 중..."
    if docker exec server-kotlin-mysql-1 mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
        log_success "MySQL 연결 성공"
    else
        log_error "MySQL 연결 실패. 설정을 확인해주세요."
        exit 1
    fi
}

# 데이터베이스 초기화
init_database() {
    log_info "데이터베이스 초기화 중..."

    # SQL 파일을 사용하여 데이터 초기화
    if [ -f "test-coupon-setting-data.sql" ]; then
        log_info "SQL 파일을 사용하여 데이터 초기화 중..."
        docker exec -i server-kotlin-mysql-1 mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < test-coupon-setting-data.sql
        log_success "데이터베이스 초기화 완료"
    else
        log_error "test-coupon-setting-data.sql 파일을 찾을 수 없습니다."
        exit 1
    fi
}

# 쿠폰 서비스 상태 확인
check_coupon_service() {
    log_info "쿠폰 서비스 상태 확인 중..."

    if curl -s -f "$COUPON_SERVICE_URL/actuator/health" > /dev/null 2>&1; then
        log_success "쿠폰 서비스 정상 동작 중"
    else
        log_warning "쿠폰 서비스가 실행되지 않았습니다. 8081 포트에서 서비스를 시작해주세요."
        log_info "서비스 시작 명령: ./gradlew :coupon:bootRun --args='--server.port=8081'"
        exit 1
    fi
}

# 쿠폰 발급 API 테스트
test_coupon_issue_api() {
    log_info "쿠폰 발급 API 테스트 시작..."

    local success_count=0
    local total_requests=0

    # 각 유저가 각 쿠폰에 대해 발급 요청 (3x3 = 9개 요청)
    for user_id in 1 2 3; do
        for coupon_id in 1 2 3; do
            total_requests=$((total_requests + 1))

            log_info "유저 $user_id가 쿠폰 $coupon_id 발급 요청 중..."

            response=$(curl -s -w "\n%{http_code}" -X POST \
                "$COUPON_SERVICE_URL/api/coupons/$coupon_id/issue?userId=$user_id" \
                -H "Content-Type: application/json")

            http_code=$(echo "$response" | tail -n1)
            response_body=$(echo "$response" | sed '$d')

            if [ "$http_code" = "200" ]; then
                log_success "유저 $user_id -> 쿠폰 $coupon_id 발급 요청 성공"
                success_count=$((success_count + 1))
            else
                log_error "유저 $user_id -> 쿠폰 $coupon_id 발급 요청 실패 (HTTP: $http_code)"
                echo "응답: $response_body"
            fi

            # 요청 간 간격 (API 부하 방지)
            sleep 0.1
        done
    done

    log_info "API 테스트 완료: $success_count/$total_requests 성공"

    if [ $success_count -eq $total_requests ]; then
        log_success "모든 API 요청이 성공했습니다!"
    else
        log_warning "일부 API 요청이 실패했습니다."
    fi
}

# Consumer 처리 확인
check_consumer_processing() {
    log_info "Consumer 처리 확인 중..."
    log_info "잠시 대기 중... (Consumer가 이벤트를 처리하는 시간)"
    sleep 5

    # 발급된 쿠폰 확인
    log_info "발급된 쿠폰 확인 중..."

    issued_coupons=$(docker exec server-kotlin-mysql-1 mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" -sN -e "SELECT COUNT(*) FROM user_coupon WHERE user_id IN (1, 2, 3);" 2>/dev/null)

    log_info "발급된 쿠폰 수: $issued_coupons"

    if [ "$issued_coupons" -gt 0 ]; then
        log_success "Consumer가 쿠폰을 성공적으로 발급했습니다!"

        # 상세 정보 출력
        log_info "발급된 쿠폰 상세 정보:"
        docker exec server-kotlin-mysql-1 mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB"  -sN -e "
SELECT
    uc.user_coupon_id,
    uc.user_id,
    uc.coupon_id,
    uc.status
FROM user_coupon uc
WHERE uc.user_id IN (1, 2, 3)
ORDER BY uc.user_id, uc.coupon_id;"
    else
        log_error "Consumer가 쿠폰을 발급하지 못했습니다."
        log_info "Kafka Consumer 로그를 확인해주세요."
    fi
}

# 쿠폰 재고 확인
check_coupon_stock() {
    log_info "쿠폰 재고 확인 중..."

    docker exec server-kotlin-mysql-1 mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB"  -sN -e"
SELECT
    coupon_id,
    description,
    stock,
    (10 - stock) as issued_count
FROM coupon
WHERE coupon_id IN (1, 2, 3)
ORDER BY coupon_id;
"
}

# 메인 실행
main() {
    echo -e "${BLUE}📋 테스트 시나리오:${NC}"
    echo "1. 데이터베이스 초기화 (쿠폰 3개, 유저 3명)"
    echo "2. 쿠폰 발급 API 테스트 (9개 요청)"
    echo "3. Consumer 처리 확인"
    echo "4. 결과 분석"
    echo ""

    # 1. MySQL 연결 테스트
    test_mysql_connection

    # 2. 데이터베이스 초기화
    init_database

    # 3. 쿠폰 서비스 상태 확인
    check_coupon_service

    # 4. API 테스트
    test_coupon_issue_api

    # 5. Consumer 처리 확인
    check_consumer_processing

    # 6. 재고 확인
    check_coupon_stock

    echo ""
    echo -e "${GREEN}🎉 테스트 완료!${NC}"
    echo "================================================"
}

# 스크립트 실행
main "$@"
