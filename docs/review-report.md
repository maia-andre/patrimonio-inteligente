# Review report — 18/08/2026 (INC-03)
Spec: docs/spec.md, versão 1 (10/08/2026) | Incremento: INC-03 — Lista de leituras com deduplicação e contador | Build report: 18/08/2026 (INC-03)
## VEREDITO: APROVADO

## Verificação requisito a requisito
| Item | Status | Evidência / Falha |
|------|--------|-------------------|
| REQ-04 | Atendido | `EstadoTelaScanner.leituras` + `TelaScanner` com "Conferidos na sessão: N" e `LinhaDeLeitura` exibindo chave (código; bruto quando código nulo, coerente com RN-01), `rotuloDaOrigem` e horário HH:mm:ss. Dirigido no verify com saída literal. |
| REQ-05 | Atendido | `AcumuladorDeLeituras.acumular` (puro) não insere chave repetida; ViewModel sinaliza com card de aviso + log. Testes de acumulador (6) e ViewModel; verify mostrou `contador=2 aviso=Item 147258 já conferido`. |
| RN-01 | Atendido | `LeituraPatrimonial.chave = codigo ?: bruto`; testes cobrem código presente, código nulo (dedup pelo bruto) e origens diferentes com mesma chave (mesmo bem). |
| RN-06 | Atendido | Inserção no topo (`listOf(nova) + listaAtual`); ordem verificada em teste e no verify (369852 acima de 147258). |
| RN-07 | Atendido | Estado só em `MutableStateFlow`; grep do auditor sem `Room`/`SharedPreferences`/`File` em `domain/` e `ui/`. |
| CE-01 | Atendido | Sem linha nova, contador imóvel, sinalização limitada a 1/s por chave (`LimitadorDeSinalizacao`, decide pelo `instante` — determinístico). Limites 999 ms (bloqueia) e 1 000 ms (permite) testados; verify: 4 duplicatas em rajada → 2 sinalizações. |
| CE-14 | Atendido | `leituras.isEmpty()` → card "Nenhum item conferido ainda..." em vez de lista em branco; estado inicial vazio testado. |
| Regressão | Limpa | Suíte re-executada pelo auditor com `--rerun-tasks`: 42/42; `MainActivity`, `ble/` e `scan/` sem diff nesta rodada. |
| Escopo | Limpo | Mudanças = exatamente os arquivos do incremento; a substituição do card "Último Ativo" pela lista é a materialização do REQ-04 (o card era o display pré-lista das leituras), registrada no build-report. |

## Qualidade dos testes (TDD)
- Vermelho comprovado antes do verde (referências não resolvidas a `chave`/`AcumuladorDeLeituras`).
- Inversão mental: trocar prepend por append derruba os testes de ordem; remover o `ifEmpty`-equivalente da dedup (comparar por objeto em vez de chave) derruba o teste de origens diferentes; alterar o intervalo do limitador derruba os testes de 999/1000 ms; não limpar o aviso derruba `leitura nova limpa o aviso`.
- Valores-limite da RN/CE cobertos (999 ms, 1 000 ms, chaves independentes); duplicata cross-origem coberta.
- Itens da definição de concluído deste incremento: "duas leituras com a mesma chave → uma linha, contador 1" e "duas leituras de origens diferentes com códigos distintos → duas linhas com suas origens" — ambos com teste dedicado.

## Segurança
- Nenhum achado novo. Conteúdo do payload BLE aparece na UI via Compose `Text` (sem interpretação de markup — sem XSS); processamento 100% local (RNF-07); dados fictícios (RNF-03/LGPD).
- Permanece a observação **baixa** do INC-02 (buffer de fragmentos e logs sem limite — comportamento herdado); a lista de leituras é limitada pelo conjunto de chaves únicas do acervo, sem crescimento por repetição.
- Nenhuma dependência nova nesta rodada.

## Correções necessárias (para o /build)
Nenhuma.
