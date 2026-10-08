// uhf_r200.h — modulo UHF YPD-R200 (MagicRF M100) sobre a UART2 do ESP32
//
// Protocolo e parser portados do teste_r200.py, que leu as primeiras tags em
// 26/09/2026 (docs/HARDWARE_R200.md, secoes 4 e 8d). Moldura deste modulo:
//   AA | Type | Cmd | LenMSB | LenLSB | Params... | Checksum | DD
// e nao BB ... 7E como na documentacao do MagicRF.

#ifndef UHF_R200_H
#define UHF_R200_H

#include <Arduino.h>

// Maior EPC que o protocolo admite: 62 bytes, 124 caracteres hex.
#define UHF_EPC_MAX_BYTES 62

struct LeituraTag {
    char epc[UHF_EPC_MAX_BYTES * 2 + 1];  // hexadecimal maiusculo
    int rssi;                              // dBm
};

class UhfR200 {
public:
    // Abre a UART, espera o modulo responder e configura regiao e potencia.
    // Devolve false se o modulo nao respondeu; o resto do firmware segue.
    bool begin(HardwareSerial& porta, int pinoRx, int pinoTx);

    void iniciarInventario();
    void pararInventario();
    bool inventarioAtivo() const { return ativo; }

    // Chamar a cada volta do loop(). Consome a UART e devolve true, com a
    // tag preenchida, a cada notificacao de tag. Tambem rearma o inventario
    // continuo quando ele termina sozinho.
    bool proximaTag(LeituraTag& tag);

private:
    // Maior frame valido: 7 bytes de moldura + 128 de parametros. Acima
    // disso o tamanho lido e de cabecalho falso (ver extrairFrame).
    static const size_t MAX_PARAMS = 128;
    static const size_t FRAME_MINIMO = 7;
    static const size_t TAM_BUFFER = 512;

    HardwareSerial* uart = nullptr;
    uint8_t buffer[TAM_BUFFER];
    size_t ocupado = 0;
    bool ativo = false;
    unsigned long ultimoFrameMs = 0;

    void enviarFrame(uint8_t cmd, const uint8_t* params, size_t tamParams);
    void encherBuffer();
    void descartar(size_t n);
    bool extrairFrame(uint8_t* frame, size_t& tamFrame);
    bool comando(uint8_t cmd, const uint8_t* params, size_t tamParams,
                 uint8_t* resposta, size_t& tamResposta, unsigned long timeoutMs);
    void registrarInfo(uint8_t tipoInfo, const char* rotulo);
    bool configurar(uint8_t cmd, const uint8_t* params, size_t tamParams, const char* rotulo);
    void registrarFrameSemTag(const uint8_t* frame, size_t tamFrame);
};

#endif // UHF_R200_H
