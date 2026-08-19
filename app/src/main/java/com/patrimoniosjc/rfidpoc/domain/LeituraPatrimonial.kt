package com.patrimoniosjc.rfidpoc.domain

/**
 * Uma leitura de bem patrimonial, de qualquer origem.
 *
 * [codigo] é nulo quando a origem não emite código estruturado (DT-02);
 * [bruto] guarda sempre o payload original, íntegro.
 */
data class LeituraPatrimonial(
    val codigo: String?,
    val descricao: String?,
    val origem: OrigemLeitura,
    val bruto: String,
    val instante: Long
)
