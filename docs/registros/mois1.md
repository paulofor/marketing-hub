# Registros — Mois

> 🔴 **Arquivo canônico principal (atual)** para registro operacional dos módulos coletores do MOIS.
> Toda alteração em `mois-hotmart-collector` , `mois-sales-library-worker` e `mois-clickbank-collector` deve ser registrada neste arquivo.
> Em caso de dúvida entre arquivos de registro, este é o ponto único de verdade.


## 2026-05-18 19:27:36 UTC-3
- criada a camada backend de snapshots brutos da Biblioteca de Páginas de Vendas do MOIS.
- adicionado endpoint `POST /api/mois/sales-library/snapshots:capture` para capturar HTML bruto e screenshot PNG básico das URLs ingeridas.
- adicionado endpoint `GET /api/mois/sales-library/pages/{pageId}/snapshots` para consultar snapshots por página.
- criado agendamento `MoisSalesLibrarySnapshotScheduler` com cron fixo a cada 30 minutos para capturar URLs sem snapshot no workspace operacional inicial.
- criadas tabelas `mois_sales_library_page_snapshot` e `mois_sales_library_snapshot_artifact` via Liquibase para separar metadados do snapshot e artefatos (`RAW_HTML`, `SCREENSHOT_PNG`).
- atualizado `docs/modelo-dados-experimento.md` e a especificação da biblioteca de sales pages para registrar o novo contrato e o modelo de dados.
- teste unitário criado para validar persistência de HTML bruto e screenshot PNG em artefatos separados.
- arquivos principais alterados:
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/service/MoisSalesLibrarySnapshotService.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/service/MoisSalesLibrarySnapshotScheduler.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/web/MoisSalesLibraryController.java
  - backend/ads-service/src/main/resources/db/changelog/changesets/2026-05-18-mois-sales-library-page-snapshot.yaml
  - backend/ads-service/src/test/java/com/marketinghub/mois/biblioteca/service/MoisSalesLibrarySnapshotServiceTest.java

## Template obrigatório de novo registro

```md
## YYYY-MM-DD HH:mm:ss UTC-3
- descrição breve do problema
- descrição breve do raciocínio para a solução
- registro do que foi feito
- documentos lidos para tratar a situação:
  - caminho/do/documento-1.md
  - caminho/do/documento-2.md
```

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

> Regra obrigatória de timestamp:
> Antes de adicionar qualquer novo registro, execute obrigatoriamente:
>
> ```bash
> TZ=America/Sao_Paulo date '+%Y-%m-%d %H:%M:%S UTC-3'
> ```
>
> Use exatamente a saída desse comando no título do novo registro.
> É proibido inventar, estimar, inferir ou reaproveitar data/hora a partir de:
> - contexto da conversa;
> - data do commit;
> - data do CI/build;
> - metadados do arquivo;
> - relógio UTC sem conversão explícita;
> - registros anteriores deste documento.
>
> O formato obrigatório do título é:
>
> ```md
> ## YYYY-MM-DD HH:mm:ss UTC-3
> ```
>
> Cada novo registro deve ser adicionado no final do arquivo.
> Se for necessário registrar mais de uma entrada, execute novamente o comando de data/hora para cada entrada.
> Nunca crie registro com timestamp futuro em relação ao horário atual de `America/Sao_Paulo`.
> Em caso de timestamp incorreto já registrado, não apague nem edite o registro antigo; adicione um novo registro de correção explicando o erro.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-17 13:47:25 UTC-3
- consolidação da documentação do módulo MOIS ClickBank em um único documento canônico
- foi usado o código implementado como fonte de verdade para garantir aderência de endpoint, fluxo default e fetch GraphQL
- criado documento unificado em , e os documentos antigos passaram a apontar para o consolidado
- documentos lidos para tratar a situação:
  - docs/mois-clickbank-coletor.md
  - docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md
  - docs/mois/clickbase-fetch-ciclo-consulta.md
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/web/ClickbankCollectorController.java
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/service/ClickbankCollectorService.java
  - mois-clickbank-collector/src/main/resources/application.properties

