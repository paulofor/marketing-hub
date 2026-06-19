

## 2026-06-19 — OPRM NichoCNAE versão 2

- **Backend:** `backend/ads-service`.
- **Pacote protegido:** `com.marketinghub.oprm.nichocnae.v2.candidategenerator`.
- **Endpoint pending canônico:** `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/pending`.
- **Aplicação:** protocolo padrão backend aplicado para a etapa inicial `candidate-generator`, com controller canônico, service canônico, contrato `record` em subpacote de service e regra ArchUnit dedicada em `ArquiteturaTest`.
