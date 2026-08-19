# Plan: Modos de captura — código de barras, NFC e RFID UHF

Spec: docs/spec.md, versão 1 (10/08/2026, aprovada) | Gerado: 18/08/2026
Progresso: 5/7 incrementos concluídos

## Backlog

### INC-01 — Camada de domínio e parser UHF
Status: concluído
Itens da spec: REQ-01, REQ-09 | RNF-01, RNF-02, RNF-03, RNF-07 | RN-03 | CE-07, CE-08, CE-09
Depende de: —
Entrega verificável: `./gradlew test` roda e passa com os testes do parser UHF (payload com `;`, sem `;`, descrição vazia, múltiplos `;`) e `domain/` compila sem nenhum import de `android.*`.

Escopo: criar `domain/` com `LeituraPatrimonial`, `OrigemLeitura` e `FonteDeLeitura` (REQ-01) e o parser puro do payload UHF conforme RN-03, cobrindo CE-07/CE-08/CE-09 por teste unitário. Estabelece o padrão da RNF-02 (interpretação de payload em funções puras) que os incrementos seguintes seguem. As RNF-01 (português), RNF-03 (só dado fictício) e RNF-07 (processamento só no aparelho) ficam ancoradas aqui e valem para todos os incrementos.

### INC-02 — ScannerViewModel e fonte UHF sobre o BleManager
Status: concluído
Itens da spec: REQ-02, REQ-08, REQ-13 | RNF-08
Depende de: INC-01
Entrega verificável: o app sobe, conecta ao ESP32 (simulação do firmware) e exibe a última leitura UHF vinda do `ScannerViewModel` — com a `MainActivity` sem parsing, buffer de fragmentos, deduplicação ou controle de fonte, e `model/BleMessage.kt` removido do repositório.

Escopo: mover a lógica de protocolo da `MainActivity` (remontagem por `__END__`, interpretação do payload) para uma `FonteDeLeitura` UHF que embrulha o `BleManager` existente sem reescrevê-lo, preservando `LED_ON`/`LED_OFF` (REQ-08). Criar o `ScannerViewModel` (REQ-02) e organizar os diretórios por responsabilidade — `domain/`, `scan/`, `ble/`, `ui/` (RNF-08). Remover `model/BleMessage.kt` (REQ-13).

### INC-03 — Lista de leituras com deduplicação e contador
Status: concluído
Itens da spec: REQ-04, REQ-05 | RN-01, RN-06, RN-07 | CE-01, CE-14
Depende de: INC-02
Entrega verificável: leituras UHF acumulam em lista (mais recente no topo) com contador de itens da sessão, cada linha com código, origem e horário; leitura repetida não cria linha e sinaliza "já conferido" (limitada a 1 sinalização/segundo por código); sem leituras, a tela mostra estado vazio explicativo.

Escopo: acúmulo em memória no `ScannerViewModel` com chave de deduplicação `codigo ?: bruto` (RN-01), ordem cronológica inversa (RN-06), lista volátil (RN-07), sinalização de duplicata com limitação de frequência (CE-01) e estado vazio (CE-14). Deduplicação e limitação implementadas como lógica pura, testável sem aparelho.

### INC-04 — Seletor de modos e ciclo de vida das fontes
Status: concluído
Itens da spec: REQ-03, REQ-10, REQ-11 | RNF-04 | RN-05 | CE-10, CE-11, CE-13
Depende de: INC-03
Entrega verificável: a tela exibe o seletor com os três modos (exatamente um ativo); trocar de modo para a fonte anterior antes de iniciar a nova, e leitura de fonte parada é descartada (verificável com fontes falsas em teste); modo indisponível aparece desabilitado com o motivo; queda do BLE torna o modo UHF indisponível preservando a lista; ir a segundo plano para a fonte ativa e voltar a retoma.

Escopo: seletor dos três modos (REQ-03), orquestração parar-antes-de-iniciar no ViewModel (REQ-10, RN-05, CE-10, CE-13) e a mecânica de disponibilidade com motivo legível (REQ-11), incluindo o caso UHF "scanner BLE não conectado" e a queda de conexão em uso (CE-11). Manifesto declara NFC e câmera com `android:required="false"` (RNF-04). Os modos código de barras e NFC aparecem no seletor, mas seguem indisponíveis até seus incrementos — os motivos específicos de câmera e NFC completam-se em INC-05 e INC-06.

