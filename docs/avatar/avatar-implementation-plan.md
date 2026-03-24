# Plano de Implementação — Avatar

## 1) Objetivo

Implementar a funcionalidade de **Avatar por tenant** com geração/ingestão de imagem, armazenamento no backend, orquestração via Worker IA e consumo no frontend com rollout progressivo e controle de custo.

---

## 2) Backlog priorizado

## MVP (entrega mínima utilizável)

1. **Backend — modelo e API base de avatar por tenant**
   - Criar entidade `tenant_avatar` (tenant_id, asset_id, status, source_type, modelo, prompt, created_at, updated_at).
   - Expor APIs:
     - `POST /api/tenants/{tenantId}/avatar` (criação/solicitação)
     - `GET /api/tenants/{tenantId}/avatar` (consulta)
     - `DELETE /api/tenants/{tenantId}/avatar` (remoção e fallback para default)
   - Validar `prompt` obrigatório e `modelo` quando origem for IA.

2. **Worker IA — pipeline de geração e otimização**
   - Reutilizar pipeline de otimização de imagem já existente.
   - Consumir solicitação de avatar via fila/evento.
   - Persistir resultado no backend via upload de asset + vínculo ao tenant.

3. **Frontend — configuração e visualização**
   - Tela de configuração de avatar por tenant com upload e geração IA.
   - Exibir status (`PENDING`, `PROCESSING`, `READY`, `FAILED`) e motivo de falha.
   - Tooltip explicativo nos campos que acionam Worker IA.

4. **Plataforma — observabilidade inicial + feature flag global**
   - Feature flag: `avatar.enabled` (default OFF).
   - Métricas básicas: latência, taxa de erro, custo por geração.

## V1 (escala controlada)

1. **Backend**
   - Versionamento de avatar (histórico dos últimos N avatares).
   - Endpoint de retry: `POST /api/tenants/{tenantId}/avatar/{avatarId}/retry`.

2. **Worker IA**
   - Retry automático com backoff para falhas transitórias.
   - Suporte a provedor secundário de modelo (fallback).

3. **Frontend**
   - Histórico com comparação lado a lado.
   - Ação de restaurar versão anterior.

4. **Plataforma**
   - Rate limit por tenant.
   - Dashboards de erro/custo por ambiente e tenant.

## V2 (otimização e governança)

1. **Backend**
   - Políticas por plano (quota mensal, tamanho máximo, retenção).
   - Auditoria completa de eventos de avatar.

2. **Worker IA**
   - Classificação de segurança da imagem gerada (policy gate).
   - Cache semântico de prompts para reduzir custo.

3. **Frontend**
   - Assistente de prompt com templates por segmento.
   - Alertas proativos de consumo de quota.

4. **Plataforma**
   - Autoscaling por fila de geração.
   - FinOps: budget alerts + projeção de custo mensal.

---

## 3) Decomposição por sprint

> Planejamento sugerido para sprints de 2 semanas.

## Sprint 1 — Fundacional (MVP parte 1)

### Responsáveis por frente
- **Backend:** Squad Core API
- **Worker:** Squad IA Pipeline
- **Frontend:** Squad Experience
- **Plataforma:** SRE/DevOps

### APIs/contratos a entregar
- Contrato de domínio `TenantAvatar` (status, origem, metadata de IA).
- `POST /api/tenants/{tenantId}/avatar` com payload:
  ```json
  {
    "sourceType": "UPLOAD|AI",
    "assetId": "optional-when-upload-already-done",
    "prompt": "required when sourceType=AI",
    "model": "optional but recommended when sourceType=AI"
  }
  ```
- `GET /api/tenants/{tenantId}/avatar` retornando status e URL assinada do asset quando `READY`.

### Migrations necessárias
- `tenant_avatar`.
- Índice único por `(tenant_id, active)` para garantir 1 avatar ativo.
- Colunas obrigatórias para rastreabilidade de IA: `modelo`, `prompt`.

### Dependências
- Serviço de assets disponível no backend.
- Fila/event bus para assíncrono do worker.
- Configuração inicial de feature flag.

### Métricas de sucesso
- P95 `POST /avatar` < 300ms (apenas enfileirar).
- Tempo médio `PENDING -> READY` < 90s.
- Taxa de erro de processamento < 5%.

### Risco principal
- Gargalo no upload/processamento de imagem gerando filas longas.

### Plano de rollback
- Desligar `avatar.enabled` globalmente.
- Interromper consumidores de fila do worker.
- Manter fallback para avatar default sem remover dados já criados.

### Critérios de conclusão
- APIs MVP publicadas e documentadas.
- Worker processando fluxo feliz fim a fim.
- Frontend exibindo upload/geração + status.
- Métricas e logs mínimos ativos.

---

## Sprint 2 — Robustez operacional (MVP parte 2 + V1 parcial)

### Responsáveis por frente
- **Backend:** Squad Core API
- **Worker:** Squad IA Pipeline
- **Frontend:** Squad Experience
- **Plataforma:** SRE/DevOps

### APIs/contratos a entregar
- `DELETE /api/tenants/{tenantId}/avatar`.
- `POST /api/tenants/{tenantId}/avatar/{avatarId}/retry`.
- Contrato de erro padronizado com códigos de falha de geração (`MODEL_TIMEOUT`, `UNSAFE_CONTENT`, `IMAGE_TOO_LARGE`, etc.).

### Migrations necessárias
- `tenant_avatar_history` para versionamento.
- Campo `failure_code` + `failure_detail` em `tenant_avatar`.

### Dependências
- Catálogo de erros compartilhado backend/worker/frontend.
- Política de retry e DLQ no broker.

