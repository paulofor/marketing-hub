## 2026-06-27 — MOIS Biblioteca de Páginas: atualização da tela após reprocessar dossiê
- Causa-raiz identificada na página `/mois/sales-pages-library/286`: o backend aceitou o reprocessamento e o worker concluiu o dossiê, mas a tela de detalhe não invalidava a query da própria página ao clicar em **Reprocessar dossiê** e também não fazia polling do estado do dossiê.
- Correção aplicada no frontend: o comando agora invalida a query do detalhe da página e as consultas de detalhe/pipeline do dossiê são atualizadas periodicamente, reduzindo a chance de a tela mostrar status antigo enquanto o backend já avançou.
- Verificação operacional: via MCP, a página 286 ficou com `status_pipeline_dossieproduto=CONCLUIDO` e etapa `dossier-synthesis`; logs do `mois-sales-library-worker` confirmaram execução das etapas após a solicitação.

## 2026-06-17 — Protocolo padrão backend na Biblioteca de Páginas de Vendas
- aplicado o protocolo padrão backend no pacote `com.marketinghub.mois.bibliotecapaginavenda.worker.v1` com regras ArchUnit para controller único/canônico, fachada de service canônica, contratos imutáveis e persistência centralizada no pacote `com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1`.
- objetivo: reduzir risco de dispersão arquitetural na Biblioteca de Páginas de Vendas e preservar a capacidade do MOIS de transformar páginas coletadas em inteligência comercial para vendas.

## 2026-06-16 — Objetivo comercial da Biblioteca de Páginas de Vendas
- atualizado o cânone MOIS para deixar claro que a Biblioteca de Páginas de Vendas não tem como objetivo principal encontrar oportunidades de mercado.
- registrado que o objetivo correto é identificar e reutilizar fórmulas comerciais que já vendem com consistência no digital, copiando a estrutura de venda vencedora sem plagiar conteúdo literal, marca, identidade visual ou ativos de terceiros.
- alinhadas especificações complementares da biblioteca para reforçar que rankings, scores e análises devem responder qual fórmula de venda funciona e como adaptá-la para produtos próprios do Marketing Hub.

## 2026-06-11 18:20:00 UTC
- corrigido o cálculo de custo da análise da Biblioteca de Páginas de Vendas do MOIS para não depender diretamente do pacote `com.marketinghub.openai`, preservando o isolamento arquitetural validado por ArchUnit.
- criado serviço local no pacote `com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service` para calcular preço batch a partir dos tokens do worker.
- criada leitura JDBC no gateway/repositório permitido `com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1` para obter `price_input_batch` e `price_output_batch` da tabela canônica `openai_model`.
- adicionados testes unitários do cálculo e da leitura de preços, além da validação de arquitetura para prevenir recorrência do acoplamento com OpenAI central.

## 2026-06-07 — Timeout de captura HTML ampliado para 5 minutos

- aumentado o timeout de captura HTTP/HTML da Biblioteca de Páginas de Vendas do MOIS para 5 minutos (`300000 ms`) no backend e no worker.
- atualizado o cânone MOIS para registrar o novo timeout operacional padrão de captura, mantendo cooldown e limite de falhas como proteção contra loops improdutivos.
- atualizado o documento de especificação do worker para refletir o novo valor padrão de `MOIS_REQUEST_TIMEOUT_MS`.
- documentos/códigos consultados:
  - backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotService.java
  - mois-sales-library-worker/src/main/resources/application.yml
  - docs/canonical/mois-worker-canon.v1.md
  - docs/novos-modulos/mois-biblioteca-pagina-venda/especificacao-worker-biblioteca-sales-pages.md


## 2026-06-03 — Bootstrap Hotmart 400 para Biblioteca de Páginas de Vendas

- implementado endpoint backend `POST /api/mois/sales-library/hotmart-products:ingest` para ingerir até 400 produtos Hotmart já persistidos em `mois_collected_reference` na Biblioteca de Páginas de Vendas.
- o fluxo usa `sales_page_url` com fallback em `product_url` e `url`, grava/deduplica em `mois_sales_library_url_ingest` e cria jobs `PENDING` apenas para URLs novas.
- atualizado o cânone MOIS para registrar o contrato de bootstrap operacional do MVP 1 da biblioteca.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/novos-modulos/mois-biblioteca-pagina-venda/plano-pipeline-biblioteca-paginas-venda-gerado-por-chatgpt.md`

# Registros — Mois

> 🔴 **Arquivo canônico principal (atual)** para registro operacional dos módulos coletores do MOIS.
> Toda alteração em `mois-hotmart-collector` , `mois-sales-library-worker` e `mois-clickbank-collector` deve ser registrada neste arquivo.
> Em caso de dúvida entre arquivos de registro, este é o ponto único de verdade.



## 2026-05-29 - Agendamento pontual do ciclo 1 Hotmart

- ajustado o agendamento do ciclo 1 do `mois-hotmart-collector` para executar em 29/05 às 19:30 no timezone `America/Sao_Paulo`, conforme solicitação operacional.
- mantido cron literal diretamente na anotação `@Scheduled`, conforme regra operacional para agendamentos Spring Boot.

Arquivos alterados:
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorScheduler.java`

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

## 2026-05-20 11:14:37 UTC-3
- ajuste na tela de Biblioteca de Páginas de Vendas (MOIS) para exibir o JSON de retorno da OpenAI dentro da coluna de erro da fila de jobs.
- foi criada formatação defensiva para detectar trecho JSON no erro bruto e renderizar com indentação, preservando o prefixo textual quando existir.
- a coluna Erro agora usa bloco com quebra de linha para tornar legível o payload de erro retornado pelo backend/OpenAI.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/mois1.md

## 2026-05-20 11:17:54 UTC-3
- refinamento da exibição de erro OpenAI na tela de jobs MOIS para ficar aderente ao cânone operacional: destaque explícito do bloco `OpenAI JSON` quando o payload JSON estiver presente.
- corrigida a detecção de início de JSON para evitar comportamento implícito com listas vazias e manter fallback do erro bruto quando não houver JSON válido.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-worker-canon.v1.md
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx
  - docs/registros/mois1.md

## 2026-05-20 11:21:48 UTC-3
- atualização de regra canônica do MOIS Worker para erros de integração OpenAI: passou a ser obrigatório obter/processar também o `error_file_id` (JSONL) em caso de falha batch.
- refinada a seção de conformidade para explicitar que o JSONL de erro é parte do diagnóstico de causa-raiz e da composição da mensagem persistida/exibida no job.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md

## 2026-05-20 11:26:28 UTC-3
- implementação no worker da biblioteca de páginas MOIS para tratamento de erro OpenAI batch com captura de `error_file_id` (JSONL) quando status terminal não for `completed` ou quando faltar `output_file_id`.
- mensagens de falha agora carregam contexto operacional (`batchId`, `status`, `outputFileId`, `errorFileId`) e incluem o conteúdo `error_jsonl` quando disponível.
- adicionada leitura defensiva de `error_file_id` com log explícito em caso de falha de download do JSONL.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-worker-canon.v1.md
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/openai/OpenAiSalesPageAnalyzer.java
  - docs/registros/mois1.md

## 2026-05-20 14:47:23 UTC-3
- aumento do timeout do modo batch OpenAI no worker da biblioteca de páginas de vendas (MOIS) para 30 minutos efetivos, ajustando o default de `OPENAI_BATCH_TIMEOUT_MS` para `1800000` no `application.yml`.
- atualização do cânone `docs/canonical/mois-worker-canon.v1.md` para manter consistência documental com o novo valor default de timeout (`1800000 ms` / `PT30M`).
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - mois-sales-library-worker/src/main/resources/application.yml
  - docs/registros/mois1.md

## 2026-05-20 20:49:01 UTC
- correção no `backend/ads-service` para validação do Liquibase: o `include` do changelog master apontava para `db/changelog/changesets/...` sem `relativeToChangelogFile`, causando erro de arquivo não encontrado no classpath.
- ajuste aplicado em `src/main/resources/db/changelog/db.changelog-master.yaml` para usar `changesets/2026-05-17-mois-sales-library-analysis.yaml` com `relativeToChangelogFile: true`, alinhando com o padrão dos demais includes.
- validação executada: `mvn -q liquibase:validate` (neste ambiente falhou por ausência de `database URL`, sem novo erro de arquivo ausente).

## 2026-05-20 21:05:00 UTC
- correção de causa-raiz para `ValidationFailedException` do Liquibase por `duplicate identifiers`: o mesmo changelog `changesets/2026-05-17-mois-sales-library-analysis.yaml` estava incluído duas vezes no `db.changelog-master.yaml` do `backend/ads-service`.
- removido o include duplicado no topo do master, mantendo apenas a inclusão única já existente na seção cronológica de changesets.
- impacto: elimina duplicidade de changeset id (`2026-05-17-mois-sales-library-analysis-01::codex`) durante `liquibase validate/update`.

## 2026-05-20 21:20:00 UTC
- frontend MOIS (`/mois/sales-pages-library`): os blocos de JSON em "Detalhes da análise da página" foram migrados para visualização colapsável usando `CollapsibleJsonViewer`, substituindo `<pre>` estático para facilitar leitura e navegação de payloads longos.
- seções afetadas: `Seções (JSON)`, `Copy (JSON)`, `Visual (JSON)` e `Imagem (JSON)`.
- arquivo alterado: `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`.

## 2026-05-21 — Diagrama canônico do fluxo de alimentação da biblioteca de páginas de vendas
- solicitada melhoria do documento canônico existente com explicação textual e diagrama ponta a ponta do fluxo de alimentação da biblioteca de páginas de vendas.
- atualizado `docs/canonical/mois-worker-canon.v1.md` com seção nova contendo:
  - ingestão por Hotmart e ClickBank;
  - disponibilização de URL e criação de job `PENDING`;
  - rotina de claim/fetch/análise do worker;
  - prompt/schema (`text.format.type=json_object`) e campos esperados;
  - persistência final em `mois_sales_library_page_analysis` e transições `DONE`/`FAILED`.
- sem mudança de código executável; atualização estritamente documental canônica.


## 2026-05-21 03:45:00 UTC
- correção de sintaxe do diagrama Mermaid na seção `12.2 Diagrama (sequência ponta a ponta)` do cânone MOIS para eliminar erro de renderização no GitHub (`Parse error on line 19`).
- causa-raiz: quebras de linha literais dentro do texto das setas (`API->>DB`, `WK->>OAI`, `WK->>API`) sem escape adequado para Mermaid sequenceDiagram.
- correção aplicada: substituição das quebras por `<br/>` nas mensagens das setas, mantendo o mesmo conteúdo funcional.
- arquivo alterado: `docs/canonical/mois-worker-canon.v1.md`.

## 2026-05-21 04:20:00 UTC
- tela MOIS `Biblioteca de Páginas de Vendas` simplificada para exibir apenas uma tabela consolidada com produto coletado e fase atual, conforme o diagrama canônico `docs/canonical/mois-worker-canon.v1.md#12.2`.
- removidos da tela os blocos de entradas ingeridas, fila de jobs, paginação e detalhes de análise, mantendo foco na leitura operacional de fase por produto.
- adicionada função de mapeamento de status (`PENDING`, `FETCHING`, `ANALYZING`, `RETRY_WAIT`, `DONE`, `FAILED`) para fase textual do fluxo canônico.
- arquivo alterado: `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`.

## 2026-05-21 14:50:00 UTC
- frontend MOIS (`/mois/sales-pages-library`): adicionadas ações por linha para acessar o detalhe da análise e abrir a página original em nova aba (`target="_blank"`).
- criada nova rota de detalhe `/mois/sales-pages-library/:pageId` exibindo payloads de resposta do modelo em blocos colapsáveis (`<details>`), além de blocos colapsáveis para request enviado e prompt utilizado.
- todos os trechos JSON foram apresentados em formato colapsável para reduzir ruído visual e facilitar inspeção.
- arquivos alterados: `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`, `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`, `frontend/src/App.tsx`.

## 2026-05-21 15:20:00 UTC
- tela de detalhe da Biblioteca de Páginas de Vendas (MOIS) ganhou ações manuais de status: **Voltar para pendente** e **Marcar como anulado**.
- adicionado endpoint backend `POST /api/mois/sales-library/pages/{pageId}:status` para persistir transição manual de status (`PENDING` e `ANULADO`) com registro em `mois_sales_library_page_analysis`.
- adicionada navegação de produtividade no detalhe com botão **Próximo →** para avançar para o próximo item da lista.
- arquivos alterados: `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`, `frontend/src/api/mois/useMoisSalesLibrary.ts`, `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`, `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`.
## 2026-05-21 15:34:02 UTC-3
- solicitado ajuste na tela de detalhe da análise da biblioteca de páginas de vendas (MOIS) para reforçar contexto do produto e rastreabilidade operacional.
- substituído o título estático pelo nome do produto e adicionado subtítulo com o coletor/origem usado na obtenção.
- após os cards de JSON, foi adicionado um card "Histórico da página" com linha do tempo de eventos (coleta/ingestão, snapshots brutos e avaliação), sempre com data e hora formatadas.
- criadas consultas no frontend para endpoint de detalhe da página e snapshots já existentes no backend, sem necessidade de novo contrato.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx
  - frontend/src/api/mois/useMoisSalesLibrary.ts
  - frontend/src/api/mois/types.ts

## 2026-05-21 15:55:00 UTC
- correção de erro de compilação TypeScript na tela `MoisSalesPageLibraryDetailPage`.
- causa-raiz: uso de variável inexistente (`pageQuery`) e uso de `history` sem definição local, que colidia com o tipo global `History`.
- correção aplicada: adição das queries `useMoisSalesLibraryPage` e `useMoisSalesLibraryPageSnapshots`, além de construção explícita de `history: HistoryItem[]` a partir dos snapshots.
- arquivo alterado: `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`.

## 2026-05-21 20:58:00 UTC
- melhoria na tela de detalhe da Biblioteca de Páginas de Vendas (MOIS) para exibir JSON no formato de navegação hierárquica expansível (clicar para abrir/fechar nós), em vez de texto cru em `<pre>`.
- implementado renderizador recursivo de árvore JSON para objetos e arrays, com contagem de itens por nó e expansão inicial da raiz para facilitar inspeção.
- adicionado fallback seguro: quando o conteúdo não for JSON válido, a UI mantém exibição textual para não bloquear diagnóstico.
- arquivo alterado: `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`.

## 2026-05-22 23:20:00 UTC
- ajuste na tela de detalhe da Biblioteca de Páginas de Vendas (MOIS) para mostrar explicitamente a URL da página de venda usada como base da análise do modelo.
- a URL canônica (`urlCanonical`) agora aparece no cabeçalho e é clicável em nova aba (`target="_blank"`), facilitando auditoria do conteúdo de origem das respostas.
- arquivo alterado: `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`.

- 2026-05-22: Incluída regra operacional no AGENTS do módulo `mois-sales-library-worker` exigindo logs de request/resposta OpenAI e payload ao backend com `jobId` do Marketing Hub.

## 2026-05-23 00:00:00 UTC
- atualização do cânone MOIS (`docs/canonical/mois-worker-canon.v1.md`) com a nova **Seção 13 — Dados**.
- adicionado modelo de dados relacional compartilhado entre coletores (Hotmart/ClickBank) e projeto da Biblioteca de Páginas de Vendas.
- documentadas as tabelas centrais e seus papéis no fluxo: `mois_collected_reference`, `mois_sales_library_url_ingest`, `mois_sales_library_processing_job`, `mois_sales_library_page_analysis`, `mois_sales_library_page_snapshot` e `mois_sales_library_snapshot_artifact`.
- incluídas regras de integração canônicas entre coleta e biblioteca para preservar rastreabilidade e evitar escrita direta fora do backend.
- complemento solicitado pelo usuário: adicionado diagrama adicional detalhado de dados (ER com PK/FK/UK e cardinalidades) na seção 13.5 do cânone MOIS.
- ajuste solicitado: o diagrama da seção 13.5 foi simplificado para exibir **somente chaves** (PK/FK/UK), removendo campos não-chave.

