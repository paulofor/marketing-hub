# Facebook Campaign Publication Canon v1

> Changelog v1
> - consolida o checklist de publicação do Facebook Ads Worker em formato canônico
> - referencia fontes de verdade (tabelas `experiment`, `creative`, `lead_portal_flow`, `targeting_element` e métricas do funil)
> - descreve o contrato de liberação, monitoramento do funil e dependências externas
> - esclarece que para o público manual do experimento, 1 cargo (`JOB_TITLE`) aprovado já atende o mínimo operacional
> - esclarece que o formulário de captação do experimento é do fluxo interno do Marketing Hub (não é o Instant Form nativo da Meta)
> - adiciona invariante canônico de unicidade: um experimento não pode gerar campanhas duplicadas na mesma plataforma
> - adiciona fluxo canônico de deduplicação de upload de imagem com reuso de `meta_image_hash`

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
| `lead_portal_flow.experiment_id` + `lead_portal_flow.slug` | `marketinghubdb.lead_portal_flow` | O fluxo precisa estar vinculado ao experimento ativo para liberar o portal em `https://oportunidadebrasil.shop/api/flows/{slug}/page` (ex.: experimento 11 usa `slug='exp-11-landing'`). |
| `targeting_element` (job_title) | `marketinghubdb.targeting_element` | Para publicação manual, pelo menos **1** elemento `JOB_TITLE` com `status='APPROVED'`. |
| `experiment.daily_budget`, `facebook_release_requested_at`, `funnel_reset_at`, `market_niche.facebook_pixel_id`, `status` | `marketinghubdb.experiment` | Controlam orçamento, liberação, resets e sincronismo de pixel. |
| `experiment_campaign_metric` + eventos do Lead Portal + checkout/pagamentos | bancos do domínio de experimentos e `lead-portal` | Usados para preencher o funil e o custo por etapa. |

## 5. Bloqueios canônicos de publicação (diagnóstico do worker)

Implementação: `ExperimentReadinessService` (backend) expõe os mesmos critérios usados pelo cartão **Campanha de Facebook Ads** e pelo `facebook-ads-worker`. **Todos os itens abaixo precisam estar resolvidos** para que o worker gere conjuntos de anúncios.

1. **Criativos aprovados**
   - `experiment.creative_approved = true` e pelo menos um registro em `creative` do experimento com `status = 'READY'`.
   - O botão **Gerar anúncios do pipeline** pode produzir até 3 anúncios (texto + prompt) via Worker AI (`gpt-image-1.5`). Eles entram como `DRAFT` e precisam ser aprovados antes da liberação.
   - Quando múltiplos criativos `READY` existem, o worker publica todos no mesmo ad set para preservar as variações aprovadas.
2. **Fluxo do Portal do Lead**
   - O experimento precisa ter um `lead_portal_flow` associado e ativo, servindo páginas pelo domínio `oportunidadebrasil.shop`.
   - O link de campanha deve apontar para `https://oportunidadebrasil.shop/api/flows/{slug}/page` (exemplo atual do experimento 11: `https://oportunidadebrasil.shop/api/flows/exp-11-landing/page`).
   - A página de formulário precisa carregar o pixel do Facebook do nicho (`market_niche.facebook_pixel_id`) antes da publicação.
3. **Público completo**
   - Para seleção manual de público, o mínimo de liberação é ter pelo menos **1 cargo (`JOB_TITLE`) aprovado** em `targeting_element`.
   - Como alternativa, o playbook de ad sets finalizado também atende o requisito de público.

Se qualquer bloqueio falhar o worker interrompe a publicação e retorna a lista de pendências no alerta cinza da UI.

## 6. Configurações monitoradas (não bloqueantes)

O cartão também lista itens operacionais que não travam o worker, mas devem ser revisados antes da liberação:

