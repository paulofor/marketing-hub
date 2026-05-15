# MOIS Hotmart — Mapeamento de dados (Ciclo 1, Ciclo 2 e banco)
# [Canônico] MOIS Hotmart — Mapeamento de dados (Ciclo 1, Ciclo 2 e banco)

## Objetivo

Este documento mapeia, de forma explícita, quais dados são obtidos no **ciclo 1** e no **ciclo 2** do coletor Hotmart e para quais campos eles são persistidos na tabela `mois_collected_reference`.

## Visão geral do fluxo

- **Ciclo 1 (listagem):** consulta produtos no endpoint de busca da Hotmart e monta o snapshot base.
- **Ciclo 2 (detalhes):** parte dos produtos já listados, consulta detalhe por `productId` e enriquece principalmente os campos de URL/página de vendas.
- **Persistência:** o coletor envia `references` + `rawMetadata` para o backend, e o backend grava em `mois_collected_reference` com regras de fallback.

---

## 1) Dados obtidos no Ciclo 1 (listagem)

Fonte principal: resposta de listagem (search) e/ou base retornada pelo backend para enriquecimento.

Campos extraídos para snapshot base (`HotmartProductSnapshot`):

- `ucode` / `productUcode`
- `title` / `name` / `productName`
- `image` / `productImage`
- `reviewRating` / `rating`
- `totalAnswers`
- `blueprint`
- `priceValue` / `value`
- `category`
- `format`
- `producerName`
- `detailsUrl` / `productUrl` / `url`
- `temperature` / `hotmartTemperature`
- `salesPageUrl` / `pageUrl` (quando já vier na listagem)

Observação: no ciclo 1, pode não existir página de vendas final confiável; por isso o ciclo 2 é usado para reforçar/confirmar o link final.

---

## 2) Dados obtidos no Ciclo 2 (detalhes por produto)

Fonte principal: endpoint de detalhes `v1/market/product/{id}/details`.

Campos priorizados/extraídos no enriquecimento:

- **Página de vendas (prioridade alta):**
  - `salesPageUrl`
  - `pageSalesLink`
  - `salesPage`
  - `pageUrl`
  - `url`
- **URL de detalhe/produto (apoio):**
  - `detailsUrl`
  - `productUrl`
  - `checkoutUrl`
- **Dados complementares do produto:**
  - `name` / `title`
  - `image`
  - `producer.name`

Regra aplicada no snapshot enriquecido:

- `salesPageUrl_final = salesPageFromDetails || salesPageUrl_base || detailPageUrl`

Ou seja: o ciclo 2 tenta usar a página de vendas específica do detalhe; se ausente, usa a já existente; se ainda ausente, cai para URL de detalhe.

---

## 3) Mapeamento para persistência no backend (`mois_collected_reference`)

A persistência acontece via payload `references` + `rawMetadata`. O backend grava com fallback por coluna.

| Coluna no banco (`mois_collected_reference`) | Origem no payload | Regra de fallback atual |
|---|---|---|
| `title` | `reference.title` | direto |
| `url` | `reference.url` | coletor envia `salesPageUrl || detailsUrl` |
| `product_name` | `reference.title`, `rawMetadata.productName`, `rawMetadata.hotmartProductName` | primeiro não-vazio |
| `product_url` | `reference.url`, `rawMetadata.productUrl` | primeiro não-vazio |
| `producer_name` | `rawMetadata.hotmartProducer`, `rawMetadata.producerName`, `rawMetadata.producer` | primeiro não-vazio |
| `sales_page_url` | `rawMetadata.salesPageUrl`, `rawMetadata.pageSalesLink`, `rawMetadata.checkoutUrl`, `reference.url` | primeiro não-vazio |
| `hotmart_temperature` | `rawMetadata.hotmartTemperature` | parse decimal |
| `hotmart_image_url` | `rawMetadata.hotmartImageUrl` | direto |
| `hotmart_producer` | `rawMetadata.hotmartProducer` | direto |
| `hotmart_description` | `rawMetadata.hotmartDescription` | direto |
| `hotmart_highlight` | `rawMetadata.hotmartHighlight` | direto |

---

## 4) Pontos de atenção operacionais

1. A coluna **`sales_page_url`** é a coluna canônica para “página de vendas” no banco.
2. O ciclo 2 existe para reduzir ambiguidade de URL e melhorar qualidade comercial do link final.
3. Se `salesPageUrl` vier vazio mesmo no ciclo 2, a persistência ainda pode usar fallback (`checkoutUrl`/`reference.url`) para não quebrar o fluxo.
4. Para consumo em UI, priorizar sempre `salesPageUrl` exposto pelo backend quando disponível.

