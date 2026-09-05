// diag_r200.ino — diagnostico da UART entre o ESP32 e o modulo UHF R200
//
// Ferramenta de bancada, nao o firmware do projeto. Sem PC no meio: o ESP32
// manda sozinho o comando de versao de hardware pela Serial2, varrendo as
// velocidades da lista BAUDS, e imprime no monitor serial (115200) tudo que
// voltar, em hexadecimal, alem do nivel de repouso do pino RX2. Serve para
// mexer nos fios com o monitor aberto e ver a resposta aparecer, e para
// descobrir em que velocidade o modulo esta: e a que devolver um frame
// limpo comecando com BB 01 03.
//
// Como ler:
//   RX2 em repouso = 1  -> o TXD do R200 esta ligado e o modulo esta ligado
//                          (pull-up interno desligado, entao e prova real)
//   RX2 em repouso = 0  -> fio TXD solto, sem contato no furo, trocado,
//                          ou modulo sem energia
//   resposta BB 01 03 ... -> UART funcionando nos dois sentidos
//   nenhuma resposta com RX2 = 1 -> o R200 nao esta recebendo: RXD solto,
//                                    trocado, ou a linha presa pelo CH340
//
// Ligacao: docs/HARDWARE_R200.md, secao 3.

#include <Arduino.h>
#include <driver/gpio.h>

static const uint32_t BAUD = 115200;  // monitor serial (USB)
static const uint32_t BAUDS[] = {9600, 19200, 38400, 57600, 115200, 230400};
static const size_t N_BAUDS = sizeof(BAUDS) / sizeof(BAUDS[0]);
static const int PINO_RX2 = 16;
static const int PINO_TX2 = 17;
static const int PINO_LED = 2;

// BB 00 03 00 01 00 04 7E — versao de hardware
static const uint8_t CMD_VERSAO_HW[] = {0xBB, 0x00, 0x03, 0x00, 0x01, 0x00, 0x04, 0x7E};

void setup() {
    Serial.begin(BAUD);
    Serial2.begin(BAUDS[0], SERIAL_8N1, PINO_RX2, PINO_TX2);
    // O driver da UART liga um pull-up interno no RX2, que sozinho ja daria
    // "repouso = 1" com o fio solto. Troca por pull-down fraco: agora so le 1
    // se o TXD do R200 estiver de fato segurando a linha em nivel alto.
    gpio_pullup_dis((gpio_num_t)PINO_RX2);
    gpio_pulldown_en((gpio_num_t)PINO_RX2);
    pinMode(PINO_LED, OUTPUT);
    delay(500);
    Serial.println();
    Serial.println("[diag] ESP32 <-> R200 pela Serial2 (RX2=16, TX2=17) a 115200");
    Serial.println("[diag] versao de hardware em cada velocidade da lista, em ciclo");
}

void loop() {
    static size_t i = 0;
    uint32_t baud = BAUDS[i];
    i = (i + 1) % N_BAUDS;
    Serial2.updateBaudRate(baud);
    delay(50);

    int repouso = digitalRead(PINO_RX2);
    Serial.print("RX2 em repouso = ");
    Serial.print(repouso);
    Serial.print(" | ");
    Serial.print(baud);
    Serial.print(" baud | TX: ");
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
    if (i == 0) Serial.println("----");
    delay(600);
}
