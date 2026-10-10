# 🚗 Smart Parking — Sistema Inteligente de Estacionamento

## 1. Visão geral

O **Smart Parking** será um sistema inteligente para gerenciamento e monitoramento de vagas de estacionamento.

A proposta integra:

- Aplicação Mobile em Kotlin Multiplatform;
- Aplicação Web em Kotlin Multiplatform;
- Aplicação Desktop em Kotlin Multiplatform;
- Backend em Spring Boot com Kotlin;
- API REST;
- PostgreSQL;
- Autenticação e autorização com JWT;
- ESP32 programado em C++;
- Sensor ultrassônico;
- MQTT;
- Broker MQTT;
- Monitoramento das vagas.

O ESP32 será responsável por detectar se uma vaga está **livre ou ocupada** utilizando um sensor de distância. Os dados serão publicados em um Broker MQTT e o Backend ficará inscrito no tópico correspondente para processar as informações.

---

# 2. Objetivo

Criar um sistema que permita:

- Cadastrar blocos;
- Cadastrar vagas;
- Cadastrar dispositivos IoT;
- Cadastrar usuários;
- Associar vagas a blocos;
- Associar dispositivos às vagas;
- Monitorar o estado das vagas;
- Receber dados dos sensores via MQTT;
- Atualizar o status das vagas automaticamente;
- Consultar vagas por bloco;
- Disponibilizar as informações para Mobile, Web e Desktop;
- Utilizar autenticação e autorização com JWT.

---

# 3. Requisitos da avaliação

O projeto deverá atender aos seguintes requisitos:

- [ ] 4 CRUDs completos;
- [ ] 2 CRUDs por integrante;
- [ ] Mobile em Kotlin Multiplatform;
- [ ] Web em Kotlin Multiplatform;
- [ ] Desktop em Kotlin Multiplatform;
- [ ] Backend em Spring Boot com Kotlin;
- [ ] API REST;
- [ ] JWT;
- [ ] Autenticação;
- [ ] Autorização;
- [ ] ESP32;
- [ ] Código C++;
- [ ] Sensor;
- [ ] Broker MQTT;
- [ ] Publicação dos dados pelo ESP32;
- [ ] Backend inscrito no tópico MQTT;
- [ ] Backend recebendo dados do ESP32;
- [ ] Persistência dos dados.

---

# 4. CRUDs obrigatórios

Como o grupo possui 2 integrantes:

```text
2 CRUDs × 2 integrantes = 4 CRUDs
```

Os quatro CRUDs serão:

1. **Blocos**
2. **Vagas**
3. **Dispositivos**
4. **Usuários**

A entidade `Veiculo` não será um CRUD obrigatório nesta primeira versão, pois o sensor ultrassônico não identifica qual veículo está ocupando a vaga. Ela poderá ser adicionada futuramente como entidade de suporte caso seja implementado RFID, QR Code ou outro mecanismo de identificação.

---

# 5. Arquitetura

```text
                         SMART PARKING
                              │
                ┌─────────────┴─────────────┐
                │                           │
             Aplicações                    IoT
                │                           │
       ┌────────┼────────┐             ┌────┴────┐
       │        │        │             │  ESP32  │
       ▼        ▼        ▼             │         │
    Mobile     Web    Desktop          │ Sensor  │
     KMP       KMP       KMP            └────┬────┘
       │        │        │                   │
       └────────┼────────┘                  MQTT
                │                            │
                │ REST                       ▼
                │                    ┌───────────────┐
                │                    │ MQTT Broker   │
                │                    │  Mosquitto    │
                │                    └───────┬───────┘
                │                            │
                │                            │ Subscribe
                │                            ▼
                │                    ┌─────────────────┐
                └───────────────────►│ Spring Boot     │
                                     │ Kotlin          │
                                     │                 │
                                     │ REST API        │
                                     │ JWT             │
                                     │ MQTT Consumer   │
                                     └────────┬────────┘
                                              │
                                              ▼
                                      ┌──────────────┐
                                      │ PostgreSQL   │
                                      └──────────────┘
```

---

# 6. Modelo de domínio

A estrutura principal do sistema será:

```text
                    SMART PARKING
                         │
             ┌───────────┼───────────┐
             │           │           │
           Bloco        Vaga     Dispositivo
             │           │           │
             │           │           │
             └───────┬───┘           │
                     │               │
                     └───────┬───────┘
                             │
                           Sensor
                             │
                           ESP32
                             │
                           MQTT
                             │
                         Backend
                             │
                          Usuário
```

Relacionamentos principais:

