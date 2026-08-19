# Verify report — 18/08/2026 (INC-02)
Incremento: INC-02 — ScannerViewModel e fonte UHF sobre o BleManager | Build report: 18/08/2026 (INC-02)
Como rodei: (1) `sh gradlew assembleDebug` para provar o empacotamento real; (2) driver Kotlin descartável na JVM 21 dirigindo a fiação idêntica à da `MainActivity` — `FonteUhfBle` + `ScannerViewModel` reais — com o transporte BLE substituído pela simulação da sequência de mensagens do ESP32 (fragmentos + `__END__` + `SCANNER_OFF`), compilado contra as classes do app com kotlin-compiler-embeddable. Driver apagado ao final. Sem aparelho físico/emulador nesta máquina.
Suíte de testes: 27/27 passando — citado do build-report, sem re-execução.

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| Empacotamento | APK debug montado com a nova arquitetura | `sh gradlew assembleDebug` → `BUILD SUCCESSFUL`, `app-debug.apk` (30 MB) | FUNCIONA |
| REQ-02 | Estado da tela exposto pelo ViewModel: conexão alternada → status muda; leitura chega → estado atualiza | `alternarConexao()` → `status="Scanner Conectado" conectado=true`; após fragmentos → `ultimaLeitura no estado: LeituraPatrimonial(codigo=147258, descricao=Notebook Positivo, origem=RFID_UHF, bruto=147258;Notebook Positivo, instante=1787100542294)` | FUNCIONA |
| REQ-02 | `MainActivity` sem lógica de protocolo | `grep -nE "__END__\|messageBuffer\|interpretar\|StringBuilder\|dedup" MainActivity.kt` → nenhuma ocorrência (exit 1) | FUNCIONA |
| REQ-08 | `LED_ON`/`LED_OFF` preservados via fonte | `iniciarLeitura()` → `[BLE TX] LED_ON`; `pararLeitura()` → `[BLE TX] LED_OFF`; `comandos enviados ao BLE: [LED_ON, LED_OFF]` | FUNCIONA |
| REQ-08 | Remontagem por `__END__` preservada fora da MainActivity | fragmentos `"147258;Notebo"` + `"ok Positivo"` + `"__END__"` → leitura única com `bruto=147258;Notebook Positivo` | FUNCIONA |
| REQ-08 | `SCANNER_OFF` tratado como controle, não como leitura | `apos SCANNER_OFF: status="Scanner Desligado"`, log `[RX] Scanner desligado.`, nenhuma leitura nova | FUNCIONA |
| REQ-08 | `BleManager` reaproveitado sem reescrita | `git diff HEAD -- app/src/main/java/com/patrimoniosjc/rfidpoc/ble/` → vazio (arquivo intocado) | FUNCIONA |
| REQ-13 | `model/BleMessage.kt` removido | `test -f .../model/BleMessage.kt` → REMOVIDO; diretório `model/` extinto | FUNCIONA |
| RNF-08 | Estrutura por responsabilidade | `ls .../rfidpoc/` → `ble domain scan ui MainActivity.kt`; UI em Compose (`ui/TelaScanner.kt`) | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- UI real em aparelho (toque nos botões, renderização Compose) e conexão BLE física com o ESP32: sem aparelho/emulador nesta máquina. A fiação exata da `MainActivity` foi dirigida na JVM; o teste em aparelho segue na seção "Verificação que exige aparelho" da spec.
