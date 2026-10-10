# BFF de Bloqueio de Cartões

Backend for Frontend (BFF) que recebe solicitações de bloqueio de portadores vinculados a um cartão, valida a entrada e encaminha a operação para o serviço de domínio de bloqueio.

> **Fora do escopo:** frontend, testes E2E e pipelines de CI/CD não são documentados nem implementados neste projeto. A autorização do consumidor também está fora do escopo deste scaffold (veja [Autenticação](#autenticação)).

## Sumário

- [Objetivo](#objetivo)
- [Arquitetura](#arquitetura)
- [Responsabilidades](#responsabilidades)
- [Fluxo do endpoint](#fluxo-do-endpoint)
- [Endpoint](#endpoint)
- [Autenticação](#autenticação)
- [Pré-requisitos](#pré-requisitos)
- [Configuração externa](#configuração-externa)
- [Configuração da integração](#configuração-da-integração)
- [Timeouts e tratamento de erros](#timeouts-e-tratamento-de-erros)
- [Logs](#logs)
- [Trace / correlation ID](#trace--correlation-id)
- [Execução local](#execução-local)
- [Testes](#testes)
- [Troubleshooting](#troubleshooting)

## Objetivo

Disponibilizar ao frontend um contrato próprio para criar uma **operação de bloqueio** de um ou mais portadores de um cartão. O BFF adapta o contrato, valida o formato da entrada, orquestra a chamada ao serviço de domínio e protege o consumidor de detalhes internos da integração.

O BFF **não** contém regra de negócio de bloqueio, não persiste dados e não acessa banco: essas responsabilidades pertencem ao serviço de domínio.

## Arquitetura

```text
          Consumidor (frontend / API Gateway)
                        |
                        v
+----------------------- BFF -----------------------+
|  CorrelationIdFilter     (observability)          |
|        |                                          |
|  BloqueioCartaoController        (controller)     |
|        |  BloqueioCartaoRequest (DTO de entrada)  |
|  BloqueioCartaoApplicationService (application)   |
|        |  BloqueioCartaoMapper -> BloqueioCartao  |
|  BloqueioCartaoGateway       (domain/gateway)     |
|        |  BloqueioCartaoGatewayImpl  (gateway)    |
|        |  BloqueioCartaoGatewayMapper             |
|  BloqueioCartaoClient (OpenFeign + OkHttp)        |
|                                                   |
|  GlobalExceptionHandler      (exception)          |
+------------------------|--------------------------+
                         v
            Serviço de domínio de bloqueio
```

Estrutura de pacotes (`src/main/java/br/com/empresa/bff`):

| Pacote | Conteúdo |
|---|---|
| `controller` / `controller/dto` | Endpoint REST e DTOs de entrada e saída da API. |
| `application/service` | Execução da jornada no BFF. |
| `mapper` | Conversão do DTO de entrada para o objeto de domínio. |
| `domain/model` | Objetos de domínio (`BloqueioCartao`, `Portador`, `TipoBloqueio`, `ResultadoBloqueioCartao`). |
| `domain/gateway` | Contrato (`BloqueioCartaoGateway`) da fronteira com o serviço externo. |
| `gateway` | Implementação do gateway, DTOs e mapper do contrato downstream. |
| `gateway/client` | HTTP client declarativo (OpenFeign). |
| `config/feign` | Configuração do client: transporte OkHttp, headers, retry e decodificação de erros. |
| `exception` | Contrato de erro padronizado e handler global. |
| `observability` | Correlation ID (filtro, header e chave de MDC). |

Os pacotes `security` e `validation` fazem parte da estrutura padrão do BFF e estão reservados para evolução; hoje não contêm implementação. O pacote `authorization` da estrutura padrão foi omitido porque a autorização está fora do escopo deste scaffold.

Os contratos de cada camada são separados: **DTO da API ≠ objeto de domínio ≠ DTO downstream**.

## Responsabilidades

| Componente | Responsável por | Não deve |
|---|---|---|
| `BloqueioCartaoController` | Expor `POST /operacoes/bloqueio-cartoes`, validar o payload (Bean Validation), documentar o contrato OpenAPI e responder `202`. | Conter regra de negócio ou chamar o client. |
| `BloqueioCartaoApplicationService` | Coordenar a jornada: converter para o domínio, chamar o gateway e montar a resposta. | Conhecer detalhes HTTP. |
| `BloqueioCartaoMapper` | Converter `BloqueioCartaoRequest` para `BloqueioCartao`. | — |
| `BloqueioCartaoGatewayImpl` | Converter o domínio para o contrato downstream, chamar o client e traduzir falhas de integração. | Expor detalhes do downstream ao consumidor. |
| `BloqueioCartaoClient` | Comunicação HTTP com o serviço de domínio. | Conter regra de negócio. |
| `BloqueioCartaoFeignConfig` | Transporte OkHttp, `X-Api-Key`, propagação do correlation ID, desligar retry e decodificar respostas de erro. | — |
| `GlobalExceptionHandler` | Converter exceções no contrato de erro `{code, message, traceId}`. | Retornar stack trace ou dados internos. |
| `CorrelationIdFilter` | Garantir um correlation ID por requisição, gravar no MDC, devolver no header e registrar o log de acesso. | — |

## Fluxo do endpoint

1. O `CorrelationIdFilter` obtém ou gera o `X-Correlation-ID` e o grava no MDC.
2. O `BloqueioCartaoController` desserializa e valida o `BloqueioCartaoRequest`. Payload inválido → `400`.
3. O `BloqueioCartaoApplicationService` usa o `BloqueioCartaoMapper` para criar o objeto de domínio `BloqueioCartao`.
4. O `BloqueioCartaoGatewayImpl` converte o domínio para `BloqueioCartaoDownstreamRequest` e chama o `BloqueioCartaoClient`.
5. O client envia `POST {url}{path}` ao serviço de domínio com `X-Api-Key` e `X-Correlation-ID`.
6. A resposta do downstream é convertida para `ResultadoBloqueioCartao` e depois para `BloqueioCartaoResponse`.
7. O BFF responde `202 Accepted` com o protocolo e o status da operação.
8. Em qualquer falha, o `GlobalExceptionHandler` responde com o contrato de erro padronizado (veja [Timeouts e tratamento de erros](#timeouts-e-tratamento-de-erros)).

## Endpoint

| Item | Valor |
|---|---|
| Método e rota | `POST /bff/operacoes/bloqueio-cartoes` (context path `/bff`) |
| Content-Type | `application/json` |
| Header opcional | `X-Correlation-ID` |
| Resposta de sucesso | `202 Accepted` (operação assíncrona) |
| Documentação | Swagger UI em `/bff/swagger-ui.html` e JSON em `/bff/v3/api-docs` |

### Requisição

```json
{
  "cartaoId": "123456",
  "tipoBloqueio": "DEFINITIVO",
  "motivo": "SOLICITACAO_CLIENTE",
  "portadores": [
    { "portadorId": "987654" },
    { "portadorId": "456789" }
  ]
}
```

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `cartaoId` | string | Sim | Não pode ser vazio. |
| `tipoBloqueio` | enum | Sim | `TEMPORARIO` ou `DEFINITIVO`. Outros valores retornam `400`. |
| `motivo` | string | Não | Texto livre. |
| `portadores` | array | Sim | Ao menos um portador. |
| `portadores[].portadorId` | string | Sim | Não pode ser vazio. |

### Resposta de sucesso — `202 Accepted`

```json
{
  "protocoloId": "8f6d2c10",
  "status": "PROCESSING"
}
```

`protocoloId` e `status` são repassados do serviço de domínio sem alteração.

### Resposta de erro

Todas as falhas usam o mesmo contrato, sem stack trace nem detalhes do downstream:

```json
{
  "code": "DOWNSTREAM_TIMEOUT",
  "message": "Tempo limite do serviço excedido",
  "traceId": "corr-123"
}
```

Os códigos possíveis estão em [Timeouts e tratamento de erros](#timeouts-e-tratamento-de-erros).

## Autenticação

**BFF → serviço de domínio.** Toda chamada ao downstream leva o header `X-Api-Key`, preenchido pelo próprio BFF a partir de `bloqueio-cartao.api-key` (variável `BLOQUEIO_SERVICE_API_KEY`). A chave é configurada somente no servidor: o consumidor **não** deve enviá-la.

**Consumidor → BFF.** Fora do escopo deste scaffold: o BFF não autentica nem autoriza o consumidor, e o endpoint não retorna `401` nem `403`. Um BFF derivado que precise de autorização deve aplicá-la no Application Service, antes da chamada ao Gateway, como prevê o padrão de BFF, e incluir os códigos de erro e as respostas correspondentes no contrato e no OpenAPI.

## Pré-requisitos

| Item | Versão / observação |
|---|---|
| JDK | 25 ou superior (validado no build pelo `maven-enforcer-plugin`) |
| Maven | Não precisa instalar: use o wrapper `mvnw` / `mvnw.cmd` (Maven 3.9+) |
| Acesso ao Maven Central | Para baixar dependências na primeira execução |
| Serviço de domínio e API key | Apenas para executar a aplicação contra um downstream real. **Os testes não precisam.** |

## Configuração externa

URLs, timeouts e credenciais vêm de configuração externa (`src/main/resources/application.yml`) e podem ser sobrescritos por variáveis de ambiente. Segredos não devem ser versionados: em ambientes reais, `BLOQUEIO_SERVICE_API_KEY` deve vir do mecanismo de secrets management.

| Variável | Propriedade | Padrão | Descrição |
|---|---|---|---|
| `SERVER_PORT` | `server.port` | `8080` | Porta HTTP do BFF. |
| `BLOQUEIO_SERVICE_BASE_URL` | `spring.cloud.openfeign.client.config.bloqueioCartao.url` | `http://localhost:8081` | URL base do serviço de domínio. |
| `BLOQUEIO_SERVICE_PATH` | `bloqueio-cartao.path` | `/api/v1/bloqueios-cartao` | Rota do serviço de domínio. |
| `BLOQUEIO_SERVICE_API_KEY` | `bloqueio-cartao.api-key` | **obrigatória** | Valor do header `X-Api-Key`. |
| `BLOQUEIO_SERVICE_CONNECT_TIMEOUT` | `...bloqueioCartao.connectTimeout` | `3000` | Timeout de conexão (ms). |
| `BLOQUEIO_SERVICE_TIMEOUT` | `...bloqueioCartao.readTimeout` | `5000` | Timeout de leitura (ms). |
| `ENV_HTTPCLIENT_CONFIG_DEFAULT_WRITE_TIMEOUT` | `okhttp-configuracao-geral.write-timeout-millis` | `5000` | Timeout de escrita do OkHttp (ms). |
| `ENV_OKHTTPCLIENT_POOL_MAX_IDLE_CONNECTIONS` | `okhttp-connection-pool.max-idle-connections` | `20` | Conexões ociosas mantidas no pool. |
| `ENV_OKHTTPCLIENT_POOL_KEEP_ALIVE_DURATION_SEGUNDOS` | `okhttp-connection-pool.keep-alive-duration-segundos` | `60` | Tempo de vida de uma conexão ociosa (s). |
| `ENV_OKHTTPCLIENT_DISPATCHER_MAX_REQUESTS` | `okhttp-dispatcher.max-requests` | `64` | Limite do dispatcher do OkHttp.¹ |
| `ENV_OKHTTPCLIENT_DISPATCHER_MAX_REQUESTS_PER_HOST` | `okhttp-dispatcher.max-requests-per-host` | `5` | Limite por host do dispatcher do OkHttp.¹ |
| `LOG_LEVEL_ROOT` | `logging.level.root` | `INFO` | Nível de log geral. |
| `LOG_LEVEL_BR_COM_EMPRESA` | `logging.level.br.com.empresa.bff` | `DEBUG` | Nível de log da aplicação. |
| `LOG_LEVEL_OKHTTP3` | `logging.level.okhttp3` | `OFF` | Nível de log do OkHttp. |
| `LOG_LEVEL_ORG_SPRINGFRAMEWORK` | `logging.level.org.springframework` | `INFO` | Nível de log do Spring. |

¹ Os limites do dispatcher só valem para chamadas assíncronas do OkHttp. O Feign faz chamadas síncronas, então esses valores **não** limitam a concorrência com o downstream.

### Profile `local`

O profile `local` (`application-local.yml`) define `BLOQUEIO_SERVICE_API_KEY` como `dummy-key` quando a variável não está presente, permitindo subir a aplicação sem credencial real. Não use esse profile fora do desenvolvimento.

## Configuração da integração

O `BloqueioCartaoClient` é uma interface OpenFeign registrada com o nome `bloqueioCartao` e configurada por `BloqueioCartaoFeignConfig`:

| Aspecto | Comportamento |
|---|---|
| Transporte | OkHttp com pool de conexões próprio (`feign-okhttp`). |
| Requisição | `POST {BLOQUEIO_SERVICE_BASE_URL}{BLOQUEIO_SERVICE_PATH}` com corpo JSON. |
| Headers enviados | `X-Api-Key` (sempre) e `X-Correlation-ID` (quando existe no MDC, ou seja, em toda requisição recebida pelo BFF). |
| Retry | Desligado (`Retryer.NEVER_RETRY` e `retryOnConnectionFailure: false`): uma requisição ao BFF gera exatamente uma chamada ao downstream. |
| Respostas de erro | O `ErrorDecoder` converte qualquer status ≥ 400 em `DownstreamIntegrationException`, preservando o status original. |

Contrato enviado ao serviço de domínio:

```json
{
  "cartaoId": "123456",
  "tipoBloqueio": "DEFINITIVO",
  "motivo": "SOLICITACAO_CLIENTE",
  "portadores": [{ "portadorId": "987654" }]
}
```

Contrato esperado do serviço de domínio (qualquer status 2xx):

```json
{ "protocoloId": "8f6d2c10", "status": "PROCESSING" }
```

## Timeouts e tratamento de erros

| Timeout | Padrão | Variável |
|---|---|---|
| Conexão | 3000 ms | `BLOQUEIO_SERVICE_CONNECT_TIMEOUT` |
| Leitura | 5000 ms | `BLOQUEIO_SERVICE_TIMEOUT` |
| Escrita | 5000 ms | `ENV_HTTPCLIENT_CONFIG_DEFAULT_WRITE_TIMEOUT` |

Mapeamento de falhas para o consumidor:

| Situação | Status | `code` |
|---|---|---|
| Payload inválido, JSON malformado ou enum desconhecido | `400` | `VALIDATION_ERROR` |
| Timeout de conexão ou leitura, ou downstream respondeu `504` | `504` | `DOWNSTREAM_TIMEOUT` |
| Falha de conexão (recusada, reset, DNS), ou downstream respondeu `503` | `503` | `DOWNSTREAM_UNAVAILABLE` |
| Downstream respondeu outro erro (4xx/5xx) ou uma resposta ilegível | `502` | `DOWNSTREAM_ERROR` |
| Falha inesperada no BFF | `500` | `INTERNAL_ERROR` |

Um `401` ou `403` vindo do **downstream** (por exemplo, API key inválida) é tratado como erro de integração e retorna `502`, não `401`/`403`.

## Logs

Os logs vão para o console no formato:

```text
%d{yyyy-MM-dd HH:mm:ss} [%X{correlationId}] [%thread] %-5level %logger %kvp - %msg%n
```

O campo entre colchetes é o correlation ID da requisição, e `%kvp` imprime os atributos estruturados do evento. A aplicação registra:

| Evento | Nível | Origem | Atributos |
|---|---|---|---|
| Fim de cada requisição HTTP | `INFO` | `CorrelationIdFilter` | `event=http_request_completed`, `method`, `status`, `traceId`, `durationMs` |
| Requisição rejeitada com erro | `WARN` (4xx) / `ERROR` (5xx) | `GlobalExceptionHandler` | `code`, `status`, `traceId` |

Exemplo de uma chamada com o downstream fora do ar:

```text
2026-10-09 23:06:51 [corr-123] [http-nio-8080-exec-2] ERROR br.com.empresa.bff.exception.GlobalExceptionHandler code="DOWNSTREAM_UNAVAILABLE" status="503" traceId="corr-123" - Requisição rejeitada
2026-10-09 23:06:51 [corr-123] [http-nio-8080-exec-2] INFO  br.com.empresa.bff.observability.CorrelationIdFilter event="http_request_completed" method="POST" status="503" traceId="corr-123" durationMs="35" - Requisição finalizada
```

Os logs não incluem o payload da requisição, a API key nem o corpo das respostas de erro do downstream. O status de erro do downstream não é registrado: para investigar uma falha de integração, procure o `traceId` nos logs do serviço de domínio.

## Trace / correlation ID

| Etapa | Comportamento |
|---|---|
| Entrada | O `CorrelationIdFilter` lê o header `X-Correlation-ID`. Valores aceitos: até 128 caracteres entre letras ASCII, números, `.`, `_` e `-`. Se o header estiver ausente ou for inválido, o BFF gera um UUID. |
| Contexto | O valor é gravado no MDC com a chave `correlationId` e aparece em todas as linhas de log da requisição. |
| Downstream | O interceptor do Feign envia o mesmo valor no header `X-Correlation-ID`. |
| Resposta | O BFF devolve o header `X-Correlation-ID` em todas as respostas, inclusive de erro. |
| Erro | O mesmo valor aparece no campo `traceId` do contrato de erro. |

Para rastrear uma requisição de ponta a ponta, filtre os logs do BFF e do serviço de domínio pelo mesmo correlation ID.

## Execução local

Comandos em PowerShell, a partir da raiz do projeto. No Linux/macOS, use `./mvnw` no lugar de `./mvnw.cmd`.

### 1. Gerar o pacote

```powershell
./mvnw.cmd clean package
```

### 2. Executar

Com um serviço de domínio real:

```powershell
$env:BLOQUEIO_SERVICE_BASE_URL="http://localhost:8081"
$env:BLOQUEIO_SERVICE_API_KEY="<sua-api-key>"
java -jar target\bradesco-0.0.1-SNAPSHOT.jar
```

Sem credencial real, para explorar a API e o OpenAPI:

```powershell
java -jar target\bradesco-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Também é possível executar sem gerar o jar: `./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`.

A aplicação sobe em `http://localhost:8080/bff`:

- Swagger UI: `http://localhost:8080/bff/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/bff/v3/api-docs`

### 3. Chamar o endpoint

O corpo é enviado a partir de um arquivo, o que funciona igual no Windows PowerShell 5.1 e no PowerShell 7 (as duas versões tratam aspas de forma diferente quando o JSON vai direto na linha de comando):

```powershell
@'
{"cartaoId":"123456","tipoBloqueio":"DEFINITIVO","motivo":"SOLICITACAO_CLIENTE","portadores":[{"portadorId":"987654"}]}
'@ | Set-Content -Encoding ascii bloqueio.json

curl.exe -i -X POST "http://localhost:8080/bff/operacoes/bloqueio-cartoes" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-ID: corr-123" `
  -d "@bloqueio.json"
```

Sem um serviço de domínio na URL configurada, a resposta esperada é `503 DOWNSTREAM_UNAVAILABLE`, com o header `X-Correlation-ID: corr-123` e `"traceId":"corr-123"` no corpo.

## Testes

Os testes seguem os níveis do padrão BFF e **não dependem de nenhum serviço externo**: o downstream é simulado pelo WireMock em porta dinâmica, e nenhuma variável de ambiente é necessária.

| Nível | Pacote (`src/test/java/br/com/empresa/bff`) | Ferramentas | O que valida |
|---|---|---|---|
| Unitário | `unit` | JUnit 5, Mockito | Application Service, mappers, gateway (tradução de falhas), configuração do Feign, exception handler. |
| Componente | `component` | `@WebMvcTest`, MockMvc | Controller: status HTTP, validação, serialização/desserialização, contrato de erro, correlation ID. |
| Integração | `integration` | `@SpringBootTest` + WireMock | BFF real → HTTP client → downstream simulado: método, rota, headers, payload, `202`, erros HTTP, timeout, falha de conexão, ausência de retry e contrato OpenAPI. |

### Comandos

```powershell
# Todos os testes + relatório e validação de cobertura + regras de build
./mvnw.cmd clean verify

# Somente um nível (o filtro precisa usar "/" e não ".")
./mvnw.cmd test "-Dtest=br/com/empresa/bff/unit/**"
./mvnw.cmd test "-Dtest=br/com/empresa/bff/component/**"
./mvnw.cmd test "-Dtest=br/com/empresa/bff/integration/**"

# Uma classe específica
./mvnw.cmd test "-Dtest=BloqueioCartaoIntegrationTest"
```

Resultados por classe: `target\surefire-reports\`.

### Cobertura (JaCoCo)

Qualquer execução de testes gera o relatório em `target\site\jacoco\`:

- `index.html`: relatório navegável;
- `jacoco.xml`: formato consumido pelo Sonar;
- `jacoco.csv`: resumo por classe.

O `verify` falha se a cobertura ficar abaixo do mínimo:

| Métrica | Mínimo | Propriedade no `pom.xml` |
|---|---|---|
| Linhas | 80% | `jacoco.line.minimum` |
| Branches | 80% | `jacoco.branch.minimum` |

A classe `BradescoApplication` (bootstrap do Spring) fica fora da medição.

### Regras de build

O `maven-enforcer-plugin` falha o build se:

- o JDK for anterior ao 25 ou o Maven anterior ao 3.9;
- houver dependência declarada em duplicidade no `pom.xml`;
- houver dependência de persistência (JPA, JDBC, Hibernate), pois o BFF não acessa banco.

## Troubleshooting

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| A aplicação não sobe: `Could not resolve placeholder 'BLOQUEIO_SERVICE_API_KEY'` | `BLOQUEIO_SERVICE_API_KEY` não definida. | Defina a variável ou, só em desenvolvimento, use `--spring.profiles.active=local`. |
| A aplicação não sobe: `Port 8080 was already in use` | Outra aplicação na porta. | Defina `SERVER_PORT` com outra porta. |
| Build falha em `RequireJavaVersion` | JDK anterior ao 25. | Instale o JDK 25 e ajuste `JAVA_HOME`. |
| Build falha em `jacoco:check` (`Coverage checks have not been met`) | Cobertura abaixo do mínimo. | Abra `target\site\jacoco\index.html` e cubra as linhas e branches indicados. |
| `-Dtest=br.com.empresa.bff.unit.**` executa todos os testes | Filtro com `.` não é reconhecido como pacote. | Use `/`: `-Dtest=br/com/empresa/bff/unit/**`. |
| `400 VALIDATION_ERROR` | Campo obrigatório ausente, lista de portadores vazia, `tipoBloqueio` inválido ou JSON malformado. | Confira as regras em [Requisição](#requisição). Para campo ausente ou vazio, a `message` indica o campo; para `tipoBloqueio` inválido ou JSON malformado, a mensagem é genérica (`Erro de validação`). |
| `503 DOWNSTREAM_UNAVAILABLE` | Serviço de domínio fora do ar, URL errada, ou o downstream respondeu `503`. | Verifique `BLOQUEIO_SERVICE_BASE_URL`, a disponibilidade do serviço, proxy e firewall. |
| `504 DOWNSTREAM_TIMEOUT` | O downstream não respondeu dentro do timeout, ou respondeu `504`. | Verifique a latência do serviço antes de aumentar `BLOQUEIO_SERVICE_TIMEOUT` / `BLOQUEIO_SERVICE_CONNECT_TIMEOUT`. |
| `502 DOWNSTREAM_ERROR` | O downstream respondeu um erro (inclusive `401`/`403` por API key inválida, ou `404` por path errado) ou um JSON fora do contrato. | Confira `BLOQUEIO_SERVICE_API_KEY` e `BLOQUEIO_SERVICE_PATH`, e procure o `traceId` nos logs do serviço de domínio. |
| `500 INTERNAL_ERROR` | Falha inesperada no BFF. | Procure o `traceId` nos logs do BFF. |
| Não encontro a requisição nos logs do downstream | Busca feita pelo ID errado. | Use o valor do header `X-Correlation-ID` (ou do `traceId` do erro): é o mesmo enviado ao downstream. |
