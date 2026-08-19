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

    // ---- INC-03: lista com deduplicação e contador ----

    private fun leituraDe(
        codigo: String?,
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
    fun `estado inicial tem lista vazia para a tela mostrar o estado vazio`() {
        val viewModel = novoViewModel()

        assertTrue(viewModel.estado.value.leituras.isEmpty())
    }

    @Test
    fun `leituras de chaves distintas acumulam com a mais recente no topo`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle()

        fonte.canal.emit(leituraDe("147258", "147258", OrigemLeitura.CODIGO_BARRAS, 1_000L))
        fonte.canal.emit(leituraDe("369852", "369852", OrigemLeitura.NFC, 2_000L))
        advanceUntilIdle()

        val leituras = viewModel.estado.value.leituras
        assertEquals(2, leituras.size)
        assertEquals("369852", leituras[0].codigo)
        assertEquals(OrigemLeitura.NFC, leituras[0].origem)
        assertEquals("147258", leituras[1].codigo)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, leituras[1].origem)
    }

    @Test
    fun `chave repetida nao gera linha nova e sinaliza ja conferido`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle()

        fonte.canal.emit(leituraDe("147258", "147258;Notebook Positivo", instante = 1_000L))
        fonte.canal.emit(leituraDe("147258", "147258;Notebook Positivo", instante = 2_500L))
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(1, estado.leituras.size)
        assertTrue(estado.avisoJaConferido!!.contains("147258"))
    }

    @Test
    fun `sinalizacao de duplicata e limitada a uma por segundo para a mesma chave`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle()

        fonte.canal.emit(leituraDe("147258", "147258", instante = 1_000L))
        fonte.canal.emit(leituraDe("147258", "147258", instante = 2_000L)) // sinaliza
        fonte.canal.emit(leituraDe("147258", "147258", instante = 2_300L)) // bloqueada
        fonte.canal.emit(leituraDe("147258", "147258", instante = 3_100L)) // sinaliza de novo
        advanceUntilIdle()

        val sinalizacoes = viewModel.estado.value.logs.count { it.contains("já conferido") }
        assertEquals(2, sinalizacoes)
        assertEquals(1, viewModel.estado.value.leituras.size)
    }

    @Test
    fun `leitura nova limpa o aviso de ja conferido`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle()

        fonte.canal.emit(leituraDe("147258", "147258", instante = 1_000L))
        fonte.canal.emit(leituraDe("147258", "147258", instante = 2_000L))
        advanceUntilIdle()
        assertTrue(viewModel.estado.value.avisoJaConferido != null)

        fonte.canal.emit(leituraDe("369852", "369852", instante = 3_000L))
        advanceUntilIdle()
        assertEquals(null, viewModel.estado.value.avisoJaConferido)
        assertEquals(2, viewModel.estado.value.leituras.size)
    }
}
