# Execução em sala de aula — Docker + ESP32 na Arduino IDE

Guia para rodar o Smart Parking com o **kit físico** na faculdade: API, frontend Web,
banco e broker MQTT em **containers Docker**, e o firmware gravado pela **Arduino IDE**.

```text
ESP32 + HC-SR04 ──Wi-Fi──► Mosquitto (container, :1883) ──► API (container, :8085) ──► PostgreSQL (container)
      ▲                                                            ▲
   USB (COM4) ← só para gravar e ver o Monitor Serial         Navegador → Frontend Web (container, :8080)
```

> **A porta COM serve só para gravar o firmware e abrir o Monitor Serial.** Depois de
> gravado, o ESP32 manda as leituras por **Wi-Fi/MQTT**, não pela USB. Por isso a porta
> COM é configurada **apenas na Arduino IDE** — nenhum container nem a API usam ela.

---

## Checklist rápido (o que mudar em cada máquina/sala)

| # | O quê | Onde | Exemplo na faculdade |
|---|---|---|---|
| 1 | **Porta COM do kit** | Arduino IDE → *Ferramentas > Porta* | `COM4` |
| 2 | Placa | Arduino IDE → *Ferramentas > Placa* | `ESP32 Dev Module` |
| 3 | Nome da rede Wi-Fi | `config.h` → `WIFI_SSID` | rede 2,4 GHz da sala / roteador do celular |
| 4 | Senha do Wi-Fi | `config.h` → `WIFI_PASSWORD` | |
| 5 | **IP do PC que roda o Docker** | `config.h` → `MQTT_BROKER` | resultado do `ipconfig`, ex. `192.168.0.100` |
| 6 | Porta MQTT | `config.h` → `MQTT_PORT` **=** `.env` → `MQTT_PORT` | `1883` |
| 7 | Qual vaga o kit representa | `config.h` → `DEVICE_ID`, `VAGA_ID`, `MQTT_TOPIC` | `ESP32-VAGA-01`, `1`, `smart-parking/vaga/01` |
| 8 | Distância que conta como "ocupada" | `config.h` → `LIMITE_OCUPADA_CM` | calibrar no kit (seção 6) |
| 9 | Pinos do sensor (se o kit for diferente) | `config.h` → `PINO_TRIG`, `PINO_ECHO` | `5` e `18` |
| 10 | Portas no PC (só se houver conflito) | `.env` → `WEB_PORT`, `API_PORT`, `POSTGRES_PORT` | `8080`, `8085`, `5432` |

O `config.h` usado pela Arduino IDE é o de **`iot/arduino-ide/esp32-vaga/config.h`**.

---

## 1. Pré-requisitos no PC da sala

| Ferramenta | Para quê |
|---|---|
| Docker Desktop (aberto e rodando) | API, frontend, PostgreSQL e Mosquitto |
| Arduino IDE 2.x | Gravar o ESP32 |
| Driver USB da placa (**CP210x** ou **CH340**) | Para a porta COM aparecer |

Não precisa de JDK, Gradle, Node nem Android SDK: tudo é compilado dentro dos containers.

## 2. Subir API + frontend + banco + broker (Docker)

Na raiz do projeto:

```powershell
docker compose --profile app up -d --build
```

A primeira execução baixa as imagens e compila o projeto (pode levar **10–15 min**; nas
próximas, segundos). Leve o projeto já compilado de casa se a internet da faculdade for
lenta — basta ter rodado o comando acima uma vez na mesma máquina.

| Serviço | Endereço no PC | Container |
|---|---|---|
| Frontend Web | <http://localhost:8080> | `smartparking-frontend` |
| API REST | <http://localhost:8085> | `smartparking-backend` |
| Swagger | <http://localhost:8085/swagger-ui.html> | `smartparking-backend` |
| Broker MQTT | `IP-DO-PC:1883` | `smartparking-mosquitto` |
| PostgreSQL | `localhost:5432` | `smartparking-postgres` |

