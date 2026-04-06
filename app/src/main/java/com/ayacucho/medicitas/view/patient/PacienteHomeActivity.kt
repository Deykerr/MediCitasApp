package com.ayacucho.medicitas.view.patient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.utils.Constants
import com.ayacucho.medicitas.utils.NotificationHelper
import com.ayacucho.medicitas.view.auth.LoginActivity
import com.ayacucho.medicitas.viewmodel.PacienteViewModel
import com.google.android.material.card.MaterialCardView

/**
 * Pantalla principal del Paciente (Dashboard).
 * Opciones: Nueva Reserva, Mis Citas, Mi Perfil.
 */
class PacienteHomeActivity : AppCompatActivity() {

    private lateinit var viewModel: PacienteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_paciente_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[PacienteViewModel::class.java]

        // Cargar datos del paciente para el saludo
        viewModel.cargarDatosPaciente()
        viewModel.datosPaciente.observe(this) { paciente ->
            findViewById<TextView>(R.id.tvSaludo).text = "Hola, ${paciente.nombres}"
        }

        // Navegación
        findViewById<MaterialCardView>(R.id.cardNuevaReserva).setOnClickListener {
            startActivity(Intent(this, ReservaActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMisCitas).setOnClickListener {
            startActivity(Intent(this, MisCitasActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMiPerfil).setOnClickListener {
            startActivity(Intent(this, MiPerfilActivity::class.java))
        }

        // Cerrar sesión (RF01.5)
        findViewById<ImageButton>(R.id.btnCerrarSesion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage(getString(R.string.msg_cerrar_sesion))
                .setPositiveButton("Sí") { _, _ ->
                    viewModel.cerrarSesion()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("No", null).show()
        }

        solicitarPermisoNotificaciones()
        iniciarListenerCitas()
    }

    private fun iniciarListenerCitas() {
        viewModel.escucharCambiosCitas()
        viewModel.cambioCitaDetectado.observe(this) { cita ->
            if (cita.estadoCita == Constants.ESTADO_CITA_CANCELADA) {
                val notifHelper = NotificationHelper(this)
                notifHelper.mostrarNotificacion(
                    "Cita Cancelada",
                    "Tu cita en ${cita.nombrePosta} el ${cita.fecha} a las ${cita.hora} ha sido CANCELADA.",
                    3
                )
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Necesitamos permiso para enviarte recordatorios", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
