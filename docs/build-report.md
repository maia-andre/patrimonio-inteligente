# Build report — 19/08/2026

Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-05 — Modo código de barras (CameraX + ZXing)
Rodada: construção
Testes: 70 passando / 70 total — `sh gradlew testDebugUnitTest` (e `assembleDebug` compilando sem erro)

## Requisitos atendidos

- **REQ-06** — Atendido — a fonte `scan/FonteCodigoBarras.kt` captura pela câmera com CameraX (`ImageAnalysis`, `STRATEGY_KEEP_ONLY_LATEST`) e decodifica com `scan/DecodificadorZxing.kt` sobre `com.google.zxing:core` 3.5.4, restrito por hints **e** por filtro explícito a Code 128, Code 39 e QR Code. Coberto por `DecodificadorZxingTest` (decodificação das três simbologias, inclusive 1D rotacionado 90°) e `InterpretadorCodigoBarrasTest`. A decodificação real pela câmera fica para o `/verify` em aparelho, como a spec prevê.
- **REQ-12** — Atendido — a permissão de câmera é pedida quando o usuário seleciona o modo (`ScannerViewModel.selecionarModo` → `solicitarPermissaoCamera`), nunca na abertura; a troca de modo fica pendente até a resposta e respeita o parar-antes-de-iniciar. Coberto por `ScannerViewModelTest` (seção INC-05: pedido nasce da seleção, concessão completa a troca na ordem `parar → iniciar`, checagem passiva no `onStart` só reporta concessão, nunca negativa).
- **RNF-05** — Atendido — dependências novas: `com.google.zxing:core:3.5.4` (Apache 2.0) e `androidx.camera:*:1.6.1`. `gradlew :app:dependencies --configuration debugRuntimeClasspath` não contém `gms` nem `play-services` (0 ocorrências).

## Regras de negócio

- **RN-04** — Atendido — `SIMBOLOGIAS_ACEITAS`/`simbologiaAceita` (função pura, RNF-02) mais os hints `POSSIBLE_FORMATS`. Testes provam que EAN-13 e UPC-A codificados pelo próprio ZXing **não** são decodificados, e que o filtro rejeita EAN-8, UPC-E, DataMatrix e ITF.

## Casos extremos cobertos

- **CE-03** — Atendido — negar a permissão desabilita o modo com o motivo "Permissão de câmera negada" e expõe `podeReabrirPermissao`, renderizado como botão "Permitir câmera" (TelaScanner); o pedido pode ser reaberto e, na negativa permanente (sem diálogo do sistema), a MainActivity abre a tela de configurações do aplicativo. Negar não inicia fonte nenhuma, mantém o modo anterior e não derruba os demais modos — tudo em `ScannerViewModelTest`.
- **CE-12** — Atendido — quadro ilegível devolve nulo sem exceção nem mensagem: testes com ruído aleatório, quadro uniforme e código cortado ao meio. Na fonte, quadro sem leitura é silêncio e a câmera segue analisando.

## Transversais (ancorados no INC-01, verificados nesta rodada)

- **RNF-01** — código, comentários e testes novos em português.
- **RNF-02** — decodificação (`DecodificadorZxing`, `rotacionar90`, `simbologiaAceita`) e interpretação (`interpretarCodigoBarras`) são puras, testadas na JVM sem aparelho; a fonte CameraX é só adaptador.
- **RNF-03** — somente dados fictícios nos testes (147258, 369852, PATR-*, EAN/UPC inventados com dígito verificador válido).
- **RNF-07** — nenhuma chamada de rede; todo o processamento no aparelho.
- **RNF-04** — o manifesto já declarava câmera com `android:required="false"` (INC-04); esta rodada acrescentou só `uses-permission android.permission.CAMERA`.

## Decisões técnicas aplicadas

- **DT-01/DT-03** — `com.google.zxing:core` com analisador CameraX próprio; prévia via `PreviewView` criada programaticamente em Compose (`AndroidView`), sem layout XML e sem `zxing-android-embedded`.
- Decodificador tenta o quadro na orientação original e rotacionado 90° (códigos 1D na vertical), com `TRY_HARDER`.
- Testes do INC-04 que trocavam para o modo código de barras ganharam a pré-condição `atualizarPermissaoCamera(true)` — a seleção do modo agora passa pela permissão, por exigência do REQ-12.

## Perguntas em aberto / pendências

Nenhuma. Itens que exigem aparelho (decodificação real pela câmera, não-decodificação de um EAN-13 impresso, fluxo real do diálogo de permissão) ficam para o `/verify`, conforme a seção "Verificação que exige aparelho" da spec.
