# 🚗 Smart Parking — Sistema Inteligente de Estacionamento

| |                                                                           |
|---|---------------------------------------------------------------------------|
| **Projeto** | Smart Parking — Sistema Inteligente de Estacionamento                     |
| **Grupo** | `Rafael Mascarenhas Borges` `Guilherme Souza Alves`                       |
| **Disciplina** | Laboratório de Desenvolvimento Multiplataforma — FATEC (DSM, 6º semestre) |
| **Repositório** | <https://github.com/RafaelBorges22/PI-6SM-PUB>                                |

## Integrantes e responsabilidades

| Integrante | Entidades (backend + frontend) |
|---|---|
| `<NOME DO INTEGRANTE 1>` | **Bloco** e **Vaga** |
| `<NOME DO INTEGRANTE 2>` | **Dispositivo** e **Usuário** (e a autenticação `/signin` / `/signup`) |

Cada integrante implementou, para as suas entidades: entidade JPA, repositório (com a
consulta JPQL `LIKE`), DTOs de request/response, mapper, serviço, controller REST e a tela
de formulário no frontend.

## O que a aplicação faz

Em estacionamentos de shoppings, faculdades e empresas, o motorista perde tempo
circulando atrás de uma vaga livre e a administração não sabe, em tempo real, quantas
vagas estão ocupadas, quais setores lotam primeiro ou se algum sensor parou de responder.

O Smart Parking resolve isso com um sensor ultrassônico (HC-SR04) ligado a um ESP32 em
cada vaga. O ESP32 publica o estado da vaga (livre/ocupada) via MQTT; o backend consome
essas mensagens, atualiza a vaga e guarda o histórico de medições. Os aplicativos Mobile,
Web e Desktop — um único código em Kotlin Multiplatform — mostram o mapa de vagas por
bloco, o dashboard de ocupação e permitem administrar blocos, vagas, dispositivos e
usuários. As melhorias trazidas são:

- ocupação visível em tempo real, por bloco e por tipo de vaga (PCD, idoso, moto);
- detecção de sensores offline pela data da última comunicação;
- controle de acesso por perfil (ADMIN administra, USER apenas consulta);
- histórico de medições para análise de uso do estacionamento.

```text
Veículo → HC-SR04 → ESP32 → MQTT → Mosquitto → Spring Boot → PostgreSQL → API REST → Mobile / Web / Desktop
```

## Tecnologias utilizadas

| Camada | Tecnologias |
|---|---|
| Backend | Kotlin 2.2, Spring Boot 3.5 (Spring Web, Spring Data JPA, Validation, Spring Security), JWT (jjwt 0.12), Flyway, springdoc-openapi (Swagger) |
| Banco de dados | PostgreSQL 16 (H2 em memória nos testes) |
| Frontend | Kotlin Multiplatform + Compose Multiplatform (Android, Desktop/JVM e Web/Wasm), Ktor Client, kotlinx.serialization |
| IoT | ESP32 em C++ (PlatformIO), sensor HC-SR04, MQTT (Eclipse Paho + Mosquitto) |
| Infra | Docker Compose, Gradle |

## Camadas da arquitetura

### Backend — `backend/src/main/kotlin/br/com/smartparking/api/`

| Camada | Pasta | Conteúdo |
|---|---|---|
| Entidades (modelo JPA) | `domain/` | `Bloco`, `Vaga`, `Dispositivo`, `Usuario`, `Medicao`, enums |
| Repositórios | `repository/` | interfaces `JpaRepository` com consultas JPQL `LIKE` |
| DTOs | `web/dto/` | `XxxRequest` (entrada, com Bean Validation) e `XxxResponse` (saída) |
| Mappers | `mapper/` | `BlocoMapper`, `VagaMapper`, `DispositivoMapper`, `UsuarioMapper` (DTO ↔ entidade) |
| Serviços | `service/` | regras de negócio e CRUD (criar, listar, buscar por id, atualizar, remover) |
| Controllers REST | `web/` | `BlocoController`, `VagaController`, `DispositivoController`, `UsuarioController`, `AuthController` |
| Segurança | `security/` | `JwtUtil` (gera/valida token), `JwtAuthenticationFilter`, `SecurityConfig` (PasswordEncoder, SecurityFilterChain, regras de autorização) |
| Configuração | `config/` | seed de usuários, OpenAPI |
| Integração IoT | `mqtt/` | assinante MQTT e processamento das leituras |
| Erros | `common/` + `web/GlobalExceptionHandler.kt` | corpo padrão de erro |
| Migrations | `src/main/resources/db/migration/` | scripts Flyway |

