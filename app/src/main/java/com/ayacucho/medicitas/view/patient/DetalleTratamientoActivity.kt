package com.ayacucho.medicitas.view.patient

import android.graphics.Color
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.Horario
import com.ayacucho.medicitas.model.SesionTratamiento
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

/**
 * Detalle de un plan de tratamiento.
 * Muestra info del plan, barra de progreso, lista de sesiones,
 * y permite agendar la próxima sesión pendiente.
 */
class DetalleTratamientoActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel
    private var idPlan = ""
    private var idMedico = ""
    private var precioPorSesion = 0.0
    private var proximaSesionPendiente: SesionTratamiento? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_tratamiento)

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Obtener datos del intent
        idPlan = intent.getStringExtra("idPlan") ?: ""
        idMedico = intent.getStringExtra("idMedico") ?: ""
        precioPorSesion = intent.getDoubleExtra("precioPorSesion", 0.0)

        // Mostrar info del plan
        findViewById<TextView>(R.id.tvEspecialidad).text = intent.getStringExtra("nombreEspecialidad") ?: ""
        findViewById<TextView>(R.id.tvMedico).text = "Dr. ${intent.getStringExtra("nombreMedico") ?: ""}"
        findViewById<TextView>(R.id.tvDiagnostico).text = "Diagnóstico: ${intent.getStringExtra("diagnostico") ?: ""}"
        findViewById<TextView>(R.id.tvDescripcion).text = intent.getStringExtra("descripcion") ?: ""

        val totalSesiones = intent.getIntExtra("totalSesiones", 0)
        val sesionesCompletadas = intent.getIntExtra("sesionesCompletadas", 0)
        val progressBar = findViewById<ProgressBar>(R.id.progressSesiones)
        progressBar.max = totalSesiones
        progressBar.progress = sesionesCompletadas
        findViewById<TextView>(R.id.tvProgreso).text =
            "Sesión $sesionesCompletadas de $totalSesiones completadas"

        // RecyclerView de sesiones
        val rvSesiones = findViewById<RecyclerView>(R.id.rvSesiones)
        rvSesiones.layoutManager = LinearLayoutManager(this)

        // Botón agendar
        val btnAgendar = findViewById<MaterialButton>(R.id.btnAgendarSesion)
        val estadoPlan = intent.getStringExtra("estado") ?: ""
        btnAgendar.visibility = if (estadoPlan == Constants.ESTADO_TRATAMIENTO_ACTIVO) View.VISIBLE else View.GONE
        btnAgendar.text = "Agendar Próxima Sesión — S/. ${"%.2f".format(precioPorSesion)}"
        btnAgendar.setOnClickListener {
            if (proximaSesionPendiente != null) {
                iniciarFlujoAgendarSesion()
            } else {
                Toast.makeText(this, "No hay sesiones pendientes por agendar", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar sesiones
        viewModel.isLoading.observe(this) { loading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.mensaje.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
        }

        viewModel.sesionesTratamiento.observe(this) { sesiones ->
            rvSesiones.adapter = SesionesAdapter(sesiones)
            // Encontrar la próxima sesión pendiente (no agendada)
            proximaSesionPendiente = sesiones.firstOrNull {
                it.estado == Constants.ESTADO_SESION_PENDIENTE
            }
            if (proximaSesionPendiente == null) {
                btnAgendar.isEnabled = false
                btnAgendar.text = "Todas las sesiones agendadas"
            }
        }

        viewModel.sesionAgendada.observe(this) { agendada ->
            if (agendada == true) {
                viewModel.sesionAgendadaCompletada()
                viewModel.cargarSesiones(idPlan) // Recargar
            }
        }

        // Cargar datos
        viewModel.cargarDatosPaciente()
        viewModel.cargarSesiones(idPlan)
    }

    /**
     * Inicia el flujo de agendar sesión: seleccionar horario del médico y pagar.
     */
    private fun iniciarFlujoAgendarSesion() {
        viewModel.cargarSlots(idMedico)

        viewModel.slotsHorario.observe(this) { slots ->
            if (slots.isEmpty()) {
                Toast.makeText(this, "No hay slots disponibles para este médico", Toast.LENGTH_SHORT).show()
                return@observe
            }
            mostrarDialogSeleccionHorario(slots)
        }
    }

    private fun mostrarDialogSeleccionHorario(slots: List<com.ayacucho.medicitas.model.SlotHorario>) {
        val items = slots.map { "${it.fecha} — ${it.horaInicio}" }.toTypedArray()
        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Horario para Sesión")
            .setSingleChoiceItems(items, 0) { _, which -> selectedIndex = which }
            .setPositiveButton("Pagar S/. ${"%.2f".format(precioPorSesion)}") { _, _ ->
                val selected = slots[selectedIndex]
                mostrarDialogPagoSesion(selected)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogPagoSesion(slot: com.ayacucho.medicitas.model.SlotHorario) {
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

        tvMontoPagar.text = "S/. ${"%.2f".format(precioPorSesion)}"
        btnPagar.text = "Pagar S/. ${"%.2f".format(precioPorSesion)}"

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
                        // Simular QR de Yape (usando un icono del sistema como placeholder)
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
                progressPago.visibility = View.GONE
                tvEstadoProceso.text = "✅ ¡Pago aprobado!"
                tvEstadoProceso.setTextColor(ContextCompat.getColor(this, R.color.primary))

                val referencia = "SES-${UUID.randomUUID().toString().take(8).uppercase()}"

                val sesion = proximaSesionPendiente!!
                viewModel.agendarSesion(sesion, slot, precioPorSesion, metodoSeleccionado, referencia)

                Handler(Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                }, 500)
            }, 2000)
        }

        dialog.show()
    }
}

