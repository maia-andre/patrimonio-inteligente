#include "ble_service.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include "led_controller.h"

// Instância do controlador do LED para usarmos aqui
extern LedController led;

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
BLECharacteristic* pRxCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// UUIDs para o Serviço e as Características
// Você pode gerar outros no site uuidgenerator.net se quiser, mas estes funcionarão perfeitamente.
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" // App escreve aqui (ESP32 recebe)
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // ESP32 notifica por aqui (App recebe)

// Callbacks para eventos de conexão do Servidor
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("[BLE] Dispositivo conectado!");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("[BLE] Dispositivo desconectado!");
    }
};

// Callbacks para receber mensagens na característica RX
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      String rxValue = pCharacteristic->getValue().c_str();

      if (rxValue.length() > 0) {
        Serial.print("[RX] Recebido: ");
        Serial.println(rxValue);

        // Lógica de controle do LED baseada no comando recebido
        if (rxValue == "LED_ON") {
            led.turnOn();
            Serial.println("[LED] ON");
            sendBLENotification("OK_LED_ON");
        } 
        else if (rxValue == "LED_OFF") {
            led.turnOff();
            Serial.println("[LED] OFF");
            sendBLENotification("OK_LED_OFF");
        }
      }
    }
};

void setupBLE() {
    Serial.println("[BOOT] Inicializando BLE...");
    
    // Cria o dispositivo BLE com o nome que irá aparecer no Android
    BLEDevice::init("RFID-POC-ESP32");

    // Cria o servidor BLE
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    // Cria o serviço BLE usando o UUID definido
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // Cria a característica TX (para o ESP32 enviar notificações ao Android)
    pTxCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_TX,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
    // Adiciona o descritor necessário para notificações no Android (Client Characteristic Configuration)
    pTxCharacteristic->addDescriptor(new BLE2902());

    // Cria a característica RX (para o ESP32 receber dados do Android)
    pRxCharacteristic = pService->createCharacteristic(
                       CHARACTERISTIC_UUID_RX,
                       BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
                     );
    pRxCharacteristic->setCallbacks(new MyCallbacks());

    // Inicia o serviço
    pService->start();

    // Configura e inicia o Advertising (para que o Android encontre o ESP32)
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    // Funções para compatibilidade com iPhone, mas úteis para Android também
    pAdvertising->setMinPreferred(0x06);  
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();
    
    Serial.println("[BOOT] BLE iniciado. Aguardando conexões...");
}

void sendBLENotification(const char* message) {
    if (deviceConnected && pTxCharacteristic != NULL) {
        pTxCharacteristic->setValue(std::string(message));
        pTxCharacteristic->notify();
        Serial.print("[TX] Enviado: ");
        Serial.println(message);
    }
}
