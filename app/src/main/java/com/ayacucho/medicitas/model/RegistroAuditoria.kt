package com.ayacucho.medicitas.model

/**
 * Modelo de datos para el Historial de Auditoría.
 * Permite registrar acciones críticas realizadas por usuarios del sistema.
 */
data class RegistroAuditoria(
    val idRegistro: String = "",
    val fechaHora: String = "",
    val idUsuario: String = "",
    val rolUsuario: String = "",
    val accion: String = "",             // Ej: "CREAR_CITA", "CANCELAR_CITA", "DESACTIVAR_MEDICO"
    val entidadAfectada: String = "",    // Ej: "CitaMedica", "PersonalSalud"
    val idEntidadAfectada: String = "",  // ID del documento afectado
    val detalles: String = ""            // Detalles adicionales en formato texto
)