// ==================== Adaptador de Sesiones ====================

class SesionesAdapter(
    private val sesiones: List<SesionTratamiento>
) : RecyclerView.Adapter<SesionesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumeroSesion: TextView = view.findViewById(R.id.tvNumeroSesion)
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHora)
        val tvNotas: TextView = view.findViewById(R.id.tvNotas)
        val tvEstadoSesion: TextView = view.findViewById(R.id.tvEstadoSesion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sesion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sesion = sesiones[position]

        // Número de sesión con fondo circular
        holder.tvNumeroSesion.text = sesion.numeroSesion.toString()
        val circleBg = GradientDrawable()
        circleBg.shape = GradientDrawable.OVAL
        val circleColor = when (sesion.estado) {
            Constants.ESTADO_SESION_ATENDIDA -> "#2E7D32"
            Constants.ESTADO_SESION_AGENDADA -> "#1565C0"
            Constants.ESTADO_SESION_PENDIENTE -> "#9E9E9E"
            else -> "#C62828"
        }
        circleBg.setColor(Color.parseColor(circleColor))
        holder.tvNumeroSesion.background = circleBg

        // Fecha y hora
        holder.tvFechaHora.text = when {
            sesion.fecha.isNotBlank() -> "${sesion.fecha} a las ${sesion.hora}"
            else -> "Sin agendar"
        }

        // Notas (si hay)
        if (sesion.notas.isNotBlank()) {
            holder.tvNotas.text = "📝 ${sesion.notas}"
            holder.tvNotas.visibility = View.VISIBLE
        } else {
            holder.tvNotas.visibility = View.GONE
        }

        // Badge de estado
        val (textColor, bgColor, label) = when (sesion.estado) {
            Constants.ESTADO_SESION_ATENDIDA -> Triple("#2E7D32", "#E8F5E9", "✅ Completada")
            Constants.ESTADO_SESION_AGENDADA -> Triple("#1565C0", "#E3F2FD", "🔵 Agendada")
            Constants.ESTADO_SESION_PENDIENTE -> Triple("#757575", "#F5F5F5", "⚪ Pendiente")
            Constants.ESTADO_SESION_NO_ASISTIO -> Triple("#E65100", "#FFF3E0", "⚠️ No Asistió")
            Constants.ESTADO_SESION_CANCELADA -> Triple("#C62828", "#FFEBEE", "❌ Cancelada")
            else -> Triple("#757575", "#F5F5F5", sesion.estado)
        }
        holder.tvEstadoSesion.text = label
        holder.tvEstadoSesion.setTextColor(Color.parseColor(textColor))
        val badgeBg = GradientDrawable()
        badgeBg.setColor(Color.parseColor(bgColor))
        badgeBg.cornerRadius = 16f
        holder.tvEstadoSesion.background = badgeBg
    }

    override fun getItemCount() = sesiones.size
}
