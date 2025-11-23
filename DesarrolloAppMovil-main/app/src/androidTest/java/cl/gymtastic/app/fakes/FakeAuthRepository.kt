package cl.gymtastic.app.fakes

import android.content.Context
import cl.gymtastic.app.data.remote.UserProfileDto
import cl.gymtastic.app.data.repository.AuthRepository

// IMPORTANTE: AuthRepository debe ser 'open' en su definición original para que esto funcione.
class FakeAuthRepository(context: Context) : AuthRepository(context) {

    // Simulamos el login sin red
    override suspend fun login(email: String, password: String, rememberMe: Boolean): Boolean {
        // Lógica simple de prueba
        return email == "test@gym.com" && password == "123456"
    }

    // Simulamos el perfil para que la app sepa a dónde navegar
    override suspend fun getUserProfile(email: String): UserProfileDto? {
        return UserProfileDto(
            email = "test@gym.com",
            nombre = "Usuario Test",
            rol = "user",
            planEndMillis = System.currentTimeMillis() + 100000
        )
    }
}