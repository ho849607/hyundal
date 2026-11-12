package com.hyundai.pleos.drivecheck.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundai.pleos.drivecheck.data.gateway.*
import com.hyundai.pleos.drivecheck.data.local.IncidentDatabaseHelper
import com.hyundai.pleos.drivecheck.data.pdf.IncidentPdfExporter
import com.hyundai.pleos.drivecheck.data.sdk.ReadState
import com.hyundai.pleos.drivecheck.data.sdk.VehicleBridge
import com.hyundai.pleos.drivecheck.domain.gateway.*
import com.hyundai.pleos.drivecheck.domain.model.*
import com.hyundai.pleos.drivecheck.domain.planner.MaintenanceRead
import com.hyundai.pleos.drivecheck.domain.reducer.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class DriveCheckViewModel(
    private val speechInput: SpeechInput = DefaultSpeechInput(),
    private val locationSource: LocationSource = DefaultLocationSource(),
    private val vehicleStateSource: VehicleStateSource = DefaultVehicleStateSource(),
    private val callGateway: CallGateway = DefaultCallGateway()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveCheckUiState())
    val uiState: StateFlow<DriveCheckUiState> = _uiState.asStateFlow()

    private var dbHelper: IncidentDatabaseHelper? = null
    private var vehicleBridge: VehicleBridge? = null
    private var guardianTimerJob: Job? = null
    private var voiceSimulationJob: Job? = null

    init {
        val probeReport = vehicleStateSource.probePlatformCapabilities()
        _uiState.update { it.copy(sdkInspectionReport = probeReport) }

        viewModelScope.launch {
            vehicleStateSource.vehicleState.collect { veh ->
                _uiState.update { it.copy(vehicle = veh) }
            }
        }
        viewModelScope.launch {
            locationSource.locationState.collect { loc ->
                _uiState.update { it.copy(location = loc) }
            }
        }
        viewModelScope.launch {
            speechInput.speechState.collect { sp ->
                _uiState.update { it.copy(speech = sp) }
            }
        }
    }

    fun attachDatabase(context: Context) {
        if (dbHelper == null) {
            val helper = IncidentDatabaseHelper(context.applicationContext)
            dbHelper = helper
            val incidentId = _uiState.value.incident.incidentId
            val loadedEvents = helper.loadEvents(incidentId)
            val loadedUtterances = helper.loadUtterances(incidentId)
            val loadedMemos = helper.loadCorrectionMemos(incidentId)
            val loadedExports = helper.loadExports(incidentId)
            _uiState.update {
                it.copy(
                    events = loadedEvents,
                    utterances = loadedUtterances,
                    correctionMemos = loadedMemos,
                    exportedVersions = loadedExports
                )
            }
        }
        if (vehicleBridge == null) {
            val bridge = VehicleBridge(context.applicationContext)
            vehicleBridge = bridge
            bridge.open()
                .onSuccess {
                    _uiState.update { it.copy(sdkInitLine = "SDK 초기화(Vehicle.initialize): 성공") }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(sdkInitLine = "SDK 초기화(Vehicle.initialize): 실패 (${VehicleBridge.describeError(err)})")
                    }
                }
        }
    }

    /**
     * 1단계: 앞왼쪽 도어 위치 조회로 SDK 연결을 확인합니다.
     * 실패하면 다른 메서드를 호출하지 않고 오류만 표시합니다.
     */
    fun checkDoorLinkOnly() {
        val bridge = vehicleBridge
        if (bridge == null) {
            _uiState.update { it.copy(sdkDoorLine = "앞왼쪽 도어 위치 조회: VehicleBridge 미초기화") }
            return
        }
        _uiState.update { it.copy(sdkDoorLine = "앞왼쪽 도어 위치 조회: 요청함, 응답 대기") }
        bridge.readFrontLeftDoor(
            onPosition = { pos ->
                _uiState.update {
                    it.copy(
                        sdkDoorLine = "앞왼쪽 도어 위치 조회: 응답 받음, 값 = $pos (연결 확인용, 정비 상태와 무관)"
                    )
                }
            },
            onFailed = { err ->
                _uiState.update {
                    it.copy(
                        sdkDoorLine = "앞왼쪽 도어 위치 조회: 실패 (${VehicleBridge.describeError(err)})"
                    )
                }
            }
        )
    }

    /**
     * 2단계: 도어 연결 확인 후 카탈로그의 typed API를 순서대로 조회합니다.
     * - 차량 주행 정보: 누적 주행거리
     * - 차량 안전 상태: 타이어 공기압, 경고등
     * - EV 및 연료: 배터리 잔량
     */
    fun readCatalogTypedApisSequentially() {
        val bridge = vehicleBridge ?: return
        bridge.readOdometerKm { odoState ->
            _uiState.update {
                it.copy(maintenanceRead = it.maintenanceRead.copy(odometerKm = odoState))
            }
            bridge.readLowTirePressure { tireState ->
                _uiState.update {
                    it.copy(maintenanceRead = it.maintenanceRead.copy(tirePressure = tireState))
                }
                bridge.readWarningLamps { lampState ->
                    _uiState.update {
                        it.copy(maintenanceRead = it.maintenanceRead.copy(warningLamps = lampState))
                    }
                    bridge.readBatterySocPercent { socState ->
                        _uiState.update {
                            it.copy(maintenanceRead = it.maintenanceRead.copy(batterySocPercent = socState))
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        vehicleBridge?.close()
        super.onCleared()
    }

    fun dispatch(action: IncidentAction) {
        val next = IncidentReducer.reduce(_uiState.value, action)
        _uiState.value = next
        persistCurrentState(next)

        if (action is IncidentAction.StartGuardianCountdown &&
            next.guardianAlertStatus == GuardianAlertStatus.COUNTDOWN
        ) {
            startGuardianCountdownTimer()
        } else if (action is IncidentAction.CancelGuardianAlert ||
            action is IncidentAction.ResetToDemoStart
        ) {
            guardianTimerJob?.cancel()
        }
    }

    private fun startGuardianCountdownTimer() {
        guardianTimerJob?.cancel()
        guardianTimerJob = viewModelScope.launch {
            while (_uiState.value.guardianAlertStatus == GuardianAlertStatus.COUNTDOWN &&
                _uiState.value.guardianCountdownSeconds > 0
            ) {
                delay(1000L)
                if (_uiState.value.guardianAlertStatus == GuardianAlertStatus.COUNTDOWN) {
                    dispatch(IncidentAction.TickGuardianCountdown)
                }
            }
        }
    }

    /**
     * UX-07: Simulates progressive ASR partial captions -> finalized utterance
     * ("운전하는 엄마가..." -> "운전하는 엄마가 대답을..." -> "운전하는 엄마가 대답을 안 해요.")
     */
    fun simulateProgressiveChildReport() {
        voiceSimulationJob?.cancel()
        voiceSimulationJob = viewModelScope.launch {
            speechInput.startListening(SpeakerRole.CHILD_PASSENGER_ESTIMATED)
            dispatch(IncidentAction.StartVoiceListening(SpeakerRole.CHILD_PASSENGER_ESTIMATED))
            delay(450L)
            speechInput.pushPartialTranscript("운전하는 엄마가…", 0.62f)
            dispatch(IncidentAction.UpdatePartialSpeech("운전하는 엄마가…", 0.62f))
            delay(550L)
            speechInput.pushPartialTranscript("운전하는 엄마가 대답을…", 0.78f)
            dispatch(IncidentAction.UpdatePartialSpeech("운전하는 엄마가 대답을…", 0.78f))
            delay(600L)
            val finalSentence = "운전하는 엄마가 대답을 안 해요."
            speechInput.pushFinalizedTranscript(finalSentence, SpeakerRole.CHILD_PASSENGER_ESTIMATED)
            dispatch(
                IncidentAction.UpdatePartialSpeech(
                    partialText = finalSentence,
                    inputLevel = 0.25f
                )
            )
            delay(750L)
            dispatch(
                IncidentAction.FinalizeSpeech(
                    finalText = finalSentence,
                    speakerRole = SpeakerRole.CHILD_PASSENGER_ESTIMATED
                )
            )
        }
    }

    fun simulateVoiceCommand(
        utterance: String,
        speakerRole: SpeakerRole = SpeakerRole.UNKNOWN_SPEAKER,
        isTtsEcho: Boolean = false
    ) {
        voiceSimulationJob?.cancel()
        speechInput.pushFinalizedTranscript(
            finalText = utterance,
            speakerRole = speakerRole,
            isTtsEcho = isTtsEcho
        )
        dispatch(
            IncidentAction.FinalizeSpeech(
                finalText = utterance,
                speakerRole = speakerRole,
                isTtsEcho = isTtsEcho
            )
        )
    }

    fun updateVehicleSimulation(
        speedKmh: Int,
        gear: String,
        parkingBrake: Boolean,
        brakePedal: Boolean,
        isSingleZeroSample: Boolean = false
    ) {
        vehicleStateSource.updateDrivingState(
            speedKmh = speedKmh,
            gear = gear,
            parkingBrake = parkingBrake,
            brakePedal = brakePedal,
            isSingleZeroSample = isSingleZeroSample
        )
    }

    fun updateSafetyTelemetrySimulation(
        seatbelt: Boolean?,
        airbagLamp: Boolean?,
        tirePressureNormal: Boolean?,
        hazardState: HazardLightState? = null
    ) {
        vehicleStateSource.updateSafetySignals(
            hazardState = hazardState,
            seatbeltSimulated = seatbelt,
            airbagSimulated = airbagLamp,
            tirePressureSimulated = tirePressureNormal
        )
    }

    fun setReduceMotion(enabled: Boolean) {
        vehicleStateSource.setReduceMotion(enabled)
    }

    fun updateLocationSimulation(
        status: LocationStatus,
        address: String? = "대구 달서구 월배로 120",
        observedAt: String = if (status == LocationStatus.STALE) "14:30" else "14:32"
    ) {
        locationSource.setSimulatedLocationState(
            status = status,
            address = address,
            observedAtText = observedAt,
            addressConversionSucceeded = address != null
        )
    }

    fun exportAndPreparePdf(context: Context, launchShareChooser: Boolean = false): File {
        val current = _uiState.value
        val nextVer = current.exportedVersions.size + 1
        val pdfFile = IncidentPdfExporter.generateIncidentPdf(context, current, nextVer)
        dispatch(IncidentAction.RecordPdfExported(pdfFile.absolutePath))
        if (launchShareChooser) {
            IncidentPdfExporter.launchSystemSharePdf(context, pdfFile, current.incident.incidentId)
        }
        return pdfFile
    }

    fun shareLatestExportedPdf(context: Context) {
        val latest = _uiState.value.exportedVersions.lastOrNull()
        val file = if (latest != null && File(latest.filePath).exists()) {
            File(latest.filePath)
        } else {
            exportAndPreparePdf(context, launchShareChooser = false)
        }
        IncidentPdfExporter.launchSystemSharePdf(context, file, _uiState.value.incident.incidentId)
    }

    private fun persistCurrentState(state: DriveCheckUiState) {
        dbHelper?.syncStateToDatabase(
            incident = state.incident,
            events = state.events,
            utterances = state.utterances,
            memos = state.correctionMemos,
            exports = state.exportedVersions
        )
    }
}
