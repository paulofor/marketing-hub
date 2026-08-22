# PDE Platform Canon v1

## Objetivo

A PDE Platform é o motor reutilizável de **Produto Digital Experiencial** do Marketing Hub.

Ela deve entregar produtos digitais como experiência guiada, não como pacote de arquivos soltos. O objetivo comercial é aumentar valor percebido, reduzir esforço do consumidor e transformar princípios científicos do MDS/research em aplicação prática, simples e vendável.

Toda publicação comercial de PDE deve seguir tambem o protocolo canônico geral em `docs/canonical/protocolo-publicacao-comercial-pde-canon.v1.md`. Esse protocolo vale para qualquer produto PDE, nao apenas para o Metodo MUSA, e define os gates de linguagem publica, validacao mobile/desktop, eventos, metricas limpas e bloqueio antes de trafego ou `RUNNING`.

A criação e comercialização de um PDE deve seguir a cadeia simples de seis macroprocessos definida em
`docs/canonical/cadeia-produtos-pde-canon.v1.md`, da descoberta da oportunidade até venda, entrega,
aprendizado e decisão de escala.

### Experimento Pausa de Transição v1

Decisão canônica de 2026-08-05: o mecanismo “Pausa de Transição” entra no motor PDE como hipótese experimental separada, sem ser apresentado como auto-hipnose, terapia ou tratamento.

- produto: `pausa-de-transicao`;
- versão: `pausa-de-transicao-v1`;
- variante A: relaxamento breve voluntário, visualização do primeiro passo e ação mínima;
- variante B: instrução objetiva para escolher e executar o primeiro passo;
- consentimento, ciência dos limites e participação voluntária são obrigatórios antes da atribuição A/B;
- a atribuição deve permanecer estável durante a sessão e os resultados devem ser identificados por versão e variante;
- medir início da tarefa em até dez minutos, primeiro passo concluído, mudança no esforço percebido e taxa de interrupção por segurança;
- ansiedade, dissociação, tontura, desconforto ou perda de autonomia exigem interrupção imediata e evento auditável;
- a experiência não pode ser usada durante direção ou operação de máquinas, com menores, em contexto clínico ou como promessa garantida;
- eventos simulados não confirmam eficácia; somente comportamento e relato humano consentidos alimentam a decisão;
- não liberar mídia paga, oferta ou escala antes de amostra voluntária supervisionada e revisão dos eventos de segurança.

## Decisão arquitetural

O padrão obrigatório é:

- **um motor PDE multi-produto**;
- **frontend React/Vite independente**;
- **backend Java 21 + Spring Boot 3 + Maven independente**;
- **Docker próprio** para execução isolada;
- **imagem e container próprios para cada versão pública de PDE** usada em tráfego, experimento ou campanha;
- **conteúdo por produto em configuração/contrato**, não em front/back exclusivo;
- **checkout externo automatizável**, começando por Pepper como caminho preferencial;
- **Marketing Hub/FEO fabrica o pacote PDE**;
- **PDE Platform entrega experiência, acesso, progresso e materiais**.
- **Marketing Hub é a fonte de verdade para mudanças comerciais do PDE** quando a experiência estiver em campanha ou experimento ativo.

### Domínio público por produto

- o Método MUSA mantém seus domínios em `clubemusa.com.br`;
- outros produtos PDE usam domínio corporativo próprio ou subdomínio explícito de
  `digicomdigital.com.br`, como `kit-whatsapp-pronto.digicomdigital.com.br`;
- o domínio deve identificar um único produto, apontar para o host canônico da PDE Platform e ter
  certificado TLS válido antes da validação e ativação do slot;
- compartilhar o motor PDE não autoriza compartilhar imagem ou container entre versões públicas:
  cada produto/versão continua com imagem, container, URL e diagnóstico próprios.

Não criar um front/back novo para cada produto, salvo exceção explícita e registrada. Essa regra não autoriza reaproveitar a mesma imagem pública para versões comerciais diferentes: o motor continua reutilizável, mas cada versão PDE publicada em domínio próprio deve ser empacotada e executada como imagem/container próprios.

### Isolamento obrigatório por versão pública

Decisão canônica de 2026-07-31: o modelo operacional de “slot” compartilhado para PDE público fica substituído por **versão pública isolada por imagem e container Docker**.

Regra obrigatória:

