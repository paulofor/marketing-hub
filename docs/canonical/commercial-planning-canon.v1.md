# Planejamento Comercial Canonico v1

## Fronteira estratégica e operacional dos agentes

Por decisão de 2026-08-28, Atena define mercado, desejo, comportamento, concorrência,
posicionamento, tese de oferta, portfólio e hipóteses no Contrato Estratégico de Mercado v2. Dédalo
materializa produto, experiência pós-compra, entregáveis e provas reais. Íris materializa mensagem,
landing, copy, e-mails, peças não audiovisuais estruturadas e briefings; Apolo materializa vídeo e
áudio. Psique revisa a experiência humana e Têmis revisa a integridade comercial em gates separados.
Hermes recebe a mesma versão/hash e define somente distribuição,
instrumentação, atribuição, funil e otimização. Plutus governa a economia.

Nenhuma etapa posterior pode reconstruir estratégia a partir de campos operacionais. Evidência real
que invalide a tese retorna para nova execução de Atena; não autoriza qualquer etapa posterior a
reescrevê-la.

## Objetivo

O planejamento comercial do Marketing Hub deve transformar objetivos de venda em metas mensais e semanais mensuraveis, conectando produto, experimento, campanha, funil, custo e receita.

## Fonte comum versionada para agentes

O Dossiê de Oportunidade é a etapa canônica anterior ao Plano Comercial. Ele deve permanecer em um portfólio segregado enquanto Argos registra evidências factuais e Atena decide a estratégia de mercado. Plutus só entra depois dessa estratégia, no gate econômico do Plano Comercial; Psique só recebe produto ou artefato real; Hermes só recebe operação autorizada e eventos. Somente um dossiê aprovado por decisão humana pode ser convertido, uma única vez, em novo Plano Comercial. A conversão copia uma fotografia auditável do contexto validado e mantém o vínculo permanente com o dossiê de origem; não herda orçamento, custos, receita ou métricas de planos existentes.

Cada novo dossiê abre no backend um ciclo `productdiscovery.v1/research` e uma tarefa vinculada na mesa de Argos (`market-radar`). O executor consome exclusivamente o endpoint `pending` canônico da descoberta; ao concluir, as evidências reais são incorporadas ao dossiê e a tarefa é concluída. Resultado vazio permanece auditável sem evidência artificial, e falha operacional bloqueia a tarefa com visibilidade no monitor. Dossiês legados em pesquisa recebem o mesmo vínculo por migração idempotente.

Argos usa uma sessão Codex individual e persistente, reconectável pelo monitor de agentes. O pedido de reconexão é persistido pelo backend e consumido exclusivamente pelo Product Discovery Worker usando a chave `market-radar`; somente URL, código temporário e resultado operacional retornam ao painel. O `CODEX_HOME` de Argos deve ser gravável e isolado. Antes de substituir o worker, o deploy deve derivar o UID/GID da imagem, reconciliar proprietário e modos da sessão sem seguir links simbólicos e comprovar dentro da imagem exata que `config.toml` é legível, o diretório é gravável e `codex login status` reconhece a sessão. Credenciais, cookies e tokens de Hotmart e ClickBank nunca entram nessa sessão, no prompt ou no contrato de reconexão.

Por decisão operacional de 2026-09-02, Argos deve ser publicado no VPS de agentes
`163.245.202.80`. O host legado `191.252.120.96`, com 957 MB de RAM e pressão recorrente de memória
e I/O, não pode voltar a receber o executor. Workflow, observabilidade MCP, health e inventário de
segredos devem apontar juntos para o host vigente, preservando a sessão individual e a credencial
Brave sem expor ou duplicar seus valores.

Argos deve cruzar ofertas estruturadas de Hotmart e ClickBank com evidências da Biblioteca de Anúncios da Meta já persistidas pelo coletor canônico. O gate comercial exige ao menos dez ofertas comparáveis dos marketplaces; anúncios Meta não contam para esse mínimo. Anúncio ativo, comercial e observado por pelo menos duas vezes ao longo de trinta dias constitui sinal de investimento sustentado, nunca prova isolada de vendas. A conclusão deve preservar anunciante, texto, destino, snapshot, datas, contagem de observações, longevidade e a ressalva metodológica.

