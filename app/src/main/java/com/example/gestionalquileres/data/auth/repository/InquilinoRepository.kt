package com.example.gestionalquileres.data.auth.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Inquilino
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InquilinoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Referencia a la coleccion principal en Firestore
    private val collection = firestore.collection("inquilinos")

    suspend fun registrar(inquilino: Inquilino): AppResult<Inquilino> {
        return try {
            // Generamos una referencia a un nuevo documento vacío en Firestore con un ID único automático.
            val documento = collection.document()

    // Creamos una copia del inquilino original asignándole el ID generado por Firebase
    // y asegurándonos de que su estado 'active' sea verdadero (activo).
            val nuevoInquilino = inquilino.copy(
                idInquilino = documento.id,
                active = true
            )
        // Guardamos (o escribimos) el objeto completo en la base de datos de forma asíncrona usando .await()
            documento.set(nuevoInquilino).await()

        // devolvemos un AppResult de éxito con el inquilino ya creado y su ID asignado.
            AppResult.Exito(nuevoInquilino)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerTodos(): AppResult<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()
        //consultar y filtrar los registro quedandonos con los inquilinos activo
        //descargando los y esperando la respuesta del servidor

            val inquilinos = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inquilino::class.java)
            } //recorre los registros obtenidos y los transforma en objetos tipo inquilino

            AppResult.Exito(inquilinos)
            //empaqueta esa lista para enviarselo al vm
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerPorDni(dni: String): AppResult<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("dni", dni)
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents.mapNotNull { documento ->
                documento.toObject(Inquilino::class.java)
            }

            AppResult.Exito(inquilinos)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun obtenerPorNombre(nombre: String): AppResult<List<Inquilino>> {
        return try {
            val snapshot = collection
                .whereEqualTo("active", true)
                .get()
                .await()

            val inquilinos = snapshot.documents
                .mapNotNull { documento ->
                    documento.toObject(Inquilino::class.java)
                }
                .filter { inquilino ->
                    inquilino.nombre.contains(nombre, ignoreCase = true)
                }

            AppResult.Exito(inquilinos)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun actualizar(inquilino: Inquilino): AppResult<Inquilino> {
        return try {
            //verificamos que el inquilino tenga un ID válido antes de intentar actualizar
            if (inquilino.idInquilino.isBlank()) {
                // Si el ID está vacío, detenemos la ejecución y devolvemos un error
                return AppResult.Error(
                    Exception("El ID del inquilino es obligatorio.")
                )
            }
            // 2. Apuntamos al documento  en Firestore usando su ID único existente
            collection
                .document(inquilino.idInquilino)
                //Sobrescribimos el documento con los datos nuevos del inquilino
                .set(inquilino)
                //Esperamos que la operación en la nube se complete exitosamente
                .await()

            // devolvemos un AppResult de éxito con el objeto actualizado
            AppResult.Exito(inquilino)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    suspend fun darDeBaja(idInquilino: String): AppResult<Unit> {
        return try {
            //verificamos que el ID del inquilino no esté vacío o en blanco
            if (idInquilino.isBlank()) {
                // Si el ID es inválido, detenemos la ejecución y devolvemos un error
                return AppResult.Error(
                    Exception("El ID del inquilino es obligatorio.")
                )
            }
            // 2. Apuntamos al documento  en Firestore usando su ID único
            collection
                .document(idInquilino)
                // en lugar de reescribir todo el documento,
                // modificamos únicamente el campo "active" cambiándolo a falso (soft delete / dar de baja)
                .update("active", false)
                // 4. Esperamos que la operación en la nube se complete con éxito
                .await()
            // Devolvemos un resultado exitoso envolviendo 'Unit'
            AppResult.Exito(Unit)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }

    // Función asíncrona que busca un inquilino por su número de DNI.
// Devuelve un AppResult que puede contener un Inquilino (si lo encuentra) o null (si no existe).
    suspend fun buscarPorDniCualquiera(dni: String): AppResult<Inquilino?> {
        return try {
            // Consultamos a Firestore filtrando por el campo "dni",
            // limitando el resultado a 1 solo documento para optimizar la búsqueda.
            val snapshot = collection
                .whereEqualTo("dni", dni)
                .limit(1)
                .get()
                .await()

            // Tomamos el primer documento que arroje la consulta (si existe) y lo convertimos a un objeto Inquilino;
            // si la lista está vacía, devuelve null automáticamente usando firstOrNull().
            val inquilino = snapshot.documents.firstOrNull()?.toObject(Inquilino::class.java)

            // Si todo sale bien, devolvemos el resultado envuelto en la clase de éxito
            AppResult.Exito(inquilino)

        } catch (error: Exception) {
            // Si ocurre un fallo de red o conexión, atrapamos el error de forma segura
            AppResult.Error(error)
        }
    }

    suspend fun reactivarYActualizar(inquilino: Inquilino): AppResult<Inquilino> {
        return try {
            // 1. Creamos una copia del inquilino cambiando su estado 'active' a verdadero (reactivándolo)
            val inquilinoReactivado = inquilino.copy(active = true)

            // 2. Apuntamos directamente al documento existente en Firestore usando su ID único original
            collection
                .document(inquilinoReactivado.idInquilino)

                // 3. Sobrescribimos el documento con los nuevos datos actualizados y la bandera 'active = true'
                .set(inquilinoReactivado)

                // 4. Esperamos de forma asíncrona a que Firebase confirme la actualización en la nube
                .await()

            // 5. Si todo sale bien, devolvemos un contenedor de éxito con el inquilino ya reactivado
            AppResult.Exito(inquilinoReactivado)
        } catch (error: Exception) {
            AppResult.Error(error)
        }
    }
}