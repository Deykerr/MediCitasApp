package com.ayacucho.medicitas.utils

/**
 * Constantes globales del sistema MEDICITAS.
 * Centraliza valores reutilizables para evitar "magic strings" en el código.
 */
object Constants {

    // ==================== Nombres de Colecciones en Firestore ====================
    const val COLLECTION_PACIENTES = "pacientes"
    const val COLLECTION_PERSONAL_SALUD = "personal_salud"
    const val COLLECTION_ADMINISTRADORES = "administradores"
    const val COLLECTION_POSTAS = "postas_medicas"
    const val COLLECTION_ESPECIALIDADES = "especialidades"
    const val COLLECTION_HORARIOS = "horarios"
    const val COLLECTION_CITAS = "citas_medicas"
    const val COLLECTION_NOTIFICACIONES = "notificaciones"

    // ==================== Roles de Usuario (RF01.6) ====================
    const val ROL_PACIENTE = "Paciente"
    const val ROL_MEDICO = "Medico"
    const val ROL_ADMIN = "Admin"

    // ==================== Estados de Cita (RF04, RF05.3, RF08.1) ====================
    const val ESTADO_CITA_RESERVADA = "Reservada"
    const val ESTADO_CITA_CANCELADA = "Cancelada"
    const val ESTADO_CITA_ATENDIDA = "Atendida"
    const val ESTADO_CITA_NO_ASISTIO = "No Asistio"

    // ==================== Estados de Cuenta ====================
    const val ESTADO_ACTIVO = "Activo"
    const val ESTADO_INACTIVO = "Inactivo"

    // ==================== Estados de Horario (RF05.5) ====================
    const val ESTADO_HORARIO_DISPONIBLE = "Disponible"
    const val ESTADO_HORARIO_BLOQUEADO = "Bloqueado"

    // ==================== Estados de Lectura de Notificación ====================
    const val NOTIFICACION_LEIDA = "Leido"
    const val NOTIFICACION_NO_LEIDA = "No Leido"

    // ==================== Tipos de Notificación (RF07) ====================
    const val TIPO_NOTIFICACION_CONFIRMACION = "CONFIRMACION"
    const val TIPO_NOTIFICACION_RECORDATORIO = "RECORDATORIO"
    const val TIPO_NOTIFICACION_CANCELACION = "CANCELACION"
    const val TIPO_NOTIFICACION_CAMBIO = "CAMBIO"

    // ==================== Formato de Fechas ====================
    const val FORMATO_FECHA = "dd/MM/yyyy"
    const val FORMATO_HORA = "HH:mm"
    const val FORMATO_FECHA_HORA = "dd/MM/yyyy HH:mm"

    // ==================== Sesión (RF01.9) ====================
    const val TIEMPO_INACTIVIDAD_MS = 15 * 60 * 1000L // 15 minutos en milisegundos

    // ==================== Validaciones (RF01.8) ====================
    const val DNI_LENGTH = 8
}
