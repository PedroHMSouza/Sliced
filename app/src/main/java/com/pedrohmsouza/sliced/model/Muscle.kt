package com.pedrohmsouza.sliced.model

/** Grupos musculares (os mesmos do catálogo de exercícios), com o nome em português. */
enum class Muscle(val label: String) {
    CHEST("Peito"),
    SHOULDERS("Ombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    FOREARMS("Antebraços"),
    TRAPS("Trapézio"),
    LATS("Dorsais"),
    MIDDLE_BACK("Meio das costas"),
    LOWER_BACK("Lombar"),
    ABS("Abdômen"),
    GLUTES("Glúteos"),
    QUADS("Quadríceps"),
    HAMSTRINGS("Posteriores da coxa"),
    ADDUCTORS("Adutores"),
    ABDUCTORS("Abdutores"),
    CALVES("Panturrilhas"),
    NECK("Pescoço"),
}