Para anúncios comerciais gerais no Brasil, o coletor canônico da Descoberta pode ser o navegador
público limitado do Product Discovery Worker. Ele recebe somente a URL oficial vinculada pelo
backend, confirma país, Instagram e status ativo e reporta cards visíveis sem login ou cookies
persistentes. CAPTCHA, bloqueio, filtro ausente ou layout ambíguo devem migrar para observação humana
supervisionada; nunca podem ser convertidos em zero anúncios ou ausência de mercado.

Na ClickBank, Gravity deve ser preservada como tração observada, com identidade estável do produto entre coletas. O coletor não pode fabricar score de sucesso fixo: ausência de Gravity permanece ausência de evidência, e Gravity nunca deve ser apresentada como número de vendas.

Os estados canônicos do dossiê são `RESEARCHING`, `UNDER_REVIEW`, `READY_FOR_TEST`, `APPROVED`, `DISCARDED` e `CONVERTED_TO_PLAN`. Evidências devem registrar fonte e data; pareceres devem registrar agente, decisão, justificativa, riscos, recomendação e data. Dossiês e pareceres não autorizam gastos, publicação, preço ou campanhas.

Ao entrar em `UNDER_REVIEW`, a decisão estratégica deve nascer como execução consumível na fila exclusiva de Atena. Atena recebe o contexto factual persistido completo, executa prompt e schema versionados em seu worker e reporta conclusão ou falha ao backend. Uma lease inativa admite uma única retomada e a reincidência termina bloqueada. O frontend deve mostrar aguardando, trabalhando, bloqueado ou concluído a partir do estado persistido, nunca por inferência de logs.

O monitor operacional deve tratar `opportunity_agent_review` como a execução canônica da decisão de Atena. Decisão `PENDING` sem `started_at` por mais de três minutos deve aparecer como alerta, mesmo quando o executor estiver `READY`. Após indisponibilidade transitória do backend, o worker volta a consultar a mesma fila idempotente; uma lease `RUNNING` órfã é retomada automaticamente uma única vez para Atena, sem criar outro parecer nem perder o vínculo com o dossiê. Registros antigos de Psique, Plutus e Hermes permanecem apenas como histórico e nunca voltam à fila.

Pesquisa externa indisponível não é resultado de mercado. Se todas as consultas de Argos falharem por provider, credencial, rede ou contrato HTTP, o ciclo deve falhar e bloquear a tarefa com provider, quantidade de tentativas e status auditáveis. Somente consultas executadas com sucesso e sem resultados podem concluir com zero evidências. O MCP deve expor health e logfile do executor no host operacional real.

O Plano Comercial é a fonte oficial comum para usuários e agentes atuarem sobre o mesmo objetivo de vendas e lucro. Cada criação ou alteração deve gerar uma versão imutável contendo objetivo, público, dor, desejo/oferta, canal, métricas, orçamento, receita esperada, realizado, gargalo, próxima ação e critérios de sucesso e parada.

Gargalo atual, causa-raiz e próxima ação são contratos operacionais completos, não rótulos curtos. Esses campos devem aceitar contexto longo sem truncamento e permitir que especialistas iterem autonomamente na própria sandbox até devolver `APROVADO`, `BLOQUEADO` ou `AJUSTE_NECESSARIO`. Essa autonomia cobre investigação, produção e homologação local; não autoriza gasto, publicação externa, alteração de preço ou mudança de oferta.

## Separação das experiências administrativas

Plano Comercial e Planejamento Mensal devem possuir itens de menu e rotas administrativas distintos. A visão de planos comerciais apresenta o contrato de negócio, seus bloqueios, decisões e histórico; a visão de planejamentos mensais apresenta metas, semanas, funil e execução temporal. As duas visões podem consumir a mesma fonte canônica persistida, mas não devem voltar a ser apresentadas como um único item de navegação. A criação de um Plano Comercial permanece na visão comercial; o planejamento mensal apenas organiza sua execução no período.

## Projeção financeira por Plutus