- cada versão pública de PDE que receba campanha, experimento, QA comercial ou tráfego real deve ter imagem Docker própria;
- cada versão pública deve rodar em container próprio, com nome, porta, URL pública e `experienceVersion` explícitos;
- `v5.clubemusa.com.br`, `v6.clubemusa.com.br`, `v7.clubemusa.com.br` ou equivalente não podem apontar para a mesma imagem genérica de frontend;
- o deploy deve permitir publicar uma versão sem recriar as demais versões ativas;
- rollback deve trocar somente a imagem/container da versão afetada;
- o endpoint público de diagnóstico canônico é `GET /version-diagnostics.json`;
- o diagnóstico deve expor `version`, `publicUrl`, `experienceVersion`, `image`, `imageVersionId`, `imageTag`, `commitSha` e `deployedAt`;
- o backend PDE deve expor `GET /api/pde/build-identity` e `GET /actuator/info` com identidade estável da build implantada, incluindo aplicação, artefato, versão, commit, branch, tag/imagem, ambiente, URL do backend PDE, URL pública do frontend, backend administrativo do Marketing Hub configurado e data de deploy; o `/actuator/info` é o contrato usado pela tool MCP `runtime_build_info`;
- o cockpit administrativo do Marketing Hub deve exibir a identidade retornada por `GET /api/pde/build-identity` a partir da mesma URL pública usada para consultar analytics, antes de interpretar métrica zerada como ausência de tráfego;
- `GET /slot-diagnostics.json` pode existir somente como alias legado temporário, sem ser usado como contrato novo;
- contratos, painéis e documentação nova devem usar “versão PDE” em vez de “slot PDE”, exceto quando estiverem preservando compatibilidade com campos legados como `slotCode`;
- métricas comerciais devem ser segmentadas por produto, campanha, experimento, URL pública, versão PDE e `experienceVersion`, não por slot operacional genérico.
- toda análise operacional de métricas PDE via MCP deve validar primeiro a tool `pde_db_health` e conferir `datasourceTarget.host`, `datasourceTarget.port` e `datasourceTarget.schema` contra o banco usado pelo PDE Platform Backend produtivo da URL analisada. Se o MCP consultar base diferente, réplica defasada ou alvo não comprovado, os dados do MCP não podem ser usados como prova comercial até o deploy do MCP ser realinhado. O deploy versionado do MCP deve herdar o datasource canônico `PDE_ACCESS_JDBC_*` do PDE quando `MCP_PDE_DATASOURCE_*` não for informado explicitamente.

Benefício comercial esperado: impedir que campanha com tráfego pago seja julgada por métricas de outra versão, reduzir risco de publicar criativo/oferta em URL errada e preservar aprendizado limpo para decisão de escala.

## Responsabilidades

### Marketing Hub/FEO

O FEO fabrica o pacote PDE com:

- nome, promessa, público e transformação;
- diagnóstico;
- missões guiadas;
- exemplos visuais;
- e-book de apoio;
- checklists, templates e materiais baixáveis;
- imagens, capa e infográficos;
- metadados comerciais para página de venda.
- pacote científico operacional versionado quando o produto usar artigos ou evidências externas para sustentar mecanismo, prova ou orientação por IA.

### Atualização comercial de experiências PDE

Toda mudança comercial em um PDE usado para campanha, experimento ou monitoramento pós-deploy deve ser feita pelo Marketing Hub, no contrato versionado da experiência do produto, e não diretamente no frontend/backend do `pde-platform`.

Entram nessa regra:

- primeira dobra;
- promessa;
- CTA;
- diagnóstico;
- quantidade de opções;
- ordem das etapas;
- copy de paywall;
- materiais de apoio;
- missões;
- qualquer microinteração usada para medir interesse.

O objetivo é preservar a associação entre versão da experiência, campanha, eventos de funil, painel pós-deploy e decisão comercial. Alterações diretas no código do PDE só são permitidas para capacidade técnica genérica do motor, correção de bug, tracking, integração, performance, segurança ou componentes reutilizáveis que não representem uma variação comercial específica do produto.

Cada contrato PDE publicado pelo Marketing Hub deve declarar uma versão comercial explícita da experiência, como `experienceVersion`, `funnelVersion` ou campo equivalente canônico. Essa versão deve mudar sempre que a alteração puder afetar conversão, interesse ou comportamento do usuário. Eventos de analytics enviados pelo frontend PDE devem carregar essa versão nos metadados persistidos para permitir comparar resultados por versão sem misturar tráfego antigo e novo.

O campo canônico para comparação automática é `experienceVersion`. O campo `funnelVersion` agrupa a arquitetura comercial maior do funil. O backend PDE deve persistir `experienceVersion` em coluna própria dos eventos de funil, mantendo os metadados como apoio auditável, para permitir consulta simples por SQL e painel pós-deploy.

Quando uma alteração de PDE for publicada, o relatório/painel deve separar pelo menos:

- produto;
- experimento/campanha quando disponível;
- versão da experiência;
- data/hora de publicação;
- eventos de entrada, clique, login, paywall, checkout e compra;
- consumo de vídeo parcial e completo segmentado pela mesma versão/origem usada no funil;
- decisão comercial tomada para aquela versão.

