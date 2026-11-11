package com.hyundai.pleos.drivecheck.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyundai.pleos.drivecheck.R
import com.hyundai.pleos.drivecheck.domain.model.GuardianAlertStatus
import com.hyundai.pleos.drivecheck.ui.theme.*

/**
 * UX-08: Rectangular 119 emergency button (#BD2F50 -> #9E1C3B, 12dp radius, white text).
 */
@Composable
fun Emergency119RectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "119 전화"
) {
    Box(
        modifier = modifier
            .width(188.dp)
            .height(64.dp)
            .shadow(10.dp, RoundedCornerShape(12.dp), ambientColor = EmergencyRedEnd, spotColor = EmergencyRedEnd)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(EmergencyRedStart, EmergencyRedEnd)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.asset_phone),
                contentDescription = "전화",
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * UX-08: Large circular 119 button (#A91C3C center radial gradient, used in 05C driver_unresponsive & 03C suggest).
 */
@Composable
fun Emergency119CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(20.dp, CircleShape, ambientColor = EmergencyCircleCenter, spotColor = EmergencyCircleCenter)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC83253),
                        EmergencyCircleCenter,
                        Color(0xFF8E1531)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.asset_phone),
                contentDescription = "119 전화",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "119",
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "전화",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Section 5: Translucent blue-gray glass button (#F7FBFD -> #CDE0EA, 12dp corner radius, #194457 text, no border).
 */
@Composable
fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    width: Dp = 168.dp,
    height: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .widthIn(min = width)
            .height(height)
            .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x33194457))
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GlassBtnTop, GlassBtnBottom)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(GlassBtnText)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = GlassBtnText,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * UX-12 & Section 5: Right-hand incident progression list with a single active orange dot (#C85A17).
 * Current step opacity = 100%, past step = 80%, upcoming step = 67%.
 * Dot brightness pulses at 1.46s period ONLY when isAnimationAllowed == true.
 */
@Composable
fun RightProgressionTimeline(
    steps: List<String>,
    activeIndex: Int,
    isAnimationAllowed: Boolean,
    guardianStatus: GuardianAlertStatus = GuardianAlertStatus.IDLE,
    guardianCountdownSec: Int = 10,
    onCancelGuardian: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timelineDotPulse")
    val animatedDotAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 730, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    val dotAlpha = if (isAnimationAllowed) animatedDotAlpha else 1.0f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        steps.forEachIndexed { index, stepTitle ->
            val isCurrent = index == activeIndex
            val isPast = index < activeIndex
            val textAlpha = when {
                isCurrent -> 1.0f
                isPast -> 0.80f
                else -> 0.67f
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(26.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(dotAlpha)
                                .clip(CircleShape)
                                .background(ActiveOrangeDot)
                        )
                    }
                }

                Text(
                    text = stepTitle,
                    color = if (isCurrent) TextPrimaryDark else TextMutedStep,
                    fontSize = 25.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.alpha(textAlpha)
                )

                // FN-08 & Rule 7: Show countdown and cancel only during active guardian countdown
                if (stepTitle == "보호자 알림" && guardianStatus == GuardianAlertStatus.COUNTDOWN) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${guardianCountdownSec}초",
                        color = ActiveOrangeDot,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (onCancelGuardian != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = GlassBtnBottom.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(onClick = onCancelGuardian)
                        ) {
                            Text(
                                text = "취소",
                                color = GlassBtnText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else if (stepTitle == "보호자 알림" && guardianStatus == GuardianAlertStatus.CANCELLED) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "취소됨",
                        color = TextMutedStep,
                        fontSize = 15.sp
                    )
                } else if (stepTitle == "보호자 알림" && guardianStatus == GuardianAlertStatus.SENT) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "발송됨",
                        color = TextCategoryTeal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * UX-05 & UX-06: Mochi character view with optional voice waveform.
 * Character scale and waveform react ONLY when isAnimationAllowed == true.
 */
@Composable
fun MochiCharacterView(
    isMicOpen: Boolean,
    inputLevel: Float,
    isAnimationAllowed: Boolean,
    modifier: Modifier = Modifier,
    characterSize: Dp = 220.dp,
    showWaveform: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mochiBreath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mochiScale"
    )

    val effectiveScale = if (isAnimationAllowed) {
        breathScale + (if (isMicOpen) inputLevel * 0.04f else 0f)
    } else {
        1.0f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.asset_mochi),
            contentDescription = "DriveCheck 캐릭터",
            modifier = Modifier
                .size(characterSize)
                .scale(effectiveScale),
            contentScale = ContentScale.Fit
        )

        if (showWaveform) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(20.dp)
            ) {
                val barHeights = listOf(8.dp, 12.dp, 15.dp, 11.dp, 16.dp, 12.dp, 14.dp, 10.dp, 8.dp)
                barHeights.forEach { baseH ->
                    val h = if (isAnimationAllowed && isMicOpen) baseH else 10.dp
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(h)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF425475))
                    )
                }
            }
        }
    }
}

