package com.example.medly_proyecto.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medly_proyecto.repository.CitasRepository
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class NotificationSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()

        val scheduler = ReminderScheduler(applicationContext)
        val recetasRepo = RecetasRepository()
        val citasRepo = CitasRepository()

        // 1. Sincronizar Medicamentos (Tomas del día)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoyStr = sdf.format(Date())

        recetasRepo.getTomasPorFecha(userId, hoyStr) { tomas ->
            tomas?.filter { it.estado == "PENDIENTE" }?.groupBy { it.horaProgramada }?.forEach { (hora, lista) ->
                val ids = lista.map { it.id }.toTypedArray()
                val medNombres = lista.joinToString(", ") { it.nombreMedicamento }
                val dosisInfo = if (lista.size >= 3) "Múltiples dosis" else lista.joinToString(" + ") { it.dosis }
                val baseToma = lista[0]
                scheduler.programarAlarmasAgrupadas(ids, medNombres, dosisInfo, baseToma.fecha, baseToma.horaProgramada)
            }
        }

        // 2. Sincronizar Citas
        citasRepo.getCitasDesencriptadas(userId) { citas ->
            citas?.filter { it.fechaCita >= System.currentTimeMillis() }?.forEach { cita ->
                scheduler.programarRecordatoriosCita(cita)
            }
        }

        // 3. Programar los 3 recordatorios diarios de dosis pendientes
        scheduler.programarRecordatoriosDiarios()

        return Result.success()
    }
}