Cada Plano Comercial pode solicitar projeções tipadas a Plutus. A solicitação congela a versão oficial do plano, abre uma tarefa correlacionada na mesa do agente e entra em fila própria, independente da conciliação financeira e dos gates de vídeo. A resposta deve conter cenários conservador, base e otimista, premissas explícitas, ponto de equilíbrio, investimento inicial e limite por ciclo recomendados, critérios de continuar, ajustar e parar, limitações e candidato de aprendizado.

As premissas mínimas estruturadas do plano são preço da oferta, custo variável por venda, tráfego mensal esperado, conversão esperada, CAC esperado, taxa esperada de reembolso e custo operacional fixo. Elas são hipóteses versionadas, nunca métricas realizadas. Valores monetários e tráfego não podem ser negativos; percentuais devem permanecer entre zero e cem; custo variável por venda não pode superar o preço. Cada produto mantém suas próprias premissas e histórico.

Quando essas premissas estiverem ausentes, o plano pode solicitar uma definição conjunta. Atena deve pesquisar evidências públicas, comparar exatamente três alternativas e propor faixas conservadoras; Plutus deve validar preço, margem, CAC, ponto de equilíbrio e compatibilidade com o teto. Somente uma validação financeira `APPROVE` pode preencher campos ainda vazios e criar uma versão identificada por `ATENA_PLUTUS`; valores já definidos pelo usuário nunca são sobrescritos. A aprovação registra hipóteses de planejamento e não autoriza gasto, campanha, publicação ou alteração de preço público.

Projeção nunca é receita realizada, não altera orçamento e não autoriza gasto. O aprendizado de Plutus deve comparar posteriormente previsão e resultado real; conclusões novas permanecem candidatas e só podem ser promovidas pelo fluxo governado após evidência fora da amostra.

Quando uma etapa de comunicação exigir economia aprovada como predecessor, somente a projeção
concluída para a versão vigente do plano pode compor o contrato de entrada. A evidência vem da
execução financeira canônica, não apenas da tarefa correlacionada na mesa. Parecer antigo permanece
no histórico e não libera Íris depois de mudança de versão comercial.

Tarefas, gates e execuções devem registrar `planId` e número da versão consumida, ou uma referência equivalente `commercial-plan:<id>@v<numero>`, para que decisões antigas não sejam reinterpretadas com contexto novo. O monitor deve apresentar esse vínculo quando existir.

Versões novas não autorizam gasto, publicação, mudança de preço ou campanha. Esses atos continuam sujeitos aos gates próprios. Planos de produtos distintos não podem compartilhar orçamento ou resultados; MUSA e Agenda Cheia permanecem segregados.

## Prestação de contas dos agentes no frontend

O detalhe do Plano Comercial deve exibir uma visão persistida e atualizada dos trabalhos dos agentes vinculados ao plano. A visão deve apresentar agente, tarefa ou execução, estado, dificuldade, decisão externa pendente, referência de origem, data e versão do contexto consumido.

O mesmo painel deve consolidar teto, custo de campanha, custo de IA, custo total e receita em BRL. Ciclos audiovisuais devem preservar também teto, custo conhecido e decisão financeira em USD. A tela não pode deduzir estados por heurística nem misturar registros de outros planos; tarefas entram no dossiê pela referência versionada `commercial-plan:<id>@v<numero>`, e execuções entram por vínculo persistido com `commercial_plan_id` ou com o experimento oficial do plano.

O detalhe de cada plano deve separar a operação em duas abas dentro do mesmo contexto: `Estado atual` e `Histórico`. O estado atual deve apresentar somente fase vigente, um gargalo consolidado por causa-raiz, responsável, causa, impacto, próxima ação, critério de aprovação, prazo ou última atualização e comando operacional aplicável. O gargalo oficial versionado tem precedência sobre dificuldades derivadas de execuções e marcos da mesma jornada.

