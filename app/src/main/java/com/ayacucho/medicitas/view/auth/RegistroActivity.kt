package com.ayacucho.medicitas.view.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.databinding.ActivityRegistroBinding
import com.ayacucho.medicitas.utils.NetworkUtils
import com.ayacucho.medicitas.viewmodel.AuthViewModel

/**
 * Pantalla de Registro de Pacientes.
 * RF01.1: Registro con datos personales básicos.
 * RF01.2: Validación de DNI único (se valida en el repositorio).
 * RF01.8: Validación de formato de DNI (8 dígitos numéricos).
 *
 * Nota: Solo los pacientes se auto-registran. Médicos y administradores
 * son creados por el administrador del sistema (Fase 3).
 */
class RegistroActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        configurarListeners()
        observarEstados()
    }

    private fun configurarListeners() {
        binding.btnRegistrarse.setOnClickListener {
            // RF08: Verificación de Internet antes de Registro
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No hay conexión a Internet. Por favor, verifica tu red.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val nombres = binding.etNombres.text.toString().trim()
            val apellidos = binding.etApellidos.text.toString().trim()
            val dni = binding.etDni.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()
            val correo = binding.etCorreo.text.toString().trim()
            val contrasena = binding.etContrasena.text.toString().trim()
            val confirmarContrasena = binding.etConfirmarContrasena.text.toString().trim()

            // RF08: Validaciones de formulario (Local)
            if (nombres.isEmpty()) { binding.etNombres.error = "Campo requerido"; return@setOnClickListener }
            if (apellidos.isEmpty()) { binding.etApellidos.error = "Campo requerido"; return@setOnClickListener }
            if (dni.isEmpty() || dni.length != 8) { binding.etDni.error = "El DNI debe tener 8 dígitos"; return@setOnClickListener }
            if (telefono.isEmpty()) { binding.etTelefono.error = "Campo requerido"; return@setOnClickListener }
            if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                binding.etCorreo.error = "Formato de correo inválido"
                return@setOnClickListener
            }
            if (contrasena.length < 6) { binding.etContrasena.error = "Mínimo 6 caracteres"; return@setOnClickListener }
            if (contrasena != confirmarContrasena) { binding.etConfirmarContrasena.error = "Las contraseñas no coinciden"; return@setOnClickListener }

            authViewModel.registrarPaciente(nombres, apellidos, dni, telefono, correo, contrasena, confirmarContrasena)
        }

        binding.tvYaTienesCuenta.setOnClickListener {
            finish() // Volver al Login
        }
    }

    private fun observarEstados() {
        authViewModel.isLoading.observe(this) { cargando ->
            binding.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnRegistrarse.isEnabled = !cargando
        }

        authViewModel.errorMessage.observe(this) { mensaje ->
            mensaje?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                authViewModel.limpiarError()
            }
        }

        authViewModel.registroExitoso.observe(this) { exitoso ->
            if (exitoso) {
                authViewModel.registroCompletado()
                Toast.makeText(this, getString(R.string.msg_registro_exitoso), Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
        }
    }
}
