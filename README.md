# DriveCheck · 차량 위기 대응 및 사고 기록 서비스 (v12)

현대자동차그룹(HMG) **Pleos App Market** 공모전 출품용 Android Automotive OS / Pleos Connect SDK 대응 애플리케이션 프로젝트입니다.

---

## 1. 프로젝트 개요 및 설계 원칙

**DriveCheck**는 주행 중 운전자의 급성 의료 위기, 사고 의심 상황, 혹은 동승자(아이 등)의 응급 도움 요청이 발생했을 때 **음성 및 큰 터치 버튼 중심의 단순한 인터페이스**로 위기 여부를 신속히 확인하고, **119 신고 연계·위치 브리핑·사건 타임라인 기록·PDF 내보내기**까지 일관되게 지원하는 차량용 안전 보조 서비스입니다.

### 핵심 안전 및 API 준수 원칙
- **공식 eCall / SOS 최우선 정책**: 차량 내장 하드웨어 SOS 버튼 및 공식 eCall 시스템을 대체하지 않으며, 모든 위기 화면에서 **공식 SOS 버튼 우선 안내**를 상시 표시합니다.
- **미지원 차량 제어/센서 기능의 정직한 고지**: 공개 SDK 권한이 확인되지 않은 **충돌/에어백 전개 감지, 자동 비상등 점등, 자동 갓길 정차, 실제 119 자동 발신**은 실제 차량 제어로 위장하지 않고 **시뮬레이션(모의 데이터)** 배지로 명확히 구분합니다.
- **실데이터 vs 시뮬레이션 이중 어댑터 구조**: `VehicleStateSource`, `LocationSource`, `SpeechInput`, `CallGateway` 4개 도메인 인터페이스로 하드웨어/SDK 의존성을 분리하여, 실제 Pleos SDK(`.aar`)가 연결되면 실데이터를 조회하고 미연결 환경에서는 모든 예외/지연/폴백 시나리오를 결정론적으로 검증할 수 있습니다.
- **주행 중 조작 최소화 (Driver Distraction 가이드 준수)**: 차량용 16:9 가로 화면(`1920×1080` 기준)에서 한 화면 최대 핵심 액션 2개, 큰 터치 영역, 명확한 상태 배지, 타임아웃 시 안전한 기본 전이를 제공합니다.
- **보안 및 개인정보 보호**: API 키, 클라이언트 시크릿, 개인 인증정보는 소스 코드나 Git 저장소에 하드코딩하지 않으며 `local.properties`(Git 제외) 또는 런타임 주입 방식을 사용합니다.

---

## 2. 마일스톤별 아키텍처 (M0 ~ M4)

