package com.ayacucho.medicitas.view.admin

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ayacucho.medicitas.R
import com.ayacucho.medicitas.model.CitaMedica
import com.ayacucho.medicitas.utils.Constants
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class AdminReportesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_reportes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        cargarReportes()
    }

    private fun cargarReportes() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        db.collection(Constants.COLLECTION_CITAS).get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                val citas = result.toObjects(CitaMedica::class.java)

                var confirmadas = 0
                var atendidas = 0
                var canceladas = 0
                var ingresos = 0.0

                for (cita in citas) {
                    when (cita.estadoCita) {
                        Constants.ESTADO_CITA_CONFIRMADA -> confirmadas++
                        "Atendida" -> {
                            atendidas++
                            if (cita.estadoPago == Constants.ESTADO_PAGO_PAGADO) {
                                ingresos += cita.montoPago
                            }
                        }
                        Constants.ESTADO_CITA_CANCELADA -> canceladas++
                    }
                    // Sumar ingresos también de confirmadas que estén pagadas
                    if (cita.estadoCita == Constants.ESTADO_CITA_CONFIRMADA && cita.estadoPago == Constants.ESTADO_PAGO_PAGADO) {
                         ingresos += cita.montoPago
                    }
                }

                findViewById<TextView>(R.id.tvCitasConfirmadas).text = confirmadas.toString()
                findViewById<TextView>(R.id.tvCitasAtendidas).text = atendidas.toString()
                findViewById<TextView>(R.id.tvCitasCanceladas).text = canceladas.toString()
                findViewById<TextView>(R.id.tvTotalIngresos).text = "S/. ${"%.2f".format(ingresos)}"
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
            }
    }
}
