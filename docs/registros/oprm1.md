## 2026-06-28 — OPRM NichoCNAE v3: input do request OpenAI separado na auditoria

- Separado o campo `input` do request bruto da OpenAI em `pipeline_nichocnae.request_input`, mantendo o request completo para auditoria e oferecendo leitura direta do prompt enviado.
- A tela do pipeline v3 agora exibe o input extraído em bloco próprio, alinhado ao bloco de texto extraído do response.
- Prevenção de recorrência: teste unitário cobre extração de `input` textual, `input` estruturado e ausência de campo no request.

## 2026-06-27 — OPRM NichoCNAE v3: etapa 1 qualifica o CNAE antes da geração de personas

- Corrigida a causa-raiz da entrada da etapa `persona-candidate-generator`: a etapa `cnae-intake` agora nasce com `cnaeCode` e `cnaeDescription` vindos da dimensão canônica de CNAE e devolve esses campos na própria saída funcional.
- Prevenção de recorrência: testes unitários garantem que a etapa 1 bloqueia avanço sem `cnaeDescription` e que o backend cria a pendência inicial já qualificada com o nome completo do CNAE.

## 2026-06-23 — OPRM NichoCNAE v2: download de relatório do job concluído

- Ajustada a tela do pipeline por CNAE para permitir baixar, em cada job concluído, um relatório Markdown com etapas executadas, entradas, saídas, processamento, falhas e próximos passos persistidos pelo backend.
- Causa-raiz tratada: o histórico mostrava apenas resumo e ações de visualização/materialização, sem uma forma direta de levar o relatório completo para análise operacional ou compartilhamento.

## 2026-06-22 — OPRM NichoCNAE v2: cancelamento manual de job

- Corrigida a causa-raiz do erro ao cancelar job v2: em produção a coluna `oprm_nichocnae_v2_stage_execution.status` ainda estava como `ENUM` sem o valor `CANCELED`, enquanto o backend já usa status flexível por enum Java.
- Criado changelog incremental para converter a coluna `status` para `VARCHAR(40)`, permitindo o cancelamento manual e futuras evoluções de status sem truncamento pelo MySQL.

## 2026-06-21 — OPRM NichoCNAE v2: bootstrap do backend corrigido

- Causa-raiz tratada: services de etapas OPRM NichoCNAE v2 tinham múltiplos construtores e o Spring podia tentar instanciá-los pelo construtor vazio inexistente durante testes `@SpringBootTest`, derrubando o contexto completo do backend.
- Correção aplicada: marcado explicitamente o construtor canônico de injeção do Spring, preservando o construtor auxiliar usado por teste unitário e evitando falha em testes de outros módulos.
- Prevenção de recorrência: a regra do backend sobre múltiplos construtores em beans Spring foi aplicada às etapas OPRM v2 afetadas.

## 2026-06-21 — OPRM OpenAI em modo Flex com GPT-5.2

- Decisão operacional aplicada: acessos OpenAI das etapas OPRM NichoCNAE com IA devem usar modo Flex (`service_tier=flex`) e modelo `gpt-5.2`.
- Correção de causa-raiz: o fallback local do coletor ainda permitia modelos antigos e a segmentação MEI não enviava `service_tier` no corpo da Responses API; agora seed e segmentação MEI têm fallback `gpt-5.2` e payload Flex auditável.
- Prevenção de recorrência: changelog incremental reconfigura as etapas OpenAI do pipeline OPRM para `gpt-5.2` quando o modelo existir no catálogo, mantendo a tela administrativa e o executor alinhados.

## 2026-06-20 11:45:00 (UTC-3) — OPRM NichoCNAE v2: leitura operacional dos jobs simplificada

- Ajustada a tela do pipeline NichoCNAE v2 para trocar a tabela técnica de jobs por cards de acompanhamento com etapa, situação em linguagem humana, resumo do motivo e próximo passo operacional.
- Causa-raiz tratada: a tela exibia identificadores longos e stack trace bruto como decisão principal, impedindo o usuário de entender rapidamente onde o job parou e o que precisa acontecer para avançar.

## 2026-06-19 00:00:00 (UTC) — OPRM NichoCNAE v2: separação visual entre jobs abertos e concluídos

- Ajustada a tela do pipeline NichoCNAE v2 para separar visualmente os blocos de jobs abertos e concluídos com cards dedicados, trilha lateral de status e divisor elegante entre os painéis.
- Causa-raiz tratada: os dois blocos usavam cards genéricos muito próximos, reduzindo a leitura rápida entre operação em andamento e histórico encerrado.

## 2026-06-19 00:00:00 (UTC) — OPRM NichoCNAE v2: lista de jobs por CNAE na tela

- Ajustada a tela do pipeline NichoCNAE v2 para exibir, por CNAE, jobs abertos com etapa atual e jobs encerrados em histórico separado.
- O backend passou a expor contrato de leitura agrupado por `jobId`, mantendo a UI como apresentação da verdade persistida nas execuções de etapa v2.

## 2026-06-18 21:15:00 (UTC-3) — OPRM NichoCNAE: download de relatório sem abrir nova aba

- Causa-raiz tratada: a tela de jobs recentes usava link com `target="_blank"` para baixar relatório, abrindo outra aba do navegador e expondo erro visual quando o browser tentava navegar diretamente para o endpoint de arquivo.
- Correção aplicada: o botão de relatório agora executa o download via `fetch` no próprio frontend, cria um blob local e dispara o arquivo Markdown sem sair da tela de acompanhamento.
- Prevenção de recorrência: adicionado teste de tela cobrindo que o botão baixa o relatório do job por blob e mantém o fluxo dentro da mesma aba.

## 2026-06-18 20:20:00 (UTC-3) — OPRM NichoCNAE: logs operacionais na etapa de busca pública

- Diagnóstico do ciclo #74: o banco respondeu normalmente via MCP e o ciclo continuava avançando na etapa `source-searcher`, sem evidência de lentidão do MySQL; a lacuna real era baixa observabilidade para saber se o coletor estava parado antes de buscar, durante a chamada ao provedor ou ao concluir no backend.
- Correção aplicada: adicionados logs no `oprm-coletor-mei` para registrar carregamento de pendências, início/fim do lote, início/fim de cada query, chamada ao provedor público, duração, contadores de risco e confirmação de conclusão no backend.
- Prevenção de recorrência: a próxima execução da etapa três passa a indicar exatamente em qual ponto operacional o job parou, diferenciando fila vazia, provedor lento, falha de complete no backend ou processamento normal.

## 2026-06-18 — OPRM NichoCNAE: custo preservado em reprocessamento do mesmo job

- Causa-raiz tratada: ao reabrir o mesmo `researchCycleId`, o backend limpava o seed anterior para permitir nova execução, mas junto apagava o custo de IA já consumido e a tela podia voltar o custo para zero durante o reprocessamento.
- Correção aplicada: antes de limpar artefatos reexecutáveis, o backend acumula o custo já registrado no ciclo e as consultas de acompanhamento/materialização somam esse custo preservado com o novo custo da tentativa atual.
- Prevenção de recorrência: teste unitário cobre que o reprocessamento preserva e acumula o custo anterior do mesmo job antes de deletar o seed.

## 2026-06-18 02:23:30 (UTC) — OPRM NichoCNAE: endpoint de jobs por status para reprocessamento pelo executor

- Causa-raiz identificada no ciclo #68: o backend marcava `NEEDS_MORE_RESEARCH` durante a triagem pré-segmentação MEI/autônomo, antes de o job chegar ao gate de qualidade que já aciona o reprocessamento automático no executor.
- Correção aplicada: criado endpoint interno `GET /api/internal/oprm/nichocnae/routine-research-orchestrator/jobs?status=...&limit=...` para o executor OPRM buscar jobs por status recuperável, obter os dados da tentativa anterior e decidir nova tentativa com aprendizado nos prompts, mantendo o backend como leitura/escrita e o controle operacional no executor.
- Prevenção de recorrência: adicionado teste unitário garantindo normalização de status, limite seguro e consulta filtrada por status no backend.

## 2026-06-17 00:00:00 (UTC) — OPRM NichoCNAE: evitar repetição de subnichos por CNAE

- Causa-raiz tratada: a etapa `oprmNicheResearchSeedBuilder` já quebrava o CNAE em subnichos vendáveis, mas não recebia explicitamente os subnichos já materializados para o mesmo CNAE, permitindo repetir ou aproximar semanticamente um recorte já existente.
- Correção aplicada: o backend passou a enviar no pending da etapa 2 a lista de subnichos já materializados para o CNAE; o coletor OPRM passou a inserir essa lista no prompt como recortes proibidos e a orientar a IA, como especialista em Marketing e Mercado, a escolher um novo subnicho com distância mercadológica, dor urgente, capacidade de pagar e resultado observável.
- Prevenção: atualizados Swagger, cânone OPRM e testes de regressão para garantir que a etapa de seed use subnichos existentes como restrição de portfólio, evitando canibalização e repetição operacional.

## 2026-06-17 17:06:25 (UTC) — OPRM NichoCNAE: indicador animado de execução

- Ajustada a tela de detalhe do CNAE/subnicho para exibir uma ilustração animada quando o pipeline NichoCNAE estiver em execução, reforçando visualmente que o backend continua processando as etapas automaticamente.

## 2026-06-14 00:00:00 (UTC) — OPRM NichoCNAE: múltiplos nichos por CNAE

- Alterado o conceito de materialização para permitir mais de um `market_niche` por CNAE quando ciclos aprovados representarem subnichos diferentes.
- A materialização final agora só atualiza nicho existente quando houver perfil anterior com o mesmo `cnae_code` e o mesmo `neutral_niche_name` normalizado; vínculo antigo do candidato/perfil não bloqueia a criação de novo nicho para subnicho diferente.

## 2026-06-14 00:00:00 (UTC) — OPRM NichoCNAE: rotina executora antes de genérico

- Implementado o status `NEEDS_EXECUTOR_ROUTINE_EVIDENCE` para diferenciar nicho comercialmente promissor com lacuna de evidência da rotina manual executada, evitando reprovação genérica prematura.
- O gate agora direciona o próximo movimento `BUSCAR_TAREFAS_REAIS_EXECUTOR`, com reprocessamento automático recuperável no backend e reprocessamento manual pela UI.
- A etapa de seed passou a orientar pesquisas focadas em tarefas reais, materiais, tempo, deslocamento, higiene, retrabalho e relatos brasileiros, reduzindo a dominância de agenda/captação/cobrança nessa rodada.
- A busca e extração de sinais receberam reforços para relatos de execução, retrabalho/reclamação, materiais/insumos e higiene/biossegurança.

## 2026-06-14 19:42:42 (UTC-3) — OPRM NichoCNAE: plano para validação de rotina executora

- Criado plano de implementação em `docs/implementacao/oprm/plano-melhoria-nichocnae-validacao-rotina-executora.md` para corrigir a causa-raiz observada nos ciclos #49 a #52 do CNAE 9602501: o pipeline validou dor comercial e fit MEI/autônomo, mas reprovou por não comprovar tarefas concretas da executora.
- Proposta principal: introduzir status/fluxo `NEEDS_EXECUTOR_ROUTINE_EVIDENCE` e próximo movimento `BUSCAR_TAREFAS_REAIS_EXECUTOR`, separando validação de dor vendável da validação de rotina manual real antes de reprovar como `GENERIC`.

## 2026-06-13 — OPRM NichoCNAE: registro de efeito perceptivo das cores antigas

- Registrado ponto de pesquisa futura sobre as cores antigas dos cards do pipeline: `bg-success-subtle` (`#d1e7dd`) para concluído e `bg-primary-subtle` (`#cfe2ff`) para em execução criavam um efeito visual interessante; de perto pareciam muito semelhantes, mas de longe a diferença entre verde claro e azul claro ficava mais perceptível.
- Hipótese para investigar depois: esse comportamento pode estar ligado a contraste cromático de baixa saturação, distância de observação, mistura óptica e percepção periférica, podendo ser útil para interfaces que precisam indicar estados sem gerar poluição visual.

## 2026-06-13 — OPRM NichoCNAE: ícones para etapas com pesquisa na internet

- Adicionados ícones/selo `Web` nas etapas `Busca` e `Coleta` da tela `/oprm/cnaes/:cnaeCode`, deixando claro quais fases acessam a internet para pesquisar e coletar fontes públicas.
- Causa-raiz tratada: a tela diferenciava uso de IA, mas não indicava visualmente as etapas que dependem de pesquisa externa na internet, reduzindo clareza operacional durante acompanhamento do pipeline.

## 2026-06-13 — OPRM NichoCNAE: contraste visual dos cards de execução

- Ajustada a tela `/oprm/cnaes/:cnaeCode` para diferenciar claramente cards concluídos e em execução: concluído passou a usar fundo verde sólido com texto claro, enquanto etapa em execução permanece em azul claro com borda reforçada.
- Causa-raiz tratada: os dois estados usavam fundos leves muito próximos, reduzindo a leitura operacional rápida do andamento do pipeline.

## 2026-06-12 — OPRM CNAE: detalhe acionável com pipeline NichoCNAE

- Evoluída a tela `/oprm/cnaes/:cnaeCode` para exibir descrição do CNAE, score OPRM, componentes do score, quantidade de estabelecimentos, empresas e MEIs.
- Adicionado comando manual para disparar o pipeline NichoCNAE do CNAE selecionado quando houver candidato pendente, sem obrigar o usuário a depender apenas da fila automática por score.
- Criado acompanhamento por cards das fases do pipeline na tela de detalhe, usando o último ciclo do CNAE para indicar início, status, sinais extraídos e conclusão/falha.
- Causa-raiz tratada: a tela existia apenas como placeholder, deixando o usuário sem dados de decisão e sem comando direto para transformar CNAE priorizado em pesquisa operacional.

# Registros — OPRM

- 2026-06-11 21:26:05 (UTC-3): atualizado o fluxo OPRM NichoCNAE para tratar aquisição de clientes como realidade operacional obrigatória do MEI/autônomo, reforçando no prompt e no cânone a busca por evidências de captação, canais, indicação, redes sociais, WhatsApp, orçamento, agenda vazia, retorno, fidelização, cancelamento, reativação e recorrência, sem permitir solução, campanha ou oferta.

  > Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
  > Este documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

- 2026-05-11 14:25:00 (UTC-3): implementação inicial do plano de importação de CNAEs no backend: criado catálogo `oprm_niche_catalog` via Liquibase, endpoint `POST /api/niches/catalog:ingest` com validações de payload e regra de idempotência por `cnaeCode` normalizado, mantendo backend como único ponto de persistência. Resultado esperado: habilitar carga inicial de todos os CNAEs para suportar mapeamento ocupação↔CNAE e ranking de nichos.
- 2026-05-11 15:05:00 (UTC-3): ajuste de arquitetura solicitado: removida regra de negócio de normalização/deduplicação/upsert do backend na ingestão de catálogo CNAE. O backend passou a atuar apenas como camada de persistência do lote recebido (saveAll), mantendo validações de contrato no DTO e constraints de banco. A lógica de negócio deve permanecer no módulo OPRM/coletor.
- 2026-05-11 15:35:00 (UTC-3): implementação do coletor OPRM para ingestão de catálogo CNAE. Criado endpoint `POST /api/oprm-mei/catalog/collect` no módulo `oprm-coletor-mei`, com lógica de negócio de normalização de `cnaeCode`, deduplicação e envio em lotes para `POST /api/niches/catalog:ingest` no backend. Mantida diretriz: negócio no coletor/OPRM e backend apenas persistência.

- 2026-05-11: Adicionado agendamento de ingestão no OPRM coletor (cron 15:10 America/Sao_Paulo) com payload externo configurável.

- 2026-05-11 15:20:00 (UTC-3): diagnóstico de falha no `docker compose pull` do `oprm-coletor-mei` em CI: removido campo legado `version` do `docker-compose.yml` para eliminar warning de obsolescência e reduzir ruído operacional; identificado que erro final permanece de infraestrutura do runtime containerd (`failed commit on ref ... no such file or directory`), fora do escopo do compose do projeto.
- 2026-05-11 20:43:53 (UTC): verificação operacional da importação de CNAEs da Receita Federal pelo `oprm-coletor-mei` via MCP (`db_query`) na tabela `oprm_niche_catalog`: resultado atual `total=0` registros e `active_total=null`, indicando que a carga ainda não populou o catálogo no backend (ou não foi executada no ambiente consultado). Próxima ação recomendada: executar/reexecutar a ingestão do coletor e validar novamente contagem por `source` e faixa de `created_at`.
- 2026-05-11 17:47:43 (UTC-3): esclarecimento do diagnóstico solicitado (“não executou ou executou com falha?”): com as evidências disponíveis no momento, o status é **inconclusivo** sobre execução do job (não há confirmação de run bem-sucedido no ambiente consultado e houve timeout intermitente no MCP para coleta de logs). Fato objetivo confirmado via banco: catálogo `oprm_niche_catalog` permanece sem dados (`total=0`). Interpretação operacional: ou a importação não foi executada neste ambiente, ou executou com falha antes de persistir registros.
- 2026-05-11 17:53:06 (UTC-3): adicionada observabilidade da execução no módulo \: novo endpoint de URL \ para consultar histórico recente (manual/agendado) com status e contadores da ingestão, além de registro explícito de erros de agendamento/payload para diagnóstico operacional.
- 2026-05-11 17:53:45 (UTC-3): correção do registro anterior: adicionada observabilidade da execução no módulo `oprm-coletor-mei` com endpoint de URL `GET /api/oprm-mei/catalog/executions` para consulta do histórico recente (manual/agendado), incluindo `status` e contadores (`received`, `normalized`, `persisted`) da ingestão, com registro explícito de erros de agendamento/payload para diagnóstico operacional.
- 2026-05-12 09:00:00 (UTC-3): reprogramado o agendamento padrão da busca/ingestão de CNAEs da Receita Federal no módulo `oprm-coletor-mei` para executar ao meio-dia (12:00) no fuso `America/Sao_Paulo` (`cron` padrão `0 0 12 * * *`), mantendo possibilidade de override por variável `OPRM_COLLECTOR_SCHEDULE_CRON`.

- 2026-05-12 13:40:00 (UTC-3): habilitado endpoint Actuator de arquivo de log no módulo `oprm-coletor-mei` com exposição `logfile` e configuração de `logging.file.name` (default `/tmp/oprm-coletor-mei.log`) para permitir consulta operacional via URL `/actuator/logfile`.

- 2026-05-12 19:30:00 (UTC-3): reprogramado novamente o agendamento padrão da ingestão de CNAEs da Receita Federal no módulo `oprm-coletor-mei` para executar às 19:30 no fuso `America/Sao_Paulo` (`cron` padrão `0 30 19 * * *`), mantendo override por `OPRM_COLLECTOR_SCHEDULE_CRON`.

- 2026-05-12 22:15:00 (UTC-3): adicionados logs explícitos nas primeiras linhas do método agendado `runScheduledIngestion` no `oprm-coletor-mei` (incluindo `enabled`, `cron`, `timezone`, `source` e `payloadFile`) para diagnosticar ausência de execução; também reprogramado o agendamento padrão para 23:00 no fuso `America/Sao_Paulo` (`cron` padrão `0 0 23 * * *`) e habilitado por padrão (`enabled=true`), mantendo overrides por variáveis de ambiente.
- 2026-05-13 02:59:11 (UTC): ajuste operacional para eliminar execução agendada sem insumo no `oprm-coletor-mei`: no `docker-compose.deploy.yml`, a variável `OPRM_COLLECTOR_SCHEDULE_PAYLOAD_FILE` passou a ser obrigatória (fail-fast em ausência), e no `docker-compose.yml` local foi definido default para `/app/config/receita-cnaes.json` para facilitar validação local com volume/config montado.

- 2026-05-12 22:35:00 (UTC-3): ajuste solicitado: agendamento fixado diretamente no método `runScheduledIngestion` com `@Scheduled(cron = "0 0 23 * * *", zone = "America/Sao_Paulo")` para eliminar dependência de configuração externa do horário de execução.
- 2026-05-13 03:05:00 (UTC): agendamento da ingestão CNAE (Receita Federal) alterado para 01:00 no fuso `America/Sao_Paulo` (Brasília), com atualização do cron hardcoded no `@Scheduled` e do cron padrão configurável em `application.yml` para `0 0 1 * * *`.

- 2026-05-13: Criado documento `docs/novos-modulos/OPRM/cnpj-open-data-2026-04-12.md` com descrição de cada ZIP da base CNPJ (snapshot 2026-04-12), URLs diretas e relevância para o OPRM/Marketing Hub.
- 2026-05-13 16:10:00 (UTC-3): complemento do registro do dia 2026-05-13: documento `docs/novos-modulos/OPRM/cnpj-open-data-2026-04-12.md` criado com inventário dos ZIPs, URLs diretas e importância por dataset para uso no OPRM.
- 2026-05-13 16:30:00 (UTC-3): refinado documento `docs/novos-modulos/OPRM/cnpj-open-data-2026-04-12.md` para foco inicial exclusivamente quantitativo de tamanho de mercado, removendo priorização geográfica e adicionando métricas recomendadas sem recorte territorial.
- 2026-05-13 16:50:00 (UTC-3): criado `docs/novos-modulos/OPRM/oprm-plano-importacao-cnpj-market-size.md` com desenho de tabelas (staging, dimensões, fatos e agregado), fluxo de importação por etapas, endpoints backend necessários e estratégia de rollout para implementação da carga CNPJ com foco em tamanho de mercado.
- 2026-05-13 17:35:00 (UTC-3): execução das etapas C e D do plano `docs/novos-modulos/OPRM/oprm-plano-importacao-cnpj-market-size.md` no backend OPRM: adicionada persistência do agregado `oprm_market_size_by_cnae` (entidade/repositório + migration Liquibase), suporte de upsert de `marketSizes` no evento de arquivo, endpoint `POST /api/oprm/market/import-runs/{runId}/complete` para finalização da execução, e reforço das validações de vínculo `runId/fileId` e status final automático (`COMPLETED`/`PARTIAL`) conforme resultado dos arquivos.
- 2026-05-14 20:15:00 (UTC-3): agendamento do `oprm-coletor-mei` ajustado para execução às 23:00 no fuso `America/Sao_Paulo` com cron padrão `0 0 23 * * *` em `application.yml`, e o `@Scheduled` passou a usar propriedades (`oprm.collector.schedule.cron`/`timezone`) para manter o horário configurável sem novo deploy; mantidos logs operacionais e fluxo de chamada dos endpoints do backend para rastreabilidade completa.

- 2026-05-14 17:00:00 (UTC-3): ajuste solicitado de agendamento no Spring Boot do módulo `oprm-coletor-mei` para execução diária às 16:45 (fuso `America/Sao_Paulo`), atualizando o cron padrão configurável em `application.yml` para `0 45 16 * * *` e sincronizando a documentação de operação no `README.md`.
- 2026-05-15 00:10:00 (UTC-3): documentação do OPRM corrigida para remover o fluxo de ingestão por JSON (`receita-cnaes.json`) como pipeline principal e consolidar oficialmente o pipeline real de produção baseado em ZIPs da base CNPJ (Cnaes.zip, Empresas*.zip, Estabelecimentos*.zip, Simples.zip, Socios\*.zip), com persistência em `oprm_cnpj_import_run`, `oprm_cnpj_import_file`, `oprm_cnpj_cnae_dim` e `oprm_market_size_by_cnae`.
- 2026-05-15 00:25:00 (UTC-3): desativado e removido do código do `oprm-coletor-mei` o fluxo legado de ingestão de catálogo CNAE por JSON (`/api/oprm-mei/catalog/collect`, scheduler com `payload-file` e integração `/api/niches/catalog:ingest`), mantendo o módulo alinhado apenas ao pipeline real de ingestão CNPJ por ZIP.
- 2026-05-15 00:45:00 (UTC-3): implementado scheduler do pipeline real de importação CNPJ por ZIP no `oprm-coletor-mei`, configurado para disparo às 22:00 (`America/Sao_Paulo`) com criação automática de `import_run` no backend (`/api/oprm/market/import-runs`) e seed dos arquivos oficiais (Cnaes, Empresas0-9, Estabelecimentos0-9, Simples, Socios0-9) para snapshot padrão `2026-05-15`.

- 2026-05-15 01:30:00 (UTC-3): reprogramado o agendamento padrão do pipeline de importação CNPJ do módulo `oprm-coletor-mei` para executar diariamente às 04:00 no fuso `America/Sao_Paulo`, com atualização do cron padrão em `application.yml` para `0 0 4 * * *` (mantendo override por `OPRM_MARKET_IMPORT_SCHEDULE_CRON`).

- 2026-05-15 08:20:00 (UTC): implementação ponta a ponta no `oprm-coletor-mei` do pipeline agendado com execução real por arquivo: download dos 32 ZIPs para diretório temporário configurável (`oprm.market-import.collector.temp-dir`, padrão `/tmp/oprm-cnpj-import`), logs operacionais por etapa (download, unzip/leitura, persistência de status no backend), publicação de eventos por arquivo em `/api/oprm/market/import-runs/{runId}/files/{fileId}/events`, finalização de run em `/api/oprm/market/import-runs/{runId}/complete` e rotina de limpeza de temporários em bloco `finally` para sucesso/falha.

- 2026-05-15 12:15:00 (UTC-3): reprogramado o agendamento padrão da importação CNPJ/CNAEs no módulo `oprm-coletor-mei` para executar diariamente às 15:15 no fuso `America/Sao_Paulo`, atualizando o cron padrão em `application.yml` para `0 15 15 * * *` (mantido override por `OPRM_MARKET_IMPORT_SCHEDULE_CRON`).
- 2026-05-15 17:40:00 (UTC-3): ajuste operacional solicitado no `oprm-coletor-mei`: `snapshot-date` padrão fixado para `2026-05-10` (string fixa) e agendamento padrão reprogramado para 18:30 no fuso `America/Sao_Paulo`, com cron `0 30 18 * * *` em `application.yml` (mantido override por variáveis de ambiente).
- 2026-05-15 22:10:00 (UTC): criada visão de ranking dos CNAEs por volume no OPRM com novo endpoint backend `GET /api/oprm/market/import-runs/cnaes/top-volume?limit=` (snapshot mais recente, ordenado por `totalEmpresas`) e nova tela frontend `/oprm/cnaes-volume` exibindo os principais CNAEs com métricas de volume (empresas, MEI, Simples e estabelecimentos ativos).
- 2026-05-16 00:20:00 (UTC): criado documento `docs/novos-modulos/OPRM/oprm-modelo-ingestao-cnpj-cnae.md` descrevendo o modelo de dados e o fluxo de ingestão CNPJ/CNAE no OPRM (tabelas, endpoints, regras de consolidação, armazenamento de linhas lidas e uso para ranking de volume).
- 2026-05-17 09:10:00 (UTC-3): correção de causa-raiz no pipeline de importação CNPJ/CNAE do `oprm-coletor-mei`: o scheduler passou a fazer parse do conteúdo do `Cnaes.zip` (linhas brutas com log obrigatório de ingestão), montar payload `cnaes` e enviá-lo no evento `POST /api/oprm/market/import-runs/{runId}/files/{fileId}/events`, alinhando o contrato para popular `oprm_cnpj_cnae_dim` no backend. Também foi expandido o DTO de evento para suportar `cnaes` e `marketSizes` no coletor, mantendo compatibilidade com as etapas futuras de consolidação.

- 2026-05-17 03:41:06 (UTC-3): reprogramado o agendamento padrão da importação CNPJ/CNAEs no `oprm-coletor-mei` para executar diariamente às 05:00 no fuso `America/Sao_Paulo`, atualizando o cron padrão em `application.yml` para `0 0 5 * * *` (mantido override por `OPRM_MARKET_IMPORT_SCHEDULE_CRON`). Também foi validada a URL `Empresas0.zip` com retorno HTTP 200 para confirmar disponibilidade atual da fonte.

- 2026-05-17 13:30:00 (UTC-3): diagnóstico e correção de causa-raiz da ingestão noturna CNPJ/CNAE no módulo `oprm-coletor-mei`: identificado que a lista de arquivos `EMPRESAS` era montada com índice inicial 0 (`Empresas0.zip`), gerando falha HTTP 404 e encerramento `PARTIAL` da run (31/32). Ajustado o scheduler para gerar apenas `Empresas1.zip` a `Empresas9.zip`, alinhando com a nomenclatura válida da fonte e evitando bloqueio da etapa final de consolidação para tela `/oprm/cnaes-volume`.
- 2026-05-17 14:50:00 (UTC-3): reprogramado o agendamento padrão da importação CNPJ/CNAEs no `oprm-coletor-mei` para executar diariamente às 14:50 no fuso `America/Sao_Paulo`, atualizando o cron padrão em `application.yml` para `0 50 14 * * *` (mantido override por `OPRM_MARKET_IMPORT_SCHEDULE_CRON`).
- 2026-05-17 19:29:18 (UTC): diagnóstico da execução mais recente da importação de CNAEs solicitado pelo usuário: tentativa de validação operacional via backend (`http://191.252.181.168:8000/api/oprm/market/import-runs`) e via MCP (`https://mcpserverdigi.shop/mcp`) retornou indisponibilidade de rede no ambiente atual (connection refused/timeout), impedindo confirmação online da última run. Causa-raiz para “não aparece na tela” identificada no frontend OPRM: a tela `/oprm/cnaes-volume` dependia exclusivamente do endpoint de ranking por volume (`/cnaes/top-volume`), que pode retornar vazio enquanto a consolidação de `marketSizes` não estiver disponível, mesmo com catálogo de CNAEs já importado. Ajuste aplicado: fallback na própria tela para consumir também `/api/oprm/market/import-runs/cnaes`, exibindo os CNAEs importados com alerta explícito de “volume em consolidação”, evitando tela vazia e preservando transparência operacional.
- 2026-05-17 19:35:52 (UTC): pesquisa operacional via MCP Server refeita com sucesso parcial (intermitência de rede, porém consultas SQL responderam) para importação CNAE/CNPJ: `oprm_cnpj_import_run` mostrou run mais recente `id=6` em `STARTED` (`files_total=31`, `files_processed=0`), enquanto `oprm_cnpj_import_file` da mesma run já contém arquivos em `COMPLETED` (incluindo `Cnaes.zip`), sinalizando divergência de status de finalização da run. Também confirmado que a visualização de volume depende de consolidação em `oprm_market_size_by_cnae`; para evitar leitura enganosa na tela, a UI `/oprm/cnaes-volume` foi ajustada para fallback sem métricas zeradas: quando `top-volume` estiver vazio e houver catálogo, exibir CNAEs importados com coluna de status (`Importado/Inativo`) e alerta explícito de consolidação pendente.

- 2026-05-17 20:15:00 (UTC): ajuste na tela `/oprm/cnaes-volume` para suportar coluna explícita de **Quantidade** e paginação de 50 registros por página, com ordenação decrescente por quantidade (`totalEmpresas`) para priorizar CNAEs de maior volume no topo do ranking.
- 2026-05-17 20:45:00 (UTC): nova validação operacional via MCP Server para status de consolidação CNPJ/CNAE: banco respondeu `db_health=ok`, run mais recente (`id=6`) permanece em `STARTED` e `oprm_market_size_by_cnae` segue com `0` linhas. Para melhorar observabilidade de causa-raiz, foram adicionados logs no `oprm-coletor-mei` ao publicar eventos de arquivo e ao publicar a consolidação final do run (`/complete`), incluindo status e contadores agregados, além de confirmação explícita de sucesso após persistência.
- 2026-05-17 21:10:00 (UTC): para atender solicitação operacional de fechar somente a etapa de totalização hoje às 18:30 (America/Sao_Paulo), foi criado agendamento pontual no backend (`OprmMarketImportFinalizationScheduler`) com cron `0 30 18 17 5 *` que busca o run mais recente em `STARTED` e executa `completeRun` com carimbo de finalização e mensagem de fechamento manual. O agendamento possui guarda de data (`2026-05-17`) para evitar reexecução fora do dia solicitado.
- 2026-05-17 20:55:00 (UTC): validação via MCP Server da consolidação CNPJ/CNAE com tentativas e retries: confirmado que a run mais recente permanece `id=6` em `STARTED` e a tabela `oprm_market_size_by_cnae` segue com `total=0`, indicando que a consolidação de volume ainda não iniciou/persistiu. Como ação corretiva de observabilidade (causa-raiz), foram adicionados logs no backend (`OprmMarketImportService.registerFileEvent`) para registrar recebimento de `marketSizes` por arquivo (`runId/fileId`, quantidades e snapshotDate) e confirmação explícita após persistência da consolidação, facilitando diagnóstico em próximas execuções.

- 2026-05-17 22:20:00 (UTC): diagnóstico da execução mais recente da totalização de CNAEs via MCP Server: a tabela de agregação `oprm_market_size_by_cnae` permanece vazia (`0` linhas), enquanto a dimensão `oprm_cnpj_cnae_dim` está populada (`1359` CNAEs, `updated_at` em 2026-05-17 17:50:18). Em `oprm_cnpj_import_run`, as últimas execuções estão em `PARTIAL` (incluindo a run `id=6`, finalizada manualmente com `rows_read/rows_valid=0` e mensagem `Finalização manual agendada para 18:30`), indicando que a etapa de consolidação de volume por CNAE não completou/persistiu. Causa-raiz operacional observada: ingestões não chegaram ao estado `COMPLETED`, logo a tela de totais segue sem dados de volume.

