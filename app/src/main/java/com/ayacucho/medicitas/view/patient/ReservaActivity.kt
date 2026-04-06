package com.ayacucho.medicitas.view.patient

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.*
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Flujo de Reserva paso a paso con ViewFlipper.
 * Paso 1: Seleccionar Posta
 * Paso 2: Seleccionar Especialidad
 * Paso 3: Seleccionar Médico
 * Paso 4: Seleccionar Horario + Confirmar
 *
 * RF02, RF03, RF04
 */
class ReservaActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var tvTituloPaso: TextView
    private lateinit var progressBar: ProgressBar

    // Step indicators
    private lateinit var pasoViews: List<TextView>
    private lateinit var lineaViews: List<View>

    private var pasoActual = 0

    // Listas
    private val listaPostas = mutableListOf<PostaMedica>()
    private val listaEspecialidades = mutableListOf<Especialidad>()
    private val listaMedicos = mutableListOf<PersonalSalud>()
    private val listaHorarios = mutableListOf<Horario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserva)

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        inicializarVistas()
        configurarRecyclerViews()
        observarEstados()

        // Cargar datos del paciente y postas
        viewModel.cargarDatosPaciente()
        viewModel.cargarPostas()
    }

    private fun inicializarVistas() {
        viewFlipper = findViewById(R.id.viewFlipper)
        tvTituloPaso = findViewById(R.id.tvTituloPaso)
        progressBar = findViewById(R.id.progressBar)

        pasoViews = listOf(
            findViewById(R.id.tvPaso1), findViewById(R.id.tvPaso2),
            findViewById(R.id.tvPaso3), findViewById(R.id.tvPaso4)
        )
        lineaViews = listOf(
            findViewById(R.id.lineaPaso1), findViewById(R.id.lineaPaso2),
            findViewById(R.id.lineaPaso3)
        )

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedCustom() }

        // Botón confirmar
        findViewById<MaterialButton>(R.id.btnConfirmar).setOnClickListener {
            val motivo = findViewById<TextInputEditText>(R.id.etMotivo).text.toString()
            val horario = viewModel.horarioSeleccionado.value
            if (horario == null) {
                Toast.makeText(this, "Selecciona un horario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Confirmar Reserva")
                .setMessage("¿Confirmas tu reserva de cita médica?")
                .setPositiveButton("Confirmar") { _, _ ->
                    viewModel.confirmarReserva(motivo, horario.horaInicio)
                }
                .setNegativeButton("Cancelar", null).show()
        }

        actualizarIndicadorPasos()
    }

    private fun configurarRecyclerViews() {
        // Paso 1: Postas
        val rvPostas = findViewById<RecyclerView>(R.id.rvPostas)
        rvPostas.layoutManager = LinearLayoutManager(this)
        rvPostas.adapter = SeleccionAdapter(listaPostas,
            onBind = { holder, item ->
                holder.tvTitulo.text = item.nombrePosta
                holder.tvSubtitulo.text = "${item.direccion} - ${item.distrito}"
                holder.ivIcono.setImageResource(android.R.drawable.ic_menu_mapmode)
            },
            onClick = { item ->
                viewModel.seleccionarPosta(item)
                avanzarPaso()
            }
        )

        // Paso 2: Especialidades
        val rvEspecialidades = findViewById<RecyclerView>(R.id.rvEspecialidades)
        rvEspecialidades.layoutManager = LinearLayoutManager(this)
        rvEspecialidades.adapter = SeleccionAdapter(listaEspecialidades,
            onBind = { holder, item ->
                holder.tvTitulo.text = item.nombreEspecialidad
                holder.tvSubtitulo.text = item.descripcion.ifBlank { "Especialidad médica" }
                holder.ivIcono.setImageResource(android.R.drawable.ic_menu_info_details)
            },
            onClick = { item ->
                viewModel.seleccionarEspecialidad(item)
                avanzarPaso()
            }
        )

        // Paso 3: Médicos
        val rvMedicos = findViewById<RecyclerView>(R.id.rvMedicos)
        rvMedicos.layoutManager = LinearLayoutManager(this)
        rvMedicos.adapter = SeleccionAdapter(listaMedicos,
            onBind = { holder, item ->
                holder.tvTitulo.text = "Dr. ${item.nombres} ${item.apellidos}"
                holder.tvSubtitulo.text = item.nombreEspecialidad
                holder.ivIcono.setImageResource(android.R.drawable.ic_menu_myplaces)
            },
            onClick = { item ->
                viewModel.seleccionarMedico(item)
                avanzarPaso()
            }
        )

        // Paso 4: Horarios
        val rvHorarios = findViewById<RecyclerView>(R.id.rvHorarios)
        rvHorarios.layoutManager = LinearLayoutManager(this)
        rvHorarios.adapter = HorarioSeleccionAdapter(listaHorarios) { horario ->
            viewModel.seleccionarHorario(horario)
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

        // Paso 1: Postas
        viewModel.postas.observe(this) { lista ->
            listaPostas.clear()
            listaPostas.addAll(lista)
            findViewById<RecyclerView>(R.id.rvPostas).adapter?.notifyDataSetChanged()
            findViewById<TextView>(R.id.tvVacioPaso1).visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        // Paso 2: Especialidades
        viewModel.especialidades.observe(this) { lista ->
            listaEspecialidades.clear()
            listaEspecialidades.addAll(lista)
            findViewById<RecyclerView>(R.id.rvEspecialidades).adapter?.notifyDataSetChanged()
            findViewById<TextView>(R.id.tvVacioPaso2).visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        // Paso 3: Médicos
        viewModel.medicos.observe(this) { lista ->
            listaMedicos.clear()
            listaMedicos.addAll(lista)
            findViewById<RecyclerView>(R.id.rvMedicos).adapter?.notifyDataSetChanged()
            findViewById<TextView>(R.id.tvVacioPaso3).visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        // Paso 4: Horarios
        viewModel.horarios.observe(this) { lista ->
            listaHorarios.clear()
            listaHorarios.addAll(lista)
            findViewById<RecyclerView>(R.id.rvHorarios).adapter?.notifyDataSetChanged()
            findViewById<TextView>(R.id.tvVacioPaso4).visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        // Horario seleccionado → mostrar resumen y botón
        viewModel.horarioSeleccionado.observe(this) { horario ->
            val cardResumen = findViewById<View>(R.id.cardResumen)
            val tilMotivo = findViewById<TextInputLayout>(R.id.tilMotivo)
            val btnConfirmar = findViewById<MaterialButton>(R.id.btnConfirmar)

            if (horario != null) {
                cardResumen.visibility = View.VISIBLE
                tilMotivo.visibility = View.VISIBLE
                btnConfirmar.visibility = View.VISIBLE

                // Llenar resumen
                val posta = viewModel.postaSeleccionada.value
                val especialidad = viewModel.especialidadSeleccionada.value
                val medico = viewModel.medicoSeleccionado.value

                findViewById<TextView>(R.id.tvResumenPosta).text = "📍 ${posta?.nombrePosta ?: ""}"
                findViewById<TextView>(R.id.tvResumenEspecialidad).text =
                    "🏥 ${especialidad?.nombreEspecialidad ?: ""}"
                findViewById<TextView>(R.id.tvResumenMedico).text =
                    "👨‍⚕️ Dr. ${medico?.nombres ?: ""} ${medico?.apellidos ?: ""}"
                findViewById<TextView>(R.id.tvResumenHorario).text =
                    "📅 ${horario.dia} ${horario.fecha}  🕐 ${horario.horaInicio} - ${horario.horaFin}"
            } else {
                cardResumen.visibility = View.GONE
                tilMotivo.visibility = View.GONE
                btnConfirmar.visibility = View.GONE
            }
        }

        // Reserva exitosa → cerrar y volver al home
        viewModel.reservaExitosa.observe(this) { idCita ->
            idCita?.let {
                viewModel.reservaCompletada()

                // 1. Mostrar la notificación push localmente (RF07.1)
                val horario = viewModel.horarioSeleccionado.value
                val posta = viewModel.postaSeleccionada.value
                val medico = viewModel.medicoSeleccionado.value

                if (horario != null) {
                    val notifHelper = com.ayacucho.medicitas.utils.NotificationHelper(this)
                    val titulo = "¡Cita Confirmada!"
                    val mensaje = "Tu cita para el ${horario.fecha} a las ${horario.horaInicio} ha sido registrada con éxito."
                    notifHelper.mostrarNotificacion(titulo, mensaje, 1)

                    // 2. Programar recordatorio con WorkManager (RF07.2)
                    try {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es", "PE"))
                        val fechaCita = sdf.parse("${horario.fecha} ${horario.horaInicio}")
                        
                        if (fechaCita != null) {
                            val tiempoRestanteMilis = fechaCita.time - System.currentTimeMillis()
                            // Recordatorio 2 horas antes
                            val delayMilis = tiempoRestanteMilis - (2 * 60 * 60 * 1000)
                            
                            if (delayMilis > 0) {
                                val inputData = androidx.work.Data.Builder()
                                    .putString("titulo", "Recordatorio de Cita")
                                    .putString("mensaje", "Recuerda tu cita a las ${horario.horaInicio} en ${posta?.nombrePosta} con el Dr. ${medico?.nombres}.")
                                    .putInt("idNotificacion", 2)
                                    .build()
                                    
                                val recordatorioRequest = androidx.work.OneTimeWorkRequestBuilder<com.ayacucho.medicitas.utils.RecordatorioWorker>()
                                    .setInitialDelay(delayMilis, java.util.concurrent.TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .build()
                                    
                                androidx.work.WorkManager.getInstance(this).enqueue(recordatorioRequest)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                AlertDialog.Builder(this)
                    .setTitle("🎉 ¡Reserva Exitosa!")
                    .setMessage("Tu cita ha sido reservada correctamente.\n\nID: ${it.take(8)}...")
                    .setPositiveButton("Aceptar") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    // ==================== NAVEGACIÓN DE PASOS ====================

    private fun avanzarPaso() {
        if (pasoActual < 3) {
            pasoActual++
            viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
            viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
            viewFlipper.displayedChild = pasoActual
            actualizarIndicadorPasos()
        }
    }

    private fun retrocederPaso() {
        if (pasoActual > 0) {
            pasoActual--
            viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
            viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
            viewFlipper.displayedChild = pasoActual
            actualizarIndicadorPasos()
        }
    }

    private fun actualizarIndicadorPasos() {
        val titulos = arrayOf(
            "Selecciona una Posta", "Selecciona una Especialidad",
            "Selecciona un Médico", "Elige Horario y Confirma"
        )
        tvTituloPaso.text = titulos[pasoActual]

        for (i in pasoViews.indices) {
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            if (i <= pasoActual) {
                bg.setColor(Color.parseColor("#1565C0"))
                pasoViews[i].setTextColor(Color.WHITE)
            } else {
                bg.setColor(Color.parseColor("#E0E0E0"))
                pasoViews[i].setTextColor(Color.parseColor("#9E9E9E"))
            }
            pasoViews[i].background = bg
        }

        for (i in lineaViews.indices) {
            lineaViews[i].setBackgroundColor(
                if (i < pasoActual) Color.parseColor("#1565C0") else Color.parseColor("#E0E0E0")
            )
        }
    }

    private fun onBackPressedCustom() {
        if (pasoActual > 0) {
            retrocederPaso()
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        onBackPressedCustom()
    }
}

// ==================== ADAPTADORES ====================

/**
 * Adaptador genérico para listas de selección (Postas, Especialidades, Médicos).
 */
class SeleccionAdapter<T>(
    private val items: List<T>,
    private val onBind: (SeleccionViewHolder, T) -> Unit,
    private val onClick: (T) -> Unit
) : RecyclerView.Adapter<SeleccionAdapter.SeleccionViewHolder>() {

    class SeleccionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvSubtitulo: TextView = view.findViewById(R.id.tvSubtitulo)
        val ivIcono: ImageView = view.findViewById(R.id.ivIcono)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeleccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seleccion, parent, false)
        return SeleccionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeleccionViewHolder, position: Int) {
        val item = items[position]
        onBind(holder, item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}

/**
 * Adaptador para selección de horarios en el paso 4.
 */
class HorarioSeleccionAdapter(
    private val horarios: List<Horario>,
    private val onSeleccionar: (Horario) -> Unit
) : RecyclerView.Adapter<HorarioSeleccionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaHorario)
        val tvHoras: TextView = view.findViewById(R.id.tvHorasHorario)
        val tvCupos: TextView = view.findViewById(R.id.tvCuposHorario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horario_seleccion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val horario = horarios[position]
        holder.tvFecha.text = "${horario.dia} - ${horario.fecha}"
        holder.tvHoras.text = "${horario.horaInicio} a ${horario.horaFin}"
        holder.tvCupos.text = "${horario.cuposDisponibles} cupos disponibles"
        holder.itemView.setOnClickListener { onSeleccionar(horario) }
    }

    override fun getItemCount() = horarios.size
}
