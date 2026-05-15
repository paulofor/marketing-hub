# Clickbase — Fetch base para ciclo de consulta

Este documento define o `fetch` de referência para iniciar um novo ciclo de consulta no módulo **clickbase** (coletor ClickBank do MOIS).

## Endpoint

- URL: `https://accounts.clickbank.com/graphql`
- Método: `POST`
- Tipo: GraphQL (`application/json`)
- Autenticação: `Authorization: Bearer <CLICKBANK_JWT_TOKEN>`

## Fetch de referência

> Segurança: nunca registrar token real neste arquivo. Use sempre placeholder no header `authorization`.

```javascript
fetch("https://accounts.clickbank.com/graphql", {
  "headers": {
    "accept": "application/json, text/plain, */*",
    "accept-language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6",
    "authorization": "Bearer <CLICKBANK_JWT_TOKEN>",
    "content-type": "application/json",
    "priority": "u=1, i",
    "sec-ch-ua": "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"",
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "\"Windows\"",
    "sec-fetch-dest": "empty",
    "sec-fetch-mode": "cors",
    "sec-fetch-site": "same-origin"
  },
  "referrer": "https://accounts.clickbank.com/master/dashboard/affiliate-marketplace?v=0.3470398243396684",
  "body": "{\"query\":\"query ($parameters: MarketplaceSearchParameters!) {\\n\\t\\t\\tmarketplaceSearch(parameters: $parameters) {\\n\\t\\t\\t\\ttotalHits\\n\\t\\t\\t\\toffset\\n\\t\\t\\t\\thits {\\n\\t\\t\\t\\t\\tsite\\n\\t\\t\\t\\t\\ttitle\\n\\t\\t\\t\\t\\tdescription\\n\\t\\t\\t\\t\\tfavorite\\n\\t\\t\\t\\t\\turl\\n\\t\\t\\t\\t\\turlTitle\\n\\t\\t\\t\\t\\turlDescription\\n\\t\\t\\t\\t\\tmarketplaceStats {\\n\\t\\t\\t\\t\\t\\tactivateDate\\n\\t\\t\\t\\t\\t\\tcategory\\n\\t\\t\\t\\t\\t\\tsubCategory\\n\\t\\t\\t\\t\\t\\tinitialDollarsPerSale\\n\\t\\t\\t\\t\\t\\taverageDollarsPerSale\\n\\t\\t\\t\\t\\t\\tgravity\\n\\t\\t\\t\\t\\t\\ttotalRebill\\n\\t\\t\\t\\t\\t\\tde\\n\\t\\t\\t\\t\\t\\ten\\n\\t\\t\\t\\t\\t\\tes\\n\\t\\t\\t\\t\\t\\tfr\\n\\t\\t\\t\\t\\t\\tit\\n\\t\\t\\t\\t\\t\\tpt\\n\\t\\t\\t\\t\\t\\tstandard\\n\\t\\t\\t\\t\\t\\tphysical\\n\\t\\t\\t\\t\\t\\trebill\\n\\t\\t\\t\\t\\t\\tupsell\\n\\t\\t\\t\\t\\t\\tstandardUrlPresent\\n\\t\\t\\t\\t\\t\\tmobileEnabled\\n\\t\\t\\t\\t\\t\\twhitelistVendor\\n\\t\\t\\t\\t\\t\\tcpaVisible\\n\\t\\t\\t\\t\\t\\tdollarTrial\\n\\t\\t\\t\\t\\t\\thasAdditionalSiteHoplinks\\n\\t\\t\\t\\t\\t\\tdirectTracking\\n\\t\\t\\t\\t\\t\\texpectedReturnRate\\n\\t\\t\\t\\t\\t\\treturnRateSource\\n\\t\\t\\t\\t\\t\\tinitialEPC\\n\\t\\t\\t\\t\\t\\tfutureEPC\\n\\t\\t\\t\\t\\t\\taverageEPC\\n\\t\\t\\t\\t\\t\\tconversionRate\\n\\t\\t\\t\\t\\t\\tnetEPC\\n\\t\\t\\t\\t\\t\\tbiGravity\\n\\t\\t\\t\\t\\t\\tscore\\n\\t\\t\\t\\t\\t\\trank\\n            sellerVolume\\n\\t\\t\\t\\t\\t}\\n\\t\\t\\t \\t\\taffiliateToolsUrl\\n\\t\\t\\t  \\taffiliateSupportEmail\\n          skypeName\\n          telegramName\\n          offerImageUrl\\n\\t\\t\\t\\t}\\n        facets {\\n\\t\\t\\t\\t\\tfield\\n\\t\\t\\t\\t\\tbuckets {\\n\\t\\t\\t\\t\\t\\tvalue\\n\\t\\t\\t\\t\\t\\tcount\\n\\t\\t\\t\\t\\t}\\n\\t\\t\\t\\t}\\n\\t\\t\\t}\\n    }\",\"variables\":{\"parameters\":{\"sortField\":\"rank\",\"sortDescending\":false,\"productAttributes\":[\"shippable\"],\"resultsPerPage\":30,\"offset\":0,\"nicknameMasq\":null}}}",
  "method": "POST",
  "mode": "cors",
  "credentials": "include"
});
```
