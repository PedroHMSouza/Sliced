package com.pedrohmsouza.sliced.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ícones de traço desenhados como os SVGs da versão web. Usar ImageVector próprio evita
 * a dependência material-icons-extended (vários MB) por causa de três ícones.
 */
object SlicedIcons {
    val ChevronDown = stroke("M6 9l6 6 6-6", 2.2f)
    val Check = stroke("M5 12.5l4.5 4.5L19 7.5", 3f)
    val Timer = stroke("M12 5a8 8 0 1 0 0.01 0zM12 9v4l2.5 2.5M9.5 2.5h5", 2f)
    val Back = stroke("M15 6l-6 6 6 6", 2.2f)

    private fun stroke(path: String, width: Float) = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}
