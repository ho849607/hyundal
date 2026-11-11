package com.hyundai.pleos.drivecheck.data.gateway

import android.util.Log
import com.hyundai.pleos.drivecheck.domain.gateway.*
import com.hyundai.pleos.drivecheck.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DefaultSpeechInput : SpeechInput {
    private val _speechState = MutableStateFlow(SpeechSnapshot())
    override val speechState: StateFlow<SpeechSnapshot> = _speechState.asStateFlow()

    override fun startListening(speakerRole: SpeakerRole) {
        _speechState.value = SpeechSnapshot(
            isMicOpen = true,
            inputLevel = 0.45f,
            partialTranscript = "",
            finalizedTranscript = "",
            speakerRole = speakerRole,
            recognitionFailed = false,
            isTtsEchoIgnored = false,
            originMode = DataOriginMode.SIMULATION
        )
    }

    override fun pushPartialTranscript(partialText: String, level: Float) {
        _speechState.update {
            it.copy(
                isMicOpen = true,
                inputLevel = level.coerceIn(0f, 1f),
                partialTranscript = partialText,
                recognitionFailed = false
            )
        }
    }

    override fun pushFinalizedTranscript(
        finalText: String,
        speakerRole: SpeakerRole,
        isTtsEcho: Boolean,
        originMode: DataOriginMode
    ) {
        _speechState.value = SpeechSnapshot(
            isMicOpen = false,
            inputLevel = 0f,
            partialTranscript = "",
            finalizedTranscript = if (isTtsEcho) "" else finalText.trim(),
            speakerRole = speakerRole,
            recognitionFailed = false,
            isTtsEchoIgnored = isTtsEcho,
            originMode = originMode
        )
    }

    override fun triggerRecognitionFailure() {
        _speechState.value = SpeechSnapshot(
            isMicOpen = false,
            inputLevel = 0f,
            partialTranscript = "",
            finalizedTranscript = "",
            recognitionFailed = true,
            originMode = DataOriginMode.SIMULATION
        )
    }

    override fun stopListening() {
        _speechState.update { it.copy(isMicOpen = false, inputLevel = 0f) }
    }
}

class DefaultLocationSource : LocationSource {
    private val _locationState = MutableStateFlow(
        LocationSnapshot(
            status = LocationStatus.AVAILABLE,
            latitude = 35.8184,
            longitude = 128.5364,
            accuracyMeters = 8.5f,
            resolvedAddress = "대구 달서구 월배로 120",
            addressConversionSucceeded = true,
            observedAtText = "14:32",
            originMode = DataOriginMode.SIMULATION
        )
    )
    override val locationState: StateFlow<LocationSnapshot> = _locationState.asStateFlow()

    override fun refreshLocation() {
        val current = _locationState.value
        if (current.status == LocationStatus.AVAILABLE) {
            val nowText = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date())
            _locationState.value = current.copy(observedAtText = nowText)
        }
    }

    override fun setSimulatedLocationState(
        status: LocationStatus,
        address: String?,
        observedAtText: String,
        addressConversionSucceeded: Boolean
    ) {
        _locationState.value = LocationSnapshot(
            status = status,
            latitude = 35.8184,
            longitude = 128.5364,
            accuracyMeters = if (status == LocationStatus.AVAILABLE) 8.5f else 45.0f,
            resolvedAddress = if (addressConversionSucceeded) address else null,
            addressConversionSucceeded = addressConversionSucceeded,
            observedAtText = observedAtText,
            originMode = DataOriginMode.SIMULATION
        )
    }
}

class DefaultVehicleStateSource : VehicleStateSource {
    private val _vehicleState = MutableStateFlow(VehicleStateSnapshot())
    override val vehicleState: StateFlow<VehicleStateSnapshot> = _vehicleState.asStateFlow()

    override fun probePlatformCapabilities(): String {
        val hasAaosCarApi = try {
            Class.forName("android.car.Car")
            true
        } catch (_: Throwable) {
            false
        }

        val hasPleosVehicleSdk = try {
            Class.forName("ai.pleos.sdk.vehicle.Vehicle")
            true
        } catch (_: Throwable) {
            false
        }

        val status = when {
            hasPleosVehicleSdk -> SdkConnectionStatus.AAOS_READ_ONLY_ACTIVE
            hasAaosCarApi -> SdkConnectionStatus.AAOS_READ_ONLY_ACTIVE
            else -> SdkConnectionStatus.PLEOS_AAR_NOT_LINKED
        }

        _vehicleState.update { it.copy(sdkStatus = status) }

        val report = "M0 SDK Probe -> PleosAAR=$hasPleosVehicleSdk, AAOS_CarAPI=$hasAaosCarApi, Status=${status.name}"
        try {
            Log.i("DriveCheckM0", report)
        } catch (_: Throwable) {
            // Ignored in pure JVM unit tests
        }
        return report
    }

