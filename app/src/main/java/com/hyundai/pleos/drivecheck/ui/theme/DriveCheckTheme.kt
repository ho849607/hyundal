package com.hyundai.pleos.drivecheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgVehicleStart = Color(0xFFF6F9FB)
val BgVehicleEnd = Color(0xFFE7EFF4)
val TextPrimaryDark = Color(0xFF102533)
val TextSecondarySlate = Color(0xFF2B4C5E)
val TextCategoryTeal = Color(0xFF1A6665)
val TextMutedStep = Color(0xFF4A6575)
val EmergencyRedStart = Color(0xFFBD2F50)
val EmergencyRedEnd = Color(0xFF9E1C3B)
val EmergencyCircleCenter = Color(0xFFA91C3C)
val GlassBtnTop = Color(0xFFF7FBFD)
val GlassBtnBottom = Color(0xFFCDE0EA)
val GlassBtnText = Color(0xFF194457)
val ActiveOrangeDot = Color(0xFFC85A17)
val ToggleTealActive = Color(0xFF1C6B6E)

private val DriveCheckColorScheme = lightColorScheme(
    primary = EmergencyRedStart,
    secondary = TextCategoryTeal,
    background = BgVehicleStart,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

@Composable
fun DriveCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DriveCheckColorScheme,
        content = content
    )
}
