package com.patrimoniosjc.rfidpoc.scan

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.patrimoniosjc.rfidpoc.domain.FonteDeLeitura
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.interpretarCodigoBarras
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fonte de leitura do modo código de barras: o CameraX entrega os quadros e o
 * DecodificadorZxing — puro, coberto por teste unitário — decide o que é
 * leitura (REQ-06, DT-01, DT-03). Quadro ilegível é silêncio: a câmera segue
 * tentando, sem mensagem de erro (CE-12). Iniciar e parar devem vir da thread
 * principal, como o CameraX exige — é de lá que o ViewModel os chama.
 */
class FonteCodigoBarras(
    private val contexto: Context,
    private val donoDoCiclo: LifecycleOwner,
    private val relogio: () -> Long = System::currentTimeMillis
) : FonteDeLeitura {

    private val decodificador = DecodificadorZxing()
    private val canal = MutableSharedFlow<LeituraPatrimonial>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val leituras: Flow<LeituraPatrimonial> = canal.asSharedFlow()

    private var provedorCamera: ProcessCameraProvider? = null
    private var executorDeAnalise: ExecutorService? = null
    private var previa: Preview? = null
    private var provedorDeSuperficie: Preview.SurfaceProvider? = null
    private var ativa = false

    /** A tela anexa aqui a superfície onde a prévia da câmera é desenhada. */
    fun anexarPrevia(superficie: Preview.SurfaceProvider) {
        provedorDeSuperficie = superficie
        ContextCompat.getMainExecutor(contexto).execute {
            previa?.setSurfaceProvider(superficie)
        }
    }

    override fun iniciar() {
        ativa = true
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener({
            // parar() pode ter chegado antes de a câmera ficar pronta
            if (!ativa) return@addListener
            val provedor = futuro.get()
            provedorCamera = provedor

            val executor = Executors.newSingleThreadExecutor()
            executorDeAnalise = executor
            val analise = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analise.setAnalyzer(executor) { quadro -> analisar(quadro) }

            val previa = Preview.Builder().build()
            provedorDeSuperficie?.let { previa.setSurfaceProvider(it) }
            this.previa = previa

            provedor.unbindAll()
            provedor.bindToLifecycle(
                donoDoCiclo, CameraSelector.DEFAULT_BACK_CAMERA, analise, previa
            )
        }, ContextCompat.getMainExecutor(contexto))
    }

    /** REQ-10/CE-10/CE-13 — parar libera a câmera para o sistema. */
    override fun parar() {
        ativa = false
        provedorCamera?.unbindAll()
        provedorCamera = null
        previa = null
        executorDeAnalise?.shutdown()
        executorDeAnalise = null
    }

    private fun analisar(quadro: ImageProxy) {
        quadro.use {
            val texto = decodificador.decodificar(luminanciaDe(it), it.width, it.height)
            if (texto != null) canal.tryEmit(interpretarCodigoBarras(texto, relogio()))
        }
    }

    /** Extrai o plano de luminância (Y) respeitando o passo de linha do buffer. */
    private fun luminanciaDe(quadro: ImageProxy): ByteArray {
        val plano = quadro.planes[0]
        val buffer = plano.buffer
        buffer.rewind()
        val largura = quadro.width
        val altura = quadro.height
        val passo = plano.rowStride
        val bytes = ByteArray(largura * altura)
        if (passo == largura) {
            buffer.get(bytes, 0, minOf(buffer.remaining(), bytes.size))
        } else {
            for (linha in 0 until altura) {
                buffer.position(linha * passo)
                buffer.get(bytes, linha * largura, largura)
            }
        }
        return bytes
    }
}
