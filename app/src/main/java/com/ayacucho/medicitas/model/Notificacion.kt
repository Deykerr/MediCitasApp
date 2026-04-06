package com.ayacucho.medicitas.model

/**
 * Modelo de datos para las Notificaciones del sistema.
 * Representa una alerta o recordatorio enviado a un usuario.
 *
 * RF07.1: Notificación al confirmar reserva.
 * RF07.2: Recordatorios antes de la cita programada.
 * RF07.3: Notificación sobre cambios o cancelaciones.
 * RF07.4: Notificaciones al personal médico sobre citas programadas.
 * RF07.5: Configurar el tiempo de anticipación de recordatorios.
 *
 * Nota: Firebase Cloud Messaging (FCM) se usa para el envío push (RNF01.4),
 * pero este modelo almacena el historial de notificaciones en Firestore
 * para que el usuario pueda revisarlas dentro de la app.
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class Notificacion(
    val idNotificacion: String = "",     // ID autogenerado por Firestore
    val titulo: String = "",
    val mensaje: String = "",
    val tipo: String = "",               // "CONFIRMACION", "RECORDATORIO", "CANCELACION", "CAMBIO"
    val fechaHora: String = "",          // Formato "dd/MM/yyyy HH:mm"
    val estadoLectura: String = "No Leido", // "Leido", "No Leido"
    val idUsuario: String = "",          // UID del destinatario (paciente o médico)
    val idCita: String = ""              // Referencia a la cita relacionada (opcional)
)