O histórico deve preservar pareceres, execuções, bloqueios superados e versões em ordem cronológica, com filtros por agente, fase, estado e período. Registro histórico é evidência para confirmar ou descartar uma causa-raiz, nunca fila de trabalho nem bloqueio ativo. Agentes e orquestrador devem consumir prioritariamente o estado atual deduplicado e devolver decisão operacional curta (`APROVADO`, `BLOQUEADO` ou `AJUSTE_NECESSARIO`) acompanhada da próxima ação. Ausência de dado estruturado pode ser mostrada como premissa faltante; estado operacional não pode ser inferido de logs ou texto histórico.

As metas devem respeitar os tipos de produto definidos em `docs/canonical/product-types-canon.v1.md`: low-ticket como pacote de infoprodutos de baixo custo produzido por IA, e Produto IA como infoproduto/ferramenta com integracao OpenAI por tras e experiencia simples para o usuario.

## Regra canonica de metas numericas

Todo plano comercial mensal deve persistir metas numericas planejadas em campos estruturados, nao apenas em texto livre:

- `max_budget`: custo maximo permitido no periodo.
- `target_revenue`: receita minima que valida o objetivo principal.
- `operational_revenue_target`: meta operacional desejada acima do minimo.
- `experiments_to_create`: quantidade de experimentos que devem ser criados no periodo.
- `experiments_to_publish`: quantidade de experimentos que devem ser publicados e validados no periodo.
- `products_to_validate`: quantidade maxima de produtos que receberao evidencia comercial nova no periodo.
- `product_types_to_explore`: quantidade de tipos de PDE distintos que serao investigados sem desviar a execucao do produto prioritario.
- `approaches_to_test`: quantidade de mecanismos, territorios de desejo ou abordagens comerciais comparaveis que serao testados.
- `customer_conversations_target`: quantidade de conversas estruturadas com clientes ou compradores usadas como evidencia comercial.

Essas metas medem aprendizado, nao atividade vazia. Um produto, tipo ou abordagem so deve contar quando possuir hipotese explicita, evidencia registrada e decisao `CONTINUE`, `ADJUST` ou `STOP`. Criar cadastros duplicados nao aumenta o realizado. Metas de exploracao nunca substituem vendas aprovadas, entrega satisfatoria e conversao do funil como resultados principais.

Quando o gargalo vigente for instrumentacao, checkout ou entrega, o plano semanal deve limitar o trabalho em paralelo. A referencia inicial recomendada e: um produto prioritario, um tipo de PDE, ate duas abordagens, um experimento pronto para execucao e cinco conversas com clientes. Novas frentes so avancam depois que o gate do gargalo atual estiver verde.

Todo marco semanal do plano pode persistir metas numericas planejadas proprias:

- `target_cost`: custo maximo da semana ou acumulado definido para o marco.
- `target_revenue`: receita esperada da semana ou acumulada.
- `experiments_to_create`: quantidade de experimentos que devem ser criados ate o marco.
- `experiments_to_publish`: quantidade de experimentos que devem ser publicados/validados ate o marco.
- `products_to_validate`, `product_types_to_explore`, `approaches_to_test` e `customer_conversations_target`: recorte semanal das metas de aprendizado, sujeito aos mesmos gates de evidencia e foco comercial do plano.

## Planejamento semanal como realização temporal do plano

O Planejamento Semanal não é uma estratégia paralela nem uma cópia editável do Plano Comercial. Ele é a realização temporal da versão oficial do plano: transforma objetivo, gargalo, próxima ação, meta de venda, lucro e geração de valor em compromissos executáveis de segunda a domingo.

Todo compromisso semanal novo deve persistir:

- `plan_id` e `plan_version_number` usados na decisão;
- ação objetiva e resultado comercial mensurável esperado;
- agente responsável, quando a execução for delegada;
- estado `PLANNED`, `IN_PROGRESS`, `BLOCKED`, `COMPLETED` ou `CANCELLED`;
- prazo dentro da semana comercial;
- custo e receita planejados, quando aplicáveis;
- custo, receita, funil e evidências realizados vindos das fontes operacionais oficiais.

Uma versão posterior do Plano Comercial não reinterpreta silenciosamente compromissos antigos. O usuário deve conseguir comparar planejado e realizado por semana, identificar agente, dificuldade e decisão externa pendente e verificar contribuição para venda, lucro e entrega de valor. Planejamento semanal não autoriza gasto, publicação, alteração de preço ou campanha; os gates próprios continuam obrigatórios.

