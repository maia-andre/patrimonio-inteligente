package com.patrimoniosjc.rfidpoc.domain

/**
 * Origem de uma leitura patrimonial. Os três modos de captura são
 * complementares (NFC não substitui RFID UHF); o lançamento manual é o
 * recurso de exceção para o bem sem etiqueta legível.
 */
enum class OrigemLeitura {
    CODIGO_BARRAS,
    NFC,
    RFID_UHF,
    MANUAL
}