### Frontend — `frontend/composeApp/src/`

| Camada | Pasta | Conteúdo |
|---|---|---|
| Modelos (entidades) | `commonMain/.../app/model/` | `Bloco`, `Vaga`, `Dispositivo`, `Usuario` e DTOs |
| Dados / API | `commonMain/.../app/data/` | `SmartParkingApi` (cliente REST), `AppStore` (estado) |
| Telas (UI) | `commonMain/.../app/ui/` | telas de lista + formulário de cada entidade (botões **Gravar** e **Limpar Campos**), login, dashboard |
| Plataformas | `androidMain/`, `desktopMain/`, `wasmJsMain/` | ponto de entrada de cada alvo |

## Atendimento à avaliação (AV1)

| Item | Onde |
|---|---|
| 1a — 2 entidades por integrante, ≥ 5 campos, ≥ 3 tipos, `@Column(length)` | `domain/` — ex.: `Bloco` (String, Int, Boolean), `Vaga` (Int, Double, String), `Dispositivo` (String, LocalDate, LocalDateTime), `Usuario` (String, Boolean, LocalDateTime) |
| 1b — JPQL com `LIKE` em cada repositório | `repository/Repositories.kt` (`buscarPorNome`, `buscarPorObservacao`, `buscarPorNomeDoDispositivo`); rotas `GET /api/<recurso>/buscar` |
| 1c — Serviços com CRUD | `service/` |
| 1d — Controllers GET (todos / por id), POST, PUT, DELETE | `web/` |
| 1e — `/signin` e `/signup` com JWT | `web/AuthController.kt` → `POST /api/auth/signin`, `POST /api/auth/signup` |
| 1f — Utilitário JWT + Spring Security | `security/JwtUtil.kt`, `security/JwtAuthenticationFilter.kt`, `security/SecurityConfig.kt` |
| 1g — DTO de request e response + mapper | `web/dto/`, `mapper/` |
| 1h — ≥ 2 regras de Bean Validation por DTO de request | `web/dto/` (`@NotBlank`, `@Size`, `@Email`, `@Min`, `@PositiveOrZero`, `@DecimalMin`, `@PastOrPresent`, `@Pattern`...) |
| 2a — Entidades no frontend (≥ 5 propriedades, ≥ 2 tipos) | `frontend/.../model/Models.kt` |
| 2b — Formulário por entidade com **Gravar** e **Limpar Campos** | `frontend/.../ui/BlocosScreen.kt`, `VagasScreen.kt`, `DispositivosScreen.kt`, `UsuariosScreen.kt` (componente `DialogoFormulario` em `Componentes.kt`) |

---

## Estrutura do repositório

```text
PI-6SM/
├── backend/              API REST em Spring Boot + Kotlin (CRUDs, JWT, consumidor MQTT)
├── frontend/             Kotlin Multiplatform: um código-fonte para Mobile, Web e Desktop
├── iot/esp32-vaga/       Firmware C++ do ESP32 com o sensor HC-SR04
├── infra/mosquitto/      Configuração do broker MQTT
├── scripts/              Simulador de sensor e automação para subir o app Mobile
├── docs/                 API, checklist da avaliação e roteiro de apresentação
└── docker-compose.yml    PostgreSQL + Mosquitto
```

---

## Pré-requisitos

| Ferramenta | Versão | Para quê |
|---|---|---|
| JDK | 21 | Backend e frontends |
| Docker Desktop | atual | PostgreSQL e Mosquitto |
| Android Studio / SDK | API 35+ | Aplicativo Mobile (o AVD usado no projeto é API 36) |
| PlatformIO ou Arduino IDE | atual | Gravar o firmware no ESP32 |

O Gradle não precisa ser instalado: cada projeto traz o seu wrapper (`gradlew`).

