# Política de Segurança — Patrimônio Inteligente

## Escopo

Este repositório contém três frentes de código que juntas formam uma prova
de conceito de rastreamento patrimonial público com RFID UHF:

- **Aplicativo Android** (`app/`) — Kotlin/Compose, faz leitura de tags UHF
  via BLE, códigos de barras e NFC, e forma um cadastro local do inventário.
- **Firmware Arduino/ESP32** (`firmware/`) — ponte UART BLE ⇄ módulo R200
  (leitor UHF), com protocolo `AA … DD` já documentado em
  `docs/HARDWARE_R200.md`.
- **Documentação de reprodutibilidade** (`docs/`) — inclui plantas de solda,
  fotos do hardware e o `lastro/`, que pode conter material de campo.

O que uma falha aqui pode expor, quando o código sair da PoC para uma
implantação real numa prefeitura ou órgão:

- **Dados de patrimônio público** — identificação de bens, localização,
  responsáveis pela guarda, valor contábil, número de tombamento;
- **Credenciais de servidor(a)** que operam o inventário (uma vez que o
  app for integrado a um backend real);
- **Superfície BLE** — o leitor UHF conecta por BLE sem autenticação por
  padrão; qualquer dispositivo com o protocolo publicado pode enviar
  comandos ao módulo R200 na área.

## O que este repositório **é** e o que **não é** hoje

**É:** prova de conceito com hardware validado (R200 lendo tags a 18 dBm) e
app Android com clean architecture e testes unitários (`app/src/test/`). O
protocolo BLE está publicado; qualquer segurança sobre ele é do transporte,
não do payload.

**Não é (ainda):** produto de produção. Sem backend, sem autenticação de
servidor, sem TLS mútuo, sem gestão de chave em `keystore` Android
hardened, sem apagamento remoto de sessão. Um município que adote isto
hoje precisa cobrir essa camada por conta própria.

## Como reportar uma vulnerabilidade

Prefira o canal privado do GitHub:

1. Abra <https://github.com/maia-andre/patrimonio-inteligente/security/advisories/new>.
2. Descreva o vetor, o impacto e um passo a passo mínimo de reprodução.
3. Se envolver hardware, indique o revisor (R200, ESP32) e a versão de
   firmware relevante.

Compromisso deste projeto (PoC mantida em tempo compartilhado):

- Confirmação de recebimento em até **14 dias corridos**.
- Diagnóstico inicial em até **60 dias**.
- Correções críticas (leitura arbitrária de tags, injeção via app,
  comandos BLE não previstos que possam derrubar o módulo) recebem
  prioridade sobre o roadmap.

## O que **não** é vulnerabilidade

- **Ausência de autenticação no protocolo BLE do R200** — é característica
  do módulo, não do código deste repositório. Mitigação prevista no
  roadmap é encapsular o BLE em sessão autenticada pela ponte ESP32.
- **Leitura de tags UHF por qualquer leitor no raio de alcance** — RFID
  UHF passivo é broadcast por natureza física; a resposta é reduzir o
  tamanho do EPC gravado e não colocar dado sensível em tag clara.
- **Uso interno de dados de teste** em `docs/lastro/` — o lastro é
  intencional e público; se algo ali for sensível, reporte como falha
  de conteúdo, não de código.

## Divulgação coordenada

Vulnerabilidades corrigidas são divulgadas via GitHub Security Advisory
após o release contendo a correção. Peço um período razoável (**até 90
dias** a partir do reporte) antes de publicação ampla, para que
municípios que adotarem o código consigam atualizar.
