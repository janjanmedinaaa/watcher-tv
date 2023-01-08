package com.medina.juanantonio.watcher.data.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val token: String,
    val imageUrl: String,
    val mobileNo: String,
    val countryCode: String
)