/**
 * UX-03 & FN-07: Vehicle image (side 3/4 view or top view) with optional expanding radar circles when parked.
 */
@Composable
fun Ioniq5VehicleView(
    showRadarCircles: Boolean,
    isAnimationAllowed: Boolean,
    useTopView: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vehicleMotion")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarScale"
    )
    // Section 5: Top-view rotates 1 revolution per 16s ONLY when park is confirmed
    val topViewRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "topViewRotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showRadarCircles) {
            val circleScale = if (isAnimationAllowed) radarScale else 1.0f
            Canvas(
                modifier = Modifier
                    .size(310.dp)
                    .scale(circleScale)
            ) {
                drawCircle(
                    color = Color(0xFF9FB4C2).copy(alpha = 0.35f),
                    radius = size.minDimension * 0.38f,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = Color(0xFF9FB4C2).copy(alpha = 0.22f),
                    radius = size.minDimension * 0.49f,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        if (useTopView) {
            Image(
                painter = painterResource(id = R.drawable.asset_ioniq5_top_view),
                contentDescription = "아이오닉 5 탑뷰",
                modifier = Modifier
                    .size(240.dp)
                    .rotate(if (isAnimationAllowed) topViewRotation else 0f),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.asset_ioniq5),
                contentDescription = "아이오닉 5",
                modifier = Modifier
                    .width(360.dp)
                    .height(185.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * UX-09: Semi-transparent contact selection panel (03C suggest.png & 15A other_contacts.png).
 * Hides tow & insurance before safety/stop confirmation!
 */
@Composable
fun OtherContactsGlassPanel(
    showTowAndInsurance: Boolean,
    onGuardianClick: () -> Unit,
    onPolice112Click: () -> Unit,
    onTowClick: () -> Unit,
    onInsuranceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(340.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x22102533)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = "다른 연락",
                color = TextPrimaryDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ContactIconTile(
                    iconRes = R.drawable.asset_guardian,
                    label = "”보호자 알림”",
                    onClick = onGuardianClick
                )
                ContactIconTile(
                    iconRes = R.drawable.asset_police,
                    label = "”112 전화”",
                    onClick = onPolice112Click
                )
            }

            if (showTowAndInsurance) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ContactIconTile(
                        iconRes = R.drawable.asset_tow,
                        label = "”견인 연락”",
                        onClick = onTowClick
                    )
                    ContactIconTile(
                        iconRes = R.drawable.asset_insurance,
                        label = "”보험사 연락”",
                        onClick = onInsuranceClick
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "※ 안전 정차 확인 후 견인·보험사 연락이 표시됩니다.",
                    color = TextMutedStep,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ContactIconTile(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .sizeIn(minWidth = 110.dp, minHeight = 96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = TextPrimaryDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
