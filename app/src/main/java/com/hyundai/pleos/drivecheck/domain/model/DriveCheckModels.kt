package com.hyundai.pleos.drivecheck.domain.model

/**
 * Core state machine phases defined in DriveCheck_MVP_Rules.md (Section 2 UML)
 * and DriveCheck_Requirements_v12.md (Section 4 S-01..S-28).
 */
enum class IncidentPhase {
    IDLE,
    LISTENING,
    CHECK,
    RECHECK,
    UNKNOWN,
    OFFER,
    DIAL,
    CONNECTED,
    FALLBACK,
    SAFE_CLOSED,
    GUIDE,
    RECORD
}

/**
 * Sub-view inside RECORD phase (20A Done, 20B Report Table, 20C Voice Transcript, 21C/21D Export PDF).
 */
enum class RecordSubView {
    DONE_SUMMARY,
    VEHICLE_REPORT_TABLE,
    VOICE_TRANSCRIPT_DETAIL,
    EXPORT_PDF_CONFIG,
    EXPORT_PDF_READY
}

/**
 * Sub-scenario when in GUIDE phase (05B, 11A-11D, 12A-12D, 15A-15F).
 */
enum class GuideType {
    NONE,
    DRIVER_ILLNESS_STOP,       // 05B 운전 곤란 진술
    BATTERY_OVERHEAT_ONLY,     // 12A 고전압 배터리 과열 경고 (R-03)
    FIRE_MOVING,               // 12B 주행 중 불·연기 신고 (R-02, R-04)
    FIRE_STOPPED,              // 12C 정차 확인 후 불·연기 신고
    FIRE_STOP_UNKNOWN,         // 12D 정차 미확인 상태의 불·연기 신고
    OEM_AUTO_STOP_STARTED,     // 11A 제조사 긴급 정차 시작
    OEM_DECELERATING,          // 11B 감속 확인
    OEM_STOP_CONFIRMED,        // 11C 정차 완료 회신
    OEM_AUTO_HAZARD_CONFIRMED, // 11D 비상등 자동 점등 확인
    OEM_STOP_UNSUPPORTED,      // 15E 정차 지원 미제공
    OEM_RESULT_UNAVAILABLE,    // 15F 차량 조치 결과 미확인
    OTHER_CONTACTS_MENU,       // 15A 정차 후 다른 연락 메뉴
    ROUTE_POLICE_112,          // 14C 112 경찰 신고 안내
    ROUTE_TOW,                 // 15B 견인 연락
    ROUTE_INSURANCE,           // 15C 보험사 연락
    CONTACT_UNCONFIGURED,      // 15D 연락처 미등록
    MEDICAL_CARD_VIEW          // 14B / S-21 의료 정보 카드
}

/**
 * Call connection progress states (Rule 3: requested -> dialer_opened -> connected | failed | unknown).
 */
enum class CallProgressState {
    IDLE,
    REQUESTED,
    DIALER_OPENED,
    CONNECTED,
    FAILED,
    UNKNOWN,
    OFFICIAL_SOS_ACTIVE
}

/**
 * Location availability states (Rule 4: available | stale | denied | unavailable).
 */
enum class LocationStatus {
    AVAILABLE,
    STALE,
    DENIED,
    UNAVAILABLE
}

/**
 * Vehicle stop confirmation state (Rule 2 & R-04).
 * Single 0 km/h sample or brake pedal alone MUST remain UNKNOWN, not CONFIRMED_STOPPED.
 */
enum class StopConfirmationState {
    MOVING,
    CONFIRMED_STOPPED,
    UNKNOWN
}

/**
 * Guardian alert state (Rule 7: mutually exclusive SENT vs CANCELLED; 10s demo countdown).
 */
enum class GuardianAlertStatus {
    IDLE,
    COUNTDOWN,
    SENT,
    CANCELLED,
    FAILED_OFFLINE
}

/**
 * Call assist states during an active 119 or guardian call (Section 6, UX-13, FN-14, FN-17, FN-18).
 */
enum class CallAssistMode {
    NONE,
    ADDRESS_READY,           // 07D 통화에 주소 전달 가능
    SPEAKING_ADDRESS,        // 08B 주소 전달 중
    ASSIST_INTRO,            // 09A 대신 답하기 시작
    OPERATOR_QUESTION,       // 09B 상황요원 질문 수신 ("다친 사람이 있나요?")
    SPEAKING_SELECTED_REPLY, // 09C 선택한 답변 음성 전달 중
    NO_REPLY_ANNOUNCED,      // 09D 탑승자 답변 미확인 1회 안내
    DIRECT_SPEAKING          // 08C 직접 말하기 (합성 발언 즉시 중단, 통화 유지)
}

/**
 * Speaker role estimation (FN-04, R-09).
 */
