#include "uhf_r200.h"

static const uint32_t BAUD = 115200;  // confirmado em 26/09/2026

static const uint8_t FRAME_HEAD = 0xAA;
static const uint8_t FRAME_END = 0xDD;

static const uint8_t TIPO_COMANDO = 0x00;
static const uint8_t TIPO_RESPOSTA = 0x01;
static const uint8_t TIPO_NOTIFICACAO = 0x02;

static const uint8_t CMD_INFO = 0x03;
static const uint8_t CMD_SET_REGIAO = 0x07;
static const uint8_t CMD_INVENTARIO_UNICO = 0x22;  // tambem o cmd da notificacao de tag
static const uint8_t CMD_INVENTARIO_MULTI = 0x27;
static const uint8_t CMD_PARAR = 0x28;
static const uint8_t CMD_SET_POTENCIA = 0xB6;
static const uint8_t CMD_ERRO = 0xFF;

// No MagicRF, 0x15 e a rodada de inventario que nao achou tag. Com o
// inventario ativo e nenhuma tag na antena, pode chegar a cada rodada;
// nao vale uma linha de log por vez. A conferir na bancada.
static const uint8_t ERRO_SEM_TAG = 0x15;

static const uint8_t REGIAO_US = 0x02;             // 902-928 MHz (secao 5 do HARDWARE_R200)
static const uint16_t POTENCIA_CENTESIMOS = 1800;  // 18,00 dBm, a potencia de bancada
static const uint16_t RODADAS_INVENTARIO = 10000;  // a mesma do teste_r200.py

// O inventario continuo termina depois das rodadas pedidas. Com ele ativo,
// silencio total da UART por este tempo e tomado como fim, e ele e rearmado.
static const unsigned long REARME_MS = 3000;

static const int TENTATIVAS_BOOT = 8;
static const unsigned long TIMEOUT_RESPOSTA_MS = 500;

static void imprimirHex(const uint8_t* dados, size_t tam) {
    for (size_t i = 0; i < tam; i++) {
        if (dados[i] < 0x10) Serial.print('0');
        Serial.print(dados[i], HEX);
        if (i + 1 < tam) Serial.print(' ');
    }
}

bool UhfR200::begin(HardwareSerial& porta, int pinoRx, int pinoTx) {
    uart = &porta;
    // Folga para as notificacoes que chegam enquanto o loop() envia pelo BLE.
    uart->setRxBufferSize(1024);
    uart->begin(BAUD, SERIAL_8N1, pinoRx, pinoTx);

    // O R200 liga junto com o ESP32 e leva alguns segundos para aceitar
    // comando. Pergunta-se a versao de hardware ate ele responder.
    uint8_t resposta[FRAME_MINIMO + MAX_PARAMS];
    size_t tamResposta = 0;
    const uint8_t versaoHardware = 0x00;
    bool respondeu = false;
    for (int i = 0; i < TENTATIVAS_BOOT && !respondeu; i++) {
        respondeu = comando(CMD_INFO, &versaoHardware, 1, resposta, tamResposta, TIMEOUT_RESPOSTA_MS)
                    && resposta[2] == CMD_INFO;
    }
    if (!respondeu) {
        Serial.println("[UHF] Modulo nao respondeu. Conferir alimentacao, fiacao e antena.");
        return false;
    }

    registrarInfo(0x00, "Hardware");
    registrarInfo(0x01, "Firmware");
    registrarInfo(0x02, "Fabricante");

    const uint8_t regiao = REGIAO_US;
    const uint8_t potencia[] = {(uint8_t)(POTENCIA_CENTESIMOS >> 8), (uint8_t)(POTENCIA_CENTESIMOS & 0xFF)};
    bool ok = configurar(CMD_SET_REGIAO, &regiao, 1, "Regiao 902-928 MHz");
    ok = configurar(CMD_SET_POTENCIA, potencia, sizeof(potencia), "Potencia 18,00 dBm") && ok;
    return ok;
}