Se a versão da experiência não estiver disponível nos eventos, a comparação deve ser considerada incompleta: pode indicar tendência por janela de tempo, mas não deve ser usada como prova limpa de melhora ou piora entre formatos.

Antes de publicar uma mudanca comercial de PDE ou colocar o experimento vinculado em `RUNNING`, o Marketing Hub deve executar o protocolo canônico de publicação comercial de PDE. A versao so pode receber trafego quando o protocolo confirmar ausencia de vazamento tecnico, experiencia publica coerente, mobile/desktop validos, imagem/container próprios e metricas segmentadas por produto, campanha, experimento, URL pública, versão PDE e `experienceVersion`.

### Checkout externo

O checkout externo deve:

- vender o produto/oferta;
- processar pagamento;
- enviar webhook de compra aprovada;
- permitir automação por API sempre que possível.

Pepper é a preferência inicial para automação. Mercado Pago pode ser fallback.

### Backend PDE

O backend PDE deve:

- receber webhooks de compra aprovada;
- liberar acesso por produto e e-mail;
- expor catálogo do produto;
- controlar diagnóstico e progresso;
- registrar missões concluídas;
- expor biblioteca de materiais;
- preparar base para assinatura/continuidade;
- ser a única API consumida pelo frontend PDE;
- acessar banco de dados ou serviços internos quando isso for necessário para entregar a experiência PDE.
- criar, persistir e expor solicitações de orientação por IA quando a experiência precisar de personalização guiada.
- receber resultados de workers de IA com saída funcional estruturada, payload bruto, modelo, tier, tokens, custo quando houver e erro.

O backend PDE pode acessar dados persistidos diretamente ou por contratos internos definidos para o módulo, desde que preserve a fronteira de produto: o frontend PDE não deve conhecer nem consumir endpoints do backend principal `backend/ads-service`.

### IA direcionada no PDE

A IA no PDE deve funcionar como personalização guiada por etapa, nunca como chat aberto genérico na experiência inicial.

Regras obrigatórias:

- o backend PDE é fonte de verdade de acesso, contexto, pendência, status, resultado e auditoria;
- a chamada OpenAI deve ser executada por worker próprio ou módulo executor, não pelo frontend;
- o worker deve consumir pendências pelo endpoint canônico `pending` do backend PDE;
- prompt operacional e schema JSON de saída devem ficar versionados no worker;
- quando o produto possuir pacote científico operacional, o backend PDE deve entregá-lo no contrato `pending` e o worker deve injetá-lo no prompt como base de plausibilidade, limites e linguagem permitida;
- para chamadas MUSA, o worker deve falhar antes da OpenAI se o pacote científico operacional estiver ausente ou incompleto, evitando orientação genérica sem apoio dos artigos definidos para o produto;
- a resposta deve ser curta, estruturada e diretamente aplicável à missão;
- o frontend deve exibir a orientação como cartão de produto, não como conversa livre;
- toda solicitação deve ser mensurável no funil e associada ao token, produto e missão.

Para o Método MUSA, a Consultora MUSA deve atuar nos 7 dias como orientação guiada por missão: a cliente preenche três sinais ou respostas práticas do dia e recebe um cartão curto, aplicável e coerente com o histórico da jornada. O Dia 1 pode ser usado como amostra gratuita de valor; os Dias 2 a 7 permanecem como parte do acesso completo quando o funil estiver em modo de paywall interno.

Para o Método MUSA, a Consultora MUSA deve usar o `musa-evidence-pack-v1` como bastidor científico. O pacote apoia microações sobre roupa, cor, acabamento, postura, coerência visual e peça-sinal, mas a resposta visível não deve virar citação acadêmica recorrente nem promessa absoluta. A linguagem deve preservar o desejo de presença elegante acessível e evitar afirmações como garantia de elegância, mudança universal de percepção externa ou transformação de personalidade.

### Frontend PDE

O frontend PDE deve:

- exibir uma tela inicial de entrada/login antes da área de orientações;
- apresentar uma parte inicial da experiência guiada antes da compra;
- bloquear as partes principais da experiência até a compra do acesso;
- conduzir a cliente pelo diagnóstico;
- mostrar missões diárias;
- exibir progresso;
- disponibilizar biblioteca de apoio;
- reforçar promessa, transformação e próximos passos.

O frontend PDE deve consumir somente endpoints do próprio backend PDE, preferencialmente sob `/api/pde/...`, usando proxy local/deploy apontado para `pde-platform-backend`. É proibido o frontend PDE chamar diretamente endpoints do `backend/ads-service`, hosts do backend principal, endpoints administrativos do Marketing Hub ou APIs internas de outros módulos. Quando a tela PDE precisar de dados que hoje existam no `ads-service` ou em outro repositório, o contrato deve ser criado no backend PDE, e o backend PDE deve fazer a leitura, persistência ou integração necessária.

