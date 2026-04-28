# Diagnóstico — Experimento 15 (aba Landing) — 2026-04-28

## Contexto
Solicitação: após clicar em **IA + LHM** para gerar HTML do pipeline, não aparecem as duas URLs esperadas na aba **Landing**.

## Evidências coletadas via MCP Server (`https://mcpserverdigi.shop/mcp`)

### 1) Estado do experimento 15
- A coluna `landing_page_html` do experimento 15 está preenchida (`has_html = 1`, `html_len = 21778`).
- Ou seja, existe HTML armazenado no experimento.

### 2) Tabela `landing_page` (fonte da lista de landings publicadas)
- Não existe nenhum registro para `experiment_id = 15`.
- Resultado da consulta: **0 linhas**.

Impacto: a grade/lista de landings na aba Landing fica vazia, e a UI exibe a mensagem “Nenhuma landing gerada ainda...” (mesmo havendo HTML no experimento).

### 3) Jobs do pipeline (`experiment_pipeline_generation_job`)
Para o experimento 15:
- Há um job `LANDING_PAGE_HTML` com `model = LHM` em status **COMPLETED**.
- Há também um job `LANDING_PAGE_HTML` com `model = gpt-5.2` em status **FAILED**.
- Erro do job falho: quebra de contrato de resposta do artefato HTML (retorno não aceito como HTML puro e inválido como JSON estrito).

### 4) Logs do módulo Java (`ai-worker`)
Nos logs:
- O job `fa4bbc85-5041-4644-bd0b-16bf0b0097fe` (LANDING_PAGE_HTML, experimento 15) recebe resposta da OpenAI.
- Em seguida, ocorre erro de contrato: “LANDING_PAGE_HTML não veio em HTML puro e também não é JSON válido”.
- O job termina como **failed**.

## Causa raiz observada
A etapa **LANDING_PAGE_HTML** teve execução concorrente/duplicada por modelo, com:
- sucesso em LHM;
- falha no job IA (gpt-5.2) por contrato de payload.

Com isso, o sistema fica em estado inconsistente para a aba Landing:
- há HTML no experimento;
- mas não há registro em `landing_page` para o experimento 15;
- portanto não surgem as entradas esperadas na seção de variantes/publicações.

## Ação corretiva recomendada
1. **Corrigir o contrato de saída do job IA (LANDING_PAGE_HTML)** para aceitar somente HTML puro no caminho principal (ou robustecer parser para caso `{"htmlDocument":"..."}` válido).
2. **Ajustar a consolidação de status por seção/modelo** para não bloquear publicação quando já existe versão válida (LHM) para o mesmo `section`.
3. **Backfill operacional** para o experimento 15:
   - reprocessar publicação da landing a partir do HTML já salvo;
   - criar os registros faltantes em `landing_page` (ou reexecutar fluxo de aprovação/publicação) para materializar as URLs na listagem da aba.
4. **Validação de UX**: alinhar os critérios de “landing gerada” (HTML em `experiment`) versus “landing publicada/listada” (`landing_page`) para evitar mensagem contraditória.
