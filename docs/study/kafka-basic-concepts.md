# Apache Kafka 기초 개념 및 학습 가이드

## 1. Kafka란 무엇인가?

Apache Kafka는 **대규모 실시간 데이터 스트리밍을 위한 분산 메시징 시스템**입니다.

### 핵심 특징
- **고성능**: 초당 수백만 개의 메시지 처리 가능
- **고가용성**: 분산 아키텍처로 단일 장애점 없음
- **확장성**: 수평적 확장 지원
- **영속성**: 메시지를 디스크에 저장
- **순서 보장**: 파티션 내에서 메시지 순서 보장

## 2. Kafka 구성 요소

### 2.1 Broker
- **정의**: Kafka 서버의 기본 단위
- **구성**: 카프카는 클러스터 -> 브로커 -> 토픽 -> 파티션 -> 세그먼트로 구성
- **역할**:
  - Producer : 메시지를 받아서 저장
    - topic, partition, offset 관리
  - Consumer : 메시지를 읽고 메시지 소비
    - offset 기반 메시지 제공
  - Broker : 메시지 전달 및 저장 (Kafka 시스템을 구성하는 개별 서버)
    - Broker 관련 개념
      - **Controller**: 다른 브로커를 모니터링하고 장애 발생 시 Leader 파티션 재분배
      - **Coordinator**: 컨슈머 그룹을 모니터링하고 Rebalancing 수행
      - **Cluster**: 여러 Broker가 모여 하나의 Kafka 클러스터를 형성

### 2.2 Topic & Partition
```
Topic: order_created
├── Partition 0: [msg1, msg2, msg3, msg4]
├── Partition 1: [msg5, msg6, msg7, msg8]
└── Partition 2: [msg9, msg10, msg11, msg12]
```

- **Topic**: 메시지를 분류하는 논리적 단위 (데이터베이스의 테이블과 유사)
- **Partition**: Topic을 구성하는 물리적 저장 단위
  - **순차 처리**: **파티션 내에서는 발행된 순서대로 소비됨**
  - **병렬 처리**: 파티션 개수만큼 병렬 처리 가능
  - **키 기반 분산**: **메시지 키의 해시값으로 파티션 결정** (Producer에서 결정)

### 2.3 Producer
- **역할**: 메시지를 Kafka 브로커에 발행하는 클라이언트
- **파티셔닝**: 메시지 키를 기반으로 파티션 결정
- **메시지 구조**: `<Key, Value>` 형태

### 2.4 Consumer & Consumer Group
- **Consumer**: 카프카 브로커에서 메시지를 읽어오는 클라이언트
- **Consumer Group**: 하나의 토픽을 여러 서비스가 소비할 수 있도록 하는 논리적 그룹

#### Offset 관리
- **Current Offset**: 컨슈머가 마지막으로 처리한 메시지의 위치
- **Commit**: 처리 완료된 offset을 브로커에 저장하는 작업
- **Auto Commit vs Manual Commit**: 자동/수동 커밋 전략

#### Consumer Group과 Partition의 관계
**- 최대 컨슈머 수 = 파티션 수**
```
Topic: order_events (3 partitions)
Consumer Group: order-service-group

Case 1: 컨슈머 1개
Consumer-1 ← Partition-0, Partition-1, Partition-2

Case 2: 컨슈머 2개
Consumer-1 ← Partition-0, Partition-1
Consumer-2 ← Partition-2

Case 3: 컨슈머 3개
Consumer-1 ← Partition-0
Consumer-2 ← Partition-1
Consumer-3 ← Partition-2

Case 4: 컨슈머 4개 (비효율)
Consumer-1 ← Partition-0
Consumer-2 ← Partition-1
Consumer-3 ← Partition-2
Consumer-4 ← (할당된 파티션 없음)
```

### 2.5 Rebalancing
- **정의**: 컨슈머 그룹 내에서 파티션 소유권을 재분배하는 과정
- **발생 조건**:
  1. Consumer Group 내에 새로운 Consumer 추가
  2. 기존 Consumer 장애로 소비 중단
  3. Topic에 새로운 Partition 추가
- **주의사항**: 리밸런싱 중에는 메시지 소비가 중단됨

### 2.6 Cluster & Replication
#### Cluster
- **목적**: 고가용성(HA)을 위해 여러 브로커를 묶어 운영
- **장점**: 브로커 증가 시 처리량 분산 및 확장성 향상

#### Replication
- **Leader Replica**: 모든 읽기/쓰기 요청을 처리하는 주 복제본
- **Follower Replica**: Leader의 데이터를 복제하는 백업 복제본
- **ISR (In-Sync Replica)**: Leader와 동기화된 Replica들

