package com.ayacucho.medicitas.utils

import android.util.Log
import com.ayacucho.medicitas.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object DatabaseSeeder {

    fun poblarBaseDeDatos(onResult: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // LIMPIEZA TOTAL: Borrar médicos y horarios residuales antes de sembrar
        db.collection(Constants.COLLECTION_PERSONAL_SALUD).get()
            .addOnSuccessListener { medicosSnaps ->
                val deleteBatch = db.batch()
                medicosSnaps.documents.forEach { deleteBatch.delete(it.reference) }

                db.collection(Constants.COLLECTION_HORARIOS).get()
                    .addOnSuccessListener { horariosSnaps ->
                        horariosSnaps.documents.forEach { deleteBatch.delete(it.reference) }

                        deleteBatch.commit().addOnSuccessListener {
                            // Ahora sí sembramos desde cero sin fantasmas
                            iniciarSembrado(db, auth, onResult)
                        }
                    }
            }
            .addOnFailureListener {
                // Failsafe: sembrar directo si falla la purga
                iniciarSembrado(db, auth, onResult)
            }
    }

    private fun iniciarSembrado(db: FirebaseFirestore, auth: FirebaseAuth, onResult: (String) -> Unit) {
        val batch = db.batch()

        // ── Especialidades (con precios de consulta) ──────────────────────────
        val especialidades = listOf(
            Especialidad("ESP01", "Medicina General",   "Atención médica integral primaria",       "Activo", 50.0),
            Especialidad("ESP02", "Pediatría",          "Atención médica a niños y adolescentes",  "Activo", 60.0),
            Especialidad("ESP03", "Ginecología",        "Salud femenina y control prenatal",       "Activo", 80.0),
            Especialidad("ESP04", "Cardiología",        "Diagnóstico y tratamiento cardiovascular","Activo", 100.0),
            Especialidad("ESP05", "Dermatología",       "Enfermedades y estética de la piel",      "Activo", 70.0),
            Especialidad("ESP06", "Traumatología",      "Lesiones óseas, musculares y articulares","Activo", 90.0)
        )
        especialidades.forEach { batch.set(db.collection(Constants.COLLECTION_ESPECIALIDADES).document(it.idEspecialidad), it) }

        // ── Médicos: 2 por especialidad = 12 médicos ─────────────────────────
        val medicosMock = listOf(
            // Medicina General
            PersonalSalud("MED01", "Juan Carlos",  "Quispe Huamán",     "40215678", "jcquispe@clinica.pe",    "ESP01", "Medicina General",  "Activo"),
            PersonalSalud("MED02", "María Elena",  "Huamán Sulca",      "45321098", "mehuaman@clinica.pe",    "ESP01", "Medicina General",  "Activo"),
            // Pediatría
            PersonalSalud("MED03", "Carmen Rosa",  "Palomino Ayala",    "41236547", "crpalomino@clinica.pe",  "ESP02", "Pediatría",         "Activo"),
            PersonalSalud("MED04", "Patricia",     "Quispe Ccoicca",    "48901234", "pquispe@clinica.pe",     "ESP02", "Pediatría",         "Activo"),
            // Ginecología
            PersonalSalud("MED05", "Lucía María",  "Flores Vargas",     "43890124", "lmflores@clinica.pe",    "ESP03", "Ginecología",       "Activo"),
            PersonalSalud("MED06", "Ana Sofía",    "Ccente Mendoza",    "46789012", "asccente@clinica.pe",    "ESP03", "Ginecología",       "Activo"),
            // Cardiología
            PersonalSalud("MED07", "Luis Alberto", "Cárdenas Rojas",    "43578214", "lacardenas@clinica.pe",  "ESP04", "Cardiología",       "Activo"),
            PersonalSalud("MED08", "Roberto",      "Vargas Lira",       "40981234", "rvargas@clinica.pe",     "ESP04", "Cardiología",       "Activo"),
            // Dermatología
            PersonalSalud("MED09", "Sandra Luz",   "Huanca Mamani",     "49012345", "slhuanca@clinica.pe",    "ESP05", "Dermatología",      "Activo"),
            PersonalSalud("MED10", "Rosa Elena",   "Huayta Condori",    "44123785", "rehuayta@clinica.pe",    "ESP05", "Dermatología",      "Activo"),
            // Traumatología
            PersonalSalud("MED11", "Jorge Luis",   "Ramos Chipana",     "47654321", "jlramos@clinica.pe",     "ESP06", "Traumatología",     "Activo"),
            PersonalSalud("MED12", "Carlos Iván",  "Saravia Gutierrez", "40112233", "cisaravia@clinica.pe",   "ESP06", "Traumatología",     "Activo")
        )

        val idMap = mutableMapOf<String, String>()

        // Registrar los 12 médicos en Firebase Auth de forma recursiva
        crearUsuariosRecursivo(0, medicosMock, auth, "medico123456", idMap) {

            // Guardar documentos en Firestore con UIDs reales de Auth
            medicosMock.forEach { med ->
                val realUid = idMap[med.idPersonal] ?: return@forEach
                val medicoValido = med.copy(idPersonal = realUid)
                batch.set(
                    db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(realUid),
                    medicoValido
                )
            }

            // ── Horarios: 1 por médico, turnos variados mañana/tarde ──────────
            val d1 = DateUtils.sumarDiasAFechaActual(1)  // mañana
            val d2 = DateUtils.sumarDiasAFechaActual(2)
            val d3 = DateUtils.sumarDiasAFechaActual(3)
            val d4 = DateUtils.sumarDiasAFechaActual(4)
            val d5 = DateUtils.sumarDiasAFechaActual(5)

            val horarios = listOf(
                // 08:00-12:00 = 240min / 30min = 8 slots
                Horario("H01", d1, "Lunes",     "08:00", "12:00", 30, 8, 8, "Disponible", "MED01"),
                Horario("H02", d2, "Martes",    "08:00", "12:00", 30, 8, 8, "Disponible", "MED02"),
                Horario("H03", d3, "Miércoles", "09:00", "13:00", 30, 8, 8, "Disponible", "MED03"),
                // 14:00-18:00 = 240min / 30min = 8 slots
                Horario("H04", d4, "Jueves",    "14:00", "18:00", 30, 8, 8, "Disponible", "MED04"),
                Horario("H05", d1, "Lunes",     "14:00", "18:00", 30, 8, 8, "Disponible", "MED05"),
                // 08:00-14:00 = 360min / 30min = 12 slots
                Horario("H06", d2, "Martes",    "08:00", "14:00", 30, 12, 12, "Disponible", "MED06"),
                Horario("H07", d3, "Miércoles", "08:00", "12:00", 30, 8, 8, "Disponible", "MED07"),
                Horario("H08", d5, "Viernes",   "09:00", "13:00", 30, 8, 8, "Disponible", "MED08"),
                Horario("H09", d1, "Lunes",     "08:00", "12:00", 30, 8, 8, "Disponible", "MED09"),
                Horario("H10", d3, "Miércoles", "14:00", "18:00", 30, 8, 8, "Disponible", "MED10"),
                // 20-minute slots for shorter consultations
                Horario("H11", d4, "Jueves",    "08:00", "12:00", 20, 12, 12, "Disponible", "MED11"),
                Horario("H12", d5, "Viernes",   "14:00", "18:00", 20, 12, 12, "Disponible", "MED12")
            )

            horarios.forEach { hor ->
                val realUidMedico = idMap[hor.idPersonal]
                if (realUidMedico != null) {
                    val horarioValido = hor.copy(idPersonal = realUidMedico)
                    batch.set(
                        db.collection(Constants.COLLECTION_HORARIOS).document(horarioValido.idHorario),
                        horarioValido
                    )
                }
            }

            // Confirmar todo el Batch
            batch.commit()
                .addOnSuccessListener {
                    onResult(
                        "✅ ¡Éxito! 12 médicos sembrados.\n" +
                        "📋 Cobertura: 6 especialidades con precios\n" +
                        "📅 12 horarios creados (turnos mañana y tarde)\n" +
                        "🔑 Contraseña de todos: medico123456"
                    )
                }
                .addOnFailureListener { e ->
                    onResult("❌ Error al subir documentos: ${e.message}")
                }
        }
    }

    private fun crearUsuariosRecursivo(
        index: Int,
        lista: List<PersonalSalud>,
        auth: FirebaseAuth,
        pass: String,
        idMap: MutableMap<String, String>,
        onComplete: () -> Unit
    ) {
        if (index >= lista.size) {
            onComplete()
            return
        }

        val usuario = lista[index]
        auth.createUserWithEmailAndPassword(usuario.correoElectronico, pass)
            .addOnSuccessListener { result ->
                val realUid = result.user?.uid
                if (realUid != null) idMap[usuario.idPersonal] = realUid
                crearUsuariosRecursivo(index + 1, lista, auth, pass, idMap, onComplete)
            }
            .addOnFailureListener {
                // Si el correo ya existía, iniciamos sesión para obtener su UID real
                auth.signInWithEmailAndPassword(usuario.correoElectronico, pass)
                    .addOnSuccessListener { result ->
                        val realUid = result.user?.uid
                        if (realUid != null) idMap[usuario.idPersonal] = realUid
                        crearUsuariosRecursivo(index + 1, lista, auth, pass, idMap, onComplete)
                    }
                    .addOnFailureListener {
                        crearUsuariosRecursivo(index + 1, lista, auth, pass, idMap, onComplete)
                    }
            }
    }
}