Essa fronteira deve ser validada automaticamente no CI do PDE frontend antes do build. A validação deve bloquear referências diretas a `ads-service`, ao host do backend principal `191.252.181.168`, à porta `8000` do backend principal ou a endpoints fora do contrato PDE.

### Vídeos HLS obrigatórios em PDE

Todo vídeo consumido por PDE público deve ser entregue em HLS (`.m3u8`). MP4 pode existir como arquivo de origem, fallback técnico interno ou artefato de auditoria, mas não deve ser a URL canônica publicada para a experiência PDE.

O Marketing Hub deve gerenciar os HLS usados por PDEs em ativos comerciais rastreáveis. Para vídeos de experimento, o campo canônico é `experiment_video_asset.hls_playback_url`, exposto como `hlsPlaybackUrl` nos contratos da API. Vídeos `LANDING_HERO` destinados a PDE só podem ser aprovados para uso comercial quando estiverem `READY`, com revisão `APPROVED`, áudio validado e `hlsPlaybackUrl` preenchido com playlist `.m3u8`.

Playlist HLS empacotada no build do PDE pode existir apenas como localizacao fisica ou contingencia tecnica. Para uso comercial, essa playlist precisa estar cadastrada no Marketing Hub em ativo rastreavel, visivel na biblioteca de videos e vinculada ao experimento/projeto/job correspondente.

O contrato `heroVideos` do PDE deve priorizar `hlsPlaybackUrl` e manter `playbackUrl` como alias compatível apontando para a mesma playlist HLS. É proibido publicar nova versão PDE usando MP4 como `playbackUrl` principal.

### Funil comercial obrigatório Clube MUSA/PDE

O Clube MUSA/PDE deve usar um funil de entrada com login antes da compra e paywall interno.

Fluxo canônico:

```text
Anuncio
→ tela de login do Clube MUSA/PDE
→ entrada no sistema
→ visualizacao da parte inicial gratuita
→ bloqueio das partes mais importantes
→ oferta de compra do acesso
→ checkout
→ compra aprovada
→ liberacao do acesso completo
→ continuidade da experiencia guiada
```

Regras obrigatórias:

- os anúncios devem direcionar para a tela de login do Clube MUSA/PDE, não diretamente para checkout;
- o login libera somente a entrada no sistema e a parte inicial gratuita;
- o preço do acesso pago não deve aparecer em anúncio, experimento ou área pública quando a estratégia comercial for revelar o valor somente no momento em que a usuária solicitar a liberação das funcionalidades pagas dentro da área logada;
- a parte inicial deve gerar percepção de valor, diagnóstico, orientação ou amostra suficiente para criar desejo de continuidade;
- as partes mais importantes do produto devem permanecer bloqueadas até a compra do acesso;
- a compra aprovada libera o acesso completo ao produto PDE comprado;
- é proibido documentar ou implementar fluxo em que qualquer e-mail válido libere acesso completo sem compra;
- é proibido tratar o login como equivalente a compra, assinatura ou liberação total.

O objetivo comercial é transformar o anúncio em entrada de relacionamento, permitir que a lead veja valor dentro do sistema e vender o acesso quando ela quiser continuar nas partes de maior valor.

### Jornada Persuasiva Interativa do PDE

A Jornada Persuasiva Interativa do PDE deve ser lida como **funil experiencial por estágios comerciais**, não como AIDA simples.

O modelo AIDA pode ser usado como apoio psicológico dentro de cada estágio, mas a unidade principal de análise deve ser o avanço comercial real do consumidor:

1. **Contato com a promessa**: anúncio e primeira dobra fazem a pessoa reconhecer a dor/promessa e aceitar entrar.
2. **Envolvimento diagnóstico**: questionário e plano/amostra gratuita aumentam informação, valor percebido e desejo pela continuidade.
3. **Compromisso de continuidade**: login, cadastro, plano salvo, primeira missão ou ação equivalente transformam interesse em intenção mensurável.
4. **Conversão comercial**: paywall, clique de assinatura, checkout e compra transformam intenção em receita.
5. **Validação pós-compra**: acesso liberado, primeiro uso, missão concluída e materiais abertos confirmam que a promessa vendida começou a ser aplicada.

Cada evento comercial e cada consulta administrativa de analytics deve preservar `experienceVersion`. Quando um experimento estiver ligado a um slot produtivo, o monitor deve consultar a versão exata do slot e não pode somar sessões, eventos, origens ou jornadas de outras versões do mesmo produto. A jornada guiada também deve medir `MISSION_FEEDBACK_SUBMITTED` após a conclusão das missões, com percepção de utilidade, facilidade e aplicabilidade, e `JOURNEY_COMPLETED` ao concluir o plano inteiro.

