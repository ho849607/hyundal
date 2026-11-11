package com.hyundai.pleos.drivecheck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyundai.pleos.drivecheck.R
import com.hyundai.pleos.drivecheck.domain.model.*
import com.hyundai.pleos.drivecheck.domain.reducer.DriveCheckUiState
import com.hyundai.pleos.drivecheck.domain.reducer.IncidentAction
import com.hyundai.pleos.drivecheck.ui.components.*
import com.hyundai.pleos.drivecheck.ui.theme.*
import com.hyundai.pleos.drivecheck.ui.viewmodel.DriveCheckViewModel

@Composable
fun DriveCheckMainScreen(
    viewModel: DriveCheckViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showSimControlPanel by remember { mutableStateOf(false) }
    var showSdkStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.attachDatabase(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgVehicleStart, BgVehicleEnd)
                )
            )
    ) {
        // Main content area: switches between 16:9 full vehicle layout and narrow 1-pane layout (16E)
        Box(
            modifier = if (state.isNarrowWindowMode) {
                Modifier
                    .width(460.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(BgVehicleStart, BgVehicleEnd)))
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            when (state.phase) {
                IncidentPhase.IDLE -> IdlePhaseView(state = state, viewModel = viewModel)
                IncidentPhase.LISTENING -> ListeningPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.CHECK -> CheckPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.RECHECK -> RecheckPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.UNKNOWN -> UnknownDriverPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.OFFER -> PassengerOfferPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.DIAL -> DialingPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.CONNECTED -> ConnectedLocationPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.FALLBACK -> FallbackPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.SAFE_CLOSED -> SafeClosedPhaseView(state = state, viewModel = viewModel)
                IncidentPhase.GUIDE -> GuidePhaseView(state = state, viewModel = viewModel)
                IncidentPhase.RECORD -> RecordPhaseView(
                    state = state,
                    viewModel = viewModel,
                    onExportPdf = { launchShare ->
                        viewModel.exportAndPreparePdf(context, launchShareChooser = launchShare)
                    },
                    onSharePdf = {
                        viewModel.shareLatestExportedPdf(context)
                    }
                )
            }
        }

        // Top-right non-intrusive simulation & SDK status bar (clearly marks mock vs real data per MVP rules)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD9102533),
                modifier = Modifier.clickable { showSdkStatusDialog = true }
            ) {
                Text(
                    text = "차량/SDK 상태 (${state.vehicle.stopConfirmation.name})",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (showSimControlPanel) ActiveOrangeDot else TextCategoryTeal,
                modifier = Modifier.clickable { showSimControlPanel = !showSimControlPanel }
            ) {
                Text(
                    text = if (showSimControlPanel) "시연 제어판 닫기 ▲" else "시연 제어판 (P0~P2) ▼",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Collapsible simulation & verification drawer
        AnimatedVisibility(
            visible = showSimControlPanel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            SimulationControlDrawer(
                state = state,
                viewModel = viewModel,
                onExportPdf = {
                    viewModel.exportAndPreparePdf(context, launchShareChooser = false)
                }
            )
        }

        if (showSdkStatusDialog) {
            VehicleSdkTelemetryDialog(
                state = state,
                viewModel = viewModel,
                onDismiss = { showSdkStatusDialog = false }
            )
        }
    }
}

/**
 * 03A manual_home.png & 03C suggest.png & 16E narrow.png
 */
@Composable
private fun IdlePhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    if (state.isNarrowWindowMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "위험 감지 · 충돌 신호",
                    color = TextCategoryTeal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "도움이 필요하세요?",
                    color = TextPrimaryDark,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.simulateProgressiveChildReport()
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.asset_mic),
                        contentDescription = "마이크",
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(TextCategoryTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "”도와줘”",
                        color = TextSecondarySlate,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Ioniq5VehicleView(
                showRadarCircles = false,
                isAnimationAllowed = state.vehicle.isAnimationAllowed,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Emergency119RectButton(
                onClick = {
                    viewModel.dispatch(IncidentAction.Request119Call(isDirectFromIdle = true))
                }
            )
        }
        return
    }

    if (state.showStoppedContactSelector) {
        // 03C suggest.png: 정차 후 필요한 연락을 고를 때
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "연락 선택 · 정차 확인",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "119에 전화할까요?",
                    color = TextPrimaryDark,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.simulateVoiceCommand("119 전화")
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.asset_mic),
                        contentDescription = "음성",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(TextCategoryTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "”119 전화”",
                        color = TextSecondarySlate,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(26.dp))
                Emergency119CircleButton(
                    onClick = {
                        viewModel.dispatch(IncidentAction.Request119Call(isDirectFromIdle = true))
                    },
                    modifier = Modifier.padding(start = 72.dp)
                )
            }

            OtherContactsGlassPanel(
                showTowAndInsurance = state.vehicle.stopConfirmation == StopConfirmationState.CONFIRMED_STOPPED,
                onGuardianClick = {
                    viewModel.dispatch(IncidentAction.StartGuardianCountdown())
                },
                onPolice112Click = {
                    viewModel.dispatch(IncidentAction.TriggerGuideScenario(GuideType.ROUTE_POLICE_112))
                },
                onTowClick = {
                    viewModel.dispatch(IncidentAction.TriggerGuideScenario(GuideType.ROUTE_TOW))
                },
                onInsuranceClick = {
                    viewModel.dispatch(IncidentAction.TriggerGuideScenario(GuideType.ROUTE_INSURANCE))
                }
            )
        }
        return
    }

    // 03A manual_home.png: 무엇을 도와드릴까요?
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Text(
                    text = "무엇을 도와드릴까요?",
                    color = TextPrimaryDark,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.dispatch(IncidentAction.StartVoiceListening())
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.asset_mic),
                        contentDescription = "마이크",
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(TextCategoryTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "”도와줘”",
                        color = TextSecondarySlate,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Text(
                        text = "”119 전화”",
                        color = TextSecondarySlate,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            viewModel.simulateVoiceCommand("119 전화")
                        }
                    )
                    Text(
                        text = "”보호자 알림”",
                        color = TextSecondarySlate,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            viewModel.dispatch(IncidentAction.StartGuardianCountdown())
                            viewModel.dispatch(IncidentAction.TriggerAmbiguousHelpRequest())
                        }
                    )
                    Text(
                        text = "”동승자 제보 시연”",
                        color = TextCategoryTeal,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            viewModel.simulateProgressiveChildReport()
                        }
                    )
                }
            }

            Emergency119RectButton(
                onClick = {
                    viewModel.dispatch(IncidentAction.Request119Call(isDirectFromIdle = true))
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            MochiCharacterView(
                isMicOpen = false,
                inputLevel = 0f,
                isAnimationAllowed = state.vehicle.isAnimationAllowed,
                characterSize = 230.dp,
                modifier = Modifier.clickable {
                    viewModel.dispatch(IncidentAction.StartVoiceListening())
                }
            )
        }
    }
}

