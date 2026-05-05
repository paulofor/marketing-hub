# Sugestão de Prompt Único — Criação do Fluxo de Geração de Prompt (Worker AI)

## Objetivo

Este documento traz um **prompt único, completo e pronto para execução** para implementar o Fluxo 01 do Gera Landing (Worker AI), incluindo backend, worker, testes e documentação, com aderência aos cânones do projeto.

---

## Prompt completo (copiar e usar)

Você é um agente de engenharia sênior atuando no repositório **Marketing Hub**. Execute em **uma única implementação ponta a ponta** a criação (ou ajuste final) do **Fluxo 01 — geração de prompt pelo Worker AI** do módulo Gera Landing.

Siga rigorosamente as regras abaixo.

### 1) Missão de negócio e direção funcional

- Preserve o framework do produto: **Dor → Resultado → Mecanismo → Prova → Oferta**.
- O fluxo deve pegar execuções pendentes com status `INICIADO`, montar o prompt da etapa e enviar ao backend para transição para `EM_PROCESSAMENTO`.
- Entregar solução operacional real (produção), sem mock definitivo e sem workaround frágil.

### 2) Fontes de verdade obrigatórias (ler antes de codar)

1. `docs/canonical/system-governance-canon.v2.md`
2. `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
3. `docs/gera-landing/modelo-canonico-gera-landing.md`
4. `docs/modelo-dados-experimento.md`
5. `docs/database/liquibase-mysql57.md`

Se houver divergência entre código e documentação canônica, **alinhe código + testes + documentação**.

### 3) Arquitetura e restrições mandatórias

- Stack padrão: Java 21 + Spring Boot 3 (backend), Worker AI do projeto.
- Banco: **MySQL 5.7**.
- Apenas o **backend** acessa banco.
- Worker fala com backend via HTTP interno.
- Não criar acesso direto do worker ao banco.
- Evitar “JSON dentro de JSON” em campos de texto quando não for estritamente necessário.

### 4) Uso obrigatório de MCP Server para validação operacional

Você deve usar o MCP Server `https://mcpserverdigi.shop/mcp` via JSON-RPC quando precisar validar conectividade e consistência operacional, especialmente para:

- checar saúde de banco (tool `db_health`),
- consultar logs de backend em caso de erro HTTP,
- confirmar dados reais quando necessário para diagnóstico.

Se houver `400`, inspecione URL/método/header/body e compare com DTO/contrato.  
Se houver `422`, siga SOP obrigatório:
1. logs via MCP,
2. payload literal enviado,
3. especificação literal esperada,
4. diferença exata,
5. validação que rejeitou,
6. ação corretiva (priorizar ajuste de prompt/contrato canônico).

### 5) Escopo técnico obrigatório

Implemente/ajuste os itens a seguir:

#### 5.1 Worker AI — polling de pendências

- Scheduler com cron configurável:
  - `geralanding.execution.fixed-cron` (default `0 */1 * * * *`)
- Limite configurável:
  - `geralanding.execution.pending-limit` (default `20`, mínimo efetivo `1`)
- Endpoint de leitura:
  - `GET /api/internal/geralanding/stage-executions/pending?limit={n}`

#### 5.2 Contrato de pendências

Consumir pendências com campos obrigatórios:
- `experimentId`
- `idJob`
- `stageCode`

Se item vier inválido (ex.: `idJob` ou `stageCode` ausente), pular com log estruturado.

#### 5.3 Filtro de etapa

- Normalizar `stageCode` com `trim().toLowerCase()`.
- Nesta versão, processar somente `landing-page-wireframe`.

#### 5.4 Montagem de prompt

- Base em arquivo: `prompts/geralanding/{etapa}.md`
- Resolver placeholders:
  - `{prompt-*}`: inclusão recursiva de prompts
  - `{dados-*}`: serialização JSON legível de contexto
- Implementar proteção contra ciclo de referência entre prompts, com erro explícito.

#### 5.5 Callback de persistência

- Endpoint:
  - `POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`
- Regra crítica:
  - usar no path **o mesmo `idJob` recebido em `/pending`**.
- Body obrigatório:
  - `experimentId`
  - `stageCode`
  - `prompt`

#### 5.6 Backend — persistência e status

Ao receber callback:
- localizar por `idJob` (prioritário),
- fallback por `(experimentId, stageCode)` quando necessário,
- persistir `prompt` (texto longo),
- preencher `processing_started_at`,
- mudar status para `EM_PROCESSAMENTO`,
- retornar `202 Accepted` no sucesso.

#### 5.7 Schema/Liquibase

Garantir aderência de `gera_landing_stage_execution`:
- `id_job` como PK ativa,
- `prompt`/`prompt_content` com tipo compatível com texto longo (`LONGTEXT`),
- migrations incrementais Liquibase YAML com preconditions para MySQL 5.7 quando necessário.

#### 5.8 Observabilidade

- Logs com `experimentId`, `idJob`, `stageCode`, tempo e resultado.
- Em falhas HTTP, logar URL, status code e response body.

### 6) Testes obrigatórios

#### Worker

- teste: callback usa mesmo `idJob` de `/pending`
- teste: filtro por `stageCode`
- teste: skip de payload inválido
- teste: proteção contra referência circular

#### Backend

- teste controller: `/pending`
- teste controller: `/receive-prompt`
- teste service: persistência de `prompt`, `processing_started_at` e transição para `EM_PROCESSAMENTO`

Sempre atualizar testes afetados por mudanças de contrato/regra canônica.

### 7) Documentação obrigatória

Atualize, ao final:
- `docs/gera-landing/modelo-canonico-gera-landing.md` (estado real implementado)
- `docs/gera-landing/registros.md` (data/hora, resumo técnico e impacto)
- se houver mudança de entidade/relacionamento, atualizar `docs/modelo-dados-experimento.md`

### 8) Critérios de aceite (todos obrigatórios)

1. Pendências `INICIADO` são consumidas corretamente.
2. Etapa `landing-page-wireframe` gera prompt final válido.
3. Callback usa exatamente o `idJob` da pendência.
4. Backend persiste prompt, seta `processing_started_at` e muda para `EM_PROCESSAMENTO`.
5. Testes dos módulos alterados passam.
6. Documentação canônica e registros ficam sincronizados com o código.

### 9) Entregáveis finais

- Código backend + worker do fluxo completo.
- Testes automatizados cobrindo cenário principal e erros críticos.
- Documentação atualizada.
- Commit(s) claro(s) com mensagem objetiva.
- Resumo final com:
  - arquivos alterados,
  - decisões técnicas,
  - evidências de teste,
  - riscos/pendências.
