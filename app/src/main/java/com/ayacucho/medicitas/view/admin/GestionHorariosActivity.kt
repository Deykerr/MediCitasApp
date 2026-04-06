package com.ayacucho.medicitas.view.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.Horario
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.viewmodel.AdminViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Gestión de Horarios de Atención (RF06.3).
 * Permite asignar bloques horarios a los médicos con DatePicker y TimePicker.
 */
class GestionHorariosActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvListaVacia: TextView

    private val listaHorarios = mutableListOf<Horario>()
    private var listaPersonal = listOf<PersonalSalud>()

    private val diasSemana = arrayOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_lista)

        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Gestionar Horarios"
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvListaVacia = findViewById(R.id.tvListaVacia)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HorarioAdapter(listaHorarios) { horario ->
            AlertDialog.Builder(this)
                .setTitle("Eliminar Horario")
                .setMessage("¿Eliminar este horario?")
                .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarHorario(horario.idHorario) }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        findViewById<FloatingActionButton>(R.id.fabAgregar).setOnClickListener {
            mostrarDialogHorario()
        }

        observarEstados()
        viewModel.cargarPersonalMedico()
        viewModel.cargarHorarios()
    }

    private fun mostrarDialogHorario() {
        if (listaPersonal.isEmpty()) {
            Toast.makeText(this, "Primero registra al menos un médico", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_horario, null)
        val spinnerMedico = dialogView.findViewById<Spinner>(R.id.spinnerMedico)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.etFecha)
        val etHoraInicio = dialogView.findViewById<TextInputEditText>(R.id.etHoraInicio)
        val etHoraFin = dialogView.findViewById<TextInputEditText>(R.id.etHoraFin)
        val etCupos = dialogView.findViewById<TextInputEditText>(R.id.etCupos)

        // Spinner de médicos
        val nombresMedicos = listaPersonal.map { "${it.nombres} ${it.apellidos}" }
        spinnerMedico.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresMedicos)

        // DatePicker para Fecha
        etFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val fecha = String.format("%02d/%02d/%04d", day, month + 1, year)
                etFecha.setText(fecha)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // TimePicker para Hora Inicio
        etHoraInicio.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                etHoraInicio.setText(String.format("%02d:%02d", hour, minute))
            }, 8, 0, true).show()
        }

        // TimePicker para Hora Fin
        etHoraFin.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                etHoraFin.setText(String.format("%02d:%02d", hour, minute))
            }, 14, 0, true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Nuevo Horario")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val posMedico = spinnerMedico.selectedItemPosition
                val medicoSeleccionado = listaPersonal[posMedico]
                val fecha = etFecha.text.toString()

                // Determinar día de la semana
                val dia = try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = sdf.parse(fecha)
                    val cal = Calendar.getInstance()
                    cal.time = date!!
                    diasSemana[cal.get(Calendar.DAY_OF_WEEK) - 1]
                } catch (e: Exception) { "" }

                val cupos = etCupos.text.toString().toIntOrNull() ?: 0

                viewModel.agregarHorario(
                    fecha = fecha,
                    dia = dia,
                    horaInicio = etHoraInicio.text.toString(),
                    horaFin = etHoraFin.text.toString(),
                    cuposTotales = cupos,
                    idPersonal = medicoSeleccionado.idPersonal,
                    idPosta = medicoSeleccionado.idPosta
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        viewModel.horarios.observe(this) { lista ->
            listaHorarios.clear()
            listaHorarios.addAll(lista)
            recyclerView.adapter?.notifyDataSetChanged()
            tvListaVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.personalMedico.observe(this) { lista ->
            listaPersonal = lista
        }
    }
}

// ==================== Adaptador para Horarios ====================

class HorarioAdapter(
    private val items: List<Horario>,
    private val onEliminar: (Horario) -> Unit
) : RecyclerView.Adapter<HorarioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaHorario)
        val tvHoras: TextView = view.findViewById(R.id.tvHorasHorario)
        val tvCupos: TextView = view.findViewById(R.id.tvCuposHorario)
        val tvMedico: TextView = view.findViewById(R.id.tvMedicoHorario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_horario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val horario = items[position]
        holder.tvFecha.text = "${horario.dia} - ${horario.fecha}"
        holder.tvHoras.text = "${horario.horaInicio} a ${horario.horaFin}"
        holder.tvCupos.text = "Cupos: ${horario.cuposDisponibles}/${horario.cuposTotales}"
        holder.tvMedico.text = "Médico ID: ${horario.idPersonal.take(8)}..."
        holder.itemView.findViewById<View>(R.id.btnEliminar).setOnClickListener {
            onEliminar(horario)
        }
    }

    override fun getItemCount() = items.size
}