void UhfR200::iniciarInventario() {
    if (!uart) return;
    const uint8_t params[] = {CMD_INVENTARIO_UNICO,
                              (uint8_t)(RODADAS_INVENTARIO >> 8), (uint8_t)(RODADAS_INVENTARIO & 0xFF)};
    enviarFrame(CMD_INVENTARIO_MULTI, params, sizeof(params));
    ativo = true;
    ultimoFrameMs = millis();
    Serial.println("[UHF] Inventario iniciado");
}

void UhfR200::pararInventario() {
    if (!uart) return;
    // A resposta (AA 01 28 00 01 00 2A DD) chega depois e sai no log pelo
    // proximaTag(); tags que ainda cheguem ate la sao descartadas.
    enviarFrame(CMD_PARAR, nullptr, 0);
    ativo = false;
    Serial.println("[UHF] Inventario parado");
}

bool UhfR200::proximaTag(LeituraTag& tag) {
    if (!uart) return false;
    encherBuffer();

    uint8_t frame[FRAME_MINIMO + MAX_PARAMS];
    size_t tamFrame = 0;
    while (extrairFrame(frame, tamFrame)) {
        if (frame[1] != TIPO_NOTIFICACAO || frame[2] != CMD_INVENTARIO_UNICO) {
            registrarFrameSemTag(frame, tamFrame);
            continue;
        }
        if (!ativo) continue;

        // Params: RSSI (1) | PC (2) | EPC (n) | CRC (2)
        const uint8_t* params = frame + 5;
        size_t tamParams = tamFrame - FRAME_MINIMO;
        if (tamParams < 5) continue;
        size_t tamEpc = tamParams - 5;
        if (tamEpc == 0 || tamEpc > UHF_EPC_MAX_BYTES) continue;

        tag.rssi = (int8_t)params[0];  // byte com sinal
        static const char HEX_MAIUSCULO[] = "0123456789ABCDEF";
        for (size_t i = 0; i < tamEpc; i++) {
            tag.epc[2 * i] = HEX_MAIUSCULO[params[3 + i] >> 4];
            tag.epc[2 * i + 1] = HEX_MAIUSCULO[params[3 + i] & 0x0F];
        }
        tag.epc[2 * tamEpc] = '\0';
        return true;
    }

    if (ativo && millis() - ultimoFrameMs > REARME_MS) {
        Serial.println("[UHF] UART em silencio com inventario ativo: rearmando");
        iniciarInventario();
    }
    return false;
}

void UhfR200::enviarFrame(uint8_t cmd, const uint8_t* params, size_t tamParams) {
    uint8_t frame[FRAME_MINIMO + MAX_PARAMS];
    frame[0] = FRAME_HEAD;
    frame[1] = TIPO_COMANDO;
    frame[2] = cmd;
    frame[3] = (tamParams >> 8) & 0xFF;
    frame[4] = tamParams & 0xFF;
    if (tamParams > 0) memcpy(frame + 5, params, tamParams);
    uint8_t soma = 0;
    for (size_t i = 1; i < 5 + tamParams; i++) soma += frame[i];
    frame[5 + tamParams] = soma;
    frame[6 + tamParams] = FRAME_END;
    uart->write(frame, FRAME_MINIMO + tamParams);
}

void UhfR200::encherBuffer() {
    size_t disponivel = uart->available();
    size_t espaco = TAM_BUFFER - ocupado;
    size_t n = disponivel < espaco ? disponivel : espaco;
    if (n > 0) ocupado += uart->read(buffer + ocupado, n);
}

void UhfR200::descartar(size_t n) {
    if (n >= ocupado) {
        ocupado = 0;
        return;
    }
    memmove(buffer, buffer + n, ocupado - n);
    ocupado -= n;
}

