package com.patrimoniosjc.rfidpoc.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/** RN-04 — simbologias aceitas: só as de patrimônio; EAN e UPC de varejo ficam de fora. */
val SIMBOLOGIAS_ACEITAS: Set<BarcodeFormat> = setOf(
    BarcodeFormat.CODE_128,
    BarcodeFormat.CODE_39,
    BarcodeFormat.QR_CODE
)

/** Filtro de simbologia como função pura (RNF-02). */
fun simbologiaAceita(formato: BarcodeFormat): Boolean = formato in SIMBOLOGIAS_ACEITAS

/**
 * Decodifica um quadro de luminância com o ZXing, restrito às simbologias
 * da RN-04. Quadro ilegível devolve nulo, sem exceção nem mensagem (CE-12).
 * Códigos 1D na vertical são cobertos pela segunda tentativa, com o quadro
 * rotacionado 90 graus. Puro JVM: roda em teste unitário sem aparelho (RNF-02).
 * Não é seguro para uso concorrente — o analisador da câmera é de thread única.
 */
class DecodificadorZxing {

    private val leitor = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to SIMBOLOGIAS_ACEITAS.toList(),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    fun decodificar(luminancia: ByteArray, largura: Int, altura: Int): String? =
        tentar(luminancia, largura, altura)
            ?: tentar(rotacionar90(luminancia, largura, altura), altura, largura)

    private fun tentar(luminancia: ByteArray, largura: Int, altura: Int): String? {
        val fonte = PlanarYUVLuminanceSource(
            luminancia, largura, altura, 0, 0, largura, altura, false
        )
        return try {
            val resultado = leitor.decodeWithState(BinaryBitmap(HybridBinarizer(fonte)))
            // As hints já restringem os formatos; o filtro explícito é a garantia da RN-04
            if (simbologiaAceita(resultado.barcodeFormat)) resultado.text else null
        } catch (_: ReaderException) {
            null
        } finally {
            leitor.reset()
        }
    }
}

/** Rotaciona o quadro de luminância 90 graus em sentido horário. */
fun rotacionar90(luminancia: ByteArray, largura: Int, altura: Int): ByteArray {
    val saida = ByteArray(luminancia.size)
    for (y in 0 until altura) {
        for (x in 0 until largura) {
            saida[x * altura + (altura - 1 - y)] = luminancia[y * largura + x]
        }
    }
    return saida
}
