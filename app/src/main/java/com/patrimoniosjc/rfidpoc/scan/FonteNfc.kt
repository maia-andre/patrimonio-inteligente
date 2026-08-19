package com.patrimoniosjc.rfidpoc.scan

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.RegistroNdef
import com.patrimoniosjc.rfidpoc.domain.interpretarEtiquetaNfc
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fonte de leitura do modo NFC em primeiro plano, via
 * `NfcAdapter.enableReaderMode`, aceitando NfcA/NfcB/NfcF/NfcV (REQ-07).
 * A etiqueta detectada é reduzida a bytes e interpretada pela função pura
 * `interpretarEtiquetaNfc` (RN-02), coberta por teste unitário. O callback
 * do reader mode chega em thread do sistema; o canal absorve isso.
 */
class FonteNfc(
    private val atividade: Activity,
    private val relogio: () -> Long = System::currentTimeMillis
) : FonteDeLeitura {

    private val canal = MutableSharedFlow<LeituraPatrimonial>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val leituras: Flow<LeituraPatrimonial> = canal.asSharedFlow()

    override fun iniciar() {
        val adaptador = NfcAdapter.getDefaultAdapter(atividade) ?: return
        adaptador.enableReaderMode(
            atividade,
            ::aoDetectarEtiqueta,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    /** REQ-10/CE-10/CE-13 — parar libera a antena para o sistema. */
    override fun parar() {
        NfcAdapter.getDefaultAdapter(atividade)?.disableReaderMode(atividade)
    }

    private fun aoDetectarEtiqueta(etiqueta: Tag) {
        val uid = etiqueta.id ?: ByteArray(0)
        canal.tryEmit(interpretarEtiquetaNfc(registrosDe(etiqueta), uid, relogio()))
    }

    /** Reduz a mensagem NDEF (se houver) aos bytes que a RN-02 precisa; sem NDEF, lista vazia (CE-05). */
    private fun registrosDe(etiqueta: Tag): List<RegistroNdef> {
        val mensagem = Ndef.get(etiqueta)?.cachedNdefMessage ?: return emptyList()
        return mensagem.records.map { RegistroNdef(it.tnf, it.type, it.payload) }
    }
}
