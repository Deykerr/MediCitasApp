package com.ayacucho.medicitas.model

/**
 * Modelo de datos para el Paciente.
 * Representa a un usuario con rol de paciente en el sistema MEDICITAS.
 *
 * RF01.1: Registro con datos personales básicos (DNI, nombres, apellidos, teléfono, correo).
 * RF01.2: El DNI debe ser único (validación en Repository/ViewModel).
 * RF01.8: Validación de formato de DNI (8 dígitos numéricos).
 *
 * Nota: La contraseña NO se almacena en Firestore por seguridad,
 * Firebase Authentication se encarga de la gestión de credenciales (RNF01.4).
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class Paciente(
    val idPaciente: String = "",       // UID generado por Firebase Auth
    val nombres: String = "",
    val apellidos: String = "",
    val dni: String = "",              // 8 dígitos, único en el sistema
    val fechaNacimiento: String = "",  // Formato "dd/MM/yyyy"
    val telefono: String = "",
    val correoElectronico: String = "",
    val estadoCuenta: String = "Activo", // Activo, Inactivo
    val fechaRegistro: String = "",    // Formato "dd/MM/yyyy HH:mm"
    val rol: String = "Paciente"       // Siempre "Paciente" para esta entidad (RF01.6)
)
