package com.patrimoniosjc.rfidpoc.scan

/**
 * Remonta mensagens longas fragmentadas pelo BLE: acumula fragmentos até o
 * marcador de fim e então devolve o payload completo (nulo se nada acumulou).
 */
class RemontadorDeFragmentos {

    private val buffer = StringBuilder()

    fun receber(fragmento: String): String? {
        if (fragmento == MARCADOR_FIM) {
            val completo = buffer.toString()
            buffer.clear()
            return completo.ifEmpty { null }
        }
        buffer.append(fragmento)
        return null
    }

    companion object {
        const val MARCADOR_FIM = "__END__"
    }
}