### INC-05 — Modo código de barras (CameraX + ZXing)
Status: concluído
Itens da spec: REQ-06, REQ-12 | RNF-05 | RN-04 | CE-03, CE-12
Depende de: INC-04
Entrega verificável: apontar a câmera para um Code 128, Code 39 ou QR fictício adiciona o item à lista; um EAN-13 de varejo é ignorado; a permissão de câmera é pedida só ao selecionar o modo, e negá-la deixa o modo desabilitado com motivo e caminho para pedir de novo, sem travar o app.

Escopo: `FonteDeLeitura` de código de barras com CameraX e `com.google.zxing:core` (DT-01, DT-03, RNF-05 — sem Google Play Services), aceitando só Code 128, Code 39 e QR Code (REQ-06, RN-04). Filtro de simbologia em função pura (RNF-02). Permissão em tempo de execução na seleção do modo (REQ-12), negação tratada (CE-03), quadro ilegível silencioso (CE-12).

### INC-06 — Modo NFC (reader mode)
Status: pendente
Itens da spec: REQ-07 | RN-02 | CE-02, CE-04, CE-05, CE-06
Depende de: INC-04
Entrega verificável: encostar uma etiqueta NFC adiciona o item à lista com o código vindo do registro NDEF de texto ou, na falta dele, do UID em hexadecimal maiúsculo; aparelho sem NFC e aparelho com NFC desligado mostram o modo desabilitado com motivos distintos.

Escopo: `FonteDeLeitura` NFC com `NfcAdapter.enableReaderMode` para NfcA/NfcB/NfcF/NfcV (REQ-07). Extração do código como função pura conforme a precedência da RN-02, cobrindo etiqueta sem NDEF (CE-05) e NDEF sem registro de texto ou com texto vazio (CE-06) por teste unitário. Motivos de indisponibilidade "aparelho sem NFC" (CE-02) e "NFC desligado" (CE-04) distintos entre si.

### INC-07 — Documentação: README e CONTRIBUTING
Status: pendente
Itens da spec: REQ-14, REQ-15 | RNF-06
Depende de: INC-05, INC-06
Entrega verificável: cada ponto do REQ-14 é conferível linha a linha no README (tabela dos três modos com a advertência NFC ≠ UHF, linhas novas no "Estado real do projeto", gargalo delimitado ao modo UHF, fecho sem o pré-requisito de R$ 1.200, caminho de reprodução sem hardware, diagrama com três origens, débito técnico do protocolo BLE, roadmap e sumário atualizados); o CONTRIBUTING delimita o bloqueio ao modo UHF (REQ-15); a tabela de ambiente declara minSdk 28 em acordo com o `build.gradle.kts` (RNF-06).

Escopo: só documentação. Deve refletir exatamente o que os incrementos anteriores entregaram — por isso vem por último.

## Cobertura

Todo item da spec está em exatamente um incremento:

- REQ: 01, 09 → INC-01 · 02, 08, 13 → INC-02 · 04, 05 → INC-03 · 03, 10, 11 → INC-04 · 06, 12 → INC-05 · 07 → INC-06 · 14, 15 → INC-07. (15/15)
- RNF: 01, 02, 03, 07 → INC-01 · 08 → INC-02 · 04 → INC-04 · 05 → INC-05 · 06 → INC-07. (8/8)
- RN: 03 → INC-01 · 01, 06, 07 → INC-03 · 05 → INC-04 · 04 → INC-05 · 02 → INC-06. (7/7)
- CE: 07, 08, 09 → INC-01 · 01, 14 → INC-03 · 10, 11, 13 → INC-04 · 03, 12 → INC-05 · 02, 04, 05, 06 → INC-06. (14/14)

Observações de cobertura:

- RNF-01, RNF-03 e RNF-07 são transversais: ficam ancoradas em INC-01, mas valem como critério de aceite de todos os incrementos.
- RNF-02 idem: o padrão nasce em INC-01 e cada modo novo (INC-05, INC-06) implementa seu extrator como função pura.
- REQ-02 fala em "acumular leituras"; o acúmulo em si é o REQ-04 e entra em INC-03 — em INC-02 o ViewModel nasce controlando a fonte e expondo o estado.
- Itens listados em "Verificação que exige aparelho" na spec (câmera real, etiqueta NFC real, UHF contra o ESP32) ficam para o `/verify` dos incrementos correspondentes.
