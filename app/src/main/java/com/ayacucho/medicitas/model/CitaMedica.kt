package com.ayacucho.medicitas.model

/**
 * Modelo de datos para la Cita Médica.
 * Representa una reserva de atención médica hecha por un paciente.
 *
 * RF04.1: Confirmar reserva en horario disponible.
 * RF04.2: Validar disponibilidad antes de confirmar.
 * RF04.3: Evitar citas duplicadas en el mismo horario.
 * RF04.4: Generar comprobante digital con detalles de la cita.
 * RF04.5: Historial de citas clasificadas en "Próximas" y "Pasadas".
 * RF04.6: Cancelar cita reservada.
 * RF04.7: Reprogramar cita si hay cupos disponibles.
 * RF05.3: El médico cambia el estado (Atendido, No asistió, Cancelado).
 * RF08.1: Registrar asistencia del paciente.
 *
 * Campos desnormalizados (nombrePaciente, nombreMedico, etc.) para evitar
 * lecturas adicionales a Firestore al listar citas.
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class CitaMedica(
    val idCita: String = "",             // ID autogenerado por Firestore
    val fecha: String = "",              // Formato "dd/MM/yyyy"
    val hora: String = "",               // Formato "HH:mm"
    val estadoCita: String = "Reservada", // Reservada, Cancelada, Atendida, No Asistió
    val motivoConsulta: String = "",
    val idPaciente: String = "",         // Referencia al paciente
    val nombrePaciente: String = "",     // Desnormalizado para visualización rápida (RF05.2)
    val dniPaciente: String = "",        // Desnormalizado para que el médico lo vea (RF05.2)
    val idPersonal: String = "",         // Referencia al médico
    val nombreMedico: String = "",       // Desnormalizado para el comprobante (RF04.4)
    val idPosta: String = "",            // Referencia a la posta médica
    val nombrePosta: String = "",        // Desnormalizado para el comprobante (RF04.4)
    val idEspecialidad: String = "",     // Referencia a la especialidad
    val nombreEspecialidad: String = "", // Desnormalizado para el comprobante (RF04.4)
    val fechaCreacion: String = ""       // Fecha y hora de creación de la reserva
)
