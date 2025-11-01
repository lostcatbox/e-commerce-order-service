# 인기 상품 조회 및 집계 성능 개선 결과 보고서

------
# 관련 보고서
- [인기 상품 조회 및 집계 성능 개선](../study/improve-product-sale-rank.md)

------
## 인기상품 조회 Read 캐시 적용 성능 테스트 결과
### 목적
- 오늘을 포함한 최근 3일 동안의 판매량을 집계해서 가장 많이 팔린 상품 5개를 조회 응답 속도 개선
- 높은 조회 요청 수에 대응하여 DB 부하 최소화 및 응답 속도 향상
- 기존 RDB Only 방식에서 Redis 캐시를 활용한 신규 방식으로 전환 후 성능 개선 결과 분석

### 테스트 조건
- 테스트 환경: Redis 캐시 서버, RDB 서버, 애플리케이션 서버
- 테스트 도구: Apache JMeter
- 테스트 시나리오
  - 요청 대상 : 최근 3일간 판매량 TOP 5 상품을 조회
  - 동시 사용자 수: 5초 동안 1000명 증가 (Ramp-Up Period: 5초)
  - 총 요청 수: 5000개

### 결과
#### 테스트 결과 사진
![인기 상품 조회의 Read 캐시 적용 성능테스트 결과.png](image/%EC%9D%B8%EA%B8%B0%20%EC%83%81%ED%92%88%20%EC%A1%B0%ED%9A%8C%EC%9D%98%20Read%20%EC%BA%90%EC%8B%9C%20%EC%A0%81%EC%9A%A9%20%EC%84%B1%EB%8A%A5%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EA%B2%B0%EA%B3%BC.png)
#### 결과 요약
- **응답 시간**: 평균 응답 시간이 3214ms에서 5ms로 약 600배 이상 향상
- **DB 부하 감소**: RDB 조회 요청이 99% 이상 감소하여
- DB 서버의 CPU 및 메모리 사용량이 크게 감소

------------
## 인기 상품 집계 시 Write-Back 캐시 적용 성능 테스트 결과
### 목적
- 판매량 집계 시 RDB 부하 최소화 및 응답 속도 향상
- 기존 RDB Only 방식에서 Redis 캐시를 활용하여 Write-Back 전략으로 전환 후 성능 개선 결과 분석
- 판매 발생 시점에 Redis에 데이터를 기록하고, 주기적으로 RDB에 동기화하는 방식으로 DB 부하 감소

### 테스트 조건
- 테스트 환경: Redis 캐시 서버, RDB 서버, 애플리케이션 서버
- 테스트 도구: Apache JMeter
- 테스트 시나리오
  - 요청 대상 : 주문 완료 시 판매량 기록 (OrderCompletedEvent 발생 시점과 동일)
  - 요청 데이터 : 주문 요청 한 건당 상품1, 상품2에 대해 각각 1개의 판매량 기록
  - 동시 사용자 수: 5초 동안 1000명 증가 (Ramp-Up Period: 5초)
  - 총 요청 수: 5000개

### 결과
#### 테스트 결과 사진
- JMeter Summary Report
![인기 상품 통계 write 캐시 적용 성능 테스트 결과.png](image/%EC%9D%B8%EA%B8%B0%20%EC%83%81%ED%92%88%20%ED%86%B5%EA%B3%84%20write%20%EC%BA%90%EC%8B%9C%20%EC%A0%81%EC%9A%A9%20%EC%84%B1%EB%8A%A5%20%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EA%B2%B0%EA%B3%BC.png)
- Redis에서 판매량 집계 결과
![인기 상품 통계 write 캐시 적용 성능 테스트 결과2.png](image/%EC%9D%B8%EA%B8%B0%20%EC%83%81%ED%92%88%20%ED%86%B5%EA%B3%84%20write%20%EC%BA%90%EC%8B%9C%20%EC%A0%81%EC%9A%A9%20%EC%84%B1%EB%8A%A5%20%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EA%B2%B0%EA%B3%BC2.png)

#### 결과 요약
- 상품 판매량 통계 시, 정확히 1000개 판매량 기록됨(동시성 이슈 없음)
- **응답 시간**: 평균 응답 시간이 1400ms에서 13ms로 약 100배 이상 향상
- **DB 부하 감소**: RDB 쓰기 요청이 99% 이상 감소
- DB 서버의 CPU 및 메모리 사용량이 크게 감소


------
# 참고사항
## JMeter Summary Report 항목 설명:
- Summary Report
    - Label : Sampler 명
    - Samples : 샘플 실행 수 (Number of Threads X Ramp-up period)
    - Average : 평균 걸린 시간 (ms)
    - Min : 최소
    - Max : 최대
    - Std. Dev. : 표준편차
    - Error % : 에러율
    - Throughput : 초당 처리량 (bps) = JMeter에서는 시간 단위를 보통  TPS (Transaction Per Second)로 표현
