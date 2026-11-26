/*
 * Smart Parking - firmware da vaga
 * ESP32 + sensor ultrassonico HC-SR04 + LED de status + MQTT
 *
 * Montagem: Wokwi "ESP32 - Smart Parking (Versao Fisica)"
 *   https://wokwi.com/projects/481580197751757825  (pinos em config.h)
 *
 * Fluxo (secao 11 da especificacao):
 *   Conectar Wi-Fi -> Conectar MQTT -> Ler HC-SR04 -> Calcular distancia
 *   -> Verificar limite -> Definir status -> Acender/apagar LED -> Criar JSON
 *   -> Publicar -> Repetir
 *
 * A leitura do sensor e o LED nunca dependem da rede: sem Wi-Fi ou sem broker
 * a vaga continua sendo medida e sinalizada, e a reconexao e tentada em
 * segundo plano, sem travar o loop.
 *
 * Payload publicado em MQTT_TOPIC:
 *   {"deviceId":"ESP32-VAGA-01","vagaId":1,"distancia":80.5,"status":"OCUPADA"}
 */

#include <Arduino.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <WiFi.h>

#include "config.h"

static WiFiClient wifiClient;
static PubSubClient mqttClient(wifiClient);

// Client ID MQTT = DEVICE_ID + final do MAC. Em broker publico dois clientes com
// o mesmo ID derrubam um ao outro; o sufixo do MAC evita essa colisao.
static char mqttClientId[48];

// Ultimo status publicado; NULL indica que ainda nao houve publicacao.
static const char* ultimoStatus = NULL;
static unsigned long ultimaPublicacaoMs = 0;

static bool wifiConectado = false;
static unsigned long inicioTentativaWiFiMs = 0;
static unsigned long ultimaTentativaMqttMs = 0;

// Velocidade do som ~0,0343 cm/us; ida e volta -> divide por 2.
static const float CM_POR_MICROSSEGUNDO = 0.0343f / 2.0f;

// Timeout do pulseIn: ~400 cm de alcance util do HC-SR04.
static const unsigned long TIMEOUT_ECHO_US = 25000UL;

// Distancia assumida quando nao ha eco (caminho livre ate o alcance maximo).
static const float DISTANCIA_SEM_ECO_CM = 400.0f;

static const unsigned long ESPERA_INICIAL_WIFI_MS = 15000UL;
static const unsigned long NOVA_TENTATIVA_WIFI_MS = 20000UL;
static const unsigned long NOVA_TENTATIVA_MQTT_MS = 3000UL;

// ---------------------------------------------------------------- Wi-Fi -----

static void iniciarWiFi() {
  Serial.printf("Conectando ao Wi-Fi %s ...\n", WIFI_SSID);
  WiFi.disconnect();
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD, WIFI_CANAL);
  inicioTentativaWiFiMs = millis();
}

/** Informa mudancas de estado do Wi-Fi e recomeca a conexao se demorar demais. */
static bool manterWiFi() {
  bool conectado = (WiFi.status() == WL_CONNECTED);

  if (conectado && !wifiConectado) {
    Serial.print("WiFi conectado. IP: ");
    Serial.println(WiFi.localIP());
  } else if (!conectado && wifiConectado) {
    Serial.println("WiFi desconectado; tentando reconectar...");
    inicioTentativaWiFiMs = millis();
  }
  wifiConectado = conectado;

  if (!conectado && millis() - inicioTentativaWiFiMs >= NOVA_TENTATIVA_WIFI_MS) {
    iniciarWiFi();
  }
  return conectado;
}

// ----------------------------------------------------------------- MQTT -----

/** Tenta conectar ao broker no maximo a cada NOVA_TENTATIVA_MQTT_MS, sem bloquear. */
static void manterMqtt() {
  if (mqttClient.connected()) {
    mqttClient.loop();
    return;
  }

  unsigned long agora = millis();
  if (ultimaTentativaMqttMs != 0 && agora - ultimaTentativaMqttMs < NOVA_TENTATIVA_MQTT_MS) {
    return;
  }
  ultimaTentativaMqttMs = agora;

  Serial.printf("Conectando ao broker MQTT %s:%d como %s ... ", MQTT_BROKER, MQTT_PORT, mqttClientId);

  bool conectado;
  if (strlen(MQTT_USER) > 0) {
    conectado = mqttClient.connect(mqttClientId, MQTT_USER, MQTT_PASSWORD);
  } else {
    conectado = mqttClient.connect(mqttClientId);
  }

  if (conectado) {
    Serial.println("MQTT conectado");
    // Forca uma publicacao logo apos reconectar para o backend saber o estado atual.
    ultimoStatus = NULL;
  } else {
    Serial.printf("falhou (rc=%d). Nova tentativa em %lu s\n", mqttClient.state(),
                  NOVA_TENTATIVA_MQTT_MS / 1000UL);
  }
}

static void manterConexoes() {
  if (manterWiFi()) {
    manterMqtt();
  }
}

/** Espera sem bloquear a rede: o cliente MQTT continua respondendo ao broker. */
static void esperar(unsigned long ms) {
  unsigned long inicio = millis();
  while (millis() - inicio < ms) {
    if (mqttClient.connected()) {
      mqttClient.loop();
    }
    delay(50);
  }
}

