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

        // Paso 3: Horarios (Slots)
        viewModel.slotsHorario.observe(this) { lista ->
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
                rv.adapter = SlotHorarioSelectionAdapter(lista) { slot, position ->
                    horaSeleccionada = slot.horaInicio
                    horarioIdxSeleccionado = position
                    viewModel.seleccionarSlot(slot)
                    mostrarResumen(slot)
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

    private fun mostrarResumen(slot: com.ayacucho.medicitas.model.SlotHorario) {
        val cardResumen = findViewById<MaterialCardView>(R.id.cardResumen)
        val tilMotivo = findViewById<TextInputLayout>(R.id.tilMotivo)
        val btnConfirmar = findViewById<MaterialButton>(R.id.btnConfirmar)
        val llSeleccionPaciente = findViewById<View>(R.id.llSeleccionPaciente)
        val spinnerPacientes = findViewById<android.widget.Spinner>(R.id.spinnerPacientes)

        val especialidad = viewModel.especialidadSeleccionada.value
        val medico = viewModel.medicoSeleccionado.value
        val paciente = viewModel.datosPaciente.value

        if (especialidad != null && medico != null && paciente != null) {
            findViewById<TextView>(R.id.tvResumenEspecialidad).text =
                "Especialidad: ${especialidad.nombreEspecialidad}"
            findViewById<TextView>(R.id.tvResumenMedico).text =
                "Médico: ${medico.nombres} ${medico.apellidos}"
            findViewById<TextView>(R.id.tvResumenHorario).text =
                "Fecha: ${slot.fecha} de ${slot.horaInicio} a ${slot.horaFin}"
            findViewById<TextView>(R.id.tvResumenPrecio).text =
                "S/. ${"%.2f".format(precioConsulta)}"

            cardResumen.visibility = View.VISIBLE
            tilMotivo.visibility = View.VISIBLE
            llSeleccionPaciente.visibility = View.VISIBLE
            btnConfirmar.visibility = View.VISIBLE
            btnConfirmar.text = "Pagar S/. ${"%.2f".format(precioConsulta)}"

            // Poblar spinner con titular + dependientes
            viewModel.cargarDependientes()
            viewModel.dependientes.observe(this) { dependientes ->
                val opciones = mutableListOf("Para mí (${paciente.nombres} ${paciente.apellidos})")
                opciones.addAll(dependientes.map { "Para ${it.nombres} ${it.apellidos} (${it.relacion})" })
                
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, opciones)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPacientes.adapter = adapter
            }
        }
    }

    // ==================== SIMULACIÓN DE PAGO ====================

    private fun mostrarDialogPago() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_simulacion_pago, null)
        val tvMontoPagar = dialogView.findViewById<TextView>(R.id.tvMontoPagar)
        val toggleMetodo = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleMetodoPago)
        val layoutTarjeta = dialogView.findViewById<View>(R.id.layoutTarjeta)
        val layoutQR = dialogView.findViewById<View>(R.id.layoutQR)
        val ivQR = dialogView.findViewById<android.widget.ImageView>(R.id.ivQR)
        val tvInstruccionQR = dialogView.findViewById<TextView>(R.id.tvInstruccionQR)
        val btnPagar = dialogView.findViewById<MaterialButton>(R.id.btnPagar)
        val progressPago = dialogView.findViewById<ProgressBar>(R.id.progressPago)
        val tvEstadoProceso = dialogView.findViewById<TextView>(R.id.tvEstadoProceso)

        tvMontoPagar.text = "S/. ${"%.2f".format(precioConsulta)}"

        // Selección de método
        toggleMetodo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnMetodoTarjeta -> {
                        layoutTarjeta.visibility = View.VISIBLE
                        layoutQR.visibility = View.GONE
                    }
                    R.id.btnMetodoYape -> {
                        layoutTarjeta.visibility = View.GONE
                        layoutQR.visibility = View.VISIBLE
                        tvInstruccionQR.text = "Escanea este QR con tu app Yape"
                        // Simular QR de Yape
                        ivQR.setImageResource(android.R.drawable.ic_menu_view)
                        ivQR.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_purple))
                    }
                    R.id.btnMetodoPlin -> {
                        layoutTarjeta.visibility = View.GONE
                        layoutQR.visibility = View.VISIBLE
                        tvInstruccionQR.text = "Escanea este QR con tu app Plin"
                        ivQR.setImageResource(android.R.drawable.ic_menu_view)
                        ivQR.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                    }
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnPagar.setOnClickListener {
            val metodoSeleccionado = when (toggleMetodo.checkedButtonId) {
                R.id.btnMetodoYape -> com.ayacucho.medicitas.utils.Constants.METODO_PAGO_YAPE
                R.id.btnMetodoPlin -> com.ayacucho.medicitas.utils.Constants.METODO_PAGO_PLIN
                else -> com.ayacucho.medicitas.utils.Constants.METODO_PAGO_TARJETA
            }

            if (metodoSeleccionado == com.ayacucho.medicitas.utils.Constants.METODO_PAGO_TARJETA) {
                val etNumero = dialogView.findViewById<TextInputEditText>(R.id.etNumeroTarjeta)
                if (etNumero.text.toString().length < 16) {
                    etNumero.error = "Número de tarjeta inválido"
                    return@setOnClickListener
                }
            }

            btnPagar.isEnabled = false
            progressPago.visibility = View.VISIBLE
            tvEstadoProceso.visibility = View.VISIBLE
            tvEstadoProceso.text = "Procesando $metodoSeleccionado..."

            Handler(Looper.getMainLooper()).postDelayed({
                tvEstadoProceso.text = "Confirmando transacción..."
                progressPago.visibility = View.GONE
                tvEstadoProceso.text = "✅ ¡Pago aprobado!"
                tvEstadoProceso.setTextColor(ContextCompat.getColor(this, R.color.primary))

                val referenciaPago = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
                val motivo = findViewById<TextInputEditText>(R.id.etMotivo).text.toString()

                val spinnerPacientes = findViewById<android.widget.Spinner>(R.id.spinnerPacientes)
                val posicionSeleccionada = spinnerPacientes.selectedItemPosition
                
                var pacienteRealNombre: String? = null
                var pacienteRealDni: String? = null

                if (posicionSeleccionada > 0) {
                    val dependientes = viewModel.dependientes.value
                    if (dependientes != null && posicionSeleccionada - 1 < dependientes.size) {
                        val dependiente = dependientes[posicionSeleccionada - 1]
                        pacienteRealNombre = "${dependiente.nombres} ${dependiente.apellidos}"
                        pacienteRealDni = dependiente.dni
                    }
                }

                viewModel.confirmarReserva(
                    motivo = motivo,
                    montoPago = precioConsulta,
                    metodoPago = metodoSeleccionado,
                    referenciaPago = referenciaPago,
                    pacienteRealNombre = pacienteRealNombre,
                    pacienteRealDni = pacienteRealDni
                )

                Handler(Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                }, 800)
            }, 2000)
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

// ==================== Adaptador de Slots de Horario ====================

class SlotHorarioSelectionAdapter(
    private val slots: List<com.ayacucho.medicitas.model.SlotHorario>,
    private val onSelect: (com.ayacucho.medicitas.model.SlotHorario, Int) -> Unit
) : RecyclerView.Adapter<SlotHorarioSelectionAdapter.ViewHolder>() {

    private var selectedPosition = -1

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
        holder.tvPrimary.text = "${slot.fecha} — ${slot.horaInicio} a ${slot.horaFin}"
        holder.tvSecondary.text = "Estado: ${slot.estado}"
        holder.tvSecondary.visibility = View.VISIBLE

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
            onSelect(slot, selectedPosition)
        }
    }

    override fun getItemCount() = slots.size
}

