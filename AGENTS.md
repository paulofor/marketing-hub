# AGNETS.md — Contrato operacional

## Missão do sistema

A missão do Marketing Hub é criar produtos digitais que realmente transformem a vida das pessoas, resolvendo necessidades reais com melhoria prática, percebida e aplicável. Lembrando que o consumidor é um ser humano e ele é regido pelo **principio fundamental de afastar a dor e o esforço, buscar a facilidade e o prazer**.

O Marketing Hub é uma fábrica automatizada de produtos digitais: descobre dores reais em nichos específicos, cria ofertas baseadas em mecanismos plausíveis, gera os ativos comerciais, publica experimentos, mede vendas e escala os produtos vencedores.



## 1. Fontes de verdade

- **Liquibase / MySQL 5.7**: `docs/database/liquibase-mysql57.md`. Use sempre `databaseChangeLog` em YAML, `preConditions` com `dbms:mysql`, `splitStatements: true`, `stripComments: true` e valide mentalmente o SQL.
- **Liquibase / includes relativos (obrigatório)**: todo item `include` em changelog mestre que referenciar arquivo por caminho relativo (ex.: `changesets/...`) deve declarar imediatamente `relativeToChangelogFile: true`. Antes de finalizar qualquer changelog mestre, valide no diff que não existe `include` com `file: changesets/...` sem esse parâmetro, para evitar erro de resolução de caminho em execução do Liquibase.
- **Liquibase / MySQL 5.7 — erro 1093 (obrigatório)**: em `UPDATE` ou `DELETE`, nunca leia a mesma tabela-alvo em subconsulta no `WHERE`/`SET` (ex.: `UPDATE pipeline_stage ps ... NOT EXISTS (SELECT 1 FROM pipeline_stage ...)`), pois o MySQL 5.7 pode falhar com `You can't specify target table ... for update in FROM clause`. Para checagens de conflito/idempotência, use `LEFT JOIN ... IS NULL`, tabela derivada materializada em nível seguro ou divida em changelogs/SQLs separados. Antes de finalizar changelog Liquibase, procure mentalmente e no diff por `UPDATE/DELETE <tabela>` combinado com subconsulta da mesma `<tabela>`.
- **Documentos canônicos básicos**: os documentos canônicos básicos do projeto ficam em `/docs/canonical` e devem ser consultados como referência primária.
- **Decisão de mudança de regras (obrigatório)**: quando o usuário decidir mudar regras, altere imediatamente o documento correspondente em `/docs/canonical` referente ao tema.

## 2. Convenções de engenharia

- **Regra Número 1** : O objetivo principal do sistema é gerar VENDAS de produtos que ofereçam valor através do uso da Inteligencia Artificial.
- **Regra Número 2** : Sempre que tiver algum problema não tentar resolver consequencias. Buscar SEMPRE resolver a causa-raiz.
- **Prevenção obrigatória de recorrência**: sempre que estiver resolvendo um problema, antes de propor a solução, faça internamente para o próprio raciocínio do modelo as perguntas: por que o problema aconteceu e como evitar que ele volte a acontecer. Quando resolver a causa-raiz exigir decisão do usuário para realizar uma alteração, comunique essa necessidade ao usuário de forma objetiva, direta e simples.
- **Confirmação de hipóteses pelo histórico (obrigatório)**: antes de transformar uma hipótese em causa-raiz ou implementar correção, use o passado disponível para confirmar ou descartar a hipótese. Isso inclui comparar execuções anteriores bem-sucedidas e falhas do mesmo fluxo/entidade, dados já persistidos no banco, logs históricos, registros em `/docs/registros` e documentação oficial quando houver limite/contrato externo envolvido. Se o histórico contradisser a hipótese, não implemente a correção baseada nela; registre a conclusão e siga investigando a causa real.
- **Revisão obrigatória após correção de problema**: sempre que corrigir um problema, verificar novamente se ainda é possível corrigir a causa-raiz desse problema; se for possível, priorize a correção da causa-raiz antes de considerar a tarefa concluída.
- **Consulta obrigatória de loops conhecidos**: antes de corrigir problema recorrente ou investigar falha em GeraLanding, Facebook Ads, Lead Portal, OpenAI/schema, pipelines administrativos ou pipeline de hipótese, consultar `docs/registros/loops.md`. Se o problema corresponder a um `LOOP-*`, a correção deve fechar a causa-raiz sistêmica, atualizar ou criar teste de contrato que previna recorrência e registrar no documento de tema correspondente.
- **Regra Número 3** : Seja SIMPLES, OBJETIVO e EFICAZ.
- **Respostas ao usuário nesta interação**: ao responder o usuário, priorize um nível mais alto, conceitual, objetivo e orientado ao negócio; baseie as respostas principalmente na análise do código, do banco de dados e dos logs, e só depois complemente com a documentação quando necessário; reduza detalhes técnicos, comandos e implementação interna ao mínimo necessário para sustentar a decisão ou o próximo passo.
- **Tamanho das respostas ao usuário**: manter respostas simples, objetivas e no tamanho aproximado da resposta anterior validada pelo usuário nesta conversa; usar seções curtas, bullets e apenas os detalhes necessários para sustentar a conclusão, decisão ou próximo passo.
- invesgtigação da causa raiz
- Se for um erro na tela:
- 1. Pesquisar o dado da tela de qual endpoint ele vem
- 2. Identificar de qual base de dados vem
- 3. Analisar se no banco de dados esta correto ou não
- 4. Se não estiver correto pesquisar qual endpoint ou classe grava esse item na tabela.
- 5. Verificar se existe logs desse endpoint que faz gravações
- 6. Verificar a origem do dados antes da gravação.
- 7. Se não tiver informações suficientes para chegar em uma conclusão segura coloque logs e solicite nova execução.
- **Investigação de comando do front-end que não funciona (procedimento obrigatório)**: quando o usuário solicitar a investigação do motivo de um comando no front-end não estar funcionando, siga obrigatoriamente este fluxo ponta a ponta, buscando a causa-raiz e comprovando cada etapa antes de concluir:
  1. Identificar qual endpoint é chamado pelo comando no front-end.
  2. Identificar qual controller do backend está sendo chamado por esse endpoint.
  3. Identificar qual service é chamado pelo controller.
  4. Identificar qual mudança no banco de dados é feita pelo service.
  5. Usando o MCP Server, verificar se a alteração foi feita no banco de dados.
  6. Se essa mudança for para disponibilizar esse registro, descobrir isso analisando os outros métodos do service e o fluxo de disponibilização do dado.
  7. Identificar qual endpoint expõe esse dado para consumo.
  8. Identificar qual módulo chama esse endpoint.
  9. Verificar no log desse módulo se esse endpoint está sendo chamado.
  10. Verificar no log do módulo se os parâmetros enviados estão corretos.
  11. Verificar no log do módulo o que é feito ao chegar essa informação.
  12. Verificar no log se a ação esperada está sendo feita.
  13. Verificar no log se a resposta da ação aconteceu.
  14. Verificar no log se o tratamento da resposta foi feito corretamente.
  15. Verificar se o backend foi novamente chamado pelo módulo para registrar o resultado da ação.
  16. Verificar se o backend registrou corretamente o resultado da ação.
