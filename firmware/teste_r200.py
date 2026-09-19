#!/usr/bin/env python3
"""
teste_r200.py — Teste de bancada do modulo UHF Invelion YPD-R200 (Impinj E710)

Roda no PC (notebook), conectado ao modulo pelo micro-USB da placa
(precisa do driver CH340) ou por um ESP32 gravado com ponte_uart/ponte_uart.ino
(usa o driver embutido do Windows; ver docs/HARDWARE_R200.md).
NAO e gravado no modulo. O R200 ja tem firmware de fabrica.

Uso:
    pip install pyserial
    python teste_r200.py                 # lista as portas disponiveis
    python teste_r200.py COM5            # Windows
    python teste_r200.py /dev/ttyUSB0 --varrer   # descobre a velocidade da UART
    python teste_r200.py /dev/ttyUSB0 9600       # forca uma velocidade
    python teste_r200.py /dev/ttyUSB0 --diagnostico  # porta abre mas nada volta
    python teste_r200.py /dev/ttyUSB0 --cego         # nada volta: ouvir o buzzer
    python teste_r200.py /dev/ttyUSB0 --eco          # loopback no J3
    python teste_r200.py /dev/ttyUSB0    # Linux

ATENCAO: conecte a antena ANTES de energizar o modulo.
Transmitir sem carga na saida RF pode danificar o amplificador.
"""

import sys
import time

try:
    import serial
    import serial.tools.list_ports
except ImportError:
    sys.exit("Falta a biblioteca pyserial. Rode:  pip install pyserial")

BAUD_PADRAO = 115200
BAUDS_CANDIDATOS = [9600, 19200, 38400, 57600, 115200, 230400]
ESPERA_BOOT = 2.5  # s -- abrir a porta reinicia o modulo; ver abrir_porta()

# ---------------------------------------------------------------- protocolo
# Estrutura do frame MagicRF:
#   BB | Type | Cmd | LenMSB | LenLSB | Params... | Checksum | 7E
# Checksum = soma de (Type ... ultimo param), byte baixo.

FRAME_HEAD = 0xBB
FRAME_END = 0x7E


def montar_frame(tipo: int, cmd: int, params: bytes = b"") -> bytes:
    """Monta um frame completo com checksum calculado."""
    corpo = bytes([tipo, cmd, (len(params) >> 8) & 0xFF, len(params) & 0xFF]) + params
    checksum = sum(corpo) & 0xFF
    return bytes([FRAME_HEAD]) + corpo + bytes([checksum, FRAME_END])


# Comandos usados aqui
CMD_INFO = lambda tipo_info: montar_frame(0x00, 0x03, bytes([tipo_info]))
CMD_SET_REGIAO = lambda regiao: montar_frame(0x00, 0x07, bytes([regiao]))
CMD_SET_POTENCIA = lambda centesimos_dbm: montar_frame(
    0x00, 0xB6, bytes([(centesimos_dbm >> 8) & 0xFF, centesimos_dbm & 0xFF])
)
CMD_INVENTARIO_UNICO = montar_frame(0x00, 0x22)
CMD_INVENTARIO_MULTI = lambda n: montar_frame(
    0x00, 0x27, bytes([0x22, (n >> 8) & 0xFF, n & 0xFF])
)
CMD_PARAR = montar_frame(0x00, 0x28)

REGIOES = {"china900": 0x01, "us": 0x02, "eu": 0x03, "china800": 0x04}


