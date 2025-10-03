# Mapeamento de Campos de Entrada e Saída do Facebook Ads Worker

Este documento descreve como o **Facebook Ads Worker** converte os
experimentos aprovados pelo backend em chamadas à Graph API do Facebook e em
registros persistidos nas tabelas `facebook_ads_*`. O fluxo atual cria a
hierarquia completa (campanha → ad set → criativo → anúncio) utiliza os valores
definidos na conta marcada para o worker na tela **Contas do Facebook**. O
backend expõe esses dados via `GET /api/accounts/facebook/worker-config`, que
reúne token, App ID/Secret e parâmetros padrão de orçamento, destino e criativos.

Os experimentos que alimentam esse fluxo são apresentados ao time na tela
"Experimentos para Campanha" do frontend, garantindo o alinhamento entre a visão
operacional e a automação do worker.

## Visão Geral do Fluxo

```mermaid
flowchart TD
    scheduler["FacebookCampaignScheduler"] --> service["FacebookCampaignService"]
    service --> fetch["GET /facebook-campaigns/experiments-ready"]
    fetch --> experiments["Experimentos prontos"]
    experiments --> service
    service --> createCampaign["POST /v20.0/act_<adAccountId>/campaigns"]
    createCampaign --> createAdSet["POST /v20.0/act_<adAccountId>/adsets"]
    createAdSet --> createCreative["POST /v20.0/act_<adAccountId>/adcreatives"]
    createCreative --> createAd["POST /v20.0/act_<adAccountId>/ads"]
    createAd --> persist["POST /facebook-campaigns"]
    persist --> db["Tabelas facebook_ads_*"]
```

## 1. Coleta de experimentos prontos

* **Endpoint consultado:** `GET {backend.base-url}{api-prefix}/facebook-campaigns/experiments-ready`
* **Tratamento de respostas:** se o backend retornar `404 NOT FOUND`, o worker
  considera que não há experimentos pendentes e encerra o ciclo atual.
* **Contrato esperado:** uma lista de objetos `Experiment` com a estrutura abaixo.

| Campo | Tipo | Uso atual |
| --- | --- | --- |
| `id` | `long` | Disponível para evoluções futuras (não utilizado diretamente na criação) |
| `name` | `string` | Usado como nome base para campanha, ad set, criativo e anúncio |

## 2. Criação da campanha no Facebook

* **Endpoint da Graph API:** `POST https://graph.facebook.com/v20.0/act_<adAccountId>/campaigns`
* **Payload enviado:**

| Campo | Origem | Observações |
| --- | --- | --- |
| `name` | `Experiment.name` | Nome exibido no Gerenciador de Anúncios |
| `objective` | Constante | Valor fixo `OUTCOME_TRAFFIC` |
| `status` | Constante | `PAUSED` para evitar publicação automática |
| `special_ad_categories` | Constante | Lista com `NONE`, atendendo às políticas atuais |
| `access_token` | Conta configurada (`worker-config.accessToken`) | Token com permissão para o Ad Account |

* **Resposta tratada:** o identificador retornado em `id` abastece as etapas seguintes.

## 3. Criação do conjunto de anúncios

* **Endpoint da Graph API:** `POST https://graph.facebook.com/v20.0/act_<adAccountId>/adsets`
* **Payload enviado:**

| Campo | Origem | Observações |
| --- | --- | --- |
| `name` | `Experiment.name` + sufixo | Mantém rastreabilidade visual no Gerenciador |
| `campaign_id` | Resposta da campanha | Vincula o ad set à campanha recém-criada |
| `daily_budget` | Conta configurada (`worker-config.adSetDailyBudget`) | Valor em centavos da moeda da conta |
| `billing_event` | Conta configurada (`worker-config.adSetBillingEvent`) | Default `IMPRESSIONS` |
| `optimization_goal` | Conta configurada (`worker-config.adSetOptimizationGoal`) | Default `LINK_CLICKS` |
| `destination_type` | Conta configurada (`worker-config.adSetDestinationType`) ou `LEAD_GENERATION` quando o criativo referencia um formulário de leads | Ajustado dinamicamente conforme o destino resolvido |
| `targeting.geo_locations.countries` | Conta configurada (`worker-config.adSetTargetCountry`) | Segmentação inicial simplificada |
| `promoted_object.page_id` | Conta configurada (`worker-config.defaultPageId`) | Necessário para campanhas de tráfego |
| `status` | Constante | `PAUSED` |
| `access_token` | Conta configurada (`worker-config.accessToken`) | Token com permissão para o Ad Account |

* **Resposta tratada:** o `id` do ad set é utilizado na criação do anúncio.

## 4. Criação do criativo

* **Endpoint da Graph API:** `POST https://graph.facebook.com/v20.0/act_<adAccountId>/adcreatives`
* **Payload enviado:**