---

## 5) Referências de implementação

- Coletor Hotmart (ciclo 2 e montagem de `rawMetadata`):
  - `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorService.java`
- Persistência relacional no backend (`mois_collected_reference`):
  - `backend/ads-service/src/main/java/com/marketinghub/mois/service/MoisCollectionPersistenceService.java`
- Registro histórico da evolução dos ciclos:
  - `docs/registros/coletor-mois1.md`

---

## 6) Dados apresentados no frontend (cards de produto em `/hotmart`)

A tela `/hotmart` consome o endpoint `GET /api/v1/mois/hotmart/products` e renderiza os cards com os campos abaixo.

| Campo exibido no card | Campo da API (`/api/v1/mois/hotmart/products`) | Origem principal no banco (`mois_collected_reference`) |
|---|---|---|
| Título do produto | `title` | `title` |
| Imagem do produto | `imageUrl` | `hotmart_image_url` |
| Produtor | `producerName` | `producer_name` |
| Score de sucesso | `successScore` | `success_score` |
| Temperatura | `temperature` | `hotmart_temperature` |
| Página de vendas (texto + link) | `salesPageUrl` | `sales_page_url` |
| Preço | `price` | derivado de metadata no serviço (quando disponível) |
| Moeda | `currency` (fallback visual para `BRL`) | derivado de metadata no serviço (quando disponível) |

### Regra de exibição aplicada na UI

- O frontend usa **somente** `salesPageUrl` para o link de página de vendas.
- Se `salesPageUrl` estiver vazio/nulo:
  - o texto mostra `Não informada`;
  - o botão principal fica desabilitado com o rótulo `Página de vendas indisponível`.

Isso evita abrir URL incorreta de fallback de ciclo 1 na experiência do usuário.

---

## 7) Fluxo ponta a ponta da **Página de Vendas** (coleta → backend → tela)

### 7.1 Coleta no `mois-hotmart-collector`

1. O coletor chama a API de busca da Hotmart (`/v2/market/search`) e tenta extrair:
   - `salesPageUrl` com fallback em `pageUrl`, `page_url`, `link`.
   - `url` com fallback em `checkoutUrl`, `productUrl`, `url`, `link`.
2. Em seguida, para cada produto, chama o endpoint de detalhe (`/v1/market/product/{id}/details`) e recalcula:
   - `salesPageUrl_final = salesPageFromDetails || salesPageUrl_base || detailPageUrl`.
3. Ao montar o payload para o backend, o coletor publica `rawMetadata.salesPageUrl` e `rawMetadata.pageSalesLink` além de `reference.url`.

### 7.2 Persistência no backend (`MoisCollectionPersistenceService`)

Na gravação da tabela `mois_collected_reference`, a coluna `sales_page_url` usa o seguinte fallback:

- `rawMetadata.salesPageUrl`
- `rawMetadata.salesPageUrls`
- `rawMetadata.pageSalesLink`
- `rawMetadata.checkoutUrl`

Ou seja, se a Hotmart não devolver URL de página de vendas canônica, o sistema persiste `checkoutUrl` como fallback final.

### 7.3 Leitura para API da tela Hotmart (`/api/v1/mois/hotmart/products`)

1. O backend encontra o `latestJobId` de HOTMART para o `workspaceId`.
2. Busca os itens desse job em `mois_collected_reference`.
3. Mapeia:
   - `salesPageUrl = sales_page_url`
   - `pageSalesLink = sales_page_url` (mesma coluna duplicada na resposta)

### 7.4 Renderização na tela (`frontend/src/pages/hotmart/HotmartPage.tsx`)

A tela calcula o link exibido com:

- `salesPageUrlVisual = item.pageSalesLink?.trim() || item.salesPageUrl?.trim() || null`

E usa esse valor tanto no texto quanto no botão “Ver página de vendas”.

---

## 8) Onde o link pode “continuar errado” (causa-raiz provável)

Com a implementação atual, o link exibido na tela será exatamente o conteúdo de `sales_page_url` do banco para o último job. Se a Hotmart não devolver `salesPageUrl` e o fallback cair em `checkoutUrl`, a tela abre checkout/detalhe em vez da página de vendas.

Em resumo: o erro normalmente não está na UI; ele nasce na qualidade do campo retornado na coleta/fallback e é propagado corretamente até a tela.
