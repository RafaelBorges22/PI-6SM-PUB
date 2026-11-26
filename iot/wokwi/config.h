// Smart Parking - configuracao do dispositivo PARA O SIMULADOR WOKWI.
// Cole como aba "config.h" no projeto do Wokwi (guia: iot/wokwi/LEIA-ME.md).
//
//
// Montagem de referencia (Wokwi "ESP32 - Smart Parking (Versao Fisica)"):
//   https://wokwi.com/projects/481580197751757825
//
//   HC-SR04 VCC  -> 5V da ESP32        HC-SR04 GND -> GND
//   HC-SR04 TRIG -> GPIO 5
//   HC-SR04 ECHO -> [1 kOhm] -+-> GPIO 18      (divisor: 5 V -> ~3,3 V)
//                             +-[2 kOhm]-> GND
//   GPIO 19 -> [220 Ohm] -> LED vermelho (anodo); catodo -> GND
//
// Cada ESP32 instalado no estacionamento recebe uma copia deste arquivo com o
// seu proprio DEVICE_ID, VAGA_ID e MQTT_TOPIC. Os valores devem bater com o
// dispositivo cadastrado em POST /api/dispositivos.

#ifndef CONFIG_H
#define CONFIG_H

// ---------------------------------------------------------------- Wi-Fi -----
// Rede 2,4 GHz (a ESP32 nao conecta em 5 GHz). No simulador Wokwi use
// "Wokwi-GUEST", senha "" e canal 6.
#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASSWORD ""
#define WIFI_CANAL 6  // canal do Wokwi-GUEST (conecta mais rapido)

// ----------------------------------------------------------------- MQTT -----
// Escolha UMA das opcoes e use o mesmo broker/topico no backend:
//
//  A) Mosquitto do docker compose (rede local; exige porta 1883 liberada no PC):
//       MQTT_BROKER "192.168.0.100"  (IP do PC no ipconfig, nunca "localhost")
//       MQTT_TOPIC  "smart-parking/vaga/01"
//
//  B) Broker publico (notebook sem acesso ao firewall, rede isolada ou Wokwi).
//     ESP32 e backend so fazem conexoes de saida:
//       MQTT_BROKER "broker.hivemq.com"
//       MQTT_TOPIC  "spk-TROQUE-AQUI/vaga/01"  (prefixo aleatorio do grupo)
//     Backend: MQTT_BROKER=tcp://broker.hivemq.com:1883 e
//              MQTT_TOPICO=spk-TROQUE-AQUI/vaga/+
#define MQTT_BROKER "broker.hivemq.com"  // opcao B: o Wokwi nao alcanca o PC
#define MQTT_PORT 1883
#define MQTT_USER ""      // vazio = broker anonimo (padrao do docker-compose)
#define MQTT_PASSWORD ""

// ------------------------------------------------------------ Identidade -----
#define DEVICE_ID "ESP32-VAGA-01"            // igual ao campo "nome" do dispositivo
#define VAGA_ID 1                             // id da vaga monitorada
#define MQTT_TOPIC "spk-TROQUE-AQUI/vaga/01"  // prefixo igual ao MQTT_TOPICO do backend

// ------------------------------------------------------------------ Pinos ----
#define PINO_TRIG 5    // TRIG do HC-SR04 (ligacao direta)
#define PINO_ECHO 18   // ECHO do HC-SR04, SEMPRE pelo divisor 1k/2k
#define PINO_LED 19    // LED vermelho com resistor de 220 Ohm: aceso = OCUPADA

// ---------------------------------------------------------- Sensor HC-SR04 --
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
