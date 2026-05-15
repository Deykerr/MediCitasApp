package com.ayacucho.medicitas.view.doctor

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.ayacucho.medicitas.view.auth.LoginActivity
import com.ayacucho.medicitas.viewmodel.MedicoViewModel
import com.google.android.material.button.MaterialButton
import java.util.Calendar

/**
 * Pantalla principal del Médico.
 * Muestra la agenda diaria con citas en tiempo real y permite gestionar estados.
 *
 * RF05.1: Visualizar agenda diaria.
 * RF05.2: Datos del paciente visibles en cada tarjeta.
 * RF05.3: Marcar citas como Atendido, No Asistió o Cancelado.
 */
class MedicoHomeActivity : AppCompatActivity() {

    private lateinit var viewModel: MedicoViewModel

    // Vistas
    private lateinit var tvSaludo: TextView
    private lateinit var tvEspecialidad: TextView
    private lateinit var tvTotalCitas: TextView
    private lateinit var tvPendientes: TextView
    private lateinit var tvAtendidas: TextView
    private lateinit var btnFecha: MaterialButton
    private lateinit var rvCitas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinCitas: TextView

    private val listaCitas = mutableListOf<CitaMedica>()
    private lateinit var citasAdapter: CitasMedicoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medico_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[MedicoViewModel::class.java]

        inicializarVistas()
        configurarListeners()
        observarEstados()

        // Cargar datos iniciales
        viewModel.cargarDatosMedico()
        viewModel.escucharAgendaDelDia()
    }

    private fun inicializarVistas() {
        tvSaludo = findViewById(R.id.tvSaludo)
        tvEspecialidad = findViewById(R.id.tvEspecialidad)
        tvTotalCitas = findViewById(R.id.tvTotalCitas)
        tvPendientes = findViewById(R.id.tvPendientes)
        tvAtendidas = findViewById(R.id.tvAtendidas)
        btnFecha = findViewById(R.id.btnFecha)
        rvCitas = findViewById(R.id.rvCitas)
        progressBar = findViewById(R.id.progressBar)
        tvSinCitas = findViewById(R.id.tvSinCitas)

        // Configurar RecyclerView
        citasAdapter = CitasMedicoAdapter(listaCitas,
            onAtendida = { cita ->
                AlertDialog.Builder(this)
                    .setTitle("Confirmar")
                    .setMessage("¿Marcar cita de ${cita.nombrePaciente} como ATENDIDA?")
                    .setPositiveButton("Sí") { _, _ -> viewModel.marcarComoAtendida(cita.idCita) }
                    .setNegativeButton("No", null).show()
            },
            onNoAsistio = { cita ->
                AlertDialog.Builder(this)
                    .setTitle("Confirmar")
                    .setMessage("¿Marcar que ${cita.nombrePaciente} NO ASISTIÓ?")
                    .setPositiveButton("Sí") { _, _ -> viewModel.marcarComoNoAsistio(cita.idCita) }
                    .setNegativeButton("No", null).show()
            },
            onCancelar = { cita ->
                AlertDialog.Builder(this)
                    .setTitle("Confirmar")
                    .setMessage("¿CANCELAR la cita de ${cita.nombrePaciente}?")
                    .setPositiveButton("Sí") { _, _ -> viewModel.marcarComoCancelada(cita.idCita) }
                    .setNegativeButton("No", null).show()
            },
            onCrearTratamiento = { cita ->
                mostrarDialogTratamiento(cita)
            }
        )

        rvCitas.layoutManager = LinearLayoutManager(this)
        rvCitas.adapter = citasAdapter

        // Mostrar fecha de hoy en el botón
        btnFecha.text = DateUtils.fechaActual()
    }

    /**
     * Muestra el diálogo para crear un plan de tratamiento por sesiones.
     */
    private fun mostrarDialogTratamiento(cita: CitaMedica) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plan_tratamiento, null)
        val etDiagnostico = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDiagnostico)
        val etDescripcion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescripcionTratamiento)
        val etSesiones = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTotalSesiones)
        val etPrecio = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPrecioPorSesion)
        val tvResumen = dialogView.findViewById<TextView>(R.id.tvResumenTratamiento)

        // Calcular costo total dinámicamente
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val sesiones = etSesiones.text.toString().toIntOrNull() ?: 0
                val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
                val total = sesiones * precio
                tvResumen.text = "Costo total estimado: S/. ${"%.2f".format(total)} ($sesiones sesiones × S/. ${"%.2f".format(precio)})"
            }
        }
        etSesiones.addTextChangedListener(watcher)
        etPrecio.addTextChangedListener(watcher)

        AlertDialog.Builder(this)
            .setTitle("Plan de Tratamiento — ${cita.nombrePaciente}")
            .setView(dialogView)
            .setPositiveButton("Crear Plan") { _, _ ->
                val diagnostico = etDiagnostico.text.toString()
                val descripcion = etDescripcion.text.toString()
                val sesiones = etSesiones.text.toString().toIntOrNull() ?: 0
                val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0

                viewModel.crearPlanTratamiento(cita, diagnostico, descripcion, sesiones, precio)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarListeners() {
        // Selector de fecha (DatePicker)
        btnFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val fecha = String.format("%02d/%02d/%04d", day, month + 1, year)
                btnFecha.text = fecha
                viewModel.cambiarFechaAgenda(fecha)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Ver mis horarios
        findViewById<ImageButton>(R.id.btnMisHorarios).setOnClickListener {
            startActivity(Intent(this, MisHorariosActivity::class.java))
        }

        // Cerrar sesión
        findViewById<ImageButton>(R.id.btnCerrarSesion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage(getString(R.string.msg_cerrar_sesion))
                .setPositiveButton("Sí") { _, _ ->
                    viewModel.cerrarSesion()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("No", null).show()
        }
    }

    private fun observarEstados() {
        // Datos del médico
        viewModel.datosMedico.observe(this) { medico ->
            tvSaludo.text = "Dr. ${medico.nombres} ${medico.apellidos}"
            tvEspecialidad.text = medico.nombreEspecialidad
        }

        // Loading
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Mensajes
        viewModel.mensaje.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
        }

        // Citas del día (tiempo real)
        viewModel.citasDelDia.observe(this) { citas ->
            listaCitas.clear()
            listaCitas.addAll(citas)
            citasAdapter.notifyDataSetChanged()

            tvSinCitas.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
            rvCitas.visibility = if (citas.isEmpty()) View.GONE else View.VISIBLE
        }

        // Estadísticas
        viewModel.totalCitas.observe(this) { tvTotalCitas.text = it.toString() }
        viewModel.citasPendientes.observe(this) { tvPendientes.text = it.toString() }
        viewModel.citasAtendidas.observe(this) { tvAtendidas.text = it.toString() }
    }
}

