# Guia de Conexão IoT — ESP32 + HC-SR04 + MQTT

Passo a passo para colocar um sensor de vaga em funcionamento e ver a leitura chegar no
backend.

```text
Veículo → HC-SR04 → ESP32 ──Wi-Fi──► Mosquitto (:1883) ──► Spring Boot (MqttSubscriber)
                                                              └─► PostgreSQL → API → Web/Mobile/Desktop
```

| Peça | Arquivo |
|---|---|
| Firmware | `iot/esp32-vaga/src/main.cpp` |
| Configuração do dispositivo | `iot/esp32-vaga/src/config.h` |
| Build / bibliotecas | `iot/esp32-vaga/platformio.ini` (PubSubClient 2.8, ArduinoJson 7) |
| Broker | `infra/mosquitto/config/mosquitto.conf` + `docker-compose.yml` |
| Consumidor no backend | `backend/src/main/kotlin/br/com/smartparking/api/mqtt/` |
| Simulador (sem placa) | `scripts/simular-sensor.ps1` / `scripts/simular-sensor.sh` |

---

## 1. Material

- 1 × ESP32 DevKit (placa `esp32dev`)
- 1 × sensor ultrassônico HC-SR04
- 2 resistores para o divisor de tensão do ECHO (ex.: **1 kΩ + 2 kΩ**, ou 10 kΩ + 20 kΩ)
- Protoboard, jumpers e um cabo USB **de dados** (alguns cabos só carregam)

## 2. Ligação elétrica

| HC-SR04 | ESP32 | Observação |
|---|---|---|
| VCC | **VIN / 5V** | O HC-SR04 precisa de 5 V |
| GND | GND | GND comum |
| TRIG | **GPIO 5** | 3,3 V do ESP32 é suficiente para disparar |
| ECHO | **GPIO 18 via divisor** | ⚠️ O ECHO sai em **5 V**; o GPIO do ESP32 aceita só **3,3 V** |

Divisor de tensão no ECHO:

```text
ECHO ──[ 1 kΩ ]──┬── GPIO 18
                 │
              [ 2 kΩ ]
                 │
                GND          → 5 V × 2/(1+2) ≈ 3,3 V
```

Os pinos podem ser trocados em `config.h` (`PINO_TRIG`, `PINO_ECHO`). Evite os GPIO 6–11
(flash) e os pinos de boot 0, 2, 12 e 15.

## 3. Subir o broker e o backend

```powershell
docker compose up -d                   # na raiz: PostgreSQL + Mosquitto
cd backend; .\gradlew.bat bootRun      # API em http://localhost:8085
```

No log do backend devem aparecer:

```text
Conectado ao broker MQTT tcp://localhost:1883 como smart-parking-backend
Inscrito no topico smart-parking/vaga/+ (qos=1, reconexao=false)
```

## 4. Liberar a rede para o ESP32

O ESP32 é **outro host** na rede, então `localhost` não serve para ele.

1. **Descubra o IP do PC** que roda o Docker:
   ```powershell
   ipconfig        # "Endereço IPv4" do adaptador Wi-Fi/Ethernet, ex.: 192.168.0.100
   ```
2. **Libere a porta 1883 no Firewall do Windows** (PowerShell como administrador):
   ```powershell
   New-NetFirewallRule -DisplayName "Mosquitto MQTT 1883" -Direction Inbound -Protocol TCP -LocalPort 1883 -Action Allow
   ```
3. **Mesma rede:** o ESP32 e o PC precisam estar na mesma rede local. Redes da faculdade
   costumam isolar clientes; se for o caso, use o **roteador do celular**.
4. **Wi-Fi 2,4 GHz:** o ESP32 **não** conecta em redes só de 5 GHz.
5. Teste a partir de outra máquina/celular (app *MQTT Explorer* ou *MyMQTT*) conectando em
   `IP-DO-PC:1883` antes de gravar a placa.

## 5. Cadastro no backend

O backend descarta mensagens de dispositivos não cadastrados. A migration
`V2__dados_iniciais.sql` já cria quatro dispositivos vinculados às vagas 1–4 do Bloco A:

| `DEVICE_ID` (= campo `nome`) | `MQTT_TOPIC` (= campo `mqttTopic`) | `VAGA_ID` |
|---|---|---:|
| `ESP32-VAGA-01` | `smart-parking/vaga/01` | 1 |
| `ESP32-VAGA-02` | `smart-parking/vaga/02` | 2 |
| `ESP32-VAGA-03` | `smart-parking/vaga/03` | 3 |
| `ESP32-VAGA-04` | `smart-parking/vaga/04` | 4 |

