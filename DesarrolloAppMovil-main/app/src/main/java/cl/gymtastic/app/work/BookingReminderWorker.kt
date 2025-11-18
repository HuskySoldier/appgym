package cl.gymtastic.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cl.gymtastic.app.R
import java.text.SimpleDateFormat
import java.util.*

class BookingReminderWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        // Claves para pasar datos al Worker
        const val KEY_TRAINER_NAME = "trainerName"
        const val KEY_BOOKING_TIME_MILLIS = "bookingTimeMillis"
    }

    override suspend fun doWork(): Result {
        // Obtener datos pasados al worker
        val trainerName = inputData.getString(KEY_TRAINER_NAME) ?: "Tu Trainer"
        val bookingTimeMillis = inputData.getLong(KEY_BOOKING_TIME_MILLIS, 0L)

        // Formatear la hora para la notificación
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val bookingTimeStr = if (bookingTimeMillis > 0) {
            timeFormat.format(Date(bookingTimeMillis))
        } else {
            "pronto"
        }

        // Crear y mostrar la notificación
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gymtastic_bookings" // Canal diferente para reservas

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Reservas", // Nombre del canal visible al usuario
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para recordatorios
            ).apply {
                description = "Notificaciones para tus sesiones agendadas con trainers."
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu ícono
            .setContentTitle("Recordatorio de Sesión")
            .setContentText("Tienes una sesión agendada con $trainerName a las $bookingTimeStr.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta
            .setAutoCancel(true) // Se cierra al tocarla
            // Opcional: Añadir acción para abrir la app
            // .setContentIntent(...)
            .build()

        // Usar un ID único basado en el tiempo de reserva (o un ID de reserva si lo tuvieras)
        // para evitar que notificaciones futuras sobrescriban las viejas si hay errores.
        // Convertir Long a Int puede causar colisiones, pero es simple para este caso.
        val notificationId = bookingTimeMillis.toInt().takeIf { it != 0 } ?: Random().nextInt()
        nm.notify(notificationId, notification)

        return Result.success()
    }
}