package com.patrimoniosjc.rfidpoc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.patrimoniosjc.rfidpoc.ble.BleManager
import com.patrimoniosjc.rfidpoc.scan.FonteUhfBle
import com.patrimoniosjc.rfidpoc.ui.ScannerViewModel
import com.patrimoniosjc.rfidpoc.ui.TelaScanner

/**
 * Só faz a fiação: cria BleManager, fonte UHF e ViewModel, pede as permissões
 * de BLE e monta a tela. Protocolo, parsing e controle de fonte vivem em
 * scan/ e ui/ (REQ-02).
 */
class MainActivity : ComponentActivity() {

    private var bleManager: BleManager? = null

    private val fonteUhf = FonteUhfBle(
        enviarComando = { comando -> bleManager?.sendCommand(comando) },
        aoDesligarScanner = { viewModel.scannerDesligado() }
    )

    private val viewModel: ScannerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ScannerViewModel(
                    fonte = fonteUhf,
                    conectar = { bleManager?.startScan() },
                    desconectar = { bleManager?.disconnect() }
                ) as T
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.registrarLog("Permissões concedidas. Pronto para conectar.")
        } else {
            viewModel.registrarLog("ERRO: Permissões negadas.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(
            context = this,
            onMessageLog = { msg -> viewModel.registrarLog(msg) },
            onMessageReceived = { msg -> fonteUhf.aoReceberMensagem(msg) },
            onConnectionStateChange = { conectado -> viewModel.atualizarConexao(conectado) }
        )

        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val estado by viewModel.estado.collectAsState()
                    TelaScanner(
                        estado = estado,
                        aoAlternarConexao = viewModel::alternarConexao,
                        aoIniciarLeitura = viewModel::iniciarLeitura,
                        aoPararLeitura = viewModel::pararLeitura
                    )
                }
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        } else {
            viewModel.registrarLog("Permissões OK.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager?.disconnect()
    }
}
