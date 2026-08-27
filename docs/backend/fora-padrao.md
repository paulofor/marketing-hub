# Endpoints backend fora do padrão atual

| Endpoint | Pacote atual | Motivo do registro | Direção de correção |
| --- | --- | --- | --- |
| `/api/business-processes` | `com.marketinghub.businessprocess` | Controller, service e DTOs ainda residem na raiz do módulo legado. | Migrar em refatoração própria para `controller`, `service` e DTOs por operação, preservando o contrato público. |
| `/api/agents/{id}` | `com.marketinghub.agent.web` | Controller legado em `.web` e DTO mutável fora do subpacote da operação. | A leitura consolidada passa a usar `/api/agents/{agentId}/details`, no módulo canônico `agentdetail`; manter a rota antiga apenas para compatibilidade até a refatoração dos fluxos de edição. |

O endpoint novo `/api/business-process-execution-resources` já nasce no padrão atual, em módulo
próprio com controller único, service único, DTO de leitura por operação e repository centralizado.
