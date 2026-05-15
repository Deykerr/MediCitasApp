package com.ayacucho.medicitas.view.doctor

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.DateUtils
import com.ayacucho.medicitas.view.auth.LoginActivity
import com.ayacucho.medicitas.viewmodel.RecepcionistaViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.UUID

class RecepcionistaHomeActivity : AppCompatActivity() {

    private lateinit var viewModel: RecepcionistaViewModel
    private lateinit var adapter: CitasRecepcionistaAdapter

    private lateinit var tvSaludo: TextView
    private lateinit var tvTotalCitas: TextView
    private lateinit var tvTotalPagados: TextView
    private lateinit var tvTotalPendientes: TextView
    private lateinit var btnFecha: MaterialButton
    private lateinit var btnNuevoPaciente: MaterialButton
    private lateinit var btnNuevaCita: MaterialButton
    private lateinit var rvCitas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinCitas: TextView

    private val listaCitas = mutableListOf<CitaMedica>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recepcionista_home)

        viewModel = ViewModelProvider(this)[RecepcionistaViewModel::class.java]

        inicializarVistas()
        configurarListeners()
        observarEstados()

        viewModel.cargarDatos()
        viewModel.escucharCitas()
    }

    private fun inicializarVistas() {
        tvSaludo = findViewById(R.id.tvSaludo)
        tvTotalCitas = findViewById(R.id.tvTotalCitas)
        tvTotalPagados = findViewById(R.id.tvTotalPagados)
        tvTotalPendientes = findViewById(R.id.tvTotalPendientes)
        btnFecha = findViewById(R.id.btnFecha)
        btnNuevoPaciente = findViewById(R.id.btnNuevoPaciente)
        btnNuevaCita = findViewById(R.id.btnNuevaCita)
        rvCitas = findViewById(R.id.rvCitas)
        progressBar = findViewById(R.id.progressBar)
        tvSinCitas = findViewById(R.id.tvSinCitas)

        adapter = CitasRecepcionistaAdapter(listaCitas,
            onCobrar = { cita -> mostrarDialogCobro(cita) },
            onAsistencia = { cita -> 
                AlertDialog.Builder(this)
                    .setTitle("Control de Asistencia")
                    .setMessage("¿Marcar que ${cita.nombrePaciente} HA LLEGADO y está en espera?")
                    .setPositiveButton("Sí") { _, _ -> viewModel.marcarAsistencia(cita.idCita, Constants.ESTADO_ASISTENCIA_EN_ESPERA) }
                    .setNegativeButton("No", null).show()
            },
            onCancelar = { cita ->
                AlertDialog.Builder(this)
                    .setTitle("Cancelar Cita")
                    .setMessage("¿Estás seguro de cancelar la cita de ${cita.nombrePaciente}?")
                    .setPositiveButton("Sí, Cancelar") { _, _ -> viewModel.cancelarCita(cita.idCita) }
                    .setNegativeButton("No", null).show()
            }
        )

        rvCitas.layoutManager = LinearLayoutManager(this)
        rvCitas.adapter = adapter
        btnFecha.text = DateUtils.fechaActual()
    }

    private fun configurarListeners() {
        btnFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val fecha = String.format("%02d/%02d/%04d", day, month + 1, year)
                btnFecha.text = fecha
                viewModel.cambiarFecha(fecha)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnNuevoPaciente.setOnClickListener {
            Toast.makeText(this, "Registrar paciente (En desarrollo)", Toast.LENGTH_SHORT).show()
        }

        btnNuevaCita.setOnClickListener {
            Toast.makeText(this, "Crear cita manual (En desarrollo)", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnCerrarSesion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Deseas salir del panel de recepción?")
                .setPositiveButton("Sí") { _, _ ->
                    viewModel.cerrarSesion()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("No", null).show()
        }
    }

    private fun observarEstados() {
        viewModel.datosRecepcionista.observe(this) { p ->
            tvSaludo.text = "Hola, ${p.nombres}"
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.mensaje.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.todasLasCitas.observe(this) { citas ->
            listaCitas.clear()
            listaCitas.addAll(citas)
            adapter.notifyDataSetChanged()

            tvSinCitas.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
            rvCitas.visibility = if (citas.isEmpty()) View.GONE else View.VISIBLE

            actualizarContadores(citas)
        }
    }

    private fun actualizarContadores(citas: List<CitaMedica>) {
        tvTotalCitas.text = citas.size.toString()
        tvTotalPagados.text = citas.count { it.estadoPago == Constants.ESTADO_PAGO_PAGADO }.toString()
        tvTotalPendientes.text = citas.count { it.estadoPago == Constants.ESTADO_PAGO_PENDIENTE }.toString()
    }

    private fun mostrarDialogCobro(cita: CitaMedica) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_simulacion_pago, null)
        val tvMonto = dialogView.findViewById<TextView>(R.id.tvMontoPagar)
        val toggleMetodo = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleMetodoPago)
        val layoutTarjeta = dialogView.findViewById<View>(R.id.layoutTarjeta)
        val layoutQR = dialogView.findViewById<View>(R.id.layoutQR)
        val ivQR = dialogView.findViewById<android.widget.ImageView>(R.id.ivQR)
        val tvInstruccionQR = dialogView.findViewById<TextView>(R.id.tvInstruccionQR)
        val btnPagar = dialogView.findViewById<MaterialButton>(R.id.btnPagar)
        val progressPago = dialogView.findViewById<ProgressBar>(R.id.progressPago)
        val tvEstadoProceso = dialogView.findViewById<TextView>(R.id.tvEstadoProceso)

        // Obtener el precio desde el modelo de cita
        // Si la cita tiene montoPago 0 (pero es privada), buscaríamos el precio real.
        // Para este ejemplo, usaremos un precio base de 50 si es 0.
        val montoACobrar = if (cita.montoPago > 0) cita.montoPago else 50.0
        tvMonto.text = "S/. ${"%.2f".format(montoACobrar)}"

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
                        tvInstruccionQR.text = "Muestra este QR al paciente para Yapear"
                        ivQR.setImageResource(android.R.drawable.ic_menu_view)
                        ivQR.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_purple))
                    }
                    R.id.btnMetodoPlin -> {
                        layoutTarjeta.visibility = View.GONE
                        layoutQR.visibility = View.VISIBLE
                        tvInstruccionQR.text = "Muestra este QR al paciente para pagar con Plin"
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
            val metodo = when (toggleMetodo.checkedButtonId) {
                R.id.btnMetodoYape -> Constants.METODO_PAGO_YAPE
                R.id.btnMetodoPlin -> Constants.METODO_PAGO_PLIN
                else -> Constants.METODO_PAGO_TARJETA
            }

            btnPagar.isEnabled = false
            progressPago.visibility = View.VISIBLE
            tvEstadoProceso.visibility = View.VISIBLE
            tvEstadoProceso.text = "Registrando pago con $metodo..."

            Handler(Looper.getMainLooper()).postDelayed({
                val referencia = "REC-${UUID.randomUUID().toString().take(6).uppercase()}"
                viewModel.marcarPagada(cita.idCita, montoACobrar, metodo, referencia)
                dialog.dismiss()
            }, 1500)
        }

        dialog.show()
    }
}

