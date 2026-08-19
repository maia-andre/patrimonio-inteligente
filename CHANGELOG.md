# Changelog

## INC-05 — Modo código de barras (CameraX + ZXing) (19/08/2026)
- REQ-06: o modo código de barras captura pela câmera do celular — CameraX com decodificação ZXing pura (sem Google Play Services, RNF-05), com prévia na tela e leitura de códigos 1D em qualquer orientação.
- RN-04: só Code 128, Code 39 e QR Code são aceitos; EAN e UPC de varejo são ignorados, para não capturar o código da embalagem no lugar do código do bem.
- REQ-12: a permissão de câmera é pedida só quando o usuário seleciona o modo — nunca na abertura do aplicativo.
- CE-03: permissão negada desabilita o modo com o motivo e o botão "Permitir câmera" para pedir de novo (negativa permanente leva às configurações do aplicativo); negar não trava nada.
- CE-12: quadro ilegível ou fora de foco é silêncio — a câmera segue tentando, sem mensagens de erro.

## INC-04 — Seletor de modos e ciclo de vida das fontes (18/08/2026)
- REQ-03: a tela ganha o seletor com os três modos de captura — código de barras, NFC e RFID UHF — com exatamente um ativo por vez.
- REQ-10/RN-05: trocar de modo para a fonte anterior antes de iniciar a nova; leitura de fonte parada é descartada (CE-10).
- REQ-11: modo indisponível aparece desabilitado com o motivo legível; queda do BLE torna o UHF indisponível preservando a lista (CE-11).
- CE-13: em segundo plano a captura para; ao voltar, o modo selecionado é retomado.
- RNF-04: NFC e câmera declarados opcionais no manifesto — o app permanece instalável em aparelho sem esses recursos.

## INC-03 — Lista de leituras com deduplicação e contador (18/08/2026)
- REQ-04: as leituras acumulam em lista na tela, com contador de itens conferidos na sessão; cada linha mostra o código, a origem e o horário, com a mais recente no topo.
- REQ-05: item já conferido não gera linha nova — o aplicativo avisa "já conferido", limitado a uma sinalização por segundo por código (CE-01).
- RN-01: deduplicação pela chave `código ?: bruto`, inclusive entre origens diferentes.
- CE-14: sem leituras, a tela mostra estado vazio explicativo em vez de lista em branco.

## INC-02 — ScannerViewModel e fonte UHF sobre o BleManager (18/08/2026)
- REQ-02: a lógica de protocolo sai da `MainActivity` — nasce o `ScannerViewModel`, que controla a fonte de leitura e expõe o estado da tela; a Activity vira só fiação.
- REQ-08: o modo RFID UHF passa a operar como `FonteDeLeitura` (`scan/FonteUhfBle`) sobre o `BleManager` existente, sem reescrevê-lo — `LED_ON`/`LED_OFF` e a remontagem de fragmentos por `__END__` preservados.
- REQ-13: removido `model/BleMessage.kt`, que não tinha nenhuma referência no projeto.
- RNF-08: estrutura de diretórios por responsabilidade consolidada (`domain/`, `scan/`, `ble/`, `ui/`), com a tela em Compose movida para `ui/TelaScanner.kt`.

## INC-01 — Camada de domínio e parser UHF (18/08/2026)
- REQ-01: o aplicativo ganha uma camada de domínio independente de Android — `LeituraPatrimonial`, `OrigemLeitura` (código de barras, NFC, RFID UHF) e o contrato `FonteDeLeitura`, porta única pela qual toda origem de captura entrega leituras.
- REQ-09: o payload do modo RFID UHF passa a ser interpretado por um parser puro tolerante a dois formatos (RN-03): `codigo;descricao` e o formato atual do firmware, sem separador — com o payload bruto sempre preservado.
- Cobertos os casos extremos CE-07, CE-08 e CE-09 por teste unitário (12 testes na suíte, sem exigir aparelho nem emulador).
