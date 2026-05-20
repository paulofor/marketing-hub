# AGNETS.md — Contrato operacional

## Missão do sistema

A missão do Marketing Hub é criar produtos digitais que realmente transformem a vida das pessoas, resolvendo necessidades reais com melhoria prática, percebida e aplicável. Lembrando que o consumidor é um ser humano e ele é regido pelo **principio fundamental de afastar a dor e o esforço, buscar a facilidade e o prazer**.

O objetivo comercial do sistema é identificar necessidades relevantes de mercado, entender onde existe oportunidade concreta de transformação, pesquisar como essa melhoria pode ser alcançada de forma plausível e convertê-la em produtos digitais produzidos com apoio de IA e com viabilidade comercial.

Toda decisão de arquitetura, dados, prompts, automações, integrações e artefatos deve reforçar esta missão:
- descobrir necessidades reais, não inventar demandas artificiais;
- buscar mecanismos e melhorias com potencial de gerar resultado concreto;
- transformar conhecimento em produto digital claro, útil, escalável e vendável;
- usar IA como meio de produção, estruturação e aceleração, sem perder aderência à realidade do usuário e do mercado;
- priorizar soluções que combinem transformação real para o cliente com sustentabilidade econômica para o negócio.



## 1. Fontes de verdade

- **Regras**: docs/canonical/system-governance-canon.v2.md sempre leia e atualize se necessário.
- **Artefatos**: docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md sempre leia e atualize se necessário.
- **Modelo de dados**: `docs/modelo-dados-experimento.md`. Alterou entidades ou relacionamentos? Atualize o documento imediatamente.
- **Liquibase / MySQL 5.7**: `docs/database/liquibase-mysql57.md`. Use sempre `databaseChangeLog` em YAML, `preConditions` com `dbms:mysql`, `splitStatements: true`, `stripComments: true` e valide mentalmente o SQL.
- **Documentos canônicos básicos**: os documentos canônicos básicos do projeto ficam em `/docs/canonical` e devem ser consultados como referência primária.
- **Decisão de mudança de regras (obrigatório)**: quando o usuário decidir mudar regras, altere imediatamente o documento correspondente em `/docs/canonical` referente ao tema.

## 2. Convenções de engenharia

- **Regra Número 1** : O objetivo principal do sistema é gerar vendas.
- **Regra Número 2** : Sempre que tiver algum problema não tentar resolver consequencias. Buscar SEMPRE resolver a causa-raiz.
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
- **URL do BACKEND** : http://191.252.181.168:8000
- **URL do BACKEND (Codex)** : para acessos executados pelo Codex, usar preferencialmente `http://191.252.181.168:80` (porta 80); o backend também responde nessa porta.
- **Modelo único**: entidades residem no backend. Os demais módulos acessam o banco de dados pelo backend.
- **Fluxo entre containers**: nada de chamadas diretas entre serviços (frontend, workers, lead-portal etc). Todo tráfego passa pelo backend principal; apenas o backend fala com o banco.
- **Escopo de controllers por módulo**: cada módulo só pode acessar os controllers do próprio módulo (ex.: MOIS usa apenas controllers do pacote MOIS; OPRM usa apenas controllers do pacote OPRM; e assim sucessivamente). É proibido um módulo consumir controllers de outro módulo diretamente.
- **Novos endpoints**: verifique se o contrato já existe; caso contrário, defina-o no backend, atualize a documentação e adicione testes.
- **Manual do usuário**: todos os links devem usar `target="_blank"`.
- **Frontend**: sempre que alterar o frontend crie os métodos do backend para suportar. Tanto back quanto o front estão sendo executados no mesmo host.
- **Frontend → Backend (validação de endpoint)**: sempre que criar no frontend uma chamada para o backend, verifique primeiro se o endpoint já existe. Se não existir, crie o endpoint no pacote/backend referente ao assunto da tela (ex.: MOIS, OPRM, etc.), respeitando o escopo do módulo.
- **Qualidade**: sempre que alterar um módulo Java realizar os testes unitários antes de publicar o PR.
- **Documentação em código Java (obrigatório)**: toda classe Java deve conter comentário descrevendo sua responsabilidade básica, e todo método deve conter um comentário breve explicando o que ele faz.
- **Logs**: os logs dos modulos Java Spring Boot podem ser acessados pelo MCP Server.  Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC.
- **Logs de exceção capturada (obrigatório)**: todo bloco `catch` que captura `Exception`, `RuntimeException` ou exceção de integração deve registrar log antes de relançar, converter ou responder erro. O log deve incluir contexto operacional suficiente para localizar o ponto da falha (módulo, operação, identificadores relevantes como `experimentId`, `landingId`, URL/endpoint quando aplicável) e a exceção completa (`ex`) para preservar stack trace; não basta usar apenas `NestedExceptionUtils.getMostSpecificCause(ex).getMessage()` ou somente a mensagem da causa raiz.
- **Agendamentos Spring Boot**: quando criar rotinas agendadas com `@Scheduled`, não usar variáveis para o cron; definir diretamente a string no formato cron na anotação.
- **Logs de ingestão (obrigatório)**: sempre que for construído um fluxo de ingestão de dados, registrar em log o dado bruto recebido da fonte (payload original), antes de qualquer transformação/normalização.
- **GitHub Actions (logs e execuções)**: também podem ser consultados via MCP Server usando as tools `github_actions_list_workflows`, `github_actions_list_runs`, `github_actions_get_run_summary` e `github_actions_get_run_logs` no endpoint https://mcpserverdigi.shop/mcp (JSON-RPC).
- **Testes Unitários**: os testes unitários precisam sempre estar em concordancia com as regras dos documentos canonicos.
- **Regra Geral (cânone x testes)**: sempre que houver alteração de regra em documento canônico, revisar e atualizar os testes unitários relacionados para manter aderência entre documentação, regras de domínio e validações automatizadas.
- **Telas do Usuario**: as telas de usuario, ou frontend precisam sempre estar dando as informações mais importantes e precisas para  o usuario e ofereçendo so comandos necessários para o direcionamento dos fluxo de processos mantidos pelo sistema. Evite informações contraditórias, em excesso e desorganizadas. Mantenha sempre a conformidade com os documentos canonicos.
- **Experimentos** : se estiver fazendo trabalho relacionado ao pacote geralanding registre sempre o trabalho em : /docs/registros/experimentos.md
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