/**
 * 17A listen_0.png ~ 17D listen_3.png & 16A recognition_retry.png
 */
@Composable
private fun ListeningPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    if (state.speech.recognitionFailed) {
        // 16A recognition_retry.png
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "음성 인식",
                        color = TextCategoryTeal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "다시 말해 주세요.",
                        color = TextPrimaryDark,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.simulateVoiceCommand("119 전화")
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.asset_mic),
                            contentDescription = "마이크",
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(TextCategoryTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "”119 전화”",
                            color = TextSecondarySlate,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Emergency119RectButton(
                    onClick = {
                        viewModel.dispatch(IncidentAction.Request119Call(isDirectFromIdle = true))
                    }
                )
            }

            Box(
                modifier = Modifier.weight(0.9f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.asset_mic),
                    contentDescription = "음성 재시도",
                    modifier = Modifier
                        .size(110.dp)
                        .clickable {
                            viewModel.dispatch(IncidentAction.StartVoiceListening())
                        },
                    colorFilter = ColorFilter.tint(TextCategoryTeal)
                )
            }
        }
        return
    }

    val activeCaption = state.speech.partialTranscript.ifBlank { state.speech.finalizedTranscript }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // UX-04: Orange indicator dot at top-right ONLY when mic is open
        if (state.speech.isMicOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 12.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(ActiveOrangeDot)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (activeCaption.isBlank()) "음성으로 요청하세요" else "",
                color = TextCategoryTeal,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )

            MochiCharacterView(
                isMicOpen = state.speech.isMicOpen,
                inputLevel = state.speech.inputLevel,
                isAnimationAllowed = state.vehicle.isAnimationAllowed,
                characterSize = 215.dp,
                showWaveform = activeCaption.isBlank() || state.speech.partialTranscript.isNotBlank()
            )

            if (activeCaption.isNotBlank()) {
                // 17D listen_3.png / 18A listen_unknown.png
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    Text(
                        text = state.speech.speakerRole.displayLabel,
                        color = TextSecondarySlate,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = activeCaption,
                        color = TextPrimaryDark,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // 17A listen_0.png: Voice command examples (all interactive!)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 18.dp)
                ) {
                    Text(
                        text = "”119 전화”",
                        color = TextPrimaryDark,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.simulateVoiceCommand("119 전화")
                        }
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VoiceQuoteSuggestion("”보호자 알림”") {
                            viewModel.simulateVoiceCommand("보호자 알림")
                        }
                        VoiceQuoteSuggestion("”112 전화”") {
                            viewModel.simulateVoiceCommand("112 전화")
                        }
                        VoiceQuoteSuggestion("”견인 연락”") {
                            viewModel.simulateVoiceCommand("견인 연락")
                        }
                        VoiceQuoteSuggestion("”보험사 연락”") {
                            viewModel.simulateVoiceCommand("보험사 연락")
                        }
                        VoiceQuoteSuggestion("”엄마가 대답을 안 해요”") {
                            viewModel.simulateProgressiveChildReport()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceQuoteSuggestion(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = TextSecondarySlate,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

/**
 * 04A flow_detect.png (△ 충돌 신호) & 04B flow_check.png (괜찮으세요?)
 */
@Composable
private fun CheckPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    val infiniteTransition = rememberInfiniteTransition(label = "titlePulse")
    val titleAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 810, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleBrightness"
    )
    val effectiveTitleAlpha = if (state.vehicle.isAnimationAllowed) titleAlpha else 1.0f

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "긴급 대응",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (state.vehicle.crashSignalReceived) "⚠ 충돌 신호" else "괜찮으세요?",
                    color = TextPrimaryDark,
                    fontSize = if (state.vehicle.crashSignalReceived) 36.sp else 42.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(effectiveTitleAlpha)
                )
            }

            Ioniq5VehicleView(
                showRadarCircles = state.vehicle.crashSignalReceived,
                isAnimationAllowed = state.vehicle.isAnimationAllowed,
                modifier = Modifier.padding(start = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Emergency119RectButton(
                    onClick = {
                        viewModel.dispatch(IncidentAction.Request119Call())
                    }
                )
                if (!state.vehicle.crashSignalReceived) {
                    GlassActionButton(
                        text = "괜찮아요",
                        onClick = {
                            viewModel.dispatch(IncidentAction.DriverRespondedSafe())
                        }
                    )
                } else {
                    GlassActionButton(
                        text = "상태 확인",
                        onClick = {
                            viewModel.dispatch(IncidentAction.TriggerAmbiguousHelpRequest())
                        }
                    )
                }
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 04D driver_no_reply.png: 아직 응답을 듣지 못했어요.
 */
@Composable
private fun RecheckPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "상태 확인 · 미응답",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "아직 응답을 듣지 못했어요.",
                    color = TextPrimaryDark,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "괜찮으면 말하거나 버튼을 눌러 주세요.",
                    color = TextSecondarySlate,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "동승자는 운전자 상태를 말해 주세요.",
                    color = TextSecondarySlate,
                    fontSize = 20.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Emergency119RectButton(
                    onClick = {
                        viewModel.dispatch(IncidentAction.Request119Call())
                    }
                )
                GlassActionButton(
                    text = "괜찮아요",
                    onClick = {
                        viewModel.dispatch(IncidentAction.DriverRespondedSafe())
                    }
                )
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 05A driver_no_reply_repeated.png: 운전자 상태를 확인할 수 없어요.
 */
@Composable
private fun UnknownDriverPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "상태 확인 · 미응답",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "운전자 상태를 확인할 수 없어요.",
                    color = TextPrimaryDark,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "동승자는 운전자 상태를 말해 주세요.",
                    color = TextSecondarySlate,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "※ 단순 미응답만으로 의식불명 판단이나 자동 발신을 수행하지 않습니다.",
                    color = TextMutedStep,
                    fontSize = 13.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Emergency119RectButton(
                    onClick = {
                        viewModel.dispatch(IncidentAction.Request119Call())
                    }
                )
                GlassActionButton(
                    text = "괜찮아요",
                    onClick = {
                        viewModel.dispatch(IncidentAction.DriverRespondedSafe())
                    }
                )
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 05C driver_unresponsive.png: 동승자 제보 · 의식 상태 미확인 / “운전자가 반응하지 않아요.” + 큰 원형 119
 */
@Composable
private fun PassengerOfferPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "동승자 제보 · 의식 상태 미확인",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "”${state.passengerStatementText.ifBlank { "운전자가 반응하지 않아요." }}”",
                    color = TextPrimaryDark,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.dispatch(IncidentAction.Request119Call(byVoice = true))
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.asset_mic),
                        contentDescription = "음성",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(TextCategoryTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "”119 전화”",
                        color = TextSecondarySlate,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Emergency119CircleButton(
                onClick = {
                    viewModel.dispatch(IncidentAction.Request119Call(byVoice = false))
                },
                modifier = Modifier.padding(start = 96.dp, bottom = 8.dp)
            )
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 05D flow_direct.png & 06B flow_dial.png: 전화 화면 열림
 */
@Composable
private fun DialingPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "긴급 대응",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(34.dp))
                Text(
                    text = "전화 화면 열림",
                    color = TextPrimaryDark,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "시스템 긴급전화 발신 경로 요청됨 (시연 모의 상태 · 실제 119 미발신)",
                    color = TextMutedStep,
                    fontSize = 15.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassActionButton(
                    text = "연결 확인 (시연)",
                    onClick = { viewModel.dispatch(IncidentAction.CallConnectedConfirmed()) },
                    width = 148.dp,
                    height = 54.dp
                )
                GlassActionButton(
                    text = "연결 실패 (06D)",
                    onClick = { viewModel.dispatch(IncidentAction.CallConnectionFailed()) },
                    width = 148.dp,
                    height = 54.dp
                )
                GlassActionButton(
                    text = "연결 미확인 (07B)",
                    onClick = { viewModel.dispatch(IncidentAction.CallConnectionUnknown()) },
                    width = 154.dp,
                    height = 54.dp
                )
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 06C flow_location.png & 07C flow_stale.png & 07D/08B/08C/09A-09D Call Assist
 */
@Composable
private fun ConnectedLocationPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.28f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (state.callProgress == CallProgressState.OFFICIAL_SOS_ACTIVE) {
                        "공식 SOS 통화 진행 중 (중복 발신 방지됨)"
                    } else {
                        "119 통화 중"
                    },
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(22.dp))

                when {
                    state.callAssistMode == CallAssistMode.OPERATOR_QUESTION -> {
                        // 09B assist_question.png
                        Text(
                            text = "상황요원 질문",
                            color = TextSecondarySlate,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "”${state.operatorQuestion}”",
                            color = TextPrimaryDark,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    state.callAssistMode == CallAssistMode.SPEAKING_SELECTED_REPLY -> {
                        // 09C assist_answer.png
                        Text(
                            text = "회선에 답변 음성 전달 중",
                            color = TextCategoryTeal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "”탑승자가 '${state.selectedAssistAnswer}'라고 답했습니다.”",
                            color = TextPrimaryDark,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    state.callAssistMode == CallAssistMode.NO_REPLY_ANNOUNCED -> {
                        // 09D assist_no_reply.png
                        Text(
                            text = "자동 답변 · 응답 미확인 1회 전달",
                            color = TextCategoryTeal,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "”현재 탑승자 답변을 확인하지 못했습니다.”",
                            color = TextPrimaryDark,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    state.callAssistMode == CallAssistMode.SPEAKING_ADDRESS -> {
                        // 08B flow_call_speaking.png
                        Text(
                            text = "주소 전달 중",
                            color = TextCategoryTeal,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.location.resolvedAddress ?: "대구 달서구 월배로 120",
                            color = TextPrimaryDark,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    state.callAssistMode == CallAssistMode.DIRECT_SPEAKING -> {
                        // 08C flow_call_return.png
                        Text(
                            text = "직접 통화 중 (AI 합성 발언 중단됨)",
                            color = TextPrimaryDark,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.location.resolvedAddress ?: "대구 달서구 월배로 120",
                            color = TextSecondarySlate,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    state.location.status == LocationStatus.STALE -> {
                        // 07C flow_stale.png
                        Text(
                            text = "최신 위치 미확인",
                            color = TextPrimaryDark,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "마지막 확인 · ${state.location.observedAtText}",
                            color = TextSecondarySlate,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.location.resolvedAddress ?: "대구 달서구 월배로 120",
                            color = TextPrimaryDark,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "주변 표지판을 확인해 주세요.",
                            color = TextSecondarySlate,
                            fontSize = 20.sp
                        )
                    }

                    state.location.status == LocationStatus.DENIED ||
                        state.location.status == LocationStatus.UNAVAILABLE -> {
                        Text(
                            text = "위치 확인 불가 (${state.location.status.name})",
                            color = TextPrimaryDark,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "주변 도로명이나 표지판을 상황요원에게 직접 말해 주세요.",
                            color = TextSecondarySlate,
                            fontSize = 20.sp
                        )
                    }

                    else -> {
                        // 06C flow_location.png
                        val addr = state.location.resolvedAddress ?: "대구 달서구 월배로 120"
                        val lines = if (addr == "대구 달서구 월배로 120") {
                            "대구 달서구\n월배로 120"
                        } else {
                            addr
                        }
                        Text(
                            text = lines,
                            color = TextPrimaryDark,
                            fontSize = 44.sp,
                            lineHeight = 54.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Text(
                            text = "이 주소를 말해 주세요.",
                            color = TextSecondarySlate,
                            fontSize = 21.sp
                        )
                    }
                }
            }

            // UX-13 & Section 6: Call Assist & Next Step Action Row
            if (state.callAssistMode == CallAssistMode.OPERATOR_QUESTION) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassActionButton(
                        text = "직접 말하기",
                        iconRes = R.drawable.asset_mic,
                        onClick = { viewModel.dispatch(IncidentAction.SwitchToDirectSpeaking()) },
                        width = 148.dp,
                        height = 56.dp
                    )
                    GlassActionButton(
                        text = "있어요",
                        onClick = { viewModel.dispatch(IncidentAction.SelectCallAssistAnswer("있어요")) },
                        width = 110.dp,
                        height = 56.dp
                    )
                    GlassActionButton(
                        text = "없어요",
                        onClick = { viewModel.dispatch(IncidentAction.SelectCallAssistAnswer("없어요")) },
                        width = 110.dp,
                        height = 56.dp
                    )
                    GlassActionButton(
                        text = "잘 모르겠어요",
                        onClick = { viewModel.dispatch(IncidentAction.SelectCallAssistAnswer("잘 모르겠어요")) },
                        width = 136.dp,
                        height = 56.dp
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassActionButton(
                        text = "통화에 주소 전달",
                        iconRes = R.drawable.asset_speaker,
                        onClick = { viewModel.dispatch(IncidentAction.StartSpeakingAddressInCall()) },
                        width = 172.dp,
                        height = 56.dp
                    )
                    GlassActionButton(
                        text = "대신 답하기",
                        iconRes = R.drawable.asset_mic,
                        onClick = { viewModel.dispatch(IncidentAction.OpenCallAssist()) },
                        width = 148.dp,
                        height = 56.dp
                    )
                    GlassActionButton(
                        text = "기록 저장 완료",
                        iconRes = R.drawable.asset_receipt,
                        onClick = { viewModel.dispatch(IncidentAction.CompleteAndSaveRecord()) },
                        width = 156.dp,
                        height = 56.dp
                    )
                }
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.72f)
        )
    }
}

/**
 * 06D flow_failed.png (연결 실패) & 07B flow_unknown.png (연결 미확인)
 */
@Composable
private fun FallbackPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    val isFailed = state.callProgress == CallProgressState.FAILED

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "긴급 대응",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = if (isFailed) "연결 실패" else "연결 미확인",
                    color = TextPrimaryDark,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = if (isFailed) {
                        "휴대폰에서 119에 전화하세요."
                    } else {
                        "전화 화면을 확인해 주세요."
                    },
                    color = TextSecondarySlate,
                    fontSize = 21.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Emergency119RectButton(
                    onClick = { viewModel.dispatch(IncidentAction.Request119Call()) },
                    text = "다시 시도"
                )
                GlassActionButton(
                    text = "사건 기록 저장",
                    iconRes = R.drawable.asset_receipt,
                    onClick = { viewModel.dispatch(IncidentAction.CompleteAndSaveRecord()) }
                )
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 04C safe_answer.png: 응답 확인 / 응답을 확인했어요. + 닫기
 */
@Composable
private fun SafeClosedPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "응답 확인",
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "응답을 확인했어요.",
                    color = TextPrimaryDark,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassActionButton(
                    text = "닫기",
                    onClick = { viewModel.dispatch(IncidentAction.DismissSafeScreenToIdle) },
                    width = 148.dp
                )
                GlassActionButton(
                    text = "사건 기록 보기",
                    iconRes = R.drawable.asset_receipt,
                    onClick = {
                        viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE))
                    },
                    width = 180.dp
                )
            }
        }

        Box(
            modifier = Modifier.weight(0.9f),
            contentAlignment = Alignment.Center
        ) {
            MochiCharacterView(
                isMicOpen = false,
                inputLevel = 0f,
                isAnimationAllowed = state.vehicle.isAnimationAllowed,
                characterSize = 205.dp
            )
        }
    }
}

/**
 * S-09, S-10, S-11, S-13, S-21 Guide screens (Overheat, Fire, Driver Illness, OEM Emergency Stop, Medical Card, Tow, Insurance)
 */
@Composable
private fun GuidePhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel
) {
    val (category, headline, bodyText) = when (state.guideType) {
        GuideType.DRIVER_ILLNESS_STOP -> Triple(
            "운전 곤란 진술 · 05B",
            "안전한 곳에 정차하세요.",
            "비상등을 켜고 갓길 등 안전한 곳에 정차해 주세요. 위급하면 바로 119에 연결합니다."
        )
        GuideType.BATTERY_OVERHEAT_ONLY -> Triple(
            "차량 경고 · 고전압 배터리 과열 (12A)",
            "안전한 곳에 정차하세요.",
            "차종 매뉴얼에 따라 즉시 안전하게 정차하고 차량 상태를 확인해 주세요."
        )
        GuideType.FIRE_MOVING -> Triple(
            "화재 의심 · 주행 중 (12B)",
            "즉시 안전 정차 후 대피하세요.",
            "주행 중 연기·화재가 신고되었습니다. 즉시 안전 정차한 뒤 차량에서 멀리 떨어지세요."
        )
        GuideType.FIRE_STOPPED -> Triple(
            "화재 의심 · 정차 확인됨 (12C)",
            "주변을 살피고 차량에서 떨어지세요.",
            "정차가 확인되었습니다. 시동을 끄고 차량 밖 안전한 곳으로 대피해 119에 신고하세요."
        )
        GuideType.FIRE_STOP_UNKNOWN -> Triple(
            "화재 의심 · 정차 여부 미확인 (12D)",
            "주행 중이면 정차하고, 정차 후에는 대피하세요.",
            "차량 정차 상태가 아직 확인되지 않았습니다 (0 km/h 단일 샘플 미확정). 안전 확보 후 대피하세요."
        )
        GuideType.OEM_AUTO_STOP_STARTED -> Triple(
            "제조사 안전 기능 · 시연 모의 신호 (11A)",
            "긴급 정차 작동 신호 수신",
            "제조사 Emergency Stop 작동 신호를 수신했습니다 (시연 데이터)."
        )
        GuideType.OEM_DECELERATING -> Triple(
            "제조사 안전 기능 · 시연 모의 신호 (11B)",
            "감속 상태 확인됨",
            "차량이 감속 중입니다. 정차 완료 회신을 기다립니다."
        )
        GuideType.OEM_STOP_CONFIRMED -> Triple(
            "제조사 안전 기능 · 시연 모의 신호 (11C)",
            "정차 완료 확인됨",
            "제조사 정차 완료 회신이 확인되었습니다."
        )
        GuideType.OEM_AUTO_HAZARD_CONFIRMED -> Triple(
            "제조사 안전 기능 · 시연 모의 신호 (11D)",
            "정차 · 비상등 자동 점등 확인",
            "정차 완료 및 비상등 자동 점등 출처가 확인되었습니다."
        )
        GuideType.MEDICAL_CARD_VIEW -> Triple(
            "사전 등록 의료 정보 카드 (14B)",
            state.medicalCard.drugAllergies,
            "복용약: ${state.medicalCard.medications}\n진료 병원: ${state.medicalCard.hospitalName} (${state.medicalCard.hospitalPhone})\n메모: ${state.medicalCard.notes}"
        )
        GuideType.ROUTE_POLICE_112 -> Triple(
            "별도 경찰 신고 · 112 연결 (14C)",
            "112 교통 통제 및 사고 신고",
            "진행 중인 119 대응을 유지하면서 112 신고 경로를 안내합니다 (시연 모의 안내)."
        )
        GuideType.ROUTE_TOW -> Triple(
            "안전 정차 확인 후 · 견인 연락 (15B)",
            state.contactsConfig.towCompanyName,
            "등록 전화번호: ${state.contactsConfig.towPhone} (안전 정차 확인 완료)"
        )
        GuideType.ROUTE_INSURANCE -> Triple(
            "안전 정차 확인 후 · 보험사 연락 (15C)",
            state.contactsConfig.insurerName,
            "등록 전화번호: ${state.contactsConfig.insurerPhone} · 정차 후 사건 대응 보고서 PDF를 공유할 수 있습니다."
        )
        else -> Triple(
            "상황별 대응 가이드",
            "안전 상태를 확인해 주세요.",
            "현재 상황에 맞는 다음 행동을 선택해 주세요."
        )
    }

    val useTopView = state.guideType in setOf(
        GuideType.OEM_AUTO_STOP_STARTED,
        GuideType.OEM_DECELERATING,
        GuideType.OEM_STOP_CONFIRMED,
        GuideType.OEM_AUTO_HAZARD_CONFIRMED
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = category,
                    color = TextCategoryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = headline,
                    color = TextPrimaryDark,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = bodyText,
                    color = TextSecondarySlate,
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                )
            }

            if (useTopView) {
                Ioniq5VehicleView(
                    showRadarCircles = true,
                    isAnimationAllowed = state.vehicle.isAnimationAllowed,
                    useTopView = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Emergency119RectButton(
                    onClick = { viewModel.dispatch(IncidentAction.Request119Call()) }
                )
                GlassActionButton(
                    text = "사건 대응 보고서",
                    iconRes = R.drawable.asset_receipt,
                    onClick = {
                        viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE))
                    },
                    width = 190.dp
                )
            }
        }

        RightProgressionTimeline(
            steps = state.timelineSteps,
            activeIndex = state.activeStepIndex,
            isAnimationAllowed = state.vehicle.isAnimationAllowed,
            guardianStatus = state.guardianAlertStatus,
            guardianCountdownSec = state.guardianCountdownSeconds,
            onCancelGuardian = { viewModel.dispatch(IncidentAction.CancelGuardianAlert()) },
            modifier = Modifier.weight(0.75f)
        )
    }
}

