# Exemplo genérico — bloco OpenAI Core

Este exemplo mostra como criar um novo bloco/etapa usando o padrão do módulo `openai.core.wireframe` do Worker AI, sem copiar regras específicas de wireframe ou de landing page.

## Objetivo

Usar o `StageWorker<I, O>` como orquestrador genérico para uma etapa que:

1. busca jobs pendentes no backend;
2. monta prompt, schema e request para a OpenAI;
3. despacha a requisição;
4. registra o prompt/request enviado;
5. aguarda e valida a resposta;
6. envia sucesso ou falha de volta ao backend.

## Mapa do padrão observado no módulo real

| Papel | Implementação real no wireframe | Exemplo genérico equivalente |
| --- | --- | --- |
| Orquestrador | `StageWorker<WireframeInput, WireframeOutput>` | `StageWorker<BlocoInput, BlocoOutput>` |
| Dados de entrada | `WireframeInput` | `BlocoInput` |
| Dados de saída | `WireframeOutput` | `BlocoOutput` |
| Cliente backend | `WireframeBackendClient` | `BlocoBackendClient` |
| Builder de prompt | `WireframePromptBuilder` | `BlocoPromptBuilder` |
| Validador | `WireframeResponseValidator` | `BlocoResponseValidator` |
| Handler/log | `WireframeResponseHandler` | `BlocoResponseHandler` |
| Scheduler | `WireframeExecutionScheduler` | `BlocoExecutionScheduler` |
| Propriedades | `WireframeWorkerProperties` | `BlocoWorkerProperties` |
| Configuração Spring | `WireframeWorkerConfiguration` | `BlocoWorkerConfiguration` |

## Fluxo operacional

```text
Scheduler
  -> StageWorker.processPending(limit)
    -> BackendPort.listPending(limit)
    -> PromptBuilder.build(execution)
    -> OpenAiClientPort.dispatch(request)
    -> BackendPort.markDispatched(execution, dispatch)
    -> OpenAiClientPort.awaitResult(dispatch)
    -> ResponseValidator.validateAndParse(modelResponse)
    -> ResponseHandler.handleSuccess(...)
    -> BackendPort.markCompleted(...)

Se qualquer passo falhar:
    -> ResponseHandler.handleFailure(...)
    -> BackendPort.markFailed(...)
```

## Estrutura sugerida de pacote

```text
com.marketinghub.worker.openai.core.bloco
├── BlocoBackendClient.java
├── BlocoExecutionScheduler.java
├── BlocoInput.java
├── BlocoOutput.java
├── BlocoPromptBuilder.java
├── BlocoResponseHandler.java
├── BlocoResponseValidator.java
├── BlocoWorkerConfiguration.java
└── BlocoWorkerProperties.java
```

## Contratos mínimos

### `BlocoInput`

```java
/** Responsabilidade: transportar os dados de entrada necessários para montar o prompt do bloco genérico. */
public record BlocoInput(
        Long aggregateId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    /** Normaliza os dados de prompt para evitar mapa nulo durante a renderização. */
    public BlocoInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
```

### `BlocoOutput`

```java
/** Responsabilidade: transportar o payload validado retornado pela OpenAI para o bloco genérico. */
public record BlocoOutput(
        Map<String, Object> payload
) {
    /** Normaliza o payload validado para evitar mapa nulo no envio ao backend. */
    public BlocoOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
```

### `BlocoWorkerProperties`

```java
/** Responsabilidade: concentrar as propriedades operacionais do worker OpenAI do bloco genérico. */
@Validated
@ConfigurationProperties(prefix = "bloco.worker")
public record BlocoWorkerProperties(
        boolean enabled,
        @Min(1) int pendingLimit,
        @NotBlank String backendBaseUrl,
        String apiPrefix,
        @NotBlank String promptResource,
        @NotBlank String schemaResource,
        @NotBlank String schemaName,
        @NotNull Duration timeout
) {
    /** Normaliza valores opcionais usados pelo worker do bloco quando a configuração externa omite o campo. */
    public BlocoWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
    }
}
```

## Endpoints esperados no backend

O cliente do backend deve manter o mesmo contrato conceitual do wireframe, mudando apenas o caminho do domínio/etapa:

| Operação | Método | Exemplo de rota |
| --- | --- | --- |
| Buscar pendentes | `GET` | `/api/internal/<dominio>/<bloco>/stage-executions/pending` |
| Registrar prompt/request | `POST` | `/api/internal/<dominio>/<bloco>/stage-executions/{idJob}/recebe-prompt` |
| Registrar resposta ou erro | `POST` | `/api/internal/<dominio>/<bloco>/stage-executions/{idJob}/recebe-resposta` |

### Payload de prompt enviado ao backend

```json
{
  "prompt": "prompt final renderizado",
  "promptMarkdownContent": "template markdown original",
  "schemaJson": "schema JSON do contrato de saída",
  "requestBodyJson": "corpo cru enviado à OpenAI",
  "jobidopenai": "resp_abc123"
}
```

### Payload de resposta enviado ao backend

```json
{
  "experimentId": 123,
  "stageCode": "BLOCO_GENERICO",
  "modelResponse": "{\"resultado\":{\"titulo\":\"Exemplo\"}}",
  "inputTokens": 1000,
  "outputTokens": 500,
  "costUsd": 0.01,
  "openAiJobId": "resp_abc123",
  "errorMessage": null,
  "errorDetail": null
}
```

> Observação: o campo `modelResponse` aparece como texto porque o contrato real da etapa envia a resposta bruta do modelo. Para novos contratos, prefira campos estruturados quando o backend já suportar esse formato e evite JSON serializado dentro de JSON.

## Regras importantes para adaptar o bloco

- Mantenha o acesso ao banco exclusivamente no backend; o Worker AI deve chamar apenas endpoints internos.
- Registre logs com `idJob`, identificador agregado e contexto operacional em sucesso e falha.
- Registre request cru enviado à OpenAI, resposta crua recebida e payload enviado ao backend.
- Use schema JSON estrito para reduzir variação de resposta.
- Não publique metadados técnicos no artefato final do cliente.
- Para cada nova etapa, mantenha prompt, schema, DTOs, cliente, validador e scheduler dentro do pacote da própria etapa.

## Arquivos auxiliares deste exemplo

- `exemplo-configuracao.properties`: propriedades mínimas para habilitar o bloco.
- `prompt-bloco-generico.md`: template simples com placeholders.
- `schema-bloco-generico.json`: schema JSON estrito de saída.
- `exemplo-payload-pendente.json`: exemplo de resposta do backend para `listPending`.
