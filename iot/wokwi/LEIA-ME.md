# Simulação no Wokwi — Smart Parking (Versão Física)

Montagem de referência: <https://wokwi.com/projects/481580197751757825>

Esta pasta tem tudo o que o projeto do Wokwi precisa para rodar o **mesmo firmware** do kit
físico e mandar as leituras para o backend:

| Arquivo no Wokwi | De onde vem |
|---|---|
| `sketch.ino` | conteúdo de [`../arduino-ide/esp32-vaga/esp32-vaga.ino`](../arduino-ide/esp32-vaga/esp32-vaga.ino) |
| `config.h` | [`config.h`](config.h) desta pasta (Wi-Fi `Wokwi-GUEST` + broker público) |
| `libraries.txt` | [`libraries.txt`](libraries.txt) desta pasta (PubSubClient, ArduinoJson) |
| `diagram.json` | [`diagram.json`](diagram.json) desta pasta (cópia da montagem física) |

```text
ESP32 (Wokwi) ──Wokwi-GUEST──► broker.hivemq.com:1883 ◄── backend no PC (conexão de saída)
```

O simulador roda na nuvem: ele **não alcança** o `localhost` nem o IP local do PC. Por isso
ESP32 e backend se encontram num broker público, e nenhum dos dois precisa de porta liberada
no firewall.

## 1. Montar o projeto no Wokwi

1. Abra o link acima, entre na conta e use **Save a copy** para ter uma cópia editável.
2. Aba `sketch.ino`: apague tudo e cole o conteúdo de `iot/arduino-ide/esp32-vaga/esp32-vaga.ino`.
3. Clique na seta ao lado das abas → **New file…** → `config.h` → cole o `config.h` desta pasta.
4. **New file…** → `libraries.txt` → cole o `libraries.txt` desta pasta.
5. Escolha um prefixo aleatório para o grupo (ex.: `spk-7f3a`) e troque `spk-TROQUE-AQUI`
   no `MQTT_TOPIC`. O broker é público: o prefixo evita misturar mensagens com outras pessoas.

O `diagram.json` desta pasta é igual ao do projeto; só é preciso colá-lo se o circuito for
alterado ou perdido.

## 2. Subir o backend apontando para o broker público

Com Docker (tudo em containers), crie o `.env` na raiz a partir do `.env.example` com:

```dotenv
MQTT_BROKER=tcp://broker.hivemq.com:1883
MQTT_TOPICO=spk-7f3a/vaga/+
MQTT_CLIENT_ID=spk-7f3a-backend
```

```powershell
docker compose --profile app up -d --build
docker compose --profile app logs -f backend
```

Ou sem a API em container:

```powershell
docker compose up -d
$env:MQTT_BROKER = "tcp://broker.hivemq.com:1883"
$env:MQTT_TOPICO = "spk-7f3a/vaga/+"
$env:MQTT_CLIENT_ID = "spk-7f3a-backend"
cd backend; .\gradlew.bat bootRun
```

O banco não precisa de alteração: o tópico novo não está cadastrado, mas o backend
encontra o dispositivo pelo `deviceId` (`ESP32-VAGA-01`) e vincula à vaga 1.

## 3. Testar

1. Clique em **▶ Start**. O Monitor Serial deve mostrar:

   ```text
   === Smart Parking - sensor de vaga ===
   Dispositivo: ESP32-VAGA-01 | Vaga: 1 | Topico: spk-7f3a/vaga/01
   Pinos: TRIG=5 ECHO=18 LED=19 | Limite: 150 cm
   Conectando ao Wi-Fi Wokwi-GUEST ...
   WiFi conectado. IP: 10.10.0.2
   Conectando ao broker MQTT broker.hivemq.com:1883 como ESP32-VAGA-01-XXXXXX ... MQTT conectado
   Distancia: 400.0 cm | Status: LIVRE | LED: APAGADO
   Mensagem publicada em spk-7f3a/vaga/01: {"deviceId":"ESP32-VAGA-01","vagaId":1,...}
   ```

2. Clique no HC-SR04 durante a simulação e arraste a distância para **≤ 150 cm**: o LED
   acende, o status vira `OCUPADA` e a mensagem é publicada.
3. No log do backend aparece
   `MQTT [spk-7f3a/vaga/01] device=ESP32-VAGA-01 ... status=OCUPADA vaga=1 mudou=true`, e no
   app a vaga 1 muda de cor.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `config.h: No such file or directory` | Falta a aba `config.h` (passo 1.3) |
| `PubSubClient.h: No such file` | Falta o `libraries.txt` (passo 1.4) |
| Distância sempre `400.0 cm (sem eco)` | O simulador pode não propagar o sinal pelo divisor 1k/2k. **Só no Wokwi**, ligue o ECHO direto no GPIO 18 (no kit real o divisor é obrigatório) |
| `falhou (rc=-2)` | Broker público fora do ar: troque por `test.mosquitto.org` nos dois lados |
| Mensagem publicada, mas o backend não registra nada | `MQTT_TOPICO` do backend diferente do prefixo do `MQTT_TOPIC` |

> Broker público é aberto: qualquer um que souber o tópico pode ler ou publicar. Serve para
> aula e demonstração, não para produção.
