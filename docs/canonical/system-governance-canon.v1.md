# Marketing Hub — System Governance Canon v1

## 1. Propósito

- Estabelecer um documento-mãe curto que orienta como o Marketing Hub deve preservar a unidade de regras entre backend (`backend/ads-service`), frontend, workers (por exemplo `ai-worker`, `facebook-ads-worker`, `video-management-service`) e serviços satélites (`lead-portal`, `email-service`, `image-watermark-service`, `image-zipper-service`, `lead-portal-payments-service`).
- Criar uma "constituição" de governança para reduzir o drift observado nas evoluções independentes de módulos (vide alertas em `docs/inconsistencias-modelo-canonico-artefatos-pipeline-experimento.md`).
- Definir o roteiro para cânones específicos por domínio (experimentos, mídia, captura de leads, prompts/IA etc.), mantendo este arquivo como referência global mínima.

## 2. Escopo

| Cobre agora | Ainda não cobre |
| --- | --- |
| Princípios que determinam onde mora a fonte da verdade (por exemplo, modelos em `docs/data-model.md` e `docs/modelo-dados-experimento.md`, contratos REST do backend, schemas canônicos). | Regras detalhadas de cada fluxo (ex.: todas as etapas de publicação de anúncios, validações campo a campo do formulário de experimento, sequências completas do Lead Portal). |
| Critérios para detectar divergências entre frontend (`docs/frontend-screens-entities.md`), backend e serviços auxiliares. | Diagramas de arquitetura completos, design de infraestrutura, listas de endpoints ou tabelas completas (essa granularidade ficará nos cânones de domínio). |
| Estrutura futura da família de cânones e como evoluir versões. | Decisões irrevogáveis sobre topologias (monólito x microserviço), ferramentas como OPA/workflow engine ou refactors abrangentes (podem ser considerados em documentos futuros). |

## 3. Princípios Canônicos Globais

1. **Fonte de verdade explícita.** Cada regra operacional deve apontar para seu contrato oficial: modelos de dados (`docs/data-model.md` e derivados) vivem no backend, payloads canônicos vivem em `docs/modelo-canonico-artefatos-pipeline-experimento.md`, e qualquer worker deve apenas consumir/produzir registros seguindo esses contratos.
2. **Frontend não reimplementa domínio.** A UI (vide `docs/frontend-screens-entities.md`) só exibe ou coleta dados; cálculo de elegibilidade, aprovação de experimento, gating de packages etc. acontece no backend. Qualquer lógica temporária em `frontend/src` deve ser acompanhada por validação equivalente no backend até ser removida.
3. **Workers e integrações emitem fatos.** `ai-worker`, `facebook-ads-worker`, `market-research-service`, `video-management-service`, `image-watermark-service` e `image-zipper-service` processam filas, mas não decidem regras de negócio finais. Eles devem persistir fatos (artefatos gerados, métricas coletadas) junto com `model` e `prompt` (regra do `AGENTS.md`), deixando a decisão para o backend.
4. **Flags derivadas não são verdade primária.** Campos como `novo` em `success_product`, `status` em `lead_portal_package` ou rótulos de UI são projeções de estados declarados no backend. Workers e frontend só podem lê-los; atualizações acontecem via caso de uso oficial.
5. **Contratos entre módulos são explícitos e versionados.** Integrações (`/api/internal/targeting/elements/...`, `/internal/video/...`, `lead-portal` callbacks, payloads do `lead-portal-payments-service`) devem ter schema documentado sob `docs/canonical` ou no OpenAPI, com versionamento claro. Nenhum módulo pode depender de campos "ocultos" sem registro.
6. **Mudanças relevantes exigem testes + documentação.** Quando uma regra mudar (por exemplo, validação de targeting no backend e no `facebook-ads-worker`), a alteração deve incluir testes automatizados no repositório responsável e atualização do cânone aplicável.

## 4. Critérios para identificar risco de drift