## 2026-05-23 00:00:00 UTC
- atualização solicitada no cânone MOIS (`docs/canonical/mois-worker-canon.v1.md`) na seção **12.2 Diagrama (sequência ponta a ponta)** para explicitar quais tabelas são lidas e gravadas em cada etapa do fluxo.
- diagrama mermaid ajustado com marcações `READ` e `WRITE` por etapa (`/urls:ingest`, `/jobs:claim`, `/jobs/{jobId}:complete`, `/jobs/{jobId}:fail`).
- adicionada subseção **12.2.1 Tabelas lidas e gravadas por etapa do fluxo** com tabela-resumo operacional por endpoint.


## 2026-05-23 00:00:00 UTC
- atualização solicitada no documento canônico `docs/canonical/mois-worker-canon.v1.md` com inclusão de diagrama de arquitetura por unidade de módulo/pacote.
- adicionada a seção **12.5 Diagrama de arquitetura por módulo/pacote** com Mermaid destacando dependências entre coletores, worker, backend MOIS, OpenAI API e MySQL 5.7.
- reforçadas no próprio cânone as regras de integração: sem acesso direto ao banco fora do backend e integração OpenAI concentrada no worker.


## 2026-05-23 00:00:00 UTC
- ajuste no diagrama de sequência da seção 12.2 do cânone MOIS para posicionar o elemento MySQL como o mais à direita da arquitetura.
- aplicado destaque visual do MySQL em cor distinta usando `box rgb(255, 245, 210)` para separar a camada de persistência no Mermaid.

## 2026-05-23 14:11:34 UTC-3
- ajuste na tela de detalhe da biblioteca de páginas de vendas do MOIS para eliminar interpretação incorreta do bloco "Request enviado ao modelo".
- identificado que o campo exibido estava usando `analysisNotes`, que representa observações resumidas da análise, e não o payload completo enviado ao modelo.
- atualizado o frontend para rotular corretamente esse conteúdo como notas do worker e mostrar "Não disponível neste registro" quando o request literal não existe no contrato retornado pela API.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx
  - docs/registros/mois1.md

## 2026-05-23 14:15:16 UTC-3
- correção solicitada: o usuário precisa visualizar o request literal enviado ao modelo na tela, e não apenas notas resumidas.
- causa-raiz: o pipeline não persistia o payload de request da OpenAI em `mois_sales_library_page_analysis`, portanto o frontend não tinha como exibir esse dado.
- aplicado ajuste fim-a-fim: worker agora envia `requestPayloadJson`, backend persiste/retorna esse campo e frontend passa a renderizar o request real quando disponível.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - backend/AGENTS.md
  - frontend/AGENTS.md
  - backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java
  - mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/openai/OpenAiSalesPageAnalyzer.java
  - frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx

## 2026-05-23 14:20:26 UTC-3
- revisão solicitada dos documentos canônicos e modelo de dados após inclusão do request literal do modelo na biblioteca MOIS.
- causa-raiz documental: o código já persistia `request_payload_json`, mas os documentos canônicos ainda não refletiam completamente esse campo e mantinham referência desatualizada (`processed_at`).
- atualizados `docs/modelo-dados-experimento.md` e `docs/canonical/mois-worker-canon.v1.md` para incluir `request_payload_json` e alinhar o timestamp operacional para `analyzed_at`.
- documentos lidos para tratar a situação:
  - docs/modelo-dados-experimento.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md

## 2026-05-23 14:23:19 UTC-3
- ajuste solicitado no modelo de dados dentro do cânone MOIS para aderência ao schema real em produção.
- causa-raiz documental: seção 13.3 do cânone ainda listava campos legados/incorretos (`captured_at`, `attempt_count`, `html_content`, `artifact_payload`) divergentes das tabelas atuais.
- correção aplicada: atualização dos campos de destaque de `mois_sales_library_url_ingest`, `mois_sales_library_processing_job`, `mois_sales_library_page_snapshot` e `mois_sales_library_snapshot_artifact` para os nomes efetivos usados no backend.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-worker-canon.v1.md
  - docs/modelo-dados-experimento.md
## 2026-05-23 14:13:48 UTC-3
- solicitada melhoria na tela da Biblioteca Sales Pages do MOIS para exibir totalizações operacionais de produtos.
- foi aproveitado o contrato já existente do endpoint da biblioteca, calculando os indicadores no frontend sem necessidade de novo endpoint.
- adicionados cards com: total coletados, total de produtos Hotmart, total de produtos Clickbank e total com análise.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx
  - docs/registros/mois1.md

## 2026-05-23 14:26:00 UTC-3
- correção de erro de renderização Mermaid na seção 12.2 do cânone MOIS Worker no GitHub Preview.
- causa-raiz: uso de bloco `box` com `Note` dentro do `sequenceDiagram`, combinação que o parser Mermaid do GitHub rejeitou na linha do `Note over DB`.
- correção aplicada: substituição por bloco `rect` destacado e `Note over DB` diretamente, preservando a semântica visual do banco sem violar a gramática suportada.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md

## 2026-05-23 17:56:00 UTC
- correção adicional do diagrama Mermaid na seção 12.5 do cânone MOIS Worker após falha de parse no GitHub Preview.
- causa-raiz: label de aresta com `/v1/responses (batch)` sem aspas no `graph TD`, gerando token inválido no parser do GitHub.
- correção aplicada: encapsulada a label da aresta entre aspas (`|"/v1/responses (batch)"|`) para aderência à gramática Mermaid suportada.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md

## 2026-05-23 18:05:00 UTC
- inclusão de diagrama de sequência focado exclusivamente no coletor Hotmart logo após o diagrama geral do módulo MOIS Worker.
- causa-raiz: ausência de visão detalhada da etapa de ingestão Hotmart no trecho imediatamente subsequente ao fluxo ponta a ponta, dificultando leitura operacional solicitada.
- correção aplicada: adicionada nova subseção `12.2.1` com `sequenceDiagram` dedicado ao fluxo Hotmart (`POST /urls:ingest`, normalização/deduplicação e criação de job `PENDING`), e renumerada a seção de tabelas para `12.2.2`.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md


## 2026-05-23 — Diagrama Hotmart com site de origem e URL acessada
- atualizado o diagrama `12.2.1` em `docs/canonical/mois-worker-canon.v1.md` para incluir explicitamente o participante **Hotmart Site (hotmart.com)** no fluxo.
- detalhado no diagrama que o coletor acessa `salesPageUrl` com fallback para `detailsUrl`, deixando explícita a URL de origem consultada antes do `POST /urls:ingest`.
- objetivo: reduzir ambiguidade operacional sobre qual URL está sendo acessada no passo de coleta.

Arquivos alterados:
- docs/canonical/mois-worker-canon.v1.md
- docs/registros/mois1.md

## 2026-05-23 21:20:34 UTC-3
- solicitado criar diagramas no mesmo padrão da seção 12.5 para separar a arquitetura por coletor Hotmart e por coletor ClickBank.
- foram adicionados dois diagramas canônicos independentes na documentação do MOIS Worker, mantendo o backend como ponto único de integração e o MySQL como persistência exclusiva do backend.
- registro realizado para manter rastreabilidade documental do módulo MOIS conforme política de registros.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md

## 2026-05-24 00:00:00 UTC
- melhoria dos diagramas canônicos da biblioteca de páginas de vendas do MOIS para explicitar integração com OpenAI e o fluxo de obtenção/montagem de prompt.
- causa-raiz: os diagramas existentes mostravam chamada para OpenAI, mas não deixavam visível a etapa de construção do prompt (URL canônica + conteúdo extraído) e metadados de rastreabilidade (`promptVersion`/`parserVersion`).
- correção aplicada:
  - seção 12.1 atualizada para detalhar a montagem do prompt antes do envio ao `/v1/responses`;
  - seção 12.2 (sequência ponta a ponta) atualizada com participante `Prompt Builder (Worker)` e passos explícitos de montagem/retorno de prompt e validação do output;
  - seção 12.5 (arquitetura por módulo/pacote) atualizada para destacar o componente `openai.OpenAiSalesPageAnalyzer` entre worker e OpenAI;
  - regras de integração atualizadas com nota explícita sobre origem do prompt no worker.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/canonical/mois-worker-canon.v1.md
  - docs/registros/mois1.md

- 2026-05-24: Adicionada regra ArchUnit no módulo `mois-sales-library-worker` para exigir classe no pacote `bibliotecapaginavenda.worker.vN.service` com método `recordPromptBuilderOpenAiResult` recebendo request bruto, resposta bruta, jobId OpenAI e JSON validado; incluída implementação inicial `OpenAiPromptResultRecorder` no v1.

- 2026-05-24: Regra ArchUnit ampliada no `mois-sales-library-worker` para exigir também método de inserção backend (`insertOpenAiIntegrationRecord`) em `mois.bibliotecapaginavenda.worker.vN.service`, com parâmetros: request cru, resposta crua, jobId OpenAI, JSON validado e jobId Marketing Hub.

## 2026-05-25 — Diagramas canônicos dos ciclos de acesso Hotmart (Ciclo 1 e Ciclo 2)
- Atualizado `docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md` com seção nova de diagramas para os dois fluxos operacionais.
- Incluído diagrama do **Ciclo 1 (listagem)** com módulos/pacotes Java envolvidos, URL Hotmart de busca, endpoint backend de persistência e dados recebidos da Hotmart.
- Incluído diagrama do **Ciclo 2 (detalhes)** com módulos/pacotes Java envolvidos, endpoint backend de leitura de produtos, URL Hotmart de detalhes e fallback de `salesPageUrl` na persistência.
- Documentada uma visão consolidada de pacotes Java por módulo e lista objetiva de URLs/endpoints usados no fluxo.
- Documentos lidos para execução:
  - AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-05-25 — Correção de renderização Mermaid (MOIS Hotmart ciclos 1 e 2)
- Corrigidos os dois diagramas Mermaid em `docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md` para eliminar erro de parser no GitHub.
- Causa-raiz: rótulos com caracteres especiais não escapados no conteúdo dos nós (`{}` em URLs com placeholders e quebra de linha literal dentro do nó), que quebravam o parser Mermaid.
- Correção aplicada:
  - padronização dos rótulos com aspas (`["..."]`) para texto rico com `<br/>`;
  - remoção de placeholders com chaves nas labels dos nós (`{n}`, `{id}`, `{session}`), substituindo por texto equivalente sem chaves;
  - normalização de quebras de linha para `<br/>` no nó de persistência do ciclo 1.
- Documentos lidos para execução:
  - AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-05-25 — Paginação no ciclo 1 do coletor Hotmart (até 100 produtos)
- Ajustado `mois-hotmart-collector` para o ciclo 1 percorrer páginas da API Hotmart (`page` incremental) até esgotar resultados ou atingir 100 produtos.
- Causa-raiz: o coletor fazia apenas uma chamada fixa com `page=1`, limitando a coleta à primeira página.
- Correção aplicada:
  - limite máximo por execução ampliado para 100 produtos;
  - paginação com `rows` por página e incremento de `page` a cada requisição;
  - parada quando a página retorna vazio ou quando vem menos itens que o `rows` solicitado.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md

## 2026-05-25 — Ajuste de limite de paginação do ciclo 1 para 100 páginas
- Ajustado o loop de paginação do ciclo 1 no coletor Hotmart para respeitar limite explícito de até 100 páginas por execução.
- Causa-raiz: o ajuste anterior limitava por quantidade de produtos e não deixava explícito o teto de páginas solicitado.
- Correção aplicada:
  - adicionada constante `HOTMART_MAX_PAGES_PER_RUN = 100`;
  - loop agora executa enquanto `page <= HOTMART_MAX_PAGES_PER_RUN`;
  - log informativo quando o limite de páginas é atingido antes do alvo de produtos.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md

## 2026-05-25 — Agendamento do coletor Hotmart somente 10:00 e 17:00
- Ajustado o `HotmartCollectorScheduler` para executar apenas duas vezes por dia.
- Causa-raiz: o scheduler estava horário (de hora em hora), excedendo a janela operacional desejada.
- Correção aplicada:
  - `@Scheduled(cron = "0 0 10 * * *")` para o ciclo 1 (listagem);
  - `@Scheduled(cron = "0 0 17 * * *")` para o ciclo 2 (detalhes);
  - removida a lógica de alternância por hora ímpar/par.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md

## 2026-05-26 — Ajuste de horário do ciclo 1 para 18:15 + logs de paginação detalhados
- Ajustado o agendamento do ciclo 1 do coletor Hotmart para executar diariamente às 18:15.
- Causa-raiz: necessidade operacional de observar a execução do ciclo 1 em horário específico para análise de paginação.
- Correção aplicada:
  - `@Scheduled(cron = "0 15 18 * * *")` no método `collectFirstCycleAtFifteen`;
  - atualização da mensagem do scheduler para `hora=18:15`;
  - inclusão de logs expressivos de paginação no ciclo 1: início da coleta com limites, log antes da requisição de cada página, log de fechamento por página (itens retornados/adicionados) e log de encerramento consolidado da execução.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-05-27 — Agendamento do coletor Hotmart para 00:05 em 27 de maio
- Ajustado o scheduler do ciclo 1 do coletor Hotmart para executar às 00:05 no dia 27 de maio.
- Causa-raiz: necessidade operacional de janela pontual para execução do coletor nesse marco específico.
- Correção aplicada:
  - `@Scheduled(cron = "0 5 0 27 5 *")` no método do ciclo 1;
  - renomeado o método para refletir o novo agendamento;
  - atualizado log operacional para explicitar `hora=00:05` e `dia=27/05`.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-05-31 — Agendamento do ciclo 1 Hotmart para 18:15 em 31 de maio
- Ajustado o scheduler do ciclo 1 do coletor Hotmart para executar às 18:15 no dia 31 de maio, no horário de São Paulo.
- Causa-raiz: necessidade operacional de executar a listagem Hotmart hoje em janela específica solicitada pelo operador.
- Correção aplicada:
  - `@Scheduled(cron = "0 15 18 31 5 *", zone = "America/Sao_Paulo")` no método do ciclo 1;
  - renomeado o método para refletir o novo agendamento pontual;
  - atualizado o log operacional para explicitar `hora=18:15`, `dia=31/05` e `timezone=America/Sao_Paulo`.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-05-31 — Hotmart ciclo 1: alerta acionável para token JWT expirado

- Corrigida a causa-raiz do ciclo 1 da Hotmart persistir `COLLECTION_EXECUTED` mesmo quando a API retornava `401 invalid_token` por JWT expirado.
- O coletor agora classifica respostas `401/403`, `invalid_token`, `Expired JWT` e `BadJWTException` como falha acionável, persiste o job como `COLLECTION_ERROR` e grava mensagem explícita orientando o usuário a atualizar o token na tela Hotmart.
- A tela `/hotmart` passou a destacar a ação necessária com alerta vermelho quando a última execução indicar erro de token Hotmart.

