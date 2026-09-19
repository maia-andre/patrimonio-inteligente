# Changelog

## Bancada Debian e o micro-USB descartado (19/09/2026)
Dia inteiro de bancada tentando chegar à primeira tag **sem solda**, pelo micro-USB da própria placa. O caminho não funciona neste módulo. Fica registrado com o diagnóstico, porque beco sem saída documentado também poupa a tarde de quem vier depois. Detalhamento em `docs/HARDWARE_R200.md`, seções 2c e 8b.

- **Bancada nova:** notebook Debian 13 em modo texto, do serviço. Reconhece o CH340 nativamente, sem driver — resolve o bloqueio que travava a bancada no Windows sem administrador desde 05/09. Roteiro completo na seção 2c, com as armadilhas que custam uma tarde a quem não as conhece: `python3-serial` pelo apt em vez de `pip` (PEP 668), grupo `dialout` só valendo na sessão seguinte, o `brltty` reivindicando o CH341 e derrubando o `ttyUSB0`, o `dmesg` exigindo privilégio (`kernel.dmesg_restrict=1`) e o `su -c` sem o traço não carregando o `PATH` do root, o que faz o `usermod` "não existir".
- **Conclusão da sessão: o micro-USB do R200 não serve de caminho nesta placa.** O `RX` do CH340 não recebe o `TXD` do módulo. O monitor cru — que não espera boot, não limpa buffer, não exige checksum e não para no primeiro punhado de bytes — viu zero bytes em 15 s a 115200; a 9600 veio um único `F0`, `11110000`, mesma família do `00 80 C0 E0 F0 FC FE` já catalogado, 1,5 s depois do comando. Byte solto que só aparece em baud baixo é assinatura de entrada flutuante, não de resposta.
- Hipóteses levantadas e descartadas, nesta ordem, cada uma com o teste que a derrubou: porta errada (`1A86:7523` confirma o CH340), módulo sem energia (LED verde), `DTR`/`RTS` prendendo reset (as quatro combinações mudas), velocidade errada (silêncio em todas, e velocidade errada dá lixo, não silêncio), comando chegando em cima do boot (espera de 2,5 s não mudou nada), e filtro no próprio script.
- **Dois defeitos reais encontrados nos scripts**, a partir da pergunta "o que garante que não é o script deixando passar?": `abrir_porta` descartava com `reset_input_buffer()` tudo que chegasse durante a espera de boot, e `ser.read(ser.in_waiting or 1)` truncava resposta que viesse em fluxo. Os dois teriam contaminado qualquer medição futura, inclusive pela ponte do ESP32. Novo `ler_ate_silencio()` acumula até a linha calar; o que chega durante o boot passa a ser relatado, não apagado.
- `firmware/teste_r200.py` vira kit de diagnóstico: `--varrer` (velocidade da UART, com previsão e tempo real de execução), `--diagnostico` (identidade da porta, escuta passiva, matriz de `DTR`/`RTS`), `--cego` (inventário sem ler resposta, para ouvir o buzzer quando só a volta está muda), `--eco` (loopback no `J3`) e `--cru` (monitor sem decisão nenhuma). Também aceita a velocidade por argumento, em vez do 115200 fixo no código.
- **Correção de pinagem:** a fileira `J2` tem **seis** furos, não cinco — `G` · `R` · `C` · `D` · `5V` · ■ de cima para baixo. O `5V` é o quinto, e **o furo quadrado não é ele**. Como a convenção usual é o quadrado marcar o pino 1, era erro esperando para acontecer, com risco de dano ao módulo.
- README: o micro-USB deixa de ser omissão e vira beco sem saída documentado, na linha de estado e no aviso de pinos do guia de reprodução; o gargalo do modo UHF passa a ser nomeado pelo que é hoje — um serviço de solda de poucos minutos, não compra nem tempo de bancada.
- Pendente, como antes: a velocidade da UART e a primeira tag. As duas agora dependem do `J3` com contato firme.

## Inscrição no BBSIA e pasta de lastro (13/09/2026)
Frente institucional, fora do ciclo de spec: o projeto passa a registrar no próprio repositório o que troca com o mundo.

- `docs/bbsia-inscricao.md`: reconstrução das respostas ao formulário do **BBSIA** — Banco Brasileiro de Soluções de IA para a Gestão Pública (LIIA/ENAP). O cadastro original foi feito em agosto de 2026 e o rascunho se perdeu por não estar versionado; este arquivo passa a ser a fonte de qualquer atualização. Dados de contato pessoal ficam fora de propósito.
- `docs/lastro/`: registro das interações externas que o projeto recebe — contatos de outros órgãos, pesquisadores, fornecedores e contribuidores, relatos de uso e de fracasso, comparações de hardware. É a evidência de que a dor descrita no README é compartilhada, e o material que a próxima conversa reaproveita.
- Formato fixo por interação: pasta `AAAA-MM-<origem>/` com um `registro.md` (quem, quando, demanda, o que enviamos, autorização, o que voltou). O que sai do próprio repositório não é copiado — o registro aponta a tag ou o commit exato, que é o lastro do que a pessoa de fato recebeu.
- Regra de privacidade explícita, porque o diretório é público: nome, instituição e função só depois de autorização expressa; telefone, e-mail e mensagens originais nunca entram, ficando em `docs/interno/`, que o git ignora.
- Primeira interação registrada: **Prefeitura do Campus da UnB**, mestrado PPGGIPP/FACE — contato acadêmico e praticante de patrimônio, respondido, com autorização de registro nominal pendente. Deck para gestores e TCC anexados à pasta. Tag `lastro/2026-09-unb`.

