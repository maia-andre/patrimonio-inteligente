package com.patrimoniosjc.rfidpoc.domain

/**
 * Interpreta o payload UHF remontado, tolerante aos dois formatos (RN-03):
 * com `;`, o trecho anterior ao primeiro separador é o código e o restante a descrição;
 * sem `;` — formato atual do firmware —, o payload inteiro é a descrição.
 * Campo vazio vira nulo: vazio não é código nem descrição, é ausência (DT-02).
 */
fun interpretarPayloadUhf(payload: String, instante: Long): LeituraPatrimonial {
    val separador = payload.indexOf(';')
    val codigo: String?
    val descricao: String?
    if (separador >= 0) {
        codigo = payload.substring(0, separador).ifEmpty { null }
        descricao = payload.substring(separador + 1).ifEmpty { null }
    } else {
        codigo = null
        descricao = payload.ifEmpty { null }
    }
    return LeituraPatrimonial(
        codigo = codigo,
        descricao = descricao,
        origem = OrigemLeitura.RFID_UHF,
        bruto = payload,
        instante = instante
    )
}
