# Event-Driven에서 @EventListener vs @TransactionalEventListener 사용 규칙

## 1️⃣ @TransactionalEventListener + @Async
### 해당 조합 사용 시 유의할 점
- @TransactionalEventListener는 @Transactional과 함께 사용 금지
    - 이유 : @TransactionalEventListener는 이미 트랜잭션 경계 내에서 호출되므로 중복
- @Async와 함께 사용하여 독립 트랜잭션 보장

```kotlin
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleBusinessEvent(event: DomainEvent) {
    // 비즈니스 로직 처리
}
```

### TransactionPhase 옵션 선택 기준
| Phase              | 실행 시점      | 특징                                                | 사용 사례                                 |
|:-------------------|:-----------|:--------------------------------------------------|:--------------------------------------|
| `BEFORE_COMMIT`    | 트랜잭션 커밋 직전 | - 메인 트랜잭션과 같은 트랜잭션<br>- 이벤트 처리 실패 시 전체 롤백         | - 필수적인 데이터 일관성 유지<br>- 동기적 처리가 필요한 경우 |
| `AFTER_COMMIT`     | 트랜잭션 커밋 후  | - 새로운 트랜잭션<br>- 메인 로직과 분리<br>- 실패해도 메인 트랜잭션 영향 없음 | - 외부 시스템 연동<br>- 통계 데이터 처리<br>- 알림 발송 |
| `AFTER_ROLLBACK`   | 트랜잭션 롤백 후  | - 메인 트랜잭션 실패 시에만 실행                               | - 실패 알림<br>- 로깅<br>- 모니터링             |
| `AFTER_COMPLETION` | 트랜잭션 완료 후  | - 성공/실패 관계없이 실행                                   | - 리소스 정리<br>- 감사 로그                   |


## 2️⃣ @EventListener
### 해당 조합 사용 시 유의할 점
- 즉시 동기적 처리가 필요한 경우만 사용
```kotlin
@EventListener
fun handleImmediateAction(event: DomainEvent) {
}
```

# 참고사항 : Transaction Propagation
- 물리적 트랜잭션과 논리적 트랜잭션을 다룰 수 있는 다양한 전파 옵션 제공

```kotlin
enum class Propagation {
    REQUIRED,        // 트랜잭션이 있으면 참여, 없으면 새로 생성 (기본값)
    SUPPORTS,        // 트랜잭션이 있으면 참여, 없으면 트랜잭션 없이 실행
    MANDATORY,       // 반드시 트랜잭션 내에서 실행, 없으면 예외
    REQUIRES_NEW,    // 항상 새로운 트랜잭션 생성 (기존 트랜잭션 일시 중단)
    NOT_SUPPORTED,   // 트랜잭션 없이 실행 (기존 트랜잭션 일시 중단)
    NEVER,          // 트랜잭션 없이 실행, 트랜잭션이 있으면 예외
    NESTED          // 중첩 트랜잭션 (savepoint 사용)(기존 트랜잭션 있다면, savepoint 생성)(없다면 새로 생성)
}
```
