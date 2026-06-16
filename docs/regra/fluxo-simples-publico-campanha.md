# Fluxo simples para criação de público de campanha

Este fluxo gera públicos para experimentos/campanhas com um processo curto e rastreável.

## 1) Cadastro de segmentações no nicho

Na tela de nicho (`/niches/{id}`), a seção **Segmentações sugeridas** permite salvar:

- Interesses
- Cargos
- Comportamentos

As listas são persistidas no `market_niche` (`interest_list`, `role_list`, `behavior_list`).

## 2) Enriquecimento automático na Meta Ads (objetivo)

Após salvar o nicho, o backend sincroniza os itens em `targeting_element` e o **Facebook Ads Worker** executa este passo automaticamente:

1. busca itens pendentes em `GET /api/internal/targeting/elements/metaads-pending`;
2. consulta a Graph API em `GET /{API_VERSION}/search` com `type` específico:
   - `adinterest` (interesses)
   - `adworkposition` (cargos)
   - `adbehavior` (comportamentos)
3. captura e grava no backend:
   - `metaId` (código oficial Meta)
   - `metaAudienceSizeLowerBound` (limite mínimo)
   - `metaAudienceSizeUpperBound` (limite máximo)
4. atualiza o item em `PATCH /api/internal/targeting/elements/{id}/metaads`.
5. quando não houver ID oficial após todas as tentativas, marca
   `metaIdUnavailable=true` no mesmo endpoint para o backend retirar o item da
   fila automática e evitar novas chamadas repetidas à Meta.

> Resultado esperado: cada termo manual de nicho fica com **código oficial + faixa min/max** antes de ser usado no fluxo de campanha.

## 3) Seleção no experimento

Na aba **Segmentação** de `/experiments/{id}`:

- o usuário escolhe os itens do nicho para o experimento;
- clica em **Salvar seleção**.

A seleção é persistida em `experiment_targeting_selection`, vinculada ao `experiment_id`.

## 4) Resolução simples no Facebook Ads

Ainda na aba Segmentação:

- clique em **Executar fluxo simples**;
- o backend cria uma `targeting_request` interna e os candidatos de targeting;
- os jobs de resolução são enfileirados para buscar os códigos oficiais na Meta Ads API;
- ao validar, os IDs ficam disponíveis para uso na campanha/ad set.

## 5) Monitoramento e diagnóstico

Depois de executar o fluxo simples, a aba de segmentação do experimento exibe:

- resumo da última `targeting_request` (data/hora e quantidade resolvida);
- contagem de jobs pendentes/em processamento/concluídos/falhos;
- lista dos candidatos e até 3 opções retornadas por termo;
- mensagem de erro mais recente quando houver falha.

O painel atualiza automaticamente a cada 10 segundos enquanto houver candidatos pendentes.

## Endpoints envolvidos

- `PUT /api/experiments/{experimentId}/targeting-selections`
- `GET /api/experiments/{experimentId}/targeting-selections`
- `POST /api/experiments/{experimentId}/targeting-selections/run-simple-flow`
- `GET /api/internal/targeting/elements/metaads-pending`
- `PATCH /api/internal/targeting/elements/{id}/metaads`

## Referência da Meta Ads API

- `GET /{API_VERSION}/search` com `type=adinterest|adworkposition|adbehavior`
- Doc: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search
