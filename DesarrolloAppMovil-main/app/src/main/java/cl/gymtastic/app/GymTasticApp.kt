package cl.gymtastic.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cl.gymtastic.app.work.ReminderWorker
import java.util.concurrent.TimeUnit

class GymTasticApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- LÓGICA PARA PROGRAMAR EL WORKER MOVIDA AQUÍ ---
        setupDailyReminderWorker()

    }


    //* Configura y programa el Worker periódico para el recordatorio diario.

    private fun setupDailyReminderWorker() {
        val workManager = WorkManager.getInstance(applicationContext)

        // Crea la solicitud periódica (cada 1 día)
        val dailyReminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .build()

        // Programa el trabajo único para evitar duplicados
        // ExistingPeriodicWorkPolicy.KEEP asegura que si ya está programado, no se haga nada.
        workManager.enqueueUniquePeriodicWork(
            "dailyTrainingReminder", // Nombre único para este trabajo
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReminderRequest
        )
        // Opcional: Log para confirmar que se intentó programar
        // android.util.Log.d("GymTasticApp", "Daily reminder worker setup attempted.")
    }
}
