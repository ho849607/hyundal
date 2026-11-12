package com.hyundai.pleos.drivecheck.domain.reducer

import com.hyundai.pleos.drivecheck.domain.model.*
import com.hyundai.pleos.drivecheck.domain.planner.MaintenanceRead

/**
 * Complete UI & domain state for DriveCheck (M2).
 */
data class DriveCheckUiState(
    val incident: Incident = Incident(),
    val phase: IncidentPhase = IncidentPhase.IDLE,
    val recordSubView: RecordSubView = RecordSubView.VEHICLE_REPORT_TABLE,
    val guideType: GuideType = GuideType.NONE,
    val isNarrowWindowMode: Boolean = false,
    val showStoppedContactSelector: Boolean = false, // 03C suggest.png
    val direct119Requested: Boolean = false,         // Skips 상태 확인 row when true (05D flow_direct.png)
    val includeMedicalRow: Boolean = false,          // Adds 의료 정보 row when needed (14B / 20A)
    val includePolice112Row: Boolean = false,        // Adds 112 연결 row when needed (14C)
    val passengerStatementText: String = "",
    val callProgress: CallProgressState = CallProgressState.IDLE,
    val callAssistMode: CallAssistMode = CallAssistMode.NONE,
    val operatorQuestion: String = "다친 사람이 있나요?",
    val selectedAssistAnswer: String = "",
    val autoAnswerEnabled: Boolean = false,
    val guardianAlertStatus: GuardianAlertStatus = GuardianAlertStatus.IDLE,
    val guardianCountdownSeconds: Int = 10,
    val vehicle: VehicleStateSnapshot = VehicleStateSnapshot(),
    val location: LocationSnapshot = LocationSnapshot(),
    val speech: SpeechSnapshot = SpeechSnapshot(),
    val events: List<IncidentEvent> = defaultSeededDemoEvents(),
    val utterances: List<UtteranceRecord> = defaultSeededUtterances(),
    val correctionMemos: List<CorrectionMemo> = emptyList(),
    val exportConfig: ReportExportConfig = ReportExportConfig(),
    val exportedVersions: List<ReportExportVersion> = emptyList(),
    val medicalCard: MedicalCardInfo = MedicalCardInfo(),
    val contactsConfig: EmergencyContactsConfig = EmergencyContactsConfig(),
    val sdkInspectionReport: String = "Pleos SDK 상태 확인 대기 중",
    val sdkInitLine: String = "SDK 초기화: 아직 시도 전",
    val sdkDoorLine: String = "앞왼쪽 도어 위치 조회(연결 확인용): 아직 시도 전",
    val maintenanceRead: MaintenanceRead = MaintenanceRead(),
    val statusBannerMessage: String = "시연 모드 (모의 데이터 출처 표시 · 실제 119 미발신)"
) {
    /**
     * Right-hand step list items based on current incident path (DriveCheck_Requirements_v12.md Section 1).
     */
    val timelineSteps: List<String>
        get() = buildList {
            if (direct119Requested && phase == IncidentPhase.DIAL) {
                add("119 연결")
                add("위치 안내")
            } else {
                add("위험 감지")
                if (!direct119Requested) {
                    add("상태 확인")
                }
                add("119 연결")
                add("위치 안내")
                if (includeMedicalRow) {
                    add("의료 정보")
                }
                if (includePolice112Row) {
                    add("112 연결")
                }
                add("보호자 알림")
            }
        }

    /**
     * Active orange dot index in the right-hand progression list (UX-12).
     */
    val activeStepIndex: Int
        get() {
            val steps = timelineSteps
            val targetName = when (phase) {
                IncidentPhase.IDLE, IncidentPhase.LISTENING -> "위험 감지"
                IncidentPhase.CHECK -> if (vehicle.crashSignalReceived) "위험 감지" else "상태 확인"
                IncidentPhase.RECHECK, IncidentPhase.UNKNOWN, IncidentPhase.SAFE_CLOSED -> "상태 확인"
                IncidentPhase.OFFER, IncidentPhase.DIAL, IncidentPhase.FALLBACK -> "119 연결"
                IncidentPhase.CONNECTED -> "위치 안내"
                IncidentPhase.GUIDE -> when (guideType) {
                    GuideType.MEDICAL_CARD_VIEW -> "의료 정보"
                    GuideType.ROUTE_POLICE_112 -> "112 연결"
                    else -> "위험 감지"
                }
                IncidentPhase.RECORD -> steps.lastOrNull() ?: "보호자 알림"
            }
            return steps.indexOf(targetName).coerceAtLeast(0)
        }
}