- 2026-05-17 22:35:00 (UTC): validação adicional solicitada: confirmado em código backend o agendamento pontual mais recente para **somente totalização** (`OprmMarketImportFinalizationScheduler`, cron `0 30 18 17 5 *`, timezone `America/Sao_Paulo`, guarda de data `2026-05-17`). Esse agendamento executa `completeRun` no run `STARTED` mais recente com mensagem `Finalização manual agendada para 18:30`; no banco, a run `id=6` aparece finalizada manualmente em `PARTIAL` e a tabela `oprm_market_size_by_cnae` permanece com `0` linhas, portanto a tela segue sem totais por ausência de consolidação persistida.

- 2026-05-17 22:55:00 (UTC): verificação solicitada sobre logs da execução recente: nos últimos 500 logs do backend obtidos via MCP não apareceram linhas explícitas de início da rotina `OprmMarketImportFinalizationScheduler` (janela de log provavelmente já rotacionada/fora do recorte). Porém, a evidência persistida em `oprm_cnpj_import_run` confirma execução da etapa de finalização/totalização para o run `id=6`, com `finished_at=2026-05-17T21:30:00`, `status=PARTIAL` e mensagem `Finalização manual agendada para 18:30`.

- 2026-05-17 23:10:00 (UTC): revisão detalhada do módulo OPRM (backend) concluída com ajuste estrutural para isolar a etapa de totalização: `OprmMarketImportFinalizationScheduler` passou a usar agendamento dedicado diário às 19:40 (`oprm.market-import.totalization.schedule.cron=0 40 19 * * *`, timezone `America/Sao_Paulo`) e removida a trava de data única. Também foram adicionados logs granulares `[OPRM-TOTALIZACAO]` no scheduler (início, run candidato, disparo e sucesso) e no `OprmMarketImportService.completeRun` (payload de entrada, contadores pré-finalização por status de arquivo, e snapshot final persistido) para rastrear ponta a ponta o fluxo de fechamento da totalização.

- 2026-05-18 01:55:58 (UTC): adicionado novo agendamento diário de totalização OPRM para 23:30 (America/Sao_Paulo) no backend, mantendo o agendamento existente de 19:40; scheduler agora possui execuções separadas com label de horário e propriedade configurável `oprm.market-import.totalization.schedule.cron-2330`.

- 2026-05-18 02:05:00 (UTC): ajuste solicitado no backend OPRM para deixar os agendamentos de totalização fixos em código (sem variáveis/propriedades): 19:40 e 23:30 em America/Sao_Paulo, removendo configuração via `application.properties`.

- 2026-05-18 02:12:52 (UTC): ajuste solicitado no backend OPRM para remover o agendamento das 19:40 e manter somente a totalização diária fixa às 23:30 no fuso America/Sao_Paulo.

- 2026-05-18 02:30:00 (UTC): ajuste solicitado no backend OPRM para reagendar a execução do totalizador para **00:00 do dia 18 de maio** (fuso `America/Sao_Paulo`), alterando o cron do `OprmMarketImportFinalizationScheduler` para `0 0 0 18 5 *` e atualizando o rótulo de execução para `00:00 18/05`.
- 2026-05-18 00:00:00 (UTC): agendamento do `OprmMarketImportFinalizationScheduler` alterado para 11:00 no dia 18/05 (fuso `America/Sao_Paulo`), com cron hardcoded `0 0 11 18 5 *` na anotação `@Scheduled`.

- 2026-05-18: Removido agendamento de finalização no backend (`OprmMarketImportFinalizationScheduler`) e criado endpoint `POST /api/oprm/market/import-runs/finalize-latest-started`; criado agendamento no módulo `oprm-coletor-mei` às 11:00 America/Sao_Paulo para acionar esse endpoint.
- 2026-05-19 02:46:58 (UTC): diagnóstico operacional do totalizador de CNAEs executado via MCP (banco + logs) e reforço de observabilidade no `oprm-coletor-mei`: adicionados logs explícitos por arquivo com `datasetType`, `cnaesCount` e `marketSizesCount`, além de `WARN` quando evento é publicado sem `marketSizes` (situação que impede atualização de `oprm_market_size_by_cnae`). Também foi executada finalização manual por endpoint para validar o fluxo atual (`POST /api/oprm/market/import-runs/finalize-latest-started`), que retornou sem run `STARTED` pendente.
- 2026-05-19 03:00:00 (UTC): agendada a próxima execução da importação OPRM CNPJ/CNAE para **00:30 do dia 19/05** no fuso `America/Sao_Paulo`, com cron hardcoded `0 30 0 19 5 *` diretamente na anotação `@Scheduled` do método `runScheduledImport` no `oprm-coletor-mei`.

- 2026-05-19 00:20:00 (UTC-3): ajuste solicitado para remover índice 0 na semente de arquivos da importação CNPJ/CNAE no `oprm-coletor-mei`: `buildFiles` passou a gerar `Estabelecimentos1.zip..Estabelecimentos9.zip` e `Socios1.zip..Socios9.zip` (além de `Empresas1.zip..Empresas9.zip` já vigente), eliminando referências `*0.zip` que vinham provocando falha/`PARTIAL` quando inexistentes na fonte.

- 2026-05-19 09:55:00 (UTC-3): ajuste solicitado para reagendar a próxima execução da importação OPRM CNPJ/CNAE para **10:00 de hoje (19/05)** no fuso `America/Sao_Paulo`, com cron hardcoded `0 0 10 19 5 *` na anotação `@Scheduled` de `runScheduledImport`.

- 2026-05-19 01:40:00 (UTC-3): ajuste solicitado no `oprm-coletor-mei` para reagendar o coletor CNPJ/CNAE para **04:40** no fuso `America/Sao_Paulo`, atualizando o cron hardcoded de `runScheduledImport` para `0 40 4 * * *` e sincronizando o cron padrão em `application.yml` para o mesmo horário.

- 2026-05-20 00:00:00 (UTC): revisão do `runScheduledImport` no `oprm-coletor-mei` confirmou ausência de totalização por CNAE no payload `marketSizes` (sempre `null`). Implementada totalização incremental por CNAE a partir dos arquivos `Estabelecimentos*.zip` no próprio coletor (contagem de `totalEstabelecimentos` e `totalEstabelecimentosAtivos` por `cnae_principal`), com acúmulo em memória durante a run e publicação do snapshot acumulado em `marketSizes` nos eventos `COMPLETED` de cada arquivo de estabelecimentos para persistência em `oprm_market_size_by_cnae` no backend.

- 2026-05-20 08:50:00 (UTC-3): ajuste solicitado para reagendar a execução diária do coletor OPRM CNPJ/CNAE para **21:40** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 40 21 * * *` e sincronização do cron padrão em `application.yml` para o mesmo horário.

- 2026-05-20: Ajustado agendamento do importador OPRM CNPJ (runScheduledImport) de 21:40 para 23:30 (America/Sao_Paulo), conforme solicitação operacional.
- 2026-05-20 00:40:00 (UTC): tratamento de causa-raiz para perda de market size por finalização prematura da run OPRM CNPJ/CNAE: adicionada regra canônica em `docs/canonical/system-governance-canon.v2.md` bloqueando fechamento quando houver arquivo `ESTABELECIMENTOS` em `STARTED`; implementado bloqueio no backend (`OprmMarketImportService.completeRun`) com retorno `HTTP 409` e log explícito de causa-raiz. Objetivo: impedir conversão automática silenciosa de `STARTED` para `FAILED` antes da consolidação de `marketSizes`.
- 2026-05-20 03:35:00 (UTC): melhoria de observabilidade solicitada para o coletor OPRM CNPJ/CNAE: adicionados logs de acompanhamento da leitura dos arquivos `Estabelecimentos*.zip` no `OprmMarketImportScheduler`, incluindo início por `ZipEntry`, progresso periódico (a cada 500.000 linhas), contadores de linhas lidas/válidas/ignoradas e resumo final por arquivo com total consolidado de CNAEs para market size.
- 2026-05-20 03:50:00 (UTC): atualização canônica solicitada para explicitar identificação de CNAE no dataset `Estabelecimentos*.zip`: formalizado no `system-governance-canon.v2.md` que a totalização deve usar o CNAE principal na posição 11 (índice zero-based), com normalização para dígitos e contabilização/log obrigatório de linhas ignoradas.
- 2026-05-20 04:05:00 (UTC): ajuste solicitado para definir o próximo agendamento da importação OPRM CNPJ/CNAE para **09:00 de hoje (20/05)** no fuso `America/Sao_Paulo`, com cron hardcoded de `runScheduledImport` alterado para `0 0 9 20 5 *` e sincronização do valor padrão em `application.yml` para o mesmo cron.
- 2026-05-20 14:20:00 (UTC): atualização canônica solicitada para o fluxo OPRM CNPJ/CNAE: formalizado no `system-governance-canon.v2.md` que o endpoint de finalização (`/api/oprm/market/import-runs/{runId}/complete`) só pode ser chamado após a leitura de **todos** os arquivos da run (apenas estados terminais `COMPLETED`/`FAILED`, sem `STARTED` remanescente), proibindo chamada antecipada de `completeRun`.
- 2026-05-20 14:35:00 (UTC): criado documento canônico específico do OPRM em `docs/canonical/oprm-canon.v1.md`, formalizando regra de chamar `completeRun` apenas após leitura total da run, bloqueio `HTTP 409` com `ESTABELECIMENTOS` em `STARTED`, regra de identificação de CNAE e requisitos mínimos de observabilidade.
- 2026-05-20 14:50:00 (UTC): ajuste canônico solicitado: removido integralmente o item 14 de `system-governance-canon.v2.md` e migrado para `docs/canonical/oprm-canon.v1.md`, com título explícito sobre ingestão de CNAE e totalização de market size.
- 2026-05-20 15:05:00 (UTC): ajuste de implementação no `oprm-coletor-mei` para garantir que `completeRun` seja chamado somente após concluir a leitura de todos os arquivos da run. Adicionada guarda `readAllFilesInRun` no `OprmMarketImportScheduler` e proteção para falha de publicação de evento `FAILED` sem abortar o loop de leitura.

- 2026-05-20 15:30:00 (UTC): ajuste solicitado para agendar nova ingestão de CNAEs para **12:30 de hoje (20/05)** no fuso `America/Sao_Paulo`, com cron hardcoded de `runScheduledImport` alterado para `0 30 12 20 5 *` no `oprm-coletor-mei`.
- 2026-05-20 17:48:12 (UTC): investigação de causa-raiz da leitura de arquivo grande `Estabelecimentos*.zip` no `oprm-coletor-mei`: adicionados logs de observabilidade no parse de `marketSizes` para registrar duração da leitura em memória por `ZipEntry`, tamanho em bytes carregado, memória livre/total JVM no momento, total de linhas segmentadas por entry, duração de processamento da entry e duração total do parse com contadores acumulados. Objetivo: identificar com precisão se o gargalo/falha ocorre na etapa de carga em memória, segmentação de linhas ou processamento por campo.

- 2026-05-20 17:55:00 (UTC): ajuste solicitado para agendar a próxima execução da importação OPRM CNPJ/CNAE para **16:00** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 0 16 * * *` e sincronização do cron padrão em `application.yml` para o mesmo horário.
- 2026-05-20 18:30:00 (UTC): ajuste solicitado para o próximo agendamento da importação OPRM CNPJ/CNAE para **18:30** no fuso `America/Sao_Paulo`, com cron hardcoded de `runScheduledImport` alterado para `0 30 18 * * *` e sincronização do cron padrão em `application.yml` para o mesmo horário.
- 2026-05-21 00:00:00 (UTC): diagnóstico da execução mais recente da ingestão de CNAEs via arquivo `Estabelecimentos*.zip` (run_id=14) consultado via MCP (`db_query`): todos os arquivos `Estabelecimentos1..9.zip` finalizaram com `FAILED`, `rows_read=0` e erro 500 por truncamento de dado (`Data too long for column 'cnae_code'`) na inserção em `oprm_market_size_by_cnae`, mantendo a run em status `PARTIAL`.
- 2026-05-21 00:00:00 (UTC): melhoria de observabilidade no backend (`OprmMarketImportService`) para diagnosticar truncamento em `cnae_code` durante persistência de `oprm_market_size_by_cnae`: adicionado log antes do `save` com `cnaeCodeRaw`, `cnaeCodeNormalized`, `cnaeCodeLength` e métricas do payload; e log de erro no `catch RuntimeException` com contexto operacional completo (`runId`, `fileId`, `snapshotDate`, payload e stacktrace) antes de relançar a exceção.
- 2026-05-21 00:00:00 (UTC): ajuste solicitado para agendar a próxima execução da importação OPRM CNPJ/CNAE para **00:40** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 40 0 * * *` e sincronização do cron padrão em `application.yml` para o mesmo horário.
- 2026-05-21 00:00:00 (UTC): ajuste de documentação em código no backend (`OprmMarketImportService`) para conformidade: adicionados comentários de responsabilidade da classe e comentários breves em todos os métodos públicos/privados do serviço OPRM de importação.
- 2026-05-21 00:00:00 (UTC): ajuste solicitado para alterar o agendamento da importação OPRM CNPJ/CNAE para **01:30** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 30 1 * * *` e sincronização do cron padrão em `application.yml` para o mesmo horário.

- 2026-05-21: Ajustado o agendamento da ingestão OPRM de CNAEs no coletor MEI para 10:00 (America/Sao_Paulo), atualizando o cron de `runScheduledImport` para `0 0 10 * * *`.

- 2026-05-21 00:00:00 (UTC): validação operacional solicitada via MCP para localizar log explícito do insert em `oprm_market_size_by_cnae` na janela da run 16: filtros por `insert into oprm_market_size_by_cnae`, `cnaeCodeRaw` e `Data too long for column 'cnae_code'` retornaram 0 linhas no backend (com intermitência pontual em um dos filtros), mantendo evidência principal no `error_message` da tabela de arquivos. Na mesma tarefa, o agendamento diário da ingestão OPRM CNPJ/CNAE foi atualizado para **13:30** (America/Sao_Paulo) com cron hardcoded `0 30 13 * * *` em `runScheduledImport` e sincronização do cron padrão em `application.yml`. Também foi feita verificação e ajuste de conformidade de comentários de responsabilidade na classe `OprmMarketImportScheduler` (classe + métodos).

- 2026-05-21 00:00:00 (UTC): ajuste solicitado para alterar o agendamento diário da ingestão OPRM CNPJ/CNAE para **14:30** (America/Sao_Paulo), com atualização do cron hardcoded de `runScheduledImport` para `0 30 14 * * *` e sincronização do cron padrão em `application.yml`.
- 2026-05-21 18:15:00 (UTC): criada a exception específica `SQLException` no módulo OPRM (backend ads-service) para encapsular falhas SQL com a instrução tentada, e atualizado `OprmMarketImportService` para registrar explicitamente a SQL de upsert (`oprm_market_size_by_cnae`) no log de erro e relançar essa exception para diagnóstico de causa-raiz.
- 2026-05-21 18:30:00 (UTC): ajuste solicitado para agendar nova execução da importação OPRM CNPJ/CNAE para **15:30** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 30 15 * * *` e sincronização do cron padrão em `application.yml`.
- 2026-05-21 18:45:00 (UTC): correção de nomenclatura solicitada no backend OPRM: renomeada a exception customizada de `SQLExcpetion` para `SQLException` (classe/arquivo/imports/uso no serviço), mantendo comportamento de transportar a SQL tentada no erro de persistência.
- 2026-05-21 19:00:00 (UTC): correção de causa-raiz no coletor OPRM CNAE para evitar envio de dois campos no cnae_code da totalização de Estabelecimentos\*.zip: substituída extração por dígitos agregados por extração do primeiro match válido de 7 dígitos (extractPrimaryCnaeCode), com log de linha ignorada quando coluna11 vier inválida. Objetivo: impedir truncamento 'Data too long for column cnae_code' no insert em oprm_market_size_by_cnae.
- 2026-05-21 19:15:00 (UTC): ajuste solicitado para agendar a próxima ingestão OPRM CNPJ/CNAE para **17:00** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 0 17 * * *` e sincronização do cron padrão em `application.yml`.

- 2026-05-21 19:30:00 (UTC): atualização canônica solicitada para OPRM: formalizada no `docs/canonical/oprm-canon.v1.md` a regra da visão `/oprm/cnaes-volume` com ordenação obrigatória por **Quantidade** (descendente, maior→menor) e paginação obrigatória com tamanho padrão de 50 registros por página, com ordenação aplicada no backend.

- 2026-05-21 19:50:00 (UTC): implementação solicitada do ranking `/oprm/cnaes-volume` com paginação real no backend: endpoint `GET /api/oprm/market/import-runs/cnaes/top-volume` atualizado para receber `page` e `size` (default `0`/`50`), serviço OPRM ajustado para aplicar `PageRequest` e frontend alterado para consumir paginação server-side mantendo ordenação por quantidade no backend.

- 2026-05-21 20:05:00 (UTC): correção na tela OPRM `/oprm/cnaes-volume` para garantir ordenação visual do ranking por **Estab. ativos** (descendente, maior→menor) no frontend, adicionando ordenação defensiva client-side em `OprmCnaeVolumePage` após resposta paginada do backend e ajustando o texto da seção para explicitar o critério de estabelecimentos ativos.

- 2026-05-21 17:20:00 (UTC-3): ajuste solicitado na tela de CNAEs para exibir os 50 registros com maior quantidade de estabelecimentos ativos em ordem decrescente: ordenação backend do endpoint `GET /api/oprm/market/import-runs/cnaes/top-volume` alterada para `totalEstabelecimentosAtivos DESC`, preservando paginação de 50 por página.

- 2026-05-21 17:35:00 (UTC-3): adição de comentários de responsabilidade no `OprmMarketSizeByCnaeRepository` (classe e método) para atender revisão de PR e manter conformidade com a regra de documentação Java do projeto.

- 2026-05-22 00:00:00 (UTC): iniciado novo processo de ingestão no coletor OPRM para preencher métricas por CNAE com dados de EMPRESAS/SIMPLES. No `OprmMarketImportScheduler`, foi adicionada consolidação de `total_empresas` a partir dos arquivos `Empresas*.zip` (mapeando CNPJ base -> CNAE principal) e consolidação de `total_empresas_simples` / `total_empresas_mei` a partir do `Simples.zip`, com deduplicação por CNPJ base para evitar dupla contagem. Também foi atualizado o payload de `marketSizes` para enviar esses campos preenchidos ao backend.

- 2026-05-22 00:00:00 (UTC): reforçada observabilidade do novo processo OPRM (EMPRESAS/SIMPLES) com logs de etapa e payload em exceções. Foram adicionados logs de contexto no catch do loop principal (datasetType, fileUrl, snapshotDate e contadores) e logs com último payload bruto processado (`lastRawPayload`) nos parsers de EMPRESAS e SIMPLES antes de relançar exceções.

- 2026-05-22 00:00:00 (UTC): ajuste solicitado para agendar execução única da ingestão OPRM CNPJ/CNAE para amanhã (23/05/2026) às 10:00 em America/Sao_Paulo, atualizando o cron hardcoded de `runScheduledImport` para `0 0 10 23 5 *` e sincronizando o valor padrão em `application.yml`. Também foi reforçado o comentário de responsabilidade da classe `OprmMarketImportScheduler`.

- 2026-05-22 00:00:00 (UTC): correção de causa-raiz na persistência de contagem MEI por CNAE no `oprm-coletor-mei`: o scheduler publicava `marketSizes` apenas nos arquivos `ESTABELECIMENTOS`, antes do processamento do `Simples.zip`, fazendo `total_empresas_mei`/`total_empresas_simples` persistirem como zero. Ajustado `runScheduledImport` para recalcular e publicar snapshot consolidado de `marketSizes` também após o dataset `SIMPLES`, garantindo atualização de `total_empresas`, `total_empresas_mei` e `total_empresas_simples` no backend para cada CNAE.

- 2026-05-22 15:50:00 (UTC-3): ajuste solicitado para agendar a próxima ingestão OPRM CNPJ/CNAE para **16:30** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 30 16 * * *` e sincronização do cron padrão em `application.yml`.

- 2026-05-23 00:00:00 (UTC): ajuste solicitado para executar a ingestão OPRM CNPJ/CNAE às **00:01 de 23/05/2026** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 1 0 23 5 *`, sincronização do cron padrão em `application.yml` e correção da mensagem de log final para refletir 00:01.

- 2026-05-23 03:58:00 (UTC): diagnóstico operacional via MCP solicitado sobre a importação de CNAEs das 00:01 (America/Sao_Paulo). Consulta de logs do módulo `oprm-coletor-receita` no intervalo 02:30Z–04:30Z confirmou execução ativa às 03:01:13Z (00:01:13 UTC-3), com múltiplos registros `payload_bruto_cnae` no `run=21 fileId=602`, indicando processamento da carga de CNAEs em andamento nesse horário. Houve intermitência do endpoint MCP para consultas SQL de consolidação do run (`db_query`), sem retorno estável do status final da execução nesta rodada.

- 2026-05-23 04:20:00 (UTC): validação de causa-raiz da totalização MEI/SIMPLES por CNAE e adição de logs diagnósticos no `OprmMarketImportScheduler` para rastrear mapeamento `SIMPLES -> CNAE` (linhas lidas, linhas com match, sem match, amostra de CNPJ base sem correspondência e versão apenas com dígitos). Objetivo: confirmar em runtime divergência de chave entre `Simples.zip` e mapa de `Empresas*.zip`.

- 2026-05-23 04:37:00 (UTC): ajuste solicitado para agendar execução da ingestão OPRM CNPJ/CNAE às 10:00 de 24/05/2026 (America/Sao_Paulo), com atualização do cron hardcoded de `runScheduledImport` para `0 0 10 24 5 *`, sincronização do cron padrão em `application.yml` e atualização do `snapshot-date` padrão para `2026-05-24`.

- 2026-05-23 05:05:00 (UTC): adicionado diagnóstico aprofundado de conteúdo no parse de EMPRESAS e SIMPLES no `oprm-coletor-mei` para investigação de causa-raiz da totalização zerada de MEI/SIMPLES. O scheduler agora registra amostras controladas (até 10) com campos literais de `cnpjBase`, `cnpjBaseDigits`, opt-in de SIMPLES/MEI e `cnaeCode` encontrado (match) ou ausente (missing), além de amostras de mapeamento `EMPRESAS -> CNAE` construído em runtime. Objetivo: evidenciar divergência exata de chave entre arquivos de origem sem gerar volume de log descontrolado.

- 2026-05-23 05:20:00 (UTC): ajuste solicitado para o próximo agendamento da ingestão OPRM CNPJ/CNAE para **14:06 de 24/05/2026** (America/Sao_Paulo), com atualização do cron hardcoded de `runScheduledImport` para `0 6 14 24 5 *`, sincronização do cron padrão em `application.yml` e ajuste da mensagem de log para refletir 14:06.
- 2026-05-23 00:00:00 (UTC): validação solicitada do módulo de logs no MCP confirmando nomenclatura operacional `module=oprm-coletor-receita` para consultar o serviço `oprm-coletor-mei`; em seguida, reagendado o coletor OPRM CNPJ/CNAE para **18:20 de hoje (23/05/2026)** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 20 18 23 5 *`, sincronização do cron padrão em `application.yml` e ajuste do `snapshot-date` padrão para `2026-05-23`.

- 2026-05-25 00:00:00 (UTC): ajuste solicitado para adicionar log imediatamente após criação da run no `runScheduledImport`, registrando `runId`, `snapshotDate`, `sourceUrl` e `filesTotal`; e alteração do agendamento hardcoded da ingestão OPRM CNPJ/CNAE para **16:45** (America/Sao_Paulo) com cron `0 45 16 * * *`, incluindo atualização da mensagem final de sucesso para refletir 16:45.

- 2026-05-25 00:00:00 (UTC): ajuste complementar solicitado por revisão: adicionado log textual explícito com `runId` logo após a criação da run no `runScheduledImport` para facilitar buscas operacionais por `contains=runId=` nos logs do MCP.

- 2026-05-25 00:00:00 (UTC): ajuste solicitado em revisão para inserir log no início do método `runScheduledImport` (primeira instrução), registrando disparo da rotina antes de qualquer validação de `enabled`.

- 2026-05-25 17:20:00 (UTC-3): atualização canônica solicitada no OPRM para deixar explícito que o `snapshotDate` da ingestão CNPJ/CNAE deve permanecer fixo em `2026-05-10`, proibindo troca automática para datas mais novas sem decisão explícita do usuário; documento atualizado em `docs/canonical/oprm-canon.v1.md`.

- 2026-05-25 17:25:00 (UTC-3): ajuste solicitado no cânone OPRM para explicitar a URL completa de download do snapshot fixo (`https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-05-10/`) e tornar obrigatória a validação prévia de acesso HTTP 200 (HEAD) dos arquivos de referência (`Cnaes.zip`, `Empresas1.zip`, `Estabelecimentos1.zip`) antes da execução.

- 2026-05-25 17:35:00 (UTC-3): ajuste solicitado no `oprm-coletor-mei` para reagendar a execução da ingestão OPRM CNPJ/CNAE para **17:45** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 45 17 * * *`, sincronização do cron padrão em `application.yml` e ajuste da mensagem de log final para refletir 17:45.

- 2026-05-25 17:45:00 (UTC-3): correção solicitada no `oprm-coletor-mei` para remover dependência de data atual no diretório da fonte e fixar o `snapshot-date` padrão em `2026-05-10` no `application.yml`, mantendo o diretório de ingestão alinhado ao cânone OPRM.

- 2026-05-26 00:00:00 (UTC): ajuste solicitado para reduzir ruído operacional no método `runScheduledImport` do `oprm-coletor-mei`, removendo logs auxiliares de INFO/WARN/ERROR dentro do fluxo e mantendo somente o log inicial `Iniciando runScheduledImport do OPRM CNPJ/CNAE.` para facilitar rastreio no MCP sem poluição de saída.

- 2026-05-26 00:00:00 (UTC): ajuste solicitado para agendar nova execução da ingestão OPRM CNPJ/CNAE para **00:05 de 26/05/2026** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 5 0 26 5 *` e sincronização do cron padrão em `application.yml`.

- 2026-05-26 00:00:00 (UTC): ajuste solicitado para ampliar observabilidade no mapeamento `SIMPLES -> CNAE` em `parseAndAccumulateSimplesAndMeiByCnaeFromSimplesZip`, adicionando logs explícitos para casos com match/sem match de `cnpjBase`, além de logs de incremento dos contadores `totalEmpresasSimples` e `totalEmpresasMei` por `cnaeCode`.
- 2026-05-26 13:05:00 (UTC-3): ajuste solicitado no `oprm-coletor-mei` para mapear CNAE por `cnpjBase` usando exclusivamente os arquivos `Estabelecimentos1..9.zip` (em vez de `Empresas1..9.zip`) no fluxo da importação CNPJ/CNAE. O método de mapeamento para o `SIMPLES` foi alterado para extrair `cnaePrincipal` da coluna de CNAE principal de ESTABELECIMENTOS (`cols[11]`), preservando o parse de market size e os logs diagnósticos de match/sem-match.
- 2026-05-26 13:10:00 (UTC-3): ajuste solicitado para novo agendamento pontual da ingestão OPRM CNPJ/CNAE para **14:05 de 25/05/2026** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 5 14 25 5 *`.
- 2026-05-26 17:40:00 (UTC): ajuste solicitado para diagnóstico dirigido do dataset SIMPLES no `oprm-coletor-mei`: o parser foi configurado para interromper o processamento após registrar exatamente 20 ocorrências de `SIMPLES` sem match de CNAE, com logs detalhados por ocorrência (`cnpjBase`, `cnpjBaseDigits`, flags de optante SIMPLES/MEI e índice progressivo do diagnóstico).

