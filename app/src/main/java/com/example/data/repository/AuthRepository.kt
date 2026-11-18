package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class AuthRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val prefsRepo: UserPreferencesRepository
) {
    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUser(userId)

    suspend fun registerWithEmail(
        email: String,
        username: String,
        displayName: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanUsername = username.trim().removePrefix("@").lowercase()
            val cleanDisplay = displayName.trim().ifBlank { cleanUsername }

            require(cleanEmail.contains("@") && cleanEmail.contains(".")) {
                "Ingresa un correo electrónico válido."
            }
            require(cleanUsername.length >= 3 && cleanUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                "El usuario debe tener al menos 3 caracteres (letras, números o _)."
            }
            require(password.length >= 6) {
                "La contraseña debe tener al menos 6 caracteres."
            }
            if (userDao.getUserByEmail(cleanEmail) != null) {
                return@withContext Result.failure(IllegalStateException("Ya existe una cuenta con este correo."))
            }
            if (userDao.getUserByUsername(cleanUsername) != null) {
                return@withContext Result.failure(IllegalStateException("Ese nombre de usuario ya está en uso."))
            }

            val salt = generateSalt()
            val hash = hashPassword(password, salt)
            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                email = cleanEmail,
                username = cleanUsername,
                displayName = cleanDisplay,
                passwordHash = hash,
                passwordSalt = salt
            )
            userDao.insertUser(user)
            prefsRepo.setCurrentUserId(user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmailOrUsername(
        identifier: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val clean = identifier.trim().removePrefix("@")
            require(clean.isNotBlank() && password.isNotBlank()) {
                "Completa tu correo/usuario y contraseña."
            }
            val user = if (clean.contains("@")) {
                userDao.getUserByEmail(clean)
            } else {
                userDao.getUserByUsername(clean)
            } ?: return@withContext Result.failure(IllegalStateException("Cuenta no encontrada."))

            val expectedHash = hashPassword(password, user.passwordSalt)
            if (expectedHash != user.passwordHash) {
                return@withContext Result.failure(IllegalStateException("Contraseña incorrecta."))
            }
            prefsRepo.setCurrentUserId(user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recoverPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            require(newPassword.length >= 6) { "La nueva contraseña debe tener al menos 6 caracteres." }
            val user = userDao.getUserByEmail(cleanEmail)
                ?: return@withContext Result.failure(IllegalStateException("No existe ninguna cuenta con ese correo."))
            val newSalt = generateSalt()
            val newHash = hashPassword(newPassword, newSalt)
            userDao.updateUser(user.copy(passwordHash = newHash, passwordSalt = newSalt))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        username: String,
        photoPickerUri: Uri?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val current = userDao.getUserById(userId)
                ?: return@withContext Result.failure(IllegalStateException("Usuario no encontrado."))
            val cleanUsername = username.trim().removePrefix("@").lowercase()
            require(cleanUsername.length >= 3 && cleanUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                "Usuario inválido (mínimo 3 caracteres alfanuméricos o _)."
            }
            val existingOwner = userDao.getUserByUsername(cleanUsername)
            if (existingOwner != null && existingOwner.id != userId) {
                return@withContext Result.failure(IllegalStateException("Ese nombre de usuario ya pertenece a otra cuenta."))
            }

            val savedPhotoUri = if (photoPickerUri != null) {
                copyUriToAppImages(photoPickerUri, "avatar_$userId")
            } else {
                current.photoUri
            }

            val updated = current.copy(
                displayName = displayName.trim().ifBlank { current.displayName },
                username = cleanUsername,
                photoUri = savedPhotoUri
            )
            userDao.updateUser(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setParentalControl(userId: String, enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.getUserById(userId) ?: error("Usuario no encontrado")
            userDao.updateUser(user.copy(parentalControlEnabled = enabled))
        }
    }

    suspend fun toggleBlockUser(userId: String, targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.getUserById(userId) ?: error("Usuario no encontrado")
            val currentBlocked = user.blockedUserIds
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (currentBlocked.contains(targetUserId)) {
                currentBlocked.remove(targetUserId)
            } else {
                currentBlocked.add(targetUserId)
            }
            userDao.updateUser(user.copy(blockedUserIds = currentBlocked.joinToString(",")))
        }
    }

    suspend fun signOut() {
        prefsRepo.setCurrentUserId(null)
    }

    suspend fun deleteAccount(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userDao.deleteUser(userId)
            prefsRepo.setCurrentUserId(null)
        }
    }

    private fun copyUriToAppImages(uri: Uri, prefix: String): String {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val outFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("No se pudo leer la imagen seleccionada.")
        return Uri.fromFile(outFile).toString()
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password".toByteArray(Charsets.UTF_8)
        return digest.digest(combined).joinToString("") { "%02x".format(it) }
    }
}
