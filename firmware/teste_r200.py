#!/usr/bin/env python3
"""
teste_r200.py — Teste de bancada do modulo UHF Invelion YPD-R200 (Impinj E710)

Roda no PC (notebook), conectado ao modulo pelo micro-USB da placa.
NAO e gravado no modulo. O R200 ja tem firmware de fabrica.

Uso:
    pip install pyserial
    python teste_r200.py                 # lista as portas disponiveis
    python teste_r200.py COM5            # Windows
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

BAUD = 115200

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


# ---------------------------------------------------------------- parser
def extrair_frames(buffer: bytearray):
    """Consome o buffer e devolve os frames completos encontrados."""
    frames = []
    while True:
        try:
            inicio = buffer.index(FRAME_HEAD)
        except ValueError:
            buffer.clear()
            break

        if inicio > 0:
            del buffer[:inicio]

        if len(buffer) < 7:  # frame minimo
            break

        tam_params = (buffer[3] << 8) | buffer[4]
        tam_total = 7 + tam_params

        if len(buffer) < tam_total:
            break  # frame ainda chegando

        frame = bytes(buffer[:tam_total])
        del buffer[:tam_total]

        if frame[-1] != FRAME_END:
            continue  # frame corrompido, descarta e segue

        corpo = frame[1:-2]
        if (sum(corpo) & 0xFF) != frame[-2]:
            print("  [aviso] checksum invalido, frame descartado")
            continue

        frames.append(frame)
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


# ---------------------------------------------------------------- principal
def main():
    if len(sys.argv) < 2:
        listar_portas()
        return

    porta = sys.argv[1]
    print(f"Abrindo {porta} a {BAUD} baud...")

    with serial.Serial(porta, BAUD, timeout=0.2) as ser:
        time.sleep(0.5)

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
