# Contrato de submissão/engajamento — Lead Portal (v1)

Versão: `lead-portal-submission-engagement.v1`

## Objetivo

Padronizar os eventos públicos recebidos pelo Lead Portal e os callbacks internos enviados ao backend de marketing, cobrindo:

- render completo do formulário;
- submit do formulário;
- campos obrigatórios para submit;
- comportamento idempotente e tratamento de falha.

## Endpoints públicos do Lead Portal

- Render completo: `POST /api/public/lead-portal/flows/{slug}/render-complete`
- Submit: `POST /api/public/lead-portal/flows/{slug}/submission`

## Callbacks internos para o Marketing Hub

- Render completo: `POST /api/internal/lead-portal/flows/{slug}/render-complete`
- Submit: `POST /api/internal/lead-portal/flows/{slug}/submission`
- Analytics de página: `POST /api/internal/lead-portal/flows/{slug}/page-analytics`

O front público, lead ou cliente nunca deve chamar o backend principal do Marketing Hub diretamente. Ele chama o Lead Portal; o Lead Portal encaminha eventos internos ao Marketing Hub apenas para métricas, funil e auditoria.

## Payload de submit (v1)

```json
{
  "contractVersion": "lead-portal-submission-engagement.v1",
  "slug": "flow-slug",
  "submissionId": "f8fd3f64-e15b-4c61-b4cf-53f44a251001",
  "submittedAt": "2026-04-11T10:15:30Z",
  "contato": {
    "nome": "Maria Silva",
    "email": "maria@exemplo.com",
    "telefone": "+55 11 99999-0000"
  },
  "campaignCode": "meta-campanha-001",
  "idempotencyKey": "f8fd3f64-e15b-4c61-b4cf-53f44a251001"
}
```

### Campos obrigatórios (submit)

- `slug`
- `submissionId`
- `submittedAt`
- `contato.nome`
- `contato.email`

## Idempotência e falhas

- O backend trata `submissionId` como chave idempotente do evento de submit no funil.
- Reenvios com o mesmo `submissionId` retornam `status=duplicate` sem duplicar evento.
- Erros de validação retornam 4xx e precisam ser corrigidos pelo caller.
- Erros transitórios (5xx/rede) devem usar retry com o mesmo `idempotencyKey`.

## Metadados de publicação de fluxo

Ao publicar o fluxo para o runtime público, o payload inclui:

- `engagementContract.version`
- `engagementContract.renderCompleteEndpoint`
- `engagementContract.submissionEndpoint`
- `engagementContract.idempotencyField`
- `engagementContract.submissionRequiredFields`

Esses metadados permitem o runtime validar versão e obrigatoriedade sem depender de JavaScript embarcado no HTML customizado.
