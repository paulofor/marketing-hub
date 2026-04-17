# Facebook Campaign Publication Canon v1

> Changelog v1
> - consolida o checklist de publicação do Facebook Ads Worker em formato canônico
> - referencia fontes de verdade (tabelas `experiment`, `creative`, `lead_portal_flow`, `targeting_element` e métricas do funil)
> - descreve o contrato de liberação, monitoramento do funil e dependências externas

Este documento complementa o `system-governance-canon.v2.md` e passa a ser a fonte de verdade para prontidão, liberação e telemetria de campanhas de experimento no Facebook Ads Worker.

## 1. Propósito

- Garantir que a decisão de publicar campanhas de experimento siga invariantes únicos e rastreáveis no backend.
- Amarrar UI, worker e integrações ao mesmo contrato para evitar drift entre checklist, botões de liberação e funil.
- Explicitar dependências externas (Marketing API e domínio do Lead Portal) que condicionam a execução.

## 2. Escopo e exclusões

| Cobre | Não cobre |
| --- | --- |
| Requisitos mínimos para o worker liberar conjuntos de anúncios, regras de orquestração da liberação e telemetria do funil em 9 etapas. | Configuração detalhada de criativos, copywriting, regras de aprovação editorial ou playbooks completos de segmentação. |
| Fontes de verdade das flags usadas pela UI (cartão Campanha de Facebook Ads) e pelos serviços (`ExperimentReadinessService`, `/api/facebook-campaigns/experiments-ready`, `/api/facebook-pixels`). | Contratos de outros canais pagos ou fluxos do Lead Portal que já possuem cânone próprio. |

## 3. Ownership e módulos

| Regra | Dono | Consumidores |
| --- | --- | --- |
| Invariantes de prontidão (`creative`, `lead_portal_flow`, `targeting_element`) | Backend `ads-service` + domínio de experimentos | Frontend (cartão checklist), `facebook-ads-worker`, `ai-worker` |
| Fluxo de liberação (`facebook_release_requested_at`, `status`, `market_niche.facebook_pixel_id`) | Backend `ads-service` | UI Experimentos, `facebook-ads-worker`, pixel worker |
| Funil de 9 etapas (`experiment_campaign_metric`, eventos do Lead Portal e checkout) | Backend `ads-service` + `lead-portal` | Frontend (aba Funil), operadores, times de mídia |

## 4. Entidades e fontes de verdade

| Entidade / Campo | Fonte | Observações |
| --- | --- | --- |
| `experiment.creative_approved`, `creative.status` | Tabelas do schema `marketinghubdb` | Ao menos um `creative` do experimento precisa estar em `READY` ou `IN_PRODUCTION` após aprovação. |
| `lead_portal_flow.experiment_id` | `marketinghubdb.lead_portal_flow` | O fluxo precisa estar vinculado ao experimento ativo para liberar o portal em `https://oportunidadebrasil.shop/flows/{slug}` (domínio apontado para 191.252.120.96). |
| `targeting_element` (interest, job_title, behavior) | `marketinghubdb.targeting_element` | Deve haver pelo menos um elemento `APPROVED` de cada tipo **ou** o playbook de ad sets precisa estar concluído. |
| `experiment.daily_budget`, `facebook_release_requested_at`, `funnel_reset_at`, `market_niche.facebook_pixel_id`, `status` | `marketinghubdb.experiment` | Controlam orçamento, liberação, resets e sincronismo de pixel. |
| `experiment_campaign_metric` + eventos do Lead Portal + checkout/pagamentos | bancos do domínio de experimentos e `lead-portal` | Usados para preencher o funil e o custo por etapa. |

## 5. Bloqueios canônicos de publicação (diagnóstico do worker)

Implementação: `ExperimentReadinessService` (backend) expõe os mesmos critérios usados pelo cartão **Campanha de Facebook Ads** e pelo `facebook-ads-worker`. **Todos os itens abaixo precisam estar resolvidos** para que o worker gere conjuntos de anúncios.

1. **Criativos aprovados**
   - `experiment.creative_approved = true` e pelo menos um registro em `creative` do experimento com `status IN ('READY','IN_PRODUCTION')`.
   - O botão **Gerar anúncios do pipeline** pode produzir até 3 anúncios (texto + prompt) via Worker AI (`gpt-image-1.5`). Eles entram como `DRAFT` e precisam ser aprovados antes da liberação.
   - Quando múltiplos criativos `READY` existem, o worker publica todos no mesmo ad set para preservar as variações aprovadas.
2. **Fluxo do Portal do Lead**
   - O experimento precisa ter um `lead_portal_flow` associado e ativo, servindo páginas pelo domínio `oportunidadebrasil.shop`.
3. **Público completo**
   - Ao menos um interesse, um cargo (job title) e um comportamento com `status='APPROVED'` em `targeting_element`, **ou** o playbook de ad sets finalizado.

