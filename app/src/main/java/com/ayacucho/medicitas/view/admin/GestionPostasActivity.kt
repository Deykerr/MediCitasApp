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
import com.ayacucho.medicitas.model.PostaMedica
import com.ayacucho.medicitas.viewmodel.AdminViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * CRUD de Postas Médicas (RF06.5).
 */
class GestionPostasActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvListaVacia: TextView
    private val listaPostas = mutableListOf<PostaMedica>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_lista)

        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Gestionar Postas"
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvListaVacia = findViewById(R.id.tvListaVacia)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GenericAdapter(listaPostas,
            onBind = { holder, item ->
                holder.tvNombre.text = item.nombrePosta
                holder.tvDescripcion.text = "${item.direccion} - ${item.distrito}"
            },
            onEdit = { item -> mostrarDialogPosta(item) },
            onDelete = { item ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar Posta")
                    .setMessage("¿Eliminar \"${item.nombrePosta}\"?")
                    .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarPosta(item.idPosta) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        findViewById<FloatingActionButton>(R.id.fabAgregar).setOnClickListener {
            mostrarDialogPosta(null)
        }

        observarEstados()
        viewModel.cargarPostas()
    }

    private fun mostrarDialogPosta(posta: PostaMedica?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_posta, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombrePosta)
        val etDireccion = dialogView.findViewById<TextInputEditText>(R.id.etDireccion)
        val etDistrito = dialogView.findViewById<TextInputEditText>(R.id.etDistrito)
        val etTelefono = dialogView.findViewById<TextInputEditText>(R.id.etTelefono)

        posta?.let {
            etNombre.setText(it.nombrePosta)
            etDireccion.setText(it.direccion)
            etDistrito.setText(it.distrito)
            etTelefono.setText(it.telefono)
        }

        val titulo = if (posta == null) "Nueva Posta Médica" else "Editar Posta"

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString()
                val direccion = etDireccion.text.toString()
                val distrito = etDistrito.text.toString()
                val telefono = etTelefono.text.toString()
                if (posta == null) {
                    viewModel.agregarPosta(nombre, direccion, distrito, telefono)
                } else {
                    viewModel.editarPosta(posta.idPosta, nombre, direccion, distrito, telefono)
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
        viewModel.postas.observe(this) { lista ->
            listaPostas.clear()
            listaPostas.addAll(lista)
            recyclerView.adapter?.notifyDataSetChanged()
            tvListaVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