Para o Mobile, o SDK do Android precisa ter os pacotes **platform-tools** (`adb`) e
**emulator**, e pelo menos um AVD criado em *Tools > Device Manager*. O script de
subida procura o SDK em `ANDROID_HOME`, depois `ANDROID_SDK_ROOT`, e por último em
`%LOCALAPPDATA%\Android\Sdk`.

---

## Portas usadas

| Porta | Serviço | Onde muda |
|---|---|---|
| **8085** | API REST do backend | `SERVER_PORT` ou `backend/src/main/resources/application.yml` |
| 5432 | PostgreSQL | `POSTGRES_PORT` no `docker compose` |
| 1883 | Mosquitto (MQTT) | `infra/mosquitto/` |
| 5554 / 5555 | Console e adb do emulador Android | fixas, do emulador |

> **Atenção à porta da API.** O padrão do projeto é **8085**, e todo o repositório
> já aponta para ela — inclusive o campo de endereço na tela de login dos frontends
> e os exemplos em [`docs/API.md`](docs/API.md). Se o login falhar com erro de
> conexão, confira a porta no campo antes de qualquer outra investigação. Para
> rodar em outra porta, suba o backend com `$env:SERVER_PORT = "8080"`.

---

## Instalação, execução e acesso

### 1. Infraestrutura

```bash
docker compose up -d
```

Sobe o PostgreSQL em `localhost:5432` (banco/usuário/senha `smartparking`) e o
Mosquitto em `localhost:1883`.

Se a porta 5432 já estiver ocupada por um PostgreSQL local:

```bash
POSTGRES_PORT=5433 docker compose up -d
# e suba o backend apontando para ela:
DB_URL=jdbc:postgresql://127.0.0.1:5433/smartparking ./gradlew bootRun
```

Use `127.0.0.1` e não `localhost` na `DB_URL` — no Windows com Docker Desktop,
`localhost` resolve `::1` primeiro e trava o handshake do Postgres.

### 2. Backend

```bash
cd backend
./gradlew bootRun          # Windows: .\gradlew.bat bootRun
```

A API sobe em <http://localhost:8085>. Na primeira execução o Flyway cria o schema,
insere 3 blocos, 12 vagas e 4 dispositivos, e o `DataSeeder` cria os usuários:

| Email | Senha | Perfil |
|---|---|---|
| `admin@smartparking.com` | `123456` | ADMIN |
| `user@smartparking.com` | `123456` | USER |

> Essas senhas são de laboratório. Troque-as antes de qualquer uso real.

Documentação interativa: <http://localhost:8085/swagger-ui.html>

### 3. Frontends

Todos partem do mesmo código em `frontend/composeApp/src/commonMain`.

```bash
cd frontend

./gradlew :composeApp:run                          # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun  # Web (abre no navegador)
./gradlew :composeApp:assembleDebug                # Mobile (APK em composeApp/build/outputs/apk/debug)
./gradlew :composeApp:installDebug                 # Mobile (instala direto no emulador aberto)
```

#### Abrir o emulador (pré-requisito do `installDebug`)

`installDebug` só instala em um device já conectado — sem isso ele falha com
`No connected devices!`. Já `assembleDebug` passa mesmo sem emulador, porque só gera o
APK. Para subir o emulador, um comando — **execute no PowerShell**, não no Prompt de
Comando (`cmd.exe`): o operador de chamada `&` e a sintaxe `$env:` são do PowerShell e
o `cmd` não entende nenhum dos dois.

Comando para executar diretamente no emulador caso não funcione o passo anterior
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd SmartParking_API36
```

Deixe essa janela aberta e rode o `installDebug` em outro terminal. Confira antes com
`adb devices`: precisa aparecer `emulator-5554  device` (se vier `offline`, o Android
ainda está bootando). O caminho vale para o SDK padrão do Android Studio; se o seu SDK
estiver em outro lugar, use `$env:ANDROID_HOME` no lugar de `$env:LOCALAPPDATA\Android\Sdk`,
e veja os AVDs disponíveis com `emulator -list-avds`.

#### Endereço da API na tela de login

O campo de endereço fica na própria tela de login e vale para os três frontends:

| Onde o app roda | Endereço da API |
|---|---|
| Emulador Android | `http://10.0.2.2:8085` |
| Desktop (`jvm`) e Web (`wasmJs`) | `http://localhost:8085` |
| Celular físico na mesma rede | `http://SEU-IP:8085` (veja com `ipconfig`) |

