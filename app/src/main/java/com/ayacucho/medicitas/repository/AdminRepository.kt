package com.ayacucho.medicitas.repository

import android.content.Context
import com.ayacucho.medicitas.model.*
import com.ayacucho.medicitas.utils.Constants
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Repositorio del Administrador.
 * Encapsula toda la comunicación con Firestore para las operaciones CRUD
 * de Especialidades, Personal Médico y Horarios.
 * Adaptado para clínica privada (sin postas).
 *
 * RF06.1: CRUD Especialidades (con precio)
 * RF06.2: CRUD Personal Médico
 * RF06.3: Configurar horarios
 */
class AdminRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auditoria = AuditoriaRepository()

    // ==================== ESPECIALIDADES (RF06.1) ====================

    fun agregarEspecialidad(
        nombre: String, descripcion: String, precioConsulta: Double,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        val nueva = Especialidad(
            idEspecialidad = id,
            nombreEspecialidad = nombre,
            descripcion = descripcion,
            estado = Constants.ESTADO_ACTIVO,
            precioConsulta = precioConsulta
        )
        db.collection(Constants.COLLECTION_ESPECIALIDADES).document(id).set(nueva)
            .addOnSuccessListener { 
                auditoria.registrarAccion("CREAR_ESPECIALIDAD", "Especialidad", id, "Nombre: $nombre")
                onSuccess() 
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al agregar especialidad") }
    }

    fun obtenerEspecialidades(
        onSuccess: (List<Especialidad>) -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_ESPECIALIDADES)
            .whereEqualTo("estado", Constants.ESTADO_ACTIVO)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(Especialidad::class.java)
                onSuccess(lista)
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al obtener especialidades") }
    }

    fun editarEspecialidad(
        id: String, nombre: String, descripcion: String, precioConsulta: Double,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val datos = mapOf(
            "nombreEspecialidad" to nombre,
            "descripcion" to descripcion,
            "precioConsulta" to precioConsulta
        )
        db.collection(Constants.COLLECTION_ESPECIALIDADES).document(id).update(datos)
            .addOnSuccessListener { 
                auditoria.registrarAccion("EDITAR_ESPECIALIDAD", "Especialidad", id, "Nombre: $nombre")
                onSuccess() 
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al editar especialidad") }
    }

    fun eliminarEspecialidad(
        id: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        // Eliminación lógica: cambiar estado a Inactivo
        db.collection(Constants.COLLECTION_ESPECIALIDADES).document(id)
            .update("estado", Constants.ESTADO_INACTIVO)
            .addOnSuccessListener { 
                auditoria.registrarAccion("ELIMINAR_ESPECIALIDAD", "Especialidad", id, "Eliminación lógica")
                onSuccess() 
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al eliminar especialidad") }
    }

    // ==================== PERSONAL MÉDICO (RF06.2) ====================

    /**
     * Registra un médico creando su cuenta en Firebase Auth y guardando sus datos en Firestore.
     *
     * IMPORTANTE: Usamos una instancia secundaria de FirebaseApp para que
     * la sesión del administrador no se cierre al crear la cuenta del médico.
     */
    fun registrarMedico(
        context: Context,
        correo: String, contrasena: String,
        nombres: String, apellidos: String, dni: String,
        idEspecialidad: String, nombreEspecialidad: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        // Crear instancia secundaria de Firebase para no cerrar sesión del admin
        val secondaryApp = try {
            val options = FirebaseApp.getInstance().options
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(options.projectId)
                    .setApplicationId(options.applicationId)
                    .setApiKey(options.apiKey)
                    .build(),
                "SecondaryApp"
            )
        } catch (e: IllegalStateException) {
            // Ya existe la instancia secundaria
            FirebaseApp.getInstance("SecondaryApp")
        }

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

        secondaryAuth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { authResult ->
                val uidMedico = authResult.user?.uid ?: run {
                    onFailure("Error al obtener UID del médico")
                    return@addOnSuccessListener
                }

                // Cerrar sesión en la instancia secundaria
                secondaryAuth.signOut()

                val nuevoMedico = PersonalSalud(
                    idPersonal = uidMedico,
                    nombres = nombres,
                    apellidos = apellidos,
                    dni = dni,
                    correoElectronico = correo,
                    idEspecialidad = idEspecialidad,
                    nombreEspecialidad = nombreEspecialidad,
                    estado = Constants.ESTADO_ACTIVO,
                    rol = Constants.ROL_MEDICO
                )

                db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(uidMedico).set(nuevoMedico)
                    .addOnSuccessListener { 
                        auditoria.registrarAccion("CREAR_MEDICO", "PersonalSalud", uidMedico, "Nombre: $nombres $apellidos, Especialidad: $nombreEspecialidad")
                        onSuccess() 
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.localizedMessage ?: "Error al guardar datos del médico")
                    }
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.localizedMessage?.contains("email address is already") == true ->
                        "Este correo ya está registrado"
                    else -> e.localizedMessage ?: "Error al crear cuenta del médico"
                }
                onFailure(msg)
            }
    }

    fun obtenerPersonalMedico(
        onSuccess: (List<PersonalSalud>) -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PERSONAL_SALUD)
            .whereEqualTo("estado", Constants.ESTADO_ACTIVO)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(PersonalSalud::class.java)
                onSuccess(lista)
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al obtener personal médico") }
    }

    fun desactivarMedico(
        id: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(id)
            .update("estado", Constants.ESTADO_INACTIVO)
            .addOnSuccessListener { 
                auditoria.registrarAccion("DESACTIVAR_MEDICO", "PersonalSalud", id, "Baja lógica")
                onSuccess() 
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al desactivar médico") }
    }

    // ==================== HORARIOS (RF06.3) ====================

    fun agregarHorario(
        fecha: String, dia: String, horaInicio: String, horaFin: String,
        duracionCitaMinutos: Int, idPersonal: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        // Generar lista de Slots
        val slotsGenerados = generarSlots(fecha, horaInicio, horaFin, duracionCitaMinutos, idPersonal)
        if (slotsGenerados.isEmpty()) {
            onFailure("No se pueden crear slots con los horarios indicados")
            return
        }

        val id = UUID.randomUUID().toString()
        val nuevo = Horario(
            idHorario = id,
            fecha = fecha,
            dia = dia,
            horaInicio = horaInicio,
            horaFin = horaFin,
            duracionCitaMinutos = duracionCitaMinutos,
            cuposDisponibles = slotsGenerados.size,
            cuposTotales = slotsGenerados.size,
            estado = Constants.ESTADO_HORARIO_DISPONIBLE,
            idPersonal = idPersonal
        )

        val batch = db.batch()
        val horarioRef = db.collection(Constants.COLLECTION_HORARIOS).document(id)
        batch.set(horarioRef, nuevo)

        // Asignar idHorario a los slots y agregarlos al batch
        for (slot in slotsGenerados) {
            val slotFinal = slot.copy(idHorario = id)
            val slotRef = db.collection("slots_horario").document(slotFinal.idSlot)
            batch.set(slotRef, slotFinal)
        }

        batch.commit()
            .addOnSuccessListener { 
                auditoria.registrarAccion("CREAR_HORARIO", "Horario", id, "Fecha: $fecha, Médico ID: $idPersonal, Slots: ${slotsGenerados.size}")
                onSuccess() 
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al agregar horario y slots") }
    }

    /**
     * Genera la lista de SlotHorario a partir de la configuración.
     */
    private fun generarSlots(fecha: String, horaInicio: String, horaFin: String, duracionMinutos: Int, idMedico: String): List<SlotHorario> {
        val result = mutableListOf<SlotHorario>()
        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val inicio = sdf.parse(horaInicio) ?: return result
            val fin = sdf.parse(horaFin) ?: return result
            if (duracionMinutos <= 0) return result

            val cal = java.util.Calendar.getInstance()
            cal.time = inicio

            val calFin = java.util.Calendar.getInstance()
            calFin.time = fin

            while (cal.before(calFin)) {
                val hInicioStr = sdf.format(cal.time)
                cal.add(java.util.Calendar.MINUTE, duracionMinutos)
                if (cal.after(calFin)) break // No crear slot que termine después de horaFin
                val hFinStr = sdf.format(cal.time)

                val idSlot = UUID.randomUUID().toString()
                result.add(SlotHorario(
                    idSlot = idSlot,
                    idMedico = idMedico,
                    fecha = fecha,
                    horaInicio = hInicioStr,
                    horaFin = hFinStr,
                    estado = "Disponible"
                ))
            }
        } catch (e: Exception) { /* Ignorar */ }
        return result
    }

    /**
     * Calcula la cantidad de slots de cita en un bloque horario.
     * Ej: 08:00 a 12:00 con duración de 30 min = 8 slots.
     */
    private fun calcularSlots(horaInicio: String, horaFin: String, duracionMinutos: Int): Int {
        return try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val inicio = sdf.parse(horaInicio)
            val fin = sdf.parse(horaFin)
            if (inicio == null || fin == null || duracionMinutos <= 0) return 0
            val totalMinutos = ((fin.time - inicio.time) / 60000).toInt()
            if (totalMinutos <= 0) return 0
            totalMinutos / duracionMinutos
        } catch (e: Exception) { 0 }
    }

    fun obtenerHorarios(
        onSuccess: (List<Horario>) -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_HORARIOS)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(Horario::class.java)
                onSuccess(lista)
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al obtener horarios") }
    }

    fun eliminarHorario(
        id: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_HORARIOS).document(id).delete()
            .addOnSuccessListener { 
                auditoria.registrarAccion("ELIMINAR_HORARIO", "Horario", id, "Eliminación física")
                onSuccess() 
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al eliminar horario") }
    }
}
