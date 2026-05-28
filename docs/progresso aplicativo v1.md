# Progresso Aplicativo Android - v1 (Fase 2 Concluída)

## Resumo
A Fase 2 focada no desenvolvimento do aplicativo Android nativo em Kotlin foi finalizada. O objetivo principal era construir um aplicativo simples capaz de buscar dispositivos BLE, conectar-se ao ESP32 configurado na Fase 1, e trocar mensagens (enviar comandos e receber notificações) de forma bidirecional.

## Estrutura do Aplicativo Criada
O código foi estruturado de forma modularizada no pacote `com.patrimoniosjc.rfidpoc`, seguindo a arquitetura abaixo:

*   **`ui/MainActivity.kt` e `res/layout/activity_main.xml`**:
    *   **UI (XML)**: Interface simples contendo um visor de status de conexão, botões para Conectar/Desconectar, botões para ligar/desligar o LED (`LED_ON`, `LED_OFF`) e um console de logs rolável em tempo real.
    *   **Lógica da Activity**: Lida com a solicitação dinâmica de permissões ao usuário, integração com os botões e atualização visual da tela baseada nos eventos do `BleManager`.

*   **`ble/BleManager.kt`**: A classe principal que encapsula toda a complexidade do Bluetooth. Ela é responsável por:
    *   Fazer o *scanning* usando o `BluetoothLeScanner` e procurar especificamente pelo nome "RFID-POC-ESP32".
    *   Conectar-se ao dispositivo via servidor GATT.
    *   Mapear e interagir com as características TX e RX.
    *   **Escrita**: Enviar um array de bytes (`LED_ON` ou `LED_OFF`) na Característica RX.
    *   **Leitura/Notificação**: Habilitar as notificações na Característica TX para ouvir quando o ESP32 envia as respostas (`OK_LED_ON`, `OK_LED_OFF`).

*   **`ble/BleConstants.kt`**: Arquivo de constantes contendo:
    *   O nome do dispositivo alvo (`RFID-POC-ESP32`).
    *   Os mesmos UUIDs definidos no código do ESP32 (Service, RX, TX) para que o aplicativo saiba exatamente em quais "endereços" se comunicar.

*   **`model/BleMessage.kt`**: Um modelo de dados (data class) preparado para escalar a estrutura de logs e mensagens internamente, classificando as mensagens entre *LOG* interno, *TX* (enviado) e *RX* (recebido).

*   **`AndroidManifest.xml`**:
    *   Foi configurado com o suporte completo para permissões legado (Android 11 ou inferior) e as novas exigências de permissão granulares do Android 12+ (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`).
    *   Permissão de Localização (`ACCESS_FINE_LOCATION`) mantida por ser um requisito do Android para realizar varreduras BLE.

## Como Testar
Devido à necessidade de hardware Bluetooth físico, não é possível testar esse fluxo no emulador do Android Studio.
1. Conecte o ESP32 na energia, já com o firmware da Fase 1 carregado.
2. Ative o "Modo Desenvolvedor" (Developer Options) e a "Depuração USB" (USB Debugging) no seu celular Android.
3. Conecte o celular ao computador via cabo USB.
4. No Android Studio, selecione o seu dispositivo físico na barra superior e clique em **Run** (Play).
5. Após o app abrir no seu celular:
   - Aceite o prompt de permissões.
   - Clique em "Conectar ESP32".
   - Verifique os logs até o status alterar para "Conectado".
   - Teste enviando os comandos pelos botões do LED.
