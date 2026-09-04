package com.patrimoniosjc.rfidpoc.scan

import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.interpretarCodigoManual
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fonte de leitura do lançamento manual: a tela chama [registrar] com o que
 * o usuário digitou e a leitura segue pela mesma porta das outras origens —
 * mesma lista, mesma deduplicação (RN-01). Não há hardware para iniciar ou
 * parar; o ciclo de vida é só o contrato.
 */
class FonteManual(
    private val relogio: () -> Long = System::currentTimeMillis
) : FonteDeLeitura {

    private val canal = MutableSharedFlow<LeituraPatrimonial>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val leituras: Flow<LeituraPatrimonial> = canal.asSharedFlow()

    /** Devolve true quando o texto virou leitura; em branco, false e nada é emitido. */
    fun registrar(texto: String): Boolean {
        val leitura = interpretarCodigoManual(texto, relogio()) ?: return false
        return canal.tryEmit(leitura)
    }

    override fun iniciar() = Unit

    override fun parar() = Unit
}
