# Facebook Campaign Publication Canon v1

> Changelog v1
> - consolida o checklist de publicação do Facebook Ads Worker em formato canônico
> - referencia fontes de verdade (tabelas `experiment`, `creative`, `lead_portal_flow`, `targeting_element` e métricas do funil)
> - descreve o contrato de liberação, monitoramento do funil e dependências externas
> - esclarece que para o público manual do experimento, 1 cargo (`JOB_TITLE`) aprovado já atende o mínimo operacional
> - esclarece que o formulário de captação do experimento é do fluxo interno do Marketing Hub (não é o Instant Form nativo da Meta)
> - adiciona invariante canônico de unicidade: um experimento não pode gerar campanhas duplicadas na mesma plataforma
> - adiciona fluxo canônico de deduplicação de upload de imagem com reuso de `meta_image_hash`
> - formaliza que a publicação de campanhas é uma etapa plugável executada pelo `facebook-ads-worker` seguindo o padrão `Pipeline Stage Execution Engine`
> - formaliza que o fallback de segmentação manual aprovado pelo backend pertence ao `facebook-ads-worker`, sem delegação ao `ai-worker`

Este documento complementa o `system-governance-canon.v2.md` e passa a ser a fonte de verdade para prontidão, liberação e telemetria de campanhas de experimento no Facebook Ads Worker.

## 1. Propósito

- Garantir que a decisão de publicar campanhas de experimento siga invariantes únicos e rastreáveis no backend.
- Amarrar UI, worker e integrações ao mesmo contrato para evitar drift entre checklist, botões de liberação e funil.
- Explicitar dependências externas (Marketing API e domínio de publicação da landing) que condicionam a execução.

## 2. Escopo e exclusões

| Cobre | Não cobre |
| --- | --- |
| Requisitos mínimos para o worker liberar conjuntos de anúncios, regras de orquestração da liberação e telemetria do funil em 9 etapas. | Configuração detalhada de criativos, copywriting, regras de aprovação editorial ou playbooks completos de segmentação. |
| Fontes de verdade das flags usadas pela UI (cartão Campanha de Facebook Ads) e pelos serviços (`ExperimentReadinessService`, `/api/facebook-campaigns/experiments-ready`, `/api/facebook-pixels`). | Contratos de outros canais pagos ou fluxos do Lead Portal que já possuem cânone próprio. |
| Padrão arquitetural da publicação de campanhas como etapa plugável dentro do `facebook-ads-worker`. | Migração genérica de todos os workers para pipeline; este cânone cobre somente publicação de campanha Facebook. |

## 3. Ownership e módulos

| Regra | Dono | Consumidores |
| --- | --- | --- |
| Invariantes de prontidão (`creative`, `experiment.follow_up_action_url`, `targeting_element`) | Backend `ads-service` + domínio de experimentos | Frontend (cartão checklist), `facebook-ads-worker` |
| Publicação de campanha, criação de campanha/ad set/criativos/anúncios e fallback de segmentação manual | `facebook-ads-worker` | Backend `ads-service`, Meta Marketing API |
| Fluxo de liberação (`facebook_release_requested_at`, `status`, `market_niche.facebook_pixel_id`) | Backend `ads-service` | UI Experimentos, `facebook-ads-worker`, pixel worker |
| Geração por IA de ativos prévios (ex.: texto/imagem criativa antes da aprovação) | `ai-worker` | Backend `ads-service`, Frontend |
| Funil de 9 etapas (`experiment_campaign_metric`, eventos do Lead Portal e checkout) | Backend `ads-service` + `lead-portal` | Frontend (aba Funil), operadores, times de mídia |

## 4. Entidades e fontes de verdade

| Entidade / Campo | Fonte | Observações |
| --- | --- | --- |
| `experiment.creative_approved`, `creative.status` | Tabelas do schema `marketinghubdb` | Ao menos um `creative` do experimento precisa estar em `READY` ou `IN_PRODUCTION` após aprovação. |
| `experiment.follow_up_action_url` | `marketinghubdb.experiment` | Representa a landing aprovada na aba Landing; com valor preenchido, a página está publicada para uso como destino da campanha. |
| `targeting_element` (job_title) | `marketinghubdb.targeting_element` | Para publicação manual, pelo menos **1** elemento `JOB_TITLE` com `status='APPROVED'`; quando existirem `meta_id`/`meta_key`, o `facebook-ads-worker` deve preferi-los no payload de targeting. |
| `experiment.daily_budget`, `facebook_release_requested_at`, `funnel_reset_at`, `market_niche.facebook_pixel_id`, `status` | `marketinghubdb.experiment` | Controlam orçamento, liberação, resets e sincronismo de pixel. |
| `experiment_campaign_metric` + eventos do Lead Portal + checkout/pagamentos | bancos do domínio de experimentos e `lead-portal` | Usados para preencher o funil e o custo por etapa. |

