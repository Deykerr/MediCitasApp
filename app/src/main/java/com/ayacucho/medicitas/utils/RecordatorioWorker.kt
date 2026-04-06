package com.ayacucho.medicitas.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class RecordatorioWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val titulo = inputData.getString("titulo") ?: "Recordatorio de Cita"
        val mensaje = inputData.getString("mensaje") ?: "Tienes una cita médica pronto."
        val idNotif = inputData.getInt("idNotificacion", 2)

        val notifHelper = NotificationHelper(context)
        notifHelper.mostrarNotificacion(titulo, mensaje, idNotif)

        return Result.success()
    }
}