| Campo | Origem | Observações |
| --- | --- | --- |
| `name` | `Experiment.name` + sufixo | Nome amigável para o criativo |
| `object_story_spec.page_id` | Conta configurada (`worker-config.defaultPageId`) | Página responsável pelo anúncio |
| `object_story_spec.instagram_actor_id` | Conta configurada (`worker-config.defaultInstagramActorId`) | Opcional; usado quando há Instagram vinculado |
| `object_story_spec.link_data.message` | Template (`worker-config.defaultCreativeMessageTemplate`) | Substitui `%s` pelo nome do experimento |
| `object_story_spec.link_data.link` | Conta configurada (`worker-config.defaultWebsiteUrl`) ou `Creative.destinationUrl` | O worker envia apenas quando existe URL; formulários de leads funcionam sem este campo |
| `object_story_spec.link_data.call_to_action.type` | Conta configurada (`worker-config.defaultCallToActionType`) ou `Creative.cta` | Lista completa documentada em [call-to-action-types.md](call-to-action-types.md) |
| `object_story_spec.link_data.call_to_action.value.link` | Mesmo valor de `link` | Omitido quando a campanha utiliza formulário de leads |
| `object_story_spec.link_data.call_to_action.value.lead_gen_form_id` | `Creative.leadGenFormId` ou `worker-config.defaultLeadGenFormId` | Permite direcionar o CTA para formulários do Facebook ou Instagram |
| `access_token` | Conta configurada (`worker-config.accessToken`) | Token com permissão para o Ad Account |

* **Resposta tratada:** o `id` do criativo abastece a criação do anúncio.

## 5. Criação do anúncio

* **Endpoint da Graph API:** `POST https://graph.facebook.com/v20.0/act_<adAccountId>/ads`
* **Payload enviado:**

| Campo | Origem | Observações |
| --- | --- | --- |
| `name` | `Experiment.name` + sufixo | Permite localizar o anúncio dentro do conjunto |
| `adset_id` | Resposta do ad set | Mantém o vínculo com a etapa anterior |
| `creative.creative_id` | Resposta do criativo | Referencia o criativo recém-criado |
| `status` | Constante | `PAUSED` |
| `access_token` | Conta configurada (`worker-config.accessToken`) | Token com permissão para o Ad Account |

* **Resposta tratada:** o identificador alimenta o payload enviado ao backend,
  mantendo o vínculo entre anúncio, conjunto e criativo na base própria.

## 6. Persistência da campanha no backend

Após criar a hierarquia na Graph API, o worker envia um `CreateCampaignRequest`
para o backend.

* **Endpoint chamado:** `POST {backend.base-url}{api-prefix}/facebook-campaigns`
* **Campos enviados:**

| Campo | Fonte | Transformação |
| --- | --- | --- |
| `id` | Resposta da Graph API (campanha) | Copiado diretamente do `id` retornado na criação |
| `adAccountId` | Conta configurada (`worker-config.adAccountId`) | Mantido conforme configuração do worker |
| `name` | `Experiment.name` | Replicado para manter rastreabilidade entre backend e Facebook |
| `objective` | Constante | Valor fixo `OUTCOME_TRAFFIC` |
| `budgetMode` | Constante | Valor fixo `CAMPAIGN` até que o backend passe a enviar planejamento detalhado |
| `experimentId` | `Experiment.id` | Garante vínculo direto com o experimento que originou a campanha |
| `facebookAccountId` | `worker-config.accountId` | Mantém rastreabilidade com a conta utilizada pelo worker |
| `adSet.id` | Resposta da Graph API (ad set) | Persistido como `facebook_ads_ad_set.id` |
| `adSet.targetCountry` | `worker-config.adSetTargetCountry` | Serializado para JSON em `facebook_ads_ad_set.targeting_json` |
| `adSet.pageId` | `worker-config.defaultPageId`, página associada ao experimento ou `Experiment.pageId` (legado) | Serializado como `promoted_object_json` |
| `adCreative.id` | Resposta da Graph API (criativo) | Persistido como `facebook_ads_ad_creative.id` |
| `adCreative.websiteUrl` | Configuração do worker ou criativo aprovado | Serializado em `link_data_json` |
| `adCreative.leadGenFormId` | `Creative.leadGenFormId` ou fallback da conta | Persistido em `link_data_json.call_to_action.value.lead_gen_form_id` |
| `ad.id` | Resposta da Graph API (anúncio) | Persistido como `facebook_ads_ad.id` e vinculado ao ad set/criativo salvos |

## Observações

* O payload enviado ao backend inclui `experimentId`, `facebookAccountId` e as
  estruturas completas de `adSet`, `adCreative` e `ad`, garantindo que todas as
  tabelas `facebook_ads_*` recebam os identificadores gerados na Graph API.
* A persistência do ad set replica a segmentação simplificada utilizada hoje
  (`geo_locations.countries`) e o `page_id` promovido. Evoluções futuras podem
  complementar esse JSON com segmentações ricas e planejamento de orçamento.
* A composição das URLs dos endpoints do backend utiliza `UrlUtils.joinPath`
  para garantir que `backend.base-url`, `backend.api-prefix` e o caminho do
  recurso não gerem barras duplicadas.
