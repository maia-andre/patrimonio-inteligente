#include <Arduino.h>
#include "led_controller.h"
#include "ble_service.h"

// Variáveis globais úteis
LedController led;

// Controle de reconexão do BLE
extern BLEServer* pServer;
extern bool deviceConnected;
extern bool oldDeviceConnected;

// Temporizador para piscar o LED
unsigned long previousMillis = 0;
const long interval = 1000; // Piscar a cada 1 segundo (usado apenas se não estivermos usando os comandos explícitos)

void setup() {
    // 1. Inicializa o Monitor Serial
    Serial.begin(115200);
    delay(1000); // Pequeno atraso para o Serial Monitor estabilizar
    Serial.println("\n[BOOT] ESP32 iniciado");

    // 2. Inicializa o LED
    led.begin();
    Serial.println("[BOOT] LED configurado");

    // 3. Inicializa o BLE
    setupBLE();
    
    // Pisca rapidamente para mostrar que ligou
    for(int i=0; i<3; i++) {
        led.turnOn();
        delay(100);
        led.turnOff();
        delay(100);
    }
}

void loop() {
    // O loop principal pode ser usado para gerenciar reconexões BLE
    
    // Se o dispositivo desconectou
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // Dá um tempo para a pilha BLE se estabilizar
        pServer->startAdvertising(); // Reinicia a publicidade para ser encontrado novamente
        Serial.println("[BLE] Advertising reiniciado. Aguardando nova conexão...");
        oldDeviceConnected = deviceConnected;
    }
    
    // Se um novo dispositivo conectou
    if (deviceConnected && !oldDeviceConnected) {
        // faz algo aqui se quiser quando conectar (o callback de onConnect já avisa na Serial)
        oldDeviceConnected = deviceConnected;
    }

    // Nota: O controle do LED via BLE é feito por eventos (interruptions virtuais) 
    // dentro da classe MyCallbacks no arquivo ble_service.cpp, através da função onWrite.
    // Portanto, não precisamos verificar "if (mensagem_recebida)" aqui no loop.
}
