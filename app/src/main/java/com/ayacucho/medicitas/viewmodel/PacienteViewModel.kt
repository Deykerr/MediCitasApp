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
 * Adaptado para clínica privada (sin postas, con pago).
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

    // ==================== EXPLORACIÓN EN CASCADA (sin postas) ====================

    private val _especialidades = MutableLiveData<List<Especialidad>>()
    val especialidades: LiveData<List<Especialidad>> = _especialidades

    private val _medicos = MutableLiveData<List<PersonalSalud>>()
    val medicos: LiveData<List<PersonalSalud>> = _medicos

    private val _horarios = MutableLiveData<List<Horario>>()
    val horarios: LiveData<List<Horario>> = _horarios

    // Selecciones actuales del paso a paso
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

    private val _citaHoy = MutableLiveData<CitaMedica?>()
    val citaHoy: LiveData<CitaMedica?> = _citaHoy

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

    fun actualizarPerfil(datos: Map<String, Any>) {
        if (uidPaciente.isBlank()) return
        _isLoading.value = true
        repo.actualizarPerfilPaciente(uidPaciente, datos,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Perfil actualizado exitosamente"
                cargarDatosPaciente() // Recargar datos actualizados
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== DEPENDIENTES ====================

    private val _dependientes = MutableLiveData<List<Dependiente>>()
    val dependientes: LiveData<List<Dependiente>> = _dependientes

    fun cargarDependientes() {
        if (uidPaciente.isBlank()) return
        repo.obtenerDependientes(uidPaciente,
            onSuccess = { _dependientes.value = it },
            onFailure = { _mensaje.value = it }
        )
    }

    fun agregarDependiente(dependiente: Dependiente) {
        if (uidPaciente.isBlank()) return
        _isLoading.value = true
        val dependienteFinal = dependiente.copy(idTitular = uidPaciente)
        repo.agregarDependiente(dependienteFinal,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Familiar agregado exitosamente"
                cargarDependientes()
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    // ==================== FLUJO DE EXPLORACIÓN (sin postas) ====================

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
        // Cargar médicos filtrados solo por especialidad
        cargarMedicos(especialidad.idEspecialidad)
    }

    fun cargarMedicos(idEspecialidad: String) {
        _isLoading.value = true
        repo.obtenerMedicosFiltrados(idEspecialidad,
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

    private val _slotsHorario = MutableLiveData<List<SlotHorario>>()
    val slotsHorario: LiveData<List<SlotHorario>> = _slotsHorario

    private val _slotSeleccionado = MutableLiveData<SlotHorario?>()
    val slotSeleccionado: LiveData<SlotHorario?> = _slotSeleccionado

    // ...

    fun seleccionarMedico(medico: PersonalSalud) {
        _medicoSeleccionado.value = medico
        _horarioSeleccionado.value = null
        _slotSeleccionado.value = null
        cargarSlots(medico.idPersonal)
    }

    fun cargarSlots(idMedico: String) {
        _isLoading.value = true
        repo.obtenerSlotsDisponibles(idMedico,
            onSuccess = { lista ->
                _isLoading.value = false
                _slotsHorario.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    /**
     * Versión con callback directo para reprogramación.
     * Evita el problema de LiveData que emite el último valor cacheado
     * al agregar un nuevo observador (stale data issue).
     */
    fun cargarSlotsParaReprogramar(idMedico: String, onResult: (List<SlotHorario>) -> Unit) {
        _isLoading.value = true
        repo.obtenerSlotsDisponibles(idMedico,
            onSuccess = { lista ->
                _isLoading.value = false
                onResult(lista)
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
                onResult(emptyList())
            }
        )
    }

    fun seleccionarSlot(slot: SlotHorario) {
        _slotSeleccionado.value = slot
    }

    // ==================== RESERVA CON PAGO (RF04) ====================

    fun confirmarReserva(motivo: String, montoPago: Double, metodoPago: String, referenciaPago: String,
                         pacienteRealNombre: String? = null, pacienteRealDni: String? = null) {
        val paciente = _datosPaciente.value
        val medico = _medicoSeleccionado.value
        val especialidad = _especialidadSeleccionada.value
        val slot = _slotSeleccionado.value

        if (paciente == null || medico == null ||
            especialidad == null || slot == null) {
            _mensaje.value = "Error: faltan datos para completar la reserva"
            return
        }

        _isLoading.value = true
        repo.confirmarReservaSlot(
            slotSeleccionado = slot,
            motivo = motivo,
            paciente = paciente,
            medico = medico,
            especialidad = especialidad,
            montoPago = montoPago,
            metodoPago = metodoPago,
            referenciaPago = referenciaPago,
            pacienteRealNombre = pacienteRealNombre ?: "${paciente.nombres} ${paciente.apellidos}",
            pacienteRealDni = pacienteRealDni ?: paciente.dni,
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
                
                // Buscar cita de hoy (solo Reservada o Atendida)
                val hoy = todasLasCitas.find { 
                    it.fecha == fechaHoy && 
                    (it.estadoCita == Constants.ESTADO_CITA_CONFIRMADA || it.estadoCita == Constants.ESTADO_CITA_ATENDIDA)
                }
                _citaHoy.value = hoy

                // Separar en próximas (Reservada con fecha >= hoy) y pasadas
                _citasProximas.value = todasLasCitas.filter {
                    it.estadoCita == Constants.ESTADO_CITA_CONFIRMADA &&
                    compararFechas(it.fecha, fechaHoy) >= 0
                }.sortedBy { it.fecha + it.hora }

                _citasPasadas.value = todasLasCitas.filter {
                    it.estadoCita != Constants.ESTADO_CITA_CONFIRMADA ||
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

    fun cancelarCita(cita: CitaMedica, motivo: String = "") {
        _isLoading.value = true
        repo.cancelarCita(cita, motivo,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Cita cancelada exitosamente. Pago reembolsado."
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

    // ==================== TRATAMIENTOS Y SESIONES ====================

    private val _misTratamientos = MutableLiveData<List<PlanTratamiento>>()
    val misTratamientos: LiveData<List<PlanTratamiento>> = _misTratamientos

    private val _sesionesTratamiento = MutableLiveData<List<SesionTratamiento>>()
    val sesionesTratamiento: LiveData<List<SesionTratamiento>> = _sesionesTratamiento

    private val _sesionAgendada = MutableLiveData<Boolean>()
    val sesionAgendada: LiveData<Boolean> = _sesionAgendada

    fun cargarMisTratamientos() {
        if (uidPaciente.isBlank()) return
        _isLoading.value = true
        repo.obtenerMisTratamientos(uidPaciente,
            onSuccess = { lista ->
                _isLoading.value = false
                _misTratamientos.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun cargarSesiones(idPlan: String) {
        _isLoading.value = true
        repo.obtenerSesionesDeTratamiento(idPlan,
            onSuccess = { lista ->
                _isLoading.value = false
                _sesionesTratamiento.value = lista
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun agendarSesion(
        sesion: SesionTratamiento,
        slot: SlotHorario,
        montoPago: Double,
        metodoPago: String,
        referenciaPago: String
    ) {
        _isLoading.value = true
        repo.agendarSesion(sesion, slot, montoPago, metodoPago, referenciaPago,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "¡Sesión agendada y pagada exitosamente!"
                _sesionAgendada.value = true
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun sesionAgendadaCompletada() { _sesionAgendada.value = false }


    // ==================== REPROGRAMACION DE CITAS ====================

    private val _reprogramacionExitosa = MutableLiveData<Boolean>()
    val reprogramacionExitosa: LiveData<Boolean> = _reprogramacionExitosa

    fun reprogramarCita(cita: CitaMedica, nuevoSlot: SlotHorario) {
        _isLoading.value = true
        repo.reprogramarCita(cita, nuevoSlot,
            onSuccess = {
                _isLoading.value = false
                _mensaje.value = "Cita reprogramada al ${nuevoSlot.fecha} a las ${nuevoSlot.horaInicio}"
                _reprogramacionExitosa.value = true
            },
            onFailure = { msg ->
                _isLoading.value = false
                _mensaje.value = msg
            }
        )
    }

    fun reprogramacionCompletada() { _reprogramacionExitosa.value = false }

    override fun onCleared() {
        super.onCleared()
        listenerCitas?.remove()
    }
}

