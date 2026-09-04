package com.patrimoniosjc.rfidpoc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Lançamento manual — o texto digitado vira LeituraPatrimonial por função
 * pura (RNF-02); em branco não é leitura. Dados fictícios (RNF-03).
 */
class InterpretadorCodigoManualTest {

    @Test
    fun `texto digitado vira codigo e bruto, sem os espacos das pontas`() {
        val leitura = interpretarCodigoManual("  PATR-147258 ", 1_000L)!!

        assertEquals("PATR-147258", leitura.codigo)
        assertEquals("PATR-147258", leitura.bruto)
        assertNull(leitura.descricao)
        assertEquals(OrigemLeitura.MANUAL, leitura.origem)
        assertEquals(1_000L, leitura.instante)
    }

    @Test
    fun `texto em branco nao vira leitura`() {
        assertNull(interpretarCodigoManual("", 1L))
        assertNull(interpretarCodigoManual("   ", 1L))
    }

    @Test
    fun `chave de deduplicacao bate com a mesma chave lida por outra origem`() {
        val manual = interpretarCodigoManual("PATR-147258", 1L)!!
        val barras = interpretarCodigoBarras("PATR-147258", 2L)

        assertEquals(barras.chave, manual.chave)
    }
}