### Métricas de sucesso
- Redução de falhas finais (FAILED sem recuperação) para < 2%.
- Sucesso de retry manual > 60% em erros transitórios.
- MTTR de incidentes de avatar < 30 min.

### Risco principal
- Inconsistência entre status no backend e estado real do processamento no worker.

### Plano de rollback
- Desabilitar endpoint de retry via flag `avatar.retry.enabled`.
- Voltar leitura para última versão estável do avatar.
- Drenar e pausar reprocessamentos em lote.

### Critérios de conclusão
- Histórico e retry funcionando em produção controlada.
- Erros padronizados propagando corretamente até UI.
- Alertas operacionais cobrindo backlog de fila e custo anômalo.

---

## Sprint 3 — Escala por tenant (V1 completo)

### Responsáveis por frente
- **Backend:** Squad Core API
- **Worker:** Squad IA Pipeline
- **Frontend:** Squad Experience
- **Plataforma:** SRE/DevOps + FinOps

### APIs/contratos a entregar
- Endpoint de listagem de histórico:
  - `GET /api/tenants/{tenantId}/avatars?limit=20&cursor=...`
- Endpoint de restauração:
  - `POST /api/tenants/{tenantId}/avatar/{avatarId}/restore`
- Contrato de quota:
  - `GET /api/tenants/{tenantId}/avatar/quota`

### Migrations necessárias
- Tabela de quota/consumo por tenant (`tenant_avatar_quota_usage`).
- Índices para consultas por `tenant_id`, `created_at`, `status`.

### Dependências
- Definição de planos comerciais e limites por plano.
- Dashboards com visão por tenant.

### Métricas de sucesso
- 99% de disponibilidade dos endpoints de avatar.
- Custo médio por geração dentro do budget definido (±10%).
- Satisfação de usuários beta (CSAT da feature) >= 4/5.

### Risco principal
- Explosão de custo por uso indevido (loops de retry, prompts excessivos).

### Plano de rollback
- Ativar throttle agressivo por tenant.
- Desabilitar geração IA mantendo upload manual.
- Reverter rollout para tenants canário apenas.

### Critérios de conclusão
- Histórico, restore e quota operacionais.
- Rollout para pelo menos 30% dos tenants elegíveis com estabilidade.
- Playbook de incidentes e custo validado em on-call.

---

## Sprint 4 — Governança avançada (V2 inicial)

### Responsáveis por frente
- **Backend:** Squad Core API + Security
- **Worker:** Squad IA Pipeline
- **Frontend:** Squad Experience
- **Plataforma:** SRE/DevOps + FinOps

### APIs/contratos a entregar
- Contrato de moderação/classificação de imagem.
- Endpoint de auditoria:
  - `GET /api/tenants/{tenantId}/avatar/audit-events`

### Migrations necessárias
- Tabela de auditoria (`tenant_avatar_audit_event`).
- Campos de classificação (`safety_label`, `safety_score`).

### Dependências
- Serviço/política corporativa de moderação.
- Aprovação de segurança/compliance.

### Métricas de sucesso
- 100% dos eventos críticos auditáveis.
- 0 incidentes de compliance por imagem não moderada.
- Redução de 20% no custo com cache semântico.

### Risco principal
- Falso positivo de moderação bloqueando uso legítimo.

### Plano de rollback
- Rodar moderação em modo shadow (não bloqueante).
- Permitir override operacional por tenant sob aprovação.

### Critérios de conclusão
- Auditoria completa e rastreável ponta a ponta.
- Moderação com estratégia de exceção documentada.
- Pronto para expansão para 100% dos tenants.

---

## 4) Dependências cruzadas (resumo)

- **Backend ⇄ Worker:** contrato de status, catálogo de erros, campos `modelo`/`prompt` obrigatórios em criações por IA.
- **Worker ⇄ Plataforma:** filas, retries, DLQ, métricas de custo/latência.
- **Frontend ⇄ Backend:** contratos versionados e erros padronizados.
- **Produto/Negócio ⇄ Plataforma:** definição de quota, plano e estratégia de rollout.

---

## 5) Checklist de rollout progressivo por tenant

## Pré-rollout
- [ ] Feature flag `avatar.enabled` criada (global + por tenant).
- [ ] SLOs e alertas definidos (erro, latência, custo por geração).
- [ ] Dashboards com corte por tenant, modelo e ambiente.
- [ ] Runbook de incidentes e rollback validado com on-call.

## Canary
- [ ] Selecionar 3-5 tenants de baixo risco.
- [ ] Habilitar apenas upload manual (dia 1).
- [ ] Habilitar geração IA para 1 tenant canário (dia 2-3).
- [ ] Expandir para demais canários se erro < 2% e custo dentro do esperado por 48h.

## Expansão gradual
- [ ] 10% dos tenants elegíveis.
- [ ] 30% dos tenants elegíveis.
- [ ] 60% dos tenants elegíveis.
- [ ] 100% apenas após duas janelas consecutivas sem alertas críticos.

## Monitoramento de erro/custo
- [ ] Alerta de taxa de falha por tenant (>5% em 15 min).
- [ ] Alerta de latência de processamento (P95 > 120s).
- [ ] Alerta de custo por geração acima do budget diário.
- [ ] Relatório semanal de eficiência (sucesso, retry, custo, satisfação).

## Rollback operacional
- [ ] Desligar flag por tenant afetado.
- [ ] Congelar novos jobs de geração IA.
- [ ] Manter leitura do último avatar válido.
- [ ] Abrir postmortem em até 24h com plano de ação.
