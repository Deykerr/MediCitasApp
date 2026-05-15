package com.ayacucho.medicitas.repository

import com.ayacucho.medicitas.model.RegistroAuditoria
import com.ayacucho.medicitas.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Repositorio para gestionar el Historial de Auditoría.
 */
class AuditoriaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun registrarAccion(
        accion: String,
        entidadAfectada: String,
        idEntidadAfectada: String,
        detalles: String = ""
    ) {
        val currentUser = auth.currentUser ?: return
        
        // Obtener rol del usuario actual desde local o base de datos (por simplicidad, lo dejaremos como "Usuario" si no lo tenemos a mano)
        // Lo ideal sería obtenerlo de SharedPrefs o consultando a BD, pero para auditoría rápida:
        
        val idRegistro = UUID.randomUUID().toString()
        val registro = RegistroAuditoria(
            idRegistro = idRegistro,
            fechaHora = DateUtils.fechaHoraActual(),
            idUsuario = currentUser.uid,
            rolUsuario = "Desconocido", // Se podría mejorar pasando el rol
            accion = accion,
            entidadAfectada = entidadAfectada,
            idEntidadAfectada = idEntidadAfectada,
            detalles = detalles
        )

        db.collection("auditoria").document(idRegistro).set(registro)
    }
}