```text
Bloco 1 ───────── N Vagas

Vaga 1 ────────── 1 Dispositivo

Dispositivo 1 ─── N Medições

Usuário ─────────── acesso ao sistema
```

---

# 7. Funcionamento do sistema

O funcionamento principal será:

```text
1. Administrador cria um Bloco
        ↓
2. Administrador cria as Vagas
        ↓
3. Cada Vaga é associada a um Bloco
        ↓
4. Um Dispositivo ESP32 é cadastrado
        ↓
5. O Dispositivo é associado a uma Vaga
        ↓
6. ESP32 lê o sensor
        ↓
7. ESP32 identifica LIVRE ou OCUPADA
        ↓
8. ESP32 publica mensagem MQTT
        ↓
9. Broker recebe a mensagem
        ↓
10. Backend recebe a mensagem
        ↓
11. Backend atualiza o status da Vaga
        ↓
12. Banco de dados é atualizado
        ↓
13. Frontends consultam a API REST
        ↓
14. Mobile/Web/Desktop exibem o novo status
```

---

# 8. Exemplo da estrutura física

Imagine um estacionamento dividido em blocos:

```text
ESTACIONAMENTO
│
├── BLOCO A
│   ├── VAGA 01
│   ├── VAGA 02
│   ├── VAGA 03
│   └── VAGA 04
│
├── BLOCO B
│   ├── VAGA 05
│   ├── VAGA 06
│   ├── VAGA 07
│   └── VAGA 08
│
└── BLOCO C
    ├── VAGA 09
    ├── VAGA 10
    ├── VAGA 11
    └── VAGA 12
```

Cada vaga poderá possuir um dispositivo IoT responsável pelo monitoramento.

```text
Bloco A
│
├── Vaga 01
│    └── ESP32-001
│
├── Vaga 02
│    └── ESP32-002
│
├── Vaga 03
│    └── ESP32-003
│
└── Vaga 04
     └── ESP32-004
```

---

# 9. Sensor

## Sensor ultrassônico HC-SR04

O sensor sugerido é o **HC-SR04**.

Ele mede a distância entre o sensor e um objeto.

### Vaga livre

```text
HC-SR04
   │
   │ 250 cm
   │
   ▼

[          VAGA LIVRE          ]

Status:
LIVRE
```

### Vaga ocupada

```text
HC-SR04
   │
   │ 80 cm
   │
   ▼
     🚗

Status:
OCUPADA
```

O valor de distância utilizado como limite deverá ser calibrado de acordo com a instalação física.

Exemplo:

```text
Distância > 150 cm → LIVRE
Distância <= 150 cm → OCUPADA
```

O valor de `150 cm` é apenas um exemplo.

---

# 10. Reconhecimento do veículo

O sensor ultrassônico **não identifica qual veículo está na vaga**.

Ele apenas detecta se existe um objeto dentro da distância configurada.

Portanto:

```text
HC-SR04
   ↓
Existe objeto?
   ↓
SIM → OCUPADA
NÃO → LIVRE
```

O sistema não precisa identificar a placa do veículo para cumprir os requisitos da avaliação.

Caso seja desejado implementar identificação futuramente, poderão ser utilizadas tecnologias como:

- RFID;
- QR Code;
- Câmera;
- Reconhecimento de placa.

Essas funcionalidades serão consideradas extras.

---

# 11. ESP32

O ESP32 ficará responsável por:

- Conectar ao Wi-Fi;
- Conectar ao Broker MQTT;
- Ler o sensor;
- Calcular a distância;
- Determinar o estado da vaga;
- Criar o JSON;
- Publicar a mensagem MQTT;
- Repetir a leitura periodicamente.

Fluxo:

```text
ESP32
  ↓
Conectar Wi-Fi
  ↓
Conectar MQTT
  ↓
Ler HC-SR04
  ↓
Calcular distância
  ↓
Verificar limite
  ↓
Definir status
  ↓
Criar JSON
  ↓
Publicar MQTT
  ↓
Aguardar
  ↓
Repetir
```

---

# 12. MQTT

## Tópicos

Cada dispositivo poderá possuir um tópico específico.

Exemplo:

```text
smart-parking/vaga/01
smart-parking/vaga/02
smart-parking/vaga/03
smart-parking/vaga/04
```

O Backend poderá utilizar:

```text
smart-parking/vaga/+
```

para receber mensagens de todas as vagas.

---

# 13. Payload MQTT

Exemplo de vaga ocupada:

