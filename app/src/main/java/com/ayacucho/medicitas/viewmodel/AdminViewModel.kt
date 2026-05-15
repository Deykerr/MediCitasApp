package com.ayacucho.medicitas.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayacucho.medicitas.model.*
import com.ayacucho.medicitas.repository.AdminRepository
import com.ayacucho.medicitas.utils.ValidationUtils

/**
 * ViewModel del Administrador.
 * Conecta las vistas del admin con el AdminRepository.
 * Expone LiveData para listas, estados de carga y mensajes.
 * Adaptado para clínica privada (sin postas, con precios).
 */
class AdminViewModel : ViewModel() {

    private val adminRepository = AdminRepository()

    // Estado general
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _operacionExitosa = MutableLiveData(false)
    val operacionExitosa: LiveData<Boolean> = _operacionExitosa

    // Listas
    private val _especialidades = MutableLiveData<List<Especialidad>>()
    val especialidades: LiveData<List<Especialidad>> = _especialidades

    private val _personalMedico = MutableLiveData<List<PersonalSalud>>()
    val personalMedico: LiveData<List<PersonalSalud>> = _personalMedico

    private val _horarios = MutableLiveData<List<Horario>>()
    val horarios: LiveData<List<Horario>> = _horarios

    // ==================== ESPECIALIDADES ====================

    fun cargarEspecialidades() {
        _isLoading.value = true
        adminRepository.obtenerEspecialidades(
            onSuccess = { lista ->
                _isLoading.value = false
                _especialidades.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun agregarEspecialidad(nombre: String, descripcion: String, precioConsulta: Double) {
        if (nombre.isBlank()) {
            _mensaje.value = "El nombre de la especialidad es obligatorio"
            return
        }
        if (precioConsulta <= 0) {
            _mensaje.value = "El precio de consulta debe ser mayor a 0"
            return
        }
        _isLoading.value = true
        adminRepository.agregarEspecialidad(nombre.trim(), descripcion.trim(), precioConsulta,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Especialidad agregada correctamente"
                _operacionExitosa.value = true
                cargarEspecialidades()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun editarEspecialidad(id: String, nombre: String, descripcion: String, precioConsulta: Double) {
        if (nombre.isBlank()) {
            _mensaje.value = "El nombre de la especialidad es obligatorio"
            return
        }
        if (precioConsulta <= 0) {
            _mensaje.value = "El precio de consulta debe ser mayor a 0"
            return
        }
        _isLoading.value = true
        adminRepository.editarEspecialidad(id, nombre.trim(), descripcion.trim(), precioConsulta,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Especialidad actualizada correctamente"
                _operacionExitosa.value = true
                cargarEspecialidades()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun eliminarEspecialidad(id: String) {
        _isLoading.value = true
        adminRepository.eliminarEspecialidad(id,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Especialidad eliminada"
                cargarEspecialidades()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== PERSONAL MÉDICO ====================

    fun cargarPersonalMedico() {
        _isLoading.value = true
        adminRepository.obtenerPersonalMedico(
            onSuccess = { lista ->
                _isLoading.value = false
                _personalMedico.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun registrarMedico(
        context: Context,
        correo: String, contrasena: String,
        nombres: String, apellidos: String, dni: String,
        idEspecialidad: String, nombreEspecialidad: String
    ) {
        if (nombres.isBlank() || apellidos.isBlank() || dni.isBlank() ||
            correo.isBlank() || contrasena.isBlank()
        ) {
            _mensaje.value = "Todos los campos son obligatorios"
            return
        }
        if (!ValidationUtils.validarDni(dni)) {
            _mensaje.value = "El DNI debe tener 8 dígitos numéricos"
            return
        }
        if (!ValidationUtils.validarCorreo(correo)) {
            _mensaje.value = "Ingresa un correo válido"
            return
        }
        if (!ValidationUtils.validarContrasena(contrasena)) {
            _mensaje.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }
        if (idEspecialidad.isBlank()) {
            _mensaje.value = "Selecciona una especialidad"
            return
        }

        _isLoading.value = true
        adminRepository.registrarMedico(context, correo, contrasena,
            nombres.trim(), apellidos.trim(), dni.trim(),
            idEspecialidad, nombreEspecialidad,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Médico registrado correctamente"
                _operacionExitosa.value = true
                cargarPersonalMedico()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun desactivarMedico(id: String) {
        _isLoading.value = true
        adminRepository.desactivarMedico(id,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Médico desactivado"
                cargarPersonalMedico()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== HORARIOS ====================

    fun cargarHorarios() {
        _isLoading.value = true
        adminRepository.obtenerHorarios(
            onSuccess = { lista ->
                _isLoading.value = false
                _horarios.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun agregarHorario(
        fecha: String, dia: String, horaInicio: String, horaFin: String,
        duracionCitaMinutos: Int, idPersonal: String
    ) {
        if (fecha.isBlank() || horaInicio.isBlank() || horaFin.isBlank() || idPersonal.isBlank()) {
            _mensaje.value = "Todos los campos del horario son obligatorios"
            return
        }
        if (duracionCitaMinutos <= 0) {
            _mensaje.value = "La duración de cada cita debe ser mayor a 0 minutos"
            return
        }
        _isLoading.value = true
        adminRepository.agregarHorario(fecha, dia, horaInicio, horaFin, duracionCitaMinutos, idPersonal,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Horario agregado correctamente"
                _operacionExitosa.value = true
                cargarHorarios()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun eliminarHorario(id: String) {
        _isLoading.value = true
        adminRepository.eliminarHorario(id,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Horario eliminado"
                cargarHorarios()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // Utilidades
    fun limpiarMensaje() { _mensaje.value = null }
    fun operacionCompletada() { _operacionExitosa.value = false }
}
