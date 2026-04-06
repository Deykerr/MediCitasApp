package com.ayacucho.medicitas.view.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.Especialidad
import com.ayacucho.medicitas.viewmodel.AdminViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * CRUD de Especialidades Médicas (RF06.1).
 */
class GestionEspecialidadesActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvListaVacia: TextView
    private val listaEspecialidades = mutableListOf<Especialidad>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_lista)

        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Gestionar Especialidades"
        toolbar.setNavigationOnClickListener { finish() }

        // Vistas
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvListaVacia = findViewById(R.id.tvListaVacia)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GenericAdapter(listaEspecialidades,
            onBind = { holder, item ->
                holder.tvNombre.text = item.nombreEspecialidad
                holder.tvDescripcion.text = item.descripcion.ifBlank { "Sin descripción" }
            },
            onEdit = { item -> mostrarDialogEspecialidad(item) },
            onDelete = { item ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar Especialidad")
                    .setMessage("¿Eliminar \"${item.nombreEspecialidad}\"?")
                    .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarEspecialidad(item.idEspecialidad) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        // FAB
        findViewById<FloatingActionButton>(R.id.fabAgregar).setOnClickListener {
            mostrarDialogEspecialidad(null)
        }

        observarEstados()
        viewModel.cargarEspecialidades()
    }

    private fun mostrarDialogEspecialidad(especialidad: Especialidad?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_especialidad, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombre)
        val etDescripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcion)

        // Si es edición, pre-llenar campos
        especialidad?.let {
            etNombre.setText(it.nombreEspecialidad)
            etDescripcion.setText(it.descripcion)
        }

        val titulo = if (especialidad == null) "Nueva Especialidad" else "Editar Especialidad"

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString()
                val descripcion = etDescripcion.text.toString()
                if (especialidad == null) {
                    viewModel.agregarEspecialidad(nombre, descripcion)
                } else {
                    viewModel.editarEspecialidad(especialidad.idEspecialidad, nombre, descripcion)
                }
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
        viewModel.especialidades.observe(this) { lista ->
            listaEspecialidades.clear()
            listaEspecialidades.addAll(lista)
            recyclerView.adapter?.notifyDataSetChanged()
            tvListaVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}

// ==================== Adaptador Genérico Reutilizable ====================

/**
 * RecyclerView Adapter genérico para listas con dos textos y botones editar/eliminar.
 * Reutilizable para Especialidades y Postas.
 */
class GenericAdapter<T>(
    private val items: List<T>,
    private val onBind: (GenericViewHolder, T) -> Unit,
    private val onEdit: (T) -> Unit,
    private val onDelete: (T) -> Unit
) : RecyclerView.Adapter<GenericAdapter.GenericViewHolder>() {

    class GenericViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): GenericViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_generic, parent, false)
        return GenericViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
        val item = items[position]
        onBind(holder, item)
        holder.itemView.findViewById<View>(R.id.btnEditar).setOnClickListener { onEdit(item) }
        holder.itemView.findViewById<View>(R.id.btnEliminar).setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}