Duas regras que economizam tempo:

- **Sem `/api` no final.** O cliente já monta os caminhos (`SmartParkingApi.kt` chama
  `${baseUrl}/api/auth/signin`). Com `/api` no campo, a chamada vira `/api/api/...` e
  responde 404.
- **`10.0.2.2` é o host visto de dentro do emulador.** `localhost` lá dentro aponta
  para o próprio Android, não para a sua máquina.

O campo já vem preenchido com a porta **8085**, definida em `Plataforma.android.kt`,
`Plataforma.desktop.kt` e `Plataforma.wasmJs.kt`. Se você subir o backend em outra
porta, corrija o campo na tela de login ou ajuste esses três arquivos.

Em celular físico, além do IP, o Firewall do Windows precisa liberar a porta 8085 para
entrada. O tráfego HTTP puro já está liberado no app (`usesCleartextTraffic="true"` no
`AndroidManifest.xml`).

#### Mobile no emulador, em um comando (Windows)

O script `scripts/subir-mobile.ps1` faz a sequência inteira: acha o SDK, escolhe o AVD,
avisa se a API não está no ar, sobe o emulador, espera o Android bootar, compila,
instala o APK e abre o app.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\subir-mobile.ps1
```

O `-ExecutionPolicy Bypass` é necessário porque a política padrão do Windows
(`Restricted`) recusa scripts `.ps1`. Para poder chamar `.\scripts\subir-mobile.ps1`
direto, libere uma vez só para o seu usuário:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

Parâmetros:

| Parâmetro | Para quê |
|---|---|
| `-Avd SmartParking_API36` | Escolhe o emulador. Sem ele, usa o primeiro da lista (`emulator -list-avds`). |
| `-PortaBackend 8080` | Porta que o script checa antes de instalar. Padrão: 8085. |
| `-PularBackendCheck` | Instala sem reclamar que a API está fora do ar. |
| `-ReaproveitarSnapshot` | Sobe pelo snapshot (quick boot), ~30s mais rápido. |
| `-TimeoutBootSegundos 300` | Aumenta a espera do boot em máquina lenta. |

É seguro reexecutar: se o emulador já estiver aberto e saudável, ele é reaproveitado;
se estiver travado em `offline`, o script derruba e sobe de novo.

Por padrão o emulador sobe em **boot frio**, ignorando o snapshot `default_boot`. Custa
uns 30s a mais e evita o travamento descrito em [Solução de problemas](#solução-de-problemas).

O script não gerencia a infra nem o backend — suba os dois antes, em outro terminal:

```powershell
docker compose up -d
cd backend; .\gradlew bootRun
```

### 4. ESP32

1. Abra `iot/esp32-vaga/` no PlatformIO.
2. Edite `src/config.h` com o SSID/senha do Wi-Fi, o IP do broker e o `DEVICE_ID`/`MQTT_TOPIC`
   correspondentes ao dispositivo cadastrado na API.
3. Ligue o HC-SR04: `TRIG` no GPIO 5, `ECHO` no GPIO 18 (via divisor de tensão para 3,3 V),
   `VCC` em 5 V e `GND` no comum.
4. `pio run -t upload && pio device monitor`.

### 5. Sem a placa em mãos

O simulador publica o mesmo payload que o ESP32 publicaria:

```bash
./scripts/simular-sensor.sh              # alterna LIVRE/OCUPADA na vaga 01 a cada 5s
./scripts/simular-sensor.sh ocupada      # uma única leitura OCUPADA
```

No Windows: `.\scripts\simular-sensor.ps1`.

---

## Decisões de implementação

**O vínculo vaga ↔ dispositivo tem uma única coluna.** A especificação (seção 21) lista
`vagas.dispositivo_id` e `dispositivos.vaga_id`. Manter as duas colunas permitiria que elas
se contradissessem. A implementação mantém apenas `vagas.dispositivo_id` (com `UNIQUE`), e a
API continua aceitando e devolvendo `vagaId` no dispositivo — o backend sincroniza o outro
lado. Nenhum contrato da especificação mudou.

**Vaga em MANUTENÇÃO não é sobrescrita pelo sensor.** `MANUTENCAO` é um estado
administrativo; se o sensor pudesse alterá-lo, bastaria um carro estacionar para a vaga
voltar a ser considerada utilizável. A medição continua sendo registrada no histórico.

**Os usuários iniciais são criados em código, não em migration.** O hash BCrypt precisa vir
do mesmo `PasswordEncoder` usado no login, então o `DataSeeder` cria o ADMIN e o USER na
primeira execução, apenas se a tabela estiver vazia.

**O backend sobe mesmo sem o broker.** Se o Mosquitto estiver fora do ar, a falha é
registrada em log e a API REST continua atendendo; o cliente Paho reconecta sozinho e
reassina o tópico.

**`dispositivos.ultima_comunicacao` é um campo a mais.** Não estava na especificação, mas
é o que permite o monitoramento de dispositivos offline citado na seção 44.

---

## Testes

```bash
cd backend && ./gradlew test
```

44 testes de integração cobrindo os 4 CRUDs, `/signin` e `/signup`, as buscas `LIKE`, autenticação/autorização JWT e o consumo das
mensagens MQTT. Rodam sobre H2 em memória e não precisam de Docker.

---

## Solução de problemas

**`ERROR: JAVA_HOME is set to an invalid directory`**

O `JAVA_HOME` precisa apontar para a *pasta* do JDK, não para o `java.exe`. No PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"   # vale só para a janela atual
setx JAVA_HOME "C:\Program Files\Java\jdk-21"     # permanente (reabra o terminal)
```

