package com.ayacucho.medicitas.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Funciones de extensión y utilidades de formato para el sistema MEDICITAS.
 * Centraliza la lógica de formateo de fechas y validaciones comunes.
 */
object DateUtils {

    private val localePeru = Locale("es", "PE")

    /**
     * Obtiene la fecha actual formateada como "dd/MM/yyyy".
     */
    fun fechaActual(): String {
        val sdf = SimpleDateFormat(Constants.FORMATO_FECHA, localePeru)
        return sdf.format(Date())
    }

    /**
     * Obtiene la fecha y hora actual formateada como "dd/MM/yyyy HH:mm".
     */
    fun fechaHoraActual(): String {
        val sdf = SimpleDateFormat(Constants.FORMATO_FECHA_HORA, localePeru)
        return sdf.format(Date())
    }

    /**
     * Obtiene la hora actual formateada como "HH:mm".
     */
    fun horaActual(): String {
        val sdf = SimpleDateFormat(Constants.FORMATO_HORA, localePeru)
        return sdf.format(Date())
    }
}

/**
 * Utilidades de validación para el sistema MEDICITAS.
 */
object ValidationUtils {

    /**
     * Valida que el DNI tenga exactamente 8 dígitos numéricos (RF01.8).
     */
    fun validarDni(dni: String): Boolean {
        return dni.length == Constants.DNI_LENGTH && dni.all { it.isDigit() }
    }

    /**
     * Valida que el correo tenga un formato básico válido.
     */
    fun validarCorreo(correo: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()
    }

    /**
     * Valida que el teléfono tenga 9 dígitos (formato peruano).
     */
    fun validarTelefono(telefono: String): Boolean {
        return telefono.length == 9 && telefono.all { it.isDigit() }
    }

    /**
     * Valida que la contraseña tenga al menos 6 caracteres
     * (requisito mínimo de Firebase Auth).
     */
    fun validarContrasena(contrasena: String): Boolean {
        return contrasena.length >= 6
    }
}
