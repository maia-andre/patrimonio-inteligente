package com.patrimoniosjc.rfidpoc.domain

/** TNF "well-known" da especificação NDEF. */
const val TNF_CONHECIDO: Short = 0x01

/** Tipo do registro de texto NDEF (RTD "T"). */
val TIPO_TEXTO: ByteArray = byteArrayOf(0x54)

/**
 * Um registro NDEF reduzido ao que a RN-02 precisa — TNF, tipo e payload como
 * bytes —, para que a interpretação seja pura, sem tipos Android (RNF-02).
 */
class RegistroNdef(
    val tnf: Short,
    val tipo: ByteArray,
    val payload: ByteArray
)

/**
 * Interpreta uma etiqueta NFC conforme a precedência da RN-02: o primeiro
 * registro NDEF de texto não vazio é o código; na falta dele (CE-05, CE-06),
 * o código é o UID em hexadecimal maiúsculo, sem separadores.
 */
fun interpretarEtiquetaNfc(
    registros: List<RegistroNdef>,
    uid: ByteArray,
    instante: Long
): LeituraPatrimonial {
    val codigo = registros.firstNotNullOfOrNull { extrairTextoNdef(it) } ?: uidEmHexadecimal(uid)
    return LeituraPatrimonial(
        codigo = codigo,
        descricao = null,
        origem = OrigemLeitura.NFC,
        bruto = codigo,
        instante = instante
    )
}

/**
 * Decodifica o payload de um registro de texto NDEF (status + idioma + texto,
 * UTF-8 ou UTF-16). Registro que não é de texto, vazio ou malformado devolve
 * nulo — o que faz a precedência cair para o UID (CE-06).
 */
private fun extrairTextoNdef(registro: RegistroNdef): String? {
    if (registro.tnf != TNF_CONHECIDO || !registro.tipo.contentEquals(TIPO_TEXTO)) return null
    val payload = registro.payload
    if (payload.isEmpty()) return null
    val status = payload[0].toInt()
    val inicioTexto = 1 + (status and 0x3F)
    if (inicioTexto >= payload.size) return null
    val charset = if (status and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
    return String(payload, inicioTexto, payload.size - inicioTexto, charset).ifEmpty { null }
}

/** UID em hexadecimal maiúsculo, sem separadores, com zero à esquerda preservado (RN-02). */
fun uidEmHexadecimal(uid: ByteArray): String =
    uid.joinToString("") { "%02X".format(it) }
