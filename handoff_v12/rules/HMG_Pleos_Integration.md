# 현대 공모전과 Pleos 연동 기준

확인일 2026.11.10. 공개 문서와 공모전 참가 가이드를 바탕으로 작성했다. 접근 승인, 동작, 배포 적합성은 대상 에뮬레이터 및 차종에서 확인한다.

## 차량 수집 정보와 API

| 수집 정보 또는 기능 | 공식 경로 | 계획과 한계 |
| --- | --- | --- |
| 차량 음성 | Pleos SpeechToText, `ResultListener.onUpdated(stt, completed)`, `pleos.car.permission.STT_SERVICE` | 최종 발화로 의도 분기. 중간값은 표시용. 활성화 시점·차량 권한을 검증. |
| 차량 위치 | Pleos FusedLocation `isSupported`, `getLastKnownLocation`, 위치 콜백, `pleos.car.permission.FUSED_LOCATION` | 동의된 사건에서 좌표·갱신 시각·정확도 조회. 제공 여부와 위치 차단 처리. 주소 변환은 별도 구현. |
| 주차 브레이크·페달 | Vehicle SDK `Vehicle.getBrake()`, 도메인 capability, `pleos.car.permission.CAR_DRIVE` | 읽기만 사용. 기어·속도는 지원되는 AAOS 속성으로 별도 조회. |
| 속도·기어 | AAOS `CarPropertyManager`, `PERF_VEHICLE_SPEED`, `GEAR_SELECTION` | 대상 차량 속성과 읽기 권한이 확인된 경우 사건 시점만 읽는다. m/s는 UI 표시 시 km/h로 환산한다. |
| 차량 음성 안내 | Pleos TextToSpeech, `pleos.car.permission.TTS_SERVICE` | 차량 스피커 재생. 긴급 통화 송화와 분리한다. |
| 화면과 조작 제한 | AAOS `CarUxRestrictions`, Pleos 앱 카테고리·DO 정책 | 주행 규제는 플랫폼이 결정한다. 주행 중 앱 사용을 보장하지 않는다. |
| 시스템 긴급전화 | Android Telecom 및 제조사 승인 공식 SOS | 일반 앱은 시스템 긴급전화 경로를 사용한다. 회신 미확인 시 연결됨으로 표시하지 않는다. 시연은 실제 119를 호출하지 않는다. |
| 충돌·에어백·정차·비상등 | 제조사 공식 안전 기능과 대상 차종 승인 신호 | 특정 신호·제어의 앱 접근은 검증 전이다. 도메인 이름이나 블루링크 서비스 안내만으로 API를 가정하지 않는다. |
| NaviHelper 및 원격 Vehicle Data API | 별도 Pleos SDK / OAuth 기반 REST API | NaviHelper는 추후 내비게이션 연계 검토. 원격 REST API를 국내 인카 MVP의 실시간 입력으로 채택하지 않는다. |

## 개발 환경과 검증

- Android Studio, Kotlin, AAOS 기반 Pleos Connect SDK. 공식 최소 Android API 28·Gradle 8.0. Emulator 및 Virtual Console 가이드 기준 2.1.0. SDK·서비스·시스템 이미지의 실제 조합을 확인하고 README에 버전을 고정한다.
- Pleos Playground Workspace → My Project에서 프로젝트를 만들고 각 SDK의 사용 가능·신청 필요·제휴 필요 상태를 확인한다. 공식 예제 AVD는 API 34의 Pleos Connect v2.1.0 이미지다.
- 차량 앱은 ViewModel·StateFlow와 Room/SQLite를 사용한다는 팀 제안이다. SDK 입력과 테스트 이벤트를 별도 어댑터로 연결한다. 시연 전용 가상 입력은 출처를 표시한다.
- 차량 도메인 `checkXXXCapabilities`, 위치 `isSupported`, AAOS 속성 구성·권한 확인. 승인 범위 안에서 필요한 권한만 선언한다. 차종 이름으로 지원 여부를 추정하지 않는다.
- 주행 상태의 입력·애니메이션·편집 제한, 권한 거부, 위치 차단, 오프라인 기록, 안정적인 앱 실행을 시험한다. 배포 검토 때 개인·위치 데이터의 처리방침과 권한 사전 협의를 준비한다.

## 제출물 구분

| 단계 | 공식 요구사항과 준비 |
| --- | --- |
| 1차 접수 | 2026.11.15 23:59까지 공식 접수양식 작성. 팀 참가 시 대표·팀원 전원의 개별 동의·서명·날짜와 이메일 CC 확인. 작품 소개에는 성명·소속·연락처를 넣지 않는다. |
| 1차 선택 산출물 | 목업·프로토타입·화면 녹화 시연 영상, 소스와 release APK가 담긴 GitHub 저장소는 선택이며 가산점 대상. APK 단독 첨부 대신 저장소 `apk/` 또는 Release 경로를 제공한다. |
| GitHub 제출 시 | `팀명_앱이름_release.apk`, README에 이름·한 줄 소개·실행 방법·테스트 환경 및 설정·APK 경로. 비공개 저장소는 공식 심사용 계정 접근을 확인한다. |
| 2차 PT | 대상팀은 12.03~12.04 심사 전 실제 동작 앱을 완성해 에뮬레이터 또는 기기로 시연한다. |
| Pleos App Market | 주행 주의 분산, 권한, 위치·개인정보, 기능 안정성을 별도 검증. 공모전 접수와 App Market 배포 승인은 다른 절차다. |

## 출품 전에 확인할 권리와 공개 범위

- 화면 PNG의 차량 이미지·아이콘, 영상의 음성·이미지·음원은 사용 허락 범위를 확인한다. 허락이 확인되지 않은 참고 이미지는 팀 제작 자산으로 교체한 뒤 출품한다.
- 시연 영상과 심사용 저장소는 공식 양식의 익명 심사 기준에 따라 팀원 실명·연락처·계정 정보를 제거하고, 시연용 좌표와 연락처만 사용한다.

## 공식 출처

- [현대 공모전 지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission) · [심사·시상](https://appdevchallenge.auto-hmg.io/guide/judging-awards) · [FAQ](https://appdevchallenge.auto-hmg.io/faq) · [출품 유의사항](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000006)
- [Pleos 프로젝트·SDK 신청](https://document.pleos.ai/docs/pleos-only/create-my-project) · [Pleos 개발 환경](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) · [Vehicle SDK](https://document.pleos.ai/api-reference/connect-sdk-pleos/Vehicle/intro) · [Vehicle 권한](https://document.pleos.ai/api-reference/connect-sdk-pleos/Vehicle/vehicle-permission)
- [STT 리스너](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/SpeechToText/result-listeners) · [TTS](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro) · [FusedLocation 권한](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions)
- [API 가용성](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability) · [주행 규제](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline) · [App Market 심사 가이드](https://document.pleos.ai/en/docs/pleos-only/distribute/app-review-guide)
- [AAOS VehiclePropertyIds](https://developer.android.com/reference/android/car/VehiclePropertyIds) · [Android Telecom](https://developer.android.com/develop/connectivity/telecom/dialer-app) · [원격 Vehicle Data API 지원 지역](https://document.pleos.ai/en/api-reference/vehicle-data-api/intro)
