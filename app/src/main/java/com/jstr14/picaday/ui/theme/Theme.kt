package com.jstr14.picaday.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jstr14.picaday.ui.theme.util.Season
import com.jstr14.picaday.ui.theme.util.SeasonManager

@Composable
fun PicADayTheme(
    specialDay: SeasonManager.SpecialDay = SeasonManager.getSpecialDay(),
    season: Season = SeasonManager.getCurrentSeason(),
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        specialDay == SeasonManager.SpecialDay.HALLOWEEN -> if (darkTheme) HalloweenDarkScheme else HalloweenLightScheme
        specialDay == SeasonManager.SpecialDay.CHRISTMAS -> if (darkTheme) ChristmasDarkScheme else ChristmasLightScheme
        specialDay == SeasonManager.SpecialDay.VALENTINE -> if (darkTheme) ValentineDarkScheme else ValentineLightScheme
        specialDay == SeasonManager.SpecialDay.NEW_YEAR -> if (darkTheme) NewYearDarkScheme else NewYearLightScheme
        else -> when (season) {
            Season.SPRING -> if (darkTheme) SpringDarkScheme else SpringLightScheme
            Season.SUMMER -> if (darkTheme) SummerDarkScheme else SummerLightScheme
            Season.AUTUMN -> if (darkTheme) AutumnDarkScheme else AutumnLightScheme
            Season.WINTER -> if (darkTheme) WinterDarkScheme else WinterLightScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// --- SEASONAL SCHEMES ---

private val SpringLightScheme = lightColorScheme(
    primary = SpringPrimary, onPrimary = White,
    primaryContainer = SpringSecondary, onPrimaryContainer = SpringOnPrimaryDark,
    secondary = SpringAccent, onSecondary = SpringOnPrimaryDark,
    background = SpringBackground, surface = SpringSurface, onBackground = DarkGray
)
private val SpringDarkScheme = darkColorScheme(
    primary = SpringPrimaryDark, onPrimary = SpringOnPrimaryDark,
    primaryContainer = SpringContainerDark, onPrimaryContainer = SpringSecondary,
    secondary = SpringAccent, onSecondary = SpringOnPrimaryDark,
    background = SpringBackgroundDark, surface = SpringContainerDark, onBackground = White
)

private val SummerLightScheme = lightColorScheme(
    primary = SummerPrimary, onPrimary = SummerOnPrimaryDark,
    primaryContainer = SummerSecondary, onPrimaryContainer = SummerOnPrimaryDark,
    secondary = SummerTeal, onSecondary = SummerOnPrimaryDark,
    background = SummerBackground, surface = SummerTealBackground, onBackground = DarkGray
)
private val SummerDarkScheme = darkColorScheme(
    primary = SummerPrimaryDark, onPrimary = SummerOnPrimaryDark,
    primaryContainer = SummerContainerDark, onPrimaryContainer = SummerSecondary,
    secondary = SummerTeal, onSecondary = SummerOnPrimaryDark,
    background = SummerBackgroundDark, surface = SummerContainerDark, onBackground = White
)

private val AutumnLightScheme = lightColorScheme(
    primary = AutumnPrimary, onPrimary = White,
    primaryContainer = AutumnSurface, onPrimaryContainer = AutumnOnPrimaryDark,
    secondary = AutumnAmber, onSecondary = AutumnOnPrimaryDark,
    background = AutumnBackground, surface = AutumnSurface, onBackground = DarkGray
)
private val AutumnDarkScheme = darkColorScheme(
    primary = AutumnPrimaryDark, onPrimary = AutumnOnPrimaryDark,
    primaryContainer = AutumnContainerDark, onPrimaryContainer = AutumnSurface,
    secondary = AutumnAmber, onSecondary = AutumnOnPrimaryDark,
    background = AutumnBackgroundDark, surface = AutumnContainerDark, onBackground = White
)

private val WinterLightScheme = lightColorScheme(
    primary = WinterPrimary, onPrimary = White,
    primaryContainer = WinterSecondary, onPrimaryContainer = WinterOnPrimaryDark,
    secondary = WinterLavender, onSecondary = WinterOnPrimaryDark,
    background = WinterBackground, surface = WinterSurface, onBackground = DarkGray
)
private val WinterDarkScheme = darkColorScheme(
    primary = WinterPrimaryDark, onPrimary = WinterOnPrimaryDark,
    primaryContainer = WinterContainerDark, onPrimaryContainer = WinterSecondary,
    secondary = WinterLavender, onSecondary = WinterOnPrimaryDark,
    background = WinterBackgroundDark, surface = WinterContainerDark, onBackground = White
)

// --- SPECIAL DAY SCHEMES ---

private val HalloweenLightScheme = lightColorScheme(
    primary = HalloweenPumpkin, onPrimary = White,
    primaryContainer = HalloweenSurface, onPrimaryContainer = HalloweenOnPrimaryDark,
    secondary = HalloweenPurple, onSecondary = White,
    background = HalloweenBackground, surface = HalloweenSurface, onBackground = DarkGray
)
private val HalloweenDarkScheme = darkColorScheme(
    primary = HalloweenSurface, onPrimary = HalloweenOnPrimaryDark,
    primaryContainer = HalloweenContainerDark, onPrimaryContainer = HalloweenSurface,
    secondary = HalloweenSoftPurple, onSecondary = HalloweenOnSecondaryDark,
    background = HalloweenDarkBackground, surface = HalloweenSurfaceDark, onBackground = White
)

private val ChristmasLightScheme = lightColorScheme(
    primary = ChristmasRed, onPrimary = White,
    primaryContainer = ChristmasRedLight, onPrimaryContainer = ChristmasOnPrimaryContainer,
    secondary = ChristmasGreen, onSecondary = White,
    background = ChristmasBackground, surface = ChristmasGreenLight, onBackground = DarkGray
)
private val ChristmasDarkScheme = darkColorScheme(
    primary = ChristmasPrimaryDark, onPrimary = ChristmasOnPrimaryContainer,
    primaryContainer = ChristmasContainerDark, onPrimaryContainer = ChristmasRedLight,
    secondary = ChristmasSecondaryDark, onSecondary = ChristmasOnSecondaryDark,
    background = ChristmasBackgroundDark, surface = ChristmasSurfaceDark, onBackground = White
)

private val ValentineLightScheme = lightColorScheme(
    primary = ValentineRose, onPrimary = White,
    primaryContainer = ValentineSurface, onPrimaryContainer = ValentineOnPrimaryContainer,
    secondary = ValentineMauve, onSecondary = White,
    background = ValentineBackground, surface = ValentineSoft, onBackground = DarkGray
)
private val ValentineDarkScheme = darkColorScheme(
    primary = ValentineBlush, onPrimary = ValentineOnPrimaryContainer,
    primaryContainer = ValentineContainerDark, onPrimaryContainer = ValentineSurface,
    secondary = ValentineMauve, onSecondary = White,
    background = ValentineBackgroundDark, surface = ValentineSurfaceDark, onBackground = White
)

private val NewYearLightScheme = lightColorScheme(
    primary = NewYearGold, onPrimary = White,
    primaryContainer = NewYearSurface, onPrimaryContainer = NewYearOnPrimaryContainer,
    secondary = NewYearMidnight, onSecondary = White,
    background = NewYearBackground, surface = NewYearChampagne, onBackground = DarkGray
)
private val NewYearDarkScheme = darkColorScheme(
    primary = NewYearChampagne, onPrimary = NewYearOnPrimaryContainer,
    primaryContainer = NewYearContainerDark, onPrimaryContainer = NewYearSurface,
    secondary = NewYearMidnight, onSecondary = White,
    background = NewYearBackgroundDark, surface = NewYearSurfaceDark, onBackground = White
)