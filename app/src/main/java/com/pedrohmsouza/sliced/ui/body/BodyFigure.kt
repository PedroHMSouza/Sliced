package com.pedrohmsouza.sliced.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.Muscle.ABDUCTORS
import com.pedrohmsouza.sliced.model.Muscle.ABS
import com.pedrohmsouza.sliced.model.Muscle.ADDUCTORS
import com.pedrohmsouza.sliced.model.Muscle.BICEPS
import com.pedrohmsouza.sliced.model.Muscle.CALVES
import com.pedrohmsouza.sliced.model.Muscle.CHEST
import com.pedrohmsouza.sliced.model.Muscle.FOREARMS
import com.pedrohmsouza.sliced.model.Muscle.GLUTES
import com.pedrohmsouza.sliced.model.Muscle.HAMSTRINGS
import com.pedrohmsouza.sliced.model.Muscle.LATS
import com.pedrohmsouza.sliced.model.Muscle.LOWER_BACK
import com.pedrohmsouza.sliced.model.Muscle.MIDDLE_BACK
import com.pedrohmsouza.sliced.model.Muscle.NECK
import com.pedrohmsouza.sliced.model.Muscle.QUADS
import com.pedrohmsouza.sliced.model.Muscle.SHOULDERS
import com.pedrohmsouza.sliced.model.Muscle.TRAPS
import com.pedrohmsouza.sliced.model.Muscle.TRICEPS
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/*
 * Boneco frente/costas desenhado no Canvas, sem imagens: cada grupo muscular é uma forma
 * suave numa grade de 200 × 380. Só o lado esquerdo da figura é descrito; o direito é o
 * espelho (x → 200 − x). As formas passam por uma curva Catmull-Rom, então poucos pontos
 * já dão contornos arredondados.
 */

private const val W = 200f
private const val H = 380f

private class Part(val muscle: Muscle?, val points: List<Offset>, val mirror: Boolean = true)

private class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val mirror: Boolean = true)

private fun pts(vararg xy: Int) = xy.toList().chunked(2) { (x, y) -> Offset(x.toFloat(), y.toFloat()) }

private val Head = Oval(100f, 26f, 16f, 20f, mirror = false)
private val Joints = listOf(Oval(37f, 199f, 6f, 9f), Oval(80f, 277f, 9f, 8f), Oval(80f, 364f, 9f, 6f))
private val Pelvis = Part(null, pts(70, 160, 84, 172, 100, 178, 116, 172, 130, 160, 133, 186, 100, 198, 67, 186), mirror = false)

private val Front = listOf(
    Part(NECK, pts(92, 44, 108, 44, 110, 58, 90, 58), mirror = false),
    Part(TRAPS, pts(91, 52, 72, 63, 92, 62)),
    Part(SHOULDERS, pts(72, 62, 58, 64, 48, 76, 46, 94, 54, 98, 62, 86, 70, 74)),
    Part(CHEST, pts(97, 66, 76, 66, 64, 80, 64, 98, 76, 108, 97, 104)),
    Part(BICEPS, pts(52, 100, 45, 112, 43, 132, 48, 142, 56, 136, 60, 114)),
    Part(FOREARMS, pts(44, 146, 38, 162, 34, 186, 40, 190, 48, 170, 54, 148)),
    Part(ABS, pts(88, 110, 112, 110, 113, 140, 111, 168, 100, 176, 89, 168, 87, 140), mirror = false),
    Part(ABS, pts(85, 112, 70, 110, 66, 130, 70, 156, 84, 170, 86, 140)),
    Part(ABDUCTORS, pts(70, 170, 64, 180, 62, 200, 67, 194)),
    Part(QUADS, pts(90, 186, 74, 180, 64, 204, 64, 244, 74, 268, 88, 266, 94, 232, 92, 204)),
    Part(ADDUCTORS, pts(99, 192, 94, 200, 93, 232, 98, 246)),
    Part(CALVES, pts(74, 288, 68, 312, 70, 342, 78, 352, 86, 338, 88, 306, 84, 288)),
)