// ---------------------------------------------------------------- Sensor ----

/** Uma leitura do HC-SR04 em centimetros; retorna -1 quando nao ha eco. */
static float lerDistanciaCm() {
  digitalWrite(PINO_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PINO_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PINO_TRIG, LOW);

  unsigned long duracaoUs = pulseIn(PINO_ECHO, HIGH, TIMEOUT_ECHO_US);
  if (duracaoUs == 0) {
    return -1.0f;  // sem eco: nenhum obstaculo dentro do alcance
  }
  return duracaoUs * CM_POR_MICROSSEGUNDO;
}

/**
 * Mediana de LEITURAS_POR_AMOSTRA leituras.
 *
 * O HC-SR04 produz leituras esporadicas fora da faixa; a mediana descarta esses
 * picos e evita que a vaga fique piscando entre LIVRE e OCUPADA.
 */
static float medirDistanciaCm() {
  float amostras[LEITURAS_POR_AMOSTRA];
  int validas = 0;

  for (int i = 0; i < LEITURAS_POR_AMOSTRA; i++) {
    float leitura = lerDistanciaCm();
    if (leitura > 0) {
      amostras[validas++] = leitura;
    }
    delay(60);  // o datasheet pede >= 60 ms entre disparos
  }

  if (validas == 0) {
    return -1.0f;
  }

  // Insertion sort: o vetor tem poucos elementos.
  for (int i = 1; i < validas; i++) {
    float atual = amostras[i];
    int j = i - 1;
    while (j >= 0 && amostras[j] > atual) {
      amostras[j + 1] = amostras[j];
      j--;
    }
    amostras[j + 1] = atual;
  }

  return amostras[validas / 2];
}

// ------------------------------------------------------------- Publicacao ---

static void publicar(float distanciaCm, const char* status) {
  JsonDocument doc;
  doc["deviceId"] = DEVICE_ID;
  doc["vagaId"] = VAGA_ID;
  doc["distancia"] = round(distanciaCm * 10.0f) / 10.0f;  // uma casa decimal
  doc["status"] = status;

  char payload[192];
  serializeJson(doc, payload, sizeof(payload));

  if (mqttClient.publish(MQTT_TOPIC, payload)) {
    Serial.printf("Mensagem publicada em %s: %s\n", MQTT_TOPIC, payload);
    ultimoStatus = status;
    ultimaPublicacaoMs = millis();
  } else {
    Serial.println("Falha ao publicar a mensagem MQTT");
  }
}

// --------------------------------------------------------------- Arduino ----

void setup() {
  Serial.begin(115200);
  delay(200);

  pinMode(PINO_TRIG, OUTPUT);
  pinMode(PINO_ECHO, INPUT);
  pinMode(PINO_LED, OUTPUT);
  digitalWrite(PINO_TRIG, LOW);
  digitalWrite(PINO_LED, LOW);

  uint64_t mac = ESP.getEfuseMac();
  snprintf(mqttClientId, sizeof(mqttClientId), "%s-%06lX", DEVICE_ID,
           (unsigned long)((mac >> 24) & 0xFFFFFF));

  Serial.println();
  Serial.println("=== Smart Parking - sensor de vaga ===");
  Serial.printf("Dispositivo: %s | Vaga: %d | Topico: %s\n", DEVICE_ID, VAGA_ID, MQTT_TOPIC);
  Serial.printf("Pinos: TRIG=%d ECHO=%d LED=%d | Limite: %.0f cm\n", PINO_TRIG, PINO_ECHO, PINO_LED,
                (float)LIMITE_OCUPADA_CM);

  WiFi.mode(WIFI_STA);
  iniciarWiFi();
  while (WiFi.status() != WL_CONNECTED && millis() - inicioTentativaWiFiMs < ESPERA_INICIAL_WIFI_MS) {
    delay(250);
  }

  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  manterConexoes();
}

void loop() {
  manterConexoes();

  float distancia = medirDistanciaCm();
  bool semEco = (distancia < 0);
  if (semEco) {
    // Sem eco significa caminho livre ate o limite do sensor.
    distancia = DISTANCIA_SEM_ECO_CM;
  }

  bool ocupada = (distancia <= LIMITE_OCUPADA_CM);
  const char* status = ocupada ? "OCUPADA" : "LIVRE";
  digitalWrite(PINO_LED, ocupada ? HIGH : LOW);

  Serial.printf("Distancia: %.1f cm%s | Status: %s | LED: %s\n", distancia,
                semEco ? " (sem eco)" : "", status, ocupada ? "ACESO" : "APAGADO");

  // Publica quando o status muda e, periodicamente, como heartbeat: assim o
  // backend consegue marcar o dispositivo como ONLINE mesmo sem movimentacao.
  bool statusMudou = (ultimoStatus == NULL) || (strcmp(status, ultimoStatus) != 0);
  bool heartbeat = (millis() - ultimaPublicacaoMs) >= INTERVALO_HEARTBEAT_MS;

  if (mqttClient.connected() && (statusMudou || heartbeat)) {
    publicar(distancia, status);
  }

  esperar(INTERVALO_LEITURA_MS);
}
