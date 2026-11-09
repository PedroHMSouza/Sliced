package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Regra 8: vírgula ou ponto na entrada; exibição em pt-BR. */
class FmtTest {
    @Test fun parse_acceptsCommaAndDot() {
        assertEquals(22.5, Fmt.parse("22,5")!!, 0.0)
        assertEquals(22.5, Fmt.parse("22.5")!!, 0.0)
        assertEquals(10.0, Fmt.parse(" 10 ")!!, 0.0)
        assertNull(Fmt.parse(""))
        assertNull(Fmt.parse("abc"))
        assertNull(Fmt.parse("1,2,3"))
    }

    @Test fun number_formatsPtBr() {
        assertEquals("22,5", Fmt.number(22.5))
        assertEquals("40", Fmt.number(40.0))
        assertEquals("1.234", Fmt.number(1234))
        assertEquals("1234", Fmt.plain(1234))
        assertEquals("", Fmt.number(null))
    }

    @Test fun sanitize_keepsOnlyAllowedChars() {
        assertEquals("22,5", Fmt.sanitize("22,5kg", FieldKey.KG))
        assertEquals("12", Fmt.sanitize("12,5", FieldKey.REPS))
        assertEquals("1234567", Fmt.sanitize("123456789", FieldKey.KG))
    }

    @Test fun set_formatsEachType() {
        assertEquals("40 kg × 12", Fmt.set(RecordType.WR, LoggedSet(kg = 40.0, reps = 12)))
        assertEquals("– kg × 12", Fmt.set(RecordType.WR, LoggedSet(reps = 12)))
        assertEquals("10 reps", Fmt.set(RecordType.R, LoggedSet(reps = 10)))
        assertEquals("35 s", Fmt.set(RecordType.S, LoggedSet(seconds = 35)))
        assertEquals("16 kg · 40 m", Fmt.set(RecordType.WM, LoggedSet(kg = 16.0, meters = 40)))
        assertEquals("30 min · 7,5%", Fmt.set(RecordType.CARDIO, LoggedSet(minutes = 30, incline = 7.5)))
        assertEquals("—", Fmt.set(RecordType.WR, null))
        assertEquals("42,5×10", Fmt.setShort(RecordType.WR, LoggedSet(kg = 42.5, reps = 10)))
    }

    @Test fun dates_andClocks() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val ms = LocalDate.of(2026, 11, 9).atTime(7, 5).atZone(zone).toInstant().toEpochMilli()
        assertEquals("seg, 09/11", Fmt.dateLabel(ms, zone))
        assertEquals("07:05", Fmt.hhmm(ms, zone))
        assertEquals("07:42", Fmt.clock(462_000))
        assertEquals("1:02:03", Fmt.clock(3_723_000))
        assertEquals("1:30", Fmt.seconds(90))
        assertEquals("0:05", Fmt.seconds(5))
    }
}