/**
 * 20A flow_done.png, 20B report_vehicle.png, 20C record_voice.png, 21C mobile_export.png, 21D mobile_exported.png
 */
@Composable
private fun RecordPhaseView(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel,
    onExportPdf: (Boolean) -> Unit,
    onSharePdf: () -> Unit
) {
    when (state.recordSubView) {
        RecordSubView.DONE_SUMMARY -> {
            // 20A flow_done.png: 기록 저장됨 + 사건 보기
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 56.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "긴급 대응",
                            color = TextCategoryTeal,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "기록 저장됨",
                            color = TextPrimaryDark,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassActionButton(
                        text = "사건 보기",
                        iconRes = R.drawable.asset_receipt,
                        onClick = {
                            viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE))
                        },
                        width = 188.dp
                    )
                }

                RightProgressionTimeline(
                    steps = state.timelineSteps,
                    activeIndex = state.activeStepIndex,
                    isAnimationAllowed = state.vehicle.isAnimationAllowed,
                    guardianStatus = state.guardianAlertStatus,
                    guardianCountdownSec = state.guardianCountdownSeconds,
                    modifier = Modifier.weight(0.75f)
                )
            }
        }

        RecordSubView.VEHICLE_REPORT_TABLE -> {
            // 20B report_vehicle.png: 사건 대응 보고서
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 56.dp, vertical = 34.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "사건 대응 보고서",
                        color = TextPrimaryDark,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(text = state.incident.incidentId, color = TextSecondarySlate, fontSize = 18.sp)
                        Text(text = state.incident.startedAtKst, color = TextSecondarySlate, fontSize = 18.sp)
                        Text(text = state.incident.summaryRegion, color = TextSecondarySlate, fontSize = 18.sp)
                        Text(
                            text = if (state.incident.isSimulationRecord) "시연 기록" else "실측 기록",
                            color = TextSecondarySlate,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text("시각", color = TextMutedStep, fontSize = 15.sp, modifier = Modifier.weight(0.18f))
                        Text("출처", color = TextMutedStep, fontSize = 15.sp, modifier = Modifier.weight(0.22f))
                        Text("기록 내용", color = TextMutedStep, fontSize = 15.sp, modifier = Modifier.weight(0.42f))
                        Text("상태", color = TextMutedStep, fontSize = 15.sp, modifier = Modifier.weight(0.18f))
                    }

                    // Table rows
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.events.forEach { ev ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ev.observedAt,
                                    color = TextPrimaryDark,
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.18f)
                                )
                                Text(
                                    text = ev.source.korLabel,
                                    color = TextSecondarySlate,
                                    fontSize = 17.sp,
                                    modifier = Modifier.weight(0.22f)
                                )
                                Text(
                                    text = ev.kind,
                                    color = TextPrimaryDark,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(0.42f)
                                )
                                Text(
                                    text = ev.outcome,
                                    color = TextCategoryTeal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.18f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassActionButton(
                        text = "음성 자막 보기",
                        iconRes = R.drawable.asset_record,
                        onClick = {
                            viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VOICE_TRANSCRIPT_DETAIL))
                        },
                        width = 210.dp
                    )

                    if (state.vehicle.isEditingAllowed) {
                        GlassActionButton(
                            text = "휴대폰에서 열기 (PDF 내보내기)",
                            iconRes = R.drawable.asset_mobile,
                            onClick = {
                                viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.EXPORT_PDF_CONFIG))
                            },
                            width = 250.dp
                        )
                    } else {
                        Text(
                            text = "※ 주행 중 또는 정차 미확인 시 내보내기/편집이 제한됩니다 (CarUxRestrictions).",
                            color = EmergencyRedStart,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        RecordSubView.VOICE_TRANSCRIPT_DETAIL -> {
            // 20C record_voice.png: 음성 자막 기록 + 사후 정정 메모
            var memoDraft by remember { mutableStateOf("") }
            val latestUtt = state.utterances.lastOrNull()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 56.dp, vertical = 38.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "사건 ${state.incident.incidentId}",
                        color = TextCategoryTeal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "음성 자막 기록",
                        color = TextPrimaryDark,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "${latestUtt?.observedAt ?: "14:32:08"}  ·  ${latestUtt?.speakerRole?.displayLabel ?: "동승자 추정"}",
                        color = TextSecondarySlate,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "”${latestUtt?.finalizedText ?: "운전하는 엄마가 대답을 안 해요."}”",
                        color = TextPrimaryDark,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    // Rule 5 & FN-10: Separate user correction memo without mutating original event/utterance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = memoDraft,
                            onValueChange = { memoDraft = it },
                            label = { Text("정정 메모 추가 (원본과 별도 저장)") },
                            singleLine = true,
                            modifier = Modifier.width(360.dp)
                        )
                        GlassActionButton(
                            text = "메모 저장",
                            onClick = {
                                if (memoDraft.isNotBlank()) {
                                    viewModel.dispatch(IncidentAction.AddCorrectionMemo(memoDraft))
                                    memoDraft = ""
                                }
                            },
                            width = 120.dp,
                            height = 52.dp
                        )
                    }
                    if (state.correctionMemos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "저장된 정정 메모 (${state.correctionMemos.size}건): ${state.correctionMemos.last().memoText}",
                            color = TextCategoryTeal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassActionButton(
                        text = "보고서로 돌아가기",
                        onClick = {
                            viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE))
                        },
                        width = 180.dp
                    )
                    GlassActionButton(
                        text = "휴대폰에서 검토",
                        iconRes = R.drawable.asset_mobile,
                        onClick = {
                            viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.EXPORT_PDF_CONFIG))
                        },
                        width = 210.dp
                    )
                }
            }
        }

        RecordSubView.EXPORT_PDF_CONFIG -> {
            // 21C mobile_export.png: 보험사 제출 항목 (음성 자막 기본 해제!)
            val cfg = state.exportConfig
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .width(520.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "‹ 돌아가기",
                                    color = TextSecondarySlate,
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable {
                                        viewModel.dispatch(
                                            IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE)
                                        )
                                    }
                                )
                                Text(
                                    text = state.incident.incidentId,
                                    color = TextSecondarySlate,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "보험사 제출 항목",
                                color = TextPrimaryDark,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "보낼 항목을 확인해 주세요.",
                                color = TextSecondarySlate,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            ExportToggleRow(
                                label = "사건 시각과 위치",
                                checked = cfg.includeTimeAndLocation,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        IncidentAction.UpdateExportConfig(cfg.copy(includeTimeAndLocation = it))
                                    )
                                }
                            )
                            ExportToggleRow(
                                label = "센서와 차량 조치",
                                checked = cfg.includeSensorAndVehicleActions,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        IncidentAction.UpdateExportConfig(cfg.copy(includeSensorAndVehicleActions = it))
                                    )
                                }
                            )
                            ExportToggleRow(
                                label = "통화와 알림 결과",
                                checked = cfg.includeCallAndAlertResults,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        IncidentAction.UpdateExportConfig(cfg.copy(includeCallAndAlertResults = it))
                                    )
                                }
                            )
                            ExportToggleRow(
                                label = "동승자 음성 자막",
                                checked = cfg.includePassengerVoiceTranscript,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        IncidentAction.UpdateExportConfig(
                                            cfg.copy(includePassengerVoiceTranscript = it)
                                        )
                                    )
                                }
                            )
                        }

                        GlassActionButton(
                            text = "제출용 PDF 만들기",
                            iconRes = R.drawable.asset_receipt,
                            onClick = { onExportPdf(false) },
                            modifier = Modifier.fillMaxWidth(),
                            height = 58.dp
                        )
                    }
                }
            }
        }

        RecordSubView.EXPORT_PDF_READY -> {
            // 21D mobile_exported.png: 보험사 제출 / 사건 대응 보고서 v1 + PDF 공유
            val latestVer = state.exportedVersions.lastOrNull()
            val verNum = latestVer?.versionNumber ?: 1

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .width(520.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "‹ 항목 다시 선택",
                                color = TextSecondarySlate,
                                fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.EXPORT_PDF_CONFIG))
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "보험사 제출",
                                color = TextPrimaryDark,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Image(
                                painter = painterResource(id = R.drawable.asset_receipt),
                                contentDescription = "보고서 아이콘",
                                modifier = Modifier.size(56.dp),
                                colorFilter = ColorFilter.tint(ToggleTealActive)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "사건 대응 보고서 v$verNum",
                                color = TextPrimaryDark,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "상태: ${latestVer?.shareStatus ?: "파일 준비 완료"} · 시연 모의 출처 명시됨",
                                color = TextCategoryTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (latestVer != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "생성 경로: ${latestVer.filePath}",
                                    color = TextMutedStep,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        GlassActionButton(
                            text = "PDF 공유",
                            iconRes = R.drawable.asset_share,
                            onClick = onSharePdf,
                            modifier = Modifier.fillMaxWidth(),
                            height = 58.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimaryDark,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ToggleTealActive
            )
        )
    }
}

