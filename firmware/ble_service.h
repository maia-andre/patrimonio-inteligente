#ifndef BLE_SERVICE_H
#define BLE_SERVICE_H

#include <Arduino.h>

// Inicializa o servidor BLE
void setupBLE();

// Envia uma mensagem de texto (Notificação) para o App Android
void sendBLENotification(const char* message);

#endif // BLE_SERVICE_H
