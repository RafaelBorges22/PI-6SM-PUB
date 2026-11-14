"""
Ponte HTTP -> MQTT para os testes do UC01 no Postman (simulador do ESP32).

O Postman nao exporta requisicoes MQTT em collections, entao esta ponte recebe
a leitura por HTTP e a publica no broker Mosquitto (container
smartparking-mosquitto), exatamente como o ESP32 faria. Nao altera o sistema:
faz o mesmo papel do scripts/simular-sensor.ps1.

Uso:
    python scripts/ponte-mqtt-http.py [--porta 8099] [--log-backend CAMINHO]

Endpoints:
    POST /publicar?topico=smart-parking/vaga/01
        Corpo (texto puro) = mensagem MQTT publicada sem alteracao.
        Resposta: topico, mensagem, hora da publicacao e, se --log-backend foi
        informado, as linhas que o backend registrou apos a publicacao.
    GET /saude
        Verifica se a ponte esta no ar.

Somente biblioteca padrao do Python; requer o Docker com o container do broker.
"""

import argparse
import json
import os
import re
import subprocess
import time
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

CONTAINER = "smartparking-mosquitto"
ESPERA_LOG_SEGUNDOS = 2.0

# Linhas do backend que interessam ao UC01 (consumo MQTT e atualizacao da vaga).
FILTRO_LOG = re.compile(
    r"LeituraSensorService|VagaService|MqttSubscriber|overflow|Caused by",
)
# "2026-11-12T18:18:52.177-03:00  INFO 9368 --- [app] [thread] b.c.s.api.x.Classe : msg"
FORMATO_SPRING = re.compile(
    r"^\d{4}-\d{2}-\d{2}T(\d{2}:\d{2}:\d{2})\.\d+\S*\s+(\w+)\s+\d+\s+---\s+\[[^\]]*\]\s+\[[^\]]*\]\s+(\S+)\s+:\s+(.*)$",
)

args = None


def resumir_linha(linha):
    m = FORMATO_SPRING.match(linha)
    if not m:
        return linha.strip()[:400]
    hora, nivel, classe, mensagem = m.groups()
    return f"{hora} {nivel} {classe.split('.')[-1]}: {mensagem.strip()[:400]}"


def tamanho_log():
    if not args.log_backend or not os.path.exists(args.log_backend):
        return None
    return os.path.getsize(args.log_backend)


def linhas_novas_do_log(posicao):
    if posicao is None:
        return ["(log do backend nao configurado: inicie a ponte com --log-backend)"]
    with open(args.log_backend, "rb") as f:
        f.seek(posicao)
        texto = f.read().decode("utf-8", errors="replace")
    return [
        resumir_linha(l) for l in texto.splitlines()
        if FILTRO_LOG.search(l) and not l.lstrip().startswith("at ")
    ]


class Handler(BaseHTTPRequestHandler):
    def _responder(self, status, corpo):
        dados = json.dumps(corpo, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(dados)))
        self.end_headers()
        self.wfile.write(dados)

    def do_GET(self):
        if urlparse(self.path).path == "/saude":
            self._responder(200, {"status": "ok", "broker": CONTAINER, "logBackend": args.log_backend})
        else:
            self._responder(404, {"erro": "use POST /publicar?topico=..."})

    def do_POST(self):
        url = urlparse(self.path)
        if url.path != "/publicar":
            self._responder(404, {"erro": "use POST /publicar?topico=..."})
            return
        topico = (parse_qs(url.query).get("topico") or [""])[0]
        if not topico:
            self._responder(400, {"erro": "informe ?topico=smart-parking/vaga/NN"})
            return
        tamanho = int(self.headers.get("Content-Length") or 0)
        mensagem = self.rfile.read(tamanho)

        posicao = tamanho_log()
        hora = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
        proc = subprocess.run(
            ["docker", "exec", "-i", CONTAINER, "mosquitto_pub", "-h", "localhost", "-q", "1", "-t", topico, "-s"],
            input=mensagem,
            capture_output=True,
        )
        if proc.returncode != 0:
            self._responder(502, {
                "publicado": False,
                "erro": proc.stderr.decode("utf-8", errors="replace").strip(),
            })
            return

        time.sleep(ESPERA_LOG_SEGUNDOS)
        self._responder(200, {
            "publicado": True,
            "broker": f"{CONTAINER}:1883",
            "topico": topico,
            "mensagem": mensagem.decode("utf-8", errors="replace"),
            "horaPublicacao": hora,
            "logBackend": linhas_novas_do_log(posicao),
        })

    def log_message(self, formato, *valores):
        print(f"[ponte] {self.address_string()} {formato % valores}", flush=True)


def main():
    global args
    parser = argparse.ArgumentParser(description="Ponte HTTP -> MQTT para os testes do UC01")
    parser.add_argument("--porta", type=int, default=8099)
    parser.add_argument("--log-backend", default=None, help="arquivo de log do backend (opcional)")
    args = parser.parse_args()
    servidor = ThreadingHTTPServer(("127.0.0.1", args.porta), Handler)
    print(f"[ponte] ouvindo em http://localhost:{args.porta} -> broker {CONTAINER}", flush=True)
    servidor.serve_forever()


if __name__ == "__main__":
    main()
