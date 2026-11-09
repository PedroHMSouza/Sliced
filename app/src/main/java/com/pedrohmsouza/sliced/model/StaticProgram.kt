package com.pedrohmsouza.sliced.model

import com.pedrohmsouza.sliced.model.RecordType.CARDIO
import com.pedrohmsouza.sliced.model.RecordType.R
import com.pedrohmsouza.sliced.model.RecordType.S
import com.pedrohmsouza.sliced.model.RecordType.WM
import com.pedrohmsouza.sliced.model.RecordType.WR

/** A ficha atual: divisão de 5 dias, cada grupo muscular 2x por semana. */
object StaticProgram : ProgramSource {
    override val treinos: List<Treino> = listOf(
        Treino(
            "A", "Superior 1", "Segunda", listOf(
                PlannedExercise("supino-maquina", "Supino reto na máquina", 3, 8..12, 90, WR, "Escápulas encostadas no banco"),
                PlannedExercise("puxada-frontal", "Puxada frontal (pulley)", 3, 10..12, 90, WR, "Puxe até o queixo, sem jogar o tronco"),
                PlannedExercise("desenvolvimento-maquina", "Desenvolvimento na máquina", 2, 10..12, 75, WR, "Não trave os cotovelos no alto"),
                PlannedExercise("triceps-polia", "Tríceps na polia (barra ou corda)", 2, 10..15, 60, WR, "Cotovelos parados ao lado do corpo"),
                PlannedExercise("rosca-direta", "Rosca direta com halteres", 2, 10..12, 60, WR, "Sem balançar o tronco"),
            ),
        ),
        Treino(
            "B", "Inferior 1", "Terça", listOf(
                PlannedExercise("leg-press-45", "Leg press 45°", 3, 10..12, 90, WR, "Lombar colada no encosto; desça até ~90° no joelho"),
                PlannedExercise("mesa-flexora", "Mesa flexora", 3, 10..12, 75, WR, "Quadril colado no banco"),
                PlannedExercise("cadeira-extensora", "Cadeira extensora", 2, 12..15, 60, WR, "Segure 1 s em cima"),
                PlannedExercise("elevacao-pelvica", "Elevação pélvica (máquina ou banco)", 2, 10..12, 75, WR, "Aperte o glúteo no topo"),
                PlannedExercise("panturrilha-pe", "Panturrilha em pé", 2, 12..15, 60, WR, "Amplitude completa, pausa embaixo"),
            ),
        ),
        Treino(
            "C", "Condicionamento e core", "Quarta", listOf(
                PlannedExercise(
                    "cardio-continuo", "Cardio contínuo", 1, null, 0, CARDIO,
                    "Esteira inclinada (6–10%, 5–6 km/h), bike ou elíptico. Ritmo em que dá para falar frases curtas.",
                    target = "30 min",
                ),
                PlannedExercise("dead-bug", "Dead bug", 2, 8..10, 60, R, "Lombar sempre encostada no chão", target = "2 voltas × 8–10 por lado"),
                PlannedExercise("prancha-frontal", "Prancha frontal", 2, null, 60, S, "Corpo reto; apoie os joelhos se precisar", target = "2 × 20–40 s"),
                PlannedExercise("prancha-lateral", "Prancha lateral", 2, null, 60, S, "Joelhos apoiados se precisar", target = "2 × 15–30 s por lado"),
                PlannedExercise("fazendeiro", "Caminhada do fazendeiro", 2, null, 60, WM, "Postura ereta, carga moderada (kg por mão)", target = "2 × 30–40 m"),
            ),
        ),
        Treino(
            "D", "Superior 2", "Quinta", listOf(
                PlannedExercise("remada-maquina", "Remada na máquina", 3, 8..12, 90, WR, "Peito apoiado poupa a lombar"),
                PlannedExercise("supino-inclinado-halteres", "Supino inclinado com halteres", 3, 8..12, 90, WR, "Banco a ~30°"),
                PlannedExercise("elevacao-lateral", "Elevação lateral com halteres", 2, 12..15, 60, WR, "Carga leve, sobe até a altura do ombro"),
                PlannedExercise("face-pull", "Face pull na polia (corda)", 2, 12..15, 60, WR, "Puxe em direção à testa"),
                PlannedExercise("rosca-martelo", "Rosca martelo com halteres", 2, 10..12, 60, WR, "Alternado ou simultâneo"),
            ),
        ),
        Treino(
            "E", "Inferior 2", "Sexta", listOf(
                PlannedExercise("agachamento-smith", "Agachamento no Smith ou hack", 3, 8..12, 90, WR, "Desça só até onde mantém a coluna neutra"),
                PlannedExercise("stiff-halteres", "Stiff com halteres", 3, 10..12, 90, WR, "Quadril para trás, joelhos semiflexionados"),
                PlannedExercise("afundo-estatico", "Afundo estático", 2, 10..10, 75, WR, "Segure num banco ou máquina para equilíbrio", target = "2 × 10 por perna"),
                PlannedExercise("flexora-sentado", "Cadeira flexora (sentado)", 2, 10..12, 60, WR, "Volta controlada"),
                PlannedExercise("panturrilha-sentado", "Panturrilha sentado", 2, 15..20, 60, WR, "Pausa de 1 s embaixo"),
            ),
        ),
    )
}