sealed interface IncidentAction {
    data class StartVoiceListening(val speakerRole: SpeakerRole = SpeakerRole.UNKNOWN_SPEAKER) : IncidentAction
    data class UpdatePartialSpeech(val partialText: String, val inputLevel: Float = 0.65f) : IncidentAction
    data class FinalizeSpeech(
        val finalText: String,
        val speakerRole: SpeakerRole = SpeakerRole.UNKNOWN_SPEAKER,
        val isTtsEcho: Boolean = false,
        val timestamp: String = "14:32:08"
    ) : IncidentAction
    data object SpeechRecognitionFailed : IncidentAction
    data class TriggerCrashSignalSimulation(val timestamp: String = "14:32:03") : IncidentAction
    data class TriggerAmbiguousHelpRequest(val timestamp: String = "14:32:04") : IncidentAction
    data class DriverRespondedSafe(val timestamp: String = "14:32:09") : IncidentAction
    data class FirstCheckNoReply(val timestamp: String = "14:32:10") : IncidentAction
    data class SecondCheckNoReply(val timestamp: String = "14:32:15") : IncidentAction
    data class PassengerReportedUnresponsive(
        val statement: String = "운전하는 엄마가 대답을 안 해요.",
        val speakerRole: SpeakerRole = SpeakerRole.CHILD_PASSENGER_ESTIMATED,
        val timestamp: String = "14:32:08"
    ) : IncidentAction
    data class Request119Call(
        val byVoice: Boolean = false,
        val isDirectFromIdle: Boolean = false,
        val timestamp: String = "14:32:11"
    ) : IncidentAction
    data class CallDialerOpened(val timestamp: String = "14:32:11") : IncidentAction
    data class CallConnectedConfirmed(val timestamp: String = "14:32:12") : IncidentAction
    data class CallConnectionFailed(val timestamp: String = "14:32:12") : IncidentAction
    data class CallConnectionUnknown(val timestamp: String = "14:32:12") : IncidentAction
    data class SetOfficialSosActive(val active: Boolean, val timestamp: String = "14:32:05") : IncidentAction
    data class StartSpeakingAddressInCall(val timestamp: String = "14:32:14") : IncidentAction
    data class OpenCallAssist(val operatorQuestion: String = "다친 사람이 있나요?") : IncidentAction
    data class SelectCallAssistAnswer(val answer: String, val timestamp: String = "14:32:16") : IncidentAction
    data class NotifyNoPassengerReplyInAutoCall(val timestamp: String = "14:32:17") : IncidentAction
    data class SwitchToDirectSpeaking(val timestamp: String = "14:32:18") : IncidentAction
    data class StartGuardianCountdown(val timestamp: String = "14:32:19") : IncidentAction
    data object TickGuardianCountdown : IncidentAction
    data class CancelGuardianAlert(val timestamp: String = "14:32:21") : IncidentAction
    data class GuardianAlertFailedOffline(val timestamp: String = "14:32:23") : IncidentAction
    data class TriggerGuideScenario(val guideType: GuideType, val timestamp: String = "14:32:18") : IncidentAction
    data class CompleteAndSaveRecord(val timestamp: String = "14:32:25") : IncidentAction
    data class OpenRecordView(val subView: RecordSubView) : IncidentAction
    data class UpdateExportConfig(val config: ReportExportConfig) : IncidentAction
    data class AddCorrectionMemo(val memoText: String, val timestamp: String = "14:35:00") : IncidentAction
    data class RecordPdfExported(val filePath: String, val timestamp: String = "14:36:00") : IncidentAction
    data class ToggleNarrowWindow(val isNarrow: Boolean) : IncidentAction
    data class ToggleStoppedContactSelector(val show: Boolean) : IncidentAction
    data object DismissSafeScreenToIdle : IncidentAction
    data object ResetToDemoStart : IncidentAction
}

/**
 * Pure Kotlin state reducer implementing DriveCheck_MVP_Rules.md and DriveCheck_Requirements_v12.md.
 */
object IncidentReducer {