Para um dispositivo novo, cadastre via `POST /api/dispositivos` (perfil ADMIN) e vincule a
uma vaga. O tópico precisa começar com `smart-parking/vaga/` para casar com a assinatura
`smart-parking/vaga/+` do backend.

**Como o backend encontra o dispositivo** (`LeituraSensorService`), nesta ordem:
1. pelo **tópico** em que a mensagem chegou (`mqttTopic`);
2. pelo **`deviceId`** do payload, comparado com o `nome` (sem diferenciar maiúsculas);
3. pela **vaga** do `vagaId` do payload.

## 6. Configurar e gravar o firmware

Edite `iot/esp32-vaga/src/config.h`:

```cpp
#define WIFI_SSID      "MinhaRede"
#define WIFI_PASSWORD  "minhasenha"
#define MQTT_BROKER    "192.168.0.100"          // IP do PC do passo 4
#define DEVICE_ID      "ESP32-VAGA-01"
#define VAGA_ID        1
#define MQTT_TOPIC     "smart-parking/vaga/01"
#define LIMITE_OCUPADA_CM 150.0                 // calibrar (passo 8)
```

> ⚠️ `config.h` é versionado. **Não faça commit com o SSID e a senha reais** — antes do
> commit, volte os valores de exemplo ou use `git update-index --skip-worktree iot/esp32-vaga/src/config.h`.

Gravar com o PlatformIO (VS Code + extensão PlatformIO, ou CLI):

```powershell
cd iot\esp32-vaga
pio run -t upload          # compila e grava
pio device monitor         # monitor serial a 115200
```

Se o upload ficar em `Connecting........`, segure o botão **BOOT** da placa até começar a
gravação. Se a porta COM não aparecer, instale o driver **CP210x** ou **CH340** (depende do
chip USB da placa).

Na Arduino IDE: instale o pacote de placas *esp32 by Espressif*, as bibliotecas
**PubSubClient** e **ArduinoJson (7.x)**, renomeie `main.cpp` para `esp32-vaga.ino` (mantendo
`config.h` na mesma pasta) e grave com a placa *ESP32 Dev Module*.

## 7. Verificar ponta a ponta

**Monitor serial do ESP32:**

```text
=== Smart Parking - sensor de vaga ===
Dispositivo: ESP32-VAGA-01 | Vaga: 1 | Topico: smart-parking/vaga/01
WiFi conectado. IP: 192.168.0.57
Conectando ao broker MQTT 192.168.0.100:1883 ... MQTT conectado
Distancia: 80.5 cm | Status: OCUPADA
Mensagem publicada em smart-parking/vaga/01: {"deviceId":"ESP32-VAGA-01","vagaId":1,"distancia":80.5,"status":"OCUPADA"}
```

**Escutar o broker** (em outro terminal):

```powershell
docker exec -it smartparking-mosquitto mosquitto_sub -t "smart-parking/#" -v
```

**Log do backend:**

```text
MQTT [smart-parking/vaga/01] device=ESP32-VAGA-01 distancia=80.5cm status=OCUPADA vaga=1 mudou=true
```

**API:** `GET /api/vagas/1` deve mostrar o novo `status`, e `GET /api/dispositivos/1` deve
mostrar `ONLINE` com a `ultimaComunicacao` atualizada.

## 8. Calibração e comportamento do firmware

- `distancia <= LIMITE_OCUPADA_CM` → **OCUPADA**; acima disso → **LIVRE**.
- Sem eco (nada no alcance de ~4 m) → firmware assume 400 cm → **LIVRE**.
- Cada amostra é a **mediana de 5 leituras** (`LEITURAS_POR_AMOSTRA`), o que filtra os picos do
  HC-SR04.
- Publica quando o status **muda** e, mesmo sem mudança, a cada **30 s** (heartbeat,
  `INTERVALO_HEARTBEAT_MS`).
- Para calibrar: com a vaga vazia, anote a distância do monitor serial (ex.: 220 cm); com um
  carro/caixa, anote de novo (ex.: 70 cm); defina o limite entre as duas (ex.: 150 cm).

Payload publicado (o backend também aceita um `timestamp` ISO opcional; sem ele, usa a hora
do servidor):

```json
{"deviceId":"ESP32-VAGA-01","vagaId":1,"distancia":80.5,"status":"OCUPADA"}
```

Regras aplicadas no backend: `distancia` ausente/negativa → descartada; `status` diferente de
`LIVRE`/`OCUPADA` → descartada; vaga em **MANUTENCAO** → a medição é gravada, mas o status não
muda (RN04); mesmo status → nada a alterar (RN06).

