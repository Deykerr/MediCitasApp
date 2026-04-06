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

            // Datos
            findViewById<TextView>(R.id.tvNombreCompleto).text =
                "${paciente.nombres} ${paciente.apellidos}"
            findViewById<TextView>(R.id.tvCorreo).text = paciente.correoElectronico
            findViewById<TextView>(R.id.tvDni).text = paciente.dni
            findViewById<TextView>(R.id.tvTelefono).text =
                paciente.telefono.ifBlank { "No registrado" }
            findViewById<TextView>(R.id.tvFechaRegistro).text =
                paciente.fechaRegistro.ifBlank { "No disponible" }
        }
    }
}
