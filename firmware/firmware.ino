#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include "led_controller.h"
#include "ble_service.h"
#include "uhf_r200.h"

// Variáveis globais úteis
LedController led;
UhfR200 uhf;

// Ligação do R200 (docs/HARDWARE_R200.md, seção 3): J3 TXD -> RX2, J3 RXD -> TX2
const int PINO_RX2 = 16;
const int PINO_TX2 = 17;

// Controle de reconexão do BLE
extern BLEServer* pServer;
extern bool deviceConnected;
extern bool oldDeviceConnected;

// Janela de silêncio por EPC: o R200 notifica a mesma tag ~20 vezes por
// segundo, e cada envio BLE fragmentado custa ~150 ms. Uma tag só volta a
// ser enviada depois desta janela.
const unsigned long JANELA_EPC_MS = 3000;
const int MAX_TAGS_RECENTES = 32;

struct TagRecente {
    char epc[UHF_EPC_MAX_BYTES * 2 + 1];
    unsigned long enviadaMs;
    uint16_t leituras;  // leituras desde o último envio
    bool usada;
};
TagRecente tagsRecentes[MAX_TAGS_RECENTES];

// Comandos também pela Serial USB, para testar na bancada sem o celular
String linhaSerial;

// Devolve true se a tag deve ser enviada agora; em *leituras, quantas vezes
// ela foi lida desde o último envio.
bool registrarLeitura(const char* epc, uint16_t* leituras) {
    unsigned long agora = millis();
    int livre = -1;
    int maisAntiga = 0;
    for (int i = 0; i < MAX_TAGS_RECENTES; i++) {
        TagRecente& t = tagsRecentes[i];
        if (!t.usada) {
            if (livre < 0) livre = i;
            continue;
        }
        if (strcmp(t.epc, epc) == 0) {
            t.leituras++;
            if (agora - t.enviadaMs < JANELA_EPC_MS) return false;
            *leituras = t.leituras;
            t.enviadaMs = agora;
            t.leituras = 0;
            return true;
        }
        if (t.enviadaMs < tagsRecentes[maisAntiga].enviadaMs) maisAntiga = i;
    }
    // Tag nova: ocupa uma posição livre ou a da tag enviada há mais tempo
    TagRecente& t = tagsRecentes[livre >= 0 ? livre : maisAntiga];
    strncpy(t.epc, epc, sizeof(t.epc) - 1);
    t.epc[sizeof(t.epc) - 1] = '\0';
    t.enviadaMs = agora;
    t.leituras = 0;
    t.usada = true;
    *leituras = 1;
    return true;
}

void iniciarScanner() {
    for (int i = 0; i < MAX_TAGS_RECENTES; i++) tagsRecentes[i].usada = false;
    uhf.iniciarInventario();
    led.turnOn();
}

void pararScanner() {
    uhf.pararInventario();
    led.turnOff();
    sendBLENotification("SCANNER_OFF");
}

void executarComando(ComandoScanner comando) {
    if (comando == COMANDO_SCAN_START) iniciarScanner();
    else if (comando == COMANDO_SCAN_STOP) pararScanner();
}

void lerComandoSerial() {
    while (Serial.available()) {
        char c = Serial.read();
        if (c == '\n' || c == '\r') {
            if (linhaSerial.length() > 0) {
                ComandoScanner comando = interpretarComando(linhaSerial);
                Serial.printf("[USB] Recebido: %s\n", linhaSerial.c_str());
                if (comando == COMANDO_NENHUM) Serial.println("[USB] Comando desconhecido, ignorado");
                executarComando(comando);
                linhaSerial = "";
            }
        } else if (linhaSerial.length() < 32) {
            linhaSerial += c;
        }
    }
}

void setup() {
    // 1. Inicializa o Monitor Serial
    Serial.begin(115200);
    delay(1000); // Pequeno atraso para o Serial Monitor estabilizar
    Serial.println("\n[BOOT] ESP32 iniciado");

    // 2. Inicializa o LED
    led.begin();
    Serial.println("[BOOT] LED configurado");

    // 3. Inicializa o R200 antes do BLE, para o log do boot sair em ordem
    Serial.println("[BOOT] Inicializando R200...");
    if (uhf.begin(Serial2, PINO_RX2, PINO_TX2)) {
        Serial.println("[BOOT] R200 pronto");
    } else {
        Serial.println("[BOOT] R200 com falha; SCAN_START tenta mesmo assim");
    }

    // 4. Inicializa o BLE
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
    // Se o dispositivo desconectou
    if (!deviceConnected && oldDeviceConnected) {
        // Ninguém mais ouve: o rádio do R200 não fica transmitindo à toa
        if (uhf.inventarioAtivo()) pararScanner();
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

    // Comandos chegam pelo BLE (registrados no callback do ble_service.cpp)
    // ou pela Serial USB; os dois são executados aqui.
    executarComando(lerComandoPendente());
    lerComandoSerial();

    // Payload no formato da RN-03: "EPC;" (código = EPC, descrição vazia).
    // O RSSI fica só no log serial.
    LeituraTag tag;
    while (uhf.proximaTag(tag)) {
        uint16_t leituras = 0;
        if (!registrarLeitura(tag.epc, &leituras)) continue;
        Serial.printf("[TAG] %s  RSSI %d dBm  (%u leituras)%s\n", tag.epc, tag.rssi, (unsigned)leituras,
                      deviceConnected ? "" : "  [sem BLE conectado]");
        String payload = String(tag.epc) + ";";
        sendBLELongMessage(payload.c_str());
    }
}