**`Bind for 0.0.0.0:5432 failed: port is already allocated`**

Já existe um PostgreSQL usando a porta. Suba na 5433 e aponte o backend para ela:

```powershell
$env:POSTGRES_PORT = "5433"
docker compose up -d
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5433/smartparking"
```

**Frontend não conecta na API**

Confira o endereço na tela de login — na esmagadora maioria das vezes é a **porta**: o
campo e o backend precisam bater na **8085**. Desktop e Web usam
`http://localhost:8085`; o emulador Android precisa de `http://10.0.2.2:8085`; celular
físico precisa do IP da máquina (`ipconfig`), da porta liberada no firewall e do backend
acessível na mesma rede. E o endereço vai **sem** `/api` no final.

**`.ps1 não pode ser carregado porque a execução de scripts foi desabilitada`**

Política padrão do Windows (`Restricted`). Rode o script pelo executável:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\subir-mobile.ps1
```

Ou libere de uma vez para o seu usuário: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

**Emulador fica em `device offline` e o app nunca instala**

Sintoma: a janela do emulador abre, mas o terminal repete sem parar
`adb command '... getprop sys.boot_completed' failed: 'adb.exe: device offline'`.

O emulador (a VM) subiu, mas o Android dentro dele não — o `adbd` do sistema convidado
nunca responde. A causa quase sempre é o snapshot de quick boot (`default_boot`)
corrompido, salvo de um emulador que foi fechado à força.

O script já trata isso: ele sobe em boot frio por padrão e derruba emulador travado antes
de tentar de novo. Se mesmo assim acontecer, apague o snapshot com o emulador **fechado**:

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.android\avd\SEU_AVD.avd\snapshots"
```

Reiniciar o adb (`adb kill-server`) **não** resolve este caso: o problema está no
convidado, não no adb. Para conferir o estado real: `adb devices` deve mostrar `device`,
não `offline`.

**Backend trava ao conectar no PostgreSQL (Windows + Docker Desktop)**

Use `127.0.0.1` em vez de `localhost` na `DB_URL`. No Windows, `localhost` resolve `::1`
primeiro e cai no `wslrelay` do Docker Desktop, que trava o handshake. É por isso que o
`application.yml` já vem com `jdbc:postgresql://127.0.0.1:5432/smartparking`.

**`Connection refused` no broker ao subir o backend**

O Mosquitto não está no ar. A API sobe assim mesmo (só o consumo MQTT fica inativo);
rode `docker compose up -d` e reinicie o backend.

---

## Documentação adicional

- [`docs/API.md`](docs/API.md) — endpoints, payloads e exemplos com `curl`
- [`docs/CHECKLIST.md`](docs/CHECKLIST.md) — situação de cada item exigido na avaliação
- [`docs/ROTEIRO-APRESENTACAO.md`](docs/ROTEIRO-APRESENTACAO.md) — passo a passo da demonstração
