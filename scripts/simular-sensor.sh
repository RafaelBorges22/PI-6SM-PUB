#!/usr/bin/env bash
# Publica leituras MQTT como se fossem de um ESP32.
#
# Serve para testar todo o caminho (broker -> backend -> banco -> frontends)
# antes de ter a placa em maos.
#
# Uso:
#   ./scripts/simular-sensor.sh                     # alterna LIVRE/OCUPADA a cada 5s na vaga 01
#   ./scripts/simular-sensor.sh ocupada             # publica uma unica leitura OCUPADA
#   ./scripts/simular-sensor.sh livre 2 ESP32-VAGA-02
#
# Requer o container smartparking-mosquitto no ar (docker compose up -d).

set -euo pipefail

ACAO="${1:-ciclo}"
VAGA="${2:-1}"
DEVICE_ID="${3:-ESP32-VAGA-0${VAGA}}"
TOPICO="smart-parking/vaga/0${VAGA}"
CONTAINER="smartparking-mosquitto"

publicar() {
  local status="$1"
  local distancia="$2"
  local payload
  payload=$(printf '{"deviceId":"%s","vagaId":%s,"distancia":%s,"status":"%s"}' \
    "$DEVICE_ID" "$VAGA" "$distancia" "$status")

  docker exec "$CONTAINER" mosquitto_pub -h localhost -t "$TOPICO" -m "$payload"
  echo "[$(date +%H:%M:%S)] $TOPICO <- $payload"
}

case "$ACAO" in
  ocupada) publicar "OCUPADA" "80.5" ;;
  livre)   publicar "LIVRE" "250.8" ;;
  ciclo)
    echo "Alternando o status da vaga $VAGA a cada 5s. Ctrl+C para parar."
    while true; do
      publicar "OCUPADA" "80.5"
      sleep 5
      publicar "LIVRE" "250.8"
      sleep 5
    done
    ;;
  *)
    echo "Acao invalida: $ACAO (use: ciclo, ocupada ou livre)" >&2
    exit 1
    ;;
esac
