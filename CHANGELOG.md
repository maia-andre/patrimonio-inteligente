# Changelog

## INC-02 — ScannerViewModel e fonte UHF sobre o BleManager (18/08/2026)
- REQ-02: a lógica de protocolo sai da `MainActivity` — nasce o `ScannerViewModel`, que controla a fonte de leitura e expõe o estado da tela; a Activity vira só fiação.
- REQ-08: o modo RFID UHF passa a operar como `FonteDeLeitura` (`scan/FonteUhfBle`) sobre o `BleManager` existente, sem reescrevê-lo — `LED_ON`/`LED_OFF` e a remontagem de fragmentos por `__END__` preservados.
- REQ-13: removido `model/BleMessage.kt`, que não tinha nenhuma referência no projeto.
- RNF-08: estrutura de diretórios por responsabilidade consolidada (`domain/`, `scan/`, `ble/`, `ui/`), com a tela em Compose movida para `ui/TelaScanner.kt`.

## INC-01 — Camada de domínio e parser UHF (18/08/2026)
- REQ-01: o aplicativo ganha uma camada de domínio independente de Android — `LeituraPatrimonial`, `OrigemLeitura` (código de barras, NFC, RFID UHF) e o contrato `FonteDeLeitura`, porta única pela qual toda origem de captura entrega leituras.
- REQ-09: o payload do modo RFID UHF passa a ser interpretado por um parser puro tolerante a dois formatos (RN-03): `codigo;descricao` e o formato atual do firmware, sem separador — com o payload bruto sempre preservado.
- Cobertos os casos extremos CE-07, CE-08 e CE-09 por teste unitário (12 testes na suíte, sem exigir aparelho nem emulador).
