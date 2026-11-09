package com.pedrohmsouza.sliced.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerTest {

    private class FakeEffects : RestEffects {
        val log = mutableListOf<String>()
        override fun scheduleAlarm(endAt: Long, label: String) { log += "alarm@$endAt" }
        override fun cancelAlarm() { log += "cancelAlarm" }
        override fun showCountdown(state: RestState) { log += "countdown@${state.endAt}" }
        override fun showDone(label: String) { log += "done:$label" }
        override fun cancelNotifications() { log += "cancelNotif" }
        override fun alertInApp() { log += "beep" }
    }

    /** O relógio do timer acompanha o tempo virtual do teste. */
    private fun TestScope.timer(fx: FakeEffects) = RestTimer(backgroundScope, fx, null) { testScheduler.currentTime }

    @Test fun foreground_countsDownBeepsAndHidesAfter6s() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.onAppForeground()
        t.start(90, "Supino")
        assertEquals(90, t.state.value!!.secondsLeft(testScheduler.currentTime))
        assertTrue("alarm@90000" in fx.log)
        assertFalse(fx.log.any { it.startsWith("countdown") })

        advanceTimeBy(90_001)
        assertTrue(t.state.value!!.finished)
        assertEquals(1, fx.log.count { it == "beep" })

        advanceTimeBy(6_001)
        assertNull(t.state.value)
    }

    @Test fun background_showsCountdownAndDoneNotification() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.onAppForeground()
        t.start(60, "Remada")
        t.onAppBackground()
        assertTrue("countdown@60000" in fx.log)
        advanceTimeBy(60_001)
        assertTrue("done:Remada" in fx.log)
        assertFalse("beep" in fx.log)
    }

    @Test fun alarmAndCoroutineTogether_alertOnlyOnce() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.onAppForeground()
        t.start(60, "x")
        advanceTimeBy(60_000); runCurrent()
        t.onAlarm(60_000)
        assertEquals(1, fx.log.count { it == "beep" })
    }

    @Test fun staleAlarmIsIgnored() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.onAppForeground()
        t.start(60, "x")
        t.adjust(15)
        t.onAlarm(60_000) // alarme do horário antigo
        assertFalse(t.state.value!!.finished)
        assertEquals(75_000, t.state.value!!.endAt)
    }

    @Test fun adjustGrowsTotalLikeWeb() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.onAppForeground()
        t.start(60, "x")
        t.adjust(15)
        assertEquals(75, t.state.value!!.totalSec)
        t.adjust(-15)
        assertEquals(75, t.state.value!!.totalSec)
        assertEquals(60, t.state.value!!.secondsLeft(testScheduler.currentTime))
    }

    @Test fun skipCancelsEverything() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.onAppForeground()
        t.start(60, "x")
        t.stop()
        assertNull(t.state.value)
        assertTrue("cancelAlarm" in fx.log && "cancelNotif" in fx.log)
        advanceTimeBy(70_000)
        assertFalse("beep" in fx.log)
    }

    @Test fun zeroRestDoesNothing() = runTest {
        val fx = FakeEffects(); val t = timer(fx)
        t.start(0, "Cardio")
        assertNull(t.state.value)
        assertTrue(fx.log.isEmpty())
    }
}
