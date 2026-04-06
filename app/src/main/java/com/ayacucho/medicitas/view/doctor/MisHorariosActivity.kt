package com.ayacucho.medicitas.view.doctor

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
import com.ayacucho.medicitas.model.Horario
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.viewmodel.MedicoViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Pantalla de Mis Horarios del Médico.
 * RF05.4: Visualizar horarios asignados.
 * RF05.5: Bloquear/desbloquear horarios por ausencia.
 */
class MisHorariosActivity : AppCompatActivity() {

    private lateinit var viewModel: MedicoViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVacio: TextView
    private val listaHorarios = mutableListOf<Horario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_lista)

        viewModel = ViewModelProvider(this)[MedicoViewModel::class.java]

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Mis Horarios"
        toolbar.setNavigationOnClickListener { finish() }

        // Ocultar FAB (el médico no crea horarios, solo los ve)
        findViewById<View>(R.id.fabAgregar).visibility = View.GONE

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvVacio = findViewById(R.id.tvListaVacia)
        tvVacio.text = "No tienes horarios asignados"

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HorarioMedicoAdapter(listaHorarios,
            onToggleBloqueo = { horario ->
                if (horario.estado == Constants.ESTADO_HORARIO_BLOQUEADO) {
                    AlertDialog.Builder(this)
                        .setTitle("Desbloquear Horario")
                        .setMessage("¿Desbloquear el horario del ${horario.fecha}?")
                        .setPositiveButton("Sí") { _, _ -> viewModel.desbloquearHorario(horario.idHorario) }
                        .setNegativeButton("No", null).show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Bloquear Horario")
                        .setMessage("¿Bloquear el horario del ${horario.fecha}? Los pacientes no podrán reservar.")
                        .setPositiveButton("Sí") { _, _ -> viewModel.bloquearHorario(horario.idHorario) }
                        .setNegativeButton("No", null).show()
                }
            }
        )

        observarEstados()
        viewModel.cargarMisHorarios()
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
        viewModel.misHorarios.observe(this) { horarios ->
            listaHorarios.clear()
            listaHorarios.addAll(horarios)
            recyclerView.adapter?.notifyDataSetChanged()
            tvVacio.visibility = if (horarios.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (horarios.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}

// ==================== Adaptador de Horarios del Médico ====================

class HorarioMedicoAdapter(
    private val horarios: List<Horario>,
    private val onToggleBloqueo: (Horario) -> Unit
) : RecyclerView.Adapter<HorarioMedicoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaHorario)
        val tvHoras: TextView = view.findViewById(R.id.tvHorasHorario)
        val tvCupos: TextView = view.findViewById(R.id.tvCuposHorario)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoHorario)
        val btnToggle: MaterialButton = view.findViewById(R.id.btnToggleBloqueo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horario_medico, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val horario = horarios[position]

        holder.tvFecha.text = "${horario.dia} - ${horario.fecha}"
        holder.tvHoras.text = "${horario.horaInicio} a ${horario.horaFin}"
        holder.tvCupos.text = "Cupos: ${horario.cuposDisponibles}/${horario.cuposTotales}"

        val esBloqueado = horario.estado == Constants.ESTADO_HORARIO_BLOQUEADO

        // Estado con color
        holder.tvEstado.text = horario.estado
        if (esBloqueado) {
            holder.tvEstado.setTextColor(Color.parseColor("#C62828"))
            holder.btnToggle.text = "Desbloquear"
            holder.btnToggle.setTextColor(Color.parseColor("#2E7D32"))
            holder.btnToggle.setStrokeColorResource(R.color.success)
        } else {
            holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))
            holder.btnToggle.text = "Bloquear"
            holder.btnToggle.setTextColor(Color.parseColor("#C62828"))
            holder.btnToggle.setStrokeColorResource(R.color.error)
        }

        holder.btnToggle.setOnClickListener { onToggleBloqueo(horario) }
    }

    override fun getItemCount() = horarios.size
}
