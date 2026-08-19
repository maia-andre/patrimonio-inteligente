package com.patrimoniosjc.rfidpoc.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CE-01 — a sinalização de "já conferido" é limitada a uma ocorrência
 * por segundo para o mesmo código.
 */
class LimitadorDeSinalizacaoTest {

    @Test
    fun `primeira sinalizacao de uma chave e permitida`() {
        val limitador = LimitadorDeSinalizacao()

        assertTrue(limitador.deveSinalizar("147258", instante = 10_000L))
    }

    @Test
    fun `sinalizacao repetida dentro de um segundo e bloqueada`() {
        val limitador = LimitadorDeSinalizacao()

        assertTrue(limitador.deveSinalizar("147258", instante = 10_000L))
        assertFalse(limitador.deveSinalizar("147258", instante = 10_400L))
        assertFalse(limitador.deveSinalizar("147258", instante = 10_999L))
    }

    @Test
    fun `apos um segundo a mesma chave pode sinalizar de novo`() {
        val limitador = LimitadorDeSinalizacao()

        assertTrue(limitador.deveSinalizar("147258", instante = 10_000L))
        assertTrue(limitador.deveSinalizar("147258", instante = 11_000L))
    }

    @Test
    fun `chaves diferentes tem limites independentes`() {
        val limitador = LimitadorDeSinalizacao()

        assertTrue(limitador.deveSinalizar("147258", instante = 10_000L))
        assertTrue(limitador.deveSinalizar("369852", instante = 10_100L))
    }
}