Antes de abrir nova frente, a semana deve priorizar o gargalo oficial do plano. Um compromisso concluído sem evidência de avanço comercial deve ser registrado como entrega operacional, não como venda, lucro ou validação de valor.

## Regra canonica de semanas comerciais do mes

O planejamento mensal do Marketing Hub deve organizar semanas comerciais sempre a partir das segundas-feiras existentes dentro do proprio mes. A semana comercial nao deve ser calculada por dia 1 a dia 7, nem por semana ISO do calendario, porque o objetivo e manter ciclos operacionais completos de segunda a domingo.

Definicao obrigatoria:

- Semana 1 do mes: comeca na primeira segunda-feira do mes e termina no domingo seguinte.
- Semana 2 do mes: comeca na segunda segunda-feira do mes e termina no domingo seguinte.
- Semana 3 do mes: comeca na terceira segunda-feira do mes e termina no domingo seguinte.
- Semana 4 do mes: comeca na quarta segunda-feira do mes e termina no domingo seguinte.
- Semana 5 do mes: existe somente quando houver uma quinta segunda-feira no mes; comeca nessa quinta segunda-feira e termina no domingo seguinte, mesmo que o domingo caia no mes seguinte.

Dias anteriores a primeira segunda-feira do mes nao pertencem a nenhuma semana comercial daquele mes. Esses dias devem ser tratados como periodo de preparacao, fechamento, transicao ou execucao remanescente do mes anterior, conforme a decisao operacional registrada no plano.

Quando a semana 5 atravessar para o mes seguinte, ela continua pertencendo ao mes em que sua segunda-feira comecou. O planejamento do mes seguinte so inicia sua semana 1 na primeira segunda-feira dentro desse mes seguinte.

Exemplo canonico para agosto de 2026:

| Semana comercial | Inicio | Fim |
|---|---:|---:|
| Semana 1 | 2026-08-03 | 2026-08-09 |
| Semana 2 | 2026-08-10 | 2026-08-16 |
| Semana 3 | 2026-08-17 | 2026-08-23 |
| Semana 4 | 2026-08-24 | 2026-08-30 |
| Semana 5 | 2026-08-31 | 2026-09-06 |

## Regra canonica de metricas de funil no planejamento

Planos mensais, marcos semanais e objetivos comerciais devem passar a usar metricas de funil como parte obrigatoria da decisao. O planejamento nao deve acompanhar apenas custo, receita e quantidade de experimentos; deve explicitar o volume esperado, executado e a conversao de cada etapa critica do caminho ate venda, liberacao de acesso e primeiro uso.

O funil minimo para produtos digitais com acesso/autenticacao deve considerar:

- Visualizacao do anuncio.
- Clique no anuncio para o produto ou experiencia.
- Entrada na tela inicial do produto ou experiencia.
- Login ou criacao de conta.
- Visualizacao da oferta de assinatura ou compra.
- Clique no plano, checkout ou etapa equivalente de pagamento.
- Assinatura, compra ou pagamento aprovado.
- Acesso liberado.
- Primeiro uso ou ativacao.

Para cada etapa do funil usada em planejamento mensal, marco semanal ou objetivo, os relatorios e telas devem favorecer campos estruturados:

- `planned_total`: volume planejado para a etapa.
- `actual_total`: volume executado na etapa.
- `conversion_from_previous_step`: percentual vs. etapa anterior.
- `cost_per_conversion`: custo por conversao da etapa quando houver custo atribuivel.
- `unique_count`: quantidade de usuarios/leads unicos quando a fonte permitir deduplicacao.
- `last_event_at`: data/hora do ultimo evento usado no calculo.

Objetivos comerciais devem ser formulados em termos de gargalo de funil, nao apenas em termos de entrega operacional. Exemplo: "aumentar clique no checkout", "reduzir queda entre login e oferta", "elevar primeiro uso apos acesso liberado" ou "validar custo aceitavel por assinatura aprovada".

Quando o produto ou experimento nao possuir alguma etapa do funil minimo, o planejamento deve declarar a etapa equivalente ou marcar a etapa como nao aplicavel. Nao se deve remover silenciosamente a etapa, para manter comparacao mensal, semanal e entre produtos.