# ---------------------------------------------------------------- porta
def abrir_porta(porta: str, baud: int, timeout: float = 0.2,
                dtr: bool = False, rts: bool = False,
                espera_boot: float = ESPERA_BOOT):
    """Abre a serial com DTR/RTS no estado pedido, ja a partir da abertura.

    Por padrao o pyserial levanta as duas linhas ao abrir. Em placas que usam
    DTR ou RTS como reset -- o caso do R200 em 19/09/2026, que bipava de boot
    justamente quando o script fechava a porta e soltava as linhas -- isso
    mantem o modulo parado enquanto a porta estiver aberta, e o sintoma e
    silencio em todas as velocidades.

    Definir os estados ANTES de open() evita o pulso da abertura; soltar as
    duas e inofensivo quando elas nao estao ligadas a nada.

    E espera o boot. Em 19/09/2026 ficou provado que abrir a porta reinicia o
    modulo: o buzzer deu bipe de boot sempre no mesmo instante depois da
    abertura, identico em todas as velocidades -- se fosse leitura de tag
    dependeria do baud certo e nao se repetiria igual. Comando enviado antes
    disso chega durante o boot e se perde, que foi a causa real do
    "(sem resposta)" em toda a varredura daquele dia.
    """
    ser = serial.Serial()
    ser.port = porta
    ser.baudrate = baud
    ser.timeout = timeout
    ser.dtr = dtr
    ser.rts = rts
    ser.open()
    if espera_boot > 0:
        time.sleep(espera_boot)
        ser.reset_input_buffer()
    return ser


# ---------------------------------------------------------------- parser
# Maior frame que o modulo emite: notificacao de tag com EPC de ate 62 bytes
# (RSSI 1 + PC 2 + EPC 62 + CRC 2 = 67 params). Acima disso e cabecalho falso.
MAX_PARAMS = 128
FRAME_MINIMO = 7  # BB Type Cmd LenMSB LenLSB Checksum 7E


def extrair_frames(buffer: bytearray):
    """Consome o buffer e devolve os frames completos e validos encontrados.

    Um 0xBB pode aparecer dentro do EPC ou do RSSI. Por isso nada e descartado
    antes de validar o frame inteiro: se o candidato falha no tamanho, no 0x7E
    ou no checksum, descarta-se so o primeiro byte e procura-se o proximo 0xBB.
    """
    frames = []
    while True:
        try:
            inicio = buffer.index(FRAME_HEAD)
        except ValueError:
            buffer.clear()
            break

        if inicio > 0:
            del buffer[:inicio]

        if len(buffer) < FRAME_MINIMO:
            break  # frame ainda chegando

        tam_params = (buffer[3] << 8) | buffer[4]
        if tam_params > MAX_PARAMS:
            del buffer[0]  # cabecalho falso: tamanho impossivel
            continue

        tam_total = FRAME_MINIMO + tam_params
        if len(buffer) < tam_total:
            break  # frame ainda chegando

        candidato = bytes(buffer[:tam_total])
        corpo = candidato[1:-2]
        valido = candidato[-1] == FRAME_END and (sum(corpo) & 0xFF) == candidato[-2]

        if not valido:
            if candidato[-1] == FRAME_END:
                print("  [aviso] checksum invalido, frame descartado")
            del buffer[0]  # descarta so o cabecalho falso, preserva o resto
            continue

        del buffer[:tam_total]
        frames.append(candidato)
    return frames


def interpretar_tag(frame: bytes):
    """Extrai RSSI e EPC de uma notificacao de tag (cmd 0x22)."""
    params = frame[5:-2]
    if len(params) < 5:
        return None

    rssi_bruto = params[0]
    rssi = rssi_bruto - 256 if rssi_bruto > 127 else rssi_bruto  # byte com sinal
    epc = params[3:-2]  # pula PC (2 bytes), remove CRC (2 bytes)
    return epc.hex().upper(), rssi


# ---------------------------------------------------------------- helpers
def enviar(ser, frame: bytes, rotulo: str, espera: float = 0.3):
    print(f"\n>>> {rotulo}")
    print(f"    TX: {frame.hex(' ').upper()}")
    ser.reset_input_buffer()
    ser.write(frame)
    time.sleep(espera)
    resposta = ser.read(ser.in_waiting or 1)
    if resposta:
        print(f"    RX: {resposta.hex(' ').upper()}")
    else:
        print("    RX: (sem resposta)")
    return resposta


