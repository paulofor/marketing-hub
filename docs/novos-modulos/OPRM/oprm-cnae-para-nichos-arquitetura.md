# OPRM — Arquitetura CNAE → Score → Enriquecimento → Nichos

## Objetivo

Este documento descreve como a lista de CNAEs deve ser usada para descobrir, priorizar e transformar oportunidades de mercado em candidatos de nicho, preservando o eixo **Dor → Resultado → Mecanismo → Prova → Oferta**.

A proposta evita criar nichos automaticamente apenas pelo nome do CNAE. O CNAE entra como evidência de tamanho e concentração de mercado; o score, o enriquecimento, as pesquisas externas e a geração de candidatos são responsabilidades do **módulo OPRM**, executadas por ciclos agendados. O backend atua somente como camada de API e persistência: lê dados, valida contrato técnico e grava dados, sem cálculo, enriquecimento ou regra de negócio.

## Princípios operacionais

1. **CNAE não é nicho final**: CNAE é fonte estruturada para encontrar mercados com volume e densidade.
2. **Backend é somente API e persistência**: o backend lê e grava dados, aplica validações técnicas de contrato e integridade, mas não calcula score, não enriquece dados e não decide prioridade de negócio.
3. **OPRM é dono da regra de negócio CNAE → oportunidade**: todo cálculo de score, seleção de CNAEs para enriquecimento, pesquisa externa, geração de sinais e criação de candidatos fica no módulo OPRM.
4. **Processamento é automático por scheduler**: o usuário não solicita geração de score; o OPRM periodicamente busca CNAEs sem score e grava o resultado. Outro scheduler seleciona CNAEs com melhor score para enriquecimento e pesquisa externa.
5. **Ciclos precisam ser rastreáveis**: cada execução agendada deve ter `cycleId`, `cycleType`, `cycleNumber` e logs com início, critérios, quantidade lida, quantidade processada, falhas e resumo final.
6. **Frontend orienta decisão humana**: a UI mostra ranking, score, candidatos, ciclos e próximos passos, mas a criação oficial do nicho depende de aprovação humana.
7. **Integrações externas são insumos, não fonte única de verdade**: dados públicos, fontes web, MDS e Worker AI complementam o banco interno e os documentos canônicos.

## Separação de responsabilidades

| Componente                        | Pode fazer                                                                                                                                    | Não pode fazer                                                                                                                       |
| --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Frontend Marketing Hub            | Exibir CNAEs, scores, ciclos, enriquecimentos e candidatos; permitir aprovação/rejeição humana.                                               | Calcular score, enriquecer dados, chamar integrações externas diretamente ou gravar no banco.                                        |
| Backend Marketing Hub             | Expor endpoints do pacote OPRM; consultar e persistir dados; validar contrato técnico; aplicar paginação e filtros de leitura.                | Calcular score, escolher CNAEs prioritários, chamar MDS/AI/fontes externas para enriquecimento ou executar regra de negócio do OPRM. |
| Módulo OPRM                       | Executar schedulers; calcular score; selecionar melhores CNAEs; pesquisar fontes externas; acionar MDS/AI; gerar candidatos e justificativas. | Escrever direto no banco ou consumir controllers de outro módulo fora do escopo OPRM.                                                |
| MySQL 5.7                         | Guardar catálogo CNAE, market size, scores, ciclos, artefatos e candidatos.                                                                   | Executar regra de negócio fora de consultas/filtros necessários.                                                                     |
| MDS / Worker AI / fontes externas | Fornecer evidências, mecanismos, conteúdo bruto e apoio de estruturação.                                                                      | Ser fonte única de verdade ou publicar nicho automaticamente.                                                                        |

## Modelo de ciclos operacionais

Para facilitar logs, comunicação e auditoria, cada execução do OPRM deve registrar um ciclo operacional.