- **Servidor MCP** : Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC. Quando precisar analisar casos específicos acesse o banco de dados usando esse servidor.
- **Tecnologias padrão**: Java 21 + Spring Boot 3, React 18 + Vite + TypeScript, Zustand para state, TanStack Query para dados. Formatação: Spotless (backend) e Prettier (frontend).
- **Banco**: MySQL 5.7. Somente o backend acessa o banco; demais módulos conversam via APIs do backend. Prefira filtros no SQL ao invés de pós-processar em memória.
- **JPA/Hibernate x Liquibase (bloqueio de backend)**: nunca confie apenas na nomenclatura automática do Hibernate para colunas já definidas por Liquibase, principalmente campos booleanos ou com siglas/acrônimos (`OpenAI`, `URL`, `HTML`, `ID`, etc.). Toda entidade/campo novo ou alterado que tenha coluna canônica deve declarar `@Column(name = "nome_canonico_no_banco")` quando houver qualquer risco de divergência. Antes de inserir dados por Liquibase em tabela com colunas `NOT NULL`, compare o changelog, a entidade JPA e o schema real (via MCP quando necessário) para impedir colunas duplicadas/legadas que travem o bootstrap do backend.
- **URL do BACKEND** : http://191.252.181.168:8000
- **URL do BACKEND (Codex)** : para acessos executados pelo Codex, usar preferencialmente `http://191.252.181.168` (porta 80); o backend também responde nessa porta.
- **Modelo único**: entidades residem no backend. Os demais módulos acessam o banco de dados pelo backend.
- **Fluxo entre containers**: nada de chamadas diretas entre serviços (frontend, workers, lead-portal etc). Todo tráfego passa pelo backend principal; apenas o backend fala com o banco.
- **Controle de avanço de pipeline (obrigatório)**: em qualquer pipeline, a decisão e o controle de passar uma execução de uma etapa para outra devem ser feitos exclusivamente no backend. Frontend e módulos de apoio (AI Worker, Facebook Ads Worker, coletores, workers e demais executores) podem executar etapas, consultar filas e reportar resultado/status ao backend, mas nunca devem orquestrar, decidir ou disparar diretamente a próxima etapa do pipeline.
- **Persistência e relatório de execução de pipeline (obrigatório)**: todo fluxo de pipeline deve persistir dados suficientes de execução para gerar relatório claro ao usuário pelo frontend. Isso inclui, no mínimo, job/ciclo, etapa, status, horários, entradas e saídas estruturadas, artefatos, decisões, motivos de reprovação, falhas técnicas, custos quando existirem e referência às fontes/evidências usadas. O frontend não deve depender de logs técnicos ou recomputação para explicar o andamento do pipeline; o backend deve expor dados persistidos e auditáveis para a tela de relatório.
- **Versionamento de pacotes de pipeline (obrigatório)**: ao criar ou reestruturar um pipeline inteiro, considere que pode existir a necessidade legítima de manter versões paralelas do fluxo. Mesmo na primeira criação, o pipeline deve nascer como `v1` no nome do pacote e dos contratos, porque uma mudança completa futura poderá exigir `v2` sem sobrescrever a versão inicial. O nome do pacote do pipeline deve carregar explicitamente o número da versão tanto no módulo executor quanto no backend (ex.: `...nichocnaev1.pipeline` ou `...nichocnaev2.pipeline` no executor e `...nichocnae.v1.<etapa>` ou `...nichocnae.v2.<etapa>` no backend), preservando compatibilidade, rollout gradual e rollback seguro. A versão nova deve continuar plugável: núcleo genérico separado das etapas concretas, etapas independentes entre si e comunicação por contratos/artefatos/endpoint `pending`, nunca por acoplamento direto entre implementações de etapas.
- **Controle de execução de rotinas/agendamentos (obrigatório)**: ajustes de execução operacional de rotinas devem ficar no módulo executor responsável pela rotina, nunca no backend principal. Isso inclui cron, polling, frequência, janela de execução, pausa/retomada operacional, retries locais e qualquer controle de quando o módulo roda. O backend principal deve apenas entregar contratos/dados, expor pendências e receber status/resultados. Exceção: quando a solicitação for explicitamente uma configuração ou comando de tela administrativa, o backend pode persistir e expor essa configuração/comando para a UI, mas a rotina agendada continua sendo executada e controlada pelo módulo executor.
- **Modelos de IA / prompt, schema, auditoria e flex (obrigatório)**: sempre que qualquer fluxo usar modelo de IA para gerar, revisar, classificar, enriquecer ou transformar dados, o prompt operacional e o schema JSON de saída devem ficar em arquivos versionados do módulo executor/worker responsável, seguindo o padrão do GeraLanding (`src/main/resources/prompts/.../*.md` e `*-schema.json`). Não deixe prompt longo, contrato de saída ou schema hardcoded em classe Java/TypeScript. A classe deve apenas carregar o prompt/schema do classpath, resolver placeholders com o contexto persistido pelo backend, montar a requisição e validar a resposta. Toda interação com modelo de IA deve registrar de forma auditável o request enviado e o response bruto recebido, vinculados ao job/execução/entidade do Marketing Hub, além de modelo, status, erro, tokens/custo quando disponíveis. Quando o provedor for OpenAI, a request deve usar modo Flex (`service_tier: "flex"`) por padrão; exceções só são permitidas quando houver justificativa funcional explícita e registrada. Quando o fluxo depender de contexto de nicho, hipótese, experimento ou pipeline, o backend deve persistir/expor o contexto rico necessário e o worker deve injetá-lo no prompt por placeholder, sem recomputar no frontend e sem consultar banco diretamente.
- **Escopo de controllers por módulo**: cada módulo só pode acessar os controllers do próprio módulo (ex.: MOIS usa apenas controllers do pacote MOIS; OPRM usa apenas controllers do pacote OPRM; e assim sucessivamente). É proibido um módulo consumir controllers de outro módulo diretamente.
- **Novos endpoints**: verifique se o contrato já existe; caso contrário, defina-o no backend, atualize a documentação e adicione testes.
- **Manual do usuário**: todos os links devem usar `target="_blank"`.
- **Frontend**: sempre que alterar o frontend crie os métodos do backend para suportar. Tanto back quanto o front estão sendo executados no mesmo host.
- **Frontend → Backend (validação de endpoint)**: sempre que criar no frontend uma chamada para o backend, verifique primeiro se o endpoint já existe. Se não existir, crie o endpoint no pacote/backend referente ao assunto da tela (ex.: MOIS, OPRM, etc.), respeitando o escopo do módulo.
- **Novo pedido no frontend (procedimento obrigatório)**: quando o usuário solicitar algo novo no front-end, siga obrigatoriamente este fluxo:
  1. Investigar qual módulo do backend se refere ao pedido.
  2. Verificar se já existe endpoint que atenda ao pedido.
  3. Se o endpoint existir, verificar se o módulo está dentro do padrão atual de arquitetura do backend.
  4. Se estiver dentro do padrão atual, usar esse endpoint e alterar o frontend.
  5. Se não estiver dentro do padrão atual, criar um novo endpoint dentro do padrão, documentar no Swagger, registrar o endpoint antigo em `/docs/backend/fora-padrao.md` e alterar o frontend para usar o endpoint novo.