def listar_portas():
    portas = serial.tools.list_ports.comports()
    if not portas:
        print("Nenhuma porta serial encontrada. O modulo esta plugado?")
        return
    print("Portas disponiveis:")
    for p in portas:
        print(f"  {p.device:<20} {p.description}")
    print("\nRode de novo passando a porta, ex:  python teste_r200.py " + portas[0].device)


# ---------------------------------------------------------------- varredura
def varrer_baud(porta: str):
    """Descobre em que velocidade a UART do modulo fala.

    Manda a versao de hardware em cada candidata e verifica se volta um frame
    MagicRF valido. Velocidade errada devolve lixo que nao fecha checksum; a
    certa devolve algo como  BB 01 03 ... 7E.
    """
    print(f"Varrendo velocidades em {porta}...\n")
    print(f"  espera de boot: {ESPERA_BOOT} s por velocidade")
    print(f"  candidatas: {BAUDS_CANDIDATOS}")
    print(f"  previsao: cerca de {len(BAUDS_CANDIDATOS) * (ESPERA_BOOT + 1.0):.0f} s")
    print()

    achadas = []
    comecou = time.time()

    for baud in BAUDS_CANDIDATOS:
        try:
            with abrir_porta(porta, baud) as ser:
                # Se uma sessao anterior morreu no meio de um inventario
                # continuo, o modulo ainda esta despejando notificacao de tag e
                # a resposta da versao se perde no meio. Parar antes de perguntar.
                ser.write(CMD_PARAR)
                time.sleep(0.3)
                ser.reset_input_buffer()

                ser.write(CMD_INFO(0x00))
                time.sleep(0.6)
                resposta = ser.read(ser.in_waiting or 1)
        except serial.SerialException as erro:
            print(f"  {baud:>6} baud : erro ao abrir a porta ({erro})")
            continue

        if not resposta:
            print(f"  {baud:>6} baud : (sem resposta)")
            continue

        frames = extrair_frames(bytearray(resposta))
        if frames:
            print(f"  {baud:>6} baud : FRAME VALIDO   {frames[0].hex(' ').upper()}")
            achadas.append(baud)
        else:
            amostra = resposta[:16].hex(" ").upper()
            print(f"  {baud:>6} baud : {len(resposta)} bytes sem frame valido   {amostra}")

    print()
    print(f"(varredura levou {time.time() - comecou:.1f} s)")
    print()
    if len(achadas) == 1:
        baud = achadas[0]
        print(f"Velocidade do modulo: {baud} baud.")
        print(f"Anote na secao 4 do docs/HARDWARE_R200.md e rode:")
        print(f"    python3 teste_r200.py {porta} {baud}")
    elif not achadas:
        print("Nenhuma velocidade devolveu frame valido.")
        print()
        print("Se todas devolveram bytes deformados parecidos (00 80 C0 E0 F0 FC FE),")
        print("o problema e contato eletrico, nao velocidade. Pelo micro-USB da placa")
        print("isso nao deveria acontecer: confira a alimentacao e troque o cabo")
        print("(cabo so de carga nao tem as linhas de dados).")
    else:
        print(f"Mais de uma velocidade respondeu: {achadas}.")
        print("Rode o teste completo em cada uma e fique com a que ler tag.")


# ---------------------------------------------------------------- diagnostico
COMBINACOES_CONTROLE = [
    (True, True),    # padrao do pyserial ao abrir
    (False, False),  # ambas soltas: descarta reset preso por DTR/RTS
    (True, False),
    (False, True),
]


