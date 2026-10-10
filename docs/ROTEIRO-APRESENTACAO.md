# Roteiro de apresentação

Segue a demonstração sugerida na seção 42 de `Smart_Parking_PI.md`: mostrar que uma
mudança física — um carro ocupando a vaga — percorre toda a arquitetura e aparece nos
três aplicativos.

---

## Antes de começar

Deixe rodando, em janelas separadas:

```bash
docker compose up -d                                    # PostgreSQL + Mosquitto
cd backend  && ./gradlew bootRun                        # API + consumidor MQTT
cd frontend && ./gradlew :composeApp:run                # Desktop
cd frontend && ./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web
```

Mobile: instale o APK no emulador ou no celular e confira o endereço da API na tela de login.

Monitor do broker (mostra as mensagens cruas passando):

```bash
docker exec -it smartparking-mosquitto mosquitto_sub -h localhost -t 'smart-parking/#' -v
```

Ligue o ESP32 e deixe o monitor serial aberto (`pio device monitor`).

**Plano B:** se o Wi-Fi da sala não cooperar, use `./scripts/simular-sensor.sh ocupada`
e `./scripts/simular-sensor.sh livre` — o payload é idêntico ao do ESP32, e todo o
restante da cadeia é exatamente o mesmo.

---

## 1. Login e JWT

Na Web, entre com `admin@smartparking.com` / `123456`.

Mostre o token no Swagger (`http://localhost:8085/swagger-ui.html`):

```bash
curl -X POST http://localhost:8085/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@smartparking.com","senha":"123456"}'
```

Vale demonstrar a autorização: entre como `user@smartparking.com` e mostre que o botão
"Novo bloco" some e que a aba Usuários não aparece.

## 2. Criar um bloco

Web → Blocos → **Novo bloco**: `Bloco A`, "Estacionamento Principal", Térreo, ATIVO.

## 3. Criar uma vaga

Web → Vagas → **Nova vaga**: número 1, tipo NORMAL, status LIVRE, Bloco A.

## 4. Cadastrar o dispositivo

Web → Sensores → **Novo dispositivo**:

- Nome: `ESP32-VAGA-01` (igual ao `DEVICE_ID` do firmware)
- MAC: o MAC real da placa
- Tópico: `smart-parking/vaga/01`
- Vaga: a vaga 1

Explique que é pelo tópico e pelo nome que o backend reconhece de qual sensor veio a
mensagem.

## 5. Mostrar o ESP32

No monitor serial:

```text
WiFi conectado. IP: 192.168.0.42
MQTT conectado
Distancia: 250.5 cm | Status: LIVRE
Mensagem publicada em smart-parking/vaga/01: {...}
```

## 6. Ocupar a vaga

Coloque o objeto/carrinho na frente do sensor:

```text
Distancia: 80.5 cm | Status: OCUPADA
Mensagem publicada
```

## 7. Mostrar o MQTT

Na janela do `mosquitto_sub`:

```text
smart-parking/vaga/01 {"deviceId":"ESP32-VAGA-01","vagaId":1,"distancia":80.5,"status":"OCUPADA"}
```

## 8. Mostrar o backend recebendo

No log do `bootRun`:

```text
MQTT [smart-parking/vaga/01] device=ESP32-VAGA-01 distancia=80.5cm status=OCUPADA vaga=1 mudou=true
```

## 9. Mostrar o banco

```bash
docker exec -it smartparking-postgres psql -U smartparking -d smartparking \
  -c "SELECT numero, status FROM vagas WHERE numero = 1;"
```

E o histórico que prova a persistência da leitura:

```bash
docker exec -it smartparking-postgres psql -U smartparking -d smartparking \
  -c "SELECT dispositivo_id, distancia, status, data_hora FROM medicoes ORDER BY data_hora DESC LIMIT 5;"
```

## 10, 11 e 12. Mostrar Web, Mobile e Desktop

Os três atualizam sozinhos a cada 5 segundos. A vaga 1 aparece em vermelho, `OCUPADA`,
nos três ao mesmo tempo. A aba **Leituras** mostra a medição recém-chegada com a distância.

## 13. Liberar a vaga

Retire o objeto. O sensor volta a ler ~250 cm, o status volta a `LIVRE` e os três
aplicativos ficam verdes de novo.

---

## Diferenciais que valem mencionar

- **Um único código-fonte** em `commonMain` gera Mobile, Web e Desktop.
- **Vaga em MANUTENÇÃO não é sobrescrita pelo sensor** — mostre colocando a vaga 12 em
  manutenção e publicando uma leitura nela: a medição é gravada, mas o status não muda.
- **Histórico de medições** já preparado para os relatórios da seção 44.
- **35 testes automatizados** (`cd backend && ./gradlew test`), incluindo os cenários de
  JWT e o processamento das mensagens MQTT.
- **O backend não cai se o broker sair do ar** — desligue o container do Mosquitto e mostre
  que a API continua respondendo e reconecta sozinha quando ele volta.

---

## Perguntas prováveis

**Como o sistema sabe qual carro está na vaga?**
Não sabe, e não precisa saber: o HC-SR04 detecta presença, não identidade. A seção 10 da
especificação prevê RFID, QR Code ou leitura de placa como evolução futura.

**Por que MQTT em vez de o ESP32 chamar a API REST direto?**
MQTT é publish/subscribe: o dispositivo não precisa conhecer o backend, aguentar
autenticação HTTP nem ficar tentando reenviar. O broker desacopla os dois lados e um
mesmo tópico pode ter vários consumidores.

**E se dois ESP32 publicarem ao mesmo tempo?**
O backend assina `smart-parking/vaga/+` e resolve o dispositivo pelo tópico de chegada,
então cada mensagem cai na vaga certa independentemente da ordem.