Arquivos alterados:
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorService.java`
- `mois-hotmart-collector/src/test/java/com/marketinghub/moishotmart/service/HotmartCollectorServiceTest.java`
- `frontend/src/pages/hotmart/HotmartPage.tsx`

## 2026-05-31 20:12:49 UTC-3
- solicitado novo agendamento operacional do ciclo 1 do coletor Hotmart para 20:40 de 31 de maio, no horário de São Paulo.
- a causa-raiz operacional era que o agendamento especial vigente estava fixado para 18:15 do mesmo dia, horário já inadequado para a nova janela solicitada.
- atualizado o `HotmartCollectorScheduler` para disparar o ciclo 1 com cron `0 40 20 31 5 *`, mantendo `zone = "America/Sao_Paulo"` e log operacional alinhado para `hora=20:40`.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md

## 2026-06-01 — Hotmart: persistência de logs, healthcheck e jobs vazios diagnósticos
- Implementada correção operacional para evitar perda de logs do `mois-hotmart-collector` após reinício/recriação do container.
- Causa-raiz tratada: o arquivo exposto por `/ops-monitor/mois-hotmart-log` ficava dentro de `/app/logs` sem volume persistente no compose de deploy, e jobs sem produtos podiam ser persistidos como `COLLECTION_EXECUTED` sem mensagem diagnóstica suficiente.
- Correções aplicadas:
  - adicionado volume persistente para `/app/logs` no compose local e no compose de deploy;
  - adicionado `healthcheck` HTTP para `/api/v1/mois-hotmart/health` no compose local e de deploy;
  - adicionado `curl` à imagem runtime para suportar o healthcheck;
  - ajustado o script de deploy para criar também o diretório remoto de logs;
  - ajustado o coletor para persistir jobs sem produtos como `COLLECTION_SKIPPED` com mensagem operacional explícita;
  - adicionados logs de início/fim, token, busca de base do ciclo 2, enriquecimento, payload de persistência e resposta do backend;
  - removida do cânone a regra desatualizada de alternância por hora ímpar/par.
- Documentos lidos para execução:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-06-01 00:07:55 UTC-3
- ajuste operacional solicitado para agendar a próxima execução do ciclo 1 Hotmart para 01/06 às 04:00 no timezone `America/Sao_Paulo`.
- o agendamento do ciclo 1 é definido no código do `mois-hotmart-collector` por cron literal em `@Scheduled`, então a solução foi atualizar a expressão pontual do ciclo 1 em vez de criar configuração paralela.
- mantido o ciclo 2 sem alteração.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md

## 2026-06-01 — Hotmart ciclo 1 configurado para 20 páginas
- Ajustado o `mois-hotmart-collector` para o ciclo 1 buscar até 20 páginas completas da API de busca Hotmart.
- Causa-raiz: a execução anterior parou na página 2 porque o alvo operacional estava configurado para 25 produtos; com 20 itens por página, isso encerrava a coleta após 20 + 5 itens.
- Alterações realizadas:
  - limite de páginas do ciclo 1 definido para 20;
  - alvo máximo por execução derivado de 20 páginas x 20 itens = 400 produtos;
  - valor padrão de `collector.scheduler.max-products` atualizado para 400;
  - cânone Hotmart atualizado para documentar o alvo operacional padrão de 20 páginas.

## 2026-06-01 — Hotmart ciclo 1 reagendado para 14:00
- ajuste operacional solicitado para agendar a próxima execução do ciclo 1 Hotmart para 01/06 às 14:00 no timezone `America/Sao_Paulo`.
- atualizado o `HotmartCollectorScheduler` para disparar o ciclo 1 com cron literal `0 0 14 1 6 *`, mantendo a regra de `@Scheduled` com string direta e sem alterar o ciclo 2.
- mantido o foco operacional do ciclo 1 em listagem/search Hotmart para alimentar a base MOIS com snapshots comerciais úteis.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - docs/registros/mois1.md

## 2026-06-01 — Hotmart ciclo 1 reagendado para 16:15
- ajuste operacional solicitado para alterar a próxima execução do ciclo 1 Hotmart para **01/06/2026 às 16:15** no timezone `America/Sao_Paulo`.
- atualizado o `HotmartCollectorScheduler` para disparar o ciclo 1 com cron literal `0 15 16 1 6 *`, com guarda operacional para executar somente no ano de 2026.
- atualizado o cânone Hotmart para registrar o horário operacional do ciclo 1 e manter rastreabilidade da decisão.
- mantido o ciclo 2 sem alteração.

## 2026-06-01 - Aumento do alvo operacional do ciclo 1 Hotmart para 500 produtos

- Ajustado o limite do ciclo 1 do `mois-hotmart-collector` para percorrer até 25 páginas de 20 itens, totalizando alvo operacional padrão de 500 produtos por execução.
- Atualizado o padrão `collector.scheduler.max-products` para 500, mantendo possibilidade de override por `COLLECTOR_SCHEDULER_MAX_PRODUCTS`.
- Sincronizado o documento canônico do fluxo Hotmart e o teste unitário que valida o limite operacional do ciclo 1.

Arquivos alterados:
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorService.java`
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorScheduler.java`
- `mois-hotmart-collector/src/main/resources/application.properties`
- `mois-hotmart-collector/src/test/java/com/marketinghub/moishotmart/service/HotmartCollectorServiceTest.java`
- `docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md`


## 2026-06-01 — Hotmart ciclo 1 reagendado para 18:15

- ajuste operacional solicitado para alterar a execução do ciclo 1 Hotmart de **01/06/2026 às 16:15** para **01/06/2026 às 18:15** no timezone `America/Sao_Paulo`.
- atualizado o `HotmartCollectorScheduler` para disparar o ciclo 1 com cron literal `0 15 18 1 6 *`, mantendo guarda operacional para executar somente no ano de 2026.
- atualizado o cânone Hotmart para refletir o novo horário vigente do ciclo 1.
- mantido o alvo operacional de 500 produtos e o ciclo 2 sem alteração.

## 2026-06-01 — Hotmart ciclo 1 corrigido para 400 produtos sem duplicação

- Corrigida a causa-raiz da execução do ciclo 1 coletar apenas 25 itens: os `docker-compose` local e de deploy ainda sobrescreviam o padrão canônico com `COLLECTOR_SCHEDULER_MAX_PRODUCTS=25`, apesar do código/documentação operacional indicarem alvo maior.
- Alinhado o alvo operacional vigente para **400 produtos**: 20 páginas completas x 20 itens por página.
- Ajustado o ciclo 1 para sempre requisitar páginas completas de 20 itens, evitando sobreposição de ranking quando a última página era solicitada com menos linhas.
- Adicionada deduplicação em memória por `ucode` ou por combinação normalizada de título/produtor antes de persistir o produto do ciclo 1.
- Atualizados cânone, README/configurações e testes unitários do coletor Hotmart para travar o comportamento esperado.

Arquivos alterados:
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorService.java`
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorScheduler.java`
- `mois-hotmart-collector/src/main/resources/application.properties`
- `mois-hotmart-collector/docker-compose.yml`
- `mois-hotmart-collector/docker-compose.deploy.yml`
- `mois-hotmart-collector/README.md`
- `mois-hotmart-collector/src/test/java/com/marketinghub/moishotmart/service/HotmartCollectorServiceTest.java`
- `docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md`

## 2026-06-01 — Hotmart ciclo 1 reagendado para 21:00

- ajuste operacional solicitado para alterar a execução do ciclo 1 Hotmart de **01/06/2026 às 18:15** para **01/06/2026 às 21:00** no timezone `America/Sao_Paulo`.
- atualizado o `HotmartCollectorScheduler` para disparar o ciclo 1 com cron literal `0 0 21 1 6 *`, mantendo guarda operacional para executar somente no ano de 2026.
- atualizado o cânone Hotmart para refletir o novo horário vigente do ciclo 1.
- mantido o alvo operacional de 400 produtos e o ciclo 2 sem alteração.

Arquivos alterados:
- `mois-hotmart-collector/src/main/java/com/marketinghub/moishotmart/service/HotmartCollectorScheduler.java`
- `mois-hotmart-collector/src/test/java/com/marketinghub/moishotmart/service/HotmartCollectorSchedulerTest.java`
- `docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md`

## 2026-06-03 — Atalho Pipeline na biblioteca de páginas de vendas

- Adicionado botão **Pipeline** no cabeçalho da Biblioteca de Páginas de Vendas do MOIS.
- Criada rota inicial `/mois/sales-pages-library/pipeline` com tela placeholder para direcionar o usuário à visão de pipeline que será construída na próxima etapa.
- Mantido o fluxo simples e sem novo contrato de backend, pois a alteração atual é apenas navegação/placeholder sem consumo de dados.

Arquivos alterados:
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `frontend/src/App.tsx`

## 2026-06-03 — Pipeline de captura de HTML bruto a partir de `mois_collected_reference`
- criado backend como dono da leitura/persistência da primeira etapa do novo pipeline, com endpoints `POST /api/mois/sales-library/collected-reference-html:claim`, `POST /api/mois/sales-library/collected-reference-html/{captureId}:complete` e `POST /api/mois/sales-library/collected-reference-html/{captureId}:fail`.
- adicionada tabela `mois_collected_reference_html_capture` para reservar URLs de `mois_collected_reference`, guardar URL original/final, status, HTTP status, content type, hash, tamanho e HTML bruto completo.
- ajustado o `mois-sales-library-worker` para executar a captura na internet: reserva uma referência no backend, faz GET da URL priorizada (`sales_page_url`, fallback `product_url`, fallback `url`) e devolve o HTML bruto ao backend para a próxima etapa.

## 2026-06-03 07:38:12 UTC-3
- adicionada a primeira etapa visual do pipeline da Biblioteca de Páginas de Vendas do MOIS para orientar a obtenção dos HTML.
- a solução foi mantida simples e estática porque a tela de pipeline ainda não possui contrato específico de métricas agregadas para este card.
- o card agora apresenta entrada, saída esperada, critério de qualidade e informações operacionais que devem ser acompanhadas na captura de HTML bruto.
- documentos lidos para tratar a situação:
  - docs/novos-modulos/mois-biblioteca-pagina-venda/especificacao-biblioteca-sales-pages.md
  - docs/novos-modulos/mois-biblioteca-pagina-venda/guia-secoes-biblioteca-paginas-vendas.md
- arquivos alterados:
  - frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx
  - docs/novos-modulos/mois-biblioteca-pagina-venda/guia-secoes-biblioteca-paginas-vendas.md
  - docs/registros/mois1.md

## 2026-06-03 — Primeira etapa do pipeline de páginas de venda: obtenção de HTML bruto
- implementada a etapa `htmlcapture` no `mois-sales-library-worker` seguindo o padrão de núcleo genérico `pipeline` + etapa concreta `pipeline.htmlcapture`, com `StageProcessor`, `StageResult`, `StageArtifact`, `PipelineWorker` e proteção ArchUnit contra acoplamento entre etapas.
- criados endpoints backend `POST /api/mois/sales-library/html-captures:claim`, `POST /api/mois/sales-library/html-captures/{snapshotId}:complete` e `POST /api/mois/sales-library/html-captures/{snapshotId}:fail` para reservar URLs normalizadas da biblioteca e persistir HTML bruto versionado em `mois_sales_library_page_snapshot` + `mois_sales_library_snapshot_artifact`.
- a etapa registra hash SHA-256, tamanho, status HTTP, content type, URL final e data da captura, preservando falhas de acesso/bloqueio/timeout como snapshots `FAILED` para auditoria e reprocessamento.

## 2026-06-03 — Pipeline de páginas de vendas com execução manual da etapa 1
- Corrigida a causa-raiz da tela `/mois/sales-pages-library/pipeline` aparentar não executar: ela ainda estava como placeholder visual, embora o backend já possuísse contrato operacional de captura de snapshots.
- A tela agora consome `POST /api/mois/sales-library/snapshots:capture`, permite executar a etapa **Obtenção dos HTML**, exibe contadores da execução e lista o resultado por URL com status, HTTP, tamanho do HTML e detalhe/hash.
- Atualizado o contrato Swagger da Biblioteca de Páginas de Vendas para documentar o endpoint de captura manual utilizado pelo frontend.

Arquivos alterados:
- `frontend/src/api/mois/types.ts`
- `frontend/src/api/mois/useMoisSalesLibrary.ts`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-03 — Cooldown de falhas na obtenção de HTML bruto
- Corrigida a causa-raiz do reprocessamento improdutivo de URLs de sales page que já falharam por destino indisponível, DNS, 404, redirecionamento sem HTML ou timeout.
- A seleção automática da etapa de captura agora ignora snapshots `FAILED` nas últimas 24 horas e URLs com 3 ou mais falhas acumuladas, salvo quando o acionamento for explícito com `force=true`.
- As falhas persistidas passam a registrar categoria operacional no `error_message` e, quando disponível, `http_status`/`content_type`, facilitando diagnóstico na tela e auditoria pelo banco.
- Atualizados testes unitários, Swagger e cânone MOIS para documentar o cooldown, o limite de falhas e as categorias de falha.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotServiceTest.java`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/registros/mois1.md`

## 2026-06-03 — Fallback pela raiz do redirecionamento na etapa 1 da Biblioteca Sales Pages
- Adicionada inteligência operacional na etapa **Obtenção dos HTML** para registrar, em `mois_sales_library_page_snapshot`, a URL final após redirecionamento (`redirect_destination_url`) e a raiz do destino (`redirect_root_url`).
- Quando o destino redirecionado não entrega HTML capturável, o pipeline tenta a raiz do domínio antes de registrar falha terminal, permitindo aproveitar casos como `/secarapido` indisponível mas domínio raiz ativo.
- Atualizados DTOs, Swagger, testes e cânone MOIS para preservar a rastreabilidade do redirecionamento e reduzir descarte indevido de páginas de vendas potencialmente úteis.

Arquivos alterados:
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-06-03-mois-sales-library-snapshot-redirect-urls.yaml`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotService.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/pipeline/htmlcapture/HtmlCaptureProcessor.java`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/canonical/mois-worker-canon.v1.md`

## 2026-06-04 — Plano de simplificação do pipeline de páginas de vendas em duas tabelas
- Documentado o plano faseado para simplificar o pipeline operacional da Biblioteca de Páginas de Vendas do MOIS em duas tabelas principais: `mois_sales_page` para estado atual consolidado e `mois_sales_page_job_execution` para histórico/auditoria de execuções.
- O plano preserva `mois_collected_reference` como origem bruta dos coletores e propõe migração incremental com criação do novo schema, backfill, escrita dupla temporária, migração do frontend, troca da escrita principal e congelamento do legado.
- Registrada a intenção arquitetural de reduzir ambiguidade entre referências coletadas, URLs consolidadas, snapshots, análises e jobs, mantendo diagnóstico de causa-raiz por execução.

Arquivos alterados:
- `docs/novos-modulos/MOIS/mois_sales_page_pipeline_simplificacao_duas_tabelas.md`
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/registros/mois1.md`

## 2026-06-04 00:08:14 UTC-3
- execução da Fase 1 do plano de simplificação do pipeline da Biblioteca de Páginas de Vendas do MOIS em duas tabelas operacionais.
- foi mantida a compatibilidade com o fluxo atual, sem remoção de tabelas legadas, preparando apenas o modelo estrutural novo para backfill e escrita dupla nas próximas fases.
- criados changelogs Liquibase incrementais para `mois_sales_page` e `mois_sales_page_job_execution`, com índices operacionais de leitura e teste de contrato de schema.
- atualizado o cânone MOIS para registrar responsabilidades, índices obrigatórios e regra de transição da Fase 1.
- documentos lidos para tratar a situação:
  - docs/novos-modulos/MOIS/mois_sales_page_pipeline_simplificacao_duas_tabelas.md
  - docs/canonical/mois-worker-canon.v1.md
  - backend/AGENTS.md
  - AGENTS.md

## 2026-06-04 — Fase 2 do pipeline de páginas de vendas em duas tabelas
- implementada rotina idempotente de backfill inicial para popular `mois_sales_page` a partir de `mois_sales_library_url_ingest` consolidando fonte, URL, status atual, última captura, última análise, score, erro e vínculo possível com `mois_collected_reference`.
- implementada migração de histórico mínimo para `mois_sales_page_job_execution`, incluindo o último job de processamento, a última análise, o último snapshot e a última captura bruta ligada à referência coletada quando houver vínculo.
- adicionados contadores operacionais em log para páginas legadas, páginas novas antes/depois, execuções antes/depois, linhas afetadas/inseridas por etapa e ponteiros `last_job_execution_id` atualizados.
- adicionada proteção para ignorar a rotina quando o schema de backfill ainda não existir, mantendo testes e ambientes incompletos sem falha de inicialização.
- tentativa de execução direta contra o MySQL pela rede do Codex falhou por bloqueio de conexão direta à porta 3306; a rotina fica habilitada para execução automática no backend após deploy/inicialização.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageBackfillRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageBackfillService.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageBackfillGateway.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageBackfillServiceTest.java`
- `backend/ads-service/src/main/resources/application.properties`
- `docs/registros/mois1.md`

