package com.patrimoniosjc.rfidpoc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * RN-03 — interpretação do payload UHF remontado.
 * Todos os dados destes testes são fictícios (RNF-03).
 */
class InterpretadorPayloadUhfTest {

    @Test
    fun `payload com separador produz codigo e descricao`() {
        val leitura = interpretarPayloadUhf("147258;Notebook Positivo", instante = 1_000L)

        assertEquals("147258", leitura.codigo)
        assertEquals("Notebook Positivo", leitura.descricao)
        assertEquals("147258;Notebook Positivo", leitura.bruto)
    }

    @Test
    fun `payload sem separador produz codigo nulo e payload inteiro na descricao`() {
        // CE-07 — formato atual do firmware
        val leitura = interpretarPayloadUhf("Cadeira giratoria cinza", instante = 1_000L)

        assertNull(leitura.codigo)
        assertEquals("Cadeira giratoria cinza", leitura.descricao)
        assertEquals("Cadeira giratoria cinza", leitura.bruto)
    }

    @Test
    fun `payload com descricao vazia produz descricao nula`() {
        // CE-08
        val leitura = interpretarPayloadUhf("147258;", instante = 1_000L)

        assertEquals("147258", leitura.codigo)
        assertNull(leitura.descricao)
        assertEquals("147258;", leitura.bruto)
    }

    @Test
    fun `apenas o primeiro separador divide o payload`() {
        // CE-09
        val leitura = interpretarPayloadUhf("147258;Notebook;fonte 90W", instante = 1_000L)

        assertEquals("147258", leitura.codigo)
        assertEquals("Notebook;fonte 90W", leitura.descricao)
        assertEquals("147258;Notebook;fonte 90W", leitura.bruto)
    }

    @Test
    fun `codigo vazio antes do separador vira nulo`() {
        // Coerente com CE-08 e DT-02: vazio não é código, é ausência
        val leitura = interpretarPayloadUhf(";Notebook Positivo", instante = 1_000L)

        assertNull(leitura.codigo)
        assertEquals("Notebook Positivo", leitura.descricao)
        assertEquals(";Notebook Positivo", leitura.bruto)
    }

    @Test
    fun `payload vazio produz codigo e descricao nulos com bruto preservado`() {
        val leitura = interpretarPayloadUhf("", instante = 1_000L)

        assertNull(leitura.codigo)
        assertNull(leitura.descricao)
        assertEquals("", leitura.bruto)
    }

    @Test
    fun `payload que e so o separador produz codigo e descricao nulos`() {
        val leitura = interpretarPayloadUhf(";", instante = 1_000L)

        assertNull(leitura.codigo)
        assertNull(leitura.descricao)
        assertEquals(";", leitura.bruto)
    }

    @Test
    fun `origem e sempre rfid uhf`() {
        assertEquals(OrigemLeitura.RFID_UHF, interpretarPayloadUhf("147258;X", instante = 1_000L).origem)
        assertEquals(OrigemLeitura.RFID_UHF, interpretarPayloadUhf("sem separador", instante = 1_000L).origem)
    }

    @Test
    fun `instante informado e preservado`() {
        val leitura = interpretarPayloadUhf("147258;Notebook Positivo", instante = 987_654_321L)

        assertEquals(987_654_321L, leitura.instante)
    }
}
