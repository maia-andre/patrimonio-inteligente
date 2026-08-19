# Verify report — 18/08/2026 (INC-04)
Incremento: INC-04 — Seletor de modos e ciclo de vida das fontes | Build report: 18/08/2026 (INC-04)
Como rodei: `sh gradlew assembleDebug` + inspeção do manifesto **mesclado** do APK + driver Kotlin descartável na JVM 21 dirigindo o `ScannerViewModel` real com a `FonteUhfBle` real (UHF) e uma fonte roteirizada no lugar do modo código de barras (que só nasce no INC-05). Driver apagado ao final. Sem aparelho físico/emulador.
Suíte de testes: 50/50 passando — citado do build-report, sem re-execução.

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| RNF-04 | Manifesto mesclado do APK declara os recursos como opcionais | `grep` no merged manifest → `android.hardware.nfc ... required="false"` e `android.hardware.camera ... required="false"` | FUNCIONA |
| REQ-03/REQ-11 | Seletor com os três modos, um selecionado, indisponíveis com motivo | estado inicial: `RFID_UHF disponivel=false motivo=Scanner BLE não conectado`; `NFC disponivel=false motivo=Ainda não disponível nesta versão`; `modoSelecionado=RFID_UHF` | FUNCIONA |
| REQ-11 (dinâmica) | Conectar BLE muda a disponibilidade do UHF | após `alternarConexao()`: `RFID_UHF disponivel=true motivo=null` | FUNCIONA |
| CE-11 | Queda do BLE durante o uso: modo indisponível, lista preservada | leitura acumulada (`contador=1`) → desconexão → `RFID_UHF disponivel=false motivo=Scanner BLE não conectado`, `contador apos queda=1` | FUNCIONA |
| REQ-10/RN-05 | Troca de modo para a fonte anterior antes de iniciar a nova — com a fonte UHF **real** | `eventos=[uhf.comando(LED_OFF), barras.iniciar]` — o `parar()` real do UHF (LED_OFF) precede o `iniciar()` da nova | FUNCIONA |
| CE-10 | Leitura da fonte parada após a troca é descartada | payload UHF completo (`999111;Fantasma` + `__END__`) após a troca → `contador=1` (não entrou) | FUNCIONA |
| CE-13 | Segundo plano para a captura; voltar retoma | `eventos=[barras.parar, barras.iniciar]` via `aoEntrarEmSegundoPlano`/`aoVoltarAoPrimeiroPlano` (ligados a `onStop`/`onStart`) | FUNCIONA |
| REQ-03 (defesa) | Selecionar modo indisponível é ignorado | `selecionarModo(NFC)` → `modoSelecionado=CODIGO_BARRAS` (inalterado) | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- Liberação física de câmera/antena NFC na troca de modo: essas fontes ainda não existem (INC-05/06); o contrato `parar()`-antes-de-`iniciar()` foi dirigido com a fonte real UHF e fontes falsas.
- Instalação real em aparelho sem NFC/câmera (RNF-04): conferida no manifesto mesclado; a instalação física segue para o `/verify` com aparelho.
- `onStart`/`onStop` reais do Android: fiação por override simples na MainActivity; o comportamento por trás foi dirigido na JVM.