## 2026-06-04 — Fase 3 do pipeline de páginas de vendas em duas tabelas
- implementada escrita dupla temporária para manter o modelo legado da Biblioteca de Páginas de Vendas e o modelo consolidado `mois_sales_page`/`mois_sales_page_job_execution` sincronizados durante a transição.
- a ingestão de URLs agora espelha inserções e atualizações em `mois_sales_page`, criando execução consolidada do job pendente quando uma página nova gera processamento.
- alterações de job/análise (`FETCHING`, `DONE`, `FAILED`, reanálise e atualização manual de status) passam a registrar execuções no modelo novo e recalcular o estado atual consolidado da página.
- capturas de HTML/snapshots e capturas brutas vinculadas a referências coletadas passam a registrar histórico em `mois_sales_page_job_execution` e atualizar o estado atual de captura em `mois_sales_page`.
- adicionados logs de divergência para sinalizar quando estágio/status calculados a partir do legado não batem com o estado gravado no modelo novo.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageDualWriteGateway.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageDualWriteRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotServiceTest.java`
- `backend/ads-service/src/test/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageDualWriteRepositoryTest.java`
- `docs/registros/mois1.md`

## 2026-06-04 — Correção dos testes da escrita dupla da Biblioteca de Páginas de Vendas
- corrigidos os testes unitários de `MoisSalesLibraryServiceTest` para injetar o gateway de escrita dupla usado pela Fase 3 do pipeline, eliminando `NullPointerException` nos fluxos que sincronizam capturas HTML coletadas.
- ajustados os stubs Mockito de `JdbcTemplate.queryForObject` para refletirem os dois parâmetros reais enviados pela busca de URL canônica (`workspace_id` e `url_canonical`), eliminando falsos erros de `PotentialStubbingProblem` em modo strict.
- verificado o teste unitário específico da Biblioteca de Páginas de Vendas e a suíte unitária completa do `ads-service` após a correção.

Arquivos alterados:
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `docs/registros/mois1.md`

## 2026-06-04 — Fase 4 do pipeline de páginas de vendas em duas tabelas
- migrada a leitura operacional da Biblioteca de Páginas de Vendas para o modelo consolidado `mois_sales_page`, incluindo listagem, detalhe e resumo global de páginas por workspace.
- criado endpoint de resumo global baseado em `mois_sales_page` para total de páginas, pendentes, em captura, capturadas, analisadas, falhas, bloqueadas por cooldown e totais por fonte Hotmart/ClickBank, eliminando contadores derivados apenas da página carregada no frontend.
- criado endpoint de histórico consolidado baseado em `mois_sales_page_job_execution` para explicar as execuções de captura/análise por página a partir do modelo novo.
- atualizadas as telas `/mois/sales-pages-library`, `/mois/sales-pages-library/pipeline` e `/mois/sales-pages-library/:pageId` para usar os dados consolidados, exibir contadores globais reais e montar o histórico a partir das execuções novas.
- preservadas ações transicionais de reanálise e atualização manual com resolução do identificador legado enquanto a escrita dupla ainda estiver ativa.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryControllerTest.java`
- `frontend/src/api/mois/types.ts`
- `frontend/src/api/mois/useMoisSalesLibrary.ts`
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/registros/mois1.md`

## 2026-06-04 — Fase 5 do pipeline de páginas de vendas em duas tabelas
- alterada a execução operacional de análise da Biblioteca de Páginas de Vendas para usar `mois_sales_page_job_execution` como fila principal de claim/conclusão/falha, retornando o `executionId` como `jobId` para o worker.
- conclusões e falhas de análise agora atualizam primeiro `mois_sales_page_job_execution` e consolidam o estado atual em `mois_sales_page`, preservando espelhos legados apenas para auditoria transicional quando houver vínculo com `mois_sales_library_url_ingest`/`mois_sales_library_processing_job`.
- reanálise e atualização manual de status passaram a criar execuções no histórico consolidado antes do espelho legado, reforçando `mois_sales_page` e `mois_sales_page_job_execution` como fonte operacional principal.
- adicionados logs de transição com `pageId` e `executionId` para rastrear claim, conclusão, falha, reanálise e atualização manual de status no modelo novo.
- atualizado o cânone MOIS e o Swagger da Biblioteca para explicitar a regra da Fase 5 e o significado operacional do `jobId` nos endpoints de worker.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-04 — MOIS Biblioteca de Páginas de Vendas — Fase 5 do modelo em duas tabelas

- executada a Fase 5 do plano `docs/novos-modulos/MOIS/mois_sales_page_pipeline_simplificacao_duas_tabelas.md`.
- ingestão explícita e ingestão Hotmart agora escrevem primariamente em `mois_sales_page` e criam execução pendente `PAGE_ANALYSIS` em `mois_sales_page_job_execution` para páginas novas.
- captura HTML por worker/backend agora cria e conclui execuções `HTML_CAPTURE` diretamente em `mois_sales_page_job_execution`, atualizando `mois_sales_page` em claim, sucesso, duplicidade e falha.
- tabelas legadas permanecem apenas como auditoria transitória quando houver espelhamento compatível; leitura operacional permanece concentrada nas duas tabelas novas.
- Swagger `docs/swagger/mois-sales-library-swagger.yaml` atualizado para documentar ingestão, análise e captura como contratos do modelo operacional novo.

## 2026-06-04 — Fase 5 MOIS sales page pipeline: escrita principal no modelo novo

- removida a dependência operacional da escrita dupla/espelho legado na Biblioteca de Páginas de Vendas MOIS.
- endpoints de análise (`/jobs:claim`, `/jobs/{jobId}:complete`, `/jobs/{jobId}:fail`) passam a aceitar somente execuções vigentes em `mois_sales_page_job_execution` como identificador operacional.
- endpoints de captura de referência coletada (`/collected-reference-html:*`) agora leem `mois_collected_reference` apenas como origem bruta, criam/atualizam `mois_sales_page` e gravam HTML/erro em `mois_sales_page_job_execution`.
- listagem e busca de jobs passam a consultar o histórico consolidado `mois_sales_page_job_execution`, mantendo o campo compatível `urlIngestId` como identificador da página operacional.
- removidas as classes de escrita dupla MOIS para evitar que o legado volte a ser tratado como caminho principal.

## 2026-06-04 — MOIS Biblioteca de Páginas de Vendas — Fase 5 leitura de entradas operacional

- finalizada a troca da listagem `/api/mois/sales-library/entries` para ler `mois_sales_page`, removendo a última leitura operacional do service da Biblioteca contra `mois_sales_library_url_ingest`.
- atualizado o Swagger do módulo para documentar `/entries`, `/jobs` e `/jobs/{jobId}` como contratos apoiados em `mois_sales_page`/`mois_sales_page_job_execution` na Fase 5.
- ajustado o cânone MOIS para explicitar que a UI principal deve consultar apenas as duas tabelas operacionais novas, deixando tabelas antigas somente para auditoria histórica explícita.

## 2026-06-04 — Fase 6 da simplificação da Biblioteca de Páginas de Vendas

- executada a Fase 6 do plano de simplificação em duas tabelas da Biblioteca de Páginas de Vendas do MOIS.
- marcado no cânone que `mois_sales_page` é a fonte de verdade do estado atual e `mois_sales_page_job_execution` é a fonte de verdade do histórico/auditoria.
- congeladas as tabelas legadas em somente leitura para backfill idempotente, auditoria e diagnóstico histórico, com janela mínima de 180 dias até 2026-12-01.
- adicionado teste automatizado para impedir escrita/DDL acidental nas tabelas legadas congeladas no código Java principal do backend.
- atualizado o Swagger da biblioteca para declarar a Fase 6 como contrato operacional vigente.

Arquivos alterados:
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/novos-modulos/MOIS/mois_sales_page_pipeline_simplificacao_duas_tabelas.md`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/schema/MoisSalesLibraryLegacyFreezeTest.java`

## 2026-06-04 — Exibição de URLs únicas coletadas na tela do pipeline MOIS

- criado endpoint de resumo de URLs únicas vindas da origem bruta de referências coletadas, usando a prioridade `sales_page_url`, `product_url` e `url`, sem expor contagem de linhas brutas repetidas.
- adicionada cobertura operacional do resumo: total único coletado, URLs já consolidadas na biblioteca, URLs ainda faltantes, páginas explícitas e desdobramentos por origem/tipo de URL.
- atualizada a tela `/mois/sales-pages-library/pipeline` para exibir esses números antes dos cards operacionais de captura/análise, mantendo o foco nos indicadores úteis para decisão.
- atualizado o Swagger da Biblioteca MOIS e adicionado teste unitário para proteger a contagem por URLs únicas canônicas.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `frontend/src/api/mois/types.ts`
- `frontend/src/api/mois/useMoisSalesLibrary.ts`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-05 — Diagnóstico do claim automático de referências brutas MOIS

- investigada a estagnação do indicador `Faltam consolidar` em 276 URLs no pipeline da Biblioteca de Páginas de Vendas.
- causa-raiz identificada: o claim automático de `collected-reference-html` filtrava apenas por `collected_reference_id` com execução `FETCHING`/`CAPTURED`, permitindo gastar ciclos em referências brutas duplicadas cuja URL canônica já existia em `mois_sales_page`.
- ajustado o backend para escanear candidatos da origem bruta, canonicalizar a URL efetiva e pular URLs já consolidadas antes de reservar a próxima referência faltante.
- adicionados logs operacionais no recebimento do claim, seleção de candidato, ausência de candidato e reserva final para confirmar se o worker está chamando o backend e quantas referências foram puladas por já estarem consolidadas.
- atualizado o cânone MOIS para explicitar que a seleção automática deve reduzir faltantes por URL única canonicalizada, e não por linha/referência histórica.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/registros/mois1.md`

## 2026-06-05 — Correção do claim de HTML da biblioteca de páginas de venda MOIS

- diagnosticado via MCP que a tela de pipeline mostrava 276 URLs Hotmart faltantes porque a busca de candidatos do backend varria as primeiras 2000 linhas brutas por data, mas esse recorte continha apenas 20 URLs únicas já consolidadas, deixando as URLs faltantes fora da janela de processamento.
- ajustado o claim `POST /api/mois/sales-library/collected-reference-html:claim` para deduplicar a origem bruta por URL efetiva antes do limite de varredura, usando `GROUP BY effective_url` e campos normalizados com `TRIM`, permitindo que as 276 URLs faltantes entrem na fila operacional.
- adicionado teste unitário garantindo que o SQL do claim usa URLs efetivas únicas e não apenas contagem de linhas brutas repetidas.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`

## 2026-06-06 — Logfile público do MOIS Sales Library Worker para MCP

- habilitada geração de arquivo de log Spring Boot no `mois-sales-library-worker`, com Actuator expondo `logfile` para auditoria operacional remota.
- publicado o worker na porta padrão `8097` no host usado pelo workflow de deploy (`191.252.120.96`), preservando configuração por variáveis de ambiente.
- atualizado o MCP Server para consultar o log do módulo `mois` no endpoint público `http://191.252.120.96:8097/actuator/logfile`, permitindo investigar ciclos do worker via tool `java_module_logs`.

Arquivos alterados:
- `mois-sales-library-worker/pom.xml`
- `mois-sales-library-worker/src/main/resources/application.yml`
- `mois-sales-library-worker/docker-compose.yml`
- `mois-sales-library-worker/docker-compose.deploy.yml`
- `mcp-server/src/main/resources/application.yml`
- `mcp-server/README.md`
- `mcp-server/AGENTS.md`
- `docs/registros/mois1.md`

## 2026-06-06 — Alias de logs MCP para MOIS Sales Library Worker

- adicionado o módulo `mois-sales-library-worker` ao tool MCP `java_module_logs`, apontando para o logfile público `http://191.252.120.96:8097/actuator/logfile`.
- preservado o módulo `mois` existente e criada configuração dedicada `MCP_LOG_MOIS_SALES_LIBRARY_WORKER_PATH` para permitir ajuste independente no ambiente.
- atualizadas a documentação do MCP e a referência operacional de módulos aceitos para investigação de logs.

Arquivos alterados:
- `mcp-server/src/main/resources/application.yml`
- `mcp-server/src/main/java/com/marketinghub/mcpserver/config/McpProperties.java`
- `mcp-server/src/main/java/com/marketinghub/mcpserver/service/ModuleLogService.java`
- `mcp-server/src/main/java/com/marketinghub/mcpserver/controller/McpController.java`
- `mcp-server/src/test/java/com/marketinghub/mcpserver/controller/McpControllerTest.java`
- `mcp-server/README.md`
- `mcp-server/AGENTS.md`
- `AGENTS.md`

## 2026-06-06 — Correção de ambiguidade SQL no claim de referências coletadas MOIS

- corrigida a causa-raiz do erro `Column 'title' in field list is ambiguous` no claim `POST /api/mois/sales-library/collected-reference-html:claim`.
- o `ON DUPLICATE KEY UPDATE` do upsert em `mois_sales_page` agora referencia explicitamente os campos atuais da tabela alvo (`mois_sales_page.title`, `mois_sales_page.collected_reference_id`, `mois_sales_page.source_job_id`, `mois_sales_page.source_reference_id`) ao combinar dados já existentes com `VALUES(...)` vindos da referência coletada.
- adicionada cobertura unitária para impedir regressão para referência não qualificada de `title` no SQL de reserva de captura.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `docs/registros/mois1.md`

## 2026-06-06 — Simplificação dos totais do pipeline de páginas de vendas MOIS

- simplificada a tela `/mois/sales-pages-library/pipeline` para reduzir confusão visual e exibir apenas três indicadores principais: total bruto, total na tabela de sales e total com página obtida (HTML/imagem).
- removidos da visão principal os detalhamentos intermediários de origem, tipo, pendências, falhas, captura, análise e cooldown, preservando o foco operacional nos números necessários para decisão.

Arquivos alterados:
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/registros/mois1.md`

## 2026-06-07 — Critério canônico de captura correta por html_bytes

- definido `html_bytes > 0` como critério operacional para considerar captura de página correta na Biblioteca de Páginas de Vendas MOIS.
- ajustada a etapa 1 para selecionar páginas sem HTML útil (`COALESCE(html_bytes, 0) = 0`) e evitar recapturar páginas já úteis mesmo em execução forçada.
- atualizado o resumo do pipeline para exibir páginas com HTML útil em vez de somar status operacionais.

Arquivos alterados:
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibrarySnapshotServiceTest.java`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`

## 2026-06-07 — Card da etapa 2 no pipeline de páginas de vendas
- Verificado que a etapa 2 de análise comercial já está implementada no backend da Biblioteca de Páginas de Vendas do MOIS via endpoints de claim, complete e fail de jobs de análise.
- Adicionado card da **Etapa 2 — Análise comercial da página** na tela `/mois/sales-pages-library/pipeline`, usando os contadores consolidados já expostos pelo backend para mostrar páginas capturadas, analisadas e backlog estimado.

Arquivos alterados:
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/registros/mois1.md`

## 2026-06-08 — Listagem das 20 análises mais recentes na biblioteca MOIS

- ajustada a tela `/mois/sales-pages-library` para exibir somente 20 páginas por requisição, com coluna explícita de data da análise.
- ajustada a ordenação do endpoint de páginas operacionais para priorizar `last_analyzed_at` em ordem decrescente, preservando `updated_at` e `id` como desempate.
- adicionada cobertura unitária e documentação Swagger para proteger a ordenação por data de análise mais recente.

Arquivos alterados:
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-08 — Etapa 2 de análise baseada no HTML capturado

- alterada a etapa 2 da Biblioteca de Páginas de Vendas para criar fila real de análise a partir de páginas com HTML útil já capturado (`html_bytes > 0`) e sem análise ativa/concluída.
- o claim `POST /api/mois/sales-library/jobs:claim` passa a devolver o HTML bruto capturado mais recente ao worker, evitando depender novamente de acesso ao site externo durante a análise.
- o worker passa a extrair texto do HTML capturado na etapa 1, usando acesso ao vivo da URL apenas como fallback de compatibilidade para payload antigo sem `rawHtml`.
- o resumo da tela do pipeline passa a exibir pendentes, em execução e falhas da etapa 2, além do backlog estimado.
- adicionados logs de request/resposta brutos da OpenAI e payload enviado ao backend com `jobId` para rastreabilidade operacional.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryControllerTest.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/client/BackendClient.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/model/WorkerDtos.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/openai/OpenAiSalesPageAnalyzer.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/PipelineRunner.java`
- `frontend/src/api/mois/types.ts`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-09 — Pipeline oficial da Biblioteca de Páginas de Vendas no CRUD de pipelines

- cadastrado no backend o contrato oficial `mois-sales-page-library-pipeline` para o fluxo MOIS de páginas de vendas, com as etapas canônicas de obtenção de HTML e análise comercial da página.
- adicionada na tela `/pipelines` a ativação de pipelines oficiais ausentes, permitindo gravar o pipeline canônico no banco pelo endpoint já existente de sincronização oficial.
- adicionados testes para proteger os metadados, a versão canônica e a sincronização do pipeline oficial da Biblioteca de Páginas de Vendas.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/pipeline/definition/PipelineDefinitionRegistry.java`
- `backend/ads-service/src/test/java/com/marketinghub/pipeline/service/PipelineServiceTest.java`
- `frontend/src/api/pipeline/usePipelineMutations.ts`
- `frontend/src/pages/pipeline/PipelineCrudPage.tsx`
- `docs/registros/mois1.md`

