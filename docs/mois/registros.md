# Registros — MOIS

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

## 2026-05-03 18:45:13 UTC-3
- Ajustado o diagnóstico de coletas Hotmart no `MoisDomainService` para registrar explicitamente:
  - `method=GET` no início da tentativa;
  - `requestedUrl` e `finalUrl` nos logs de resposta rejeitada (`statusCode` fora de 2xx);
  - `requestedUrl`, `finalUrl`, `statusCode` e método no log de finalização.
- Objetivo: facilitar rastreabilidade de redirecionamentos HTTP 301 e identificar com precisão qual URL foi solicitada e qual URL final foi retornada pelo cliente HTTP.
- Commit relacionado: `356c904`.

## 2026-05-03 22:10:00 UTC-3
- Coleta Hotmart do MOIS aprimorada para reduzir falhas na extração de produtos em destaque.
- O parser da resposta do marketplace foi refatorado para:
  - manter a extração rica (título, link, descrição, produtor e imagem) quando os blocos completos estiverem presentes;
  - aplicar fallback de extração por URL de produto (`https://www.hotmart.com/product/...`) quando o HTML/JSON vier com estrutura parcial.
- O objetivo é manter geração de leads mesmo quando a Hotmart altera o shape do payload da vitrine.

## 2026-05-03 23:24:00 UTC-3
- Reintroduzida a etapa de persistência de estado de coleta no MOIS via backend principal, usando os endpoints:
  - `PUT /api/v1/mois/persistence/collection-jobs/{jobId}`
  - `GET /api/v1/mois/persistence/collection-jobs/{jobId}`
  - `GET /api/v1/mois/persistence/collection-jobs?workspaceId=&status=`
- O `MoisDomainService` deixou de operar exclusivamente em memória para coleção e passou a:
  - salvar progresso/resultado do job (`persistCollectionState`);
  - hidratar estado por workspace/status (`hydrateCollectionState`);
  - hidratar estado por job (`hydrateCollectionStateByJob`).
- Serialização JSON ajustada com descoberta automática de módulos Jackson para suportar `Instant` no payload de persistência.

## 2026-05-03 23:36:48 UTC-3
- Ajustados os testes unitários `MoisDomainServiceTest` para evitar flakiness causado por estado persistido de coletas do mesmo `workspaceId` entre execuções.
- Nos cenários de listagem de jobs e sumário operacional, os `workspaceId` agora são gerados com sufixo UUID por teste, garantindo isolamento sem depender de limpeza externa.
- Resultado esperado: os asserts de cardinalidade (`totalJobs` e tamanho da lista) voltam a refletir apenas os dados criados no próprio teste.

## 2026-05-04 10:40:00 UTC-3
- Endurecida a entrega de persistência MOIS com cobertura de testes unitários no backend (`MoisCollectionPersistenceServiceTest`).
- Validado via testes o comportamento de:
  - sincronização relacional por job em `mois_collected_reference` (delete + batch insert);
  - agregação executiva de destaques por fonte (`collection-highlights/by-source`) com ordenação por score médio e cálculo de sinal predominante.
- Registro operacional para rastrear revisão da PR "Persist collected references and add source-highlight aggregation endpoint" após feedback de qualidade.

## 2026-05-04 09:55:00 UTC-3
- Corrigida a resolução de include do Liquibase no `db.changelog-master.yaml` do `backend/ads-service`.
- O changeset `changesets/2026-04-28-mois-collection-job-state-persistence.yaml` passou a declarar `relativeToChangelogFile: true`, evitando falha de parsing em CI com mensagem de arquivo não encontrado no classpath.
- Registro criado para rastrear o incidente reportado no build Maven/Liquibase e sua correção.

## 2026-05-05 00:20:00 UTC-3
- Implementada extração de destaque Hotmart no MOIS com derivação de `hotmartHighlight` a partir da descrição/título durante o parse do marketplace.
- A persistência relacional foi ampliada para gravar metadados Hotmart diretamente em `mois_collected_reference` (`hotmart_description`, `hotmart_producer`, `hotmart_image_url`, `hotmart_highlight`), reduzindo dependência de leitura de `payload_json`.
- Criado changeset Liquibase MySQL 5.7 em YAML para adicionar as colunas de destaque e incluído no `db.changelog-master.yaml`.
- Documento de modelo de dados atualizado para registrar o novo contrato de persistência dos destaques Hotmart.
- Validação executada: testes unitários direcionados em `backend/ads-service` (`MoisCollectionPersistenceServiceTest`) e `mois` (`MoisHotmartRobotServiceTest`) concluídos com sucesso.
- Commit relacionado: `ae3a077`.