- 2026-05-26 18:20:00 (UTC-3): ajuste solicitado para reagendar novamente a ingestão OPRM CNPJ/CNAE para **18:20** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 20 18 * * *` e sincronização do cron padrão em `application.yml`.
- 2026-05-27 00:07:22 (UTC): ajuste solicitado para agendar nova execução da ingestão OPRM CNPJ/CNAE para **23:10** no fuso `America/Sao_Paulo`, com atualização do cron hardcoded de `runScheduledImport` para `0 10 23 * * *` no `oprm-coletor-mei`.
- 2026-05-27 00:00:00 (UTC): ajuste solicitado na ingestão de CNAEs do `oprm-coletor-mei` para processar **somente** os arquivos `Estabelecimentos1..9.zip` e `Simples.zip` na montagem da lista de arquivos da run, removendo o processamento de `Cnaes.zip`, `Empresas1..9.zip` e `Socios1..9.zip` neste momento.
- 2026-05-27 00:00:00 (UTC): criação de nova estrutura no backend para apoiar ingestão operacional de estabelecimentos por chave mínima de negócio, com changelog Liquibase adicionando a tabela `oprm_estabelecimento_cnae_raiz` (campos `cnpj_raiz` e `cnae_code`, PK composta e índice por `cnae_code`) para permitir persistência dedicada do vínculo CNPJ raiz ↔ CNAE no fluxo OPRM.
- 2026-05-27 00:00:00 (UTC): ajuste solicitado após revisão para incluir o campo `email` na tabela `oprm_estabelecimento_cnae_raiz` via changelog incremental (`addColumn`), preservando compatibilidade com bancos já migrados e adicionando índice `idx_oprm_estab_cnae_raiz_email` para consultas operacionais por contato.
- 2026-05-28 00:00:00 (UTC): ajuste solicitado para a nova tabela de ingestão de estabelecimentos no backend, adicionando os campos booleanos `is_mei` e `is_simples` em `oprm_estabelecimento_cnae_raiz` via changelog incremental, ambos `NOT NULL DEFAULT 0`, para marcar se o estabelecimento aparece como MEI e/ou Simples na fonte.

- 2026-05-28 16:10:00 (UTC): correção de causa-raiz no changelog Liquibase `2026-05-28-oprm-estabelecimento-cnae-raiz-mei-simples.yaml`: removido `ADD COLUMN IF NOT EXISTS`, incompatível com MySQL 5.7, e substituído por dois changesets idempotentes com `preConditions` por coluna antes de executar `ALTER TABLE ADD COLUMN`.
- 2026-05-28 14:05:00 (UTC-3): criado novo processo simples no `oprm-coletor-mei` para baixar todos os arquivos `Estabelecimentos0..9.zip` do snapshot canônico e inserir em lotes na tabela `oprm_estabelecimento_cnae_raiz` do backend, extraindo `cnpj_raiz`, `cnae_code` e `email`; processo agendado para executar em **28/05 às 14:05** no fuso `America/Sao_Paulo`.
- 2026-05-28 00:00:00 (UTC): ajuste solicitado para reagendar a ingestão simples OPRM de estabelecimentos na tabela `oprm_estabelecimento_cnae_raiz` para **29/05/2026 às 10:00** no fuso `America/Sao_Paulo`, com cron hardcoded `0 0 10 29 5 *` no método `runScheduledEstabelecimentoCnaeRaizIngestion`.

- 2026-05-29 23:20:00 (UTC): correção de causa-raiz para `OutOfMemoryError` no `OprmMarketImportScheduler` durante o mapeamento `cnpjBase -> cnaePrincipal`: removido o mapa global de todos os estabelecimentos em heap e implementado processamento particionado do cruzamento `SIMPLES` x `ESTABELECIMENTOS` em 128 blocos, mantendo apenas uma partição em memória por vez; o cânone OPRM também passou a proibir materialização global desse vínculo.
- 2026-05-31 00:00:00 (UTC): alteração solicitada na tela `/oprm/cnaes-volume` para ordenar o ranking de CNAEs por `Empresas MEI` em ordem decrescente; backend passou a ordenar a consulta por `totalEmpresasMei DESC` com desempate por CNAE, frontend deixou de reordenar a página em memória e o cânone OPRM foi sincronizado com a nova regra.
- 2026-05-31 00:00:00 (UTC): criado documento de arquitetura para o fluxo CNAE → candidatos de nicho no OPRM, incluindo diagrama de arquitetura, diagrama de sequência, estados sugeridos, contratos candidatos e campos mínimos para transformar ranking CNAE em nichos aprováveis sem criação automática direta de `MarketNiche`.

- 2026-05-31 00:00:00 (UTC): corrigida a sintaxe Mermaid do diagrama de arquitetura CNAE → nichos, substituindo o rótulo do subgraph do banco para formato compatível com o renderizador do GitHub e preservando os nós de tabelas como cilindros.

- 2026-05-31: Revisado o documento `docs/novos-modulos/OPRM/oprm-cnae-para-nichos-arquitetura.md` para refletir que o score e o enriquecimento CNAE serão executados por schedulers do módulo OPRM, com ciclos rastreáveis, enquanto o backend ficará restrito a leitura/gravação e persistência. Atualizado o cânone OPRM com a regra de responsabilidade do OPRM no fluxo CNAE → oportunidade.
- 2026-06-01 00:00:00 (UTC): implementada a primeira versão operacional do fluxo CNAE → score → enriquecimento → candidatos: backend recebeu tabelas, entidades, DTOs e endpoints OPRM apenas para leitura/gravação; `oprm-coletor-mei` recebeu schedulers automáticos `CNAE_SCORE` e `CNAE_ENRICHMENT` com `cycleId`, `cycleType` e `cycleNumber`; frontend `/oprm/cnaes-volume` passou a exibir score e últimos ciclos sem botão de geração manual.

- 2026-05-31 23:12:12 (UTC-3): simplificada a navegação inicial do OPRM no frontend para manter somente a entrada de CNAEs; a rota `/oprm` agora abre diretamente o ranking de CNAEs, removendo da tela inicial os atalhos de workspace, rotina, oferta, evidências, feedback, catálogo e operações para permitir reinício do fluxo OPRM por CNAE.

- 2026-06-01 03:25:00 (UTC): implementação do próximo passo de geração de vendas no OPRM CNAE: o enriquecimento automático passou a executar catch-up na subida do coletor para processar scores sem `enriched_at`, registrar falhas críticas sem derrubar a aplicação e usar `OprmCnaeRoutineSignalBuilder` para transformar CNAEs priorizados em sinais concretos de rotina, dor, resultado, mecanismo, prova e oferta por arquétipo operacional. Também foram adicionados testes unitários cobrindo enriquecimento de beleza e serviços técnicos, garantindo ausência de JSON textual nos sinais funcionais.
- 2026-06-01 00:15:00 (UTC): ajustada a tela `/oprm/cnaes-volume` para ordenar CNAEs por Score OPRM decrescente diretamente no backend, mantendo paginação de 50 registros e exibindo volume de mercado como contexto para decisão comercial. Atualizado o cânone OPRM para refletir a nova regra de ordenação por score.

- 2026-06-01 00:00:00 (UTC): planejadas novas telas de acompanhamento operacional do OPRM para dar visibilidade ao usuário sobre ingestão de mercado, ciclos CNAE, enriquecimento, candidatos de nicho, fila de decisão e diagnóstico, preservando o eixo Dados de mercado → Score → Enriquecimento → Nicho candidato → Decisão humana → Oferta vendável.

- 2026-06-01 00:00:00 (UTC): adicionados wireframes SVG das telas de acompanhamento operacional do OPRM para facilitar validação visual do Painel OPRM, Ingestão de Mercado, Ciclos CNAE, Candidatos de Nicho, Fila de Decisão e Diagnóstico antes da implementação frontend.

- 2026-06-01 00:00:00 (UTC): adicionado botão na tela `/oprm/cnaes-volume` para o usuário consultar diretamente os nichos já enriquecidos pelo OPRM; backend recebeu endpoint de leitura dos candidatos enriquecidos mais recentes e frontend passou a exibir tabela com nicho, CNAE, dor, resultado, mecanismo, score, status e data de enriquecimento.
- 2026-06-01 18:52:03 (UTC-3): ajuste na tela de CNAEs por Score OPRM para que a lista "Nichos já enriquecidos" seja carregada pelo backend em ordem decrescente de `opportunityScore`, com `createdAt` como desempate, deixando os maiores scores no começo para priorizar decisões com maior potencial de venda.

- 2026-06-02 00:00:00 (UTC): criado no backend o novo pacote `com.marketinghub.oprm.nichocnae` para o pipeline OPRM de pesquisa de rotina por nicho CNAE, com a primeira etapa `routineresearchcycle` estruturada no padrão canônico por etapa (`web`, `service`, `pending`, `listStageExecutions`, `detailStageExecution`, `recebePrompt`, `recebeResposta`) para preparar a evolução até o `oprm_niche_routine_card`.

- 2026-06-02 00:00:00 (UTC): implementada a etapa 0 do novo pipeline OPRM `com.marketinghub.oprm.nichocnae`, com orquestrador backend para selecionar o próximo candidato de nicho CNAE pendente por maior score, criar `oprm_routine_research_cycle`, marcar o candidato como `RESEARCH_RUNNING` e expor contratos internos para acompanhamento e início do ciclo.
- 2026-06-02 12:05:00 (UTC-3): iniciado no `oprm-coletor-mei` o submódulo `com.marketinghub.nichocnae` para a etapa 0 do pipeline de pesquisa de rotina do nicho CNAE, sem agendamento automático; criado núcleo genérico de pipeline por etapa, processor/cliente/controller manual do `oprmRoutineResearchOrchestrator` e teste ArchUnit para proteger o isolamento do núcleo conforme a nova metodologia.
- 2026-06-02 14:19:32 (UTC-3): reforçada a guarda ArchUnit do pipeline `nichocnae` no `oprm-coletor-mei`: o núcleo `pipeline` agora valida qualquer dependência direta contra etapas concretas, etapas concretas não podem depender de outras etapas, e classes `*Processor` de etapas devem implementar `StageProcessor`, protegendo inclusão/remoção de etapas contra acoplamento colateral.
- 2026-06-02 14:25:00 (UTC-3): documentada na metodologia de pipeline por etapas a necessidade obrigatória de etapas concretas plugáveis e removíveis, explicitando que etapas podem depender do núcleo/infra compartilhada permitida, mas não de outras etapas concretas, para permitir inclusão, remoção e substituição sem dano colateral.

- 2026-06-02 00:00:00 (UTC): implementada no coletor OPRM MEI a etapa 1 `oprmRoutineResearchCycle` no pacote `com.marketinghub.nichocnae`, com cliente backend, processor pelo `PipelineWorker`, endpoints manuais de acompanhamento/processamento e testes unitários sem agendamento automático.

- 2026-06-02 00:00:00 (UTC): adicionada navegação frontend do OPRM com botão `Pipeline` ao lado de `CNAEs` e criada a tela estática `/oprm/pipeline` com três cards iniciais das etapas Ingestão de Mercado, Score OPRM e Enriquecimento Comercial para preparar a evolução visual do pipeline sem acionar novos endpoints.

## 2026-06-02 — OPRM nichocnae etapa 2 seed builder no coletor MEI

- Implementada no `oprm-coletor-mei` a etapa `oprmNicheResearchSeedBuilder` no pacote `com.marketinghub.nichocnae.nicheresearchseedbuilder`, seguindo o padrão de etapa plugável do motor `pipeline`.
- A etapa lista ciclos pendentes no backend, chama a OpenAI Responses API com schema JSON estrito, valida seed e 12 a 15 queries específicas, persiste a conclusão no backend e registra falha operacional quando necessário.
- Expostos endpoints manuais do coletor em `/api/oprm-mei/nichocnae/niche-research-seed-builder` para listar pendências, detalhar ciclo e processar pendentes.
- Atualizada a documentação Swagger OPRM nichocnae com os contratos backend esperados para pending, complete, fail e detail da etapa dois.

- 2026-06-02 00:00:00 (UTC): evoluído o backend do OPRM `oprm/nichocnae` com a etapa 2 `oprmNicheResearchSeedBuilder`, incluindo persistência das tabelas `oprm_niche_research_seed` e `oprm_research_query`, endpoints internos de pending/complete/fail, endpoint de detalhe operacional, validação de 1 a 15 queries com objetivos permitidos e atualização do Swagger `docs/swagger/oprm-nichocnae-swagger.yaml`.
- 2026-06-02 00:00:00 (UTC): ajustada a tela `/oprm/pipeline` para remover os cards de ingestão/score/enriquecimento CNAE e apresentar os cards corretos do pipeline NichoCNAE: orquestrador de pesquisa, ciclo de pesquisa de rotina, seed de pesquisa, busca/coleta de fontes, extração de sinais, síntese da rotina e gate de qualidade.

- 2026-06-02 00:00:00 (UTC): evoluído o backend do OPRM `oprm/nichocnae` com a etapa 3 `oprmSourceSearcher`, incluindo persistência da tabela `oprm_source_candidate`, endpoints internos de pending/complete/fail para resultados de busca pública, endpoint de detalhe por ciclo, atualização de `oprm_research_query.result_count/status`, atualização de `oprm_routine_research_cycle.total_source_candidates` e documentação dos novos contratos no Swagger `docs/swagger/oprm-nichocnae-swagger.yaml`.
- 2026-06-02 00:00:00 (UTC): evoluído o backend do OPRM `oprm/nichocnae` com a etapa 4 `oprmSourceFetcher`, incluindo persistência da tabela `oprm_source_snapshot`, campos de seleção/relevância/rejeição em `oprm_source_candidate`, endpoints internos de pending/complete/fail para coleta curta de fontes, endpoint de detalhe por ciclo, atualização de `oprm_routine_research_cycle.total_source_snapshots` e documentação dos contratos no Swagger `docs/swagger/oprm-nichocnae-swagger.yaml`.

- 2026-06-03 00:00:00 (UTC): adicionado no `oprm-coletor-mei` o agendamento inicial do pipeline NichoCNAE para 03/06/2026 às 22h no fuso `America/Sao_Paulo`, disparando a etapa zero `oprmRoutineResearchOrchestrator` por `RoutineResearchOrchestratorService.runNext` com guarda de data para evitar reexecução anual acidental do mesmo cron.
- 2026-06-03 00:00:00 (UTC): ajustado o card da etapa `oprmRoutineResearchOrchestrator` na tela `/oprm/pipeline` para exibir os últimos 10 nichos processados com horário de criação do ciclo de pesquisa; backend expôs o endpoint `/api/oprm/nichocnae/routine-research-orchestrator/recent-processed?limit=10`, consultando `oprm_routine_research_cycle` em ordem decrescente de `started_at`, e o Swagger OPRM NichoCNAE foi atualizado para documentar o contrato.
- 2026-06-03 03:15:00 (UTC): ajustada a tela `/oprm/pipeline` para não duplicar a etapa zero `oprmRoutineResearchOrchestrator`: o acompanhamento dos últimos processados permanece no card operacional superior e a grade inferior passa a iniciar no ciclo de pesquisa de rotina. Também foi corrigida a consulta de prévia de pendências no backend para usar uma query sem bloqueio pessimista, evitando erro de transação read-only ao validar a fila antes do processamento.

- 2026-06-03 03:44:00 (UTC): adicionados logs diagnósticos na etapa zero do pipeline OPRM NichoCNAE (`oprmRoutineResearchOrchestrator`) no coletor e no backend, cobrindo carregamento do scheduler, acionamento do cron, chamada ao backend, seleção de candidato, criação do ciclo e atualização do status do candidato para investigar por que a tela `/oprm/pipeline` continua sem ciclos criados.

- 2026-06-03 03:55:00 (UTC): ajustado o agendamento inicial do pipeline OPRM NichoCNAE para executar hoje, 03/06/2026, às 04h no fuso `America/Sao_Paulo`, mantendo a guarda de data para bloquear reexecução anual acidental do mesmo cron.

- 2026-06-03 00:00:00 (UTC): canonizada a regra de execução do pipeline OPRM NichoCNAE para manter chamadas ao modelo dentro do próprio módulo executor OPRM (`oprm-coletor-mei`), seguindo `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md`, sem usar `ai-worker` para essas etapas e preservando etapas plugáveis por contratos oficiais.

- 2026-06-03 00:00:00 (UTC): adicionada inicialização agendada da etapa dois `oprmNicheResearchSeedBuilder` no próprio `oprm-coletor-mei`, com cron fixo a cada minuto, guarda local contra execução concorrente e chamada ao serviço de etapa que gera seed/queries por IA sem usar `ai-worker`.

- 2026-06-03 00:00:00 (UTC): ajustada a publicação do `oprm-coletor-mei` para o mesmo host do `ai-worker` (`191.252.120.96`) e configurada a etapa `oprmNicheResearchSeedBuilder` para ler a chave OpenAI pelo arquivo montado `/run/secrets/openai_api_key`, reutilizando o segredo já provisionado no host sem commitar credenciais.
- 2026-06-03 00:00:00 (UTC): diagnosticada a falha do ciclo #1 do pipeline OPRM NichoCNAE via MCP/banco: a etapa `oprmNicheResearchSeedBuilder` enviava ao backend um payload aninhado (`output.seed.nicheName`) enquanto o contrato do endpoint de conclusão exige campos achatados (`nicheName`, `businessType`, `queries` etc.), gerando erro `nicheName is required`. Corrigido o coletor para montar o DTO achatado do backend, converter a resposta para a saída interna do worker e ajustada a tela `/oprm/pipeline` para exibir `errorMessage` e `finishedAt` quando o ciclo estiver `FAILED`.
- 2026-06-03 00:00:00 (UTC): automatizada a recuperação da etapa `oprmNicheResearchSeedBuilder` após a correção do contrato achatado: o backend passa a recolocar na fila ciclos `FAILED` sem seed/queries cujo erro seja a falha retryável legada `nicheName is required` no endpoint de conclusão, e a conclusão bem-sucedida reabre o ciclo como `RUNNING`, limpando `finishedAt` e `errorMessage` para permitir avanço automático do pipeline sem intervenção manual no banco.
- 2026-06-03 00:00:00 (UTC): ajustada a tela `/oprm/pipeline` para expor no card da etapa 1 (`oprmRoutineResearchCycle`) erro de continuidade quando existe ciclo `RUNNING`, mas a consulta da fila da etapa seguinte `oprmNicheResearchSeedBuilder` falha; o card passa a destacar a etapa com borda de alerta e mensagem operacional apontando o erro retornado pelo endpoint de pendências do seed.

- 2026-06-03 00:00:00 (UTC): ajustada a tela `/oprm/pipeline` para deixar explícita a execução da etapa 1 (`oprmRoutineResearchCycle`) dentro do próprio card da etapa, exibindo ciclo, horário de criação, CNAE, score, status, finalização e erro da execução mais recente, além da validação de continuidade para a etapa 2.

- 2026-06-04 00:00:00 (UTC): diagnosticado via MCP/banco que o ciclo #1 do pipeline OPRM NichoCNAE continua `RUNNING`, mas a etapa `oprmNicheResearchSeedBuilder` já gerou e gravou o seed #1 com 15 queries `PENDING` em `oprm_niche_research_seed` e `oprm_research_query`; ajustada a tela `/oprm/pipeline` para consultar o endpoint público de detalhe da etapa 2 e exibir no card "Seed de Pesquisa do Nicho" os dados gerados pela IA, evitando que o usuário confunda ausência de pendência na fila com ausência de saída gerada.
- 2026-06-04 00:00:00 (UTC): criada tela de detalhe `/oprm/pipeline/niche-research-seed-builder/:researchCycleId` para o usuário auditar a etapa `oprmNicheResearchSeedBuilder`, mostrando a prévia da requisição enviada à OpenAI Responses API com prompt/schema e o JSON gerado/gravado com seed e queries; o card da etapa 2 no pipeline passou a direcionar para esse detalhe em vez de concentrar a auditoria completa no card.
- 2026-06-04 00:00:00 (UTC): ativada a execução automática da etapa 3 `oprmSourceSearcher` no `oprm-coletor-mei`, com scheduler, provedor público `DUCKDUCKGO_HTML`, conclusão/falha por query pendente via contratos do backend e resumo da última execução no card da tela `/oprm/pipeline`.

- 2026-06-04 00:00:00 (UTC): ajustado o cânone de arquitetura por etapa para restringir a obrigatoriedade dos subpacotes `recebePrompt` e `recebeResposta` às etapas que acessam modelos de IA; etapas determinísticas do pipeline OPRM, como buscadores/fetchers sem modelo, podem usar subpacotes por operação real (`pending`, `completeStageExecution`, `failStageExecution`, `detailStageExecution`) mantendo DTOs como records.

- 2026-06-04 17:15:00 (UTC): implementada a execução operacional da etapa 4 `oprmSourceFetcher` no `oprm-coletor-mei`, com scheduler próprio, cliente backend, processor plugável, coleta curta via Jsoup com logs do payload bruto recebido, persistência apenas de metadados/snippet/trecho curto pelo backend existente e testes unitários da etapa; a tela `/oprm/pipeline` passou a consultar e exibir o resumo real da Coleta de Fontes (candidatas, snapshots, status, última coleta e orientação de pendências).
- 2026-06-04 20:20:00 (UTC): implementação da etapa 5 `oprmSignalExtractor` do pipeline OPRM NichoCNAE. Criados modelo `oprm_extracted_signal`, status de extração em `oprm_source_snapshot`, endpoints backend internos/públicos, worker agendado no coletor OPRM, documentação Swagger e exibição dos sinais na tela `/oprm/pipeline`. Resultado esperado: transformar snapshots curtos da etapa 4 em sinais classificados de rotina, dores, perguntas, linguagem e oportunidades de mecanismo para sustentar a síntese da rotina.
- 2026-06-04 23:50:00 (UTC): implementação da etapa 6 `oprmRoutineSynthesizer` do pipeline OPRM NichoCNAE. Criados modelo `oprm_niche_routine_card`, endpoints backend internos/públicos de pendência/conclusão/falha/detalhe, worker agendado no `oprm-coletor-mei`, documentação Swagger e exibição do card sintetizado na tela `/oprm/pipeline`. Resultado esperado: transformar sinais extraídos em cartão de rotina com rotina, dores, resultados, oportunidades de mecanismo e evidências sem criar oferta ou campanha.

- 2026-06-04 00:00:00 (UTC): implementação da etapa 7 `oprmRoutineQualityGate` do pipeline OPRM NichoCNAE. Adicionados campos de decisão de qualidade ao `oprm_niche_routine_card`, endpoints backend de pendência/conclusão/falha/detalhe, worker agendado no `oprm-coletor-mei`, documentação Swagger e exibição da decisão na tela `/oprm/pipeline`. Resultado esperado: bloquear cards genéricos ou fracos e liberar para hipótese somente cartões com fontes, sinais, especificidade e confiança suficientes.
- 2026-06-05 00:00:00 (UTC): ajustada a tela `/oprm/pipeline` para exibir os status operacionais do pipeline OPRM em português, incluindo `LIGHTLY_RESEARCHED` como "Pesquisa inicial concluída", preservando os códigos técnicos apenas no contrato/backend e traduzindo também notas de qualidade exibidas ao usuário.

- 2026-06-05 00:00:00 (UTC): definido e implementado o fechamento do pipeline OPRM NichoCNAE com a etapa final `oprmEnrichedNicheMaterializer`, responsável por alimentar `market_niche` e a nova tabela `market_niche_enrichment_profile` a partir de cartões aprovados no gate de qualidade, sem criar hipótese ou experimento. Atualizados cânone OPRM, Liquibase, backend, coletor OPRM, Swagger e tela `/oprm/pipeline` para expor a materialização final.
- 2026-06-05 01:45:00 (UTC): ajustada a regra ArchUnit de isolamento do OPRM para manter o bloqueio geral contra dependências internas fora de `com.marketinghub.oprm` e `com.marketinghub.repository.jpa.oprm`, com exceção nominal e restrita para `BackendEnrichedNicheMaterializerService` acessar somente `MarketNiche`, `MarketNicheEnrichmentProfile`, `MarketNicheRepository` e `MarketNicheEnrichmentProfileRepository`, incluindo o teste unitário do próprio materializador para validar essa exceção específica.
- 2026-06-05 00:00:00 (UTC): adicionada navegação da lista de nichos para o perfil de nicho enriquecido materializado pelo OPRM. O backend passa a marcar cada nicho com `enrichedNicheProfileId`, o OPRM expõe detalhe público por perfil e o frontend mostra o link "Nicho enriquecido" somente nas linhas que possuem perfil materializado, abrindo uma tela dedicada com rotina, dores, resultados, mecanismos e evidências.
- 2026-06-05 15:40:00 (UTC): diagnosticada a falha de visualização da tela `/oprm/enriched-niches/profile/:profileId`: o perfil #1 existe no banco em `market_niche_enrichment_profile`, mas a chamada do detalhe usava URL relativa `/api/...` e podia receber o HTML do frontend em vez do JSON do backend quando acessada pelo Vite/Nginx na porta 5173. Ajustado o hook do OPRM para montar a URL absoluta via `buildApiUrl`, direcionando o detalhe do nicho enriquecido para o backend na porta 8000.
- 2026-06-05 17:15:00 (UTC): revisão do pipeline OPRM NichoCNAE com código como fonte básica para diagnosticar viés de solução/IA na pesquisa de rotina; criado o plano `docs/implementacao/oprm/plano-ajuste-pipeline-nichocnae-pesquisa-sem-vies.md` com diagnóstico por etapa, pontos de causa-raiz e fases de implementação para neutralizar entrada, prompt, queries, fontes, sinais, quality gate, materialização e tela.
- 2026-06-05 17:21:00 (UTC): revisado o plano `docs/implementacao/oprm/plano-ajuste-pipeline-nichocnae-pesquisa-sem-vies.md` para atender a decisão de escopo: o NichoCNAE não deve executar framework comercial nem preparar solução; a pesquisa deve focar apenas visão realista da rotina, tarefas, dificuldades, perguntas, linguagem e evidências públicas do nicho.
- 2026-06-05 17:30:00 (UTC): executada a Fase 0 do plano de ajuste do pipeline OPRM NichoCNAE sem viés de solução. O cânone OPRM passou a definir `ROUTINE_REALITY_RESEARCH` como modo obrigatório da pesquisa inicial, limitada a rotina, tarefas, dificuldades, perguntas, contexto, linguagem e evidências públicas; solução, produto, campanha, oferta e hipótese ficam em fluxo posterior separado.
- 2026-06-05 17:40:00 (UTC): executada a Fase 1 do plano de ajuste do pipeline OPRM NichoCNAE sem viés de solução. O backend passou a normalizar o nome operacional do ciclo, remover prefixos de solução como `IA para crescimento de`, persistir nome original, nome neutro, modo `ROUTINE_REALITY_RESEARCH` e score de risco de linguagem de solução; o coletor OPRM passou a receber esses campos nos DTOs de ciclo e registrar logs contextuais com `researchCycleId`, `sourceNicheId`, nomes original/neutro e modo da pesquisa.
- 2026-06-05 18:05:00 (UTC): executada a Fase 2 do plano de ajuste do pipeline OPRM NichoCNAE sem viés de solução. A etapa `oprmNicheResearchSeedBuilder` passou a gerar prompt, schema e validação focados somente em rotina, tarefas, dificuldades, perguntas, linguagem e contexto operacional; objetivos comerciais/produto/oferta foram removidos do contrato aceito e queries com termos de solução sem presença literal no CNAE passaram a ser rejeitadas no coletor e no backend.
- 2026-06-05 20:25:00 (UTC): executada a Fase 3 do plano de ajuste do pipeline OPRM NichoCNAE sem viés de solução. A etapa `oprmSourceSearcher` passou a classificar fontes públicas por intenção antes da persistência, priorizar relatos de rotina, perguntas reais e guias práticos, registrar escore de evidência de rotina e marcar páginas comerciais/linguagem de solução como risco de contaminação para impedir que virem base principal da pesquisa.

- 2026-06-05 21:30:00 (UTC): criado o contrato oficial do pipeline OPRM NichoCNAE (`oprm-nicho-cnae-pipeline`) com nove etapas baseadas no código backend/coletor e seed operacional via Liquibase para aparecer na tela de pipelines como fluxo oficial, preservando a separação canônica entre pesquisa de rotina, gate de qualidade e materialização de nicho enriquecido.
- 2026-06-05 00:00:00 (UTC): executada a etapa 4 do plano de ajuste NichoCNAE sem viés de solução. A etapa `oprmSourceFetcher` agora propaga a classificação da etapa de busca para snapshots curtos (`sourceIntent`, `routineEvidenceScore`, `commercialPageRisk`, `solutionLanguageRisk`), com colunas próprias em fonte candidata e snapshot, contrato backend/coletor atualizado, Swagger sincronizado e política de não persistir HTML completo preservada.
- 2026-06-05 00:00:00 (UTC): corrigida a causa-raiz da falha de bootstrap Liquibase do pipeline oficial OPRM NichoCNAE: o campo Java `requiresOpenAiModel` agora aponta explicitamente para a coluna canônica `requires_openai_model`, e um changelog prévio remove/renomeia a coluna legada `requires_open_ai_model` criada por auto-DDL para impedir erro de `NOT NULL` sem default ao inserir etapas oficiais.

- 2026-06-05 00:00:00 (UTC): executada a fase 5 do plano de ajuste do pipeline NichoCNAE sem viés. A etapa 6 passou a sintetizar cartão focado em rotina observada, dificuldades, perguntas, contexto operacional, linguagem, evidências e alertas de contaminação por solução; a etapa 7 passou a aprovar pela suficiência de rotina/dificuldade/perguntas-fontes e a bloquear domínio de linguagem de IA/software, persistindo scores próprios de evidência de rotina, dificuldade, diversidade de fontes e risco de solução.

## 2026-06-05 — Correção de include Liquibase para scores de qualidade da rotina OPRM

- Ajustado o changelog master do backend para resolver o changeset `2026-06-05-oprm-routine-quality-scores.yaml` de forma relativa ao arquivo master, evitando falha de bootstrap quando o Liquibase não encontra `changesets/...` no search path configurado.
- 2026-06-06 00:00:00 (UTC): executada a fase 6 do plano de ajuste NichoCNAE sem viés: materialização passou a usar nome neutro no `market_niche`, preservar nome original apenas na descrição de auditoria, não preencher campos comerciais de promessa/oferta/gatilhos/objeções na etapa inicial de rotina, expor diagnóstico de registros históricos com linguagem de solução e ampliar a tela `/oprm/pipeline` com nome original, nome neutro, modo de pesquisa, mix de objetivos/fontes, risco de linguagem de solução e motivo do gate.
- 2026-06-06 03:29:11 (UTC): reforçada a etapa 7 `oprmRoutineQualityGate` do pipeline NichoCNAE para bloquear aprovação quando houver linguagem textual de solução no cartão mesmo sem contador persistido e para exigir evidências auditáveis com múltiplas fontes, mantendo aprovação baseada apenas em rotina, dificuldades, perguntas/linguagem e baixo risco de contaminação.
- 2026-06-06 00:00:00 (UTC): executada a etapa 8 do plano de ajuste NichoCNAE sem viés, reforçando a materialização final para usar nome neutro no `market_niche`, preservar nome original apenas para auditoria, gravar modo de pesquisa e scores de rotina/dificuldade/diversidade/risco no perfil enriquecido, e impedir que o campo legado de oportunidades eternize conteúdo de solução quando o cartão não trouxer contexto operacional compatível.

- 2026-06-06 17:30:00 (UTC): corrigida falha de persistência dos candidatos OPRM após o refactor do pipeline NichoCNAE: novos `oprm_niche_candidate` criados pelo enriquecimento CNAE agora nascem com `routine_research_status = PENDING`, evitando erro MySQL `Column 'routine_research_status' cannot be null` e permitindo que a etapa `oprmRoutineResearchOrchestrator` selecione o candidato para pesquisa de rotina. Também foi ampliado o registro de falhas da etapa `oprmNicheResearchSeedBuilder` para preservar detalhe técnico/cause raiz enviado pelo coletor no `error_message` do ciclo, reduzindo mensagens genéricas como “Falha ao gerar seed da etapa dois OPRM nichocnae.”.
- 2026-06-06 21:02:50 (UTC): adicionada opção de reprocessar CNAE com falha na tela `/oprm/pipeline`; o backend expõe endpoint para liberar o candidato de ciclo `FAILED` voltando `routine_research_status` para `PENDING`, permitindo que o orquestrador automático crie um novo ciclo, e o Swagger OPRM NichoCNAE foi atualizado com o contrato.

- 2026-06-07 00:00:00 (UTC): a tela geral de pipelines passou a exibir, para o pipeline oficial OPRM NichoCNAE, o módulo executor `oprm-coletor-mei`, os pacotes backend `com.marketinghub.oprm.nichocnae.*` e os pacotes correspondentes do coletor `com.marketinghub.nichocnae.*`, com metadados agregados no contrato `/api/pipelines/metadata`.
- 2026-06-07 03:45:00 (UTC): corrigida a causa-raiz do botão `Reprocessar CNAE` na tela `/oprm/pipeline`: antes ele apenas devolvia o candidato para `PENDING`, mas não havia agendamento recorrente da etapa zero para abrir novo ciclo imediatamente. O endpoint de reprocessamento agora cria um novo `oprm_routine_research_cycle` em status `RUNNING`, marca o candidato como `RESEARCH_RUNNING`, atualiza `last_routine_research_cycle_id` e mantém o Swagger/testes alinhados ao comportamento real esperado pelo usuário.
- 2026-06-07 04:56:52 (UTC): corrigida a causa-raiz da nova falha da etapa dois `oprmNicheResearchSeedBuilder` no pipeline OPRM NichoCNAE: a validação de termos de solução não usa mais `Set.of(...)` sobre tokens vindos da IA/CNAE, evitando `duplicate element: e` quando palavras comuns aparecem repetidas na query; adicionada regressão unitária para payload com conectivo repetido e normalização determinística de duplicidade de `queryText`.
- 2026-06-07 05:04:19 (UTC): verificado via MCP que o erro `duplicate element: e` ocorreu no ciclo reprocessado `oprm_routine_research_cycle.id=3`, criado após solicitação de reprocessamento do CNAE `9602501`; a solicitação de reprocessamento foi gatilho operacional, mas a causa-raiz técnica estava no validador do `oprm-coletor-mei` (`NicheResearchSeedBuilderValidator`) usando `Set.of(...)` sobre tokens repetidos. O coletor foi corrigido para tokenizar com `HashSet`, mantendo a correção espelhada no backend como defesa de contrato.
- 2026-06-07 05:08:00 (UTC): melhorada a rastreabilidade do reprocessamento OPRM NichoCNAE: novos ciclos criados pelo endpoint `Reprocessar CNAE` passam a gravar `trigger_source=MANUAL_REPROCESS` em vez de `AUTO_SCORE_QUEUE`, permitindo distinguir gatilho manual de fila automática no histórico recente e reduzir ambiguidade na investigação de falhas futuras.
- 2026-06-07 19:45:00 (UTC): reforçada a regra Brasil-first do pipeline OPRM NichoCNAE. A etapa de seed agora orienta queries em português do Brasil e fontes brasileiras, a busca pública acrescenta marcador Brasil e usa região `br-pt`, e a tela `/oprm/pipeline` passa a oferecer novo ciclo manual também para `NEEDS_MORE_RESEARCH`/`GENERIC`, não apenas para `FAILED`, permitindo o usuário sair de pesquisa insuficiente sem avançar material fraco para hipótese/oferta.
- 2026-06-07 20:20:00 (UTC): verificada a tela administrativa de pipelines para o pipeline oficial OPRM NichoCNAE; causa-raiz identificada no contrato persistente/metadata, que marcava todas as nove etapas como consumidoras de modelo OpenAI. Corrigido o contrato para expor seleção de modelo apenas na etapa `niche-research-seed-builder`, alinhando registry, DTO de metadata, sincronizador persistente, frontend e changelog incremental para reparar `pipeline_stage_definition.requires_openai_model`.
- 2026-06-07 20:35:00 (UTC): reforçada a correção das flags OpenAI do pipeline OPRM NichoCNAE para usar o código como fonte de verdade: adicionada regressão que lê os pacotes Java reais de cada etapa no `oprm-coletor-mei` e compara o uso direto de OpenAI com `requiresOpenAiModel`, impedindo que a tela administrativa volte a exibir modelo OpenAI em etapa que não usa IA no código.
- 2026-06-08 00:00:00 (UTC): corrigida a causa-raiz da contaminação por linguagem de solução na etapa 5 `oprmSignalExtractor` do pipeline OPRM NichoCNAE. O extrator deixou de transformar termos de organização/controle/processo em `MECHANISM_OPPORTUNITY` com texto de automação/IA e passou a registrar apenas `CONTEXT_MARKER` operacional, com regressão unitária para impedir que termos isolados de solução criem sinal de mecanismo antes da aprovação da rotina.
- 2026-06-08 00:00:00 (UTC): reforçada a correção de causa-raiz da etapa 5 `oprmSignalExtractor`: termos explícitos de solução encontrados na fonte (`ia`, automação, sistema, software, app, ferramenta ou curso) agora geram `SOLUTION_LANGUAGE_RISK`, e não `MECHANISM_OPPORTUNITY`, mantendo a evidência como risco operacional auditável em vez de sugestão positiva de mecanismo.
- 2026-06-08 00:00:00 (UTC): adicionada orientação local no `oprm-coletor-mei/AGENTS.md` para tornar obrigatória a investigação de causa-raiz antes de corrigir sintomas em pipelines/status/scores/gates/materializações, incluindo rastreamento de origem do dado, diferenciação entre fonte externa/IA/regra determinística/UI e tratamento de contaminação por linguagem de solução como risco auditável em vez de sinal positivo.

- 2026-06-08 00:00:00 (UTC): ajustada a tela `/oprm/pipeline` para mostrar, em cada execução recente de CNAE, a etapa atual inferida pelo status do ciclo. O status `ROUTINE_SYNTHESIZED` passou a aparecer como “Rotina sintetizada” e a linha informa ao usuário que o ciclo está na etapa 7 aguardando o gate de qualidade, evitando interpretação de status técnico cru como erro.
- 2026-06-08 20:36:20 (UTC): ajustada a tela `/oprm/pipeline` para explicitar o ponto do problema quando um ciclo exige ação do usuário. Status `NEEDS_MORE_RESEARCH` e `GENERIC` agora indicam que o bloqueio aconteceu na etapa 7 `oprmRoutineQualityGate`, `ENRICHED_NICHE_FAILED` indica a etapa 8 de materialização, e falhas genéricas continuam exibindo a mensagem técnica do ciclo para investigar a etapa exata.
- 2026-06-09 00:00:00 (UTC): ajustada a tela administrativa de pipelines para exibir um indicador visual "IA" com ícone nos cards de etapas que usam modelo OpenAI, facilitando distinguir rapidamente etapas automatizadas por IA das etapas determinísticas no fluxo OPRM/Pipelines.
- 2026-06-09 00:00:00 (UTC): ajustado o fluxo da tela `/oprm/pipeline` para permitir que ciclos `ENRICHED_NICHE_FAILED` sejam refeitos pelo próprio front-end, sem orientar acesso direto ao banco; o backend passou a aceitar esse status no endpoint de reprocessamento e a UI exibe detalhe operacional da falha de materialização com ação “Refazer pelo front-end”.
- 2026-06-09 03:30:00 (UTC): corrigida a causa-raiz da falha `NullPointerException` na etapa 8 `oprmEnrichedNicheMaterializer`: o coletor não usa mais `List.of(...)` com frases opcionais que podem ser descartadas por conter linguagem de solução, evitando quebrar a materialização de um card já aprovado e reduzindo a necessidade de criar novo ciclo completo quando só a materialização final falhou.
- 2026-06-09 00:00:00 (UTC): adicionada opção de download Markdown na tela do nicho enriquecido OPRM, com endpoint backend que consolida ciclo, seed, queries, fontes candidatas, snapshots, sinais, gate de qualidade e conclusão final materializada do perfil enriquecido.

- 2026-06-09 00:00:00 (UTC): criado plano de implementação para redirecionar o pipeline OPRM NichoCNAE ao público-alvo de profissionais MEI/autônomos brasileiros, priorizando comportamento, rotina, dores, sonhos, medos, linguagem, fontes recentes e arquitetura do projeto, sem avançar para produto/oferta nesta fase.
- 2026-06-09 00:00:00 (UTC): executada a etapa 1 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: o cânone OPRM agora diferencia CNAE de público-alvo MEI/autônomo, prioriza o profissional brasileiro que executa o trabalho, exige entendimento de comportamento/rotina/dores/sonhos/linguagem, reforça Brasil-first e bloqueia avanço para produto, oferta, campanha ou promessa comercial nessa fase.
- 2026-06-09 00:00:00 (UTC): executada a etapa 2 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: mapeados backend, coletor OPRM, tabelas via MCP, tela `/oprm/pipeline`, telas de detalhe, endpoints e testes acoplados; produzido diagnóstico indicando que a próxima mudança precisa começar pelo contrato persistente de perfil de público-alvo para evitar misturar CNAE/nicho com produto, oferta ou empresa estruturada.
- 2026-06-09 00:00:00 (UTC): executada a etapa 3 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: avaliado que `market_niche_enrichment_profile` mistura materialização de nicho/hipótese com campos comerciais e exige `market_niche_id`, então foi criado o contrato persistente `oprm_mei_audience_profile` para representar o público-alvo MEI/autônomo com rastreabilidade por ciclo, cartão, candidato, CNAE e scores de aderência/risco sem avançar para produto, oferta ou campanha.
- 2026-06-09 00:00:00 (UTC): ajustado o contrato backend do perfil MEI/autônomo para aderir ao padrão de arquitetura do backend: entidade movida para a raiz funcional `oprm.nichocnae.meiaudienceprofile`, repository movido para a subdivisão OPRM equivalente, DTOs reorganizados por operação em `service.upsertAudienceProfile` e `service.detailAudienceProfile`, e criado service único `BackendMeiAudienceProfileService` para orquestrar gravação/detalhe sem criar controllers ou endpoints fora do escopo da etapa.
- 2026-06-09 00:00:00 (UTC): executada a etapa 4 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: a etapa `oprmNicheResearchSeedBuilder` passou a orientar e validar queries sobre o profissional MEI/autônomo brasileiro, trabalhador por conta própria e dono-operador, cobrindo rotina, aquisição de clientes, atendimento, cobrança, agenda, materiais, entrega, retrabalho, dores emocionais, sonhos, medos, canais, linguagem real pt-BR e atualidade de fontes; o backend, schema, Swagger e tela de detalhe foram alinhados aos novos `queryGoal` compatíveis e ao bloqueio de pesquisa genérica ou direcionada a solução/produto/oferta/IA.
- 2026-06-09 00:00:00 (UTC): reforçada a etapa 4 do redirecionamento OPRM NichoCNAE para MEI/autônomo com testes de regressão no coletor e no backend garantindo que queries sem marcador explícito de público MEI/autônomo ou sem contexto Brasil/pt-BR sejam bloqueadas antes de avançar no pipeline.
- 2026-06-09 00:00:00 (UTC): executada a etapa 5 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: a etapa `oprmSourceSearcher` passou a classificar fontes por atualidade, relevância Brasil-first, evidência de MEI/autônomo, risco de fonte antiga e risco de desvio para empresa estruturada; o backend persiste e propaga esses indicadores até o snapshot curto da etapa `oprmSourceFetcher`, mantendo HTML completo fora do contrato final e permitindo que fontes antigas/corporativas sejam auditadas como risco em vez de verdade principal.
- 2026-06-09 00:00:00 (UTC): executada a etapa 6 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: foi feita avaliação técnica/jurídica de redes sociais e comunidades públicas, bloqueando scraping social amplo e definindo que a futura etapa opcional `social-behavior-searcher` só poderá operar com fonte pública, mecanismo permitido, estabilidade, logs de ingestão do payload bruto e persistência exclusiva de sinais agregados de comportamento/linguagem, sem dados pessoais e sem produto/oferta.
- 2026-06-10 00:00:00 (UTC): executada a etapa 7 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: criada a etapa `oprmMeiAudienceSegmenter` após a síntese de rotina para transformar sinais, fontes recentes, indicadores e cartão de rotina em perfil comportamental MEI/autônomo persistido em `oprm_mei_audience_profile`; o backend expõe pendência/conclusão/falha, o coletor processa e valida o perfil sem produto/oferta/campanha/solução, e o gate de qualidade passa a aguardar a segmentação antes de avaliar o cartão.
- 2026-06-10 00:00:00 (UTC): criado plano de implementação para inserir públicos gerais no Marketing Hub como fluxo OPRM separado do NichoCNAE, usando sementes, subnichos, ângulos, quality gate e conversão controlada para nicho/hipótese/experimento sem criar CNAEs artificiais nem sobrepor o pipeline atual.
- 2026-06-10 00:00:00 (UTC): executada a etapa 8 do plano de redirecionamento NichoCNAE para público MEI/autônomo. A extração de sinais do coletor OPRM passou a classificar modo de trabalho autônomo, aquisição de clientes, canais, dores práticas/emocionais, sonhos, medos, status, pressão de tempo, instabilidade de renda, reputação, preço e cancelamentos com evidência curta; a síntese de rotina passou a persistir e expor blocos comportamentais separados no backend, mantendo compatibilidade com os campos legados e bloqueando promoção de linguagem de solução/oferta.
- 2026-06-10 03:41:06 (UTC): executada a etapa 9 do plano de redirecionamento NichoCNAE para MEI/autônomo. O gate `routine-quality-gate` passou a exigir fontes brasileiras recentes, aderência ao dono-operador MEI/autônomo, sinais de comportamento humano, aquisição/canal, dor prática e dor emocional/sonho/medo, além de bloquear fontes antigas, conteúdo corporativo demais e contaminação por solução/produto/oferta antes da materialização.

## 2026-06-10 — OPRM Públicos Gerais: cadastro de sementes

- Implementada a etapa 1 do plano de públicos gerais sem sobreposição CNAE: cadastro, listagem, detalhe, atualização e arquivamento manual de sementes de público geral no backend.
- O fluxo permanece separado do NichoCNAE e não cria nicho, hipótese, experimento ou campanha automaticamente.
- 2026-06-10 00:00:00 (UTC): executada a etapa 10 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: o backend passou a expor o detalhe do perfil final aprovado em `/api/oprm/nichocnae/mei-audience-profiles/research-cycles/{researchCycleId}`, liberando para MDS, MOIS e estratégia apenas perfis com gate `MEI_AUDIENCE_READY`, preservando rastreabilidade por ciclo/cartão/CNAE/scores e bloqueando payload final contaminado por linguagem de produto, oferta, campanha ou solução.
- 2026-06-10 00:00:00 (UTC): executada a etapa 11 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: a tela `/oprm/pipeline` passou a mostrar foco da pesquisa, nome neutro, público MEI/autônomo identificado, scores de aderência/atualidade, riscos de fonte antiga e empresa estruturada, status/justificativa do gate, queries, fontes recentes/antigas e sinais de rotina, dores, sonhos, medos e canais; o backend passou a enriquecer o histórico recente com esses campos para manter a UI simples e orientada à decisão.
- 2026-06-10 00:00:00 (UTC): executada a etapa 2 do plano de públicos gerais sem sobreposição com CNAE: o backend passou a persistir e expor subnichos derivados de sementes gerais em tabela própria `oprm_general_audience_subniche`, com endpoints OPRM para listar, cadastrar, detalhar, revisar, aprovar e rejeitar subnichos sem gravar esses públicos nas tabelas OPRM CNAE nem tratá-los como campanha pronta.

## 2026-06-10 — Frontend operacional de públicos gerais OPRM

- Implementada a fase 3 do plano de públicos gerais sem sobreposição CNAE: navegação interna OPRM com item **Públicos Gerais**, tela de listagem/cadastro de sementes, detalhe da semente com subnichos derivados e detalhe do subnicho com mapa de dores, linguagem, aprovação e rejeição.
- Mantida a separação arquitetural entre Público Geral e NichoCNAE: a tela explicita a origem `Público Geral`, não grava dados como CNAE e deixa conversão para `MarketNiche`/experimento bloqueada para a fase futura prevista no plano.

## 2026-06-10 — Pipeline de descoberta de públicos gerais OPRM

- Implementada a fase 4 do plano de públicos gerais sem sobreposição CNAE: criado o pipeline oficial `OPRM_GENERAL_AUDIENCE_DISCOVERY`, com etapas de revisão de semente, descoberta de subnichos, mapeamento de dores, construção de ângulos seguros, quality gate e brief de experimento.
- Criadas as tabelas próprias de dores/ângulos e evidências agregadas, mantendo públicos gerais fora das tabelas CNAE e persistindo apenas evidência resumida e rastreável.
- Adicionados endpoints OPRM para cadastrar/listar/revisar/aprovar ângulos, registrar evidências agregadas e executar quality gate bloqueando saída genérica ou promessa arriscada antes de avanço comercial.

- 2026-06-10 14:03:12 (UTC): corrigida a causa-raiz do erro de teste em `BackendRoutineResearchOrchestratorServiceTest`: o serviço passou a depender dos repositórios de perfil MEI e card de rotina para montar o histórico recente, mas o teste ainda não injetava mocks dessas dependências, gerando `NullPointerException` antes da validação do contrato.
- 2026-06-10 00:00:00 (UTC): executada a etapa 5 do plano de públicos gerais sem sobreposição CNAE: o backend passou a converter subnichos gerais aprovados em `MarketNiche` por endpoint OPRM controlado, carregando apenas nome, descrição, segmentação base, interesses/demografia sugeridos e vínculo `market_niche_id`, sem consultar ou alterar tabelas CNAE e sem criar hipótese, experimento ou campanha automaticamente.
- 2026-06-10 00:00:00 (UTC): executada a etapa 6 do plano de públicos gerais sem sobreposição CNAE: o backend passou a criar hipótese específica a partir de ângulo de dor aprovado, exigindo subnicho já convertido em `MarketNiche`, usando o modelo “Acreditamos que...” com dor, isca/promessa segura e mecanismo percebido, sem criar experimento, campanha ou venda direta automaticamente.
- 2026-06-10 00:00:00 (UTC): executada a etapa 7 do plano de públicos gerais sem sobreposição CNAE: o backend passou a criar experimento planejado de lead/isca a partir de ângulo aprovado, exigindo hipótese, público/subnicho, dor principal, isca, promessa segura, pergunta qualificadora, métrica principal, stop-loss de CPL, duração curta e orçamento pequeno, sem publicar campanha ou iniciar venda direta automaticamente.
- 2026-06-10 14:18:00 (UTC): executada a etapa 12 do plano de redirecionamento OPRM NichoCNAE para MEI/autônomo: reforçados testes de prevenção de recorrência no backend e no coletor para objetivos de query, bloqueio de solução, persistência completa do perfil comportamental, gate MEI/autônomo coerente, schema estrito da segmentação, fontes antigas e desvio corporativo; o Swagger do perfil aprovado foi documentado explicitamente para deixar claro que o contrato final não expõe produto, oferta ou campanha.

## 2026-06-10 — OPRM Públicos Gerais: targeting inicial conservador

- Executada a etapa 8 do plano de públicos gerais sem sobreposição CNAE: criado preparo de targeting inicial próprio para Público Geral a partir de ângulo aprovado, combinando cargos/termos, interesses, comportamentos, frase de triagem do criativo e confirmação na landing sem consultar nem alterar tabelas CNAE.
- Mantido o modo conservador do publicador atual: o backend só marca o pacote como publicável quando existe `JOB_TITLE` aprovado e resolvido com identificador da Meta; interesses e comportamentos entram apenas como enriquecimento e o endpoint retorna bloqueios explícitos para impedir ad set amplo puro.

## 2026-06-10 — OPRM Públicos Gerais: landing/formulário de confirmação

- Executada a etapa 9 do plano de públicos gerais sem sobreposição CNAE: criado endpoint OPRM para materializar fluxo de Lead Portal como landing/formulário de confirmação a partir de ângulo aprovado, exigindo subnicho convertido, experimento, pergunta qualificadora e opções de triagem.
- A landing/formulário passa a explicitar para quem é, qual dor resolve, o que a pessoa recebe, por que faz sentido, próximo passo e duas perguntas obrigatórias: pertencimento ao público e confirmação textual da dor, sem publicar campanha ou oferta final automaticamente.
- 2026-06-10 15:05:00 (UTC): implementada a etapa de sinais iniciais Meta Ads para nichos enriquecidos OPRM NichoCNAE. Ao concluir a materialização final, o backend agora gera interesses, cargos e comportamentos por CNAE, grava esses sinais nas listas do `market_niche` e publica `targeting_element` aprovado com origem `OPRM_NICHE`; o Facebook Ads Worker passa a consumir esses elementos pendentes e devolver ID oficial e faixa de alcance da Meta (`audience_size_lower_bound`/`audience_size_upper_bound`) para decisão de campanha.
- 2026-06-10 17:27:59 (UTC): adicionada navegação do perfil de nicho enriquecido OPRM para a nova tela de hipótese, passando o `marketNicheId` pela rota `/niches/{nicheId}/hypotheses/new`; a tela de nova hipótese foi simplificada temporariamente como espaço em branco/placeholder para construção guiada nos próximos passos.
- 2026-06-10 17:30:00 (UTC): registrada e aplicada a regra de separação OPRM x Facebook Ads: o OPRM deixa de materializar/acessar targeting diretamente e passa a registrar no backend dados de público para coleta posterior pelo Facebook Ads; também foi criado o armazenamento `oprm_general_audience_facebook_ads_data` para sinais de público geral sem acoplamento ao módulo de targeting.

## 2026-06-10 — OPRM Públicos Gerais: leitura de qualidade do público

- Executada a etapa 10 do plano de públicos gerais sem sobreposição CNAE: o backend passou a registrar leituras de qualidade real por subnicho, medindo sinais bons além de CTR/CPL — profissão correta, dor real, pedido de material, resposta no WhatsApp e pergunta de preço/próximo passo.
- A leitura também registra sinais ruins — público fora do perfil, curiosos sem profissão, baixo preenchimento, promessa confusa e lead que baixa a isca sem responder — calculando score, bloqueios e recomendações antes de qualquer escala comercial.
- O quality gate de públicos gerais agora considera a leitura mais recente, preservando a causa-raiz comercial: público amplo só avança quando os leads demonstram aderência e intenção real, não apenas clique barato.
- 2026-06-10 17:35:00 (UTC): corrigida a falha de compilação do serviço de sinais Meta Ads do nicho enriquecido OPRM, adicionando o import explícito de `java.util.Locale` usado na normalização pt-BR segura do texto operacional.
- 2026-06-10 17:46:10 (UTC): corrigida a falha de compilação do teste `OprmGeneralAudienceDiscoveryServiceTest`, alinhando os mocks ao construtor atual do serviço de Públicos Gerais (`OprmGeneralAudienceFacebookAdsDataRepository`) e atualizando a asserção do bloqueio de targeting conservador para a mensagem operacional vigente.

## 2026-06-10 — Correção de limite arquitetural do OPRM em confirmação de landing

- Decisão registrada: o OPRM deve apenas tratar a situação de confirmação e gravar os dados em banco próprio, sem criar fluxo no Lead Portal nem alterar Experimento diretamente.
- Ajuste técnico: a confirmação de público geral passou a persistir um registro OPRM próprio em `oprm_general_audience_landing_confirmation`.
- Prevenção de recorrência: o cânone OPRM agora explicita que `com.marketinghub.oprm..` não deve importar services, DTOs, entidades ou repositories de módulos externos para materializar etapas posteriores.

## 2026-06-10 — OPRM Públicos Gerais: correção da URL padrão do backend no frontend

- Corrigida a causa-raiz de ações do fluxo de Públicos Gerais parecerem travadas quando o frontend era acessado em `:5173`: a URL padrão do backend no frontend apontava para `:8000`, porta que pode recusar conexão nesse ambiente, enquanto o backend público responde pelo mesmo host na porta 80.
- Mantido suporte a `VITE_API_URL` para ambientes que precisem sobrescrever a URL do backend.

## 2026-06-10 — OPRM NichoCNAE: remoção do bloqueio por marcador literal Brasil/pt-BR no seed

- Removido do validador da etapa `niche-research-seed-builder` o bloqueio determinístico que rejeitava queries sem marcador literal `Brasil`/`pt-BR`, pois ele gerou falso negativo para frases em português com contexto brasileiro, como `cidades brasileiras`.
- Mantida a validação obrigatória de marcador de público MEI/autônomo, a rejeição de query genérica e o bloqueio de linguagem de solução antes da persistência do seed.
- Atualizado o teste de regressão para garantir que uma query com nicho e público MEI/autônomo continue válida mesmo sem o marcador literal Brasil/pt-BR.
- 2026-06-10 22:30:00 (UTC): corrigida a experiência operacional da tela `/oprm/general-audiences/subniches/:id` após aprovação de subnicho: a aprovação já registrada agora fica clara para o usuário, e a próxima ação real disponível passa a ser a conversão controlada do subnicho aprovado em MarketNiche, evitando a percepção de botão travado quando o backend já gravou `APPROVED_FOR_EXPERIMENT`.

## 2026-06-11 — OPRM NichoCNAE: confiança no modelo na etapa de seed e queries

- Decisão operacional registrada: a etapa `niche-research-seed-builder` deve confiar no modelo para o conteúdo semântico das queries e não deve travar o ciclo por marcador literal de MEI/autônomo, Brasil/pt-BR, objetivo fechado, quantidade fixa, duplicidade textual, linguagem de solução ou genericidade.
- Ajuste técnico: o validador do `oprm-coletor-mei`, o schema da Responses API, o preview do frontend e a validação do backend foram reduzidos ao mínimo persistível: estrutura, ciclo correto, seed obrigatório, lista não vazia de queries e campos de query exigidos pelo banco.
- Causa-raiz tratada: falsos negativos em português natural estavam bloqueando uma etapa preparatória que deve apenas criar seed e perguntas de pesquisa para as etapas seguintes comprovarem com fontes, sinais, síntese e gate.

## 2026-06-11 — OPRM NichoCNAE: remoção efetiva dos bloqueios restantes e etapa da falha

- Corrigida a causa-raiz do erro recorrente `Query sem marcador de MEI/autônomo brasileiro` no ciclo de rotina: a etapa `niche-research-seed-builder` deixou de executar o validador bloqueante no coletor e o backend passou a gravar seed/queries com defaults quando algum campo estrutural vier ausente.
- A tela `/oprm/pipeline` agora identifica a etapa exata da falha a partir da mensagem técnica do ciclo, mostrando `Etapa 2 · Seed de Pesquisa do Nicho` quando o erro vem da geração de seed.
- Prevenção de recorrência: falhas por formato incompleto do modelo não travam mais a criação da pesquisa; as etapas seguintes continuam responsáveis por buscar fontes, extrair sinais e filtrar qualidade.

## 2026-06-11 — OPRM NichoCNAE: alinhamento do teste do prompt da etapa 2

- Corrigida a causa-raiz da falha do teste `NicheResearchSeedBuilderPromptBuilderTest`: o teste ainda exigia marcadores literais de fonte Brasil/domínio `.br` e objetivos antigos, enquanto a regra operacional atual da etapa 2 passou a confiar no modelo e não bloquear por marcador literal.
- O teste agora valida o comportamento correto do prompt: pesquisa de rotina real, proibição de solução/oferta/produto/ferramenta, ausência de exigência de marcadores literais e prevenção de metadado técnico no texto funcional.

## 2026-06-11 — OPRM NichoCNAE: correção sem truncamento para `query_goal` longo

- Corrigida a causa-raiz do erro de banco `Data too long for column 'query_goal'` sem truncar campos e sem ampliar o banco: o schema JSON da etapa `niche-research-seed-builder` agora declara `maxLength` nos campos gravados em colunas curtas, fazendo o modelo responder dentro do contrato persistível.
- A fila da etapa 2 considera como reprocessáveis as falhas sem seed/queries causadas pelo contrato legado (`nicheName is required`) ou pelo estouro de `query_goal`, permitindo recuperação após o deploy da correção.
- Adicionados testes de regressão para garantir que o schema solicite ao modelo limites compatíveis com `query_text`, `query_goal`, `source_group`, `created_by`, `niche_name`, `business_type` e `confidence_level`, sem corte silencioso no backend.

## 2026-06-12 — OPRM NichoCNAE: materializador com campos comerciais não-ofertivos

- Atualizado o materializador de nicho enriquecido para preencher gatilhos comportamentais e objeções prováveis a partir de evidências do cartão aprovado, usando dores, resultados, comportamento do cliente, canais e linguagem pública sem criar produto, promessa, campanha ou landing page.
- Causa-raiz tratada: os campos `commercialTriggers` e `objections` permaneciam nulos porque a etapa final preservava somente contexto operacional, deixando sinais comerciais observáveis sem registro mesmo quando havia evidência suficiente.
- Prevenção de recorrência: adicionados testes para garantir preenchimento determinístico em perfis aprovados e ausência de linguagem técnica/de solução nos campos comerciais sintetizados.

## 2026-06-12 — OPRM Rotina: apresentação por blocos de valor

- A tela de rotina do OPRM passou a organizar as tarefas e sinais em blocos de valor: antes do atendimento, durante o atendimento, depois do atendimento, administração entre clientes, aquisição/fidelização, dores e riscos observados e oportunidades de produto.
- A apresentação prioriza leitura comercial para identificar dores vendáveis e evita repetir listas genéricas como “gerenciar rotina e agenda”, mantendo fallback compatível com o payload atual e com campos mais específicos que o backend passar a entregar.
- O pipeline NichoCNAE também passou a exibir um resumo dos blocos de valor na etapa de síntese, ajudando o usuário a validar rapidamente se a rotina tem sinais úteis para produto digital antes de seguir para oferta.
- 2026-06-11 21:35:00 (UTC-3): aprimorado o gate de qualidade de rotina do `oprm-coletor-mei` para bloquear cards que repetem a frase genérica "Gerenciar rotina de atendimento e agenda do nicho", exigir tarefas concretas distintas no `routineSummary`, penalizar rotina limitada a gestão/agenda/atendimento/organização e impedir hipótese quando a rotina não revela tarefas reais do executor. Adicionados testes de regressão para rotina genérica repetida e rotina concreta de manicure/cabeleireiro com evidência suficiente.

## 2026-06-12 — OPRM NichoCNAE: busca por rotina executada

- Ajustada a etapa de seed para orientar o modelo a priorizar queries sobre rotina executada, tarefas do dia a dia, procedimentos práticos, CBO, guias profissionais e relatos de profissionais antes de dores genéricas ou temas comerciais.
- Reforçado o classificador de fontes da etapa de busca para aumentar score de execução prática e penalizar páginas dominadas por software, agenda online, app, automação ou sistema quando não descrevem tarefas concretas do executor.
- Atualizada a ordenação de fontes candidatas para colocar evidência de rotina prática antes de sinais secundários, reduzindo o risco de fontes comerciais virarem base da análise OPRM.
- 2026-06-12 00:00:00 (UTC): ajustado o gate de qualidade OPRM NichoCNAE para diferenciar rotina operacional suficiente de nicho comercialmente acionável; aquisição, canais, recorrência e comportamento de clientes agora precisam de evidência útil, e placeholders como “Sem evidência suficiente” bloqueiam avanço com `NEEDS_MORE_MEI_RESEARCH`.

## 2026-06-12 — OPRM NichoCNAE: sinais de rotina com tarefas específicas

- Corrigida a causa-raiz da perda de especificidade na etapa cinco do `oprm-coletor-mei`: evidências públicas com ações concretas de manicure/cabeleireiro agora geram sinais `ROUTINE_TASK` com verbo e objeto de trabalho, como esterilizar alicates, lixar/cutilar/esmaltar unhas, lavar/cortar/finalizar cabelo, preparar química/hidratação e confirmar/remarcar horários pelo WhatsApp.
- Prevenção de recorrência: quando uma rotina específica é extraída, o coletor deixa de substituir o sinal pela frase genérica de agenda/atendimento, preservando valor operacional para dor, resultado, mecanismo e oferta nas etapas posteriores.
- 2026-06-11 21:27:42 (UTC-3): ajustada a síntese da etapa seis OPRM NichoCNAE para priorizar sinais comerciais específicos (`CUSTOMER_ACQUISITION_BEHAVIOR`, `CHANNEL_USAGE`, `COMMERCIAL_TASK`) antes de equivalentes genéricos, impedindo que aquisição e canais fiquem residuais no cartão de rotina. Quando aquisição ou canais não têm evidência própria, o cartão passa a orientar nova pesquisa específica antes de venda, evitando materialização comercial pobre. Adicionado teste de regressão validando presença de aquisição/canais nos resumos e orientação de nova pesquisa para blocos vazios.

## 2026-06-12 — OPRM NichoCNAE: prompt sem aconselhamento de marketing

- Corrigida a causa-raiz da falha do teste `NicheResearchSeedBuilderPromptBuilderTest`: o prompt da etapa 2 já restringia aquisição a comportamento operacional, mas não trazia a instrução explícita esperada para impedir que as queries virassem aconselhamento de marketing.
- Prevenção de recorrência: a frase de bloqueio ficou diretamente no prompt enviado ao modelo, mantendo a etapa focada em rotina real do MEI/autônomo e não em campanha, funil, anúncio, oferta ou estratégia de venda.

## 2026-06-12 — OPRM NichoCNAE: persona da IA orientada a marketing

- Ajustada a instrução inicial da etapa de seed para apresentar a IA como especialista em Marketing e Comportamento do Consumidor no Digital, evitando linguagem interna como construtor/executor de pipeline na requisição exibida ao usuário.
- Causa-raiz tratada: a tela reconstruía corretamente a requisição, mas o contrato textual da etapa ainda expunha nomenclatura operacional interna, reduzindo clareza de negócio para validação do usuário.
- Prevenção de recorrência: adicionado teste garantindo presença da persona de marketing/comportamento e ausência das expressões técnicas antigas no prompt da etapa.

## 2026-06-12 — OPRM Pipeline: indicação de nicho já materializado

- A tela `/oprm/pipeline` passou a diferenciar ciclos que já possuem `market_niche` associado, exibindo o badge “Nicho já existe” e priorizando a ação “Abrir nicho existente”.
- O endpoint de últimos ciclos processados agora informa `existingMarketNicheId` e `alreadyMaterialized`, evitando que a interface induza o usuário a criar um novo nicho quando o ativo comercial já foi materializado.
- Causa-raiz tratada: a tabela mostrava o ciclo apenas como execução do pipeline e não levava para o nicho operacional já criado, gerando ambiguidade entre atualizar/consultar um nicho existente e criar novamente.

## 2026-06-12 — OPRM: regra de arquitetura preservada na materialização

- Corrigida a causa-raiz da falha ArchUnit no OPRM sem afrouxar a regra de arquitetura: removido o `record` interno que fazia o materializador final parecer uma nova classe dependente de `MarketNiche` fora da exceção nominal.
- A etapa final de materialização agora resolve o nicho existente por CNAE/nome neutro com variáveis locais e salva explicitamente o `market_niche` antes de criar o perfil enriquecido, mantendo a indicação correta de criação versus atualização.
- A consulta usada pela etapa zero para indicar nicho já materializado foi movida para o repositório OPRM, evitando dependência direta do serviço OPRM em repositórios/classes do pacote de nicho e mantendo a UI informando “nicho já existe” sem violar o isolamento modular.
- 2026-06-12 00:12:11 (UTC-3): ajustada a tela de CNAEs por Score OPRM para transformar o nome/descrição do nicho em link para `/oprm/cnaes/:cnaeCode`, com criação de tela placeholder de detalhe do nicho CNAE para evolução futura do fluxo de análise de nicho.

## 2026-06-12 — Reprocessamento de perfil enriquecido OPRM

- Ajustada a regra canônica do NichoCNAE para permitir múltiplos registros em `market_niche_enrichment_profile` para o mesmo nicho quando houver reprocessamento operacional.
- A materialização final passa a reaproveitar o `market_niche` existente e criar um novo perfil enriquecido rastreável, preservando histórico em vez de bloquear pela materialização anterior.
- Criado changelog incremental para remover unicidade rígida por `routine_card_id` e `research_cycle_id` e manter índices não únicos de consulta.

## 2026-06-12 — OPRM NichoCNAE: diagrama de dados

- Documentado o diagrama de dados do pipeline OPRM NichoCNAE em `docs/relatorios/oprm-nichocnae-diagrama-dados.md`, cobrindo base CNAE/CNPJ, candidatos, ciclo de pesquisa, seed, queries, fontes, snapshots, sinais, cartão de rotina, perfil MEI/autônomo, `market_niche` e `market_niche_enrichment_profile`.
- A análise foi validada contra o schema real via MCP e registrou que parte dos vínculos OPRM é lógica por `*_id`, com FK física confirmada entre `market_niche_enrichment_profile.market_niche_id` e `market_niche.id`.

## 2026-06-12 — OPRM NichoCNAE: reinício manual completo por CNAE

- Alterada a regra operacional do botão de execução manual por CNAE: quando o usuário solicita novo ciclo para um CNAE específico, o backend encerra automaticamente todos os ciclos ainda abertos desse CNAE e cria uma execução completamente nova.
- Causa-raiz tratada: o endpoint manual só aceitava candidatos pendentes, então CNAEs já em `RESEARCH_RUNNING` devolviam 404 e impediam nova execução mesmo quando a decisão operacional era recomeçar do zero.
- Prevenção de recorrência: a regra foi registrada no cânone OPRM, o contrato Swagger foi atualizado e foi criado teste garantindo que ciclos antigos abertos sejam marcados como `CANCELLED_BY_MANUAL_RESTART` antes da criação do novo ciclo.

## 2026-06-12 — OPRM NichoCNAE: detalhe por card do CNAE

- A tela de detalhe do CNAE ganhou botão “Ver detalhes” em cada card do pipeline, abrindo uma página separada para não poluir a visão principal.
- A nova página informa as tabelas populadas por etapa, o tipo de conteúdo gravado, os dados retornados pelo backend e se a etapa acessa modelo de IA.
- Para etapas com IA, a página exibe request/response conceitual, modelo configurado e deixa explícito quando tokens e custo ainda não estão persistidos no detalhe operacional.
- 2026-06-12 00:00:00 (UTC): diagnosticada parada do ciclo OPRM NichoCNAE #20 na etapa `oprmNicheResearchSeedBuilder`: endpoint backend operacional em `http://191.252.181.168/api/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/pending` retorna o ciclo, enquanto a porta `:8000` responde indisponível para o coletor e os logs publicados não exibem o boot do scheduler `Scheduler da etapa dois OPRM NichoCNAE carregado`. Corrigido o default do `oprm-coletor-mei` para usar a URL operacional sem porta, adicionado teste de carregamento do pacote `com.marketinghub.nichocnae` e criada proteção backend para marcar ciclos `RUNNING` sem progresso como `STALLED`, evitando tela com execução aparentemente saudável quando o pipeline estiver parado.