- **Qualidade**: sempre que alterar um módulo Java realizar os testes unitários antes de publicar o PR.
- **Documentação em código Java (obrigatório)**: toda classe Java deve conter comentário descrevendo sua responsabilidade básica, e todo método deve conter um comentário breve explicando o que ele faz.
- **Idioma dos comentários em classes Java (obrigatório)**: todos os comentários dentro de classes Java devem ser escritos em português, incluindo comentários de responsabilidade da classe, comentários de métodos e comentários internos.
- **Classes alteradas sem comentário (obrigatório)**: toda classe Java alterada que ainda não possui comentário de responsabilidade básica deve ser atualizada no mesmo PR para incluir esse comentário.
- **Responsabilidade única da classe (obrigatório)**: cada classe Java deve ter apenas uma única responsabilidade básica; se houver acúmulo de responsabilidades, refatore antes de ampliar o comportamento.
- **Leitura prévia obrigatória (classe e método)**: antes de alterar qualquer classe/método Java, ler primeiro os comentários de responsabilidade básica da classe e a descrição do que cada método faz, para manter aderência ao design pretendido.
- **Execução operacional obrigatória (comentários de responsabilidade)**: para eliminar ambiguidade, todo trabalho em Java deve seguir este fluxo mínimo: (1) antes de editar, ler os comentários da classe e dos métodos; (2) ao alterar método existente, revisar e atualizar o comentário do método quando o comportamento mudar; (3) ao criar método novo, adicionar comentário breve no próprio método no mesmo commit; (4) ao alterar classe sem comentário de responsabilidade, incluir o comentário da classe no mesmo PR; (5) antes de abrir PR, validar que nenhuma classe Java alterada ficou sem comentário de responsabilidade e que nenhum método alterado/novo ficou sem comentário.
- **Regra de bloqueio de PR (Java)**: PR com alteração em Java que não cumpra integralmente o fluxo de comentários de responsabilidade (classe + métodos) deve ser considerado incompleto e não pode ser finalizado.
- **Logs**: os logs dos modulos Java Spring Boot podem ser acessados pelo MCP Server.  Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC.
- **Logs de exceção capturada (obrigatório)**: todo bloco `catch` que captura `Exception`, `RuntimeException` ou exceção de integração deve registrar log antes de relançar, converter ou responder erro. O log deve incluir contexto operacional suficiente para localizar o ponto da falha (módulo, operação, identificadores relevantes como `experimentId`, `landingId`, URL/endpoint quando aplicável) e a exceção completa (`ex`) para preservar stack trace; não basta usar apenas `NestedExceptionUtils.getMostSpecificCause(ex).getMessage()` ou somente a mensagem da causa raiz.
- **Agendamentos Spring Boot**: quando criar rotinas agendadas com `@Scheduled`, não usar variáveis para o cron; definir diretamente a string no formato cron na anotação.
- **Logs de ingestão (obrigatório)**: sempre que for construído um fluxo de ingestão de dados, registrar em log o dado bruto recebido da fonte (payload original), antes de qualquer transformação/normalização.
- **Testes Unitários**: os testes unitários precisam sempre estar em concordancia com as regras dos documentos canonicos.
- **Regra de mensagens ArchUnit (obrigatório)**: toda mensagem de falha em testes de arquitetura/ArchUnit deve iniciar com o prefixo literal `[ARQUITETURA] ` (incluindo mensagens de `.because(...)`, descrições de `ArchCondition` e mensagens de `SimpleConditionEvent`).
- **Padrão obrigatório para regras de Arquitetura (ArchUnit)**: quando a regra envolver restrição de dependência por contexto/pacote (ex.: `geralanding.*`), priorizar o padrão explícito com `classes().that()...should(ArchCondition customizada)` + mensagens detalhadas via `SimpleConditionEvent`, no mesmo estilo já validado no backend (ex.: `onlyDependOnProvisorioWithinSameStage`). Evitar depender apenas de encadeamentos genéricos `noClasses().should().dependOnClassesThat()...andShould()` quando houver risco de falso positivo/falso negativo.
- **Protocolo padrão backend (gatilho obrigatório)**: sempre que o usuário solicitar literalmente `aplique o protocolo padrão backend`, adicionar ou ajustar no backend (`backend/ads-service/src/test/java/com/marketinghub/architecture/ArquiteturaTest.java`) regras ArchUnit para o pacote/módulo alvo com a mesma configuração arquitetural padrão baseada nos pontos de sucesso do GeraLanding: separação por etapa, controller único/canônico por etapa, service canônico obrigatório por etapa, endpoint interno `pending` obrigatório por etapa como ponto inicial canônico de consumo da fila pelo executor, callbacks canônicos de recebimento de prompt/entrada quando houver IA ou integração externa, callback canônico de resposta/resultado, DTOs/contratos como `record` em subpacotes de service, persistência de execução por etapa com status, entrada, saída, erro, custo quando existir, evidências/artefatos e decisão de próxima etapa, e artefato consolidado separado de metadados técnicos. O endpoint `pending` deve seguir o padrão `/api/internal/<dominio>/<etapa>/stage-executions/pending` (ou variação canônica documentada). O backend deve ser a fonte de verdade do avanço: ao receber sucesso funcional, enfileira automaticamente a próxima etapa quando ela depender apenas da etapa anterior; quando houver risco comercial, gasto, publicação, aprovação humana ou contrato inválido, deve bloquear e expor causa/ação em dados persistidos, nunca apenas em log. Quando o pacote representar um pipeline inteiro, a versão deve aparecer explicitamente no backend desde a primeira versão, antes da etapa (ex.: `com.marketinghub.<dominio>.<pipeline>.v1.<etapa>` e `/api/internal/<dominio>/<pipeline>/v1/<etapa>/stage-executions/pending`; para uma mudança completa futura, usar `v2`) para permitir convivência entre versões, rollout gradual e rollback. A configuração deve ficar obrigatoriamente no arquivo `backend/ads-service/src/test/java/com/marketinghub/architecture/ArquiteturaTest.java`, usando o padrão explícito `classes().that()...should(ArchCondition customizada)`, mensagens iniciadas por `[ARQUITETURA] ` e validações estruturais com `JavaClasses` quando precisar contar controllers/services por pacote. Ao aplicar este protocolo, registrar obrigatoriamente em `/docs/registro-protocolo/padrao-backend.md` qual pacote/módulo recebeu o protocolo e a data de aplicação.
- **Protocolo padrão módulo (gatilho obrigatório)**: sempre que o usuário solicitar literalmente `aplique o protocolo padrão módulo` ou `aplique protocolo padrão módulo`, aplicar o padrão descrito em `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md` **somente no módulo executor/worker responsável por executar o fluxo**. **Nunca aplicar este protocolo no backend principal** (`backend/ads-service`) apenas porque o backend expõe endpoints, persiste estado ou possui contratos de API. Para backend, use exclusivamente o gatilho separado `aplique o protocolo padrão backend`.
  1. **Primeira decisão obrigatória**: identificar qual módulo realmente executa as etapas (ex.: `oprm-coletor-mei`, `facebook-ads-worker`, `ai-worker`, `mois-sales-library-worker`). O backend principal só entra no escopo do protocolo padrão módulo se o usuário disser explicitamente que o módulo-alvo é o backend como executor, o que deve ser tratado como exceção rara e confirmada no cânone.
  2. **Codificação por etapa no executor**: organizar o módulo executor como motor genérico de etapas, com núcleo em pacote raiz do pipeline e implementações concretas em subpacotes por etapa (`pipeline.<etapa>` ou pacote equivalente já canônico no executor). Toda criação de pipeline inteiro deve explicitar a versão desde a primeira versão (ex.: `nichocnaev1.pipeline.<etapa>`); quando a mudança representar uma nova versão inteira, criar outro pacote versionado (ex.: `nichocnaev2.pipeline.<etapa>`) em vez de sobrescrever silenciosamente o pacote da versão anterior.
  3. **Núcleo genérico no executor**: manter no núcleo apenas contratos e orquestração genérica, como `PipelineWorker`, `StageProcessor`, `StageContext`, `StageResult`, `StageArtifact`, `ArtifactStore`, portas para backend e handlers de resposta. O núcleo não pode conhecer etapa concreta nem tecnologia específica.
  4. **Etapas plugáveis**: cada etapa concreta deve ser independente, removível e substituível, implementando o contrato de processamento da etapa e mantendo seus inputs, outputs, propriedades, configurações, clients e detalhes tecnológicos dentro do próprio pacote da etapa.
  5. **Proibição de acoplamento entre etapas**: uma etapa concreta não pode importar ou chamar diretamente outra etapa concreta. Quando precisar consumir resultado anterior, deve usar contrato persistido no backend, artefato auditável, storage key, DTO oficial, evento de fila ou outro contrato canônico, nunca classe concreta da etapa anterior.
  6. **Tecnologias concretas isoladas no executor**: OpenAI, WebClient, Jsoup, Playwright, Selenium, S3, parsers de PDF, clientes HTTP e integrações externas devem ficar dentro da etapa concreta do executor ou infraestrutura compartilhada permitida, nunca no núcleo genérico do pipeline e nunca deslocadas para o backend por causa deste protocolo.
  7. **Backend fora do protocolo módulo**: o protocolo padrão módulo, por si só, não autoriza nenhuma alteração no backend. Se existir uma necessidade separada de suporte ao executor, ela deve ser tratada como demanda própria de contrato/API/persistência do backend, nunca como parte do protocolo padrão módulo. Não criar núcleo `pipeline`, `StageProcessor`, `StageContext`, `StageResult` ou regras ArchUnit de protocolo módulo no backend quando o fluxo é executado por worker/módulo externo. Em PRs/documentação desse protocolo, não declarar alteração ou teste de backend se o diff não alterou backend.
  8. **Ponto inicial de acesso ao backend**: ao aplicar o protocolo padrão módulo, o executor deve iniciar o consumo do trabalho sempre pelo endpoint `pending` canônico exposto pelo backend para a etapa/fila, no mesmo padrão do protocolo padrão backend: `/api/internal/<dominio>/<etapa>/stage-executions/pending` ou variação canônica documentada para o fluxo. O módulo executor não deve buscar diretamente registros por endpoints administrativos, consultas paralelas ou atalhos de banco para descobrir trabalho pendente; após receber a execução pelo `pending`, deve executar a etapa e reportar status/resultado ao backend pelos contratos oficiais do fluxo.
  9. **Contrato completo de entrada e saída por etapa**: repetir o ponto de sucesso do GeraLanding em todos os pipelines: cada etapa concreta deve declarar entrada mínima, saída funcional estruturada, artefatos, critérios de conclusão, critérios de bloqueio, aliases aceitos somente quando documentados e próxima etapa permitida. É proibido retornar `nextStageCode` para etapa sem contrato backend/executor completo, sem endpoint `pending` e sem processor registrado no catálogo do executor.
  10. **Callbacks e validação de resposta**: etapas com IA ou integração externa devem separar montagem de request/prompt/schema, validação de resposta, handler de aplicação e callback de resultado, preservando request bruto, response bruto, modelo/integração, status, erro, tokens/custo quando houver e correlação por `jobId`/execução. Quando uma etapa de pipeline acessar qualquer API externa, é obrigatório registrar log do request enviado, da resposta recebida e da URL chamada, com correlação da execução/job para auditoria e diagnóstico. O executor nunca deve tratar resposta inválida como sucesso técnico.
  11. **Artefatos e saída estruturada**: toda etapa que capture, gere ou transforme dado relevante deve registrar artefatos auditáveis (`StageArtifact` ou equivalente), separando payload bruto, metadados técnicos internos e saída funcional estruturada. Não serializar JSON dentro de JSON sem contrato explícito. Artefato final publicável/consumível deve ser separado de auditoria, payload bruto, flags internas, comentários técnicos e dados de debug.
  12. **Gates e bloqueios orientados à causa-raiz**: todo pipeline deve ter gate funcional equivalente ao risco do domínio antes de publicar, materializar, gastar mídia, avançar para oferta ou encerrar como sucesso. O gate deve apontar causa-raiz, impacto comercial e etapa recomendada de correção, seguindo o aprendizado do Quality Review do GeraLanding.
  13. **Relatório ao usuário baseado em dados persistidos**: ao aplicar o protocolo em qualquer pipeline, garantir que cada etapa reporte ao backend dados persistíveis suficientes para o frontend montar relatório de execução do usuário, incluindo andamento, evidências, decisões, custos, erros e próximos movimentos. Logs técnicos não substituem a persistência funcional do relatório.
  14. **ArquiteturaTest obrigatório no módulo executor**: no mesmo PR, adicionar ou ajustar regras ArchUnit no arquivo de arquitetura do módulo executor (ou teste equivalente do próprio worker/módulo) para validar: núcleo não depende de etapas concretas; etapas não dependem umas das outras; etapas não têm ciclos; processors concretos implementam o contrato de etapa; núcleo não depende de tecnologias concretas.
  15. **Padrão ArchUnit obrigatório**: as regras devem usar mensagens iniciadas por `[ARQUITETURA] ` e, quando envolver restrição por pacote/contexto, priorizar `classes().that()...should(ArchCondition customizada)` com `SimpleConditionEvent` detalhado, evitando regras genéricas que possam gerar falso positivo ou falso negativo.
  16. **Validação antes do PR**: rodar os testes unitários/ArchUnit do módulo executor alterado e revisar no diff se não existe importação de etapa concreta pelo núcleo, dependência direta entre etapas, tecnologia concreta no núcleo ou processor de etapa fora do contrato.
  17. **Registro obrigatório de aplicação**: ao aplicar este protocolo, registrar obrigatoriamente em `/docs/registro-protocolo/padrao-modulo.md` qual pacote/módulo executor recebeu o protocolo e a data de aplicação.
