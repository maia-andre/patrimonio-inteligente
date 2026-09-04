package com.patrimoniosjc.rfidpoc.domain

/**
 * Interpreta o código digitado à mão: o texto, sem os espaços das pontas,
 * é o código do bem. Em branco não é código, é ausência — devolve nulo.
 * Função pura (RNF-02), sem nenhuma API Android.
 */
fun interpretarCodigoManual(texto: String, instante: Long): LeituraPatrimonial? {
    val codigo = texto.trim()
    if (codigo.isEmpty()) return null
    return LeituraPatrimonial(
        codigo = codigo,
        descricao = null,
        origem = OrigemLeitura.MANUAL,
        bruto = codigo,
        instante = instante
    )
}