## 2026-06-12 — OPRM NichoCNAE: fila MEI/autônomo por ciclo elegível

- Ajustada a fila da etapa MEI/autônomo para expor somente cartões do ciclo mais recente realmente elegível em `ROUTINE_SYNTHESIZED`, sem puxar ciclos falhos, cancelados por reinício manual, já segmentados ou antigos substituídos por execução mais nova.
- Causa-raiz tratada: a consulta anterior olhava apenas a ausência de perfil MEI/autônomo e podia reenfileirar cartões históricos ou descartados, consumindo IA em ciclos que não faziam mais parte do fluxo ativo de venda.
- Prevenção de recorrência: adicionada revalidação no serviço da fila e teste de regressão garantindo que somente o ciclo `ROUTINE_SYNTHESIZED` mais recente aparece para processamento.

## 2026-06-12 — Correção do modelo e telemetria OpenAI da etapa seed NichoCNAE

- causa-raiz: a tela `/pipelines` gravava o modelo da etapa `niche-research-seed-builder` no pipeline operacional, mas o coletor OPRM usava apenas a propriedade local `gpt-4.1-mini`; além disso, o contrato de conclusão da etapa descartava `usage` da OpenAI, impedindo cálculo e exibição de tokens/custo.
- correção aplicada:
  - o backend passou a enviar na pendência da etapa seed o modelo configurado no pipeline oficial/pipeline operacional;
  - o coletor OPRM passou a priorizar esse modelo recebido do backend na chamada à OpenAI Responses API;
  - a conclusão da etapa passou a enviar e persistir modelo, resposta bruta, tokens de entrada/saída, `openAiResponseId` e custo estimado;
  - a tela de detalhe da etapa passou a exibir a telemetria real quando persistida.