- **Protocolo leitura escrita (gatilho obrigatório)**: sempre que o usuário solicitar literalmente `aplique o protocolo leitura escrita` ou `aplique protocolo leitura escrita` para um pacote do backend, adicionar ou ajustar no backend (`backend/ads-service/src/test/java/com/marketinghub/architecture/ArquiteturaTest.java`) regras ArchUnit que protejam esse pacote contra controle operacional de execução que deve ficar em módulos externos. O objetivo é garantir que o pacote backend permaneça responsável por leitura, escrita, contratos, persistência, publicação de pendências e recebimento de status/resultados, sem assumir rotina de execução operacional. A regra deve bloquear, no pacote alvo, responsabilidades como `@Scheduled`, polling, retries locais, janela/frequência de execução, workers/runners/processors operacionais, núcleo `PipelineWorker`, `StageProcessor`, `StageContext`, `StageResult`, `StageArtifact`, chamadas diretas a tecnologias de execução externa (OpenAI, WebClient operacional para fonte externa, Jsoup, Playwright, Selenium, parsers/downloaders externos, S3 operacional quando aplicável) e qualquer cálculo/enriquecimento/scraping/chamada externa que pertença ao worker/coletor/módulo executor. A regra deve permitir apenas responsabilidades de backend: controllers/contratos do próprio módulo, endpoint `pending` quando aplicável, callbacks de resultado, services de leitura/escrita/validação técnica, repositories canônicos, DTOs/records, auditoria de estado e comandos administrativos explícitos. Implementar com padrão explícito `classes().that()...should(ArchCondition customizada)` + `SimpleConditionEvent`, mensagens iniciadas por `[ARQUITETURA] `, evitando whitelist ampla ou regra genérica que gere falso positivo/falso negativo. Antes de aplicar, identificar qual módulo externo executa o fluxo e registrar essa separação na mensagem da regra. Ao aplicar este protocolo, registrar obrigatoriamente em `/docs/registro-protocolo/leitura-escrita.md` qual pacote backend foi protegido, qual módulo executor externo mantém o controle operacional e a data de aplicação.
- **Protocolo jobid (gatilho obrigatório)**: sempre que o usuário solicitar literalmente `aplique protocolo jobid` ou `aplique o protocolo jobid` em um fluxo/processo, implementar rastreabilidade operacional ponta a ponta baseada em `jobId`: (1) criar ou reutilizar uma tabela de passos do job com `job_id`, data-hora, etapa, status, payload enviado, payload recebido, endpoint/ação e erro, podendo a mesma tabela atender todas as etapas do pacote/fluxo quando o contexto for o mesmo, sem criar uma tabela por etapa; (2) gerar o `jobId` no backend/orquestrador no primeiro momento em que o processo for disparado, preferencialmente como hash estável da liberação/execução; (3) expor o `jobId` no contrato entregue ao worker/módulo executor; (4) criar endpoint no backend para o executor registrar cada passo; (5) registrar o passo inicial no backend/orquestrador; (6) registrar no endpoint cada interação externa ou etapa crítica do executor, sempre com payloads estruturados e sem JSON dentro de JSON; (7) documentar o contrato no Swagger/cânone correspondente, adicionar registro em `/docs/registros` do tema e registrar obrigatoriamente em `/docs/registro-protocolo/jobid.md` qual pacote/fluxo recebeu o protocolo e a data de aplicação.
- **Protocolo monitor (gatilho obrigatório)**: sempre que o usuário solicitar literalmente `aplique protocolo monitor` ou `aplique o protocolo monitor` para uma versão de pipeline, aplicar o padrão canônico em `docs/canonical/protocolo-monitor.md` para que o Ops Monitor acompanhe a saúde operacional do fluxo, não apenas o health HTTP do container. A implementação deve declarar o módulo executor responsável, a versão do pipeline, a tabela/contrato backend que representa fila ou execução, o status pendente monitorado, o limite máximo aceitável sem consumo, severidade/criticidade, mensagem operacional com job/entidade/etapa parada e comportamento na tela (`DEGRADED`, incidente aberto ou ambos). O backend principal deve continuar como fonte de verdade de fila/status/relatório; o módulo executor continua responsável por agendamento, polling, retries e execução; o Ops Monitor apenas observa sinais persistidos e/ou heartbeats públicos e não executa etapas nem decide transições. Ao aplicar este protocolo, adicionar teste unitário/contrato cobrindo incidente/degradação, registrar em `docs/registros/<tema>.md`, registrar em `docs/registros/ops-monitor.md` e registrar obrigatoriamente em `/docs/registro-protocolo/monitor.md` qual pipeline/versão, módulo executor, sinal operacional, comportamento no Ops Monitor e data de aplicação.
- **ArquiteturaTest** : Somente faça mudanças no ArquiteturaTest quando for diretamente solicitado.
- **Regra Geral (cânone x testes)**: sempre que houver alteração de regra em documento canônico, revisar e atualizar os testes unitários relacionados para manter aderência entre documentação, regras de domínio e validações automatizadas.
- **Telas do Usuario**: as telas de usuario, ou frontend precisam sempre estar dando as informações mais importantes e precisas para  o usuario e ofereçendo so comandos necessários para o direcionamento dos fluxo de processos mantidos pelo sistema. Evite informações contraditórias, em excesso e desorganizadas. Mantenha sempre a conformidade com os documentos canonicos.
- **Perguntas sobre assunto** : quando o usuário perguntar sobre um assunto:
  1. Verifique inicialmente do que se trata e qual parte do sistema esta envolvida
  2. Verifique relacionado a esse assunto tabelas e banco de dados usando o MCP Server ( se não conseguir tente com timeout maior e varias vezes )
  3. Verifque logs relacioandos usando também o MCP Server
  4. Indique telas do front end onde o usuário pode obter dados referente ao assunto.
  5. Indique documentos canonicos sobre o assunto
  6. Seguindo os objetivo maiores do sistema que é vendas. Sugira o que pode ser feito a seguir.
  7. Se existir um problema detectado pesquise e sugira a solução sempre buscando causa-raiz.