    fun reduce(state: DriveCheckUiState, action: IncidentAction): DriveCheckUiState {
        return when (action) {
            is IncidentAction.StartVoiceListening -> {
                state.copy(
                    phase = IncidentPhase.LISTENING,
                    speech = SpeechSnapshot(
                        isMicOpen = true,
                        inputLevel = 0.5f,
                        partialTranscript = "",
                        finalizedTranscript = "",
                        speakerRole = action.speakerRole,
                        recognitionFailed = false,
                        originMode = DataOriginMode.SIMULATION
                    )
                )
            }

            is IncidentAction.UpdatePartialSpeech -> {
                // Rule 1: Intermediate/partial ASR results are ONLY for display, never for intent transitions!
                state.copy(
                    phase = IncidentPhase.LISTENING,
                    speech = state.speech.copy(
                        isMicOpen = true,
                        inputLevel = action.inputLevel,
                        partialTranscript = action.partialText,
                        recognitionFailed = false
                    )
                )
            }

            is IncidentAction.FinalizeSpeech -> {
                // Rule 1: Ignore TTS echo completely.
                if (action.isTtsEcho) {
                    return state.copy(
                        speech = state.speech.copy(
                            isMicOpen = false,
                            inputLevel = 0f,
                            partialTranscript = "",
                            isTtsEchoIgnored = true
                        )
                    )
                }

                val text = action.finalText.trim()
                if (text.isEmpty()) {
                    return reduce(state, IncidentAction.SpeechRecognitionFailed)
                }

                val updatedSpeech = state.speech.copy(
                    isMicOpen = false,
                    inputLevel = 0f,
                    partialTranscript = "",
                    finalizedTranscript = text,
                    speakerRole = action.speakerRole,
                    recognitionFailed = false
                )

                val newUtterance = UtteranceRecord(
                    utteranceId = "UTT-${state.utterances.size + 1}",
                    incidentId = state.incident.incidentId,
                    observedAt = action.timestamp,
                    speakerRole = action.speakerRole,
                    finalizedText = text,
                    isSimulated = true
                )
                val baseState = state.copy(
                    speech = updatedSpeech,
                    utterances = state.utterances + newUtterance
                )

                classifyAndTransitionFinalUtterance(baseState, text, action.speakerRole, action.timestamp)
            }

            IncidentAction.SpeechRecognitionFailed -> {
                state.copy(
                    phase = IncidentPhase.LISTENING,
                    speech = state.speech.copy(
                        isMicOpen = false,
                        inputLevel = 0f,
                        partialTranscript = "",
                        recognitionFailed = true
                    )
                )
            }

            is IncidentAction.TriggerCrashSignalSimulation -> {
                val updatedVehicle = state.vehicle.copy(
                    crashSignalReceived = true,
                    airbagWarningLamp = SupportedFieldStatus.Supported(true, DataOriginMode.SIMULATION),
                    observedAtTimeText = action.timestamp
                )
                val event = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.VEHICLE_SENSOR,
                    kind = "충돌 이벤트 수신 (시연 모의 신호)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "신호 수신",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CHECK,
                    direct119Requested = false,
                    vehicle = updatedVehicle,
                    events = state.events + event
                )
            }

            is IncidentAction.TriggerAmbiguousHelpRequest -> {
                val event = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.USER_SPEECH,
                    kind = "모호한 도움 요청 수신 (“도와줘”)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "상태 확인 진입",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CHECK,
                    direct119Requested = false,
                    vehicle = state.vehicle.copy(crashSignalReceived = false),
                    events = state.events + event
                )
            }

            is IncidentAction.DriverRespondedSafe -> {
                val hasContinuousHazard = state.vehicle.batteryOverheatWarning ||
                    (state.phase == IncidentPhase.GUIDE && state.guideType in setOf(
                        GuideType.FIRE_MOVING,
                        GuideType.FIRE_STOPPED,
                        GuideType.FIRE_STOP_UNKNOWN
                    ))
                val hasActiveCall = state.callProgress == CallProgressState.CONNECTED ||
                    state.callProgress == CallProgressState.OFFICIAL_SOS_ACTIVE

                val event = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.USER_TOUCH,
                    kind = "운전자 괜찮음 응답 확인",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = if (hasContinuousHazard || hasActiveCall) "위험 지속 안내 유지" else "응답 확인 완료",
                    isSimulated = true
                )