## 9. Testar sem a placa

O simulador publica o mesmo payload pelo container do Mosquitto:

```powershell
.\scripts\simular-sensor.ps1                     # alterna LIVRE/OCUPADA na vaga 01 a cada 5 s
.\scripts\simular-sensor.ps1 -Acao ocupada -Vaga 2
```

Publicação manual:

```powershell
docker exec smartparking-mosquitto mosquitto_pub -t "smart-parking/vaga/01" -m '{\"deviceId\":\"ESP32-VAGA-01\",\"vagaId\":1,\"distancia\":80.5,\"status\":\"OCUPADA\"}'
```

## 10. Solução de problemas

| Sintoma (monitor serial / log) | Causa provável | Solução |
|---|---|---|
| `Conectando ao Wi-Fi ....` infinito | SSID/senha errados ou rede 5 GHz | Corrija o `config.h`; use rede 2,4 GHz |
| `falhou (rc=-2)` | Broker inalcançável | IP errado, firewall bloqueando 1883 ou redes diferentes (passo 4) |
| `falhou (rc=5)` | Broker exige autenticação | Preencha `MQTT_USER`/`MQTT_PASSWORD` ou use `allow_anonymous true` |
| Distância sempre 400 cm | ECHO sem sinal | Confira fiação/divisor do ECHO e alimentação 5 V do sensor |
| Distância oscilando muito | Superfície inclinada/macia ou sensor solto | Fixe o sensor perpendicular à vaga; aumente `LEITURAS_POR_AMOSTRA` |
| ESP32 reinicia ao ligar o sensor | Alimentação insuficiente | Use outra porta USB/fonte; cabo mais curto |
| Backend: `Nenhum dispositivo cadastrado para o topico ...` | Tópico/`deviceId` não cadastrados | Cadastre o dispositivo (passo 5) |
| Backend: `nao esta vinculado a nenhuma vaga` | Dispositivo sem vaga | Vincule o dispositivo a uma vaga na API |
| Backend: `Falha ao conectar no broker MQTT` | Mosquitto fora do ar | `docker compose up -d`; o backend reconecta sozinho |
| Mensagem chega no `mosquitto_sub` mas a vaga não muda | Vaga em MANUTENCAO ou status igual ao atual | Comportamento esperado (RN04/RN06) |

## 11. Pontos de atenção (revisão do IoT)

### Detecção de dispositivo OFFLINE (implementada)

A cada mensagem MQTT o backend marca o dispositivo `ONLINE` e grava `ultimaComunicacao`. A
rotina agendada `MonitorDispositivosService` faz o caminho inverso: a cada 30 s, marca
`OFFLINE` todo dispositivo `ONLINE` sem mensagem há mais de **90 s** (3 heartbeats perdidos).
Dispositivos em `MANUTENCAO` não são alterados. Quando o ESP32 volta a publicar, o dispositivo
retorna a `ONLINE` automaticamente.

Configuração (`application.yml`, seção `smartparking.dispositivos`):

| Variável de ambiente | Padrão | Significado |
|---|---|---|
| `TEMPO_LIMITE_OFFLINE` | `90` | Segundos sem mensagem até virar OFFLINE |
| `INTERVALO_VERIFICACAO_OFFLINE_MS` | `30000` | Intervalo entre verificações |
| `MONITOR_OFFLINE_HABILITADO` | `true` | Liga/desliga a rotina |

Para ver funcionando: publique com o simulador, pare o simulador e aguarde ~2 min. O log
mostra `1 dispositivo(s) marcado(s) OFFLINE ...` e `GET /api/dispositivos/1` passa a `OFFLINE`.

### Pontos em aberto

Nenhum impede a demonstração:

1. **Reconexão bloqueante.** `conectarWiFi()` e `conectarMqtt()` ficam em laço até conectar; se o
   Wi-Fi cair durante a reconexão MQTT, o laço não volta a checar o Wi-Fi. Na prática basta
   reiniciar a placa; uma melhoria seria limitar as tentativas e chamar `ESP.restart()`.
2. **Credenciais no `config.h` versionado** (ver aviso no passo 6).
3. **Broker sem autenticação** (`allow_anonymous true`): adequado para laboratório; em produção,
   usar `password_file` e preencher `MQTT_USER`/`MQTT_PASSWORD` no firmware e
   `SMARTPARKING_MQTT_USUARIO`/`SMARTPARKING_MQTT_SENHA` no backend (propriedades
   `smartparking.mqtt.usuario`/`senha` de `MqttProperties`).
