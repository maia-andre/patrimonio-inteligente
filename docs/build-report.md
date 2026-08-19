# Build report — 18/08/2026 (INC-02)
Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-02 — ScannerViewModel e fonte UHF sobre o BleManager
Rodada: construção
Testes: 27 passando / 27 total — `sh gradlew test` (unit; instrumentada exige aparelho)

## Requisitos atendidos
- **REQ-02** — Atendido — `ui/ScannerViewModel.kt` controla a fonte (iniciar/parar/conectar/desconectar) e expõe `EstadoTelaScanner` por `StateFlow`; a `MainActivity.kt` ficou só com fiação (criação de objetos, permissões BLE e `setContent`) — sem parsing, buffer, deduplicação ou controle de fonte. Coberto por `ScannerViewModelTest` (6 testes com fonte falsa).
- **REQ-08** — Atendido — `scan/FonteUhfBle.kt` embrulha o `BleManager` por lambdas sem alterá-lo em nada (arquivo `ble/BleManager.kt` intocado); preserva `LED_ON`/`LED_OFF` (constantes `COMANDO_INICIAR`/`COMANDO_PARAR`) e a remontagem por `__END__` via `scan/RemontadorDeFragmentos.kt`. Coberto por `FonteUhfBleTest` (5) e `RemontadorDeFragmentosTest` (4).
- **REQ-13** — Atendido — `model/BleMessage.kt` removido (`git rm`); antes da remoção, grep confirmou zero referências.
- **RNF-08** — Atendido — UI segue em Compose (`ui/TelaScanner.kt`, tela movida da MainActivity sem mudança visual); estrutura por responsabilidade: `domain/`, `scan/`, `ble/`, `ui/` (diretório `model/` extinto).

## Casos extremos cobertos
- Nenhum CE da spec pertence a este incremento (CE-10/11/13 são do INC-04). Robustez adicional testada: `__END__` sem fragmentos não emite leitura; `SCANNER_OFF` aciona aviso e não vira leitura; mensagens sucessivas geram leituras independentes com buffer limpo.

## Perguntas em aberto / pendências
- O card "Último Ativo Escaneado" continua exibindo o payload bruto (comportamento idêntico ao anterior); a exibição estruturada com código/origem/horário é o REQ-04 (INC-03).
- Comportamento herdado preservado: interpretação das leituras acontece via `FonteUhfBle` com `instante` de relógio injetável (`System.currentTimeMillis` em produção), o que viabiliza os testes de dedup do INC-03.
- Dependências novas: `androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0` (ViewModel) e `kotlinx-coroutines-test:1.10.2` (só em teste).
- Durante o TDD, 1 teste do ViewModel falhou por corrida de inscrição no `SharedFlow` (coletor do `init` ainda não inscrito); corrigido no próprio teste com `advanceUntilIdle()` antes do `emit` — registro honesto de que a correção foi no teste, não no código de produção.
