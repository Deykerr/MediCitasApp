package com.ayacucho.medicitas.view.admin

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
import com.ayacucho.medicitas.model.Especialidad
import com.ayacucho.medicitas.model.PersonalSalud
import com.ayacucho.medicitas.viewmodel.AdminViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Gestión de Personal Médico (RF06.2).
 * Permite registrar doctores con cuenta Firebase Auth + Firestore,
 * seleccionando su especialidad desde un Spinner.
 * Adaptado para clínica privada (sin selector de posta).
 */
class GestionPersonalActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvListaVacia: TextView

    private val listaPersonal = mutableListOf<PersonalSalud>()
    private var listaEspecialidades = listOf<Especialidad>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_lista)

        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Gestionar Personal Médico"
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvListaVacia = findViewById(R.id.tvListaVacia)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PersonalAdapter(listaPersonal) { medico ->
            AlertDialog.Builder(this)
                .setTitle("Desactivar Médico")
                .setMessage("¿Desactivar a ${medico.nombres} ${medico.apellidos}?")
                .setPositiveButton("Desactivar") { _, _ -> viewModel.desactivarMedico(medico.idPersonal) }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        findViewById<FloatingActionButton>(R.id.fabAgregar).setOnClickListener {
            mostrarDialogRegistro()
        }

        observarEstados()

        // Cargar datos necesarios para el Spinner de especialidades
        viewModel.cargarEspecialidades()
        viewModel.cargarPersonalMedico()
    }

    private fun mostrarDialogRegistro() {
        if (listaEspecialidades.isEmpty()) {
            Toast.makeText(this, "Primero registra al menos una especialidad", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_personal, null)
        val etNombres = dialogView.findViewById<TextInputEditText>(R.id.etNombres)
        val etApellidos = dialogView.findViewById<TextInputEditText>(R.id.etApellidos)
        val etDni = dialogView.findViewById<TextInputEditText>(R.id.etDni)
        val etCorreo = dialogView.findViewById<TextInputEditText>(R.id.etCorreo)
        val etContrasena = dialogView.findViewById<TextInputEditText>(R.id.etContrasena)
        val spinnerEspecialidad = dialogView.findViewById<Spinner>(R.id.spinnerEspecialidad)

        // Llenar Spinner de especialidades
        val nombresEspecialidades = listaEspecialidades.map { it.nombreEspecialidad }
        spinnerEspecialidad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresEspecialidades)

        AlertDialog.Builder(this)
            .setTitle("Registrar Médico")
            .setView(dialogView)
            .setPositiveButton("Registrar") { _, _ ->
                val posEsp = spinnerEspecialidad.selectedItemPosition
                val especialidadSeleccionada = listaEspecialidades[posEsp]

                viewModel.registrarMedico(
                    context = this,
                    correo = etCorreo.text.toString(),
                    contrasena = etContrasena.text.toString(),
                    nombres = etNombres.text.toString(),
                    apellidos = etApellidos.text.toString(),
                    dni = etDni.text.toString(),
                    idEspecialidad = especialidadSeleccionada.idEspecialidad,
                    nombreEspecialidad = especialidadSeleccionada.nombreEspecialidad
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
        viewModel.personalMedico.observe(this) { lista ->
            listaPersonal.clear()
            listaPersonal.addAll(lista)
            recyclerView.adapter?.notifyDataSetChanged()
            tvListaVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.especialidades.observe(this) { lista ->
            listaEspecialidades = lista
        }
    }
}

// ==================== Adaptador para Personal Médico ====================

class PersonalAdapter(
    private val items: List<PersonalSalud>,
    private val onDesactivar: (PersonalSalud) -> Unit
) : RecyclerView.Adapter<PersonalAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreMedico)
        val tvDni: TextView = view.findViewById(R.id.tvDniMedico)
        val tvEspecialidad: TextView = view.findViewById(R.id.tvEspecialidadMedico)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_personal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medico = items[position]
        holder.tvNombre.text = "${medico.nombres} ${medico.apellidos}"
        holder.tvDni.text = "DNI: ${medico.dni}"
        holder.tvEspecialidad.text = medico.nombreEspecialidad.ifBlank { "Sin especialidad" }
        holder.itemView.findViewById<View>(R.id.btnDesactivar).setOnClickListener {
            onDesactivar(medico)
        }
    }

    override fun getItemCount() = items.size
}