// ==================== Adaptador de Citas del Médico ====================

class CitasMedicoAdapter(
    private val citas: List<CitaMedica>,
    private val onAtendida: (CitaMedica) -> Unit,
    private val onNoAsistio: (CitaMedica) -> Unit,
    private val onCancelar: (CitaMedica) -> Unit,
    private val onCrearTratamiento: (CitaMedica) -> Unit
) : RecyclerView.Adapter<CitasMedicoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora: TextView = view.findViewById(R.id.tvHoraCita)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoCita)
        val tvNombre: TextView = view.findViewById(R.id.tvNombrePaciente)
        val tvDni: TextView = view.findViewById(R.id.tvDniPaciente)
        val tvMotivo: TextView = view.findViewById(R.id.tvMotivo)
        val btnAtendida: MaterialButton = view.findViewById(R.id.btnAtendida)
        val btnNoAsistio: MaterialButton = view.findViewById(R.id.btnNoAsistio)
        val btnCancelar: MaterialButton = view.findViewById(R.id.btnCancelar)
        val layoutAcciones: View = view.findViewById(R.id.layoutAcciones)
        val dividerAcciones: View = view.findViewById(R.id.dividerAcciones)
        val btnCrearTratamiento: MaterialButton = view.findViewById(R.id.btnCrearTratamiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita_medico, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cita = citas[position]

        holder.tvHora.text = cita.hora
        holder.tvNombre.text = cita.nombrePaciente.ifBlank { "Paciente sin nombre" }
        holder.tvDni.text = "DNI: ${cita.dniPaciente}"
        holder.tvMotivo.text = if (cita.motivoConsulta.isBlank()) "Sin motivo especificado"
                               else "Motivo: ${cita.motivoConsulta}"

        // Configurar badge de estado con colores
        configurarEstado(holder.tvEstado, cita.estadoCita)

        // Mostrar/ocultar botones según el estado actual
        val esPendiente = cita.estadoCita == Constants.ESTADO_CITA_RESERVADA
        holder.layoutAcciones.visibility = if (esPendiente) View.VISIBLE else View.GONE
        holder.dividerAcciones.visibility = if (esPendiente) View.VISIBLE else View.GONE

        // Botón crear tratamiento: solo visible en citas Atendidas
        val esAtendida = cita.estadoCita == Constants.ESTADO_CITA_ATENDIDA
        holder.btnCrearTratamiento.visibility = if (esAtendida) View.VISIBLE else View.GONE

        // Listeners de los botones
        holder.btnAtendida.setOnClickListener { onAtendida(cita) }
        holder.btnNoAsistio.setOnClickListener { onNoAsistio(cita) }
        holder.btnCancelar.setOnClickListener { onCancelar(cita) }
        holder.btnCrearTratamiento.setOnClickListener { onCrearTratamiento(cita) }
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

