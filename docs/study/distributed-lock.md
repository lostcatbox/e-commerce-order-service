# 분산락 사용 이유
- DB 트랜잭션 이상의 범위, 분산 환경에서 Lock 을 적용할 수 있는 방법
- 다량의 트래픽을 처리하기 위해 적은 DB 부하로 올바르게 기능을 제공할 방법

# 분산락 이란?
- 여러 프로세스, 서버, 인스턴스에 걸쳐 자원에 대한 동시 접근을 제어하는 메커니즘
- 분산 시스템에서 데이터 일관성과 무결성을 유지하는 데 중요
- 예시: Redis 등을 활용한 분산
  - Redis 기반의 분산락
  - key-value 기반의 원자성을 이용한 Redis 를 통해 DB 부하를 최소화하는 Lock 을 설계

# 분산락 사용 시 고려사항
- Redis 락과 트랜잭션 동시 사용 시 주의
  - Redis 락은 DB 트랜잭션과 별개로 동작
  - 반드시 **락 획득 -> 트랜잭션 시작 -> 비즈니스로직 수행 -> 트랜잭션 종료 -> 락 해제 순서로 진행**
    - 만약 **트랜잭션 시작**이 **락 획득**보다 먼저라면, 다른 트랜잭션의 데이터 조회 + 수정이 가능해져서 동시성 이슈 발생
    - 만약 **트랜잭션 종료**가 **락 해제**보다 나중이라면, 락이 해제된 후 바로 다른 프로세스가 락을 획득하여 데이터 변경이 가능해져서 동시성 이슈 발생

## Redis 분산락과 트랜잭션 순서별 케이스 다이어그램

### (정상 유형) 올바른 락과 트랜잭션 순서
락 획득 → 트랜잭션 시작 → 비즈니스로직 수행 → 트랜잭션 종료 → 락 해제

```mermaid
sequenceDiagram
    participant A as Transaction A
    participant Redis as Redis
    participant DB as Database
    participant B as Transaction B

    Note over A,B: 올바른 순서: 락 획득 → 트랜잭션 시작 → 비즈니스로직 → 트랜잭션 종료 → 락 해제

    A->>Redis: 1. 락 획득 시도
    Redis-->>A: 락 획득 성공

    B->>Redis: 1. 락 획득 시도
    Redis-->>B: 락 획득 실패 (대기)

    A->>DB: 2. 트랜잭션 시작 (BEGIN)
    A->>DB: 3. 데이터 조회 (SELECT)
    DB-->>A: 데이터 반환
    A->>DB: 4. 비즈니스로직 수행 (UPDATE/INSERT)
    A->>DB: 5. 트랜잭션 커밋 (COMMIT)
    DB-->>A: 커밋 완료

    A->>Redis: 6. 락 해제
    Redis-->>A: 락 해제 완료

    B->>Redis: 락 획득 재시도
    Redis-->>B: 락 획득 성공

    B->>DB: 트랜잭션 시작 (BEGIN)
    B->>DB: 데이터 조회 (최신 데이터)
    DB-->>B: 업데이트된 데이터 반환
    B->>DB: 비즈니스로직 수행
    B->>DB: 트랜잭션 커밋 (COMMIT)
    B->>Redis: 락 해제
```

### (문제 발생) 트랜잭션 시작이 락 획득보다 먼저
트랜잭션 시작 → 락 획득 → 비즈니스로직 수행 → 트랜잭션 종료 → 락 해제

```mermaid
sequenceDiagram
    participant A as Transaction A
    participant Redis as Redis
    participant DB as Database
    participant B as Transaction B

    Note over A,B: ❌ 잘못된 순서: 트랜잭션 시작 → 락 획득 (동시성 이슈 발생)

    A->>DB: 1. 트랜잭션 시작 (BEGIN) - 락 없이!
    B->>DB: 1. 트랜잭션 시작 (BEGIN) - 락 없이!

    A->>DB: 2. 데이터 조회 (SELECT balance = 1000)
    B->>DB: 2. 데이터 조회 (SELECT balance = 1000) - 같은 값!

    A->>Redis: 3. 락 획득 시도
    Redis-->>A: 락 획득 성공

    B->>Redis: 3. 락 획득 시도
    Redis-->>B: 락 획득 실패 (대기)

    A->>DB: 4. 비즈니스로직 (balance + 500 = 1500)
    A->>DB: 5. 트랜잭션 커밋 (COMMIT)
    DB-->>A: 커밋 완료 (balance = 1500)

    A->>Redis: 6. 락 해제

    B->>Redis: 락 획득 재시도
    Redis-->>B: 락 획득 성공

    Note over B: ⚠️ B는 이미 조회한 잘못된 데이터(1000)로 로직 수행
    B->>DB: 비즈니스로직 (1000 + 300 = 1300) - 잘못된 계산!
    B->>DB: 트랜잭션 커밋 (COMMIT)
    DB-->>B: 커밋 완료 (balance = 1300)

    Note over A,B: 💥 결과: 1000 + 500 + 300 = 1800이어야 하지만 1300이 됨
```

### (문제 발생) 트랜잭션 종료가 락 해제보다 나중
락 획득 → 트랜잭션 시작 → 비즈니스로직 수행 → 락 해제 → 트랜잭션 종료

```mermaid
sequenceDiagram
    participant A as Transaction A
    participant Redis as Redis
    participant DB as Database
    participant B as Transaction B

    Note over A,B: ❌ 잘못된 순서: 락 해제 → 트랜잭션 종료 (동시성 이슈 발생)

    A->>Redis: 1. 락 획득
    Redis-->>A: 락 획득 성공

    B->>Redis: 1. 락 획득 시도
    Redis-->>B: 락 획득 실패 (대기)

    A->>DB: 2. 트랜잭션 시작 (BEGIN)
    A->>DB: 3. 데이터 조회 (SELECT balance = 1000)
    A->>DB: 4. 비즈니스로직 수행 (balance + 500)

    Note over A: ⚠️ 트랜잭션은 아직 커밋되지 않음
    A->>Redis: 5. 락 해제 (너무 이른 해제!)
    Redis-->>A: 락 해제 완료

    B->>Redis: 락 획득 재시도
    Redis-->>B: 락 획득 성공

    B->>DB: 트랜잭션 시작 (BEGIN)
    B->>DB: 데이터 조회 (SELECT balance = 1000) - A의 변경사항 미반영!

    A->>DB: 6. 트랜잭션 커밋 (COMMIT) - 늦은 커밋
    DB-->>A: 커밋 완료 (balance = 1500)

    B->>DB: 비즈니스로직 (1000 + 300 = 1300)
    B->>DB: 트랜잭션 커밋 (COMMIT)
    DB-->>B: 커밋 완료 (balance = 1300) - A의 변경사항 덮어씀!

    B->>Redis: 락 해제

    Note over A,B: 💥 결과: A의 +500 변경사항이 유실됨 (Lost Update)
```
