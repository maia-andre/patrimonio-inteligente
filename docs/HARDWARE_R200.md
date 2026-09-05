# Montagem e teste do módulo UHF YPD-R200

Documento de bancada para o projeto **patrimônio-inteligente**.
Cobre desde o primeiro teste do módulo no PC até a ligação definitiva com o ESP32.

**Onde isso se encaixa no repositório.** O firmware em `firmware/` hoje só simula o leitor: o comando BLE `LED_ON` acende o LED e devolve um texto fixo, fragmentado em pacotes de 20 bytes e fechado por `__END__` (`ble_service.cpp`). O aplicativo Android já interpreta o payload `codigo;descricao` (RN-03 da `docs/spec.md`, parser em `domain/InterpretadorPayloadUhf.kt`). A Fase 2 deste documento é o que faz o firmware trocar a simulação pelo R200 e emitir esse formato.

---

## 0. Regras que não se negociam

| Regra | Porquê |
|---|---|
| **Antena conectada antes de energizar** | Transmitir sem carga na saída RF pode queimar o amplificador de potência. Vale para todo ciclo de liga/desliga. |
| **Aperto de dedo no SMA** | Alicate arranca o conector da placa. Firme com a mão basta. |
| **VCC/GND do R200 com fio soldado ou borne** | Jumper Dupont tem contato ruim e o módulo puxa picos acima de 500 mA na transmissão. Contato intermitente = reset no meio do inventário. |
| **5 V de fonte separada, nunca do 3V3 do ESP32** | O regulador do ESP32 não sustenta o pico de TX. |
| **GND sempre comum** | Sem terra em comum entre fonte, R200 e ESP32, a UART não funciona. |
| **O R200 não é programável** | Firmware de fábrica no Impinj E710. Só se conversa com ele por serial. O Arduino IDE é para o ESP32, nunca para o R200. |

---

## 1. Material

- Módulo YPD-R200 (Impinj E710), SMA fêmea, 15–26 dBm, 902–928 MHz
- Antena cerâmica com rabicho SMA macho
- Cabo micro-USB (acompanha o módulo)
- ESP32 DevKit
- Tags UHF Higgs3 adesivas (superfície não metálica)
- Tags UHF ABS anti-metal
- Protoboard, jumpers fêmea-fêmea
- Fonte 5 V externa (fase 2)

---

## 2. Fase 1 — teste isolado no PC

Objetivo: provar que o módulo está vivo **antes** de envolver o ESP32.
Se algo falhar depois, você já sabe que não é o módulo. Em Windows sem administrador, pule para a seção 2b: o caminho pelo micro-USB não abre.

### Passos

1. Remover o adesivo azul `REMOVE AFTER WASHING` da placa (é só marca de processo de fábrica).
2. Rosquear a antena no conector `CON` da placa. Aperto de dedo.
3. Puxar levemente o cabo para confirmar que está firme.
4. Plugar o micro-USB **em porta direta do notebook** — não em hub sem fonte. Notebook na tomada, porque em bateria algumas máquinas limitam corrente nas portas USB.
5. Rodar:

```bash
pip install pyserial
python teste_r200.py                 # lista as portas
python teste_r200.py COM5            # Windows
python teste_r200.py /dev/ttyUSB0    # Linux
```

O micro-USB da placa **não é só alimentação** — ele passa por um conversor USB-serial embutido. Por isso tem que ir no computador, não em carregador de parede: no carregador o módulo liga, mas não há como falar com ele.

### O que esperar

- Resposta em `RX:` ao comando de versão de firmware → **módulo vivo**
- `RX: (sem resposta)` em tudo → ver seção 6
- EPCs aparecendo no inventário contínuo → **cadeia RF completa funcionando**

---

### 2b. Windows sem driver do CH340 (sem administrador)

