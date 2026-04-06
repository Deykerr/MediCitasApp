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

    private val _postas = MutableLiveData<List<PostaMedica>>()
    val postas: LiveData<List<PostaMedica>> = _postas

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

    fun agregarEspecialidad(nombre: String, descripcion: String) {
        if (nombre.isBlank()) {
            _mensaje.value = "El nombre de la especialidad es obligatorio"
            return
        }
        _isLoading.value = true
        adminRepository.agregarEspecialidad(nombre.trim(), descripcion.trim(),
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

    fun editarEspecialidad(id: String, nombre: String, descripcion: String) {
        if (nombre.isBlank()) {
            _mensaje.value = "El nombre de la especialidad es obligatorio"
            return
        }
        _isLoading.value = true
        adminRepository.editarEspecialidad(id, nombre.trim(), descripcion.trim(),
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

    // ==================== POSTAS ====================

    fun cargarPostas() {
        _isLoading.value = true
        adminRepository.obtenerPostas(
            onSuccess = { lista ->
                _isLoading.value = false
                _postas.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun agregarPosta(nombre: String, direccion: String, distrito: String, telefono: String) {
        if (nombre.isBlank() || direccion.isBlank() || distrito.isBlank()) {
            _mensaje.value = "Nombre, dirección y distrito son obligatorios"
            return
        }
        _isLoading.value = true
        adminRepository.agregarPosta(nombre.trim(), direccion.trim(), distrito.trim(), telefono.trim(),
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Posta agregada correctamente"
                _operacionExitosa.value = true
                cargarPostas()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun editarPosta(id: String, nombre: String, direccion: String, distrito: String, telefono: String) {
        if (nombre.isBlank() || direccion.isBlank() || distrito.isBlank()) {
            _mensaje.value = "Nombre, dirección y distrito son obligatorios"
            return
        }
        _isLoading.value = true
        adminRepository.editarPosta(id, nombre.trim(), direccion.trim(), distrito.trim(), telefono.trim(),
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Posta actualizada correctamente"
                _operacionExitosa.value = true
                cargarPostas()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun eliminarPosta(id: String) {
        _isLoading.value = true
        adminRepository.eliminarPosta(id,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Posta eliminada"
                cargarPostas()
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
        idEspecialidad: String, nombreEspecialidad: String,
        idPosta: String, nombrePosta: String
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
        if (idEspecialidad.isBlank() || idPosta.isBlank()) {
            _mensaje.value = "Selecciona una especialidad y una posta"
            return
        }

        _isLoading.value = true
        adminRepository.registrarMedico(context, correo, contrasena,
            nombres.trim(), apellidos.trim(), dni.trim(),
            idEspecialidad, nombreEspecialidad, idPosta, nombrePosta,
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
        cuposTotales: Int, idPersonal: String, idPosta: String
    ) {
        if (fecha.isBlank() || horaInicio.isBlank() || horaFin.isBlank() || idPersonal.isBlank()) {
            _mensaje.value = "Todos los campos del horario son obligatorios"
            return
        }
        if (cuposTotales <= 0) {
            _mensaje.value = "Los cupos deben ser mayor a 0"
            return
        }
        _isLoading.value = true
        adminRepository.agregarHorario(fecha, dia, horaInicio, horaFin, cuposTotales, idPersonal, idPosta,
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
