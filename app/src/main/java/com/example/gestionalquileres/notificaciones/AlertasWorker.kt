package com.example.gestionalquileres.notificaciones

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.gestionalquileres.data.auth.AuthRepository
import com.example.gestionalquileres.domain.model.AppResult
import com.example.gestionalquileres.domain.model.Contrato
import com.example.gestionalquileres.domain.model.Recibo
import com.example.gestionalquileres.domain.model.UserRole
import com.example.gestionalquileres.domain.notificaciones.Alerta
import com.example.gestionalquileres.domain.notificaciones.EvaluadorAlertas
import com.example.gestionalquileres.domain.notificaciones.ReglasNotificacion
import com.example.gestionalquileres.domain.notificaciones.TipoAlerta
import com.example.gestionalquileres.domain.repository.ContratoRepository
import com.example.gestionalquileres.domain.repository.ReciboRepository
import com.example.gestionalquileres.ui.navigation.Rutas
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class AlertasWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val sesionAlertas: SesionAlertas,
    private val reciboRepository: ReciboRepository,
    private val contratoRepository: ContratoRepository,
    private val evaluador: EvaluadorAlertas,
    private val registroAvisos: RegistroAvisos,
    private val notificacionHelper: NotificacionHelper,
    private val programador: ProgramadorAlertas
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. ¿Hay sesión y sabemos el rol?
        val uid = authRepository.getCurrentUserId() ?: run {
            // NUEVO: sin sesión no tiene sentido seguir revisando cada día.
            programador.desactivar()
            return terminar("Sin sesión activa: se apagó la revisión diaria.")
        }
        val rol = sesionAlertas.rolDe(uid)
            ?: return terminar("No hay rol guardado. Cierra la app, ábrela de nuevo o vuelve a iniciar sesión.")

        // 2. Cargar solo los datos que ese rol necesita.
        val recibos: List<Recibo> = if (rol == UserRole.SECRETARIO) {
            when (val resultado = reciboRepository.obtenerTodosLosRecibos()) {
                is AppResult.Exito -> resultado.valor
                is AppResult.Error -> return reintentar(
                    "No se pudieron leer los recibos: ${resultado.excepcion.message}"
                )
            }
        } else {
            emptyList()
        }

        val contratos: List<Contrato> = when (val resultado = contratoRepository.obtenerContratosActivos()) {
            is AppResult.Exito -> resultado.valor
            is AppResult.Error ->
                if (rol == UserRole.ADMIN) {
                    return reintentar("No se pudieron leer los contratos: ${resultado.excepcion.message}")
                } else {
                    Log.w(TAG, "Contratos no disponibles; se omite el recordatorio de emisión")
                    emptyList()
                }
        }

        // 3. Evaluar, quitar lo ya avisado y publicar.
        registroAvisos.limpiarAntiguas()
        val alertas = evaluador.evaluar(rol, recibos, contratos, LocalDate.now())
        val nuevas = alertas.filterNot { registroAvisos.yaAvisada(it.clave) }
        val mostradas = publicar(nuevas)

        val bloqueadas = nuevas.isNotEmpty() && mostradas == 0

        // Anota que hoy ya se revisó, salvo que las notificaciones estén bloqueadas:
        // si el usuario las activa más tarde, se reintentará al abrir la app.
        if (!bloqueadas) sesionAlertas.marcarRevision(LocalDate.now().toEpochDay())

        val avisoBloqueo = if (bloqueadas) " · notificaciones bloqueadas" else ""
        return terminar(
            "Rol $rol · ${recibos.size} recibos · ${contratos.size} contratos · " +
                    "${alertas.size} alertas · ${nuevas.size} nuevas · $mostradas mostradas$avisoBloqueo"
        )
    }

    /** Publica por canal: pocas alertas van sueltas, muchas se agrupan en una sola notificación. */
    private fun publicar(nuevas: List<Alerta>): Int =
        nuevas.groupBy { it.tipo.canal() }.entries.sumOf { (canal, grupo) ->
            if (grupo.size <= ReglasNotificacion.MAX_AVISOS_INDIVIDUALES) {
                publicarIndividuales(grupo)
            } else {
                publicarResumen(canal, grupo)
            }
        }

    private fun publicarIndividuales(grupo: List<Alerta>): Int {
        var mostradas = 0
        for (alerta in grupo) {
            val ok = notificacionHelper.mostrar(
                canal = alerta.tipo.canal(),
                idNotificacion = IdsNotificacion.de(alerta),
                titulo = alerta.titulo,
                mensaje = alerta.mensaje,
                destino = alerta.tipo.destino() // NUEVO
            )
            if (!ok) break // sin permiso: no insistir ni marcar como avisada
            registroAvisos.marcarAvisada(alerta.clave)
            mostradas++
        }
        return mostradas
    }

    private fun publicarResumen(canal: CanalNotificacion, grupo: List<Alerta>): Int {
        val ordenadas = grupo.sortedBy { it.tipo.prioridad() }
        val (idNotificacion, titulo) = when (canal) {
            CanalNotificacion.RECIBOS ->
                IdsNotificacion.RESUMEN_RECIBOS to "Tienes ${grupo.size} avisos de recibos"
            CanalNotificacion.CONTRATOS ->
                IdsNotificacion.RESUMEN_CONTRATOS to "Tienes ${grupo.size} avisos de contratos"
        }

        val ok = notificacionHelper.mostrarResumen(
            canal = canal,
            idNotificacion = idNotificacion,
            titulo = titulo,
            lineas = ordenadas.map { it.mensaje },
            destino = canal.destino() // NUEVO
        )
        if (!ok) return 0

        ordenadas.forEach { registroAvisos.marcarAvisada(it.clave) }
        return grupo.size
    }

    private fun TipoAlerta.canal(): CanalNotificacion = when (this) {
        TipoAlerta.RECIBO_POR_VENCER,
        TipoAlerta.RECIBO_VENCIDO,
        TipoAlerta.EMITIR_RECIBOS -> CanalNotificacion.RECIBOS

        TipoAlerta.CONTRATO_POR_VENCER,
        TipoAlerta.CONTRATO_VENCIDO -> CanalNotificacion.CONTRATOS
    }

    /** Pantalla que se abre al tocar la notificación. */
    private fun TipoAlerta.destino(): String = when (this) {
        TipoAlerta.RECIBO_POR_VENCER,
        TipoAlerta.RECIBO_VENCIDO,
        TipoAlerta.EMITIR_RECIBOS -> Rutas.RECIBOS

        TipoAlerta.CONTRATO_POR_VENCER,
        TipoAlerta.CONTRATO_VENCIDO -> Rutas.CONTRATOS
    }

    private fun CanalNotificacion.destino(): String = when (this) {
        CanalNotificacion.RECIBOS -> Rutas.RECIBOS
        CanalNotificacion.CONTRATOS -> Rutas.CONTRATOS
    }

    /** Lo más urgente primero en el resumen. */
    private fun TipoAlerta.prioridad(): Int = when (this) {
        TipoAlerta.RECIBO_VENCIDO, TipoAlerta.CONTRATO_VENCIDO -> 0
        TipoAlerta.RECIBO_POR_VENCER, TipoAlerta.CONTRATO_POR_VENCER -> 1
        TipoAlerta.EMITIR_RECIBOS -> 2
    }

    private fun terminar(texto: String): Result {
        Log.i(TAG, texto)
        return Result.success(workDataOf(CLAVE_RESUMEN to texto))
    }

    /** Reintenta unas pocas veces (por ejemplo, si el servidor falló) y luego se rinde por hoy. */
    private fun reintentar(motivo: String): Result {
        Log.w(TAG, motivo)
        return if (runAttemptCount < MAX_REINTENTOS) Result.retry()
        else terminar("Se omitió la revisión: $motivo")
    }

    companion object {
        const val CLAVE_RESUMEN = "resumen"
        private const val TAG = "AlertasWorker"
        private const val MAX_REINTENTOS = 2
    }
}