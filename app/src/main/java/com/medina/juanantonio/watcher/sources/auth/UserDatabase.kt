package com.medina.juanantonio.watcher.sources.auth

import com.medina.juanantonio.watcher.data.models.user.User
import com.medina.juanantonio.watcher.database.WatcherDb

class UserDatabase(watcherDb: WatcherDb) : IUserDatabase {
    private val userDao = watcherDb.userDao()

    override suspend fun addUser(user: User) {
        userDao.insert(user)
    }

    override suspend fun getAllUsers(): List<User> {
        return userDao.getAll()
    }

    override suspend fun getUser(id: String): User? {
        return userDao.getUser(id)
    }

    override suspend fun getUserViaToken(token: String): User? {
        return userDao.getUserViaToken(token)
    }

    override suspend fun updateUserInfo(
        id: String,
        username: String,
        imageUrl: String,
        token: String
    ) {
        userDao.updateInfo(id, username, imageUrl, token)
    }

    override suspend fun deleteUser(id: String) {
        userDao.delete(id)
    }
}

interface IUserDatabase {
    suspend fun addUser(user: User)
    suspend fun getAllUsers(): List<User>
    suspend fun getUser(id: String): User?
    suspend fun getUserViaToken(token: String): User?
    suspend fun updateUserInfo(id: String, username: String, imageUrl: String, token: String)
    suspend fun deleteUser(id: String)
}