package cl.gymtastic.app

import cl.gymtastic.app.ui.auth.calcPasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {

    // La función calcPasswordStrength ahora es pública, así que podemos importarla o usarla directamente
    // si estamos en el mismo paquete, o importarla si está en otro.
    // Como este test está en el mismo paquete (cl.gymtastic.app.ui.auth), la vemos directo.

    @Test
    fun `empty password returns 0`() {
        val result = calcPasswordStrength("")
        assertEquals(0, result)
    }

    @Test
    fun `short password with only letters returns 0`() {
        // "abcdef" -> Largo < 8, sin Mayús, sin Números, sin Símbolos
        val result = calcPasswordStrength("abcdef")
        assertEquals(0, result)
    }

    @Test
    fun `short password with digit returns 1`() {
        // "1" -> Largo < 8, Digito (+1)
        val result = calcPasswordStrength("1")
        assertEquals(1, result)
    }

    @Test
    fun `password length 8 with lower case returns 1`() {
        // "abcdefgh" -> Largo >= 8 (+1)
        val result = calcPasswordStrength("abcdefgh")
        assertEquals(1, result)
    }

    @Test
    fun `password with upper case adds point`() {
        // "A" -> Mayús (+1)
        // Nota: Según tu lógica, largo < 8 no suma, pero Mayús sí suma por su cuenta.
        val result = calcPasswordStrength("A")
        assertEquals(1, result)
    }

    @Test
    fun `medium password (length 8 + upper) returns 2`() {
        // "Abcdefgh" -> Largo (+1), Mayús (+1) = 2
        val result = calcPasswordStrength("Abcdefgh")
        assertEquals(2, result)
    }

    @Test
    fun `strong password (length 8 + upper + digit) returns 3`() {
        // "Abcdefg1" -> Largo (+1), Mayús (+1), Digito (+1) = 3
        val result = calcPasswordStrength("Abcdefg1")
        assertEquals(3, result)
    }

    @Test
    fun `complex password (length 8 + upper + digit + symbol) returns 4`() {
        // "Abcdef1@" -> Largo (+1), Mayús (+1), Digito (+1), Símbolo (+1) = 4
        val result = calcPasswordStrength("Abcdef1@")
        assertEquals(4, result)
    }

    @Test
    fun `very long password gets bonus but capped at 4`() {
        // "Abcdef1@2025" (12 chars)
        // Puntos: Largo>=8 (+1), Mayús (+1), Digito (+1), Símbolo (+1), Largo>=12 (+1) = 5
        // Pero la función tiene .coerceAtMost(4)
        val result = calcPasswordStrength("Abcdef1@2025")
        assertEquals(4, result)
    }
}