O micro-USB do R200 tem um **CH340** (VID `1A86`, PID `7523`), que exige o driver de fabricante da WCH. Em máquina corporativa sem administrador, e com o Windows Update apontado para servidor interno, o driver não vem sozinho: o aparelho fica como "USB Serial" com erro, código 28. Foi o caso na máquina do trabalho em 05/09/2026.

O ESP32 DevKit **não** tem esse problema: seu conversor é um **CH9102** (PID `55D4`), que fala USB CDC e usa o driver `usbser` embutido no Windows. Foi assim que o ESP32 sempre funcionou sem administrador.

Três saídas, em ordem de preferência:

1. **ESP32 como ponte serial.** Gravar `firmware/ponte_uart/ponte_uart.ino` no ESP32 e ligar o R200 nos pinos conforme a seção 3, com o micro-USB do R200 **desconectado** e a alimentação vindo de fonte 5 V. O `teste_r200.py` roda sem alteração contra a porta do ESP32. Vantagem: já valida a fiação da Fase 2.
2. **Celular Android com cabo OTG.** O aplicativo *Serial USB Terminal* (Kai Morich) fala com CH340 sem driver nem root; em modo hexadecimal dá para mandar os frames da seção 4 e ver a resposta. Serve para provar que o módulo está vivo.
3. **Notebook Linux.** O kernel reconhece o CH340 nativamente (`/dev/ttyUSB0`).

## 3. Fase 2 — ligação com o ESP32

Só depois que a Fase 1 passar.

### O que a placa tem (conferido em foto, 05/09/2026)

A placa vem **sem pinos soldados**. Dois grupos de furos importam:

- **`J3`**, na borda direita, 4 furos em coluna, de cima para baixo: `3V3`, `RXD`, `TXD`, `GND` (o furo quadrado é o `GND`). É a UART do módulo, em nível 3,3 V, o mesmo do ESP32: liga direto, sem conversor de nível. Os resistores `R14`/`R15` ao lado devem ser os de série que separam essa UART do CH340, mas isso é suposição, não foi medido.
- Fileira de 5 furos à esquerda do módulo blindado, ao lado da serigrafia `YPD-R200`, com um furo marcado **`5V`**. É a entrada de 5 V que alimenta o regulador da placa, o mesmo caminho do micro-USB.

O micro-USB, o CH340 e o `J3` compartilham a única UART do módulo. Portanto: USB do R200 desconectado enquanto o ESP32 estiver no `J3`.

Para pôr pinos: cortar 4 posições da barra de pinos e **soldar** em `J3`, mais 1 pino reto soldado no furo `5V`. Não há atalho sem solda: em 05/09/2026 foram tentados pino em L inclinado no furo, pino reto atravessando o furo com a placa apoiada na protoboard e pressão com o dedo. Alimentação (`5V`, `GND`) até funciona assim, porque a placa liga e não reinicia; os sinais `TXD`/`RXD` não, porque o contato fica resistivo e a UART recebe só bytes deformados (ver seção 6). Dez minutos de ferro resolvem o que uma tarde de improviso não resolveu.

### Pinagem

| R200 | ESP32 DevKit V1 (30 pinos) | Observação |
|---|---|---|
| `J3 TXD` | `RX2` (GPIO16) | dados do R200 para o ESP32 |
| `J3 RXD` | `TX2` (GPIO17) | dados do ESP32 para o R200 |
| `J3 GND` | `GND` | obrigatório, terra comum com a fonte |
| `5V` (fileira da esquerda) | `VIN` ou fonte externa 5 V | ver abaixo; **nunca** o `3V3` do ESP32 |

No ESP32 DevKit V1 os pinos `RX2` e `TX2` estão serigrafados na coluna da direita; `VIN` e `GND` ficam na ponta inferior da coluna da esquerda, junto do micro-USB.

UART2 a 115200 8N1.

### Alimentação do R200

Por ordem de preferência:

