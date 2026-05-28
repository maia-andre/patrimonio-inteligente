# Fase 2 — Aplicativo Android Kotlin para Controle BLE do ESP32

Quero criar um aplicativo Android nativo em Kotlin para comunicação BLE com ESP32.

Objetivo:
- Android detectar ESP32
- conectar via BLE
- enviar comandos
- controlar LED onboard do ESP32

Stack:
- Kotlin
- Android Studio
- BLE Android API nativa
- Material Design simples

# Funcionalidades

## Tela principal

A tela deve possuir:

- Status conexão BLE
- Botão "Conectar ESP32"
- Botão "LED ON"
- Botão "LED OFF"
- Área de logs

Exemplo logs:
[BLE] conectado
[TX] LED_ON
[RX] OK_LED_ON

# Comunicação BLE

ESP32 BLE Name:
RFID-POC-ESP32

Usar:
- Service UUID customizado
- Characteristic RX
- Characteristic TX

## Fluxo esperado

Botão:
LED ON

Android envia:
LED_ON

ESP32 responde:
OK_LED_ON

Android exibe:
LED ligado

---

Botão:
LED OFF

Android envia:
LED_OFF

ESP32 responde:
OK_LED_OFF

Android exibe:
LED desligado

# Arquitetura

Quero arquitetura simples e limpa:

/app
  /ble
    BleManager.kt
    BleConstants.kt
  /ui
    MainActivity.kt
  /model
    BleMessage.kt

# Requisitos técnicos

- BLE scanning
- conexão BLE
- write characteristic
- notify characteristic
- tratamento básico de erro
- logs em tela
- código didático
- evitar overengineering

# Permissões Android

Adicionar:
- BLUETOOTH
- BLUETOOTH_CONNECT
- BLUETOOTH_SCAN
- localização (se necessário)

Compatível com:
- Android 12+

# UI

Interface simples:
- Material Design básico
- sem Compose inicialmente
- XML tradicional pode ser usado

# Extras

Adicionar:
- explicação do fluxo BLE
- explicação de Service/Characteristic
- explicação do callback de notify
- instruções para testar no Android real

# Resultado esperado

Ao final:
- Android encontra ESP32
- conecta via BLE
- envia comandos
- ESP32 responde
- LED acende/apaga
- logs aparecem na tela