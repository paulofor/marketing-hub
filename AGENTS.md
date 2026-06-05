# AGNETS.md — Contrato operacional

## Missão do sistema

A missão do Marketing Hub é criar produtos digitais que realmente transformem a vida das pessoas, resolvendo necessidades reais com melhoria prática, percebida e aplicável. Lembrando que o consumidor é um ser humano e ele é regido pelo **principio fundamental de afastar a dor e o esforço, buscar a facilidade e o prazer**.

O Marketing Hub é uma fábrica automatizada de produtos digitais: descobre dores reais em nichos específicos, cria ofertas baseadas em mecanismos plausíveis, gera os ativos comerciais, publica experimentos, mede vendas e escala os produtos vencedores.



## 1. Fontes de verdade

- **Liquibase / MySQL 5.7**: `docs/database/liquibase-mysql57.md`. Use sempre `databaseChangeLog` em YAML, `preConditions` com `dbms:mysql`, `splitStatements: true`, `stripComments: true` e valide mentalmente o SQL.
- **Documentos canônicos básicos**: os documentos canônicos básicos do projeto ficam em `/docs/canonical` e devem ser consultados como referência primária.
- **Decisão de mudança de regras (obrigatório)**: quando o usuário decidir mudar regras, altere imediatamente o documento correspondente em `/docs/canonical` referente ao tema.

## 2. Convenções de engenharia

- **Regra Número 1** : O objetivo principal do sistema é gerar VENDAS de produtos que ofereçam valor através do uso da Inteligencia Artificial.
- **Regra Número 2** : Sempre que tiver algum problema não tentar resolver consequencias. Buscar SEMPRE resolver a causa-raiz.
- **Regra Número 3** : Seja SIMPLES, OBJETIVO e EFICAZ.
- invesgtigação da causa raiz
- Se for um erro na tela:
- 1. Pesquisar o dado da tela de qual endpoint ele vem
- 2. Identificar de qual base de dados vem
- 3. Analisar se no banco de dados esta correto ou não
- 4. Se não estiver correto pesquisar qual endpoint ou classe grava esse item na tabela.
- 5. Verificar se existe logs desse endpoint que faz gravações
- 6. Verificar a origem do dados antes da gravação.
- 7. Se não tiver informações suficientes para chegar em uma conclusão segura coloque logs e solicite nova execução.
- **Servidor MCP** : Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC. Quando precisar analisar casos específicos acesse o banco de dados usando esse servidor.
- **Tecnologias padrão**: Java 21 + Spring Boot 3, React 18 + Vite + TypeScript, Zustand para state, TanStack Query para dados. Formatação: Spotless (backend) e Prettier (frontend).
- **Banco**: MySQL 5.7. Somente o backend acessa o banco; demais módulos conversam via APIs do backend. Prefira filtros no SQL ao invés de pós-processar em memória.
- **JPA/Hibernate x Liquibase (bloqueio de backend)**: nunca confie apenas na nomenclatura automática do Hibernate para colunas já definidas por Liquibase, principalmente campos booleanos ou com siglas/acrônimos (`OpenAI`, `URL`, `HTML`, `ID`, etc.). Toda entidade/campo novo ou alterado que tenha coluna canônica deve declarar `@Column(name = "nome_canonico_no_banco")` quando houver qualquer risco de divergência. Antes de inserir dados por Liquibase em tabela com colunas `NOT NULL`, compare o changelog, a entidade JPA e o schema real (via MCP quando necessário) para impedir colunas duplicadas/legadas que travem o bootstrap do backend.
- **URL do BACKEND** : http://191.252.181.168:8000
- **URL do BACKEND (Codex)** : para acessos executados pelo Codex, usar preferencialmente `http://191.252.181.168` (porta 80); o backend também responde nessa porta.
- **Modelo único**: entidades residem no backend. Os demais módulos acessam o banco de dados pelo backend.
- **Fluxo entre containers**: nada de chamadas diretas entre serviços (frontend, workers, lead-portal etc). Todo tráfego passa pelo backend principal; apenas o backend fala com o banco.
- **Escopo de controllers por módulo**: cada módulo só pode acessar os controllers do próprio módulo (ex.: MOIS usa apenas controllers do pacote MOIS; OPRM usa apenas controllers do pacote OPRM; e assim sucessivamente). É proibido um módulo consumir controllers de outro módulo diretamente.
- **Novos endpoints**: verifique se o contrato já existe; caso contrário, defina-o no backend, atualize a documentação e adicione testes.
- **Manual do usuário**: todos os links devem usar `target="_blank"`.
- **Frontend**: sempre que alterar o frontend crie os métodos do backend para suportar. Tanto back quanto o front estão sendo executados no mesmo host.
- **Frontend → Backend (validação de endpoint)**: sempre que criar no frontend uma chamada para o backend, verifique primeiro se o endpoint já existe. Se não existir, crie o endpoint no pacote/backend referente ao assunto da tela (ex.: MOIS, OPRM, etc.), respeitando o escopo do módulo.
- **Qualidade**: sempre que alterar um módulo Java realizar os testes unitários antes de publicar o PR.
- **Documentação em código Java (obrigatório)**: toda classe Java deve conter comentário descrevendo sua responsabilidade básica, e todo método deve conter um comentário breve explicando o que ele faz.
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
  - Módulos aceitos em `module`: `backend`, `ai-worker`, `lead-portal`, `facebook-ads`, `email-service`, `lead-portal-payment`, `mds`, `mois`, `mois-hotmart`, `clickbank-coletor-mois`, `oprm-coletor-receita`.
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
    - o filtro `contains` é literal (sem regex/full-text avançado);
    - a retenção/janela disponível depende do `actuator/logfile` do módulo (logs antigos podem não estar mais disponíveis);
    - para logs de GitHub Actions, a tool é `github_actions_get_run_logs` e aceita apenas `run_id` (sem `contains`, `from`, `to`).

- **Erro 400 (Bad Request) em APIs**: em validações de contrato/payload, o backend pode responder `400 Bad Request` sem registrar detalhes úteis no log de aplicação. Nesses casos, **não basta procurar logs**; é obrigatório inspecionar a requisição enviada (URL, método, headers, body) e comparar com DTO/contrato esperado para identificar campo, estrutura ou tipo inválido.

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
