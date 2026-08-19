package com.patrimoniosjc.rfidpoc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-04/REQ-05, RN-01, RN-06 — acúmulo com deduplicação por `codigo ?: bruto`
 * e ordem cronológica inversa. Dados fictícios (RNF-03).
 */
class AcumuladorDeLeiturasTest {

    private fun leitura(
        codigo: String? = null,
        bruto: String,
        origem: OrigemLeitura = OrigemLeitura.RFID_UHF,
        instante: Long = 1_000L
    ) = LeituraPatrimonial(
        codigo = codigo,
        descricao = null,
        origem = origem,
        bruto = bruto,
        instante = instante
    )

    @Test
    fun `chave de deduplicacao e o codigo quando presente e o bruto quando nulo`() {
        assertEquals("147258", leitura(codigo = "147258", bruto = "147258;X").chave)
        assertEquals("payload sem separador", leitura(codigo = null, bruto = "payload sem separador").chave)
    }

    @Test
    fun `leitura nova entra no topo da lista`() {
        val primeira = leitura(codigo = "147258", bruto = "147258;A", instante = 1_000L)
        val segunda = leitura(codigo = "369852", bruto = "369852;B", instante = 2_000L)

        val depoisDaPrimeira = AcumuladorDeLeituras.acumular(emptyList(), primeira)
        val depoisDaSegunda = AcumuladorDeLeituras.acumular(depoisDaPrimeira.lista, segunda)

        assertFalse(depoisDaSegunda.jaConferida)
        assertEquals(listOf(segunda, primeira), depoisDaSegunda.lista)
    }

    @Test
    fun `chave repetida nao gera nova linha e marca ja conferida`() {
        val original = leitura(codigo = "147258", bruto = "147258;Notebook", instante = 1_000L)
        val repetida = leitura(codigo = "147258", bruto = "147258;Notebook", instante = 5_000L)

        val lista = AcumuladorDeLeituras.acumular(emptyList(), original).lista
        val resultado = AcumuladorDeLeituras.acumular(lista, repetida)

        assertTrue(resultado.jaConferida)
        assertEquals(1, resultado.lista.size)
        assertEquals(original, resultado.lista.single())
    }

    @Test
    fun `leituras sem codigo deduplicam pelo bruto`() {
        val original = leitura(codigo = null, bruto = "Cadeira giratoria cinza")
        val repetida = leitura(codigo = null, bruto = "Cadeira giratoria cinza", instante = 9_000L)

        val lista = AcumuladorDeLeituras.acumular(emptyList(), original).lista
        val resultado = AcumuladorDeLeituras.acumular(lista, repetida)

        assertTrue(resultado.jaConferida)
        assertEquals(1, resultado.lista.size)
    }

    @Test
    fun `origens diferentes com a mesma chave sao o mesmo bem`() {
        val porBarras = leitura(codigo = "147258", bruto = "147258", origem = OrigemLeitura.CODIGO_BARRAS)
        val porNfc = leitura(codigo = "147258", bruto = "147258", origem = OrigemLeitura.NFC, instante = 2_000L)

        val lista = AcumuladorDeLeituras.acumular(emptyList(), porBarras).lista
        val resultado = AcumuladorDeLeituras.acumular(lista, porNfc)

        assertTrue(resultado.jaConferida)
        assertEquals(1, resultado.lista.size)
    }

    @Test
    fun `codigos distintos de origens diferentes geram duas linhas`() {
        val porBarras = leitura(codigo = "147258", bruto = "147258", origem = OrigemLeitura.CODIGO_BARRAS)
        val porNfc = leitura(codigo = "369852", bruto = "369852", origem = OrigemLeitura.NFC, instante = 2_000L)

        val lista = AcumuladorDeLeituras.acumular(emptyList(), porBarras).lista
        val resultado = AcumuladorDeLeituras.acumular(lista, porNfc)

        assertFalse(resultado.jaConferida)
        assertEquals(2, resultado.lista.size)
        assertEquals(OrigemLeitura.NFC, resultado.lista[0].origem)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, resultado.lista[1].origem)
    }
}
