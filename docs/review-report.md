# Review report — 18/08/2026 (INC-02)
Spec: docs/spec.md, versão 1 (10/08/2026) | Incremento: INC-02 — ScannerViewModel e fonte UHF sobre o BleManager | Build report: 18/08/2026 (INC-02)
## VEREDITO: APROVADO

## Verificação requisito a requisito
| Item | Status | Evidência / Falha |
|------|--------|-------------------|
| REQ-02 | Atendido | `ui/ScannerViewModel.kt` seleciona/controla a fonte e expõe `EstadoTelaScanner` via `StateFlow`; `MainActivity.kt` auditada linha a linha: só fiação (criação de objetos, permissões BLE, `setContent`) — grep por `__END__`/buffer/parsing/dedup sem ocorrências. 6 testes com fonte falsa. A seleção entre múltiplas fontes chega no INC-04, conforme nota de cobertura do plano. |
| REQ-08 | Atendido | `git diff HEAD -- .../ble/` vazio — `BleManager` reaproveitado sem reescrita; `LED_ON`/`LED_OFF` preservados (`FonteUhfBleTest.iniciar envia LED_ON e parar envia LED_OFF`); remontagem `__END__` em `scan/RemontadorDeFragmentos.kt` com 4 testes; verify dirigiu a cadeia real com fragmentos e `SCANNER_OFF`. |
| REQ-13 | Atendido | `model/BleMessage.kt` removido via `git rm` (status `D`); zero referências confirmadas por grep antes da remoção; diretório `model/` extinto. |
| RNF-08 | Atendido | Estrutura `ble/ domain/ scan/ ui/` + `MainActivity.kt`; UI em Compose (`ui/TelaScanner.kt`, migrada sem mudança visual ou funcional). |
| Regressão | Limpa | Suíte completa re-executada pelo auditor com `--rerun-tasks`: 27/27; testes do INC-01 verdes; `domain/` sem diff. |
| Escopo | Limpo | Conjunto de mudanças = exatamente os arquivos do incremento + docs do ciclo; tela replicada sem funcionalidades novas (card continua exibindo o payload bruto, como antes). |

## Qualidade dos testes (TDD)
- Vermelho comprovado antes do verde (referências não resolvidas). Durante o verde, 1 teste falhou por corrida de inscrição no `SharedFlow` e a correção foi **no teste** (`advanceUntilIdle()` antes do emit) — correta: o comportamento de produção (coletor do `init` inscrito no `viewModelScope` real) não tinha o defeito; o build-report registra isso honestamente.
- Inversão mental: sem limpar o buffer do remontador, `buffer e limpo apos completar` falha; trocar LED_ON/LED_OFF derruba o teste de comandos; parar de interpretar payload derruba `fragmentos remontados viram leitura interpretada`; inverter a ordem dos logs derruba `log mais recente fica no topo`.
- Sem CE da spec neste incremento (CE-10/11/13 pertencem ao INC-04); robustez extra coberta (`__END__` vazio, `SCANNER_OFF`, mensagens sucessivas).
- Itens da definição de concluído tocados por este incremento: "MainActivity.kt não contém parsing..." satisfeito (grep objetivo); "BleMessage.kt não existe mais" satisfeito.

## Segurança
- Payload BLE é tratado por funções totais (parser sem exceções); nenhum dado sai do aparelho (RNF-07); sem segredos; sem dados pessoais.
- **Baixa** (observação, não bloqueia): o buffer do `RemontadorDeFragmentos` cresce sem limite se `__END__` nunca chegar, e a lista de logs do estado também é ilimitada — ambos comportamentos herdados da MainActivity original, preservados por exigência do REQ-08. Registrar como candidato a endurecimento em incremento futuro (fora de escopo agora).
- Dependências novas (`lifecycle-viewmodel-ktx:2.10.0`, `kotlinx-coroutines-test:1.10.2`): sem CVE conhecida; nenhuma dependência de Google Play Services no grafo.

## Correções necessárias (para o /build)
Nenhuma.
