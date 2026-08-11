# Spec: Modos de captura — código de barras, NFC e RFID UHF

Versão: 1 — 10/08/2026 | Status: aprovada

## Objetivo

O aplicativo hoje sabe capturar leitura patrimonial de uma única origem: o ESP32 com módulo RFID UHF, via BLE. Como o módulo leitor ainda não foi adquirido, **nenhum colaborador consegue rodar o aplicativo de ponta a ponta sem antes montar uma bancada de hardware** — o que é a maior barreira de adoção de um projeto publicado como solução compartilhável.

Esta entrega acrescenta dois modos de captura que **não exigem hardware nenhum além do próprio celular**: leitura de código de barras pela câmera e leitura de etiqueta NFC pela antena do aparelho. Para sustentar os três modos sem duplicar lógica, a captura passa a viver atrás de uma porta de domínio única, e a lógica de protocolo sai da `MainActivity`.

O resultado esperado é que qualquer servidor público com um Android na mão consiga conferir bens hoje, e que o módulo UHF deixe de ser pré-requisito do projeto para ser pré-requisito de apenas um dos três modos.

### Advertência técnica que esta spec preserva

**NFC não é RFID UHF e não o substitui.** São faixas e normas distintas, com perfis operacionais incomparáveis:

| Modo | Faixa / norma | Hardware | Alcance | Itens por leitura |
|---|---|---|---|---|
| Código de barras | óptico | câmera do celular | visada direta | 1 |
| NFC | 13,56 MHz · ISO 14443 / 15693 | antena do celular | ~4 cm | 1 |
| RFID UHF | 860–960 MHz · EPC Gen2 / ISO 18000-6C | ESP32 + módulo leitor, via BLE | até 6 m | dezenas por segundo |

O celular **não lê etiqueta UHF**. Os três modos são complementares: barcode e NFC atendem à conferência item a item do acervo legado; o UHF é o único caminho para inventário em massa e continua sendo o objetivo final do projeto. Nenhum texto desta entrega — código, comentário ou documentação — pode sugerir que os modos novos resolvem o problema que o UHF resolve.

## Escopo

### Requisitos funcionais

- **REQ-01** — Existe uma camada de domínio independente de Android com: `LeituraPatrimonial(codigo: String?, descricao: String?, origem: OrigemLeitura, bruto: String, instante: Long)`, o enum `OrigemLeitura(CODIGO_BARRAS, NFC, RFID_UHF)` e a interface `FonteDeLeitura { fun iniciar(); fun parar(); val leituras: Flow<LeituraPatrimonial> }`.
- **REQ-02** — Existe um `ScannerViewModel` que seleciona a fonte ativa, acumula as leituras e expõe o estado da tela. A `MainActivity` não contém lógica de protocolo, parsing, deduplicação nem controle de fonte.
- **REQ-03** — A tela exibe um seletor com os três modos. Exatamente um está ativo por vez.
- **REQ-04** — As leituras se acumulam em uma lista em memória, exibida com contador de itens conferidos na sessão. Cada linha mostra o código, a origem da leitura e o horário.
- **REQ-05** — Uma leitura cuja chave já consta na lista não gera nova linha. O aplicativo sinaliza ao usuário que o item já havia sido conferido.
- **REQ-06** — O modo código de barras captura pela câmera usando CameraX com decodificação ZXing, aceitando exclusivamente Code 128, Code 39 e QR Code.
- **REQ-07** — O modo NFC captura por `NfcAdapter.enableReaderMode` em primeiro plano, aceitando as tecnologias NfcA, NfcB, NfcF e NfcV.
- **REQ-08** — O modo RFID UHF reaproveita o `BleManager` existente sem reescrevê-lo, preservando o envio de `LED_ON`/`LED_OFF` e a remontagem de fragmentos pelo marcador `__END__`.
- **REQ-09** — O payload do modo RFID UHF é interpretado por um parser tolerante a dois formatos, conforme **RN-03**.
- **REQ-10** — Ao trocar de modo, a fonte anterior é parada antes de a nova ser iniciada, liberando câmera, antena NFC ou conexão BLE conforme o caso.
- **REQ-11** — Um modo indisponível aparece desabilitado no seletor, acompanhado do motivo em texto legível (aparelho sem NFC, NFC desligado, permissão de câmera negada, scanner BLE não conectado).
- **REQ-12** — A permissão de câmera é solicitada em tempo de execução, no momento em que o usuário seleciona o modo código de barras — não na abertura do aplicativo.
- **REQ-13** — O arquivo `model/BleMessage.kt`, que hoje não tem nenhuma referência no projeto, é removido.
- **REQ-14** — O `README.md` é atualizado nos seguintes pontos, todos verificáveis linha a linha:
  - Tabela dos três modos com seus perfis operacionais (faixa, hardware, alcance, itens por leitura) e a advertência de que NFC não substitui UHF.
  - Duas linhas novas na tabela "Estado real do projeto": leitura por código de barras e leitura por NFC, ambas como funcionais.
  - O callout "O gargalo, dito com todas as letras" deixa de afirmar que **o projeto** está travado e passa a delimitar o bloqueio ao **modo UHF**.
  - O fecho do documento deixa de afirmar que modernizar o patrimônio começa com R$ 1.200, já que dois dos três modos passam a custar zero.
  - A seção "Reproduza no seu município" ganha o caminho sem hardware nenhum, hoje ausente: toda a lista de materiais pressupõe ESP32 e YRM100.
  - O diagrama de "Como funciona" passa a representar as três origens de captura, não apenas a rota UHF.
  - O callout de débito técnico da seção "Protocolo BLE" registra que o lado do aplicativo já aceita o payload estruturado definido em **RN-03**, restando adotá-lo no firmware.
  - O roadmap de curto prazo reflete o que esta entrega conclui e o que permanece pendente.
  - O sumário incorpora qualquer seção nova criada acima.
