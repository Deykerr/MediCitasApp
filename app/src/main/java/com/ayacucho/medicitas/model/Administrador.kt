package com.ayacucho.medicitas.model

/**
 * Modelo de datos para el Administrador del sistema.
 * Representa al usuario con rol administrativo que gestiona el sistema MEDICITAS.
 *
 * RF06: El administrador realiza el mantenimiento del sistema (CRUD de especialidades,
 *       personal médico, postas, horarios, citas y reportes).
 * RF01.6: Rol asignado al momento de creación.
 * RF01.7: Acceso restringido según rol.
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class Administrador(
    val idAdministrador: String = "",    // UID generado por Firebase Auth
    val nombres: String = "",
    val apellidos: String = "",
    val correoElectronico: String = "",
    val rol: String = "Admin",           // Siempre "Admin" para esta entidad (RF01.6)
    val estado: String = "Activo"        // Activo, Inactivo
)
