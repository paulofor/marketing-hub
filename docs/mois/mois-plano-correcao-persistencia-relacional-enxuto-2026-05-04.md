# Plano enxuto de correção — persistência relacional do MOIS

Data: 2026-05-04  
Escopo: módulo `mois` + backend façade (`ads-service`)  
Objetivo: fazer o MOIS popular as tabelas relacionais de domínio (além de `mois_collection_job_state`) com execução consistente e rastreável.

---

## 1) Problema observado (resumo executivo)

Atualmente, o fluxo operacional persiste o estado consolidado de coleta em `mois_collection_job_state`, porém as tabelas de domínio (`mois_discovery_request`, `mois_source_snapshot`, `mois_offer_card`, `mois_offer_*_signal`) permanecem sem carga.

Impacto:
- baixa observabilidade analítica por entidade de domínio;
- perda de capacidade de consulta SQL granular por etapa do framework **Dor → Resultado → Mecanismo → Prova → Oferta**;
- dependência excessiva de payload consolidado para auditoria.

---

## 2) Meta de correção (MVP)

Implementar escrita transacional no backend para as entidades MOIS de domínio, mantendo compatibilidade com o fluxo atual de `collection_job_state`.

Critério de sucesso mínimo:
- cada execução válida de coleta/discovery gerar registros em:
  - `mois_discovery_request`;
  - `mois_source_snapshot`;
  - `mois_offer_card`;
  - ao menos uma tabela de sinais (`mois_offer_promise_signal`, `mois_offer_proof_signal`, `mois_offer_mechanism_claim`, `mois_offer_funnel_pattern`) quando houver evidência.

---

## 3) Plano enxuto em 5 passos

### Passo 1 — Contrato canônico e mapeamento de campos (0,5 dia)
- Revisar e fixar mapeamento **DTO MOIS → tabelas relacionais** para:
  - chaves de negócio;
  - timestamps (`created_at`/`updated_at`);
  - status e lineage.
- Validar aderência com:
  - `docs/canonical/system-governance-canon.v2.md`;
  - `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`;
  - `docs/modelo-dados-experimento.md`.

**Entrega:** matriz de mapeamento (campo origem x coluna destino) anexada ao PR.

### Passo 2 — Camada de persistência backend para entidades MOIS (1 dia)
- Implementar no `backend/ads-service` serviço de persistência relacional para:
  - upsert de discovery request;
  - insert/upsert de snapshots;
  - insert/upsert de offer cards;
  - insert de sinais por oferta.
- Preservar `dbms:mysql`, SQL compatível MySQL 5.7 e filtros no SQL.

**Entrega:** serviço transacional + testes unitários de serviço.

### Passo 3 — Integração do módulo `mois` com endpoint de persistência granular (1 dia)
- Evoluir integração HTTP entre `mois` e backend para enviar payload estruturado por entidade (sem “json dentro de json” desnecessário).
- Manter endpoint atual de `collection_job_state` para backward compatibility.
- Adicionar idempotência por chave composta (ex.: `canonical_url + content_signature` para oferta quando aplicável).

**Entrega:** endpoint(s) novos versionados e integração ativa no fluxo de execução.

### Passo 4 — Testes e validação operacional (0,5–1 dia)
- Backend (obrigatório):
  - testes unitários de persistência;
  - testes de contrato dos endpoints.
- Teste E2E mínimo:
  1. disparar execução;
  2. confirmar `COMPLETED`;
  3. validar contagens > 0 nas tabelas alvo.
- Para `400/422`, seguir SOP oficial (logs MCP + comparação literal payload x contrato).

**Entrega:** evidências de teste no PR (comandos + resultado).

### Passo 5 — Observabilidade e rollout seguro (0,5 dia)
- Incluir logs estruturados por `jobId`, `workspaceId`, `entity`, `rowsAffected`.
- Criar query operacional padrão de monitoramento diário (últimas 24h).
- Habilitar rollout gradual por feature flag de persistência granular (on/off).

**Entrega:** checklist de operação e fallback documentado.

---

## 4) Riscos e mitigação (enxuto)

- **Risco:** duplicidade de registros por reprocessamento.  
  **Mitigação:** upsert com chave natural/técnica + idempotency key por execução.

- **Risco:** divergência entre payload e DTO (400/422).  
  **Mitigação:** validação de contrato antes da escrita + testes de serialização.

- **Risco:** degradação de performance por inserts em lote.  
  **Mitigação:** batch insert controlado e índices mínimos nas colunas de busca.

---

## 5) Definição de pronto (DoD)

Considerar concluído quando:
1. Execução de coleta/discovery preenche `mois_collection_job_state` **e** tabelas relacionais de domínio.
2. Testes unitários Java do módulo alterado passam no CI/local.
3. Documentação canônica/modelo de dados estiver sincronizada com os campos efetivos persistidos.
4. Consulta operacional de verificação retorna dados recentes por tabela sem inconsistências de chave/status.

---

## 6) Comandos de verificação sugeridos (pós-implantação)

```sql
SELECT COUNT(*) FROM mois_discovery_request;
SELECT COUNT(*) FROM mois_source_snapshot;
SELECT COUNT(*) FROM mois_offer_card;
SELECT COUNT(*) FROM mois_offer_promise_signal;
SELECT COUNT(*) FROM mois_offer_proof_signal;
SELECT COUNT(*) FROM mois_offer_mechanism_claim;
SELECT COUNT(*) FROM mois_offer_funnel_pattern;
```

```sql
SELECT DATE(updated_at) AS d, COUNT(*)
FROM mois_collection_job_state
GROUP BY DATE(updated_at)
ORDER BY d DESC;
```

