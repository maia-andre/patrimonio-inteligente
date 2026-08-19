# Review report — 19/08/2026 (INC-06)
Spec: docs/spec.md, versão 1 (10/08/2026, aprovada) | Incremento: INC-06 — Modo NFC (reader mode) | Build report: 19/08/2026 (INC-06)

## VEREDITO: APROVADO

Suíte rodada pelo auditor a partir de `clean`: **86/86 passando, 0 falhas** (`sh gradlew clean testDebugUnitTest`). Verify-report de 19/08 (INC-06) sem FALHAs em aberto.

## Verificação requisito a requisito

| Item | Status | Evidência / Falha |
|------|--------|-------------------|
| REQ-07 | Atendido | `scan/FonteNfc.kt` — `enableReaderMode` no `iniciar()` com exatamente `FLAG_READER_NFC_A or NFC_B or NFC_F or NFC_V` (as quatro tecnologias do requisito, sem `SKIP_NDEF_CHECK`, preservando o `cachedNdefMessage`); `disableReaderMode` no `parar()`. Etiqueta reduzida a bytes e entregue à função pura. Cadeia bytes→extrator→lista dirigida no verify. Detecção física fica para verify com aparelho, como a spec prevê. |
| RN-02 | Atendido | `domain/InterpretadorEtiquetaNfc.kt` — precedência exata: primeiro registro TNF well-known + RTD "T" com texto não vazio → `codigo`; senão UID maiúsculo sem separadores. Auditados os limites do parser: payload vazio, idioma declarado maior que o payload (`inicioTexto >= payload.size`), bit de codificação UTF-16 (decodificação com BOM pelo `Charsets.UTF_16`), tipo "T" fora do TNF well-known rejeitado. Os dois itens da definição de concluído reproduzidos literalmente em teste e no driver do verify. |
| CE-02 | Atendido | Motivo "Aparelho sem NFC" (`ScannerViewModel.montarModos`); estado inicial conservador; teste "sem hardware nfc os demais modos operam normalmente" prova o convívio (leitura UHF entra). Selecionar modo indisponível é ignorado (dirigido no verify). |
| CE-04 | Atendido | Motivo "NFC desligado", textualmente distinto do CE-02 (teste compara); reavaliação a cada `onStart` na `MainActivity` cobre ligar o NFC nas configurações e voltar; queda em uso preserva a lista (teste + verify). |
| CE-05 | Atendido | `Ndef.get(etiqueta) == null` → lista vazia → UID (`FonteNfc.registrosDe`); teste "sem ndef o codigo e o uid..." e driver do verify. |
| CE-06 | Atendido | NDEF com só URI, texto vazio, payload malformado e TNF errado: 4 testes, todos caindo para o UID sem exceção. |

### Transversais e regressão

| Item | Status | Evidência |
|------|--------|-----------|
| RNF-01/RNF-03 | Atendido | Tudo em português; dados fictícios (UID da própria spec). |
| RNF-02 | Atendido | `interpretarEtiquetaNfc`/`extrairTextoNdef`/`uidEmHexadecimal` puros — `RegistroNdef` carrega só bytes, nenhum tipo `android.*` no domain. |
| RNF-04 | Atendido | Manifesto mesclado: `android.hardware.nfc` segue `required="false"`; só `uses-permission android.permission.NFC` (permissão normal, concedida na instalação) foi acrescentada. |
| RNF-07/RNF-08 | Atendido | Sem rede; arquivos em `domain/` e `scan/` conforme a estrutura. |
| Regressão INC-01..05 | Limpa | 86/86 do zero; RN-01 entre origens dirigida no verify (NFC + código de barras com a mesma chave não duplica). A correção de ordem de inicialização do ViewModel (campos consultados por `montarModos` movidos para antes do `_estado`) foi pega por teste vermelho na própria rodada e beneficia também o estado da permissão de câmera. |
| Escopo | Limpo | Só leitura (`cachedNdefMessage`), sem `connect()` nem gravação de etiqueta — "Fora de escopo" respeitado. |

## Qualidade dos testes (TDD)

- Inversão mental: precedência invertida, filtro de TNF removido, UTF-16 ignorado, zero à esquerda perdido, motivo trocado — cada um derrubaria um teste específico.
- Entradas inválidas cobertas de verdade (payload vazio, malformado, TNF de mídia), não só caminho feliz.
- O bug real de inicialização (`montarModos` rodando antes de `estadoNfc` existir) foi capturado por teste novo na fase vermelha — evidência de que os testes exercitam comportamento, não implementação.
- Lacuna consciente: `FonteNfc` (cola Android) sem teste unitário, mesmo padrão aceito para `BleManager` e `FonteCodigoBarras`; toda a lógica de interpretação é pura e testada.

## Segurança

- **Entradas**: conteúdo de etiqueta NFC é entrada não confiável por natureza (qualquer pessoa pode aproximar uma etiqueta). O parser é bounds-checked, não lança exceção com payload malformado (4 testes) e o texto extraído vai para `Text` do Compose e log local — sem superfície de injeção, sem rede (RNF-07). Severidade: nenhuma.
- **Permissões**: `NFC` é permissão normal de instalação, mínima para o reader mode; recurso opcional preserva RNF-04.
- **Segredos/LGPD**: nenhum segredo; só dados fictícios.
- **Dependências**: nenhuma dependência nova nesta rodada.

## Correções necessárias (para o /build)

Nenhuma.

**Próximo passo**: `/ship` para fechar o INC-06.