```json
{
  "deviceId": "ESP32-VAGA-01",
  "vagaId": 1,
  "distancia": 80.5,
  "status": "OCUPADA",
  "timestamp": "2026-08-14T14:30:00"
}
```

Exemplo de vaga livre:

```json
{
  "deviceId": "ESP32-VAGA-01",
  "vagaId": 1,
  "distancia": 250.8,
  "status": "LIVRE",
  "timestamp": "2026-08-14T14:35:00"
}
```

---

# 14. Broker MQTT

O projeto poderá utilizar o **Eclipse Mosquitto** como Broker MQTT.

Fluxo:

```text
ESP32
  │
  │ Publish
  ▼
Mosquitto
  │
  │ Subscribe
  ▼
Spring Boot
```

O ESP32 não enviará os dados diretamente para a API REST.

A comunicação do IoT será feita através do MQTT.

---

# 15. CRUD 1 — Blocos

## Responsável

**Integrante 1**

## Objetivo

Representar uma área ou setor do estacionamento.

## Entidade

```text
Bloco
├── id
├── nome
├── descricao
├── localizacao
└── status
```

## Exemplo

```json
{
  "nome": "Bloco A",
  "descricao": "Estacionamento Principal",
  "localizacao": "Térreo",
  "status": "ATIVO"
}
```

## Endpoints

```http
POST   /api/blocos
GET    /api/blocos
GET    /api/blocos/{id}
PUT    /api/blocos/{id}
DELETE /api/blocos/{id}
```

## Endpoint adicional

```http
GET /api/blocos/{id}/vagas
```

Exemplo de resposta:

```json
{
  "id": 1,
  "nome": "Bloco A",
  "descricao": "Estacionamento Principal",
  "vagas": [
    {
      "id": 1,
      "numero": 1,
      "status": "LIVRE"
    },
    {
      "id": 2,
      "numero": 2,
      "status": "OCUPADA"
    }
  ]
}
```

---

# 16. CRUD 2 — Vagas

## Responsável

**Integrante 1**

## Entidade

```text
Vaga
├── id
├── numero
├── tipo
├── status
├── blocoId
└── dispositivoId
```

## Tipos de vaga

```text
NORMAL
PCD
IDOSO
MOTO
```

## Status

```text
LIVRE
OCUPADA
MANUTENCAO
```

## Exemplo

```json
{
  "numero": 1,
  "tipo": "NORMAL",
  "status": "LIVRE",
  "blocoId": 1,
  "dispositivoId": 1
}
```

## Endpoints

```http
POST   /api/vagas
GET    /api/vagas
GET    /api/vagas/{id}
PUT    /api/vagas/{id}
DELETE /api/vagas/{id}
```

## Filtros úteis

```http
GET /api/vagas?status=LIVRE
GET /api/vagas?blocoId=1
GET /api/vagas?tipo=PCD
```

---

# 17. CRUD 3 — Dispositivos

## Responsável

**Integrante 2**

## Objetivo

Representar os dispositivos IoT instalados no estacionamento.

## Entidade

```text
Dispositivo
├── id
├── nome
├── macAddress
├── tipo
├── mqttTopic
├── status
└── vagaId
```

## Exemplo

```json
{
  "nome": "ESP32 Vaga 01",
  "macAddress": "AA:BB:CC:DD:EE:FF",
  "tipo": "ESP32",
  "mqttTopic": "smart-parking/vaga/01",
  "status": "ONLINE",
  "vagaId": 1
}
```

## Endpoints

```http
POST   /api/dispositivos
GET    /api/dispositivos
GET    /api/dispositivos/{id}
PUT    /api/dispositivos/{id}
DELETE /api/dispositivos/{id}
```

---

# 18. CRUD 4 — Usuários

## Responsável

**Integrante 2**

## Entidade

```text
Usuario
├── id
├── nome
├── email
├── senha
├── role
└── ativo
```

## Roles

```text
ADMIN
USER
```

## Endpoints

```http
POST   /api/usuarios
GET    /api/usuarios
GET    /api/usuarios/{id}
PUT    /api/usuarios/{id}
DELETE /api/usuarios/{id}
```

---

# 19. Autenticação JWT

Endpoint:

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "admin@smartparking.com",
  "senha": "123456"
}
```

Response:

```json
{
  "token": "JWT...",
  "type": "Bearer"
}
```

Fluxo:

```text
Mobile/Web/Desktop
        │
        │ POST /auth/login
        ▼
Spring Boot
        │
        │ valida credenciais
        ▼
      JWT
        │
        ▼
Frontend armazena token
        │
        │ Authorization: Bearer TOKEN
        ▼
