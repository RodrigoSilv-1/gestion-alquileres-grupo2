package com.example.gestionalquileres.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

internal const val COLECCION_OCUPACION = "ocupacion"

/** Cuántas unidades de este inmueble tienen contrato vigente. Si no existe el documento, es 0. */
internal fun FirebaseFirestore.ocupacionInmueble(idInmueble: String): DocumentReference =
    collection(COLECCION_OCUPACION).document("inmueble_$idInmueble")

/** Contrato vigente de este inquilino. Si no existe el documento, no tiene contrato. */
internal fun FirebaseFirestore.ocupacionInquilino(idInquilino: String): DocumentReference =
    collection(COLECCION_OCUPACION).document("inquilino_$idInquilino")