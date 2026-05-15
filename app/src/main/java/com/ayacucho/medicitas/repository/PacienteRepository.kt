package com.ayacucho.medicitas.repository

import com.ayacucho.medicitas.model.*
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Repositorio del Paciente.
 * Encapsula la comunicación con Firestore para la exploración,
 * reserva, historial y cancelación de citas médicas.
 * Adaptado para clínica privada (sin postas).
 *
 * RF02: Exploración de especialidades.
 * RF03: Búsqueda de médicos y horarios.
 * RF04: Reserva, cancelación e historial de citas.
 */
class PacienteRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ==================== DATOS DEL PACIENTE ====================

    fun obtenerDatosPaciente(
        uid: String,
        onSuccess: (Paciente) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PACIENTES).document(uid).get()
            .addOnSuccessListener { doc ->
                val paciente = doc.toObject(Paciente::class.java)
                if (paciente != null) onSuccess(paciente)
                else onFailure("No se encontraron datos del paciente")
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener datos del paciente")
            }
    }

    // ==================== EXPLORACIÓN EN CASCADA (RF02, RF03) ====================

    /**
     * RF02.2: Obtener especialidades activas.
     */
    fun obtenerEspecialidades(
        onSuccess: (List<Especialidad>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_ESPECIALIDADES)
            .whereEqualTo("estado", Constants.ESTADO_ACTIVO)
            .get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Especialidad::class.java))
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener especialidades")
            }
    }

    /**
     * RF03.4: Buscar médicos activos por especialidad (sin filtro de posta).
     */
    fun obtenerMedicosFiltrados(
        idEspecialidad: String,
        onSuccess: (List<PersonalSalud>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PERSONAL_SALUD)
            .whereEqualTo("idEspecialidad", idEspecialidad)
            .whereEqualTo("estado", Constants.ESTADO_ACTIVO)
            .get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(PersonalSalud::class.java))
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener médicos")
            }
    }

    /**
     * RF03.5: Obtener horarios disponibles de un médico (con cupos > 0 y no bloqueados).
     */
    fun obtenerHorariosDisponibles(
        idMedico: String,
        onSuccess: (List<Horario>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_HORARIOS)
            .whereEqualTo("idPersonal", idMedico)
            .whereEqualTo("estado", Constants.ESTADO_HORARIO_DISPONIBLE)
            .get()
            .addOnSuccessListener { result ->
                val horarios = result.toObjects(Horario::class.java)
                // Solo mostrar horarios con cupos disponibles y fecha >= hoy
                val fechaHoy = DateUtils.fechaActual()
                val filtrados = horarios.filter {
                    it.cuposDisponibles > 0 && compararFechas(it.fecha, fechaHoy) >= 0
                }.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                onSuccess(filtrados)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener horarios")
            }
    }

    // ==================== RESERVA DE CITA CON PAGO (RF04) ====================

    /**
     * RF04.1: Confirmar reserva con simulación de pago.
     * RF04.2: Valida disponibilidad (cupos > 0).
     * RF04.3: Evita citas duplicadas (mismo paciente, mismo médico, misma fecha).
     *
     * Proceso:
     * 1. Verificar que no haya cita duplicada
     * 2. Verificar cupos del horario
     * 3. Crear documento de la cita con datos de pago
     * 4. Decrementar cuposDisponibles del horario
     */
    fun confirmarReserva(
        horarioSeleccionado: Horario,
        horaSeleccionada: String,
        motivo: String,
        paciente: Paciente,
        medico: PersonalSalud,
        especialidad: Especialidad,
        montoPago: Double,
        metodoPago: String,
        referenciaPago: String,
        onSuccess: (String) -> Unit,      // Devuelve el ID de la cita creada
        onFailure: (String) -> Unit
    ) {
        val uidPaciente = paciente.idPaciente

        // RF04.3: Verificar duplicados (mismo paciente + mismo médico + misma fecha)
        db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("idPaciente", uidPaciente)
            .whereEqualTo("idPersonal", medico.idPersonal)
            .whereEqualTo("fecha", horarioSeleccionado.fecha)
            .whereEqualTo("estadoCita", Constants.ESTADO_CITA_RESERVADA)
            .get()
            .addOnSuccessListener { existentes ->
                if (!existentes.isEmpty) {
                    onFailure("Ya tienes una cita reservada con este médico para esta fecha")
                    return@addOnSuccessListener
                }

                // RF04.2: Verificar cupos en tiempo real
                db.collection(Constants.COLLECTION_HORARIOS)
                    .document(horarioSeleccionado.idHorario).get()
                    .addOnSuccessListener { docHorario ->
                        val cuposActuales = docHorario.getLong("cuposDisponibles")?.toInt() ?: 0
                        if (cuposActuales <= 0) {
                            onFailure("Lo sentimos, ya no hay cupos disponibles para este horario")
                            return@addOnSuccessListener
                        }

                        // Crear la cita con campos desnormalizados y datos de pago (RF04.4)
                        val idCita = UUID.randomUUID().toString()
                        val nuevaCita = CitaMedica(
                            idCita = idCita,
                            fecha = horarioSeleccionado.fecha,
                            hora = horaSeleccionada,
                            estadoCita = Constants.ESTADO_CITA_RESERVADA,
                            motivoConsulta = motivo.trim(),
                            idPaciente = uidPaciente,
                            nombrePaciente = "${paciente.nombres} ${paciente.apellidos}",
                            dniPaciente = paciente.dni,
                            idPersonal = medico.idPersonal,
                            nombreMedico = "${medico.nombres} ${medico.apellidos}",
                            idEspecialidad = especialidad.idEspecialidad,
                            nombreEspecialidad = especialidad.nombreEspecialidad,
                            fechaCreacion = DateUtils.fechaHoraActual(),
                            montoPago = montoPago,
                            estadoPago = Constants.ESTADO_PAGO_PAGADO,
                            metodoPago = metodoPago,
                            referenciaPago = referenciaPago
                        )

                        // Guardar cita + decrementar cupos en batch + guardar notificacion
                        val batch = db.batch()
                        val citaRef = db.collection(Constants.COLLECTION_CITAS).document(idCita)
                        val horarioRef = db.collection(Constants.COLLECTION_HORARIOS)
                            .document(horarioSeleccionado.idHorario)

                        val idNotif = UUID.randomUUID().toString()
                        val notifRef = db.collection(Constants.COLLECTION_NOTIFICACIONES).document(idNotif)
                        val tituloNotif = "¡Cita Confirmada!"
                        val mensajeNotif = "Tu cita para el ${horarioSeleccionado.fecha} a las $horaSeleccionada ha sido registrada. Pago: S/. ${"%.2f".format(montoPago)} ($metodoPago)"
                        val registroNotificacion = Notificacion(
                            idNotificacion = idNotif,
                            titulo = tituloNotif,
                            mensaje = mensajeNotif,
                            tipo = "CONFIRMACION",
                            fechaHora = DateUtils.fechaHoraActual(),
                            estadoLectura = "No Leído",
                            idUsuario = uidPaciente,
                            idCita = idCita
                        )

                        batch.set(citaRef, nuevaCita)
                        batch.set(notifRef, registroNotificacion)
                        batch.update(horarioRef, "cuposDisponibles", FieldValue.increment(-1))
                        batch.commit()
                            .addOnSuccessListener { onSuccess(idCita) }
                            .addOnFailureListener { e ->
                                onFailure(e.localizedMessage ?: "Error al confirmar la reserva")
                            }
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al verificar disponibilidad")
            }
    }

    // ==================== HISTORIAL DE CITAS (RF04.5) ====================

    /**
     * Obtiene todas las citas del paciente logueado.
     */
    fun obtenerMisCitas(
        idPaciente: String,
        onSuccess: (List<CitaMedica>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("idPaciente", idPaciente)
            .get()
            .addOnSuccessListener { result ->
                val citas = result.toObjects(CitaMedica::class.java)
                val ordenadas = citas.sortedByDescending { it.fecha + it.hora }
                onSuccess(ordenadas)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener historial de citas")
            }
    }

    /**
     * RF07.3: Escuchar cambios en las citas en tiempo real (Ej. cancelación administrativa)
     */
    fun escucharCambiosCitasRealtime(
        idPaciente: String,
        onCambioDetectado: (CitaMedica) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("idPaciente", idPaciente)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                for (dc in snapshot.documentChanges) {
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                        val cita = dc.document.toObject(CitaMedica::class.java)
                        onCambioDetectado(cita)
                    }
                }
            }
    }

    // ==================== CANCELACIÓN CON REEMBOLSO (RF04.6) ====================

    /**
     * Cancela una cita reservada, restaura el cupo en el horario y marca pago como reembolsado.
     */
    fun cancelarCita(
        cita: CitaMedica,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Buscar el horario correspondiente para restaurar el cupo
        db.collection(Constants.COLLECTION_HORARIOS)
            .whereEqualTo("idPersonal", cita.idPersonal)
            .whereEqualTo("fecha", cita.fecha)
            .get()
            .addOnSuccessListener { horarios ->
                val batch = db.batch()

                // Actualizar estado de la cita y marcar pago como reembolsado
                val citaRef = db.collection(Constants.COLLECTION_CITAS).document(cita.idCita)
                batch.update(citaRef, mapOf(
                    "estadoCita" to Constants.ESTADO_CITA_CANCELADA,
                    "estadoPago" to Constants.ESTADO_PAGO_REEMBOLSADO
                ))

                // Restaurar cupo si encontramos el horario
                if (!horarios.isEmpty) {
                    val horarioRef = horarios.documents.first().reference
                    batch.update(horarioRef, "cuposDisponibles", FieldValue.increment(1))
                }

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.localizedMessage ?: "Error al cancelar la cita")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al buscar horario para restaurar cupo")
            }
    }

    // ==================== UTILIDADES ====================

    /**
     * Compara dos fechas en formato dd/MM/yyyy.
     * Retorna: negativo si fecha1 < fecha2, 0 si iguales, positivo si fecha1 > fecha2.
     */
    private fun compararFechas(fecha1: String, fecha2: String): Int {
        return try {
            val sdf = java.text.SimpleDateFormat(Constants.FORMATO_FECHA, java.util.Locale("es", "PE"))
            val d1 = sdf.parse(fecha1)
            val d2 = sdf.parse(fecha2)
            d1!!.compareTo(d2!!)
        } catch (e: Exception) {
            0
        }
    }

    // ==================== TRATAMIENTOS Y SESIONES ====================

    /**
     * Obtiene los planes de tratamiento activos del paciente.
     */
    fun obtenerMisTratamientos(
        idPaciente: String,
        onSuccess: (List<PlanTratamiento>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_TRATAMIENTOS)
            .whereEqualTo("idPaciente", idPaciente)
            .get()
            .addOnSuccessListener { result ->
                val planes = result.toObjects(PlanTratamiento::class.java)
                val ordenados = planes.sortedByDescending { it.fechaCreacion }
                onSuccess(ordenados)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener tratamientos")
            }
    }

    /**
     * Obtiene las sesiones de un plan de tratamiento específico.
     */
    fun obtenerSesionesDeTratamiento(
        idPlan: String,
        onSuccess: (List<SesionTratamiento>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_SESIONES)
            .whereEqualTo("idPlan", idPlan)
            .get()
            .addOnSuccessListener { result ->
                val sesiones = result.toObjects(SesionTratamiento::class.java)
                val ordenadas = sesiones.sortedBy { it.numeroSesion }
                onSuccess(ordenadas)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener sesiones")
            }
    }

    /**
     * Agenda la próxima sesión pendiente de un tratamiento.
     * Similar a confirmarReserva pero vinculada al plan.
     */
    fun agendarSesion(
        sesion: SesionTratamiento,
        horario: Horario,
        horaSeleccionada: String,
        montoPago: Double,
        metodoPago: String,
        referenciaPago: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Verificar cupos en tiempo real
        db.collection(Constants.COLLECTION_HORARIOS)
            .document(horario.idHorario).get()
            .addOnSuccessListener { docHorario ->
                val cuposActuales = docHorario.getLong("cuposDisponibles")?.toInt() ?: 0
                if (cuposActuales <= 0) {
                    onFailure("No hay cupos disponibles para este horario")
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                // Actualizar sesión con datos de agenda y pago
                val sesionRef = db.collection(Constants.COLLECTION_SESIONES).document(sesion.idSesion)
                batch.update(sesionRef, mapOf(
                    "fecha" to horario.fecha,
                    "hora" to horaSeleccionada,
                    "estado" to Constants.ESTADO_SESION_AGENDADA,
                    "precioPagado" to montoPago,
                    "estadoPago" to Constants.ESTADO_PAGO_PAGADO,
                    "metodoPago" to metodoPago,
                    "referenciaPago" to referenciaPago,
                    "idHorario" to horario.idHorario
                ))

                // Decrementar cupo del horario
                val horarioRef = db.collection(Constants.COLLECTION_HORARIOS).document(horario.idHorario)
                batch.update(horarioRef, "cuposDisponibles", com.google.firebase.firestore.FieldValue.increment(-1))

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.localizedMessage ?: "Error al agendar sesión")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al verificar disponibilidad")
            }
    }
}

