package com.example.gestionalquileres.data.repository

import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ContratoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val contratosCollection = firestore.collection("contratos")

    // Función suspendida para registrar un nuevo contrato de manera segura y atómica en Firestore
    suspend fun registrarContrato(contrato: Contrato): AppResult<Contrato> {
        return try {
            // 1. Creamos una referencia a un nuevo documento con un ID automático en la colección de contratos
            val documentoRef = contratosCollection.document()

            // 2. Copiamos el objeto contrato original, asignándole el ID generado por Firestore y activándolo (active = true)
            val contratoConId = contrato.copy(
                idContrato = documentoRef.id,
                active = true
            )

            // 3. Obtenemos la referencia exacta de la unidad alquilable dentro del inmueble correspondiente
            val unidadRef = firestore.collection("inmuebles")
                .document(contrato.inmuebleId)
                .collection("unidadesAlquilables")
                .document(contrato.unidadId)

            // 4. Ejecutamos una transacción de Firestore para garantizar que la validación y el registro ocurran juntos sin interferencias
            firestore.runTransaction { transaction ->
                // Leemos el estado actual de la unidad dentro de la transacción
                val unidadSnapshot = transaction.get(unidadRef)
                val estaOcupada = unidadSnapshot.getBoolean("estaOcupada") ?: false

                // Si la unidad ya está ocupada, lanzamos una excepción para cancelar y revertir la transacción inmediatamente
                if (estaOcupada) {
                    throw IllegalStateException("Esta unidad ya ha sido ocupada por otro contrato.")
                }

                // Si está libre, guardamos el nuevo contrato con su ID
                transaction.set(documentoRef, contratoConId)
                // Y actualizamos el estado de la unidad alquilable para marcarla como ocupada (estaOcupada = true)
                transaction.update(unidadRef, "estaOcupada", true)
            }.await() // Esperamos a que la transacción se complete de forma asíncrona

            // Si todo el proceso se completa sin errores, devolvemos un resultado exitoso con el contrato ya registrado
            AppResult.Exito(contratoConId)
        } catch (e: Exception) {
            // Si ocurre cualquier error de red, base de datos o si la unidad ya estaba ocupada, lo atrapamos y devolvemos como error
            AppResult.Error(e)
        }
    }

    suspend fun obtenerContratosActivos(): AppResult<List<Contrato>> {
        return try {
            val snapshot = contratosCollection
                .whereEqualTo("active", true)
                .get()
                //Esperamos que la operación en la nube se complete exitosamente
                .await()

            val contratos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Contrato::class.java)
            }
            //recorre los registros obtenidos y los transforma en objetos tipo contrato
            AppResult.Exito(contratos)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    // 1. Función para obtener el contrato activo de un inquilino específico
    suspend fun obtenerContratoActivoPorInquilino(inquilinoId: String): AppResult<Contrato?> {
        return try {
            // Consultamos la colección de contratos en Firestore aplicando filtros
            val snapshot = contratosCollection
                .whereEqualTo("inquilinoId", inquilinoId) // Filtramos por el ID del inquilino
                .whereEqualTo("active", true)              // Buscamos solo los contratos que estén activos
                .limit(1)                                    // Limitamos la búsqueda a un solo resultado
                .get()
                .await()                                     // Esperamos el resultado de manera asíncrona

            // Obtenemos el primer documento que coincida y lo transformamos a un objeto de tipo Contrato
            val contrato = snapshot.documents.firstOrNull()?.toObject(Contrato::class.java)

            // Si todo va bien, retornamos el contrato envuelto en un éxito
            AppResult.Exito(contrato)
        } catch (e: Exception) {
            // Si ocurre un error de red o en Firestore, lo atrapamos y devolvemos como error
            AppResult.Error(e)
        }
    }

    // 2. Función para obtener el historial completo de todos los contratos registrados
    suspend fun obtenerHistorialCompleto(): AppResult<List<Contrato>> {
        return try {
            // Descargamos todos los documentos de la colección de contratos sin filtros
            val snapshot = contratosCollection
                .get()
                .await()

            // Recorremos cada documento y lo convertimos a un objeto Contrato, descartando los nulos
            val contratos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Contrato::class.java)
            }

            // Devolvemos la lista completa de contratos con un resultado exitoso
            AppResult.Exito(contratos)
        } catch (e: Exception) {
            // Capturamos cualquier excepción imprevista
            AppResult.Error(e)
        }
    }

    // 3. Función para finalizar un contrato de manera segura usando una transacción de Firestore
    suspend fun finalizarContrato(idContrato: String, inmuebleId: String, unidadId: String): AppResult<Unit> {
        return try {
            // Validamos primero que los identificadores obligatorios no estén vacíos o en blanco
            if (idContrato.isBlank() || inmuebleId.isBlank() || unidadId.isBlank()) {
                return AppResult.Error(Exception("Los identificadores son obligatorios para finalizar el contrato."))
            }

            // Creamos la referencia al documento del contrato que se va a cerrar
            val contratoRef = contratosCollection.document(idContrato)

            // Creamos la referencia a la unidad alquilable específica dentro del inmueble
            val unidadRef = firestore.collection("inmuebles")
                .document(inmuebleId)
                .collection("unidadesAlquilables")
                .document(unidadId)

            // Ejecutamos una transacción atómica: o se actualizan ambas cosas o no se actualiza ninguna
            firestore.runTransaction { transaction ->
                // Actualizamos el estado del contrato a inactivo (active = false)
                transaction.update(contratoRef, "active", false)
                // Actualizamos el estado de la unidad para marcarla como desocupada (estaOcupada = false)
                transaction.update(unidadRef, "estaOcupada", false)
            }.await()

            // Si la transacción se completa con éxito, retornamos Unit indicando que finalizó correctamente
            AppResult.Exito(Unit)
        } catch (e: Exception) {
            // Si la transacción falla por concurrencia o errores de red, retornamos el error
            AppResult.Error(e)
        }
    }
}