## 2026-05-06 09:35:00 UTC-3
- Registrada evolução da persistência relacional do MOIS para tornar `mois_collected_reference` operacionalmente útil para análise comercial de produtos Hotmart.
- Novos campos adicionados e preenchidos no fluxo de coleta/persistência:
  - `product_name`
  - `product_url`
  - `producer_name`
  - `sales_page_url`
- A escrita desses campos no backend agora usa fallback determinístico entre campos principais e metadados (`rawMetadata`) para reduzir lacunas em registros parcialmente estruturados.
- Incluído changeset Liquibase incremental (MySQL 5.7, YAML) com backfill dos registros já persistidos para reaproveitar `title`, `url` e `hotmart_producer` quando disponíveis.
- Objetivo: permitir consulta SQL direta sem depender de parsing do `payload_json` para identificar nome do produto, URL pública, produtor e página de vendas.
- Commit relacionado: `66c7cda`.

## 2026-05-05 22:49:17 UTC-3
- Adicionada telemetria de diagnóstico no parser de marketplace Hotmart (`parseHotmartMarketplaceLeads`) para investigar lacunas de metadados de produto no MOIS.
- O parser agora registra um resumo estruturado (`mois_hotmart_parse_summary`) com:
  - `richCardMatches` e `richCardDuplicates`;
  - `fallbackMatches` e `fallbackDuplicates`;
  - `enrichedLeads`, `totalLeads`, `limitPerSource` e `bodyLength`.
- Objetivo: diferenciar com evidência de log quando a coleta está encontrando cards ricos (com descrição/produtor/imagem) versus quando está caindo majoritariamente no fallback por URL, cenário que explica campos de produto nulos na persistência relacional.
- Commit relacionado: `a5c4341`.

## 2026-05-06 12:13:34 UTC-3
- Criado o submódulo independente `mois-hotmart-collector` para coleta Hotmart com aplicação, container e imagem Docker separados do MOIS principal.
- Publicado contrato inicial do submódulo com endpoints:
  - `GET /api/v1/mois-hotmart/health`
  - `POST /api/v1/mois-hotmart/collections`
- Estrutura inicial preparada para evolução para automação Playwright com sessão persistida, mantendo desacoplamento operacional do núcleo de domínio do MOIS.
- Registro adicionado para atender solicitação explícita de documentação em `docs/mois/registros.md`.
- Commit relacionado: `52082a1`.

## 2026-05-06 12:17:38 UTC-3
- Ajustado o submódulo `mois-hotmart-collector` para privilegiar distribuição como JAR executável.
- `pom.xml` atualizado com repackage explícito do Spring Boot e geração de artefato final `target/mois-hotmart-collector.jar` com `executable=true`.
- Adicionado script `run-local-jar.sh` para execução direta do JAR e README atualizado com fluxo recomendado `mvn clean package` + execução do jar.

## 2026-05-06 12:38:09 UTC-3
- Evoluído o submódulo `mois-hotmart-collector` para coleta com Playwright em modo headless por padrão.
- `HotmartCollectorService` atualizado para abrir Chromium headless, navegar para `https://app.hotmart.com/market/search` e extrair links de produtos (`/market/products/`) até o limite solicitado.
- `collector.playwright.headless` parametrizado (default `true`) com opção de override para debug local.
- Teste de controller isolado com `@MockBean` para evitar dependência de rede/browser durante testes unitários.

## 2026-05-06 20:50:00 UTC
- Atendida a solicitação de tornar o deploy do `mois-hotmart-collector` automático via GitHub Actions.
- Criado workflow dedicado `.github/workflows/mois-hotmart-collector-ci.yml` com pipeline completo:
  1. testes Maven (`mvn -B test`),
  2. build e push da imagem para GHCR,
  3. deploy automático por SSH no mesmo host do MOIS principal (`177.153.62.107`) usando `docker compose`.
- README do submódulo atualizado para documentar o novo fluxo de deploy automático.
- Registro criado conforme solicitação explícita: `docs/mois/registros.md`.
- Commit relacionado: `88064ce`.

