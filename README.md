# 대용량 트래픽 이커머스 시스템 🛒

----------------------------------------

## 프로젝트 개발 이슈 관리
- ✅ [프로젝트 타임라인](https://github.com/users/lostcatbox/projects/3)
- ✅ [프로젝트 이슈 칸반](https://github.com/users/lostcatbox/projects/3/views/2)

## 이커머스 서비스 설계 기초 문서
- 📝 [요구사항 분석 문서 작성](docs/Requirements.md)
- 📝 [시퀀스 다이어그램 작성](docs/SequenceDiagrams.md)
- 📝 [상태 다이어그램 작성](docs/StatusDiagrams.md)
- 📝 [도메인 모델 작성](docs/DomainModels.md)
- 📝 [클래스 다이어그램 작성](docs/ClassDiagrams.md)
- 📝 [ERD 설계 및 작성](docs/ERD.md)

## 기술 보고서
🔥🔥🔥 [예상되는 병목 현상 및 해결 방안](docs/study/bottleneck-effect-in-6week-commit.md) 🔥🔥🔥 

- 📋️ **시스템 아키텍처 보고서**
  - [클린 아키텍처와 헥사고날 아키텍처](docs/study/clean-architecture-and-hexagonal-architecture.md)
- 📋️ **DB 성능 최적화 보고서**
  - [인기 판매 상품 성능 개선 (Redis를 DB와 캐시로 활용)](docs/study/product-seller-rank-improve.md)
- 📋 **동시성 이슈 분석 및 해결 보고서**
  - [동시성 이슈 발생 구간 정리 및 해결](docs/study/concurrency-issues-and-solvent.md)
  - [선착순 쿠폰 발급 절차 개선 (분산락 적용)](docs/study/distributed-lock.md)
- 📋️ **Redis 를 사용한 선착순 쿠폰 발급 절차 개선**
  - [선착순 쿠폰 발급 절차 개선 (비동기 처리)](docs/study/coupon-issue-improving(asynchronous).md)
- 📋️ **Event-Driven Architecture 도입 보고서**
  - [OrderFacade를 Event로 분리](docs/study/order-facade-improving-by-event-driven.md)
  - [CouponFacade를 Event로 분리](docs/study/coupon-service-refactoring.md)
- 📋️ **MSA 이벤트 기반 아키텍처 보고서**
  - [Kafka, MSA 기반으로 선착순 쿠폰 발급 시스템 개선 (멀티모듈 도입)](docs/study/improve-coupon-issue-with-kafka.md)

## 테스트 가이드
- 📋 [쿠폰 발급 시스템 통합 테스트 가이드 (Consumer 스케일 아웃 포함)](COUPON_TEST_README.md)
- 📋 [주문, 결제, 포인트, 제품 도메인들의 단위테스트 및 통합 테스트 위치](core/src/test/kotlin/kr/hhplus/be/server/core)
- 📋 [쿠폰 도메인의 단위테스트 및 통합 테스트 위치](coupon/src/test/kotlin/kr/hhplus/be/server/core)


## 참고 자료
- 📚 [트랜잭션 격리 수준(Isolation Level) 보고서](docs/study/transaction-acid-and-isolation-level.md)
- 📚 [Cache (캐시)의 종류와 특징](docs/study/caching.md)
- 📚 [Kafka 기본 개념 보고서](docs/study/kafka-basic-concepts.md)
- 📚 [왜 ApplicationEvent를 사용하는가?](docs/study/application-event.md)