| Campo                      | Exemplo                                         | Finalidade                                                         |
| -------------------------- | ----------------------------------------------- | ------------------------------------------------------------------ |
| `cycleId`                  | `OPRM-CNAE-SCORE-20260531-001`                  | Identificador legível e único do ciclo.                            |
| `cycleType`                | `CNAE_SCORE` ou `CNAE_ENRICHMENT`               | Diferencia o ciclo de cálculo de score do ciclo de enriquecimento. |
| `cycleNumber`              | `1`, `2`, `3`                                   | Número sequencial por tipo de ciclo.                               |
| `startedAt` / `finishedAt` | timestamps UTC                                  | Auditoria de duração e janela operacional.                         |
| `selectionCriteria`        | `score ausente`, `top score >= 80`, `limite 25` | Critério usado pelo OPRM para buscar dados via backend.            |
| `processedCount`           | `50`                                            | Quantidade processada no ciclo.                                    |
| `failedCount`              | `2`                                             | Quantidade com falha rastreável.                                   |
| `status`                   | `RUNNING`, `COMPLETED`, `FAILED`, `PARTIAL`     | Estado do ciclo.                                                   |

Tipos iniciais sugeridos:

1. **Ciclo de score — `CNAE_SCORE`**
   - Scheduler do OPRM roda de tempos em tempos.
   - OPRM chama o backend para buscar CNAEs sem score ou com score vencido.
   - OPRM calcula `opportunityScore` e componentes do score.
   - OPRM grava score e justificativa por endpoint do backend.

2. **Ciclo de enriquecimento — `CNAE_ENRICHMENT`**
   - Scheduler separado do OPRM roda de tempos em tempos.
   - OPRM chama o backend para buscar CNAEs com melhor score e ainda não enriquecidos.
   - OPRM pesquisa fontes externas, aciona MDS/AI quando necessário e gera sinais de rotina, dor, resultado, mecanismo, prova e oferta.
   - OPRM grava artefatos, candidatos e vínculos por endpoint do backend.

## Diagrama de arquitetura

```mermaid
flowchart LR
    subgraph FE["Frontend Marketing Hub"]
        FE1["Tela /oprm/cnaes-volume"]
        FE2["Tela de ciclos OPRM"]
        FE3["Tela de candidatos de nicho"]
        FE4["Telas de nicho e experimentos"]
    end

    subgraph BE["Backend Marketing Hub - API e persistencia"]
        BE1["API leitura CNAE e ranking"]
        BE2["API leitura/gravação de scores"]
        BE3["API ciclos OPRM"]
        BE4["API artefatos e candidatos"]
        BE5["API aprovação de nicho"]
    end

    subgraph DB["MySQL 5.7"]
        DB1["oprm_cnpj_cnae_dim"]
        DB2["oprm_market_size_by_cnae"]
        DB3["oprm_cnae_opportunity_score"]
        DB4["oprm_cnae_processing_cycle"]
        DB5["oprm_cnae_enrichment_artifact"]
        DB6["oprm_niche_candidate"]
        DB7["market_niche e experimentos"]
    end

    subgraph OPRM["Modulo OPRM"]
        O1["Scheduler de score CNAE"]
        O2["Calculador de score de oportunidade"]
        O3["Scheduler de enriquecimento"]
        O4["Resolvedor CNAE para ocupação/persona"]
        O5["Pesquisa de rotina e dores"]
        O6["Gerador Dor Resultado Mecanismo Prova Oferta"]
    end

    subgraph EXT["Integrações externas"]
        E1["Fonte pública CNPJ/CNAE"]
        E2["Fontes públicas do nicho"]
        E3["MDS - evidências e mecanismos"]
        E4["Worker AI / LLM"]
    end

    E1 --> O1
    O1 -->|"lê CNAEs sem score"| BE1
    BE1 --> DB1
    BE1 --> DB2
    O1 --> O2
    O2 -->|"grava score"| BE2
    BE2 --> DB3
    O1 -->|"registra ciclo"| BE3
    BE3 --> DB4

    O3 -->|"lê melhores scores"| BE2
    BE2 --> DB3
    O3 --> O4
    O4 --> O5
    O5 --> E2
    O6 --> E3
    O6 --> E4
    O5 --> O6
    O6 -->|"grava artefatos e candidatos"| BE4
    BE4 --> DB5
    BE4 --> DB6
    O3 -->|"registra ciclo"| BE3

    FE1 -->|"GET ranking e scores"| BE1
    FE1 --> BE2
    FE2 -->|"GET ciclos"| BE3
    FE3 -->|"GET candidatos"| BE4
    FE3 -->|"aprovar/rejeitar"| BE5
    BE5 --> DB6
    BE5 --> DB7
    FE4 --> BE5
```

