# Progresso Geral v2 — Sessão de 30/05/2026

## Contexto da Sessão
Esta sessão deu continuidade ao trabalho iniciado na v1, onde o firmware do ESP32 (Fase 1) e o aplicativo Android (Fase 2) foram criados e validados independentemente. O objetivo desta sessão era **integrar ambos**, resolver os problemas de build do Android, e evoluir a POC para um cenário mais próximo do uso real: um **Simulador de Scanner Patrimonial**.

---

## 1. Resolução do Build Android (Gradle)

### Problema
O projeto Android foi gerado pelo Android Studio com o **AGP (Android Gradle Plugin) 9.2.1**, que é uma versão muito recente e com mudanças estruturais significativas. Isso causou um conflito persistente:

```
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

### Causa Raiz Descoberta
O **AGP 9.x embute o plugin `org.jetbrains.kotlin.android` internamente**. Quando o `build.gradle.kts` também declarava esse plugin separadamente (como era padrão nas versões anteriores do AGP), a extensão `kotlin` era registrada duas vezes, causando crash imediato.

### Solução Aplicada
- **Removido** o plugin `kotlin-android` de todos os arquivos Gradle (TOML, root, app).
- **Mantido** apenas o plugin `kotlin-compose` (`org.jetbrains.kotlin.plugin.compose`), que **não** é embutido pelo AGP e precisa ser declarado separadamente.
- A versão do Kotlin foi restaurada para `2.2.10` (a que o AGP 9.2.1 embute internamente).

### Decisão: Migração de XML para Jetpack Compose
Durante a resolução, decidiu-se migrar a interface do app de **XML tradicional** (`AppCompatActivity` + `findViewById`) para **Jetpack Compose** (`ComponentActivity` + `@Composable`). Motivações:
- O projeto já havia sido gerado com dependências do Compose pelo Android Studio.
- Manter XML e Compose misturados gerava conflitos de dependência desnecessários.
- Compose é o padrão moderno do Android e facilita a evolução futura da interface.

O arquivo `activity_main.xml` foi excluído e toda a UI foi reescrita em Kotlin puro.

### Arquivos de Configuração Finais
| Arquivo | Plugins Declarados |
|---|---|
| `libs.versions.toml` | `android-application`, `kotlin-compose` |
| `build.gradle.kts` (raiz) | `android.application` (apply false), `kotlin.compose` (apply false) |
| `app/build.gradle.kts` | `android.application`, `kotlin.compose` |

> [!TIP]
> **Lição aprendida para projetos futuros com AGP 9.x:** Nunca declarar `org.jetbrains.kotlin.android` separadamente. O AGP cuida disso. Só o Compose Compiler precisa ser declarado à parte.

---

## 2. Teste Integrado — Fase 1 + Fase 2

Após a correção do Gradle, o APK foi compilado com sucesso e instalado no celular físico via:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat installDebug
```

### Resultado do Teste
- ✅ App abriu no celular e solicitou permissões BLE.
- ✅ Botão "Conectar ESP32" encontrou o dispositivo `RFID-POC-ESP32`.
- ✅ Conexão BLE estabelecida com sucesso.
- ✅ Comando `LED_ON` enviado → LED físico do ESP32 acendeu → resposta `OK_LED_ON` recebida no app.
- ✅ Comando `LED_OFF` enviado → LED apagou → resposta `OK_LED_OFF` recebida.
- ✅ Comunicação bidirecional 100% funcional.

---

## 3. Planejamento da Fase 3 — RFID UHF

### Decisão de Hardware
Foram avaliados dois módulos UHF compatíveis com ESP32:

| Módulo | Preço (R$) | Interface | Vantagens | Desvantagens |
|---|---|---|---|---|
| **JRD4035** | ~700 (com impostos) | UART | Industrial, anti-colisão avançada | Custo elevado para POC |
| **YRM100** | ~80 | USB / TTL (UART) | Antena integrada (1-5.5 dBi), barato, lê até 6m | Anti-colisão básica, pico de corrente alto |

### Decisão
O **YRM100** foi selecionado como melhor custo-benefício para a POC. Especificações confirmadas:
- **Protocolo:** EPCglobal UHF Class 1 Gen 2 / ISO 18000-6C
- **Frequência:** 865-868 MHz (EU) ou 902-928 MHz (US)
- **Alcance:** 0-6 metros (depende de antena/tags/ambiente)
- **Tensão:** 3.7V-5V (módulo)
- **Potência RF:** 15-26 dBm (ajustável)
- **Comunicação com ESP32:** Serial UART (TX/RX)

> [!WARNING]
> **Atenção na alimentação:** O YRM100 consome picos de 200-260mA. Não alimentar diretamente pelo pino 3.3V/5V do ESP32. Usar fonte externa 5V com GND compartilhado.

### Status
Aguardando liberação de verba para aquisição do módulo YRM100.

---

## 4. Simulador de Scanner Patrimonial

