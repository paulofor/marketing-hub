# OPRM Coletor MEI

Serviço Spring Boot do OPRM para orquestrar a ingestão da base pública de CNPJ da Receita Federal no backend do Marketing Hub, usando **arquivos ZIP oficiais** como fonte primária.

## Stack
- Java 21
- Spring Boot 3
- Maven
- Docker

## Execução local
```bash
cd oprm-coletor-mei
mvn spring-boot:run
```

Health check:
- `GET http://localhost:8094/api/oprm-mei/health`

## Pipeline real (produção): ingestão CNPJ por ZIP

A referência oficial do módulo OPRM é o pipeline de importação por snapshot CNPJ (Receita), com processamento dos ZIPs oficiais.

### Etapas do pipeline
1. Criar execução em `oprm_cnpj_import_run` (`STARTED`) e registrar arquivos em `oprm_cnpj_import_file`.
2. Fazer download de cada ZIP oficial para diretório temporário local por execução (`/tmp/oprm-cnpj-import/run-<runId>` por padrão).
3. Fazer unzip/leitura de cada arquivo com logs explícitos por etapa (`download`, `unzip`, `leitura`, `persistência de status`).
4. Publicar evento por arquivo no backend (`/files/{fileId}/events`) com `rowsRead/rowsValid/rowsRejected` e status.
5. Finalizar execução no backend (`/complete`) e limpar todos os arquivos temporários ao final (incluindo cenários de erro).

### Fontes de verdade do pipeline
- `docs/novos-modulos/OPRM/oprm-plano-importacao-cnpj-market-size.md`
- `docs/novos-modulos/OPRM/cnpj-open-data-2026-04-12.md`

## Pipeline NichoCNAE v3

O módulo também executa o pipeline `com.marketinghub.pipelines.nichocnae.v3` para pesquisa de rotina por CNAE. Todas as etapas devem gerar saídas funcionais compatíveis com seus objetivos, sem criar oferta, campanha ou landing page antes da etapa canônica apropriada:

1. `cnae-intake`: qualifica o CNAE e delimita o público como MEI/profissional autônomo não CLT.
2. `persona-candidate-generator`: gera personas operacionais candidatas com OpenAI, prompt/schema versionados e auditoria de request/response no backend.
3. `persona-tournament`: prioriza a persona com maior evidência de dor, tarefas e sinais de compra.
4. `routine-query-planner`: transforma a persona vencedora em plano de busca acionável para validar rotina real.
5. `source-searcher`: só libera avanço quando recebe fontes reais auditáveis; sem fontes, bloqueia com causa persistível.
6. `source-fetcher`: transforma fontes selecionadas em `sourceSnapshots` auditáveis, com URL, título, trecho de evidência e relevância de rotina.
7. `routine-signal-extractor`: extrai `routineSignals` com tarefa, dor operacional, sinal de compra e referência do snapshot.
8. `daily-tasks-synthesizer`: consolida `dailyTasks` com dor, evidência, fonte e alavanca de facilidade.
9. `quality-gate`: decide avanço por critérios persistíveis de evidência e informa causa/correção quando bloquear.
10. `persona-routine-materializer`: materializa o perfil aprovado com persona, rotina, dores, evidências e candidato funcional para o backend persistir em `market_niche` e `market_niche_enrichment_profile`.

A etapa `persona-candidate-generator`, quando chama OpenAI, audita o request pelo endpoint backend `recebeRequest` antes do envio ao provedor e audita o retorno pelo `recebeResponse` tanto em sucesso quanto em erro HTTP da OpenAI.

## Build Docker
```bash
docker build -t oprm-coletor-mei:local .
```
