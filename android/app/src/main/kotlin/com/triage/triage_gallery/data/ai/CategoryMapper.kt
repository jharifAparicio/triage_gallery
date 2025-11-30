package com.triage.triage_gallery.data.ai

object CategoryMapper {

    const val CAT_PEOPLE = "cat_people"
    const val CAT_PETS = "cat_pets"
    const val CAT_FOOD = "cat_food"
    const val CAT_NATURE = "cat_nature"
    const val CAT_DOCUMENTS = "cat_documents"
    const val CAT_VEHICLES = "cat_vehicles"
    const val CAT_OTHER = "cat_other"

    fun mapLabelToCategoryId(label: String): String {
        // Como ahora TU entrenaste el modelo, las etiquetas son exactamente
        // los nombres de las carpetas que creaste en 'dataset/train/'
        val l = label.lowercase().trim()

        return when (l) {
            "personas" -> CAT_PEOPLE
            "mascotas" -> CAT_PETS
            "comidas"   -> CAT_FOOD
            "paisajes" -> CAT_NATURE
            "documentos"     -> CAT_DOCUMENTS // O "documentos", según tu carpeta
            "vehiculos" -> CAT_VEHICLES
            else -> CAT_OTHER
        }
    }

    fun getCategoryName(id: String): String {
        return when(id) {
            CAT_PEOPLE -> "Personas"
            CAT_PETS -> "Mascotas"
            CAT_FOOD -> "Comida"
            CAT_NATURE -> "Naturaleza"
            CAT_DOCUMENTS -> "Documentos"
            CAT_VEHICLES -> "Vehículos"
            else -> "Otros"
        }
    }
}