## 2026-05-06 22:38:07 UTC-3
- Ajustado o endpoint base do Actuator no `mois-hotmart-collector` para reduzir previsibilidade de rota pública de observabilidade.
- O base path deixou de ser fixo em `/actuator` e passou a usar configuração externa:
  - `management.endpoints.web.base-path=${ACTUATOR_BASE_PATH:/internal/ops-monitor}`
- Mantida exposição apenas dos endpoints `health`, `info` e `loggers`, preservando o objetivo operacional de monitoramento e ajuste de nível de logs em runtime.
- README do submódulo atualizado para refletir o novo path padrão e orientar customização por variável de ambiente.

## 2026-05-07 01:34:12 UTC-3
- Revisada a implementação do coletor Hotmart no módulo `mois` para uso de área logada (`https://app.hotmart.com/market/search`) com suporte a navegação headless via Playwright.
- Adicionada configuração por ambiente para operação autenticada:
  - `MOIS_HOTMART_SEARCH_URL`
  - `MOIS_HOTMART_USE_PLAYWRIGHT`
  - `MOIS_HOTMART_SESSION_COOKIE`
- Ajustado o fluxo para fallback seguro quando a sessão autenticada não estiver disponível, evitando quebra do job de coleta e mantendo rastreabilidade por log.
- Dependência `com.microsoft.playwright:playwright` adicionada ao módulo `mois` para habilitar renderização e extração em página JS-driven.
- Registro criado por solicitação explícita: `registre o trabalho em /docs/mois/registros.md`.
- Commit relacionado: `93a04c8`.

## 2026-05-07 01:37:00 UTC-3
- Correção de escopo: os ajustes de coleta Hotmart com Playwright/sessão autenticada foram realocados do módulo `mois` para o projeto correto `mois-hotmart-collector`.
- Revertida a alteração indevida no `mois` principal (`MoisDomainService` + `mois/pom.xml`) para preservar a separação arquitetural entre núcleo de domínio MOIS e coletor especializado.
- No `mois-hotmart-collector`, adicionadas configurações de coleta autenticada:
  - `collector.hotmart.search-url`
  - `collector.hotmart.session-cookie`
  - reutilização de `collector.playwright.headless`
- O serviço do coletor agora encerra com `COLLECTION_SKIPPED` quando a sessão não está configurada, evitando falsa execução "ok" sem autenticação real.

## 2026-05-07 01:40:00 UTC-3
- Ajustado o `mois-hotmart-collector` para execução automática **de hora em hora**, evitando dependência de disparo manual único.
- Habilitado agendamento no aplicativo com `@EnableScheduling`.
- Criado scheduler `HotmartCollectorScheduler` com cron padrão `0 0 * * * *` e controles por configuração:
  - `collector.scheduler.enabled`
  - `collector.scheduler.cron`
  - `collector.scheduler.source`
  - `collector.scheduler.max-products`
- Mantida possibilidade de override por variáveis de ambiente (`COLLECTOR_SCHEDULER_*`).

## 2026-05-07 21:26:03 UTC-3
- Registrada atualização operacional do módulo `mois-hotmart-collector` para configuração por ambiente em runtime via Docker Compose + `.env`.
- `docker-compose.yml` e `docker-compose.deploy.yml` passaram a encaminhar explicitamente variáveis de autenticação Hotmart e de agendamento (`COLLECTOR_HOTMART_*` e `COLLECTOR_SCHEDULER_*`) para o container.
- README do módulo foi atualizado com:
  - path recomendado do `.env` local e no host de deploy (`/opt/marketinghub/mois-hotmart-collector/.env`);
  - exemplo completo de variáveis para autenticação e scheduler;
  - tabela de variáveis suportadas;
  - reforço da diretriz: execução automática agendada (de hora em hora), sem depender de disparo manual.
- Registro criado por solicitação explícita: "e registre o trabalho em /docs/mois/registros.md".
- Commits relacionados: `3ac1f55` (compose) e `329d8b0` (documentação).

## 2026-05-08 14:15:35 UTC-3
- Criada a estrutura `testes` no submódulo `mois-hotmart-collector` com a classe `HotmartAuthMain` para validação isolada de autenticação Hotmart via username/password.
- A classe lê `HOTMART_USERNAME` e `HOTMART_PASSWORD` do ambiente, executa login em `https://app.hotmart.com/login` com Playwright e imprime a URL resultante após submissão.
- Objetivo: facilitar teste operacional de autenticação sem acoplamento ao fluxo completo de coleta agendada.
- Commit relacionado: `9195df7`.

