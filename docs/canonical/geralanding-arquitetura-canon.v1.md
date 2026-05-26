# Cânone de Arquitetura — GeraLanding (v1)

## Objetivo

Consolidar a arquitetura do GeraLanding com base nas regras automatizadas de ArchUnit, separando explicitamente os limites do **backend** e do **worker ai**.

## 1) Backend (ads-service / `com.marketinghub.geralanding`)

```mermaid
flowchart LR
    WEB["Pacote: geralanding.<etapa>.web"]
    SERV["Pacote: geralanding.<etapa>.service"]
    PROV["Pacote: geralanding.<etapa>.provisorio"]
    EXP["Pacote: com.marketinghub.experiment (Experiment + ExperimentRepository)"]
    EXEC["Pacote: com.marketinghub.geralanding.execution (GeraLandingStageExecution + GeraLandingStageExecutionRepository)"]

    WEB -->|pode usar| SERV
    SERV -->|pode usar| EXP
    SERV -->|pode usar| EXEC
```

> Leitura do diagrama: cada caixa é um pacote. Só existe seta quando há dependência permitida. Sem seta = não pode usar diretamente.


Regras arquiteturais refletidas (ArchUnit):
- `GeraLandingStageExecutionService` não pode chamar assinaturas legadas dos assemblers de wireframe/copy/design preset.
- `GeraLandingStageExecutionService` deve chamar explicitamente as assinaturas canônicas dos assemblers.
- `WireframeProvisionalHtmlAssembler` deve residir em `geralanding.wireframe` e `DesignPresetProvisionalHtmlAssembler` em `geralanding.designpreset`.
- Serviços em `com.marketinghub.geralanding..service..` podem depender de classes do próprio pacote de serviço (mesma etapa) e de `Experiment`, `ExperimentRepository`, `GeraLandingStageExecution` e `GeraLandingStageExecutionRepository` no domínio `com.marketinghub`.
- `geralanding.*.web` só pode acessar `geralanding.*.web` e `geralanding.*.service` da mesma etapa.
- `geralanding.*.provisorio` só pode acessar `geralanding.*.provisorio` da mesma etapa.
- `geralanding.*.service` só pode acessar classes `com.marketinghub` permitidas: classes do próprio pacote `service` da etapa, `Experiment`, `ExperimentRepository`, `GeraLandingStageExecution` e `GeraLandingStageExecutionRepository`.

## 2) Worker AI (ai-worker / `com.marketinghub.worker.geralanding`)

```mermaid
flowchart TD
    SCH[GeraLandingExecutionScheduler] --> EXEC[GeraLandingExecutionService]
    EXEC --> BACK[GeraLandingBackendClient<br/>HTTP /internal/geralanding/stage-executions/*]
    EXEC --> OPENAI[GeraLandingOpenAiFlexClient]
    EXEC --> STAGE[geralanding.stage.*]
    EXEC --> COMUM[geralanding.comum.*]

    subgraph SLICES[Subpacotes com independência obrigatória]
      SW[wireframe]
      SC[copy]
      SI[imageplanning]
      SP[presetdesign]
    end

    SW -. notDependOnEachOther .- SC
    SW -. notDependOnEachOther .- SI
    SW -. notDependOnEachOther .- SP
    SC -. notDependOnEachOther .- SI
    SC -. notDependOnEachOther .- SP
    SI -. notDependOnEachOther .- SP

    COPY[geralanding.copy] -->|somente| COPYOK[geralanding.copy + geralanding.comum]
    PRESET[geralanding.presetdesign] -->|somente| PRESETOK[geralanding.presetdesign + geralanding.comum]
    WIRE[geralanding.wireframe] -->|somente| WIREOK[geralanding.wireframe + geralanding.comum]
    IMG[geralanding.imageplanning] -->|somente| IMGOK[geralanding.imageplanning + geralanding.comum]
    DELIV[geralanding.deliverables] -->|somente| DELIVOK[geralanding.deliverables + geralanding.comum]
    STG[geralanding.stage] -->|somente| STGOK[geralanding.stage + geralanding.comum]
    COMMON[geralanding.comum] -->|somente| COMMONOK[geralanding.comum]
```

Regras arquiteturais refletidas (ArchUnit):
- `geralanding..` não pode depender de `experimentpipeline..` (isolamento do pipeline legado).
- `wireframe`, `copy`, `imageplanning` e `presetdesign` devem ser independentes entre si.
- Cada subpacote funcional (`copy`, `presetdesign`, `stage`, `wireframe`, `deliverables`, `imageplanning`) só pode acessar o próprio pacote e `geralanding.comum` dentro de `com.marketinghub`.
- `geralanding.comum` só pode acessar o próprio pacote.

## 3) Regras de integração

- O **Worker AI não acessa banco**; toda leitura/gravação de estado da execução passa pelo backend GeraLanding.
- O backend concentra regras de contrato, montagem de HTML provisório/final e publicação.
- O worker ai concentra orquestração por etapa e integração com OpenAI, devolvendo resultados ao backend pelos endpoints do domínio GeraLanding.
