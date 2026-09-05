# Montagem e teste do módulo UHF YPD-R200

Documento de bancada para o projeto **patrimônio-inteligente**.
Cobre desde o primeiro teste do módulo no PC até a ligação definitiva com o ESP32.

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
Se algo falhar depois, você já sabe que não é o módulo.

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

## 3. Fase 2 — ligação com o ESP32

Só depois que a Fase 1 passar.

### Pinagem

| R200 | ESP32 | Observação |
|---|---|---|
| `TXD` | `GPIO16` (RX2) | dados do R200 para o ESP32 |
| `RXD` | `GPIO17` (TX2) | dados do ESP32 para o R200 |
| `GND` | `GND` | obrigatório, terra comum com a fonte |
| `5V` | — | fonte externa 5 V, **não** o ESP32 |

UART2 a 115200 8N1.

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

Esse é o mesmo parser que está em `uhf_r200.cpp`. Se funciona no Python, funciona no ESP32.

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
| Porta não aparece na listagem | Driver do conversor USB (CH340 ou CP2102). É o mesmo do ESP32 — checar no Gerenciador de Dispositivos. |
| Porta abre mas `RX: (sem resposta)` | Baud errado (deve ser 115200); ou cabo USB só de carga, sem linhas de dados. Testar outro cabo. |
| `[aviso] checksum inválido` | Ruído na linha, cabo longo demais, ou disputa de UART (USB + ESP32 ao mesmo tempo). |
| Responde a comandos mas não lê tag | Antena mal rosqueada; potência baixa demais; tag encostada em metal sem ser anti-metal; distância. Começar com a tag a 5–10 cm. |
| Módulo reinicia durante o inventário | Alimentação insuficiente — pico de TX. Fonte externa, contato firme no VCC/GND. |

---

## 7. Próximos passos

- [ ] Fase 1 concluída — módulo responde e lê tag
- [ ] Fase 2 — ESP32 lendo via UART2, saída BLE em `epcHex;rssi=X`
- [ ] Testar tags ABS anti-metal em superfície metálica real (datasheet não substitui teste físico)
- [ ] Comparar alcance Higgs3 adesiva vs. ABS anti-metal, em papel e em metal
- [ ] Levantar curva de potência × alcance (18 / 20 / 22 / 26 dBm)
- [ ] Confirmar com a Proxion: homologação ANATEL por escrito, faixa de frequência, acesso ao SDK Zebra