## 5. Bloqueios canônicos de publicação (diagnóstico do worker)

Implementação: `ExperimentReadinessService` (backend) expõe os mesmos critérios usados pelo cartão **Campanha de Facebook Ads** e pelo `facebook-ads-worker`. **Todos os itens abaixo precisam estar resolvidos** para que o worker gere conjuntos de anúncios.

1. **Criativos aprovados**
   - `experiment.creative_approved = true` e pelo menos um registro em `creative` do experimento com `status = 'READY'`.
   - O botão **Gerar anúncios do pipeline** pode produzir até 3 anúncios (texto + prompt) via Worker AI (`gpt-image-1.5`). Eles entram como `DRAFT` e precisam ser aprovados antes da liberação.
   - Quando múltiplos criativos `READY` existem, o worker publica todos no mesmo ad set para preservar as variações aprovadas.
2. **Landing criada e aprovada na aba Landing**
   - A landing precisa estar criada no próprio experimento (artefato persistido em `experiment`) e aprovada na aba **Landing**.
   - O critério operacional de publicação é `experiment.follow_up_action_url` preenchido com a URL aprovada para destino da campanha.
   - O vínculo é do experimento com a própria landing aprovada; não há dependência bloqueante de `lead_portal_flow` para liberar campanha no Facebook Ads Worker.
3. **Público completo**
   - Para seleção manual de público, o mínimo de liberação é ter pelo menos **1 cargo (`JOB_TITLE`) aprovado** em `targeting_element`.
   - Como alternativa, o playbook de ad sets finalizado também atende o requisito de público.

Se qualquer bloqueio falhar o worker interrompe a publicação e retorna a lista de pendências no alerta cinza da UI.

## 5.1 Explicação complementar em linguagem simples (para operação)

Este resumo existe para facilitar o entendimento de quem opera a campanha no dia a dia.

Em termos práticos, o experimento só pode ser liberado para campanha quando 3 perguntas forem respondidas com **sim**:

1. **Tem anúncio aprovado?**
   - Pelo menos um criativo do experimento precisa estar pronto para uso.
2. **Tem página de destino publicada?**
   - A landing precisa estar aprovada e com URL final preenchida para receber o tráfego.
3. **Tem público definido?**
   - Precisa haver público aprovado (no fluxo manual, no mínimo 1 cargo aprovado).

Se qualquer resposta for **não**, a liberação deve ser interrompida até a pendência ser resolvida.

Depois que estiver tudo certo e o operador clicar em **Liberar para o Facebook Ads Worker**, o sistema coloca o experimento na fila de publicação e mantém o controle para evitar campanha duplicada do mesmo experimento.

> Esta seção é apenas explicativa para operação; as regras válidas continuam sendo as regras canônicas e técnicas definidas nas seções 5, 6 e 7.

## 6. Configurações monitoradas (não bloqueantes)

O cartão também lista itens operacionais que não travam o worker, mas devem ser revisados antes da liberação:

- **Conta do Facebook Ads conectada** – exposta pelo hook `useFacebookConfigurationStatus` e validada no backend.
- **Página do Facebook** e **Conta do Instagram** – precisam existir no hub e permanecer válidas para evitar erros de publicação.
- **Orçamento diário** – `experiment.daily_budget` deve estar preenchido para refletir a automação de mídia.
- **Formulário de captação** – quando existir link válido de formulário no fluxo do experimento, ele é tratado como publicado para operação do Marketing Hub.
- **Importante**: o formulário usado neste checklist é o formulário interno publicado pelo **Gera Landing** no Marketing Hub, **não** o Instant Form nativo do Facebook Ads.

## 7. Contrato de liberação para o Facebook Ads Worker

