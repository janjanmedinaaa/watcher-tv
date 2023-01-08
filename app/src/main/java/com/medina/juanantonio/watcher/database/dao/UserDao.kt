package com.medina.juanantonio.watcher.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medina.juanantonio.watcher.data.models.user.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getUser(id: String): User?

    @Query("SELECT * FROM user WHERE token = :token")
    suspend fun getUserViaToken(token: String): User?

    @Query("UPDATE user SET username = :username, imageUrl = :imageUrl, token = :token WHERE id = :id")
    suspend fun updateInfo(id: String, username: String, imageUrl: String, token: String)

    @Query("DELETE FROM user WHERE id = :id")
    suspend fun delete(id: String)
}