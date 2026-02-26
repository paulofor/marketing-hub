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


## 4) Monitoramento e diagnóstico

Depois de executar o fluxo simples, a aba de segmentação do experimento exibe um painel com:

- resumo da última `targeting_request` gerada para o experimento (data/hora e quantidade de termos resolvidos);
- contagem de jobs pendentes/em processamento/concluídos/falhos na Meta Ads;
- lista dos candidatos enviados com respectivos status e até 3 opções retornadas por termo;
- mensagem de erro mais recente quando algum job falha, facilitando o ajuste do termo e nova execução.

O painel é atualizado automaticamente a cada 10 segundos enquanto houver candidatos pendentes, garantindo visibilidade do progresso e das causas de falha sem depender de logs externos.

## Referência da Meta Ads API

Consulta de interesses/cargos/comportamentos via Graph API:

- `GET /{API_VERSION}/search` com `type=adinterest`, `adworkposition`, `adTargetingCategory`
- Doc: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search

Também é recomendado validar status dos objetos com `type=targetingoptionstatus`.
