# Verify report — 19/08/2026 (INC-06)
Incremento: INC-06 — Modo NFC (reader mode) | Build report: 19/08/2026 (INC-06)
Como rodei: `sh gradlew assembleDebug` + inspeção do manifesto **mesclado** + driver Kotlin descartável na JVM 21 dirigindo a cadeia real: bytes NDEF/UID (montados como uma etiqueta os carrega) → `interpretarEtiquetaNfc` real → `ScannerViewModel` real com as três fontes roteirizadas. Driver apagado ao final. Sem aparelho físico/emulador.
Suíte de testes: 86/86 passando — citado do build-report, sem re-execução.

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| RN-02 (1º caminho) | Etiqueta com registro NDEF de texto | `NDEF texto '147258' -> codigo=147258`; UTF-16 também: `'369852' -> codigo=369852` | FUNCIONA |
| RN-02 (2º caminho) | Etiqueta sem NDEF: UID hexadecimal maiúsculo | UID `04 A2 24 B2 5C 61 80` → `codigo=04A224B25C6180` (item literal da definição de concluído) | FUNCIONA |
| CE-05/CE-06 | NDEF só com URI e NDEF com texto vazio caem para o UID | `NDEF só com URI -> 04A224B25C6180` · `NDEF texto vazio -> 04A224B25C6180` | FUNCIONA |
| CE-02 | Sem hardware: modo desabilitado com motivo próprio | `disponivel=false motivo='Aparelho sem NFC'`; selecionar é ignorado (`modoSelecionado=RFID_UHF, eventos=[]`) | FUNCIONA |
| CE-04 | NFC desligado: motivo distinto do sem-hardware | `disponivel=false motivo='NFC desligado'` ≠ `'Aparelho sem NFC'` | FUNCIONA |
| REQ-10/RN-05 | Com NFC disponível, a troca para antes de iniciar | `eventos=[uhf.parar, nfc.iniciar], modoSelecionado=NFC` | FUNCIONA |
| REQ-07 + REQ-04/05 | Etiqueta encostada entra na lista; repetida sinaliza | `contador=1, topo=147258 origem=NFC`; sem NDEF → `contador=2, topo=04A224B25C6180`; repetida → `aviso='Item 04A224B25C6180 já conferido'`, contador inalterado | FUNCIONA |
| RN-01 (regressão) | Mesma chave vinda de origem diferente não duplica | barcode `'147258'` após etiqueta NFC `'147258'` → `contador=2 (não duplicou), aviso='Item 147258 já conferido'` | FUNCIONA |
| CE-04 (dinâmica) | NFC desligado durante o uso preserva a lista | `disponivel=false motivo='NFC desligado', contador=2` | FUNCIONA |
| REQ-07 (manifesto) | Permissão NFC declarada; recurso segue opcional (RNF-04) | manifesto mesclado: `android.permission.NFC` presente; `android.hardware.nfc` com `required="false"` | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- Detecção física de etiqueta via `enableReaderMode` (flags NfcA/NfcB/NfcF/NfcV), leitura do `cachedNdefMessage` de uma etiqueta real e a liberação da antena no `parar()`: exigem aparelho, como a spec prevê em "Verificação que exige aparelho". A cadeia bytes→extrator→lista foi dirigida com as mesmas estruturas de bytes que o Android entrega.
- `NfcAdapter.getDefaultAdapter`/`isEnabled` reais (aparelho sem NFC e NFC desligado de verdade): comportamento do sistema; a lógica de motivos por trás foi dirigida na JVM nos três estados.
