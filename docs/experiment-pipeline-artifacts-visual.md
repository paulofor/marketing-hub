# Pipeline de Artefatos do Experimento (visão visual)

Este documento mostra, de forma visual, os itens do pipeline:

- Ângulo de Campanha
- Texto do Anúncio
- Prompt da Imagem
- Texto da Landing
- Layout da Landing

---

## Fluxo entre os itens

```mermaid
flowchart LR
    A[Ângulo de Campanha\nCAMPAIGN_ANGLE] --> B[Texto do Anúncio\nAD_COPY]
    B --> C[Prompt da Imagem\nAD_IMAGE_BRIEFING]
    C --> D[Texto da Landing\nLANDING_PAGE_COPY]
    D --> E[Layout da Landing\nLANDING_PAGE_WIREFRAME]
```

---

## Onde cada item é salvo

```mermaid
erDiagram
    EXPERIMENT ||--o{ EXPERIMENT_PIPELINE_GENERATION_JOB : "origina jobs"

    EXPERIMENT {
        BIGINT id PK
        LONGTEXT campaign_angle
        LONGTEXT ad_copy
        LONGTEXT ad_image_briefing
        LONGTEXT landing_page_copy
        LONGTEXT landing_page_wireframe
    }

    EXPERIMENT_PIPELINE_GENERATION_JOB {
        CHAR_36 id PK
        BIGINT experiment_id FK
        VARCHAR_48 section
        VARCHAR_32 status
        VARCHAR_32 stage
        VARCHAR_191 model
        LONGTEXT prompt
        LONGTEXT response_content
        DATETIME created_at
        DATETIME updated_at
    }
```

> Observação: os cinco artefatos acima ficam no registro de `experiment`.
> A tabela `experiment_pipeline_generation_job` registra execução, status e metadados da geração por seção.

---

## Mapeamento rápido (item → seção → coluna)

| Item visual | Seção do pipeline | Coluna na tabela `experiment` |
|---|---|---|
| Ângulo de Campanha | `CAMPAIGN_ANGLE` | `campaign_angle` |
| Texto do Anúncio | `AD_COPY` | `ad_copy` |
| Prompt da Imagem | `AD_IMAGE_BRIEFING` | `ad_image_briefing` |
| Texto da Landing | `LANDING_PAGE_COPY` | `landing_page_copy` |
| Layout da Landing | `LANDING_PAGE_WIREFRAME` | `landing_page_wireframe` |

---

## Dependência lógica entre os artefatos

1. **Ângulo de Campanha** define a tese central.
2. **Texto do Anúncio** usa o ângulo como base de copy.
3. **Prompt da Imagem** deriva do texto para orientar direção visual.
4. **Texto da Landing** mantém consistência com promessa/copy.
5. **Layout da Landing** organiza visualmente o texto final em blocos.

Esse encadeamento ajuda a manter coerência entre mensagem, criativo e página de destino.
