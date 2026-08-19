package com.patrimoniosjc.rfidpoc.domain

/** Resultado de acumular uma leitura: a lista resultante e se o bem já havia sido conferido. */
data class ResultadoAcumulo(
    val lista: List<LeituraPatrimonial>,
    val jaConferida: Boolean
)

/**
 * Acúmulo em memória com deduplicação pela chave (RN-01) e ordem
 * cronológica inversa — a mais recente no topo (RN-06). Função pura.
 */
object AcumuladorDeLeituras {

    fun acumular(listaAtual: List<LeituraPatrimonial>, nova: LeituraPatrimonial): ResultadoAcumulo {
        val jaConferida = listaAtual.any { it.chave == nova.chave }
        return if (jaConferida) {
            ResultadoAcumulo(listaAtual, jaConferida = true)
        } else {
            ResultadoAcumulo(listOf(nova) + listaAtual, jaConferida = false)
        }
    }
}