- **Registros após tarefas** : sempre que for feito alguma tarefa faça o registro dela no local adequado:
     1. Tema: Experimentos, registre em /docs/registros/experimentos.md
     2. Tema: Módulos Mois, registre em /docs/registros/mois1.md
     3. Tema: Módulos OPRM, regsitre em /docs/registros/oprm1.md

## 3. Orientações Práticas:

- **Teste de acesso ao banco via MCP (comando validado)**: comando usado na rodada que retornou 100% de sucesso (10/10 respostas HTTP 200 e sem erro JSON-RPC) para validar conectividade com o banco via tool `db_health`:
  ```bash
  python - <<'PY'
  import json,subprocess,statistics
  url='https://mcpserverdigi.shop/mcp'
  payload={"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"db_health","arguments":{}}}
  times=[]; codes=[]; errs=0
  for _ in range(10):
      cmd=['curl','-sS','-o','/tmp/mcp_db_test.json','-w','%{http_code} %{time_total}',url,'-H','Content-Type: application/json','--data',json.dumps(payload)]
      out=subprocess.check_output(cmd,text=True).strip().split()
      code=int(out[0]); t=float(out[1]); codes.append(code); times.append(t)
      body=open('/tmp/mcp_db_test.json').read()
      if '"error"' in body or code != 200:
          errs += 1
  print('codes=',codes)
  print('errors=',errs)
  print('avg_s=',round(statistics.mean(times),3),'max_s=',round(max(times),3))
  PY
  ```

