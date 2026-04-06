package com.ayacucho.medicitas.utils

import android.util.Log
import com.ayacucho.medicitas.model.Administrador
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AdminBootstrap {

    fun crearAdminPorDefecto(onResult: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val correoAdmin = "admin@medicitas.com"
        val passAdmin = "admin123456"

        auth.createUserWithEmailAndPassword(correoAdmin, passAdmin)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                
                val admin = Administrador(
                    idAdministrador = uid,
                    nombres = "Super",
                    apellidos = "Administrador",
                    correoElectronico = correoAdmin,
                    rol = Constants.ROL_ADMIN,
                    estado = "Activo"
                )

                db.collection(Constants.COLLECTION_ADMINISTRADORES).document(uid)
                    .set(admin)
                    .addOnSuccessListener {
                        onResult("ADMIN CREADO EXITOSAMENTE: $correoAdmin / $passAdmin")
                    }
                    .addOnFailureListener { e ->
                        onResult("Error al guardar admin en db: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                // Si el correo ya existe, no es problema
                onResult("Admin bootstrap info: ${e.message}")
            }
    }
}