class CitasRecepcionistaAdapter(
    private val citas: List<CitaMedica>,
    private val onCobrar: (CitaMedica) -> Unit,
    private val onAsistencia: (CitaMedica) -> Unit,
    private val onCancelar: (CitaMedica) -> Unit
) : RecyclerView.Adapter<CitasRecepcionistaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora: TextView = view.findViewById(R.id.tvHora)
        val tvNombre: TextView = view.findViewById(R.id.tvNombrePaciente)
        val tvMedico: TextView = view.findViewById(R.id.tvMedicoEspecialidad)
        val tvEstadoCita: TextView = view.findViewById(R.id.tvEstadoCita)
        val tvEstadoPago: TextView = view.findViewById(R.id.tvEstadoPago)
        val btnCobrar: MaterialButton = view.findViewById(R.id.btnCobrar)
        val btnAsistencia: MaterialButton = view.findViewById(R.id.btnAsistencia)
        val btnCancelar: MaterialButton = view.findViewById(R.id.btnCancelar)
        val layoutExtra: View = view.findViewById(R.id.layoutAccionesExtra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cita_recepcionista, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cita = citas[position]
        holder.tvHora.text = cita.hora
        holder.tvNombre.text = cita.nombrePaciente
        holder.tvMedico.text = "Dr. ${cita.nombreMedico} - ${cita.nombreEspecialidad}"

        configurarBadgeCita(holder.tvEstadoCita, cita.estadoCita)
        configurarBadgePago(holder.tvEstadoPago, cita, holder.btnCobrar)

        // Lógica de asistencia
        val esConfirmada = cita.estadoCita == Constants.ESTADO_CITA_CONFIRMADA || cita.estadoCita == Constants.ESTADO_CITA_PENDIENTE
        holder.btnAsistencia.visibility = if (esConfirmada && cita.estadoAsistencia == Constants.ESTADO_ASISTENCIA_PROGRAMADO) View.VISIBLE else View.GONE
        holder.layoutExtra.visibility = if (esConfirmada) View.VISIBLE else View.GONE

        holder.btnCobrar.setOnClickListener { onCobrar(cita) }
        holder.btnAsistencia.setOnClickListener { onAsistencia(cita) }
        holder.btnCancelar.setOnClickListener { onCancelar(cita) }

        // Animación de entrada
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(position * 50L).start()
    }

    private fun configurarBadgeCita(tv: TextView, estado: String) {
        tv.text = estado
        val (txt, bg) = when (estado) {
            Constants.ESTADO_CITA_CONFIRMADA, Constants.ESTADO_CITA_PENDIENTE -> Pair("#1E40AF", "#DBEAFE")
            Constants.ESTADO_CITA_ATENDIDA -> Pair("#166534", "#DCFCE7")
            Constants.ESTADO_CITA_CANCELADA -> Pair("#991B1B", "#FEE2E2")
            else -> Pair("#92400E", "#FEF3C7")
        }
        tv.setTextColor(Color.parseColor(txt))
        val shape = GradientDrawable()
        shape.cornerRadius = 20f
        shape.setColor(Color.parseColor(bg))
        tv.background = shape
    }

    private fun configurarBadgePago(tv: TextView, cita: CitaMedica, btnCobrar: View) {
        if (cita.estadoPago == Constants.ESTADO_PAGO_PAGADO) {
            tv.text = "Pagado: S/. ${"%.2f".format(cita.montoPago)}"
            tv.setTextColor(Color.parseColor("#166534"))
            btnCobrar.visibility = View.GONE
        } else {
            // Si el monto es 0 en el registro (ej: cita creada sin pago previo), asumimos un precio base
            val precio = if (cita.montoPago > 0) cita.montoPago else 50.0
            tv.text = "Pendiente: S/. ${"%.2f".format(precio)}"
            tv.setTextColor(Color.parseColor("#991B1B"))
            btnCobrar.visibility = if (cita.estadoCita != Constants.ESTADO_CITA_CANCELADA) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount() = citas.size
}
