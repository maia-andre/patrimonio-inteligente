# Review report — 18/08/2026 (INC-04)
Spec: docs/spec.md, versão 1 (10/08/2026) | Incremento: INC-04 — Seletor de modos e ciclo de vida das fontes | Build report: 18/08/2026 (INC-04)
## VEREDITO: APROVADO

## Verificação requisito a requisito
| Item | Status | Evidência / Falha |
|------|--------|-------------------|
| REQ-03 | Atendido | `SeletorDeModos` renderiza os três modos (`OrigemLeitura.entries`); `modoSelecionado` é um único valor, destacado; troca muda a seleção (teste + verify). |
| REQ-10 | Atendido | `selecionarModo`: cancela coleta → `parar()` na anterior → assina e `iniciar()` na nova. Ordem provada por trilha de eventos com fontes falsas (`["uhf.parar", "barras.iniciar"]`) e no verify com a fonte UHF **real** (`LED_OFF` antes de `barras.iniciar`) — exatamente o item da definição de concluído. |
| REQ-11 | Atendido | `ModoDaTela` com `disponivel`/`motivo`; motivo "Scanner BLE não conectado" dinâmico com a conexão; UI desabilita o botão e mostra o motivo. Motivo interino "Ainda não disponível nesta versão" para modos sem fonte, previsto no plano (os motivos de câmera/NFC entram nos INC-05/06). |
| RNF-04 | Atendido | `uses-feature` nfc e camera com `required="false"` no manifesto-fonte (2 ocorrências) **e** no manifesto mesclado do APK (conferido no verify). |
| RN-05 | Atendido | Uma única `fonteAtiva`; iniciar implica parar a anterior (mesma prova do REQ-10); coleta única por vez (`coleta?.cancel()` antes de reassinar). |
| CE-10 | Atendido | Teste `leitura de fonte parada apos a troca e descartada` + verify (payload UHF completo pós-troca não entra: `contador=1`). Cancelamento da coleta garante o descarte mesmo se a fonte parada emitir. |
| CE-11 | Atendido | Teste + verify: desconexão → UHF `disponivel=false` com motivo certo, `leituras` intactas. |
| CE-13 | Atendido | `onStop`/`onStart` → `aoEntrarEmSegundoPlano`/`aoVoltarAoPrimeiroPlano`; retoma só se havia captura em andamento (2 testes, incluindo o caso negativo). |
| Regressão | Limpa | Suíte re-executada pelo auditor: 50/50; `ble/`, `domain/` e `scan/` sem diff; comportamentos dos INC-02/03 cobertos pelos testes antigos, todos verdes. |
| Escopo | Limpo | Mudanças = manifesto (RNF-04), ViewModel/Tela (seletor + ciclo de vida), MainActivity (fiação `onStart`/`onStop` e mapa de fontes), testes. |

## Qualidade dos testes (TDD)
- Vermelho comprovado (30 erros de compilação por API inexistente) antes do verde.
- Inversão mental: inverter a ordem parar/iniciar derruba o teste de trilha de eventos; não cancelar a coleta derruba CE-10; recomputar `modos` sem o motivo derruba os testes de disponibilidade; retomar sem `capturaEmAndamento` derruba o caso negativo de CE-13.
- Casos extremos com teste dedicado cada (CE-10, CE-11, CE-13 + negativo).
- Interpretação aceita pelo auditor: ao sair do modo UHF, `parar()` envia `LED_OFF` e **mantém** a conexão BLE — a conexão é recurso de sessão (a disponibilidade do REQ-11 e o CE-11 dependem dela); derrubá-la na troca impediria voltar ao modo UHF sem reconectar, contradizendo o seletor. Registrado no build-report.

## Segurança
- Nenhum achado novo. Sem dependências novas; motivos são strings estáticas; nenhum dado do payload em decisão de disponibilidade.
- `uses-feature required=false` não amplia superfície de permissão (nenhuma permissão de câmera/NFC pedida ainda — chegam nos INC-05/06 com seus fluxos).
- Permanece a observação **baixa** herdada (buffers ilimitados do INC-02).

## Correções necessárias (para o /build)
Nenhuma.
