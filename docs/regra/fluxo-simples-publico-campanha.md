# Fluxo simples para criação de público de campanha

Este fluxo permite gerar públicos para experimentos/campanhas em **3 passos**.

## 1) Cadastro de segmentações no nicho

Na tela de nicho (`/niches/{id}`), a seção **Segmentações sugeridas** agora permite salvar:

- Interesses
- Cargos
- Comportamentos

As listas ficam persistidas no próprio `market_niche` (`interest_list`, `role_list`, `behavior_list`).

## 2) Seleção no experimento

Na aba **Segmentação** de `/experiments/{id}`:

- o usuário escolhe os itens do nicho para o experimento;
- clica em **Salvar seleção**.

A seleção é persistida em `experiment_targeting_selection`, vinculada ao `experiment_id`.

## 3) Resolução simples no Facebook Ads

Ainda na aba Segmentação:

- clique em **Executar fluxo simples**;
- o backend cria uma `targeting_request` interna e os candidatos de targeting;
- os jobs de resolução são enfileirados para buscar os códigos oficiais na Meta Ads API;
- ao validar, os IDs ficam disponíveis para uso na campanha/ad set.

## Endpoints envolvidos

- `PUT /api/experiments/{experimentId}/targeting-selections`
- `GET /api/experiments/{experimentId}/targeting-selections`
- `POST /api/experiments/{experimentId}/targeting-selections/run-simple-flow`

## Referência da Meta Ads API

Consulta de interesses/cargos/comportamentos via Graph API:

- `GET /{API_VERSION}/search` com `type=adinterest`, `adworkposition`, `adTargetingCategory`
- Doc: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search

Também é recomendado validar status dos objetos com `type=targetingoptionstatus`.
