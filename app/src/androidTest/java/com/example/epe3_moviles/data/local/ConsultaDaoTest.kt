package com.example.epe3_moviles.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Pruebas instrumentadas para [ConsultaDao] utilizando una base de datos Room en memoria.
 *
 * Valida:
 * 1. Inserción masiva de 220 registros.
 * 2. Consulta monolítica [ConsultaDao.getAllConsultas] ordenada por fecha DESC.
 * 3. Conteo correcto [ConsultaDao.getCount].
 * 4. Obtención de la fuente paginada [ConsultaDao.getPagingConsultas].
 */
@RunWith(AndroidJUnit4::class)
class ConsultaDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ConsultaDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.consultaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insercionMasiva_inserta220RegistrosCorrectamente() = runBlocking {
        val consultas = ConsultaFicticiaDataGenerator.generateConsultas(220)
        dao.insertAll(consultas)

        val count = dao.getCount()
        assertEquals(220, count)
    }

    @Test
    fun getAllConsultas_retornaTodosLosRegistrosOrdenadosPorFechaDesc() = runBlocking {
        val consultas = ConsultaFicticiaDataGenerator.generateConsultas(220)
        dao.insertAll(consultas)

        val resultado = dao.getAllConsultas()
        assertEquals(220, resultado.size)

        // Verificar orden cronológico inverso
        for (i in 0 until resultado.size - 1) {
            assertTrue(
                "El registro $i debe ser más reciente o igual al registro ${i + 1}",
                resultado[i].fecha >= resultado[i + 1].fecha
            )
        }
    }

    @Test
    fun getPagingConsultas_retornaPagingSourceValido() {
        val pagingSource = dao.getPagingConsultas()
        assertNotNull(pagingSource)
    }
}