- **Mesma regra repetida em múltiplos módulos.** Ex.: targeting e score de anchors presentes no backend e no `facebook-ads-worker` (`docs/facebook-targeting-integracao.md`). Se a regra não tiver uma descrição canônica única, há risco imediato.
- **Bloqueios condicionais diferentes entre frontend e backend.** `docs/frontend-screens-entities.md` mostra que a UI manipula `Experiment`, `Hypothesis`, `LeadPortalSimpleFormStyle`, enquanto o backend reforça estados via `docs/modelo-dados-experimento.md`. Divergências em validação de status ou orçamento são sinais de drift.
- **Workers inferindo elegibilidade.** Quando `ai-worker`, `image-watermark-service` ou `image-zipper-service` passam a aprovar/reprovar itens com base em heurísticas locais (em vez de estados como `WATERMARK_PENDING`), a regra precisa voltar ao backend.
- **Ausência de contrato único para estados encadeados.** O pipeline `Lead Portal → watermark → zipper → email → payments` depende de mesmo `packageId`/`status`. Qualquer campo transitório não registrado (por exemplo, `optimizedAsset` usado como miniatura sem contrato formal) deve ser promovido a schema.
- **Flags múltiplas para o mesmo conceito.** O documento de inconsistências evidencia `landingCodeBundle` vs `landingPageHtml`. Sempre que nomes ou flags duplicadas aparecerem, normalize antes que módulos diferentes adotem versões distintas.
- **Cópias locais de modelo.** Toda vez que um serviço tenta manter entidades próprias (por exemplo, redefinir tabelas do backend em outro módulo), valide se há justificativa ou se deve reutilizar o artefato já publicado (regra explícita no backend `AGENTS.md`).

## 5. Áreas candidatas a futuros cânones específicos

| Domínio sugerido | Justificativa breve |
| --- | --- |
| **Experiments & Activation** | Abrange nicho → hipótese → targeting → ad sets → creatives → métricas (`backend/ads-service`, `docs/modelo-dados-experimento.md`, `facebook-ads-worker`). Precisa de um cânone que descreva estados, SLAs e contratos Meta. |
| **Lead Capture & Portal / Payments** | `lead-portal` (frontend+backend) e `lead-portal-payments-service` compartilham eventos (`LEAD_PORTAL_FUNNEL_TRACKING_URL`, webhooks Mercado Pago). Um cânone deve fixar contratos de `flow`, `submission`, `package`, `purchase` e reenvios. |
| **Media Asset Lifecycle** | Pacotes processados por `image-watermark-service`, `image-zipper-service` e distribuídos pelo `email-service`. Requer regras claras de status, nomes de arquivos e limites (por exemplo, geração da miniatura 364x364). |
| **Ads Delivery & Channel Integrations** | `facebook-ads-worker` hoje cobre Meta; documentos como `docs/facebook-targeting-integracao.md` e `pipeline-3-publicos-meta-ads.md` mostram complexidade suficiente para um cânone dedicado a contratos externos. |
| **AI Prompt & Worker Governance** | `ai-worker`, `market-research-service`, `video-management-service` e fluxos descritos em `docs/experiment-image-generation-worker-flow.md` compartilham obrigações de registrar `model`/`prompt`, budget de tokens e versionamento de prompt. |
| **Vitrines & Content Entitlements** | O produto `vitrines` (frontend + backend) introduz governance própria de roles, planos e conteúdos (`vitrines/README.md`). Um cânone específico manterá alinhados autenticação, magic links e políticas de acesso. |

## 6. Regras de evolução

- Este documento deve continuar enxuto; qualquer detalhe operacional deve migrar para um cânone de domínio assim que existir.
- Atualizações que mudam princípios, escopo ou estrutura exigem nova versão (`system-governance-canon.v2.md`) e changelog curto no topo do arquivo.
- Cada cânone de domínio deve referenciar explicitamente o contrato (OpenAPI, schema, diagrama) que rege aquela regra e apontar testes automatizados correspondentes.
- Conflitos entre cânones devem ser registrados como tal (seção "Conflitos conhecidos") até que a revisão os elimine; nunca esconda divergências.
- Toda alteração que muda comportamento em produção precisa alinhar: (1) código/SQL, (2) testes, (3) cânone relevante, (4) comunicação para módulos dependentes (ex.: workers via version bump do contrato).

## 7. Estrutura-alvo da família de cânones

```
docs/canonical/
├─ system-governance-canon.v1.md              # documento-mãe
├─ experiments-canon.v1.md                    # estados e contratos do pipeline de experimentos
├─ experiments-decision-schema.v1.json        # schema machine-readable para validações automáticas
├─ lead-capture-canon.v1.md                   # fluxos do Lead Portal, packages e payments
├─ media-packages-canon.v1.md                 # lifecycle watermark → zip → email → entrega
├─ ads-integrations-canon.v1.md               # contratos Meta / canais pagos
├─ ai-workers-canon.v1.md                     # prompts, modelos e auditoria cross serviços
└─ <domínio>-decision-schema.v1.json          # schemas específicos quando necessário
```

> Cada novo documento deve declarar propósito, limites, contratos oficiais e tabela de estados, referenciando explicitamente os módulos (código, serviços ou docs) que implementam as regras descritas.
