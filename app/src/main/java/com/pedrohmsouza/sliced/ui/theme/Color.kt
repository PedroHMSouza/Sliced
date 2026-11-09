package com.pedrohmsouza.sliced.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta do app, copiada dos tokens CSS da versão web (`--bg`, `--surface`, `--pA`...).
 *
 * O Material 3 tem o próprio esquema de cores, mas os nomes dele (primary, tertiary...)
 * não descrevem bem um diário de treino. Por isso guardamos aqui uma paleta com nomes
 * do domínio e a expomos via CompositionLocal (`SlicedTheme.colors`). Também mapeamos
 * esses valores no ColorScheme do Material para que componentes prontos (bottom sheet,
 * diálogo, snackbar) já saiam com as cores certas.
 */
@Immutable
data class SlicedColors(
    val bg: Color,
    val surface: Color,
    val fg: Color,
    val muted: Color,
    val line: Color,
    val soft: Color,
    val ok: Color,
    val okSoft: Color,
    val onOk: Color,
    val warn: Color,
    val danger: Color,
    /** Cores das anilhas olímpicas: A vermelho, B azul, C amarelo, D verde, E cinza. */
    val plates: Map<String, Color>,
) {
    fun plate(treinoId: String): Color = plates[treinoId] ?: muted
}

val LightColors = SlicedColors(
    bg = Color(0xFFF1F3F2), surface = Color(0xFFFFFFFF), fg = Color(0xFF16191A),
    muted = Color(0xFF5F6764), line = Color(0xFFD7DCD9), soft = Color(0xFFE6EAE8),
    ok = Color(0xFF2C8550), okSoft = Color(0xFFE1F0E6), onOk = Color(0xFFFFFFFF),
    warn = Color(0xFFA8670F), danger = Color(0xFFB3261E),
    plates = mapOf(
        "A" to Color(0xFFC9372C), "B" to Color(0xFF2F5FB3), "C" to Color(0xFFC9950F),
        "D" to Color(0xFF2C8550), "E" to Color(0xFF66717A),
    ),
)

val DarkColors = SlicedColors(
    bg = Color(0xFF101312), surface = Color(0xFF191D1B), fg = Color(0xFFECEFED),
    muted = Color(0xFF9BA4A0), line = Color(0xFF2B312E), soft = Color(0xFF222725),
    ok = Color(0xFF52B877), okSoft = Color(0xFF1B2E22), onOk = Color(0xFF0D1A12),
    warn = Color(0xFFE3A64A), danger = Color(0xFFF2867D),
    plates = mapOf(
        "A" to Color(0xFFE5574B), "B" to Color(0xFF7398E3), "C" to Color(0xFFE6B83D),
        "D" to Color(0xFF52B877), "E" to Color(0xFF9EA9B2),
    ),
)

val LocalSlicedColors = staticCompositionLocalOf { LightColors }
