package com.ayacucho.medicitas.repository

import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.model.Horario
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Repositorio del Médico.
 * Encapsula la comunicación con Firestore para la agenda médica.
 *
 * RF05.1: Visualizar agenda diaria y semanal.
 * RF05.2: Mostrar datos básicos del paciente (desnormalizados en CitaMedica).
 * RF05.3: Cambiar estado de la cita (Atendido, No Asistió, Cancelado).
 * RF05.4: Visualizar horarios disponibles.
 * RF05.5: Bloquear horarios por ausencia.
 */
class MedicoRepository {

    private val db = FirebaseFirestore.getInstance()

    // Listener activo para actualizaciones en tiempo real
    private var citasListener: ListenerRegistration? = null

    // ==================== DATOS DEL MÉDICO ====================

    /**
     * Obtiene los datos del médico logueado desde Firestore.
     */
    fun obtenerDatosMedico(
        uid: String,
        onSuccess: (PersonalSalud) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(uid).get()
            .addOnSuccessListener { doc ->
                val medico = doc.toObject(PersonalSalud::class.java)
                if (medico != null) {
                    onSuccess(medico)
                } else {
                    onFailure("No se encontraron datos del médico")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener datos del médico")
            }
    }

    // ==================== AGENDA DEL DÍA - TIEMPO REAL (RF05.1) ====================

    /**
     * Escucha en tiempo real las citas del médico para una fecha específica.
     * Usa SnapshotListener para que la agenda se actualice automáticamente
     * cuando un paciente reserve o cancele una cita.
     */
    fun escucharCitasDelDia(
        idMedico: String,
        fecha: String,
        onUpdate: (List<CitaMedica>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Cancelar listener previo si existe
        citasListener?.remove()

        citasListener = db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("idPersonal", idMedico)
            .whereEqualTo("fecha", fecha)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onFailure(error.localizedMessage ?: "Error al escuchar citas")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val listaCitas = snapshots.toObjects(CitaMedica::class.java)
                    // Ordenar por hora manualmente (SnapshotListener no soporta orderBy directo en combinación)
                    val citasOrdenadas = listaCitas.sortedBy { it.hora }
                    onUpdate(citasOrdenadas)
                }
            }
    }

