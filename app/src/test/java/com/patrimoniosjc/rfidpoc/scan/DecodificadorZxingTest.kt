package com.patrimoniosjc.rfidpoc.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * REQ-06/RN-04/CE-12 — o decodificador aceita exclusivamente Code 128,
 * Code 39 e QR Code; EAN e UPC de varejo são ignorados; quadro ilegível
 * devolve nulo em silêncio. Roda na JVM, sem aparelho (RNF-02).
 * As imagens são geradas pelo codificador do próprio ZXing, com dados
 * fictícios (RNF-03).
 */
class DecodificadorZxingTest {

    private val decodificador = DecodificadorZxing()

    /** Converte a matriz codificada em luminância: preto 0, branco 255. */
    private fun luminanciaDe(matriz: BitMatrix): ByteArray {
        val bytes = ByteArray(matriz.width * matriz.height)
        for (y in 0 until matriz.height) {
            for (x in 0 until matriz.width) {
                bytes[y * matriz.width + x] = if (matriz.get(x, y)) 0 else 0xFF.toByte()
            }
        }
        return bytes
    }

    private fun codificar(texto: String, formato: BarcodeFormat, largura: Int, altura: Int): BitMatrix =
        MultiFormatWriter().encode(texto, formato, largura, altura)

    // ---- Simbologias aceitas (RN-04) ----

    @Test
    fun `decodifica code 128`() {
        val matriz = codificar("147258", BarcodeFormat.CODE_128, 400, 120)

        val texto = decodificador.decodificar(luminanciaDe(matriz), matriz.width, matriz.height)

        assertEquals("147258", texto)
    }

    @Test
    fun `decodifica code 39`() {
        val matriz = codificar("PATR-147258", BarcodeFormat.CODE_39, 600, 120)

        val texto = decodificador.decodificar(luminanciaDe(matriz), matriz.width, matriz.height)

        assertEquals("PATR-147258", texto)
    }

    @Test
    fun `decodifica qr code`() {
        val matriz = codificar("QR-147258", BarcodeFormat.QR_CODE, 200, 200)

        val texto = decodificador.decodificar(luminanciaDe(matriz), matriz.width, matriz.height)

        assertEquals("QR-147258", texto)
    }

    @Test
    fun `decodifica code 128 rotacionado 90 graus`() {
        // A câmera entrega o quadro na orientação do sensor; um código de
        // barras 1D na vertical precisa da segunda tentativa, rotacionada.
        val matriz = codificar("369852", BarcodeFormat.CODE_128, 400, 120)
        val luminancia = luminanciaDe(matriz)
        val rotacionada = ByteArray(luminancia.size)
        for (y in 0 until matriz.height) {
            for (x in 0 until matriz.width) {
                rotacionada[x * matriz.height + (matriz.height - 1 - y)] =
                    luminancia[y * matriz.width + x]
            }
        }

        val texto = decodificador.decodificar(rotacionada, matriz.height, matriz.width)

        assertEquals("369852", texto)
    }

    // ---- Simbologias de varejo ignoradas (RN-04) ----

    @Test
    fun `ignora ean 13 de varejo`() {
        val matriz = codificar("1234567890128", BarcodeFormat.EAN_13, 400, 120)

        assertNull(decodificador.decodificar(luminanciaDe(matriz), matriz.width, matriz.height))
    }

    @Test
    fun `ignora upc a de varejo`() {
        val matriz = codificar("123456789012", BarcodeFormat.UPC_A, 400, 120)

        assertNull(decodificador.decodificar(luminanciaDe(matriz), matriz.width, matriz.height))
    }

    // ---- Quadro ilegível (CE-12) ----

    @Test
    fun `quadro de ruido devolve nulo sem lancar excecao`() {
        val ruido = ByteArray(400 * 120)
        Random(42).nextBytes(ruido)

        assertNull(decodificador.decodificar(ruido, 400, 120))
    }

    @Test
    fun `quadro uniforme sem codigo devolve nulo`() {
        val cinza = ByteArray(400 * 120) { 0x80.toByte() }

        assertNull(decodificador.decodificar(cinza, 400, 120))
    }

    @Test
    fun `codigo parcial cortado ao meio devolve nulo`() {
        val matriz = codificar("147258", BarcodeFormat.CODE_128, 400, 120)
        val luminancia = luminanciaDe(matriz)
        val metade = 200
        val cortada = ByteArray(metade * matriz.height)
        for (y in 0 until matriz.height) {
            System.arraycopy(luminancia, y * matriz.width, cortada, y * metade, metade)
        }

        assertNull(decodificador.decodificar(cortada, metade, matriz.height))
    }

    // ---- Filtro de simbologia como função pura (RNF-02) ----

    @Test
    fun `filtro aceita exatamente as tres simbologias da rn-04`() {
        assertTrue(simbologiaAceita(BarcodeFormat.CODE_128))
        assertTrue(simbologiaAceita(BarcodeFormat.CODE_39))
        assertTrue(simbologiaAceita(BarcodeFormat.QR_CODE))

        assertFalse(simbologiaAceita(BarcodeFormat.EAN_13))
        assertFalse(simbologiaAceita(BarcodeFormat.EAN_8))
        assertFalse(simbologiaAceita(BarcodeFormat.UPC_A))
        assertFalse(simbologiaAceita(BarcodeFormat.UPC_E))
        assertFalse(simbologiaAceita(BarcodeFormat.DATA_MATRIX))
        assertFalse(simbologiaAceita(BarcodeFormat.ITF))
    }
}