## Bancada do módulo UHF R200 (05/09/2026)
Fora do ciclo de spec: o módulo leitor YPD-R200 (Impinj E710) chegou e foi ligado ao ESP32 pela primeira vez. Registro completo em `docs/HARDWARE_R200.md`.
- `docs/HARDWARE_R200.md`: documento de bancada alinhado ao repositório — pinagem real da placa (`J3` em 3,3 V, furo `5V` separado), o que o firmware da Fase 2 precisa entregar (payload `EPC;` na RN-03, `SCAN_START`/`SCAN_STOP` com sinônimos, janela de silêncio por EPC), saída para Windows sem driver do CH340, diagnóstico dos sintomas vistos e registro do dia.
- `firmware/teste_r200.py`: parser resiliente a `0xBB` dentro do EPC e a tamanho impossível; espera o reset do ESP32 ao abrir a porta.
- `firmware/ponte_uart/`: ESP32 como conversor USB-serial para o R200 (validado em loop).
- `firmware/diag_r200/`: ESP32 testa a UART do R200 sozinho, varrendo 9600–230400, com o nível de repouso do `RX2` sem o pull-up interno.
- README e CONTRIBUTING: o modo UHF deixa de estar "bloqueado por uma compra"; módulo trocado de YRM100 para YPD-R200 na lista de materiais, no diagrama e no roadmap; estado real ganha a linha "Módulo leitor UHF na bancada".
- Provado: módulo vivo e alimentado pelo `VIN` do ESP32, fiação da Fase 2 correta, módulo reage a comandos. Pendente: solda dos pinos (contato sem solda deforma a UART), velocidade da serial, primeira tag.

## Redesenho da tela, lançamento manual e ícone (03/09/2026)
Feito fora do ciclo de spec, por decisão do usuário, depois do primeiro smoke test em aparelho real (Galaxy A34, Android 16): a tela do INC-04 era uma colagem do seletor de modos sobre a tela antiga do BLE. Registro em `docs/interno/smoke-test-2026-09-03.md`.
- Tela com um só sistema de controle: o seletor de modos manda e o painel abaixo mostra o que o modo selecionado precisa — prévia da câmera com botão de fechar, instrução de espera do NFC, conexão e comandos do scanner BLE só dentro do modo UHF, campo do lançamento manual. A lista da sessão é o conteúdo principal, com a última leitura em destaque; o registro técnico fica recolhido.
- Motivo de indisponibilidade com a ação que o resolve: "Conectar" ao lado de "Scanner BLE não conectado", no mesmo padrão de "Permitir câmera" (CE-03).
- Lançamento manual: nova origem `MANUAL`, `interpretarCodigoManual` (função pura, RNF-02) e `FonteManual`; entra na mesma lista, com a mesma deduplicação (RN-01).
- `capturando` no estado do `ScannerViewModel`: "Fechar câmera" e "Parar" param a fonte sem trocar de modo; "Abrir câmera" e "Retomar" reiniciam.
- Tema próprio (`RfidpocTheme`): paleta azul-placa em claro e escuro, sem cor dinâmica; cores de estado separadas da cor de destaque; escala tipográfica definida. Fim das cores fixas em hexadecimal na tela. Conteúdo respeita as barras do sistema (edge-to-edge).
- Ícone adaptativo novo (placa patrimonial com barras e ondas de rádio) e rótulo do launcher "Patrimônio".
- Suíte: 95 testes (9 novos — interpretador e fonte manual, quatro modos no seletor, `capturando`).

## INC-07 — Documentação: README e CONTRIBUTING (19/08/2026)
- REQ-14: README atualizado — nova seção "Os três modos de captura" (tabela de perfis + advertência NFC ≠ UHF), duas linhas funcionais no "Estado real", gargalo delimitado ao modo UHF, fecho sem o pré-requisito de R$ 1.200, caminho "Comece sem hardware nenhum", diagrama com as três origens, débito técnico registrando o payload `codigo;descricao` já aceito no app, roadmap e sumário atualizados.
- REQ-15: CONTRIBUTING delimita o bloqueio de R$ 1.200 ao modo UHF, preservando o pedido de acesso a leitor como prioritário.
- RNF-06: requisito mínimo corrigido para Android 9 (API 28), em acordo com o `build.gradle.kts`; menção de permissões Bluetooth "Android 12+" preservada por estar correta.
- Fecha o backlog da spec de modos de captura: 7/7 incrementos concluídos.

## INC-06 — Modo NFC (reader mode) (19/08/2026)
- REQ-07: o modo NFC captura etiquetas pela antena do celular em primeiro plano (reader mode), aceitando NfcA, NfcB, NfcF e NfcV.
- RN-02: o código vem do registro NDEF de texto da etiqueta; sem NDEF ou com texto vazio (CE-05/CE-06), vale o UID em hexadecimal maiúsculo — ex.: `04A224B25C6180`.
- CE-02/CE-04: aparelho sem NFC e NFC desligado mostram o modo desabilitado com motivos distintos ("Aparelho sem NFC" ≠ "NFC desligado"); ligar o NFC nas configurações e voltar reabilita o modo.
- Deduplicação entre origens (RN-01): a mesma chave lida por NFC e por código de barras continua sendo um só item na lista.

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
