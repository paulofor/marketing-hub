# Diagrama de classes — OPRM NichoCNAE com feedback entre ciclos

Este desenho propõe o pipeline NichoCNAE como um fluxo iterativo por ciclos encadeados. A etapa de qualidade não chama diretamente etapas anteriores: ela produz um feedback estruturado e um plano de correção. O orquestrador cria o próximo ciclo usando esse plano como contexto obrigatório.

## Visão de classes

```mermaid
classDiagram
    direction LR

    class NicheCnaePipelineOrchestrator {
      +startCycle(command) ResearchCycle
      +continueCycle(cycleId) void
      +handleQualityDecision(cycleId, review) ResearchCycle?
    }

    class ResearchCycle {
      +String cycleId
      +String rootCycleId
      +String parentCycleId
      +String cnaeCode
      +Long cycleNumber
      +CycleStatus status
      +ReprocessPolicy reprocessPolicy
      +Instant startedAt
      +Instant finishedAt
    }

    class StageExecution {
      +Long id
      +String cycleId
      +StageCode stageCode
      +StageStatus status
      +Long inputArtifactId
      +Long outputArtifactId
      +Instant startedAt
      +Instant finishedAt
      +String errorMessage
    }

    class StageProcessor {
      <<interface>>
      +supports(stageCode) boolean
      +process(context) StageResult
    }

    class StageContext {
      +String cycleId
      +String cnaeCode
      +List~StageArtifact~ previousArtifacts
      +QualityFeedback previousFeedback
      +CorrectionPlan correctionPlan
    }

    class StageResult {
      +StageStatus status
      +StageArtifact outputArtifact
      +String summary
      +String errorMessage
    }

    class StageArtifact {
      +Long id
      +String cycleId
      +StageCode stageCode
      +ArtifactType type
      +String contentRef
      +String checksum
      +Instant createdAt
    }

    class QualityGateProcessor {
      +process(context) StageResult
      -evaluate(card, artifacts) QualityReview
    }

    class QualityReview {
      +Integer score
      +QualityDecision decision
      +List~QualityProblem~ problems
      +CorrectionPlan correctionPlan
    }

    class QualityProblem {
      +String code
      +Severity severity
      +StageCode affectedStage
      +String evidence
      +String recommendation
    }

    class CorrectionPlan {
      +ReprocessPolicy policy
      +List~StageCode~ rerunStages
      +List~StageCode~ reuseStages
      +List~String~ instructions
    }

    class QualityFeedback {
      +String sourceCycleId
      +Integer previousScore
      +QualityDecision previousDecision
      +List~QualityProblem~ openProblems
      +CorrectionPlan correctionPlan
    }

    class CycleRepository {
      +save(cycle) ResearchCycle
      +findById(cycleId) ResearchCycle
      +findLastByRootCycle(rootCycleId) ResearchCycle
    }

    class StageExecutionRepository {
      +save(execution) StageExecution
      +findByCycle(cycleId) List~StageExecution~
    }

    class ArtifactStore {
      +save(artifact) StageArtifact
      +findByCycle(cycleId) List~StageArtifact~
      +findReusableArtifacts(parentCycleId, plan) List~StageArtifact~
    }

    NicheCnaePipelineOrchestrator --> CycleRepository
    NicheCnaePipelineOrchestrator --> StageExecutionRepository
    NicheCnaePipelineOrchestrator --> ArtifactStore
    NicheCnaePipelineOrchestrator --> StageProcessor
    NicheCnaePipelineOrchestrator --> ResearchCycle

    ResearchCycle "1" --> "0..1" ResearchCycle : parentCycle
    ResearchCycle "1" --> "0..*" StageExecution
    StageExecution "1" --> "0..1" StageArtifact : input
    StageExecution "1" --> "0..1" StageArtifact : output

    StageProcessor <|.. QualityGateProcessor
    StageProcessor --> StageContext
    StageProcessor --> StageResult
    StageContext --> QualityFeedback
    StageContext --> CorrectionPlan
    StageContext --> StageArtifact
    StageResult --> StageArtifact

    QualityGateProcessor --> QualityReview
    QualityReview --> QualityProblem
    QualityReview --> CorrectionPlan
    QualityFeedback --> QualityProblem
    QualityFeedback --> CorrectionPlan
    ArtifactStore --> StageArtifact
```

## Leitura arquitetural

- `ResearchCycle` é o agregado principal: cada tentativa de qualificação do CNAE/subnicho é um ciclo versionado.
- `parentCycleId` liga um reprocessamento ao ciclo anterior; `rootCycleId` agrupa todas as tentativas da mesma investigação.
- `QualityGateProcessor` não reprocessa nada diretamente; ele apenas emite `QualityReview` com decisão, problemas e plano de correção.
- `NicheCnaePipelineOrchestrator` é o único responsável por criar o próximo ciclo quando a decisão for `REPROCESS_REQUIRED`.
- `StageContext` carrega o conhecimento do ciclo anterior por contrato, usando `QualityFeedback`, `CorrectionPlan` e artefatos reutilizáveis.
- `StageProcessor` mantém as etapas plugáveis e coesas; uma etapa não importa nem chama diretamente outra etapa concreta.
- `ArtifactStore` preserva entradas e saídas auditáveis, permitindo reprocessamento parcial ou complementar sem perder rastreabilidade.

## Decisões de reprocessamento

```mermaid
classDiagram
    class QualityDecision {
      <<enumeration>>
      APPROVED
      REPROCESS_REQUIRED
      MANUAL_REVIEW
      REJECTED
    }

    class ReprocessPolicy {
      <<enumeration>>
      NONE
      FULL_REPROCESS
      PARTIAL_REPROCESS
      COMPLEMENTARY_REPROCESS
      MANUAL_REVIEW
    }

    class CycleStatus {
      <<enumeration>>
      CREATED
      RUNNING
      QUALITY_REVIEW
      APPROVED
      REPROCESS_REQUIRED
      REPROCESSING
      MATERIALIZED
      FAILED
    }

    class StageCode {
      <<enumeration>>
      ROUTINE_RESEARCH_ORCHESTRATOR
      ROUTINE_RESEARCH_CYCLE
      NICHE_RESEARCH_SEED_BUILDER
      SOURCE_SEARCHER
      SOURCE_FETCHER
      SIGNAL_EXTRACTOR
      ROUTINE_SYNTHESIZER
      MEI_AUDIENCE_SEGMENTER
      ROUTINE_QUALITY_GATE
      ENRICHED_NICHE_MATERIALIZER
    }
```

## Regra central

O próximo ciclo só pode nascer a partir de uma decisão explícita do `QualityReview`. Assim, o sistema deixa de “rodar de novo” e passa a executar uma nova tentativa orientada por causa: problema identificado, etapa afetada, artefato reutilizável e plano de correção.