- **MCP com timeout alto (comando validado)**: quando houver instabilidade/intermitência no endpoint MCP, use um timeout mais alto, exemplo:
  ```bash
  curl -sS --max-time 90 https://mcpserverdigi.shop/mcp -H 'Content-Type: application/json' --data '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | head -c 4000
  ```

- **MCP Server — filtros disponíveis para pesquisa de logs (referência operacional)**:
  - Tool de logs dos módulos Java: `java_module_logs`.
  - Módulos aceitos em `module`: `backend`, `ai-worker`, `lead-portal`, `facebook-ads`, `email-service`, `lead-portal-payment`, `mds`, `mois`, `mois-sales-library-worker`, `mois-hotmart`, `clickbank-coletor-mois`, `oprm-coletor-receita`.
  - Observação importante de nomenclatura: para o coletor OPRM de MEI/CNPJ, a aplicação/serviço no projeto está nomeada como `oprm-coletor-mei`, porém no MCP o identificador aceito em `module` continua sendo `oprm-coletor-receita` (alias legado de integração do módulo de logs).
  - Filtros/parâmetros disponíveis:
    1. `module` (**obrigatório**): módulo Java de origem do log.
    2. `contains`: filtro por texto literal contido na linha (não é regex).
    3. `from`: data/hora inicial em ISO-8601 UTC (ex.: `2026-05-23T03:00:00Z`).
    4. `to`: data/hora final em ISO-8601 UTC (ex.: `2026-05-23T03:20:00Z`).
    5. `lines`: quantidade de linhas por página (mín. 1, máx. 500; padrão 200).
    6. `offset`: paginação por deslocamento dentro do conjunto filtrado.
    7. `cursor`: paginação por cursor retornado em `nextCursor`.
  - Limitações conhecidas:
    - as URLs diretas de log dos serviços podem existir na configuração do MCP, mas normalmente não são acessíveis diretamente pelo ambiente do Codex; para investigar logs reais, tente sempre pela tool `java_module_logs` do MCP Server em vez de acessar essas URLs diretas.
    - o filtro `contains` é literal (sem regex/full-text avançado);
    - a retenção/janela disponível depende do `actuator/logfile` do módulo (logs antigos podem não estar mais disponíveis);
    - para logs de GitHub Actions, a tool é `github_actions_get_run_logs` e aceita apenas `run_id` (sem `contains`, `from`, `to`).