1. **Fonte externa 5 V no furo `5V`**: carregador de celular ou power bank com cabo USB cortado (vermelho = 5 V, preto = GND). É o que aguenta os picos de transmissão. O GND dessa fonte precisa estar no GND do ESP32, o que o fio `J3 GND` já garante.
2. **`VIN` do ESP32 no furo `5V`**: os 5 V da USB do PC passam pelo ESP32 e alimentam o R200. Uma porta USB 3 dá até 900 mA; a ponte serial não liga o rádio do ESP32, então a maior parte sobra para o R200. Em 18 dBm tende a funcionar. Se o módulo reiniciar no meio do inventário, é falta de corrente: voltar à opção 1.
3. **Micro-USB do R200 em carregador ou power bank, nunca em computador**: alimenta pelo conector, sem pino de força. Como o CH340 acorda junto e segura a linha `RXD` em repouso, só é aceitável se `R14`/`R15` forem mesmo resistores em série. Último recurso.

### O que o firmware da Fase 2 precisa entregar

- Novo módulo `firmware/uhf_r200.h/.cpp`: montagem de frame, parser (o mesmo do `teste_r200.py`) e controle de inventário sobre `Serial2` a 115200.
- `ble_service.cpp` sem bloqueio: o comando BLE só muda um estado; quem lê a UART e envia pelo BLE é o `loop()`. Hoje o `onWrite` faz `delay(800)` e envia dentro do callback, o que não serve para leitura contínua.
- Comandos `SCAN_START`/`SCAN_STOP`, mantendo `LED_ON`/`LED_OFF` como sinônimos até o aplicativo ser atualizado.
- Payload BLE no formato da RN-03: **`EPC;`** (código = EPC em hexadecimal maiúsculo, descrição vazia). O RSSI vai só para o log serial do ESP32; se um dia entrar no aplicativo, será campo próprio, não texto na descrição. Fragmentação em 20 bytes e `__END__` continuam, porque `EPC;` com 24 caracteres já passa do limite.
- Janela de silêncio por EPC no firmware: o R200 notifica a mesma tag dezenas de vezes por segundo e cada envio BLE fragmentado custa cerca de 150 ms. Sem isso o BLE afoga.
- LED aceso enquanto o inventário está ativo.

### Cuidado com a UART compartilhada

Verificar se o micro-USB da placa e o header de pinos usam a **mesma** UART do R200. Se usarem — que é o caso mais comum nessas placas:

- **Não** manter o USB plugado enquanto o ESP32 estiver ligado nos pinos. O conversor USB disputa a linha TX e corrompe os frames.
- Na montagem final: alimentar pelo pino `5V`, USB desconectado.

### Montagem na protoboard

- Sinais (`TXD`/`RXD`) podem ir de Dupont sem problema — são de corrente baixa.
- Alimentação (`5V`/`GND`) merece fio soldado ou borne parafuso. Se for usar protoboard mesmo assim, use trilhos de alimentação e cheque o contato antes de energizar.
- Manter o cabo da antena longe do ESP32 e dos fios da UART. Reduz ruído nas leituras.

---

## 4. Protocolo serial (MagicRF / Invelion)

Estrutura do frame:

```
BB | Type | Cmd | LenMSB | LenLSB | Params... | Checksum | 7E
```

`Checksum` = soma dos bytes de `Type` até o último parâmetro, byte baixo.

Velocidade: **115200 8N1 é a suposição de fábrica, ainda não confirmada** neste módulo. O `diag_r200.ino` varre 9600 a 230400 e mostra em qual delas volta um frame limpo `BB 01 03 ...`; anotar aqui quando confirmar.

### Comandos usados no teste