Se qualquer bloqueio falhar o worker interrompe a publicação e retorna a lista de pendências no alerta cinza da UI.

## 6. Configurações monitoradas (não bloqueantes)

O cartão também lista itens operacionais que não travam o worker, mas devem ser revisados antes da liberação:

- **Conta do Facebook Ads conectada** – exposta pelo hook `useFacebookConfigurationStatus` e validada no backend.
- **Página do Facebook** e **Conta do Instagram** – precisam existir no hub e permanecer válidas para evitar erros de publicação.
- **Orçamento diário** – `experiment.daily_budget` deve estar preenchido para refletir a automação de mídia.

## 7. Contrato de liberação para o Facebook Ads Worker

1. **Ação de liberação** – o botão **Liberar para o Facebook Ads Worker** marca `experiment.status = 'PLANNED'`, define `facebook_release_requested_at = now()` e zera o funil (descarta eventos anteriores à liberação).
2. **Fila de publicação** – o worker consome `/api/facebook-campaigns/experiments-ready` apenas para experimentos com `status='PLANNED'` **e** `facebook_release_requested_at` preenchido. Alterar o status manualmente não substitui o botão.
3. **Reprocessamentos** – apertar o botão novamente gera novo carimbo, limpa o funil e recoloca o experimento na fila (útil após reset de campanhas).
4. **Persistência do carimbo** – `facebook_release_requested_at` permanece preenchido quando o status muda para `RUNNING` ou `PAUSED`, preservando o filtro do funil. Só muda no próximo clique.
5. **Pixel worker** – a mesma liberação coloca o nicho na fila de criação de pixel enquanto `market_niche.facebook_pixel_id` estiver vazio. O worker consulta `/api/facebook-pixels/niches-ready` e só considera nichos com experimentos liberados (`facebook_release_requested_at` definido, `creative_approved=true`, `status IN ('PLANNED','RUNNING','PAUSED')` e plataforma Facebook). Ao registrar o pixel do nicho, todos os experimentos herdam o mesmo ID/HTML.
6. **Execução registrada** – cada anúncio publicado referencia o valor de rastreamento (`utm_campaign`) exibido na UI junto com as conversões atribuídas.

## 8. Funil e telemetria operacional

1. **Aba Funil de vendas** – expõe nove etapas da jornada (impressão → download/compra) usando `experiment_campaign_metric` para mídia, eventos do `lead-portal` para engajamentos e eventos de checkout/pagamento para conversões finais.
2. **Custo por etapa** – o cartão mostra o gasto total sincronizado pela Marketing API do Meta Ads e divide o valor por conversão em cada etapa, permitindo encontrar gargalos sem sair do experimento. (Fonte externa: [Meta Marketing API](https://developers.facebook.com/docs/marketing-api/)).
3. **Zerar contagens** – o botão atualiza `experiment.funnel_reset_at`, e somente eventos com `occurred_at >= funnel_reset_at` permanecem visíveis. Use quando testes internos poluírem o funil sem necessidade de liberar novamente o worker.
4. **Execução registrada por anúncio** – cada criativo listado traz sua referência de rastreio e a tabela de conversões para as etapas 3 a 9, permitindo diagnosticar rapidamente qual anúncio sustentou o restante do funil.
5. **Diagnóstico estatístico por etapa** – o backend expõe `GET /api/experiments/{experimentId}/funnel/diagnostics` com status por transição prioritária, separando explicitamente risco estatístico (`INSUFFICIENT_DATA`, `WEAK_SIGNAL`, `STATISTICALLY_FAILED`) de suspeita técnica (`TECHNICAL_ISSUE_SUSPECTED`). A UI consome o diagnóstico e não replica regras críticas.

## 9. Dependências externas e domínio publicado

- **Marketing API (Meta Ads)** – única fonte autorizada para gastos e sincronização de pixels. Integrações devem seguir o contrato público (vide link acima) e evitar campos não documentados.
- **Lead Portal / Domínio** – `oportunidadebrasil.shop` (A record → `191.252.120.96`) hospeda os fluxos usados pelos experimentos. Uma liberação não deve ocorrer se o fluxo associado estiver indisponível nesse domínio.

## 10. Referências cruzadas

- `system-governance-canon.v2.md` – precedência canônica e critérios de criação de novos cânones.
- `ExperimentReadinessService` (backend) – cálculo dos bloqueios.
- Endpoints: `/api/facebook-campaigns/experiments-ready`, `/api/facebook-pixels/niches-ready`, `/api/experiments/{experimentId}/funnel/diagnostics`.
- Tabelas do schema `marketinghubdb`: `experiment`, `creative`, `lead_portal_flow`, `targeting_element`, `experiment_campaign_metric`.
