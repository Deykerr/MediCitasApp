package com.ayacucho.medicitas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.repository.RecepcionistaRepository
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth

class RecepcionistaViewModel : ViewModel() {

    private val repository = RecepcionistaRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _datosRecepcionista = MutableLiveData<PersonalSalud>()
    val datosRecepcionista: LiveData<PersonalSalud> = _datosRecepcionista

    private val _todasLasCitas = MutableLiveData<List<CitaMedica>>()
    val todasLasCitas: LiveData<List<CitaMedica>> = _todasLasCitas

    private val _fechaSeleccionada = MutableLiveData(DateUtils.fechaActual())
    val fechaSeleccionada: LiveData<String> = _fechaSeleccionada

    fun cargarDatos() {
        val uid = auth.currentUser?.uid ?: return
        repository.obtenerDatosRecepcionista(uid,
            onSuccess = { _datosRecepcionista.value = it },
            onFailure = { _mensaje.value = it }
        )
    }

    fun escucharCitas() {
        val fecha = _fechaSeleccionada.value ?: DateUtils.fechaActual()
        _isLoading.value = true
        repository.escucharTodasLasCitas(fecha,
            onUpdate = {
                _isLoading.value = false
                _todasLasCitas.value = it
            },
            onFailure = {
                _isLoading.value = false
                _mensaje.value = it
            }
        )
    }

    fun cambiarFecha(nuevaFecha: String) {
        _fechaSeleccionada.value = nuevaFecha
        escucharCitas()
    }

    fun marcarAsistencia(idCita: String, estadoAsistencia: String) {
        repository.actualizarEstadoAsistencia(idCita, estadoAsistencia,
            onSuccess = { _mensaje.value = "Asistencia actualizada" },
            onFailure = { _mensaje.value = it }
        )
    }

    fun marcarPagada(idCita: String, monto: Double, metodo: String, referencia: String) {
        repository.registrarPago(idCita, monto, metodo, referencia,
            onSuccess = { _mensaje.value = "Pago registrado exitosamente" },
            onFailure = { _mensaje.value = it }
        )
    }

    fun cancelarCita(idCita: String) {
        repository.actualizarEstadoCita(idCita, Constants.ESTADO_CITA_CANCELADA,
            onSuccess = { _mensaje.value = "Cita cancelada" },
            onFailure = { _mensaje.value = it }
        )
    }

    fun cerrarSesion() {
        repository.detenerEscucha()
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        repository.detenerEscucha()
    }
}
