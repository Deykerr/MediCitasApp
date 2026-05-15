package com.ayacucho.medicitas.model

/**
 * Modelo de datos para una Sesión de Tratamiento.
 * Representa una sesión individual dentro de un Plan de Tratamiento.
 * El paciente agenda y paga cada sesión independientemente.
 *
 * Ejemplo: "Sesión 3 de 10 — Fisioterapia" agendada para el 15/05/2026 a las 10:00.
 *
 * Campos desnormalizados para facilitar consultas sin joins.
 */
data class SesionTratamiento(
    val idSesion: String = "",
    val idPlan: String = "",                  // Referencia al plan de tratamiento
    val numeroSesion: Int = 0,                // Sesión 1, 2, 3...
    val fecha: String = "",                   // dd/MM/yyyy (vacío si no agendada)
    val hora: String = "",                    // HH:mm (vacío si no agendada)
    val estado: String = "Pendiente",         // Pendiente, Agendada, Atendida, No Asistió, Cancelada
    val notas: String = "",                   // Notas del médico después de la sesión
    val idMedico: String = "",
    val nombreMedico: String = "",
    val idPaciente: String = "",
    val nombrePaciente: String = "",
    val nombreEspecialidad: String = "",
    val precioPagado: Double = 0.0,
    val estadoPago: String = "Pendiente",     // Pendiente, Pagado, Reembolsado
    val metodoPago: String = "",
    val referenciaPago: String = "",
    val idHorario: String = ""                // Horario donde se agendó (para restaurar cupo)
)