## 2026-06-09 — Plano da Etapa 3 de aquecimento e ecossistema de mercado

- Criado plano de implementação da **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado** para a Biblioteca de Páginas de Vendas do MOIS.
- O plano separa a entrega em prompts executáveis, cobrindo canonização, schema, contratos, backend, worker, frontend, score, observabilidade e priorização comercial.
- A proposta foca em transformar análises de páginas em dossiês de mercado com fontes, sinais, score de aquecimento, risco de saturação e recomendação de próximo experimento.

Arquivos alterados:
- `docs/implementacao/mois/plano-etapa-3-aquecimento-ecossistema-mercado.md`
- `docs/registros/mois1.md`

## 2026-06-09 — Canonização da Etapa 3 de aquecimento no pipeline MOIS

- Canonizada a **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado** no documento oficial do MOIS, com código `MARKET_WARMUP_RESEARCH`, slug `market-warmup-research`, posição 3, responsabilidade, módulo executor e aliases operacionais.
- Atualizado o registry oficial do backend para expor o pipeline `mois-sales-page-library-pipeline` com três etapas: obtenção de HTML, análise comercial e pesquisa de aquecimento de mercado.
- Atualizados os testes de contrato do pipeline oficial para proteger a nova etapa e a sincronização segura do pipeline ausente.

Arquivos alterados:
- `docs/canonical/mois-worker-canon.v1.md`
- `backend/ads-service/src/main/java/com/marketinghub/pipeline/definition/PipelineDefinitionRegistry.java`
- `backend/ads-service/src/test/java/com/marketinghub/pipeline/service/PipelineServiceTest.java`
- `docs/registros/mois1.md`

## 2026-06-09 — Schema da Etapa 3 de aquecimento de mercado

- Criado changelog Liquibase MySQL 5.7 para a **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado** da Biblioteca de Páginas de Vendas do MOIS.
- Adicionadas as tabelas operacionais de jobs, fontes públicas, sinais comerciais e resumo final do dossiê de aquecimento, com chaves estrangeiras para `mois_sales_page` e relacionamentos internos da etapa.
- Adicionados índices para operação por workspace, página, status, score e plataforma, preservando o backend como único ponto de acesso ao banco.

Arquivos alterados:
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-06-09-mois-sales-page-market-warmup.yaml`
- `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `docs/registros/mois1.md`

## 2026-06-09 — Contratos da Etapa 3 de aquecimento de mercado

- Definidos os DTOs e enums HTTP da **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado** na biblioteca MOIS, cobrindo status de job, temperatura, ecossistema, recomendação, plataforma, tipo de fonte e tipo de sinal.
- Documentados no Swagger os contratos públicos de solicitação/consulta da pesquisa e os contratos internos do worker para claim, conclusão e falha.
- O contrato de conclusão usa listas estruturadas de fontes, sinais e resumo, evitando JSON serializado dentro de campos textuais funcionais.

Arquivos alterados:

- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-09 — Card da Etapa 3 no pipeline de páginas de vendas

- Adicionado na tela `/mois/sales-pages-library/pipeline` o card visual da **Etapa 3 — Pesquisa de aquecimento e ecossistema**, deixando claro o objetivo comercial da etapa antes da execução operacional completa.
- O card apresenta entrada, saída esperada, critério de qualidade, volume elegível a partir das páginas já analisadas e checklist dos sinais que a etapa deve acompanhar.
- A mudança mantém a tela alinhada ao contrato canônico `MARKET_WARMUP_RESEARCH` e evita sugerir execução ativa enquanto o worker dedicado ainda está em ativação operacional.

Arquivos alterados:
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/registros/mois1.md`

## 2026-06-09 — Conclusão contratual da Fase 3 da Etapa 3 MOIS

- Concluída a Fase 3 da **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado** no nível de contrato HTTP.
- Atualizado o Swagger da Biblioteca de Páginas de Vendas para declarar respostas de erro padronizadas nos endpoints públicos e internos de aquecimento de mercado.
- O contrato passa a explicitar erros de validação, ausência de página/job/dossiê, falta de permissão operacional do worker e conflito de estado, mantendo a regra de não transportar JSON serializado em campos textuais funcionais.

Arquivos alterados:

- `docs/swagger/mois-sales-library-swagger.yaml`
- `docs/registros/mois1.md`

## 2026-06-10 — Fase 4 da Etapa 3 de aquecimento de mercado

- Implementado o service de responsabilidade única para orquestrar a pesquisa de aquecimento da Biblioteca de Páginas de Vendas MOIS, cobrindo solicitação, consulta de resumo, fontes, sinais, claim interno, conclusão e falha de jobs.
- Centralizada a persistência JDBC da etapa no pacote permitido `com.marketinghub.repository`, mantendo o backend como único ponto de acesso ao banco.
- Adicionados testes unitários do service para proteger reutilização de job ativo, claim do worker, persistência de fontes/sinais/resumo, validação de índice de fonte e leitura de listas funcionais sem JSON serializado em texto.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupService.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jdbc/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageMarketWarmupGateway.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jdbc/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageMarketWarmupRepository.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupServiceTest.java`
- `docs/registros/mois1.md`

## 2026-06-10 — Correção arquitetural do aquecimento de mercado MOIS

- Corrigida a violação arquitetural da Biblioteca de Páginas de Vendas MOIS removendo dependências do pacote de repository para DTOs internos do módulo MOIS.
- O gateway de persistência passou a expor dados primitivos/contratos próprios da camada de repository, e o service MOIS assumiu explicitamente os mapeamentos entre contratos HTTP e persistência.
- Movida a persistência de aquecimento para o pacote canônico `repository.jpa.mois.bibliotecapaginavenda.worker.v1`, alinhado à exceção arquitetural já validada para a biblioteca MOIS.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupService.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageMarketWarmupGateway.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageMarketWarmupRepository.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupServiceTest.java`
- `docs/registros/mois1.md`

## 2026-06-10 — Fase 5 da Etapa 3 de aquecimento de mercado

- Implementados no controller da Biblioteca de Páginas de Vendas MOIS os endpoints públicos da **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado** para solicitar pesquisa, consultar resumo, listar fontes e listar sinais.
- Implementados no mesmo escopo MOIS os contratos internos do worker para reservar, concluir e falhar jobs de aquecimento, com validação de payload e mapeamento objetivo de erros HTTP.
- Adicionados testes de controller para proteger os contratos públicos, o claim interno, a conclusão/falha do worker e os cenários de validação/conflito.

Arquivos alterados:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryControllerTest.java`
- `docs/registros/mois1.md`

## 2026-06-10 — Fase 6 da Etapa 3 de aquecimento de mercado

- Implementado no worker MOIS o ciclo V1 da **Etapa 3 — Pesquisa de Aquecimento e Ecossistema de Mercado**, com claim, processamento, conclusão e falha sempre via backend principal.
- Adicionada coleta pública configurável por busca web, com geração de queries a partir da análise comercial concluída da página e registro do payload bruto recebido antes da normalização.
- Criado dossiê inicial estruturado com fontes, sinais básicos e resumo comercial sem acesso direto ao banco e sem JSON serializado dentro de campos textuais funcionais.
- Adicionados testes unitários para geração de queries e montagem do dossiê V1.

Arquivos alterados:
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/client/BackendClient.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/config/WorkerProperties.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/model/WorkerDtos.java`
- `mois-sales-library-worker/src/main/resources/application.yml`
- `mois-sales-library-worker/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/`
- `mois-sales-library-worker/pom.xml`
- `docs/registros/mois1.md`

## 2026-06-10 — Etapa 3 MOIS / Fase 7 — Motor de score e recomendação

- Implementado motor backend do Market Warm-up Score para calcular score, temperatura, tipo de ecossistema e recomendação objetiva a partir das fontes e sinais rastreáveis salvos na pesquisa de aquecimento.
- A conclusão do job passa a persistir o resumo calculado pelo backend, reduzindo dependência de classificação enviada pelo worker e mantendo a explicação do score vinculada aos sinais.
- Adicionados testes unitários para cenários quente, promissor, frio e saturado.

Arquivos principais:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupScoreEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupService.java`
- `backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupServiceTest.java`

## 2026-06-10 — MOIS Etapa 3 — Fase 8: UI de aquecimento no detalhe da página

- Executada a Fase 8 do plano `docs/implementacao/mois/plano-etapa-3-aquecimento-ecossistema-mercado.md`.
- Criados hooks frontend para consultar resumo, fontes e sinais da pesquisa de aquecimento e para solicitar uma nova pesquisa via backend MOIS.
- Adicionado bloco **Aquecimento do Mercado** na tela de detalhe da página da biblioteca de páginas de venda, com score, temperatura, ecossistema, recomendação, fontes rastreáveis, sinais comerciais, estados de carregamento, erro e vazio.
- Mantido o padrão arquitetural: o frontend consome somente endpoints do backend MOIS já existentes e os links externos das fontes abrem em nova aba.

Arquivos principais:

- `frontend/src/api/mois/types.ts`
- `frontend/src/api/mois/useMoisSalesLibrary.ts`
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`

## 2026-06-10 — MOIS Etapa 3 — Fase 9: biblioteca e pipeline de aquecimento

- Executada a Fase 9 do plano `docs/implementacao/mois/plano-etapa-3-aquecimento-ecossistema-mercado.md`.
- O backend passou a expor contadores globais da Etapa 3 no resumo da biblioteca e os campos de aquecimento mais recentes na listagem de páginas.
- A tela de pipeline passou a mostrar cobertura real da Etapa 3: elegíveis, concluídos, fila, falhas e temperaturas de mercado.
- A biblioteca passou a permitir priorização por score de aquecimento, com filtro simples por dossiê, temperatura quente/promissora, fila, falha ou ausência de dossiê.

Arquivos principais:

- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `frontend/src/api/mois/types.ts`
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/swagger/mois-sales-library-swagger.yaml`

## 2026-06-10 — MOIS Etapa 3 fase 9: priorização na biblioteca e pipeline

- Executada a fase 9 do plano de aquecimento do ecossistema de mercado, conectando a biblioteca e o pipeline aos campos consolidados da Etapa 3.
- A listagem da Biblioteca de Páginas de Vendas agora aceita filtro operacional de aquecimento e ordenação backend por maior score, permitindo priorização global das páginas quentes/promissoras sem depender apenas da ordenação local da primeira página.
- O card de pipeline da Etapa 3 direciona o operador diretamente para a biblioteca filtrada por mercados quentes/promissores e ordenada pelo score de aquecimento.

Arquivos principais:

- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`
- `frontend/src/api/mois/useMoisSalesLibrary.ts`
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/swagger/mois-sales-library-swagger.yaml`

## 2026-06-10 — MOIS Etapa 3 fase 10: observabilidade e saneamento operacional

- Executada a fase 10 do plano de aquecimento do ecossistema de mercado para impedir que jobs da Etapa 3 fiquem parados silenciosamente.
- Adicionados logs operacionais mais explícitos para claim sem fila, concorrência de claim, início de complete, fontes públicas persistidas e fail com categoria/mensagem.
- Criada ação backend para refileirar jobs `FETCHING` antigos, permitindo reprocessamento sem intervenção direta no banco.
- Adicionada métrica de jobs presos ao resumo da biblioteca e ao card de pipeline da Etapa 3, com botão operacional para reprocessar presos.

Arquivos principais:

- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupService.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageMarketWarmupRepository.java`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `docs/swagger/mois-sales-library-swagger.yaml`

## 2026-06-10 — Fase 11 da Etapa 3: ranking de oportunidades MOIS

- implementado endpoint backend `GET /api/mois/sales-library/market-warmup/opportunity-ranking` para priorizar páginas por score comercial combinado, usando score da página, score de aquecimento, risco de saturação e recência das evidências.
- adicionados contratos de resposta para ranking de oportunidades da Etapa 3, com próxima ação objetiva para experimento ou pesquisa OPRM/MDS.
- adicionada visualização na Biblioteca de Páginas de Vendas com ranking de oportunidades comerciais e evidências resumidas.
- adicionados tipos e hook frontend para consumir o ranking pelo backend principal, mantendo o frontend sem acesso direto ao banco.

Arquivos principais:
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/web/MoisSalesLibraryController.java
- backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java
- frontend/src/api/mois/types.ts
- frontend/src/api/mois/useMoisSalesLibrary.ts
- frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx

## 2026-06-10 — Correção da listagem da Etapa 3 MOIS

- Corrigida a causa-raiz da falha ao carregar a Biblioteca de Páginas de Vendas com filtros de aquecimento e o ranking de oportunidades comerciais.
- A consulta de leitura usava `mws.recommendation`, mas o schema real persiste a recomendação comercial em `mois_sales_page_market_warmup_job.recommendation`; a tabela de resumo não possui essa coluna.
- A listagem, o detalhe da página e o ranking passam a ler `mwj.recommendation`, mantendo a tela apta a listar páginas sem dossiê para iniciar a Etapa 3.

Arquivos principais:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`

## 2026-06-10 — Correção da pesquisa pública do Aquecimento MOIS

- Corrigida a causa-raiz do erro `PUBLIC_SEARCH_ERROR` no dossiê de Aquecimento do Mercado da Biblioteca de Páginas de Vendas.
- A coleta primária no DuckDuckGo pode retornar uma página de desafio antirobô sem resultados rastreáveis; o worker agora registra essa condição e usa fallback RSS público para não transformar bloqueio operacional em falha comercial do mercado.
- Adicionados testes de regressão para garantir que página de bloqueio não vire fonte falsa e que o fallback entregue fontes rastreáveis ao dossiê.

Arquivos principais:
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/DuckDuckGoPublicWebSearchClient.java`
- `mois-sales-library-worker/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/DuckDuckGoPublicWebSearchClientTest.java`

## 2026-06-11 — Reorientação da Etapa 3 para engenharia de sucesso do produto

- Atualizada a regra canônica da **Etapa 3** da Biblioteca de Páginas de Vendas: o objetivo deixa de ser apenas medir aquecimento genérico de mercado e passa a ser explicar **como um produto aparentemente vencedor conseguiu vender**.
- A investigação agora deve buscar evidências de autoridade, produtor/marca, influenciador, canais de aquisição, aula/live, WhatsApp, comunidade, afiliados, marketplace, prova social e riscos comerciais.
- Ajustados os textos da UI para apresentar o bloco como **Engenharia de Sucesso do Produto**, com foco em hipótese de venda, canais encontrados, alavancas de sucesso e próxima investigação.
- Ajustado o worker para incluir o produtor/marca no payload de claim e montar buscas públicas mais específicas, reduzindo resultados genéricos por palavras isoladas do nome do produto.

Arquivos principais:
- `docs/canonical/mois-worker-canon.v1.md`
- `docs/swagger/mois-sales-library-swagger.yaml`
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `frontend/src/pages/mois/MoisSalesPagesPipelinePage.tsx`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesPageMarketWarmupService.java`
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/MoisSalesPageMarketWarmupRepository.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupQueryBuilder.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessor.java`

## 2026-06-11 — Detalhamento narrativo do dossiê de sucesso do produto

- Refinada a tela de detalhe da Biblioteca de Páginas de Vendas para apresentar uma leitura executiva no formato esperado pelo operador: por que o produto provavelmente fez sucesso e quais evidências sustentam essa hipótese.
- Adicionado detalhamento em blocos objetivos: figura de autoridade, canal forte provável, funil de venda, promessa/dor clara, oferta com valor percebido e máquina provável de venda.
- Ajustado o resumo produzido pelo worker para escrever explicitamente que o sucesso pode não depender só da página de vendas, mas de autoridade, audiência, funil educacional, promessa clara e prova social/oferta.

Arquivos principais:
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessor.java`
- `mois-sales-library-worker/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessorTest.java`

