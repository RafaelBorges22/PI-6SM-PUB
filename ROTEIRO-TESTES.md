# Roteiro de Testes — Smart Parking

Roteiro para executar os testes automatizados do backend e gerar o relatório de cobertura
(JaCoCo).

## 1. Ferramentas

| Ferramenta | Papel |
|---|---|
| **JUnit 5 (Jupiter)** | Framework de testes (`@Test`, `@DisplayName`, `@BeforeEach`, `@ParameterizedTest`) |
| **Mockito** | Mocks dos repositórios nos testes unitários |
| **Spring Boot Test + MockMvc** | Sobe a aplicação e chama a API REST nos testes de integração |
| **H2 (em memória)** | Banco usado nos testes — não precisa de Docker nem de PostgreSQL |
| **JaCoCo 0.8.13** | Mede a cobertura de código dos testes |

Pré-requisito: **JDK 21** com `JAVA_HOME` apontando para a pasta do JDK.

> Todos os comandos abaixo são executados **dentro da pasta `backend/`**.
> No PowerShell use `.\gradlew.bat`; no Git Bash / Linux / macOS use `./gradlew`.

## 2. Executar todos os testes

```powershell
cd backend
.\gradlew.bat test
```

Para forçar a execução mesmo sem alteração no código (o Gradle reaproveita o resultado anterior):

```powershell
.\gradlew.bat clean test
```

O relatório JaCoCo é gerado **automaticamente** ao final do `test` (`finalizedBy(jacocoTestReport)`
no `build.gradle.kts`). Para gerar só o relatório:

```powershell
.\gradlew.bat jacocoTestReport
```

## 3. Testes unitários

Testam uma classe isolada, sem subir o Spring e sem banco. As dependências são substituídas
por mocks do Mockito.

| Classe | Pacote | Testes | O que cobre |
|---|---|---:|---|
| `EntidadesUc01Test` | `domain` | 5 | Entidades do UC01 instanciadas diretamente (sem mocks) |
| `LeituraSensorServiceTest` | `mqtt` | 17 | Caminhos C1–C11 do grafo de fluxo do UC01 (V(G) = 11) |
| `VagaServiceTest` | `service` | 6 | Transições de estado da vaga (RN04 — manutenção, RN06 — sem alteração) |
| `MonitorDispositivosServiceTest` | `service` | 3 | Rotina que marca dispositivos OFFLINE (tempo limite, gravação) |

Executar todos os unitários:

```powershell
.\gradlew.bat test --tests "br.com.smartparking.api.domain.*" --tests "br.com.smartparking.api.service.*" --tests "br.com.smartparking.api.mqtt.*"
```

Executar uma classe:

```powershell
.\gradlew.bat test --tests "*LeituraSensorServiceTest"
.\gradlew.bat test --tests "*VagaServiceTest"
.\gradlew.bat test --tests "*EntidadesUc01Test"
.\gradlew.bat test --tests "*MonitorDispositivosServiceTest"
```

## 4. Testes de integração

Sobem o contexto completo do Spring Boot (`@SpringBootTest`) com H2 e fazem requisições HTTP
reais via MockMvc, passando por Spring Security, JWT, controllers, services, JPA e Flyway.
A base comum fica em `TesteBase.kt`.

| Classe | Testes | O que cobre (seção do documento) |
|---|---:|---|
| `CrudBlocoVagaTest` | 8 | CRUD de Blocos e Vagas (seção 33) |
| `CrudDispositivoUsuarioTest` | 9 | CRUD de Dispositivos e Usuários (seção 33) |
| `AutenticacaoTest` | 11 | `/signin`, `/signup`, JWT: sem token → 401, token inválido → 401, USER → 403, ADMIN → 200 (seção 34) |
| `LeituraMqttTest` | 9 | Consumo das mensagens MQTT e atualização da vaga (seção 38) |
| `RequisitosAvaliacaoTest` | 7 | Requisitos da avaliação AV1 (buscas `LIKE` etc.) |
| `MonitorDispositivosOfflineTest` | 2 | Dispositivo sem comunicação há mais de 90 s → OFFLINE; volta a ONLINE ao publicar |

