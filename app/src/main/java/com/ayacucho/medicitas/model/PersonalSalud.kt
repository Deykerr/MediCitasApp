package com.ayacucho.medicitas.model

/**
 * Modelo de datos para el Personal de Salud (Médico).
 * Representa a un profesional médico asignado a una especialidad en la clínica privada.
 *
 * RF03.4: Los pacientes pueden visualizar los médicos asignados a una especialidad.
 * RF05: El médico gestiona su agenda de citas.
 * RF06.2: El administrador puede registrar, editar y desactivar cuentas del personal médico.
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class PersonalSalud(
    val idPersonal: String = "",         // UID generado por Firebase Auth
    val nombres: String = "",
    val apellidos: String = "",
    val dni: String = "",
    val correoElectronico: String = "",
    val idEspecialidad: String = "",      // Referencia al documento de Especialidad en Firestore
    val nombreEspecialidad: String = "",  // Desnormalizado para consultas rápidas
    val estado: String = "Activo",       // Activo, Inactivo (RF06.2: desactivar cuentas)
    val rol: String = "Medico"           // Siempre "Medico" para esta entidad (RF01.6)
)
