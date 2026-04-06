package com.ayacucho.medicitas.model

/**
 * Modelo de datos para el Horario de atención médica.
 * Representa un bloque de tiempo en el que un médico está disponible para citas.
 *
 * RF03.5: Calendario con fechas, horarios y cupos disponibles del médico.
 * RF05.4: El médico visualiza sus horarios disponibles.
 * RF05.5: El médico puede bloquear horarios por ausencia o imprevistos.
 * RF06.3: El administrador configura bloques de horarios y los asigna a médicos.
 *
 * Todas las propiedades tienen valores por defecto para la deserialización de Firestore.
 */
data class Horario(
    val idHorario: String = "",          // ID autogenerado por Firestore
    val fecha: String = "",              // Fecha específica: "dd/MM/yyyy"
    val dia: String = "",                // Día de la semana: "Lunes", "Martes", etc.
    val horaInicio: String = "",         // Ej: "08:00"
    val horaFin: String = "",            // Ej: "14:00"
    val cuposDisponibles: Int = 0,       // Cupos restantes para esta franja
    val cuposTotales: Int = 0,           // Cupos totales configurados
    val estado: String = "Disponible",   // Disponible, Bloqueado (RF05.5)
    val idPersonal: String = "",         // Referencia al médico dueño del horario
    val idPosta: String = ""             // Referencia a la posta donde atiende
)