## Diagrama de sequência

```mermaid
sequenceDiagram
    autonumber
    participant S1 as Scheduler score OPRM
    participant S2 as Scheduler enriquecimento OPRM
    participant OPRM as Modulo OPRM
    participant BE as Backend Marketing Hub
    participant DB as MySQL 5.7
    participant EXT as Fontes externas / MDS / AI
    actor Operador as Operador
    participant FE as Frontend Marketing Hub

    S1->>OPRM: Dispara ciclo CNAE_SCORE
    OPRM->>BE: POST /api/oprm/cnae-cycles start cycleType=CNAE_SCORE
    BE->>DB: Grava ciclo em RUNNING
    OPRM->>BE: GET /api/oprm/cnaes/opportunity-scores/missing?limit=50
    BE->>DB: Consulta CNAEs sem score ou com score vencido
    DB-->>BE: CNAEs elegíveis
    BE-->>OPRM: Lista de CNAEs para score
    OPRM->>OPRM: Calcula score e justificativa
    OPRM->>BE: PUT /api/oprm/cnaes/{cnaeCode}/opportunity-score
    BE->>DB: Persiste score e componentes recebidos
    OPRM->>BE: PATCH /api/oprm/cnae-cycles/{cycleId} status=COMPLETED
    BE->>DB: Atualiza resumo do ciclo

    S2->>OPRM: Dispara ciclo CNAE_ENRICHMENT
    OPRM->>BE: POST /api/oprm/cnae-cycles start cycleType=CNAE_ENRICHMENT
    BE->>DB: Grava ciclo em RUNNING
    OPRM->>BE: GET /api/oprm/cnaes/opportunity-scores/top?minScore=80&notEnriched=true&limit=25
    BE->>DB: Consulta scores conforme filtros recebidos
    DB-->>BE: CNAEs priorizados por score já gravado
    BE-->>OPRM: Lista de CNAEs para enriquecer
    OPRM->>EXT: Pesquisa rotina, dores, linguagem e evidências externas
    EXT-->>OPRM: Conteúdo bruto e evidências
    OPRM->>EXT: Solicita apoio MDS/AI para mecanismo e estruturação
    EXT-->>OPRM: Sinais estruturados de oportunidade
    OPRM->>OPRM: Gera candidatos e justificativas
    OPRM->>BE: POST /api/oprm/cnae-enrichments
    BE->>DB: Persiste artefatos, candidatos e vínculos recebidos
    OPRM->>BE: PATCH /api/oprm/cnae-cycles/{cycleId} status=COMPLETED
    BE->>DB: Atualiza resumo do ciclo

    Operador->>FE: Abre telas OPRM
    FE->>BE: GET ranking, scores, ciclos e candidatos
    BE->>DB: Consulta dados persistidos
    DB-->>BE: Dados operacionais
    BE-->>FE: Dados para decisão humana
    FE-->>Operador: Exibe oportunidades e candidatos
    Operador->>FE: Aprova candidato como nicho oficial
    FE->>BE: POST /api/oprm/cnae-niche-candidates/{id}/approve
    BE->>DB: Cria ou vincula MarketNiche conforme dados aprovados
    BE-->>FE: Nicho oficial criado ou vinculado
```

## Fluxo funcional esperado

| Etapa                      | Responsável                                  | Entrada                                     | Saída                                                                              |
| -------------------------- | -------------------------------------------- | ------------------------------------------- | ---------------------------------------------------------------------------------- |
| 1. Ingestão CNAE/CNPJ      | Módulo OPRM / coletor + backend persistência | Dados públicos CNPJ/CNAE                    | Catálogo CNAE, market size e vínculos persistidos via backend                      |
| 2. Ranking                 | Backend                                      | `oprm_market_size_by_cnae` + catálogo CNAE  | Lista paginada de CNAEs por Empresas MEI desc, sem cálculo de oportunidade         |
| 3. Ciclo de score          | OPRM scheduler                               | CNAEs sem score lidos via backend           | Score de oportunidade, componentes e justificativa gravados via backend            |
| 4. Monitoramento           | Frontend                                     | Ranking, scores e ciclos lidos via backend  | Visão operacional para o usuário, sem botão obrigatório de gerar score             |
| 5. Ciclo de enriquecimento | OPRM scheduler                               | CNAEs com melhores scores lidos via backend | Pesquisa externa, artefatos e candidatos gravados via backend                      |
| 6. Apoio externo           | OPRM + MDS + Worker AI                       | Dores, rotina e mecanismos candidatos       | Evidências, mecanismos plausíveis e estrutura Dor/Resultado/Mecanismo/Prova/Oferta |
| 7. Candidato de nicho      | OPRM grava via backend                       | Artefatos OPRM enriquecidos                 | Candidatos de nicho com status, score, justificativa e origem                      |
| 8. Aprovação               | Frontend + operador + backend persistência   | Candidatos priorizados                      | Nicho oficial criado ou vinculado no Marketing Hub                                 |
| 9. Experimento             | Backend + Frontend                           | Nicho aprovado                              | Hipóteses, ofertas, páginas, públicos e experimentos                               |

