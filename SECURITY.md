# Política de Segurança — Patrimônio Inteligente

## Escopo

Este repositório contém três frentes de código que juntas formam uma prova
de conceito de rastreamento patrimonial público com RFID UHF:

- **Aplicativo Android** (`app/`) — Kotlin/Compose. Confere bens por código
  de barras, NFC, digitação e RFID UHF; no modo UHF recebe por BLE o EPC que
  o ESP32 lê do R200. A lista vive só em memória (RN-07): não há cadastro
  local nem backend.
- **Firmware ESP32** (`firmware/`) — lê o módulo R200 pela UART (protocolo
  `AA … DD`, documentado em `docs/HARDWARE_R200.md`) e envia o EPC ao app
  por um serviço BLE. As ferramentas de bancada (`diag_r200`, `ponte_uart`,
  `teste_r200.py`) ficam à parte.
- **Documentação de reprodutibilidade** (`docs/`) — montagem, pinagem e
  fotos do hardware, e o `lastro/`, que registra interações externas.

O que uma falha aqui pode expor, quando o código sair da PoC para uma
implantação real numa prefeitura ou órgão:

- **Dados de patrimônio público** — identificação de bens, localização,
  responsáveis pela guarda, valor contábil, número de tombamento;
- **Credenciais de servidor(a)** que operam o inventário (uma vez que o
  app for integrado a um backend real);
- **Superfície BLE** — o serviço BLE do ESP32 (`firmware/ble_service.cpp`)
  não tem autenticação nesta PoC: qualquer dispositivo no alcance pode se
  conectar, receber os EPCs lidos e enviar os comandos de início e fim de
  leitura. O R200 em si não tem Bluetooth; ele fala só por UART com o ESP32.

## O que este repositório **é** e o que **não é** hoje

**É:** prova de conceito com hardware validado (R200 lendo tags a 18 dBm) e
app Android com clean architecture e testes unitários (`app/src/test/`). O
protocolo BLE está documentado e não tem autenticação nem cifra (ver
Superfície BLE acima).

**Não é (ainda):** produto de produção. Sem backend, sem autenticação de
servidor, sem TLS mútuo, sem gestão de chave em `keystore` Android
hardened, sem apagamento remoto de sessão. Um município que adote isto
hoje precisa cobrir essa camada por conta própria.

## Como reportar uma vulnerabilidade

Prefira o canal privado do GitHub:

1. Abra <https://github.com/maia-andre/patrimonio-inteligente/security/advisories/new>.
2. Descreva o vetor, o impacto e um passo a passo mínimo de reprodução.
3. Se envolver hardware, indique a placa (R200, ESP32) e a versão de
   firmware relevante.

Compromisso deste projeto (PoC mantida em tempo compartilhado):

- Confirmação de recebimento em até **14 dias corridos**.
- Diagnóstico inicial em até **60 dias**.
- Correções críticas (dados recebidos por BLE ou UART que travem o
  firmware ou corrompam a conferência no app) recebem prioridade sobre o
  roadmap.

## O que **não** é vulnerabilidade

- **Leitura de tags UHF por qualquer leitor no raio de alcance** — RFID
  UHF passivo é broadcast por natureza física; a resposta é reduzir o
  tamanho do EPC gravado e não colocar dado sensível em tag clara.
- **Conteúdo de `docs/lastro/`** — o lastro é público por intenção e segue
  a regra de privacidade de `docs/lastro/README.md`. Dado pessoal publicado
  ali sem autorização é falha de conteúdo, não de código; reporte pelo
  mesmo canal privado acima.

## Divulgação coordenada

Vulnerabilidades corrigidas são divulgadas via GitHub Security Advisory
após o release contendo a correção. Peço um período razoável (**até 90
dias** a partir do reporte) antes de publicação ampla, para que
municípios que adotarem o código consigam atualizar.
