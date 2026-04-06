package com.ayacucho.medicitas.view.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Pantalla de Recuperación de Contraseña.
 * RF01.4: Envía un enlace de restablecimiento al correo del usuario.
 */
class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    // Vistas
    private lateinit var etCorreo: TextInputEditText
    private lateinit var btnEnviarEnlace: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVolverLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar_contrasena)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar ViewModel
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Enlazar vistas
        etCorreo = findViewById(R.id.etCorreo)
        btnEnviarEnlace = findViewById(R.id.btnEnviarEnlace)
        progressBar = findViewById(R.id.progressBar)
        tvVolverLogin = findViewById(R.id.tvVolverLogin)

        // Configurar listeners
        configurarListeners()

        // Observar LiveData
        observarEstados()
    }

    private fun configurarListeners() {
        btnEnviarEnlace.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            authViewModel.recuperarContrasena(correo)
        }

        tvVolverLogin.setOnClickListener {
            finish() // Volver al Login
        }
    }

    private fun observarEstados() {
        // Loading
        authViewModel.isLoading.observe(this) { cargando ->
            progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
            btnEnviarEnlace.isEnabled = !cargando
        }

        // Errores
        authViewModel.errorMessage.observe(this) { mensaje ->
            mensaje?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                authViewModel.limpiarError()
            }
        }

        // Envío exitoso
        authViewModel.recuperacionExitosa.observe(this) { exitoso ->
            if (exitoso) {
                authViewModel.recuperacionCompletada()
                Toast.makeText(this, getString(R.string.msg_correo_recuperacion), Toast.LENGTH_LONG).show()
                finish() // Volver al Login
            }
        }
    }
}