def diagnosticar(porta: str):
    """Investiga o caso 'porta abre mas nada volta' em todas as velocidades.

    Velocidade errada devolve lixo, nao silencio. Silencio em todas quer dizer
    que nada chega do modulo ao PC, e as causas sao outras: porta que nao e o
    R200, modulo sem alimentar, modulo presa em reset por DTR/RTS, ou o CH340
    isolado da UART do modulo na propria placa.
    """
    print("=" * 62)
    print("1. Identidade da porta")
    print("=" * 62)

    encontrada = None
    for p in serial.tools.list_ports.comports():
        if p.device == porta:
            encontrada = p

    if encontrada is None:
        print(f"  {porta} nao aparece entre as portas seriais.")
    else:
        vid = f"{encontrada.vid:04X}" if encontrada.vid is not None else "????"
        pid = f"{encontrada.pid:04X}" if encontrada.pid is not None else "????"
        print(f"  dispositivo : {encontrada.device}")
        print(f"  VID:PID     : {vid}:{pid}")
        print(f"  descricao   : {encontrada.description}")
        print(f"  fabricante  : {encontrada.manufacturer}")
        if (vid, pid) == ("1A86", "7523"):
            print("  --> CH340 do R200, como esperado.")
        else:
            print("  --> NAO e o CH340 esperado (1A86:7523). Porta errada?")

    print()
    print("=" * 62)
    print("2. Escuta passiva, 5 s, sem enviar nada")
    print("=" * 62)
    print("  Alguns modulos falam sozinhos ao ligar. Se vier byte aqui,")
    print("  o caminho modulo -> PC existe e o problema e so de comando.")

    espontaneos = b""
    try:
        with abrir_porta(porta, BAUD_PADRAO, timeout=0.5) as ser:
            fim = time.time() + 5.0
            while time.time() < fim:
                espontaneos += ser.read(ser.in_waiting or 1)
    except serial.SerialException as erro:
        print(f"  erro ao abrir: {erro}")
        return

    if espontaneos:
        print(f"  {len(espontaneos)} bytes: {espontaneos[:32].hex(' ').upper()}")
    else:
        print("  nada (normal: o R200 so responde quando perguntado)")

    print()
    print("=" * 62)
    print("3. Linhas de controle: DTR/RTS prendem o modulo em reset?")
    print("=" * 62)
    print("  O pyserial levanta DTR e RTS ao abrir. Em placas que usam essas")
    print("  linhas para reset, isso mantem o modulo parado o tempo todo.")

    houve_resposta = False
    for dtr, rts in COMBINACOES_CONTROLE:
        try:
            with abrir_porta(porta, BAUD_PADRAO, timeout=0.3, dtr=dtr, rts=rts) as ser:
                ser.write(CMD_INFO(0x00))
                time.sleep(0.6)
                resposta = ser.read(ser.in_waiting or 1)
        except serial.SerialException as erro:
            print(f"  DTR={dtr!s:<5} RTS={rts!s:<5} : erro ({erro})")
            continue

        rotulo = f"  DTR={dtr!s:<5} RTS={rts!s:<5} : "
        if resposta:
            houve_resposta = True
            print(f"{rotulo}{len(resposta)} bytes  {resposta[:24].hex(' ').upper()}")
        else:
            print(f"{rotulo}(sem resposta)")

    print()
    print("=" * 62)
    print("Conclusao")
    print("=" * 62)

    if houve_resposta or espontaneos:
        print("  Chegou byte do modulo. O caminho existe.")
        print("  Se foi so em uma combinacao de DTR/RTS, era reset preso:")
        print("  rode a varredura de velocidade nessa combinacao.")
        return

    print("  Nenhum byte, em nenhuma condicao. O modulo nao esta falando com")
    print("  o PC. Em ordem de probabilidade:")
    print()
    print("  1. O modulo nao esta ligado de fato. Ele tem LED verde e bipe de")
    print("     boot (visto em 05/09 quando alimentado pelo VIN do ESP32).")
    print("     Sem LED e sem bipe ao plugar, e alimentacao: porta USB fraca")
    print("     ou o CH340 enumerando com o modulo sem energia. Teste o")
    print("     micro-USB num carregador de parede so para ver se acende.")
    print()
    print("  2. O CH340 esta isolado da UART do modulo nesta revisao da placa")
    print("     (R14/R15 sem componente). Nesse caso o micro-USB NUNCA vai")
    print("     conversar com o modulo, e o J3 e o unico caminho -- o que")
    print("     traz a solda de volta. Ver secao 3 do docs/HARDWARE_R200.md.")
    print()
    print("  3. Cabo com mau contato intermitente. Menos provavel: a porta")
    print("     enumerou, entao as linhas de dados funcionam.")


