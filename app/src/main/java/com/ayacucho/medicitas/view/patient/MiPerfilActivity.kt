package com.ayacucho.medicitas.view.patient

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.appbar.MaterialToolbar

/**
 * Pantalla de Perfil del Paciente.
 * Muestra los datos personales del paciente logueado.
 */
class MiPerfilActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mi_perfil)

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        viewModel.cargarDatosPaciente()

        viewModel.datosPaciente.observe(this) { paciente ->
            // Iniciales para avatar
            val iniciales = "${paciente.nombres.firstOrNull() ?: ""}${paciente.apellidos.firstOrNull() ?: ""}"
            findViewById<TextView>(R.id.tvIniciales).text = iniciales.uppercase()

            // Datos Personales
            findViewById<TextView>(R.id.tvNombreCompleto).text = "${paciente.nombres} ${paciente.apellidos}"
            findViewById<TextView>(R.id.tvCorreo).text = paciente.correoElectronico
            findViewById<TextView>(R.id.tvDni).text = paciente.dni
            findViewById<TextView>(R.id.tvTelefono).text = paciente.telefono.ifBlank { "No registrado" }
            findViewById<TextView>(R.id.tvFechaRegistro).text = paciente.fechaRegistro.ifBlank { "No disponible" }

            // Datos Extendidos
            findViewById<TextView>(R.id.tvSexo).text = paciente.sexo.ifBlank { "No especificado" }
            findViewById<TextView>(R.id.tvDireccion).text = paciente.direccion.ifBlank { "No registrada" }
            findViewById<TextView>(R.id.tvContactoEmergencia).text = paciente.contactoEmergencia.ifBlank { "No registrado" }
            findViewById<TextView>(R.id.tvAlergias).text = paciente.alergias.ifBlank { "Ninguna registrada" }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditarPerfil).setOnClickListener {
            mostrarDialogoEdicion(viewModel.datosPaciente.value)
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAgregarFamiliar).setOnClickListener {
            mostrarDialogoAgregarFamiliar()
        }

        viewModel.dependientes.observe(this) { lista ->
            val container = findViewById<android.widget.LinearLayout>(R.id.llDependientesContainer)
            container.removeAllViews()
            if (lista.isEmpty()) {
                val tvVacio = TextView(this)
                tvVacio.text = "No tienes familiares registrados."
                tvVacio.setTextColor(resources.getColor(R.color.text_secondary, null))
                container.addView(tvVacio)
            } else {
                for (dep in lista) {
                    val view = layoutInflater.inflate(R.layout.item_dependiente, container, false)
                    view.findViewById<TextView>(R.id.tvNombreFamiliar).text = "${dep.nombres} ${dep.apellidos}"
                    view.findViewById<TextView>(R.id.tvRelacionFamiliar).text = "Relación: ${dep.relacion} - DNI: ${dep.dni}"
                    container.addView(view)
                }
            }
        }

        viewModel.cargarDependientes()
    }

    private fun mostrarDialogoAgregarFamiliar() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_familiar, null)
        val etNombres = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNombresFam)
        val etApellidos = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etApellidosFam)
        val etDni = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDniFam)
        val etRelacion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRelacionFam)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Agregar Familiar")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val dependiente = com.ayacucho.medicitas.model.Dependiente(
                    idDependiente = java.util.UUID.randomUUID().toString(),
                    nombres = etNombres.text.toString().trim(),
                    apellidos = etApellidos.text.toString().trim(),
                    dni = etDni.text.toString().trim(),
                    relacion = etRelacion.text.toString().trim(),
                    fechaRegistro = com.ayacucho.medicitas.utils.DateUtils.fechaHoraActual()
                )
                viewModel.agregarDependiente(dependiente)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEdicion(paciente: com.ayacucho.medicitas.model.Paciente?) {
        if (paciente == null) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_editar_perfil, null)
        val etSexo = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSexo)
        val etDireccion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDireccion)
        val etContacto = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etContactoEmergencia)
        val etAlergias = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAlergias)

        etSexo.setText(paciente.sexo)
        etDireccion.setText(paciente.direccion)
        etContacto.setText(paciente.contactoEmergencia)
        etAlergias.setText(paciente.alergias)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Editar Perfil Clínico")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val actualizados = mapOf(
                    "sexo" to etSexo.text.toString().trim(),
                    "direccion" to etDireccion.text.toString().trim(),
                    "contactoEmergencia" to etContacto.text.toString().trim(),
                    "alergias" to etAlergias.text.toString().trim()
                )
                viewModel.actualizarPerfil(actualizados)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