| Ação | Frame |
|---|---|
| Versão de hardware | `BB 00 03 00 01 00 04 7E` |
| Versão de firmware | `BB 00 03 00 01 01 05 7E` |
| Fabricante | `BB 00 03 00 01 02 06 7E` |
| Região 902–928 MHz | `BB 00 07 00 01 02 0A 7E` |
| Potência 18,00 dBm | `BB 00 B6 00 02 07 08 C7 7E` |
| Potência 20,00 dBm | `BB 00 B6 00 02 07 D0 8F 7E` |
| Inventário único | `BB 00 22 00 00 22 7E` |
| Inventário contínuo | `BB 00 27 00 03 22 27 10 83 7E` |
| Parar inventário | `BB 00 28 00 00 28 7E` |

### Notificação de tag (resposta a `0x22`)

```
BB 02 22 [LenMSB] [LenLSB] [RSSI] [PC:2] [EPC:n] [CRC:2] [Checksum] 7E
```

`RSSI` é byte com sinal (valor > 127 → subtrair 256).
O EPC começa após o PC de 2 bytes e termina 2 bytes antes do fim dos parâmetros (o CRC não faz parte do EPC).

Este é o parser do `teste_r200.py`, a portar para `firmware/uhf_r200.cpp` na Fase 2. Se funciona no Python, funciona no ESP32. Detalhe que importa nos dois: `0xBB` pode aparecer dentro do EPC ou do RSSI, então nada é descartado antes de validar tamanho, `0x7E` e checksum do frame inteiro; frame inválido descarta só um byte e procura o próximo `0xBB`.

---

## 5. Região e conformidade

O R200 não tem opção "Brasil". As regiões disponíveis:

| Valor | Região | Faixa |
|---|---|---|
| `0x01` | China 900 | 920–925 MHz |
| `0x02` | US | 902–928 MHz |
| `0x03` | EU | 865–868 MHz |
| `0x04` | China 800 | 840–845 MHz |

Usamos `0x02`. A ANATEL libera **902–907,5 MHz** e **915–928 MHz**; a região US cobre essas faixas mas também 907,5–915 MHz, que não é liberada aqui.

Para bancada com potência baixa é o caminho normal. **Para o sistema em produção isso vira item de conferência** — pergunta a levar para a Proxion junto com o número de homologação ANATEL e a confirmação de acesso ao SDK.

---

## 6. Quando não funcionar

| Sintoma | Causa provável |
|---|---|
| Porta não aparece na listagem | Driver do conversor USB. No Gerenciador de Dispositivos, "USB Serial" com código 28 confirma. O ESP32 usa outro chip (CH9102) e não serve de referência. Ver seção 2b. |
| Porta abre mas `RX: (sem resposta)` | Baud errado (deve ser 115200); ou cabo USB só de carga, sem linhas de dados. Testar outro cabo. |
| `[aviso] checksum inválido` | Ruído na linha, cabo longo demais, ou disputa de UART (USB + ESP32 ao mesmo tempo). Um aviso isolado no meio de leituras boas é normal: o parser se ressincroniza sozinho. |
| Responde a comandos mas não lê tag | Antena mal rosqueada; potência baixa demais; tag encostada em metal sem ser anti-metal; distância. Começar com a tag a 5–10 cm. |
| Módulo reinicia durante o inventário | Alimentação insuficiente — pico de TX. Fonte externa, contato firme no VCC/GND. |
| `diag_r200`: `RX2 em repouso` alterna entre `0` e `1` de uma linha para outra | Contato do `TXD` indo e vindo. Pino sem solda. |
| `diag_r200`: respostas só com bytes como `00 00 00 80 C0 E0 F0 FC FE`, em todas as velocidades | Não é velocidade errada: é linha que fica em baixo e sobe devagar, assinatura de contato resistivo. Pino sem solda. Velocidade errada dá lixo diferente, e em uma das velocidades o frame sai limpo. |
| Monitor serial cheio de caracteres estranhos | Velocidade do monitor diferente de 115200, ou a placa está com o `ponte_uart` (que repassa bytes binários crus) em vez do `diag_r200`. |

---

## 7. Ferramentas de bancada (em `firmware/`)

