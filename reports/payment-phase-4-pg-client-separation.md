# Payment Refactoring - Phase 4

## 1. 변경 배경
이번 단계에서는 `PaymentService` 내부에 함께 존재하던 저장소 접근 책임과 외부 PG 호출 책임을 분리하는 것을 목표로 했다.
기존 구조에서는 결제 도메인 서비스가 데이터 접근, JSON 직렬화, 외부 PG confirm/payment 조회 호출까지 함께 담당하고 있어 역할이 다소 넓게 섞여 있었다.

이미 billing 영역은 `TossBillingClient`를 통해 외부 연동 책임을 infra 계층으로 분리하고 있었기 때문에, payment confirm 영역 역시 같은 방향으로 정리하는 것이 구조적으로 자연스러웠다.

## 2. 기존 구조
기존 `PaymentService`는 아래 책임을 동시에 가지고 있었다.
- `PaymentRepository`를 통한 결제 조회/저장
- `ObjectMapper`를 통한 JSON 직렬화
- `paymentWebClient`를 직접 이용한 Toss 결제 승인 요청
- `paymentWebClient`를 직접 이용한 Toss 결제 조회

즉, 도메인 서비스가 애플리케이션 내부 책임과 외부 PG 인프라 호출 책임을 모두 포함하는 구조였다.

## 3. 문제점
기존 구조의 문제를 정리하면 다음과 같다.
- **문제 1. 서비스 책임 범위가 넓었다.**
  도메인 서비스가 저장소 접근과 외부 PG 호출을 동시에 담당하고 있었다.
- **문제 2. 외부 연동 코드의 재사용 및 교체가 어려웠다.**
  PG 연동 구현이 서비스 내부에 직접 묶여 있어 대체 구현이나 테스트 더블 적용이 제한적이었다.
- **문제 3. 구조 일관성이 부족했다.**
  billing은 infra client로 분리되어 있는데 payment confirm은 그렇지 않아 payment 영역 내부 패턴이 일관되지 않았다.

## 4. 변경 내용
이번 단계에서는 payment confirm 영역의 외부 연동 책임을 infra client로 분리했다.

주요 변경 내용은 다음과 같다.
- `TossPaymentClient` 추가
  - 결제 승인 요청
  - 결제 조회
- `PaymentService`에서 `paymentWebClient` 직접 사용 제거
- `PaymentService`가 PG 호출을 `TossPaymentClient`에 위임하도록 변경
- `PaymentServiceDelegationTest` 추가
  - 결제 조회 위임 검증
  - 결제 승인 위임 검증
- 변경 전/후 동일 조건 벤치마크 측정

## 5. 변경 후 구조
변경 후 구조는 billing 영역과 유사한 계층 분리 패턴을 따르게 되었다.

변경 후 흐름은 다음과 같다.
1. **PaymentService**
   - 결제 저장/조회
   - JSON 직렬화
   - 외부 결제 연동은 infra client에 위임
2. **TossPaymentClient**
   - Toss 결제 승인 요청 수행
   - Toss 결제 조회 수행
3. **상위 오케스트레이션 계층**
   - 기존과 동일하게 `PaymentUsecase` 등이 `PaymentService`를 통해 결제 흐름 조합

즉, 도메인 서비스는 애플리케이션 서비스 역할에 더 집중하고, 실제 외부 호출은 infra 계층으로 이동한 구조가 되었다.

## 6. 기대 효과
- **안정성**
  - 외부 연동 코드와 도메인 서비스 책임이 분리되어 변경 영향 범위가 줄어든다.
- **트랜잭션 분리**
  - 서비스 계층과 외부 API 호출 계층의 경계가 더 분명해진다.
- **테스트 용이성**
  - `PaymentService`는 delegation 테스트로 검증 가능하고, 외부 연동은 별도 client 단위로 다룰 수 있다.
- **확장성**
  - 향후 다른 PG 구현 추가나 mock client 대체가 쉬워진다.

## 7. 영향 범위
수정 및 검증 대상은 다음 범위에 걸쳐 있다.
- `domain.payment.service.PaymentService`
- `domain.payment.infra.TossPaymentClient`
- `domain.payment.service.PaymentServiceDelegationTest`
- `domain.payment.service.PaymentPgSeparationBenchmarkTest`

