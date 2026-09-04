package com.patrimoniosjc.rfidpoc.scan

import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lançamento manual — a fonte entrega o código digitado pela mesma porta
 * das outras origens (FonteDeLeitura), sem nenhuma API Android.
 * Dados fictícios (RNF-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FonteManualTest {

    @Test
    fun `registrar emite a leitura pelo flow com origem manual`() = runTest {
        val fonte = FonteManual(relogio = { 5_000L })
        val recebidas = mutableListOf<LeituraPatrimonial>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fonte.leituras.collect { recebidas += it }
        }

        assertTrue(fonte.registrar(" PATR-147258 "))

        assertEquals(1, recebidas.size)
        assertEquals("PATR-147258", recebidas[0].codigo)
        assertEquals(OrigemLeitura.MANUAL, recebidas[0].origem)
        assertEquals(5_000L, recebidas[0].instante)
    }

    @Test
    fun `texto em branco nao registra nada`() = runTest {
        val fonte = FonteManual(relogio = { 5_000L })
        val recebidas = mutableListOf<LeituraPatrimonial>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fonte.leituras.collect { recebidas += it }
        }

        assertFalse(fonte.registrar("   "))

        assertTrue(recebidas.isEmpty())
    }

    @Test
    fun `iniciar e parar cumprem o contrato sem efeito colateral`() {
        val fonte = FonteManual()

        fonte.iniciar()
        fonte.parar()
    }
}
