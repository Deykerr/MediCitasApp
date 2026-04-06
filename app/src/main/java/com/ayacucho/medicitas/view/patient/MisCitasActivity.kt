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
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

/**
 * Historial de Citas del Paciente (RF04.5).
 * Tabs: "Próximas" y "Pasadas".
 * Permite cancelar citas próximas (RF04.6).
 */
class MisCitasActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel
    private lateinit var rvCitas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVacio: TextView
    private lateinit var tabLayout: TabLayout

    private val listaActual = mutableListOf<CitaMedica>()
    private var mostraPendientes = true // Tab activo

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
        rvCitas.adapter = CitasPacienteAdapter(listaActual, mostraPendientes) { cita ->
            AlertDialog.Builder(this)
                .setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de cancelar tu cita del ${cita.fecha} a las ${cita.hora}?")
                .setPositiveButton("Sí, cancelar") { _, _ -> viewModel.cancelarCita(cita) }
                .setNegativeButton("No", null).show()
        }

        // Tabs
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
    }

    private fun actualizarLista() {
        listaActual.clear()
        val nuevaLista = if (mostraPendientes) {
            viewModel.citasProximas.value ?: emptyList()
        } else {
            viewModel.citasPasadas.value ?: emptyList()
        }
        listaActual.addAll(nuevaLista)

        // Necesitamos re-crear el adapter para cambiar el flag mostraPendientes
        rvCitas.adapter = CitasPacienteAdapter(listaActual, mostraPendientes) { cita ->
            AlertDialog.Builder(this)
                .setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de cancelar tu cita del ${cita.fecha} a las ${cita.hora}?")
                .setPositiveButton("Sí, cancelar") { _, _ -> viewModel.cancelarCita(cita) }
                .setNegativeButton("No", null).show()
        }

        tvVacio.visibility = if (listaActual.isEmpty()) View.VISIBLE else View.GONE
        rvCitas.visibility = if (listaActual.isEmpty()) View.GONE else View.VISIBLE
        tvVacio.text = if (mostraPendientes) "No tienes citas próximas" else "No tienes citas pasadas"
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
    }
}

// ==================== Adaptador de Citas del Paciente ====================

class CitasPacienteAdapter(
    private val citas: List<CitaMedica>,
    private val mostrarCancelar: Boolean,
    private val onCancelar: (CitaMedica) -> Unit
) : RecyclerView.Adapter<CitasPacienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaCita)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoCita)
        val tvHora: TextView = view.findViewById(R.id.tvHoraCita)
        val tvEspecialidad: TextView = view.findViewById(R.id.tvEspecialidadCita)
        val tvMedico: TextView = view.findViewById(R.id.tvMedicoCita)
        val tvPosta: TextView = view.findViewById(R.id.tvPostaCita)
        val tvMotivo: TextView = view.findViewById(R.id.tvMotivoCita)
        val btnCancelar: MaterialButton = view.findViewById(R.id.btnCancelar)
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
        holder.tvPosta.text = "📍 ${cita.nombrePosta}"
        holder.tvMotivo.text = if (cita.motivoConsulta.isBlank()) "" else "Motivo: ${cita.motivoConsulta}"

        // Badge de estado
        configurarEstado(holder.tvEstado, cita.estadoCita)

        // Solo mostrar cancelar en citas próximas con estado Reservada
        holder.btnCancelar.visibility = if (mostrarCancelar &&
            cita.estadoCita == Constants.ESTADO_CITA_RESERVADA) View.VISIBLE else View.GONE
        holder.btnCancelar.setOnClickListener { onCancelar(cita) }
    }

    private fun configurarEstado(tvEstado: TextView, estado: String) {
        tvEstado.text = estado
        val (textColor, bgColor) = when (estado) {
            Constants.ESTADO_CITA_RESERVADA -> Pair("#1565C0", "#E3F2FD")
            Constants.ESTADO_CITA_ATENDIDA -> Pair("#2E7D32", "#E8F5E9")
            Constants.ESTADO_CITA_NO_ASISTIO -> Pair("#E65100", "#FFF3E0")
            Constants.ESTADO_CITA_CANCELADA -> Pair("#C62828", "#FFEBEE")
            else -> Pair("#757575", "#F5F5F5")
        }
        tvEstado.setTextColor(Color.parseColor(textColor))
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor(bgColor))
        bg.cornerRadius = 16f
        tvEstado.background = bg
    }

    override fun getItemCount() = citas.size
}