# ---------------------------------------------------------------- cego
JANELA_CEGA = 8  # segundos com a tag na antena, por velocidade


def inventario_cego(porta: str):
    """Testa o caminho PC -> modulo quando o caminho de volta esta mudo.

    Situacao de 19/09/2026: modulo alimentado (LED verde), porta certa
    (1A86:7523), comandos saindo, e nenhum byte voltando em nenhuma
    velocidade nem em nenhum estado de DTR/RTS -- mas o modulo bipou. Se ele
    recebe e nao responde, o defeito esta so na volta, e a unica prova
    possivel e fisica: mandar inventario continuo e ouvir o buzzer enquanto
    uma tag encosta na antena.

    Nao le nada de volta de proposito. Quem observa e voce.
    """
    print("=" * 62)
    print("INVENTARIO CEGO -- o script nao le resposta, voce escuta a placa")
    print("=" * 62)
    print()
    print("  ANTENA ROSQUEADA? Este modo LIGA O RADIO de verdade.")
    print("  Transmitir sem antena pode danificar o amplificador.")
    print()
    print("  Em cada velocidade o script manda regiao, potencia e inventario")
    print(f"  continuo, espera {JANELA_CEGA} s e manda parar. Encoste a tag na antena")
    print("  durante a contagem e anote em qual velocidade houve bipe ou LED.")
    print()
    try:
        input("  Enter para comecar (Ctrl+C para sair)... ")
    except KeyboardInterrupt:
        print()
        print("  cancelado")
        return

    for baud in BAUDS_CANDIDATOS:
        print()
        print(f"--- {baud} baud ---")
        try:
            with abrir_porta(porta, baud) as ser:
                ser.write(CMD_SET_REGIAO(REGIOES["us"]))
                time.sleep(0.2)
                ser.write(CMD_SET_POTENCIA(1800))
                time.sleep(0.2)
                ser.write(CMD_INVENTARIO_MULTI(10000))

                print("  ENCOSTE A TAG NA ANTENA agora:")
                for restante in range(JANELA_CEGA, 0, -1):
                    if restante % 2 == 0 or restante <= 3:
                        print(f"      faltam {restante} s")
                    time.sleep(1.0)

                ser.write(CMD_PARAR)
                time.sleep(0.2)
                sobrou = ser.read(ser.in_waiting or 1)
        except serial.SerialException as erro:
            print(f"  erro na porta: {erro}")
            continue

        if sobrou:
            print(f"  ATENCAO: voltaram {len(sobrou)} bytes nesta velocidade!")
            print(f"  {sobrou[:32].hex(' ').upper()}")
            print("  O caminho de volta existe. Rode --varrer de novo.")
        else:
            print("  (nada de volta, como esperado neste modo)")

    print()
    print("=" * 62)
    print("Leitura do resultado")
    print("=" * 62)
    print("  BIPOU em alguma velocidade  -> o modulo recebe e le a tag; o")
    print("     defeito esta so no retorno (modulo -> CH340). Essa e a")
    print("     velocidade da UART. O micro-USB nao serve para ler EPC, e o")
    print("     J3 volta a ser necessario -- mas a cadeia RF esta provada.")
    print()
    print("  NAO BIPOU em nenhuma  -> nada sai do PC para o modulo tambem. O")
    print("     CH340 esta isolado da UART do modulo nesta placa, nos dois")
    print("     sentidos. O micro-USB nunca vai servir; o caminho e o J3.")
    print()
    print("  Em qualquer dos casos, anotar o resultado na secao 8 do")
    print("  docs/HARDWARE_R200.md antes de desmontar a bancada.")