Como a compra do YRM100 está pendente, foi implementada uma **simulação completa** do fluxo de escaneamento patrimonial, usando o hardware já existente (ESP32 + LED + BLE + App Android).

### Lógica da Simulação

**Firmware (ESP32):**
- Ao receber `LED_ON` (botão "Escanear"):
  - Acende o LED (simulando antena UHF ativa).
  - Aguarda 800ms (simulando tempo de leitura do leitor).
  - Envia via BLE: `"Placa Patrimonial 147258 - Notebook Positivo encontrado e registrado no inventario da unidade 124 - Departamento de Planejamento e Gestao de Recursos."`
- Ao receber `LED_OFF` (botão "Desliga Scanner"):
  - Apaga o LED.
  - Envia `SCANNER_OFF`.

**Fragmentação BLE:**
Como a mensagem patrimonial tem ~140 caracteres e o BLE padrão transmite no máximo ~20 bytes por notificação, foi implementado:
- `sendBLELongMessage()` no firmware: quebra a mensagem em pacotes de 20 bytes e envia sequencialmente com intervalos de 50ms, finalizando com um marcador `__END__`.
- `handleReceivedMessage()` no app: acumula os fragmentos em um `StringBuilder` até receber `__END__`, e então exibe a mensagem completa.

**Interface do App (Jetpack Compose):**

| Botão Original | Botão Novo | Comando BLE |
|---|---|---|
| Conectar ESP32 | **Ligar Scanner** | Scan + Connect |
| LED ON | **Escanear** | `LED_ON` |
| LED OFF | **Desliga Scanner** | `LED_OFF` |

Um **Card verde de destaque** foi adicionado à tela para exibir o texto "Último Ativo Escaneado" com a mensagem patrimonial completa.

### Resultado do Teste
- ✅ Firmware compilado e carregado no ESP32 via Arduino IDE.
- ✅ APK compilado e instalado no celular.
- ✅ Fluxo completo testado: Ligar Scanner → Escanear → LED acende → Mensagem patrimonial aparece no Card verde do celular → Desliga Scanner → LED apaga.

---

## 5. Arquitetura Atual do Projeto

```
rfidpoc/
├── firmware/                     # Código C++ para ESP32 (Arduino IDE)
│   ├── firmware.ino              # Main: setup/loop, reconexão BLE
│   ├── ble_service.h             # Header do serviço BLE
│   ├── ble_service.cpp           # Servidor BLE, callbacks, fragmentação
│   ├── led_controller.h          # Header do controle do LED
│   └── led_controller.cpp        # GPIO 2 (LED onboard)
│
├── app/                          # Aplicativo Android (Kotlin + Compose)
│   └── src/main/java/.../
│       ├── MainActivity.kt       # UI Compose + lógica de reconstrução BLE
│       ├── ble/
│       │   ├── BleManager.kt     # Scan, connect, write, notify
│       │   └── BleConstants.kt   # UUIDs e nome do dispositivo
│       └── model/
│           └── BleMessage.kt     # Data class para mensagens
│
├── docs/                         # Documentação de progresso
│   ├── fase1.md
│   ├── fase2.md
│   ├── progresso arduino v1.md
│   ├── progresso aplicativo v1.md
│   └── progresso geral v2.md     # ← Este arquivo
│
└── gradle/                       # Configuração de build (AGP 9.2.1)
```

---

## 6. Próximos Passos

### Curto Prazo (Pré-hardware)
- [ ] Melhorar a interface do app com mais dados simulados (ex: múltiplos ativos, lista de inventário).
- [ ] Considerar adicionar um banco de dados local (Room) para armazenar histórico de leituras.

### Médio Prazo (Pós-aquisição do YRM100)
- [ ] Adquirir o módulo YRM100 (~R$ 80).
- [ ] Conectar YRM100 ao ESP32 via UART (TX/RX) com fonte externa 5V.
- [ ] Implementar os comandos HEX de inventário do YRM100 no firmware.
- [ ] Substituir a mensagem mockada pelo EPC real lido da tag UHF.
- [ ] Testar leitura de tags EPC Gen2 a diferentes distâncias.

### Longo Prazo (Produção)
- [ ] Avaliar migração para módulo industrial (JRD4035 ou superior) para anti-colisão em massa.
- [ ] Integrar com sistema de patrimônio (API REST para validar/cadastrar ativos).
- [ ] Adicionar antena UHF externa para alcance superior a 6m.

---

## Ambiente de Desenvolvimento

| Componente | Versão/Config |
|---|---|
| ESP32 | DevKit V1 (ESP32-D0WD-V3 rev3.1) |
| Arduino IDE | Última versão estável |
| Baud Rate Serial | 115200 |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.4.1 |
| Kotlin | 2.2.10 (embutido pelo AGP) |
| Compose BOM | 2026.02.01 |
| JAVA_HOME | `C:\Program Files\Android\Android Studio\jbr` |
| Build command | `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat installDebug` |
