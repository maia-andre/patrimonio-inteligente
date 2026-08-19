# Build report — 19/08/2026 (INC-07)

Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-07 — Documentação: README e CONTRIBUTING
Rodada: construção (só documentação — nenhum código ou teste tocado)
Testes: 86 passando / 86 total — `sh gradlew testDebugUnitTest` (inalterados desde o INC-06)

## Requisitos atendidos

- **REQ-14** — Atendido — todos os nove pontos, conferíveis linha a linha no `README.md`:
  1. Tabela dos três modos com faixa/norma, hardware, alcance e itens por leitura + advertência "NFC não é RFID UHF e não o substitui" — nova seção "Os três modos de captura" (linha ~127).
  2. Duas linhas novas em "Estado real do projeto": "Leitura por código de barras (câmera)" e "Leitura por NFC", ambas ✅ Funciona (linhas 153-154).
  3. Callout "O gargalo, dito com todas as letras": agora "O **modo RFID UHF** — e apenas ele — está travado..." (linha 169), com a ressalva de que código de barras e NFC funcionam hoje.
  4. Fecho: "Modernizar o patrimônio começa com um celular Android que o seu setor já tem — e escala com R$ 1.200 e uma decisão" (linha 414) — não afirma mais que começa com R$ 1.200.
  5. "Reproduza no seu município" ganhou "Comece sem hardware nenhum" (passo a passo com os dois modos novos); a lista de materiais foi retitulada "(modo RFID UHF)".
  6. Diagrama "Como funciona" com as três origens: código de barras → câmera/ZXing, etiqueta NFC → antena/reader mode, UHF → BLE, convergindo no ScannerViewModel (porta única).
  7. Callout de débito técnico do "Protocolo BLE" registra que o aplicativo já aceita o payload `codigo;descricao` (RN-03), restando o firmware emiti-lo.
  8. Roadmap de curto prazo: três itens entregues marcados `[x]` (modos sem hardware; domínio+ViewModel+lista; payload aceito no app) e pendências mantidas `[ ]` (simulação multi-ativos, Room, renomear comandos/emitir payload no firmware, tela de auditoria).
  9. Sumário incorpora a seção nova "Os três modos de captura".
- **REQ-15** — Atendido — `CONTRIBUTING.md` seção "Acesso a um leitor RFID UHF": o bloqueio é delimitado ao **modo RFID UHF** ("o único capaz de inventário em massa"), com o pedido de acesso a leitor preservado como válido e prioritário.
- **RNF-06** — Atendido — tabela de ambiente: "Android mínimo | 9 (API 28)", em acordo com `minSdk = 28` do `build.gradle.kts`; as duas menções a "Android 12+" como requisito de execução corrigidas para "Android 9+ (API 28)" (Estado real e lista de materiais); a menção no contexto das permissões Bluetooth **permanece** (linha 275), por estar correta.

## Casos extremos cobertos

Nenhum CE no escopo deste incremento (só documentação).

## Verificação da definição de concluído (itens de docs)

- `grep 'R$ 1.200'` no README e CONTRIBUTING: 4 ocorrências, nenhuma afirmando que **o projeto** está bloqueado — duas delimitam ao modo UHF, uma é o custo da bancada, uma é o fecho reformulado.
- `grep 'Android 12+'`: resta só a menção de permissões Bluetooth, que a RNF-06 manda preservar.
- Advertência NFC ≠ UHF, linhas novas do Estado real, sumário, diagrama e callout do protocolo: conferidos por grep (saídas registradas na rodada).

## Ajustes de coerência (dentro do escopo "refletir o que os incrementos entregaram")

- CONTRIBUTING, padrões Android: estrutura de diretórios atualizada de `ble/, model/, ui/` (defasada desde a remoção de `model/` no INC-02) para `domain/, scan/, ble/, ui/` (RNF-08), com a regra das funções puras de interpretação (RNF-02).
- "Como funciona" ganhou um parágrafo introdutório citando as duas novas origens, exigido pela mudança do diagrama.

## Perguntas em aberto / pendências

Nenhuma.