## 2026-06-11 — Tela da Etapa 3 em formato de resposta executiva

- Ajustada a tela de detalhe da Biblioteca de Páginas de Vendas para apresentar o dossiê de sucesso em formato parecido com uma resposta consultiva: resposta executiva, leitura do produtor, cinco blocos de análise com conclusão e a máquina provável de venda.
- A tela passa a priorizar uma explicação legível para o operador antes das fontes e sinais técnicos, mantendo as evidências rastreáveis logo abaixo.

Arquivo principal:
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`

## 2026-06-11 — Correção de compilação do worker de Biblioteca de Páginas MOIS

- Corrigida a causa-raiz do erro de compilação que aparecia como `cannot find symbol: variable log` no worker MOIS.
- O arquivo de processamento de aquecimento continha métodos duplicados após a evolução da Etapa 3; isso interrompia a compilação e impedia o Lombok de gerar corretamente o logger nas classes anotadas.
- Removida a duplicidade mantendo a versão mais específica da sugestão de investigação da máquina de venda.

Arquivo principal:
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessor.java`

## 2026-06-11 — Remoção de conclusões fixas na investigação de sucesso MOIS

- removidas conclusões hardcoded da narrativa da tela de detalhe da Biblioteca de Páginas de Vendas; a UI passa a exibir perguntas de pesquisa, evidências públicas e leituras persistidas, deixando lacunas sem conclusão.
- ajustado o worker de aquecimento de mercado para não gerar hipótese genérica de sucesso quando não houver sinais públicos suficientes; próximas ações passam a ser solicitações de pesquisa ao modelo.
- atualizado o cânone do MOIS para bloquear conclusões comerciais fixas antes da combinação entre modelo, solicitações de pesquisa e fontes/sinais públicos.

Arquivos principais:
- frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx
- mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessor.java
- docs/canonical/mois-worker-canon.v1.md

## 2026-06-11 — Revisão de causa-raiz nas conclusões da Etapa 3 MOIS

- verificado que a correção anterior ainda tratava consequência: a tela deixou de mostrar conclusões antigas, mas continuava criando seções e perguntas fixas no próprio frontend.
- corrigida a causa-raiz na UI: a narrativa de investigação agora renderiza somente conclusão persistida, próxima pesquisa registrada, fontes públicas e sinais/leitura persistidos; quando o contrato não traz dado, a tela mostra lacuna operacional.
- reforçada a regra canônica para impedir não só conclusões fixas, mas também perguntas/seções explicativas hardcoded na UI da Etapa 3.

Arquivos principais:
- frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx
- docs/canonical/mois-worker-canon.v1.md

## 2026-06-11 — Custo do modelo por página de venda MOIS

- solicitação: exibir e persistir o custo de uso do modelo para cada página de venda da Biblioteca MOIS, garantindo que novos nichos já nasçam com custo inicial quando houver custo de origem informado.
- causa-raiz tratada: a análise da página registrava score e payload, mas não persistia tokens/custo do modelo no estado consolidado da página; além disso, a criação de nicho permitia nascer com `cost` informado e `totalCost` nulo.
- foi feito: adicionada persistência de `model_name`, `input_tokens`, `output_tokens` e `model_cost_usd` em `mois_sales_page` e `mois_sales_page_job_execution`, cálculo de custo batch pelo catálogo OpenAI do backend e exibição na biblioteca/detalhe da página.
- foi feito: a criação de nicho passa a iniciar `totalCost` com `totalCost` explícito, ou com `cost` quando o total não for informado, ou zero como fallback.
- frontend MOIS (`/mois/sales-pages-library`): ajustada a listagem de priorização para exibir temperatura Hotmart e data do dossiê, removendo score de sucesso e fase no diagrama da tabela principal para deixar a decisão comercial mais direta.

## 2026-06-11 — Correção de build da Biblioteca MOIS

- corrigida a compilação do backend após evolução do contrato `SalesLibraryPageResponse`, alinhando o mock de teste aos campos de aquecimento de mercado adicionados ao DTO.
- corrigida a compilação do frontend restaurando a função de apresentação da fase/status do pipeline na listagem da Biblioteca de Páginas de Vendas.

Arquivos principais:
- backend/ads-service/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryServiceTest.java
- frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx

## 2026-06-11 — Clareza do dossiê de sucesso MOIS

- corrigida a causa-raiz da confusão na tela de dossiê: a UI mostrava pesquisas públicas sem explicar que elas servem para encontrar autoridade, audiência, prova social e canais que podem justificar as vendas do produto.
- o detalhe da Biblioteca MOIS passa a destacar produto analisado, produtor, oferta, promessa, mecanismo e provas antes das fontes públicas.
- o backend passa a expor `productName` e `producerName` no contrato de página consolidada, usando `mois_sales_page` como estado operacional e `mois_collected_reference` apenas como origem bruta vinculada.

Arquivos principais:
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java
- frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx
- docs/swagger/mois-sales-library-swagger.yaml

## 2026-06-11 — Auditoria OpenAI no dossiê MOIS

- adicionada seção visual separada no detalhe da Biblioteca MOIS para exibir prompt usado, request enviado para OpenAI, response cru da OpenAI e schema JSON quando houver.
- a visualização usa layout JSON colapsável, seguindo o padrão usado nas telas de GeraLanding para facilitar auditoria pelo usuário.
- corrigida a persistência da resposta crua da OpenAI na análise de página, separando `analysisNotes` de `responsePayloadJson`.

Arquivos principais:
- frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java
- backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java
- mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/openai/OpenAiSalesPageAnalyzer.java

## 2026-06-11 — Reinício factual do dossiê MOIS para produto 1057

- revertida a tentativa anterior de resolver o dossiê por filtragem genérica de fontes, pois a decisão agora é reconstruir a Etapa 3 passo a passo a partir de fatos simples.
- limpo o cânone da Etapa 3 para remover narrativa, score, hipóteses, fontes, sinais e conclusões de tela enquanto o dossiê não for reconstruído incrementalmente.
- verificado no banco que o produto 1057 tinha `hotmart_price` e `hotmart_producer` vazios; o campo legado `producer_name` apontava para `HubX Digital Marketing`, diferente do produtor exibido na página Hotmart.
- adicionados os dois fatos levantados manualmente na página Hotmart: preço `R$ 5.997,00` e produtor `Abrantes Lima Empreendimentos LTDA`.
- simplificada a tela de detalhe para mostrar somente o primeiro bloco factual do dossiê: preço e produtor Hotmart.

Arquivos principais:
- `docs/canonical/mois-worker-canon.v1.md`
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-06-11-mois-product-1057-hotmart-facts.yaml`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`

## 2026-06-11 — Pesquisa social qualificada do produtor no dossiê MOIS

- implementada a próxima parte do dossiê: quando o nome do produtor Hotmart existe, a Etapa 3 passa a gerar buscas específicas em Instagram, YouTube e TikTok usando o produtor como âncora.
- causa-raiz tratada: a pesquisa pública podia considerar resultados sociais genéricos, homônimos ou perfis do produtor falando de outro assunto; agora fontes sociais só entram quando contêm o mesmo nome do produtor e tokens comerciais semelhantes ao produto/promessa/mecanismo/oferta.
- o detalhe da Biblioteca MOIS passa a mostrar a seção “redes do produtor”, destacando temperatura, recomendação, score e fontes sociais qualificadas, sem misturar perfis não confirmados ao dossiê.

Arquivos principais:
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupQueryBuilder.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessor.java`
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`

## 2026-06-11 — Temperatura numérica Hotmart na Biblioteca MOIS

- exposto no contrato da Biblioteca de Páginas de Vendas o campo `hotmartTemperature`, lido de `mois_collected_reference.hotmart_temperature` junto com os demais fatos Hotmart.
- ajustada a tabela `/mois/sales-pages-library` para mostrar o indicador numérico coletado da Hotmart junto do rótulo de aquecimento do dossiê.
- ajustada a tela de detalhe do produto para exibir a temperatura Hotmart no bloco factual inicial do dossiê.

Arquivos principais:
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/dto/MoisSalesLibraryDtos.java`
- `backend/ads-service/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/service/MoisSalesLibraryService.java`
- `frontend/src/pages/mois/MoisSalesPagesLibraryPage.tsx`
- `frontend/src/pages/mois/MoisSalesPageLibraryDetailPage.tsx`
- `docs/swagger/mois-sales-library-swagger.yaml`

## 2026-06-13 — Foco operacional Hotmart na Biblioteca de Páginas de Vendas
- Após reset operacional da biblioteca, o `mois-sales-library-worker` foi configurado para usar apenas `HOTMART` como fonte padrão de análise/claim, removendo `CLICKBANK` da rotação `MOIS_SOURCES` nos compose local/deploy.
- O scheduler do `mois-clickbank-collector` passou a ficar desabilitado por padrão no `application.properties` e nos compose local/deploy, preservando o módulo disponível para acionamento explícito futuro, mas impedindo novos ciclos automáticos de ClickBank durante o reinício das páginas de vendas.
- Os cânones do worker MOIS e da coleta ClickBank foram atualizados para refletir que a operação vigente deve manter apenas Hotmart como fonte ativa nesse reinício.

## 2026-06-13 — Dados comerciais obrigatórios nas coletas Hotmart
- O coletor Hotmart passou a carregar descrição do produto no snapshot e no `rawMetadata`, junto com temperatura, produtor e `collectedAt`, para preservar comparações de novas coletas do mesmo produto no futuro.
- O `referenceId` enviado para persistência Hotmart passou a ser estável por produto quando houver `ucode`, mantendo histórico por job sem sobrescrever coletas futuras.
- O backend passou a expor `description` em `/api/v1/mois/hotmart/products`, e a tela Hotmart passou a exibir descrição e data de coleta por card.
- Adicionado changelog para ampliar `mois_collected_reference.hotmart_description` para 1000 caracteres e reduzir truncamento de descrições comerciais.

## 2026-06-13 — Hotmart ciclo 1 agendado para execução única às 00:05
- Ajustado o `HotmartCollectorScheduler` para executar o ciclo 1 de listagem uma única vez em **13/06/2026 às 00:05** no timezone `America/Sao_Paulo`.
- Atualizados teste do scheduler e cânone Hotmart para refletir o novo cron `0 5 0 13 6 *` e preservar a guarda operacional de execução somente no ano de 2026.

## 2026-06-13 — Reexecução Hotmart ciclo 1 às 01:35
- Após falha operacional às 00:05 causada por JWT Hotmart expirado, o ciclo 1 de listagem do `mois-hotmart-collector` foi reagendado para execução pontual única em **13/06/2026 às 01:35** no timezone `America/Sao_Paulo`.
- Atualizados scheduler, teste unitário e cânone Hotmart para usar o cron `0 35 1 13 6 *`, mantendo o alvo operacional de 400 produtos e a guarda de execução apenas em 2026.

### 2026-06-13 — Backfill de páginas de venda MOIS desabilitado por padrão

- Decidido que o backfill `MOIS_SALES_PAGE_BACKFILL_ENABLED` não é rotina automática de produção; ele deve permanecer `false` e ser usado apenas em migração controlada/manual.
- Atualizado o deploy do backend para injetar `MOIS_SALES_PAGE_BACKFILL_ENABLED=false` por padrão e alinhado o default interno do backend para evitar execução acidental em novos ambientes.
- Registrada a regra na Fase 5 do cânone MOIS, exigindo confirmação operacional em logs com `operacao=salesPageBackfill` quando houver reinício/execução.


## 2026-06-13 11:58:40 UTC-3
- solicitada programação da próxima coleta de página de vendas Hotmart para 12:20.
- a causa operacional é que o ciclo 2 é o responsável por enriquecer os produtos com URL/página de vendas, então o agendamento correto deve ficar no scheduler do ciclo 2.
- ajustado o ciclo 2 do `mois-hotmart-collector` para execução pontual em 13/06/2026 às 12:20 no timezone `America/Sao_Paulo`, com guarda de ano para evitar repetição futura indevida.
- atualizado o teste do scheduler e o cânone de mapeamento dos ciclos Hotmart para manter a documentação aderente ao comportamento operacional.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/AGENTS.md

## 2026-06-14 — Reagendamento Hotmart ciclo 1 para 13:00
- Após constatar que a execução anterior do ciclo 1 não coletou produtos porque o JWT Hotmart estava expirado/inválido, o ciclo 1 de listagem do `mois-hotmart-collector` foi reagendado para execução pontual única em **14/06/2026 às 13:00** no timezone `America/Sao_Paulo`.
- Atualizados scheduler, teste unitário e cânone Hotmart para usar o cron `0 0 13 14 6 *`, mantendo o alvo operacional de 400 produtos e a guarda de execução apenas em 2026.

## 2026-06-14 — Reagendamento Hotmart ciclo 1 para 22:00
- Por solicitação operacional, o ciclo 1 de listagem do `mois-hotmart-collector` foi reagendado de 13:00 para execução pontual única em **14/06/2026 às 22:00** no timezone `America/Sao_Paulo`.
- Atualizados scheduler, teste unitário e cânone Hotmart para usar o cron `0 0 22 14 6 *`, mantendo o alvo operacional de 400 produtos e a guarda de execução apenas em 2026.

## 2026-06-15 — Hotmart: descrição completa em LONGTEXT
- Causa-raiz investigada: a coleta Hotmart de 14/06/2026 às 22:00 recebeu 400 produtos, mas 119 referências não entraram em `mois_collected_reference` porque `hotmart_description` estava limitada a `VARCHAR(1000)` e as descrições reais da Hotmart excediam esse tamanho.
- Criado changelog Liquibase para alterar o banco para `hotmart_description LONGTEXT NULL`, preservando a descrição comercial completa e evitando perda de referências relacionais em novas coletas.
- Atualizado o documento canônico do fluxo Hotmart para refletir que a descrição comercial deve ser persistida completa.

## 2026-06-15 14:24:24 UTC-3
- ajustado o agendamento pontual do ciclo 1 de coleta Hotmart para executar hoje, 15/06/2026, às 14:45 no timezone `America/Sao_Paulo`.
- mantido o cron literal diretamente na anotação `@Scheduled`, conforme regra operacional de agendamentos Spring Boot.
- atualizado o teste unitário do scheduler para prevenir divergência entre código e agendamento esperado.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md


## 2026-06-16 — Reagendamento pontual do ciclo 2 Hotmart

- Solicitação: agendar o ciclo 2 do Hotmart para execução hoje, 16/06/2026, às 04:10 no fuso `America/Sao_Paulo`.
- Foi feito: atualizado o scheduler do `mois-hotmart-collector` para cron literal `0 10 4 16 6 *`, mantendo execução pontual restrita ao ano de 2026.
- Também foi atualizado o documento canônico de mapeamento dos ciclos Hotmart para refletir o novo horário operacional.

## 2026-06-16 — Hotmart ciclo 2 sem repetição

- Alterado o fluxo do ciclo 2 Hotmart para buscar candidatos em endpoint próprio, separado da tela administrativa.
- O novo endpoint retorna produtos do ciclo 1 mais recente ainda não processados por ciclos 2 anteriores, evitando repetir os mesmos registros em reexecuções.
- A comparação considera `reference_id`, `product_url`, `sales_page_url` e combinação título + produtor para cobrir produtos sem `ucode` estável.

## 2026-06-16 12:11:47 UTC-3
- solicitação operacional: montar a próxima execução do ciclo 2 da ingestão Hotmart para hoje às 13:15.
- raciocínio aplicado: manter a execução pontual no scheduler existente do `mois-hotmart-collector`, usando cron literal direto e timezone `America/Sao_Paulo`, sem alterar o contrato de ingestão nem o fluxo de persistência.
- registro do que foi feito: reagendado o método do Ciclo 2 Hotmart para `16/06/2026 13:15 UTC-3`, atualizado o teste unitário que valida o cron e preservado o ciclo 1 sem mudança.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/AGENTS.md
  - docs/registros/mois1.md

## 2026-06-16 — Visibilidade do processamento automático da Biblioteca Sales Pages

- Adicionados indicadores operacionais na tela do pipeline de páginas de vendas para mostrar se o processamento automático está ativo, a última captura feita, páginas capturadas na última hora, páginas ainda sem HTML útil e velocidade média de avanço.
- Expandido o resumo do backend da biblioteca MOIS com métricas de acompanhamento da etapa 1 baseadas em `mois_sales_page` e `mois_sales_page_job_execution`.
- Atualizado o contrato Swagger e o tipo TypeScript do resumo para manter frontend, backend e documentação sincronizados.
## 2026-06-16 — Temperatura Hotmart na biblioteca de sales pages

- Corrigida a causa-raiz da tela `/mois/sales-pages-library` exibir `—` na temperatura Hotmart: páginas consolidadas novas estavam sem `collected_reference_id`, embora a referência Hotmart existisse em `mois_collected_reference` com a mesma URL canônica.
- Ajustada a consulta da biblioteca para usar a referência direta quando existir e, caso contrário, recuperar a última referência Hotmart pela URL da página/produto, preservando preço, produtor e temperatura na listagem e no detalhe.
- Adicionado teste de regressão para garantir que a consulta mantenha o fallback por URL e não volte a depender apenas do vínculo direto.

## 2026-06-16 — Remoção do acionamento manual da etapa 1

- Removida da tela do pipeline da Biblioteca de Sales Pages a área de execução manual da captura da etapa 1, porque essa etapa já acontece de forma automática.
- Mantidos os indicadores de processamento automático para o usuário acompanhar avanço, última captura, volume pendente e velocidade sem precisar acionar comandos manuais.

## 2026-06-17 — Bloqueio de dossiê antes da análise comercial

- Corrigida a causa-raiz de produtos capturados aparecerem com dossiê pendente antes de terem análise comercial concluída.
- O backend passa a recusar a criação de job de dossiê quando a página ainda não está em `DONE`/`ANALYZED`, e o worker deixa de reservar jobs pendentes que ainda não estejam elegíveis.
- A tela de detalhe bloqueia o botão de iniciar dossiê até a análise comercial terminar, e a listagem informa “Dossiê: Aguardando análise” para evitar uma decisão operacional incorreta.

## 2026-06-17 — Ajuste arquitetural dos repositories da Biblioteca de Sales Pages

- Corrigida a causa-raiz da falha de arquitetura em `moisSalesLibraryRepositoriesMustStayInCanonicalRepositoryPackage`: implementações JDBC concretas estavam no pacote canônico reservado a contratos/interfaces de persistência.
- Movidas as implementações JDBC para subpacote de implementação, mantendo no pacote canônico apenas os gateways/interfaces esperados pela regra arquitetural.
- Atualizado o teste unitário do repository de pricing para apontar para a nova localização da implementação sem alterar o contrato consumido pelos services.

## 2026-06-17 — Ajuste arquitetural dos DTOs da Biblioteca de Sales Pages

- Corrigida a causa-raiz da falha de arquitetura `moisSalesLibraryWebShouldOnlyDependOnServiceLayer`: o controller dependia do pacote `.dto`, mas a regra do módulo MOIS permite que a camada web dependa apenas da camada `.service` do mesmo namespace/versão.
- Movido o agrupador de contratos HTTP `MoisSalesLibraryDtos` para a camada `.service`, mantendo os contratos como `record` e evitando novo acoplamento direto da web com camada fora do padrão validado.
- Atualizados imports em controller, services, scheduler e testes relacionados para preservar o contrato funcional sem alterar endpoints.

## 2026-06-17 — Endpoint pending da etapa 2 da Biblioteca de Páginas de Vendas
- Criado endpoint backend `GET /api/mois/sales-library/pending` para listar páginas com HTML útil (`html_bytes > 0`) elegíveis para análise comercial, sem reservar job nem alterar status operacional.
- O contrato retorna workspace, origem, limite, total retornado e itens com `pageId`, `jobId` pendente quando já existir, URL canônica, bytes de HTML, status de análise, próxima tentativa e disponibilidade de HTML bruto.
- Atualizada a documentação Swagger da Biblioteca de Páginas de Vendas e adicionados testes de controller/service para prevenir regressão no contrato de auditoria da fila real da etapa 2.

## 2026-06-17 — Protocolo padrão módulo na Biblioteca de Sales Pages

- Aplicado o protocolo padrão módulo no executor `mois-sales-library-worker`, mantendo o backend fora do escopo desta alteração.
- A etapa 2 de análise comercial foi isolada como etapa plugável em `pipeline.pageanalysis`, usando o núcleo genérico `PipelineWorker`, `StageProcessor`, `StageContext`, `StageResult` e `StageArtifact`.
- As integrações OpenAI da análise comercial foram movidas para o pacote concreto da própria etapa, evitando vazamento de tecnologia para o núcleo do pipeline.
- Ajustadas as regras ArchUnit do worker para validar núcleo sem dependência de etapas concretas, independência entre etapas, ausência de ciclos, processors por contrato e núcleo sem tecnologias concretas.

## 2026-06-17 — Correção do bloqueio da Etapa 2 pageanalysis

- Verificado via MCP que a Etapa 2 de análise comercial estava sendo disparada pelo `mois-sales-library-worker`, mas a conclusão falhava no backend ao gravar o resultado em `mois_sales_page_job_execution`.
- Causa-raiz: o SQL de conclusão da análise usa `parser_version` e `prompt_version`, porém a tabela consolidada de histórico ainda não possuía essas colunas no schema real.
- Correção preparada: novo changelog incremental adiciona `parser_version` e `prompt_version` em `mois_sales_page_job_execution`, com teste de contrato para impedir recorrência entre SQL do backend e Liquibase.
## 2026-06-17 — Fuso horário São Paulo no pipeline de sales pages

- Ajustada a tela `/mois/sales-pages-library/pipeline` para exibir o horário da última captura no fuso `America/Sao_Paulo`, evitando leitura operacional em UTC ou no fuso local do navegador.
- Mantido o dado vindo do backend como fonte de verdade; o frontend apenas formata a data para o fuso usado pela operação no Brasil.

## 2026-06-18 — Correção da causa-raiz do fuso na última captura da Biblioteca Sales Pages

- Corrigida a leitura de `DATETIME` do MySQL no backend da Biblioteca de Sales Pages para interpretar os horários operacionais como UTC antes de entregar `Instant` ao frontend.
- Causa-raiz: o JDBC podia converter `DATETIME` usando o fuso padrão do servidor, fazendo a tela exibir horário futuro em São Paulo mesmo com o Windows do usuário no fuso correto.
- Adicionado teste de regressão no resumo operacional para impedir que a “última captura feita” volte a depender do fuso do servidor Java/MySQL.

## 2026-06-18 — Correção do teste de listagem operacional da Biblioteca Sales Pages

- Corrigido o mock do teste `shouldListEntriesFromOperationalSalesPageTable` para usar a mesma assinatura UTC do backend ao ler `DATETIME` de `mois_sales_page`.
- Causa-raiz: o service passou a chamar `ResultSet#getTimestamp(String, Calendar)` para evitar dependência do fuso do servidor, mas o teste ainda simulava `ResultSet#getTimestamp(String)`.
- A correção mantém a proteção contra regressão de fuso horário sem alterar o contrato funcional da listagem operacional.