# ---------------------------------------------------------------- eco
PADRAO_ECO = bytes([0x55, 0xAA, 0x00, 0xFF, 0x0F, 0xF0, 0x33, 0xCC])


def teste_eco(porta: str):
    """Loopback no J3: o CH340 consegue escutar a si mesmo?

    Serve para partir em dois o caso "a ida funciona e a volta nao". Com os
    furos RXD e TXD do J3 em curto, o que o PC escreve tem que voltar. Mesmo
    metodo do loopback RX2/TX2 que validou a ponte serial no ESP32 em
    05/09/2026, mas em outra placa: la era a UART2 do ESP32, aqui e o CH340
    do proprio R200.

    O resultado e assimetrico, porque o contato pino-furo sem solda continua
    sendo o elo fraco: eco completo prova (contato ruim nao gera eco perfeito
    por acaso), eco nenhum nao prova nada -- pode ser o defeito ou pode ser o
    mesmo contato resistivo que deformou a UART em 05/09.

    Eco completo  -> PC <-> CH340 <-> J3 esta integro. O defeito esta do J3
                     para dentro: a linha TXD do modulo nao chega ate ali.
    Eco nenhum    -> o CH340 nao alcanca o J3. Os furos nao sao esse
                     barramento, ou ha componente faltando no caminho.
    Eco parcial   -> contato ruim no curto. Refazer com pressao firme.

    Sem o curto, nada deve voltar. Se voltar, o que se ve nao e eco.
    """
    print("=" * 62)
    print("TESTE DE ECO (loopback no J3)")
    print("=" * 62)
    print()
    print("  Ligue os furos RXD e TXD do J3 um no outro. Sem solda.")
    print()
    print("  Melhor jeito: um pino reto em cada furo e um jumper femea-femea")
    print("  entre os dois -- mesmo metodo do loopback RX2/TX2 que validou a")
    print("  ponte no ESP32. So o contato pino-furo fica duvidoso; o encaixe")
    print("  jumper-pino e conector de verdade.")
    print("  Alternativa pior: clipe de papel encostando nos dois furos.")
    print()
    print("  Ordem dos furos no J3, de cima para baixo:  3V3  RXD  TXD  GND")
    print("  (o furo quadrado e o GND)")
    print()
    print("  O modulo pode ficar ligado. Curto entre RXD e TXD nao danifica.")
    print()

    for rotulo, pedir in (("SEM o curto (controle)", False), ("COM o curto", True)):
        print("-" * 62)
        if pedir:
            print(f"{rotulo}: faca o curto agora e segure.")
        else:
            print(f"{rotulo}: nao encoste em nada ainda.")
        try:
            input("  Enter quando estiver pronto... ")
        except KeyboardInterrupt:
            print()
            print("  cancelado")
            return

        try:
            with abrir_porta(porta, BAUD_PADRAO, timeout=0.5) as ser:
                ser.reset_input_buffer()
                ser.write(PADRAO_ECO)
                time.sleep(0.6)
                volta = ser.read(ser.in_waiting or 1)
        except serial.SerialException as erro:
            print(f"  erro na porta: {erro}")
            return

        enviado = PADRAO_ECO.hex(" ").upper()
        print(f"  enviado : {enviado}")
        if not volta:
            print("  recebido: (nada)")
        else:
            print(f"  recebido: {volta.hex(' ').upper()}")

        if pedir:
            if volta == PADRAO_ECO:
                print()
                print("  ECO COMPLETO. O caminho PC <-> CH340 <-> J3 esta integro.")
                print("  Logo, o defeito esta do J3 para dentro: o TXD do modulo")
                print("  nao chega a esse barramento. Proximo passo e o J3 com")
                print("  contato firme -- ou seja, solda.")
            elif volta:
                print()
                print("  ECO PARCIAL. Quase certamente contato ruim no curto.")
                print("  Refaca pressionando com firmeza e rode de novo.")
            else:
                print()
                print("  SEM ECO. O CH340 nao alcanca esses furos. Ou o J3 nao e")
                print("  o barramento do CH340, ou falta componente no caminho.")
                print("  Nesse caso o micro-USB nunca vai conversar com o modulo.")
        print()


