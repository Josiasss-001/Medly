package com.example.medly_proyecto.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medly_proyecto.model.Notificacion
import com.example.medly_proyecto.repository.NotificacionesRepository
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_DOSIS = "com.example.medly_proyecto.SHOW_DOSIS"
        const val ACTION_SHOW_CITA = "com.example.medly_proyecto.SHOW_CITA"
        const val ACTION_TAKEN = "com.example.medly_proyecto.TAKEN"
        const val ACTION_POSTPONE = "com.example.medly_proyecto.POSTPONE"
        const val ACTION_NUDGE = "com.example.medly_proyecto.NUDGE"

        const val EXTRA_TOMA_ID = "extra_toma_id"
        const val EXTRA_TOMA_IDS = "extra_toma_ids"
        const val EXTRA_MEDICAMENTO = "extra_medicamento"
        const val EXTRA_DOSIS = "extra_dosis"
        const val EXTRA_IS_ALARM = "extra_is_alarm"
        const val EXTRA_CITA_INFO = "extra_cita_info"
        const val EXTRA_CITA_ID = "extra_cita_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // Si el dispositivo se reinicia o se actualiza la app, forzamos una sincronización
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val workRequest = OneTimeWorkRequestBuilder<NotificationSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
            return
        }

        val notificationHelper = NotificationHelper(context)
        val repository = RecetasRepository()
        val notifRepo = NotificacionesRepository()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""

        when (action) {
            ACTION_SHOW_DOSIS -> {
                val tomaIds = intent.getStringArrayExtra(EXTRA_TOMA_IDS) ?: arrayOf(intent.getStringExtra(EXTRA_TOMA_ID) ?: "")
                val medicamento = intent.getStringExtra(EXTRA_MEDICAMENTO) ?: "Medicamento"
                val dosis = intent.getStringExtra(EXTRA_DOSIS) ?: "Tomar dosis"
                val esAlarma = intent.getBooleanExtra(EXTRA_IS_ALARM, false)
                
                val count = tomaIds.size
                val titulo = if (esAlarma) "Hora de tu medicamento" else "Recordatorio de tratamiento"
                
                // Formatear mensaje según requerimientos: hasta 2 nombres, luego resumen
                val mensaje = if (count > 2) {
                    val firstTwo = medicamento.split(", ").take(2).joinToString(", ")
                    "Es hora de tomar $firstTwo y ${count - 2} medicamentos más."
                } else if (count == 2) {
                    "Es hora de tomar $medicamento."
                } else {
                    if (esAlarma) "Es hora de tomar $medicamento $dosis." 
                    else "Buenos días, no olvides marcar tus dosis pendientes."
                }

                val showTakenButton = count < 3

                val builder = notificationHelper.getDosisNotificationBuilder(
                    titulo,
                    mensaje,
                    tomaIds,
                    esAlarma,
                    tomaIds.contentDeepHashCode(),
                    showTakenButton
                )
                notificationHelper.notify(tomaIds.contentDeepHashCode(), builder)

                // Guardar en historial
                if (userId.isNotEmpty()) {
                    val uniqueId = "dosis_" + System.currentTimeMillis()
                    notifRepo.guardarNotificacion(Notificacion(
                        userId = userId,
                        titulo = titulo,
                        descripcion = mensaje,
                        tipo = "MEDICAMENTO",
                        timestamp = System.currentTimeMillis(),
                        leida = false
                    ), uniqueId)
                }
            }

            ACTION_SHOW_CITA -> {
                val citaInfo = intent.getStringExtra(EXTRA_CITA_INFO) ?: "Tienes una cita médica"
                val citaId = intent.getStringExtra(EXTRA_CITA_ID) ?: ""
                val builder = notificationHelper.getCitaNotificationBuilder("Recordatorio de Cita", citaInfo)
                notificationHelper.notify(citaInfo.hashCode(), builder)

                if (userId.isNotEmpty()) {
                    val uniqueId = "cita_rem_" + System.currentTimeMillis()
                    notifRepo.guardarNotificacion(Notificacion(
                        userId = userId,
                        titulo = "Recordatorio de Cita",
                        descripcion = citaInfo,
                        tipo = "CITA",
                        timestamp = System.currentTimeMillis(),
                        leida = false
                    ), uniqueId)
                }
            }

            ACTION_TAKEN -> {
                val tomaIds = intent.getStringArrayExtra(EXTRA_TOMA_IDS) ?: arrayOf(intent.getStringExtra(EXTRA_TOMA_ID) ?: "")
                val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)
                
                tomaIds.forEach { id ->
                    if (id.isNotEmpty()) {
                        repository.actualizarEstadoToma(id, "TOMADA", System.currentTimeMillis()) {
                            if (notificationId != 0) notificationHelper.cancel(notificationId)
                        }
                    }
                }
            }

            ACTION_POSTPONE -> {
                val tomaId = intent.getStringExtra(EXTRA_TOMA_ID) ?: ""
                val medicamento = intent.getStringExtra(EXTRA_MEDICAMENTO) ?: ""
                val dosis = intent.getStringExtra(EXTRA_DOSIS) ?: ""
                val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)
                
                val scheduler = ReminderScheduler(context)
                scheduler.scheduleDoseAlarm(arrayOf(tomaId + "_postponed"), medicamento, dosis, System.currentTimeMillis() + 5 * 60 * 1000, true)
                
                if (notificationId != 0) notificationHelper.cancel(notificationId)
            }

            ACTION_NUDGE -> {
                val tomaId = intent.getStringExtra(EXTRA_TOMA_ID) ?: ""
                val medicamento = intent.getStringExtra(EXTRA_MEDICAMENTO) ?: "tu medicamento"
                
                repository.getTomaById(tomaId) { toma ->
                    if (toma != null && toma.estado == "PENDIENTE") {
                        val builder = notificationHelper.getCitaNotificationBuilder(
                            "Recordatorio de tratamiento",
                            "Buenos días, no olvides marcar tus dosis pendientes."
                        )
                        notificationHelper.notify(tomaId.hashCode() + 555, builder)

                        if (userId.isNotEmpty()) {
                            notifRepo.guardarNotificacion(Notificacion(
                                userId = userId,
                                titulo = "Recordatorio de tratamiento",
                                descripcion = "Buenos días, no olvides marcar tus dosis pendientes.",
                                tipo = "MEDICAMENTO",
                                timestamp = System.currentTimeMillis(),
                                leida = false
                            ), "nudge_$tomaId")
                        }
                    }
                }
            }
        }
    }
}
