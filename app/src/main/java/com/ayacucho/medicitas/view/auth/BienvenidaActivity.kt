package com.ayacucho.medicitas.view.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ayacucho.medicitas.R

/**
 * Pantalla de Bienvenida.
 * Muestra las tres opciones de ingreso: Paciente, Médico, Administrador.
 * Al seleccionar un rol, navega a LoginActivity con el rol como extra.
 */
class BienvenidaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROL = "extra_rol"
        const val ROL_PACIENTE = "paciente"
        const val ROL_MEDICO = "medico"
        const val ROL_ADMIN = "admin"
        const val ROL_RECEPCIONISTA = "recepcionista"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bienvenida)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cardPaciente = findViewById<CardView>(R.id.cardPaciente)
        val cardMedico = findViewById<CardView>(R.id.cardMedico)
        val cardRecepcionista = findViewById<CardView>(R.id.cardRecepcionista)
        val cardAdmin = findViewById<CardView>(R.id.cardAdmin)

        animarEntrada(cardPaciente, cardMedico, cardRecepcionista, cardAdmin)

        cardPaciente.setOnClickListener {
            animarClickYNavegar(it, ROL_PACIENTE)
        }

        cardMedico.setOnClickListener {
            animarClickYNavegar(it, ROL_MEDICO)
        }

        cardRecepcionista.setOnClickListener {
            animarClickYNavegar(it, ROL_RECEPCIONISTA)
        }

        cardAdmin.setOnClickListener {
            animarClickYNavegar(it, ROL_ADMIN)
        }
    }

    /** Animación de entrada en cascada para las tarjetas */
    private fun animarEntrada(vararg vistas: View) {
        vistas.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 80f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((100 + index * 120).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    /** Animación de press + navegación */
    private fun animarClickYNavegar(view: View, rol: String) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.95f, 1f)
        scaleX.duration = 200
        scaleY.duration = 200

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }

        view.postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(EXTRA_ROL, rol)
            startActivity(intent)
        }, 180)
    }
}
