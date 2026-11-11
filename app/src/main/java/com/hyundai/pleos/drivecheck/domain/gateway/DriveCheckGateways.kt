package com.hyundai.pleos.drivecheck.domain.gateway

import com.hyundai.pleos.drivecheck.domain.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * M1: SpeechInput interface (Pleos SpeechToText / Android SpeechRecognizer / Simulation adapter).
 * Rule 1: Only finalized utterances are used for intent classification.
 */
interface SpeechInput {
    val speechState: StateFlow<SpeechSnapshot>

    fun startListening(speakerRole: SpeakerRole = SpeakerRole.UNKNOWN_SPEAKER)
    fun pushPartialTranscript(partialText: String, level: Float = 0.6f)
    fun pushFinalizedTranscript(
        finalText: String,
        speakerRole: SpeakerRole = SpeakerRole.UNKNOWN_SPEAKER,
        isTtsEcho: Boolean = false,
        originMode: DataOriginMode = DataOriginMode.SIMULATION
    )
    fun triggerRecognitionFailure()
    fun stopListening()
}

/**
 * M1: LocationSource interface (Pleos FusedLocation / Android LocationManager / Simulation adapter).
 * Rule 4: Tracks available | stale | denied | unavailable in parallel with call progression.
 */
interface LocationSource {
    val locationState: StateFlow<LocationSnapshot>

    fun refreshLocation()
    fun setSimulatedLocationState(
        status: LocationStatus,
        address: String? = "대구 달서구 월배로 120",
        observedAtText: String = "14:32",
        addressConversionSucceeded: Boolean = true
    )
}

/**
 * M1: VehicleStateSource interface (Pleos Vehicle SDK Brake / AAOS CarPropertyManager / Simulation adapter).
 * Rule 2: Enforces CarUxRestrictions and prevents single 0 km/h sample or brake pedal alone from confirming park.
 */
interface VehicleStateSource {
    val vehicleState: StateFlow<VehicleStateSnapshot>

    fun probePlatformCapabilities(): String
    fun updateDrivingState(
        speedKmh: Int,
        gear: String,
        parkingBrake: Boolean,
        brakePedal: Boolean,
        isSingleZeroSample: Boolean = false,
        uxRestricted: Boolean = speedKmh > 0
    )
    fun updateSafetySignals(
        crashSignal: Boolean? = null,
        overheatWarning: Boolean? = null,
        hazardState: HazardLightState? = null,
        seatbeltSimulated: Boolean? = null,
        airbagSimulated: Boolean? = null,
        tirePressureSimulated: Boolean? = null
    )
    fun setReduceMotion(enabled: Boolean)
}

/**
 * M1: CallGateway interface (Android Telecom / Official SOS check / Simulation adapter).
 * Rule 3: Separates requested -> dialer_opened -> connected | failed | unknown.
 * Safety Rule: Never executes real 119 call or external emergency transmission during demo/test.
 */
interface CallGateway {
    val callProgress: StateFlow<CallProgressState>
    val callAssistMode: StateFlow<CallAssistMode>
    val activeOperatorQuestion: StateFlow<String>
    val selectedReplyText: StateFlow<String>

    /**
     * Requests opening the 119 emergency call path.
     * If Official SOS is already active, returns false (no duplicate call request).
     */
    fun requestEmergencyCall119(): Boolean
    fun markDialerOpened()
    fun markCallConnected()
    fun markCallFailed()
    fun markCallUnknown()
    fun setOfficialSosActive(active: Boolean)
    fun setCallAssistMode(mode: CallAssistMode, question: String = "다친 사람이 있나요?", reply: String = "")
    fun endCall()
}
