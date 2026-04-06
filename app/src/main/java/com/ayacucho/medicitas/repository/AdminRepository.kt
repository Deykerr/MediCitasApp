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
 * de Especialidades, Postas, Personal Médico y Horarios.
 *
 * RF06.1: CRUD Especialidades
 * RF06.2: CRUD Personal Médico
 * RF06.3: Configurar horarios
 * RF06.5: CRUD Postas Médicas
 */
class AdminRepository {

    private val db = FirebaseFirestore.getInstance()

    // ==================== ESPECIALIDADES (RF06.1) ====================

    fun agregarEspecialidad(
        nombre: String, descripcion: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        val nueva = Especialidad(
            idEspecialidad = id,
            nombreEspecialidad = nombre,
            descripcion = descripcion,
            estado = Constants.ESTADO_ACTIVO
        )
        db.collection(Constants.COLLECTION_ESPECIALIDADES).document(id).set(nueva)
            .addOnSuccessListener { onSuccess() }
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
        id: String, nombre: String, descripcion: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val datos = mapOf(
            "nombreEspecialidad" to nombre,
            "descripcion" to descripcion
        )
        db.collection(Constants.COLLECTION_ESPECIALIDADES).document(id).update(datos)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al editar especialidad") }
    }

    fun eliminarEspecialidad(
        id: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        // Eliminación lógica: cambiar estado a Inactivo
        db.collection(Constants.COLLECTION_ESPECIALIDADES).document(id)
            .update("estado", Constants.ESTADO_INACTIVO)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al eliminar especialidad") }
    }

    // ==================== POSTAS MÉDICAS (RF06.5) ====================

    fun agregarPosta(
        nombre: String, direccion: String, distrito: String, telefono: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        val nueva = PostaMedica(
            idPosta = id,
            nombrePosta = nombre,
            direccion = direccion,
            distrito = distrito,
            telefono = telefono,
            estado = Constants.ESTADO_ACTIVO
        )
        db.collection(Constants.COLLECTION_POSTAS).document(id).set(nueva)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al agregar posta") }
    }

    fun obtenerPostas(
        onSuccess: (List<PostaMedica>) -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_POSTAS)
            .whereEqualTo("estado", Constants.ESTADO_ACTIVO)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(PostaMedica::class.java)
                onSuccess(lista)
            }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al obtener postas") }
    }

    fun editarPosta(
        id: String, nombre: String, direccion: String, distrito: String, telefono: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val datos = mapOf(
            "nombrePosta" to nombre,
            "direccion" to direccion,
            "distrito" to distrito,
            "telefono" to telefono
        )
        db.collection(Constants.COLLECTION_POSTAS).document(id).update(datos)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al editar posta") }
    }

    fun eliminarPosta(
        id: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection(Constants.COLLECTION_POSTAS).document(id)
            .update("estado", Constants.ESTADO_INACTIVO)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al eliminar posta") }
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
        idPosta: String, nombrePosta: String,
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
                    idPosta = idPosta,
                    nombrePosta = nombrePosta,
                    rol = Constants.ROL_MEDICO
                )

                db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(uidMedico).set(nuevoMedico)
                    .addOnSuccessListener { onSuccess() }
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al desactivar médico") }
    }

    // ==================== HORARIOS (RF06.3) ====================

    fun agregarHorario(
        fecha: String, dia: String, horaInicio: String, horaFin: String,
        cuposTotales: Int, idPersonal: String, idPosta: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        val nuevo = Horario(
            idHorario = id,
            fecha = fecha,
            dia = dia,
            horaInicio = horaInicio,
            horaFin = horaFin,
            cuposDisponibles = cuposTotales,
            cuposTotales = cuposTotales,
            estado = Constants.ESTADO_HORARIO_DISPONIBLE,
            idPersonal = idPersonal,
            idPosta = idPosta
        )
        db.collection(Constants.COLLECTION_HORARIOS).document(id).set(nuevo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al agregar horario") }
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Error al eliminar horario") }
    }
}
