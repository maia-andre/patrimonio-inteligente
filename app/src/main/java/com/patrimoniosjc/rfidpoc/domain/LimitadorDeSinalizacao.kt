package com.patrimoniosjc.rfidpoc.domain

/**
 * Limita a sinalização de "já conferido" a uma ocorrência por intervalo
 * para a mesma chave (CE-01) — evita repetição incessante quando a câmera
 * decodifica o mesmo código continuamente. Decide pelo instante da leitura.
 */
class LimitadorDeSinalizacao(private val intervaloMs: Long = 1_000L) {

    private val ultimaSinalizacao = mutableMapOf<String, Long>()

    fun deveSinalizar(chave: String, instante: Long): Boolean {
        val ultima = ultimaSinalizacao[chave]
        if (ultima != null && instante - ultima < intervaloMs) {
            return false
        }
        ultimaSinalizacao[chave] = instante
        return true
    }
}