## Estados sugeridos

### Estado do score por CNAE

```mermaid
stateDiagram-v2
    [*] --> SCORE_PENDING: CNAE sem score
    SCORE_PENDING --> SCORE_RUNNING: ciclo CNAE_SCORE iniciou
    SCORE_RUNNING --> SCORED: score gravado
    SCORE_RUNNING --> SCORE_FAILED: falha rastreavel
    SCORE_FAILED --> SCORE_PENDING: elegivel para retentativa
    SCORED --> SCORE_EXPIRED: regra OPRM exige recalculo
    SCORE_EXPIRED --> SCORE_PENDING: novo ciclo
```

### Estado do enriquecimento e candidato

```mermaid
stateDiagram-v2
    [*] --> ENRICHMENT_PENDING: score alto e sem enriquecimento
    ENRICHMENT_PENDING --> ENRICHMENT_RUNNING: ciclo CNAE_ENRICHMENT iniciou
    ENRICHMENT_RUNNING --> ENRICHED: artefatos e candidatos gravados
    ENRICHMENT_RUNNING --> ENRICHMENT_FAILED: falha rastreavel
    ENRICHMENT_FAILED --> ENRICHMENT_PENDING: elegivel para retentativa
    ENRICHED --> APPROVED: operador aprova
    ENRICHED --> REJECTED: operador rejeita
    APPROVED --> NICHE_CREATED: MarketNiche criado ou vinculado
    NICHE_CREATED --> EXPERIMENT_READY: hipotese e experimento preparados
    REJECTED --> [*]
```

## Contratos e endpoints candidatos

> Os nomes abaixo são proposta de arquitetura. Antes de implementar, deve-se verificar se já existe contrato equivalente no backend. Todos os endpoints abaixo pertencem ao escopo OPRM e o backend deve atuar como leitura/gravação, sem cálculo de score ou enriquecimento.

| Necessidade                  | Endpoint sugerido                                                                    | Quem chama                | Observação                                                                      |
| ---------------------------- | ------------------------------------------------------------------------------------ | ------------------------- | ------------------------------------------------------------------------------- |
| Listar ranking CNAE atual    | `GET /api/oprm/market/import-runs/cnaes/top-volume`                                  | Frontend / OPRM           | Endpoint já usado pela tela de CNAEs; leitura paginada por Empresas MEI.        |
| Listar catálogo CNAE         | `GET /api/oprm/market/import-runs/cnaes`                                             | Frontend / OPRM           | Endpoint já usado pela tela de fallback do catálogo.                            |
| Buscar CNAEs sem score       | `GET /api/oprm/cnaes/opportunity-scores/missing?limit=...`                           | OPRM scheduler            | Backend apenas consulta dados elegíveis conforme filtros recebidos.             |
| Gravar score de oportunidade | `PUT /api/oprm/cnaes/{cnaeCode}/opportunity-score`                                   | OPRM scheduler            | Recebe score, componentes, justificativa, `cycleId` e versão do algoritmo OPRM. |
| Listar melhores scores       | `GET /api/oprm/cnaes/opportunity-scores/top?minScore=...&notEnriched=true&limit=...` | OPRM scheduler / Frontend | Backend retorna dados já calculados pelo OPRM.                                  |
| Criar/atualizar ciclo        | `POST/PATCH /api/oprm/cnae-cycles`                                                   | OPRM scheduler            | Registra `cycleId`, `cycleType`, estado, resumo e falhas.                       |
| Gravar enriquecimento        | `POST /api/oprm/cnae-enrichments`                                                    | OPRM scheduler            | Persiste artefatos, evidências, candidatos e vínculos recebidos.                |
| Listar candidatos            | `GET /api/oprm/cnae-niche-candidates?cnaeCode=...`                                   | Frontend                  | Retorna candidatos, score, status, ciclo e sinais principais.                   |
| Aprovar candidato como nicho | `POST /api/oprm/cnae-niche-candidates/{id}/approve`                                  | Frontend                  | Cria ou vincula `MarketNiche` após decisão humana.                              |

