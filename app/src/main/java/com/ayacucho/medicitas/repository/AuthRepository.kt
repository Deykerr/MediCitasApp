package com.ayacucho.medicitas.repository

import com.ayacucho.medicitas.model.Paciente
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repositorio de Autenticación.
 * Encapsula toda la comunicación con Firebase Authentication y Firestore
 * para las operaciones de login, registro, recuperación de contraseña y detección de rol.
 *
 * RF01.1: Registro de pacientes
 * RF01.2: Validación de DNI único
 * RF01.3: Login con correo y contraseña
 * RF01.4: Recuperación de contraseña
 * RF01.5: Cierre de sesión
 * RF01.6: Asignación de rol al crear usuario
 * RF01.7: Restricción de acceso según rol
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ==================== REGISTRO (RF01.1) ====================

    /**
     * Registra un nuevo paciente en Firebase Auth y guarda sus datos en Firestore.
     * Antes de crear la cuenta, valida que el DNI no esté duplicado (RF01.2).
     */
    fun registrarPaciente(
        correo: String,
        contrasena: String,
        nombres: String,
        apellidos: String,
        dni: String,
        telefono: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // RF01.2: Verificar que el DNI no exista en Firestore
        db.collection(Constants.COLLECTION_PACIENTES)
            .whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    onFailure("El DNI $dni ya está registrado en el sistema")
                    return@addOnSuccessListener
                }

                // DNI único confirmado → crear cuenta en Firebase Auth
                auth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: run {
                            onFailure("Error al obtener el ID del usuario")
                            return@addOnSuccessListener
                        }

                        // RF01.6: Crear objeto Paciente con rol asignado
                        val nuevoPaciente = Paciente(
                            idPaciente = uid,
                            nombres = nombres,
                            apellidos = apellidos,
                            dni = dni,
                            telefono = telefono,
                            correoElectronico = correo,
                            estadoCuenta = Constants.ESTADO_ACTIVO,
                            fechaRegistro = DateUtils.fechaHoraActual(),
                            rol = Constants.ROL_PACIENTE
                        )

                        // Guardar en Firestore con el mismo UID como ID del documento
                        db.collection(Constants.COLLECTION_PACIENTES)
                            .document(uid)
                            .set(nuevoPaciente)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onFailure(e.localizedMessage ?: "Error al guardar datos del paciente")
                            }
                    }
                    .addOnFailureListener { e ->
                        val mensaje = when {
                            e.localizedMessage?.contains("email address is already") == true ->
                                "Este correo ya está registrado"
                            e.localizedMessage?.contains("badly formatted") == true ->
                                "El formato del correo no es válido"
                            e.localizedMessage?.contains("weak password") == true ->
                                "La contraseña es muy débil (mínimo 6 caracteres)"
                            else -> e.localizedMessage ?: "Error al crear la cuenta"
                        }
                        onFailure(mensaje)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error al verificar DNI")
            }
    }

    // ==================== LOGIN (RF01.3) ====================

    /**
     * Inicia sesión con correo y contraseña, luego determina el rol del usuario
     * buscando su UID en las colecciones de Firestore.
     */
    fun iniciarSesion(
        correo: String,
        contrasena: String,
        onRoutePaciente: () -> Unit,
        onRouteMedico: () -> Unit,
        onRouteAdmin: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: run {
                    onFailure("Error al obtener el ID del usuario")
                    return@addOnSuccessListener
                }
                determinarRolYRedirigir(uid, onRoutePaciente, onRouteMedico, onRouteAdmin, onFailure)
            }
            .addOnFailureListener { e ->
                val mensaje = when {
                    e.localizedMessage?.contains("no user record") == true ->
                        "No existe una cuenta con este correo"
                    e.localizedMessage?.contains("password is invalid") == true ->
                        "La contraseña es incorrecta"
                    e.localizedMessage?.contains("blocked") == true ->
                        "Cuenta bloqueada por intentos fallidos. Intenta más tarde"
                    else -> e.localizedMessage ?: "Error al iniciar sesión"
                }
                onFailure(mensaje)
            }
    }

    // ==================== DETECCIÓN DE ROL (RF01.6, RF01.7) ====================

    /**
     * Busca el UID del usuario en las colecciones de Firestore (pacientes → personal_salud → administradores)
     * para determinar su rol y redirigirlo a la pantalla correspondiente.
     */
    fun determinarRolYRedirigir(
        uid: String,
        onRoutePaciente: () -> Unit,
        onRouteMedico: () -> Unit,
        onRouteAdmin: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // 1. Buscar en pacientes
        db.collection(Constants.COLLECTION_PACIENTES).document(uid).get()
            .addOnSuccessListener { docPaciente ->
                if (docPaciente.exists()) {
                    onRoutePaciente()
                } else {
                    // 2. Buscar en personal de salud
                    db.collection(Constants.COLLECTION_PERSONAL_SALUD).document(uid).get()
                        .addOnSuccessListener { docMedico ->
                            if (docMedico.exists()) {
                                onRouteMedico()
                            } else {
                                // 3. Buscar en administradores
                                db.collection(Constants.COLLECTION_ADMINISTRADORES).document(uid).get()
                                    .addOnSuccessListener { docAdmin ->
                                        if (docAdmin.exists()) {
                                            onRouteAdmin()
                                        } else {
                                            onFailure("Usuario no encontrado en la base de datos. Contacta al administrador.")
                                        }
                                    }
                                    .addOnFailureListener {
                                        onFailure("Error al consultar rol de administrador")
                                    }
                            }
                        }
                        .addOnFailureListener {
                            onFailure("Error al consultar rol de médico")
                        }
                }
            }
            .addOnFailureListener {
                onFailure("Error al consultar rol del usuario")
            }
    }

    // ==================== RECUPERAR CONTRASEÑA (RF01.4) ====================

    /**
     * Envía un correo de recuperación de contraseña al email proporcionado.
     */
    fun recuperarContrasena(
        correo: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                val mensaje = when {
                    e.localizedMessage?.contains("no user record") == true ->
                        "No existe una cuenta con este correo"
                    else -> e.localizedMessage ?: "Error al enviar correo de recuperación"
                }
                onFailure(mensaje)
            }
    }

    // ==================== SESIÓN (RF01.5) ====================

    /**
     * Cierra la sesión actual del usuario.
     */
    fun cerrarSesion() {
        auth.signOut()
    }

    /**
     * Retorna el UID del usuario actualmente logueado, o null si no hay sesión.
     */
    fun obtenerUsuarioActual(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Verifica si hay una sesión activa.
     */
    fun haySesionActiva(): Boolean {
        return auth.currentUser != null
    }
}
