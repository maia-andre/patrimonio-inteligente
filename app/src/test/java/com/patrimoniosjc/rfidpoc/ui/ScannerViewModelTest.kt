package com.patrimoniosjc.rfidpoc.ui

import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * REQ-02 — o ScannerViewModel controla a fonte e expõe o estado da tela,
 * verificável com fonte falsa, sem aparelho. Dados fictícios (RNF-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FonteFalsa : FonteDeLeitura {
        val canal = MutableSharedFlow<LeituraPatrimonial>(extraBufferCapacity = 8)
        var iniciadas = 0
            private set
        var paradas = 0
            private set

        override fun iniciar() {
            iniciadas++
        }

        override fun parar() {
            paradas++
        }

        override val leituras: Flow<LeituraPatrimonial> = canal
    }

    private lateinit var fonte: FonteFalsa
    private var conexoesPedidas = 0
    private var desconexoesPedidas = 0

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        fonte = FonteFalsa()
        conexoesPedidas = 0
        desconexoesPedidas = 0
    }

    @After
    fun desmontar() {
        Dispatchers.resetMain()
    }

    private fun novoViewModel() = ScannerViewModel(
        fonte = fonte,
        conectar = { conexoesPedidas++ },
        desconectar = { desconexoesPedidas++ },
        relogio = { 1_755_500_000_000L }
    )

    @Test
    fun `leitura emitida pela fonte aparece no estado da tela`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle() // garante que o coletor do init já se inscreveu na fonte
        val leitura = LeituraPatrimonial(
            codigo = "147258",
            descricao = "Notebook Positivo",
            origem = OrigemLeitura.RFID_UHF,
            bruto = "147258;Notebook Positivo",
            instante = 1_000L
        )

        fonte.canal.emit(leitura)
        advanceUntilIdle()

        assertEquals(leitura, viewModel.estado.value.ultimaLeitura)
    }

    @Test
    fun `iniciar e parar leitura delegam a fonte ativa`() {
        val viewModel = novoViewModel()

        viewModel.iniciarLeitura()
        viewModel.pararLeitura()

        assertEquals(1, fonte.iniciadas)
        assertEquals(1, fonte.paradas)
    }

    @Test
    fun `alternar conexao desconectado pede conexao e conectado pede desconexao`() {
        val viewModel = novoViewModel()

        viewModel.alternarConexao()
        assertEquals(1, conexoesPedidas)
        assertEquals(0, desconexoesPedidas)

        viewModel.atualizarConexao(true)
        viewModel.alternarConexao()
        assertEquals(1, conexoesPedidas)
        assertEquals(1, desconexoesPedidas)
    }

    @Test
    fun `atualizar conexao reflete no estado e no texto de status`() {
        val viewModel = novoViewModel()

        assertFalse(viewModel.estado.value.conectado)
        assertEquals("Desconectado", viewModel.estado.value.statusTexto)

        viewModel.atualizarConexao(true)
        assertTrue(viewModel.estado.value.conectado)
        assertEquals("Scanner Conectado", viewModel.estado.value.statusTexto)

        viewModel.atualizarConexao(false)
        assertFalse(viewModel.estado.value.conectado)
        assertEquals("Desconectado", viewModel.estado.value.statusTexto)
    }

    @Test
    fun `aviso de scanner desligado muda o status e registra log`() {
        val viewModel = novoViewModel()

        viewModel.scannerDesligado()

        assertEquals("Scanner Desligado", viewModel.estado.value.statusTexto)
        assertTrue(viewModel.estado.value.logs.first().contains("Scanner desligado"))
    }

    @Test
    fun `log mais recente fica no topo`() {
        val viewModel = novoViewModel()

        viewModel.registrarLog("primeira mensagem")
        viewModel.registrarLog("segunda mensagem")

        assertTrue(viewModel.estado.value.logs[0].contains("segunda mensagem"))
        assertTrue(viewModel.estado.value.logs[1].contains("primeira mensagem"))
    }
}
