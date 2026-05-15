package com.ayacucho.medicitas.model

/**
 * Modelo de datos para la Atención Médica.
 * Representa el registro clínico de una consulta finalizada.
 */
data class AtencionMedica(
    val idAtencion: String = "",
    val idCita: String = "",
    val idPaciente: String = "",
    val idMedico: String = "",
    val motivoConsulta: String = "",
    val sintomas: String = "",
    val diagnostico: String = "",
    val indicaciones: String = "",
    val observaciones: String = "",
    val fechaAtencion: String = "" // Formato dd/MM/yyyy HH:mm
)