                // Rule FN-03: Close crisis flow only when safe response arrives AND no continuous hazard or active call exists.
                if (hasContinuousHazard || hasActiveCall) {
                    state.copy(
                        incident = state.incident.copy(driverConsciousnessConfirmed = true),
                        events = state.events + event
                    )
                } else {
                    state.copy(
                        phase = IncidentPhase.SAFE_CLOSED,
                        incident = state.incident.copy(
                            phase = IncidentPhase.SAFE_CLOSED,
                            driverConsciousnessConfirmed = true
                        ),
                        events = state.events + event
                    )
                }
            }

            is IncidentAction.FirstCheckNoReply -> {
                // Rule 1 & FN-03: First no-reply moves to RECHECK (04D). Never assumes unconsciousness or auto-dials!
                val event = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.APP_SYSTEM,
                    kind = "1차 상태 확인 미응답",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "재확인 전환",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.RECHECK,
                    incident = state.incident.copy(
                        phase = IncidentPhase.RECHECK,
                        driverConsciousnessConfirmed = null
                    ),
                    events = state.events + event
                )
            }

            is IncidentAction.SecondCheckNoReply -> {
                // Rule 1 & FN-03: Second no-reply moves to UNKNOWN (05A).
                // Consciousness remains unconfirmed (null), and NO auto-119 call or vehicle stop is triggered!
                val event = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.APP_SYSTEM,
                    kind = "2차 재확인 미응답 (운전자 상태 미확인 대기)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "상태 미확인",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.UNKNOWN,
                    incident = state.incident.copy(
                        phase = IncidentPhase.UNKNOWN,
                        driverConsciousnessConfirmed = null
                    ),
                    events = state.events + event
                )
            }

            is IncidentAction.PassengerReportedUnresponsive -> {
                // FN-02, FN-15, S-28 (05C): Preserve passenger statement, keep consciousness unconfirmed, prioritize large 119.
                val event = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.USER_SPEECH,
                    kind = "동승자 무반응 제보 (“${action.statement}”)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "119 우선 제안",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.OFFER,
                    passengerStatementText = action.statement,
                    speech = state.speech.copy(
                        finalizedTranscript = action.statement,
                        speakerRole = action.speakerRole
                    ),
                    incident = state.incident.copy(
                        phase = IncidentPhase.OFFER,
                        driverConsciousnessConfirmed = null
                    ),
                    events = state.events + event
                )
            }

            is IncidentAction.Request119Call -> {
                // Rule 3 & FN-06: If official SOS is already active, do NOT place a duplicate call request.
                if (state.callProgress == CallProgressState.OFFICIAL_SOS_ACTIVE) {
                    val sosEvent = IncidentEvent(
                        eventId = "EVT-${state.events.size + 1}",
                        incidentId = state.incident.incidentId,
                        source = EventSource.CALL_GATEWAY,
                        kind = "공식 SOS 활성 확인 (중복 119 발신 생략)",
                        observedAt = action.timestamp,
                        receivedAt = action.timestamp,
                        outcome = "기존 SOS 유지",
                        isSimulated = true
                    )
                    return state.copy(
                        phase = IncidentPhase.CONNECTED,
                        events = state.events + sosEvent
                    )
                }

                val isDirect = action.isDirectFromIdle || state.phase == IncidentPhase.IDLE
                val reqEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = if (action.byVoice) EventSource.USER_SPEECH else EventSource.USER_TOUCH,
                    kind = "119 긴급전화 호출 요청 (시연 모의 경로)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "요청됨 (requested)",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.DIAL,
                    direct119Requested = isDirect,
                    callProgress = CallProgressState.DIALER_OPENED,
                    incident = state.incident.copy(phase = IncidentPhase.DIAL),
                    events = state.events + reqEvent
                )
            }

            is IncidentAction.CallDialerOpened -> {
                val openEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.CALL_GATEWAY,
                    kind = "전화 화면 열림",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "dialer_opened",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.DIAL,
                    callProgress = CallProgressState.DIALER_OPENED,
                    events = state.events + openEvent
                )
            }

            is IncidentAction.CallConnectedConfirmed -> {
                val connEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.CALL_GATEWAY,
                    kind = "119 연결 (시연 모의 연결)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "연결 확인",
                    isSimulated = true
                )
                val locEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 2}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.APP_SYSTEM,
                    kind = "차량 위치 안내 (${state.location.resolvedAddress ?: "좌표 표시"})",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "화면 표시",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CONNECTED,
                    callProgress = CallProgressState.CONNECTED,
                    callAssistMode = CallAssistMode.ADDRESS_READY,
                    incident = state.incident.copy(phase = IncidentPhase.CONNECTED),
                    events = state.events + connEvent + locEvent
                )
            }

            is IncidentAction.CallConnectionFailed -> {
                val failEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.CALL_GATEWAY,
                    kind = "119 발신 경로 연결 실패",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "실패 (휴대폰 119 안내)",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.FALLBACK,
                    callProgress = CallProgressState.FAILED,
                    callAssistMode = CallAssistMode.NONE,
                    incident = state.incident.copy(phase = IncidentPhase.FALLBACK),
                    events = state.events + failEvent
                )
            }

            is IncidentAction.CallConnectionUnknown -> {
                val unkEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.CALL_GATEWAY,
                    kind = "119 연결 회신 미확인",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "연결 미확인 (unknown)",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.FALLBACK,
                    callProgress = CallProgressState.UNKNOWN,
                    callAssistMode = CallAssistMode.NONE,
                    incident = state.incident.copy(phase = IncidentPhase.FALLBACK),
                    events = state.events + unkEvent
                )
            }

            is IncidentAction.SetOfficialSosActive -> {
                state.copy(
                    callProgress = if (action.active) CallProgressState.OFFICIAL_SOS_ACTIVE else CallProgressState.IDLE
                )
            }

            is IncidentAction.StartSpeakingAddressInCall -> {
                val addrEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.APP_SYSTEM,
                    kind = "통화 내 주소 음성 전달 시작",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "주소 전달 중",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CONNECTED,
                    callAssistMode = CallAssistMode.SPEAKING_ADDRESS,
                    events = state.events + addrEvent
                )
            }

            is IncidentAction.OpenCallAssist -> {
                state.copy(
                    phase = IncidentPhase.CONNECTED,
                    callAssistMode = CallAssistMode.OPERATOR_QUESTION,
                    operatorQuestion = action.operatorQuestion
                )
            }

            is IncidentAction.SelectCallAssistAnswer -> {
                val ansEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.USER_TOUCH,
                    kind = "통화 보조 답변 선택 (“${action.answer}”)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "회선 음성 전달",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CONNECTED,
                    callAssistMode = CallAssistMode.SPEAKING_SELECTED_REPLY,
                    selectedAssistAnswer = action.answer,
                    events = state.events + ansEvent
                )
            }

            is IncidentAction.NotifyNoPassengerReplyInAutoCall -> {
                // FN-18: In auto-answer mode, if passenger does not reply, announce once that reply is unconfirmed.
                // Never fabricates injury or consciousness status.
                val noReplyEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.APP_SYSTEM,
                    kind = "자동 답변: 탑승자 응답 미확인 1회 안내",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "합성 발언 전달",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CONNECTED,
                    callAssistMode = CallAssistMode.NO_REPLY_ANNOUNCED,
                    events = state.events + noReplyEvent
                )
            }

            is IncidentAction.SwitchToDirectSpeaking -> {
                // Rule 7 & FN-17: Immediately stop synthetic speech and keep existing call connected.
                val directEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.USER_TOUCH,
                    kind = "직접 말하기 전환 (합성 발언 즉시 중단)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "통화 유지",
                    isSimulated = true
                )
                state.copy(
                    phase = IncidentPhase.CONNECTED,
                    callAssistMode = CallAssistMode.DIRECT_SPEAKING,
                    events = state.events + directEvent
                )
            }

            is IncidentAction.StartGuardianCountdown -> {
                // Rule 7: Cancelled or already-sent guardian alerts are NOT retried automatically.
                if (state.guardianAlertStatus == GuardianAlertStatus.SENT ||
                    state.guardianAlertStatus == GuardianAlertStatus.CANCELLED
                ) {
                    return state
                }
                state.copy(
                    guardianAlertStatus = GuardianAlertStatus.COUNTDOWN,
                    guardianCountdownSeconds = 10
                )
            }

            IncidentAction.TickGuardianCountdown -> {
                if (state.guardianAlertStatus != GuardianAlertStatus.COUNTDOWN) {
                    return state
                }
                val nextSec = state.guardianCountdownSeconds - 1
                if (nextSec > 0) {
                    state.copy(guardianCountdownSeconds = nextSec)
                } else {
                    val sentEvent = IncidentEvent(
                        eventId = "EVT-${state.events.size + 1}",
                        incidentId = state.incident.incidentId,
                        source = EventSource.GUARDIAN_GATEWAY,
                        kind = "보호자 알림 발송",
                        observedAt = "14:32:23",
                        receivedAt = "14:32:23",
                        outcome = "서버 접수",
                        isSimulated = true
                    )
                    state.copy(
                        guardianAlertStatus = GuardianAlertStatus.SENT,
                        guardianCountdownSeconds = 0,
                        events = state.events + sentEvent
                    )
                }
            }

            is IncidentAction.CancelGuardianAlert -> {
                // Rule 7: Mutually exclusive with SENT. If already SENT, cannot cancel.
                if (state.guardianAlertStatus == GuardianAlertStatus.SENT) {
                    return state
                }
                val cancelEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.USER_TOUCH,
                    kind = "보호자 알림 발송 전 취소",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "발송 취소됨",
                    isSimulated = true
                )
                state.copy(
                    guardianAlertStatus = GuardianAlertStatus.CANCELLED,
                    guardianCountdownSeconds = 0,
                    events = state.events + cancelEvent
                )
            }

            is IncidentAction.GuardianAlertFailedOffline -> {
                val failEvent = IncidentEvent(
                    eventId = "EVT-${state.events.size + 1}",
                    incidentId = state.incident.incidentId,
                    source = EventSource.GUARDIAN_GATEWAY,
                    kind = "보호자 알림 오프라인 전송 보류 (로컬 저장)",
                    observedAt = action.timestamp,
                    receivedAt = action.timestamp,
                    outcome = "전송 실패 (재시도 대기)",
                    isSimulated = true
                )
                state.copy(
                    guardianAlertStatus = GuardianAlertStatus.FAILED_OFFLINE,
                    events = state.events + failEvent
                )
            }

            is IncidentAction.TriggerGuideScenario -> {
                val addMedical = state.includeMedicalRow || action.guideType == GuideType.MEDICAL_CARD_VIEW
                val addPolice = state.includePolice112Row || action.guideType == GuideType.ROUTE_POLICE_112
                val updatedVehicle = when (action.guideType) {
                    GuideType.OEM_STOP_CONFIRMED -> state.vehicle.copy(
                        speedKmh = 0,
                        gearSelection = "P",
                        parkingBrakeEngaged = true,
                        stopConfirmation = StopConfirmationState.CONFIRMED_STOPPED,
                        uxRestrictionsActive = false
                    )
                    GuideType.OEM_AUTO_HAZARD_CONFIRMED -> state.vehicle.copy(
                        speedKmh = 0,
                        gearSelection = "P",
                        parkingBrakeEngaged = true,
                        stopConfirmation = StopConfirmationState.CONFIRMED_STOPPED,
                        uxRestrictionsActive = false,
                        hazardLightState = HazardLightState.AUTO_ON
                    )
                    GuideType.BATTERY_OVERHEAT_ONLY -> state.vehicle.copy(
                        batteryOverheatWarning = true
                    )
                    GuideType.FIRE_MOVING -> state.vehicle.copy(
                        speedKmh = 42,
                        gearSelection = "D",
                        parkingBrakeEngaged = false,
                        stopConfirmation = StopConfirmationState.MOVING,
                        uxRestrictionsActive = true
                    )
                    GuideType.FIRE_STOPPED -> state.vehicle.copy(
                        speedKmh = 0,
                        gearSelection = "P",
                        parkingBrakeEngaged = true,
                        stopConfirmation = StopConfirmationState.CONFIRMED_STOPPED,
                        uxRestrictionsActive = false
                    )
                    GuideType.FIRE_STOP_UNKNOWN -> state.vehicle.copy(
                        speedKmh = 0,
                        isSingleZeroSpeedSample = true,
                        gearSelection = "D",
                        parkingBrakeEngaged = false,
                        stopConfirmation = StopConfirmationState.UNKNOWN,
                        uxRestrictionsActive = true
                    )
                    else -> state.vehicle
                }
                state.copy(
                    phase = IncidentPhase.GUIDE,
                    guideType = action.guideType,
                    includeMedicalRow = addMedical,
                    includePolice112Row = addPolice,
                    vehicle = updatedVehicle
                )
            }

            is IncidentAction.CompleteAndSaveRecord -> {
                state.copy(
                    phase = IncidentPhase.RECORD,
                    recordSubView = RecordSubView.DONE_SUMMARY,
                    incident = state.incident.copy(phase = IncidentPhase.RECORD)
                )
            }

            is IncidentAction.OpenRecordView -> {
                state.copy(
                    phase = IncidentPhase.RECORD,
                    recordSubView = action.subView,
                    incident = state.incident.copy(phase = IncidentPhase.RECORD)
                )
            }

            is IncidentAction.UpdateExportConfig -> {
                state.copy(exportConfig = action.config)
            }

            is IncidentAction.AddCorrectionMemo -> {
                if (action.memoText.isBlank()) return state
                val memo = CorrectionMemo(
                    memoId = "MEMO-${state.correctionMemos.size + 1}",
                    incidentId = state.incident.incidentId,
                    createdAt = action.timestamp,
                    memoText = action.memoText.trim()
                )
                state.copy(correctionMemos = state.correctionMemos + memo)
            }

            is IncidentAction.RecordPdfExported -> {
                val nextVer = state.exportedVersions.size + 1
                val ver = ReportExportVersion(
                    versionNumber = nextVer,
                    incidentId = state.incident.incidentId,
                    filePath = action.filePath,
                    createdAt = action.timestamp,
                    shareStatus = "파일 준비 완료 (v$nextVer)"
                )
                state.copy(
                    phase = IncidentPhase.RECORD,
                    recordSubView = RecordSubView.EXPORT_PDF_READY,
                    exportedVersions = state.exportedVersions + ver
                )
            }

            is IncidentAction.ToggleNarrowWindow -> {
                state.copy(isNarrowWindowMode = action.isNarrow)
            }

            is IncidentAction.ToggleStoppedContactSelector -> {
                state.copy(
                    phase = IncidentPhase.IDLE,
                    showStoppedContactSelector = action.show
                )
            }

            IncidentAction.DismissSafeScreenToIdle -> {
                state.copy(
                    phase = IncidentPhase.IDLE,
                    guideType = GuideType.NONE,
                    showStoppedContactSelector = false
                )
            }

            IncidentAction.ResetToDemoStart -> {
                DriveCheckUiState(sdkInspectionReport = state.sdkInspectionReport)
            }
        }
    }

    /**
     * Classifies ONLY finalized utterances (Rule 1, R-02, R-06).
     * Explicitly filters out negated expressions ("연기가 없어요", "불 안 났어요", "안 다쳤어요").
     */
    private fun classifyAndTransitionFinalUtterance(
        state: DriveCheckUiState,
        text: String,
        speakerRole: SpeakerRole,
        timestamp: String
    ): DriveCheckUiState {
        val normalized = text.replace(" ", "")

        // 1. Check negated expressions first (Rule 1 & R-02: "‘연기가 없어요’ 같은 부정 표현...으로 긴급 신고를 확정하지 않는다")
        val isNegatedSmokeOrFire = normalized.contains("연기가없") ||
            normalized.contains("연기없") ||
            normalized.contains("불안났") ||
            normalized.contains("불이안났") ||
            normalized.contains("안다쳤") ||
            normalized.contains("괜찮")

        if (isNegatedSmokeOrFire) {
            return reduce(state, IncidentAction.DriverRespondedSafe(timestamp))
        }

        // 2. Explicit 119 request (always prioritized, skips CHECK)
        if (normalized.contains("119") || normalized.contains("일일구") || normalized.contains("구급차")) {
            return reduce(
                state,
                IncidentAction.Request119Call(
                    byVoice = true,
                    isDirectFromIdle = state.phase == IncidentPhase.IDLE || state.phase == IncidentPhase.LISTENING,
                    timestamp = timestamp
                )
            )
        }

        // 3. Passenger report of unresponsive driver ("운전하는 엄마가 대답을 안 해요", "반응하지 않아요", "기절")
        if (normalized.contains("대답을안") ||
            normalized.contains("대답안") ||
            normalized.contains("반응하지않") ||
            normalized.contains("반응이없") ||
            normalized.contains("기절") ||
            normalized.contains("의식이없")
        ) {
            val displayQuote = if (normalized.contains("엄마") || normalized.contains("대답")) {
                "운전자가 반응하지 않아요."
            } else {
                text
            }
            return reduce(
                state,
                IncidentAction.PassengerReportedUnresponsive(
                    statement = displayQuote,
                    speakerRole = speakerRole,
                    timestamp = timestamp
                )
            )
        }

        // 4. Fire or smoke report (R-02 overrides R-03 overheat, branches on R-04 stopConfirmation)
        if (normalized.contains("연기") || normalized.contains("불이났") || normalized.contains("불났") || normalized.contains("타는냄새")) {
            val fireGuide = when (state.vehicle.stopConfirmation) {
                StopConfirmationState.MOVING -> GuideType.FIRE_MOVING
                StopConfirmationState.CONFIRMED_STOPPED -> GuideType.FIRE_STOPPED
                StopConfirmationState.UNKNOWN -> GuideType.FIRE_STOP_UNKNOWN
            }
            return reduce(state, IncidentAction.TriggerGuideScenario(fireGuide, timestamp))
        }

        // 5. Driver difficulty continuing driving (05B)
        if (normalized.contains("운전하기어려") || normalized.contains("운전못하") || normalized.contains("어지러") || normalized.contains("아파요")) {
            return reduce(state, IncidentAction.TriggerGuideScenario(GuideType.DRIVER_ILLNESS_STOP, timestamp))
        }

        // 6. Guardian alert voice command
        if (normalized.contains("보호자알림") || normalized.contains("가족알림")) {
            return reduce(state, IncidentAction.StartGuardianCountdown(timestamp))
        }

        // 7. Police 112 / Tow / Insurance voice commands
        if (normalized.contains("112") || normalized.contains("경찰")) {
            return reduce(state, IncidentAction.TriggerGuideScenario(GuideType.ROUTE_POLICE_112, timestamp))
        }
        if (normalized.contains("견인")) {
            return reduce(state, IncidentAction.TriggerGuideScenario(GuideType.ROUTE_TOW, timestamp))
        }
        if (normalized.contains("보험")) {
            return reduce(state, IncidentAction.TriggerGuideScenario(GuideType.ROUTE_INSURANCE, timestamp))
        }

        // 8. Address reading / Direct speaking in call
        if (normalized.contains("주소말해") || normalized.contains("주소전달")) {
            return reduce(state, IncidentAction.StartSpeakingAddressInCall(timestamp))
        }
        if (normalized.contains("직접말하")) {
            return reduce(state, IncidentAction.SwitchToDirectSpeaking(timestamp))
        }

        // 9. Ambiguous help request ("도와줘", etc.) -> CHECK
        return reduce(state, IncidentAction.TriggerAmbiguousHelpRequest(timestamp))
    }
}

