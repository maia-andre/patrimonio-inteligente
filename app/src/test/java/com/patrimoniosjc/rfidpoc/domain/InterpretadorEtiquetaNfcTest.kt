package com.patrimoniosjc.rfidpoc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * RN-02/CE-05/CE-06 — precedência de extração do código NFC: registro NDEF
 * de texto não vazio; na falta dele, o UID em hexadecimal maiúsculo, sem
 * separadores. Função pura, testada sem aparelho (RNF-02). Dados fictícios
 * (RNF-03); o UID de exemplo é o da definição de concluído da spec.
 */
class InterpretadorEtiquetaNfcTest {

    private val uid = byteArrayOf(0x04, 0xA2.toByte(), 0x24, 0xB2.toByte(), 0x5C, 0x61, 0x80.toByte())

    /** Monta o payload de um registro de texto NDEF (RTD "T"): status + idioma + texto. */
    private fun payloadDeTexto(texto: String, idioma: String = "pt", utf16: Boolean = false): ByteArray {
        val idiomaBytes = idioma.toByteArray(Charsets.US_ASCII)
        val textoBytes = if (utf16) texto.toByteArray(Charsets.UTF_16) else texto.toByteArray(Charsets.UTF_8)
        val status = (idiomaBytes.size or if (utf16) 0x80 else 0x00).toByte()
        return byteArrayOf(status) + idiomaBytes + textoBytes
    }

    private fun registroDeTexto(texto: String, idioma: String = "pt", utf16: Boolean = false) =
        RegistroNdef(
            tnf = TNF_CONHECIDO,
            tipo = TIPO_TEXTO,
            payload = payloadDeTexto(texto, idioma, utf16)
        )

    // ---- Caminho 1 da precedência: registro NDEF de texto (RN-02) ----

    @Test
    fun `registro ndef de texto vira o codigo`() {
        val leitura = interpretarEtiquetaNfc(listOf(registroDeTexto("147258")), uid, 1_000L)

        assertEquals("147258", leitura.codigo)
        assertEquals(OrigemLeitura.NFC, leitura.origem)
        assertEquals(1_000L, leitura.instante)
        assertNull(leitura.descricao)
    }

    @Test
    fun `registro de texto em utf-16 e decodificado`() {
        val leitura = interpretarEtiquetaNfc(listOf(registroDeTexto("369852", utf16 = true)), uid, 1_000L)

        assertEquals("369852", leitura.codigo)
    }

    @Test
    fun `primeiro registro de texto vale mesmo depois de registro de outro tipo`() {
        val uri = RegistroNdef(
            tnf = TNF_CONHECIDO,
            tipo = byteArrayOf(0x55), // RTD "U" (URI)
            payload = byteArrayOf(0x01) + "exemplo.gov.br".toByteArray(Charsets.UTF_8)
        )

        val leitura = interpretarEtiquetaNfc(listOf(uri, registroDeTexto("147258")), uid, 1_000L)

        assertEquals("147258", leitura.codigo)
    }

    // ---- Caminho 2 da precedência: UID (RN-02, CE-05, CE-06) ----

    @Test
    fun `sem ndef o codigo e o uid em hexadecimal maiusculo sem separadores`() {
        val leitura = interpretarEtiquetaNfc(emptyList(), uid, 2_000L)

        assertEquals("04A224B25C6180", leitura.codigo)
    }

    @Test
    fun `ndef sem registro de texto cai para o uid`() {
        val uri = RegistroNdef(
            tnf = TNF_CONHECIDO,
            tipo = byteArrayOf(0x55),
            payload = byteArrayOf(0x01) + "exemplo.gov.br".toByteArray(Charsets.UTF_8)
        )

        val leitura = interpretarEtiquetaNfc(listOf(uri), uid, 2_000L)

        assertEquals("04A224B25C6180", leitura.codigo)
    }

    @Test
    fun `registro de texto vazio cai para o uid`() {
        val leitura = interpretarEtiquetaNfc(listOf(registroDeTexto("")), uid, 2_000L)

        assertEquals("04A224B25C6180", leitura.codigo)
    }

    @Test
    fun `payload de texto malformado cai para o uid sem lancar excecao`() {
        val vazio = RegistroNdef(tnf = TNF_CONHECIDO, tipo = TIPO_TEXTO, payload = ByteArray(0))
        val idiomaAlemDoPayload = RegistroNdef(
            tnf = TNF_CONHECIDO,
            tipo = TIPO_TEXTO,
            payload = byteArrayOf(0x0F, 0x70) // declara idioma de 15 bytes num payload de 1
        )

        assertEquals("04A224B25C6180", interpretarEtiquetaNfc(listOf(vazio), uid, 1_000L).codigo)
        assertEquals("04A224B25C6180", interpretarEtiquetaNfc(listOf(idiomaAlemDoPayload), uid, 1_000L).codigo)
    }

    @Test
    fun `tipo texto so vale no tnf well-known`() {
        // Mesmo tipo "T", mas TNF de mídia: não é registro de texto NDEF
        val midia = RegistroNdef(tnf = 0x02, tipo = TIPO_TEXTO, payload = payloadDeTexto("999999"))

        assertEquals("04A224B25C6180", interpretarEtiquetaNfc(listOf(midia), uid, 1_000L).codigo)
    }

    @Test
    fun `uid com bytes pequenos preserva o zero a esquerda`() {
        val leitura = interpretarEtiquetaNfc(emptyList(), byteArrayOf(0x00, 0x0A, 0x01), 1_000L)

        assertEquals("000A01", leitura.codigo)
    }

    // ---- Contrato da leitura ----

    @Test
    fun `bruto preserva a fonte do codigo e a chave deduplica por ele`() {
        val comTexto = interpretarEtiquetaNfc(listOf(registroDeTexto("147258")), uid, 1_000L)
        val semNdef = interpretarEtiquetaNfc(emptyList(), uid, 2_000L)

        assertEquals("147258", comTexto.bruto)
        assertEquals("147258", comTexto.chave)
        assertEquals("04A224B25C6180", semNdef.bruto)
        assertEquals("04A224B25C6180", semNdef.chave)
    }
}
