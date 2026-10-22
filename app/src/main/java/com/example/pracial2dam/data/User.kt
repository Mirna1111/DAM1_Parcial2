
package com.example.pracial2dam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val email: String,

    val name: String,

    val isSynced: Boolean = false
)