/**
 * Interactive Simulation & Verification Drawer at the bottom of the screen for testing all M0~M4 flows.
 */
@Composable
private fun SimulationControlDrawer(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel,
    onExportPdf: () -> Unit
) {
    Surface(
        color = Color(0xF2102533),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "P0 대표 시연 및 예외 상태 검증 패널 (실제 119 미발신 · 모의 출처 기록)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Row 1: P0 Core Screens (01..14)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimChip("01 홈 (03A)") {
                    viewModel.dispatch(IncidentAction.ResetToDemoStart)
                }
                SimChip("02 음성 대기 (17A)") {
                    viewModel.dispatch(IncidentAction.StartVoiceListening())
                }
                SimChip("03→07 동승자 무반응 제보 시연") {
                    viewModel.simulateProgressiveChildReport()
                }
                SimChip("04A 충돌 신호") {
                    viewModel.dispatch(IncidentAction.TriggerCrashSignalSimulation())
                }
                SimChip("04B 상태 확인") {
                    viewModel.dispatch(IncidentAction.TriggerAmbiguousHelpRequest())
                }
                SimChip("05 1차 미응답 (04D)") {
                    viewModel.dispatch(IncidentAction.FirstCheckNoReply())
                }
                SimChip("06 2차 미응답 (05A)") {
                    viewModel.dispatch(IncidentAction.SecondCheckNoReply())
                }
                SimChip("08 직접 119 발신 (05D)") {
                    viewModel.dispatch(IncidentAction.Request119Call(isDirectFromIdle = true))
                }
                SimChip("09 119 연결·위치 안내 (06C)") {
                    viewModel.updateLocationSimulation(LocationStatus.AVAILABLE)
                    viewModel.dispatch(IncidentAction.CallConnectedConfirmed())
                }
                SimChip("10 연결 실패 (06D)") {
                    viewModel.dispatch(IncidentAction.CallConnectionFailed())
                }
                SimChip("11 연결 미확인 (07B)") {
                    viewModel.dispatch(IncidentAction.CallConnectionUnknown())
                }
                SimChip("12 오래된 위치 (07C)") {
                    viewModel.updateLocationSimulation(LocationStatus.STALE, observedAt = "14:30")
                    viewModel.dispatch(IncidentAction.CallConnectedConfirmed())
                }
                SimChip("13 사건 대응 보고서 (20B)") {
                    viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE))
                }
                SimChip("14 PDF 생성·공유 (21C/21D)") {
                    onExportPdf()
                }
            }

            // Row 2: M1/M2 Edge cases, Vehicle states, P1/P2 features
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimChip("예외: 음성 인식 실패 (16A)") {
                    viewModel.dispatch(IncidentAction.SpeechRecognitionFailed)
                }
                SimChip("규칙1: 부정 발화 (“연기가 없어요”)") {
                    viewModel.simulateVoiceCommand("연기가 없어요")
                }
                SimChip("규칙2: 0km/h 단일 샘플 (정차 미확인)") {
                    viewModel.updateVehicleSimulation(
                        speedKmh = 0,
                        gear = "D",
                        parkingBrake = false,
                        brakePedal = true,
                        isSingleZeroSample = true
                    )
                }
                SimChip("규칙2: 주행 중 (45km/h · 모션/편집 제한)") {
                    viewModel.updateVehicleSimulation(
                        speedKmh = 45,
                        gear = "D",
                        parkingBrake = false,
                        brakePedal = false
                    )
                }
                SimChip("규칙2: 정차 확정 (기어 P + 주차브레이크)") {
                    viewModel.updateVehicleSimulation(
                        speedKmh = 0,
                        gear = "P",
                        parkingBrake = true,
                        brakePedal = false
                    )
                }
                SimChip("규칙3: 공식 SOS 동시 진행") {
                    viewModel.dispatch(IncidentAction.SetOfficialSosActive(true))
                    viewModel.dispatch(IncidentAction.Request119Call())
                }
                SimChip("03C 정차 후 연락판") {
                    viewModel.dispatch(IncidentAction.ToggleStoppedContactSelector(true))
                }
                SimChip("14A 보호자 10초 알림 시작") {
                    viewModel.dispatch(IncidentAction.StartGuardianCountdown())
                }
                SimChip("11D 제조사 정차·비상등 확인") {
                    viewModel.dispatch(IncidentAction.TriggerGuideScenario(GuideType.OEM_AUTO_HAZARD_CONFIRMED))
                }
                SimChip("12A 과열 경고") {
                    viewModel.dispatch(IncidentAction.TriggerGuideScenario(GuideType.BATTERY_OVERHEAT_ONLY))
                }
                SimChip("12B/C/D 연기 신고") {
                    viewModel.simulateVoiceCommand("연기가 나요")
                }
                SimChip("14B 의료 카드") {
                    viewModel.dispatch(IncidentAction.TriggerGuideScenario(GuideType.MEDICAL_CARD_VIEW))
                }
                SimChip("16E 좁은 창 토글 (${if (state.isNarrowWindowMode) "ON" else "OFF"})") {
                    viewModel.dispatch(IncidentAction.ToggleNarrowWindow(!state.isNarrowWindowMode))
                }
            }
        }
    }
}