- verificação operacional: consulta via MCP confirmou que a etapa operacional `niche-research-seed-builder` está configurada com `gpt-5.4` no banco.

## 2026-06-12 — OPRM NichoCNAE: isolamento arquitetural da etapa seed

- Removida a dependência direta do serviço OPRM `BackendNicheResearchSeedBuilderService` em pacotes `openai` e `pipeline`.
- Criado contrato próprio da etapa seed para consultar modelo configurado e estimar custo, com implementação fora do pacote funcional OPRM para preservar o limite arquitetural validado pelo ArchUnit.
- Causa-raiz tratada: a etapa OPRM consumia diretamente serviços e repositórios compartilhados para dados auxiliares de IA, rompendo a regra de isolamento do módulo.
- Prevenção de recorrência: os testes da etapa passaram a mockar a abstração OPRM, mantendo o serviço de negócio sem imports proibidos.

## 2026-06-12 — OPRM NichoCNAE: status visual do detalhe CNAE

- Ajustada a tela de detalhe do CNAE para inferir conclusão das etapas do pipeline pelos contadores reais do ciclo: queries, fontes candidatas, snapshots coletados e sinais extraídos.
- Causa-raiz tratada: o status `FAILED` do ciclo pai era exibido como falha do card inicial, escondendo a etapa operacional provável da quebra e obrigando o usuário a abrir detalhes técnicos.
- Prevenção de recorrência: a mensagem de erro do ciclo agora aparece junto ao status atual e direciona a falha provável para o card compatível, incluindo `mei-audience-segmenter` como falha da etapa 7. MEI.

## 2026-06-12 — OPRM MEI: barreira operacional para chave OpenAI da segmentação

## 2026-06-12 — OPRM MEI: chave OpenAI da etapa de segmentação de audiência

- Configurado o ambiente Docker do `oprm-coletor-mei` para expor explicitamente `OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY_FILE` apontando para o segredo OpenAI montado em `/run/secrets/openai_api_key`.
- Causa-raiz tratada: a etapa `mei-audience-segmenter` já esperava chave própria ou fallback compartilhado no `application.yml`, mas os manifests do serviço só configuravam a chave da etapa seed, deixando a segmentação MEI sem credencial garantida no ambiente de execução.
- Prevenção de recorrência: o modelo da etapa também ficou parametrizado nos manifests, mantendo paridade operacional entre seed e segmentação MEI/autônomo.

## 2026-06-12 — OPRM NichoCNAE: status visual de bloqueio no detalhe do CNAE

- Ajustada a tela de detalhe do CNAE para não inferir “em execução” quando o ciclo já foi bloqueado pelo gate de qualidade com status como `OUTDATED_SOURCES`, `NEEDS_MORE_RESEARCH`, `NEEDS_MORE_MEI_RESEARCH`, `TOO_CORPORATE`, `SOLUTION_CONTAMINATED` ou `GENERIC`.
- Causa-raiz tratada: a tela usava apenas contadores do ciclo e `finishedAt` nulo para decidir a próxima etapa visual, então um ciclo já reprovado por qualidade aparecia como “Síntese em execução”.
- Prevenção de recorrência: adicionada mensagem de negócio explicando o bloqueio e teste de regressão garantindo que “Fontes antigas” aparece no gate de qualidade, a síntese aparece concluída e a materialização fica bloqueada.

## 2026-06-12 — OPRM NichoCNAE: correção da marcação IA do pipeline

- Corrigida a validação anterior: a etapa `mei-audience-segmenter` também acessa diretamente a OpenAI para segmentar o perfil comportamental MEI/autônomo e precisa aparecer no contrato oficial do pipeline.
- Causa-raiz tratada: a etapa MEI foi criada no fluxo operacional e no detalhe do CNAE, mas não foi incorporada ao contrato administrativo `oprm-nicho-cnae-pipeline`, deixando a tela `/pipelines` com apenas uma etapa IA e com nove etapas em vez das dez executadas no fluxo real.
- Correção aplicada: o cânone, enum oficial, teste de contrato e changelog incremental foram ajustados para incluir `mei-audience-segmenter` entre a síntese e o gate, marcada com `requires_openai_model = true`.
- Ajuste visual complementar: a mensagem de telemetria do detalhe da etapa deixou de citar apenas a etapa seed quando a tela está exibindo a segmentação MEI.

## 2026-06-13 — OPRM NichoCNAE: notas estruturadas do gate de qualidade

- Alterado o detalhe do gate de qualidade para expor `qualityNotes` como objeto JSON chave/valor, mantendo a gravação legada em texto auditável no banco e estruturando o contrato apenas na resposta de detalhe.
- Ajustada a tela de detalhe do pipeline para exibir os scores completos do gate e destacar em vermelho os indicadores que não atenderam aos limites de aprovação.
- Causa-raiz tratada: a nota do gate era uma string única com pares `chave=valor`, dificultando leitura pela tela e impedindo sinalização visual objetiva dos critérios reprovados.

## 2026-06-13 — OPRM CNAE: desligamento padrão dos ciclos automáticos

- Desligados por padrão os ciclos agendados de score CNAE (`CNAE_SCORE`) e enriquecimento CNAE (`CNAE_ENRICHMENT`) no `oprm-coletor-mei`, mantendo possibilidade de religar por configuração operacional explícita quando houver atualização real da base.
- Causa-raiz tratada: a base CNAE/mercado não muda com frequência suficiente para justificar execuções recorrentes, que geravam ciclos vazios e ruído operacional na tela de acompanhamento.
- Prevenção de recorrência: o cânone OPRM foi atualizado para definir scheduler sob demanda operacional e o teste de contexto garante que os schedulers ficam ausentes por padrão.

## 2026-06-13 — OPRM NichoCNAE: independência da fila de coleta entre ciclos

- Corrigida a fila interna da etapa 4 (`source-fetcher`) para listar apenas fontes `FOUND` de ciclos `RUNNING` ainda sem `finishedAt`, impedindo que pendências residuais de ciclos falhados entrem antes do ciclo atual.
- Causa-raiz tratada: a etapa de coleta buscava candidatas pendentes globalmente por status da fonte, sem validar o status do ciclo pai, permitindo que o ciclo #26 `FAILED` represasse a coleta do ciclo #27.
- Prevenção de recorrência: o cânone OPRM passou a exigir independência operacional entre ciclos NichoCNAE e o teste da etapa valida o filtro por ciclo ativo.

## 2026-06-13 — OPRM NichoCNAE: Flex Processing e erro claro na OpenAI da etapa seed

- Verificado que a etapa `niche-research-seed-builder` chamava a OpenAI Responses API sem `service_tier`, portanto não estava forçando o modo Flex no payload da requisição.
- Ajustado o coletor OPRM para enviar `service_tier=flex` por padrão na etapa seed, com configuração operacional `OPRM_NICHO_CNAE_SEED_BUILDER_OPENAI_SERVICE_TIER` para manter rastreabilidade e flexibilidade futura.
- Causa-raiz tratada na observabilidade: a mensagem persistida começava apenas com “Falha ao gerar seed”, escondendo do usuário que a falha veio da OpenAI; agora a exceção operacional começa com “Falha na OpenAI ao gerar seed da etapa dois OPRM nichocnae”.
- Prevenção de recorrência: adicionado teste de regressão garantindo que o payload enviado contém `service_tier=flex` e que falhas de transporte como `Broken pipe` preservam mensagem explícita de OpenAI.
- 2026-06-13 02:05:00 (UTC): desligados os ciclos automáticos OPRM CNAE_SCORE e CNAE_ENRICHMENT no `oprm-coletor-mei`; a causa-raiz dos ciclos recorrentes era a presença de `@Scheduled` nos executores de score/enriquecimento e de catch-up por `ApplicationReadyEvent` no enriquecimento. Os métodos de execução foram preservados para acionamento manual/controlado, mas sem disparo automático por cron ou inicialização da aplicação.

## 2026-06-13 — OPRM NichoCNAE: ícone IA nos cards de etapa

- Ajustada a tela de detalhe do CNAE para marcar visualmente os cards das etapas que acessam diretamente IA no pipeline NichoCNAE e aplicar fundos leves por status operacional.
- Foram sinalizadas as etapas `Seed` e `MEI`, alinhadas ao cânone OPRM que define apenas `niche-research-seed-builder` e `mei-audience-segmenter` como consumidoras diretas de modelo OpenAI configurável.
- Causa-raiz tratada: a tela operacional mostrava o estado da execução, mas não diferenciava rapidamente as etapas que dependem de IA nem criava hierarquia visual suficiente entre concluído, execução, bloqueio/falha e fila, dificultando o diagnóstico de falhas e decisões operacionais.

## 2026-06-13 — OPRM MEI: uso do modelo configurado no pipeline

- Causa-raiz corrigida: a etapa `mei-audience-segmenter` era marcada como IA configurável na tela de pipeline, mas o backend não enviava o modelo configurado na pendência da etapa e o coletor usava o fallback local `gpt-4.1-mini`.
- Correção aplicada: o backend agora lê o modelo configurado na configuração oficial/legada do pipeline OPRM NichoCNAE e envia `openAiModelCode/openAiModelName` para o coletor; o coletor passa a priorizar esse modelo ao montar a chamada para a OpenAI.
- Prevenção de recorrência: adicionados testes cobrindo a fila backend da segmentação MEI e a resolução de modelo no cliente OpenAI do coletor, além de remover da tela de detalhe o fallback visual fixo que sugeria `gpt-4.1-mini` quando não havia telemetria persistida.
- 2026-06-13 15:03:26 (UTC): registrada no cânone OPRM a regra de limite máximo de 4000 caracteres para campos textuais sintéticos persistidos no pipeline NichoCNAE, incluindo `mechanismOpportunitiesSummary`; quando houver geração por IA, o prompt deve declarar esse limite e a camada determinística deve validar ou compactar antes do envio ao backend.
- 2026-06-13 15:18:00 (UTC): ampliada a regra operacional do cartão de rotina OPRM NichoCNAE para usar limite de 20000 caracteres em campos textuais sintéticos persistidos em `LONGTEXT`, corrigindo a causa-raiz do bloqueio do ciclo #33 sem truncar evidência útil antes da etapa de segmentação MEI/autônomo.

## 2026-06-13 — OPRM MEI audience segmenter: falhas OpenAI acionáveis

- Origem comprovada: a etapa `meiaudiencesegmenter` encapsulava falhas da OpenAI com uma mensagem genérica, fazendo o backend persistir apenas `Falha ao segmentar público MEI/autônomo com OpenAI.` e removendo contexto operacional essencial.
- Correção escolhida: a mensagem de falha agora inclui tipo da exceção, causa-raiz, `researchCycleId`, `routineCardId`, modelo OpenAI, endpoint chamado e, em falhas HTTP, status e corpo resumido da resposta. Também foi adicionado teste cobrindo o envio dessa mensagem ao endpoint de falha do backend.

## 2026-06-13 — OPRM CNAE: remoção definitiva dos schedulers automáticos

- Removidas as classes `OprmCnaeOpportunityScheduler` e `OprmCnaeEnrichmentScheduler` do `oprm-coletor-mei`, eliminando a possibilidade de religar os ciclos CNAE_SCORE e CNAE_ENRICHMENT por variável de ambiente.
- Removidas as configurações `oprm.cnae-opportunity.scheduler.enabled`, `oprm.cnae-enrichment.scheduler.enabled` e `oprm.cnae-enrichment.startup-catch-up.enabled` do `application.yml` para não deixar contrato operacional ambíguo.
- Causa-raiz tratada: manter os executores como beans condicionais permitia que uma configuração externa ou imagem antiga reativasse os ciclos vazios, gerando ruído operacional na tela OPRM.
- Prevenção de recorrência: o teste de contexto agora valida que as propriedades antigas não existem e que as classes antigas de scheduler CNAE não entram mais no artefato do coletor.

## 2026-06-13 — Recomendação de negócio em falhas NichoCNAE

- Atualizada a tela de detalhe do CNAE e a tela de pipeline OPRM para transformar falhas por contaminação de solução ou ausência de evidência de dor em uma recomendação operacional objetiva.
- A interface agora orienta reprocessamento com foco em rotina/público, informa quando não há dor operacional suficiente e sugere subnicho operacional quando existe evidência de rotina.
- O comando principal para esses casos foi padronizado como **Reprocessar com subnicho operacional**.

## 2026-06-13 — OPRM NichoCNAE quebra CNAE amplo em subnichos vendáveis

- 2026-06-13 00:00:00 (UTC): adicionada etapa complementar dentro do `oprmNicheResearchSeedBuilder` para ler CNAE, descrição, score OPRM e volume MEI antes de gerar o seed; o prompt agora exige gerar mentalmente 3 a 7 subnichos operacionais focados em MEI/autônomo, pontuar recorrência, urgência da dor, capacidade de pagar, clareza do resultado e compatibilidade com produto digital, e rodar as próximas pesquisas apenas sobre o subnicho vencedor. O backend passou a enviar o volume MEI mais recente do CNAE para o coletor, preservando o pipeline profundo no seed mais promissor.

## 2026-06-13 — Seed OPRM com queries comerciais-operacionais

- Solicitação: ajustar a etapa `oprmNicheResearchSeedBuilder` para gerar buscas mais comerciais e operacionais, evitando dependência excessiva de CBO, tabelas salariais e páginas institucionais.
- Causa-raiz: o prompt anterior ainda priorizava rotina ocupacional, procedimentos, CBO e guias profissionais como primeiras buscas, o que tendia a capturar descrições oficiais da profissão em vez de sinais de venda, agenda, cobrança, materiais, retrabalho e relatos reais do autônomo.
- Correção aplicada: o prompt da etapa 2 passou a exigir cinco famílias de queries — aquisição por WhatsApp/Instagram/indicação; agenda/faltas/remarcações/clientes que somem; precificação/cobrança/pacotes/recorrência; materiais/tempo/retrabalho; relatos reais em fóruns/vídeos/comentários/perguntas frequentes — e rebaixou CBO/tabelas salariais/páginas institucionais para apoio secundário.
- Defesa backend: o fallback de query padrão também passou a usar foco comercial-operacional para não voltar ao padrão ocupacional quando o modelo retornar lista vazia.

## 2026-06-13 — OPRM NichoCNAE: bloqueio de segmentação MEI sem dor prática

- Causa-raiz corrigida: a fila da etapa `mei-audience-segmenter` podia expor cartões sintetizados com pontuação zero ou texto explícito de ausência de evidência, permitindo avanço sem dor operacional concreta.
- Correção aplicada: antes de expor a pendência, o backend valida pontuações e textos essenciais do `oprm_niche_routine_card`; cartões sem evidência mínima bloqueiam o ciclo como `NEEDS_MORE_RESEARCH` com o motivo “cartão sem evidência mínima de dor prática”.
- Prevenção de recorrência: o cânone OPRM passou a exigir esse gate antes da segmentação MEI/autônomo e o teste unitário cobre cartão vazio que não entra na fila.

## 2026-06-13 — OPRM MEI: bloqueio de incentivo indireto na segmentação

- Causa-raiz tratada: a etapa `mei-audience-segmenter` já bloqueava parte da linguagem de solução, mas o prompt e a pré-validação não reforçavam explicitamente todos os termos de contaminação indireta, como software, IA, automação e ferramenta, antes do envio ao backend.
- Correção aplicada: o prompt passou a limitar a saída a perfil comportamental (quem é, como trabalha, como consegue clientes, dores, medos, linguagem, canais e evidências), sem criar produto, sugerir solução ou mencionar oferta, software, IA, automação ou ferramenta.
- Prevenção de recorrência: o coletor agora pré-valida a resposta com a mesma lista de termos proibidos, regenera uma única vez com instrução corretiva quando detectar contaminação e só registra falha se a resposta corrigida continuar inválida.

## 2026-06-13 — OPRM MEI: correção de compilação no gate de dor prática

- Causa-raiz corrigida: o gate de evidência mínima da etapa `mei-audience-segmenter` passou a usar `Locale.ROOT` para normalizar textos de ausência de evidência, mas a classe não importava `java.util.Locale`, quebrando a compilação do `ads-service`.
- Correção aplicada: adicionado o import explícito de `java.util.Locale` no serviço backend da segmentação MEI/autônomo.
- Prevenção de recorrência: executada compilação Maven direcionada ao módulo `ads-service` para validar que o erro `cannot find symbol Locale` não retorna.

## 2026-06-14 — OPRM NichoCNAE: desbloqueio do gate de qualidade após segmentação MEI/autônomo

- Diagnosticado que o ciclo #39 do CNAE 9602501 estava com status `MEI_AUDIENCE_SEGMENTED`, cartão de rotina e perfil MEI/autônomo criados, mas sem pendência retornada para a etapa de qualidade.
- Causa-raiz corrigida no backend: a fila do gate de qualidade aplicava limite antes de filtrar cartões com perfil MEI/autônomo, permitindo que cartões antigos sem perfil ocupassem a página e escondessem o ciclo elegível atual.
- Ajustada a consulta do repositório para filtrar no banco apenas cartões não avaliados que já possuem perfil MEI/autônomo, prevenindo nova parada silenciosa do pipeline na etapa de qualidade.

## 2026-06-14 — OPRM NichoCNAE: CNAE como fonte inicial e subnicho vencedor

- Decisão operacional registrada: no pipeline NichoCNAE, o CNAE passa a ser usado apenas como fonte inicial de descoberta e auditoria; o objetivo da etapa de seed passa a ser escolher um subnicho específico vencedor, não criar ou materializar o nicho amplo.
- Ajuste técnico: o prompt da etapa `niche-research-seed-builder` reforça que o modelo deve comparar 3 a 7 subnichos por recorrência, urgência da dor, capacidade de pagar, clareza do resultado e compatibilidade com produto digital, gravando em `seed.nicheName` apenas o subnicho vencedor com público, contexto operacional e dor/resultado observável.
- Prevenção de recorrência: o backend bloqueia conclusão da etapa quando a IA retorna o CNAE amplo como nome do nicho e atualiza o ciclo para que as etapas profundas posteriores pesquisem e materializem o subnicho vencedor.

## 2026-06-14 — OPRM NichoCNAE: pré-gate comercial antes da pesquisa profunda

- Implementada a sugestão de pré-gate comercial na etapa `niche-research-seed-builder`: antes de persistir queries para busca/coleta/extração profundas, o backend valida se o seed cobre recorrência, urgência da dor, capacidade de pagar, clareza do resultado, compatibilidade com produto digital e famílias mínimas de query sobre dor, pagamento/cobrança/preço, resultado, aquisição/fidelização e evidência pública.
- Ajuste técnico: o prompt passou a orientar a IA a executar esse pré-gate antes de gerar queries profundas e a explicitar os critérios em `initialAssumptions`; o backend falha cedo quando a cobertura mínima não existe, evitando gasto de execução completa em subnicho fraco.
- Prevenção de recorrência: adicionados testes para garantir bloqueio de seed sem cobertura comercial e permanência do comportamento de aplicar o subnicho vencedor ao ciclo.

## 2026-06-14 — OPRM NichoCNAE: seleção de evidências sem contaminação de solução

- Causa-raiz tratada: ciclos do CNAE 9602501 chegavam ao gate de qualidade com sinais suficientes, mas ainda dominados por linguagem de solução/produto, porque a geração de queries e a seleção de fontes não priorizavam com força suficiente rotina manual, atendimento real, aquisição/fidelização/recorrência, dores humanas e linguagem do executor.
- Correção aplicada: o seed agora pede explicitamente busca por execução manual e relato real; o classificador de fontes amplia termos de rotina, atendimento, dores e recorrência, aumenta penalização de apps/software/automação/curso/template e a etapa de busca ordena primeiro fontes com evidência humana-operacional.
- Prevenção de recorrência: adicionados testes para garantir que relatos reais do profissional sejam priorizados e que páginas de solução continuem rebaixadas como risco antes da coleta e síntese.

## 2026-06-14 — OPRM NichoCNAE: card de erro com rejeições detalhadas

- Causa-raiz tratada: o card de bloqueio do detalhe do CNAE informava que o gate rejeitou o ciclo, mas não explicava claramente quais critérios práticos foram reprovados para orientar a ação do usuário.
- Correção aplicada: o card agora mostra o resultado apurado do gate e lista situações rejeitadas em linguagem operacional, incluindo contaminação por solução, fontes antigas, desvio corporativo, falta de tarefas reais do executor, mix MEI/autônomo incompleto, aquisição/recorrência fraca e ausência de dor prática.
- Prevenção de recorrência: teste de tela cobre a exibição das rejeições detalhadas no bloqueio por qualidade.

## 2026-06-14 — OPRM NichoCNAE: custos por execução na tela do CNAE

- Solicitação atendida: a tela de detalhe do CNAE passou a mostrar o custo do job atual, o custo total acumulado do CNAE em destaque e uma tabela final com todos os jobs executados e o custo total de cada execução.
- Causa-raiz tratada: o endpoint de acompanhamento do pipeline já listava os ciclos, mas não carregava a telemetria financeira gravada pela etapa de IA, impedindo decisão operacional sobre gasto antes de escalar o nicho.
- Correção aplicada: o backend passou a somar o custo registrado nos seeds por ciclo e a devolver também o acumulado do CNAE; o frontend passou a exibir esses valores em USD no resumo e no histórico.

## 2026-06-14 — OPRM NichoCNAE: separação entre fonte de rotina e fonte de solução

- Implementada a sugestão de separar fonte de rotina de fonte de solução na pesquisa NichoCNAE: fonte de rotina continua alimentando coleta, snapshot e síntese; fonte de solução/oferta passa a ficar preservada apenas como risco auditável.
- Causa-raiz tratada: páginas públicas com linguagem de ferramenta, app, software, curso, automação, template, funil ou promessa podiam chegar como `FOUND` quando não eram domínio comercial explícito, permitindo gasto de coleta e risco de transformar resposta vendida pelo mercado em evidência da dor.
- Correção aplicada: o classificador amplia a marcação de linguagem de solução; o backend grava essas fontes como `CONTAMINATION_RISK`, rebaixa o escore de rotina e a etapa de fetch filtra e bloqueia qualquer candidata com risco comercial ou de solução.
- Prevenção de recorrência: adicionados testes para solução pública sem marketplace, persistência como risco e bloqueio defensivo antes do snapshot curto.

## 2026-06-14 — OPRM NichoCNAE: medição de dor vendável no gate de qualidade

- Implementada a sugestão de medir dor vendável, não apenas rotina existente, na etapa `routine-quality-gate`.
- Causa-raiz tratada: cartões podiam comprovar tarefas e rotina real, mas ainda não demonstrar uma dor com força comercial suficiente para sustentar produto digital, porque o gate media principalmente evidência operacional, aquisição/canais e ausência de contaminação.
- Correção aplicada: o gate passou a calcular `dorVendavelScore` combinando urgência, recorrência, impacto em dinheiro/tempo, tentativa operacional de resolver e resultado desejado; quando o score fica abaixo do mínimo, o ciclo volta para `NEEDS_MORE_MEI_RESEARCH` antes de hipótese/oferta.
- Prevenção de recorrência: adicionados testes para aprovar rotina com dor forte e bloquear rotina existente com dor fraca ou genérica, mantendo a decisão explicável nas notas do gate.

## 2026-06-14 — OPRM NichoCNAE: próximo movimento automático após reprovação

- Implementada a sugestão de decidir automaticamente o próximo movimento quando o gate de qualidade reprovar um ciclo.
- Causa-raiz tratada: a reprovação já protegia o pipeline contra nichos fracos, contaminados ou sem dor vendável, mas ainda exigia interpretação manual para decidir se a próxima ação era refazer busca sem solução, buscar fontes recentes, trocar foco para dono-operador, validar aquisição/canais, validar dor vendável ou completar o mix MEI/autônomo.
- Correção aplicada: o gate agora grava `proximoMovimentoCodigo` e `proximoMovimento` nas notas de qualidade, com decisão determinística por causa dominante; a tela de detalhe do CNAE exibe o próximo movimento automático no bloqueio de qualidade.
- Prevenção de recorrência: testes unitários validam os movimentos automáticos para aprovação e para reprovações por solução, fontes antigas, desvio corporativo, aquisição/canais fracos e dor vendável fraca.

## 2026-06-14 — OPRM NichoCNAE: aprendizado automático entre ciclos reprovados

- Implementada propagação automática do aprendizado do gate de qualidade anterior para a etapa de seed do novo ciclo.
- Causa-raiz tratada: reprocessamentos podiam repetir o mesmo tipo de erro — solução contaminada, fontes antigas, desvio corporativo, aquisição/canais fracos ou dor vendável fraca — porque o novo seed não recebia a causa dominante da reprovação anterior como restrição operacional.
- Correção aplicada: o backend passa a expor no pending da etapa `niche-research-seed-builder` o `previousQualityStatus`, `previousNextMoveCode`, `previousNextMove` e notas compactas do gate anterior; o prompt do coletor transforma esse aprendizado em instrução obrigatória para alterar subnicho, famílias de queries e estratégia de fontes.
- Prevenção de recorrência: adicionados testes no backend e no coletor para garantir que a etapa de seed receba e use o aprendizado automático antes da nova pesquisa profunda.

## 2026-06-14 — OPRM NichoCNAE: busca Google para fontes recentes

- Adicionado provedor `GOOGLE_CUSTOM_SEARCH_RECENT` na etapa `oprmSourceSearcher`, configurável por variáveis `OPRM_NICHO_CNAE_SOURCE_SEARCHER_GOOGLE_*`, para priorizar fontes brasileiras recentes com `dateRestrict` padrão de 24 meses antes do fallback DuckDuckGo.
- Causa-raiz tratada: ciclos do CNAE 9602501 estavam chegando ao gate com sinais suficientes, mas bloqueados por `OUTDATED_SOURCES`; a busca anterior dependia somente do DuckDuckGo HTML e frequentemente retornava resultados sem data clara, aumentando risco de fonte antiga.
- Prevenção de recorrência: criado provedor composto recent-first com fallback auditável, preservando o provider persistido no backend e evitando travar o pipeline caso Google não esteja configurado ou falhe.

## 2026-06-14 — OPRM NichoCNAE: reprocessamento automático com aprendizado entre ciclos

- O gate de qualidade do NichoCNAE passa a abrir automaticamente novo ciclo quando reprovar por causa corrigível (`NEEDS_MORE_RESEARCH`, `NEEDS_MORE_MEI_RESEARCH`, `OUTDATED_SOURCES`, `TOO_CORPORATE`, `SOLUTION_CONTAMINATED` ou `GENERIC`), limitado a 3 reprocessamentos automáticos por candidato para controlar custo.
- O ciclo reprocessado preserva o subnicho vencedor e as notas estruturadas do gate anterior; a etapa de seed já recebe `previousQualityStatus`, `previousNextMoveCode`, `previousNextMove` e `previousLearningNotes`, evitando perder aprendizado e reduzindo repetição da mesma causa de reprovação.
- Causa-raiz: o fluxo dependia de clique humano após reprovações recuperáveis, e ciclos reprovados ficavam como decisão terminal sem disparar automaticamente a próxima tentativa orientada pelo aprendizado.

## 2026-06-14 — OPRM NichoCNAE: transparência do reprocessamento automático na tela do fluxo

- A tela `/oprm/cnaes/:cnaeCode` passa a informar quando o ciclo atual foi criado por reprocessamento automático do gate, mostrando o ciclo anterior, a causa de reprovação, o reaproveitamento do aprendizado e o contador de tentativas automáticas.
- O resumo de ciclos por CNAE passou a expor `triggerSource`, permitindo diferenciar ciclo manual, fila automática e reprocessamento automático sem depender de inferência visual.

## 2026-06-14 — Auditoria crua OpenAI no pipeline NichoCNAE

- A tela de detalhe das etapas OPRM passou a destacar request cru enviado à OpenAI e resposta crua recebida sempre que a etapa usa modelo.
- A etapa `oprmNicheResearchSeedBuilder` passou a persistir o payload completo enviado à Responses API e o corpo completo retornado pela OpenAI, além da resposta estruturada já existente.
- As demais etapas foram verificadas: no fluxo NichoCNAE atual, `seed` e `mei` declaram acesso ao modelo; `seed` recebeu persistência completa nesta alteração e a tela mostra alerta objetivo quando uma etapa com IA ainda não tiver payload cru persistido.

## 2026-06-15 — OPRM NichoCNAE: correção do gate para não descartar nicho bom por contador incompleto

- Investigado o CNAE 9602501 após reprovações sucessivas no gate de qualidade, incluindo ciclos recentes com muitos sinais, fontes brasileiras e subnicho comercialmente claro de manicure autônoma em domicílio.
- Causa-raiz tratada: o gate exigia dor prática apenas pelos contadores estruturados; quando a síntese textual trazia falta, remarcação, agenda instável, retrabalho e cobrança, mas os contadores vinham zerados, o mix MEI/autônomo ficava falso e o nicho bom era descartado.
- Correção aplicada: o gate agora reconhece dor prática também no texto do card, relaxa bloqueios automáticos quando o risco de solução/fonte antiga é residual e existem evidências recentes suficientes, mantendo bloqueio forte para contaminação textual ou perfil realmente dominado por solução.
- Prevenção de recorrência: adicionado teste de regressão simulando o caso do ciclo 53, garantindo aprovação quando há rotina executora, dor vendável textual, aquisição/canais e fontes brasileiras recentes mesmo com contadores de dor prática zerados.

## 2026-06-15 — OPRM NichoCNAE: lista de nichos antes do pipeline

- Alterado o fluxo da tela de detalhe do CNAE para priorizar a lista de nichos enriquecidos já gerados pelo CNAE antes de exibir as etapas do pipeline.
- Causa-raiz tratada: o clique no CNAE entrava direto na visão operacional do pipeline, dificultando perceber se o CNAE já tinha nichos gerados e aumentando risco de duplicidade, confusão entre CNAE e nicho e gasto desnecessário.
- Correção aplicada: criado endpoint backend para listar nichos enriquecidos por CNAE; o frontend passou a exibir essa lista com ação de abertura do nicho e botão único para gerar um novo nicho, mostrando as etapas do pipeline apenas após esse comando.

- 2026-06-15 00:00:00 (UTC-3): ajustado o fluxo administrativo CNAE → subnicho no frontend: a lista de CNAEs leva para uma tela com todos os subnichos do CNAE, o comando principal passou a criar novo subnicho com potencial de venda e, após o disparo, a navegação abre uma visão dedicada ao ciclo criado com etapas, custo e jobs restritos ao novo subnicho.

## 2026-06-15 — OPRM NichoCNAE: foco da fase em definição de nicho

- Decisão registrada: a fase OPRM NichoCNAE deve produzir o insumo qualificado para a próxima fase, focando definição de nicho/subnicho, público executor, contexto operacional, rotina, canais observáveis, recorrência e evidências públicas.
- Ajuste canônico: dor, mecanismo, hipótese, oferta e demais aspectos comerciais profundos foram explicitamente deslocados para pipeline posterior próprio; o gate desta fase deve medir qualidade de definição do nicho, não validação profunda de dor vendável.
- Prevenção de recorrência: a documentação canônica agora orienta pré-gate, gate e próximos movimentos por critérios de definição de nicho, evitando que execuções sejam bloqueadas por tentarem resolver uma etapa que pertence ao pipeline seguinte.

## 2026-06-15 — OPRM NichoCNAE: custo de identificação no nicho materializado