O contrato `persuasiveJourney` publicado pelo Marketing Hub deve declarar esses estágios de forma versionada, com função comercial, mudança esperada no usuário, seções/eventos rastreados, métrica principal e regra de otimização quando o estágio quebrar. O relatório do experimento deve usar essa jornada para responder em qual estágio a pessoa perdeu confiança, desejo ou disposição de pagar.

### Analytics obrigatório para campanhas PDE

Toda aplicação PDE usada como destino de campanha deve registrar eventos próprios no backend PDE antes de escalar tráfego pago. A medição mínima deve permitir reconstruir o funil por produto, campanha, origem e dispositivo.

### Monitoramento crítico 24/7 de PDEs

PDE publicado, vendido ou usado como destino ativo de campanha deve ter monitoramento operacional dedicado, independente do backend principal.

Regra canônica:

- o módulo dedicado para disponibilidade crítica é `pde-monitor-worker`;
- ele pode acessar diretamente o MySQL como exceção explícita à regra geral de que módulos externos não acessam banco;
- essa exceção vale somente para leitura de PDEs críticos em `ops_monitored_module` e gravação de saúde/incidentes em `ops_module_health_check` e `ops_module_incident`;
- o monitor deve verificar a URL pública operacional do PDE, priorizando `monitoring_url` quando existir;
- o monitor não pode orquestrar pipeline, alterar experiência comercial, liberar acesso, modificar produto, processar checkout ou substituir o backend PDE;
- o objetivo comercial é detectar indisponibilidade de venda/experiência 24/7 sem depender do backend principal ou de uma cadeia de contratos internos.

O `ops-monitor-worker` continua existindo para monitoramento geral de módulos. O `pde-monitor-worker` existe porque PDE ativo em campanha é superfície direta de venda e precisa de caminho curto de observabilidade.

Eventos mínimos:

- `PED_ENTRY`;
- `PAGE_VIEW`;
- `PAGE_LOAD`;
- `PAGE_VISIBLE_TIME`;
- `SECTION_VIEW`;
- `PRESENCE_MAP_CHOICE_SELECTED`;
- `DIAGNOSTIC_CHOICE_SELECTED`;
- `LOGIN_STARTED`;
- `LOGIN_COMPLETED`;
- `PAYWALL_VIEWED`;
- `SUBSCRIPTION_CLICKED`;
- `CHECKOUT_STARTED`;
- `SUBSCRIPTION_APPROVED`;
- `ACCESS_RELEASED`;
- `FIRST_USE`;
- `MISSION_OPEN`;
- `MISSION_COMPLETED`;
- `MATERIAL_OPEN`.

Metadados mínimos por evento quando disponíveis:

- `visitorId`;
- `sessionId`;
- `utm_source`, `utm_medium`, `utm_campaign`, `utm_content`, `utm_term`;
- URL da página;
- URL de referência;
- user-agent informado pelo navegador ou requisição;
- IP público resolvido pelo backend quando disponível;
- qualidade de tráfego classificada pelo backend (`HUMAN`, `BOT_SUSPECTED`, `PLATFORM_CRAWLER`, `INTERNAL_QA` ou `UNKNOWN`), com motivo e provedor inferido quando possível;
- tipo de dispositivo;
- tamanho de tela e viewport;
- seção, ação ou material acionado;
- tempo visível quando o evento representar permanência.

O backend PDE deve persistir esses eventos em estrutura consultável e expor resumo agregado para decisão comercial. Logs técnicos não substituem analytics persistido. Antes de liberar nova campanha para o Clube MUSA/PDE, deve ser possível responder no mínimo: quantas pessoas entraram, quantas iniciaram login, quantas concluíram login, quantas viram paywall, quantas clicaram em assinatura, quantas iniciaram checkout, quantas tiveram compra aprovada, quantas receberam acesso e quantas fizeram primeiro uso.

KPIs comerciais de campanha devem considerar por padrão apenas sessões classificadas como `HUMAN`. Sessões `BOT_SUSPECTED`, `PLATFORM_CRAWLER`, `INTERNAL_QA` e `UNKNOWN` devem permanecer persistidas para auditoria, diagnóstico técnico e investigação de tráfego, mas não podem inflar sessões, page views, breakdown por dispositivo, UTMs ou taxa de conversão usados para decisão de mídia.

### Health check público obrigatório por PDE

Todo PDE produzido para campanha, experimento ou tráfego pago deve publicar um contrato público de health comercial em `GET /pde-health-contract.json`.

Esse contrato deve declarar, no mínimo:

- `slug` do produto;
- `healthPath` da entrada pública que recebe o tráfego;
- `requiredTexts` com pelo menos headline, bloco principal da oferta/diagnóstico e CTA primário;
- `forbiddenTexts` com mensagens de erro técnico ou tela branca que nunca podem aparecer para a cliente.