enum class SpeakerRole(val displayLabel: String) {
    CHILD_PASSENGER_ESTIMATED("아동 동승자 · 추정"),
    PASSENGER_ESTIMATED("동승자 추정"),
    UNKNOWN_SPEAKER("화자 미확인")
}

/**
 * Explicit source classification for events and UI badges (Rule 5, R-01).
 */
enum class EventSource(val korLabel: String) {
    VEHICLE_SENSOR("차량 센서"),
    VEHICLE_STATE("차량 상태"),
    USER_SPEECH("음성 진술"),
    USER_TOUCH("터치 입력"),
    CALL_GATEWAY("통화 상태"),
    APP_SYSTEM("앱"),
    GUARDIAN_GATEWAY("전송 상태"),
    SIMULATION_INJECTOR("시연 입력")
}

/**
 * Indicates whether a data point comes from real Android/Pleos SDK or Simulation mode.
 */
enum class DataOriginMode(val badgeText: String) {
    REAL_PLATFORM("실제 플랫폼/SDK"),
    SIMULATION("시연 모의 데이터"),
    UNSUPPORTED("SDK 미지원/미연결")
}

/**
 * Pleos SDK & AAOS Vehicle API connection status.
 */
enum class SdkConnectionStatus(val summaryText: String, val detailText: String) {
    PLEOS_AAR_NOT_LINKED(
        "Pleos SDK 미연결 (시뮬레이션/안드로이드 기본 어댑터 작동 중)",
        "로컬 PC에 Pleos Connect SDK(.aar) 파일이 아직 배치되지 않아 추측 호출 없이 인터페이스 분리 모드로 실행됩니다."
    ),
    AAOS_READ_ONLY_ACTIVE(
        "AAOS Car API 읽기 연결됨",
        "Android Automotive CarPropertyManager 읽기 전용 속성이 연결되었습니다."
    ),
    SIMULATION_MODE(
        "시연 시뮬레이션 모드",
        "충돌·정차·비상등·119 연결을 안전하게 재현하는 공모전 시연 모드입니다 (실제 119 미발신)."
    )
}

enum class HazardLightState(val korLabel: String) {
    OFF("꺼짐"),
    MANUAL_ON("비상등 켜짐"),
    AUTO_ON("비상등 자동 점등"),
    UNVERIFIED("미확인")
}

sealed class SupportedFieldStatus<out T>(val origin: DataOriginMode) {
    data class Supported<T>(val value: T, val dataOrigin: DataOriginMode) : SupportedFieldStatus<T>(dataOrigin)
    data class Unsupported(val reason: String) : SupportedFieldStatus<Nothing>(DataOriginMode.UNSUPPORTED)
}

/**
 * Snapshot of vehicle safety & powertrain states (M1 VehicleStateSource).
 */
data class VehicleStateSnapshot(
    val sdkStatus: SdkConnectionStatus = SdkConnectionStatus.PLEOS_AAR_NOT_LINKED,
    val speedKmh: Int = 0,
    val isSingleZeroSpeedSample: Boolean = false,
    val gearSelection: String = "P", // P, D, N, R, UNKNOWN
    val parkingBrakeEngaged: Boolean = true,
    val brakePedalPressed: Boolean = false,
    val stopConfirmation: StopConfirmationState = StopConfirmationState.CONFIRMED_STOPPED,
    val uxRestrictionsActive: Boolean = false,
    val reduceMotionEnabled: Boolean = false,
    val hazardLightState: HazardLightState = HazardLightState.OFF,
    val batteryOverheatWarning: Boolean = false,
    val crashSignalReceived: Boolean = false,
    val seatbeltFastened: SupportedFieldStatus<Boolean> = SupportedFieldStatus.Unsupported("공개 Vehicle SDK 미지원 (시뮬레이션 가능)"),
    val airbagWarningLamp: SupportedFieldStatus<Boolean> = SupportedFieldStatus.Unsupported("충돌/에어백 신호 앱 접근 미검증 (P2 승인 대상)"),
    val tirePressureNormal: SupportedFieldStatus<Boolean> = SupportedFieldStatus.Unsupported("타이어 공기압 표준 API 미제공 (시뮬레이션 가능)"),
    val powertrainState: SupportedFieldStatus<String> = SupportedFieldStatus.Supported("정상 대기 (P)", DataOriginMode.SIMULATION),
    val observedAtTimeText: String = "14:32:03"
) {
    /**
     * UX-03, UX-06, UX-12, FN-07:
     * Animations (pulsing dot, rotating top-view car, character breathing) are allowed ONLY when
     * stopConfirmation == CONFIRMED_STOPPED and uxRestrictionsActive == false and reduceMotionEnabled == false.
     */
    val isAnimationAllowed: Boolean
        get() = stopConfirmation == StopConfirmationState.CONFIRMED_STOPPED &&
            !uxRestrictionsActive &&
            !reduceMotionEnabled

    /**
     * Rule 2: Editing, settings, and report export are restricted while driving or when stop state is unconfirmed.
     */
    val isEditingAllowed: Boolean
        get() = stopConfirmation == StopConfirmationState.CONFIRMED_STOPPED && !uxRestrictionsActive
}