1. **Ação de liberação** – o botão **Liberar para o Facebook Ads Worker** marca `experiment.status = 'PLANNED'`, define `facebook_release_requested_at = now()` e zera o funil (descarta eventos anteriores à liberação).
2. **Fila de publicação** – o worker consome `/api/facebook-campaigns/experiments-ready` apenas para experimentos com `status='PLANNED'` **e** `facebook_release_requested_at` preenchido. Alterar o status manualmente não substitui o botão.
3. **Invariante de unicidade de campanha por experimento** – **é proibido** publicar duas campanhas ativas para o mesmo `experiment_id` na mesma plataforma. Se já existir campanha vinculada ao experimento, uma nova liberação deve operar em modo de atualização/reuso da campanha existente (ad sets/anúncios) e nunca criar uma segunda campanha paralela.
4. **Reprocessamentos controlados** – um novo disparo de publicação só é permitido após evidência explícita de encerramento da campanha anterior (arquivada/finalizada/erro terminal com limpeza operacional). O reprocessamento mantém o mesmo vínculo canônico de campanha do experimento e não pode duplicar campanha.
5. **Persistência do carimbo** – `facebook_release_requested_at` permanece preenchido quando o status muda para `RUNNING` ou `PAUSED`, preservando o filtro do funil. Só muda no próximo clique autorizado de reprocessamento.
6. **Confirmação de publicação completa** – depois que a Meta retornar sucesso para campanha, conjunto, criativo e anúncio, o `facebook-ads-worker` deve confirmar a publicação no backend por `POST /api/facebook-campaigns` enviando `status='ACTIVE'`. O backend deve persistir `facebook_ads_campaign.status='ACTIVE'` e `experiment.status='RUNNING'`; retries do mesmo `campaignId` devem apenas atualizar essa confirmação, sem recriar hierarquia.
7. **Pixel worker** – a mesma liberação coloca o nicho na fila de criação de pixel enquanto `market_niche.facebook_pixel_id` estiver vazio. O worker consulta `/api/facebook-pixels/niches-ready` e só considera nichos com experimentos liberados (`facebook_release_requested_at` definido, `creative_approved=true`, `status IN ('PLANNED','RUNNING','PAUSED')` e plataforma Facebook). Ao registrar o pixel do nicho, todos os experimentos herdam o mesmo ID/HTML.
8. **Execução registrada** – cada anúncio publicado referencia o valor de rastreamento (`utm_campaign`) exibido na UI junto com as conversões atribuídas.
9. **Parada manual do operador** – a UI de Experimentos pode registrar `status='USER_STOPPED'` quando a campanha for interrompida por decisão humana. Esse status encerra o ciclo operacional no Hub e **não** recoloca o experimento na fila `/api/facebook-campaigns/experiments-ready` até uma nova liberação explícita.
10. **Upload de imagem com reuso canônico (Meta `image_hash`)** – para criativos de anúncio, o worker **não** deve fazer upload cego da mesma imagem em toda execução. O fluxo obrigatório é:
   - gerar hash determinístico local do arquivo (ex.: `sha256` dos bytes da imagem);
   - consultar repositório canônico no backend para verificar se já existe vínculo `hash_local -> meta_image_hash` para a mesma plataforma/conta de anúncio;
   - se existir vínculo válido, reutilizar diretamente o `meta_image_hash` no payload do anúncio;
   - se não existir, realizar upload para a Meta, capturar o `image_hash` retornado e persistir o mapeamento para reuso futuro.
   - **invariante operacional**: deduplicação por conteúdo de imagem é obrigatória para reduzir custo, latência e risco de variação acidental entre anúncios com o mesmo asset.


## 7.1 Publicação como etapa plugável do pipeline

A publicação de campanhas no Facebook Ads é uma **etapa de pipeline** executada dentro do `facebook-ads-worker`, seguindo o padrão descrito em `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md`. Esta é a versão canônica correta para o fluxo.

Regras obrigatórias:

