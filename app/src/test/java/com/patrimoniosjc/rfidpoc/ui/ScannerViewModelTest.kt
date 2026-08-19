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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * REQ-02/REQ-03/REQ-10/REQ-11, RN-05 — o ScannerViewModel controla a fonte
 * ativa, o seletor de modos e o ciclo de vida, verificável com fontes falsas.
 * Dados fictícios (RNF-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FonteFalsa(
        private val nome: String = "fonte",
        private val eventos: MutableList<String>? = null
    ) : FonteDeLeitura {
        val canal = MutableSharedFlow<LeituraPatrimonial>(extraBufferCapacity = 8)
        var iniciadas = 0
            private set
        var paradas = 0
            private set

        override fun iniciar() {
            iniciadas++
            eventos?.add("$nome.iniciar")
        }

        override fun parar() {
            paradas++
            eventos?.add("$nome.parar")
        }

        override val leituras: Flow<LeituraPatrimonial> = canal
    }

    private lateinit var fonte: FonteFalsa
    private var conexoesPedidas = 0
    private var desconexoesPedidas = 0
    private var pedidosDePermissaoCamera = 0

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        fonte = FonteFalsa()
        conexoesPedidas = 0
        desconexoesPedidas = 0
        pedidosDePermissaoCamera = 0
    }

    @After
    fun desmontar() {
        Dispatchers.resetMain()
    }

    private fun novoViewModel(
        fontes: Map<OrigemLeitura, FonteDeLeitura> = mapOf(OrigemLeitura.RFID_UHF to fonte)
    ) = ScannerViewModel(
        fontes = fontes,
        conectar = { conexoesPedidas++ },
        desconectar = { desconexoesPedidas++ },
        pedirPermissaoCamera = { pedidosDePermissaoCamera++ },
        relogio = { 1_755_500_000_000L }
    )

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

    // ---- INC-02: estado, conexão e delegação à fonte ----

    @Test
    fun `leitura emitida pela fonte aparece no estado da tela`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle() // garante que o coletor do init já se inscreveu na fonte
        val leitura = leituraDe("147258", "147258;Notebook Positivo")

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

    // ---- INC-04: seletor de modos e ciclo de vida das fontes ----

    @Test
    fun `seletor lista os tres modos com motivo legivel quando indisponiveis`() {
        val viewModel = novoViewModel()

        val modos = viewModel.estado.value.modos
        assertEquals(3, modos.size)

        val uhf = modos.single { it.origem == OrigemLeitura.RFID_UHF }
        assertFalse(uhf.disponivel)
        assertEquals("Scanner BLE não conectado", uhf.motivo)

        val barras = modos.single { it.origem == OrigemLeitura.CODIGO_BARRAS }
        assertFalse(barras.disponivel)
        assertTrue(barras.motivo!!.isNotBlank())

        val nfc = modos.single { it.origem == OrigemLeitura.NFC }
        assertFalse(nfc.disponivel)
        assertTrue(nfc.motivo!!.isNotBlank())
    }

    @Test
    fun `conectar o ble torna o modo uhf disponivel`() {
        val viewModel = novoViewModel()

        viewModel.atualizarConexao(true)

        val uhf = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.RFID_UHF }
        assertTrue(uhf.disponivel)
        assertNull(uhf.motivo)
    }

    @Test
    fun `trocar de modo para a fonte anterior antes de iniciar a nova`() = runTest(dispatcher) {
        val eventos = mutableListOf<String>()
        val uhf = FonteFalsa("uhf", eventos)
        val barras = FonteFalsa("barras", eventos)
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        viewModel.atualizarPermissaoCamera(true) // REQ-12: câmera já autorizada
        advanceUntilIdle()

        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)
        advanceUntilIdle()

        assertEquals(listOf("uhf.parar", "barras.iniciar"), eventos)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `leitura de fonte parada apos a troca e descartada`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        viewModel.atualizarPermissaoCamera(true) // REQ-12: câmera já autorizada
        advanceUntilIdle()

        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)
        advanceUntilIdle()

        uhf.canal.emit(leituraDe("147258", "147258")) // fonte parada emite tarde demais
        advanceUntilIdle()
        assertTrue(viewModel.estado.value.leituras.isEmpty())

        barras.canal.emit(leituraDe("369852", "369852", OrigemLeitura.CODIGO_BARRAS))
        advanceUntilIdle()
        assertEquals(1, viewModel.estado.value.leituras.size)
        assertEquals("369852", viewModel.estado.value.leituras.single().codigo)
    }

    @Test
    fun `selecionar modo indisponivel e ignorado`() {
        val viewModel = novoViewModel() // NFC sem fonte -> indisponível

        viewModel.selecionarModo(OrigemLeitura.NFC)

        assertEquals(OrigemLeitura.RFID_UHF, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `queda do ble torna o modo uhf indisponivel e preserva a lista`() = runTest(dispatcher) {
        val viewModel = novoViewModel()
        advanceUntilIdle()
        viewModel.atualizarConexao(true)

        fonte.canal.emit(leituraDe("147258", "147258"))
        advanceUntilIdle()
        assertEquals(1, viewModel.estado.value.leituras.size)

        viewModel.atualizarConexao(false) // CE-11: conexão cai durante o uso

        val uhf = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.RFID_UHF }
        assertFalse(uhf.disponivel)
        assertEquals("Scanner BLE não conectado", uhf.motivo)
        assertEquals(1, viewModel.estado.value.leituras.size)
    }

    @Test
    fun `segundo plano para a captura em andamento e voltar retoma`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        viewModel.atualizarPermissaoCamera(true) // REQ-12: câmera já autorizada
        advanceUntilIdle()
        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS) // inicia a captura
        advanceUntilIdle()
        assertEquals(1, barras.iniciadas)

        viewModel.aoEntrarEmSegundoPlano() // CE-13
        assertEquals(1, barras.paradas)

        viewModel.aoVoltarAoPrimeiroPlano()
        assertEquals(2, barras.iniciadas)
    }

    @Test
    fun `segundo plano sem captura em andamento nao toca a fonte`() {
        val viewModel = novoViewModel()

        viewModel.aoEntrarEmSegundoPlano()
        viewModel.aoVoltarAoPrimeiroPlano()

        assertEquals(0, fonte.iniciadas)
        assertEquals(0, fonte.paradas)
    }

    // ---- INC-05: permissão de câmera e o modo código de barras ----

    @Test
    fun `selecionar codigo de barras sem permissao pede a permissao e nao inicia a fonte`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        advanceUntilIdle()

        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS) // REQ-12: o pedido nasce da seleção

        assertEquals(1, pedidosDePermissaoCamera)
        assertEquals(0, barras.iniciadas)
        assertEquals(0, uhf.paradas)
        assertEquals(OrigemLeitura.RFID_UHF, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `permissao concedida completa a troca parando a fonte anterior antes`() = runTest(dispatcher) {
        val eventos = mutableListOf<String>()
        val uhf = FonteFalsa("uhf", eventos)
        val barras = FonteFalsa("barras", eventos)
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        advanceUntilIdle()
        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)

        viewModel.atualizarPermissaoCamera(true)
        advanceUntilIdle()

        assertEquals(listOf("uhf.parar", "barras.iniciar"), eventos)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `permissao negada desabilita o modo com motivo e mantem o modo anterior`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        advanceUntilIdle()
        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)

        viewModel.atualizarPermissaoCamera(false) // CE-03

        val modoBarras = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.CODIGO_BARRAS }
        assertFalse(modoBarras.disponivel)
        assertEquals("Permissão de câmera negada", modoBarras.motivo)
        assertEquals(0, barras.iniciadas)
        assertEquals(OrigemLeitura.RFID_UHF, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `apos a negativa ha caminho para reabrir a solicitacao`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        advanceUntilIdle()
        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)
        viewModel.atualizarPermissaoCamera(false)

        // CE-03: o modo negado expõe o caminho para pedir de novo
        val modoBarras = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.CODIGO_BARRAS }
        assertTrue(modoBarras.podeReabrirPermissao)

        viewModel.solicitarPermissaoCamera()
        assertEquals(2, pedidosDePermissaoCamera)

        viewModel.atualizarPermissaoCamera(true)
        advanceUntilIdle()

        val depois = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.CODIGO_BARRAS }
        assertTrue(depois.disponivel)
        assertNull(depois.motivo)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, viewModel.estado.value.modoSelecionado)
        assertEquals(1, barras.iniciadas)
    }

    @Test
    fun `com permissao ja concedida a troca e imediata sem novo pedido`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        viewModel.atualizarPermissaoCamera(true)
        advanceUntilIdle()

        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)
        advanceUntilIdle()

        assertEquals(0, pedidosDePermissaoCamera)
        assertEquals(1, barras.iniciadas)
        assertEquals(OrigemLeitura.CODIGO_BARRAS, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `permissao concedida sem troca pendente so atualiza a disponibilidade`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        advanceUntilIdle()

        viewModel.atualizarPermissaoCamera(true) // ex.: permissão já concedida em sessão anterior
        advanceUntilIdle()

        assertEquals(0, barras.iniciadas)
        assertEquals(OrigemLeitura.RFID_UHF, viewModel.estado.value.modoSelecionado)
        val modoBarras = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.CODIGO_BARRAS }
        assertTrue(modoBarras.disponivel)
    }

    // ---- INC-06: disponibilidade e captura do modo NFC ----

    @Test
    fun `nfc com fonte presente comeca como aparelho sem nfc ate o sistema informar`() {
        val nfc = FonteFalsa("nfc")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to fonte, OrigemLeitura.NFC to nfc)
        )

        val modoNfc = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.NFC }
        assertFalse(modoNfc.disponivel)
        assertEquals("Aparelho sem NFC", modoNfc.motivo)
    }

    @Test
    fun `nfc desligado tem motivo distinto de aparelho sem nfc`() {
        val nfc = FonteFalsa("nfc")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to fonte, OrigemLeitura.NFC to nfc)
        )

        viewModel.atualizarEstadoNfc(EstadoNfc.DESLIGADO) // CE-04

        val modoNfc = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.NFC }
        assertFalse(modoNfc.disponivel)
        assertEquals("NFC desligado", modoNfc.motivo)
        assertTrue(modoNfc.motivo != "Aparelho sem NFC")
    }

    @Test
    fun `nfc disponivel permite a troca com parar antes de iniciar`() = runTest(dispatcher) {
        val eventos = mutableListOf<String>()
        val uhf = FonteFalsa("uhf", eventos)
        val nfc = FonteFalsa("nfc", eventos)
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.NFC to nfc)
        )
        advanceUntilIdle()

        viewModel.atualizarEstadoNfc(EstadoNfc.DISPONIVEL)
        viewModel.selecionarModo(OrigemLeitura.NFC)
        advanceUntilIdle()

        assertEquals(listOf("uhf.parar", "nfc.iniciar"), eventos)
        assertEquals(OrigemLeitura.NFC, viewModel.estado.value.modoSelecionado)
    }

    @Test
    fun `leitura da fonte nfc entra na lista com a origem nfc`() = runTest(dispatcher) {
        val nfc = FonteFalsa("nfc")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to fonte, OrigemLeitura.NFC to nfc)
        )
        advanceUntilIdle()
        viewModel.atualizarEstadoNfc(EstadoNfc.DISPONIVEL)
        viewModel.selecionarModo(OrigemLeitura.NFC)
        advanceUntilIdle()

        nfc.canal.emit(leituraDe("04A224B25C6180", "04A224B25C6180", OrigemLeitura.NFC))
        advanceUntilIdle()

        val leituras = viewModel.estado.value.leituras
        assertEquals(1, leituras.size)
        assertEquals(OrigemLeitura.NFC, leituras.single().origem)
    }

    @Test
    fun `nfc desligado durante o uso torna o modo indisponivel e preserva a lista`() = runTest(dispatcher) {
        val nfc = FonteFalsa("nfc")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to fonte, OrigemLeitura.NFC to nfc)
        )
        advanceUntilIdle()
        viewModel.atualizarEstadoNfc(EstadoNfc.DISPONIVEL)
        viewModel.selecionarModo(OrigemLeitura.NFC)
        advanceUntilIdle()
        nfc.canal.emit(leituraDe("04A224B25C6180", "04A224B25C6180", OrigemLeitura.NFC))
        advanceUntilIdle()

        viewModel.atualizarEstadoNfc(EstadoNfc.DESLIGADO)

        val modoNfc = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.NFC }
        assertFalse(modoNfc.disponivel)
        assertEquals("NFC desligado", modoNfc.motivo)
        assertEquals(1, viewModel.estado.value.leituras.size)
    }

    @Test
    fun `sem hardware nfc os demais modos operam normalmente`() = runTest(dispatcher) {
        val nfc = FonteFalsa("nfc")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to fonte, OrigemLeitura.NFC to nfc)
        )
        advanceUntilIdle()
        viewModel.atualizarEstadoNfc(EstadoNfc.SEM_HARDWARE) // CE-02
        viewModel.atualizarConexao(true)

        val modoUhf = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.RFID_UHF }
        assertTrue(modoUhf.disponivel)

        fonte.canal.emit(leituraDe("147258", "147258"))
        advanceUntilIdle()
        assertEquals(1, viewModel.estado.value.leituras.size)
    }

    @Test
    fun `negar a permissao nao derruba os demais modos`() = runTest(dispatcher) {
        val uhf = FonteFalsa("uhf")
        val barras = FonteFalsa("barras")
        val viewModel = novoViewModel(
            fontes = mapOf(OrigemLeitura.RFID_UHF to uhf, OrigemLeitura.CODIGO_BARRAS to barras)
        )
        advanceUntilIdle()
        viewModel.atualizarConexao(true)
        viewModel.selecionarModo(OrigemLeitura.CODIGO_BARRAS)
        viewModel.atualizarPermissaoCamera(false) // CE-03: negar não trava o aplicativo

        val modoUhf = viewModel.estado.value.modos.single { it.origem == OrigemLeitura.RFID_UHF }
        assertTrue(modoUhf.disponivel)

        uhf.canal.emit(leituraDe("147258", "147258"))
        advanceUntilIdle()
        assertEquals(1, viewModel.estado.value.leituras.size)
    }
}
