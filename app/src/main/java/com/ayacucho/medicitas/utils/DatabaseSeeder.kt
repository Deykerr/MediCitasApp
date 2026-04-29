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

        // ── Postas ─────────────────────────────────────────────────────────────
        val postas = listOf(
            PostaMedica("PS01", "Centro de Salud Los Licenciados", "Urb. Los Licenciados Mz. E Lt. 18", "Ayacucho",   "066-319293", "Activo"),
            PostaMedica("PS02", "Centro de Salud Carmen Alto",     "Jr. Tahuantinsuyo S/N",              "Carmen Alto", "066-310588", "Activo"),
            PostaMedica("PS03", "Puesto de Salud Santa Ana",       "C.P. Santa Ana Mz. C3 Lt. 1",        "Ayacucho",   "923627717",  "Activo")
        )
        postas.forEach { batch.set(db.collection(Constants.COLLECTION_POSTAS).document(it.idPosta), it) }

        // ── Especialidades ─────────────────────────────────────────────────────
        val especialidades = listOf(
            Especialidad("ESP01", "Medicina General", "Atención médica integral primaria",       "Activo"),
            Especialidad("ESP02", "Pediatría",         "Atención médica a niños y adolescentes", "Activo"),
            Especialidad("ESP03", "Obstetricia",       "Control prenatal y salud materna",       "Activo"),
            Especialidad("ESP04", "Enfermería",        "Atención asistencial y procedimientos",  "Activo")
        )
        especialidades.forEach { batch.set(db.collection(Constants.COLLECTION_ESPECIALIDADES).document(it.idEspecialidad), it) }

        // ── Médicos: 1 por especialidad × 3 postas = 12 médicos ───────────────
        val medicosMock = listOf(
            // ── PS01: Centro de Salud Los Licenciados ──────────────────────────
            PersonalSalud("MED01", "Juan Carlos", "Quispe Huamán",     "40215678", "jcquispe@minsa.gob.pe",    "ESP01", "Medicina General", "Activo", "PS01", "Centro de Salud Los Licenciados"),
            PersonalSalud("MED02", "Carmen Rosa", "Palomino Ayala",    "41236547", "crpalomino@minsa.gob.pe",  "ESP02", "Pediatría",         "Activo", "PS01", "Centro de Salud Los Licenciados"),
            PersonalSalud("MED03", "Lucía María", "Flores Vargas",     "43890124", "lmflores@minsa.gob.pe",    "ESP03", "Obstetricia",       "Activo", "PS01", "Centro de Salud Los Licenciados"),
            PersonalSalud("MED04", "Rosa Elena",  "Huayta Condori",    "44123785", "rehuayta@minsa.gob.pe",    "ESP04", "Enfermería",        "Activo", "PS01", "Centro de Salud Los Licenciados"),
            // ── PS02: Centro de Salud Carmen Alto ──────────────────────────────
            PersonalSalud("MED05", "Luis Alberto","Cárdenas Rojas",    "43578214", "lacardenas@minsa.gob.pe",  "ESP01", "Medicina General", "Activo", "PS02", "Centro de Salud Carmen Alto"),
            PersonalSalud("MED06", "María Elena", "Huamán Sulca",      "45321098", "mehuaman@minsa.gob.pe",    "ESP02", "Pediatría",         "Activo", "PS02", "Centro de Salud Carmen Alto"),
            PersonalSalud("MED07", "Ana Sofía",   "Ccente Mendoza",    "46789012", "asccente@minsa.gob.pe",    "ESP03", "Obstetricia",       "Activo", "PS02", "Centro de Salud Carmen Alto"),
            PersonalSalud("MED08", "Jorge Luis",  "Ramos Chipana",     "47654321", "jlramos@minsa.gob.pe",     "ESP04", "Enfermería",        "Activo", "PS02", "Centro de Salud Carmen Alto"),
            // ── PS03: Puesto de Salud Santa Ana ────────────────────────────────
            PersonalSalud("MED09", "Roberto",     "Vargas Lira",       "40981234", "rvargas@minsa.gob.pe",     "ESP01", "Medicina General", "Activo", "PS03", "Puesto de Salud Santa Ana"),
            PersonalSalud("MED10", "Patricia",    "Quispe Ccoicca",    "48901234", "pquispe@minsa.gob.pe",     "ESP02", "Pediatría",         "Activo", "PS03", "Puesto de Salud Santa Ana"),
            PersonalSalud("MED11", "Sandra Luz",  "Huanca Mamani",     "49012345", "slhuanca@minsa.gob.pe",    "ESP03", "Obstetricia",       "Activo", "PS03", "Puesto de Salud Santa Ana"),
            PersonalSalud("MED12", "Carlos Iván", "Saravia Gutierrez", "40112233", "cisaravia@minsa.gob.pe",   "ESP04", "Enfermería",        "Activo", "PS03", "Puesto de Salud Santa Ana")
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
                // PS01 – Los Licenciados
                Horario("H01", d1, "Lunes",     "08:00", "12:00", 20, 20, "Disponible", "MED01", "PS01"),
                Horario("H02", d2, "Martes",    "08:00", "12:00", 15, 15, "Disponible", "MED02", "PS01"),
                Horario("H03", d3, "Miércoles", "09:00", "13:00", 12, 12, "Disponible", "MED03", "PS01"),
                Horario("H04", d4, "Jueves",    "14:00", "18:00", 18, 18, "Disponible", "MED04", "PS01"),
                // PS02 – Carmen Alto
                Horario("H05", d1, "Lunes",     "14:00", "18:00", 20, 20, "Disponible", "MED05", "PS02"),
                Horario("H06", d2, "Martes",    "08:00", "14:00", 30, 30, "Disponible", "MED06", "PS02"),
                Horario("H07", d3, "Miércoles", "08:00", "12:00", 15, 15, "Disponible", "MED07", "PS02"),
                Horario("H08", d5, "Viernes",   "09:00", "13:00", 20, 20, "Disponible", "MED08", "PS02"),
                // PS03 – Santa Ana
                Horario("H09", d1, "Lunes",     "08:00", "12:00", 15, 15, "Disponible", "MED09", "PS03"),
                Horario("H10", d3, "Miércoles", "14:00", "18:00", 12, 12, "Disponible", "MED10", "PS03"),
                Horario("H11", d4, "Jueves",    "08:00", "12:00", 10, 10, "Disponible", "MED11", "PS03"),
                Horario("H12", d5, "Viernes",   "14:00", "18:00", 15, 15, "Disponible", "MED12", "PS03")
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
                        "📋 Cobertura: 4 especialidades × 3 postas\n" +
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
