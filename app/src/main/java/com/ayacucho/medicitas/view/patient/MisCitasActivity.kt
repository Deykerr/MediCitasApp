package com.ayacucho.medicitas.view.patient

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.model.SlotHorario
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

/**
 * Historial de Citas del Paciente (RF04.5).
 * Tabs: "Próximas" y "Pasadas".
 * Permite cancelar (RF04.6) y reprogramar (RF04.7) citas próximas.
 */
class MisCitasActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel
    private lateinit var rvCitas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVacio: TextView
    private lateinit var tabLayout: TabLayout

    private val listaActual = mutableListOf<CitaMedica>()
    private var mostraPendientes = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_citas)

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvCitas = findViewById(R.id.rvCitas)
        progressBar = findViewById(R.id.progressBar)
        tvVacio = findViewById(R.id.tvVacio)
        tabLayout = findViewById(R.id.tabLayout)

        rvCitas.layoutManager = LinearLayoutManager(this)
        actualizarAdapter()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mostraPendientes = tab?.position == 0
                actualizarLista()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        observarEstados()
        viewModel.cargarMisCitas()
        viewModel.cargarDatosPaciente()
    }

    private fun actualizarAdapter() {
        rvCitas.adapter = CitasPacienteAdapter(
            citas = listaActual,
            mostrarAcciones = mostraPendientes,
            onCancelar = { cita -> mostrarDialogoCancelacion(cita) },
            onReprogramar = { cita -> mostrarDialogoReprogramacion(cita) }
        )
    }

    private fun actualizarLista() {
        listaActual.clear()
        val nuevaLista = if (mostraPendientes) {
            viewModel.citasProximas.value ?: emptyList()
        } else {
            viewModel.citasPasadas.value ?: emptyList()
        }
        listaActual.addAll(nuevaLista)
        actualizarAdapter()

        tvVacio.visibility = if (listaActual.isEmpty()) View.VISIBLE else View.GONE
        rvCitas.visibility = if (listaActual.isEmpty()) View.GONE else View.VISIBLE
        tvVacio.text = if (mostraPendientes) "No tienes citas próximas" else "No tienes citas pasadas"
    }

    private fun mostrarDialogoCancelacion(cita: CitaMedica) {
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.hint = "Motivo de cancelación (opcional)"

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 24)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Cancelar Cita")
            .setMessage("¿Estás seguro de cancelar tu cita del ${cita.fecha} a las ${cita.hora}?\n\nSe realizará el reembolso de S/. ${"%.2f".format(cita.montoPago)}")
            .setView(container)
            .setPositiveButton("Sí, cancelar") { _, _ ->
                val motivo = input.text.toString()
                viewModel.cancelarCita(cita, motivo)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun mostrarDialogoReprogramacion(cita: CitaMedica) {
        // Usamos callback directo en lugar de LiveData observe() para evitar
        // que LiveData emita el último valor cacheado antes de que lleguen los nuevos slots
        viewModel.cargarSlotsParaReprogramar(cita.idPersonal) { slots ->
            if (slots.isEmpty()) {
                Toast.makeText(
                    this,
                    "No hay slots disponibles para reprogramar con este médico",
                    Toast.LENGTH_SHORT
                ).show()
                return@cargarSlotsParaReprogramar
            }

            val items = slots.map { "📅 ${it.fecha}  🕐 ${it.horaInicio}" }.toTypedArray()
            var selectedIndex = 0

            AlertDialog.Builder(this)
                .setTitle("Reprogramar\nActual: ${cita.fecha} — ${cita.hora}")
                .setSingleChoiceItems(items, 0) { _, which -> selectedIndex = which }
                .setPositiveButton("Confirmar") { _, _ ->
                    val nuevoSlot = slots[selectedIndex]
                    viewModel.reprogramarCita(cita, nuevoSlot)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

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
        viewModel.citasProximas.observe(this) { if (mostraPendientes) actualizarLista() }
        viewModel.citasPasadas.observe(this) { if (!mostraPendientes) actualizarLista() }
        viewModel.reprogramacionExitosa.observe(this) { exitosa ->
            if (exitosa == true) {
                viewModel.reprogramacionCompletada()
                viewModel.cargarMisCitas() // Recargar lista tras reprogramar
            }
        }
    }
}

// ==================== Adaptador de Citas del Paciente ====================

class CitasPacienteAdapter(
    private val citas: List<CitaMedica>,
    private val mostrarAcciones: Boolean,
    private val onCancelar: (CitaMedica) -> Unit,
    private val onReprogramar: (CitaMedica) -> Unit
) : RecyclerView.Adapter<CitasPacienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaCita)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoCita)
        val tvHora: TextView = view.findViewById(R.id.tvHoraCita)
        val tvEspecialidad: TextView = view.findViewById(R.id.tvEspecialidadCita)
        val tvMedico: TextView = view.findViewById(R.id.tvMedicoCita)
        val tvMontoPago: TextView = view.findViewById(R.id.tvMontoPago)
        val tvEstadoPago: TextView = view.findViewById(R.id.tvEstadoPago)
        val tvMotivo: TextView = view.findViewById(R.id.tvMotivoCita)
        val btnCancelar: MaterialButton = view.findViewById(R.id.btnCancelar)
        val btnReprogramar: MaterialButton = view.findViewById(R.id.btnReprogramar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita_paciente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cita = citas[position]

        holder.tvFecha.text = cita.fecha
        holder.tvHora.text = "🕐 ${cita.hora}"
        holder.tvEspecialidad.text = cita.nombreEspecialidad
        holder.tvMedico.text = "Dr. ${cita.nombreMedico}"
        holder.tvMotivo.text = if (cita.motivoConsulta.isBlank()) "" else "Motivo: ${cita.motivoConsulta}"

        holder.tvMontoPago.text = if (cita.montoPago > 0) {
            "S/. ${"%.2f".format(cita.montoPago)} (${cita.metodoPago})"
        } else { "" }

        configurarEstado(holder.tvEstado, cita.estadoCita)
        configurarEstadoPago(holder.tvEstadoPago, cita.estadoPago)

        // Mostrar botones solo en citas próximas con estado activo
        val esActiva = cita.estadoCita == Constants.ESTADO_CITA_PENDIENTE ||
                       cita.estadoCita == Constants.ESTADO_CITA_CONFIRMADA

        holder.btnReprogramar.visibility =
            if (mostrarAcciones && esActiva) View.VISIBLE else View.GONE
        holder.btnCancelar.visibility =
            if (mostrarAcciones && esActiva) View.VISIBLE else View.GONE

        holder.btnReprogramar.setOnClickListener { onReprogramar(cita) }
        holder.btnCancelar.setOnClickListener { onCancelar(cita) }
    }

    private fun configurarEstado(tvEstado: TextView, estado: String) {
        tvEstado.text = estado
        val (textColor, bgColor) = when (estado) {
            Constants.ESTADO_CITA_CONFIRMADA  -> Pair("#1565C0", "#E3F2FD")
            Constants.ESTADO_CITA_REPROGRAMADA -> Pair("#6A1B9A", "#F3E5F5")
            Constants.ESTADO_CITA_ATENDIDA    -> Pair("#2E7D32", "#E8F5E9")
            Constants.ESTADO_CITA_NO_ASISTIO  -> Pair("#E65100", "#FFF3E0")
            Constants.ESTADO_CITA_CANCELADA   -> Pair("#C62828", "#FFEBEE")
            else -> Pair("#757575", "#F5F5F5")
        }
        tvEstado.setTextColor(Color.parseColor(textColor))
        val bg = GradientDrawable(); bg.setColor(Color.parseColor(bgColor)); bg.cornerRadius = 16f
        tvEstado.background = bg
    }

    private fun configurarEstadoPago(tvEstado: TextView, estado: String) {
        tvEstado.text = estado
        val (textColor, bgColor) = when (estado) {
            Constants.ESTADO_PAGO_PAGADO      -> Pair("#2E7D32", "#E8F5E9")
            Constants.ESTADO_PAGO_PENDIENTE   -> Pair("#E65100", "#FFF3E0")
            Constants.ESTADO_PAGO_REEMBOLSADO -> Pair("#6A1B9A", "#F3E5F5")
            else -> Pair("#757575", "#F5F5F5")
        }
        tvEstado.setTextColor(Color.parseColor(textColor))
        val bg = GradientDrawable(); bg.setColor(Color.parseColor(bgColor)); bg.cornerRadius = 16f
        tvEstado.background = bg
    }

    override fun getItemCount() = citas.size
}
