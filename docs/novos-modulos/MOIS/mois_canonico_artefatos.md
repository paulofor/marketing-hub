# MOIS — cânone inicial de artefatos

## 1. Objetivo

Este documento define o **cânone inicial de artefatos do MOIS**.

A intenção é garantir que o módulo produza artefatos:

- tipados
- versionáveis
- persistíveis
- rastreáveis por lineage
- reutilizáveis por backend, workers, prompts e relatórios

## 2. Princípios gerais

### 2.1 Envelope comum
O MOIS deve compartilhar com o restante do Marketing Hub um envelope canônico comum para governança, status, versionamento e lineage.

### 2.2 Schema próprio por domínio
O MOIS compartilha o envelope, mas mantém schemas internos próprios para o conteúdo específico de inteligência de ofertas.

### 2.3 Artefato imutável por versão
Toda versão publicada de artefato deve ser tratada como imutável.

### 2.4 Lineage obrigatório
Todo insight importante deve poder apontar para request, fonte e snapshot que o originaram.

### 2.5 Conteúdo bruto não é insight final
Snapshots e material bruto não devem ser confundidos com artefatos já interpretados.

## 3. Camadas de schema

### Camada 1 — `ArtifactBaseSchema`
Camada compartilhada com o restante do sistema.

Campos esperados no envelope compartilhado:

- `artifactId`
- `artifactType`
- `schemaVersion`
- `status`
- `createdAt`
- `updatedAt`
- `createdBy`
- `module`
- `lineage`
- `metadata`
- `content`

### Camada 2 — `MoisArtifactBaseSchema`
Camada base do domínio MOIS.

Campos adicionais recomendados:

- `requestId`
- `nicheName`
- `marketTheme`
- `painOrOutcomeFocus`
- `country`
- `language`
- `sourceKind`
- `confidence`
- `evidenceRefs`

### Camada 3 — schemas específicos do MOIS
Schemas específicos por artefato.

## 4. Artefatos centrais do MOIS

### 4.1 `marketOfferDiscoveryRequest`
Representa a solicitação de descoberta.

Finalidade:
- iniciar um ciclo de pesquisa e estruturação

Campos de conteúdo sugeridos:
- `requestLabel`
- `nicheName`
- `marketTheme`
- `painOrOutcomeFocus`
- `seedQueries[]`
- `seedUrls[]`
- `channels[]`
- `country`
- `language`
- `discoveryPolicy`

### 4.2 `marketOfferSourceSnapshot`
Representa uma fonte pública descoberta e capturada.

Finalidade:
- preservar a origem observada antes da interpretação

Campos sugeridos:
- `sourceUrl`
- `sourceTitle`
- `sourceKind`
- `capturedAt`
- `httpStatus`
- `contentHash`
- `rawExcerpt`
- `normalizedTextRef`
- `captureNotes`

### 4.3 `marketOfferLandingSnapshot`
Representa snapshot estruturado de uma landing ou página de oferta.

Finalidade:
- preservar estrutura observável de uma página de venda

Campos sugeridos:
- `pageUrl`
- `headline`
- `subheadline`
- `primaryCta`
- `sections[]`
- `pricingVisible`
- `leadCaptureVisible`
- `proofElements[]`
- `offerElements[]`

### 4.4 `marketOfferCard`
Representa o cartão estruturado de uma oferta observada.

Finalidade:
- ser o artefato-base de leitura rápida e comparação

Campos sugeridos:
- `offerName`
- `sellerOrBrand`
- `channel`
- `targetAudienceHypothesis`
- `corePromise`
- `primaryOfferType`
- `deliverables[]`
- `pricePoints[]`
- `leadCaptureType`
- `mechanismClaimSummary`
- `proofSummary`
- `positioningSummary`

### 4.5 `marketOfferPromiseSignal`
Representa a promessa central ou promessa derivada identificada.

Campos sugeridos:
- `promiseText`
- `promiseType`
- `intensity`
- `timeframeClaim`
- `targetOutcome`
- `confidence`

### 4.6 `marketOfferProofSignal`
Representa elementos de prova identificados.

Campos sugeridos:
- `proofType`
- `proofText`
- `proofStrengthHypothesis`
- `proofLocation`
- `confidence`

### 4.7 `marketOfferMechanismClaim`
Representa alegação de mecanismo observada no discurso da oferta.

Importante:
- alegação de mecanismo não equivale a mecanismo validado

Campos sugeridos:
- `claimText`
- `claimCategory`
- `claimSpecificity`
- `claimRiskLevel`
- `confidence`

