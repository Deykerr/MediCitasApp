package com.ayacucho.medicitas.view.patient

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.PlanTratamiento
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar

/**
 * Pantalla que lista los planes de tratamiento del paciente.
 * Muestra especialidad, médico, progreso de sesiones, y precio por sesión.
 * Al tocar un plan, navega a DetalleTratamientoActivity.
 */
class MisTratamientosActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_tratamientos)

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val rvTratamientos = findViewById<RecyclerView>(R.id.rvTratamientos)
        rvTratamientos.layoutManager = LinearLayoutManager(this)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvSinTratamientos = findViewById<TextView>(R.id.tvSinTratamientos)

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.mensaje.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
        }

        viewModel.misTratamientos.observe(this) { lista ->
            if (lista.isEmpty()) {
                tvSinTratamientos.visibility = View.VISIBLE
                rvTratamientos.visibility = View.GONE
            } else {
                tvSinTratamientos.visibility = View.GONE
                rvTratamientos.visibility = View.VISIBLE
                rvTratamientos.adapter = TratamientosAdapter(lista) { plan ->
                    val intent = Intent(this, DetalleTratamientoActivity::class.java)
                    intent.putExtra("idPlan", plan.idPlan)
                    intent.putExtra("nombreEspecialidad", plan.nombreEspecialidad)
                    intent.putExtra("nombreMedico", plan.nombreMedico)
                    intent.putExtra("diagnostico", plan.diagnostico)
                    intent.putExtra("descripcion", plan.descripcionTratamiento)
                    intent.putExtra("totalSesiones", plan.totalSesiones)
                    intent.putExtra("sesionesCompletadas", plan.sesionesCompletadas)
                    intent.putExtra("precioPorSesion", plan.precioPorSesion)
                    intent.putExtra("estado", plan.estado)
                    intent.putExtra("idMedico", plan.idMedico)
                    startActivity(intent)
                }
            }
        }

        viewModel.cargarMisTratamientos()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarMisTratamientos()
    }
}

// ==================== Adaptador de Tratamientos ====================

class TratamientosAdapter(
    private val tratamientos: List<PlanTratamiento>,
    private val onClick: (PlanTratamiento) -> Unit
) : RecyclerView.Adapter<TratamientosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEspecialidad: TextView = view.findViewById(R.id.tvEspecialidad)
        val tvEstadoPlan: TextView = view.findViewById(R.id.tvEstadoPlan)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvMedico: TextView = view.findViewById(R.id.tvMedico)
        val progressSesiones: android.widget.ProgressBar = view.findViewById(R.id.progressSesiones)
        val tvProgreso: TextView = view.findViewById(R.id.tvProgreso)
        val tvPrecioSesion: TextView = view.findViewById(R.id.tvPrecioSesion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tratamiento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plan = tratamientos[position]

        holder.tvEspecialidad.text = plan.nombreEspecialidad
        holder.tvDescripcion.text = plan.descripcionTratamiento.ifBlank { plan.diagnostico }
        holder.tvMedico.text = "Dr. ${plan.nombreMedico}"

        // Progreso
        holder.progressSesiones.max = plan.totalSesiones
        holder.progressSesiones.progress = plan.sesionesCompletadas
        holder.tvProgreso.text = "Sesión ${plan.sesionesCompletadas} de ${plan.totalSesiones}"
        holder.tvPrecioSesion.text = "S/. ${"%.2f".format(plan.precioPorSesion)} / sesión"

        // Badge de estado
        val (textColor, bgColor) = when (plan.estado) {
            Constants.ESTADO_TRATAMIENTO_ACTIVO -> Pair("#1565C0", "#E3F2FD")
            Constants.ESTADO_TRATAMIENTO_COMPLETADO -> Pair("#2E7D32", "#E8F5E9")
            Constants.ESTADO_TRATAMIENTO_CANCELADO -> Pair("#C62828", "#FFEBEE")
            else -> Pair("#757575", "#F5F5F5")
        }
        holder.tvEstadoPlan.text = plan.estado
        holder.tvEstadoPlan.setTextColor(Color.parseColor(textColor))
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor(bgColor))
        bg.cornerRadius = 16f
        holder.tvEstadoPlan.background = bg

        holder.itemView.setOnClickListener { onClick(plan) }
    }

    override fun getItemCount() = tratamientos.size
}
