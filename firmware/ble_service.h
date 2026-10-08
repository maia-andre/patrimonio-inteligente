#ifndef BLE_SERVICE_H
#define BLE_SERVICE_H

#include <Arduino.h>

// Comandos do scanner. SCAN_START/SCAN_STOP; LED_ON/LED_OFF continuam
// aceitos como sinônimos até o aplicativo ser atualizado.
enum ComandoScanner : uint8_t {
    COMANDO_NENHUM,
    COMANDO_SCAN_START,
    COMANDO_SCAN_STOP
};

// Inicializa o servidor BLE
void setupBLE();

// Traduz o texto de um comando (vindo do BLE ou da Serial USB)
ComandoScanner interpretarComando(const String& texto);

// Devolve o último comando recebido pelo BLE e o consome. O callback do BLE
// só registra o comando; quem o executa é o loop().
ComandoScanner lerComandoPendente();

// Envia uma mensagem curta (Notificação) para o App Android
void sendBLENotification(const char* message);

// Envia uma mensagem longa fragmentada em pacotes de 20 bytes
void sendBLELongMessage(const char* message);

#endif // BLE_SERVICE_H
