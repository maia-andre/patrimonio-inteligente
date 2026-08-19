package com.patrimoniosjc.rfidpoc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * REQ-06/RNF-02 — o texto decodificado do código de barras vira
 * LeituraPatrimonial por função pura. Dados fictícios (RNF-03).
 */
class InterpretadorCodigoBarrasTest {

    @Test
    fun `texto decodificado vira codigo e bruto preserva o original`() {
        val leitura = interpretarCodigoBarras("147258", 1_000L)

        assertEquals("147258", leitura.codigo)
        assertEquals("147258", leitura.bruto)
        assertNull(leitura.descricao)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, leitura.origem)
        assertEquals(1_000L, leitura.instante)
    }

    @Test
    fun `texto vazio vira codigo nulo e a chave cai para o bruto`() {
        val leitura = interpretarCodigoBarras("", 2_000L)

        assertNull(leitura.codigo)
        assertEquals("", leitura.bruto)
        assertEquals("", leitura.chave)
    }

    @Test
    fun `chave de deduplicacao e o proprio codigo`() {
        val leitura = interpretarCodigoBarras("PATR-369852", 3_000L)

        assertEquals("PATR-369852", leitura.chave)
    }
}
