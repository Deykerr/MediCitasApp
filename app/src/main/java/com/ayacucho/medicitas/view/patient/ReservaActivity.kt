package com.ayacucho.medicitas.view.patient

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.Especialidad
import com.ayacucho.medicitas.model.Horario
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.UUID

/**
 * Pantalla de Reserva de Cita Médica.
 * Flujo de 3 pasos adaptado para clínica privada:
 *   Paso 1: Seleccionar Especialidad
 *   Paso 2: Seleccionar Médico
 *   Paso 3: Seleccionar Horario + Ver Resumen + Proceder al Pago
 *
 * RF02: Exploración de especialidades
 * RF03: Búsqueda de médicos y horarios disponibles
 * RF04: Reserva y confirmación con pago
 */
class ReservaActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var progressBar: ProgressBar

    // Indicadores de paso
    private lateinit var tvPaso1: TextView
    private lateinit var tvPaso2: TextView
    private lateinit var tvPaso3: TextView
    private lateinit var lineaPaso1: View
    private lateinit var lineaPaso2: View
    private lateinit var tvTituloPaso: TextView

    // Paso 3: Horarios + Confirmar
    private var horaSeleccionada: String = ""
    private var horarioIdxSeleccionado: Int = -1

    // Precio actual
    private var precioConsulta: Double = 0.0

    private val titulosPasos = arrayOf(
        "Selecciona una Especialidad",
        "Selecciona un Médico",
        "Selecciona Horario y Confirma"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserva)

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Indicadores de paso
        tvPaso1 = findViewById(R.id.tvPaso1)
        tvPaso2 = findViewById(R.id.tvPaso2)
        tvPaso3 = findViewById(R.id.tvPaso3)
        lineaPaso1 = findViewById(R.id.lineaPaso1)
        lineaPaso2 = findViewById(R.id.lineaPaso2)
        tvTituloPaso = findViewById(R.id.tvTituloPaso)

        progressBar = findViewById(R.id.progressBar)
        viewFlipper = findViewById(R.id.viewFlipper)

        configurarRecyclers()
        observarEstados()

        // Cargar datos del paciente y especialidades
        viewModel.cargarDatosPaciente()
        viewModel.cargarEspecialidades()
        actualizarIndicadorPaso(0)
    }

    // ==================== CONFIGURAR RECYCLERS ====================

    private fun configurarRecyclers() {
        // Paso 1: Especialidades
        val rvEspecialidades = findViewById<RecyclerView>(R.id.rvEspecialidades)
        rvEspecialidades.layoutManager = LinearLayoutManager(this)

        // Paso 2: Médicos
        val rvMedicos = findViewById<RecyclerView>(R.id.rvMedicos)
        rvMedicos.layoutManager = LinearLayoutManager(this)

        // Paso 3: Horarios
        val rvHorarios = findViewById<RecyclerView>(R.id.rvHorarios)
        rvHorarios.layoutManager = LinearLayoutManager(this)

        // Botón confirmar (Proceder al Pago)
        val btnConfirmar = findViewById<MaterialButton>(R.id.btnConfirmar)
        btnConfirmar.setOnClickListener {
            mostrarDialogPago()
        }
    }

    // ==================== OBSERVAR ESTADOS ====================

    private fun observarEstados() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.mensaje.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
        }

        // Paso 1: Especialidades
        viewModel.especialidades.observe(this) { lista ->
            val rv = findViewById<RecyclerView>(R.id.rvEspecialidades)
            val tvVacio = findViewById<TextView>(R.id.tvVacioPaso1)
            if (lista.isEmpty()) {
                tvVacio.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                tvVacio.visibility = View.GONE
                rv.visibility = View.VISIBLE
                rv.adapter = SimpleSelectionAdapter(
                    items = lista,
                    getText = { "${it.nombreEspecialidad}  •  S/. ${"%.2f".format(it.precioConsulta)}" },
                    getSubText = { it.descripcion },
                    onSelect = { esp ->
                        precioConsulta = esp.precioConsulta
                        viewModel.seleccionarEspecialidad(esp)
                        viewFlipper.displayedChild = 1
                        actualizarIndicadorPaso(1)
                    }
                )
            }
        }

        // Paso 2: Médicos
        viewModel.medicos.observe(this) { lista ->
            val rv = findViewById<RecyclerView>(R.id.rvMedicos)
            val tvVacio = findViewById<TextView>(R.id.tvVacioPaso2)
            if (lista.isEmpty()) {
                tvVacio.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                tvVacio.visibility = View.GONE
                rv.visibility = View.VISIBLE
                rv.adapter = SimpleSelectionAdapter(
                    items = lista,
                    getText = { "${it.nombres} ${it.apellidos}" },
                    getSubText = { it.nombreEspecialidad },
                    onSelect = { medico ->
                        viewModel.seleccionarMedico(medico)
                        viewFlipper.displayedChild = 2
                        actualizarIndicadorPaso(2)
                    }
                )
            }
        }

        // Paso 3: Horarios
        viewModel.horarios.observe(this) { lista ->
            val rv = findViewById<RecyclerView>(R.id.rvHorarios)
            val tvVacio = findViewById<TextView>(R.id.tvVacioPaso3)
            if (lista.isEmpty()) {
                tvVacio.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                tvVacio.visibility = View.GONE
                rv.visibility = View.VISIBLE
                horaSeleccionada = ""
                horarioIdxSeleccionado = -1
                rv.adapter = HorarioSelectionAdapter(lista) { horario, hora, position ->
                    horaSeleccionada = hora
                    horarioIdxSeleccionado = position
                    viewModel.seleccionarHorario(horario)
                    mostrarResumen(horario, hora)
                }
            }
        }

        // Reserva exitosa
        viewModel.reservaExitosa.observe(this) { idCita ->
            idCita?.let {
                viewModel.reservaCompletada()
                Toast.makeText(this, "¡Cita reservada y pagada exitosamente! 🎉", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // ==================== RESUMEN ====================

    private fun mostrarResumen(horario: Horario, hora: String) {
        val cardResumen = findViewById<MaterialCardView>(R.id.cardResumen)
        val tilMotivo = findViewById<TextInputLayout>(R.id.tilMotivo)
        val btnConfirmar = findViewById<MaterialButton>(R.id.btnConfirmar)

        val especialidad = viewModel.especialidadSeleccionada.value
        val medico = viewModel.medicoSeleccionado.value

        if (especialidad != null && medico != null) {
            findViewById<TextView>(R.id.tvResumenEspecialidad).text =
                "Especialidad: ${especialidad.nombreEspecialidad}"
            findViewById<TextView>(R.id.tvResumenMedico).text =
                "Médico: ${medico.nombres} ${medico.apellidos}"
            findViewById<TextView>(R.id.tvResumenHorario).text =
                "Fecha: ${horario.fecha} a las $hora"
            findViewById<TextView>(R.id.tvResumenPrecio).text =
                "S/. ${"%.2f".format(precioConsulta)}"

            cardResumen.visibility = View.VISIBLE
            tilMotivo.visibility = View.VISIBLE
            btnConfirmar.visibility = View.VISIBLE
            btnConfirmar.text = "Pagar S/. ${"%.2f".format(precioConsulta)}"
        }
    }

    // ==================== SIMULACIÓN DE PAGO ====================

    private fun mostrarDialogPago() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_simulacion_pago, null)
        val etNumero = dialogView.findViewById<TextInputEditText>(R.id.etNumeroTarjeta)
        val etExpiracion = dialogView.findViewById<TextInputEditText>(R.id.etExpiracion)
        val etCvv = dialogView.findViewById<TextInputEditText>(R.id.etCvv)
        val etTitular = dialogView.findViewById<TextInputEditText>(R.id.etTitular)
        val tvTipoTarjeta = dialogView.findViewById<TextView>(R.id.tvTipoTarjeta)
        val tvMontoPagar = dialogView.findViewById<TextView>(R.id.tvMontoPagar)
        val btnPagar = dialogView.findViewById<MaterialButton>(R.id.btnPagar)
        val progressPago = dialogView.findViewById<ProgressBar>(R.id.progressPago)
        val tvEstadoProceso = dialogView.findViewById<TextView>(R.id.tvEstadoProceso)

        tvMontoPagar.text = "S/. ${"%.2f".format(precioConsulta)}"
        btnPagar.text = "Pagar S/. ${"%.2f".format(precioConsulta)}"

        // Pre-llenar datos para facilitar pruebas
        etNumero.setText("4555123456789012")
        etExpiracion.setText("12/28")
        etCvv.setText("123")

        // Detección de tipo de tarjeta por primer dígito
        etNumero.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val numero = s.toString()
                tvTipoTarjeta.text = when {
                    numero.startsWith("4") -> "💳 Visa"
                    numero.startsWith("5") -> "💳 Mastercard"
                    numero.startsWith("3") -> "💳 American Express"
                    numero.isNotEmpty() -> "💳 Tarjeta de crédito/débito"
                    else -> "Ingrese número de tarjeta"
                }
            }
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnPagar.setOnClickListener {
            val numero = etNumero.text.toString().trim()
            val expiracion = etExpiracion.text.toString().trim()
            val cvv = etCvv.text.toString().trim()
            val titular = etTitular.text.toString().trim()

            // Validaciones
            if (numero.length < 16) {
                etNumero.error = "Ingrese 16 dígitos"
                return@setOnClickListener
            }
            if (expiracion.length < 4) {
                etExpiracion.error = "Formato MM/YY"
                return@setOnClickListener
            }
            if (cvv.length < 3) {
                etCvv.error = "3 dígitos"
                return@setOnClickListener
            }
            if (titular.isBlank()) {
                etTitular.error = "Campo requerido"
                return@setOnClickListener
            }

            // Simular procesamiento
            btnPagar.isEnabled = false
            progressPago.visibility = View.VISIBLE
            tvEstadoProceso.visibility = View.VISIBLE
            tvEstadoProceso.text = "Procesando pago..."

            Handler(Looper.getMainLooper()).postDelayed({
                tvEstadoProceso.text = "Verificando datos de la tarjeta..."
            }, 800)

            Handler(Looper.getMainLooper()).postDelayed({
                tvEstadoProceso.text = "Autorizando transacción..."
            }, 1600)

            Handler(Looper.getMainLooper()).postDelayed({
                progressPago.visibility = View.GONE
                tvEstadoProceso.text = "✅ ¡Pago aprobado!"
                tvEstadoProceso.setTextColor(ContextCompat.getColor(this, R.color.primary))

                // Determinar tipo de tarjeta
                val tipoTarjeta = when {
                    numero.startsWith("4") -> "Visa"
                    numero.startsWith("5") -> "Mastercard"
                    numero.startsWith("3") -> "AmEx"
                    else -> "Tarjeta"
                }

                // Generar referencia de pago simulada
                val referenciaPago = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"

                // Confirmar la reserva con datos de pago
                val motivo = findViewById<TextInputEditText>(R.id.etMotivo).text.toString()
                viewModel.confirmarReserva(
                    motivo = motivo,
                    horaSeleccionada = horaSeleccionada,
                    montoPago = precioConsulta,
                    metodoPago = tipoTarjeta,
                    referenciaPago = referenciaPago
                )

                Handler(Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                }, 500)
            }, 2500)
        }

        dialog.show()
    }

    // ==================== INDICADOR DE PASOS ====================

    private fun actualizarIndicadorPaso(pasoActual: Int) {
        tvTituloPaso.text = titulosPasos[pasoActual]

        val pasos = listOf(tvPaso1, tvPaso2, tvPaso3)
        val lineas = listOf(lineaPaso1, lineaPaso2)

        pasos.forEachIndexed { index, tv ->
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            if (index <= pasoActual) {
                bg.setColor(ContextCompat.getColor(this, R.color.primary))
                tv.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                bg.setColor(ContextCompat.getColor(this, R.color.text_hint))
                tv.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
            tv.background = bg
        }

        lineas.forEachIndexed { index, view ->
            view.setBackgroundColor(
                if (index < pasoActual)
                    ContextCompat.getColor(this, R.color.primary)
                else
                    ContextCompat.getColor(this, R.color.text_hint)
            )
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        val currentStep = viewFlipper.displayedChild
        if (currentStep > 0) {
            viewFlipper.displayedChild = currentStep - 1
            actualizarIndicadorPaso(currentStep - 1)
        } else {
            super.onBackPressed()
        }
    }
}

// ==================== Adaptador genérico de selección ====================

class SimpleSelectionAdapter<T>(
    private val items: List<T>,
    private val getText: (T) -> String,
    private val getSubText: (T) -> String,
    private val onSelect: (T) -> Unit
) : RecyclerView.Adapter<SimpleSelectionAdapter<T>.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPrimary: TextView = view.findViewById(R.id.tvItemPrimary)
        val tvSecondary: TextView = view.findViewById(R.id.tvItemSecondary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvPrimary.text = getText(item)
        val sub = getSubText(item)
        if (sub.isNotBlank()) {
            holder.tvSecondary.text = sub
            holder.tvSecondary.visibility = View.VISIBLE
        } else {
            holder.tvSecondary.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { onSelect(item) }
    }

    override fun getItemCount() = items.size
}

// ==================== Adaptador de Horarios con Slots Individuales ====================

/**
 * Genera y muestra los slots individuales (ej: 08:00, 08:30, 09:00) para cada horario.
 * Cada slot tiene duración fija basada en duracionCitaMinutos del Horario.
 */
class HorarioSelectionAdapter(
    horarios: List<Horario>,
    private val onSelect: (Horario, String, Int) -> Unit
) : RecyclerView.Adapter<HorarioSelectionAdapter.ViewHolder>() {

    // Data class para representar un slot individual
    data class SlotItem(
        val horario: Horario,        // Horario padre
        val horaSlot: String,        // Ej: "08:30"
        val fechaDisplay: String,    // Ej: "Lunes 15/05/2026"
        val duracion: Int            // Duración en minutos
    )

    private val slots = mutableListOf<SlotItem>()
    private var selectedPosition = -1

    init {
        // Generar todos los slots individuales a partir de cada horario
        for (horario in horarios) {
            val slotsGenerados = generarSlots(horario)
            slots.addAll(slotsGenerados)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPrimary: TextView = view.findViewById(R.id.tvItemPrimary)
        val tvSecondary: TextView = view.findViewById(R.id.tvItemSecondary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slot = slots[position]
        val horaFin = calcularHoraFin(slot.horaSlot, slot.duracion)
        holder.tvPrimary.text = "${slot.fechaDisplay} — ${slot.horaSlot} a $horaFin"
        holder.tvSecondary.text = "Duración: ${slot.duracion} min | Cupos: ${slot.horario.cuposDisponibles}"
        holder.tvSecondary.visibility = View.VISIBLE

        // Highlight seleccionado
        val ctx = holder.itemView.context
        if (position == selectedPosition) {
            (holder.itemView as? MaterialCardView)?.strokeColor =
                ContextCompat.getColor(ctx, R.color.primary)
            (holder.itemView as? MaterialCardView)?.strokeWidth = 2
        } else {
            (holder.itemView as? MaterialCardView)?.strokeWidth = 0
        }

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = holder.adapterPosition
            if (oldPos >= 0) notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            onSelect(slot.horario, slot.horaSlot, selectedPosition)
        }
    }

    override fun getItemCount() = slots.size

    /**
     * Genera los slots individuales para un horario basándose en cupos ya tomados.
     * Ejemplo: 08:00-12:00 con 30min y 2 cupos usados → genera desde 09:00 en adelante.
     */
    private fun generarSlots(horario: Horario): List<SlotItem> {
        val result = mutableListOf<SlotItem>()
        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val inicio = sdf.parse(horario.horaInicio) ?: return result
            val fin = sdf.parse(horario.horaFin) ?: return result
            val duracion = if (horario.duracionCitaMinutos > 0) horario.duracionCitaMinutos else 30

            val cuposUsados = horario.cuposTotales - horario.cuposDisponibles
            val cal = java.util.Calendar.getInstance()
            cal.time = inicio

            // Avanzar a partir de los cupos ya tomados
            cal.add(java.util.Calendar.MINUTE, cuposUsados * duracion)

            val calFin = java.util.Calendar.getInstance()
            calFin.time = fin

            // Generar los slots disponibles restantes
            var slotsGenerados = 0
            while (cal.before(calFin) && slotsGenerados < horario.cuposDisponibles) {
                val horaSlot = sdf.format(cal.time)
                result.add(SlotItem(
                    horario = horario,
                    horaSlot = horaSlot,
                    fechaDisplay = "${horario.dia} ${horario.fecha}",
                    duracion = duracion
                ))
                cal.add(java.util.Calendar.MINUTE, duracion)
                slotsGenerados++
            }
        } catch (e: Exception) {
            // Fallback: mostrar al menos un slot genérico
        }
        return result
    }

    /**
     * Calcula la hora fin de un slot sumando la duración a la hora inicio.
     */
    private fun calcularHoraFin(horaInicio: String, duracionMin: Int): String {
        return try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val inicio = sdf.parse(horaInicio) ?: return horaInicio
            val cal = java.util.Calendar.getInstance()
            cal.time = inicio
            cal.add(java.util.Calendar.MINUTE, duracionMin)
            sdf.format(cal.time)
        } catch (e: Exception) { horaInicio }
    }
}

