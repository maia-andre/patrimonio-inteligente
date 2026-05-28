# 🏷️ RFID POC - Inventário Patrimonial

Bem-vindo ao projeto **RFID POC**, uma prova de conceito focada na integração de hardware embarcado (ESP32) com um aplicativo móvel nativo (Android/Kotlin) utilizando comunicação **Bluetooth Low Energy (BLE)**.

O intuito desta POC é desenvolver uma solução integrada (App + Leitor + Módulo ESP32) para a detecção de **Tags RFID UHF** anexadas a bens móveis (números de placas patrimoniais). O objetivo final é automatizar e agilizar o controle e inventário patrimonial do setor público de São José dos Campos (SJC).

Atualmente, o projeto concluiu sua etapa fundacional (Fases 1 e 2), estabelecendo um canal de comunicação bidirecional robusto entre o smartphone e o microcontrolador, validando o envio de comandos e o recebimento de respostas em tempo real.

---

## 🏗️ Arquitetura do Projeto

O ecossistema é dividido em duas frentes principais: **Firmware (ESP32)** e **Software (Android App)**.

```mermaid
graph TD
    subgraph Android App [📱 Aplicativo Android Kotlin]
        UI[MainActivity UI]
        BLE_MGR[BleManager]
        UI <-->|Comandos / Status| BLE_MGR
    end

    subgraph BLE [📶 Bluetooth Low Energy]
        RX[Característica RX]
        TX[Característica TX]
    end

    subgraph Hardware [🔌 ESP32 DevKit V1]
        FIRM[Firmware C++]
        LED((LED Onboard))
        RFID[[Leitor RFID UHF]]
        
        FIRM -->|GPIO 2| LED
        FIRM -.->|SPI| RFID
    end

    BLE_MGR -->|Escreve 'LED_ON'| RX
    TX -->|Notifica 'OK_LED_ON'| BLE_MGR

    RX --> FIRM
    FIRM --> TX
```

### 1. Firmware (ESP32 / Arduino IDE)
Desenvolvido em C++, o ESP32 atua como um **Servidor BLE** (`RFID-POC-ESP32`).
- **Service UUID**: Define o "serviço de automação/RFID" do dispositivo.
- **RX Characteristic**: Ponto de entrada. Ouve ativamente os comandos disparados pelo celular.
- **TX Characteristic**: Ponto de saída. Emite notificações (Subscribe) para o celular informando o resultado de uma operação.
- **Led Controller**: Módulo isolado de controle físico de portas (GPIO).

### 2. Software (Android App / Kotlin)
Aplicativo nativo moderno preparado para as exigências de privacidade e permissões do Android 12+.
- **`BleManager`**: O motor do aplicativo. Faz a varredura (*Scanning*), conecta-se ao servidor GATT do ESP32 e gerencia os descritores de notificação.
- **`MainActivity`**: Interface gráfica amigável com logs em tempo real na tela, permitindo auditoria visual dos pacotes trocados (`[TX]` e `[RX]`).

---

## ✅ O que já temos rodando (Estado Atual)

Neste exato momento, o projeto permite:
1. **Descoberta**: O celular Android detecta a presença do ESP32 via rádio BLE.
2. **Handshake**: A conexão GATT é firmada de forma segura.
3. **Controle Bidirecional**: 
   - Ao apertar `LED ON` no app, a string é convertida em *Bytes*, viaja pelo ar, o ESP32 intercepta na porta RX, liga o LED azul físico e manda a string `OK_LED_ON` na porta TX.
   - O aplicativo ouve a notificação e atualiza a interface instantaneamente.

---

## 🚀 Próximos Passos (Evolução para Inventário RFID UHF)

Com a espinha dorsal de comunicação BLE validada, a base está pronta para a verdadeira natureza da "Proof of Concept": **A integração do módulo leitor RFID UHF**.

### Próximas Implementações Esperadas:
- [ ] **Integração de Hardware (ESP32 + Leitor UHF)**: Conectar um leitor RFID UHF compatível ao ESP32 (via UART ou SPI).
- [ ] **Firmware de Leitura Patrimonial**: Implementar a lógica no ESP32 para realizar a varredura contínua ou sob demanda e detectar Tags RFID UHF (EPC) das placas patrimoniais.
- [ ] **Notificação Proativa via BLE**: Quando uma tag patrimonial for lida, o ESP32 enviará automaticamente o código/número da placa pela característica TX (`TAG_READ: SJC-123456`) de forma assíncrona.
- [ ] **App Android (Inventário e Processamento)**: O aplicativo Android receberá o número da placa patrimonial e o processará na tela, permitindo a contagem rápida e a reconciliação de itens durante o processo de inventário.
- [ ] **Refatoração UI/UX**: Melhorar a interface do aplicativo para focar em "Auditoria e Inventário de Bens" (listagens de itens lidos, contadores) em vez de um simples painel de botões.