## 2026-06-19 — Melhoria da pesquisa do dossiê MOIS

- Corrigida a causa-raiz de dossiês fracos quando o produto possui nome ambíguo: a etapa de aquecimento agora usa uma camada de planejamento de queries com OpenAI quando houver chave configurada, preservando fallback heurístico quando a integração não estiver disponível.
- As buscas passam a priorizar produtor, domínio da página, título/subtítulo, redes sociais, reviews, afiliados, prova social e canais de aquisição antes de concluir o dossiê.
- Adicionados testes para impedir que produtos ambíguos voltem a gerar pesquisas genéricas por termos soltos sem domínio ou contexto comercial.

## 2026-06-20 — Correção de dossiê MOIS com fontes genéricas
- diagnosticado o dossiê vazio/sem informação do produto 262: o backend entregava ao worker apenas `producer_name` legado e título fraco quando existiam fatos Hotmart mais específicos, e o worker aceitava resultados web genéricos por palavras soltas do título como "revolução".
- corrigida a causa-raiz em duas camadas: o claim do dossiê passa a priorizar `product_name` e `hotmart_producer`, e o worker bloqueia fontes públicas que não contenham âncora mínima de produtor ou semelhança suficiente com o produto.
- adicionado teste de regressão para impedir que resultados genéricos como dicionário/história sejam persistidos como dossiê comercial.

Arquivos principais:
- `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/mois/bibliotecapaginavenda/worker/v1/jdbc/MoisSalesPageMarketWarmupRepository.java`
- `mois-sales-library-worker/src/main/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessor.java`
- `mois-sales-library-worker/src/test/java/com/marketinghub/mois/bibliotecapaginavenda/worker/v1/marketwarmup/MarketWarmupProcessorTest.java`

## 2026-06-20 — Acompanhamento do dossiê MOIS do produto 379

- Diagnosticado que o dossiê do produto 379 (`Mentoria Fluxo`) entrou na fila, foi processado pelo worker e falhou por `PUBLIC_SEARCH_ERROR`, sem fontes públicas rastreáveis persistidas.
- Corrigida a causa-raiz provável: páginas consolidadas sem `collected_reference_id` agora recuperam nome do produto e produtor Hotmart por correspondência de URL no momento de criar/reservar o dossiê, evitando que o worker pesquise com âncoras fracas ou incompletas.

## 2026-06-20 — Transparência das tentativas do dossiê MOIS

- A tela de detalhe do produto passa a mostrar as tentativas de pesquisa pública feitas pelo worker do dossiê, incluindo query executada, quantidade de resultados lidos, resultados aproveitados, descartados e exemplo retornado.
- O backend passa a persistir tentativas de busca em tabela própria e expor endpoint dedicado, para que o frontend mostre a verdade operacional persistida em vez de depender de logs técnicos.
- O worker passa a reportar as tentativas ao concluir ou falhar o dossiê, explicando quando os resultados encontrados eram genéricos ou sem ligação comprovável com produto/produtor.

## 2026-06-20 — Exibição da análise comercial no detalhe da Biblioteca Sales Pages

- Diagnosticado que o produto 401 possuía análise comercial persistida (`scoreTotal=78`, status `DONE` e JSONs de seções/copy/visual/imagens), mas a tela de detalhe não renderizava essa informação.
- Corrigida a causa-raiz no frontend MOIS: a rota `/mois/sales-pages-library/:pageId` agora consulta o endpoint de análise da página e exibe score, status, modelo, data, notas e blocos colapsáveis dos JSONs comerciais retornados pelo backend.
- Mantida a regra de verdade da tela: a interface apenas apresenta os dados do contrato existente do backend, sem inferir conteúdo localmente.

## 2026-06-20 — Correção do diagnóstico visual da Biblioteca Sales Pages

- Corrigida a causa-raiz da análise comercial tratar páginas carregadas de imagens como se não tivessem evidência visual: o worker agora envia ao modelo um resumo objetivo de densidade de imagens e sinais de prova/depoimento extraídos do HTML.
- Ajustado o prompt da análise para avaliar excesso, repetição e poluição visual como explicação do sucesso observado, sem transformar a saída em lista de sugestões.

## 2026-06-20 — Cânone da análise sem sugestões na Biblioteca Sales Pages

- Registrada no cânone MOIS a regra de que a análise da Biblioteca Sales Pages é diagnóstico de sucesso de produtos vencedores, não geração de sugestões.
- Ajustado o worker para proibir sugestões, recomendações, próximos passos e chaves do tipo `recommended*` em todos os campos da resposta do modelo.
- Ajustado o prompt da análise para avaliar excesso, repetição e poluição visual, evitando recomendar mais imagens quando o problema comercial é organizar melhor o fluxo Dor → Resultado → Mecanismo → Prova → Oferta.

## 2026-06-21 — Status atual no detalhe da Biblioteca Sales Pages

- Corrigida a falta de confirmação visual após o comando **Voltar para pendente** na rota `/mois/sales-pages-library/:pageId`.
- A tela agora exibe um bloco explícito de **Status atual da solicitação**, com status geral, captura e análise comercial vindos do backend, além da confirmação retornada pelo endpoint de atualização manual.
- Ajustada a invalidação do cache do detalhe da página para recarregar a verdade atualizada do backend depois da mudança de status.

## 2026-06-22 — Formato vendido no dossiê da Biblioteca Sales Pages

- Incluído no contrato do backend o campo de formato vendido do produto, classificado a partir dos textos persistidos da página/Hotmart para diferenciar curso em vídeo, e-book/PDF, consultoria/mentoria, comunidade/assinatura ou software/ferramenta.
- Atualizada a tela de detalhe `/mois/sales-pages-library/:pageId` para exibir o card **O que está sendo vendido** no dossiê do produto, mantendo a regra de verdade da tela: o frontend apenas apresenta o dado retornado pelo backend.


## 2026-06-22 — Análise comercial da Biblioteca Sales Pages em OpenAI Flex

- Alterada a integração OpenAI da etapa de análise comercial do `mois-sales-library-worker` para usar Responses API com `service_tier: "flex"`, removendo o fluxo operacional de criação/polling da Batch API.
- Atualizado o cânone MOIS para refletir que a análise comercial usa Responses Flex, timeout canônico de 15 minutos e payload auditado com `service_tier: "flex"`.
- Ajustado o teste de contrato do analisador para impedir regressão para payload sem modo Flex.

## 2026-06-23 — Horários sem conversão de fuso na Biblioteca Sales Pages

- Corrigida a exibição dos horários no detalhe `/mois/sales-pages-library/:pageId` para apresentar o horário literal retornado pelo backend, sem conversão automática pelo fuso do navegador.
- A mudança evita que o histórico da página apareça com 3 horas a menos e mantém a tela mostrando a verdade persistida pelo backend.
## 2026-06-23 — Regra de exibição da análise mais recente na Biblioteca Sales Pages

- Revisada a decisão sobre retentativas de análise comercial: a tela deve refletir somente a execução mais recente da página, mesmo quando a tentativa mais recente falhar.
- Removida a preservação automática de análise anterior concluída no status consolidado, porque ela misturava execução antiga com tentativa nova e poderia esconder erro operacional atual.
- Mantida a leitura da causa operacional no histórico de execuções, preservando transparência para reprocessar quando a chamada OpenAI falhar.

## 2026-06-23 — Clareza do bloqueio do botão iniciar dossiê

- Corrigida a causa-raiz visual da tela de detalhe da Biblioteca Sales Pages não explicar por que o botão **Iniciar dossiê** ficava desabilitado quando o backend já retornava um dossiê existente.
- A tela agora mostra um aviso objetivo quando a página já possui dossiê registrado, evitando nova fila duplicada e direcionando o usuário para usar o dossiê exibido.


## 2026-06-23 — Reprocessamento explícito do dossiê MOIS

- Ajustada a tela de detalhe da Biblioteca Sales Pages para permitir novo pedido quando já existe dossiê concluído, exibindo explicitamente o botão **Reprocessar dossiê**.
- Mantido o bloqueio somente quando o backend indica dossiê em fila ou em processamento, e a tela informa que sempre exibirá o dossiê mais recente retornado pelo backend.

- 2026-06-23 — Ops Monitor fase 4: adicionados os coletores MOIS ClickBank, Hotmart e o MOIS Sales Library Worker ao cadastro monitorado do backend; a tela de operação agora permite filtrar módulos por criticidade/tipo e consultar histórico recente de incidentes sem inferência local de status.

## 2026-06-24 — Insumos GeraLanding na análise da biblioteca de páginas

- A análise comercial da biblioteca de páginas de vendas passou a registrar insumos específicos para o GeraLanding: wireframe, copy, prompt de imagens e preset design.
- Objetivo comercial: transformar páginas vencedoras observadas em padrões reutilizáveis para gerar landings melhores e mais alinhadas ao eixo Dor → Resultado → Mecanismo → Prova → Oferta.
- A tela de detalhe da página exibe a nova seção “Insumos para GeraLanding”, sempre baseada nos dados retornados pelo backend.

