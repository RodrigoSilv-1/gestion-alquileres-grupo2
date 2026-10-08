package com.example.gestionalquileres.data.auth

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.AppUser
import com.example.gestionalquileres.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.GoogleAuthProvider

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        role: UserRole
    ): AppResult<AppUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()

            val firebaseUser = result.user
                ?: return Result.failure(Exception("No se pudo crear el usuario."))

            val appUser = AppUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                role = role.name,
                active = true
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(appUser)
                .await()

            AppResult.Exito(appUser)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): AppResult<AppUser> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            getCurrentUser()
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }
    override suspend fun signInWithGoogle(
        idToken: String,
        role: UserRole
    ): AppResult<AppUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val result = auth.signInWithCredential(credential).await()

            val firebaseUser = result.user
                ?: return Result.failure(Exception("No se pudo iniciar sesión con Google."))

            val userDocument = firestore.collection("users")
                .document(firebaseUser.uid)

            val existingDocument = userDocument.get().await()
            val existingUser = existingDocument.toObject(AppUser::class.java)

            if (existingUser != null) {
                if (!existingUser.active) {
                    auth.signOut()
                    return Result.failure(Exception("Tu usuario está desactivado."))
                }

                return Result.success(existingUser)
            }

            val newUser = AppUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                role = role.name,
                active = true
            )

            userDocument.set(newUser).await()

            Result.success(newUser)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
    override suspend fun getCurrentUser(): AppResult<AppUser> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return AppResult.Error(Exception("No hay una sesión activa."))

            val document = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val appUser = document.toObject(AppUser::class.java)
                ?: return AppResult.Error(Exception("No se encontró el perfil del usuario."))

            if (!appUser.active) {
                auth.signOut()
                return AppResult.Error(Exception("Tu usuario está desactivado."))
            }

            AppResult.Exito(appUser)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}