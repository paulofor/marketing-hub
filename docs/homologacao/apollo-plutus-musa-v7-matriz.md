# Matriz de homologação — Apolo + Plutus no MUSA v7

| Dimensão | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | projeto com perfil e script aprovado, teto, objetivo e critério definidos e Plutus aprova | um job `TEST` vinculado ao ciclo, sem publicação |
| Reaproveitamento | ativos existentes bastam para a montagem | Apolo conclui candidato sem abrir nova tentativa paga |
| Aprendizado | objetivo ou critério de sucesso ausente | formulário e backend bloqueiam o novo ciclo antes de Plutus |
| Validação | projeto sem perfil | ciclo recusado antes da fila |
| Segurança | agente diferente de Plutus decide | HTTP 403 e nenhum job |
| Financeiro | Plutus rejeita | `FINANCIAL_BLOCKED`, custo zero e nenhum job |
| Idempotência | segunda decisão no mesmo ciclo | HTTP 409 e nenhum job adicional |
| Integração | provider falha | falha e custo ficam no ledger; ciclo não publica |
| Observabilidade | sucesso ou falha | ciclo, tarefa, job, timestamps, provider e custo correlacionáveis |
| Métricas | CTA, checkout e venda | somente eventos reais; geração não conta como resultado comercial |
| Mobile | vídeo candidato no iPhone e Pixel | controles, legendas, cortes e legibilidade aprovados pelo QA independente |
| Desktop | vídeo candidato no Chromium | reprodução, áudio e layout sem falhas |
| Segregação | dois produtos/planos | ciclo e custos não cruzam produto nem planejamento |
| Ledger incremental | custos históricos e nova tentativa coexistem | snapshot de Plutus separa por `videoProductionCycleId` e soma somente o custo novo |

Uma rodada local completa sem defeitos conclui a homologação. Caso haja correção, executar duas rodadas completas consecutivas sem falhas após o último ajuste.
