# Cobertura dos Campos de Campanha do Meta Ads Worker

Este documento resume a revisão feita sobre `meta-ads-campaign-fields.md`,
contrasta os campos descritos com o comportamento real do `facebook-ads-worker`
e aponta onde cada informação é armazenada no modelo de dados. Utilize-o como
complemento ao [diagrama de classes](class-diagram.md#diagrama-de-classes-do-facebook-ads-worker)
para navegar entre as entidades citadas.

## Visão Geral

- Alguns **pré-requisitos obrigatórios** (ID da conta de anúncios, token,
  página e CTA padrão etc.) não constam na lista original, mas são carregados a
  partir de `FacebookWorkerConfigurationClient` e das entidades `fb_account`.
- O backend agora persiste chaves estrangeiras para o experimento e a conta de
  Facebook que originaram cada campanha, bem como os identificadores dos ad
  sets, criativos e anúncios correspondentes, viabilizando auditoria ponta a
  ponta.
- Formulários instantâneos (Lead Ads) passaram a ser aceitos como destino: basta
  informar `leadGenFormId` no criativo ou `defaultLeadGenFormId` na conta para o
  worker ajustar o ad set e o CTA automaticamente.
- Há **campos planejados** no documento que ainda não são enviados pelo worker
  nem possuem suporte completo no banco (ex.: teste A/B, limites de gasto,
  segmentações avançadas).
- A base de dados possui estruturas prontas (especialmente nas tabelas
  `facebook_ads_campaign`, `facebook_ads_ad_set` e `facebook_ads_ad`) para
  receber evoluções futuras; nas tabelas abaixo destacamos o status atual de
  cada item.

## Campos de Campanha

| Campo (documento) | Tratamento atual no worker | Entidades / Propriedades relacionadas |
| --- | --- | --- |
| Nome da campanha | Derivado de `Experiment.name` e replicado em campanha, conjunto, criativo e anúncio para rastreabilidade. | Persistido em `facebook_ads_campaign.name` (ver `FacebookAdsCampaign` no diagrama). |
| Objetivo | Worker envia sempre `OUTCOME_TRAFFIC`; demais opções ainda não foram expostas. | `facebook_ads_campaign.objective`; origem em `CreateCampaignRequest.objective`. |
| Vínculo com planejamento | Novo payload inclui `experimentId` e `facebookAccountId`, garantindo rastreabilidade de origem. | `facebook_ads_campaign.experiment_id` → `experiment.id`; `facebook_ads_campaign.facebook_account_id` → `fb_account.id`. |
| Tipo de compra | Não enviado atualmente; ausência de coluna `buying_type`. | Não implementado em `FacebookAdsCampaign`. |
| Categoria especial | Enviada como lista vazia; países especiais também não são coletados. | Tabelas/tipos `facebook_ads_campaign.specialAdCategories` e `specialAdCountries`. |
| Limite de gasto da campanha | Não manipulado (`spending_limit` não enviado). | `dailyBudgetMinor` / `lifetimeBudgetMinor` permanecem nulos. |
| Orçamento da campanha (CBO) | `budgetMode` gravado como `CAMPAIGN`, porém valores vêm do ad set. | Campo `FacebookAdsCampaign.budgetMode`. |
| Teste A/B | Não suportado. | Inexistente no modelo atual. |
| Status inicial | Criado como `PAUSED` para campanha, conjunto e anúncio. | `facebook_ads_campaign.status` (`FacebookAdStatus`). |

### Lacunas adicionais em nível de campanha

1. **Credenciais e parâmetros obrigatórios** (token, `adAccountId`, CTA padrão,
   `pageId`) são carregados via `FacebookWorkerConfiguration` mas não estão
   documentados.
2. **País para categoria especial** deve acompanhar qualquer categoria diferente
   de `NONE` segundo a Graph API; a documentação atual não cobre essa exigência.

## Campos de Conjunto de Anúncios

| Campo (documento) | Tratamento atual no worker | Entidades / Propriedades relacionadas |
| --- | --- | --- |
| Nome do conjunto | Usa `Experiment.name` com sufixo “- Ad Set”. | `facebook_ads_ad_set.name` (`FacebookAdsAdSet` no diagrama). |
| Evento de conversão / Meta | `promoted_object_json` armazena o `page_id` promovido; pixel/evento ainda não configurados. | `facebook_ads_ad_set.promoted_object_json`. |
| Estratégia de otimização | Valor padrão da conta (`fb_account.ad_set_optimization_goal`). | `facebook_ads_ad_set.optimization_goal`. |
| Tipo de destino | Configurado como `WEBSITE`; automaticamente trocado para `LEAD_GENERATION` quando existe formulário de leads. | `facebook_ads_ad_set.destination_type` via payload reportado. |
| Estratégia de lance | `bid_strategy` e `bid_amount` herdados da conta; quando ausentes, o backend assume `LOWEST_COST_WITHOUT_CAP`. | `facebook_ads_ad_set.bid_strategy`, `bid_amount_minor`. |
| Orçamento (sem CBO) | Envia `daily_budget` a partir de `fb_account.ad_set_daily_budget`. | `facebook_ads_ad_set.daily_budget_minor` / `lifetime_budget_minor`. |
| Período de veiculação | `start_time`/`end_time` não configurados. | Colunas homônimas no ad set. |
| Públicos personalizados / semelhantes | Planejado para `targeting_json`; ainda não serializado. | `facebook_ads_ad_set.targeting_json`. |
| Segmentação detalhada | Não implementada. | Mesmo `targeting_json`. |
| Localizações | Apenas país padrão (`adSetTargetCountry`) incluído. | `targeting_json.geo_locations`. |
| Faixa etária, gênero, idiomas, posicionamentos, dispositivos | Ainda não expostos na UI nem enviados. | `targeting_json` aguardando evolução. |
| Limite de frequência | Sem suporte; exigiria nova coluna. | Não existente no modelo atual. |
| Janela de atribuição | Não armazenada. | Campo inexistente. |
| Rastreadores externos | Não suportado. | Requereria nova estrutura. |

## Campos de Anúncio e Criativo

| Campo (documento) | Tratamento atual no worker | Entidades / Propriedades relacionadas |
| --- | --- | --- |
| Nome do anúncio | Derivado de `Experiment.name` com sufixo “- Ad”. | `facebook_ads_ad.name`. |
| Identidade (Página/Instagram) | Resolve `pageId` usando o default da conta e, em seguida, a página associada ao experimento; se nenhuma opção estiver configurada o experimento é ignorado. O `instagram_user_id` segue o mesmo fallback conta/experimento. | `facebook_ads_ad_creative.page_id`, `instagram_user_id`. |
| Formato do anúncio | Apenas link ads; backend registra `AdCreativeKind.LINK` em todos os criativos. | `facebook_ads_ad_creative.kind`. |
| Mídia principal | Dados do criativo aprovado (`imageUrl`, `imageHash`, `videoId`); assets futuros usarão `facebook_ads_media_asset`. | `facebook_ads_media_asset` e JSON do criativo. |
| Mídias adicionais | Carrossel/coleção não suportados. | `facebook_ads_ad_creative.carousel_data_json` / `video_data_json`. |
| Texto primário | `Creative.primaryText` ou fallback da conta. | `facebook_ads_ad_creative.link_data_json.message`. |
| Título (Headline) | `Creative.headline`. | `link_data_json.name`. |
| Descrição | `Creative.description`. | `link_data_json.description`. |
| URL de destino | `Creative.destinationUrl` com fallback configurado; torna-se opcional quando há formulário de leads. | `link_data_json.link` e `call_to_action.value.link`. |
| Display Link | Estrutura disponível mas não utilizada. | `link_data_json.display_link`. |
| Formulário de leads | `Creative.leadGenFormId` ou `fb_account.default_lead_gen_form_id`. | `link_data_json.call_to_action.value.lead_gen_form_id`. |
| CTA | `Creative.cta` ou `fb_account.default_call_to_action_type`. Lista completa em [call-to-action-types.md](call-to-action-types.md). | `link_data_json.call_to_action`. |
| Parâmetros UTM | Planejado; tabela `facebook_ads_ad_tracking_utm` ainda vazia. | `facebook_ads_ad_tracking_utm`. |
| Pixel / eventos | Dependem do `promoted_object_json` no ad set. | `facebook_ads_ad_set.promoted_object_json`. |
| Deep link / App link | Não implementado. | Potencial no `call_to_action.value`. |
| Texto alternativo | Não armazenado; precisa de extensão. | Sem campo dedicado. |
| Rastreamento de chamadas | Não suportado. | Não existe no modelo atual. |

## Recomendações

1. **Atualizar `meta-ads-campaign-fields.md`** para deixar claro o que é
   obrigatório hoje, o que está em backlog e quais dados vêm da configuração de
   conta (`fb_account`).
2. **Sinalizar dependências entre campos** — por exemplo, categoria especial
   requer país; UTMs dependem de tabela específica; pixel exige configuração de
   promoted object.
3. **Planejar evoluções de schema** para recursos ainda inexistentes (teste A/B,
   limite de frequência, rastreamento externo) antes de expor esses campos na UI
   ou na automação.