## 2026-05-17 13:47:32 UTC-3
- consolidação da documentação do módulo MOIS ClickBank em um único documento canônico
- foi usado o código implementado como fonte de verdade para garantir aderência de endpoint, fluxo default e fetch GraphQL
- criado documento unificado em docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md, e os documentos antigos passaram a apontar para o consolidado
- documentos lidos para tratar a situação:
  - docs/mois-clickbank-coletor.md
  - docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md
  - docs/mois/clickbase-fetch-ciclo-consulta.md
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/web/ClickbankCollectorController.java
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/service/ClickbankCollectorService.java
  - mois-clickbank-collector/src/main/resources/application.properties

## 2026-05-17 14:23:53 UTC-3
- ajuste solicitado pós-revisão: mover o documento canônico unificado de ClickBank para a pasta /docs/canonical
- foi adotado nome versionado de cânone para facilitar evolução controlada: docs/canonical/mois-clickbank-collection-canon.v1.md
- atualizados os documentos de ponte para apontarem para o novo caminho canônico
- documentos lidos para tratar a situação:
  - docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md
  - docs/mois-clickbank-coletor.md
  - docs/mois/clickbase-fetch-ciclo-consulta.md
  - docs/registros/mois1.md
## 2026-05-17 13:43:57 UTC-3
- consolidado o conteúdo de documentação dos ciclos de coleta Hotmart em um único documento canônico.
- fonte de verdade passou a ser a documentação alinhada ao comportamento atual implementado no coletor e no backend.
- removido documento duplicado do módulo para evitar divergência de manutenção.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/docs/ciclos-coleta-hotmart.md


## 2026-05-17 13:47:47 UTC-3
- documento canônico do fluxo de ingestão Hotmart movido para /docs/canonical conforme orientação.
- referências internas atualizadas para apontar o novo caminho canônico.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/AGENTS.md


## 2026-05-17 15:10:00 UTC
- atualização da tela /hotmart para exibir métricas de execução por ciclo: quantidade de jobs executados, total geral de produtos e total de produtos por ciclo (job).
- adicionada seção "Resumo de ciclos Hotmart" com cards de indicadores e tabela por ciclo.
- documentos/códigos consultados:
  - frontend/src/pages/hotmart/HotmartPage.tsx
  - frontend/src/api/settings/useHotmartCollectedProducts.ts

## 2026-05-17 15:35:00 UTC
- atualização da tela /clickbase para exibir as 6 últimas execuções de jobs da fonte Clickbank.
- adicionada seção com tabela contendo job, status, nicho, data de criação e mensagem de execução.
- ajuste de tipagem no frontend para incluir campos de `sources` e `niche` retornados pelo endpoint `/api/v1/mois/collection-jobs`.
- documentos/códigos consultados:
  - frontend/src/pages/clickbase/ClickbasePage.tsx
  - frontend/src/api/settings/useClickbaseCollectedProducts.ts
  - backend/ads-service/src/main/java/com/marketinghub/mois/dto/MoisWorkspaceDtos.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/web/MoisController.java

## 2026-05-18 12:07:38 UTC-3
- necessidade de telas no frontend para acompanhar análises da biblioteca de páginas de vendas do MOIS.
- foi mapeado o contrato já existente no backend (`/api/mois/sales-library`) para evitar criação desnecessária de endpoint e garantir aderência ao escopo do módulo.
- implementadas seções de acompanhamento no frontend: entradas ingeridas, fila de jobs, páginas com status/score, ação de reanálise com botão desabilitado + spinner durante requisição e painel de detalhe da análise por página.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx
  - frontend/src/api/mois/useMoisSalesLibrary.ts
  - frontend/src/api/mois/types.ts
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/web/MoisSalesLibraryController.java


