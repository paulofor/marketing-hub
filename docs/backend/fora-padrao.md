# Endpoints backend fora do padrão atual

| Endpoint | Pacote atual | Motivo do registro | Direção de correção |
| --- | --- | --- | --- |
| `/api/business-processes` | `com.marketinghub.businessprocess` | Controller, service e DTOs ainda residem na raiz do módulo legado. | Migrar em refatoração própria para `controller`, `service` e DTOs por operação, preservando o contrato público. |

O endpoint novo `/api/business-process-execution-resources` já nasce no padrão atual, em módulo
próprio com controller único, service único, DTO de leitura por operação e repository centralizado.
