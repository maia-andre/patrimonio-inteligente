# Progresso Arduino - v1 (Fase 1 Concluída)

## Resumo
A Fase 1 focada na criação do firmware para o ESP32 foi finalizada com sucesso. O objetivo era configurar o ESP32 para se comportar como um Servidor BLE, permitindo a conexão de um dispositivo Android, recebendo comandos para ligar/desligar o LED onboard e enviando confirmações de volta via notificações BLE.

## Estrutura do Firmware Criada
Os arquivos foram gerados na pasta `firmware` na raiz do projeto, estruturados da seguinte forma para facilitar a abertura na Arduino IDE:

*   **`firmware.ino`**: O arquivo principal (Main) que inicializa a porta Serial, o LED e o serviço BLE. Ele também lida com o loop principal para reiniciar a publicidade (Advertising) caso o dispositivo perca a conexão.
*   **`led_controller.h` e `led_controller.cpp`**: Módulo dedicado ao controle físico do pino do LED (GPIO 2 no ESP32 DevKit V1). Contém funções simples como `turnOn()`, `turnOff()`, e `toggle()`.
*   **`ble_service.h` e `ble_service.cpp`**: O núcleo da comunicação Bluetooth. 
    *   Cria um servidor BLE chamado **"RFID-POC-ESP32"**.
    *   Expõe um **Service UUID**.
    *   Gerencia uma **Characteristic RX** para o aplicativo Android *escrever* comandos (`LED_ON`, `LED_OFF`).
    *   Gerencia uma **Characteristic TX** para o ESP32 *notificar* o aplicativo Android sobre o status da operação (`OK_LED_ON`, `OK_LED_OFF`).

## UUIDs Utilizados
Os UUIDs a seguir foram gerados e configurados de forma fixa no firmware:
*   **Service UUID**: `6E400001-B5A3-F393-E0A9-E50E24DCCA9E`
*   **Characteristic RX (Receber do Android)**: `6E400002-B5A3-F393-E0A9-E50E24DCCA9E`
*   **Characteristic TX (Enviar para Android)**: `6E400003-B5A3-F393-E0A9-E50E24DCCA9E`

## Como Testar Manualmente
1. Abra o arquivo `firmware/firmware.ino` na Arduino IDE.
2. Certifique-se de que a placa "DOIT ESP32 DEVKIT V1" está selecionada.
3. Compile e faça o upload para o ESP32.
4. Abra o **Serial Monitor** (baud rate 115200) para ver os logs de inicialização.
5. No celular, use um aplicativo como o **nRF Connect for Mobile** ou **BLE Scanner**.
6. Conecte-se ao dispositivo **RFID-POC-ESP32**.
7. Envie a string (texto) `LED_ON` na Característica RX. O LED azul do ESP32 deve acender e a Característica TX enviará `OK_LED_ON` de volta.
8. Envie a string `LED_OFF` para apagar o LED.
