package com.example.pracial2dam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipos")
data class Equipo(
    @PrimaryKey
    var id: String = "",
    var nombre: String = "",
    var numeroSerie: String = "",
    var codigoActivo: String = "",
    var estado: String = "OPERATIVO",
    var ubicacion: String = "",
    var syncPending: Boolean = false      // Bandera para sincronizacion offline
)