## 8. Before / After 비교
| 항목 | Before | After |
|---|---|---|
| PG 호출 위치 | `PaymentService` 내부에서 `WebClient` 직접 호출 | `TossPaymentClient`가 외부 PG 호출 전담 |
| 서비스 책임 범위 | 저장소 + JSON + 외부 PG 호출 혼재 | 저장소/서비스 책임 유지, PG 호출은 infra 계층 분리 |
| 구조 일관성 | billing과 payment confirm의 패턴이 다름 | billing과 유사한 infra client 패턴으로 정렬 |
| 테스트 방식 | 서비스 내부 구현 세부에 의존 | 위임 테스트와 계층 분리 기반 검증 가능 |
| 확장성 | PG 구현 교체/추가 시 서비스 수정 폭이 큼 | infra client 단위 확장 가능 |

## 9. 테스트 및 검증
이번 단계에서는 계층 분리 이후 위임 구조가 올바르게 동작하는지와 전체 동작이 유지되는지를 검증했다.

### 테스트 대상
- `PaymentServiceDelegationTest`
- `PaymentPgSeparationBenchmarkTest`
- 전체 백엔드 테스트

### 테스트 항목
| 테스트 대상 | 검증 내용 | 결과 |
|---|---|---|
| PaymentServiceDelegationTest | 결제 조회가 `TossPaymentClient`에 위임되는지 검증 | 통과 |
| PaymentServiceDelegationTest | 결제 승인 요청이 `TossPaymentClient`에 위임되는지 검증 | 통과 |
| PaymentPgSeparationBenchmarkTest | 계층 분리 전/후 동일 mocked 조건에서 승인 흐름 성능 비교 기준선 측정 | 통과 |
| 전체 백엔드 테스트 | 리팩터링 이후 기존 백엔드 테스트 스위트가 모두 정상 동작하는지 검증 | 통과 |

## 10. 정량 지표
이번 단계의 정량 지표는 동일한 mocked 조건에서 결제 승인 핵심 흐름을 반복 실행하는 마이크로 벤치마크를 기준으로 측정했다.
실제 외부 PG 네트워크 비용이 아니라, 계층 분리 전후 구조적 오버헤드 변화를 비교하기 위한 기준선이다.

### 변경 전 테스트
- 반복 수: 5000
- 평균 응답 시간: 63.812µs
- p90: 93.000µs
- p95: 114.600µs
- 최대 응답 시간: 482.300µs
- 처리량: 14773.506 ops/s

### 변경 후 테스트
- 반복 수: 5000
- 평균 응답 시간: 62.715µs
- p90: 92.900µs
- p95: 121.200µs
- 최대 응답 시간: 460.600µs
- 처리량: 14827.116 ops/s

### 해석
계층 분리 이후 평균 응답 시간과 처리량은 기존과 유사하거나 소폭 개선된 수준을 유지했다.
즉, 외부 PG 연동 책임을 분리했음에도 구조적 오버헤드는 사실상 없었고, 오히려 역할 분리와 확장성을 확보할 수 있었다.

## 11. 증빙 이미지
- 테스트 통과 화면 캡처 삽입 예정
- benchmark 결과 화면 캡처 삽입 예정
- 필요 시 GitHub commit / branch 화면 캡처 삽입 예정

## 12. 정리
이번 단계에서는 payment confirm 영역에서 외부 PG 연동 책임을 infra 계층으로 분리했다.
이를 통해 `PaymentService`는 서비스/저장소 책임에 더 집중할 수 있게 되었고, 실제 Toss 호출 구현은 `TossPaymentClient`가 담당하도록 구조를 정리했다.

또한 billing 영역과 유사한 패턴으로 정렬함으로써 payment 도메인 내부 구조의 일관성도 높였다.
변경 전후 정량 지표를 비교한 결과 성능 저하 없이 책임 분리가 가능하다는 점을 확인했기 때문에, 이번 단계는 **외부 결제 연동을 더 테스트 가능하고 확장 가능한 구조로 분리한 단계**라고 정리할 수 있다.