API REST
```

---

# 20. Autorização

## ADMIN

Pode:

- Criar blocos;
- Editar blocos;
- Excluir blocos;
- Criar vagas;
- Editar vagas;
- Excluir vagas;
- Cadastrar dispositivos;
- Alterar dispositivos;
- Gerenciar usuários.

## USER

Pode:

- Consultar blocos;
- Consultar vagas;
- Consultar disponibilidade;
- Consultar dispositivos;
- Utilizar funcionalidades permitidas pelo sistema.

---

# 21. Banco de dados

## Users

```text
users
├── id
├── nome
├── email
├── senha_hash
├── role
└── ativo
```

## Blocos

```text
blocos
├── id
├── nome
├── descricao
├── localizacao
└── status
```

## Vagas

```text
vagas
├── id
├── numero
├── tipo
├── status
├── bloco_id
└── dispositivo_id
```

## Dispositivos

```text
dispositivos
├── id
├── nome
├── mac_address
├── tipo
├── mqtt_topic
├── status
└── vaga_id
```

## Medições

```text
medicoes
├── id
├── dispositivo_id
├── distancia
├── status
└── data_hora
```

---

# 22. Relacionamentos

```text
Bloco 1 ───────── N Vagas

Vaga 1 ────────── 1 Dispositivo

Dispositivo 1 ─── N Medições

Usuário ─────────── acessa o sistema
```

Representação:

```text
┌───────────────┐
│    BLOCO      │
└───────┬───────┘
        │ 1
        │
        │ N
┌───────▼───────┐
│     VAGA      │
└───────┬───────┘
        │ 1
        │
        │ 1
┌───────▼───────┐
│ DISPOSITIVO   │
└───────┬───────┘
        │ 1
        │
        │ N
┌───────▼───────┐
│   MEDIÇÃO     │
└───────────────┘
```

---

# 23. Medições

`Medicao` não será um dos CRUDs obrigatórios.

Ela será utilizada para registrar os dados recebidos do ESP32.

Exemplo:

```json
{
  "dispositivoId": 1,
  "distancia": 80.5,
  "status": "OCUPADA",
  "dataHora": "2026-08-14T14:30:00"
}
```

Isso permitirá criar futuramente:

- Histórico de ocupação;
- Gráficos;
- Tempo de ocupação;
- Taxa de utilização;
- Relatórios;
- Monitoramento de dispositivos.

---

# 24. Mobile

O Mobile será desenvolvido em Kotlin Multiplatform.

Funcionalidades:

```text
Login
  ↓
Dashboard
  ↓
├── Blocos
│   ├── Listar
│   ├── Criar
│   ├── Editar
│   └── Excluir
│
├── Vagas
│   ├── Listar
│   ├── Criar
│   ├── Editar
│   └── Excluir
│
├── Dispositivos
│   ├── Listar
│   ├── Criar
│   ├── Editar
│   └── Excluir
│
└── Usuários
    ├── Listar
    ├── Criar
    ├── Editar
    └── Excluir
```

---

# 25. Web

A aplicação Web funcionará como painel principal.

## Dashboard

Exemplo:

```text
SMART PARKING

Blocos: 3

Total de vagas: 50

🟢 Livres:       32
🔴 Ocupadas:     15
🟡 Manutenção:    3

