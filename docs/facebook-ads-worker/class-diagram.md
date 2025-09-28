# Diagrama de Classes do Facebook Ads Worker

O diagrama abaixo apresenta as principais classes que participam do fluxo de
criação de campanhas no Facebook Ads a partir de experimentos aprovados pelo
backend. Estão incluídos o agendador, o serviço responsável por orquestrar as
chamadas e o cliente usado para conversar com a Graph API do Facebook.

```mermaid
classDiagram
    class FacebookCampaignScheduler {
        +schedule()
    }

    class FacebookCampaignService {
        -facebookAdsService : FacebookAdsService
        -backendClient : WebClient
        -backendBaseUrl : String
        -apiPrefix : String
        -adAccountId : String
        -adSetDailyBudget : String
        -adSetBillingEvent : String
        -adSetOptimizationGoal : String
        -adSetDestinationType : String
        -adSetTargetCountry : String
        -pageId : String
        -instagramActorId : String
        -websiteUrl : String
        -creativeMessageTemplate : String
        -callToActionType : String
        -experimentsBlockedByPermissions : Set<Long>
        +createCampaignsFromExperiments()
        -processExperiment(exp)
        -formatCreativeMessage(name) String
    }

    class FacebookAdsService {
        -webClient : WebClient
        -accessToken : String
        +createCampaign(adAccountId, name) String
        +createAdSet(adAccountId, request) String
        +createAdCreative(adAccountId, request) String
        +createAd(adAccountId, request) String
        +getCampaignMetrics(campaignId) JsonNode
    }

    class AdSetRequest {
        <<record>>
        +name : String
        +campaignId : String
        +dailyBudget : String
        +billingEvent : String
        +optimizationGoal : String
        +destinationType : String
        +pageId : String
        +targetCountry : String
    }

    class AdCreativeRequest {
        <<record>>
        +name : String
        +pageId : String
        +instagramActorId : String
        +websiteUrl : String
        +message : String
        +callToActionType : String
    }

    class AdRequest {
        <<record>>
        +name : String
        +adSetId : String
        +creativeId : String
    }

    class CreateCampaignRequest {
        <<record>>
        +id : String
        +adAccountId : String
        +name : String
        +objective : String
        +budgetMode : BudgetMode
    }

    class FacebookAdsCampaign {
        <<entity>>
        +id : String
        +externalId : String
        +adAccountId : String
        +name : String
        +objective : String
        +status : FacebookAdStatus
        +budgetMode : BudgetMode
        +dailyBudgetMinor : Long
        +lifetimeBudgetMinor : Long
        +apiVersion : String
        +specialAdCategories : Set<SpecialAdCategory>
        +specialAdCountries : Set<String>
        +createdAt : Instant
        +updatedAt : Instant
    }

    class FacebookAdStatus {
        <<enum>>
        PAUSED
        ACTIVE
        ARCHIVED
        DELETED
    }

    class BudgetMode {
        <<enum>>
        CAMPAIGN
        ADSET
    }

    class SpecialAdCategory {
        <<enum>>
        NONE
        CREDIT
        EMPLOYMENT
        HOUSING
        ISSUES_ELECTIONS_POLITICS
    }

    class UrlUtils {
        <<utility>>
        +joinPath(base, prefix, path) String
    }

    FacebookCampaignScheduler --> FacebookCampaignService : dispara ciclo
    FacebookCampaignService --> FacebookAdsService : cria campanha/ad set/ad
    FacebookAdsService --> AdSetRequest
    FacebookAdsService --> AdCreativeRequest
    FacebookAdsService --> AdRequest
    FacebookCampaignService --> CreateCampaignRequest : monta payload do backend
    CreateCampaignRequest ..> FacebookAdsCampaign : persiste entidade
    FacebookAdsCampaign --> FacebookAdStatus
    FacebookAdsCampaign --> BudgetMode
    FacebookAdsCampaign --> SpecialAdCategory
    FacebookCampaignService ..> UrlUtils : compõe URLs do backend
```

* `FacebookCampaignScheduler` agenda a execução periódica do worker utilizando
  `@Scheduled`.
* `FacebookCampaignService` consulta o backend por experimentos prontos, cria a
  campanha, conjunto, criativo e anúncio na Graph API e registra o resultado da
  campanha no backend. O serviço resolve o `pageId` a partir do experimento,
  garantindo que todos os criativos publicados compartilhem a mesma página.
  Quando o Facebook devolve erro de permissão, o serviço adiciona o
  experimento a uma lista de bloqueio em memória para evitar novas tentativas
  até que o worker seja reiniciado.
* `FacebookAdsService` encapsula as chamadas à Graph API (criação da hierarquia
  de mídia e consulta de métricas).
* `CreateCampaignRequest` representa o payload enviado ao backend contendo os
  campos mínimos para materializar a entidade de campanha.
* `FacebookAdsCampaign` é a entidade JPA do backend responsável por armazenar a
  campanha criada com seus metadados básicos e enums auxiliares.
* `UrlUtils` garante a composição correta das URLs ao concatenar `base-url`,
  `api-prefix` e o caminho dos endpoints do backend.

## Modelo de Dados de Campanha

O backend persiste os dados das campanhas na entidade `FacebookAdsCampaign`,
que reflete a tabela `facebook_ads_campaign`. Além dos atributos básicos
utilizados hoje (`id`, `adAccountId`, `name`, `objective` e `budgetMode`), o
modelo inclui campos para controle de status (`FacebookAdStatus`), valores de
orçamento (`dailyBudgetMinor`, `lifetimeBudgetMinor`), versão da API utilizada e
listas para categorias e países especiais (`specialAdCategories` e
`specialAdCountries`). Esses dados são enriquecidos conforme novas informações
forem fornecidas pelo worker e pelo backend.
