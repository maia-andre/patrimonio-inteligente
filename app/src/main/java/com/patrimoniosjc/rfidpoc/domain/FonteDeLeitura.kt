package com.patrimoniosjc.rfidpoc.domain

import kotlinx.coroutines.flow.Flow

/**
 * Porta única de captura: toda origem (câmera, antena NFC, scanner BLE)
 * entrega leituras por este contrato. Apenas uma fonte fica ativa por vez.
 */
interface FonteDeLeitura {
    fun iniciar()
    fun parar()
    val leituras: Flow<LeituraPatrimonial>
}
