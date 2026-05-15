package com.ayacucho.medicitas.model

/**
 * Modelo de datos para un Paciente Dependiente (Familiar asociado).
 * Permite a un paciente principal (titular) registrar y gestionar citas
 * para familiares (hijos, padres, cónyuge, etc.) sin necesidad de crearles
 * una cuenta independiente con correo/contraseña.
 */
data class Dependiente(
    val idDependiente: String = "",
    val idTitular: String = "",       // ID del paciente principal
    val nombres: String = "",
    val apellidos: String = "",
    val dni: String = "",
    val fechaNacimiento: String = "",
    val relacion: String = "",        // Ej: "Hijo", "Madre", "Cónyuge"
    val sexo: String = "",
    val alergias: String = "",
    val fechaRegistro: String = ""
)
