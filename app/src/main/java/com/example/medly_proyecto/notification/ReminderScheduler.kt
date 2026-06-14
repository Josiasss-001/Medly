package com.example.medly_proyecto.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.TomaMedicamento
import java.util.*

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDoseAlarm(tomaIds: Array<String>, medicamento: String, dosis: String, timeInMillis: Long, esAlarma: Boolean) {
        // Para alarmas médicas REALES, NO aplicamos restricción de 10 PM.
        // La restricción de las 22:00 solo aplica a recordatorios generales.
        
        if (timeInMillis < System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_DOSIS
            putExtra(ReminderReceiver.EXTRA_TOMA_IDS, tomaIds)
            putExtra(ReminderReceiver.EXTRA_MEDICAMENTO, medicamento)
            putExtra(ReminderReceiver.EXTRA_DOSIS, dosis)
            putExtra(ReminderReceiver.EXTRA_IS_ALARM, esAlarma)
        }

        val requestCode = tomaIds.contentDeepHashCode() + (if (esAlarma) 1000 else 0)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    fun programarAlarmasAgrupadas(ids: Array<String>, medNombres: String, dosisInfo: String, fecha: String, horaStr: String) {
        val cal = parseHora(fecha, horaStr) ?: return
        val timeExacta = cal.timeInMillis

        // Alarma exacta según horario real
        scheduleDoseAlarm(ids, medNombres, dosisInfo, timeExacta, true)
    }

    fun programarAlarmasToma(toma: TomaMedicamento) {
        programarAlarmasAgrupadas(arrayOf(toma.id), toma.nombreMedicamento, toma.dosis, toma.fecha, toma.horaProgramada)
    }

    fun scheduleNudge(tomaId: String, medicamento: String, timeInMillis: Long) {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
        
        // Requisito: La última notificación del día nunca debe enviarse después de las 22:00 horas.
        if (cal.get(Calendar.HOUR_OF_DAY) >= 22) {
            cal.set(Calendar.HOUR_OF_DAY, 22)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
        }

        if (cal.timeInMillis < System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NUDGE
            putExtra(ReminderReceiver.EXTRA_TOMA_ID, tomaId)
            putExtra(ReminderReceiver.EXTRA_MEDICAMENTO, medicamento)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            tomaId.hashCode() + 200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    fun programarRecordatoriosDiarios() {
        // Recordatorios automáticos: Mañana (08:00), Tarde (14:00), Noche (20:00)
        val horas = listOf(8, 14, 20)
        horas.forEach { hora ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hora)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
            scheduleNudge("daily_rem_$hora", "Recordatorio", cal.timeInMillis)
        }
    }

    fun scheduleCitaReminder(citaId: String, info: String, timeInMillis: Long) {
        if (timeInMillis < System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_CITA
            putExtra(ReminderReceiver.EXTRA_CITA_INFO, info)
            putExtra(ReminderReceiver.EXTRA_CITA_ID, citaId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            citaId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    fun programarRecordatoriosCita(cita: CitaMedica) {
        val fechaCita = Calendar.getInstance().apply { timeInMillis = cita.fechaCita }
        val info = "${cita.especialidad} en ${cita.centroMedico} a las ${cita.horaCita}"

        // Recordatorios: Hoy, 1 día antes, 3 días antes.
        listOf(0, 1, 3).forEach { dias ->
            val reminderTime = (fechaCita.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -dias)
                set(Calendar.HOUR_OF_DAY, 9) // Recordar a las 9 AM
                set(Calendar.MINUTE, 0)
            }
            
            val mensaje = when(dias) {
                0 -> "Tu cita médica es hoy a las ${cita.horaCita}."
                1 -> "Tu cita médica es mañana a las ${cita.horaCita}."
                else -> "Tu cita médica es en $dias días."
            }
            scheduleCitaReminder("${cita.id}_$dias", mensaje, reminderTime.timeInMillis)
        }
    }

    fun cancelarAlarmasToma(tomaId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, tomaId.hashCode() + 1000, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun parseHora(fecha: String, horaStr: String): Calendar? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val date = sdf.parse("$fecha $horaStr")
            Calendar.getInstance().apply { time = date!! }
        } catch (e: Exception) {
            null
        }
    }
}