Ocupação: 30%
```

Também poderá apresentar:

- Blocos;
- Vagas;
- Dispositivos;
- Usuários;
- Status em tempo real;
- Histórico;
- Informações de sensores.

---

# 26. Visualização por bloco

Exemplo:

```text
┌──────────────────────────────────────┐
│ 🅿️ BLOCO A                           │
│ Estacionamento Principal             │
├──────────────────────────────────────┤
│                                      │
│ Vaga 01 🟢 LIVRE                     │
│ Vaga 02 🔴 OCUPADA                   │
│ Vaga 03 🟢 LIVRE                     │
│ Vaga 04 🟢 LIVRE                     │
│                                      │
│ 3 livres / 1 ocupada                 │
└──────────────────────────────────────┘
```

Outro bloco:

```text
┌──────────────────────────────────────┐
│ 🅿️ BLOCO B                           │
│ Estacionamento Secundário            │
├──────────────────────────────────────┤
│                                      │
│ Vaga 05 🔴 OCUPADA                   │
│ Vaga 06 🔴 OCUPADA                   │
│ Vaga 07 🟢 LIVRE                     │
│ Vaga 08 🟢 LIVRE                     │
│                                      │
│ 2 livres / 2 ocupadas                │
└──────────────────────────────────────┘
```

---

# 27. Desktop

O Desktop poderá ser utilizado pelos operadores do estacionamento.

Funcionalidades:

- Dashboard;
- Gerenciamento de blocos;
- Gerenciamento de vagas;
- Gerenciamento de dispositivos;
- Gerenciamento de usuários;
- Visualização de ocupação;
- Histórico;
- Monitoramento dos dispositivos.

Exemplo:

```text
┌──────────────────────────────────────────┐
│ SMART PARKING - MONITORAMENTO            │
├──────────────────────────────────────────┤
│                                          │
│ BLOCO A                                  │
│                                          │
│ VAGA       STATUS       DISPOSITIVO      │
│                                          │
│ 01         🟢 LIVRE      🟢 ONLINE        │
│ 02         🔴 OCUPADA    🟢 ONLINE        │
│ 03         🟢 LIVRE      🟢 ONLINE        │
│ 04         🔴 OCUPADA    🔴 OFFLINE       │
│                                          │
└──────────────────────────────────────────┘
```

---

# 28. Fluxo completo do IoT

```text
              ┌───────────────┐
              │    Veículo    │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │    HC-SR04    │
              └───────┬───────┘
                      │
                distância
                      │
                      ▼
              ┌───────────────┐
              │     ESP32     │
              └───────┬───────┘
                      │
                    MQTT
                      │
                      ▼
              ┌───────────────┐
              │ MQTT Broker   │
              │  Mosquitto    │
              └───────┬───────┘
                      │
                  Subscribe
                      │
                      ▼
              ┌───────────────┐
              │ Spring Boot   │
              └───────┬───────┘
                      │
                Atualiza vaga
                      │
                      ▼
              ┌───────────────┐
              │  PostgreSQL   │
              └───────┬───────┘
                      │
                   REST API
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Mobile        Web       Desktop
```

---

# 29. Divisão entre integrantes

## Integrante 1

### CRUDs

- [ ] Blocos;
- [ ] Vagas.

### Responsabilidades adicionais

- [ ] Modelagem de Blocos;
- [ ] Modelagem de Vagas;
- [ ] Relacionamento Bloco → Vaga;
- [ ] Endpoints de consulta por bloco;
- [ ] Parte da interface de gerenciamento.

---

## Integrante 2

### CRUDs

- [ ] Dispositivos;
- [ ] Usuários.

### Responsabilidades adicionais

- [ ] ESP32;
- [ ] Sensor HC-SR04;
- [ ] C++;
- [ ] Wi-Fi;
- [ ] MQTT;
- [ ] Broker;
- [ ] Integração MQTT com Backend;
- [ ] JWT;
- [ ] Spring Security;
- [ ] Autorização.

---

## Responsabilidades compartilhadas

- [ ] Arquitetura;
- [ ] Banco;
- [ ] API REST;
- [ ] Integração dos frontends;
- [ ] Testes;
- [ ] Documentação;
- [ ] Deploy;
- [ ] Apresentação;
- [ ] Correção de bugs.

---

# 30. Plano de implementação

## Etapa 1 — Definição da arquitetura

### Ambos

- [ ] Definir entidades;
- [ ] Definir relacionamentos;
- [ ] Definir endpoints;
- [ ] Definir JSONs;
- [ ] Definir regras de negócio;
- [ ] Definir autenticação;
- [ ] Definir tópicos MQTT;
- [ ] Criar repositório;
- [ ] Criar branches;
- [ ] Dividir tarefas.

---

# 31. Etapa 2 — Banco de dados

- [ ] Criar banco PostgreSQL;
- [ ] Criar tabela `users`;
- [ ] Criar tabela `blocos`;
- [ ] Criar tabela `vagas`;
- [ ] Criar tabela `dispositivos`;
- [ ] Criar tabela `medicoes`;
- [ ] Criar relacionamentos;
- [ ] Criar migrations;
- [ ] Inserir dados iniciais.

---

# 32. Etapa 3 — Backend

### Integrante 1

- [ ] Criar entidade Bloco;
- [ ] Criar Repository;
- [ ] Criar Service;
- [ ] Criar Controller;
- [ ] Implementar CRUD Bloco;
- [ ] Criar entidade Vaga;
- [ ] Criar Repository;
- [ ] Criar Service;
- [ ] Criar Controller;
- [ ] Implementar CRUD Vaga;
- [ ] Implementar relacionamento Bloco → Vaga.

### Integrante 2

- [ ] Criar entidade Dispositivo;
- [ ] Criar Repository;
- [ ] Criar Service;
- [ ] Criar Controller;
- [ ] Implementar CRUD Dispositivo;
- [ ] Criar entidade Usuário;
- [ ] Criar Repository;
- [ ] Criar Service;
- [ ] Criar Controller;
- [ ] Implementar CRUD Usuário;
- [ ] Preparar autenticação.

---

# 33. Etapa 4 — Testes dos CRUDs

Cada CRUD deverá possuir:

```text
CREATE
READ
READ BY ID
UPDATE
DELETE
```

Checklist:

### Blocos

- [ ] Create;
- [ ] Read;
- [ ] Read by ID;
- [ ] Update;
- [ ] Delete.

### Vagas

- [ ] Create;
- [ ] Read;
- [ ] Read by ID;
- [ ] Update;
- [ ] Delete.

### Dispositivos

- [ ] Create;
- [ ] Read;
- [ ] Read by ID;
- [ ] Update;
- [ ] Delete.

### Usuários

- [ ] Create;
- [ ] Read;
- [ ] Read by ID;
- [ ] Update;
- [ ] Delete.

---

# 34. Etapa 5 — JWT

- [ ] Criar `/api/auth/login`;
- [ ] Gerar JWT;
- [ ] Criar filtro JWT;
- [ ] Configurar Spring Security;
- [ ] Criar roles;
- [ ] Proteger endpoints;
- [ ] Testar token válido;
- [ ] Testar token inválido;
- [ ] Testar acesso sem token;
- [ ] Testar autorização por role.

Testes esperados:

```text
Sem token → 401

