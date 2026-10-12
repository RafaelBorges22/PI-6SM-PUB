/*
 * Smart Parking - firmware da vaga
 * ESP32 + sensor ultrassonico HC-SR04 + MQTT
 *
 * Fluxo (secao 11 da especificacao):
 *   Conectar Wi-Fi -> Conectar MQTT -> Ler HC-SR04 -> Calcular distancia
 *   -> Verificar limite -> Definir status -> Criar JSON -> Publicar -> Repetir
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

// Ultimo status publicado; NULL indica que ainda nao houve publicacao.
static const char* ultimoStatus = NULL;
static unsigned long ultimaPublicacaoMs = 0;

// Velocidade do som ~0,0343 cm/us; ida e volta -> divide por 2.
static const float CM_POR_MICROSSEGUNDO = 0.0343f / 2.0f;

// Timeout do pulseIn: ~400 cm de alcance util do HC-SR04.
static const unsigned long TIMEOUT_ECHO_US = 25000UL;

// ---------------------------------------------------------------- Wi-Fi -----

static void conectarWiFi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  Serial.printf("Conectando ao Wi-Fi %s", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.print("WiFi conectado. IP: ");
  Serial.println(WiFi.localIP());
}

// ----------------------------------------------------------------- MQTT -----

static void conectarMqtt() {
  while (!mqttClient.connected()) {
    Serial.printf("Conectando ao broker MQTT %s:%d ... ", MQTT_BROKER, MQTT_PORT);

    bool conectado;
    if (strlen(MQTT_USER) > 0) {
      conectado = mqttClient.connect(DEVICE_ID, MQTT_USER, MQTT_PASSWORD);
    } else {
      conectado = mqttClient.connect(DEVICE_ID);
    }

    if (conectado) {
      Serial.println("MQTT conectado");
      // Forca uma publicacao logo apos reconectar para o backend saber o estado atual.
      ultimoStatus = NULL;
    } else {
      Serial.printf("falhou (rc=%d). Nova tentativa em 3s\n", mqttClient.state());
      delay(3000);
    }
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
  digitalWrite(PINO_TRIG, LOW);

  Serial.println();
  Serial.println("=== Smart Parking - sensor de vaga ===");
  Serial.printf("Dispositivo: %s | Vaga: %d | Topico: %s\n", DEVICE_ID, VAGA_ID, MQTT_TOPIC);

  conectarWiFi();
  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  conectarMqtt();
}

void loop() {
  conectarWiFi();
  if (!mqttClient.connected()) {
    conectarMqtt();
  }
  mqttClient.loop();

  float distancia = medirDistanciaCm();
  if (distancia < 0) {
    // Sem eco significa caminho livre ate o limite do sensor.
    distancia = 400.0f;
  }

  const char* status = (distancia <= LIMITE_OCUPADA_CM) ? "OCUPADA" : "LIVRE";
  Serial.printf("Distancia: %.1f cm | Status: %s\n", distancia, status);

  // Publica quando o status muda e, periodicamente, como heartbeat: assim o
  // backend consegue marcar o dispositivo como ONLINE mesmo sem movimentacao.
  bool statusMudou = (ultimoStatus == NULL) || (strcmp(status, ultimoStatus) != 0);
  bool heartbeat = (millis() - ultimaPublicacaoMs) >= INTERVALO_HEARTBEAT_MS;

  if (statusMudou || heartbeat) {
    publicar(distancia, status);
  }

  delay(INTERVALO_LEITURA_MS);
}