/**
 * Snapshot of location state (M1 LocationSource, Rule 4).
 */
data class LocationSnapshot(
    val status: LocationStatus = LocationStatus.AVAILABLE,
    val latitude: Double = 35.8184,
    val longitude: Double = 128.5364,
    val accuracyMeters: Float = 8.5f,
    val resolvedAddress: String? = "대구 달서구 월배로 120",
    val addressConversionSucceeded: Boolean = true,
    val observedAtText: String = "14:32",
    val originMode: DataOriginMode = DataOriginMode.SIMULATION
)

/**
 * Speech recognition state (M1 SpeechInput, Rule 1 & UX-04..UX-07).
 */
data class SpeechSnapshot(
    val isMicOpen: Boolean = false,
    val inputLevel: Float = 0f,
    val partialTranscript: String = "",
    val finalizedTranscript: String = "",
    val speakerRole: SpeakerRole = SpeakerRole.UNKNOWN_SPEAKER,
    val recognitionFailed: Boolean = false,
    val isTtsEchoIgnored: Boolean = false,
    val originMode: DataOriginMode = DataOriginMode.SIMULATION
)

/**
 * Incident header stored per emergency event (Rule 5).
 */
data class Incident(
    val incidentId: String = "DC-0142",
    val startedAtKst: String = "14:32 KST",
    val summaryRegion: String = "대구 달서구",
    val isSimulationRecord: Boolean = true,
    val phase: IncidentPhase = IncidentPhase.IDLE,
    val driverConsciousnessConfirmed: Boolean? = null // null = 미확인 (never assumed unconscious from no-reply)
)

/**
 * Immutable event log entry under an Incident (Rule 5: eventId, incidentId, source, kind, observedAt, receivedAt, outcome).
 */
data class IncidentEvent(
    val eventId: String,
    val incidentId: String,
    val source: EventSource,
    val kind: String,
    val observedAt: String,
    val receivedAt: String,
    val outcome: String,
    val isSimulated: Boolean = true
)

/**
 * Finalized utterance stored separately from main report table (Rule 6, FN-13, 20C).
 */
data class UtteranceRecord(
    val utteranceId: String,
    val incidentId: String,
    val observedAt: String,
    val speakerRole: SpeakerRole,
    val finalizedText: String,
    val isSimulated: Boolean = true
)

/**
 * User post-incident correction memo kept separate from original immutable events (Rule 5, FN-10, 22B).
 */
data class CorrectionMemo(
    val memoId: String,
    val incidentId: String,
    val createdAt: String,
    val memoText: String
)

/**
 * Export configuration for PDF generation (Rule 6, FN-11, 21C).
 * Voice transcript is unchecked (false) by default!
 */
data class ReportExportConfig(
    val includeTimeAndLocation: Boolean = true,
    val includeSensorAndVehicleActions: Boolean = true,
    val includeCallAndAlertResults: Boolean = true,
    val includePassengerVoiceTranscript: Boolean = false
)

/**
 * Exported PDF version metadata (FN-11, 21D).
 */
data class ReportExportVersion(
    val versionNumber: Int,
    val incidentId: String,
    val filePath: String,
    val createdAt: String,
    val shareStatus: String // 파일 준비 완료 | 공유 요청됨 | 수신 확인됨
)

/**
 * Pre-configured medical card info (P1 FN-12, FN-16, separated from default insurance export).
 */
data class MedicalCardInfo(
    val drugAllergies: String = "페니실린 계열 알레르기 (시연 데이터)",
    val medications: String = "혈압약 1일 1회 복용",
    val hospitalName: String = "경북대학교병원 순환기내과",
    val hospitalPhone: String = "053-200-5114",
    val notes: String = "보호자 연락 우선 요청"
)

/**
 * Pre-configured emergency contacts (P1 FN-12).
 */
data class EmergencyContactsConfig(
    val guardianName: String = "보호자 (배우자 · 시연)",
    val guardianPhone: String = "010-0000-0000",
    val guardianOptIn: Boolean = true,
    val towCompanyName: String = "현대 긴급출동 · 견인 (시연)",
    val towPhone: String = "080-600-6000",
    val towConfigured: Boolean = true,
    val insurerName: String = "현대해상 사고접수 (시연)",
    val insurerPhone: String = "1588-5656",
    val insurerConfigured: Boolean = true
)
