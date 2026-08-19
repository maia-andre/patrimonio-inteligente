# Build report — 18/08/2026 (INC-04)
Spec: docs/spec.md, versão 1 (10/08/2026, aprovada)
Incremento: INC-04 — Seletor de modos e ciclo de vida das fontes
Rodada: construção
Testes: 50 passando / 50 total — `sh gradlew test` (unit; instrumentada exige aparelho)

## Requisitos atendidos
- **REQ-03** — Atendido — `SeletorDeModos` na `TelaScanner` exibe os três modos (um botão por `OrigemLeitura`); exatamente um está ativo (`EstadoTelaScanner.modoSelecionado`, destacado). Troca coberta por `trocar de modo para a fonte anterior antes de iniciar a nova`.
- **REQ-10** — Atendido — `selecionarModo` cancela a coleta e chama `parar()` na fonte anterior **antes** de assinar e chamar `iniciar()` na nova; ordem provada com fontes falsas que registram trilha de eventos (`["uhf.parar", "barras.iniciar"]`), exatamente como pede a definição de concluído.
- **REQ-11** — Atendido — `ModoDaTela(origem, disponivel, motivo)`; indisponível aparece desabilitado com motivo legível sob o botão. Motivos nesta rodada: "Scanner BLE não conectado" (UHF sem conexão) e "Ainda não disponível nesta versão" (barcode/NFC sem fonte — os motivos específicos de câmera e NFC chegam nos INC-05/06, conforme o plano).
- **RNF-04** — Atendido — `AndroidManifest.xml` declara `android.hardware.nfc` e `android.hardware.camera` com `android:required="false"`.
- **RN-05** — Atendido — uma única `fonteAtiva` no ViewModel; iniciar nova implica parar a anterior (mesma prova do REQ-10).

## Casos extremos cobertos
- **CE-10** — `leitura de fonte parada apos a troca e descartada`: após a troca, emissão da fonte antiga não entra na lista (coleta cancelada); emissão da nova entra.
- **CE-11** — `queda do ble torna o modo uhf indisponivel e preserva a lista`: desconexão muda a disponibilidade com o motivo certo e a lista acumulada permanece intacta.
- **CE-13** — `segundo plano para a captura em andamento e voltar retoma` + `segundo plano sem captura em andamento nao toca a fonte`: `onStop`/`onStart` da MainActivity chamam `aoEntrarEmSegundoPlano`/`aoVoltarAoPrimeiroPlano`; a retomada só ocorre se havia captura em andamento.

## Perguntas em aberto / pendências
- Selecionar um modo disponível **inicia** a captura da nova fonte (REQ-10 fala em "a nova ser iniciada"); os botões Escanear/Desligar seguem como controle manual dentro do modo UHF.
- Selecionar modo indisponível é ignorado no ViewModel (defesa em profundidade — o botão já vem desabilitado na UI).
- Modos sem fonte registrada (barcode/NFC até os INC-05/06) usam o motivo interino "Ainda não disponível nesta versão" — não é um dos motivos do REQ-11, que pressupõem os modos implementados; os definitivos entram com cada modo.
