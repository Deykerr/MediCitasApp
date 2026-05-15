package com.ayacucho.medicitas.model

/**
 * Modelo de datos para un Slot de Horario específico.
 * En lugar de usar un contador "cupos", generamos slots exactos de tiempo.
 */
data class SlotHorario(
    val idSlot: String = "",
    val idHorario: String = "",       // ID del Horario maestro (ej. turno mañana)
    val idMedico: String = "",        // Para filtrar rápido por médico
    val fecha: String = "",           // Ej: 15/05/2026
    val horaInicio: String = "",      // Ej: 08:00
    val horaFin: String = "",         // Ej: 08:30
    val estado: String = "Disponible",// Disponible, Reservado, Bloqueado
    val idCita: String = ""           // Si está reservado, ID de la cita
)
