# Verify report — 18/08/2026
Incremento: INC-01 — Camada de domínio e parser UHF | Build report: 18/08/2026
Como rodei: biblioteca pura sem superfície executável (a UI só consome o domínio no INC-02) — dirigi a API pública com driver Kotlin descartável, compilado com kotlin-compiler-embeddable 2.2.10 contra as classes de `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes` e executado na JVM 21, fora de qualquer runtime Android. Driver apagado ao final.
Suíte de testes: 12/12 passando — citado do build-report, sem re-execução.

## Fluxos dirigidos
| Item | Fluxo exercitado | Evidência (comando → saída) | Resultado |
|------|------------------|-----------------------------|-----------|
| REQ-09/RN-03 | Parser com payload nos dois formatos | `interpretarPayloadUhf("147258;Notebook Positivo")` → `codigo=147258 \| descricao=Notebook Positivo \| bruto="147258;Notebook Positivo"` | FUNCIONA |
| CE-07 | Payload sem `;` (formato atual do firmware) | `interpretarPayloadUhf("Cadeira giratoria cinza")` → `codigo=null \| descricao=Cadeira giratoria cinza \| bruto` íntegro | FUNCIONA |
| CE-08 | Descrição vazia | `interpretarPayloadUhf("147258;")` → `codigo=147258 \| descricao=null \| bruto="147258;"` | FUNCIONA |
| CE-09 | Mais de um `;` | `interpretarPayloadUhf("147258;Notebook;fonte 90W")` → `codigo=147258 \| descricao=Notebook;fonte 90W` | FUNCIONA |
| RN-03 (bordas) | Código vazio, payload vazio, só separador | `";Notebook Positivo"` → `codigo=null`; `""` → ambos nulos, `bruto=""`; `";"` → ambos nulos, `bruto=";"` | FUNCIONA |
| REQ-01 | Enum com exatamente os três modos | `OrigemLeitura.entries` → `[CODIGO_BARRAS, NFC, RFID_UHF]` | FUNCIONA |
| REQ-01 | Contrato `FonteDeLeitura` implementado por fonte falsa fora do Android: `iniciar()` → emissão → coleta pelo `Flow` → `parar()` | `iniciar() -> ativa=true`; `recebida pelo Flow: LeituraPatrimonial(codigo=369852, descricao=Projetor Epson, origem=RFID_UHF, bruto=369852;Projetor Epson, instante=1755500001000)`; `parar() -> ativa=false` | FUNCIONA |
| RNF-02 | Parser executa em JVM pura, sem runtime Android | driver rodou com `java -cp` usando apenas kotlin-stdlib + kotlinx-coroutines-core-jvm | FUNCIONA |
| REQ-01 (independência) | `domain/` sem import `android.*` | `grep -rn "import android" app/src/main/java/com/patrimoniosjc/rfidpoc/domain/` → nenhuma ocorrência (exit 1) | FUNCIONA |

## Falhas encontradas (para o /build)
Nenhuma.

## Não verificável de ponta a ponta
- Integração do domínio com a UI e com o BLE real: não existe ainda por desenho — é o escopo do INC-02. O modo UHF contra o ESP32 físico segue na seção "Verificação que exige aparelho" da spec.
