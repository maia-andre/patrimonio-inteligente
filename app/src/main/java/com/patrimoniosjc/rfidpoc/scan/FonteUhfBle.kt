package com.patrimoniosjc.rfidpoc.scan

import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.interpretarPayloadUhf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fonte de leitura do modo RFID UHF sobre o BleManager existente, sem
 * reescrevê-lo: recebe os comandos e mensagens por lambdas, preservando o
 * envio de LED_ON/LED_OFF e a remontagem de fragmentos pelo `__END__`.
 * As mensagens chegam de thread do binder BLE; o canal absorve isso.
 */
class FonteUhfBle(
    private val enviarComando: (String) -> Unit,
    private val aoDesligarScanner: () -> Unit = {},
    private val relogio: () -> Long = System::currentTimeMillis
) : FonteDeLeitura {

    private val remontador = RemontadorDeFragmentos()
    private val canal = MutableSharedFlow<LeituraPatrimonial>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val leituras: Flow<LeituraPatrimonial> = canal.asSharedFlow()

    override fun iniciar() {
        enviarComando(COMANDO_INICIAR)
    }

    override fun parar() {
        enviarComando(COMANDO_PARAR)
    }

    /** Ponto de entrada das mensagens vindas do BleManager (onMessageReceived). */
    fun aoReceberMensagem(mensagem: String) {
        if (mensagem == MENSAGEM_SCANNER_DESLIGADO) {
            aoDesligarScanner()
            return
        }
        val payload = remontador.receber(mensagem) ?: return
        canal.tryEmit(interpretarPayloadUhf(payload, relogio()))
    }

    companion object {
        // Débito técnico registrado na spec: o firmware ainda chama de LED_* o que é SCAN_*
        const val COMANDO_INICIAR = "LED_ON"
        const val COMANDO_PARAR = "LED_OFF"
        const val MENSAGEM_SCANNER_DESLIGADO = "SCANNER_OFF"
    }
}