- **Erro 400 (Bad Request) em APIs**: em validações de contrato/payload, o backend pode responder `400 Bad Request` sem registrar detalhes úteis no log de aplicação. Nesses casos, **não basta procurar logs**; é obrigatório inspecionar a requisição enviada (URL, método, headers, body) e comparar com DTO/contrato esperado para identificar campo, estrutura ou tipo inválido.

- **Lição de investigação — validar o fluxo completo antes de corrigir (obrigatório)**: quando um sintoma indicar falha em qualquer fluxo do sistema, não conclua a causa-raiz apenas pelo estado exibido na tela, pelo primeiro erro visível ou pela hipótese mais provável. Antes de implementar correção, percorra o fluxo ponta a ponta: identifique a origem do dado/comando, confirme o endpoint/contrato usado, reproduza a chamada real com payload equivalente, verifique logs dos módulos envolvidos, valide o registro ou alteração no banco e compare contrato, DTO/entidade e schema real quando houver persistência. Só defina a causa-raiz após confirmar em qual etapa o fluxo quebra. Aprendizado registrado em 2026-06-25: a primeira decisão foi incorreta porque a análise parou em uma hipótese intermediária e não validou a etapa final de persistência do resultado.

- **json** : temos que evitar ao máximo json dentro de json. Ou seja json em campo texto de outro json. 


