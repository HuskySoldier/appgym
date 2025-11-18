package cl.gymtastic.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.profileDataStore by preferencesDataStore("profile_prefs")

object ProfileDataStore {
    private val KEY_NAME = stringPreferencesKey("profile_name")
    private val KEY_PHONE = stringPreferencesKey("profile_phone")
    private val KEY_BIO = stringPreferencesKey("profile_bio")
    private val KEY_AVATAR = stringPreferencesKey("profile_avatar") // 👈 nuevo

    data class Profile(
        val name: String = "",
        val phone: String = "",
        val bio: String = "",
        val avatarUri: String? = null,
        val email: String = ""
    )

    fun observe(context: Context): Flow<Profile> =
        context.profileDataStore.data.map { p ->
            Profile(
                name = p[KEY_NAME] ?: "",
                phone = p[KEY_PHONE] ?: "",
                bio = p[KEY_BIO] ?: "",
                avatarUri = p[KEY_AVATAR],


            )
        }

    suspend fun save(context: Context, profile: Profile) {
        context.profileDataStore.edit { p ->
            p[KEY_NAME] = profile.name
            p[KEY_PHONE] = profile.phone
            p[KEY_BIO] = profile.bio
            profile.avatarUri?.let { p[KEY_AVATAR] = it }
        }
    }

    suspend fun saveAvatar(context: Context, avatarUri: String) {
        context.profileDataStore.edit { p -> p[KEY_AVATAR] = avatarUri }
    }
}