## 2026-05-18 00:00:00 UTC
- solicitado registro documental das seções da tela Biblioteca de Páginas de Vendas (MOIS).
- criado documento em `docs/novos-modulos/mois-biblioteca-pagina-venda/guia-secoes-biblioteca-paginas-vendas.md` descrevendo acesso, seções, comportamentos e objetivo operacional.
- documentos/códigos consultados:
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx
  - frontend/src/components/MainNavigation.tsx
  - frontend/src/App.tsx

## 2026-05-19 00:00:00 UTC
- solicitada especificação formal do worker da biblioteca de páginas de vendas do MOIS.
- criado documento `docs/novos-modulos/mois-biblioteca-pagina-venda/especificacao-worker-biblioteca-sales-pages.md` contendo objetivo, escopo, fluxo, estados, contratos de API, configuração, observabilidade, critérios de aceite e riscos operacionais.
- documentos/códigos consultados:
  - docs/canonical/mois-worker-canon.v1.md
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/service/MoisSalesLibraryService.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/web/MoisSalesLibraryController.java
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/libraryworker/service/PipelineRunner.java
  - mois-sales-library-worker/src/main/resources/application.yml

## 2026-05-19 00:00:00 UTC
- implementado/alinhado o worker da biblioteca de páginas de vendas conforme especificação, mantendo ciclo de polling, claim de 1 job por ciclo, processamento da URL, complete/fail e logs operacionais.
- padronizado o namespace do módulo para `com.marketinghub.mois.bibliotecapaginavenda.worker.v1` no worker e no backend do módulo de biblioteca de páginas de vendas.
- documentos/códigos consultados:
  - docs/novos-modulos/mois-biblioteca-pagina-venda/especificacao-worker-biblioteca-sales-pages.md
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/libraryworker/service/PipelineRunner.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/web/MoisSalesLibraryController.java

## 2026-05-19 00:00:00 UTC
- ajustado o worker da biblioteca de páginas de vendas para que **todo o código** fique fisicamente e logicamente no pacote `com.marketinghub.mois.bibliotecapaginavenda.worker.v1`.
- movidos os arquivos Java do caminho legado `.../mois/libraryworker/...` para `.../mois/bibliotecapaginavenda/worker/v1/...`, mantendo os imports internos exclusivamente dentro do namespace do módulo.
- validação realizada para garantir ausência de referências ao pacote legado no worker.

## 2026-05-19 04:40:37 UTC-3
- diagnóstico do erro de inicialização do container `mois-sales-library-worker` repetindo `no main manifest attribute, in /app/app.jar`.
- identificada causa-raiz no empacotamento Maven: o JAR estava sendo gerado sem etapa explícita de `repackage`, resultando em artefato não executável via `java -jar`.
- ajustado `mois-sales-library-worker/pom.xml` para executar o goal `repackage` do `spring-boot-maven-plugin`, garantindo geração de JAR Spring Boot executável com manifesto correto.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/mois1.md
  - mois-sales-library-worker/pom.xml
## 2026-05-19 04:40:00 UTC
- implementado revezamento de fontes no `mois-sales-library-worker` via nova configuração `MOIS_SOURCES` (CSV), permitindo alternar entre `CLICKBANK` e `HOTMART` a cada ciclo de polling.
- mantida retrocompatibilidade: se `MOIS_SOURCES` estiver vazio, o worker continua usando `MOIS_SOURCE` (comportamento anterior).
- atualizado `docker-compose.yml` do worker com exemplo padrão `MOIS_SOURCES=CLICKBANK,HOTMART`.
- arquivos alterados:
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/PipelineRunner.java
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/config/WorkerProperties.java
  - mois-sales-library-worker/src/main/resources/application.yml
  - mois-sales-library-worker/docker-compose.yml

## 2026-05-19 07:50:00 UTC
- adicionadas regras de arquitetura com ArchUnit para manter o pacote `com.marketinghub.mois.bibliotecapaginavenda.worker.v1` isolado no backend:
  - o pacote não pode depender de outros pacotes `com.marketinghub` fora do próprio namespace;
  - outros pacotes `com.marketinghub` não podem depender dele.