- **Erro 400 (Bad Request) em APIs**: em validações de contrato/payload, o backend pode responder `400 Bad Request` sem registrar detalhes úteis no log de aplicação. Nesses casos, **não basta procurar logs**; é obrigatório inspecionar a requisição enviada (URL, método, headers, body) e comparar com DTO/contrato esperado para identificar campo, estrutura ou tipo inválido.

- 🚨 **PADRÃO OFICIAL — EXCEPTION DE CONTRATO GERALANDING (OBRIGATÓRIO)**:
  - Para falhas de contrato na publicação do GeraLanding para Lead Portal, usar a exception específica `GeraLandingContractViolationException`.
  - O status HTTP de resposta desse cenário deve ser **460** (código interno do domínio para violação de contrato), via handler dedicado `GeraLandingContractViolationExceptionHandler`.
  - O `toString()` da exception deve sempre explicitar de forma literal:
    1. o que era esperado pelo contrato (`expectedContract`),
    2. o que foi enviado (`receivedPayload`),
    3. endpoint/operação,
    4. erro retornado a montante (`upstreamError`).
  - É proibido responder **esse cenário específico (GeraLanding -> Lead Portal)** com erro HTTP padrão genérico (400/500/502) sem o envelope de contrato acima.

- 🚨 **ORIENTAÇÃO CRÍTICA — PLAYBOOK RÁPIDO PARA ERRO DE CONTRATO (400/422) [OBRIGATÓRIO]**:
  - **Objetivo**: identificar causa-raiz em até 15 minutos e evitar horas de tentativa e erro.
  - **Passo 1 (2 min)**: capturar e salvar a requisição literal que saiu do sistema (URL, método, headers e body completo).
  - **Passo 2 (3 min)**: comparar o body enviado com o DTO/contrato atual do endpoint campo a campo (nome, tipo, obrigatoriedade, formato).
  - **Passo 3 (3 min)**: validar se há campo legado/misto sendo enviado (ex.: campo antigo + campo novo no mesmo payload).
  - **Passo 4 (3 min)**: montar `curl` mínimo reproduzindo o erro com o mesmo payload para confirmar o diagnóstico sem ruído de fluxo completo.
  - **Passo 5 (4 min)**: corrigir o payload na origem (builder/mapper/DTO), adicionar teste de regressão do contrato e registrar no documento de registros do tema.
  - **Regra de bloqueio**: é proibido encerrar a análise sem informar literalmente:
    1. o payload enviado,
    2. o payload esperado pelo contrato,
    3. a diferença exata,
    4. a correção objetiva aplicada.

- **Erro 422 (Procedimento Obrigatório / SOP)**: Trate toda ocorrência de `422 Unprocessable Entity` como divergência entre payload gerado pelo modelo e contrato/validação do backend até prova em contrário.
  - **Fluxo obrigatório (sempre nesta ordem):**
    1. Acessar logs do backend via MCP Server (`https://mcpserverdigi.shop/mcp`, JSON-RPC).
    2. Localizar a requisição que falhou e extrair o payload enviado pelo modelo (campos e valores relevantes).
    3. Comparar o payload com a especificação oficial do artefato em `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`.
    4. Comparar o payload com as validações ativas no backend (DTOs, validators, regras de domínio e contratos de API).
    5. Identificar e reportar **exatamente** qual trecho (campo/estrutura/valor) gerado pelo modelo foi rejeitado.
    6. Informar a causa raiz e a correção proposta (priorizando ajuste de prompt e manutenção das definições canonicas, não fique tentando 'consertar' o que o modelo gerou, faça ele gerar de forma correta).

  - **Formato mínimo obrigatório da resposta de diagnóstico:**
    - o que o modelo entregou de forma literal
    - o que a especificação esperava de forma literal
    - diferença entre a entrega do modelo e o que era esperado
    - ação corretiva recomendada
  - Não encerrar análise de 422 sem apontar explicitamente o trecho rejeitado e a validação correspondente.
- **json** : temos que evitar ao máximo json dentro de json. Ou seja json em campo texto de outro json. 


## 4. Framework central do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

Ao alterar qualquer módulo, preserve esse eixo como referência principal de descoberta, modelagem, validação e empacotamento de valor.



## 5. Módulos e responsabilidades

- **MarketingHub Backend / Frontend**: camada administrativa e UI principal do sistema.
- **Facebook Ads Worker**: integração com a API da Meta para campanhas e públicos.
- **Worker AI**: integrações com modelos OpenAI para geração e otimização de ativos.
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
