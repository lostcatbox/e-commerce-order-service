# 선착순 쿠폰 발급 절차 개선 (비동기 처리)

# 기존 선착순 쿠폰 발급 절차
## 로직
1. 발급 요청 즉시 동기적으로 쿠폰 발급 처리 및 완료 응답
## 단점
 - 동시 다발적인 발급 요청이 들어올 경우 DB에 과부하가 걸릴 수 있음
 - 발급 요청에 대한 응답이 느림

# 신규 선착순 쿠폰 발급 절차 (비동기 처리)
## 로직
1. 발급 요청에 대해 발급 요청 성공으로 즉시 응답완료한다.
    - 동시에 많은 발급 요청은 선착순으로 messageQ 에 넣어 놓기만 하고 빠른 응답 처리
2. 발급 요청에 대한 쿠폰 발급 처리는 비동기적으로 처리한다.
    - 실제 쿠폰 발급 처리는 messageQ에 쌓여있는 쿠폰 발급 요청들을 적절한 양만큼씩 처리하여, DB가 장애가 일어나지 않도록 한다.

## Redis를 이용한 대기열 사용 정의
- redis의 List 자료구조를 활용해서, messageQ로 활용
  - 대기열 추가시 LPUSH, 제거시 RPOP 이용
  - 대기열 키 : "coupon_issue_queue"
  - 대기열 값 : JSON 문자열
    - couponIssueRequestId
        - userId
        - couponId
        - timestamp



## 시퀀스 다이어그램
```mermaid

sequenceDiagram
    participant Client as 클라이언트
    participant NewFacade as CouponAsyncFacade
    participant Redis as Redis 대기열
    participant Scheduler as 쿠폰발급스케줄러
    participant CouponService as CouponService
    participant UserCouponService as UserCouponService
    participant DB as Database

    Note over Client, DB: 기존 시스템 (동기)
    Client->>NewFacade: 쿠폰 발급 요청
    NewFacade->>DB: 쿠폰 재고량 확인
    alt 재고 있음
        NewFacade->>Redis: 대기열에 추가 (LPUSH)
        NewFacade->>Client: 즉시 응답 (발급 요청 성공)
    else 재고 없음
        NewFacade->>Client: 재고 부족 응답
    end

    Note over Redis, DB: 비동기 처리 (1초마다)
    loop 1초마다 실행
        Scheduler->>Redis: 대기열에서 1개 조회 (RPOP)
        alt 대기열에 요청 있음
            Scheduler->>UserCouponService: 중복 발급 확인
            alt 중복 아님
                Scheduler->>CouponService: 쿠폰 재고 차감
                Scheduler->>UserCouponService: UserCoupon 생성
                Scheduler->>DB: 데이터 저장
            else 중복 발급
                Note over Scheduler: 해당 요청 무시
            end
        end
    end
```
