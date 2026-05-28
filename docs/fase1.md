# Fase 1 — ESP32 + BLE + LED + Comunicação Serial

Quero criar a estrutura inicial de um projeto embarcado usando ESP32 DevKit V1.

Objetivo da Fase 1:
- Aprender ESP32
- Comunicação Serial
- Bluetooth BLE
- Controle de GPIO
- Comunicação Android ↔ ESP32

Stack:
- ESP32 DevKit V1
- Arduino IDE
- BLE nativo do ESP32
- C++

Quero uma estrutura simples e organizada para firmware.

# Objetivos Técnicos

## Meta 1 — Piscar LED
Criar firmware que:
- utilize GPIO2
- pisque LED onboard a cada 1 segundo

## Meta 2 — Serial Monitor
Adicionar logs via Serial.begin(115200)

Esperado:
- mostrar:
  - inicialização
  - status do LED
  - eventos BLE

Exemplo:
[BOOT] ESP32 iniciado
[LED] ON
[LED] OFF

## Meta 3 — BLE Server
Criar um BLE Server no ESP32.

Nome BLE:
RFID-POC-ESP32

Criar:
- 1 Service UUID customizado
- 2 Characteristics:
  - RX → receber comandos do Android
  - TX → enviar respostas ao Android

## Meta 4 — Android detectar ESP32
BLE deve:
- ficar visível
- permitir conexão
- permitir leitura/escrita

Compatível com:
- nRF Connect
- BLE Scanner

## Meta 5 — Comunicação bidirecional
Quando Android enviar:
LED_ON

ESP32:
- acende LED
- responde:
OK_LED_ON

Quando Android enviar:
LED_OFF

ESP32:
- apaga LED
- responde:
OK_LED_OFF

# Estrutura esperada

Quero uma estrutura organizada:

/firmware
  /src
    main.cpp
    ble_service.cpp
    ble_service.h
    led_controller.cpp
    led_controller.h
  /docs
    architecture.md
    ble_protocol.md

# Requisitos

- Código simples e didático
- Explicações comentadas
- Sem overengineering
- Separação mínima de responsabilidades
- Compatível com Arduino IDE
- Usar BLE nativo ESP32
- Não usar frameworks complexos

# Extras

Adicionar:
- UUIDs de exemplo
- exemplo de comando BLE
- exemplo de resposta BLE
- instruções de upload para ESP32
- instruções para testar com app BLE Scanner Android

# Resultado esperado

Ao final:
- Android conecta via BLE
- Android envia comando
- ESP32 controla LED
- ESP32 responde status
- Logs aparecem no Serial Monitor