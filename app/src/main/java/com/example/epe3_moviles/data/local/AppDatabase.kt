package com.example.epe3_moviles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de datos Room para la plataforma móvil de consultas médicas remotas.
 *
 * Incluye pre-poblado automático con más de 200 registros clínicos ficticios
 * reproducibles generados por [ConsultaFicticiaDataGenerator].
 */
@Database(
    entities = [ConsultaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun consultaDao(): ConsultaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "telemedicina_clinica.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-poblar los 220 registros en background al crear la BD
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.consultaDao()?.insertAll(
                                    ConsultaFicticiaDataGenerator.generateConsultas(220)
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Asegura de forma síncrona/coroutine que los datos estén poblados.
         * Útil para pruebas inmediatas donde se requiere garantizar que existen los 220 registros.
         */
        suspend fun asegurarDatosIniciales(database: AppDatabase) {
            val count = database.consultaDao().getCount()
            if (count < 200) {
                database.consultaDao().insertAll(
                    ConsultaFicticiaDataGenerator.generateConsultas(220)
                )
            }
        }
    }
}
