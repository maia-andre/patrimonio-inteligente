package com.patrimoniosjc.rfidpoc.model

data class BleMessage(
    val type: MessageType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class MessageType {
        LOG,    // Mensagem interna do app
        TX,     // Mensagem enviada para o ESP32
        RX      // Mensagem recebida do ESP32
    }
}