| 마일스톤 | 구현 범위 | 주요 파일 |
| :--- | :--- | :--- |
| **M0** | Android Studio 프로젝트(Kotlin + Jetpack Compose + Gradle Kotlin DSL), `minSdk 28 / targetSdk 34`, 가로 화면 고정, Pleos SDK & Car API 런타임 가용성 탐색기 | [`build.gradle.kts`](app/build.gradle.kts), [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml), [`MainActivity.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/MainActivity.kt) |
| **M1** | 음성 입력(`SpeechInput`), 위치(`LocationSource`), 차량 상태(`VehicleStateSource`), 전화/신고(`CallGateway`) 4대 독립 인터페이스 및 Pleos/Android 실환경 탐색 + 시뮬레이션 구현체 | [`DriveCheckGateways.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/domain/gateway/DriveCheckGateways.kt), [`DriveCheckGatewayImpls.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/data/gateway/DriveCheckGatewayImpls.kt) |
| **M2** | `IncidentReducer` 순수 상태 머신(`Idle`, `Listening`, `Check`, `Recheck`, `Unknown`, `Offer`, `Dial`, `Connected`, `Fallback`, `SafeClosed`, `Guide`, `Record`) 및 차량용 가로형 Compose UI 22종 상태 화면 | [`IncidentReducer.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/domain/reducer/IncidentReducer.kt), [`DriveCheckMainScreen.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/ui/screens/DriveCheckMainScreen.kt), [`DriveCheckComponents.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/ui/components/DriveCheckComponents.kt) |
| **M3** | SQLite 기반 로컬 영속 저장소(`incidents`, `incident_events`, `utterance_records`, `correction_memos`, `report_exports`), 타임라인/음성인식 원문/정정 메모 조회, Android `PdfDocument` 기반 실제 PDF 생성 및 `FileProvider` 시스템 공유 | [`IncidentDatabaseHelper.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/data/local/IncidentDatabaseHelper.kt), [`IncidentPdfExporter.kt`](app/src/main/java/com/hyundai/pleos/drivecheck/data/pdf/IncidentPdfExporter.kt) |
| **M4** | 상태 전이 단위 테스트(`IncidentReducerTest`), 에뮬레이터 시나리오 딥링크 인텐트 검증, Debug & Release APK 패키징 | [`IncidentReducerTest.kt`](app/src/test/java/com/hyundai/pleos/drivecheck/IncidentReducerTest.kt), `apk/DriveCheck_release.apk` |

---

## 3. Pleos Connect SDK 연동 구조 및 설정 안내

1. **SDK AAR 드롭인 경로**:
   - [`app/build.gradle.kts`](app/build.gradle.kts)에 `implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))`가 설정되어 있습니다.
   - Pleos Playground에서 발급받은 Vehicle State SDK `.aar` 파일을 `app/libs/` 폴더에 복사하면 별도 빌드 스크립트 수정 없이 즉시 클래스패스에 포함됩니다.
2. **런타임 SDK 감지 및 안전 폴백**:
   - [`HybridVehicleStateSource`](app/src/main/java/com/hyundai/pleos/drivecheck/data/gateway/DriveCheckGatewayImpls.kt)가 런타임에 `ai.pleos.sdk.vehicle.VehicleStateManager`(및 `android.car.Car`) 클래스 가용 여부를 점검합니다.
   - SDK가 로드된 실제 차량/Pleos 에뮬레이터 환경에서는 조회 가능한 항목(기어 단수, 속도, 안전벨트, 브레이크, 에어백 경고등, 타이어 공기압, 동력원 상태)을 표시하고, 미지원 항목(`충돌 감지 센서`, `비상등/정차 제어`)은 **`SDK 미지원 · 시뮬레이션`** 상태로 구분해 표시합니다.

---

## 4. 빌드 및 실행 방법

### 필수 환경
- **Android Studio**: Ladybug / Meerkat / 2026.2+ (번들 JBR 17~25 호환)
- **Android SDK**: API 34 (`compileSdk = 34`, `targetSdk = 34`, `minSdk = 28`)

### 터미널 명령어 (Windows PowerShell)
```powershell
# 1. 단위 테스트 실행
.\gradlew.bat testDebugUnitTest

# 2. Debug & Release APK 빌드
.\gradlew.bat assembleDebug assembleRelease

# 3. 에뮬레이터에 설치 및 실행
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.hyundai.pleos.drivecheck/.MainActivity
```

### 에뮬레이터 시나리오 바로가기 (Intent Extra 지원)
데모 및 테스트 자동화를 위해 `demo_step` 문자열 extra로 특정 상태 화면을 즉시 재현할 수 있습니다.
```powershell
# 대표 시나리오: 동승 아동 위기 신고 확인 화면
adb shell am start -S -n com.hyundai.pleos.drivecheck/.MainActivity --es demo_step child_report

# 119 발신 카운트다운 / 통화 연결 위치 브리핑 / 사고 리포트 / PDF 내보내기 완료
adb shell am start -S -n com.hyundai.pleos.drivecheck/.MainActivity --es demo_step dial_119
adb shell am start -S -n com.hyundai.pleos.drivecheck/.MainActivity --es demo_step connected_location
adb shell am start -S -n com.hyundai.pleos.drivecheck/.MainActivity --es demo_step report
adb shell am start -S -n com.hyundai.pleos.drivecheck/.MainActivity --es demo_step export_pdf
```

---

## 5. 학교/공용 컴퓨터 사용 시 보안 및 백업 가이드

1. **인증정보 분리**:
   - API 키나 클라이언트 시크릿은 절대 코드에 작성하지 말고 `local.properties`에 작성하세요. (`.gitignore`에 의해 Git 추적에서 자동 제외됩니다.)
2. **작업 종료 전 백업 방법**:
   - **방법 A (권장 - 클린 소스 압축 백업)**: `build/`, `.gradle/` 폴더를 제외한 소스 코드와 `apk/` 폴더를 ZIP으로 압축하여 개인 USB 또는 클라우드 드라이브에 보관합니다.
   - **방법 B (Git Bundle 백업)**: 원격 저장소에 푸시하지 않고도 로컬 커밋 전체를 단일 파일로 백업할 수 있습니다.
     ```powershell
     git bundle create DriveCheck_backup.bundle --all
     ```
   - 공용 PC 사용 종료 후에는 브라우저의 Pleos Playground 로그인 세션 및 개인 계정 세션을 로그아웃하세요.
