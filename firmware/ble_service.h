#ifndef BLE_SERVICE_H
#define BLE_SERVICE_H

#include <Arduino.h>

// Inicializa o servidor BLE
void setupBLE();

// Envia uma mensagem curta (Notificação) para o App Android
void sendBLENotification(const char* message);

// Envia uma mensagem longa fragmentada em pacotes de 20 bytes
void sendBLELongMessage(const char* message);

#endif // BLE_SERVICE_H
