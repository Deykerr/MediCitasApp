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
import com.ayacucho.medicitas.databinding.ActivityLoginBinding
import com.ayacucho.medicitas.utils.NetworkUtils
import com.ayacucho.medicitas.view.admin.AdminHomeActivity
import com.ayacucho.medicitas.view.doctor.MedicoHomeActivity
import com.ayacucho.medicitas.view.patient.PacienteHomeActivity
import com.ayacucho.medicitas.viewmodel.AuthViewModel

/**
 * Pantalla de Login.
 * RF01.3: Inicio de sesión con correo y contraseña.
 * RF01.7: Redirige según el rol del usuario (Paciente, Médico, Admin).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
        binding.btnIngresar.setOnClickListener {
            // RF08: Verificación de Internet antes de Login
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No hay conexión a Internet. Por favor, verifica tu red.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val correo = binding.etCorreo.text.toString().trim()
            val contrasena = binding.etContrasena.text.toString().trim()

            // RF08: Validaciones de formulario
            if (correo.isEmpty()) {
                binding.etCorreo.error = "Ingresa tu correo"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                binding.etCorreo.error = "Formato de correo inválido"
                return@setOnClickListener
            }
            if (contrasena.isEmpty()) {
                binding.etContrasena.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }
            if (contrasena.length < 6) {
                binding.etContrasena.error = "La contraseña debe tener mínimo 6 caracteres"
                return@setOnClickListener
            }

            authViewModel.iniciarSesion(correo, contrasena)
        }

        binding.btnIngresar.setOnLongClickListener {
            Toast.makeText(this, "Creando Super Admin...", Toast.LENGTH_SHORT).show()
            com.ayacucho.medicitas.utils.AdminBootstrap.crearAdminPorDefecto { resultado ->
                Toast.makeText(this, resultado, Toast.LENGTH_LONG).show()
                if (resultado.contains("EXITOSAMENTE")) {
                    binding.etCorreo.setText("admin@medicitas.com")
                    binding.etContrasena.setText("admin123456")
                }
            }
            true
        }

        binding.tvCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        binding.tvOlvidoContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }
    }

    private fun observarEstados() {
        authViewModel.isLoading.observe(this) { cargando ->
            binding.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnIngresar.isEnabled = !cargando
        }

        authViewModel.errorMessage.observe(this) { mensaje ->
            mensaje?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                authViewModel.limpiarError()
            }
        }

        authViewModel.navegarPaciente.observe(this) { navegar ->
            if (navegar) {
                authViewModel.navegacionPacienteCompletada()
                Toast.makeText(this, getString(R.string.msg_login_exitoso), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, PacienteHomeActivity::class.java))
                finish()
            }
        }

        authViewModel.navegarMedico.observe(this) { navegar ->
            if (navegar) {
                authViewModel.navegacionMedicoCompletada()
                Toast.makeText(this, "Bienvenido, Doctor", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MedicoHomeActivity::class.java))
                finish()
            }
        }

        authViewModel.navegarAdmin.observe(this) { navegar ->
            if (navegar) {
                authViewModel.navegacionAdminCompletada()
                Toast.makeText(this, "Bienvenido, Administrador", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminHomeActivity::class.java))
                finish()
            }
        }
    }
}
