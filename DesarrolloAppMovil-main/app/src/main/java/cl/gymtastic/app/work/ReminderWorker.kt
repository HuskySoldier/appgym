package cl.gymtastic.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.gymtastic.app.R

class ReminderWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params){
    override suspend fun doWork(): Result {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gymtastic_daily"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, applicationContext.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = applicationContext.getString(R.string.notif_channel_desc)
            nm.createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Hora de entrenar!")
            .setContentText("Abre GymTastic y registra tu Check-In 🏋️")
            .build()
        nm.notify(1001, notif)
        return Result.success()
    }
}