## 4. Framework central do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

Ao alterar qualquer módulo, preserve esse eixo como referência principal de descoberta, modelagem, validação e empacotamento de valor.



## 5. Módulos e responsabilidades

- **MarketingHub Backend / Frontend**: camada administrativa e UI principal do sistema.
- **Facebook Ads Worker**: integração com a API da Meta para campanhas e públicos.
- **Worker AI**: integrações com modelos OpenAI para geração e otimização de ativos; etapas assíncronas por fila/callback devem seguir o núcleo `com.marketinghub.worker.openai.core.<etapa>`.
- **Lead Portal (backend/frontend)**: experiência dedicada aos leads após anúncios.
- **Lead Portal Payments Service**: pagamentos via Mercado Pago.
- **Email Service**: envio transacional integrado ao Amazon SES.
- **Image Watermark Service**: gera marcas-d'água para prévias.
- **Image Zipper Service**: monta e distribui pacotes de produtos/amostras.
- **MCP Server**: servidor de mcp, fica na pasta /mcp-server
- **OPRM** : responsavel por obter a rotina de uma determinada ocupação, importante para entender de forma clara e precisa as dificuldade e dores de um determinado mercado, através da busca concreta em acesso a sites especializados direcionados para o nicho.
- **MDS** : modulo que vai buscar na internet artigos e informações cientificas de ponta e de credibilidade para dar apoio na construção de mecanismos eficazes que vão resolver de fato os problemas do mercado em relação a uma dor. Com esse mecanismo o Marketing Hub usa como base para criar produtos digitais transformadores de fato.

Documente qualquer alteração cross-módulo no cânone correspondente e sincronize contratos antes de integrar.


## 6. Dominios

- **oportunidadebrasil.shop** : apontando para 191.252.120.96
- **pagamentopalf.site** : apontando para  191.252.102.54


## 7. Segurança e secrets

- Nunca commite `.env` ou credenciais. Use GitHub Actions secrets.
- Revise variáveis sensíveis nos pipelines antes de publicar artefatos.

- 🚨 **CONTAMINAÇÃO DE ARTEFATO FINAL COM METADADO TÉCNICO (OBRIGATÓRIO)**:
  - **Definição**: é proibido inserir no artefato final publicado qualquer marcador técnico/operacional que não faça parte do contrato funcional do cliente (ex.: comentários `<!-- AUTO: ... -->`, flags internas de pipeline, observações de debug, metadados de regeneração).
  - **Risco**: esses marcadores causam divergência de contrato, falhas de renderização/validação em integrações e retrabalho por ciclos repetidos de correção.
  - **Exemplos proibidos no payload final**:
    1. prefixar HTML final com comentário técnico de execução (`<!-- AUTO: provisional html generated ... -->`);
    2. enviar campos legados/temporários no contrato final (`legacyPreviewHtml`, `renderMode`, `debugInfo`);
    3. serializar JSON técnico dentro de campo textual funcional sem estar previsto em contrato.
  - **Padrão obrigatório de prevenção**:
    1. separar explicitamente no código o que é **metadado técnico interno** vs **artefato final publicável**;
    2. no mapper/builder final, permitir apenas campos do DTO de contrato oficial (whitelist explícita);
    3. antes do envio, validar literal do payload final com checklist de contrato (campos, tipo e formato);
    4. adicionar teste de regressão garantindo ausência de marcadores técnicos no artefato final;
    5. ao detectar violação, bloquear publicação e registrar causa-raiz (origem da contaminação + correção objetiva).
  - **Checklist obrigatório em revisão de PR (quando houver artefato HTML/JSON final)**:
    - o payload final contém somente campos contratuais?
    - existe comentário técnico, flag interna, rótulo de debug ou metadado operacional no conteúdo publicado?
    - os testes cobrem explicitamente a ausência desses marcadores?
