// diag_r200.ino — diagnostico da UART entre o ESP32 e o modulo UHF R200
//
// Ferramenta de bancada, nao o firmware do projeto. Sem PC no meio: o ESP32
// manda sozinho o comando de versao de hardware a cada 2 s pela Serial2 e
// imprime no monitor serial (115200) tudo que voltar, em hexadecimal, alem
// do nivel de repouso do pino RX2. Serve para mexer nos fios com o monitor
// aberto e ver a resposta aparecer.
//
// Como ler:
//   RX2 em repouso = 1  -> o TXD do R200 esta ligado e o modulo esta ligado
//   RX2 em repouso = 0  -> fio TXD solto, trocado, ou modulo sem energia
//   resposta BB 01 03 ... -> UART funcionando nos dois sentidos
//   nenhuma resposta com RX2 = 1 -> o R200 nao esta recebendo: RXD solto,
//                                    trocado, ou a linha presa pelo CH340
//
// Ligacao: docs/HARDWARE_R200.md, secao 3.

#include <Arduino.h>

static const uint32_t BAUD = 115200;
static const int PINO_RX2 = 16;
static const int PINO_TX2 = 17;
static const int PINO_LED = 2;

// BB 00 03 00 01 00 04 7E — versao de hardware
static const uint8_t CMD_VERSAO_HW[] = {0xBB, 0x00, 0x03, 0x00, 0x01, 0x00, 0x04, 0x7E};

void setup() {
    Serial.begin(BAUD);
    Serial2.begin(BAUD, SERIAL_8N1, PINO_RX2, PINO_TX2);
    pinMode(PINO_LED, OUTPUT);
    delay(500);
    Serial.println();
    Serial.println("[diag] ESP32 <-> R200 pela Serial2 (RX2=16, TX2=17) a 115200");
    Serial.println("[diag] mandando versao de hardware a cada 2 s");
}

void loop() {
    int repouso = digitalRead(PINO_RX2);
    Serial.print("RX2 em repouso = ");
    Serial.print(repouso);
    Serial.print(" | TX: ");
    for (size_t i = 0; i < sizeof(CMD_VERSAO_HW); i++) {
        if (CMD_VERSAO_HW[i] < 0x10) Serial.print('0');
        Serial.print(CMD_VERSAO_HW[i], HEX);
        Serial.print(' ');
    }

    while (Serial2.available()) Serial2.read();  // limpa lixo anterior
    digitalWrite(PINO_LED, HIGH);
    Serial2.write(CMD_VERSAO_HW, sizeof(CMD_VERSAO_HW));
    Serial2.flush();

    unsigned long inicio = millis();
    int recebidos = 0;
    Serial.print("| RX: ");
    while (millis() - inicio < 400) {
        while (Serial2.available()) {
            uint8_t b = Serial2.read();
            if (b < 0x10) Serial.print('0');
            Serial.print(b, HEX);
            Serial.print(' ');
            recebidos++;
        }
    }
    digitalWrite(PINO_LED, LOW);
    if (recebidos == 0) Serial.print("(sem resposta)");
    Serial.println();
    delay(1600);
}
