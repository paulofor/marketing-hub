# AI Worker — Cadeia de valor automatizada

Este documento consolida como o Worker IA encadeia suas rotinas para transformar insumos em ativos de marketing, seguindo a jornada `Nicho → Hipótese → Público → Experimento → Ativos de mídia`. O objetivo é tornar visível a evolução dos dados conforme cada serviço especializado é disparado.

## Visão sistêmica

```mermaid
flowchart LR
    subgraph Descoberta
        SPNovo["Produto de Sucesso\n(novo = true)"]
        SPEnriquecido["Produto de Sucesso enriquecido\n(copy, funil, links)"]
        SPNovo -->|SuccessProductScheduler\n→ SuccessProductAnalyzer| SPEnriquecido
    end

    subgraph Exploração do mercado
        Nicho["Nicho gerado"]
        HipotesesSP["Hipótese inicial"]
        HipoteseNovas["Hipóteses adicionais"]
        Publicos["Públicos segmentados"]
    end

    SPEnriquecido -->|SuccessProductNicheHypothesisService\n(generate_niche_hypothesis)| Nicho
    SPEnriquecido -->|SuccessProductNicheHypothesisService| HipotesesSP

    Nicho -->|NicheHypothesisService\n(hypothesesToGenerate)| HipoteseNovas
    Nicho -->|NicheAudienceService\n(audiencesToGenerate)| Publicos

    subgraph Execução de campanhas
        Experimentos["Experimento aprovado"]
        Criativos["Criativos gerados"]
        AdSets["Conjuntos de anúncios"]
    end

    HipotesesSP -->|Seleção manual| Experimentos
    HipoteseNovas -->|Seleção manual| Experimentos
    Publicos -->|Públicos aprovados| Experimentos

    Experimentos -->|ExperimentCreativeService\n(creativesToGenerate)| Criativos
    Experimentos -->|AudienceAdSetService\n(públicos aprovados)| AdSets
    Publicos -->|Contexto para targeting| AdSets
```

O diagrama destaca:
- **Entradas** (produto de sucesso, nicho, hipótese, público, experimento).
- **Serviços do Worker IA** que realizam as transformações.
- **Saídas** prontas para alimentar as próximas etapas da cadeia de valor.
- **Interações manuais** (seleção de hipóteses e públicos para experimentos) que conectam os resultados automáticos aos planejamentos de campanha.

## Transformações guiadas pelo Worker IA

### 1. Produto de Sucesso → Copy enriquecida
- **Disparo:** `SuccessProductScheduler` a cada cinco minutos.
- **Gatilho:** registros de produto com `novo = true`.
- **Transformação:** `SuccessProductAnalyzer` envia a descrição para o `ChatGptClient` e devolve copy, etapas de funil e links prontos.
- **Resultado:** o produto fica pronto para briefing, reduzindo a necessidade de curadoria manual.
- **Referências:** documentação base em [`README.md`](README.md#produto-de-sucesso--enriquecimento-de-copy).

### 2. Produto de Sucesso → Nicho e hipótese
- **Disparo:** `SuccessProductNicheHypothesisScheduler`.
- **Gatilho:** produtos com `generate_niche_hypothesis = true`.
- **Transformação:** `SuccessProductNicheHypothesisService` extrai, via ChatGPT, um nicho e uma hipótese a partir da descrição do produto.
- **Resultado:** criação automática dos registros `MarketNiche` e `Hypothesis`, prontos para exploração.
- **Referências:** detalhes adicionais em [`produto-sucesso-nicho-hypotese-service.md`](produto-sucesso-nicho-hypotese-service.md).

### 3. Nicho → Hipóteses adicionais
- **Disparo:** `NicheHypothesisScheduler`.
- **Gatilho:** nichos com `hypothesesToGenerate > 0`.
- **Transformação:** `NicheHypothesisService` monta o contexto do nicho, consulta o ChatGPT e registra novas hipóteses.
- **Resultado:** backlog expandido de hipóteses relacionadas ao mesmo nicho.
- **Referências:** [`nicho-hypotese-service.md`](nicho-hypotese-service.md).

### 4. Nicho → Públicos
- **Disparo:** `NicheAudienceScheduler`.
- **Gatilho:** nichos com `audiencesToGenerate > 0`.
- **Transformação:** `NicheAudienceService` agrega dados do nicho e de suas hipóteses, solicita ao `AudienceChatGptClient` públicos segmentados e salva cada registro com rastreabilidade (`prompt`, `model`).
- **Resultado:** públicos prontos para testes, conectados às hipóteses relevantes.
- **Referências:** [`nicho-publico-service.md`](nicho-publico-service.md).

### 5. Experimento → Criativos
- **Disparo:** `ExperimentCreativeScheduler`.
- **Gatilho:** experimentos com `creativesToGenerate > 0`.
- **Transformação:** `ExperimentCreativeService` gera textos com o `CreativeChatGptClient` e imagens com a API de imagens da OpenAI, atualizando o experimento.
- **Resultado:** criativos textuais e visuais alinhados ao experimento.
- **Referências:** [`experimento-criativo-service.md`](experimento-criativo-service.md).

### 6. Experimento + Públicos aprovados → Conjuntos de anúncios
- **Disparo:** `AudienceAdSetScheduler`.
- **Gatilho:** experimentos para Facebook com pelo menos um público `approved = true` e sem ad sets existentes.
- **Transformação:** `AudienceAdSetService` cruza públicos aprovados do nicho/hipótese, gera segmentações detalhadas via `AudienceAdSetChatGptClient` e persiste conjuntos de anúncios (`location`, `interests`, `targetingJson`, `budget`, `durationDays`, `prompt`, `model`).
- **Resultado:** planejamento de mídia estruturado para ativação na Meta.
- **Referências:** [`experimento-adset-service.md`](experimento-adset-service.md).

## Como usar este mapa
- **Planejamento:** identifique rapidamente quais disparos precisam ser configurados em cada entidade para avançar na cadeia.
- **Operação:** acompanhe onde estão os gargalos observando as filas (`*ToGenerate`) de cada etapa.
- **Auditoria:** utilize os links de referência para validar prompts, modelos e logs de execução associados a cada transformação.
