package com.pedrohmsouza.sliced.model

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
import com.pedrohmsouza.sliced.model.RecordType.CARDIO
import com.pedrohmsouza.sliced.model.RecordType.R
import com.pedrohmsouza.sliced.model.RecordType.S
import com.pedrohmsouza.sliced.model.RecordType.WM
import com.pedrohmsouza.sliced.model.RecordType.WR

/** Exercício do catálogo embutido: fotos de início e fim do movimento + músculos trabalhados. */
data class LibraryExercise(
    val id: String,
    val name: String,
    val type: RecordType,
    val primary: List<Muscle>,
    val secondary: List<Muscle>,
) {
    /** Fotos em assets/exercises/<id>/: 0 = início do movimento, 1 = fim. */
    val frames: List<String> get() = listOf("exercises/$id/0.webp", "exercises/$id/1.webp")
}

/**
 * Catálogo de exercícios com fotos, gerado a partir do free-exercise-db
 * (github.com/yuhonas/free-exercise-db, domínio público / Unlicense). As fotos ficam
 * dentro do APK, então funcionam sem internet. Gerado por um script; para incluir outro
 * exercício, acrescente uma linha e as duas fotos em assets/exercises/<id>/.
 */
object ExerciseLibrary {
    val all: List<LibraryExercise> = listOf(
        LibraryExercise("Machine_Bench_Press", "Supino reto na máquina", WR, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Barbell_Bench_Press_-_Medium_Grip", "Supino reto com barra", WR, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Dumbbell_Bench_Press", "Supino reto com halteres", WR, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Incline_Dumbbell_Press", "Supino inclinado com halteres", WR, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Barbell_Incline_Bench_Press_-_Medium_Grip", "Supino inclinado com barra", WR, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Dumbbell_Flyes", "Crucifixo com halteres", WR, listOf(CHEST), listOf()),
        LibraryExercise("Butterfly", "Voador (peck deck)", WR, listOf(CHEST), listOf()),
        LibraryExercise("Cable_Crossover", "Crossover na polia", WR, listOf(CHEST), listOf(SHOULDERS)),
        LibraryExercise("Pushups", "Flexão de braço", R, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Dips_-_Chest_Version", "Paralelas (peito)", R, listOf(CHEST), listOf(SHOULDERS, TRICEPS)),
        LibraryExercise("Wide-Grip_Lat_Pulldown", "Puxada frontal (pulley)", WR, listOf(LATS), listOf(BICEPS, MIDDLE_BACK, SHOULDERS)),
        LibraryExercise("Close-Grip_Front_Lat_Pulldown", "Puxada fechada (pulley)", WR, listOf(LATS), listOf(BICEPS, MIDDLE_BACK, SHOULDERS)),
        LibraryExercise("Pullups", "Barra fixa", R, listOf(LATS), listOf(BICEPS, MIDDLE_BACK)),
        LibraryExercise("Seated_Cable_Rows", "Remada baixa na polia", WR, listOf(MIDDLE_BACK), listOf(BICEPS, LATS, SHOULDERS)),
        LibraryExercise("Leverage_Iso_Row", "Remada na máquina", WR, listOf(LATS), listOf(BICEPS, MIDDLE_BACK)),
        LibraryExercise("Bent_Over_Barbell_Row", "Remada curvada com barra", WR, listOf(MIDDLE_BACK), listOf(BICEPS, LATS, SHOULDERS)),
        LibraryExercise("One-Arm_Dumbbell_Row", "Remada unilateral com halter", WR, listOf(MIDDLE_BACK), listOf(BICEPS, LATS, SHOULDERS)),
        LibraryExercise("T-Bar_Row_with_Handle", "Remada cavalinho", WR, listOf(MIDDLE_BACK), listOf(BICEPS, LATS)),
        LibraryExercise("Straight-Arm_Pulldown", "Pulldown com braços estendidos", WR, listOf(LATS), listOf()),
        LibraryExercise("Hyperextensions_Back_Extensions", "Extensão lombar (banco romano)", R, listOf(LOWER_BACK), listOf(GLUTES, HAMSTRINGS)),
        LibraryExercise("Barbell_Deadlift", "Levantamento terra", WR, listOf(LOWER_BACK), listOf(CALVES, FOREARMS, GLUTES, HAMSTRINGS, LATS, MIDDLE_BACK, QUADS, TRAPS)),
        LibraryExercise("Leverage_Shoulder_Press", "Desenvolvimento na máquina", WR, listOf(SHOULDERS), listOf(TRICEPS)),
        LibraryExercise("Dumbbell_Shoulder_Press", "Desenvolvimento com halteres", WR, listOf(SHOULDERS), listOf(TRICEPS)),
        LibraryExercise("Standing_Military_Press", "Desenvolvimento militar com barra", WR, listOf(SHOULDERS), listOf(TRICEPS)),
        LibraryExercise("Side_Lateral_Raise", "Elevação lateral com halteres", WR, listOf(SHOULDERS), listOf()),
        LibraryExercise("Front_Dumbbell_Raise", "Elevação frontal com halteres", WR, listOf(SHOULDERS), listOf()),
        LibraryExercise("Face_Pull", "Face pull na polia (corda)", WR, listOf(SHOULDERS), listOf(MIDDLE_BACK)),
        LibraryExercise("Reverse_Flyes", "Crucifixo inverso", WR, listOf(SHOULDERS), listOf()),
        LibraryExercise("Upright_Barbell_Row", "Remada alta com barra", WR, listOf(SHOULDERS), listOf(TRAPS)),
        LibraryExercise("Dumbbell_Shrug", "Encolhimento com halteres", WR, listOf(TRAPS), listOf()),
        LibraryExercise("Dumbbell_Bicep_Curl", "Rosca direta com halteres", WR, listOf(BICEPS), listOf(FOREARMS)),
        LibraryExercise("Barbell_Curl", "Rosca direta com barra", WR, listOf(BICEPS), listOf(FOREARMS)),
        LibraryExercise("Hammer_Curls", "Rosca martelo com halteres", WR, listOf(BICEPS), listOf()),
        LibraryExercise("Preacher_Curl", "Rosca Scott", WR, listOf(BICEPS), listOf()),
        LibraryExercise("Concentration_Curls", "Rosca concentrada", WR, listOf(BICEPS), listOf(FOREARMS)),
        LibraryExercise("Incline_Dumbbell_Curl", "Rosca inclinada com halteres", WR, listOf(BICEPS), listOf()),
        LibraryExercise("Triceps_Pushdown", "Tríceps na polia (barra)", WR, listOf(TRICEPS), listOf()),
        LibraryExercise("Triceps_Pushdown_-_Rope_Attachment", "Tríceps na polia (corda)", WR, listOf(TRICEPS), listOf()),
        LibraryExercise("EZ-Bar_Skullcrusher", "Tríceps testa", WR, listOf(TRICEPS), listOf(FOREARMS)),
        LibraryExercise("Standing_Dumbbell_Triceps_Extension", "Tríceps francês com halter", WR, listOf(TRICEPS), listOf()),
        LibraryExercise("Tricep_Dumbbell_Kickback", "Tríceps coice", WR, listOf(TRICEPS), listOf()),
        LibraryExercise("Bench_Dips", "Mergulho no banco", R, listOf(TRICEPS), listOf(CHEST, SHOULDERS)),
        LibraryExercise("Leg_Press", "Leg press 45°", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Smith_Machine_Squat", "Agachamento no Smith", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS, LOWER_BACK)),
        LibraryExercise("Hack_Squat", "Agachamento hack", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Barbell_Squat", "Agachamento livre com barra", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS, LOWER_BACK)),
        LibraryExercise("Goblet_Squat", "Agachamento goblet", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS, SHOULDERS)),
        LibraryExercise("Leg_Extensions", "Cadeira extensora", WR, listOf(QUADS), listOf()),
        LibraryExercise("Lying_Leg_Curls", "Mesa flexora", WR, listOf(HAMSTRINGS), listOf()),
        LibraryExercise("Seated_Leg_Curl", "Cadeira flexora (sentado)", WR, listOf(HAMSTRINGS), listOf()),
        LibraryExercise("Stiff-Legged_Dumbbell_Deadlift", "Stiff com halteres", WR, listOf(HAMSTRINGS), listOf(GLUTES, LOWER_BACK)),
        LibraryExercise("Romanian_Deadlift", "Levantamento terra romeno", WR, listOf(HAMSTRINGS), listOf(CALVES, GLUTES, LOWER_BACK)),
        LibraryExercise("Split_Squat_with_Dumbbells", "Afundo estático com halteres", WR, listOf(QUADS), listOf(GLUTES, HAMSTRINGS)),
        LibraryExercise("Dumbbell_Lunges", "Avanço com halteres", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Dumbbell_Step_Ups", "Subida no banco com halteres", WR, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Barbell_Hip_Thrust", "Elevação pélvica", WR, listOf(GLUTES), listOf(CALVES, HAMSTRINGS)),
        LibraryExercise("Glute_Kickback", "Glúteo coice", R, listOf(GLUTES), listOf(HAMSTRINGS)),
        LibraryExercise("Thigh_Abductor", "Cadeira abdutora", WR, listOf(ABDUCTORS), listOf(GLUTES)),
        LibraryExercise("Thigh_Adductor", "Cadeira adutora", WR, listOf(ADDUCTORS), listOf(GLUTES, HAMSTRINGS)),
        LibraryExercise("Standing_Calf_Raises", "Panturrilha em pé", WR, listOf(CALVES), listOf()),
        LibraryExercise("Seated_Calf_Raise", "Panturrilha sentado", WR, listOf(CALVES), listOf()),
        LibraryExercise("Plank", "Prancha frontal", S, listOf(ABS), listOf()),
        LibraryExercise("Side_Bridge", "Prancha lateral", S, listOf(ABS), listOf(SHOULDERS)),
        LibraryExercise("Dead_Bug", "Dead bug", R, listOf(ABS), listOf()),
        LibraryExercise("Crunches", "Abdominal", R, listOf(ABS), listOf()),
        LibraryExercise("Cable_Crunch", "Abdominal na polia", WR, listOf(ABS), listOf()),
        LibraryExercise("Hanging_Leg_Raise", "Elevação de pernas na barra", R, listOf(ABS), listOf()),
        LibraryExercise("Russian_Twist", "Giro russo", R, listOf(ABS), listOf(LOWER_BACK)),
        LibraryExercise("Ab_Roller", "Roda abdominal", R, listOf(ABS), listOf(SHOULDERS)),
        LibraryExercise("Mountain_Climbers", "Escalador", R, listOf(QUADS), listOf(CHEST, HAMSTRINGS, SHOULDERS)),
        LibraryExercise("Walking_Treadmill", "Esteira", CARDIO, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Bicycling_Stationary", "Bicicleta ergométrica", CARDIO, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Elliptical_Trainer", "Elíptico", CARDIO, listOf(QUADS), listOf(CALVES, GLUTES, HAMSTRINGS)),
        LibraryExercise("Rowing_Stationary", "Remo ergômetro", CARDIO, listOf(QUADS), listOf(BICEPS, CALVES, GLUTES, HAMSTRINGS, LOWER_BACK, MIDDLE_BACK)),
        LibraryExercise("Rope_Jumping", "Pular corda", CARDIO, listOf(QUADS), listOf(CALVES, HAMSTRINGS)),
        LibraryExercise("Farmers_Walk", "Caminhada do fazendeiro", WM, listOf(FOREARMS), listOf(ABS, GLUTES, HAMSTRINGS, LOWER_BACK, QUADS, TRAPS)),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String?): LibraryExercise? = id?.let(byId::get)
}
