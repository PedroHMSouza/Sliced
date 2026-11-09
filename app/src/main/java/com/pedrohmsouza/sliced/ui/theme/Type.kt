package com.pedrohmsouza.sliced.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R

/**
 * As fontes ficam dentro do APK (res/font) em vez de "Downloadable Fonts":
 * o app precisa funcionar offline desde a primeira abertura, e na academia o sinal é ruim.
 */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Estilos com nomes do app, usados via `SlicedTheme.type`. */
object SlicedType {
    val display = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold)
    val body = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp)

    val brand = display.copy(fontSize = 22.sp, lineHeight = 22.sp)
    val section = display.copy(fontSize = 24.sp, lineHeight = 26.sp)
    val treinoName = display.copy(fontSize = 20.sp, lineHeight = 23.sp)
    val exerciseName = display.copy(fontSize = 21.sp, lineHeight = 24.sp)
    val stat = display.copy(fontSize = 26.sp, lineHeight = 26.sp)
    val bigTimer = display.copy(fontSize = 42.sp, lineHeight = 42.sp)
    val clock = display.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 22.sp)
    val input = display.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 19.sp)
    val eyebrow = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.sp)
    val small = TextStyle(fontFamily = Barlow, fontSize = 14.sp, lineHeight = 19.sp)
    val tiny = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp)
    val button = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
}

val MaterialTypography = Typography().let { t ->
    t.copy(
        bodyLarge = t.bodyLarge.copy(fontFamily = Barlow),
        bodyMedium = t.bodyMedium.copy(fontFamily = Barlow),
        bodySmall = t.bodySmall.copy(fontFamily = Barlow),
        labelLarge = t.labelLarge.copy(fontFamily = Barlow, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontFamily = Barlow),
        titleLarge = t.titleLarge.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = Barlow, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
    )
}