Login: `admin@smartparking.com` / `123456` (ADMIN) ou `user@smartparking.com` / `123456`.

Na tela de login o campo **Endereço da API** já vem com `http://localhost:8085`. Mude só
se trocou `API_PORT` ou se for abrir o frontend em **outra máquina** (use
`http://IP-DO-PC:8085`). Não coloque `/api` no final.

Comandos úteis:

```powershell
docker compose --profile app ps                 # status dos containers
docker compose --profile app logs -f backend    # log da API (ver leituras MQTT chegando)
docker compose --profile app down               # parar tudo (mantém os dados)
docker compose --profile app down -v            # parar e APAGAR o banco (volta ao estado inicial)
docker compose --profile app up -d --build      # recompilar após mudar o código
```

> Rodando o backend fora do Docker (`./gradlew bootRun`), continue usando só
> `docker compose up -d` — sem `--profile app` sobem apenas PostgreSQL e Mosquitto.

### Trocando portas (arquivo `.env`)

Se alguma porta já estiver ocupada no PC da sala, copie o modelo e edite:

```powershell
copy .env.example .env
```

| Variável | Padrão | Quando mudar | Efeito colateral |
|---|---|---|---|
| `WEB_PORT` | `8080` | Outra aplicação usa a 8080 | Acesse `http://localhost:NOVA` |
| `API_PORT` | `8085` | Porta ocupada | Ajuste o campo *Endereço da API* no login |
| `POSTGRES_PORT` | `5432` | Já existe um PostgreSQL instalado | Nenhum (a API usa a rede interna do Docker) |
| `MQTT_PORT` | `1883` | Já existe um broker MQTT instalado | Mude `MQTT_PORT` no `config.h` e regrave o ESP32 |
| `MQTT_TOPICO` | `smart-parking/vaga/+` | Raramente | `MQTT_TOPIC` do ESP32 precisa casar com ele |
| `TEMPO_LIMITE_OFFLINE` | `90` | Demonstrar OFFLINE mais rápido | Segundos sem mensagem até o sensor ficar OFFLINE |
| `JWT_SECRET` | chave de dev | Uso fora do laboratório | Mínimo 32 caracteres |

Depois de alterar o `.env`: `docker compose --profile app up -d`.

## 3. Liberar a rede para o ESP32

O ESP32 é **outro dispositivo** na rede: ele não enxerga `localhost` do PC.

1. **IP do PC:** rode `ipconfig` e anote o *Endereço IPv4* do adaptador Wi-Fi/Ethernet
   (ex.: `192.168.0.100`). Esse valor vai em `MQTT_BROKER`. **Ele muda de rede para
   rede** — confira sempre que mudar de sala ou de roteador.
2. **Firewall** (PowerShell como administrador, uma vez por PC):
   ```powershell
   New-NetFirewallRule -DisplayName "Mosquitto MQTT 1883" -Direction Inbound -Protocol TCP -LocalPort 1883 -Action Allow
   ```
3. **Mesma rede e 2,4 GHz:** PC e ESP32 na mesma rede; o ESP32 não conecta em 5 GHz.
   Redes da faculdade costumam isolar os dispositivos — se o ESP32 conectar no Wi-Fi mas
   não no MQTT (`rc=-2`), use o **roteador do celular** para os dois.

## 4. Preparar a Arduino IDE (uma vez por PC)

1. *Arquivo > Preferências > URLs adicionais de gerenciadores de placas*:
   `https://espressif.github.io/arduino-esp32/package_esp32_index.json`