- Causa-raiz tratada: a etapa final `oprmEnrichedNicheMaterializer` copiava rotina, segmentação e evidências para `market_niche`, mas não transferia o custo de identificação registrado em `oprm_niche_research_seed.cost_usd`; por isso o nicho materializado ficava com custo inicial zerado/nulo mesmo após gastar IA para identificar o subnicho.
- Correção aplicada: a materialização final agora soma o custo USD do seed do ciclo, converte para BRL pelo serviço canônico de moeda e preenche `market_niche.cost` e `market_niche.totalCost` quando ainda estiverem vazios/zerados, sem duplicar custo em reprocessamentos.
- Prevenção de recorrência: adicionado teste de regressão na etapa final e changelog de backfill para corrigir nichos OPRM já materializados sem custo de identificação.

## 2026-06-15 — OPRM NichoCNAE: isolamento arquitetural da conversão de moeda

- Causa-raiz tratada: o materializador enriquecido do OPRM dependia diretamente de `com.marketinghub.finance.CurrencyConversionService`, quebrando a regra ArchUnit de isolamento do módulo OPRM.
- Correção aplicada: criado conversor próprio dentro do pacote OPRM para custos de identificação, preservando a mesma configuração `app.currency.usd-to-brl` e o mesmo arredondamento financeiro sem acoplar o materializador ao pacote financeiro.
- Prevenção de recorrência: a suíte ArchUnit foi executada junto com o teste do materializador para garantir que o OPRM permaneça dependente apenas dos pacotes autorizados.

## 2026-06-17 — OPRM NichoCNAE: protocolo padrão backend

- Aplicado o protocolo padrão backend no pacote `com.marketinghub.oprm.nichocnae` por meio de regras ArchUnit específicas para as etapas do NichoCNAE.
- Causa-raiz tratada: o pacote já possuía várias etapas operacionais, mas não havia uma trava arquitetural dedicada garantindo controller canônico, service backend canônico e contratos imutáveis em subpacotes de service.
- Prevenção de recorrência: a suíte de arquitetura agora valida que cada etapa tenha um único `Backend<Etapa>Controller`, um service backend canônico e DTOs/contratos como `record`, reduzindo risco de espalhamento de contratos e endpoints no fluxo que qualifica nichos para vendas.

## 2026-06-17 — OPRM NichoCNAE: desativação da tela administrativa de pipeline

- Decisão registrada: a tela `/oprm/pipeline` ficou obsoleta e foi desativada no frontend.
- Causa-raiz tratada: manter um caminho separado de pipeline competia com o fluxo atual de criação de novo nicho pelo CNAE, aumentando risco de confusão operacional, comandos duplicados e gasto sem direcionamento comercial.
- Correção aplicada: o menu interno OPRM remove o item Pipeline, as rotas antigas de pipeline redirecionam para `/oprm` e o cânone passou a orientar que o pipeline NichoCNAE seja usado somente pelo caminho de criação de novo nicho/subnicho.
- Prevenção de recorrência: o teste de navegação OPRM valida que o link Pipeline não aparece e que a rota obsoleta não renderiza mais a tela antiga.

## 2026-06-17 06:50:00 (UTC) — OPRM NichoCNAE: correção do escopo do protocolo padrão módulo

- Corrigida a interpretação operacional do protocolo padrão módulo: ele nunca deve ser aplicado no backend principal por padrão.
- Revertida a aplicação indevida do protocolo no `backend/ads-service`; para NichoCNAE, o módulo executor correto é o `oprm-coletor-mei`.
- Atualizados o contrato operacional e a metodologia para separar claramente `protocolo padrão módulo` de `protocolo padrão backend`, evitando nova implementação no alvo errado.
- Correção adicional: a documentação e o PR desse ajuste devem deixar claro que não há alteração funcional de backend quando o diff é somente de escopo/metodologia.

## 2026-06-17 — OPRM NichoCNAE: diagrama de ciclos com feedback

- Documentado o desenho arquitetural proposto para o pipeline NichoCNAE com ciclos encadeados, feedback estruturado e reprocessamento orientado por plano de correção.
- Decisão técnica sugerida: tratar o Quality Gate como produtor de diagnóstico e plano, mantendo o orquestrador como único responsável por abrir novo ciclo e evitando acoplamento direto entre etapas concretas.
- Prevenção de recorrência: o diagrama explicita `parentCycleId`, `rootCycleId`, artefatos versionados e contratos de feedback para impedir reprocessamento cego sem aprendizado do ciclo anterior.

## 2026-06-17 — OPRM CNAE: protocolo padrão backend

- Aplicado o protocolo padrão backend no pacote `com.marketinghub.oprm.cnae` por meio de regras ArchUnit específicas para a borda HTTP, fachada de serviço e contratos de API.
- Causa-raiz tratada: o fluxo CNAE tinha leitura JDBC dentro do pacote funcional, o que misturava contrato de negócio com acesso direto ao banco e aumentava risco de espalhamento arquitetural.
- Correção aplicada: o gateway JDBC foi movido para o pacote canônico `com.marketinghub.repository.jdbc.oprm.cnae`, preservando o backend como única camada com acesso ao banco e mantendo o pacote funcional focado no contrato OPRM.
- Prevenção de recorrência: a suíte de arquitetura agora valida controller único, service único, DTOs como `record` e ausência de repositories dentro de `com.marketinghub.oprm.cnae`.

## 2026-06-17 — OPRM Opportunity: protocolo padrão módulo

- Aplicado o protocolo padrão módulo no executor `oprm-coletor-mei`, dentro de `com.marketinghub.oprmcoletormei.opportunity`, criando núcleo genérico `opportunity.pipeline` e etapas concretas plugáveis `opportunity.score` e `opportunity.enrichment`.
- Causa-raiz tratada: o fluxo CNAE de oportunidade tinha contratos e serviços úteis, mas não possuía fronteira arquitetural explícita entre núcleo genérico, etapas concretas e tecnologia/infraestrutura, permitindo recorrência de acoplamento futuro entre score e enriquecimento.
- Prevenção de recorrência: adicionadas regras ArchUnit específicas para bloquear dependência do núcleo em etapas concretas, acoplamento direto entre etapas, processors fora do contrato `StageProcessor` e tecnologia concreta no núcleo `opportunity.pipeline`.
- Não houve alteração no backend principal; o backend permanece como API/persistência do fluxo OPRM.

## 2026-06-17 — OPRM CNAE: tradução da coluna Qualidade

- Ajustada a tela de detalhe do CNAE para exibir a coluna Qualidade dos subnichos em português, mantendo o status técnico vindo do backend apenas como contrato interno.
- Causa-raiz tratada: a interface mostrava códigos técnicos como `MEI_AUDIENCE_READY` e `LIGHTLY_RESEARCHED`, reduzindo clareza operacional para priorização de nichos com potencial de venda.
- Prevenção de recorrência: a tela passou a reutilizar o mapeamento central de status já existente no módulo OPRM, evitando novas traduções divergentes no mesmo fluxo.

## 2026-06-17 — OPRM CNAE: desligamento da instância legada do coletor

- Diagnosticado via MCP que os ciclos `CNAE_SCORE` continuavam sendo criados a cada 30 minutos por logs do módulo `oprm-coletor-mei` expostos no host legado `177.153.62.107:8094`, com a classe antiga `OprmCnaeOpportunityScheduler` ainda em execução.
- Causa-raiz comprovada: o código atual já não contém os schedulers CNAE legados, mas uma instância operacional antiga do próprio módulo `oprm-coletor-mei` permaneceu ativa no host legado e continuou chamando o backend.
- Correção aplicada no módulo/deploy: o workflow do `oprm-coletor-mei` agora publica no host canônico atual e, em seguida, derruba explicitamente qualquer container/compose legado `oprm-coletor-mei` no host antigo `177.153.62.107`.
- Prevenção de recorrência: a desativação fica acoplada ao deploy do próprio módulo executor, evitando corrigir o sintoma no backend e garantindo que o módulo que dispara o ciclo seja desligado na origem.

## 2026-06-17 — OPRM NichoCNAE: nome do subnicho na execução

- Ajustada a tela de subnichos do CNAE para destacar, durante a execução do pipeline NichoCNAE, o nome do subnicho identificado pelo backend no ciclo selecionado.

## 2026-06-17 — OPRM NichoCNAE: isolamento do domínio Niche

- Corrigido o acoplamento direto do OPRM NichoCNAE com entidades e repositórios do domínio `niche`.
- Causa-raiz tratada: serviços do OPRM importavam `MarketNiche`, `MarketNicheEnrichmentProfile` e repositories de `niche`, violando a fronteira arquitetural validada pelo ArchUnit.
- Correção aplicada: o OPRM passou a depender apenas de uma porta canônica própria (`OprmEnrichedNicheGateway`), enquanto o adaptador JPA que conhece o domínio `niche` ficou fora do pacote OPRM.
- Prevenção de recorrência: a regra ArchUnit `oprmMustNotDependOnOtherMarketingHubPackages` foi validada junto com os testes de serviço afetados.

## 2026-06-17 — OPRM CNAE: visibilidade de subnichos em processamento

- Ajustada a tela de subnichos do CNAE para exibir os ciclos iniciados que ainda não foram materializados como subnicho final.
- Causa-raiz tratada: o usuário conseguia iniciar um novo subnicho, sair da tela e, ao voltar, enxergava apenas subnichos já materializados, perdendo o acesso operacional a ciclos ainda em processamento.
- Prevenção de recorrência: a tela agora cruza a verdade dos ciclos do backend com a lista de subnichos materializados e mantém uma seção fixa “Em processamento antes de virar subnicho” com ação de acompanhamento.

## 2026-06-17 — OPRM CNAE: botões Acompanhar abrem cards do pipeline

- Corrigida a navegação dos botões “Acompanhar” na tela de subnichos do CNAE para exibir automaticamente a visão dedicada com cards das etapas do pipeline quando a URL contém o ciclo selecionado.
- Causa-raiz tratada: o componente era reutilizado pelo React Router entre a lista de subnichos e a rota `/subnichos/:researchCycleId`, mas o estado local que mostra o pipeline só era inicializado na primeira montagem; ao clicar em “Acompanhar”, a URL mudava e a tela permanecia na lista.
- Prevenção de recorrência: adicionado teste de regressão cobrindo acesso direto à rota de subnicho e validando a exibição dos cards do pipeline para consulta do usuário.

## 2026-06-17 — OPRM CNAE: custo e paginação dos subnichos em processamento

- Ajustada a seção “Em processamento antes de virar subnicho” para exibir 10 ciclos por página, custo individual por ciclo e custo total no cabeçalho do card.
- Causa-raiz tratada: a lista podia crescer sem controle visual e não mostrava o impacto financeiro dos ciclos que ainda não viraram subnicho, dificultando decisão operacional sobre continuidade ou reprocessamento.
- Prevenção de recorrência: a tela passou a calcular a visualização paginada e o total financeiro diretamente a partir da verdade enviada pelo backend para os ciclos OPRM.

## 2026-06-17 — OPRM NichoCNAE: controle de reprocessamento automático movido para o executor

- Decisão operacional: o backend do OPRM passa a atuar apenas como camada de persistência/consulta para a reprovação do gate e para a gravação do novo ciclo; a decisão de abrir reprocessamento automático recuperável fica no módulo externo `oprm-coletor-mei`.
- Correção aplicada: a etapa sete do backend deixou de iniciar novo ciclo ao concluir o gate; o coletor, após persistir uma reprovação recuperável, chama o backend para reabrir o mesmo job com `triggerSource=AUTO_QUALITY_REPROCESS`.
- Causa-raiz tratada: o controle de fluxo estava no backend, contrariando a regra operacional de que rotinas/agendamentos e decisões de execução pertencem ao executor externo, enquanto o backend apenas entrega contratos/dados e recebe status/resultados.

## 2026-06-17 — OPRM NichoCNAE: reprocessamento automático preserva aprendizado

- Correção aplicada: quando o `oprm-coletor-mei` solicita `AUTO_QUALITY_REPROCESS`, o backend reabre o mesmo job preservando os campos de subnicho, nomes auditáveis, modo de pesquisa e metadados de risco/fonte do ciclo reprovado, sem voltar para o CNAE amplo do candidato.
- Causa-raiz tratada: o controle já tinha sido movido para o executor, mas a reexecução ainda podia ser tratada como novo ciclo com dados genéricos do candidato, reduzindo a inteligência do reprocessamento e aumentando risco de repetir a mesma reprovação.
- Prevenção de recorrência: o teste do orquestrador valida que a reexecução automática preserva o aprendizado do mesmo job, enquanto o seed continua recebendo status, próximo movimento e notas compactadas do gate para orientar a próxima busca.

## 2026-06-17 — OPRM NichoCNAE: reexecução no mesmo job e relatório por ciclo

- Decisão operacional: reprovação recuperável não deve ser tratada como novo job; o executor solicita a reexecução de etapas do mesmo `researchCycleId`, com novos dados de entrada orientados pelo aprendizado do gate anterior.
- Correção aplicada: o backend reabre o mesmo ciclo, limpa os artefatos derivados das etapas reexecutáveis e mantém o mesmo identificador como unidade operacional do job, evitando duplicidade conceitual no histórico.
- Relatório: criado download Markdown por `researchCycleId` na tela do CNAE/subnicho, disponível mesmo antes de materializar perfil enriquecido, com status, gatilho, observações de reexecução e artefatos atuais do pipeline.
- Prevenção de recorrência: testes validam que o reprocessamento preserva o mesmo job e que o relatório por ciclo funciona sem perfil materializado.

## 2026-06-18 — OPRM NichoCNAE: etapa atual controlada no ciclo

- Decisão operacional: o identificador operacional do fluxo NichoCNAE passa a ser o próprio `oprm_routine_research_cycle`, com a etapa atual registrada em `current_stage_code`.
- Correção aplicada: o backend passou a atualizar `current_stage_code` ao concluir/falhar etapas e os endpoints `pending` passaram a usar essa coluna como fonte primária para decidir qual fila expõe cada ciclo.
- Causa-raiz tratada: a decisão de pending estava espalhada entre status, artefatos e inferências por contadores, o que podia deixar o operador sem saber exatamente qual etapa o executor deveria consumir.
- Prevenção de recorrência: o cânone OPRM registra que callbacks do executor devem mover o `current_stage_code`, e a tela passa a consumir essa verdade do backend para exibir a etapa em execução.

## 2026-06-17 — OPRM CNAE: relatório Markdown por execução

- Adicionado botão “Relatório” para cada execução do NichoCNAE, permitindo baixar um arquivo `nicho-cnae<id>.md` com detalhamento etapa por etapa.
- Causa-raiz tratada: a tela permitia acompanhar o estado do pipeline, mas não entregava uma auditoria consolidada para o usuário analisar request/response de IA, URLs pesquisadas, retornos coletados, sinais, síntese e qualidade sem navegar por várias telas.
- Prevenção de recorrência: o relatório é gerado pelo backend a partir das tabelas canônicas da execução, mantendo a tela como consumidora da verdade do backend e evitando inferência local no frontend.

## 2026-06-18 — OPRM CNAE: ranking com subnichos, custo e pesquisa em execução

- Ajustada a tabela “CNAEs por Score OPRM” para manter somente colunas decisórias de volume, remover métricas redundantes e exibir quantidade de subnichos, custo acumulado de pesquisa e indicador visual de processamento ativo.
- Causa-raiz tratada: a tabela misturava indicadores cadastrais pouco acionáveis com dados operacionais de pesquisa, dificultando a decisão sobre onde criar, acompanhar ou interromper novos subnichos.
- Prevenção de recorrência: os novos campos vêm do endpoint do backend, preservando a regra de verdade da tela e evitando inferência local no frontend.

## 2026-06-18 — OPRM NichoCNAE: testes alinhados ao current_stage_code

- Correção aplicada: os testes das filas OPRM NichoCNAE passaram a montar ciclos com `current_stage_code` coerente com a etapa consumida e os testes de service passaram a mockar os métodos `findPendingByStatusAndCycleStage`.
- Causa-raiz tratada: os testes ainda refletiam a regra antiga baseada apenas em status/artefato, enquanto a implementação atual usa a etapa operacional do ciclo como fonte de verdade para expor pendências.
- Prevenção de recorrência: as expectativas dos testes agora protegem o contrato de pending por etapa atual, evitando que workers consumam itens fora do ponto correto do pipeline.

## 2026-06-18 — OPRM NichoCNAE: correção do backfill da etapa atual

- Corrigido o changelog de backfill de `current_stage_code` para não usar `signal` como alias SQL, pois `SIGNAL` é palavra reservada no MySQL e bloqueava a migração do backend.
- Causa-raiz tratada: o SQL do Liquibase estava semanticamente correto, mas não havia validado colisão de alias com palavra reservada do MySQL 5.7.
- Prevenção de recorrência: aliases de changelogs devem usar nomes funcionais explícitos, evitando termos reservados ou ambíguos em SQL de bootstrap.

## 2026-06-18 — OPRM NichoCNAE: backfill de etapa atual sem lock amplo

- Corrigido o backfill Liquibase de `current_stage_code` para substituir o `UPDATE` único com múltiplos `LEFT JOINs` por atualizações segmentadas por etapa usando `EXISTS`/`NOT EXISTS` nas tabelas de artefatos.
- Causa-raiz tratada: o SQL anterior fazia uma junção ampla entre várias tabelas do pipeline OPRM, multiplicando linhas por ciclo e mantendo locks por tempo suficiente para estourar `Lock wait timeout` na inicialização do backend.
- Prevenção de recorrência: backfills de bootstrap devem evitar joins amplos entre tabelas operacionais de alta cardinalidade e preferir atualizações pequenas, indexáveis e ordenadas pela etapa funcional.

## 2026-06-18 — OPRM NichoCNAE: backfill de etapa atual limitado a ciclos ativos

- Corrigido o backfill Liquibase de `current_stage_code` para atualizar somente ciclos `RUNNING`, que são os únicos necessários para reabrir as filas `pending` após a migração.
- Causa-raiz tratada: o backfill ainda varria ciclos históricos sem necessidade operacional imediata, ampliando o conjunto de linhas candidatas e aumentando a chance de conflito com locks em tabelas operacionais durante o bootstrap do backend.
- Prevenção de recorrência: backfills executados na inicialização devem limitar o escopo ao dado necessário para manter a operação ativa, evitando reclassificação massiva de histórico quando a aplicação precisa apenas publicar pendências executáveis.

## 2026-06-18 — OPRM NichoCNAE: índices para reinício manual por CNAE

- Adicionados índices compostos para as consultas de reinício manual do NichoCNAE por CNAE: ciclos por `cnae_code`/`finished_at`/`started_at` e candidatos por `cnae_code`/`opportunity_score`/`created_at`.
- Causa-raiz tratada: o reinício manual precisava localizar ciclos abertos do CNAE e o melhor candidato com bloqueio pessimista sem índice alinhado ao filtro principal, aumentando varredura, tempo de transação e chance de `Lock wait timeout`.
- Prevenção de recorrência: consultas operacionais com `FOR UPDATE` devem ter índice pelo filtro seletivo do comando para reduzir locks de faixa e tempo de espera no MySQL 5.7.

## 2026-06-18 — OPRM NichoCNAE: prevenção de timeout no Liquibase de índices

- Corrigido o changelog de índices do reinício manual NichoCNAE para separar cada índice em um changeset próprio e validar `indexExists` antes da criação.
- Aumentado o `socketTimeout` JDBC do backend para 10 minutos, evitando que operações DDL legítimas de inicialização sejam interrompidas no limite anterior de 60 segundos.
- Causa-raiz tratada: o índice em tabela operacional podia demorar mais que o timeout de leitura do conector MySQL durante o bootstrap, fechando a conexão antes do fim da DDL e impedindo a aplicação de subir.
- Prevenção de recorrência: DDLs potencialmente longas devem ser idempotentes por índice e o timeout JDBC precisa ser compatível com migrações estruturais de banco em produção.

## 2026-06-18 — OPRM NichoCNAE: backend deixa de bloquear fluxo por pré-gate semântico no seed

- Decisão de regra aplicada: o backend não deve controlar fluxo por validação semântica do seed da etapa `niche-research-seed-builder`; decisões de qualidade, reprovação, reprocessamento e próximo movimento pertencem ao executor/gates próprios do OPRM.
- Correção aplicada: removido o bloqueio backend que rejeitava `nicheName` igual ao nicho atual/CNAE e o pré-gate comercial determinístico antes da persistência. O backend passa a persistir o nome retornado pelo executor, aplicando fallback rastreável quando o campo vier ausente, e deixa o fluxo avançar para as etapas e gates responsáveis pela avaliação.
- Prevenção de recorrência: testes da etapa dois agora cobrem que o backend aceita o `nicheName` retornado pelo executor e aceita seed fraco sem transformar avaliação semântica em falha técnica de backend.

## 2026-06-18 — Tela de jobs OPRM NichoCNAE

- Criada tela administrativa `/oprm/jobs` com botão na navegação do OPRM para listar jobs recentes do pipeline NichoCNAE com paginação, custo, última etapa, relatório e acompanhamento.
- Backend expõe `/api/oprm/nichocnae/jobs` para a UI consultar os ciclos recentes sem acessar banco diretamente.

## 2026-06-19 — Bootstrap do pipeline NichoCNAE versão 2

- Criado o esqueleto inicial do pipeline NichoCNAE v2 no executor `oprm-coletor-mei`, separando núcleo genérico de execução e etapa concreta `candidate-generator`.
- Criado o contrato backend inicial para fila interna da etapa `candidate-generator`, com endpoint `pending` canônico para consumo pelo executor.
- Aplicados os protocolos padrão módulo e padrão backend com testes ArchUnit para prevenir acoplamento indevido e manter controller/service/records canônicos.

## 2026-06-19 — Regra de versionamento para pipelines completos

- Atualizada a instrução operacional para explicitar que mudanças de pipeline inteiro devem usar número de versão no pacote do executor e do backend.
- Regra registrada no cânone de arquitetura por etapa para preservar versões paralelas, rollout gradual, rollback seguro e plugabilidade entre etapas.

## 2026-06-19 — Regra v1 obrigatória para pipeline novo

- Refinada a regra de versionamento para deixar explícito que até um pipeline criado do zero deve nascer como `v1` no pacote do executor e do backend.
- Motivo: preservar a possibilidade de uma mudança completa futura criar `v2` sem sobrescrever ou contaminar a versão inicial.

## 2026-06-19 — Persistência para relatório de execução de pipeline

- Adicionada regra operacional e canônica determinando que todo pipeline deve persistir dados suficientes para gerar relatório de execução ao usuário pelo frontend.
- A regra reforça que logs técnicos não substituem dados funcionais persistidos de etapas, decisões, evidências, artefatos, custos, erros e próximos movimentos.

## 2026-06-19 — Primeiro incremento NichoCNAE v2

- Criada fundação backend da v2 para a etapa `candidate-generator`, com execução de estágio versionada por `attemptNumber`, `technicalRetryNumber` e `knowledgeVersion`.
- Separada a classificação de falhas em `INFRASTRUCTURE`, `VALIDATION`, `QUALITY` e `MARKET_EVIDENCE`, mantendo retry técnico apenas para infraestrutura.
- Mantida a materialização automática bloqueada por feature flag durante calibração da v2.
- Registrados contratos internos de `pending`, `complete` e `fail` para o executor OPRM consumir e reportar a etapa inicial.

## 2026-06-19 — Segundo incremento NichoCNAE v2

- Fortalecida a integridade da evidência da extração de sinais do NichoCNAE: sinais agora usam trecho literal dos campos persistidos do snapshot, sem evidência recomposta artificialmente.
- Adicionado bloqueio semântico para fontes de ator, ocupação ou contexto adjacente, registrando diagnóstico `SEMANTIC_CONTEXT_MISMATCH` em vez de promover o conteúdo a sinal positivo.
- Backend da etapa `signal-extractor` passou a rejeitar `evidenceExcerpt` que não exista literalmente em `sourceTitle`, `snippet` ou `shortExcerpt`, e também rejeita sinais positivos vindos de contexto incompatível.
- Adicionados testes de regressão dos ciclos 70, 72 e 75 para ator/contexto, trecho exato obrigatório e ocupação adjacente.

## 2026-06-19 — Terceiro incremento NichoCNAE v2: snapshot de conhecimento e rewind seletivo

- Implementado snapshot mínimo de conhecimento no reprocessamento de pesquisa de rotina, registrando versão, fontes aceitas, claims aceitos, fontes rejeitadas e lacunas de evidência antes de reabrir o mesmo job.
- Ajustado o reprocessamento de ciclos com falta de evidência para voltar ao query planner (`niche-research-seed-builder`) sem apagar fontes e claims já aceitos, preservando material validado para reduzir retrabalho e melhorar rastreabilidade do relatório.
- Adicionado teste de regressão do ciclo 72 para garantir preservação de evidência aceita, rejeição de contexto semântico contaminado e limpeza apenas dos artefatos reexecutáveis.

## 2026-06-19 — Protocolo leitura escrita aplicado à v2 NichoCNAE

- Aplicado o protocolo leitura escrita no backend da v2 do pipeline NichoCNAE, protegendo `com.marketinghub.oprm.nichocnae.v2..` para permanecer como camada de contratos, persistência, pendências e callbacks.
- Registrado que o controle operacional de execução da v2 permanece no módulo externo `oprm-coletor-mei`, bloqueando no backend responsabilidades como agendamento, polling, workers/runners/processors e integrações externas de execução.

## 2026-06-19 — Design visual da pipeline NichoCNAE v2 no frontend

- Adicionado botão por linha na tela de CNAEs por Score OPRM para abrir a visão da v2 do pipeline no contexto do CNAE selecionado.
- Criada a tela de design `/oprm/cnaes/:cnaeCode/pipeline-v2` com cards das etapas planejadas da v2, baseada nos planos de melhoria de qualidade, reprocessamento por conhecimento e ordem de implementação OPRM.
- A tela é informativa e não cria orquestração no frontend; as decisões de execução continuam pertencendo ao backend/executor conforme arquitetura do OPRM.

## 2026-06-19 — Implementação da etapa 2 Source Safety Filter da v2 NichoCNAE

- Implementada a etapa `source-safety-filter` da v2 NichoCNAE com contratos internos de `pending`, `complete` e `fail` no backend, reaproveitando a tabela versionada de execuções de estágio.
- Criados contratos de leitura/escrita para permitir que o executor externo solicite e consuma pendências da etapa 2 antes do planejamento adaptativo.
- Implementado no executor `oprm-coletor-mei` o processor determinístico de segurança, canonicalização, remoção de tracking, deduplicação e hard blocklist de domínios/categorias proibidas.

## 2026-06-19 — Correção leitura/escrita da etapa 2 Source Safety Filter

- Revisada a implementação da etapa `source-safety-filter` para manter o backend como camada de leitura/escrita: a decisão de próxima etapa passou a vir no callback do executor, e o backend deixou de calcular transição por `safetyDecision`.
- Removida a criação automática de pendência da etapa 2 dentro da conclusão do `candidate-generator`; o executor externo passa a solicitar explicitamente a gravação da pendência pelo contrato de escrita da etapa 2.

## 2026-06-19 — OPRM NichoCNAE v2 etapa 3 Adaptive Query Planner

- Implementada a etapa 3 `adaptive-query-planner` da v2 do pipeline NichoCNAE com backend restrito a leitura/escrita: endpoints internos de `pending`, criação de pendência, conclusão e falha técnica/cognitiva sobre a tabela genérica de execuções de etapa, sem cálculo de plano, decisão comercial ou inteligência no backend.
- Implementado no executor externo `oprm-coletor-mei` o processor plugável da etapa 3, responsável por transformar gaps de conhecimento em plano curto de queries naturais, reutilizando memória de queries anteriores, aplicando fallback de termos, deduplicação por hash e early stopping quando não há ganho informacional.
- Atualizada a tela de mapa do pipeline v2 para marcar a etapa 3 como `Design aprovado · implementação inicial`.

## 2026-06-19 — OPRM NichoCNAE v2 etapa 4 Candidate Tournament

- Implementada a etapa 4 `candidate-tournament` no executor `oprm-coletor-mei`, comparando candidatos por evidências observadas, fontes independentes e penalidades de risco antes de selecionar até dois finalistas.
- Criados contratos internos no backend apenas para leitura/escrita da etapa 4: `pending`, criação de pendência, conclusão e falha/retry técnico, mantendo lógica, controle e regra de negócio no executor externo.
- Documentado o endpoint interno em `docs/swagger/oprm-nichocnae-v2-candidate-tournament-swagger.yaml`.

## 2026-06-19 — OPRM NichoCNAE v2 etapa 5 Source Fetcher + Reranker

- Implementada a etapa 5 `source-fetcher-reranker` no executor `oprm-coletor-mei`, priorizando snapshots/fontes por prova direta, independência de domínio, aderência de ator/contexto e objetivo do gate, com retorno ao `adaptive-query-planner` quando não houver fonte direta útil.
- Criados contratos backend somente de leitura/escrita em `com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker`, preservando lógica, controle e regras comerciais no executor externo.
- Documentado o endpoint interno em `docs/swagger/oprm-nichocnae-v2-source-fetcher-reranker-swagger.yaml` e atualizada a tela de design da v2 para indicar implementação inicial da etapa 5.

## 2026-06-19 — NichoCNAE v2 etapa de gate comercial E0-E5

- Implementada a etapa `commercial-evidence-gate` no executor OPRM para calcular níveis E0-E5, confiança explicável, ganho informacional, revisão humana seletiva e liberação gradual de materialização automática.
- Criado contrato interno no backend apenas para leitura/escrita de pendências, conclusão e falhas da etapa; a lógica comercial permaneceu no executor externo.
- Atualizada a tela do pipeline v2 para sinalizar a implementação inicial do Evidence Level Gate E0-E5.

## 2026-06-19 — Reforço da etapa 7 Commercial Evidence Gate NichoCNAE v2

- Reforçada a etapa `commercial-evidence-gate` no executor `oprm-coletor-mei` para impedir materialização automática quando houver menos de três domínios independentes ou evidência contraditória pendente.
- A decisão E0-E5, confiança, gaps, contradições e próximo movimento continuam calculados exclusivamente no executor externo; o backend foi coberto por teste para atuar apenas como leitura/escrita da decisão recebida.
- Adicionados testes de regressão para independência mínima de fontes, revisão humana por contradição e contratos backend sem regra comercial.

## 2026-06-19 — NichoCNAE v2 etapa 8 Knowledge Accumulator

- Implementada a etapa 8 no executor externo `oprm-coletor-mei`, consolidando snapshot versionado com fatos validados, fontes aceitas/rejeitadas, claims rejeitados, queries executadas, assinaturas de falha, lacunas de evidência e linhagem mínima.
- Criados contratos internos no backend apenas para leitura/escrita de pendências, conclusão e falha da etapa `knowledge-accumulator`, mantendo lógica, controle, inteligência e regras de negócio no executor externo.
- Documentado o contrato em `docs/swagger/oprm-nichocnae-v2-knowledge-accumulator-swagger.yaml` e atualizada a tela do pipeline v2 para indicar implementação inicial da etapa.

## 2026-06-19 — NichoCNAE v2 etapa 9 Reprocess Controller

- Implementada a etapa 9 do pipeline NichoCNAE v2 no executor externo `oprm-coletor-mei`, mantendo a lógica de retry técnico, reprocessamento cognitivo, menor rewind, preservação de artefatos e limite por ganho informacional fora do backend.
- Adicionados contratos internos no backend apenas para leitura/escrita de pendências, conclusão e falha da etapa `reprocess-controller`; o backend persiste o plano recebido do executor e não decide regra de negócio.
- Documentado o contrato em `docs/swagger/oprm-nichocnae-v2-reprocess-controller-swagger.yaml` e atualizada a tela do mapa v2 para indicar implementação inicial da etapa 9.
- Causa-raiz preventiva: a etapa 9 estava apenas descrita no mapa do produto; agora há processor e contratos mínimos testados para impedir que decisões de reprocessamento sejam deslocadas para o backend.

## 2026-06-19 — NichoCNAE v2 etapa 10 Routine Synthesizer

- Implementada no executor externo `oprm-coletor-mei` a etapa 10 do pipeline NichoCNAE v2 (`RoutineSynthesizerProcessor`).
- A síntese usa somente claims aceitos/validados com `exactEvidenceSpan`, preserva IDs de evidência, domínio e URL, e declara gaps quando faltam rotina, dor, impacto econômico ou aquisição.
- O backend permanece fora da lógica de síntese: sua função segue limitada a leitura/escrita/contratos, enquanto regras, controle e inteligência da etapa ficam no executor OPRM.
- Atualizada a tela do mapa v2 para indicar implementação inicial da etapa 10.

## 2026-06-19 — OPRM NichoCNAE etapa 11 E0-E5

- Implementada a etapa 11 `Evidence Level Gate E0–E5` no executor `oprm-coletor-mei`, mantendo cálculo de nível, confiança, reprovação e próximo movimento fora do backend.
- Backend ficou restrito a leitura de pendências, persistência de resultado/falha e consulta para relatório do usuário.
- A transição do gate de qualidade aprovado agora envia o ciclo para `evidence-level-gate` antes da materialização enriquecida.

