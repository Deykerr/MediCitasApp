package com.ayacucho.medicitas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayacucho.medicitas.model.*
import com.ayacucho.medicitas.repository.PacienteRepository
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel del Paciente.
 * Conecta las vistas del paciente con el PacienteRepository.
 * Gestiona el flujo de exploración en cascada y la reserva de citas.
 */
class PacienteViewModel : ViewModel() {

    private val repo = PacienteRepository()
    private val auth = FirebaseAuth.getInstance()

    val uidPaciente: String get() = auth.currentUser?.uid ?: ""

    // Estado general
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    // Datos del paciente
    private val _datosPaciente = MutableLiveData<Paciente>()
    val datosPaciente: LiveData<Paciente> = _datosPaciente

    // ==================== EXPLORACIÓN EN CASCADA ====================

    private val _postas = MutableLiveData<List<PostaMedica>>()
    val postas: LiveData<List<PostaMedica>> = _postas

    private val _especialidades = MutableLiveData<List<Especialidad>>()
    val especialidades: LiveData<List<Especialidad>> = _especialidades

    private val _medicos = MutableLiveData<List<PersonalSalud>>()
    val medicos: LiveData<List<PersonalSalud>> = _medicos

    private val _horarios = MutableLiveData<List<Horario>>()
    val horarios: LiveData<List<Horario>> = _horarios

    // Selecciones actuales del paso a paso
    private val _postaSeleccionada = MutableLiveData<PostaMedica?>()
    val postaSeleccionada: LiveData<PostaMedica?> = _postaSeleccionada

    private val _especialidadSeleccionada = MutableLiveData<Especialidad?>()
    val especialidadSeleccionada: LiveData<Especialidad?> = _especialidadSeleccionada

    private val _medicoSeleccionado = MutableLiveData<PersonalSalud?>()
    val medicoSeleccionado: LiveData<PersonalSalud?> = _medicoSeleccionado

    private val _horarioSeleccionado = MutableLiveData<Horario?>()
    val horarioSeleccionado: LiveData<Horario?> = _horarioSeleccionado

    // ==================== HISTORIAL ====================

    private val _citasProximas = MutableLiveData<List<CitaMedica>>()
    val citasProximas: LiveData<List<CitaMedica>> = _citasProximas

    private val _citasPasadas = MutableLiveData<List<CitaMedica>>()
    val citasPasadas: LiveData<List<CitaMedica>> = _citasPasadas

    // Reserva exitosa
    private val _reservaExitosa = MutableLiveData<String?>()
    val reservaExitosa: LiveData<String?> = _reservaExitosa

    // ==================== CARGAR DATOS ====================

    fun cargarDatosPaciente() {
        if (uidPaciente.isBlank()) return
        repo.obtenerDatosPaciente(uidPaciente,
            onSuccess = { _datosPaciente.value = it },
            onFailure = { _mensaje.value = it }
        )
    }

    // ==================== FLUJO DE EXPLORACIÓN ====================

