package cl.gymtastic.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//El DataStore en tu app se encarga de guardar preferencias y
// datos pequeños como la sesión activa y parte del perfil del usuario, de forma persistente en el dispositivo
// Un DataStore dedicado
private val Context.checkStore by preferencesDataStore(name = "check_prefs")

data class CheckCounts(
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val lastInTs: Long? = null,
    val lastOutTs: Long? = null
)

object CheckPrefs {
    private val KEY_IN_TOTAL = intPreferencesKey("checkin_total")
    private val KEY_OUT_TOTAL = intPreferencesKey("checkout_total")
    private val KEY_IN_TS = longPreferencesKey("checkin_last_ts")
    private val KEY_OUT_TS = longPreferencesKey("checkout_last_ts")

    fun observe(context: Context): Flow<CheckCounts> {
        return context.checkStore.data.map { p ->
            CheckCounts(
                totalIn = p[KEY_IN_TOTAL] ?: 0,
                totalOut = p[KEY_OUT_TOTAL] ?: 0,
                lastInTs = p[KEY_IN_TS],
                lastOutTs = p[KEY_OUT_TS]
            )
        }
    }

    suspend fun incCheckIn(context: Context) {
        context.checkStore.edit { p ->
            p[KEY_IN_TOTAL] = (p[KEY_IN_TOTAL] ?: 0) + 1
            p[KEY_IN_TS] = System.currentTimeMillis()
        }
    }

    suspend fun incCheckOut(context: Context) {
        context.checkStore.edit { p ->
            p[KEY_OUT_TOTAL] = (p[KEY_OUT_TOTAL] ?: 0) + 1
            p[KEY_OUT_TS] = System.currentTimeMillis()
        }
    }
}


