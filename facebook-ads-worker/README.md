# Facebook Ads Worker

Worker responsável por criar campanhas no Facebook Ads, incluindo os
posicionamentos no Facebook e no Instagram, e coletar métricas usando a API de
Marketing do Facebook. O serviço reutiliza o modelo de dados definido no
projeto `backend`, evitando duplicação de entidades.

O fluxo automatizado cria toda a hierarquia necessária para veiculação:

1. **Campanha** (`POST /campaigns`) com objetivo `OUTCOME_TRAFFIC`, status
   inicial `PAUSED` e `special_ad_categories = []`, conforme documentado na
   [Marketing API](https://developers.facebook.com/docs/marketing-api/reference/ad-campaign-group#Creating) para contas que
   não se enquadram em categorias especiais.
2. **Conjunto de anúncios** (`POST /adsets`) atrelado à campanha, também em
   `PAUSED`, com segmentação geográfica simples e destino `WEBSITE`.
3. **Criativo** (`POST /adcreatives`) baseado em um `object_story_spec`
   contendo `page_id`, opcionalmente `instagram_actor_id`, mensagem formatada a
   partir do experimento e call-to-action configurável.
4. **Anúncio** (`POST /ads`) que referencia o conjunto e o criativo recém
   criados, mantido pausado até que o time operacional revise os detalhes no
   Gerenciador de Anúncios.

As chamadas ao backend utilizam o prefixo `/api`. O worker consome
`/api/facebook-campaigns/experiments-ready`, tratando respostas `404` como
"nenhum experimento disponível" para evitar falhas no agendamento. Falhas de
conexão ao recuperar os experimentos são registradas em log e ignoradas para
que o agendamento continue saudável. Após criar campanha, conjunto, criativo e
anúncio, o worker persiste o identificador da campanha via
`POST /api/facebook-campaigns`.

Todas as chamadas à Graph API são logadas detalhadamente para facilitar
investigações de erros (por exemplo, respostas `400 Bad Request`). Os logs
registram caminho da requisição, payload enviado (com `access_token`
anonimizado) e o corpo da resposta retornada pelo Facebook.

Os acessos são configurados pelas propriedades:

- `backend.base-url` (default: `http://191.252.92.222:8000`)
- `backend.api-prefix` (default: `/api`)
- `facebook.ad-set.daily-budget` (default: `2000`, em centavos da moeda da
  conta)
- `facebook.ad-set.billing-event` (default: `IMPRESSIONS`)
- `facebook.ad-set.optimization-goal` (default: `LINK_CLICKS`)
- `facebook.ad-set.destination-type` (default: `WEBSITE`)
- `facebook.ad-set.target-country` (default: `BR`)
- `facebook.page-id` (sem default – obrigatório)
- `facebook.instagram-actor-id` (opcional)
- `facebook.website-url` (sem default – obrigatório)
- `facebook.graph-api.version` (default: `v23.0` – utilizado para montar os caminhos da Graph API)
- `facebook.creative.message-template` (default: `%s` – utiliza o nome do
  experimento quando contém `%s`)
- `facebook.creative.call-to-action-type` (default: `LEARN_MORE`)

## Data Model

As tabelas prefixadas com `facebook_ads_` descritas em
[docs/data-model.md](../docs/data-model.md) são utilizadas para persistir
informações de campanhas, conjuntos de anúncios, criativos e parâmetros de
rastreamento.

## Documentation

Um diagrama de classes simplificado pode ser encontrado em
[docs/facebook-ads-worker/class-diagram.md](../docs/facebook-ads-worker/class-diagram.md).
Consulte também a documentação oficial da Graph API sempre que precisar
interagir com a plataforma: https://developers.facebook.com/docs/graph-api e
https://developers.facebook.com/docs/graph-api/reference.

## Build
```
mvn -s settings.xml package
```

## Test
```
mvn -s settings.xml test
```
