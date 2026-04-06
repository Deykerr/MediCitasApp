package com.ayacucho.medicitas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayacucho.medicitas.repository.AuthRepository
import com.ayacucho.medicitas.utils.ValidationUtils

/**
 * ViewModel de Autenticación.
 * Conecta las vistas (Activities) con el AuthRepository.
 * Expone LiveData para que las vistas observen los estados de forma reactiva.
 *
 * Valida los datos de entrada antes de enviarlos al repositorio.
 */
class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // Estado de carga (para mostrar/ocultar ProgressBar)
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Mensajes de error para la UI
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Resultado de operaciones exitosas
    private val _registroExitoso = MutableLiveData(false)
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _recuperacionExitosa = MutableLiveData(false)
    val recuperacionExitosa: LiveData<Boolean> = _recuperacionExitosa

    // Navegación por rol tras login exitoso
    private val _navegarPaciente = MutableLiveData(false)
    val navegarPaciente: LiveData<Boolean> = _navegarPaciente

    private val _navegarMedico = MutableLiveData(false)
    val navegarMedico: LiveData<Boolean> = _navegarMedico

    private val _navegarAdmin = MutableLiveData(false)
    val navegarAdmin: LiveData<Boolean> = _navegarAdmin

    // ==================== REGISTRO ====================

    /**
     * Valida todos los campos del formulario de registro y, si son correctos,
     * invoca el repositorio para crear la cuenta.
     */
    fun registrarPaciente(
        nombres: String,
        apellidos: String,
        dni: String,
        telefono: String,
        correo: String,
        contrasena: String,
        confirmarContrasena: String
    ) {
        // Validaciones (RF01.8)
        if (nombres.isBlank() || apellidos.isBlank() || dni.isBlank() ||
            telefono.isBlank() || correo.isBlank() || contrasena.isBlank()
        ) {
            _errorMessage.value = "Por favor completa todos los campos"
            return
        }

        if (!ValidationUtils.validarDni(dni)) {
            _errorMessage.value = "El DNI debe tener 8 dígitos numéricos"
            return
        }

        if (!ValidationUtils.validarTelefono(telefono)) {
            _errorMessage.value = "El teléfono debe tener 9 dígitos"
            return
        }

        if (!ValidationUtils.validarCorreo(correo)) {
            _errorMessage.value = "Ingresa un correo electrónico válido"
            return
        }

        if (!ValidationUtils.validarContrasena(contrasena)) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (contrasena != confirmarContrasena) {
            _errorMessage.value = "Las contraseñas no coinciden"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        authRepository.registrarPaciente(
            correo = correo,
            contrasena = contrasena,
            nombres = nombres.trim(),
            apellidos = apellidos.trim(),
            dni = dni.trim(),
            telefono = telefono.trim(),
            onSuccess = {
                _isLoading.value = false
                _registroExitoso.value = true
            },
            onFailure = { mensaje ->
                _isLoading.value = false
                _errorMessage.value = mensaje
            }
        )
    }

    // ==================== LOGIN ====================

    /**
     * Valida correo y contraseña, luego invoca el login.
     * Cuando Firebase confirma las credenciales, determina el rol y
     * activa el LiveData de navegación correspondiente.
     */
    fun iniciarSesion(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _errorMessage.value = "Por favor completa todos los campos"
            return
        }

        if (!ValidationUtils.validarCorreo(correo)) {
            _errorMessage.value = "Ingresa un correo electrónico válido"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        authRepository.iniciarSesion(
            correo = correo,
            contrasena = contrasena,
            onRoutePaciente = {
                _isLoading.value = false
                _navegarPaciente.value = true
            },
            onRouteMedico = {
                _isLoading.value = false
                _navegarMedico.value = true
            },
            onRouteAdmin = {
                _isLoading.value = false
                _navegarAdmin.value = true
            },
            onFailure = { mensaje ->
                _isLoading.value = false
                _errorMessage.value = mensaje
            }
        )
    }

    // ==================== RECUPERAR CONTRASEÑA ====================

    fun recuperarContrasena(correo: String) {
        if (correo.isBlank()) {
            _errorMessage.value = "Ingresa tu correo electrónico"
            return
        }

        if (!ValidationUtils.validarCorreo(correo)) {
            _errorMessage.value = "Ingresa un correo electrónico válido"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        authRepository.recuperarContrasena(
            correo = correo,
            onSuccess = {
                _isLoading.value = false
                _recuperacionExitosa.value = true
            },
            onFailure = { mensaje ->
                _isLoading.value = false
                _errorMessage.value = mensaje
            }
        )
    }

    // ==================== SESIÓN ====================

    /**
     * Verifica si hay sesión activa y redirige según el rol.
     * Se usa en MainActivity/SplashActivity para mantener la sesión (Paso 5).
     */
    fun verificarSesionActiva() {
        val uid = authRepository.obtenerUsuarioActual()
        if (uid != null) {
            _isLoading.value = true
            authRepository.determinarRolYRedirigir(
                uid = uid,
                onRoutePaciente = {
                    _isLoading.value = false
                    _navegarPaciente.value = true
                },
                onRouteMedico = {
                    _isLoading.value = false
                    _navegarMedico.value = true
                },
                onRouteAdmin = {
                    _isLoading.value = false
                    _navegarAdmin.value = true
                },
                onFailure = {
                    _isLoading.value = false
                    // Sesión inválida, el usuario verá el login
                }
            )
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }

    fun haySesionActiva(): Boolean {
        return authRepository.haySesionActiva()
    }

    // Limpiar estados de navegación después de navegar
    fun navegacionPacienteCompletada() { _navegarPaciente.value = false }
    fun navegacionMedicoCompletada() { _navegarMedico.value = false }
    fun navegacionAdminCompletada() { _navegarAdmin.value = false }
    fun registroCompletado() { _registroExitoso.value = false }
    fun recuperacionCompletada() { _recuperacionExitosa.value = false }
    fun limpiarError() { _errorMessage.value = null }
}
