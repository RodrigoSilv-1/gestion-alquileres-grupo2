package com.example.gestionalquileres.domain.model

import com.example.gestionalquileres.domain.normalizarBusqueda

/** Los 43 distritos de Lima Metropolitana (provincia de Lima), en orden alfabético. */
val DISTRITOS_LIMA: List<String> = listOf(
    "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Chaclacayo", "Chorrillos",
    "Cieneguilla", "Comas", "El Agustino", "Independencia", "Jesús María", "La Molina",
    "La Victoria", "Lima", "Lince", "Los Olivos", "Lurigancho-Chosica", "Lurín",
    "Magdalena del Mar", "Miraflores", "Pachacámac", "Pucusana", "Pueblo Libre",
    "Puente Piedra", "Punta Hermosa", "Punta Negra", "Rímac", "San Bartolo", "San Borja",
    "San Isidro", "San Juan de Lurigancho", "San Juan de Miraflores", "San Luis",
    "San Martín de Porres", "San Miguel", "Santa Anita", "Santa María del Mar",
    "Santa Rosa", "Santiago de Surco", "Surquillo", "Villa El Salvador",
    "Villa María del Triunfo"
)

private val DISTRITOS_POR_CLAVE = DISTRITOS_LIMA.associateBy { it.normalizarBusqueda() }

/**
 * Convierte un texto antiguo (ej. "miraflores", "RIMAC") al nombre oficial de la lista.
 * Devuelve null si no coincide con ningún distrito (ej. "Surco").
 */
fun String.aDistritoLimaOficial(): String? = DISTRITOS_POR_CLAVE[normalizarBusqueda()]