Token inválido → 401

USER tentando endpoint ADMIN → 403

ADMIN → acesso permitido
```

---

# 35. Etapa 6 — ESP32

- [ ] Montar ESP32;
- [ ] Conectar HC-SR04;
- [ ] Configurar Wi-Fi;
- [ ] Configurar MQTT;
- [ ] Criar tópico;
- [ ] Ler distância;
- [ ] Definir limite;
- [ ] Determinar status;
- [ ] Criar JSON;
- [ ] Publicar mensagem;
- [ ] Testar.

---

# 36. Etapa 7 — MQTT + Backend

- [ ] Configurar cliente MQTT;
- [ ] Conectar Spring Boot ao Broker;
- [ ] Inscrever no tópico;
- [ ] Receber payload;
- [ ] Validar payload;
- [ ] Identificar dispositivo;
- [ ] Identificar vaga;
- [ ] Atualizar status da vaga;
- [ ] Registrar medição;
- [ ] Testar múltiplas vagas;
- [ ] Testar reconexão.

Fluxo:

```text
ESP32
 ↓
MQTT
 ↓
Broker
 ↓
Spring Boot
 ↓
Atualização da vaga
 ↓
PostgreSQL
```

---

# 37. Etapa 8 — Frontends

## Web

- [ ] Login;
- [ ] Dashboard;
- [ ] CRUD Blocos;
- [ ] CRUD Vagas;
- [ ] CRUD Dispositivos;
- [ ] CRUD Usuários;
- [ ] Visualização por bloco;
- [ ] Status das vagas;
- [ ] Integração REST.

## Mobile

- [ ] Login;
- [ ] Dashboard;
- [ ] CRUD Blocos;
- [ ] CRUD Vagas;
- [ ] CRUD Dispositivos;
- [ ] CRUD Usuários;
- [ ] Status das vagas;
- [ ] Integração REST.

## Desktop

- [ ] Login;
- [ ] Dashboard;
- [ ] CRUD Blocos;
- [ ] CRUD Vagas;
- [ ] CRUD Dispositivos;
- [ ] CRUD Usuários;
- [ ] Status das vagas;
- [ ] Integração REST.

---

# 38. Matriz de testes

| Funcionalidade | Backend | Web | Mobile | Desktop |
|---|---:|---:|---:|---:|
| Blocos - Create | ⬜ | ⬜ | ⬜ | ⬜ |
| Blocos - Read | ⬜ | ⬜ | ⬜ | ⬜ |
| Blocos - Update | ⬜ | ⬜ | ⬜ | ⬜ |
| Blocos - Delete | ⬜ | ⬜ | ⬜ | ⬜ |
| Vagas - Create | ⬜ | ⬜ | ⬜ | ⬜ |
| Vagas - Read | ⬜ | ⬜ | ⬜ | ⬜ |
| Vagas - Update | ⬜ | ⬜ | ⬜ | ⬜ |
| Vagas - Delete | ⬜ | ⬜ | ⬜ | ⬜ |
| Dispositivos - Create | ⬜ | ⬜ | ⬜ | ⬜ |
| Dispositivos - Read | ⬜ | ⬜ | ⬜ | ⬜ |
| Dispositivos - Update | ⬜ | ⬜ | ⬜ | ⬜ |
| Dispositivos - Delete | ⬜ | ⬜ | ⬜ | ⬜ |
| Usuários - Create | ⬜ | ⬜ | ⬜ | ⬜ |
| Usuários - Read | ⬜ | ⬜ | ⬜ | ⬜ |
| Usuários - Update | ⬜ | ⬜ | ⬜ | ⬜ |
| Usuários - Delete | ⬜ | ⬜ | ⬜ | ⬜ |
| JWT | ⬜ | ⬜ | ⬜ | ⬜ |
| MQTT | ⬜ | — | — | — |
| ESP32 | ⬜ | — | — | — |

---

# 39. Checklist geral

## Backend

- [ ] Spring Boot + Kotlin;
- [ ] API REST;
- [ ] PostgreSQL;
- [ ] 4 CRUDs;
- [ ] JWT;
- [ ] Autenticação;
- [ ] Autorização;
- [ ] MQTT;
- [ ] MQTT Consumer;
- [ ] Recebimento dos dados;
- [ ] Atualização das vagas;
- [ ] Registro das medições.

## Mobile

- [ ] Kotlin Multiplatform;
- [ ] Login;
- [ ] REST;
- [ ] CRUD Blocos;
- [ ] CRUD Vagas;
- [ ] CRUD Dispositivos;
- [ ] CRUD Usuários;
- [ ] Consulta de status.

## Web

- [ ] Kotlin Multiplatform;
- [ ] Login;
- [ ] REST;
- [ ] CRUD Blocos;
- [ ] CRUD Vagas;
- [ ] CRUD Dispositivos;
- [ ] CRUD Usuários;
- [ ] Dashboard;
- [ ] Visualização por bloco.

## Desktop

- [ ] Kotlin Multiplatform;
- [ ] Login;
- [ ] REST;
- [ ] CRUD Blocos;
- [ ] CRUD Vagas;
- [ ] CRUD Dispositivos;
- [ ] CRUD Usuários;
- [ ] Dashboard;
- [ ] Monitoramento.

## IoT

- [ ] ESP32;
- [ ] C++;
- [ ] HC-SR04;
- [ ] Wi-Fi;
- [ ] MQTT;
- [ ] Broker;
- [ ] Publicação;
- [ ] Tópico;
- [ ] Payload;
- [ ] Demonstração física.

---

# 40. Ordem recomendada

Não desenvolver tudo ao mesmo tempo.

```text
1. Definição da arquitetura
        ↓
