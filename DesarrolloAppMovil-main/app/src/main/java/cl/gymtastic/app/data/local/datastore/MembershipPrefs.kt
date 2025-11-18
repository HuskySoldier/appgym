package cl.gymtastic.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

// DataStore de membresía (NO se borra en logout)
val Context.membershipDataStore by preferencesDataStore("membership_prefs")

// --- Claves (lat/lng como String para evitar problemas de tipo) ---
private val KEY_ACTIVE     = booleanPreferencesKey("membership_active")
private val KEY_SEDE_ID    = intPreferencesKey("membership_sede_id")
private val KEY_SEDE_NAME  = stringPreferencesKey("membership_sede_name")
private val KEY_SEDE_LAT   = stringPreferencesKey("membership_sede_lat")   // String
private val KEY_SEDE_LNG   = stringPreferencesKey("membership_sede_lng")   // String
private val KEY_PLAN_END   = longPreferencesKey("membership_plan_end_millis")

// Estado que exponemos a la UI
data class MembershipState(

    val sedeId: Int? = null,
    val sedeName: String? = null,
    val sedeLat: Double? = null,
    val sedeLng: Double? = null,
    val planEndMillis: Long? = null
) {
    val hasActivePlan: Boolean
        get() {
            val end = planEndMillis ?: return false
            return end > System.currentTimeMillis()
        }
}


object MembershipPrefs {

    fun observe(ctx: Context): Flow<MembershipState> =
        ctx.membershipDataStore.data.map { p ->
            val latStr = p[KEY_SEDE_LAT]
            val lngStr = p[KEY_SEDE_LNG]
            MembershipState(

                planEndMillis = p[KEY_PLAN_END],
                sedeId       = p[KEY_SEDE_ID],
                sedeName     = p[KEY_SEDE_NAME],
                // usa la función de Kotlin, NO declares otra
                sedeLat      = latStr?.toDoubleOrNull(),
                sedeLng      = lngStr?.toDoubleOrNull()
            )
        }

    // Activar plan + almacenar sede
    suspend fun setActiveWithSede(
        ctx: Context,
        id: Int,
        name: String,
        lat: Double,
        lng: Double,
        planEndMillis: Long
    ) {
        ctx.membershipDataStore.edit { p ->
            p[KEY_ACTIVE] = true
            p[KEY_SEDE_ID] = id
            p[KEY_SEDE_NAME] = name
            p[KEY_SEDE_LAT] = lat.toString()   // guardamos String
            p[KEY_SEDE_LNG] = lng.toString()   // guardamos String
            p[KEY_PLAN_END] = planEndMillis
        }
    }

    // Política: se puede comprar si no hay plan o si faltan <= thresholdDays
    suspend fun canPurchaseNewPlan(ctx: Context, thresholdDays: Int = 3): Boolean {
        val st = observe(ctx).firstOrNull() ?: MembershipState()
        if (!st.hasActivePlan) return true
        val end = st.planEndMillis ?: return true
        val diff = end - System.currentTimeMillis()
        val days = if (diff <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diff)
        return days <= thresholdDays
    }

    // Limpia TODO (si quisieras borrar manualmente)
    suspend fun clear(ctx: Context) {
        ctx.membershipDataStore.edit { it.clear() }
    }
}
