package com.patrimoniosjc.rfidpoc.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-01 — o contrato de FonteDeLeitura é implementável e exercitável
 * sem nenhuma API Android, com uma fonte falsa. Dados fictícios (RNF-03).
 */
class FonteDeLeituraTest {

    private class FonteFalsa(private val emitidas: List<LeituraPatrimonial>) : FonteDeLeitura {
        var iniciada = false
            private set
        var parada = false
            private set

        override fun iniciar() {
            iniciada = true
        }

        override fun parar() {
            parada = true
        }

        override val leituras: Flow<LeituraPatrimonial> = flowOf(*emitidas.toTypedArray())
    }

    @Test
    fun `fonte falsa implementa o contrato e emite leituras pelo flow`() = runBlocking {
        val esperadas = listOf(
            LeituraPatrimonial(
                codigo = "147258",
                descricao = "Notebook Positivo",
                origem = OrigemLeitura.RFID_UHF,
                bruto = "147258;Notebook Positivo",
                instante = 1_000L
            ),
            LeituraPatrimonial(
                codigo = "369852",
                descricao = null,
                origem = OrigemLeitura.NFC,
                bruto = "369852",
                instante = 2_000L
            )
        )
        val fonte = FonteFalsa(esperadas)

        assertFalse(fonte.iniciada)
        fonte.iniciar()
        assertTrue(fonte.iniciada)

        val recebidas = fonte.leituras.toList()
        assertEquals(esperadas, recebidas)

        assertFalse(fonte.parada)
        fonte.parar()
        assertTrue(fonte.parada)
    }

    @Test
    fun `origem de leitura tem exatamente os tres modos`() {
        assertEquals(
            listOf("CODIGO_BARRAS", "NFC", "RFID_UHF"),
            OrigemLeitura.entries.map { it.name }
        )
    }
}
