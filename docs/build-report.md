# Build report — 19/08/2026 (INC-06)

Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-06 — Modo NFC (reader mode)
Rodada: construção
Testes: 86 passando / 86 total — `sh gradlew testDebugUnitTest` (e `assembleDebug` compilando sem erro)

## Requisitos atendidos

- **REQ-07** — Atendido — `scan/FonteNfc.kt` habilita `NfcAdapter.enableReaderMode` em primeiro plano com as flags NfcA | NfcB | NfcF | NfcV no `iniciar()` e `disableReaderMode` no `parar()`. A etiqueta detectada é reduzida a bytes (TNF, tipo, payload, UID) e interpretada pela função pura. A detecção física de etiqueta fica para o `/verify` em aparelho, como a spec prevê.

## Regras de negócio

- **RN-02** — Atendido — `domain/InterpretadorEtiquetaNfc.kt`: precedência (1) primeiro registro NDEF de texto não vazio (TNF well-known + RTD "T", UTF-8 ou UTF-16, com idioma de tamanho variável) vira `codigo`; (2) senão, UID em hexadecimal maiúsculo sem separadores (`uidEmHexadecimal`, com zero à esquerda preservado). Coberta pelos dois itens da definição de concluído: texto `"147258"` → `codigo = "147258"`; UID `04 A2 24 B2 5C 61 80` → `codigo = "04A224B25C6180"` (`InterpretadorEtiquetaNfcTest`, 10 testes).

## Casos extremos cobertos

- **CE-02** — Atendido — motivo "Aparelho sem NFC" no seletor (`ScannerViewModel.montarModos`); estado inicial conservador `SEM_HARDWARE` até a Activity reportar; teste prova que os demais modos seguem operando (leitura UHF entra na lista com o NFC sem hardware).
- **CE-04** — Atendido — motivo "NFC desligado", distinto do CE-02 (teste compara os dois textos); o estado é reavaliado a cada `onStart`, então ligar o NFC nas configurações e voltar habilita o modo. NFC desligado durante o uso torna o modo indisponível preservando a lista (teste).
- **CE-05** — Atendido — etiqueta sem NDEF (lista de registros vazia) cai para o UID (teste + `registrosDe` devolvendo lista vazia quando `Ndef.get` é nulo).
- **CE-06** — Atendido — NDEF presente sem registro de texto (URI), registro de texto vazio, payload malformado (vazio, ou idioma declarado maior que o payload) e tipo "T" fora do TNF well-known: todos caem para o UID sem exceção (4 testes).

## Transversais (ancorados no INC-01, verificados nesta rodada)

- **RNF-01** — código, comentários e testes novos em português.
- **RNF-02** — `interpretarEtiquetaNfc`, `extrairTextoNdef` e `uidEmHexadecimal` são puros: recebem `RegistroNdef` (bytes) e UID, sem nenhum tipo `android.*`; a `FonteNfc` é só o adaptador que reduz `Tag`/`Ndef` a bytes.
- **RNF-03** — dados fictícios; o UID de exemplo é o da própria definição de concluído da spec.
- **RNF-04** — manifesto mantém `nfc` com `required="false"`; rodada acrescentou só `uses-permission android.permission.NFC`.
- **RNF-07** — nenhuma chamada de rede.
- **RNF-08** — arquivos novos na estrutura por responsabilidade: `domain/InterpretadorEtiquetaNfc.kt`, `scan/FonteNfc.kt`.

## Notas da rodada

- `EstadoNfc` (`SEM_HARDWARE`/`DESLIGADO`/`DISPONIVEL`) é reportado pela `MainActivity` no `onStart`, seguindo o mesmo padrão da permissão de câmera do INC-05.
- Correção durante o TDD: os campos de estado consultados por `montarModos` foram movidos para antes da declaração do `_estado` — a ordem de inicialização de propriedades fazia o primeiro `montarModos` rodar com `estadoNfc` ainda nulo (pego por teste vermelho, corrigido, suíte verde).
- Gravação de etiquetas segue fora de escopo: a fonte apenas lê (`cachedNdefMessage`), sem `connect()` nem escrita.

## Perguntas em aberto / pendências

Nenhuma. Itens que exigem aparelho (detecção física de etiqueta nos dois caminhos da RN-02, aparelho sem NFC, NFC desligado de verdade) ficam para o `/verify`, conforme "Verificação que exige aparelho" da spec.