@Composable
private fun SimChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF23475C),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Vehicle State SDK & Safety Telemetry Inspector Dialog.
 * Explicitly shows supported vs unsupported fields and real vs simulated data sources.
 */
@Composable
private fun VehicleSdkTelemetryDialog(
    state: DriveCheckUiState,
    viewModel: DriveCheckViewModel,
    onDismiss: () -> Unit
) {
    val v = state.vehicle
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pleos Vehicle State SDK 및 차량 안전 상태 점검",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• SDK 연결 상태: ${v.sdkStatus.summaryText}",
                    fontWeight = FontWeight.SemiBold,
                    color = TextCategoryTeal,
                    fontSize = 14.sp
                )
                Text(
                    text = v.sdkStatus.detailText,
                    fontSize = 12.sp,
                    color = TextSecondarySlate
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("1. 주행·브레이크·동력원 상태 (Vehicle.getBrake / CarPropertyManager)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• 동력원(Powertrain): ${formatSupportedStatus(v.powertrainState)}", fontSize = 13.sp)
                Text("• 현재 속도 / 기어: ${v.speedKmh} km/h (단일 0km/h 샘플=${v.isSingleZeroSpeedSample}) / 기어 ${v.gearSelection}", fontSize = 13.sp)
                Text("• 주차 브레이크 / 페달: 주차브레이크=${if (v.parkingBrakeEngaged) "작동(ON)" else "해제"} / 브레이크페달=${if (v.brakePedalPressed) "밟음" else "해제"}", fontSize = 13.sp)
                Text("• 정차 판정 결과 (R-04): ${v.stopConfirmation.name} (모션 허용=${v.isAnimationAllowed}, 편집 허용=${v.isEditingAllowed})", fontSize = 13.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("2. 차량 안전 상태 항목 (지원 범위 및 미지원 구분 표시)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• 안전벨트 상태: ${formatSupportedStatus(v.seatbeltFastened)}", fontSize = 13.sp)
                Text("• 에어백 경고등: ${formatSupportedStatus(v.airbagWarningLamp)}", fontSize = 13.sp)
                Text("• 타이어 공기압: ${formatSupportedStatus(v.tirePressureNormal)}", fontSize = 13.sp)
                Text("• 비상등 상태 (FN-07): ${v.hazardLightState.korLabel}", fontSize = 13.sp)

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.updateSafetyTelemetrySimulation(
                                seatbelt = true,
                                airbagLamp = false,
                                tirePressureNormal = true,
                                hazardState = HazardLightState.MANUAL_ON
                            )
                        }
                    ) {
                        Text("모의 안전상태(정상) 주입", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.updateSafetyTelemetrySimulation(
                                seatbelt = false,
                                airbagLamp = true,
                                tirePressureNormal = false,
                                hazardState = HazardLightState.AUTO_ON
                            )
                        }
                    ) {
                        Text("모의 경고상태 주입", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

private fun <T> formatSupportedStatus(status: SupportedFieldStatus<T>): String {
    return when (status) {
        is SupportedFieldStatus.Supported -> "${status.value} [${status.dataOrigin.badgeText}]"
        is SupportedFieldStatus.Unsupported -> "미지원 (${status.reason})"
    }
}
