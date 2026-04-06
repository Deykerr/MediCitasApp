package com.ayacucho.medicitas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.ayacucho.medicitas.view.admin.AdminHomeActivity
import com.ayacucho.medicitas.view.auth.LoginActivity
import com.ayacucho.medicitas.view.doctor.MedicoHomeActivity
import com.ayacucho.medicitas.view.patient.PacienteHomeActivity
import com.ayacucho.medicitas.viewmodel.AuthViewModel

import com.ayacucho.medicitas.utils.NetworkUtils

/**
 * MainActivity - Punto de entrada de la aplicación.
 * Actúa como Splash/Router: verifica si hay sesión activa y redirige.
 *
 * RF01.5: Si no hay sesión → LoginActivity
 * RF01.7: Si hay sesión → determina rol y redirige a la pantalla correcta
 * RF01.9: Sesión persistente (Firebase Auth mantiene la sesión automáticamente)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        observarNavegacion()

        // RF08: Verificación de Internet al iniciar la app
        if (!NetworkUtils.isNetworkAvailable(this)) {
            findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.GONE
            Toast.makeText(this, "La aplicación requiere conexión a internet. Verificando en modo offline...", Toast.LENGTH_LONG).show()
            // Podrías bloquear la app aquí o intentar ir al login. Enviaremos al login temporalmente.
            irAlLogin()
        } else {
            verificarSesion()
        }
    }

    private fun verificarSesion() {
        if (authViewModel.haySesionActiva()) {
            // Hay sesión activa → determinar rol y redirigir
            authViewModel.verificarSesionActiva()
        } else {
            // No hay sesión → ir al Login
            irAlLogin()
        }
    }

    private fun observarNavegacion() {
        authViewModel.navegarPaciente.observe(this) { navegar ->
            if (navegar) {
                authViewModel.navegacionPacienteCompletada()
                startActivity(Intent(this, PacienteHomeActivity::class.java))
                finish()
            }
        }

        authViewModel.navegarMedico.observe(this) { navegar ->
            if (navegar) {
                authViewModel.navegacionMedicoCompletada()
                startActivity(Intent(this, MedicoHomeActivity::class.java))
                finish()
            }
        }

        authViewModel.navegarAdmin.observe(this) { navegar ->
            if (navegar) {
                authViewModel.navegacionAdminCompletada()
                startActivity(Intent(this, AdminHomeActivity::class.java))
                finish()
            }
        }

        authViewModel.errorMessage.observe(this) { mensaje ->
            mensaje?.let {
                authViewModel.limpiarError()
                // Si hay error al verificar sesión, ir al login
                irAlLogin()
            }
        }
    }

    private fun irAlLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}