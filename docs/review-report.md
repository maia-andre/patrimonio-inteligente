# Review report — 19/08/2026
Spec: docs/spec.md, versão 1 (10/08/2026, aprovada) | Incremento: INC-05 — Modo código de barras (CameraX + ZXing) | Build report: 19/08/2026

## VEREDITO: APROVADO

Suíte rodada pelo auditor a partir de `clean`: **70/70 passando, 0 falhas** (`sh gradlew clean testDebugUnitTest`). Verify-report de 19/08 sem FALHAs em aberto.

## Verificação requisito a requisito

| Item | Status | Evidência / Falha |
|------|--------|-------------------|
| REQ-06 | Atendido | `scan/FonteCodigoBarras.kt` (CameraX `ImageAnalysis` → `analisar` → canal) + `scan/DecodificadorZxing.kt:27-58` (MultiFormatReader restrito por hints); decodificação das 3 simbologias provada em `DecodificadorZxingTest` e dirigida no verify com imagens renderizadas, inclusive 1D rotacionado 90°. |
| REQ-12 | Atendido | `ScannerViewModel.kt:111-114` — o pedido nasce em `selecionarModo`, nunca na abertura (o `checkPermissions` do `onCreate` só pede BLE; o `onStart` faz checagem passiva que **só** reporta concessão, `MainActivity.kt`). Testes: "selecionar codigo de barras sem permissao pede a permissao e nao inicia a fonte", "permissao concedida completa a troca parando a fonte anterior antes". |
| RNF-05 | Atendido | Dependências novas: `com.google.zxing:core:3.5.4` + `androidx.camera:*:1.6.1` (`app/build.gradle.kts`); `debugRuntimeClasspath` com **0** ocorrências de `gms`/`play-services` (verificado no verify e conferido pelo auditor no build.gradle — nenhuma dependência Google adicionada além de androidx). |
| RN-04 | Atendido | Dupla barreira: hints `POSSIBLE_FORMATS` + filtro puro `simbologiaAceita` (`DecodificadorZxing.kt:12-19,53`). Testes provam EAN-13 e UPC-A codificados e **não** decodificados, e o filtro rejeitando EAN-8/UPC-E/DataMatrix/ITF. |
| CE-03 | Atendido | Negativa → `montarModos` marca indisponível com motivo "Permissão de câmera negada" e `podeReabrirPermissao=true` (`ScannerViewModel.kt:87-100`); TelaScanner renderiza o botão "Permitir câmera" (caminho de reabertura); negativa permanente desvia para as configurações do app (`MainActivity.pedirPermissaoCamera`). Testes: negativa não inicia fonte, mantém modo anterior, não derruba os demais modos. |
| CE-12 | Atendido | `ReaderException` engolida e retorno nulo (`DecodificadorZxing.kt:54-56`); quadro sem leitura é silêncio na fonte (`FonteCodigoBarras.analisar`). Testes com ruído, quadro uniforme e código cortado ao meio. |

### Transversais e regressão

| Item | Status | Evidência |
|------|--------|-----------|
| RNF-01/RNF-03 | Atendido | Código, comentários e testes novos em português; só dados fictícios (PATR-*, 147258, EAN/UPC inventados com dígito verificador calculado). |
| RNF-02 | Atendido | `DecodificadorZxing`, `rotacionar90`, `simbologiaAceita`, `interpretarCodigoBarras` — puros, sem `android.*`, testados na JVM. |
| RNF-04 | Atendido | Manifesto mesclado mantém `nfc`/`camera` com `required="false"`; rodada só acrescentou `uses-permission CAMERA`. |
| RNF-07 | Atendido | Nenhuma chamada de rede no código novo. |
| RNF-08 | Atendido | Novos arquivos seguem a estrutura: `domain/InterpretadorCodigoBarras.kt`, `scan/DecodificadorZxing.kt`, `scan/FonteCodigoBarras.kt`; UI em Compose (prévia via `AndroidView`+`PreviewView` programático, coerente com DT-01). |
| Regressão INC-01..04 | Limpa | 70/70 do zero; os 3 testes do INC-04 que trocavam para código de barras ganharam a pré-condição `atualizarPermissaoCamera(true)` — adaptação legítima exigida pelo REQ-12, com a intenção original (ordem parar→iniciar, descarte de fonte parada, segundo plano) preservada e verde. |
| Escopo | Limpo | Prévia da câmera e desvio às configurações são meios necessários de REQ-06/CE-03, não funcionalidade nova. Nada do "Fora de escopo" foi tocado (sem persistência, sem NFC, sem firmware). |

## Qualidade dos testes (TDD)

- Inversão mental: cada comportamento novo tem teste que ficaria vermelho — filtro deixando EAN passar, troca imediata sem permissão, pendência não completada na concessão, motivo ausente na negativa, rotação removida. Vermelho→verde registrado na rodada (compilação falhou com as referências ausentes antes da implementação).
- CE cobertos além do caminho feliz: ruído, quadro uniforme, código parcial (CE-12); negativa, reabertura, concessão tardia, negativa sem derrubar os demais modos (CE-03).
- Sem testes vazios/triviais; os testes exercitam o comportamento, não mocks.
- Lacuna consciente e aceitável: `FonteCodigoBarras` (cola CameraX) não tem teste unitário — mesmo padrão do `BleManager`; sua lógica decodificável é 100% pura e testada, e o contrato iniciar/parar foi dirigido no verify.

## Segurança

- **Entradas**: o texto decodificado vem de etiquetas arbitrárias e vai para `Text` do Compose (sem interpretação de markup) e para log local — sem injeção. Nenhuma persistência nem rede (RNF-07). Severidade: nenhuma.
- **Permissões**: CAMERA pedida no uso, mínimo necessário; recursos opcionais no manifesto preservam instalabilidade (RNF-04).
- **Segredos**: nenhum. **LGPD**: só dados fictícios.
- **Dependências**: ZXing core 3.5.4 e CameraX 1.6.1 são as últimas estáveis; sem vulnerabilidade conhecida aplicável (ZXing core puro, sem superfície de rede).

## Correções necessárias (para o /build)

Nenhuma.

**Próximo passo**: `/ship` para fechar o INC-05.