    /**
     * Consulta única (sin tiempo real) para la agenda semanal.
     */
    fun obtenerCitasSemana(
        idMedico: String,
        fechas: List<String>,
        onSuccess: (List<CitaMedica>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (fechas.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("idPersonal", idMedico)
            .whereIn("fecha", fechas)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(CitaMedica::class.java)
                val ordenadas = lista.sortedWith(compareBy({ it.fecha }, { it.hora }))
                onSuccess(ordenadas)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener citas de la semana")
            }
    }

    // ==================== GESTIÓN DE ESTADOS (RF05.3, RF08.1) ====================

    /**
     * Actualiza el estado de una cita específica.
     * Solo modifica el campo estadoCita sin tocar el resto del documento.
     */
    fun actualizarEstadoCita(
        idCita: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_CITAS).document(idCita)
            .update("estadoCita", nuevoEstado)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al actualizar estado de la cita")
            }
    }

    // ==================== HORARIOS DEL MÉDICO (RF05.4) ====================

    /**
     * Obtiene los horarios asignados al médico.
     */
    fun obtenerMisHorarios(
        idMedico: String,
        onSuccess: (List<Horario>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_HORARIOS)
            .whereEqualTo("idPersonal", idMedico)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(Horario::class.java)
                val ordenados = lista.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                onSuccess(ordenados)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al obtener horarios")
            }
    }

    // ==================== BLOQUEAR HORARIOS (RF05.5) ====================

    /**
     * Cambia el estado de un horario a "Bloqueado" por ausencia o imprevistos.
     */
    fun bloquearHorario(
        idHorario: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_HORARIOS).document(idHorario)
            .update("estado", Constants.ESTADO_HORARIO_BLOQUEADO)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al bloquear horario")
            }
    }

    /**
     * Desbloquea un horario previamente bloqueado.
     */
    fun desbloquearHorario(
        idHorario: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_HORARIOS).document(idHorario)
            .update("estado", Constants.ESTADO_HORARIO_DISPONIBLE)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al desbloquear horario")
            }
    }

    // ==================== LIMPIEZA ====================

    /**
     * Elimina el listener de tiempo real. Llamar al destruir la Activity.
     */
    fun detenerEscuchaCitas() {
        citasListener?.remove()
        citasListener = null
    }

    // ==================== PLANES DE TRATAMIENTO ====================

    /**
     * Crea un plan de tratamiento y genera N sesiones en estado Pendiente.
     * Se llama cuando el médico marca una cita como Atendida y decide prescribir sesiones.
     */
    fun crearPlanTratamiento(
        cita: com.ayacucho.medicitas.model.CitaMedica,
        diagnostico: String,
        descripcionTratamiento: String,
        totalSesiones: Int,
        precioPorSesion: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val idPlan = java.util.UUID.randomUUID().toString()
        val fechaCreacion = com.ayacucho.medicitas.utils.DateUtils.fechaHoraActual()

        val plan = com.ayacucho.medicitas.model.PlanTratamiento(
            idPlan = idPlan,
            idPaciente = cita.idPaciente,
            nombrePaciente = cita.nombrePaciente,
            dniPaciente = cita.dniPaciente,
            idMedico = cita.idPersonal,
            nombreMedico = cita.nombreMedico,
            idEspecialidad = cita.idEspecialidad,
            nombreEspecialidad = cita.nombreEspecialidad,
            diagnostico = diagnostico,
            descripcionTratamiento = descripcionTratamiento,
            totalSesiones = totalSesiones,
            sesionesCompletadas = 0,
            precioPorSesion = precioPorSesion,
            estado = com.ayacucho.medicitas.utils.Constants.ESTADO_TRATAMIENTO_ACTIVO,
            fechaCreacion = fechaCreacion,
            idCitaOrigen = cita.idCita
        )

        val batch = db.batch()

        // Guardar el plan
        val planRef = db.collection(com.ayacucho.medicitas.utils.Constants.COLLECTION_TRATAMIENTOS).document(idPlan)
        batch.set(planRef, plan)

        // Crear las N sesiones en estado Pendiente
        for (i in 1..totalSesiones) {
            val idSesion = java.util.UUID.randomUUID().toString()
            val sesion = com.ayacucho.medicitas.model.SesionTratamiento(
                idSesion = idSesion,
                idPlan = idPlan,
                numeroSesion = i,
                estado = com.ayacucho.medicitas.utils.Constants.ESTADO_SESION_PENDIENTE,
                idMedico = cita.idPersonal,
                nombreMedico = cita.nombreMedico,
                idPaciente = cita.idPaciente,
                nombrePaciente = cita.nombrePaciente,
                nombreEspecialidad = cita.nombreEspecialidad
            )
            val sesionRef = db.collection(com.ayacucho.medicitas.utils.Constants.COLLECTION_SESIONES).document(idSesion)
            batch.set(sesionRef, sesion)
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al crear plan de tratamiento")
            }
    }

    /**
     * Marca una sesión como Atendida y actualiza el contador del plan.
     */
    fun marcarSesionAtendida(
        idSesion: String,
        idPlan: String,
        notas: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val batch = db.batch()

        val sesionRef = db.collection(com.ayacucho.medicitas.utils.Constants.COLLECTION_SESIONES).document(idSesion)
        batch.update(sesionRef, mapOf(
            "estado" to com.ayacucho.medicitas.utils.Constants.ESTADO_SESION_ATENDIDA,
            "notas" to notas
        ))

        val planRef = db.collection(com.ayacucho.medicitas.utils.Constants.COLLECTION_TRATAMIENTOS).document(idPlan)
        batch.update(planRef, "sesionesCompletadas", com.google.firebase.firestore.FieldValue.increment(1))

        batch.commit()
            .addOnSuccessListener {
                // Verificar si se completaron todas las sesiones
                db.collection(com.ayacucho.medicitas.utils.Constants.COLLECTION_TRATAMIENTOS).document(idPlan).get()
                    .addOnSuccessListener { doc ->
                        val total = doc.getLong("totalSesiones")?.toInt() ?: 0
                        val completadas = doc.getLong("sesionesCompletadas")?.toInt() ?: 0
                        if (completadas >= total) {
                            doc.reference.update("estado", com.ayacucho.medicitas.utils.Constants.ESTADO_TRATAMIENTO_COMPLETADO)
                        }
                    }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al actualizar sesión")
            }
    }
}