O pipeline de deploy deve executar o smoke test público depois da publicação usando esse contrato. A validação mínima obrigatória é:

- `GET /healthz` retorna status operacional;
- a página pública responde com HTTP válido;
- o JavaScript principal carrega;
- o primeiro elemento renderizado em `#root` fica visível;
- todos os textos comerciais críticos do contrato aparecem na tela;
- nenhum texto proibido aparece no corpo da página;
- não existem erros fatais de execução capturados pelo navegador.

Um PDE não pode ser considerado pronto para tráfego se o smoke test público falhar, mesmo quando o status HTTP estiver 200. O objetivo é bloquear recorrência de tela branca, assets misturados entre ambientes, bundle JavaScript quebrado ou primeira dobra errada antes de gastar mídia.

### Controle de versões produtivas pelo Marketing Hub

O Marketing Hub deve ser o painel operacional para decidir qual versão PDE recebe tráfego e qual versão cada experimento mede.

A decisão de direcionamento para uma versão produtiva pertence ao experimento. O repositório de anúncios, o gerador de criativos e o Facebook Ads Worker não devem escolher, inferir, sobrescrever ou redirecionar versão por conta própria. Esses componentes devem apenas consumir a URL/slot/`experienceVersion` já definidos no contrato do experimento e registrar a mesma referência nos eventos e métricas.

GitHub Actions verde não é prova suficiente de publicação produtiva. Antes de liberar tráfego pago ou considerar uma correção publicada, o Marketing Hub deve confirmar:

- URL pública produtiva respondendo;
- versão comercial da experiência PDE publicada;
- slot produtivo correto no card do produto;
- experimento vinculado à versão que será medida;
- jornada principal validada em desktop e mobile;
- eventos aparecendo no painel pós-deploy depois do tráfego real.

### Publicação versionada simultânea de PDE

Um mesmo produto PDE pode ter múltiplas versões produtivas simultâneas para teste controlado, como `v5.clubemusa.com.br` e `v6.clubemusa.com.br`.

Regras obrigatórias:

- todos os PDEs públicos devem ser publicados no host oficial `163.245.200.7`;
- workflows, inventário operacional, proxy HTTPS, certificados, validações pós-deploy, smoke tests e diagnósticos públicos de PDE devem usar `163.245.200.7` como fonte de verdade do host produtivo;
- é proibido publicar PDE produtivo em outro host sem decisão explícita registrada neste cânone antes da alteração;
- o DNS dos subdomínios versionados de PDE deve resolver para `163.245.200.7` antes de liberar tráfego, campanha ou validação comercial como pronta;
- cada versão pública deve ter subdomínio próprio, slot próprio no Marketing Hub e `experienceVersion` própria;
- duas versões comerciais diferentes de PDE nunca podem compartilhar a mesma URL pública primária;
- se a URL pública for igual, a versão comercial deve ser considerada a mesma para fins de campanha, analytics e decisão de escala;
- o deploy produtivo do backend/worker do PDE pode rodar automaticamente em `main`, mas o frontend público de uma versão com tráfego ou cliente em uso só pode ser atualizado por slot explícito (`v5`, `v6`, `all` ou equivalente), nunca como efeito colateral de outra versão;
- o mesmo motor pode atender múltiplos subdomínios, mas cada frontend público em campanha deve ter container/porta/proxy próprios para permitir deploy e rollback independente por versão;
- nenhum deploy pode ser considerado pronto se `v5` e `v6` entregarem o mesmo `experienceVersion` por engano;
- quando a versão depender de vídeo, o smoke test deve validar que o stream HLS público esperado retorna manifesto e segmentos reais, nunca HTML fallback;
- a validação pós-deploy deve cobrir cada subdomínio versionado com health público, renderização, endpoint PDE, diagnóstico público, versão esperada e asset crítico esperado;
- eventos de funil devem persistir `experienceVersion`, permitindo comparar v5 e v6 sem misturar tráfego, criativo ou jornada.

Para o Clube MUSA, a regra operacional atual é:

- `v5.clubemusa.com.br` deve servir `musa-pde-entry-v5-video-explicativo` sem vídeo de slides gerado artificialmente;
- `v6.clubemusa.com.br` deve servir `musa-pde-entry-v6-video-motivacional` sem vídeo de slides gerado artificialmente;
- vídeos comerciais do MUSA só podem ser usados quando nascerem da estrutura versionada de produção de vídeos do Marketing Hub, com roteiro, job, asset e URL de reprodução auditáveis;
- vídeos hero por versão devem ser declarados no contrato público do produto em `heroVideos`, incluindo `experienceVersion`, `placement`, `playbackUrl`, `assetId`, `experimentVideoAssetId`, `salesVideoProfileId`, `salesVideoJobId`, `status` e `reviewStatus`;
- é proibido gerar MP4/HLS comercial do MUSA a partir dos slides/imagens do diagnóstico (`musa-diagnostic-slide-*`) ou servir URLs antigas como `/assets/hls/musa-v5-video-explicativo/index.m3u8`, `/assets/hls/musa-v6-video-motivacional/index.m3u8`, `/assets/musa-v5-video-explicativo.mp4` ou `/assets/musa-v6-video-motivacional.mp4`.