- **Conta do Facebook Ads conectada** – exposta pelo hook `useFacebookConfigurationStatus` e validada no backend.
- **Página do Facebook** e **Conta do Instagram** – precisam existir no hub e permanecer válidas para evitar erros de publicação.
- **Orçamento diário** – `experiment.daily_budget` deve estar preenchido para refletir a automação de mídia.
- **Formulário de captação** – quando existir link válido de formulário no fluxo do experimento, ele é tratado como publicado para operação do Marketing Hub.
- **Importante**: o formulário usado neste checklist é o formulário interno do fluxo do Marketing Hub (Lead Portal/fluxo), **não** o Instant Form nativo do Facebook Ads.

## 7. Contrato de liberação para o Facebook Ads Worker

1. **Ação de liberação** – o botão **Liberar para o Facebook Ads Worker** marca `experiment.status = 'PLANNED'`, define `facebook_release_requested_at = now()` e zera o funil (descarta eventos anteriores à liberação).
2. **Fila de publicação** – o worker consome `/api/facebook-campaigns/experiments-ready` apenas para experimentos com `status='PLANNED'` **e** `facebook_release_requested_at` preenchido. Alterar o status manualmente não substitui o botão.
3. **Invariante de unicidade de campanha por experimento** – **é proibido** publicar duas campanhas ativas para o mesmo `experiment_id` na mesma plataforma. Se já existir campanha vinculada ao experimento, uma nova liberação deve operar em modo de atualização/reuso da campanha existente (ad sets/anúncios) e nunca criar uma segunda campanha paralela.
4. **Reprocessamentos controlados** – um novo disparo de publicação só é permitido após evidência explícita de encerramento da campanha anterior (arquivada/finalizada/erro terminal com limpeza operacional). O reprocessamento mantém o mesmo vínculo canônico de campanha do experimento e não pode duplicar campanha.
5. **Persistência do carimbo** – `facebook_release_requested_at` permanece preenchido quando o status muda para `RUNNING` ou `PAUSED`, preservando o filtro do funil. Só muda no próximo clique autorizado de reprocessamento.
6. **Pixel worker** – a mesma liberação coloca o nicho na fila de criação de pixel enquanto `market_niche.facebook_pixel_id` estiver vazio. O worker consulta `/api/facebook-pixels/niches-ready` e só considera nichos com experimentos liberados (`facebook_release_requested_at` definido, `creative_approved=true`, `status IN ('PLANNED','RUNNING','PAUSED')` e plataforma Facebook). Ao registrar o pixel do nicho, todos os experimentos herdam o mesmo ID/HTML.
7. **Execução registrada** – cada anúncio publicado referencia o valor de rastreamento (`utm_campaign`) exibido na UI junto com as conversões atribuídas.
8. **Parada manual do operador** – a UI de Experimentos pode registrar `status='USER_STOPPED'` quando a campanha for interrompida por decisão humana. Esse status encerra o ciclo operacional no Hub e **não** recoloca o experimento na fila `/api/facebook-campaigns/experiments-ready` até uma nova liberação explícita.
9. **Upload de imagem com reuso canônico (Meta `image_hash`)** – para criativos de anúncio, o worker **não** deve fazer upload cego da mesma imagem em toda execução. O fluxo obrigatório é:
   - gerar hash determinístico local do arquivo (ex.: `sha256` dos bytes da imagem);
   - consultar repositório canônico no backend para verificar se já existe vínculo `hash_local -> meta_image_hash` para a mesma plataforma/conta de anúncio;
   - se existir vínculo válido, reutilizar diretamente o `meta_image_hash` no payload do anúncio;
   - se não existir, realizar upload para a Meta, capturar o `image_hash` retornado e persistir o mapeamento para reuso futuro.
   - **invariante operacional**: deduplicação por conteúdo de imagem é obrigatória para reduzir custo, latência e risco de variação acidental entre anúncios com o mesmo asset.

## 8. Funil e telemetria operacional