## Regra canonica de executado

O planejamento deve separar claramente planejado de executado. O usuario edita as metas planejadas; o backend atualiza os valores executados a partir das fontes operacionais persistidas.

Todo plano mensal e todo marco semanal devem expor:

- `actual_campaign_cost`: custo executado de campanha.
- `actual_ai_cost`: custo executado de IA.
- `actual_total_cost`: soma de campanha e IA.
- `actual_revenue`: receita executada.
- `actual_experiments_created`: quantidade de experimentos criados no periodo.
- `actual_experiments_published`: quantidade de experimentos publicados no periodo.
- `execution_synced_at`: data/hora da ultima sincronizacao.

Custos de campanha devem vir de metricas de campanha persistidas. Custos de IA devem considerar geracoes e execucoes de IA persistidas, convertidas para BRL quando a origem estiver em USD. Receita deve vir de metricas financeiras persistidas. Quantidades de experimentos devem vir das tabelas operacionais de experimento e publicacao/campanha, nunca de texto livre.

## Regra de interpretacao

Um experimento so deve contar como publicado quando estiver comercialmente validavel: pagina de venda existente, anuncio apontando para a pagina correta e coletores de metricas ativos na pagina.

## Preparacao para IA

A futura integracao com IA deve consumir esses campos estruturados como entrada primaria para gerar cenarios, alertas e recomendacoes. Texto livre pode complementar o contexto, mas nao substitui as metas numericas persistidas.

## Vinculo e homologacao da jornada comercial

Um Plano Comercial pode e deve reunir varios experimentos independentes que testem sua oferta, publico, canal ou abordagem. O plano nunca pode ficar preso a um unico experimento nem substituir o historico anterior ao iniciar um novo teste. Cada vinculo deve ser persistido de forma relacional e auditavel; metricas, evidencias, custos, pareceres e homologacoes continuam segregados por `experimentId` e somente depois sao consolidados no nivel do plano.

O detalhe do plano deve listar todos os experimentos vinculados e permitir adicionar novos testes sem apagar os anteriores. Toda acao operacional, inclusive homologacao, deve exigir a escolha explicita do experimento alvo. O campo legado singular `commercial_plan.experiment_id` existe apenas para migracao e compatibilidade temporaria e nao pode ser usado como fonte unica de verdade ou como criterio para descartar evidencias de outros experimentos do mesmo plano.

A homologacao deve usar dados segregados (`mh_test=1`), cobrir landing, eventos, amostra, e-mail, checkout, pagamento de teste, briefing, producao e entrega, e registrar as evidencias produzidas. Esse comando nao autoriza publicacao, ativacao de midia ou gasto. Aquisicao somente pode avancar quando os gates essenciais estiverem comprovados.
## Biblioteca de Imagens e Vídeos do Produto

Todo plano comercial pode manter imagens e vídeos reutilizáveis do produto sem duplicar o arquivo no JSON do plano. Cada vínculo registra URL persistida, tipo (`IMAGE` ou `VIDEO`), descrição, finalidade (`ADS`, `LANDING`, `SOCIAL`, `DELIVERY` ou `PRODUCT_PROOF`), origem, direitos de uso, versão e estado (`DRAFT`, `APPROVED` ou `RETIRED`). `PRODUCT_PROOF` identifica captura, exportação ou evidência fiel de um produto que não é visual por natureza; não é sinônimo de criativo e não pode ser inventado por modelo generativo.

Dédalo dirige o produto, seus entregáveis e suas provas reais pelo fluxo versionado do PDE. O Estúdio
de Imagens do plano materializa somente comunicação pré-compra de Íris no container
`iris-image-studio`. Os códigos persistidos `themis-image-studio` e `pde-visual-materialization`
permanecem apenas como aliases históricos. O backend continua autoridade da fila, segregação por
plano, storage, orçamento, auditoria e avanço; o AI Worker não gera nem edita imagens desse fluxo.

