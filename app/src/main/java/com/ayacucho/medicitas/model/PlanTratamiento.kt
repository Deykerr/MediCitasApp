package com.ayacucho.medicitas.model

/**
 * Modelo de datos para el Plan de Tratamiento.
 * Representa un tratamiento prescrito por un médico después de una consulta.
 * Contiene N sesiones que el paciente debe completar.
 *
 * Ejemplo: Después de una consulta de traumatología, el médico prescribe
 * 10 sesiones de fisioterapia a S/. 40 cada una.
 *
 * Campos desnormalizados para evitar lecturas adicionales.
 */
data class PlanTratamiento(
    val idPlan: String = "",
    val idPaciente: String = "",
    val nombrePaciente: String = "",
    val dniPaciente: String = "",
    val idMedico: String = "",
    val nombreMedico: String = "",
    val idEspecialidad: String = "",
    val nombreEspecialidad: String = "",
    val diagnostico: String = "",             // Diagnóstico del médico
    val descripcionTratamiento: String = "",   // Ej: "Rehabilitación de rodilla derecha"
    val totalSesiones: Int = 0,
    val sesionesCompletadas: Int = 0,
    val precioPorSesion: Double = 0.0,        // Precio independiente de la consulta
    val estado: String = "Activo",            // Activo, Completado, Cancelado
    val fechaCreacion: String = "",
    val idCitaOrigen: String = ""             // La cita de consulta que generó este plan
)
