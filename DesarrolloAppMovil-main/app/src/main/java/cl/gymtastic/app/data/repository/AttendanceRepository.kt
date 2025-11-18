package cl.gymtastic.app.data.repository

import android.content.Context
import cl.gymtastic.app.data.local.db.GymTasticDatabase
import cl.gymtastic.app.data.local.entity.AttendanceEntity



//Es el encargado de decidir cómo obtener o guardar los datos,
// comunicándose con el DAO (el experto en base de datos) para realizar las operaciones necesarias.
class AttendanceRepository(context: Context) {
    private val db = GymTasticDatabase.get(context)


        // ...
        fun observe(userEmail: String) = db.attendance().observeByUser(userEmail) // <-- CAMBIO

        suspend fun checkIn(userEmail: String) { // <-- CAMBIO
            db.attendance().insert(
                AttendanceEntity(userEmail = userEmail, timestamp = System.currentTimeMillis()) // <-- CAMBIO
            )
        }

        suspend fun checkOut(userEmail: String) { // <-- CAMBIO
            val id = db.attendance().findLastOpenAttendanceId(userEmail) // <-- CAMBIO
            if (id != null) {
                db.attendance().updateCheckOutById(id, System.currentTimeMillis())
            }
        }
    }

