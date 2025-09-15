# Facebook Ads Worker

Worker responsável por criar campanhas no Facebook Ads, incluindo
posicionamentos no Facebook e no Instagram, e coletar métricas usando a API de
Marketing do Facebook. O serviço reutiliza o modelo de
dados definido no projeto `backend`, evitando duplicação de entidades.

As chamadas ao backend utilizam o prefixo `/api`. Atualmente o worker consome
`/api/facebook-campaigns/experiments-ready`. O endpoint
`/api/instagram-creatives/approved` permanece documentado para uso futuro.

Os acessos são configurados pelas propriedades:

- `backend.base-url` (default: `http://localhost:8000`)
- `backend.api-prefix` (default: `/api`)

## Data Model

As tabelas prefixadas com `facebook_ads_` descritas em
[docs/data-model.md](../docs/data-model.md) são utilizadas para persistir
informações de campanhas, conjuntos de anúncios, criativos e parâmetros de
rastreamento.

## Documentation

Um diagrama de classes simplificado pode ser encontrado em
[docs/facebook-ads-worker/class-diagram.md](../docs/facebook-ads-worker/class-diagram.md).

## Build
```
mvn -s settings.xml package
```

## Test
```
mvn -s settings.xml test
```