    fun cargarPostas() {
        _isLoading.value = true
        repo.obtenerPostas(
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

    fun seleccionarPosta(posta: PostaMedica) {
        _postaSeleccionada.value = posta
        // Limpiar selecciones posteriores
        _especialidadSeleccionada.value = null
        _medicoSeleccionado.value = null
        _horarioSeleccionado.value = null
        // Cargar especialidades
        cargarEspecialidades()
    }

    fun cargarEspecialidades() {
        _isLoading.value = true
        repo.obtenerEspecialidades(
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

    fun seleccionarEspecialidad(especialidad: Especialidad) {
        _especialidadSeleccionada.value = especialidad
        _medicoSeleccionado.value = null
        _horarioSeleccionado.value = null
        // Cargar médicos filtrados por posta + especialidad
        val posta = _postaSeleccionada.value ?: return
        cargarMedicos(posta.idPosta, especialidad.idEspecialidad)
    }

    fun cargarMedicos(idPosta: String, idEspecialidad: String) {
        _isLoading.value = true
        repo.obtenerMedicosFiltrados(idPosta, idEspecialidad,
            onSuccess = { lista ->
                _isLoading.value = false
                _medicos.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun seleccionarMedico(medico: PersonalSalud) {
        _medicoSeleccionado.value = medico
        _horarioSeleccionado.value = null
        cargarHorarios(medico.idPersonal)
    }

    fun cargarHorarios(idMedico: String) {
        _isLoading.value = true
        repo.obtenerHorariosDisponibles(idMedico,
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

    fun seleccionarHorario(horario: Horario) {
        _horarioSeleccionado.value = horario
    }

    // ==================== RESERVA (RF04) ====================

    fun confirmarReserva(motivo: String, horaSeleccionada: String) {
        val paciente = _datosPaciente.value
        val medico = _medicoSeleccionado.value
        val posta = _postaSeleccionada.value
        val especialidad = _especialidadSeleccionada.value
        val horario = _horarioSeleccionado.value

        if (paciente == null || medico == null || posta == null ||
            especialidad == null || horario == null) {
            _mensaje.value = "Error: faltan datos para completar la reserva"
            return
        }

        if (horaSeleccionada.isBlank()) {
            _mensaje.value = "Selecciona una hora para tu cita"
            return
        }

        _isLoading.value = true
        repo.confirmarReserva(
            horarioSeleccionado = horario,
            horaSeleccionada = horaSeleccionada,
            motivo = motivo,
            paciente = paciente,
            medico = medico,
            posta = posta,
            especialidad = especialidad,
            onSuccess = { idCita ->
                _isLoading.value = false
                _mensaje.value = "¡Cita reservada exitosamente!"
                _reservaExitosa.value = idCita
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== HISTORIAL (RF04.5) ====================

    fun cargarMisCitas() {
        if (uidPaciente.isBlank()) return
        _isLoading.value = true
        repo.obtenerMisCitas(uidPaciente,
            onSuccess = { todasLasCitas ->
                _isLoading.value = false
                val fechaHoy = DateUtils.fechaActual()
                // Separar en próximas (Reservada con fecha >= hoy) y pasadas
                _citasProximas.value = todasLasCitas.filter {
                    it.estadoCita == Constants.ESTADO_CITA_RESERVADA &&
                    compararFechas(it.fecha, fechaHoy) >= 0
                }.sortedBy { it.fecha + it.hora }

                _citasPasadas.value = todasLasCitas.filter {
                    it.estadoCita != Constants.ESTADO_CITA_RESERVADA ||
                    compararFechas(it.fecha, fechaHoy) < 0
                }.sortedByDescending { it.fecha + it.hora }
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== CANCELACIÓN (RF04.6) ====================

    fun cancelarCita(cita: CitaMedica) {
        _isLoading.value = true
        repo.cancelarCita(cita,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Cita cancelada exitosamente"
                cargarMisCitas() // Recargar la lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== UTILIDADES ====================

    fun limpiarSelecciones() {
        _postaSeleccionada.value = null
        _especialidadSeleccionada.value = null
        _medicoSeleccionado.value = null
        _horarioSeleccionado.value = null
        _reservaExitosa.value = null
    }

    fun limpiarMensaje() { _mensaje.value = null }
    fun reservaCompletada() { _reservaExitosa.value = null }

    fun cerrarSesion() { auth.signOut() }

    private fun compararFechas(fecha1: String, fecha2: String): Int {
        return try {
            val sdf = java.text.SimpleDateFormat(Constants.FORMATO_FECHA, java.util.Locale("es", "PE"))
            val d1 = sdf.parse(fecha1)
            val d2 = sdf.parse(fecha2)
            d1!!.compareTo(d2!!)
        } catch (e: Exception) { 0 }
    }

    // ==================== LISTENER EN TIEMPO REAL (RF07.3) ====================
    private val _cambioCitaDetectado = MutableLiveData<CitaMedica>()
    val cambioCitaDetectado: LiveData<CitaMedica> = _cambioCitaDetectado

    private var listenerCitas: com.google.firebase.firestore.ListenerRegistration? = null

    fun escucharCambiosCitas() {
        if (uidPaciente.isBlank() || listenerCitas != null) return
        listenerCitas = repo.escucharCambiosCitasRealtime(uidPaciente) { cita ->
            _cambioCitaDetectado.value = cita
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerCitas?.remove()
    }
}