- **REQ-15** — O `CONTRIBUTING.md` é atualizado: a afirmação de que **o projeto** está bloqueado por uma compra de R$ 1.200 (seção "Acesso a um leitor RFID UHF") passa a delimitar o bloqueio ao modo UHF, preservando o pedido de acesso a leitor, que segue válido e prioritário.

### Requisitos não-funcionais

- **RNF-01** — Código, comentários, mensagens de commit e documentação em português, conforme `CONTRIBUTING.md`.
- **RNF-02** — A interpretação de payload de cada modo (texto do UHF, NDEF/UID do NFC, texto do código de barras) é implementada em funções puras, sem dependência de API Android, de modo a ser coberta por teste unitário sem aparelho nem emulador.
- **RNF-03** — Nenhum dado patrimonial, pessoal ou de localização real em código, teste, comentário ou documentação. Somente dado fictício.
- **RNF-04** — O aplicativo permanece instalável em aparelho sem NFC e sem câmera: ambos declarados no manifesto com `android:required="false"`.
- **RNF-05** — Nenhuma dependência de Google Play Services. A decodificação de código de barras usa `com.google.zxing:core` (Apache 2.0), coerente com a licença do projeto.
- **RNF-06** — `minSdk` permanece 28. O README é corrigido onde diverge do `build.gradle.kts`: a linha "Android mínimo | 12 (API 31)" da tabela de ambiente de referência e as duas menções a "Android 12+" como requisito de execução. A menção a "Android 12+" no contexto das permissões de Bluetooth permanece, por estar correta — `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT` só existem a partir da API 31.
- **RNF-07** — Todo o processamento ocorre no aparelho. Nenhuma leitura é transmitida a serviço externo.
- **RNF-08** — A UI segue em Jetpack Compose. A estrutura de diretórios segue por responsabilidade: `domain/`, `scan/`, `ble/`, `ui/`.

## Regras de negócio

- **RN-01** — A chave de deduplicação de uma leitura é `codigo ?: bruto`. Leituras de origens diferentes com a mesma chave são consideradas o mesmo bem e não se duplicam.
- **RN-02** — No modo NFC, o código é extraído com a seguinte precedência: (1) se a etiqueta tiver mensagem NDEF com registro de texto não vazio, o conteúdo desse registro é o `codigo`; (2) caso contrário, o `codigo` é o UID da etiqueta em hexadecimal maiúsculo, sem separadores.
- **RN-03** — No modo RFID UHF, o payload remontado é interpretado assim: se contiver o separador `;`, o trecho anterior ao primeiro `;` é o `codigo` e o restante é a `descricao`; se não contiver, `codigo` é nulo e o payload inteiro vira `descricao`. O campo `bruto` guarda sempre o payload original, íntegro, nos dois casos.
- **RN-04** — No modo código de barras, apenas Code 128, Code 39 e QR Code são decodificados. Demais simbologias — notadamente EAN e UPC, de varejo — são ignoradas, para que o aplicativo não capture o código da embalagem em vez do código do bem.
- **RN-05** — Apenas uma `FonteDeLeitura` fica ativa por vez. Iniciar uma fonte implica parar a anterior.
- **RN-06** — A lista de leituras é exibida em ordem cronológica inversa: a mais recente no topo.
- **RN-07** — A lista vive apenas em memória e é perdida ao encerrar o aplicativo. Persistência é item de outro incremento.

