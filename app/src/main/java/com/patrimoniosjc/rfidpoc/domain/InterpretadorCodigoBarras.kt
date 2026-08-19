package com.patrimoniosjc.rfidpoc.domain

/**
 * Interpreta o texto decodificado de um código de barras (REQ-06): o texto
 * inteiro é o código do bem — a etiqueta patrimonial carrega só o código.
 * Texto vazio vira nulo: vazio não é código, é ausência (DT-02).
 */
fun interpretarCodigoBarras(texto: String, instante: Long): LeituraPatrimonial =
    LeituraPatrimonial(
        codigo = texto.ifEmpty { null },
        descricao = null,
        origem = OrigemLeitura.CODIGO_BARRAS,
        bruto = texto,
        instante = instante
    )