    override fun updateDrivingState(
        speedKmh: Int,
        gear: String,
        parkingBrake: Boolean,
        brakePedal: Boolean,
        isSingleZeroSample: Boolean,
        uxRestricted: Boolean
    ) {
        // Rule 2 & R-04:
        // - Single 0 km/h sample or brake pedal alone MUST NOT confirm park.
        // - Park is confirmed only when speed == 0, not a single transient sample,
        //   and either gear == "P" or parkingBrake is engaged.
        val stopConfirmation = when {
            speedKmh > 0 -> StopConfirmationState.MOVING
            isSingleZeroSample -> StopConfirmationState.UNKNOWN
            gear == "UNKNOWN" && !parkingBrake -> StopConfirmationState.UNKNOWN
            gear == "P" || parkingBrake -> StopConfirmationState.CONFIRMED_STOPPED
            else -> StopConfirmationState.UNKNOWN
        }

        val powertrainLabel = when (stopConfirmation) {
            StopConfirmationState.MOVING -> "주행 중 (${speedKmh} km/h · 기어 $gear)"
            StopConfirmationState.CONFIRMED_STOPPED -> "정차 확인됨 (기어 $gear · 주차브레이크 ${if (parkingBrake) "ON" else "OFF"})"
            StopConfirmationState.UNKNOWN -> "정차 미확인 (${speedKmh} km/h · 기어 $gear)"
        }

        _vehicleState.update { current ->
            current.copy(
                speedKmh = speedKmh,
                isSingleZeroSpeedSample = isSingleZeroSample,
                gearSelection = gear,
                parkingBrakeEngaged = parkingBrake,
                brakePedalPressed = brakePedal,
                stopConfirmation = stopConfirmation,
                uxRestrictionsActive = uxRestricted || stopConfirmation != StopConfirmationState.CONFIRMED_STOPPED,
                powertrainState = SupportedFieldStatus.Supported(powertrainLabel, DataOriginMode.SIMULATION)
            )
        }
    }

    override fun updateSafetySignals(
        crashSignal: Boolean?,
        overheatWarning: Boolean?,
        hazardState: HazardLightState?,
        seatbeltSimulated: Boolean?,
        airbagSimulated: Boolean?,
        tirePressureSimulated: Boolean?
    ) {
        _vehicleState.update { current ->
            current.copy(
                crashSignalReceived = crashSignal ?: current.crashSignalReceived,
                batteryOverheatWarning = overheatWarning ?: current.batteryOverheatWarning,
                hazardLightState = hazardState ?: current.hazardLightState,
                seatbeltFastened = when (seatbeltSimulated) {
                    null -> current.seatbeltFastened
                    else -> SupportedFieldStatus.Supported(seatbeltSimulated, DataOriginMode.SIMULATION)
                },
                airbagWarningLamp = when (airbagSimulated) {
                    null -> current.airbagWarningLamp
                    else -> SupportedFieldStatus.Supported(airbagSimulated, DataOriginMode.SIMULATION)
                },
                tirePressureNormal = when (tirePressureSimulated) {
                    null -> current.tirePressureNormal
                    else -> SupportedFieldStatus.Supported(tirePressureSimulated, DataOriginMode.SIMULATION)
                }
            )
        }
    }

    override fun setReduceMotion(enabled: Boolean) {
        _vehicleState.update { it.copy(reduceMotionEnabled = enabled) }
    }
}

class DefaultCallGateway : CallGateway {
    private val _callProgress = MutableStateFlow(CallProgressState.IDLE)
    override val callProgress: StateFlow<CallProgressState> = _callProgress.asStateFlow()

    private val _callAssistMode = MutableStateFlow(CallAssistMode.NONE)
    override val callAssistMode: StateFlow<CallAssistMode> = _callAssistMode.asStateFlow()

    private val _activeOperatorQuestion = MutableStateFlow("다친 사람이 있나요?")
    override val activeOperatorQuestion: StateFlow<String> = _activeOperatorQuestion.asStateFlow()

    private val _selectedReplyText = MutableStateFlow("")
    override val selectedReplyText: StateFlow<String> = _selectedReplyText.asStateFlow()

    override fun requestEmergencyCall119(): Boolean {
        // Rule 3 & FN-06: If verifiable official SOS is already active, do not duplicate call request.
        if (_callProgress.value == CallProgressState.OFFICIAL_SOS_ACTIVE) {
            return false
        }
        _callProgress.value = CallProgressState.REQUESTED
        return true
    }

    override fun markDialerOpened() {
        if (_callProgress.value != CallProgressState.OFFICIAL_SOS_ACTIVE) {
            _callProgress.value = CallProgressState.DIALER_OPENED
        }
    }

    override fun markCallConnected() {
        _callProgress.value = CallProgressState.CONNECTED
        _callAssistMode.value = CallAssistMode.ADDRESS_READY
    }

    override fun markCallFailed() {
        _callProgress.value = CallProgressState.FAILED
        _callAssistMode.value = CallAssistMode.NONE
    }

    override fun markCallUnknown() {
        _callProgress.value = CallProgressState.UNKNOWN
        _callAssistMode.value = CallAssistMode.NONE
    }

    override fun setOfficialSosActive(active: Boolean) {
        _callProgress.value = if (active) CallProgressState.OFFICIAL_SOS_ACTIVE else CallProgressState.IDLE
    }

    override fun setCallAssistMode(mode: CallAssistMode, question: String, reply: String) {
        _callAssistMode.value = mode
        if (question.isNotBlank()) {
            _activeOperatorQuestion.value = question
        }
        _selectedReplyText.value = reply
    }

    override fun endCall() {
        _callProgress.value = CallProgressState.IDLE
        _callAssistMode.value = CallAssistMode.NONE
        _selectedReplyText.value = ""
    }
}