2. *Ferramentas > Placa > Gerenciador de placas* → instale **esp32 by Espressif Systems**.
3. *Ferramentas > Gerenciar bibliotecas* → instale **PubSubClient** (Nick O'Leary) e
   **ArduinoJson** (Benoit Blanchon, versão **7.x**).

## 5. Configurar e gravar o kit

### 5.1 Colocar o código na Arduino IDE

O firmware tem **dois arquivos**, e os dois precisam estar no mesmo sketch:

| Arquivo | Conteúdo |
|---|---|
| `esp32-vaga.ino` | Lógica: Wi-Fi, MQTT, leitura do HC-SR04, envio do JSON |
| `config.h` | **Tudo o que você altera:** Wi-Fi, IP do broker, vaga, pinos, limite |

O `.ino` começa com `#include "config.h"`. Se você colar **só o `.ino`**, a compilação
para com `config.h: No such file or directory`.

> O código usa `WiFi.h` e só compila em placa **ESP32**. Em Arduino Uno/Nano (sem Wi-Fi)
> ele não funciona.

**Opção A — abrir o arquivo (recomendada, sem copiar nada)**

1. *Arquivo > Abrir…* → selecione **`iot/arduino-ide/esp32-vaga/esp32-vaga.ino`**.
2. A IDE abre duas abas: `esp32-vaga.ino` e `config.h`. Pronto.

A pasta já segue a regra da Arduino IDE (o `.ino` tem o mesmo nome da pasta). Se abrir o
arquivo a partir de um pendrive ou do e-mail, copie a **pasta inteira** `esp32-vaga`,
não só o `.ino`.

**Opção B — copiar e colar num sketch novo**

1. *Arquivo > Novo sketch*, apague o conteúdo padrão (`setup`/`loop` vazios) e cole **todo**
   o conteúdo de `esp32-vaga.ino`.
2. *Arquivo > Salvar como…* → nome `esp32-vaga`.
3. Clique nos **três pontinhos (⋮)** à direita das abas → **Nova aba** → digite
   exatamente `config.h` → OK.
4. Cole na nova aba o conteúdo de `iot/arduino-ide/esp32-vaga/config.h`.
5. Salve (Ctrl+S). O sketch agora tem as duas abas.

Nas duas opções, as bibliotecas da seção 4 (PubSubClient e ArduinoJson 7.x) e o pacote de
placas ESP32 precisam estar instalados.

### 5.2 Ajustar o `config.h` e gravar

1. Edite a aba **`config.h`**:

   ```cpp
   // ---- Wi-Fi ----
   #define WIFI_SSID      "NOME-DA-REDE"          // rede 2,4 GHz
   #define WIFI_PASSWORD  "SENHA-DA-REDE"

   // ---- MQTT ----
   #define MQTT_BROKER    "192.168.0.100"         // IP do PC (ipconfig) - NUNCA "localhost"
   #define MQTT_PORT      1883                    // igual ao MQTT_PORT do .env

   // ---- Identidade: qual vaga este kit representa ----
   #define DEVICE_ID      "ESP32-VAGA-01"
   #define VAGA_ID        1
   #define MQTT_TOPIC     "smart-parking/vaga/01"

   // ---- Sensor ----
   #define PINO_TRIG      5
   #define PINO_ECHO      18
   #define LIMITE_OCUPADA_CM 150.0
   ```

   `DEVICE_ID`, `VAGA_ID` e `MQTT_TOPIC` precisam bater com um dispositivo cadastrado.
   O banco já vem com quatro:

   | `DEVICE_ID` | `VAGA_ID` | `MQTT_TOPIC` |
   |---|---:|---|
   | `ESP32-VAGA-01` | 1 | `smart-parking/vaga/01` |
   | `ESP32-VAGA-02` | 2 | `smart-parking/vaga/02` |
   | `ESP32-VAGA-03` | 3 | `smart-parking/vaga/03` |
   | `ESP32-VAGA-04` | 4 | `smart-parking/vaga/04` |

   Com mais de um kit na sala, cada um recebe uma linha diferente da tabela.

3. **Placa e porta:**
   - *Ferramentas > Placa > esp32 >* **ESP32 Dev Module**
   - *Ferramentas > Porta >* **COM4** (na faculdade o kit apareceu na COM4)

   **Como descobrir a porta em outro PC:** abra *Ferramentas > Porta* com o kit
   **desconectado**, conecte o cabo USB e veja qual COM apareceu — é essa. Também dá para
   ver no *Gerenciador de Dispositivos > Portas (COM e LPT)* (“Silicon Labs CP210x … (COM4)”
   ou “USB-SERIAL CH340 (COM4)”). O número pode mudar ao trocar de porta USB ou de PC.

4. Clique em **Carregar** (→). Se travar em `Connecting........`, segure o botão **BOOT**
   da placa até começar a gravação.
5. Abra *Ferramentas > Monitor Serial* com velocidade **115200**. O esperado:

   ```text
   === Smart Parking - sensor de vaga ===
   Dispositivo: ESP32-VAGA-01 | Vaga: 1 | Topico: smart-parking/vaga/01
   WiFi conectado. IP: 192.168.0.57
   Conectando ao broker MQTT 192.168.0.100:1883 ... MQTT conectado
   Distancia: 80.5 cm | Status: OCUPADA
   Mensagem publicada em smart-parking/vaga/01: {...}
   ```

> ⚠️ Não faça commit do `config.h` com o SSID e a senha reais. Antes do commit, volte os
> valores de exemplo ou rode
> `git update-index --skip-worktree iot/arduino-ide/esp32-vaga/config.h`.

## 6. Calibrar o sensor

Com o Monitor Serial aberto: anote a distância com a vaga **vazia** (ex.: 220 cm) e com um
objeto/carrinho na vaga (ex.: 70 cm). Coloque `LIMITE_OCUPADA_CM` entre os dois (ex.: 150)
e grave de novo. Em maquete, os valores costumam ser bem menores (ex.: vazia 30 cm,
ocupada 8 cm → limite 15).

## 7. Conferir ponta a ponta

```powershell
# mensagens chegando no broker
docker exec -it smartparking-mosquitto mosquitto_sub -t "smart-parking/#" -v

# backend processando
docker compose --profile app logs -f backend
#   MQTT [smart-parking/vaga/01] device=ESP32-VAGA-01 distancia=80.5cm status=OCUPADA vaga=1 mudou=true
```

No frontend (<http://localhost:8080>), a vaga 1 muda de status e o dispositivo aparece
**ONLINE**.

Sem o kit em mãos, o simulador publica o mesmo payload: `.\scripts\simular-sensor.ps1`.

## 8. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| Nenhuma porta COM na Arduino IDE | Driver ausente ou cabo só de carga | Instale CP210x/CH340; troque o cabo |
| `Failed to connect to ESP32` / `Connecting....` | Placa não entrou em modo de gravação | Segure **BOOT** durante o upload; confira a COM |
| `Port is busy` / acesso negado na COM | Monitor Serial ou outro programa usando a porta | Feche o Monitor Serial e reenvie |
| `Conectando ao Wi-Fi ....` infinito | SSID/senha errados ou rede 5 GHz | Corrija `WIFI_SSID`/`WIFI_PASSWORD`; use 2,4 GHz |
| `MQTT ... falhou (rc=-2)` | ESP32 não alcança o PC | `MQTT_BROKER` errado, firewall da 1883 ou rede isolada (seção 3) |
| Distância sempre 400 cm | ECHO sem sinal | Fiação/divisor do ECHO e 5 V no VCC do sensor |
| Frontend abre mas login dá erro de conexão | API ainda subindo ou endereço errado | `docker compose --profile app logs backend`; confira o campo *Endereço da API* |
| `port is already allocated` no `docker compose` | Porta ocupada no PC | Troque a porta no `.env` (seção 2) |
| `Nenhum dispositivo cadastrado para o topico` no log | `MQTT_TOPIC`/`DEVICE_ID` não cadastrados | Use uma linha da tabela da seção 5 |
| Docker: `failed to connect to the docker API` | Docker Desktop fechado | Abra o Docker Desktop e aguarde ficar *running* |
