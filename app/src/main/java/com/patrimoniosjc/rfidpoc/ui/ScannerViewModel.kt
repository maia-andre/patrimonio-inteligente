package com.patrimoniosjc.rfidpoc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Estado imutável da tela do scanner. */
data class EstadoTelaScanner(
    val conectado: Boolean = false,
    val statusTexto: String = "Desconectado",
    val ultimaLeitura: LeituraPatrimonial? = null,
    val logs: List<String> = emptyList()
)

/**
 * Controla a fonte de leitura ativa e expõe o estado da tela.
 * Os callbacks do BLE chegam de threads do binder; todo o estado
 * muda por `update`, que é atômico.
 */
class ScannerViewModel(
    private val fonte: FonteDeLeitura,
    private val conectar: () -> Unit,
    private val desconectar: () -> Unit,
    private val relogio: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoTelaScanner())
    val estado: StateFlow<EstadoTelaScanner> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            fonte.leituras.collect { leitura ->
                _estado.update { it.copy(ultimaLeitura = leitura) }
                registrarLog("[ATIVO] ${leitura.bruto}")
            }
        }
    }

    fun alternarConexao() {
        if (_estado.value.conectado) desconectar() else conectar()
    }

    fun iniciarLeitura() = fonte.iniciar()

    fun pararLeitura() = fonte.parar()

    fun atualizarConexao(conectado: Boolean) {
        _estado.update {
            it.copy(
                conectado = conectado,
                statusTexto = if (conectado) "Scanner Conectado" else "Desconectado"
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
}
