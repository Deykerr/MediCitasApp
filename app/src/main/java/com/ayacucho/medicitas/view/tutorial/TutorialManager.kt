package com.ayacucho.medicitas.view.tutorial

import android.content.Context
import android.view.View
import com.ayacucho.medicitas.R

/**
 * TutorialManager — Gestiona si el tutorial debe mostrarse
 * y define la secuencia de pasos para cada pantalla.
 *
 * Usa SharedPreferences para que el tutorial solo se muestre
 * la primera vez que el paciente usa la app.
 */
object TutorialManager {

    private const val PREFS_NAME = "medicitas_tutorial_prefs"
    private const val KEY_LOGIN_SHOWN = "tutorial_login_mostrado"
    private const val KEY_REGISTRO_SHOWN = "tutorial_registro_mostrado"

    // ─── SharedPreferences helpers ────────────────────────────────────────────

    fun debeMostrarTutorialLogin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_LOGIN_SHOWN, false)
    }

    fun debeMostrarTutorialRegistro(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_REGISTRO_SHOWN, false)
    }

    fun marcarTutorialLoginMostrado(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGIN_SHOWN, true)
            .apply()
    }

    fun marcarTutorialRegistroMostrado(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REGISTRO_SHOWN, true)
            .apply()
    }

    /** Solo para pruebas: resetea ambos tutoriales */
    fun resetearTutoriales(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    // ─── Definición de pasos ──────────────────────────────────────────────────

    data class TutorialStep(
        val targetViewId: Int,
        val titulo: String,
        val descripcion: String,
        val iconRes: Int
    )

    fun getPasosLogin(context: Context): List<TutorialStep> = listOf(
        TutorialStep(
            targetViewId = R.id.tilCorreo,
            titulo = "Correo Electrónico",
            descripcion = "Ingresa el correo con el que te registraste en MEDICITAS. Asegúrate de que sea el correcto.",
            iconRes = R.drawable.ic_tutorial_email
        ),
        TutorialStep(
            targetViewId = R.id.tilContrasena,
            titulo = "Tu Contraseña",
            descripcion = "Escribe tu contraseña secreta. Debe tener mínimo 6 caracteres. ¡Nadie más puede verla!",
            iconRes = R.drawable.ic_tutorial_lock
        ),
        TutorialStep(
            targetViewId = R.id.tvOlvidoContrasena,
            titulo = "¿Olvidaste tu contraseña?",
            descripcion = "Si no recuerdas tu contraseña, toca aquí y te enviaremos un enlace de recuperación a tu correo.",
            iconRes = R.drawable.ic_tutorial_help
        ),
        TutorialStep(
            targetViewId = R.id.btnIngresar,
            titulo = "Ingresar a tu cuenta",
            descripcion = "Cuando hayas completado tu correo y contraseña, toca este botón para entrar a MEDICITAS.",
            iconRes = R.drawable.ic_tutorial_check
        ),
        TutorialStep(
            targetViewId = R.id.tvCrearCuenta,
            titulo = "¿Aún no tienes cuenta?",
            descripcion = "Si eres nuevo en MEDICITAS, toca aquí para registrarte. ¡Solo te toma un minuto!",
            iconRes = R.drawable.ic_tutorial_person
        )
    )

    fun getPasosRegistro(context: Context): List<TutorialStep> = listOf(
        TutorialStep(
            targetViewId = R.id.tilNombres,
            titulo = "Tu Nombre",
            descripcion = "Ingresa tus nombres tal como aparecen en tu documento de identidad (DNI).",
            iconRes = R.drawable.ic_tutorial_person
        ),
        TutorialStep(
            targetViewId = R.id.tilApellidos,
            titulo = "Tus Apellidos",
            descripcion = "Escribe tus apellidos completos. Esto nos ayuda a identificarte en el sistema.",
            iconRes = R.drawable.ic_tutorial_person
        ),
        TutorialStep(
            targetViewId = R.id.tilDni,
            titulo = "DNI (8 dígitos)",
            descripcion = "Tu número de DNI de 8 dígitos. Debe ser único — no puede haber dos pacientes con el mismo DNI.",
            iconRes = R.drawable.ic_tutorial_badge
        ),
        TutorialStep(
            targetViewId = R.id.tilTelefono,
            titulo = "Teléfono de Contacto",
            descripcion = "Tu número de celular (9 dígitos). Lo usaremos para recordatorios de tus citas médicas.",
            iconRes = R.drawable.ic_tutorial_phone
        ),
        TutorialStep(
            targetViewId = R.id.tilCorreo,
            titulo = "Correo Electrónico",
            descripcion = "Este correo será tu usuario para iniciar sesión. Asegúrate de tener acceso a él.",
            iconRes = R.drawable.ic_tutorial_email
        ),
        TutorialStep(
            targetViewId = R.id.tilContrasena,
            titulo = "Crea tu Contraseña",
            descripcion = "Elige una contraseña segura de al menos 6 caracteres. Combina letras y números.",
            iconRes = R.drawable.ic_tutorial_lock
        ),
        TutorialStep(
            targetViewId = R.id.btnRegistrarse,
            titulo = "¡Listo para registrarte!",
            descripcion = "Cuando hayas completado todos los campos, toca este botón para crear tu cuenta en MEDICITAS.",
            iconRes = R.drawable.ic_tutorial_check
        )
    )
}
