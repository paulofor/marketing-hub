# Bloco backend — exemplo genérico baseado em `geralanding.wireframe`

Este exemplo documenta um bloco backend genérico inspirado no módulo real `com.marketinghub.geralanding.wireframe`.
Ele deve ser usado como referência operacional para criar uma etapa backend do GeraLanding que:

- abre uma execução manual para um experimento;
- expõe uma fila interna de jobs iniciados para o Worker AI;
- recebe do Worker AI o prompt/schema/request enviados à IA;
- recebe a resposta final ou erro da IA;
- lista e detalha execuções para a tela administrativa;
- persiste o artefato final no campo correto do experimento.

> O exemplo é intencionalmente genérico e não deve ser compilado diretamente. Ao criar uma etapa real, substitua `bloco`, `Bloco`, `landing-page-bloco` e o campo de artefato pelo nome canônico da etapa.

## Estrutura sugerida

```text
backend/ads-service/src/main/java/com/marketinghub/geralanding/bloco/
├── web/
│   └── BackendBlocoController.java
└── service/
    ├── BackendBlocoService.java
    ├── GeraLandingBlocoStartResponse.java
    ├── detailStageExecution/
    │   └── RecordBackendBlocoDetalheDto.java
    ├── listStageExecutions/
    │   └── GeraLandingBlocoExecutionSummaryResponse.java
    ├── pending/
    │   ├── RecordBlocoExperiment.java
    │   ├── RecordBlocoHypothesis.java
    │   └── RecordBlocoPending.java
    ├── recebePrompt/
    │   └── RecebePromptRequest.java
    └── recebeResposta/
        └── RecebeRespostaRequest.java
```

## Contrato HTTP do bloco

Use sempre o código canônico da etapa na constante `STAGE_CODE`.

| Operação | Método e rota | Uso |
| --- | --- | --- |
| Iniciar etapa | `POST /api/experiments/{experimentId}/geralanding/bloco/start` | Cria execução com status `INICIADO`. |
| Listar execuções | `GET /api/experiments/{experimentId}/geralanding/bloco/stage-executions?includeCompleted=true` | Lista até 20 execuções mais recentes da etapa. |
| Listar fila interna | `GET /api/internal/geralanding/bloco/stage-executions/pending` | Worker AI busca jobs `INICIADO`. |
| Registrar prompt enviado | `POST /api/internal/geralanding/bloco/stage-executions/{idJob}/recebe-prompt` | Marca execução como `AGUARDANDO_RETORNO_OPENAI`. |
| Registrar resposta | `POST /api/internal/geralanding/bloco/stage-executions/{idJob}/recebe-resposta` | Conclui ou falha a execução e persiste o artefato. |
| Detalhar execução | `GET /api/experiments/{experimentId}/geralanding/bloco/stage-executions/{idJob}` | Retorna prompt, request, resposta, HTML provisório, métricas e erro. |

## Estados mínimos

- `INICIADO`: execução aberta pelo backend e pronta para ser consumida pelo Worker AI.
- `AGUARDANDO_RETORNO_OPENAI`: Worker AI já montou o prompt/request e despachou para a OpenAI.
- `CONCLUIDO`: Worker AI retornou resposta sem erro; o backend salvou o artefato no experimento.
- `FALHA`: Worker AI retornou `errorMessage` ou `errorDetail`; o backend registrou erro sem sobrescrever o artefato final.

## Fluxo raiz

1. A tela administrativa chama `start` para criar uma execução rastreável.
2. O Worker AI consulta `pending` e recebe o experimento, hipótese e artefatos anteriores necessários.
3. O Worker AI envia `recebe-prompt` com prompt, schema, request final e identificador do job OpenAI.
4. O Worker AI envia `recebe-resposta` com a resposta do modelo, métricas e erro quando houver.
5. O backend atualiza a execução e, apenas em caso de sucesso, persiste o artefato final no experimento.
6. A tela usa `listStageExecutions` e `detailStageExecution` para auditoria operacional.

## Regras de implementação

- Mantenha controller, service e records dentro do pacote da etapa para respeitar o isolamento por módulo.
- Use comentários de responsabilidade em toda classe Java e comentários breves em todos os métodos criados ou alterados.
- Não grave artefato final quando o callback contiver erro.
- Registre logs com contexto operacional em callbacks internos: `idJob`, `experimentId`, `stageCode`, `openAiJobId` e presença de erro.
- Preserve JSON estruturado nos objetos de fila sempre que possível; evite transformar artefatos em texto quando o consumidor precisa de objeto.
- Ao criar etapa real, adicione testes unitários para controller e service cobrindo início, fila, callbacks, persistência de sucesso e falha.

## Arquivos incluídos neste exemplo

- `BackendBlocoController.java.exemplo`: template do controller HTTP da etapa.
- `BackendBlocoService.java.exemplo`: template do serviço transacional da etapa.
- `Records.java.exemplo`: conjunto compacto dos records/DTOs usados pelo controller e pelo service.
- `contratos-http.md`: exemplos de chamadas e payloads mínimos para auditoria do fluxo.