## Casos extremos

- **CE-01** (afeta REQ-05, RN-01) — Mesmo código lido repetidamente: a lista não ganha linha nova, o contador não incrementa e o usuário recebe sinalização de "já conferido". No modo código de barras, em que a câmera decodifica continuamente, a sinalização é limitada a uma ocorrência por segundo para o mesmo código, evitando repetição incessante.
- **CE-02** (afeta REQ-11) — Aparelho sem hardware NFC: o modo NFC aparece desabilitado com o motivo declarado, e o aplicativo funciona normalmente nos demais modos.
- **CE-03** (afeta REQ-11, REQ-12) — Permissão de câmera negada: o modo código de barras fica desabilitado com o motivo declarado e um caminho para reabrir a solicitação. Negar a permissão não derruba nem trava o aplicativo.
- **CE-04** (afeta REQ-11) — Aparelho tem NFC mas está desligado nas configurações: o motivo exibido distingue esse caso do caso "sem hardware", já que a ação do usuário é diferente.
- **CE-05** (afeta RN-02) — Etiqueta NFC sem mensagem NDEF: usa o UID, conforme a precedência.
- **CE-06** (afeta RN-02) — Etiqueta NFC com NDEF presente mas sem registro de texto, ou com registro de texto vazio: trata como ausência de NDEF e cai para o UID.
- **CE-07** (afeta RN-03) — Payload UHF sem `;` — que é o formato atual do firmware: `codigo` nulo, payload inteiro em `descricao`. A leitura entra na lista normalmente, deduplicada pelo `bruto`.
- **CE-08** (afeta RN-03) — Payload UHF com `;` mas com descrição vazia (`"147258;"`): `codigo` recebe `147258` e `descricao` fica nula.
- **CE-09** (afeta RN-03) — Payload UHF com mais de um `;`: apenas o primeiro separa; os demais permanecem no texto da descrição.
- **CE-10** (afeta REQ-10) — Troca de modo com leitura em andamento: a fonte anterior é parada e seus recursos liberados. Leituras que chegarem da fonte parada após a troca são descartadas.
- **CE-11** (afeta REQ-08, REQ-11) — Conexão BLE cai durante o uso do modo UHF: o modo passa a indisponível com o motivo declarado, e a lista já acumulada é preservada.
- **CE-12** (afeta REQ-06) — Código de barras ilegível, parcial ou fora de foco: nada é adicionado à lista e a câmera segue tentando, sem mensagem de erro a cada quadro.
- **CE-13** (afeta REQ-10) — Aplicativo vai para segundo plano durante a captura: a fonte ativa é parada, liberando câmera e antena NFC; ao voltar, o modo selecionado é retomado.
- **CE-14** (afeta REQ-04) — Nenhuma leitura ainda realizada: a tela exibe estado vazio explicativo, não uma lista em branco.

## Fora de escopo

Não fazem parte desta entrega, e permanecem como itens de incrementos seguintes:

- Qualquer alteração no firmware do ESP32, inclusive fazê-lo emitir o formato `codigo;descricao` definido em **RN-03**. O aplicativo passa a aceitar esse formato; o firmware o adota depois.
- Renomear os comandos BLE `LED_ON`/`LED_OFF` para `SCAN_START`/`SCAN_STOP`.
- Persistência local das leituras com Room.
- Gravação de etiquetas NFC — esta entrega apenas lê.
- Integração com sistema de patrimônio, tabela de-para entre UID e placa patrimonial, e qualquer chamada de rede.
- OCR de placa patrimonial e toda a camada de IA descrita no README.
- Leitura de múltiplas etiquetas UHF simultâneas e anticolisão.
- Migração das APIs BLE depreciadas (`getStringValue`, `characteristic.value`).