2. Modelagem do banco
        ↓
3. Backend Spring Boot
        ↓
4. CRUDs
        ↓
5. JWT
        ↓
6. Testes da API
        ↓
7. ESP32
        ↓
8. MQTT
        ↓
9. Backend recebendo MQTT
        ↓
10. Web
        ↓
11. Mobile
        ↓
12. Desktop
        ↓
13. Testes integrados
        ↓
14. Correção de bugs
        ↓
15. Documentação
        ↓
16. Apresentação
```

---

# 41. Prioridades

Para aumentar a chance de obter a avaliação **Satisfatória**, a prioridade deverá ser:

```text
1º  CRUDs funcionando
2º  JWT funcionando
3º  API REST funcionando
4º  ESP32 funcionando
5º  MQTT funcionando
6º  Backend recebendo MQTT
7º  Web funcionando
8º  Mobile funcionando
9º  Desktop funcionando
10º Dashboard
11º Melhorias visuais
12º Funcionalidades extras
```

Não priorizar recursos extras enquanto os requisitos obrigatórios não estiverem concluídos.

---

# 42. Demonstração sugerida

A apresentação poderá demonstrar a integração completa.

## 1. Login

```text
Usuário
   ↓
Login
   ↓
JWT
```

## 2. Criar um bloco

```text
Bloco A
Nome: Estacionamento Principal
Localização: Térreo
```

## 3. Criar uma vaga

```text
Vaga 01
Tipo: NORMAL
Bloco: A
Status: LIVRE
```

## 4. Cadastrar dispositivo

```text
ESP32-VAGA-01
Vaga: 01
Status: ONLINE
MQTT:
smart-parking/vaga/01
```

## 5. Mostrar ESP32

Monitor Serial:

```text
WiFi conectado
MQTT conectado

Distancia: 250.5 cm
Status: LIVRE

Mensagem publicada
```

## 6. Colocar objeto/veículo em frente ao sensor

```text
Distancia: 80.5 cm
Status: OCUPADA

