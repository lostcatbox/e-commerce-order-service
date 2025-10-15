# Kafka 기반으로 선착순 쿠폰 발급 시스템 개선

## 📋 목차
1. 시스템 개요
2. 아키텍처
3. 멀티모듈 구조
4. Kafka Pub/Sub 구조
5. 결론

---

## 🎯 시스템 개요

기존 쿠폰 발급 시스템인 **쿠폰 발급 요청 즉시 발급 시스템(동기), 스케줄러 기반 쿠폰 발급 시스템(비동기)**에서
**Kafka 기반 이벤트 드리븐 아키텍처**로 개선한 시스템입니다.

### 주요 개선사항
- **비동기 처리**: 쿠폰 발급 요청과 처리 분리
- **높은 처리량**: Kafka의 고성능 메시징으로 대량 요청 처리
- **확장성**: Consumer 인스턴스 수평 확장 가능
- **순서 보장**: 파티션 키를 통한 쿠폰 종류별 순서 보장

---

## 🏗️ 아키텍처

```mermaid
graph TB
    subgraph "Client Layer"
        C[클라이언트]
    end

    subgraph "Core Module"
        CC[Core API Client]
    end

    subgraph "Coupon Module"
        API[Coupon API<br/>Controller]
        CS[Coupon Service]
        KP[Kafka Producer]
        KC[Kafka Consumer]
        CP[Coupon Service]
    end

    subgraph "Message Broker"
        K[Kafka<br/>coupon-issue-events]
    end

    subgraph "Data Layer"
        DB[(MySQL<br/>Database)]
        R[(Redis<br/>분산락)]
    end

    C -->|1. 쿠폰 발급 요청| API
    API -->|2. 검증 & 이벤트 생성| CS
    CS -->|3. 이벤트 발행| KP
    KP -->|4. 메시지 전송| K
    API -->|5. 즉시 응답| C

    K -->|6. 이벤트 소비| KC
    KC -->|7. 쿠폰 발급 처리| CP
    CP -->|8. 분산락| R
    CP -->|9. 쿠폰 발급| DB

    CC -->|쿠폰 정보 조회, 쿠폰 사용 처리 요청| API

```

### 처리 흐름

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant CouponAPI as Coupon API
    participant Kafka as Kafka Broker
    participant Consumer as Coupon Consumer
    participant DB as Database
    participant Redis as Redis<br/>(분산락)

    Client->>CouponAPI: POST /api/coupons/{couponId}/issue?userId={userId}
    CouponAPI->>CouponAPI: 1. 사용자/쿠폰 검증
    CouponAPI->>CouponAPI: 2. 재고 확인
    CouponAPI->>CouponAPI: 3. 중복 발급 확인
    CouponAPI->>Kafka: 4. CouponIssueEvent 발행
    CouponAPI->>Client: 즉시 응답 (요청 접수됨)

    Kafka->>Consumer: 이벤트 소비
    Consumer->>Redis: 분산락 획득
    Consumer->>DB: 쿠폰 재고 차감 (비관적락)
    Consumer->>DB: 사용자 쿠폰 생성
    Consumer->>Redis: 분산락 해제
    Consumer->>Kafka: ACK (처리 완료)
```

---

## 📦 멀티모듈 구조

```mermaid
graph TD

    subgraph "Common Module"
        CE[CouponIssueEvent]
    end

    subgraph "Core Module"
        CA[Controller]
        CS[Service]
        CD2[Domain]
        CR[Repository]
        CC[Core API Client]

        CA --> CS
        CS --> CD2
        CS --> CR
        CS --> CC
    end

    subgraph "Coupon Module"
        CAPI[Coupon API<br/>Controller]
        CS2[Coupon Service]
        KP[Kafka Producer]
        KC[Kafka Consumer]
        CD3[Coupon Domain]
        CR2[Coupon Repository]

        CAPI --> CS2
        CS2 --> CD3
        CS2 --> CR2
        CS2 --> KP
        KC --> CS2

    end

    CC -.->|API 호출| CAPI
```

### 모듈별 역할

#### 📦 Common Module
- **역할**: 공통 도메인 객체 및 이벤트 정의
- **구성요소**:
  - CouponIssueEvent (Kafka 이벤트)
  - 공통 도메인 객체
  - 공통 유틸리티

#### 🎯 Core Module
- **역할**: 주문, 결제, 포인트 등 핵심 비즈니스 로직
- **포트**: 8080
- **구성요소**:
  - REST API Controller (주문, 결제, 포인트)
  - 비즈니스 로직 서비스
  - Core API Client (쿠폰 서비스 호출용)
  - Repository 계층

#### 🔄 Coupon Module (Publisher + Consumer)
- **역할**: 쿠폰 관련 모든 기능 (API, Producer, Consumer)
- **포트**: 8081
- **구성요소**:
  - Coupon API Controller (쿠폰 발급, 조회, 사용)
  - Coupon Service (비즈니스 로직)
  - Kafka Producer (이벤트 발행)
  - Kafka Consumer (이벤트 소비)
  - 쿠폰 도메인 및 Repository

---

## 🔄 Kafka Pub/Sub 구조

### 핵심 특징

#### 1. 순서 보장
- **파티션 키 활용**: `coupon-{couponId}` 형식으로 쿠폰별 파티셔닝
- **같은 쿠폰 요청**: 항상 같은 파티션으로 라우팅되어 순서 보장

#### 2. 동시성 제어
- **분산락**: Redis 기반 분산락으로 쿠폰별 동시성 제어
- **비관적 락**: 데이터베이스 레벨 락으로 재고 정합성 보장

#### 3. Coupon 인스턴스 확장
```bash
# 여러 Coupon 인스턴스 실행 (수평 확장)
./gradlew :coupon:bootRun --args='--server.port=8082'
./gradlew :coupon:bootRun --args='--server.port=8083'
```

---

## 📝 결론

Kafka 기반 쿠폰 발급 시스템은 **Coupon 모듈에서 모든 쿠폰 관련 기능을 전체를 책임**을 갖도록 하였습니다.

### **현재 아키텍처의 특징**
- **이벤트 드리븐 아키텍처**: 비동기 메시징으로 높은 처리량과 확장성 확보
- **높은 응집도**: 쿠폰 관련 모든 로직이 한 모듈에 집중

### **시스템 장점**
- **높은 처리량**: 초당 수천 건의 요청 처리 가능
- **즉시 응답**: 사용자에게 빠른 피드백 제공
- **확장성**: Coupon 애플리케이션 인스턴스 수평 확장으로 처리량 증대
- **내결함성**: 메시지 재처리를 통한 안정성 확보
- **순서 보장**: 파티션 키를 통한 쿠폰별 순서 처리
- **유지보수성**: 쿠폰 관련 모든 코드가 한 모듈에 집중

이러한 아키텍처를 통해 대규모 트래픽 상황에서도 안정적이고 효율적인 쿠폰 발급 서비스를 제공할 수 있습니다.