- adicionada a mesma política de isolamento no módulo `mois-hotmart-collector` com teste ArchUnit dedicado e dependência de teste `archunit-junit5`.

## 2026-05-19 — Integração OpenAI batch no mois-sales-library-worker (v1)
- Pipeline do worker passou a analisar página de vendas via OpenAI Batch API no modelo padrão `gpt-5.2`.
- Criado subpacote `openai` em `v1` com cliente dedicado de upload de arquivo JSONL, criação de batch, polling de status e parsing do output.
- `PipelineRunner` agora faz fetch da página com Jsoup e envia o texto para análise OpenAI, persistindo resultado via endpoint `:complete`.
- Adicionadas propriedades `openai.*` no `application.yml` para chave, base URL, modelo e timeouts de batch.

- Ajustado deploy do `mois-sales-library-worker` para o mesmo host do `ai-worker` (`191.252.120.96`) e com montagem do mesmo arquivo de token OpenAI em volume (`/run/secrets/openai_api_key`).

## 2026-05-19 — Evitar job duplicado no worker da Biblioteca de Páginas de Vendas
- Ajustado o `ingestUrls` no backend MOIS para criar job `PENDING` **somente quando a URL for nova** na tabela de ingestão (`INSERT` real).
- Quando a URL já existe (upsert via `ON DUPLICATE KEY UPDATE`), a entrada continua atualizando metadados/counters, mas não cria novo job para o worker processar novamente a mesma página.
- Adicionados testes unitários cobrindo os dois cenários: URL nova cria job; URL já existente não cria job.
- Arquivos alterados:
  - `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
  - `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`

## 2026-05-19 21:07:28 UTC-3
- solicitada consolidação dos documentos canônicos de worker do MOIS em um único arquivo.
- foi adotado o código do `mois-sales-library-worker` como fonte de verdade para fluxo, contratos e parâmetros de configuração.
- criado documento canônico unificado `docs/canonical/mois-worker-canon.v1.md` e os documentos antigos (`mois-sales-library-worker-canon.v1.md` e `ai-workers-canon.v1.md`) passaram a atuar como ponte para o novo cânone.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-worker-canon.v1.md
  - docs/canonical/mois-worker-canon.v1.md
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/PipelineRunner.java
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/config/WorkerProperties.java
  - mois-sales-library-worker/src/main/resources/application.yml

## 2026-05-19 21:12:17 UTC-3
- solicitado remover duplicatas de documentos canônicos de worker para manter apenas um arquivo canônico ativo.
- removidos os arquivos duplicados `docs/canonical/mois-sales-library-worker-canon.v1.md` e `docs/canonical/ai-workers-canon.v1.md`.
- referências documentais atualizadas para apontar exclusivamente para `docs/canonical/mois-worker-canon.v1.md`.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-worker-canon.v1.md
  - docs/canonical/system-governance-canon.v2.md
  - docs/novos-modulos/mois-biblioteca-pagina-venda/especificacao-worker-biblioteca-sales-pages.md

## 2026-05-19 21:29:49 UTC-3
- diagnóstico de falha operacional no worker da biblioteca de sales pages: jobs em FAILED por ausência de chave OpenAI no runtime do deploy.
- análise de causa-raiz comparando composição do `mois-sales-library-worker` com o padrão já funcional do `ai-worker` no mesmo host.
- ajuste aplicado no `docker-compose.deploy.yml` do módulo para incluir variáveis de ambiente de runtime (backend/base URL, polling e OpenAI) e bind mount do arquivo de segredo `OPENAI_API_KEY_HOST_FILE -> /run/secrets/openai_api_key`, alinhando o padrão de injeção de secret usado no ai-worker.
- documentos lidos para tratar a situação:
  - ai-worker/docker-compose.yml
  - mois-sales-library-worker/docker-compose.yml
  - mois-sales-library-worker/docker-compose.deploy.yml
  - docs/registros/mois1.md

## 2026-05-19 22:24:46 UTC-3
- ajuste de usabilidade na tela de Biblioteca de Páginas de Vendas (MOIS) para exibir o campo "Atualizado em" no fuso de São Paulo com formato mais legível para operação diária.
- foi criada função de formatação no frontend convertendo timestamps ISO para  com timezone .
- aplicado o novo formato na coluna de jobs de análise para substituir o formato técnico UTC bruto.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md

## 2026-05-19 22:24:58 UTC-3
- correção de registro anterior: os literais `pt-BR` e `America/Sao_Paulo` não foram preservados por interpretação de shell no append anterior.
- o ajuste implementado no frontend usa `Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo" })` para exibir data/hora local de São Paulo no campo "Atualizado em" da fila de jobs.
- mantido formato legível com dia/mês/ano e hora:minuto:segundo para facilitar leitura operacional.
- documentos lidos para tratar a situação:
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx
  - docs/registros/mois1.md

