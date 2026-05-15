package com.ayacucho.medicitas.repository

import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Repositorio del Recepcionista.
 * Gestiona la asistencia y pagos de todos los pacientes de la clínica.
 */
class RecepcionistaRepository {

    private val db = FirebaseFirestore.getInstance()
    private var citasListener: ListenerRegistration? = null

    /**
     * Obtiene datos del recepcionista logueado.
     */
    fun obtenerDatosRecepcionista(
        uid: String,
        onSuccess: (PersonalSalud) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(uid).get()
            .addOnSuccessListener { doc ->
                val personal = doc.toObject(PersonalSalud::class.java)
                if (personal != null) onSuccess(personal)
                else onFailure("No se encontraron datos")
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error") }
    }

    /**
     * Escucha todas las citas de la clínica para una fecha determinada.
     */
    fun escucharTodasLasCitas(
        fecha: String,
        onUpdate: (List<CitaMedica>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        citasListener?.remove()

        citasListener = db.collection(Constants.COLLECTION_CITAS)
            .whereEqualTo("fecha", fecha)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onFailure(error.localizedMessage ?: "Error")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val lista = snapshots.toObjects(CitaMedica::class.java)
                    onUpdate(lista.sortedBy { it.hora })
                }
            }
    }

    /**
     * Registra el pago de una cita.
     */
    fun registrarPago(
        idCita: String,
        monto: Double,
        metodo: String,
        referencia: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_CITAS).document(idCita)
            .update(mapOf(
                "estadoPago" to Constants.ESTADO_PAGO_PAGADO,
                "montoPago" to monto,
                "metodoPago" to metodo,
                "referenciaPago" to referencia
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al registrar pago") }
    }

    /**
     * Actualiza el estado de la cita (Cancelada, Reprogramada).
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
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al actualizar estado cita") }
    }

    /**
     * Actualiza el estado de asistencia de la cita (Llego, En Espera).
     */
    fun actualizarEstadoAsistencia(
        idCita: String,
        nuevoEstado: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_CITAS).document(idCita)
            .update("estadoAsistencia", nuevoEstado)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al actualizar asistencia") }
    }

    fun detenerEscucha() {
        citasListener?.remove()
    }
}