## 2026-06-24 — Insumos MOIS aplicados ao GeraLanding

- Os insights `geralanding_*` da Biblioteca de Páginas de Vendas deixaram de ser apenas consulta visual e passaram a alimentar os contratos `pending` das etapas GeraLanding.
- O uso esperado é como padrão abstrato de mercado para wireframe, copy, prompts de imagem e preset visual, sem copiar conteúdo, marca, URL, promessa ou identidade de páginas externas.

## 2026-06-24 — Separação canônica MOIS → banco → GeraLanding

- Decidido que o MOIS deve apenas gravar os insumos `geralanding_*` no banco e que o GeraLanding deve consumi-los a partir da persistência, sem depender diretamente de gateway/pacote do MOIS.
- Ajustado o backend GeraLanding para ler as referências vencedoras persistidas nas tabelas da Biblioteca de Páginas de Vendas pelo repositório central já permitido do GeraLanding.
- A decisão preserva o objetivo comercial dos insumos MOIS para melhorar wireframe, copy, imagens e preset visual, mas reduz acoplamento entre módulos e evita recorrência da violação ArchUnit de dependência direta GeraLanding → MOIS.

## 2026-06-25 — Custo total na Biblioteca de Páginas de Vendas
- adicionada exposição do custo total em USD das análises comerciais consolidadas no resumo da Biblioteca de Páginas de Vendas MOIS.
- ajustada a tela `/mois/sales-pages-library` para exibir o card **Custo total da biblioteca**, ajudando o operador a acompanhar investimento acumulado de IA na biblioteca.

## 2026-06-25 — Novo processo simples do dossiê da Biblioteca de Páginas de Vendas
- definido o novo fluxo em 5 etapas para gerar dossiê: extrair termos via OpenAI, persistir termos, pesquisar resultados, analisar relação com o produto e gravar conclusões finais no backend.
- registrado que o objetivo do dossiê é entender prestígio e aquecimento público do produto, identificando recursos externos que ajudam a aquecer o público além da página de venda.
- atualizado o cânone MOIS e criado documento simples do processo para orientar implementação futura do worker, backend e tela.

## 2026-06-25 — Implementação do dossiê com protocolos padrão
- implementado no backend o suporte persistente ao novo dossiê da Biblioteca de Páginas de Vendas: termos de pesquisa, resultados pesquisados e conclusão final vinculados ao job e ao item da biblioteca.
- exposto endpoint pending canônico para o worker iniciar a etapa de dossiê pelo backend, mantendo o backend como controlador de estado e persistência.
- ajustado o `mois-sales-library-worker` para enviar termos, resultados e dossiê final estruturados ao backend, sem acessar banco diretamente.
- aplicados e registrados o protocolo padrão backend e o protocolo padrão módulo para este fluxo.

## 2026-06-25 — Rotas externas do Ops Monitor para coletores MOIS

- Identificada a causa dos alertas “Fora do ar” nos coletores MOIS ClickBank/Hotmart: o cadastro operacional do Ops Monitor apontava para `host.docker.internal`, mas esses coletores estão expostos em rota externa monitorável.
- Criado changelog incremental para restaurar as URLs externas dos coletores e corrigir o endpoint de log do Hotmart, evitando recorrência do falso offline na tela de operação.

## 2026-06-25 — Pipeline de criação de dossiê MOIS v1

- Criado esqueleto do pipeline de dossiê MOIS v1 no módulo executor `mois-sales-library-worker`.
- Criado contrato backend inicial para etapa `intake`, expondo pending canônico para o executor.
- Aplicados protocolo padrão módulo e protocolo padrão backend para garantir versionamento, separação de responsabilidades e ponto inicial canônico antes do estudo detalhado das etapas.

## 2026-06-25 15:06:34 UTC-3

- Expandido o desenho do dossiê v1 para refletir o objetivo de entender o produto e mapear recursos usados para aquecer o público.
- Criadas etapas canônicas: intake, product-understanding, investigation-anchor-builder, warmup-resource-discovery, source-product-match, warmup-signal-extraction, warmup-map-builder e dossier-synthesis.
- Mantida a separação operacional: backend publica pendências por etapa e o módulo executor mantém processamento, integrações externas e evolução dos processors.

## 2026-06-26 — Pacotes canônicos do pipeline dossiê do produto v1

- Criados os pacotes do backend no padrão `com.marketinghub.pipelines.mois.dossieproduto.v1.<etapa>` para as etapas `fatosproduto`, `analisepagina`, `planejabuscas`, `qualificafontes` e `consolidadossie`.
- Criados os pacotes do executor `mois-sales-library-worker` no padrão `com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.<etapa>`.
- Atualizadas regras ArchUnit de backend e do worker para proteger controller/service/pending, contratos `record`, núcleo genérico sem dependência de etapas concretas e independência entre etapas.

## 2026-06-26 — Consolidação dos pacotes do dossiê da Página de Vendas

- Mantido no módulo executor apenas o pacote canônico `com.marketinghub.pipelines.dossie.v1` para o pipeline v1 de dossiê.
- Ajustado o backend para usar o pacote canônico `com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1`.
- Removidos os pacotes duplicados `dossieproduto.v1` do executor e do backend para evitar divergência operacional e de contratos.

## 2026-06-26 — RecebeRequest do dossiê de produto MOIS v1

- Implementados endpoints `recebeRequest` no backend para as etapas do pipeline `moissaleslibraryworker.dossieproduto.v1`, com identificador da página/produto na URL.
- Cada chamada coloca a página/produto em espera do módulo, atualiza a data operacional em UTC e registra a auditoria em `pipeline_dossieproduto` com `id_externo`, `request`, etapa, `jobId`, plataforma, prompt, schema e versão `v1`.
- Padronizados os nomes físicos das colunas operacionais da página/produto para `status_pipeline_dossieproduto` e `data_pipeline_dossieproduto` via changelog incremental.

## 2026-06-27 — JobId UUID no dossiê de produto MOIS v1

- Alterado o backend do fluxo `moissaleslibraryworker.dossieproduto.v1` para criar `jobId` por UUID no `/start` de cada etapa.
- Ajustado o `/pending` para devolver cada objeto pendente acompanhado do `jobId` ativo da etapa.
- Ajustado o `recebeRequest` para receber o `jobId` na URL junto com o identificador do objeto, mantendo a auditoria vinculada ao mesmo job iniciado.

## 2026-06-27 — Protocolo padrão módulo em dossieproduto.v1

- Aplicado no módulo executor `mois-sales-library-worker` para a solicitação `dossieproduto.v1` usando o pacote canônico consolidado `com.marketinghub.pipelines.dossie.v1`.
- Adicionado teste de contrato do executor para validar o catálogo completo de processors do dossiê v1, execução pelo `PipelineWorker`, saída estruturada, artefatos auditáveis não nulos e bloqueio de etapa sem processor registrado.
- Mantida a decisão de não recriar pacote duplicado `dossieproduto.v1`, preservando o backend fora do protocolo padrão módulo e o worker como executor operacional.

## 2026-06-27 — Correção Liquibase das colunas do dossiê MOIS

- Removida a dependência de posição física `AFTER dossie_produto_current_stage` na criação de `dossie_produto_updated_at`, que quebrava o bootstrap quando a coluna de etapa ainda não existia.
- Criado changeset incremental para garantir as colunas canônicas `status_pipeline_dossieproduto` e `data_pipeline_dossieproduto` após a criação das colunas legadas, cobrindo bancos onde o rename anterior foi marcado como executado antes da falha.
## 2026-06-27 — Endpoints internos do dossiê MOIS v1 ajustados

- Ajustados os controllers do backend `moissaleslibraryworker.dossieproduto.v1` para expor os endpoints internos com domínio `moissaleslibraryworker`, etapa na URL e `idExterno` como identificador operacional no caminho.
- Atualizada a documentação Swagger do dossiê para refletir `start`, `recebeRequest`, `recebeResponse` e `pending` no novo padrão canônico solicitado.

## 2026-06-27 — Destravamento da fila do dossiê MOIS v1
- Diagnosticado que o dossiê do produto 286 (`Vértuz App`) estava iniciado em `product-understanding`, mas a fila canônica da etapa retornava erro 500 antes de entregar trabalhos ao executor por causa de uma pendência legada do produto 329 sem `jobId` auditável.
- Corrigida a causa-raiz no backend: os endpoints `pending` do pipeline `dossieproduto.v1` agora recompõem um `jobId` UUID com auditoria mínima quando encontram pendências antigas sem registro rastreável, evitando que um item legado bloqueie os demais produtos da fila.

## 2026-06-27 — Tela de dossiê MOIS v1 sem endpoint legado

- Removida da tela de detalhe da Biblioteca Sales Pages a dependência do endpoint legado de `market-warmup` para exibir o dossiê do produto.
- Criado endpoint público de leitura do pipeline novo `dossieproduto.v1`, expondo auditoria de cada etapa com entrada, saída, prompt, schema, modelo, tokens, custo, erro e resultado final.
- Atualizada a tela para apresentar o resultado final consolidado e os registros de cada etapa diretamente a partir do backend novo.

## 2026-06-27 — Consulta de situação do pipeline dossieproduto.v1

- Criado endpoint interno `POST /api/internal/moissaleslibraryworker/dossieproduto/v1/{etapa}/stage-executions/{idExterno}/situacao` para consultar auditorias existentes por etapa, identificador externo e lista de status.
- Adicionada coluna `status` em `pipeline_dossieproduto`, com preenchimento nas novas auditorias e backfill dos registros existentes por request/response/erro.
- Atualizado o Swagger canônico do pipeline MOIS dossiê v1 com o contrato de entrada e resposta da consulta.

## 2026-06-27 21:56:47 UTC-3
- ajustado o pipeline `dossieproduto.v1` do `mois-sales-library-worker` para transportar auditoria bruta de chamadas OpenAI no resultado da etapa.
- causa-raiz tratada: o runner só enviava ao backend o input da etapa e a saída funcional consolidada, então uma etapa futura com ChatGPT poderia perder o request exato enviado à OpenAI e a resposta exata recebida.
- registro do que foi feito: `StageResult` agora aceita `OpenAiInteraction`; quando existir interação OpenAI, o runner chama `recebeRequest` com o request bruto e `recebeResponse` com a response bruta, preservando tokens, custo, modelo e erro.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - mois-sales-library-worker/AGENTS.md

## 2026-06-28 — Resposta final limpa no pipeline dossieproduto.v1

- Adicionado o campo `resposta_final` em `pipeline_dossieproduto` para guardar o texto funcional extraído do envelope OpenAI, preservando `response` como payload bruto de auditoria.
- Todas as etapas do backend `moissaleslibraryworker.dossieproduto.v1` passaram a preencher `resposta_final` ao receber o callback de resposta.
- Criado extrator compartilhado para localizar o campo `text` nos formatos de resposta da OpenAI e manter fallback seguro para respostas diretas.
## 2026-06-28 — Objetivos funcionais das etapas dossieproduto.v1 no worker

- Verificado o pipeline `dossieproduto.v1` do `mois-sales-library-worker` contra o contrato canônico de etapas do dossiê.
- Causa-raiz tratada: os processors das etapas retornavam status genérico de implementação futura, sem saída funcional explícita que comprovasse o objetivo de cada etapa.
- Ajuste aplicado: cada etapa agora devolve `OBJECTIVE_FULFILLED`, decisão de negócio específica, evidências do contexto recebido e artefato auditável do objetivo executado.
- Prevenção de recorrência: teste de contrato do `PipelineWorker` passou a validar o objetivo esperado, status funcional e artefato auditável de todas as etapas canônicas.

## 2026-06-28 — Gate de qualidade na síntese final do dossiê MOIS v1

- Diagnosticado que a etapa final `dossier-synthesis` encerrava o pipeline com uma resposta técnica genérica, sem dossiê comercial útil.
- Causa-raiz tratada: o backend entregava para a síntese final apenas metadados operacionais e o worker aceitava saída sem evidência comercial como sucesso.
- Ajuste aplicado: o backend agora inclui auditorias e respostas anteriores no `pending` da síntese final, e o worker bloqueia a conclusão quando não houver evidências comerciais suficientes para montar conclusão, evidências e próximos passos.
- Prevenção de recorrência: teste do `PipelineWorker` valida que a síntese final falha quando recebe apenas metadados operacionais.

## 2026-06-28 — Cards do reprocessamento do dossiê MOIS v1

- Diagnosticado na página 400 que o reprocessamento iniciado em `28/06/2026 03:27` era exibido junto de auditorias históricas das etapas.
- Causa-raiz tratada: os endpoints de leitura da tela consultavam registros por página/etapa/status sem fronteira da execução atual.
- Ajuste aplicado: a situação das etapas e o resumo público do pipeline agora consideram apenas registros a partir do último `intake` iniciado, preservando histórico no banco sem contaminar os cards do fluxo atual.

## 2026-06-28 — Auditoria OpenAI na etapa product-understanding do dossiê MOIS v1

- Ajustada a etapa `product-understanding` do worker de biblioteca de páginas MOIS para chamar OpenAI Responses Flex quando houver chave configurada, preservando request bruto, response bruto, texto final extraído, modelo e tokens no contrato de auditoria enviado ao backend.
- Ajustada a tela do dossiê para exibir request enviado à OpenAI, response recebido da OpenAI em JSON colapsável, texto final extraído e métricas de modelo/tokens.
- Causa-raiz: a etapa ainda podia executar em modo local sem gerar interação OpenAI, então a auditoria técnica não recebia os dados exigidos para diagnóstico e rastreabilidade comercial.

## 2026-06-28 — Request visível junto do response no dossiê MOIS v1

- Diagnosticado que a tela mostrava a response da OpenAI, mas o card mais recente ficava sem request porque request e response são persistidos em linhas de auditoria separadas do mesmo job.
- Causa-raiz tratada: a consulta de situação devolvia cada linha isoladamente, então a linha `CONCLUIDO` usada pela tela não herdava o request registrado antes em `AGUARDANDO_RETORNO_MODULO`.
- Ajuste aplicado: a consulta de situação agora correlaciona registros por página, etapa e `jobId`, exibindo o request auditado junto da linha de response do mesmo job.

## 2026-06-29 — Documentação do pipeline dossieproduto.v1

- Criado o documento de marketing `docs/marketing/pipeline_dossieproduto.md` detalhando objetivo geral, fluxo operacional, ordem das etapas, entradas, saídas, processamentos, critérios de qualidade e resultado de negócio esperado para o pipeline de geração de dossiê de produto MOIS.
## 2026-06-29 — Enriquecimento do documento de marketing do dossiê MOIS v1

- Atualizado `docs/marketing/pipeline_dossieproduto_v1.md` com exemplos reais pesquisados no banco de dados para orientar a leitura comercial do pipeline.
- Exemplos adicionados: `BLACK MAGRA` como caso com sinais externos promissores, `PACOTE COMPLETO - Vitalício - TUDO LIBERADO + Tripé` como caso com evidência pública fraca e `Resina LAB` como caso de síntese bloqueada por falta de evidência comercial.
- Objetivo: deixar o documento mais rico para decisão de negócio, reforçando que o dossiê deve sustentar avanço comercial apenas quando houver evidência conectada ao eixo Dor → Resultado → Mecanismo → Prova → Oferta.

## 2026-06-29 — Reorganização dos exemplos por etapa do dossiê MOIS v1

- Reorganizado `docs/marketing/pipeline_dossieproduto_v1.md` para que cada etapa do pipeline funcione como capítulo com exemplo real dentro do próprio capítulo.
- Os exemplos foram distribuídos entre `intake`, `product-understanding`, `investigation-anchor-builder`, `warmup-resource-discovery`, `source-product-match`, `warmup-signal-extraction`, `warmup-map-builder` e `dossier-synthesis`, usando registros reais pesquisados no banco.
- Objetivo: facilitar a leitura operacional e comercial de cada etapa, deixando claro o que cada capítulo deve produzir e como interpretar evidência forte, evidência fraca ou bloqueio.