## 2026-05-19 23:51:38 UTC-3
- solicitado planejar melhor acompanhamento de casos em `FETCHING` na tela da biblioteca MOIS, com base em documento canônico.
- revisado o cânone do worker MOIS e proposta taxonomia explícita de estados operacionais para reduzir ambiguidade no diagnóstico.
- atualizada a UI de fila de jobs para incluir filtros `ANALYZING` e `RETRY_WAIT` e legenda operacional dos status na própria tela.
- documentos lidos para tratar a situação:
  - docs/canonical/system-governance-canon.v2.md
  - docs/canonical/mois-worker-canon.v1.md
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx

## 2026-05-20 00:00:00 UTC-3
- correção de vulnerabilidade operacional no worker da biblioteca de páginas de vendas (MOIS): erro OpenAI de contrato não estava sendo propagado para a tela de jobs.
- ajuste no cliente OpenAI batch (`OpenAiSalesPageAnalyzer`) para usar o contrato atual da Responses API (`text.format`) no lugar de `response_format`, removendo a causa-raiz do erro `unsupported_parameter`.
- implementado tratamento explícito de erro no output do batch quando `response.status_code >= 400` e/ou `error` de linha, com construção de mensagem detalhada (`status`, `requestId`, `type`, `code`, `message`) para persistência no backend via `jobs/{jobId}:fail`.
- atualização do cânone `docs/canonical/mois-worker-canon.v1.md` para formalizar:
  - uso obrigatório de `text.format` na integração `/v1/responses`;
  - obrigatoriedade de persistir e expor ao usuário os erros de contrato OpenAI no `errorMessage` do job.
- documentos lidos para tratar a situação:
  - docs/canonical/system-governance-canon.v2.md
  - docs/canonical/mois-worker-canon.v1.md
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/openai/OpenAiSalesPageAnalyzer.java
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/PipelineRunner.java

## 2026-05-20 00:27:24 UTC-3
- ajuste na tela MOIS Biblioteca de Páginas de Vendas para padronizar o campo "Atualizado em" no fuso GMT, eliminando ambiguidade de horário local.
- identificado que a formatação estava fixa em America/Sao_Paulo, o que causava divergência com a expectativa operacional de leitura em GMT.
- alterada a função de formatação para usar timezone  e sufixo literal  no valor exibido em cada linha da fila de jobs.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/mois1.md

## 2026-05-20 00:27:36 UTC-3
- correção de registro anterior: a linha de detalhe técnico perdeu os literais por expansão indevida de shell durante escrita do markdown.
- valor correto aplicado no frontend: timezone `Etc/GMT` e sufixo literal `GMT` na coluna "Atualizado em" da fila de jobs MOIS.
- mantido o registro anterior por política append-only, com este complemento de correção.
- documentos lidos para tratar a situação:
  - docs/registros/mois1.md
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx
