# Verify report — 19/08/2026 (INC-07)
Incremento: INC-07 — Documentação: README e CONTRIBUTING | Build report: 19/08/2026 (INC-07)
Como rodei: renderização real dos diagramas Mermaid com `@mermaid-js/mermaid-cli` (npx) → SVG; validação das âncoras internas com o algoritmo de slug do GitHub (python); checagem de consistência das 9 tabelas markdown; greps literais dos itens do REQ-14/REQ-15/RNF-06. Artefatos temporários apagados. Sem código tocado nesta rodada.
Suíte de testes: 86/86 passando — citado do build-report, sem re-execução (código inalterado desde o INC-06).

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| REQ-14.1 | Tabela dos três modos + advertência | seção "Os três modos de captura" com a tabela de 4 colunas de perfil; `grep 'NFC não é RFID UHF e não o substitui'` → 1 ocorrência | FUNCIONA |
| REQ-14.2 | Linhas novas no Estado real | linhas 153-154: "Leitura por código de barras (câmera) ✅ Funciona" e "Leitura por NFC ✅ Funciona" | FUNCIONA |
| REQ-14.3 | Gargalo delimitado ao modo UHF | linha 169: "O **modo RFID UHF** — e apenas ele — está travado..." | FUNCIONA |
| REQ-14.4 | Fecho sem o pré-requisito de R$ 1.200 | linha 414: "começa com um celular Android que o seu setor já tem — e escala com R$ 1.200..." | FUNCIONA |
| REQ-14.5 | Caminho sem hardware em "Reproduza" | subseção "Comece sem hardware nenhum" (3 passos) + lista de materiais retitulada "(modo RFID UHF)" | FUNCIONA |
| REQ-14.6 | Diagrama com as três origens **renderizado de verdade** | `npx @mermaid-js/mermaid-cli -i diagrama1.mmd -o diagrama1.svg` → SVG de 32 KB gerado; nós presentes no SVG: `Câmera`, `Antena NFC`, `BleManager`, `ScannerViewModel`, `Código de barras`, `Etiqueta NFC`, aresta `visada direta` (1 ocorrência cada) | FUNCIONA |
| REQ-14.7 | Débito técnico registra o lado do app pronto | `grep 'já aceita o payload estruturado'` → 1 ocorrência, no callout do Protocolo BLE | FUNCIONA |
| REQ-14.8 | Roadmap curto prazo atualizado | 3 itens `[x]` (modos sem hardware; domínio+lista; payload no app) e 4 pendências `[ ]` preservadas | FUNCIONA |
| REQ-14.9 | Sumário incorpora a seção nova | link `[Os três modos de captura](#-os-três-modos-de-captura)` no sumário; validador de âncoras (algoritmo de slug do GitHub): link novo resolve; 16 links internos, únicos apontados como "quebrados" são os 3 pré-existentes com variation selector de emoji (⚙️/🗺️/🏛️), padrão que o GitHub resolve e que esta rodada não tocou | FUNCIONA |
| REQ-15 | CONTRIBUTING delimita o bloqueio ao modo UHF | linha 13: "O **modo RFID UHF** — o único capaz de inventário em massa — está bloqueado..." com o pedido "segue válido e prioritário" preservado | FUNCIONA |
| RNF-06 | minSdk 28 em acordo com o build | README "Android mínimo \| 9 (API 28)" ↔ `build.gradle.kts` `minSdk = 28`; `grep 'Android 12+'` → resta só a menção de permissões Bluetooth (linha 275), que a RNF-06 manda preservar | FUNCIONA |
| Estrutura | Tabelas markdown bem formadas | verificador de pipes: 9 tabelas, colunas inconsistentes: nenhuma; diagrama 2 (roadmap) também renderiza (SVG 16 KB) | FUNCIONA |
| Def. de concluído | Nenhuma frase "R$ 1.200" afirmando projeto bloqueado | 4 ocorrências: duas delimitadas ao modo UHF, uma de custo de bancada, uma no fecho reformulado | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- Renderização final no GitHub.com (âncoras com emoji e mermaid do lado do servidor): validada localmente com o mermaid-cli oficial e com o algoritmo de slug documentado do GitHub; a conferência visual no repositório publicado fica para quando houver push/remoto.
