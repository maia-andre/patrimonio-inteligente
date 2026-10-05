# Demonstração no notebook Debian

Roteiro para mostrar a leitura de tag rodando, levando o notebook Debian do serviço
(modo texto) e a bancada do ESP32 + R200. Escrito em 01/10/2026, sobre o firmware da
Fase 2 (commit `30d94ff`), o mesmo que entregou o EPC ao aplicativo em 29/09.

Para ler este arquivo no próprio Debian:

```bash
less ~/patrimonio-inteligente/docs/DEMO_DEBIAN.md
```

## 1. Atualizar o repositório

Se o clone de 19/09 ainda existir:

```bash
cd ~/patrimonio-inteligente
git status            # precisa dizer "working tree clean"
git pull
git log --oneline -1  # confere o commit mais recente
```

Se o `git status` mostrar arquivo modificado, **não rode o `pull`**: primeiro descubra
o que foi alterado nessa máquina.

Se a pasta não existir, clone de novo (raso, para poupar o plano de dados):

```bash
sudo apt update
sudo apt install -y git python3-serial
git clone --depth 1 https://github.com/maia-andre/patrimonio-inteligente.git
```

Permissão da porta serial:

```bash
groups | grep dialout
```

Se não aparecer, rode `sudo usermod -aG dialout $USER` e entre de novo na sessão (ou
`newgrp dialout`). Armadilhas do Debian em detalhe: `HARDWARE_R200.md`, seção 2c.

## 2. A demonstração

Não é preciso gravar nada pelo Debian: o ESP32 já está com o firmware da Fase 2, que
aceita os mesmos comandos do aplicativo pela USB.

1. Rosqueie a antena **antes** de energizar. Depois ligue o ESP32 (com o R200 no `J3`)
   na USB do notebook.
2. Confira a porta:
   ```bash
   ls /dev/ttyUSB*
   ```
   Deve aparecer `/dev/ttyUSB0`. Se aparecer e sumir em seguida, é o `brltty`:
   `sudo apt remove brltty`.
3. Abra o terminal serial (vem junto com o `python3-serial`):
   ```bash
   python3 -m serial.tools.miniterm /dev/ttyUSB0 115200 -e
   ```
   Abrir a porta costuma reiniciar o ESP32, e o boot aparece na tela
   (`[UHF] Hardware: M100 26dBm V1.0`, região, potência). **Espere uns 5 s antes de
   digitar.** Se o boot não aparecer, aperte o botão `EN` da placa. O `-e` mostra o que
   você digita, para quem assiste acompanhar.
4. Digite `SCAN_START` e Enter. Aproxime a tag: aparece
   `[TAG] E280...  RSSI -71 dBm  (1 leituras)`.
5. `SCAN_STOP` e Enter para parar. **Ctrl+]** sai do terminal.

## 3. Cuidados

- **O buzzer do R200 apita a cada leitura**, de 20 a 37 vezes por segundo, enquanto a
  tag estiver no alcance. Avise quem estiver na sala, ou mantenha a tag longe até a hora
  de mostrar.
- O celular pode entrar junto: o log aparece no notebook e o EPC no aplicativo. Em
  29/09 o app só conectou depois de ir ao modo Manual e voltar.
- **Plano B:** se o ESP32 estiver com a `ponte_uart` gravada em vez do firmware, o
  `SCAN_START` não faz nada. Nesse caso, use o script do módulo:
  ```bash
  cd ~/patrimonio-inteligente/firmware
  python3 teste_r200.py /dev/ttyUSB0 115200
  ```
- Ensaie uma vez antes do dia.