// Um 0xAA pode aparecer dentro do EPC ou do RSSI. Por isso nada e descartado
// antes de validar o frame inteiro: se o candidato falha no tamanho, no 0xDD
// ou no checksum, descarta-se so o primeiro byte e procura-se o proximo 0xAA.
// Mesma logica do extrair_frames() do teste_r200.py.
bool UhfR200::extrairFrame(uint8_t* frame, size_t& tamFrame) {
    while (true) {
        size_t inicio = 0;
        while (inicio < ocupado && buffer[inicio] != FRAME_HEAD) inicio++;
        descartar(inicio);

        if (ocupado < FRAME_MINIMO) return false;  // frame ainda chegando

        size_t tamParams = ((size_t)buffer[3] << 8) | buffer[4];
        if (tamParams > MAX_PARAMS) {
            descartar(1);  // cabecalho falso: tamanho impossivel
            continue;
        }

        size_t tamTotal = FRAME_MINIMO + tamParams;
        if (ocupado < tamTotal) return false;  // frame ainda chegando

        uint8_t soma = 0;
        for (size_t i = 1; i < tamTotal - 2; i++) soma += buffer[i];
        bool fimCerto = buffer[tamTotal - 1] == FRAME_END;
        if (!fimCerto || soma != buffer[tamTotal - 2]) {
            if (fimCerto) Serial.println("[UHF] aviso: checksum invalido, frame descartado");
            descartar(1);  // descarta so o cabecalho falso, preserva o resto
            continue;
        }

        memcpy(frame, buffer, tamTotal);
        tamFrame = tamTotal;
        descartar(tamTotal);
        ultimoFrameMs = millis();
        return true;
    }
}

// Envia um comando e espera a resposta a ele (ou um erro 0xFF). Frames de
// outros tipos que cheguem no meio sao descartados: so e usado no boot,
// antes de qualquer inventario.
bool UhfR200::comando(uint8_t cmd, const uint8_t* params, size_t tamParams,
                      uint8_t* resposta, size_t& tamResposta, unsigned long timeoutMs) {
    enviarFrame(cmd, params, tamParams);
    unsigned long inicio = millis();
    while (millis() - inicio < timeoutMs) {
        encherBuffer();
        while (extrairFrame(resposta, tamResposta)) {
            if (resposta[1] == TIPO_RESPOSTA && (resposta[2] == cmd || resposta[2] == CMD_ERRO)) {
                return true;
            }
        }
        delay(5);
    }
    return false;
}

void UhfR200::registrarInfo(uint8_t tipoInfo, const char* rotulo) {
    uint8_t resposta[FRAME_MINIMO + MAX_PARAMS];
    size_t tamResposta = 0;
    Serial.printf("[UHF] %s: ", rotulo);
    if (!comando(CMD_INFO, &tipoInfo, 1, resposta, tamResposta, TIMEOUT_RESPOSTA_MS)
        || resposta[2] != CMD_INFO) {
        Serial.println("(sem resposta)");
        return;
    }
    // Params: tipo da informacao (1) | texto ASCII
    size_t tamParams = tamResposta - FRAME_MINIMO;
    for (size_t i = 1; i < tamParams; i++) Serial.write(resposta[5 + i]);
    Serial.println();
}

bool UhfR200::configurar(uint8_t cmd, const uint8_t* params, size_t tamParams, const char* rotulo) {
    uint8_t resposta[FRAME_MINIMO + MAX_PARAMS];
    size_t tamResposta = 0;
    bool ok = comando(cmd, params, tamParams, resposta, tamResposta, TIMEOUT_RESPOSTA_MS)
              && resposta[2] == cmd && tamResposta > FRAME_MINIMO && resposta[5] == 0x00;
    Serial.printf("[UHF] %s: %s\n", rotulo, ok ? "ok" : "FALHOU");
    return ok;
}

void UhfR200::registrarFrameSemTag(const uint8_t* frame, size_t tamFrame) {
    if (frame[1] == TIPO_RESPOSTA && frame[2] == CMD_ERRO
        && tamFrame > FRAME_MINIMO && frame[5] == ERRO_SEM_TAG) {
        return;
    }
    Serial.print("[UHF] frame: ");
    imprimirHex(frame, tamFrame);
    Serial.println();
}
