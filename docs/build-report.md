# Build report — 18/08/2026
Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-01 — Camada de domínio e parser UHF
Rodada: construção
Testes: 12 passando / 12 total — `sh gradlew test` (unit; instrumentada exige aparelho)

## Requisitos atendidos
- **REQ-01** — Atendido — `domain/LeituraPatrimonial.kt`, `domain/OrigemLeitura.kt` e `domain/FonteDeLeitura.kt`, sem nenhum import de `android.*` (verificado por grep); contrato exercitado com fonte falsa em `FonteDeLeituraTest`.
- **REQ-09** — Atendido — `domain/InterpretadorPayloadUhf.kt`, função pura `interpretarPayloadUhf`, coberta por `InterpretadorPayloadUhfTest` (9 casos).
- **RNF-01** — Atendido — código, comentários e testes em português.
- **RNF-02** — Atendido — o parser UHF é função pura, sem dependência de API Android; roda por teste unitário JVM.
- **RNF-03** — Atendido — todos os dados de teste são fictícios (`147258`, "Notebook Positivo", "Cadeira giratoria cinza").
- **RNF-07** — Atendido — nenhum código de rede; processamento inteiramente local.
- **RN-03** — Atendido — separação pelo primeiro `;`, `bruto` sempre preservado; testes cobrem os dois formatos.

## Casos extremos cobertos
- **CE-07** — `payload sem separador produz codigo nulo e payload inteiro na descricao`
- **CE-08** — `payload com descricao vazia produz descricao nula`
- **CE-09** — `apenas o primeiro separador divide o payload`
- Adicionais de robustez: payload vazio, payload `";"`, código vazio antes do separador (todos → nulo, coerente com CE-08/DT-02).

## Perguntas em aberto / pendências
- Interpretação registrada (não bloqueante): a spec define descrição vazia → nula (CE-08), mas não define código vazio (`";Notebook"`). Por coerência com CE-08 e DT-02, código vazio também vira nulo — a deduplicação então cai para `bruto`, conforme RN-01. Se a intenção for outra, corrigir em rodada de correção.
- Infra local: criado `local.properties` apontando o Android SDK (arquivo já ignorado pelo `.gitignore`, não entra no repositório).
- Dependência nova: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2` (necessária para o `Flow` do REQ-01; Apache 2.0, sem Play Services).
