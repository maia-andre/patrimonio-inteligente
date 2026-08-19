package com.patrimoniosjc.rfidpoc.domain

/** Origem de uma leitura patrimonial. Os três modos são complementares: NFC não substitui RFID UHF. */
enum class OrigemLeitura {
    CODIGO_BARRAS,
    NFC,
    RFID_UHF
}