1. **Núcleo genérico separado** – o núcleo de execução deve ficar em pacote genérico de pipeline (ex.: `facebookadsworker.pipeline`) com contratos como `StageContext`, `StageProcessor`, `StageResult` e `PipelineWorker`. Esse núcleo não pode conhecer classes concretas da etapa de publicação.
2. **Etapa concreta isolada** – a publicação de campanhas deve ficar em pacote próprio (ex.: `facebookcampaign.publication`) e implementar o contrato genérico da etapa. A etapa concreta pode depender do núcleo e dos contratos oficiais de publicação, mas não deve acoplar o núcleo a detalhes da Meta API.
3. **Dono operacional único** – criação de campanha, ad set, criativo, anúncio, publicação de Instant Form e fallback de segmentação manual pertencem ao `facebook-ads-worker`. É proibido delegar publicação ou fallback de publicação ao `ai-worker`.
4. **AI Worker fora da publicação** – o `ai-worker` pode gerar ativos prévios que serão aprovados e persistidos pelo backend, mas não deve ser usado como mecanismo para materializar ad set, segmentação final ou chamada à Meta durante a publicação.
5. **Fallback manual de targeting** – quando não houver playbook de ad set válido para o experimento, o `facebook-ads-worker` deve buscar o pacote manual aprovado no backend por `/api/facebook-adsets/experiments-ready`, selecionar o item do experimento e montar localmente o `targeting` da Meta. O fallback legado por ad set persistido (`/api/adsets?experimentId=...`) não faz parte da publicação canônica e não deve ser usado.
6. **Mínimo operacional de público** – no fallback manual, ao menos 1 `JOB_TITLE` aprovado é obrigatório. `INTEREST` e `BEHAVIOR` podem enriquecer o targeting quando aprovados, mas não são requisitos bloqueantes.
7. **Preferência por IDs oficiais** – ao montar `work_positions`, `interests` ou `behaviors`, o worker deve preferir `meta_id`/`meta_key` oficiais (`metaId`/`metaKey` no contrato JSON) e usar termos textuais apenas como fallback compatível com normalização local.
8. **Rastreabilidade** – a etapa deve preservar os logs e registros já exigidos para chamadas ao backend e à Graph API, incluindo URL completa, parâmetros, payload enviado e resposta recebida quando aplicável.

Consequência arquitetural: a publicação de campanha passa a ser substituível como etapa operacional sem transformar o `ai-worker` em publicador e sem acoplar o núcleo genérico às classes concretas da publicação Facebook Ads.

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
8. **Zerar contagens** – o botão opera exclusivamente sobre o `experimentId` aberto na tela: apaga fisicamente do banco os eventos de teste desse experimento em `experiment_funnel_event`, incluindo os analytics de sessões da landing (`source=landing-page-analytics`), e atualiza `experiment.funnel_reset_at` desse mesmo experimento para que métricas automáticas remanescentes só considerem dados com `occurred_at >= funnel_reset_at`. A UI deve invalidar as consultas do funil e da aba Analytics após o reset para não manter sessões antigas em cache. Use antes da campanha quando testes internos poluírem o funil sem necessidade de liberar novamente o worker.
9. **Execução registrada por anúncio** – cada criativo listado traz sua referência de rastreio e a tabela de conversões para as etapas 3 a 9, permitindo diagnosticar rapidamente qual anúncio sustentou o restante do funil.
10. **Diagnóstico estatístico por etapa** – o backend expõe `GET /api/experiments/{experimentId}/funnel/diagnostics` com status por transição prioritária, separando explicitamente risco estatístico (`INSUFFICIENT_DATA`, `WEAK_SIGNAL`, `STATISTICALLY_FAILED`) de suspeita técnica (`TECHNICAL_ISSUE_SUSPECTED`). A UI consome o diagnóstico e não replica regras críticas.

## 9. Dependências externas e domínio publicado

- **Marketing API (Meta Ads)** – única fonte autorizada para gastos e sincronização de pixels. Integrações devem seguir o contrato público (vide link acima) e evitar campos não documentados.
- **Lead Portal / Domínio** – `oportunidadebrasil.shop` (A record → `191.252.120.96`) hospeda os fluxos usados pelos experimentos. Uma liberação não deve ocorrer se o fluxo associado estiver indisponível nesse domínio.

## 10. Referências cruzadas

- `system-governance-canon.v2.md` – precedência canônica e critérios de criação de novos cânones.
- `ExperimentReadinessService` (backend) – cálculo dos bloqueios.
- Endpoints: `/api/facebook-campaigns/experiments-ready`, `/api/facebook-adsets/experiments-ready`, `/api/facebook-pixels/niches-ready`, `/api/experiments/{experimentId}/funnel/diagnostics`.
- Metodologia de arquitetura por etapa: `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md`.
- Tabelas do schema `marketinghubdb`: `experiment`, `creative`, `lead_portal_flow`, `targeting_element`, `experiment_campaign_metric`.