## 2026-06-19 — OPRM NichoCNAE v2 etapa 12 Enriched Niche Materializer

- Implementada a etapa 12 `enriched-niche-materializer` da v2 com backend limitado a contratos de leitura/escrita (`pending`, criação, conclusão e falha) sobre execuções de estágio.
- A decisão de materialização, validação E3+, bloqueio por feature flag e montagem do nicho enriquecido ficam no executor externo `oprm-coletor-mei`, preservando o backend sem lógica, inteligência ou regra de negócio do pipeline.
- Atualizados Swagger e tela de design da v2 para indicar a implementação inicial protegida por feature flag.

## 2026-06-19 — Correção de conflito de bean no controller NichoCNAE v2

- Corrigido o bootstrap dos testes do backend afetado por conflito de nome de bean Spring entre componentes legados e componentes versionados v2 da etapa `enriched-niche-materializer`.
- Causa-raiz: classes versionadas e legadas com o mesmo nome simples (`BackendEnrichedNicheMaterializerController` e `BackendEnrichedNicheMaterializerService`) eram registradas com bean names padrão duplicados.
- Prevenção aplicada: o controller e o service v2 passaram a declarar bean names explícitos e versionados, preservando convivência entre versões do pipeline.

## 2026-06-19 12:25:00 (UTC-3) — OPRM NichoCNAE v2: botão para iniciar novo job do CNAE selecionado

- Ajustada a tela do pipeline v2 para permitir iniciar um novo job do CNAE selecionado diretamente no mapa de etapas.
- O backend apenas grava uma execução pendente da etapa `candidate-generator`; a execução e o controle do fluxo permanecem no módulo externo `oprm-coletor-mei`, que consome o endpoint `pending` canônico.
- Prevenção de recorrência: o botão mostra carregamento durante a requisição e a mensagem da tela explicita que o job foi apenas gravado para o executor externo.

## 2026-06-19 — Liberação operacional da v2 NichoCNAE

- Corrigida a causa-raiz do erro ao clicar em **Iniciar novo job v2**: o backend não tinha valor padrão explícito para a feature flag `oprm.nichocnae.v2.enabled`, fazendo a tela oferecer o comando enquanto o contrato de gravação bloqueava a criação com `409 Conflict`.
- A v2 passa a ficar habilitada por padrão para criação de jobs operacionais, mantendo a materialização automática desligada por padrão até validação de qualidade.

## 2026-06-19 — Agendamento de pendências NichoCNAE v2 no executor OPRM

- Implementado scheduler no `oprm-coletor-mei` para consultar a cada 3 minutos os endpoints `pending` das etapas NichoCNAE v2 com contrato backend existente.
- O executor agora carrega o pacote `com.marketinghub.nichocnaev2`, processa pendências com os processors plugáveis, registra conclusão/falha no backend e cria a próxima pendência quando a próxima etapa v2 existe no catálogo local.
- Causa-raiz corrigida: a v2 possuía contratos `pending` no backend e processors no executor, mas não havia rotina operacional registrada no Spring para buscar e executar os jobs pendentes.

## 2026-06-19 — Correção de loop técnico entre Candidate Generator e Source Safety Filter v2

- Corrigida a causa-raiz do job `nichocnae-v2-candidate-2-job-1` entrar em retries técnicos repetidos na etapa `source-safety-filter`: a etapa `candidate-generator` emitia apenas `{"stage":"candidate-generator"}`, sem candidatos neutros nem `candidateUrls` para o filtro de segurança.
- O executor externo `oprm-coletor-mei` agora mantém a lógica de negócio no módulo executor: o `CandidateGeneratorProcessor` entrega candidatos neutros, URLs-semente seguras e `nextStageCode`, enquanto o `SourceSafetyFilterProcessor` rejeita contrato sem URLs como erro de validação, não como infraestrutura retryável.
- Adicionado limite operacional no executor para retry técnico por tentativa, classificando limite excedido como `VALIDATION/TECHNICAL_RETRY_LIMIT_EXCEEDED`; o backend permanece limitado a persistir o status informado pelo executor.
- Prevenção de recorrência: testes cobrem geração de payload útil para a etapa seguinte, rejeição de entrada inválida no filtro de segurança e classificação de contrato inválido sem novo retry técnico.

## 2026-06-19 — Ajuste visual dos cards de jobs NichoCNAE v2

- Ajustado o layout da tela do pipeline NichoCNAE v2 para manter os cards de jobs com o mesmo visual, mas exibidos em uma única coluna, ocupando a largura horizontal disponível da tela.
- Causa-raiz preventiva: o grid anterior dividia os cards em duas colunas em telas largas, reduzindo o espaço útil para leitura de jobs e tabelas operacionais.

## 2026-06-19 — Candidate Generator v2 usa descrição do CNAE no contexto operacional

- Ajustado o contrato `pending` da etapa `candidate-generator` para entregar `cnaeDescription` ao executor OPRM NichoCNAE v2, preservando o backend como fonte de leitura/escrita e o executor como responsável pela geração operacional.
- O `CandidateGeneratorProcessor` passou a montar candidatos genéricos com a descrição do CNAE quando disponível; para o CNAE `7319002`, a referência operacional passa a ser `Promoção de vendas` em vez de `CNAE 7319002`.
- Causa-raiz corrigida: o executor recebia apenas o código do CNAE no pending e, por isso, o fallback genérico contaminava o contexto de negócio com identificador técnico numérico.
- Prevenção de recorrência: testes cobrem a propagação da descrição no pending e a ausência de `CNAE 7319002` nos candidatos quando a descrição está disponível.

## 2026-06-20 — NichoCNAE v2 preserva ponto de falha em callbacks de erro do executor

- Ajustado o tratamento comum de falha do executor `oprm-coletor-mei` para enviar ao backend `errorMessage` com `reasonCode`, etapa, `stageExecutionId`, `jobId`, `cnaeCode`, classe da exception, primeiro frame de aplicação e stack trace completo.
- Causa-raiz corrigida: exceptions sem mensagem, como `NullPointerException`, eram persistidas apenas como `NullPointerException`, fazendo o sistema perder o ponto exato que gerou o erro.
- Como o callback comum `NichoCnaeV2BackendClient.fail(...)` atende todas as etapas v2, a preservação do ponto de falha passa a valer para `candidate-generator`, `source-safety-filter`, `adaptive-query-planner`, `candidate-tournament`, `source-fetcher-reranker`, `knowledge-accumulator`, `commercial-evidence-gate`, `reprocess-controller` e `enriched-niche-materializer`.
- Prevenção de recorrência: adicionado teste unitário cobrindo uma NPE sem mensagem e verificando a persistência do primeiro frame de aplicação junto com o stack trace.

## 2026-06-19 — Tradução dos nomes das etapas do pipeline NichoCNAE v2

- Atualizados os rótulos exibidos no frontend para apresentar as etapas do pipeline NichoCNAE v2 em português, mantendo os códigos técnicos internos sem alteração.
- Ajustado o teste de navegação do OPRM para validar os novos nomes visíveis ao usuário.

## 2026-06-20 — Tradução dos nomes das etapas na tela NichoCNAE v2

- Ajustada a tela `/oprm/cnaes/:cnaeCode/pipeline-v2` para exibir em português os nomes das etapas da v2 também quando o backend retorna nomes operacionais em inglês nos jobs abertos/concluídos.
- Incluído teste de frontend garantindo que `Source Safety Filter` seja apresentado como `Filtro de Segurança das Fontes`, evitando recorrência de rótulo técnico exposto ao usuário.
- 2026-06-20 03:10:00 (UTC): corrigida a causa-raiz da etapa 2 `source-safety-filter` do pipeline NichoCNAE v2 falhar no job `nichocnae-v2-candidate-2-job-2`: o executor adicionava metadados opcionais nulos ao `StageContext`, e o `Map.copyOf` interrompia a etapa com `NullPointerException` antes do filtro processar as URLs candidatas. O contexto genérico agora remove valores nulos antes de congelar o mapa e há teste de regressão cobrindo pending com `cnaeDescription`/`researchCycleId` ausentes.
- 2026-06-20 03:35:00 (UTC): documentado em `docs/canonical/oprm-nichocnae-v2-pending-executor.md` o contrato mínimo de `pending` que cada etapa do executor OPRM NichoCNAE v2 precisa receber, incluindo envelope comum, payload funcional por etapa e lacunas atuais de etapas referenciadas ainda sem contrato completo (`source-searcher`, `signal-extractor` e `routine-synthesizer`).
- 2026-06-20 03:45:00 (UTC): revisado o backend NichoCNAE v2 contra o contrato do executor e ajustados os endpoints `pending` para devolver envelope comum mais completo ao worker (`researchCycleId`, `cnaeDescription` e `materializationEnabled`, além dos identificadores já existentes). Também foi documentado o Swagger da etapa `adaptive-query-planner` e atualizadas as descrições dos pendings v2.

## 2026-06-20 — NichoCNAE v2 mostra decisão final e custo de IA por job/CNAE

- Ajustado o contrato de jobs do CNAE na v2 para expor decisão funcional final, motivo de encerramento, sinal de uso de IA e soma de custo de IA em USD por job e agregado do CNAE.
- A tela do pipeline v2 passa a mostrar quando um job foi encerrado sem subnicho viável (`NO_VIABLE_SUBNICHE`) em vez de apresentar apenas `COMPLETED`, reduzindo interpretação falsa de que houve nicho materializado.
- Causa-raiz corrigida: a tela recebia apenas status técnico de execução aberta/encerrada, mas não recebia a decisão de negócio persistida no `outputPayload` da última etapa nem o custo contabilizado nos payloads.
- Prevenção de recorrência: teste unitário do backend cobre agregação de decisão `NO_VIABLE_SUBNICHE`, explicação com contagens e custo de IA no job/CNAE.

## 2026-06-20 — Ícones de IA e Web nos cards do pipeline NichoCNAE v2

- Ajustada a tela do pipeline NichoCNAE v2 para sinalizar, em cada card de etapa, quando a etapa usa IA e/ou acessa a Web.
- Causa-raiz preventiva: antes, o usuário precisava inferir pelo texto se haveria consumo de IA ou pesquisa externa, aumentando risco de leitura operacional errada sobre custo e dependência de fontes.

## 2026-06-20 — NichoCNAE v2 reprocessa torneio sem finalistas viáveis

- Ajustado o fluxo do executor OPRM NichoCNAE v2 para que `NO_VIABLE_SUBNICHE` no `candidate-tournament` não encerre o job imediatamente: a etapa agora encaminha para `reprocess-controller` com decisão e motivo explícitos.
- O controlador de reprocessamento passa a reconhecer a decisão do torneio e devolver a menor etapa necessária (`candidate-tournament`) com nova tentativa cognitiva e nova versão de conhecimento.
- Causa-raiz corrigida: o torneio sem finalistas emitia `nextStageCode` vazio, então o executor não criava a etapa de reprocessamento prevista na documentação operacional.
- Prevenção de recorrência: testes cobrem o encaminhamento do torneio para reprocessamento, a leitura de `NO_VIABLE_SUBNICHE` pelo controlador e a propagação de tentativa/versão para a próxima pendência.

- 2026-06-20 00:00:00 (UTC): ajustada a tela e o contrato de jobs OPRM NichoCNAE v2 para exibir mensagem clara de sucesso ou fracasso no job concluído e CTA do backend para visualizar nicho materializado, abrir o CNAE para materialização ou pesquisar outro recorte, evitando que o usuário fique sem próximo passo.

## 2026-06-21 — Auditoria OpenAI do pipeline NichoCNAE v2

- Implementada tabela própria `oprm_nichocnae_v2_openai_interaction` para registrar, por execução de etapa v2, modelo, service tier, tokens, custo, request bruto, response bruto, status, erro e vínculo com `jobId`/`stageCode`.
- Os callbacks de conclusão das etapas backend da v2 passam a aceitar `openAiInteractions`, permitindo que o executor informe todas as chamadas OpenAI realizadas pela etapa sem depender de JSON solto no `outputPayload`.
- A tela/listagem de jobs passa a priorizar o custo auditado na tabela própria e mantém fallback para custos legados dentro do `outputPayload`, evitando perda de histórico durante transição.
- Causa-raiz tratada: custo e auditoria de IA estavam inferidos por payload funcional, o que podia esconder gasto real quando uma etapa esquecesse de incluir chaves de custo no JSON de saída.

## 2026-06-21 — Correção de arquitetura da auditoria OpenAI NichoCNAE v2

- Corrigida a violação ArchUnit que colocava `OpenAiInteractionAuditService` dentro de subpacote de contratos do `service`.
- A causa-raiz era mistura entre contrato DTO, que deve permanecer como `record` no subpacote de operação, e classe de serviço Spring, que não deve ocupar subpacote reservado a contratos.
- Prevenção de recorrência: o subpacote `openaiinteraction` fica restrito ao record de entrada, e a persistência da auditoria deve permanecer nos services canônicos de etapa, sem service auxiliar compartilhado no backend.

## 2026-06-21 — Auditoria OpenAI sem service auxiliar no backend NichoCNAE v2

- Removido o `OpenAiInteractionAuditService` para alinhar o backend ao padrão de leitura/escrita usado em GeraLanding: cada service canônico de etapa apenas persiste a auditoria recebida do executor no callback de conclusão.
- A causa-raiz era a criação de um service auxiliar compartilhado no backend para uma responsabilidade que não deveria virar controle operacional nem nova camada de orquestração; a execução OpenAI permanece no executor OPRM.
- Prevenção de recorrência: o teste da etapa `candidate-generator` agora cobre a gravação da auditoria OpenAI no próprio service canônico, sem depender de classe auxiliar fora da etapa.

## 2026-06-21 — NichoCNAE v2 preserva candidatos entre etapas do executor

- Corrigida a causa-raiz dos jobs NichoCNAE v2 encerrarem com `candidatos=0`: o executor criava a próxima pendência usando somente o output da etapa atual, apagando candidatos e contexto acumulado gerados nas etapas anteriores.
- O executor `oprm-coletor-mei` agora monta o payload da próxima etapa preservando o contexto funcional de entrada, removendo apenas o `nextStageCode` antigo e sobrepondo a saída nova da etapa concluída.
- O `candidate-tournament` passa a tratar ausência total de candidatos como erro de contrato de entrada, não como fracasso comercial, e aceita `rankedCandidates` em reprocessamentos para não perder histórico do torneio.
- Prevenção de recorrência: adicionados testes cobrindo preservação de candidatos ao criar a próxima pendência, bloqueio de torneio sem candidatos e reprocessamento a partir de candidatos ranqueados.

## 2026-06-21 — Tabela de jobs no pipeline NichoCNAE v2

- Ajustada a tela do pipeline NichoCNAE v2 para apresentar jobs abertos e concluídos em tabela compacta, com textos longos limitados e detalhes completos preservados no tooltip.
- Causa-raiz tratada: os cards deixavam cada job alto demais e dificultavam comparar rapidamente status, etapa, motivo, custo e atualização.

## 2026-06-21 — NichoCNAE v2: um job aberto por CNAE

- Nova regra operacional: o backend passa a bloquear a criação manual de outro job NichoCNAE v2 quando o mesmo CNAE já possui execução aberta (`PENDING`, `RUNNING` ou `TECHNICAL_RETRY_SCHEDULED`).
- Limpeza: criado changelog para encerrar como falha operacional os jobs presos do CNAE `4781400` anteriores a `2026-06-21 00:00:00`, liberando a tela para uma nova execução controlada.
- Causa-raiz tratada: a criação manual só contava execuções por candidato/etapa e não verificava jobs abertos por CNAE, permitindo dois jobs simultâneos para o mesmo mercado.

## 2026-06-21 — Relatório de fracasso do job NichoCNAE v2

- Adicionada tela de detalhe do job a partir dos casos de fracasso da tabela do pipeline v2, mostrando cada etapa persistida, entrada resumida, saída resumida, falha registrada e próxima etapa.
- O backend expõe o histórico cronológico por `jobId`, mantendo a tela fiel ao dado persistido e sem inferir localmente o caminho do pipeline.
- Causa-raiz tratada: a tabela indicava fracasso, mas não oferecia rastreabilidade operacional suficiente para o usuário entender até onde o job avançou antes de decidir novo recorte ou correção.
- 2026-06-21 21:30:33 (UTC): ajustada a tela de detalhe do job OPRM NichoCNAE v2 para revelar o conteúdo completo dos payloads de entrada e saída em JSON/texto por etapa, mantendo resumo inicial e expandindo sob demanda para facilitar diagnóstico de decisões do pipeline.

## 2026-06-22 — JSON expansível no detalhe do job NichoCNAE v2

- Ajustada a tela de detalhe do job OPRM NichoCNAE v2 para apresentar payloads JSON como árvore expansível por clique, permitindo abrir objetos e arrays internos conforme a investigação avança.
- Causa-raiz tratada: o JSON completo era exibido como texto único rolável, dificultando a análise de payloads grandes e a navegação por candidatos, evidências e campos internos.
- Prevenção de recorrência: teste de frontend cobre a abertura progressiva do JSON interno até campos aninhados.

## 2026-06-22 — NichoCNAE v2: descoberta operacional antes da dor

- Ajustado o executor `oprm-coletor-mei` para gerar mais candidatos neutros no `candidate-generator`, reduzindo a chance de um CNAE amplo encerrar com poucos recortes testados.
- O `candidate-tournament` passou a priorizar clareza operacional do executor, trabalho e contexto, sem exigir dor validada nesta fase; dor, urgência e mecanismo continuam reservados para pipeline posterior.
- Causa-raiz tratada: o fluxo estava aplicando critério conservador de viabilidade/dor cedo demais, encerrando jobs com `finalistas=0` mesmo quando ainda havia recortes operacionais que poderiam alimentar nova pesquisa.
- Prevenção de recorrência: testes da v2 agora cobrem geração de pelo menos 10 candidatos e seleção de até três finalistas por clareza operacional.
- 2026-06-22 00:00:00 (UTC): adicionada opção administrativa para cancelar jobs abertos/presos do OPRM NichoCNAE v2 na tela `/oprm/cnaes/:cnaeCode/pipeline-v2`. A correção fecha a causa-raiz operacional do bloqueio por `409 Conflict`: o backend agora possui contrato explícito de cancelamento por `jobId`, marca execuções abertas como `CANCELED`, preserva histórico/auditoria e libera o CNAE para iniciar novo job v2 sem apagar dados.

## 2026-06-22 — Observabilidade de ciclo em jobs abertos NichoCNAE v2

- A tela `/oprm/cnaes/:cnaeCode/pipeline-v2` passou a mostrar quando um job aberto está em ciclo operacional, usando campos explícitos do backend (`loopDetected`, `loopLabel`, `loopReason` e `repeatedStageCount`) em vez de inferência local no frontend.
- O backend do OPRM NichoCNAE v2 agora classifica jobs abertos com repetição de etapas no histórico como “Em ciclo de pesquisa”, deixando claro ao usuário que o executor está repetindo pesquisa/reclassificação antes de decidir.

## 2026-06-22 — Redefinição do fluxo anti-ciclo NichoCNAE v2

- Registradas no cânone OPRM as premissas de negócio de que o CNAE é público amplo, o fluxo existe para descobrir subnicho operacional abordável e a pesquisa na internet deve entender a realidade concreta antes de qualquer oferta.
- Definida regra anti-ciclo para NichoCNAE v2: repetição sem ganho novo de evidência deve virar falha controlada ou troca de recorte, não pesquisa infinita.
- O uso de IA fica permitido como apoio em pontos de alto impacto, com orçamento por job/subnicho, histórico compacto no prompt e auditoria obrigatória de modelo, tokens, custo, request e response.
- Causa-raiz tratada: o pipeline podia confundir ausência de evidência nova com necessidade de continuar replanejando, prendendo jobs em circuito entre etapas sem avanço comercial real.

## 2026-06-22 — Execução da trava anti-ciclo NichoCNAE v2

- Implementada no executor `oprm-coletor-mei` a trava anti-ciclo do NichoCNAE v2: antes de criar a próxima pendência, o job soma visitas por etapa e sequência sem ganho de informação.
- Quando etapas de pesquisa repetem circuito sem nova evidência, fonte, query ou finalista útil, o executor registra falha controlada `MARKET_EVIDENCE` com `RESEARCH_LOOP_WITHOUT_INFORMATION_GAIN` em vez de abrir nova pendência.
- Causa-raiz tratada: a continuidade era decidida apenas pelo `nextStageCode` da etapa atual, sem orçamento de repetição por job/subnicho.
- Prevenção de recorrência: teste unitário cobre o caso de `source-fetcher-reranker` tentando voltar para `adaptive-query-planner` após repetição sem ganho.

## 2026-06-22 — NichoCNAE v2 focado em MEI/autônomo

- Ajustado o `candidate-generator` do executor `oprm-coletor-mei` para gerar candidatos centrados em MEI/autônomo/dono-operador brasileiro desde a primeira etapa, sem especializar por CNAE específico e evitando recortes genéricos de empresa estruturada quando o job nasce do levantamento MEI.
- Atualizado o cânone OPRM e o contrato do executor v2 para declarar que este fluxo foca MEI/autônomo/dono-operador, tratando o CNAE como ponto de partida de segmentação e não como público final.
- Causa-raiz tratada: os jobs recentes do CNAE 4781400 usavam um CNAE com alto volume MEI, mas os candidatos gerados descreviam operação genérica de loja de vestuário, reduzindo a chance de encontrar evidência direta da rotina real do microempreendedor.
- Prevenção de recorrência: teste unitário valida que qualquer CNAE mantém o padrão genérico MEI/autônomo/dono-operador, preserva a descrição do CNAE apenas como contexto e não volta para especializações por atividade como `MEI_MODA_WHATSAPP_INSTAGRAM` nem operadores genéricos como `RETAIL_OPERATOR` ou `STORE_ASSISTANT`.
- Ajustado o `candidate-generator` do executor `oprm-coletor-mei` para gerar candidatos centrados em MEI/autônomo/dono-operador brasileiro desde a primeira etapa, evitando recortes genéricos de varejo/empresa estruturada quando o job nasce do levantamento MEI.
- Atualizado o cânone OPRM e o contrato do executor v2 para declarar que este fluxo foca MEI/autônomo/dono-operador, tratando o CNAE como ponto de partida de segmentação e não como público final.
- Causa-raiz tratada: os jobs recentes do CNAE 4781400 usavam um CNAE com alto volume MEI, mas os candidatos gerados descreviam operação genérica de loja de vestuário, reduzindo a chance de encontrar evidência direta da rotina real do microempreendedor.
- Prevenção de recorrência: teste unitário valida que o CNAE de vestuário gera recortes como MEI de moda por WhatsApp/Instagram, brechó/revenda e sacoleira/revendedora, sem `RETAIL_OPERATOR` ou `STORE_ASSISTANT` genéricos.

## 2026-06-23 — NichoCNAE v2 orientado a Instagram

- Revisado o fluxo NichoCNAE v2 para considerar Instagram/Meta Ads como canal principal de divulgação: o CNAE passa a ser contexto de exemplo, enquanto os candidatos são gerados por desejos e dores amplas reconhecíveis no feed.
- O `candidate-generator` do executor `oprm-coletor-mei` agora gera recortes amplos como renda com trabalho próprio, clientes pelo WhatsApp/Instagram, agenda vazia, preço/cobrança, profissionalização do atendimento, dependência de plataforma, organização da rotina e medo de ficar sem cliente.
- O `adaptive-query-planner` deixa de encerrar cedo quando existem candidatos amplos sem gaps específicos e cria buscas iniciais sobre Instagram, WhatsApp, aquisição de clientes, cobrança e trabalho autônomo.
- Causa-raiz tratada: o fluxo estava buscando microdores/subnichos específicos demais para uma divulgação que depende de público amplo e filtragem por criativo no Instagram.
- Prevenção de recorrência: cânone OPRM, contrato do executor e testes unitários foram atualizados para manter a estratégia `BROAD_CREATIVE_FIRST`.

## 2026-06-23 — Limite NichoCNAE v2 antes do pipeline de hipótese

- Ajustado o NichoCNAE v2 para deixar explícito que o material produzido é insumo do pipeline posterior de hipótese (`dor -> resultado -> oferta`), não a decisão final de dor, resultado, mecanismo, oferta, campanha ou landing.
- Removidas do catálogo operacional do executor v2 as etapas `commercial-evidence-gate` e `enriched-niche-materializer`, porque elas avançavam para evidência comercial/materialização antes da hora.
- O `candidate-generator` e o `routine-synthesizer` agora registram `commercialBoundary=NAO_GERAR_DOR_RESULTADO_OFERTA` e papéis de insumo para o pipeline de hipótese.
- Causa-raiz tratada: o fluxo ainda carregava etapas e campos que empurravam o NichoCNAE para validação/oferta, apesar de a divulgação por Instagram exigir primeiro um pacote amplo de público, rotina e linguagem.
- Prevenção de recorrência: adicionado teste garantindo que o catálogo v2 não registra etapas comerciais/materialização.

## 2026-06-23 — Direcionamento do resultado NichoCNAE v2

- Ajustado o comando de sucesso parcial do histórico NichoCNAE v2 para abrir o relatório do job, e não a tela legada do CNAE.
- Causa-raiz tratada: o backend enviava o usuário para a página geral do CNAE quando a materialização automática estava desativada, misturando resultado v2 novo com subnichos/processamentos antigos.
- Prevenção de recorrência: teste unitário valida que job v2 concluído sem nicho materializado direciona para o relatório do próprio job.

## 2026-06-23 — Confirmação manual de nicho no relatório NichoCNAE v2

- Adicionado ponto de confirmação no relatório do job NichoCNAE v2 para o usuário definir nome único do nicho, visualizar o custo atual de IA do job e confirmar a criação do `MarketNiche`.
- Backend passa a impedir repetição de nome de nicho e grava o vínculo do candidato CNAE com o nicho confirmado para uso nas próximas etapas do Marketing Hub.

## 2026-06-23 — Correção da arquitetura OPRM na confirmação de nicho

- Removida a dependência direta do `candidate-generator` OPRM v2 sobre o repositório canônico de nichos fora do pacote permitido pela regra de arquitetura.
- A criação do `MarketNiche` confirmado passa por um adaptador de persistência em `repository.jpa.oprm`, preservando o contrato do módulo OPRM e mantendo a regra ArchUnit como prevenção de recorrência.
- Causa-raiz tratada: o service OPRM tinha assumido diretamente a materialização em `MarketNicheRepository`, quebrando o isolamento do módulo OPRM.

- 2026-06-23 — Ops Monitor fase 4: adicionado o módulo `oprm-coletor-mei` ao cadastro monitorado do backend para acompanhar impacto operacional na descoberta de rotinas, dores e oportunidades.
- 2026-06-23 19:29:11 (UTC): tela OPRM de CNAEs atualizada para mostrar, na tabela principal, a contagem de nichos pendentes para materializar por CNAE. O endpoint `GET /api/oprm/market/import-runs/cnaes/top-volume` passou a expor `pendingMaterializationCount`, calculado no backend a partir dos cartões aprovados em `enriched-niche-materializer` ainda sem perfil materializado, evitando inferência local no frontend.
- 2026-06-24 00:50:00 (UTC): criação do ciclo 3 da ingestão OPRM CNPJ/CNAE por decisão operacional do usuário, alterando o snapshot canônico vigente para `2026-06-14` e a base de download para `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-06-14/`. Validação HTTP HEAD confirmou acesso 200 para `Cnaes.zip`, `Empresas1.zip` e `Estabelecimentos1.zip`.
- 2026-06-24 01:00:00 (UTC): ajuste do ciclo 3 da ingestão OPRM CNPJ/CNAE para deixar claro que o novo fluxo deve apenas cadastrar emails associados a CNAEs. A execução ficou limitada aos vínculos de ESTABELECIMENTOS com email preenchido, sem recalcular market size, score, enriquecimento ou materialização de nichos.

## 2026-06-24 — Plano de audiências Meta Ads vinculadas a nichos e experimentos

- Criado plano em `docs/implementacao/oprm/plano-audiencias-meta-nicho-experimento.md` para conectar audiências Meta Ads aos nichos materializados e aos experimentos, incluindo o conceito de parcela do nicho quando o experimento testar apenas um recorte do mercado amplo.
- Definido modelo operacional com `meta_audience`, `meta_audience_segment` e `experiment_meta_audience`, evitando audiências órfãs e preservando leitura de performance por nicho, parcela, público, oferta e experimento.
- 2026-06-24 00:00:00 (UTC): tela `/oprm/cnaes/:cnaeCode/pipeline-v2` ajustada para usar a descrição do CNAE como título principal, reaproveitando o catálogo canônico de CNAEs já consumido no OPRM e mantendo fallback para o código quando a descrição ainda não carregar.

## 2026-06-24 — Etapa 4 do plano de audiências Meta por nicho

- Solicitação: executar a Etapa 4 — Criação da audiência na Meta do plano `docs/implementacao/oprm/plano-audiencias-meta-nicho-experimento.md`.
- Foi feito: criado o contrato persistente `meta_audience`/`meta_audience_segment`, mantendo o backend somente como leitura/escrita da audiência já decidida pelo OPRM; o Facebook Ads Worker cria a Custom Audience, normaliza/deduplica emails apenas por exigência técnica da Meta, gera hash SHA-256, faz upload em lotes e retorna o `facebookAudienceId` ao backend.
- Prevenção de recorrência: a regra de negócio de público, nome, recorte, elegibilidade e volumes fica no módulo OPRM executor; a audiência persistida continua vinculada a `market_niche_id`, CNAE, segmento e conta de anúncios para evitar audiência órfã na Meta.

## 2026-06-25 — Resultado final do NichoCNAE com tarefas e auditoria

- Ajustada a materialização de nicho enriquecido para persistir explicitamente uma lista de tarefas diárias da persona, separada do resumo geral de rotina.
- A criação do perfil enriquecido passa a guardar um relatório Markdown auditável da pesquisa, reunindo queries, fontes candidatas, snapshots, sinais, card de rotina e conclusão operacional.
- Removida a promoção de `offer_idea` no momento de materialização do NichoCNAE: essa etapa permanece responsável por rotina/evidência, enquanto a oferta fica para o pipeline de hipótese/oferta.
- Causa-raiz tratada: o resultado final misturava maturidades diferentes — rotina real do nicho, hipótese comercial e ideia de oferta — e não persistia uma trilha auditável fechada no próprio perfil criado.
- Prevenção de recorrência: o cânone de experimento foi atualizado para exigir tarefas diárias e relatório auditável na passagem enriquecida `nicho-cnae → hipótese`.

## 2026-06-25 — Reexecução de CNAE com nicho existente

- Regra ajustada: executar novamente o pipeline para CNAE que já possui nicho não deve bloquear a pesquisa.
- Na conclusão, quando o candidato/CNAE já estiver ligado a um `market_niche`, o fluxo atualiza o nicho existente e renova `updated_at`, em vez de criar duplicidade silenciosa.
- O perfil enriquecido materializado por CNAE + nome neutro também passa a ser atualizado quando já existir, preservando o registro e renovando as informações auditáveis.

- 2026-06-25 — Tela OPRM de CNAEs atualizada para destacar visualmente, já no início da linha, os CNAEs com processamento de pesquisa em execução, reutilizando o indicador `nicheResearchRunning` entregue pelo backend.

## 2026-06-25 — OPRM NichoCNAE v3: criação do fluxo versionado de persona e tarefas diárias

- Solicitação: criar a v3 do fluxo NichoCNAE aplicando protocolo padrão módulo e protocolo padrão backend.
- Foi feito: criada a base backend `com.marketinghub.oprmcoletormei.nichocnae.v3` com endpoints internos por etapa, tabela `oprm_nichocnae_v3_stage_execution`, contratos `record`, endpoint `pending` por etapa e regra ArchUnit de estrutura canônica.
- Foi feito: criado o executor `com.marketinghub.pipelines.nichocnae.v3` no `oprm-coletor-mei` com núcleo genérico, catálogo de 10 etapas, processors plugáveis, scheduler de pendências, cliente backend, prompts/schemas versionados para etapas com IA e teste ArchUnit próprio.
- Objetivo de negócio: transformar um CNAE em persona operacional, rotina real e lista de tarefas diárias auditável, sem criar oferta, campanha ou landing nesta fase.
- Prevenção: a v3 nasce separada da v2 para evitar remendos no fluxo que parava após o planejador de buscas por falta de etapa cadastrada.

## 2026-06-25 — Correção preventiva de conflito de beans no NichoCNAE v3

- Corrigida a diferenciação dos nomes das classes versionadas v3 das etapas `source-fetcher` e `source-searcher`, evitando conflito de bean Spring com as classes legadas de mesmo papel.
- Verificados os componentes Spring do backend para localizar outros nomes simples duplicados com bean name padrão; não restaram duplicidades sem nome explícito após o ajuste.

## 2026-06-25 — Tela de CNAEs passa a abrir NichoCNAE v3

