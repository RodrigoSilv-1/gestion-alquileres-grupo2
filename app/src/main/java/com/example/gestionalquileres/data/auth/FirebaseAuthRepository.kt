package com.example.gestionalquileres.data.auth

import com.example.gestionalquileres.domain.model.AppUser
import com.example.gestionalquileres.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String
    ): Result<AppUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()

            val firebaseUser = result.user
                ?: return Result.failure(Exception("No se pudo crear el usuario."))

            val appUser = AppUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                role = UserRole.SECRETARIO.name,
                active = true
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(appUser)
                .await()

            Result.success(appUser)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<AppUser> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            getCurrentUser()
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun getCurrentUser(): Result<AppUser> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.failure(Exception("No hay una sesión activa."))

            val document = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val appUser = document.toObject(AppUser::class.java)
                ?: return Result.failure(Exception("No se encontró el perfil del usuario."))

            if (!appUser.active) {
                auth.signOut()
                return Result.failure(Exception("Tu usuario está desactivado."))
            }

            Result.success(appUser)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}