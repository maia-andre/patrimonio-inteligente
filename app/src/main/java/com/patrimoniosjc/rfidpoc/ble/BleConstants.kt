package com.patrimoniosjc.rfidpoc.ble

import java.util.UUID

object BleConstants {
    // Mesmos UUIDs definidos no firmware do ESP32
    val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    
    // Descritor padrão usado para notificações BLE
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    
    const val DEVICE_NAME = "RFID-POC-ESP32"
}
