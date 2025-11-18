package cl.gymtastic.app.data.local.db

import android.content.Context
import android.util.Log // <-- Importar Log para debug
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.gymtastic.app.data.local.dao.*
import cl.gymtastic.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class, ProductEntity::class, CartItemEntity::class,
        AttendanceEntity::class, TrainerEntity::class, BookingEntity::class
    ],
    // --- VERSIÓN INCREMENTADA A 9 ---
    version = 9,
    exportSchema = false // Puedes ponerla a true si quieres exportar el schema para versionamiento
)
abstract class GymTasticDatabase : RoomDatabase() {
    // --- DAOs abstractos ---
    abstract fun users(): UsersDao
    abstract fun products(): ProductsDao
    abstract fun cart(): CartDao
    abstract fun attendance(): AttendanceDao
    abstract fun trainers(): TrainersDao
    abstract fun bookings(): BookingsDao

    companion object {
        @Volatile private var INSTANCE: GymTasticDatabase? = null
        private const val DATABASE_NAME = "gymtastic.db" // Nombre de la BD

        // --- Migraciones ---
        // 1 -> 2: Añade checkOutTimestamp a attendance
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 1 a 2...")
                db.execSQL("ALTER TABLE attendance ADD COLUMN checkOutTimestamp INTEGER")
                Log.d("DB_MIGRATION", "Migración 1 a 2 completada.")
            }
        }
        // 2 -> 3: Añade columna img a products y trainers
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 2 a 3...")
                db.execSQL("ALTER TABLE products ADD COLUMN img TEXT")
                db.execSQL("ALTER TABLE trainers ADD COLUMN img TEXT")
                Log.d("DB_MIGRATION", "Migración 2 a 3 completada.")
            }
        }
        // 3 -> 4: Añade campos de suscripción a users
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 3 a 4...")
                db.execSQL("ALTER TABLE users ADD COLUMN planEndMillis INTEGER")
                db.execSQL("ALTER TABLE users ADD COLUMN sedeId INTEGER")
                db.execSQL("ALTER TABLE users ADD COLUMN sedeName TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN sedeLat REAL")
                db.execSQL("ALTER TABLE users ADD COLUMN sedeLng REAL")
                Log.d("DB_MIGRATION", "Migración 3 a 4 completada.")
            }
        }
        // 4 -> 5: Cambia attendance.userId a attendance.userEmail
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 4 a 5...")
                db.execSQL("CREATE TABLE attendance_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userEmail TEXT NOT NULL, timestamp INTEGER NOT NULL, checkOutTimestamp INTEGER)")
                // Copia asumiendo que userId contenía el email
                db.execSQL("INSERT INTO attendance_new (id, userEmail, timestamp, checkOutTimestamp) SELECT id, userId, timestamp, checkOutTimestamp FROM attendance")
                db.execSQL("DROP TABLE attendance")
                db.execSQL("ALTER TABLE attendance_new RENAME TO attendance")
                Log.d("DB_MIGRATION", "Migración 4 a 5 completada.")
            }
        }
        // 5 -> 6: Vacía
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { Log.d("DB_MIGRATION", "Migrando de 5 a 6 (No-op)...") }
        }
        // 6 -> 7: Añade avatarUri a users
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 6 a 7...")
                db.execSQL("ALTER TABLE users ADD COLUMN avatarUri TEXT")
                Log.d("DB_MIGRATION", "Migración 6 a 7 completada.")
            }
        }
        // 7 -> 8: Eliminar userId de bookings
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 7 a 8...")
                db.execSQL("CREATE TABLE bookings_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userEmail TEXT NOT NULL, trainerId INTEGER NOT NULL, fechaHora INTEGER NOT NULL, estado TEXT NOT NULL DEFAULT 'pendiente')")
                // Ajusta la JOIN según tu esquema real anterior
                db.execSQL("INSERT INTO bookings_new (id, userEmail, trainerId, fechaHora, estado) SELECT b.id, u.email, b.trainerId, b.fechaHora, b.estado FROM bookings AS b INNER JOIN users AS u ON b.userId = u.email")
                db.execSQL("DROP TABLE bookings")
                db.execSQL("ALTER TABLE bookings_new RENAME TO bookings")
                Log.d("DB_MIGRATION", "Migración 7 a 8 completada.")
            }
        }

        // --- NUEVA MIGRACIÓN 8 -> 9: Añadir fono y bio a users ---
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("DB_MIGRATION", "Migrando de 8 a 9...")
                // Añade las columnas fono y bio a la tabla users, permitiendo nulos (TEXT = String?)
                db.execSQL("ALTER TABLE users ADD COLUMN fono TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN bio TEXT")
                Log.d("DB_MIGRATION", "Migración 8 a 9 completada.")
            }
        }
        // --- FIN NUEVA MIGRACIÓN ---


        fun get(context: Context): GymTasticDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("Database", "Obteniendo instancia de la base de datos...")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymTasticDatabase::class.java,
                    DATABASE_NAME
                )
                    // ---  AÑADIR NUEVA MIGRACIÓN ---
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8, MIGRATION_8_9 // <-- Añadida
                    )
                    // .fallbackToDestructiveMigration() // Útil en desarrollo si una migración falla
                    .build()
                Log.d("Database", "Instancia construida.")

                INSTANCE = instance

                // Lanzar el seed DESPUÉS de que la instancia esté completamente creada y asignada
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d("Database", "Iniciando seedIfNeeded...")
                    seedIfNeeded(instance, context.applicationContext)
                    Log.d("Database", "seedIfNeeded completado.")
                }

                instance
            }
        }


        // ---  FUNCIÓN SEED COMPLETA  ---
        private suspend fun seedIfNeeded(db: GymTasticDatabase, context: Context) {
            // Usuarios
            val userCount = try { db.users().count() } catch (_: Exception) { 0 }
            if (userCount == 0) {
                db.users().insert(UserEntity(email = "admin@gymtastic.cl", passHash = "admin123".trim().hashCode().toString(), nombre = "Administrador", rol = "admin"))
                db.users().insert(UserEntity(email = "test@gymtastic.cl", passHash = "test1234".trim().hashCode().toString(), nombre = "Usuario Test", rol = "user"))
            }

            // PackageName para URIs de recursos drawable
            val packageName = context.packageName

            // Productos
            val products = try { db.products().getAll() } catch (_: Exception) { emptyList() }
            if (products.isEmpty()) {
                val poleraImgUri = "android.resource://$packageName/drawable/polera_gym"
                val botellaImgUri = "android.resource://$packageName/drawable/botella_agua"
                db.products().insertAll(
                    listOf(
                        ProductEntity(nombre = "Plan Mensual", precio = 19990.0, tipo = "plan"),
                        ProductEntity(nombre = "Plan Trimestral", precio = 54990.0, tipo = "plan"),
                        ProductEntity(nombre = "Polera Gym", precio = 12990.0, tipo = "merch", stock =90, img = poleraImgUri),
                        ProductEntity(nombre = "Botella", precio = 6990.0, tipo = "merch", stock = 80, img = botellaImgUri)
                    )
                )
            }

            // Trainers
            val trainersCount = try { db.trainers().count() } catch (_: Exception) { 0 }
            if (trainersCount == 0) {
                val anaImgUri = "android.resource://$packageName/drawable/trainer_ana"
                val luisImgUri = "android.resource://$packageName/drawable/trainer_luis"
                db.trainers().insertAll(
                    listOf(
                        TrainerEntity(nombre = "Ana Pérez", fono = "+56911111111", email = "ana@gymtastic.cl", especialidad = "Funcional", img = anaImgUri),
                        TrainerEntity(nombre = "Luis Gómez", fono = "+56922222222", email = "luis@gymtastic.cl", especialidad = "Hipertrofia", img = luisImgUri)
                    )
                )
            }
        }
    }
}