## 3. 왜 대용량 시스템에서 Kafka를 사용하는가?

### 3.1 높은 처리량 (High Throughput)
- **순차 I/O**: 디스크의 순차 쓰기로 빠른 성능
- **배치 처리**: 여러 메시지를 묶어서 처리

### 3.2 확장성 (Scalability)
- **수평적 확장**: 브로커/파티션 추가로 처리량 증가
- **병렬 처리**: 파티션 단위로 병렬 소비 가능

### 3.3 고가용성 (High Availability)
- **분산 아키텍처**: 단일 장애점 없음
- **복제**: 데이터 손실 방지
- **자동 장애 복구**: Controller의 자동 리더 선출

## 4. 메시지 설계 전략

### 4.1 Zero-Payload vs Full-Payload
- **Zero-Payload**: 메시지에 최소한의 정보(예: ID)만 포함
  - 장점: 메시지 크기 작음, 네트워크 부담 감소
  - 단점: 추가 조회 필요, 복잡성 증가
- **Full-Payload**: 메시지에 필요한 모든 정보 포함
  - 장점: 추가 조회 불필요, 단순 처리
  - 단점: 메시지 크기 큼, 네트워크 부담 증가

```kotlin
// Zero-Payload: ID만 전송
data class OrderCompletedEvent(
    val orderId: Long,
    val timestamp: Instant
)

// Full-Payload: 필요한 모든 정보 포함
data class OrderCompletedEvent(
    val orderId: Long,
    val userId: Long,
    val orderItems: List<OrderItem>,
    val totalAmount: Long,
    val timestamp: Instant
)
```

### 4.2 토픽 네이밍 컨벤션
```
{domain}.{action}.{version}
예: order.created.v1, payment.succeeded.v1
```

### 4.3 컨슈머 그룹 네이밍
```
{service-name}-{purpose}
예: data-platform-service, notification-service
```



## 5. 현재 Event-Driven 아키텍처의 한계점

### 5.1 현재 구조 분석
```kotlin
// 현재 애플리케이션 이벤트 기반 구조
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handlePaymentSucceeded(event: PaymentSucceededEvent) {
    // 외부 시스템 연동 (데이터 플랫폼, 알림 서비스)
    externalApiClient.sendOrderInfo(event)
    notificationService.sendNotification(event)
}
```

### 5.2 기존 구조의 문제점
1. **외부 시스템 의존성**: 외부 API 장애 시 전체 프로세스 영향
2. **재시도 복잡성**: 실패 시 재처리 로직의 복잡성
3. **단일 장애점**: 애플리케이션 서버 장애 시 이벤트 유실 가능
4. **확장성 제한**: 동일 서버 내에서만 이벤트 처리 가능

### 5.3 Kafka를 통한 개선 방향
1. **책임 분리**: 메시지 발행과 소비의 완전한 분리
2. **내구성**: 메시지의 영속적 저장으로 유실 방지
3. **확장성**: 독립적인 컨슈머 확장 가능
4. **고가용성**: 분산 처리로 단일 장애점 제거

## 6. Kafka 기반 시스템 설계 예시

### 6.1 현재 → Kafka 전환
```kotlin
// Before: 애플리케이션 이벤트
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleOrderCompleted(event: OrderCompletedEvent) {
    externalDataPlatform.sendOrderData(event)  // 장애 위험
    notificationService.sendNotification(event) // 장애 위험
}

// After: Kafka 기반
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleOrderCompleted(event: OrderCompletedEvent) {
    kafkaTemplate.send("order-completed", event) // 카프카에 발행만
}

// 별도 컨슈머에서 처리
@KafkaListener(topics = ["order-completed"], groupId = "data-platform-service")
fun processOrderData(event: OrderCompletedEvent) {
    // 데이터 플랫폼 처리
}

@KafkaListener(topics = ["order-completed"], groupId = "notification-service")
fun sendNotification(event: OrderCompletedEvent) {
    // 알림 처리
}
```

### 6.2 고가용성과 유연함이 필요한 부분
1. **주문/결제 완료 후 외부 시스템 연동**
    - 데이터 플랫폼으로의 주문 정보 전송
    - 알림톡/푸시 알림 발송
    - 배송 시스템 연동

2. **대용량 트래픽 처리**
    - 선착순 쿠폰 발급
    - 콘서트 예약 대기열 처리

3. **마이크로서비스 간 통신**
    - 도메인 간 이벤트 전파
    - 분산 트랜잭션 관리