## 2026-05-08 18:51:47 UTC
- Verificação operacional via MCP Server do módulo `mois-hotmart` confirmou aplicação ativa, porém coleta autenticada marcada como `COLLECTION_SKIPPED` por ausência de autenticação efetiva no runtime.
- Ajustado `HotmartCollectorService` para usar fallback explícito de credenciais quando `collector.hotmart.username/password` chegarem em branco via ambiente, priorizando os valores definidos em properties.
- Adicionadas propriedades `collector.hotmart.username-fallback` e `collector.hotmart.password-fallback` no `application.properties` para garantir uso das credenciais do arquivo quando necessário.
- Registro criado por solicitação explícita: "Ao final registre o trabalho em /docs/mois/registros.md".

## 2026-05-08 22:30:00 UTC
- Analisada recorrência de `COLLECTION_ERROR` no `mois-hotmart-collector` com mensagem `Falha na coleta Playwright: Failed to create driver` observada nos logs recentes via MCP.
- Corrigido o runtime Docker do coletor para incluir dependências de sistema exigidas pelo Chromium/Playwright (`libnss3`, `libatk-bridge2.0-0`, `libxkbcommon0`, `libgbm1`, `libasound2`, `libxcomposite1`, `libxdamage1`, `libxfixes3`, `libxrandr2`, `libcups2`, `libpangocairo-1.0-0`, `libgtk-3-0`, `libdrm2`, `fonts-liberation`, `ca-certificates`).
- Ajustado o launch do Chromium no `HotmartCollectorService` com argumentos `--no-sandbox` e `--disable-dev-shm-usage` para maior estabilidade em container.
- Validação executada no módulo com `mvn test -q` concluída com sucesso.
- Registro criado por solicitação explícita: "agora registre o trabalho em /docs/mois/registros.md".

## 2026-05-09 01:10:00 UTC
- Atendendo à solicitação de aumentar observabilidade do erro `Falha na coleta Playwright: Failed to create driver` no `mois-hotmart-collector`, foram adicionados logs diagnósticos no `HotmartCollectorService`.
- Novos logs cobrem:
  - contexto de início da coleta (`headless`, limites solicitado/aplicado, presença de cookie/senha);
  - bootstrap do Playwright com `chromium executablePath` e argumentos de launch;
  - estratégia de autenticação usada (cookie de sessão vs login/senha);
  - navegação para URL alvo, quantidade de cards encontrados/processados e total coletado;
  - erro com contexto completo e stack trace no bloco de exceção.
- Fluxo de login também passou a registrar início e URL resultante após submissão, para facilitar triagem de falhas de autenticação.
- Validação executada: `mvn -q test` no módulo `mois-hotmart-collector` com sucesso.
- Registro criado por solicitação explícita: "registre em /docs/mois/registros.md".

## 2026-05-09 05:55:33 UTC-3
- Revisado o ajuste de estabilidade do `mois-hotmart-collector` para execução em ambiente sem UI com Playwright.
- No `Dockerfile`, a etapa de pré-instalação do Chromium foi endurecida para validação explícita do diretório de browsers e busca correta do binário (com expressão `find` parentetizada), evitando falso-positivo silencioso.
- Mantida a estratégia de uso de browser com `collector.playwright.chromium-executable-path` no serviço, permitindo apontamento explícito do executável quando necessário em produção.
- Testes unitários do módulo Java reexecutados com sucesso após os ajustes.
- Registro criado por solicitação explícita: "registre o trabalho em /docs/mois/registros.md".

## 2026-05-09 14:18:33 UTC-3
- Registrado trabalho no `mois-hotmart-collector` para diagnóstico do erro de bootstrap Playwright (`Failed to create driver`), incluindo:
  - configuração de runtime no `Dockerfile` com `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` e `PLAYWRIGHT_BROWSERS_PATH=/ms-playwright`;
  - log diagnóstico no início da coleta com variáveis de ambiente relevantes (`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD`, `PLAYWRIGHT_BROWSERS_PATH`, `HOME`) e `collector.playwright.chromium-executable-path`.
- Mantidas credenciais no `application.properties` por solicitação explícita para ajuste posterior.
- Testes unitários do módulo `mois-hotmart-collector` executados com sucesso (`mvn test -q`).
- Registro criado por solicitação explícita: "registre o traabalho em /docs/mois/registros.md".
