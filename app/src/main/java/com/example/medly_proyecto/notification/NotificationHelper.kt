package com.example.medly_proyecto.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medly_proyecto.R
import com.example.medly_proyecto.ui.CalendarioCitasActivity
import com.example.medly_proyecto.ui.CalendarioTratamientoActivity
import com.example.medly_proyecto.ui.HomeActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_CITAS_ID = "citas_reminders"
        const val CHANNEL_DOSIS_ID = "dosis_reminders"
        const val CHANNEL_ALARMAS_ID = "dosis_alarms"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelCitas = NotificationChannel(
                CHANNEL_CITAS_ID,
                "Recordatorios de Citas Médicas",
                NotificationManager.IMPORTANCE_HIGH
            )

            val channelDosis = NotificationChannel(
                CHANNEL_DOSIS_ID,
                "Recordatorios de Medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            )

            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channelAlarmas = NotificationChannel(
                CHANNEL_ALARMAS_ID,
                "Alarmas de Dosis",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(channelCitas)
            notificationManager.createNotificationChannel(channelDosis)
            notificationManager.createNotificationChannel(channelAlarmas)
        }
    }

    fun getCitaNotificationBuilder(titulo: String, mensaje: String): NotificationCompat.Builder {
        val intent = Intent(context, CalendarioCitasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, mensaje.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_CITAS_ID)
            .setSmallIcon(R.drawable.medly)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    fun getDosisNotificationBuilder(
        titulo: String,
        mensaje: String,
        tomaIds: Array<String>,
        esAlarma: Boolean,
        notificationId: Int,
        showTakenButton: Boolean = true
    ): NotificationCompat.Builder {
        val channelId = if (esAlarma) CHANNEL_ALARMAS_ID else CHANNEL_DOSIS_ID
        val intent = Intent(context, CalendarioTratamientoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.medly)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (esAlarma) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (showTakenButton && esAlarma) {
            val takenIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TAKEN
                putExtra(ReminderReceiver.EXTRA_TOMA_IDS, tomaIds)
                putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            }
            val takenPendingIntent = PendingIntent.getBroadcast(context, notificationId + 1, takenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Dosis tomada", takenPendingIntent)
        }

        if (esAlarma) {
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        }

        return builder
    }

    fun notify(id: Int, builder: NotificationCompat.Builder) {
        notificationManager.notify(id, builder.build())
    }
    
    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }
}
