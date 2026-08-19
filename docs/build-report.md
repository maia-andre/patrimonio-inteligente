# Build report — 18/08/2026 (INC-03)
Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-03 — Lista de leituras com deduplicação e contador
Rodada: construção
Testes: 42 passando / 42 total — `sh gradlew test` (unit; instrumentada exige aparelho)

## Requisitos atendidos
- **REQ-04** — Atendido — leituras acumulam em memória no `ScannerViewModel` (`EstadoTelaScanner.leituras`); a `TelaScanner` exibe o contador ("Conferidos na sessão: N") e cada linha mostra a chave (código, ou bruto quando o código é nulo), a origem legível (`rotuloDaOrigem`) e o horário (HH:mm:ss do `instante`). Coberto por `AcumuladorDeLeiturasTest` e `ScannerViewModelTest`.
- **REQ-05** — Atendido — chave já presente não gera linha nova (`AcumuladorDeLeituras`, função pura) e o usuário é sinalizado por card de aviso ("Item X já conferido") + log. Coberto por `chave repetida nao gera linha nova e sinaliza ja conferido`.
- **RN-01** — Atendido — `LeituraPatrimonial.chave = codigo ?: bruto`; origens diferentes com a mesma chave deduplicam (teste `origens diferentes com a mesma chave sao o mesmo bem`); sem código, deduplica pelo bruto.
- **RN-06** — Atendido — inserção sempre no topo (`listOf(nova) + listaAtual`); testes verificam a ordem.
- **RN-07** — Atendido — lista apenas em `MutableStateFlow` (memória); nenhuma persistência adicionada; comentário na classe de estado registra a regra.
- **CE-01** — Atendido — repetição não cria linha nem incrementa contador; sinalização limitada a 1 ocorrência/segundo por chave via `LimitadorDeSinalizacao` (puro, decide pelo `instante` da leitura — determinístico em teste). Cobertos os limites 400 ms, 999 ms (bloqueia) e 1 000 ms (permite).
- **CE-14** — Atendido — sem leituras, a tela mostra card de estado vazio explicativo ("Nenhum item conferido ainda..."), não uma lista em branco; o estado inicial vazio é testado no ViewModel.

## Casos extremos cobertos
- **CE-01** — `sinalizacao de duplicata e limitada a uma por segundo para a mesma chave` (ViewModel) + `LimitadorDeSinalizacaoTest` (4 testes de limite).
- **CE-14** — `estado inicial tem lista vazia para a tela mostrar o estado vazio` + card de estado vazio na `TelaScanner`.

## Perguntas em aberto / pendências
- O card "Último Ativo Escaneado" (comportamento herdado do app original, mantido no INC-02) foi substituído pela lista do REQ-04 — a lista é a evolução direta daquele card; o campo `ultimaLeitura` permanece no estado.
- O aviso "já conferido" é limpo quando entra uma leitura nova de chave distinta (decisão de UI não normatizada pela spec; registrada aqui para o review).
- A linha da lista exibe exatamente os três campos do REQ-04 (código/chave, origem, horário); a descrição não é exibida por não constar do requisito.