### 4.8 `marketOfferPricingModel`
Representa a estrutura de preço percebida.

Campos sugeridos:
- `currency`
- `priceAnchors[]`
- `mainPrice`
- `paymentModel`
- `installmentVisible`
- `discountVisible`
- `urgencyDeviceVisible`

### 4.9 `marketOfferFunnelPattern`
Representa padrões de captura e progressão de funil.

Campos sugeridos:
- `entryAssetType`
- `leadCaptureFields[]`
- `ctaStyle`
- `nextStepHypothesis`
- `deliveryFormat`
- `upsellVisible`
- `retentionHint`

### 4.10 `marketOfferAudienceHypothesis`
Representa a hipótese de público inferida a partir da oferta observada.

Campos sugeridos:
- `audienceLabel`
- `audienceStage`
- `problemAwareness`
- `solutionAwareness`
- `affordabilitySignal`
- `confidence`

### 4.11 `marketGapOpportunity`
Representa lacuna ou oportunidade inferida a partir da comparação entre ofertas.

Campos sugeridos:
- `gapType`
- `gapDescription`
- `whyItMatters`
- `supportingOfferRefs[]`
- `priority`
- `confidence`

### 4.12 `marketOfferInsightReport`
Representa o relatório consolidado final.

Campos sugeridos:
- `requestSummary`
- `offersAnalyzed[]`
- `repeatedPromises[]`
- `repeatedProofPatterns[]`
- `pricingPatterns[]`
- `funnelPatterns[]`
- `mechanismClaimPatterns[]`
- `saturationNotes[]`
- `gapOpportunities[]`
- `recommendedNextActions[]`

## 5. Convenção de naming

Convenção inicial recomendada:

- `mois.marketOfferDiscoveryRequest.v1`
- `mois.marketOfferSourceSnapshot.v1`
- `mois.marketOfferLandingSnapshot.v1`
- `mois.marketOfferCard.v1`
- `mois.marketOfferPromiseSignal.v1`
- `mois.marketOfferProofSignal.v1`
- `mois.marketOfferMechanismClaim.v1`
- `mois.marketOfferPricingModel.v1`
- `mois.marketOfferFunnelPattern.v1`
- `mois.marketOfferAudienceHypothesis.v1`
- `mois.marketGapOpportunity.v1`
- `mois.marketOfferInsightReport.v1`

## 6. Regras de status

Status recomendados:

- `DRAFT`
- `COLLECTED`
- `NORMALIZED`
- `VALIDATED`
- `APPROVED`
- `REJECTED`
- `SUPERSEDED`

Observação:
- nem todo artefato precisa percorrer todos os estados, mas os estados devem ser compatíveis com a governança global.

## 7. Regras de lineage

Relações mínimas esperadas:

- `marketOfferDiscoveryRequest` → gera `marketOfferSourceSnapshot`
- `marketOfferSourceSnapshot` → pode gerar `marketOfferLandingSnapshot`
- `marketOfferLandingSnapshot` → pode gerar `marketOfferCard`
- `marketOfferCard` → pode gerar sinais de promessa, prova, mecanismo alegado, pricing e funil
- conjunto de sinais → gera `marketOfferInsightReport`
- comparação entre múltiplos `marketOfferCard` → pode gerar `marketGapOpportunity`

## 8. Regras de persistência

Direções iniciais:

- persistir envelope e metadata em estrutura consultável
- permitir conteúdo variável via JSON quando necessário
- preservar referência estável para snapshots e blobs maiores
- manter hashes e timestamps de captura
- não usar blob como única fonte de verdade do artefato

## 9. Regras de qualidade do artefato

Um artefato do MOIS só deve ser promovido quando:

- o tipo estiver correto
- o schema estiver válido
- a fonte ou lineage estiver explícita
- houver distinção clara entre observação e inferência
- a confiança estiver preenchida quando o artefato exigir sinal inferido

## 10. Compatibilidade com o restante do sistema

O MOIS deve compartilhar com o restante do Marketing Hub:

- envelope comum
- governança de status
- versionamento
- lineage básico
- padrões de publicação de artefatos

O MOIS não deve ser forçado a compartilhar rigidamente:

- schema interno do conteúdo
- estratégia física exata de armazenamento
- lógica interna de interpretação de ofertas

## 11. Próxima evolução recomendada

Na próxima fase, o Codex pode derivar deste documento:

- DDL inicial
- DTOs
- contratos de API
- eventos internos
- testes de schema
- validadores de lineage
