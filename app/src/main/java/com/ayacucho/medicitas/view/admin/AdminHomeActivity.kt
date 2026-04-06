package com.ayacucho.medicitas.view.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.view.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

/**
 * Panel de Administración (Dashboard).
 * Pantalla principal del administrador con acceso a todas las secciones CRUD.
 * RF06: Mantenimiento del Sistema.
 */
class AdminHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navegación a las secciones CRUD
        findViewById<MaterialCardView>(R.id.cardPostas).setOnClickListener {
            startActivity(Intent(this, GestionPostasActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardEspecialidades).setOnClickListener {
            startActivity(Intent(this, GestionEspecialidadesActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardPersonal).setOnClickListener {
            startActivity(Intent(this, GestionPersonalActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardHorarios).setOnClickListener {
            startActivity(Intent(this, GestionHorariosActivity::class.java))
        }

        // Cerrar sesión (RF01.5)
        findViewById<MaterialButton>(R.id.btnCerrarSesion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage(getString(R.string.msg_cerrar_sesion))
                .setPositiveButton("Sí") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
