package cl.gymtastic.app

import cl.gymtastic.app.data.model.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    @Test
    fun user_WithFuturePlan_HasActivePlan() {
        // GIVEN: Una fecha 1 día en el futuro
        val futureTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        val user = User(
            email = "test@gym.cl",
            nombre = "Test User",
            rol = "user",
            planEndMillis = futureTime
        )

        // THEN
        assertTrue("El plan debería estar activo si la fecha es futura", user.hasActivePlan)
    }

    @Test
    fun user_WithPastPlan_HasNoActivePlan() {
        // GIVEN: Una fecha 1 segundo en el pasado
        val pastTime = System.currentTimeMillis() - 1000
        val user = User(
            email = "test@gym.cl",
            nombre = "Test User",
            rol = "user",
            planEndMillis = pastTime
        )

        // THEN
        assertFalse("El plan NO debería estar activo si la fecha ya pasó", user.hasActivePlan)
    }

    @Test
    fun user_WithNullPlan_HasNoActivePlan() {
        // GIVEN: Usuario sin fecha de término
        val user = User(
            email = "test@gym.cl",
            nombre = "Test User",
            rol = "user",
            planEndMillis = null
        )

        // THEN
        assertFalse("El plan NO debería estar activo si es null", user.hasActivePlan)
    }
}