package com.ayacucho.medicitas.model

/**
 * Modelo de datos para la Especialidad Médica.
 * Representa una rama de la medicina disponible en la clínica privada.
 *
 * RF03.3: Listado de especialidades disponibles.
 * RF03.4: El paciente selecciona una especialidad para ver médicos asignados.
 * RF06.1: El administrador puede registrar, editar y eliminar especialidades.
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class Especialidad(
    val idEspecialidad: String = "",     // ID autogenerado por Firestore
    val nombreEspecialidad: String = "",
    val descripcion: String = "",
    val estado: String = "Activo",       // Activo, Inactivo
    val precioConsulta: Double = 0.0     // Precio de la consulta en soles (S/.)
)
