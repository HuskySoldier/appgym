package cl.gymtastic.app

import cl.gymtastic.app.data.model.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    @Test
    fun hasActivePlan_returnsTrue_whenPlanEndMillisIsInTheFuture() {
        // GIVEN: Una fecha futura (1 hora más desde ahora)
        val futureTime = System.currentTimeMillis() + 3600000
        val user = User(
            email = "test@gym.cl",
            nombre = "Test",
            rol = "user",
            planEndMillis = futureTime
        )

        // THEN: hasActivePlan debe ser verdadero
        assertTrue("El plan debería estar activo", user.hasActivePlan)
    }

    @Test
    fun hasActivePlanreturnsfalsewhenplanEndMillisisnull() {
        // GIVEN: Un usuario sin fecha de término
        val user = User(
            email = "test@gym.cl",
            nombre = "Test",
            rol = "user",
            planEndMillis = null
        )

        // THEN: hasActivePlan debe ser falso
        assertFalse("El plan no debería estar activo si es null", user.hasActivePlan)
    }

    @Test
    fun hasActivePlanreturnsfalsewhenplanEndMillisisinthepast() {
        // GIVEN: Una fecha pasada (1 hora atrás)
        val pastTime = System.currentTimeMillis() - 3600000
        val user = User(
            email = "test@gym.cl",
            nombre = "Test",
            rol = "user",
            planEndMillis = pastTime
        )

        // THEN: hasActivePlan debe ser falso
        assertFalse("El plan debería estar vencido", user.hasActivePlan)
    }
}