package com.ayacucho.medicitas.model

/**
 * Modelo de datos para la Posta Médica.
 * Representa un establecimiento de salud en Ayacucho donde se brindan los servicios.
 *
 * RF03.1: Listado de postas médicas disponibles.
 * RF03.2: El paciente puede seleccionar una posta médica.
 * RF06.5: El administrador gestiona las postas (registrar, editar, eliminar).
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class PostaMedica(
    val idPosta: String = "",            // ID autogenerado por Firestore
    val nombrePosta: String = "",
    val direccion: String = "",
    val distrito: String = "",           // Distrito dentro de Ayacucho
    val telefono: String = "",
    val estado: String = "Activo"        // Activo, Inactivo
)