Quando houver hipóteses, criativos ou primeiras dobras concorrentes, a operação deve criar slots produtivos paralelos em vez de depender de ambiente intermediário. A tela de experimento apenas escolhe a versão medida; criação, manutenção e publicação das URLs ficam no fluxo do produto e no pipeline versionado do repositório.

O repositório de anúncios deve permanecer neutro em relação à escolha de versão: ele pode receber a URL pública aprovada para montar ou publicar o anúncio, mas não pode conter regra própria do tipo "enviar para v5", "enviar para v6" ou qualquer fallback de versão. Se a versão do destino precisar mudar, a alteração deve ocorrer no experimento/versão produtiva e só depois ser consumida pelo fluxo de anúncios.

### Versões produtivas isoladas do PDE

O Marketing Hub deve permitir múltiplas versões produtivas de PDE para o mesmo produto quando houver hipóteses, criativos ou primeiras dobras concorrentes em tráfego pago.

O modelo canônico é uma **versão produtiva PDE** persistida no backend principal. Enquanto o contrato legado ainda existir, o campo técnico `slotCode` pode continuar sendo usado como identificador interno da versão, mas telas, documentos e fluxos novos devem tratar o conceito como versão pública isolada.

Campos mínimos:

- `slotCode`, como `v1`, `v2` ou código comercial equivalente;
- `productSlug`;
- `domain`, como `v1.clubemusa.com.br`;
- `publicUrl` usada no anúncio;
- `experienceVersion` servida naquele endereço;
- `targetEnvironment` esperado pelo pipeline;
- `status` operacional;
- experimento de origem quando existir.

Versões produtivas existem para separar aprendizado comercial e reduzir risco operacional. Um teste novo não deve obrigar a troca global de `clubemusa.com.br` quando for possível publicar uma variação em subdomínio próprio, mantendo eventos por `experienceVersion`, URL de anúncio explícita e histórico de campanha rastreável.

A URL pública primária é parte da identidade comercial da versão. Portanto, não pode existir mais de uma versão ativa ou pronta para tráfego com a mesma `publicUrl` apontando para `experienceVersion` diferente. Quando houver reaproveitamento temporário de domínio para rollback ou correção operacional, o Marketing Hub deve preservar a mesma versão comercial ou encerrar a versão anterior antes de ativar outra, registrando a data de troca para não contaminar métricas.

Quando a versão comercial for numerada, a versão produtiva deve usar o subdomínio correspondente, como `v5.clubemusa.com.br` para a versão 5. O domínio raiz pode existir como entrada institucional, legado ou redirecionamento, mas não deve ser a URL primária de uma campanha que mede uma versão específica.

O Marketing Hub pode cadastrar e acompanhar versões antes da automação completa de infraestrutura. A publicação real continua proibida por SSH manual: o deploy deve ser feito por workflow, Compose, Dockerfile ou pipeline versionados do repositório.

Cada versão pública deve rodar em imagem e container próprios, com `experienceVersion` esperada, porta e proxy dedicado. O hostname público continua sendo fonte decisiva da versão comercial exibida, mas o isolamento operacional de imagem/container/porta é obrigatório para impedir que uma publicação da v6 reinicie, atualize ou contamine a v5.

O workflow oficial de publicação do `pde-platform` deve validar cada versão produtiva ativa ou pronta, no mínimo `https://v5.clubemusa.com.br` e `https://v6.clubemusa.com.br` enquanto ambas existirem. A validação pós-deploy precisa provar health público, renderização da entrada, contrato público, jornada diagnóstica e `version-diagnostics.json` em cada subdomínio, porque um único smoke test no domínio raiz não comprova teste simultâneo de versões.

Como os subdomínios versionados do Clube MUSA são superfície direta de campanha, o workflow oficial do `pde-platform` também deve garantir que o proxy HTTPS público esteja ativo antes de aprovar a publicação. Se o proxy do `lead-portal-payments-service` estiver disponível no host, o workflow deve recriá-lo/recarregá-lo pelo Compose versionado e reconectá-lo à network pública usada pelo PDE. Se nenhum container publicar a porta 443 ou nenhum proxy puder ser encontrado, a publicação deve falhar com diagnóstico operacional claro; nunca considerar a v5/v6 pronta apenas porque as portas diretas `5176`/`5177` respondem.

