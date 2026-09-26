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
//   "linha: ..." na pausa   -> algo passou no RX2 sem ser perguntado; as
//                              duracoes decodificam em qualquer velocidade,
//                              e um baixo de milissegundos e reinicio do R200
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
static const unsigned long PAUSA_MS = 2500;  // entre envios, vigiando a linha

static void vigiar_pausa(unsigned long tx_ms);

// AA 00 03 00 01 00 04 DD — versao de hardware (este modulo usa AA...DD, nao BB...7E)
static const uint8_t CMD_VERSAO_HW[] = {0xAA, 0x00, 0x03, 0x00, 0x01, 0x00, 0x04, 0xDD};

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
    vigiar_pausa(inicio);
    if (i == 0) Serial.println("----");
}

// Entre um envio e outro nada deveria acontecer na linha. Fica olhando o RX2
// durante a pausa, como um analisador logico de pobre: guarda o instante de
// cada transicao e imprime as duracoes, em microssegundos, alternando nivel
// baixo e alto. Com isso da para decodificar no PC em qualquer velocidade, e
// a menor duracao entrega o tempo de bit de quem esta falando. Se o R200
// reiniciar, o TXD dele solta e o pull-down segura o RX2 em 0 por
// milissegundos. Os bytes que a Serial2 pegar na velocidade do ciclo tambem
// saem, como "fora de hora". A pausa longa separa os envios o bastante para
// associar cada bipe a uma velocidade so.
static const int MAX_TRANSICOES = 1500;
static uint32_t transicoes[MAX_TRANSICOES];

static void vigiar_pausa(unsigned long tx_ms) {
    unsigned long inicio = millis();
    int n = 0;
    int nivel = 1;
    // So o GPIO no laco, para nao perder pulso curto.
    while (millis() - inicio < PAUSA_MS) {
        int agora = gpio_get_level((gpio_num_t)PINO_RX2);
        if (agora != nivel) {
            nivel = agora;
            if (n < MAX_TRANSICOES) transicoes[n++] = micros();
        }
    }

    int bytes_fora = 0;
    while (Serial2.available()) {
        uint8_t b = Serial2.read();
        if (bytes_fora == 0) Serial.print("   fora de hora: ");
        if (b < 0x10) Serial.print('0');
        Serial.print(b, HEX);
        Serial.print(' ');
        bytes_fora++;
    }
    if (bytes_fora > 0) Serial.println();

    if (n > 0) {
        Serial.print("   linha: ");
        Serial.print(n);
        Serial.print(" transicoes, a primeira em +");
        Serial.print(transicoes[0] / 1000 - tx_ms);
        Serial.println(" ms do TX");
        Serial.print("   duracoes (us, baixo/alto alternados): ");
        for (int k = 1; k < n; k++) {
            Serial.print(transicoes[k] - transicoes[k - 1]);
            Serial.print(' ');
        }
        Serial.println();
        if (n == MAX_TRANSICOES) Serial.println("   (buffer cheio, resto perdido)");
    }
}
