# Verify report — 18/08/2026 (INC-03)
Incremento: INC-03 — Lista de leituras com deduplicação e contador | Build report: 18/08/2026 (INC-03)
Como rodei: `sh gradlew assembleDebug` (empacotamento real com a tela nova) + driver Kotlin descartável na JVM 21 dirigindo a cadeia real `FonteUhfBle → ScannerViewModel` com relógio controlado injetado na fonte, simulando payloads fragmentados do ESP32. Driver apagado ao final. Sem aparelho físico/emulador nesta máquina.
Suíte de testes: 42/42 passando — citado do build-report, sem re-execução.

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| Empacotamento | APK debug com a lista nova | `sh gradlew assembleDebug` → `BUILD SUCCESSFUL` | FUNCIONA |
| CE-14 | Estado inicial sem leituras | `contador=0 aviso=null` (a tela renderiza o card "Nenhum item conferido ainda..." quando `leituras.isEmpty()`) | FUNCIONA |
| REQ-04 / RN-06 | Dois payloads distintos acumulam, mais recente no topo, contador correto, linha com chave/origem/horário | `contador=2` e linhas `369852 \| RFID UHF \| 03:53:23` acima de `147258 \| RFID UHF \| 03:53:20` | FUNCIONA |
| REQ-05 / RN-01 | Mesma chave reenviada não gera linha e sinaliza | após reenvio: `contador=2 aviso=Item 147258 já conferido` | FUNCIONA |
| CE-01 | 4 duplicatas em rajada (t0, +200 ms, +500 ms, +1,6 s) → só 2 sinalizações; contador imóvel | `sinalizacoes registradas=2 ... contador=2` | FUNCIONA |
| REQ-05 (aviso) | Leitura nova de chave distinta limpa o aviso | `contador=3 aviso=null` com `555777` no topo | FUNCIONA |
| RN-07 | Lista só em memória | nenhuma escrita em disco/banco no código novo (grep sem `Room`/`SharedPreferences`/`File`); estado vive em `MutableStateFlow` | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- Renderização Compose real (card de aviso, estado vazio, lista) em aparelho: sem aparelho/emulador. A lógica que alimenta a tela foi dirigida de verdade; o composable é declarativo sobre o mesmo estado.
- Deduplicação entre origens diferentes dirigida com fontes reais distintas: só a fonte UHF existe até aqui — o caso está coberto por teste unitário (`origens diferentes com a mesma chave sao o mesmo bem`) e será dirigível de verdade após INC-05/06.