1. **Aba Funil de vendas** – expõe nove etapas da jornada (impressão → download/compra) usando `experiment_campaign_metric` para mídia, eventos do `lead-portal` para engajamentos e eventos de checkout/pagamento para conversões finais.
2. **Custo por etapa** – o cartão mostra o gasto total sincronizado pela Marketing API do Meta Ads e divide o valor por conversão em cada etapa, permitindo encontrar gargalos sem sair do experimento. (Fonte externa: [Meta Marketing API](https://developers.facebook.com/docs/marketing-api/)).
3. **Composição canônica do custo total do experimento** – para qualquer visão consolidada de custo (cards, listagens, relatórios e APIs de resumo), o valor de `custo_total_experimento_brl` deve ser calculado pela soma:
   - `custo_campanha_brl` (gasto de mídia sincronizado da Meta Ads API);
   - `custo_producao_imagens_brl` (criativos e imagens de página);
   - `custo_producao_textos_brl` (todas as execuções do Worker AI usando ChatGPT).
4. **Conversão cambial canônica (fase atual)** – custos de ChatGPT são apurados em USD por token e convertidos para BRL antes da soma final:
   - `custo_texto_usd = (tokens_totais / 1_000_000) * preco_usd_por_milhao_tokens`;
   - `custo_producao_textos_brl = custo_texto_usd * 5`;
   - taxa fixa vigente neste cânone: **`1 USD = 5 BRL`**.
5. **Fonte canônica de preço por modelo** – `preco_usd_por_milhao_tokens` deve vir do catálogo interno `openai_model` (backend, chave por `code` do modelo), respeitando o modo da execução (standard/batch). É proibido usar tabela hardcoded de preços como fonte primária para custo de experimento.
6. **Regra de consistência de unidade** – os preços de `openai_model` são expressos em **USD por 1 milhão de tokens**; qualquer cálculo operacional deve manter esta unidade (divisor `1_000_000`) para evitar drift financeiro.
7. **Regra de consistência de moeda** – `custo_total_experimento_brl` deve ser persistido/exibido em BRL. Campos operacionais em USD podem existir para auditoria, porém não substituem o total consolidado em BRL.
8. **Zerar contagens** – o botão atualiza `experiment.funnel_reset_at`, e somente eventos com `occurred_at >= funnel_reset_at` permanecem visíveis. Use quando testes internos poluírem o funil sem necessidade de liberar novamente o worker.
9. **Execução registrada por anúncio** – cada criativo listado traz sua referência de rastreio e a tabela de conversões para as etapas 3 a 9, permitindo diagnosticar rapidamente qual anúncio sustentou o restante do funil.
10. **Diagnóstico estatístico por etapa** – o backend expõe `GET /api/experiments/{experimentId}/funnel/diagnostics` com status por transição prioritária, separando explicitamente risco estatístico (`INSUFFICIENT_DATA`, `WEAK_SIGNAL`, `STATISTICALLY_FAILED`) de suspeita técnica (`TECHNICAL_ISSUE_SUSPECTED`). A UI consome o diagnóstico e não replica regras críticas.

## 9. Dependências externas e domínio publicado

- **Marketing API (Meta Ads)** – única fonte autorizada para gastos e sincronização de pixels. Integrações devem seguir o contrato público (vide link acima) e evitar campos não documentados.
- **Lead Portal / Domínio** – `oportunidadebrasil.shop` (A record → `191.252.120.96`) hospeda os fluxos usados pelos experimentos. Uma liberação não deve ocorrer se o fluxo associado estiver indisponível nesse domínio.

## 10. Referências cruzadas

- `system-governance-canon.v2.md` – precedência canônica e critérios de criação de novos cânones.
- `ExperimentReadinessService` (backend) – cálculo dos bloqueios.
- Endpoints: `/api/facebook-campaigns/experiments-ready`, `/api/facebook-pixels/niches-ready`, `/api/experiments/{experimentId}/funnel/diagnostics`.
- Tabelas do schema `marketinghubdb`: `experiment`, `creative`, `lead_portal_flow`, `targeting_element`, `experiment_campaign_metric`.
