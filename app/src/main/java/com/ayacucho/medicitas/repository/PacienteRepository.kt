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

    fun actualizarPerfilPaciente(
        uid: String,
        datos: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PACIENTES).document(uid)
            .update(datos)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al actualizar el perfil")
            }
    }

    // ==================== DEPENDIENTES ====================

    fun obtenerDependientes(
        idTitular: String,
        onSuccess: (List<Dependiente>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("dependientes")
            .whereEqualTo("idTitular", idTitular)
            .get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Dependiente::class.java))
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener familiares asociados")
            }
    }

    fun agregarDependiente(
        dependiente: Dependiente,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("dependientes").document(dependiente.idDependiente)
            .set(dependiente)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al registrar familiar")
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
     * RF03.5: Obtener slots reales de horario disponibles de un médico.
     */
    fun obtenerSlotsDisponibles(
        idMedico: String,
        onSuccess: (List<SlotHorario>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("slots_horario")
            .whereEqualTo("idMedico", idMedico)
            .whereEqualTo("estado", "Disponible")
            .get()
            .addOnSuccessListener { result ->
                val slots = result.toObjects(SlotHorario::class.java)
                val fechaHoy = DateUtils.fechaActual()
                val filtrados = slots.filter { compararFechas(it.fecha, fechaHoy) >= 0 }
                onSuccess(filtrados.sortedWith(compareBy({ it.fecha }, { it.horaInicio })))
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener horarios")
            }
    }

    // Mantenemos obtenerHorariosDisponibles temporalmente por retrocompatibilidad
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
                val fechaHoy = DateUtils.fechaActual()
                val filtrados = horarios.filter {
                    it.cuposDisponibles > 0 && compararFechas(it.fecha, fechaHoy) >= 0
                }.sortedBy { it.fecha }
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
    fun confirmarReservaSlot(
        slotSeleccionado: SlotHorario,
        motivo: String,
        paciente: Paciente,
        medico: PersonalSalud,
        especialidad: Especialidad,
        montoPago: Double,
        metodoPago: String,
        referenciaPago: String,
        pacienteRealNombre: String,
        pacienteRealDni: String,
        onSuccess: (String) -> Unit,      // Devuelve el ID de la cita creada
        onFailure: (String) -> Unit
    ) {
        val uidPaciente = paciente.idPaciente

        // Verificar duplicados
        db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("idPaciente", uidPaciente)
            .whereEqualTo("idPersonal", medico.idPersonal)
            .whereEqualTo("fecha", slotSeleccionado.fecha)
            .whereEqualTo("estadoCita", Constants.ESTADO_CITA_CONFIRMADA)
            .get()
            .addOnSuccessListener { existentes ->
                if (!existentes.isEmpty) {
                    onFailure("Ya tienes una cita reservada con este médico para esta fecha")
                    return@addOnSuccessListener
                }

                // Transacción para verificar y reservar el Slot
                db.runTransaction { transaction ->
                    val slotRef = db.collection("slots_horario").document(slotSeleccionado.idSlot)
                    val snapshot = transaction.get(slotRef)
                    
                    val estado = snapshot.getString("estado")
                    if (estado != "Disponible") {
                        throw Exception("Lo sentimos, este horario ya no está disponible")
                    }

                    val idCita = UUID.randomUUID().toString()
                    val nuevaCita = CitaMedica(
                        idCita = idCita,
                        fecha = slotSeleccionado.fecha,
                        hora = slotSeleccionado.horaInicio,
                        estadoCita = Constants.ESTADO_CITA_CONFIRMADA,
                        estadoAsistencia = Constants.ESTADO_ASISTENCIA_PROGRAMADO,
                        motivoConsulta = motivo.trim(),
                        idPaciente = uidPaciente,
                        nombrePaciente = pacienteRealNombre,
                        dniPaciente = pacienteRealDni,
                        idPersonal = medico.idPersonal,
                        nombreMedico = "${medico.nombres} ${medico.apellidos}",
                        idEspecialidad = especialidad.idEspecialidad,
                        nombreEspecialidad = especialidad.nombreEspecialidad,
                        fechaCreacion = DateUtils.fechaHoraActual(),
                        montoPago = montoPago,
                        estadoPago = Constants.ESTADO_PAGO_PAGADO,
                        metodoPago = metodoPago,
                        referenciaPago = referenciaPago,
                        idSlot = slotSeleccionado.idSlot // IMPORTANTE: vincular la cita al slot
                    )

                    val citaRef = db.collection(Constants.COLLECTION_CITAS).document(idCita)
                    val idNotif = UUID.randomUUID().toString()
                    val notifRef = db.collection(Constants.COLLECTION_NOTIFICACIONES).document(idNotif)
                    val registroNotificacion = Notificacion(
                        idNotificacion = idNotif,
                        titulo = "¡Cita Confirmada!",
                        mensaje = "La cita para $pacienteRealNombre el ${slotSeleccionado.fecha} a las ${slotSeleccionado.horaInicio} ha sido registrada. Pago: S/. ${"%.2f".format(montoPago)} ($metodoPago)",
                        tipo = "CONFIRMACION",
                        fechaHora = DateUtils.fechaHoraActual(),
                        estadoLectura = "No Leído",
                        idUsuario = uidPaciente,
                        idCita = idCita
                    )

                    transaction.update(slotRef, "estado", "Reservado")
                    transaction.update(slotRef, "idCita", idCita)
                    transaction.set(citaRef, nuevaCita)
                    transaction.set(notifRef, registroNotificacion)

                    // También decrementar cupos en el Horario maestro (opcional, pero util para reportes rápidos)
                    val horarioRef = db.collection(Constants.COLLECTION_HORARIOS).document(slotSeleccionado.idHorario)
                    transaction.update(horarioRef, "cuposDisponibles", com.google.firebase.firestore.FieldValue.increment(-1))

                    idCita // Devolver idCita de la transacción
                }.addOnSuccessListener { idCita ->
                    AuditoriaRepository().registrarAccion(
                        accion = "CREAR_CITA",
                        entidadAfectada = "CitaMedica",
                        idEntidadAfectada = idCita,
                        detalles = "Cita reservada por paciente $pacienteRealNombre con médico ${medico.nombres} ${medico.apellidos} para el ${slotSeleccionado.fecha}"
                    )
                    onSuccess(idCita)
                }.addOnFailureListener { e ->
                    onFailure(e.localizedMessage ?: "Error al confirmar la reserva")
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
        motivo: String,
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

                // Actualizar estado de la cita y marcar pago como reembolsado, y añadir motivo
                val citaRef = db.collection(Constants.COLLECTION_CITAS).document(cita.idCita)
                batch.update(citaRef, mapOf(
                    "estadoCita" to Constants.ESTADO_CITA_CANCELADA,
                    "estadoPago" to Constants.ESTADO_PAGO_REEMBOLSADO,
                    "motivoCancelacion" to motivo
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
        slot: SlotHorario,
        montoPago: Double,
        metodoPago: String,
        referenciaPago: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.runTransaction { transaction ->
            val slotRef = db.collection("slots_horario").document(slot.idSlot)
            val snapshot = transaction.get(slotRef)
            
            val estado = snapshot.getString("estado")
            if (estado != "Disponible") {
                throw Exception("Lo sentimos, este horario ya no está disponible")
            }

            val sesionRef = db.collection(Constants.COLLECTION_SESIONES).document(sesion.idSesion)
            transaction.update(sesionRef, mapOf(
                "fecha" to slot.fecha,
                "hora" to slot.horaInicio,
                "estado" to Constants.ESTADO_SESION_AGENDADA,
                "precioPagado" to montoPago,
                "estadoPago" to Constants.ESTADO_PAGO_PAGADO,
                "metodoPago" to metodoPago,
                "referenciaPago" to referenciaPago,
                "idHorario" to slot.idHorario
            ))

            transaction.update(slotRef, "estado", "Reservado")
            transaction.update(slotRef, "idCita", sesion.idSesion)

            val horarioRef = db.collection(Constants.COLLECTION_HORARIOS).document(slot.idHorario)
            transaction.update(horarioRef, "cuposDisponibles", com.google.firebase.firestore.FieldValue.increment(-1))

            null // return value inside transaction
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e.localizedMessage ?: "Error al agendar sesión")
        }
    }

    /**
     * Reprograma una cita: libera el slot anterior y reserva uno nuevo en transaccion atomica.
     */
    fun reprogramarCita(
        cita: CitaMedica,
        nuevoSlot: SlotHorario,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.runTransaction { transaction ->
            val nuevoSlotRef = db.collection("slots_horario").document(nuevoSlot.idSlot)
            val nuevoSlotSnap = transaction.get(nuevoSlotRef)
            if (nuevoSlotSnap.getString("estado") != "Disponible") {
                throw Exception("Este horario ya no esta disponible, elige otro")
            }

            val citaRef = db.collection(Constants.COLLECTION_CITAS).document(cita.idCita)

            if (cita.idSlot.isNotBlank() && cita.idSlot != nuevoSlot.idSlot) {
                val slotAnteriorRef = db.collection("slots_horario").document(cita.idSlot)
                val slotAnteriorSnap = transaction.get(slotAnteriorRef)
                if (slotAnteriorSnap.exists()) {
                    transaction.update(slotAnteriorRef, mapOf("estado" to "Disponible", "idCita" to ""))
                    val idHorarioAnterior = slotAnteriorSnap.getString("idHorario") ?: ""
                    if (idHorarioAnterior.isNotBlank()) {
                        val horarioAnteriorRef = db.collection(Constants.COLLECTION_HORARIOS).document(idHorarioAnterior)
                        transaction.update(horarioAnteriorRef, "cuposDisponibles", com.google.firebase.firestore.FieldValue.increment(1))
                    }
                }
            }

            transaction.update(nuevoSlotRef, mapOf("estado" to "Reservado", "idCita" to cita.idCita))

            val horarioNuevoRef = db.collection(Constants.COLLECTION_HORARIOS).document(nuevoSlot.idHorario)
            transaction.update(horarioNuevoRef, "cuposDisponibles", com.google.firebase.firestore.FieldValue.increment(-1))

            transaction.update(citaRef, mapOf(
                "fecha" to nuevoSlot.fecha,
                "hora" to nuevoSlot.horaInicio,
                "idSlot" to nuevoSlot.idSlot,
                "estadoCita" to Constants.ESTADO_CITA_CONFIRMADA,
                "fechaReprogramacion" to DateUtils.fechaHoraActual()
            ))

            null
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e.localizedMessage ?: "Error al reprogramar la cita")
        }
    }
}