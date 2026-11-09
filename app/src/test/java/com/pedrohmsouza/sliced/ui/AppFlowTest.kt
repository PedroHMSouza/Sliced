package com.pedrohmsouza.sliced.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohmsouza.sliced.MainActivity
import com.pedrohmsouza.sliced.SlicedApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Teste de ponta a ponta na JVM: abre a MainActivity de verdade (Room, navegação, timer)
 * e faz um treino curto: começar → preencher e marcar uma série → finalizar → salvar.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = Screens.PHONE)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = (compose.activity.application as SlicedApp).container

    @Test fun fullWorkoutIsSavedToHistory() {
        // Sem histórico: o próximo treino segue o dia da semana; começamos o que estiver aberto.
        val start = compose.onAllNodes(hasText("Começar Treino", substring = true)).fetchSemanticsNodes()
        assertEquals(1, start.size)
        compose.onNode(hasText("Começar Treino", substring = true)).performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Finalizar")).fetchSemanticsNodes().isNotEmpty() }

        val draft = runBlocking { container.drafts.draft.first() }
        assertNotNull(draft)
        val first = draft!!.exercises.first()

        // Primeiro campo do primeiro exercício (para C é o cardio: minutos).
        val field = first.type.fields.first().label
        compose.onNodeWithContentDescription("${first.name}, série 1, $field").performTextInput("30")
        compose.onAllNodesWithContentDescription("Série 1 feita")[0].performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/flow_workout.png")

        // O rascunho foi persistido no Room com a série marcada.
        compose.waitUntil(5_000) { runBlocking { container.drafts.draft.first()?.exercises?.first()?.sets?.first()?.done == true } }

        compose.onNodeWithText("Finalizar").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Salvar treino")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Salvar treino").performClick()

        compose.waitUntil(10_000) { runBlocking { container.sessions.sessions.first().isNotEmpty() } }
        // Aviso "Treino X salvo · N min" e a tela de Histórico (números do topo).
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("salvo", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/flow_history.png")
        compose.onNodeWithText("Tempo médio", ignoreCase = true).assertIsDisplayed()

        val sessions = runBlocking { container.sessions.sessions.first() }
        assertEquals(1, sessions.size)
        assertEquals(draft.treino, sessions[0].treino)
        assertEquals(null, runBlocking { container.drafts.draft.first() })
        container.restTimer.stop()
    }
}
