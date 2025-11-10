# 분산락 트랜잭션 순서 문제 해결 보고서
## 목차
- 문제점
- 해결방법
- 결론

## 문제점

### 기존 코드의 트랜잭션 순서 문제

**기존 코드:**

```kotlin
// ❌ 기존 코드
@Transactional  // TX1 시작
override fun issueCoupon(request: CouponIssueRequest): UserCoupon {
    validateCouponId(request.couponId)

    val lockKey = "lock:coupon-issue:${request.couponId}"
    distributedLockManager.executeWithLock(lockKey) {  // TX2 시작
        // 쿠폰 재고 차감
        coupon.issueCoupon()
        couponRepository.save(coupon)
        // TX2 커밋 ✓
    } // 락 해제 ✓

    //  ❌ 문제: TX1은 아직 커밋 안됨, 락은 이미 해제됨!
    val userCoupon = userCouponService.createUserCoupon(request.userId, request.couponId)
    return userCoupon
    // TX1 커밋
}
```

### 잘못된 실행 순서

```
TX1 시작 → 락 획득 → TX2 시작 → 비즈니스 로직 → TX2 커밋 → 락 해제 → TX1 커밋
                                                            ↑
                                              문제 발생 지점: 락은 해제되었지만 TX1은 아직 진행 중
```

### 발생 가능한 문제

1. **데이터 정합성 위험**: 분산락이 해제된 후에도 외부 트랜잭션(TX1)이 진행 중
2. **Dirty Read 가능성**: 다른 스레드가 락을 획득하면 아직 커밋되지 않은 데이터를 조회할 수 있음
3. **동시성 제어 실패**: 분산락의 목적이 무용지물

---

## 해결방법

### 핵심 원칙

분산락 사용 시 반드시 지켜야 하는 순서:

```
락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제
```

### 해결 전략

`DistributedLockManager.executeWithLock()`는 내부적으로 `TransactionTemplate`을 사용하여 트랜잭션을 관리합니다.
따라서 이 트랜잭션만 사용하도록 코드를 수정했습니다.

**수정 내용:**

1. `CouponService.issueCoupon()`의 `@Transactional` 어노테이션 **제거**
2. `userCouponService.createUserCoupon()` 호출을 `executeWithLock { }` **내부로 이동**
3. `UserCouponService.createUserCoupon()`의 `@Transactional(REQUIRED)`가 기존 트랜잭션에 참여하도록 구성

---

## 결과

### 최종 코드

```kotlin
// ✅ 수정된 코드
// @Transactional 제거
override fun issueCoupon(request: CouponIssueRequest): UserCoupon {
    try {
        validateCouponId(request.couponId)

        val lockKey = "lock:coupon-issue:${request.couponId}"

        // 분산락 내에서 모든 로직 처리
        return distributedLockManager.executeWithLock(lockKey) {  // TX 시작
            // 1. 쿠폰 재고 차감
            val coupon = couponRepository.findByCouponIdWithPessimisticLock(request.couponId)
                ?: throw IllegalArgumentException("존재하지 않는 쿠폰입니다. 쿠폰 ID: ${request.couponId}")

            coupon.issueCoupon()
            couponRepository.save(coupon)

            // 2. 사용자 쿠폰 생성 (같은 트랜잭션 내에서)
            userCouponService.createUserCoupon(request.userId, request.couponId)
            // TX 커밋 ✓
        } // 락 해제 ✓
    } catch (e: Exception) {
        throw e
    }
}
```

### 올바른 실행 순서

```
락 획득 → 트랜잭션 시작 → 쿠폰 재고 차감 → UserCoupon 생성 → 트랜잭션 커밋 → 락 해제
```

### 개선 효과

- ✅ **올바른 순서 보장**: 락 → 트랜잭션 → 비즈니스로직 → 커밋 → 락 해제
- ✅ **단일 트랜잭션**: 모든 DB 작업이 하나의 트랜잭션에서 실행되어 원자성 보장
- ✅ **데이터 정합성**: 완벽한 데이터 정합성 보장

---

## 📊 Before / After 비교

| 구분 | 기존 코드 | 개선 코드 |
|------|----------|----------|
| **외부 @Transactional** | ❌ 있음 (문제) | ✅ 제거 |
| **트랜잭션 개수** | 2개 (TX1, TX2) | 1개 (TX) |
| **락 해제 시점** | TX1 커밋 전 | TX 커밋 후 |
| **실행 순서** | 락 → TX2 → 락해제 → TX1 | 락 → TX → 락해제 |
| **데이터 정합성** | ⚠️ 위험 | ✅ 안전 |
| **UserCoupon 생성 위치** | 락 외부 | 락 내부 |

---

## 🎯 결론

분산락을 사용할 때는 **락 내부에서만 트랜잭션이 생성되고 커밋되도록** 보장해야 합니다.
외부에 `@Transactional`이 있으면 트랜잭션이 락 범위를 벗어나 데이터 정합성 문제가 발생할 수 있습니다.

**핵심 원칙**: 락 획득 → 트랜잭션 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제

