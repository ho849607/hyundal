package com.hyundai.pleos.drivecheck

import com.hyundai.pleos.drivecheck.data.gateway.DefaultCallGateway
import com.hyundai.pleos.drivecheck.data.gateway.DefaultVehicleStateSource
import com.hyundai.pleos.drivecheck.domain.model.*
import com.hyundai.pleos.drivecheck.domain.reducer.*
import org.junit.Assert.*
import org.junit.Test

class IncidentReducerTest {

    @Test
    fun `direct 119 voice request skips CHECK and transitions to DIAL`() {
        val initial = DriveCheckUiState()
        val next = IncidentReducer.reduce(
            initial,
            IncidentAction.FinalizeSpeech(
                finalText = "119 전화",
                speakerRole = SpeakerRole.UNKNOWN_SPEAKER
            )
        )

        assertEquals(IncidentPhase.DIAL, next.phase)
        assertTrue(next.direct119Requested)
        assertFalse(next.timelineSteps.contains("상태 확인"))
        assertEquals(CallProgressState.DIALER_OPENED, next.callProgress)
    }

    @Test
    fun `passenger unresponsive report transitions to OFFER and keeps consciousness unconfirmed`() {
        val initial = DriveCheckUiState()
        val next = IncidentReducer.reduce(
            initial,
            IncidentAction.FinalizeSpeech(
                finalText = "운전하는 엄마가 대답을 안 해요.",
                speakerRole = SpeakerRole.CHILD_PASSENGER_ESTIMATED
            )
        )

        assertEquals(IncidentPhase.OFFER, next.phase)
        assertEquals("운전자가 반응하지 않아요.", next.passengerStatementText)
        assertNull("Unconfirmed consciousness must remain null", next.incident.driverConsciousnessConfirmed)
    }

    @Test
    fun `first and second no-reply move CHECK to RECHECK to UNKNOWN without auto dialing`() {
        var state = IncidentReducer.reduce(DriveCheckUiState(), IncidentAction.TriggerAmbiguousHelpRequest())
        assertEquals(IncidentPhase.CHECK, state.phase)

        state = IncidentReducer.reduce(state, IncidentAction.FirstCheckNoReply())
        assertEquals(IncidentPhase.RECHECK, state.phase)
        assertEquals(CallProgressState.IDLE, state.callProgress)
        assertNull(state.incident.driverConsciousnessConfirmed)

        state = IncidentReducer.reduce(state, IncidentAction.SecondCheckNoReply())
        assertEquals(IncidentPhase.UNKNOWN, state.phase)
        assertEquals("No auto-dial allowed on second no-reply", CallProgressState.IDLE, state.callProgress)
        assertNull("No-reply must not assume unconsciousness", state.incident.driverConsciousnessConfirmed)
    }

    @Test
    fun `negated smoke expression and TTS echo do not trigger emergency or fire guide`() {
        val initial = IncidentReducer.reduce(DriveCheckUiState(), IncidentAction.TriggerAmbiguousHelpRequest())

        // TTS echo must be ignored
        val afterEcho = IncidentReducer.reduce(
            initial,
            IncidentAction.FinalizeSpeech(
                finalText = "119 전화",
                isTtsEcho = true
            )
        )
        assertEquals(IncidentPhase.CHECK, afterEcho.phase)
        assertTrue(afterEcho.speech.isTtsEchoIgnored)

        // Negated expression "연기가 없어요" must be treated as safe response, NOT smoke report
        val afterNegatedSmoke = IncidentReducer.reduce(
            initial,
            IncidentAction.FinalizeSpeech(
                finalText = "연기가 없어요",
                isTtsEcho = false
            )
        )
        assertEquals(IncidentPhase.SAFE_CLOSED, afterNegatedSmoke.phase)
        assertEquals(GuideType.NONE, afterNegatedSmoke.guideType)
    }

