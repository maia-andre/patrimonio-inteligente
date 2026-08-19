package com.patrimoniosjc.rfidpoc.scan

import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-08 — a fonte UHF embrulha o BleManager por lambdas, preservando
 * LED_ON/LED_OFF e a remontagem por `__END__`. Dados fictícios (RNF-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FonteUhfBleTest {

    private val comandosEnviados = mutableListOf<String>()

    private fun novaFonte(
        aoDesligarScanner: () -> Unit = {},
        relogio: () -> Long = { 1_000L }
    ) = FonteUhfBle(
        enviarComando = { comandosEnviados.add(it) },
        aoDesligarScanner = aoDesligarScanner,
        relogio = relogio
    )

    @Test
    fun `iniciar envia LED_ON e parar envia LED_OFF`() {
        val fonte = novaFonte()

        fonte.iniciar()
        fonte.parar()

        assertEquals(listOf("LED_ON", "LED_OFF"), comandosEnviados)
    }

    @Test
    fun `fragmentos remontados viram leitura interpretada pela RN-03`() = runTest {
        val fonte = novaFonte(relogio = { 777L })
        val recebidas = mutableListOf<LeituraPatrimonial>()
        val coleta = launch(UnconfinedTestDispatcher(testScheduler)) {
            fonte.leituras.collect { recebidas.add(it) }
        }

        fonte.aoReceberMensagem("147258;Note")
        fonte.aoReceberMensagem("book Positivo")
        fonte.aoReceberMensagem("__END__")
        advanceUntilIdle()

        assertEquals(1, recebidas.size)
        val leitura = recebidas.single()
        assertEquals("147258", leitura.codigo)
        assertEquals("Notebook Positivo", leitura.descricao)
        assertEquals("147258;Notebook Positivo", leitura.bruto)
        assertEquals(OrigemLeitura.RFID_UHF, leitura.origem)
        assertEquals(777L, leitura.instante)
        coleta.cancel()
    }

    @Test
    fun `mensagem SCANNER_OFF aciona o aviso e nao vira leitura`() = runTest {
        var avisado = false
        val fonte = novaFonte(aoDesligarScanner = { avisado = true })
        val recebidas = mutableListOf<LeituraPatrimonial>()
        val coleta = launch(UnconfinedTestDispatcher(testScheduler)) {
            fonte.leituras.collect { recebidas.add(it) }
        }

        fonte.aoReceberMensagem("SCANNER_OFF")
        advanceUntilIdle()

        assertTrue(avisado)
        assertTrue(recebidas.isEmpty())
        coleta.cancel()
    }

    @Test
    fun `marcador de fim sem fragmentos nao emite leitura`() = runTest {
        val fonte = novaFonte()
        val recebidas = mutableListOf<LeituraPatrimonial>()
        val coleta = launch(UnconfinedTestDispatcher(testScheduler)) {
            fonte.leituras.collect { recebidas.add(it) }
        }

        fonte.aoReceberMensagem("__END__")
        advanceUntilIdle()

        assertTrue(recebidas.isEmpty())
        coleta.cancel()
    }

    @Test
    fun `mensagens sucessivas geram leituras independentes`() = runTest {
        val fonte = novaFonte()
        val recebidas = mutableListOf<LeituraPatrimonial>()
        val coleta = launch(UnconfinedTestDispatcher(testScheduler)) {
            fonte.leituras.collect { recebidas.add(it) }
        }

        fonte.aoReceberMensagem("147258;Notebook Positivo")
        fonte.aoReceberMensagem("__END__")
        fonte.aoReceberMensagem("Cadeira giratoria cinza")
        fonte.aoReceberMensagem("__END__")
        advanceUntilIdle()

        assertEquals(2, recebidas.size)
        assertEquals("147258", recebidas[0].codigo)
        assertEquals(null, recebidas[1].codigo)
        assertEquals("Cadeira giratoria cinza", recebidas[1].descricao)
        coleta.cancel()
    }
}