- Alterado o botão da tabela de CNAEs do OPRM para abrir o pipeline NichoCNAE v3 em vez da v2.
- Criada tela inicial do pipeline v3 no frontend, com acionamento do endpoint canônico `cnae-intake` para iniciar novo job v3 por CNAE.

## 2026-06-25 — Indicadores visuais no pipeline NichoCNAE v3

- Adicionado endpoint de leitura do progresso do job v3 mais recente por CNAE, baseado nas execuções persistidas pelo backend.
- A tela do pipeline v3 passa a consultar esse progresso periodicamente e mostrar nos cards o status real de cada etapa, com destaque visual para etapas na fila/em execução.
- Ajustado para recuperar sempre o último job iniciado pelo CNAE no backend ao sair e voltar para a tela, sem depender do estado local do navegador.
- Objetivo de negócio: dar clareza operacional ao usuário sobre o que está acontecendo agora, reduzindo incerteza durante a geração de personas, rotina e tarefas diárias.

## 2026-06-25 — Verificação do agendamento do NichoCNAE v3

- Verificação: o executor `oprm-coletor-mei` possui um scheduler único em `com.marketinghub.pipelines.nichocnae.v3.execution.NichoCnaeV3PendingExecutionScheduler`, com cron `0 */3 * * * *`, que chama a varredura de todas as etapas cadastradas no catálogo v3.
- Evidência de cobertura: o catálogo v3 contém as 10 etapas `cnae-intake`, `persona-candidate-generator`, `persona-tournament`, `routine-query-planner`, `source-searcher`, `source-fetcher`, `routine-signal-extractor`, `daily-tasks-synthesizer`, `quality-gate` e `persona-routine-materializer`, todas com processor e endpoint backend v3 derivado do código da etapa.
- Prevenção: adicionados testes de contrato para impedir alteração acidental do cron de 3 em 3 minutos e para garantir que toda etapa v3 cadastrada tenha `backendPath` e `processor` disponíveis para a varredura agendada.

## 2026-06-25 — Correção do carregamento Spring do NichoCNAE v3

- Causa-raiz comprovada pelo log do container: o `oprm-coletor-mei` estava ativo e executando schedulers legados/v2, mas não havia nenhuma linha do `NichoCnaeV3PendingExecutionScheduler`; no código, o pacote `com.marketinghub.pipelines.nichocnae.v3` não estava incluído no `scanBasePackages` da aplicação Spring.
- Correção: adicionado `com.marketinghub.pipelines.nichocnae.v3` ao `scanBasePackages` do `OprmColetorMeiApplication`, permitindo que scheduler, service e client v3 sejam registrados no contexto e executem a cada 3 minutos.
- Prevenção: adicionado teste de contrato para garantir que o pacote v3 permaneça no component scan da aplicação, além dos testes de cron e catálogo v3 já criados.

## 2026-06-25 — Desligamento operacional do NichoCNAE v2 no coletor

- Decisão operacional: desligar o NichoCNAE v2 no `oprm-coletor-mei` para evitar varreduras agendadas concorrentes e ruído operacional enquanto a v3 é a versão ativa do fluxo.
- Correção: removido `com.marketinghub.nichocnaev2` do `scanBasePackages` do `OprmColetorMeiApplication`, impedindo que scheduler, service e client v2 sejam registrados no contexto Spring do container.
- Prevenção: adicionado teste de contrato garantindo que o pacote v2 permaneça fora do component scan e que o pacote v3 continue carregado.

## 2026-06-25 — Monitoramento operacional do NichoCNAE v3 no Ops Monitor

- O Ops Monitor passou a detectar pendências antigas do pipeline NichoCNAE v3 persistidas no backend como incidente sintético do módulo `oprm-coletor-mei`.
- A regra degrada o módulo quando existir execução v3 em `PENDING` há mais de 6 minutos, expondo o job, CNAE e etapa parada para facilitar ação operacional.
- A causa-raiz tratada é a diferença entre container ativo e pipeline sem consumo: saúde HTTP isolada não garante que o executor esteja processando a fila v3.

## 2026-06-25 — NichoCNAE v3 registrado no Protocolo Monitor

- O NichoCNAE v3 ficou registrado como primeiro caso do `Protocolo Monitor`, usando o `oprm-coletor-mei` como módulo executor monitorado.
- A regra operacional é detectar fila v3 parada mesmo quando o container está online, evitando falso positivo de saúde operacional.

## 2026-06-25 — OPRM NichoCNAE v3: isolamento da materialização final

- Corrigido o acoplamento direto da etapa `persona-routine-materializer` v3 com a porta da versão inicial `OprmEnrichedNicheGateway`.
- Causa-raiz tratada: o service v3 reutilizava contrato sem versão, fazendo o ArchUnit interpretar dependência direta da v3 para a versão inicial.
- Correção aplicada: a etapa v3 passou a depender de uma porta própria, localizada no pacote da própria etapa, e o adaptador JPA externo faz a tradução para a persistência canônica.
- Prevenção de recorrência: validado que não restam imports de `com.marketinghub.oprm.nichocnae.gateway` dentro do pacote v3 e executados os testes da etapa afetada.

## 2026-06-26 — NichoCNAE v3: saída final sem dores, resultados e mecanismos

- Decisão: a saída final do NichoCNAE v3 deixou de tratar dores, resultados desejados e mecanismos/oportunidades como itens finais do nicho enriquecido.
- Motivo: esses blocos pertencem a fluxos posteriores de hipótese/oferta; a etapa v3 deve finalizar apenas persona, rotina, tarefas, contexto, evidências, fontes e auditoria operacional.
- Ajuste: o backend v3 não preenche mais os campos legados desses blocos na materialização final, e a tela de nicho enriquecido não os exibe como lista principal.

## 2026-06-26 — NichoCNAE v3: vínculo de CNAE no nicho materializado

- Decisão: a materialização final do NichoCNAE v3 passa a criar ou atualizar o nicho usando vínculo explícito com o CNAE de origem.
- Ajuste: `market_niche` recebeu `source_cnae_code` e `source_cnae_description`; novos nichos v3 usam nome canônico `CNAE <código> — <descrição>`.
- Reprocessamento: quando o mesmo CNAE já tiver nicho materializado, o fluxo v3 atualiza esse nicho e o perfil enriquecido em vez de criar outro registro solto.

## 2026-06-26 — Tela do pipeline NichoCNAE v3 em coluna única

- Ajuste visual: a tela de etapas do pipeline v3 passou a exibir os cards em coluna única para facilitar leitura sequencial do fluxo.
- Ajuste de legibilidade: payloads JSON de entrada e saída agora são exibidos em árvore expansível, mantendo campos abertos por padrão nos primeiros níveis e evitando blocos longos difíceis de navegar.
- Escopo: mudança apenas de apresentação no frontend, usando os dados já retornados pelo backend.

## 2026-06-26 — NichoCNAE v3: geração real de personas candidatas

- Causa-raiz comprovada: a etapa `persona-candidate-generator` do executor OPRM NichoCNAE v3 estava concluindo o pipeline com payload técnico, sem gerar `candidatePersonas`, embora a tela prometesse geração de personas.
- Correção: a etapa passou a produzir quatro personas candidatas estruturadas por CNAE, com dores operacionais, tarefas diárias, sinais de compra, resumo de persona, resumo de rotina, limitações de evidência e `nextStageCode` para o torneio.
- Prevenção de recorrência: adicionado teste unitário garantindo que a etapa entregue personas funcionais e não apenas metadados técnicos.

## 2026-06-26 — NichoCNAE v3: persona-candidate-generator com OpenAI

- Ajuste de causa-raiz após revisão: a implementação anterior ainda gerava personas por regra determinística e não acessava a OpenAI.
- Correção: a etapa `persona-candidate-generator` passou a usar cliente da OpenAI Responses API com prompt e schema versionados, `service_tier=flex`, JSON Schema estrito e validação de quantidade mínima de personas antes de concluir.
- Prevenção de recorrência: adicionados testes de processor e cliente OpenAI garantindo que a etapa chama o gerador, envia `json_schema`/Flex e falha quando a resposta não contém personas suficientes.

## 2026-06-26 — NichoCNAE v3: erro visível na etapa falha

- Diagnóstico: a etapa `persona-candidate-generator` do CNAE `4781400` falhou com `IllegalStateException: OpenAI API key não configurada para persona-candidate-generator NichoCNAE v3.`, e o backend já persistia esse erro em `oprm_nichocnae_v3_stage_execution.error_message`.
- Causa-raiz da tela: a página do pipeline v3 exibia apenas entrada e saída da etapa, ignorando `errorMessage`; por isso o card aparecia como `Falhou`, mas mostrava `Sem saída registrada` em vez da causa da falha.
- Correção: a tela passou a mostrar o bloco `Erro registrado` quando o backend retornar `errorMessage` para a etapa.

## 2026-06-26 — NichoCNAE v3: OpenAI API key pelo mesmo fallback das versões anteriores

- Causa-raiz complementar: o `persona-candidate-generator` v3 tinha `@ConfigurationProperties`, mas o `application.yml` não declarava o bloco `oprm.pipelines.nichocnae.v3.persona-candidate-generator.openai`; por isso a etapa não herdava o fallback global `OPENAI_API_KEY`/`OPENAI_API_KEY_FILE` já usado pelas etapas anteriores do OPRM.
- Correção: o coletor OPRM passou a configurar a etapa v3 com variável específica opcional e fallback para `OPENAI_API_KEY` e `OPENAI_API_KEY_FILE`, mantendo o mesmo padrão operacional das versões anteriores.
- Prevenção de recorrência: adicionado teste de configuração garantindo que o fallback global da OpenAI continue presente no `application.yml` para a etapa `persona-candidate-generator` v3.

## 2026-06-26 — Protocolo padrão módulo: fallback OpenAI obrigatório

- Decisão de regra: a definição do protocolo padrão módulo passou a exigir que etapas OpenAI em módulos executores declarem configuração própria, mas reutilizem obrigatoriamente os fallbacks globais `OPENAI_API_KEY` e `OPENAI_API_KEY_FILE`.
- Motivo: evitar recorrência da falha em que uma etapa nova de OpenAI tinha `@ConfigurationProperties`, mas não herdava o segredo operacional já provisionado para os workers.
- Documentos atualizados: `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md` e `docs/canonical/pipeline-operacional-canon.v1.md`.

## 2026-06-26 — NichoCNAE v3: segredo OpenAI publicado no compose do executor

- Causa-raiz comprovada: a correção anterior passou a declarar fallback `OPENAI_API_KEY_FILE` no `application.yml`, mas o `docker-compose` do `oprm-coletor-mei` não publicava esse fallback global nem a variável específica da etapa `persona-candidate-generator` v3 no ambiente do container.
- Correção: os compose local e de deploy do executor passaram a expor `OPENAI_API_KEY_FILE` e `OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE`, ambos apontando por padrão para `/run/secrets/openai_api_key`, além do modelo operacional `gpt-5.2` da etapa v3.
- Prevenção de recorrência: o teste de configuração da etapa v3 agora valida também os arquivos compose, impedindo nova publicação sem o segredo OpenAI necessário para o executor.

## 2026-06-26 — NichoCNAE v3: leitura direta do arquivo seguro da OpenAI

- Causa-raiz aprofundada: depender apenas de variável de ambiente para apontar o arquivo da chave ainda deixava a etapa vulnerável a deploy sem `OPENAI_API_KEY_FILE`, mesmo com o segredo existente no servidor em `/root/infra/openai-token/openai_api_key`.
- Correção: a etapa `persona-candidate-generator` v3 agora tenta ler a chave por variável direta, pelo arquivo configurado, pelo segredo montado no container em `/run/secrets/openai_api_key` e, como fallback operacional, pelo caminho seguro do host `/root/infra/openai-token/openai_api_key`.
- Prevenção de recorrência: o `application.yml` do executor agora tem default explícito para `/run/secrets/openai_api_key`, e os testes validam a leitura de chave por arquivo seguro.

## 2026-06-26 — NichoCNAE v3: logs do request e response OpenAI

- Correção operacional: a etapa `persona-candidate-generator` do executor OPRM passou a registrar em log o payload enviado à OpenAI e a resposta bruta recebida, com `jobId`, `stageExecutionId`, `cnaeCode` e endpoint, sem expor a chave de autenticação.
- Motivo: permitir diagnosticar a causa real de falhas da OpenAI no NichoCNAE v3 sem depender apenas da mensagem genérica persistida na tela.

## 2026-06-26 — OPRM Coletor MEI: URL correta de logs no MCP

- Diagnóstico: o workflow atual publica o `oprm-coletor-mei` em `191.252.120.96`, mas o default do MCP ainda apontava o alias legado `oprm-coletor-receita` para `177.153.62.107:8094`.
- Correção: o default documentado e configurado no `mcp-server` passou a apontar para `http://191.252.120.96:8094/actuator/logfile`, mantendo o alias operacional `oprm-coletor-receita` usado pela tool `java_module_logs`.

## 2026-06-26 — NichoCNAE v3: logs completos de falhas OpenAI

- Diagnóstico: a falha recente da etapa `persona-candidate-generator` chegava ao banco e à tela como erro genérico, porque o log não registrava explicitamente `statusCode`, `statusText`, headers e corpo retornado pela OpenAI em respostas HTTP de erro.
- Correção: o cliente OpenAI da etapa passou a registrar caminhos de diagnóstico para chave ausente, origem da chave por arquivo seguro, request enviado, response recebido, corpo vazio, erro HTTP com status/corpo, falha genérica com tipo/mensagem da exceção e ausência de texto JSON extraível.
- Prevenção de recorrência: adicionado teste unitário cobrindo erro HTTP 400 da OpenAI e validando que o log contém status e corpo do provedor sem expor a chave direta.

## 2026-06-26 — NichoCNAE v3: timeout ampliado para OpenAI

- Diagnóstico: a etapa `persona-candidate-generator` do job `nichocnae-v3-4781400-1782477451721` falhou durante POST para `https://api.openai.com/v1/responses` com `Broken pipe`, após a chave e o payload terem sido resolvidos corretamente.
- Correção: o `RestClient` compartilhado do executor OPRM passou a usar timeout de conexão de 30 segundos e timeout de leitura de 5 minutos, reduzindo falhas prematuras em chamadas OpenAI longas em modo Flex.
- Prevenção de recorrência: adicionado teste unitário garantindo que os timeouts ampliados permaneçam configurados no cliente HTTP do coletor.

## 2026-06-26 — Refatoração de pacotes do pipeline NichoCNAE v3

- Decisão aplicada: organizar o pipeline NichoCNAE v3 no padrão de pacotes solicitado.
- Backend: raiz movida para `com.marketinghub.oprmcoletormei.nichocnae.v3`, mantendo as etapas abaixo de `v3.<nome-etapa>`.
- Executor OPRM MEI: raiz movida para `com.marketinghub.pipelines.nichocnae.v3`, mantendo as etapas abaixo de `v3.<nome-etapa>` e o núcleo genérico em `v3.core`.
- Correção preventiva associada: o schema OpenAI da etapa `persona-candidate-generator` passou a declarar `type: string` nos campos constantes rejeitados pela Responses API.

## 2026-06-26 — Atualização canônica do padrão de pacotes NichoCNAE v3

- Atualizados os documentos canônicos para refletir a regra decidida: backend em `com.marketinghub.pipelines.<nome-modulo>.<nome-pipeline>.v<numero-versao>.<nome-etapa>` e executor em `com.marketinghub.pipelines.<nome-pipeline>.v<numero-versao>.<nome-etapa>`.
- Canonizado o caso OPRM NichoCNAE v3 com backend em `com.marketinghub.oprmcoletormei.nichocnae.v3`, executor em `com.marketinghub.pipelines.nichocnae.v3`, núcleo executor em `.core`, operação de pendências em `.execution` e repositories JPA preservados em `com.marketinghub.repository.jpa.oprm.nichocnae.v3`.
- Objetivo: evitar nova divergência entre a refatoração de pacotes, os protocolos de arquitetura por etapa e a documentação usada como fonte de verdade.

- 2026-06-26 00:00:00 (UTC): padronizado o NichoCNAE v3 para manter no executor apenas a raiz `com.marketinghub.pipelines.nichocnae.v3`; removidos do módulo os pacotes legados `com.marketinghub.nichocnae` e `com.marketinghub.nichocnaev2`. No backend, o pacote v3 foi movido de `com.marketinghub.pipelines.oprm.nichocnae.v3` para `com.marketinghub.oprmcoletormei.nichocnae.v3`, preservando o backend como fonte de verdade das execuções e o executor como consumidor dos endpoints `pending`.

## 2026-06-26 — NichoCNAE v3: endpoint start por etapa no backend

- Correção: cada etapa interna do backend NichoCNAE v3 passou a expor `POST /start` recebendo `cnaeCode` como parâmetro para criar uma execução pendente da própria etapa.
- Service: cada service canônico da etapa recebeu método `start(String cnaeCode)`, mantendo o backend como fonte de verdade para abertura de pendências e preservando o executor externo apenas como consumidor do endpoint `pending`.
- Contrato: a documentação Swagger v3 foi atualizada com o novo endpoint genérico por etapa.

## 2026-06-26 — NichoCNAE v3: constantes operacionais nos services de etapa

- Correção: os services canônicos de etapa do backend NichoCNAE v3 receberam constantes explícitas para `STAGE_CODE`, `NEXT_STAGE` e statuses operacionais (`INICIADO`, `AGUARDANDO_RETORNO_MODULO`, `CONCLUIDO`, `FALHA`).
- Objetivo: reduzir divergência de códigos de etapa/status entre backend, executor OPRM MEI e relatórios operacionais do pipeline.

## 2026-06-26 — NichoCNAE v3: rastreio do status no cadastro CNAE

- Correção: os métodos `start(String cnaeCode)` dos services de etapa do backend NichoCNAE v3 agora localizam o CNAE pelo repository canônico, marcam o pipeline `nichocnae` como `INICIADO` e registram a etapa atual com `STAGE_CODE` antes de criar a pendência.
- Persistência: adicionadas colunas em `oprm_cnpj_cnae_dim` para manter status do pipeline e etapa atual diretamente no cadastro do CNAE.

## 2026-06-26 — NichoCNAE v3: data/hora do status no cadastro CNAE

- Ajuste: adicionada a coluna `nichocnae_pipeline_updated_at` em `oprm_cnpj_cnae_dim` para registrar a data/hora corrente sempre que o `start` de uma etapa atualizar o status e a etapa atual do pipeline NichoCNAE v3.

- 2026-06-26: Ajustado pending do backend NichoCNAE v3 para publicar até 10 CNAEs iniciados por etapa corrente a partir de oprm_cnpj_cnae_dim, ordenados por atualização ascendente, usando a constante INICIADO no suporte compartilhado das etapas.

## 2026-06-26 — Backend: tabela de auditoria `pipeline_nichocnae`

- Criada a tabela `pipeline_nichocnae` para centralizar auditoria de request/response, prompt/schema, tokens, modelo, custo, erro, etapa, plataforma e versão do pipeline NichoCNAE.

## 2026-06-28 — Correção: auditoria append-only do NichoCNAE v3

- Causa-raiz: `pipeline_nichocnae.id_externo` era chave primária, mas representa o CNAE. Com isso, callbacks de request/response do mesmo CNAE podiam sobrescrever a auditoria anterior e a tela podia exibir payload funcional ou input técnico como se fosse o request enviado à OpenAI.
- Correção: adicionada chave técnica `id` auto incremental em `pipeline_nichocnae`, mantendo `id_externo` como CNAE indexado. A entidade JPA passou a inserir eventos de auditoria separados para request e response.
- A tela v3 agora combina request e response do mesmo `jobId` e não usa mais `inputPayload` como fallback para o bloco “Request enviado”, evitando apresentar contexto de etapa como request OpenAI.
- Criadas a entidade JPA e o repository canônico no backend para permitir persistência e consultas de custo por `jobId` sem acesso direto ao banco por módulos externos.

## 2026-06-27 — NichoCNAE v3: reforço do protocolo padrão módulo no executor

- Aplicado reforço do protocolo padrão módulo no `oprm-coletor-mei` para o pacote executor `com.marketinghub.pipelines.nichocnae.v3`.
- O teste ArchUnit da v3 agora também bloqueia tecnologia concreta no núcleo `v3.core` e ciclos entre pacotes, prevenindo acoplamento que dificultaria troca, remoção ou evolução das etapas.
- Backend não foi alterado: o executor continua consumindo o trabalho pelos endpoints `pending` canônicos e reportando resultado ao backend.

## 2026-06-27 — OPRM NichoCNAE v3: regra de classes canônicas por etapa

- Adicionada regra no teste de arquitetura do `oprm-coletor-mei` para validar que subpacotes de etapa concreta em `com.marketinghub.pipelines.nichocnae.v3`, quando adotarem o padrão canônico do worker, fiquem restritos às 9 classes esperadas: `BackendClient`, `ExecutionScheduler`, `Input`, `Output`, `PromptBuilder`, `ResponseHandler`, `ResponseValidator`, `WorkerConfiguration` e `WorkerProperties`.
- Objetivo: impedir crescimento desordenado dos subpacotes de etapa e manter o executor simples, auditável e plugável.

- 2026-06-27 00:00:00 (UTC): removido o contrato oficial obsoleto `oprm-nicho-cnae-pipeline` do registry do backend e substituído pelo contrato único `oprm-nicho-cnae-v3-pipeline`, alinhado aos pacotes reais do executor `oprm-coletor-mei` em `com.marketinghub.pipelines.nichocnae.v3`; changelog incremental desativa o pipeline antigo no banco e cria a definição persistente v3.

- 2026-06-27 00:00:00 (UTC): gerado Swagger específico `docs/swagger/oprmcoletormei-nichocnae-v3-swagger.yaml` para o contrato operacional do executor `oprm-coletor-mei` no pipeline `com.marketinghub.pipelines.nichocnae.v3`, documentando consumo de `pending`, callbacks `complete`/`fail`, health do módulo, etapas registradas e separação de responsabilidade em que o backend mantém a decisão de avanço do pipeline.

## 2026-06-27 — Ajuste de endpoints internos NichoCNAE v3 do OPRM Coletor MEI

- Atualizados os controllers backend de `com.marketinghub.oprmcoletormei.nichocnae.v3` para expor o prefixo canônico `/api/internal/oprmcoletormei/nichocnae/v3/<etapa>/stage-executions`.
- Padronizados os callbacks operacionais para `/{idExterno}/start`, `/{idExterno}/{jobId}/recebeRequest`, `/{idExterno}/{jobId}/recebeResponse` e `POST /pending`.
- Sincronizados o Swagger e o cliente do executor `oprm-coletor-mei` para consumir o novo contrato.

## 2026-06-27 — NichoCNAE v3: saída útil da etapa 4

- Causa-raiz confirmada no executor: `RoutineQueryPlannerProcessor` encerrava a etapa `routine-query-planner` com metadados técnicos (`stage`, `status`, `inputKeys`) e não transformava a persona vencedora em um plano de busca acionável.
- Correção: a etapa 4 agora exige `winnerPersona` e gera `personaFocus`, objetivo de busca, consultas priorizadas, perguntas de validação, critérios de aceite de fonte e critérios de descarte antes de avançar para `source-searcher`.
- Prevenção: adicionado teste unitário garantindo que a saída contenha plano útil e bloqueie execução sem persona vencedora.

## 2026-06-27 — NichoCNAE v3: recorte MEI/autônomo na etapa 1

- Ajuste: a saída da etapa `cnae-intake` agora explicita que o pipeline está analisando MEI e profissionais autônomos que atuam por conta própria, sem contratação direta como CLT.
- Motivo: esse recorte precisa nascer na primeira etapa para orientar a geração de personas e evitar que as próximas etapas pesquisem funcionários CLT de empresas do CNAE.
- Prevenção: teste unitário da etapa 1 passou a validar `targetAudienceType`, `targetAudienceDefinition` e `employmentBoundary`.

## 2026-06-27 — Backend NichoCNAE v3: consulta de situação da auditoria

- Criado endpoint interno `POST /api/internal/oprmcoletormei/nichocnae/v1/<etapa>/stage-executions/{idExterno}/situacao` para consultar `pipeline_nichocnae` por etapa, identificador externo e lista de status, retornando registros ordenados por `data_hora` decrescente.
- Adicionada coluna `status` à auditoria `pipeline_nichocnae`, preenchida nos callbacks v3 como `AGUARDANDO_MODULO`, `CONCLUIDO` ou `FALHA`, permitindo filtro direto no banco em vez de inferência por payload.

## 2026-06-28 — NichoCNAE v3: revisão do source-searcher contra avanço sem fontes

- Diagnóstico: os logs do `oprm-coletor-mei` mostraram falha recorrente em `source-fetcher` porque a etapa anterior concluía como `FONTES_ENCONTRADAS`, mas não entregava `selectedSources` nem `foundSources`.
- Causa-raiz: contrato incompleto entre `source-searcher` e `source-fetcher`; o pipeline avançava com queries planejadas como se fossem fontes reais.
- Correção: `source-searcher` agora só libera `nextStageCode=source-fetcher` quando recebe fontes reais auditáveis; sem fontes, conclui bloqueado com `FONTES_NAO_COLETADAS`, motivo persistível e etapa recomendada de correção.
- Prevenção: adicionado teste unitário cobrindo bloqueio sem fontes e avanço apenas com `foundSources` reais.

## 2026-06-28 — NichoCNAE v3: source-searcher com busca e qualificação de fontes

- Causa-raiz complementar: a correção anterior impedia avanço sem fontes, mas a etapa 5 ainda não executava busca pública a partir das queries planejadas; ela dependia de `foundSources` já presentes no payload, deixando a etapa 6 sem insumo de qualidade quando o fluxo normal só trazia `plannedQueries`.
- Correção aplicada no executor `oprm-coletor-mei`: `source-searcher` passou a buscar fontes públicas rastreáveis, registrar payload bruto da busca, deduplicar URLs, priorizar Brasil/MEI/autônomo/rotina e entregar `selectedSources` + `foundSources` com `sourceIntent`, `routineEvidenceScore`, `brazilRelevanceScore`, `sourceFreshnessScore`, `commercialPageRisk`, `solutionLanguageRisk`, `outdatedSourceRisk` e riscos de desvio corporativo.
- Contenção de contaminação: fontes comerciais, páginas de solução, software, app, automação, curso, template, funil, landing page ou página de preço são classificadas como risco e não avançam como evidência positiva para `source-fetcher`.
- Prevenção: adicionados testes cobrindo busca a partir de queries, bloqueio de fonte comercial/solução, entrega de fontes qualificadas e preservação da classificação pela etapa `source-fetcher`.

## 2026-06-28 — NichoCNAE v3: auditoria OpenAI bruta nos callbacks

- Ajustada a etapa `persona-candidate-generator` para serializar uma única vez o body enviado à OpenAI e reutilizar exatamente esse conteúdo no callback `recebeRequest`.
- Ajustado o callback `recebeResponse` para enviar exatamente o corpo bruto retornado pela OpenAI, incluindo respostas HTTP de erro, antes de qualquer extração ou transformação operacional.
- Prevenção de recorrência: teste da etapa valida que o request auditado no backend é idêntico ao body enviado para a OpenAI e que o response auditado é o corpo bruto recebido do provedor.

## 2026-06-28 — NichoCNAE v3: resposta final limpa da OpenAI no backend

- Causa-raiz: o backend persistia o envelope bruto da OpenAI em `pipeline_nichocnae.response`, o que deixava a tela de auditoria dependente de JSON técnico para encontrar o conteúdo funcional em `content[].text`.
- Correção: adicionada a coluna `pipeline_nichocnae.resposta_final` e normalização compartilhada no callback de response v3 para gravar, em todas as etapas, o texto limpo retornado pela OpenAI quando o envelope trouxer `output[].content[].text` ou `content[].text`.
- Prevenção: teste unitário cobre extração do envelope da Responses API, extração de mensagem direta e fallback para response original quando não houver texto extraível.

## 2026-06-28 — NichoCNAE v3: verificação de objetivos das etapas

- Verificação: revisado o executor `oprm-coletor-mei` em `com.marketinghub.pipelines.nichocnae.v3` contra o objetivo funcional de cada etapa do pipeline.
- Causa-raiz encontrada: a etapa final `persona-routine-materializer` ainda concluía com payload técnico mínimo, sem materializar persona, rotina, dores, evidências e candidato funcional para persistência canônica no backend.
- Correção: `persona-routine-materializer` agora exige aprovação do `quality-gate`, `winnerPersona` e `dailyTasks`, e entrega `materializedProfile`, `marketNicheCandidate` e prontidão explícita para o backend persistir `market_niche` e `market_niche_enrichment_profile`.
- Prevenção de recorrência: adicionado teste unitário cobrindo materialização aprovada e bloqueio quando o `quality-gate` não aprovou.

## 2026-06-28 — OPRM: desativação da rotina legada de importação CNPJ/CNAE

- Causa-raiz confirmada: o `oprm-coletor-mei` mantinha a rotina agendada de importação OPRM CNPJ/CNAE ativa por padrão (`OPRM_MARKET_IMPORT_SCHEDULE_ENABLED:true`), concorrendo com o executor NichoCNAE v3 no mesmo módulo.
- Correção: o padrão da rotina foi alterado para desativado (`false`) e a finalização automática de runs antigas passou a respeitar a mesma flag.
- Prevenção de recorrência: adicionado teste de configuração para garantir que a rotina antiga não volte a iniciar automaticamente por padrão e ajustado o Surefire do módulo para executar testes JUnit 5.

## 2026-06-28 — NichoCNAE v3: prompt da etapa 2 focado em rotina neutra

- Ajustado o prompt da etapa `persona-candidate-generator` para orientar a OpenAI a produzir uma fotografia neutra do dia a dia da persona, sem antecipar dor, oferta, produto, mecanismo, solução ou linguagem comercial.
- Ajustado o schema da etapa para trocar campos de interpretação comercial por campos operacionais: contexto de atuação, fluxo do dia, tarefas recorrentes, interações, ferramentas/registros, decisões pequenas e variações de rotina.
- Prevenção: o builder e o teste funcional da etapa foram alinhados para manter a etapa 2 restrita ao entendimento da rotina antes das etapas posteriores de análise de dor/oportunidade.

## 2026-06-28 — NichoCNAE v3: etapas anteriores alimentando busca de fontes

- Diagnóstico: a execução 4781400 chegou ao `source-searcher` com persona vencedora do tipo cargo interno/CLT (`Estoquista / responsável por recebimento e reposição`) e com campos da IA (`recurringTasks`, `toolsAndRecords`) que as etapas seguintes não liam como `dailyTasks`, `operationalPains` e `buyingSignals`.
- Causa-raiz: desalinhamento de contrato entre a saída real da etapa de IA `persona-candidate-generator` e as etapas determinísticas `persona-tournament`/`routine-query-planner`; isso priorizava cargo de retaguarda e gerava queries fracas para encontrar fonte pública de MEI/autônomo.
- Correção: `persona-tournament` passou a ler aliases reais da IA, normalizar `dailyTasks` para downstream, pontuar melhor dono-operador/MEI/autônomo e penalizar cargos CLT/retaguarda; `routine-query-planner` passou a ler os mesmos aliases e reforçar MEI/autônomo/dono-operador nas queries.
- Ajuste de IA: o prompt da etapa `persona-candidate-generator` agora proíbe escolher funcionário CLT/estoquista/retaguarda como persona central e pede nomes com sinal claro de autonomia.
- Prevenção: adicionados testes cobrindo priorização de dono-operador sobre cargo interno e geração de queries a partir dos aliases reais da IA.

## 2026-06-28 — NichoCNAE v3: diagnóstico e destravamento da etapa 5 por busca curta

- Diagnóstico: a execução do CNAE 4781400 chegou à etapa `source-searcher` com 8 queries planejadas, cada uma retornando resultados brutos, mas todas as fontes foram descartadas antes de formar `foundSources`/`selectedSources`.
- Causa-raiz: as queries da etapa 5 estavam longas e ruidosas demais para busca pública, e o relatório persistido não mostrava o motivo de descarte de cada fonte, deixando o bloqueio correto sem diagnóstico acionável.
- Correção: `source-searcher` agora gera variações curtas de busca a partir da query planejada, mantém a busca Brasil/MEI/autônomo, registra `queryVariants`, preserva fontes rejeitadas com `rejectionReason` e continua bloqueando fontes sem URL, duplicadas, comerciais/solução ou com evidência insuficiente.
- Prevenção: adicionados testes cobrindo busca com query simplificada e auditoria de fontes rejeitadas com motivo explícito.

## 2026-06-28 — NichoCNAE v3: documentação da execução real 4781400

- Atualizado `docs/marketing/pipeline-nichocnae-v3.md` com exemplos reais da execução do CNAE `4781400` na tela `/oprm/cnaes/4781400/pipeline-v3`.
- Registrado que a execução avançou até `source-searcher` e bloqueou com `FONTES_NAO_COLETADAS`, antes do `quality-gate` formal, porque nenhuma fonte pública qualificada de rotina foi encontrada.
- Documentados exemplos reais de personas, tarefas planejadas e motivos de rejeição de fontes: URLs duplicadas, evidência de rotina insuficiente e risco comercial/solução.