fun defaultSeededDemoEvents(): List<IncidentEvent> = listOf(
    IncidentEvent(
        eventId = "EVT-1",
        incidentId = "DC-0142",
        source = EventSource.VEHICLE_SENSOR,
        kind = "충돌 이벤트 수신",
        observedAt = "14:32:03",
        receivedAt = "14:32:03",
        outcome = "신호 수신",
        isSimulated = true
    ),
    IncidentEvent(
        eventId = "EVT-2",
        incidentId = "DC-0142",
        source = EventSource.VEHICLE_SENSOR,
        kind = "속도 34 km/h",
        observedAt = "14:32:06",
        receivedAt = "14:32:06",
        outcome = "관측값",
        isSimulated = true
    ),
    IncidentEvent(
        eventId = "EVT-3",
        incidentId = "DC-0142",
        source = EventSource.CALL_GATEWAY,
        kind = "119 연결",
        observedAt = "14:32:12",
        receivedAt = "14:32:12",
        outcome = "연결 확인",
        isSimulated = true
    ),
    IncidentEvent(
        eventId = "EVT-4",
        incidentId = "DC-0142",
        source = EventSource.APP_SYSTEM,
        kind = "차량 위치 안내",
        observedAt = "14:32:13",
        receivedAt = "14:32:13",
        outcome = "화면 표시",
        isSimulated = true
    ),
    IncidentEvent(
        eventId = "EVT-5",
        incidentId = "DC-0142",
        source = EventSource.VEHICLE_STATE,
        kind = "정차 · 비상등 자동 점등",
        observedAt = "14:32:18",
        receivedAt = "14:32:18",
        outcome = "회신 확인",
        isSimulated = true
    ),
    IncidentEvent(
        eventId = "EVT-6",
        incidentId = "DC-0142",
        source = EventSource.GUARDIAN_GATEWAY,
        kind = "보호자 알림 발송",
        observedAt = "14:32:23",
        receivedAt = "14:32:23",
        outcome = "서버 접수",
        isSimulated = true
    )
)

fun defaultSeededUtterances(): List<UtteranceRecord> = listOf(
    UtteranceRecord(
        utteranceId = "UTT-1",
        incidentId = "DC-0142",
        observedAt = "14:32:08",
        speakerRole = SpeakerRole.PASSENGER_ESTIMATED,
        finalizedText = "운전하는 엄마가 대답을 안 해요.",
        isSimulated = true
    )
)
