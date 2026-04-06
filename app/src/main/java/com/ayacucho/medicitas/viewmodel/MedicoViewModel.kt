package com.ayacucho.medicitas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.model.Horario
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.repository.MedicoRepository
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel del Personal Médico.
 * Conecta las vistas del médico con el MedicoRepository.
 * Expone LiveData para la agenda, horarios y datos del médico.
 */
class MedicoViewModel : ViewModel() {

    private val medicoRepository = MedicoRepository()
    private val auth = FirebaseAuth.getInstance()

    // UID del médico logueado
    val uidMedico: String get() = auth.currentUser?.uid ?: ""

    // Estado general
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    // Datos del médico
    private val _datosMedico = MutableLiveData<PersonalSalud>()
    val datosMedico: LiveData<PersonalSalud> = _datosMedico

    // Citas del día (agenda - tiempo real)
    private val _citasDelDia = MutableLiveData<List<CitaMedica>>()
    val citasDelDia: LiveData<List<CitaMedica>> = _citasDelDia

    // Fecha seleccionada para la agenda
    private val _fechaSeleccionada = MutableLiveData(DateUtils.fechaActual())
    val fechaSeleccionada: LiveData<String> = _fechaSeleccionada

    // Horarios del médico
    private val _misHorarios = MutableLiveData<List<Horario>>()
    val misHorarios: LiveData<List<Horario>> = _misHorarios

    // Estadísticas del día
    private val _totalCitas = MutableLiveData(0)
    val totalCitas: LiveData<Int> = _totalCitas

    private val _citasAtendidas = MutableLiveData(0)
    val citasAtendidas: LiveData<Int> = _citasAtendidas

    private val _citasPendientes = MutableLiveData(0)
    val citasPendientes: LiveData<Int> = _citasPendientes

    // ==================== DATOS DEL MÉDICO ====================

    fun cargarDatosMedico() {
        if (uidMedico.isBlank()) return
        medicoRepository.obtenerDatosMedico(uidMedico,
            onSuccess = { medico -> _datosMedico.value = medico },
            onFailure = { msg -> _mensaje.value = msg }
        )
    }

    // ==================== AGENDA DEL DÍA (RF05.1) ====================

    /**
     * Inicia la escucha en tiempo real de las citas del día.
     * Se actualiza automáticamente cuando un paciente reserva o cancela.
     */
    fun escucharAgendaDelDia() {
        if (uidMedico.isBlank()) return
        val fecha = _fechaSeleccionada.value ?: DateUtils.fechaActual()

        _isLoading.value = true
        medicoRepository.escucharCitasDelDia(
            idMedico = uidMedico,
            fecha = fecha,
            onUpdate = { citas ->
                _isLoading.value = false
                _citasDelDia.value = citas
                actualizarEstadisticas(citas)
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    /**
     * Cambia la fecha de la agenda y reinicia la escucha.
     */
    fun cambiarFechaAgenda(nuevaFecha: String) {
        _fechaSeleccionada.value = nuevaFecha
        escucharAgendaDelDia()
    }

    // ==================== GESTIÓN DE ESTADOS (RF05.3) ====================

    fun marcarComoAtendida(idCita: String) {
        actualizarEstado(idCita, Constants.ESTADO_CITA_ATENDIDA)
    }

    fun marcarComoNoAsistio(idCita: String) {
        actualizarEstado(idCita, Constants.ESTADO_CITA_NO_ASISTIO)
    }

    fun marcarComoCancelada(idCita: String) {
        actualizarEstado(idCita, Constants.ESTADO_CITA_CANCELADA)
    }

    private fun actualizarEstado(idCita: String, nuevoEstado: String) {
        _isLoading.value = true
        medicoRepository.actualizarEstadoCita(idCita, nuevoEstado,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Cita marcada como: $nuevoEstado"
                // El SnapshotListener actualizará la lista automáticamente
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== HORARIOS (RF05.4, RF05.5) ====================

    fun cargarMisHorarios() {
        if (uidMedico.isBlank()) return
        _isLoading.value = true
        medicoRepository.obtenerMisHorarios(uidMedico,
            onSuccess = { horarios ->
                _isLoading.value = false
                _misHorarios.value = horarios
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun bloquearHorario(idHorario: String) {
        _isLoading.value = true
        medicoRepository.bloquearHorario(idHorario,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Horario bloqueado"
                cargarMisHorarios()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun desbloquearHorario(idHorario: String) {
        _isLoading.value = true
        medicoRepository.desbloquearHorario(idHorario,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Horario desbloqueado"
                cargarMisHorarios()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== ESTADÍSTICAS ====================

    private fun actualizarEstadisticas(citas: List<CitaMedica>) {
        _totalCitas.value = citas.size
        _citasAtendidas.value = citas.count { it.estadoCita == Constants.ESTADO_CITA_ATENDIDA }
        _citasPendientes.value = citas.count { it.estadoCita == Constants.ESTADO_CITA_RESERVADA }
    }

    // ==================== UTILIDADES ====================

    /**
     * Genera las fechas de la semana actual (Lun-Dom) en formato dd/MM/yyyy.
     */
    fun obtenerFechasSemanaActual(): List<String> {
        val sdf = SimpleDateFormat(Constants.FORMATO_FECHA, Locale("es", "PE"))
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        return (0..6).map {
            val fecha = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            fecha
        }
    }

    fun limpiarMensaje() { _mensaje.value = null }

    fun cerrarSesion() {
        medicoRepository.detenerEscuchaCitas()
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        medicoRepository.detenerEscuchaCitas()
    }
}