Mensagem publicada
```

## 7. Mostrar MQTT

Tópico:

```text
smart-parking/vaga/01
```

Payload:

```json
{
  "deviceId": "ESP32-VAGA-01",
  "vagaId": 1,
  "distancia": 80.5,
  "status": "OCUPADA"
}
```

## 8. Mostrar Backend

```text
MQTT MESSAGE RECEIVED

Device: ESP32-VAGA-01
Vaga: 01
Distância: 80.5 cm
Status: OCUPADA
```

## 9. Mostrar banco

```text
Vaga 01
Status = OCUPADA
```

## 10. Mostrar Web

```text
BLOCO A

Vaga 01 🔴 OCUPADA
```

## 11. Mostrar Mobile

```text
Bloco A

Vaga 01
Status: OCUPADA
```

## 12. Mostrar Desktop

```text
Bloco A

Vaga 01
Status: OCUPADA
Dispositivo: ONLINE
```

## 13. Retirar o veículo

O sensor volta a detectar uma distância maior:

```text
Distancia: 250.5 cm
Status: LIVRE
```

Todos os frontends deverão refletir:

```text
BLOCO A

VAGA 01 🟢 LIVRE
```

---

# 43. Diferencial do projeto

O Smart Parking não será apenas:

```text
4 CRUDs + ESP32
```

A ideia é demonstrar uma cadeia completa de integração:

```text
              VEÍCULO
                  ↓
               SENSOR
                  ↓
                ESP32
                  ↓
                MQTT
                  ↓
              BROKER
                  ↓
             SPRING BOOT
                  ↓
             POSTGRESQL
                  ↓
               API REST
                  ↓
       ┌──────────┼──────────┐
       ↓          ↓          ↓
    MOBILE       WEB      DESKTOP
```

O principal diferencial será demonstrar que uma alteração física no mundo real, como um veículo ocupar uma vaga, consegue percorrer toda a arquitetura do sistema e aparecer nos três aplicativos.

Além disso, a organização por **Bloco → Vaga → Dispositivo → Sensor** cria um domínio coerente e facilita a demonstração dos relacionamentos do banco.

---

# 44. Possíveis melhorias futuras

Depois que todos os requisitos obrigatórios estiverem funcionando, podem ser adicionados:

- [ ] Reserva de vagas;
- [ ] Histórico de ocupação;
- [ ] Veículos;
- [ ] Identificação por RFID;
- [ ] Identificação por QR Code;
- [ ] Reconhecimento de placas;
- [ ] Busca por placa;
- [ ] Tempo médio de ocupação;
- [ ] Dashboard com gráficos;
- [ ] Taxa de ocupação por bloco;
- [ ] Relatórios;
- [ ] Notificações;
- [ ] Monitoramento de dispositivos offline;
- [ ] Múltiplos estacionamentos;
- [ ] Múltiplos ESP32;
- [ ] Múltiplos sensores.

Essas funcionalidades são opcionais e não devem atrasar a implementação dos requisitos obrigatórios.

---

# 45. Resumo final

## Projeto

**Smart Parking — Sistema Inteligente de Estacionamento**

## Integrante 1

```text
CRUD:
- Blocos
- Vagas
```

## Integrante 2

```text
CRUD:
- Dispositivos
- Usuários
```

## Entidades de suporte

```text
- Medições
- Veículos
- Estacionamentos
```

## IoT

```text
ESP32
+
HC-SR04
+
C++
+
MQTT
```

## Backend

```text
Spring Boot
+
Kotlin
+
Spring Security
+
JWT
+
JPA
+
PostgreSQL
+
MQTT
```

## Frontends

```text
Kotlin Multiplatform

Mobile
Web
Desktop
```

## Fluxo principal

```text
Sensor
 ↓
ESP32
 ↓
MQTT
 ↓
Broker
 ↓
Spring Boot
 ↓
PostgreSQL
 ↓
API REST
 ↓
Mobile / Web / Desktop
```

## Modelo principal

```text
Bloco
  ↓
Vaga
  ↓
Dispositivo
  ↓
Sensor
  ↓
ESP32
  ↓
MQTT
  ↓
Backend
```

## Objetivo da apresentação

Demonstrar uma vaga inicialmente livre, colocar um veículo/objeto diante do sensor, mostrar o ESP32 detectando a ocupação, publicar a informação via MQTT, mostrar o Backend recebendo e atualizando o banco e, por fim, mostrar a vaga mudando de `LIVRE` para `OCUPADA` nos aplicativos Mobile, Web e Desktop.