| Ferramenta | Roda em | Para quê |
|---|---|---|
| `teste_r200.py` | PC, Python 3 + pyserial | Versão, região, potência, inventário único e contínuo com lista de EPCs. Funciona tanto pelo micro-USB do R200 (precisa do driver CH340) quanto pela porta do ESP32 com a ponte gravada. |
| `ponte_uart/ponte_uart.ino` | ESP32 | Repassa bytes USB ↔ UART2 sem interpretar. Faz o ESP32 de conversor USB-serial para o R200 quando o Windows não tem driver do CH340. |
| `diag_r200/diag_r200.ino` | ESP32 | Sem PC no meio: manda a versão de hardware em cada velocidade de 9600 a 230400, em ciclo, e imprime no monitor serial o nível de repouso do `RX2` (pull-up interno desligado, então `1` é prova real) e os bytes que voltam. Para mexer nos fios com o monitor aberto e para descobrir a velocidade do módulo. |

Nenhuma das três é o firmware do projeto, que continua em `firmware/firmware.ino`. As duas do ESP32 compilam com o core esp32 3.3.11 para a placa `esp32:esp32:esp32doit-devkit-v1`.

## 8. Registro de bancada — 05/09/2026

Máquina do trabalho, Windows 11 sem administrador. Montagem: R200 deitado sobre a protoboard com pinos retos sem solda em `J3` (`a20` a `a23`) e no furo `5V`; ESP32 fora da protoboard, jumpers fêmea-fêmea; `5V` do R200 pelo `VIN` do ESP32; antena de painel no `CON1`.

O que ficou provado:

- O CH340 do R200 não ganha driver nesta máquina (seção 2b). O ESP32 (CH9102) funciona com o driver embutido do Windows.
- Ponte serial no ESP32 validada em loop (`RX2` ligado ao `TX2`): cada frame enviado voltou idêntico.
- O R200 liga alimentado pelo `VIN` do ESP32: LED verde, bipe de boot, sem reinício durante os testes de comando.
- Com o pull-up interno desligado, `RX2 em repouso = 1` sempre que o pino do `TXD` encostava: o módulo está ligado e segurando a linha, ou seja, **o módulo está vivo e a fiação da Fase 2 está correta**.
- O módulo reage ao comando quando o `RXD` encosta: chegaram respostas de 10 a 54 bytes, mas deformadas (`00 80 C0 E0 F0 FC FE`) em todas as velocidades, e `RX2 em repouso` alternando `0`/`1`. Diagnóstico: contato resistivo dos pinos sem solda nos furos.

O que não ficou provado: a velocidade da UART do módulo e a leitura de uma tag. Os dois dependem do contato firme.

## 9. Próximos passos

- [ ] **Soldar** 4 pinos em `J3` e 1 pino no furo `5V` (casa, ferro de solda)
- [ ] Gravar `diag_r200` e ler um ciclo: `RX2 em repouso = 1` fixo e o frame `BB 01 03 ...` limpo em uma velocidade. Anotar a velocidade na seção 4 e ajustar `BAUD` no `teste_r200.py` e no `ponte_uart.ino` se não for 115200
- [ ] Gravar `ponte_uart` e rodar `teste_r200.py COM3`: versão, região, potência, inventário com uma tag Higgs3 a 5–10 cm
- [ ] Fase 2 — firmware do projeto lendo o R200 via UART2, saída BLE `EPC;` (RN-03), RSSI no log serial
- [ ] Testar tags ABS anti-metal em superfície metálica real (datasheet não substitui teste físico)
- [ ] Comparar alcance Higgs3 adesiva vs. ABS anti-metal, em papel e em metal
- [ ] Levantar curva de potência × alcance (18 / 20 / 22 / 26 dBm) — pelo `teste_r200.py`, que dá o RSSI por leitura
- [ ] Confirmar com a Proxion: homologação ANATEL por escrito, faixa de frequência, acesso ao SDK Zebra
