package com.patrimoniosjc.rfidpoc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.patrimoniosjc.rfidpoc.ble.BleManager
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import com.patrimoniosjc.rfidpoc.scan.FonteCodigoBarras
import com.patrimoniosjc.rfidpoc.scan.FonteNfc
import com.patrimoniosjc.rfidpoc.scan.FonteUhfBle
import com.patrimoniosjc.rfidpoc.ui.EstadoNfc
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

    private val fonteCodigoBarras by lazy { FonteCodigoBarras(this, this) }

    private val fonteNfc by lazy { FonteNfc(this) }

    private val viewModel: ScannerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ScannerViewModel(
                    fontes = mapOf(
                        OrigemLeitura.RFID_UHF to fonteUhf,
                        OrigemLeitura.CODIGO_BARRAS to fonteCodigoBarras,
                        OrigemLeitura.NFC to fonteNfc
                    ),
                    conectar = { bleManager?.startScan() },
                    desconectar = { bleManager?.disconnect() },
                    pedirPermissaoCamera = { pedirPermissaoCamera() }
                ) as T
        }
    }

    // CE-03: distingue a primeira negativa da permanente (sem diálogo do sistema)
    private var cameraJaNegada = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (!concedida) cameraJaNegada = true
        viewModel.atualizarPermissaoCamera(concedida)
    }

    /** REQ-12 — o pedido nasce da seleção do modo; negativa permanente leva às configurações (CE-03). */
    private fun pedirPermissaoCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED ->
                viewModel.atualizarPermissaoCamera(true)

            cameraJaNegada && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )

            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                        aoPararLeitura = viewModel::pararLeitura,
                        aoSelecionarModo = viewModel::selecionarModo,
                        aoSolicitarPermissaoCamera = viewModel::solicitarPermissaoCamera,
                        previaCamera = {
                            AndroidView(
                                factory = { contexto ->
                                    PreviewView(contexto).also {
                                        fonteCodigoBarras.anexarPrevia(it.surfaceProvider)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )
                        }
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

    override fun onStart() {
        super.onStart()
        // Concessão feita fora do fluxo (sessão anterior, tela de configurações)
        // chega aqui; negativa nunca é reportada por checagem passiva (REQ-12)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.atualizarPermissaoCamera(true)
        }
        // CE-02/CE-04: reavaliado a cada retorno — o usuário pode ligar o NFC nas configurações
        val adaptadorNfc = NfcAdapter.getDefaultAdapter(this)
        viewModel.atualizarEstadoNfc(
            when {
                adaptadorNfc == null -> EstadoNfc.SEM_HARDWARE
                !adaptadorNfc.isEnabled -> EstadoNfc.DESLIGADO
                else -> EstadoNfc.DISPONIVEL
            }
        )
        viewModel.aoVoltarAoPrimeiroPlano()
    }

    override fun onStop() {
        super.onStop()
        viewModel.aoEntrarEmSegundoPlano()
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager?.disconnect()
    }
}