    @Test
    fun `single 0 kmh sample or brake pedal alone does not confirm parked state`() {
        val vehicleSource = DefaultVehicleStateSource()

        // Moving 34 km/h
        vehicleSource.updateDrivingState(
            speedKmh = 34,
            gear = "D",
            parkingBrake = false,
            brakePedal = false
        )
        assertEquals(StopConfirmationState.MOVING, vehicleSource.vehicleState.value.stopConfirmation)
        assertFalse(vehicleSource.vehicleState.value.isAnimationAllowed)
        assertFalse(vehicleSource.vehicleState.value.isEditingAllowed)

        // Single 0 km/h sample with brake pedal pressed -> MUST remain UNKNOWN
        vehicleSource.updateDrivingState(
            speedKmh = 0,
            gear = "D",
            parkingBrake = false,
            brakePedal = true,
            isSingleZeroSample = true
        )
        assertEquals(StopConfirmationState.UNKNOWN, vehicleSource.vehicleState.value.stopConfirmation)
        assertFalse(vehicleSource.vehicleState.value.isAnimationAllowed)
        assertFalse(vehicleSource.vehicleState.value.isEditingAllowed)

        // Confirmed stop with Gear P and Parking Brake engaged
        vehicleSource.updateDrivingState(
            speedKmh = 0,
            gear = "P",
            parkingBrake = true,
            brakePedal = false,
            isSingleZeroSample = false
        )
        assertEquals(StopConfirmationState.CONFIRMED_STOPPED, vehicleSource.vehicleState.value.stopConfirmation)
        assertTrue(vehicleSource.vehicleState.value.isAnimationAllowed)
        assertTrue(vehicleSource.vehicleState.value.isEditingAllowed)
    }

    @Test
    fun `active official SOS prevents duplicate 119 call request`() {
        val gateway = DefaultCallGateway()
        gateway.setOfficialSosActive(true)
        assertFalse("Should not request duplicate 119 call when official SOS is active", gateway.requestEmergencyCall119())

        var state = IncidentReducer.reduce(DriveCheckUiState(), IncidentAction.SetOfficialSosActive(true))
        state = IncidentReducer.reduce(state, IncidentAction.Request119Call())
        assertEquals(IncidentPhase.CONNECTED, state.phase)
        assertEquals(CallProgressState.OFFICIAL_SOS_ACTIVE, state.callProgress)
    }

    @Test
    fun `guardian alert countdown and cancel are mutually exclusive`() {
        var state = IncidentReducer.reduce(DriveCheckUiState(), IncidentAction.StartGuardianCountdown())
        assertEquals(GuardianAlertStatus.COUNTDOWN, state.guardianAlertStatus)
        assertEquals(10, state.guardianCountdownSeconds)

        state = IncidentReducer.reduce(state, IncidentAction.TickGuardianCountdown)
        assertEquals(9, state.guardianCountdownSeconds)

        // Cancel before 0
        state = IncidentReducer.reduce(state, IncidentAction.CancelGuardianAlert())
        assertEquals(GuardianAlertStatus.CANCELLED, state.guardianAlertStatus)

        // Cancelled alert must not restart automatically
        val afterRetry = IncidentReducer.reduce(state, IncidentAction.StartGuardianCountdown())
        assertEquals(GuardianAlertStatus.CANCELLED, afterRetry.guardianAlertStatus)
    }

    @Test
    fun `default PDF export config excludes voice transcript and preserves original events on memo addition`() {
        var state = DriveCheckUiState()
        assertFalse("Voice transcript must be unchecked by default in PDF export", state.exportConfig.includePassengerVoiceTranscript)

        val originalEventCount = state.events.size
        state = IncidentReducer.reduce(state, IncidentAction.AddCorrectionMemo("동승자 진술 시각 정정 메모"))
        assertEquals("Original immutable events must not be mutated", originalEventCount, state.events.size)
        assertEquals(1, state.correctionMemos.size)
    }
}
