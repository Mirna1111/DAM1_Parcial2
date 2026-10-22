package com.example.pracial2dam.data

import androidx.room.*

@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipos")
    suspend fun getAllEquipos(): List<Equipo>

    @Query("SELECT * FROM equipos WHERE syncPending = 1")
    suspend fun getPendingEquipos(): List<Equipo>

    @Query("SELECT * FROM equipos WHERE id = :id LIMIT 1")
    suspend fun getEquipoById(id: String): Equipo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipo(equipo: Equipo)

    @Update
    suspend fun updateEquipo(equipo: Equipo)

    @Delete
    suspend fun deleteEquipo(equipo: Equipo)

    @Query("DELETE FROM equipos WHERE id = :id")
    suspend fun deleteEquipoById(id: String)

    @Query("UPDATE equipos SET syncPending = 0 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}