# ---------------------------------------------------------------- principal
def main():
    if len(sys.argv) < 2:
        listar_portas()
        return

    porta = sys.argv[1]

    if len(sys.argv) > 2 and sys.argv[2] in ("--varrer", "-v"):
        varrer_baud(porta)
        return

    if len(sys.argv) > 2 and sys.argv[2] in ("--diagnostico", "-d"):
        diagnosticar(porta)
        return

    if len(sys.argv) > 2 and sys.argv[2] in ("--cego", "-c"):
        inventario_cego(porta)
        return

    if len(sys.argv) > 2 and sys.argv[2] in ("--eco", "-e"):
        teste_eco(porta)
        return

    baud = BAUD_PADRAO
    if len(sys.argv) > 2:
        try:
            baud = int(sys.argv[2])
        except ValueError:
            sys.exit(f"Velocidade invalida: {sys.argv[2]}. Passe um numero ou --varrer.")

    print(f"Abrindo {porta} a {baud} baud...")

    with abrir_porta(porta, baud) as ser:
        # abrir_porta ja esperou o boot -- do modulo, e tambem do ESP32 quando
        # a porta for a da ponte serial.

        # 1) O modulo esta vivo?
        enviar(ser, CMD_INFO(0x00), "Versao de hardware")
        enviar(ser, CMD_INFO(0x01), "Versao de firmware")
        enviar(ser, CMD_INFO(0x02), "Fabricante")

        # 2) Regiao. 'us' = 902-928 MHz, a faixa compativel com o Brasil.
        enviar(ser, CMD_SET_REGIAO(REGIOES["us"]), "Definindo regiao 902-928 MHz")

        # 3) Potencia baixa para bancada: 18.00 dBm.
        #    Suba aos poucos so se precisar de mais alcance.
        enviar(ser, CMD_SET_POTENCIA(1800), "Potencia = 18.00 dBm")

        # 4) Uma leitura unica, so pra ver se responde
        enviar(ser, CMD_INVENTARIO_UNICO, "Inventario unico", espera=0.5)

        # 5) Leitura continua
        print("\n" + "=" * 55)
        print("Inventario continuo. Encoste uma tag na antena.")
        print("Ctrl+C para parar.")
        print("=" * 55 + "\n")

        ser.write(CMD_INVENTARIO_MULTI(10000))

        buffer = bytearray()
        vistas = {}
        try:
            while True:
                dados = ser.read(ser.in_waiting or 1)
                if dados:
                    buffer.extend(dados)
                    for frame in extrair_frames(buffer):
                        if frame[2] != 0x22:
                            continue
                        resultado = interpretar_tag(frame)
                        if not resultado:
                            continue
                        epc, rssi = resultado
                        if epc not in vistas:
                            vistas[epc] = 0
                            print(f"  [NOVA]  {epc}   RSSI {rssi} dBm")
                        vistas[epc] += 1
                time.sleep(0.01)

        except KeyboardInterrupt:
            print("\n\nParando...")
            ser.write(CMD_PARAR)
            time.sleep(0.2)

        print("\n" + "=" * 55)
        print(f"Tags distintas lidas: {len(vistas)}")
        for epc, contagem in vistas.items():
            print(f"  {epc}  ({contagem} leituras)")
        print("=" * 55)




if __name__ == "__main__":
    main()