## Campos mínimos

### Score de oportunidade do CNAE

| Campo                | Finalidade                                                                                             |
| -------------------- | ------------------------------------------------------------------------------------------------------ |
| `cnaeCode`           | CNAE avaliado.                                                                                         |
| `cnaeDescription`    | Descrição oficial do CNAE no momento do cálculo.                                                       |
| `opportunityScore`   | Score consolidado calculado pelo OPRM.                                                                 |
| `scoreComponents`    | Componentes estruturados do score, em campos explícitos ou tabela filha, evitando JSON dentro de JSON. |
| `scoreJustification` | Justificativa objetiva do score.                                                                       |
| `algorithmVersion`   | Versão da regra/algoritmo do OPRM usado no cálculo.                                                    |
| `cycleId`            | Ciclo que gerou ou atualizou o score.                                                                  |
| `scoredAt`           | Data/hora do cálculo.                                                                                  |
| `scoreStatus`        | Estado operacional do score.                                                                           |

### Candidato de nicho

| Campo                 | Finalidade                                                  |
| --------------------- | ----------------------------------------------------------- |
| `id`                  | Identificador do candidato.                                 |
| `cnaeCode`            | CNAE de origem.                                             |
| `cnaeDescription`     | Descrição oficial do CNAE no momento da geração.            |
| `candidateNicheName`  | Nome acionável do nicho, não apenas a descrição CNAE.       |
| `persona`             | Pessoa/ocupação ou tipo de negócio principal.               |
| `painHypothesis`      | Dor operacional ou comercial provável.                      |
| `desiredOutcome`      | Resultado concreto desejado pelo público.                   |
| `mechanismHypothesis` | Mecanismo plausível para gerar transformação.               |
| `proofDirection`      | Direção inicial de prova/evidência.                         |
| `offerIdea`           | Ideia inicial de produto digital ou oferta.                 |
| `marketVolumeSignals` | Métricas usadas: MEI, Simples, estabelecimentos ativos etc. |
| `opportunityScore`    | Score consolidado usado para priorização.                   |
| `scoreCycleId`        | Ciclo de score que originou a priorização.                  |
| `enrichmentCycleId`   | Ciclo de enriquecimento que gerou o candidato.              |
| `status`              | Estado operacional do candidato.                            |
| `sourceArtifacts`     | Referências aos artefatos OPRM/MDS usados.                  |

## Observações de implementação futura

- A criação automática de `MarketNiche` deve ser evitada; o sistema deve criar **candidatos** e deixar o operador aprovar.
- A tela de CNAEs não deve exigir ação manual para gerar score. Ela deve mostrar score, status, último ciclo e próximos passos.
- A UI pode ter comandos operacionais de acompanhamento, como abrir ciclo, ver falhas, abrir candidatos, aprovar e rejeitar. Não deve virar uma tela com excesso de comandos técnicos.
- O backend deve manter a persistência relacional e evitar JSON dentro de JSON; campos estruturados devem ter contrato explícito.
- O backend não deve chamar MDS, Worker AI ou fontes externas para este fluxo. Essas chamadas pertencem ao módulo OPRM.
- O OPRM deve publicar artefatos finais sem comentários técnicos, flags de debug ou metadados operacionais no conteúdo final apresentável ao usuário.
- Quando a etapa acionar pesquisa web ou ingestão externa, o payload bruto recebido deve ser registrado antes de transformações, conforme regra operacional de logs de ingestão.
- Logs do OPRM devem incluir `cycleId`, `cycleType`, `cycleNumber`, `cnaeCode`, operação executada e exceção completa em caso de falha capturada.