Quando a própria imagem faz parte do que a cliente recebe, ela nasce no fluxo de Dédalo com
`DELIVERY`, separada da comunicação. Quando o produto é serviço, software, texto, método ou outra
entrega não visual, a captura fiel entra como `PRODUCT_PROOF`. Íris cria somente `LANDING`, `ADS` e
`SOCIAL` e deve referenciar `PRODUCT_PROOF` ou `DELIVERY` aprovado. É proibido misturar finalidade
pós-compra e comercial no mesmo novo job. Edição sempre cria nova versão e preserva a origem.

Em produto personalizado, o conteúdo `DELIVERY` promove o negócio da cliente final e não deve ser forçado a vender o próprio produto digital, repetir o preço da oferta ou funcionar isoladamente como anúncio do Marketing Hub. O reuso em `LANDING`, `ADS` e `SOCIAL` significa exibir o arquivo real e íntegro como prova enquadrada do que o comprador recebe. Persona, marca e contato sintéticos são aceitos apenas em homologação segregada explicitamente identificada; nunca podem ser apresentados como cliente ou depoimento real.

Entregável que declare formato Story deve ser produzido e aprovado em proporção nativa `9:16`; o Estúdio usa `1152x2048` para a prova premium em 2K. Saída `2:3`, barras adicionadas ou corte posterior não substituem a homologação do Story real. Reenvio idêntico do comando da tela deve devolver o job vigente em vez de criar nova geração, custo ou artefato.

A execução produtora nunca aprova o próprio arquivo. Depois da persistência em `DRAFT`, o backend abre uma revisão visual para uma nova execução independente de Têmis, que inspeciona o arquivo real em alta definição e somente promove para `APPROVED` quando qualidade, fidelidade à entrega e reuso comercial atingem os mínimos canônicos. Falhas ou lease vencida voltam ao fluxo auditável sem publicação automática.

A comunicação de Íris, incluindo criativos, landing e social, reutiliza apenas itens `APPROVED` de
Dédalo. Íris materializa copy, hierarquia, composição, peças estruturadas e bitmap comercial, mas
nenhuma etapa pode reconstruir o produto nem consumir referência de outro plano. Criação comercial
sem `PRODUCT_PROOF` ou `DELIVERY` aprovado bloqueia antes de consumir tentativa. O subprocesso de
criativos escolhe formatos pelo canal: comunicação não audiovisual pertence a Íris; vídeo usa Apolo
e o Estúdio Áudio Vídeo quando valor, custo e risco justificarem essa rota. Têmis revisa o ativo final
e nunca o produz.
Aprovação nunca autoriza publicação, gasto ou mudança do experimento para `RUNNING`.

## Objetivo e resultado final dos processos comerciais

Por decisão de 2026-08-16, toda definição BPM comercial deve declarar separadamente o objetivo do processo e o resultado final verificável. O objetivo explica a transformação de negócio perseguida; o resultado descreve o artefato, o estado e as evidências mínimas que precisam existir no fim. Expressões genéricas como “processo concluído” não constituem resultado suficiente.

As versões vigentes dos processos de fabricação do produto, criação de criativos, geração de landing, homologação do experimento, otimização e venda/entrega devem terminar com critérios auditáveis ligados a produto íntegro, ativo visual aprovado, checkout canônico, instrumentação válida, receita, entrega ou satisfação, conforme o domínio. O nó final não pode esconder pendência, `DRAFT`, falha de entrega, aprovação própria ou impacto apenas estimado.

Quando houver imagem comercial, os papéis são segregados: `iris-image-studio`, orientado pelo
contrato de comunicação de Íris, cria ou edita e persiste a nova versão como `DRAFT`;
`meta-ad-approver-worker` executa Têmis somente para inspecionar o arquivo real e decidir o gate
independente. Somente o container produtor recebe a credencial do provedor visual; somente o revisor
recebe identidade Codex e ferramentas de inspeção. O backend continua sendo a única autoridade de
fila, persistência e avanço. Fabricação, criativo, landing, homologação, otimização e entrega devem
preservar essa segregação e a linhagem até o ativo-fonte da Biblioteca Audiovisual. Íris é autora da
peça comercial, mas não do entregável nem da prova real de Dédalo.
