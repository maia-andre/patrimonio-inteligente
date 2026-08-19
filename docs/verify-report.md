# Verify report — 19/08/2026 (INC-05)
Incremento: INC-05 — Modo código de barras (CameraX + ZXing) | Build report: 19/08/2026
Como rodei: `sh gradlew assembleDebug` + inspeção do manifesto **mesclado** e do grafo de dependências + driver Kotlin descartável na JVM 21 dirigindo a cadeia real: imagem de código de barras **renderizada** (java.awt, como uma etiqueta impressa) → `DecodificadorZxing` real → `interpretarCodigoBarras` real → `ScannerViewModel` real. Driver apagado ao final. Sem aparelho físico/emulador.
Suíte de testes: 70/70 passando — citado do build-report, sem re-execução.

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| REQ-06 | Decodificação das três simbologias a partir de imagens renderizadas | `Code 128 'PATR-147258' -> PATR-147258` · `Code 39 '369852' -> 369852` · `QR 'QR-852963' -> QR-852963` | FUNCIONA |
| REQ-06 | Código 1D na vertical (orientação do sensor da câmera) | imagem do Code 128 rotacionada 90° → `-> PATR-147258` | FUNCIONA |
| RN-04 | EAN-13 e UPC-A de varejo são ignorados | `EAN-13 de varejo -> null` · `UPC-A de varejo -> null` (imagens renderizadas de códigos válidos) | FUNCIONA |
| CE-12 | Quadro ilegível é silêncio, sem exceção | `Quadro de ruído -> null`; na cadeia completa: `contador=2 (não entrou, sem erro)` | FUNCIONA |
| REQ-12 | Pedido de permissão nasce da seleção do modo, sem troca imediata | `Após selecionar: pedidosDePermissao=1, modoSelecionado=RFID_UHF, eventos=[]` (fonte não iniciada) | FUNCIONA |
| CE-03 | Negativa desabilita com motivo e caminho de reabertura; app não trava | `disponivel=false motivo='Permissão de câmera negada' podeReabrirPermissao=true`; leitura UHF após a negativa → `contador=1` | FUNCIONA |
| CE-03 | Reabrir a solicitação e conceder ativa o modo | `Reabrir: pedidosDePermissao=2` → `Concedida: eventos=[uhf.parar, barras.iniciar], modoSelecionado=CODIGO_BARRAS, motivo=null` | FUNCIONA |
| REQ-10/RN-05 | A concessão completa a troca na ordem parar-antes-de-iniciar | `eventos=[uhf.parar, barras.iniciar]` — o `parar()` da fonte anterior precede o `iniciar()` da nova | FUNCIONA |
| REQ-06 + REQ-04/05 | Cadeia completa quadro→lista: leitura entra com origem, duplicata sinaliza (CE-01) | `Quadro QR -> contador=2, topo=QR-852963 origem=CODIGO_BARRAS`; repetido → `aviso='Item QR-852963 já conferido'`, contador inalterado | FUNCIONA |
| RNF-05 | Nenhum Google Play Services no grafo de runtime | `gradlew :app:dependencies --configuration debugRuntimeClasspath` → `ocorrencias de gms\|play-services: 0` | FUNCIONA |
| REQ-12 (manifesto) | Permissão declarada; recursos seguem opcionais (RNF-04) | manifesto mesclado: `<uses-permission android:name="android.permission.CAMERA" />`; `nfc`/`camera` com `android:required="false"` | FUNCIONA |
| — | APK monta com as dependências novas | `app-debug.apk` (44M) gerado por `assembleDebug` | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- Decodificação **pela câmera física** (foco, iluminação, enquadramento) e a não-decodificação de um EAN-13 impresso apontando a câmera de verdade: exige aparelho, como a spec prevê em "Verificação que exige aparelho". O caminho decodificador→lista foi dirigido com imagens renderizadas reais.
- Diálogo real de permissão do Android e o desvio para a tela de configurações na negativa permanente (`shouldShowRequestPermissionRationale`): comportamento do sistema, exige aparelho. A lógica por trás (estados, motivo, reabertura) foi dirigida na JVM.
- Prévia da câmera (`PreviewView`) e liberação física da câmera no `parar()`: exige aparelho; o contrato `iniciar()`/`parar()` foi dirigido de verdade.