## Definição de concluído

- [ ] Existe `domain/` com `LeituraPatrimonial`, `OrigemLeitura` e `FonteDeLeitura`, sem nenhum import de `android.*`.
- [ ] `MainActivity.kt` não contém parsing de payload, deduplicação, buffer de fragmentos nem controle direto de fonte.
- [ ] Dado o payload `"147258;Notebook Positivo"`, o parser UHF produz `codigo = "147258"` e `descricao = "Notebook Positivo"`.
- [ ] Dado um payload sem `;`, o parser UHF produz `codigo = null` e `descricao` igual ao payload, com `bruto` preservado.
- [ ] Dada uma etiqueta NFC com registro NDEF de texto `"147258"`, o extrator produz `codigo = "147258"`.
- [ ] Dada uma etiqueta NFC sem NDEF e com UID `04 A2 24 B2 5C 61 80`, o extrator produz `codigo = "04A224B25C6180"`.
- [ ] Dadas duas leituras com a mesma chave, a lista contém uma linha e o contador marca 1.
- [ ] Dadas duas leituras de origens diferentes com códigos distintos, a lista contém duas linhas, cada uma exibindo sua origem.
- [ ] Ao trocar de modo, a fonte anterior recebe `parar()` antes de a nova receber `iniciar()`, verificável com fontes falsas em teste.
- [ ] Leitura emitida por uma fonte já parada não entra na lista.
- [ ] Em aparelho sem NFC, o seletor exibe o modo NFC desabilitado com o motivo, e os demais modos operam.
- [ ] Com a permissão de câmera negada, o modo código de barras fica desabilitado com o motivo e o aplicativo não trava.
- [ ] O `AndroidManifest.xml` declara NFC e câmera com `android:required="false"`.
- [ ] `model/BleMessage.kt` não existe mais no repositório.
- [ ] Nenhuma dependência de Google Play Services no grafo do projeto.
- [ ] O README contém a tabela dos três modos, a advertência de que NFC não substitui UHF, as linhas novas em "Estado real do projeto", o caminho de teste sem hardware, e o diagrama com as três origens de captura.
- [ ] Nenhuma busca por "R$ 1.200" no README ou no CONTRIBUTING retorna uma frase que afirme que **o projeto** está bloqueado ou travado — apenas o modo UHF.
- [ ] O README declara `minSdk` 28 na tabela de ambiente de referência, em acordo com o `build.gradle.kts`.
- [ ] Nenhum dado patrimonial, pessoal ou de localização real em código, teste ou documentação.
- [ ] Todos os testes automatizados passam.

## Verificação que exige aparelho

Os itens abaixo não são cobertos por teste automatizado e ficam para o `/verify`, em aparelho físico:

- Decodificação real de Code 128, Code 39 e QR pela câmera, e a não decodificação de um EAN-13 de varejo.
- Leitura real de etiqueta NFC, nos dois caminhos da **RN-02**.
- Modo UHF de ponta a ponta contra o ESP32 com a simulação atual.

## Decisões técnicas registradas

Ficam anotadas com a justificativa porque condicionam a implementação e serão lidas por quem não participou da entrevista.

- **DT-01 — Decodificação com `com.google.zxing:core` e analisador CameraX próprio, não com `zxing-android-embedded`.** O pacote *embedded* é mais rápido de plugar, mas traz Activity e View em XML, e o `CONTRIBUTING.md` estabelece que o XML foi removido do projeto e que a interface é Jetpack Compose. Custa mais código e preserva a coerência da stack. Ambos são Apache 2.0. Confirmado em 10/08/2026.
- **DT-02 — `LeituraPatrimonial.codigo` é anulável.** O firmware atual não emite código estruturado (ver **CE-07**), e um valor-sentinela do tipo `"SEM-CODIGO"` faria o modelo afirmar algo falso sobre o dado. Nulo representa a ausência com honestidade, e a **RN-01** já define a chave de deduplicação para esse caso. Confirmado em 10/08/2026.
- **DT-03 — Biblioteca de código de barras: ZXing, com ML Kit descartado.** ZXing é Apache 2.0, igual ao projeto, e não exige Google Play Services — condição para rodar em aparelho de órgão público sem o ecossistema Google. Ver **RNF-05**.

## Perguntas em aberto

Nenhuma.
