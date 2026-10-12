// Smart Parking - configuracao do dispositivo.
//
// Cada ESP32 instalado no estacionamento recebe uma copia deste arquivo com o
// seu proprio DEVICE_ID, VAGA_ID e MQTT_TOPIC. Os valores devem bater com o
// dispositivo cadastrado em POST /api/dispositivos.

#ifndef CONFIG_H
#define CONFIG_H

// ---------------------------------------------------------------- Wi-Fi -----
#define WIFI_SSID "NOME-DA-REDE"
#define WIFI_PASSWORD "SENHA-DA-REDE"

// ----------------------------------------------------------------- MQTT -----
// IP da maquina que roda o Mosquitto (nao use "localhost": o ESP32 e outro host).
#define MQTT_BROKER "192.168.0.100"
#define MQTT_PORT 1883
#define MQTT_USER ""      // vazio = broker anonimo (padrao do docker-compose)
#define MQTT_PASSWORD ""

// ------------------------------------------------------------ Identidade -----
#define DEVICE_ID "ESP32-VAGA-01"            // igual ao campo "nome" do dispositivo
#define VAGA_ID 1                             // id da vaga monitorada
#define MQTT_TOPIC "smart-parking/vaga/01"   // igual ao campo "mqttTopic" do dispositivo

// ---------------------------------------------------------- Sensor HC-SR04 --
#define PINO_TRIG 5
#define PINO_ECHO 18

// Limite de deteccao em centimetros (secao 9 da especificacao).
// Calibrar conforme a altura de instalacao do sensor:
//   distancia <= LIMITE_OCUPADA_CM  -> OCUPADA
//   distancia >  LIMITE_OCUPADA_CM  -> LIVRE
#define LIMITE_OCUPADA_CM 150.0

// -------------------------------------------------------------- Cadencia ----
#define INTERVALO_LEITURA_MS 2000     // intervalo entre leituras do sensor
#define INTERVALO_HEARTBEAT_MS 30000  // republica mesmo sem mudanca de status
#define LEITURAS_POR_AMOSTRA 5        // mediana de N leituras para filtrar ruido

#endif  // CONFIG_H
