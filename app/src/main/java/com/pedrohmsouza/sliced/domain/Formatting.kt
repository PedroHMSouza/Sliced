package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.RecordType
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Formatação e leitura de números/datas no padrão brasileiro. Funções puras, testáveis na JVM. */
object Fmt {
    val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /** Aceita vírgula ou ponto: "22,5" e "22.5" viram 22.5. Vazio ou inválido vira null. */
    fun parse(text: String?): Double? {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return null
        return t.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /** 22.5 → "22,5"; 1234.0 → "1.234"; no máximo uma casa decimal, como na versão web. */
    fun number(n: Number?): String {
        if (n == null) return ""
        val f = NumberFormat.getNumberInstance(PT_BR).apply { maximumFractionDigits = 1 }
        return f.format(n.toDouble())
    }

    /** Como [number], sem separador de milhar: é o texto que vai para dentro de um campo. */
    fun plain(n: Number?): String {
        if (n == null) return ""
        val f = NumberFormat.getNumberInstance(PT_BR).apply { maximumFractionDigits = 1; isGroupingUsed = false }
        return f.format(n.toDouble())
    }

    /**
     * Limpa o que foi digitado: só dígitos (e vírgula/ponto nos campos decimais).
     * Em campos inteiros, para no separador: "12,5" vira "12" (e não "125").
     */
    fun sanitize(raw: String, key: FieldKey): String =
        if (key.decimal) raw.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
        else raw.takeWhile { it != ',' && it != '.' }.filter { it.isDigit() }.take(5)

    /** Série no formato longo: "40 kg × 12", "45 s", "30 min · 8%". */
    fun set(type: RecordType, s: LoggedSet?): String {
        if (s == null) return "—"
        val kg = s.kg?.let(::number); val reps = s.reps?.let(::number)
        return when (type) {
            RecordType.WR -> if (kg != null || reps != null) "${kg ?: "–"} kg × ${reps ?: "–"}" else "—"
            RecordType.R -> reps?.let { "$it reps" } ?: "—"
            RecordType.S -> s.seconds?.let { "${number(it)} s" } ?: "—"
            RecordType.WM -> if (kg != null || s.meters != null) "${kg ?: "–"} kg · ${s.meters?.let(::number) ?: "–"} m" else "—"
            RecordType.CARDIO -> s.minutes?.let { m ->
                "${number(m)} min" + (s.incline?.let { " · ${number(it)}%" } ?: "")
            } ?: "—"
        }
    }

    /** Formato compacto do histórico: "40×12" para carga × reps; os outros tipos usam [set]. */
    fun setShort(type: RecordType, s: LoggedSet): String =
        if (type == RecordType.WR) "${number(s.kg)}×${number(s.reps)}" else set(type, s)

    private val WEEKDAYS = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
    val WEEK_LETTERS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    /** "seg, 09/11". */
    fun dateLabel(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val d = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return "${WEEKDAYS[d.dayOfWeek.value - 1]}, ${pad(d.dayOfMonth)}/${pad(d.monthValue)}"
    }

    /** "09/11" (eixo do gráfico). */
    fun dayMonth(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val d = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return "${pad(d.dayOfMonth)}/${pad(d.monthValue)}"
    }

    /** Cronômetro do treino: "07:42" ou "1:02:03". */
    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600; val m = s % 3600 / 60; val x = s % 60
        return if (h > 0) "$h:${pad(m)}:${pad(x)}" else "${pad(m)}:${pad(x)}"
    }

    /** Contagem do descanso: "1:30". */
    fun seconds(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        return "${s / 60}:${pad(s % 60)}"
    }

    fun hhmm(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochMilli(epochMs).atZone(zone)
        return "${pad(t.hour)}:${pad(t.minute)}"
    }

    fun isoDate(d: LocalDate): String = d.toString() // YYYY-MM-DD

    private fun pad(n: Number) = n.toString().padStart(2, '0')
}