Executar todos os de integração:

```powershell
.\gradlew.bat test --tests "*.api.Crud*" --tests "*.api.AutenticacaoTest" --tests "*.api.LeituraMqttTest" --tests "*.api.RequisitosAvaliacaoTest" --tests "*.api.MonitorDispositivosOfflineTest"
```

Executar uma classe ou um método:

```powershell
.\gradlew.bat test --tests "*CrudBlocoVagaTest"
.\gradlew.bat test --tests "*AutenticacaoTest"
.\gradlew.bat test --tests "*AutenticacaoTest.*401*"     # métodos cujo nome contém "401"
```

## 5. Relatórios

| Relatório | Caminho (a partir da raiz do projeto) |
|---|---|
| **Cobertura JaCoCo (HTML)** | `backend/build/reports/jacoco/test/html/index.html` |
| Cobertura JaCoCo (XML — SonarQube/CI) | `backend/build/reports/jacoco/test/jacocoTestReport.xml` |
| Resultado dos testes (HTML) | `backend/build/reports/tests/test/index.html` |
| Resultado dos testes (XML JUnit) | `backend/build/test-results/test/*.xml` |

Abrir no navegador (PowerShell, dentro de `backend/`):

```powershell
start build\reports\jacoco\test\html\index.html
start build\reports\tests\test\index.html
```

### Como ler o relatório JaCoCo

- **Verde**: linha executada pelos testes.
- **Amarelo**: decisão (`if`, `when`, `?:`) em que só parte dos ramos foi testada — passe o mouse
  no losango para ver quantos ramos faltam.
- **Vermelho**: linha nunca executada.
- Colunas principais: *Instructions* (C0), *Branches* (C1 — cobertura de decisão), *Cxty*
  (complexidade ciclomática), *Lines*, *Methods*.

> Ao executar **apenas um subconjunto** de testes (seções 3 ou 4), o relatório JaCoCo reflete
> só aquele subconjunto. Para a cobertura oficial, rode `.\gradlew.bat clean test`.

### Resultado de referência (06/11/2026)

77 testes, 0 falhas — 31 unitários + 46 de integração.

| Métrica JaCoCo | Cobertura |
|---|---:|
| Linhas | 85,3 % |
| Instruções | 80,8 % |
| Métodos | 84,0 % |
| Classes | 92,2 % |
| Complexidade | 74,0 % |
| Ramos (branches) | 52,8 % |

## 6. Testes de sistema (manuais)

Não são automatizados. Execute com o sistema completo no ar e registre o resultado.

```powershell
docker compose up -d                      # PostgreSQL + Mosquitto
cd backend; .\gradlew.bat bootRun         # API em http://localhost:8085
```

| Artefato | Uso |
|---|---|
| `PI_diagramas/Planilha_Teste_Sistema_UC01_SmartParking.xlsx` | Casos de teste de sistema do UC01 |
| `documentation/CollectionPostman.json` | Coleção Postman para testar a API manualmente |
| `scripts/simular-sensor.ps1` | Simula o ESP32 publicando no MQTT (ver `iot/CONEXAO-IOT.md`) |
| Seção 38 do `Smart_Parking_PI.md` | Matriz de testes (colunas Web, Mobile e Desktop) |
| Swagger | `http://localhost:8085/swagger-ui.html` |

## 7. Problemas comuns

| Sintoma | Solução |
|---|---|
| `JAVA_HOME is set to an invalid directory` | Aponte para a pasta do JDK 21, não para o `java.exe` |
| `No tests found for given includes` | Confira o filtro do `--tests` (diferencia maiúsculas/minúsculas) |
| Relatório não mudou | O Gradle pulou a tarefa (`UP-TO-DATE`); rode `.\gradlew.bat clean test` |
| Aviso do Mockito sobre agente dinâmico | Já silenciado com `-XX:+EnableDynamicAgentLoading` no `build.gradle.kts` |
