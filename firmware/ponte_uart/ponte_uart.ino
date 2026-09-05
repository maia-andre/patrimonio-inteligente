// ponte_uart.ino — ESP32 como conversor USB-serial para o modulo UHF R200
//
// Para que serve: o R200 usa um CH340 no micro-USB, que precisa de driver de
// fabricante no Windows. O ESP32 DevKit usa um CH9102, que o Windows atende
// com o driver embutido (usbser), sem administrador. Este sketch faz o ESP32
// repassar bytes entre a USB (Serial) e a UART2 (Serial2), nos dois sentidos,
// sem interpretar nada. O teste_r200.py roda entao contra a porta do ESP32.
//
// Ligacao (docs/HARDWARE_R200.md, secao 3):
//   R200 TXD -> GPIO16 (RX2)     R200 RXD -> GPIO17 (TX2)     GND comum
//   R200 5V  -> fonte externa 5 V. Micro-USB do R200 DESCONECTADO enquanto
//   o ESP32 estiver nos pinos (UART compartilhada com o CH340).
//   Antena rosqueada ANTES de energizar.
//
// Nao e o firmware do projeto: e uma ferramenta de bancada. O firmware
// continua em firmware/firmware.ino.

#include <Arduino.h>

static const uint32_t BAUD = 115200;
static const int PINO_RX2 = 16;
static const int PINO_TX2 = 17;
static const int PINO_LED = 2;

void setup() {
    Serial.begin(BAUD);
    Serial2.begin(BAUD, SERIAL_8N1, PINO_RX2, PINO_TX2);
    pinMode(PINO_LED, OUTPUT);
    digitalWrite(PINO_LED, LOW);
    // Sem mensagem de boot proposital: qualquer byte que nao seja frame
    // MagicRF confunde a leitura humana do RX no script, mesmo que o parser
    // o ignore. O LED pisca no trafego.
}

void loop() {
    bool trafego = false;
    while (Serial.available()) {
        Serial2.write(Serial.read());
        trafego = true;
    }
    while (Serial2.available()) {
        Serial.write(Serial2.read());
        trafego = true;
    }
    digitalWrite(PINO_LED, trafego ? HIGH : LOW);
}