private val Back = listOf(
    Part(NECK, pts(92, 44, 108, 44, 110, 54, 90, 54), mirror = false),
    Part(TRAPS, pts(100, 48, 86, 58, 66, 66, 84, 74, 100, 98, 116, 74, 134, 66, 114, 58), mirror = false),
    Part(SHOULDERS, pts(66, 66, 54, 70, 46, 84, 46, 96, 56, 96, 64, 82)),
    Part(TRICEPS, pts(52, 100, 45, 112, 43, 132, 48, 142, 56, 136, 60, 114)),
    Part(FOREARMS, pts(44, 146, 38, 162, 34, 186, 40, 190, 48, 170, 54, 148)),
    Part(MIDDLE_BACK, pts(98, 100, 86, 80, 78, 90, 84, 116, 98, 128)),
    Part(LATS, pts(76, 84, 64, 94, 64, 118, 74, 142, 90, 156, 94, 146, 84, 124, 78, 100)),
    Part(LOWER_BACK, pts(98, 132, 88, 126, 86, 150, 90, 164, 98, 168)),
    Part(ABDUCTORS, pts(80, 160, 68, 164, 64, 180, 72, 174)),
    Part(GLUTES, pts(98, 172, 82, 166, 68, 180, 68, 200, 80, 210, 98, 206)),
    Part(HAMSTRINGS, pts(94, 214, 78, 214, 66, 236, 68, 262, 80, 272, 92, 262, 96, 236)),
    Part(ADDUCTORS, pts(99, 214, 96, 232, 99, 250)),
    Part(CALVES, pts(76, 284, 68, 304, 70, 330, 80, 344, 88, 330, 90, 302, 86, 284)),
)

/** Músculos que aparecem em cada vista (para a descrição de acessibilidade). */
val FrontMuscles: Set<Muscle> = Front.mapNotNull { it.muscle }.toSet()
val BackMuscles: Set<Muscle> = Back.mapNotNull { it.muscle }.toSet()

/**
 * Tons do mapa, do nível 0 (cinza) ao 4 (vermelho da anilha A). São cores opacas, misturadas
 * à mão: com transparência, as bordas sobrepostas de duas formas ficariam mais escuras.
 */
@Composable
fun heatShades(): List<Color> {
    val c = SlicedTheme.colors
    val heat = c.plate("A")
    return listOf(0f, 0.3f, 0.55f, 0.8f, 1f).map { lerp(c.soft, heat, it) }
}

/** Boneco de uma vista com cada músculo pintado conforme [level] (0 a 4). */
@Composable
fun BodyFigure(front: Boolean, level: (Muscle) -> Int, modifier: Modifier = Modifier) {
    val c = SlicedTheme.colors
    val shades = heatShades()
    val parts = if (front) Front else Back
    Canvas(modifier.aspectRatio(W / H)) {
        val k = size.width / W
        val gap = Stroke(width = 1.4f * k)
        fun fill(path: Path, color: Color) {
            drawPath(path, color)
            drawPath(path, c.surface, style = gap)
        }
        fill(Pelvis.path(k, mirrored = false), c.soft)
        drawOvalPart(Head, k, c.soft)
        Joints.forEach { drawOvalPart(it, k, c.soft) }
        parts.forEach { p ->
            val color = p.muscle?.let { shades[level(it).coerceIn(0, 4)] } ?: c.soft
            fill(p.path(k, mirrored = false), color)
            if (p.mirror) fill(p.path(k, mirrored = true), color)
        }
    }
}

private fun DrawScope.drawOvalPart(o: Oval, k: Float, color: Color) {
    fun at(cx: Float) = drawOval(color, Offset((cx - o.rx) * k, (o.cy - o.ry) * k), Size(o.rx * 2 * k, o.ry * 2 * k))
    at(o.cx)
    if (o.mirror) at(W - o.cx)
}

/** Curva fechada suave pelos pontos (Catmull-Rom convertida em Bézier cúbicas). */
private fun Part.path(k: Float, mirrored: Boolean): Path {
    val p = points.map { Offset(if (mirrored) W - it.x else it.x, it.y) * k }
    val n = p.size
    return Path().apply {
        moveTo(p[0].x, p[0].y)
        for (i in 0 until n) {
            val p0 = p[(i - 1 + n) % n]; val p1 = p[i]; val p2 = p[(i + 1) % n]; val p3 = p[(i + 2) % n]
            val c1 = p1 + (p2 - p0) / 6f
            val c2 = p2 - (p3 - p1) / 6f
            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }
        close()
    }
}