O DNS público dos subdomínios versionados do Clube MUSA deve apontar para o mesmo host oficial usado pelo workflow de deploy do PDE e pelo proxy HTTPS do `lead-portal-payments-service`. A validação produtiva deve falhar explicitamente quando `v5.clubemusa.com.br`, `v6.clubemusa.com.br`, `v7.clubemusa.com.br` ou versão futura resolverem para IP diferente do host de deploy, porque isso envia tráfego pago para infraestrutura fora do caminho publicado e contamina a leitura comercial do experimento.

Para evitar ambiguidade operacional, o host oficial citado nesta regra é `163.245.200.7`. Qualquer referência operacional de PDE a outro host deve ser tratada como divergência a corrigir antes de publicar ou reativar campanha.

## Contrato mínimo de produto

Cada produto PDE deve ter, no mínimo:

- `slug`;
- `name`;
- `promise`;
- `audience`;
- `priceLabel`, que pode ficar vazio enquanto a estratégia comercial não deve revelar preço antes da solicitação de acesso pago;
- `theme`;
- `diagnostic`;
- `missions`;
- `supportMaterials`;
- `scientificEvidencePack`, quando existir base científica operacional para IA, prova, materiais ou orientação;
- `completionOffer`.

## Regra de qualidade comercial

O produto visto pela cliente não pode expor linguagem interna como:

- `FEO`;
- `experimento`;
- `CTR`;
- `CPL`;
- `lead`;
- `checkout`;
- `score`;
- `JSON`;
- `sha256`;
- `promessa validada`;
- `mecanismo validado`.

Essa regra vale para toda nova versão comercial de PDE, incluindo versões futuras do Método MUSA. Antes de publicar ou medir uma nova versão, a primeira dobra, diagnóstico, missões, paywall, orientações da Consultora MUSA, materiais de apoio, CTAs e mensagens de erro devem passar por revisão anti-vazamento técnico.

A linguagem visível deve falar como uma conversa de consumo, não como relatório de construção do produto. A cliente deve sentir:

- "isso foi feito para mim";
- "eu entendi sem esforço";
- "parece simples começar";
- "o resultado combina com minha rotina";
- "quero continuar para ver meu próximo passo".

Quando uma versão possuir base científica, IA, contrato, estágio, métrica ou decisão de experimento, esses elementos devem permanecer no bastidor e aparecer para a cliente apenas como benefício prático, orientação guiada, exemplo visual, checklist, progresso ou redução de esforço. Termos como IA, algoritmo, pipeline, evento, score, experimento, validação, etapa, schema ou contrato só podem aparecer na experiência pública quando forem indispensáveis para transparência da usuária e escritos em linguagem comum.

Os princípios científicos devem aparecer como:

- decisão guiada;
- microação;
- exemplo visual;
- checklist;
- campo preenchível;
- evidência de progresso;
- redução de esforço.

Esta regra de qualidade comercial e detalhada e operacionalizada pelo protocolo geral `docs/canonical/protocolo-publicacao-comercial-pde-canon.v1.md`, que deve ser aplicado a qualquer PDE antes de publicacao, trafego pago, comparacao de versoes ou decisao de escala.

## Experimento 66

O experimento 66 deve usar a PDE Platform como primeira instância:

- Produto: `Método MUSA - Experiência Guiada de 7 Dias`;
- Formato: experiência guiada + e-book + checklists + templates;
- Checkout preferencial futuro: Pepper;
- Fallback existente: Mercado Pago;
- Entrega: área PDE do Marketing Hub.

## Critério de pronto

Um produto PDE só pode ser considerado pronto para tráfego quando:

1. checkout real estiver configurado;
2. webhook de compra aprovada estiver validado;
3. acesso da cliente estiver liberando corretamente;
4. experiência guiada estiver carregando;
5. materiais de apoio estiverem disponíveis;
6. progresso estiver persistindo ou registrado de forma auditável;
7. o anúncio apontar para a entrada/login da versão PDE produtiva aprovada pelo experimento, com imagem/container próprios, e o checkout existir somente no paywall interno ou na continuidade bloqueada;
8. produto da cliente não expuser termos técnicos internos.
9. funil e analytics do PED estiverem registrando eventos próprios de entrada, sessão, UTM, paywall, checkout, compra, liberação e ativação.
10. health check público comercial estiver publicado e passando com os textos críticos do PDE.

Para experimentos do tipo `PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL`, a prontidão de campanha não deve exigir GeraSalesPage v1 como página de venda tradicional. A validação correta é: contrato comercial completo, URL versionada do Clube MUSA/PDE, criativos prontos, segmentação publicável, checkout/webhook/acesso e experiência inicial/paga validados.
