package com.patrimoniosjc.rfidpoc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrimoniosjc.rfidpoc.domain.AcumuladorDeLeituras
import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.LimitadorDeSinalizacao
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Um modo no seletor: disponível, ou desabilitado com o motivo legível (REQ-11). */
data class ModoDaTela(
    val origem: OrigemLeitura,
    val disponivel: Boolean,
    val motivo: String?
)

/** Estado imutável da tela do scanner. A lista vive só em memória (RN-07). */
data class EstadoTelaScanner(
    val conectado: Boolean = false,
    val statusTexto: String = "Desconectado",
    val modoSelecionado: OrigemLeitura = OrigemLeitura.RFID_UHF,
    val modos: List<ModoDaTela> = emptyList(),
    val ultimaLeitura: LeituraPatrimonial? = null,
    val leituras: List<LeituraPatrimonial> = emptyList(),
    val avisoJaConferido: String? = null,
    val logs: List<String> = emptyList()
)

/**
 * Controla a fonte de leitura ativa, o seletor de modos e o ciclo de vida
 * das fontes (RN-05: uma ativa por vez; REQ-10: parar antes de iniciar).
 * Os callbacks do BLE chegam de threads do binder; todo o estado muda por
 * `update`, que é atômico.
 */
class ScannerViewModel(
    private val fontes: Map<OrigemLeitura, FonteDeLeitura>,
    private val conectar: () -> Unit,
    private val desconectar: () -> Unit,
    private val relogio: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _estado = MutableStateFlow(
        EstadoTelaScanner(modos = montarModos(conectado = false))
    )
    val estado: StateFlow<EstadoTelaScanner> = _estado.asStateFlow()

    private val limitador = LimitadorDeSinalizacao()

    private var fonteAtiva: FonteDeLeitura? = fontes[OrigemLeitura.RFID_UHF]
    private var coleta: Job? = null

    // CE-13: só retoma ao voltar do segundo plano se a captura estava em andamento
    private var capturaEmAndamento = false

    init {
        coleta = fonteAtiva?.let { assinar(it) }
    }

    private fun assinar(fonte: FonteDeLeitura): Job = viewModelScope.launch {
        fonte.leituras.collect { leitura -> acumular(leitura) }
    }

    /** REQ-11 — disponibilidade com motivo legível. Os motivos específicos de câmera e NFC chegam nos INC-05/06. */
    private fun montarModos(conectado: Boolean): List<ModoDaTela> =
        OrigemLeitura.entries.map { origem ->
            val motivo = when {
                fontes[origem] == null -> "Ainda não disponível nesta versão"
                origem == OrigemLeitura.RFID_UHF && !conectado -> "Scanner BLE não conectado"
                else -> null
            }
            ModoDaTela(origem = origem, disponivel = motivo == null, motivo = motivo)
        }

    /** REQ-03/REQ-10/RN-05 — troca de modo: para a fonte anterior antes de iniciar a nova. */
    fun selecionarModo(origem: OrigemLeitura) {
        val estadoAtual = _estado.value
        if (origem == estadoAtual.modoSelecionado) return
        val modo = estadoAtual.modos.firstOrNull { it.origem == origem } ?: return
        if (!modo.disponivel) return

        // CE-10: cancelar a coleta descarta leituras que a fonte parada emitir depois
        coleta?.cancel()
        fonteAtiva?.parar()

        val novaFonte = fontes.getValue(origem)
        fonteAtiva = novaFonte
        coleta = assinar(novaFonte)
        novaFonte.iniciar()
        capturaEmAndamento = true

        _estado.update { it.copy(modoSelecionado = origem) }
        registrarLog("Modo ${rotuloDaOrigem(origem)} ativado")
    }

    fun alternarConexao() {
        if (_estado.value.conectado) desconectar() else conectar()
    }

    fun iniciarLeitura() {
        fonteAtiva?.iniciar()
        capturaEmAndamento = true
    }

    fun pararLeitura() {
        fonteAtiva?.parar()
        capturaEmAndamento = false
    }

    /** CE-13 — segundo plano para a fonte ativa, liberando câmera/antena/scanner. */
    fun aoEntrarEmSegundoPlano() {
        if (capturaEmAndamento) fonteAtiva?.parar()
    }

    /** CE-13 — ao voltar, o modo selecionado é retomado. */
    fun aoVoltarAoPrimeiroPlano() {
        if (capturaEmAndamento) fonteAtiva?.iniciar()
    }

    /** CE-11 — a queda da conexão só muda a disponibilidade; a lista acumulada é preservada. */
    fun atualizarConexao(conectado: Boolean) {
        _estado.update {
            it.copy(
                conectado = conectado,
                statusTexto = if (conectado) "Scanner Conectado" else "Desconectado",
                modos = montarModos(conectado)
            )
        }
    }

    fun scannerDesligado() {
        _estado.update { it.copy(statusTexto = "Scanner Desligado") }
        registrarLog("[RX] Scanner desligado.")
    }

    fun registrarLog(mensagem: String) {
        val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(relogio()))
        _estado.update { it.copy(logs = listOf("[$hora] $mensagem") + it.logs) }
    }

    private fun acumular(leitura: LeituraPatrimonial) {
        val resultado = AcumuladorDeLeituras.acumular(_estado.value.leituras, leitura)
        if (resultado.jaConferida) {
            if (limitador.deveSinalizar(leitura.chave, leitura.instante)) {
                _estado.update { it.copy(avisoJaConferido = "Item ${leitura.chave} já conferido") }
                registrarLog("[DUPLICADA] ${leitura.chave} já conferido")
            }
        } else {
            _estado.update {
                it.copy(
                    leituras = resultado.lista,
                    ultimaLeitura = leitura,
                    avisoJaConferido = null
                )
            }
            registrarLog("[ATIVO] ${leitura.bruto}")
        }
    }
}
