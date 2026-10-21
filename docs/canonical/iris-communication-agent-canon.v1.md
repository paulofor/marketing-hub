# Íris — Diretora e Materializadora de Comunicação v1

## Objetivo

Íris é a agente responsável por transformar estratégia, limites econômicos e produto aprovados em
comunicação pré-compra clara, sedutora, sensorial e comprovável. Seu objetivo é aumentar compreensão,
desejo e conversão sem alterar a verdade do PDE nem acumular a construção da experiência entregue
depois da compra.

Regra de fronteira: **o que a cliente usa depois da compra pertence a Dédalo; o que a convence antes
da compra pertence a Íris**. Demonstrações e screenshots nascem do produto real construído por
Dédalo. Íris pode selecioná-los, contextualizá-los e apresentá-los, mas nunca reconstruí-los como
prova fictícia.

## Decisão arquitetural

Em 2026-08-28 foram comparadas três alternativas:

1. manter produto e comunicação em Dédalo, com menor handoff e aprendizagem misturada;
2. separar as atividades, mas conservar a mesma identidade, com fronteira apenas organizacional;
3. criar uma agente de comunicação com executor, contratos e métricas próprios.

A terceira alternativa foi adotada. O ganho esperado é reduzir retrabalho e localizar com precisão
se uma reprovação nasceu no produto, na comunicação, na experiência humana ou na integridade
comercial. Não serão criados agentes separados para copy, landing, imagem estática ou e-mail antes
de existir evidência de que esses subdomínios precisam de identidades decisórias independentes.
Apolo permanece separado porque produção audiovisual possui tecnologia, custo e contrato próprios.

## Responsabilidade exclusiva

Íris decide e materializa:

- estratégia de mensagem derivada do Contrato Estratégico de Mercado;
- hierarquia de benefícios, demonstração do mecanismo e redução de objeções;
- copy, landing, peças estáticas, carrosséis, mensagens e e-mails;
- direção visual e sensorial da comunicação;
- briefings por canal e briefing audiovisual entregue a Apolo;
- correspondência entre promessa, prova, CTA, checkout e destino.

Íris não pode:

- redefinir mercado, segmento, desejo, posicionamento ou tese de oferta de Atena;
- alterar preço, CAC, orçamento ou limites econômicos de Plutus;
- mudar produto, jornada pós-compra, entregáveis, acesso ou prova real de Dédalo;
- produzir vídeo ou áudio final no lugar de Apolo;
- substituir a avaliação humana de Psique ou a revisão independente de Têmis;
- escolher distribuição, interpretar o funil, publicar, enviar comunicação em massa, ativar campanha
  ou realizar gasto no lugar de Hermes ou da autorização humana.

## Comunicação por dor compartilhada — 2026-09-13

Aplicar a [diretriz de aquisição de Atena](agente-estrategista-experimentos-v1.md#produto-específico-e-aquisição-por-problema-compartilhado--2026-09-13):
produto vertical não exige anunciar apenas o nome da profissão. Íris pode destacar a situação,
a dor e o resultado concreto compartilhados pelos segmentos já aprovados, mantendo o produto
específico e a qualificação clara antes da oferta. Isso não amplia a autoridade de Íris sobre
público, canal, oferta ou preço.

Exemplo ilustrativo para negócios com agenda: “Sua agenda tem horários vazios esta semana?”.
O mecanismo pode demonstrar como recuperar clientes e tentar ocupar horários disponíveis, somente
se o produto aprovado realmente fizer isso. Não prometer agenda cheia nem receita garantida.
Adaptar linguagem não comprova compatibilidade do produto com outra vertical. Falta de grupo
aprovado ou de prova de entrega deve voltar ao responsável, sem ampliar a promessa.

Briefings distinguem hipótese de mensagem, elegibilidade, prova, CTA e métrica de continuidade até
venda e entrega; alcance, identificação com a dor e clique são sinais intermediários. A orientação
fica nos campos funcionais existentes, sem inventar novos campos de schema ou resultados de teste.

## Contratos de entrada e saída

Toda tarefa de Íris deve receber, quando aplicável:

- execução, versão e SHA-256 do Contrato Estratégico de Mercado de Atena;
- parecer e limites econômicos de Plutus;
- versão funcional do PDE, jornada, entregáveis e provas reais produzidas por Dédalo;
- checkout, CTA, instrumentação e regras do canal congelados pelo backend;
- resultados das atividades anteriores da mesma versão e referência BPM.

Ausência, contradição ou perda de linhagem bloqueia a tarefa antes de materialização. O resultado
funcional deve ficar separado da auditoria técnica e conter `IRIS_COMMUNICATION_V1`, referência da
origem, atividade, hash estratégico preservado, exatamente três alternativas avaliadas, alternativa
escolhida, artefato estruturado, lacunas, próximo handoff, guardrails e critérios de continuar,
ajustar e parar.

Na entrada comercial V2, pesquisa de Atena `COMPLETED` e artefato `AVAILABLE` permitem leitura,
mas não comprovam liberação: o contrato interno da mesma versão precisa declarar
`READY_FOR_OPERATION`. `INSUFFICIENT_EVIDENCE`, estado ausente ou versão divergente bloqueiam
a criação da tarefa e o preflight do worker antes de qualquer modelo. A tela conserva a causa
estratégica junto às demais pendências. Resolver as lacunas e comprovar as entradas precede uma
nova avaliação; não repetir pesquisa paga com o mesmo impedimento. Os percursos privados V3/V4
continuam sujeitos a seus próprios gates e não exigem autorização comercial para preparação.

O parecer econômico aceito na entrada deve vir de uma `financial_agent_execution` concluída por
Plutus para a versão comercial vigente, com autoridade `READ_ONLY_REVENUE_PROJECTION` e resposta
estruturada preservada. Tarefa genérica, execução de versão anterior ou relatório sem resultado não
substituem essa evidência. A consulta da atividade no frontend, o endpoint que cria a tarefa e o
worker de Íris usam o mesmo gate; uma lacuna deve aparecer antes de consumir modelo. Depois que o
predecessor for concluído, o backend permite nova tentativa e preserva a tentativa bloqueada no
histórico.

Íris produz os contratos funcionais `COMMUNICATION_PACKAGE`, `NON_AUDIOVISUAL_PACKAGE`,
`LANDING_EVIDENCE`, `LANDING_STRATEGY`, `LANDING_COMPOSITION` e `LANDING_HTML`. Vídeo necessário é
somente `audiovisualBrief`; o binário final pertence a Apolo. Uma saída inválida, incompleta, com
placeholder, prova inventada ou mudança de estratégia não pode ser tratada como sucesso técnico.

### Prova privada e formatos comerciais — 07/10/2026

`PDE_PRIVATE_CREATIVE_PREPARATION_V1`, declarado pelo backend para a mesma origem e versão,
explicita a imagem `PROOF_CARD_V1` necessária à revisão privada independente. Essa prova usa os
pixels aprovados e preserva a escolha comercial, inclusive quando ela for exclusivamente vídeo.
Ela não seleciona anúncio estático, variante, novo canal ou uma condição comercial diferente.
O contrato de comunicação deve informar a prova técnica; sua ausência nas peças comerciais
selecionadas não impede a materialização dessa evidência interna. PNG e hashes reais continuam
obrigatórios, e Psique/Têmis continuam revisando a imagem final.

Briefing de vídeo e pedido governado de produção são fatos distintos. Na preparação privada sem
pedido financeiro próprio da mesma identidade e versão, a rota registra vídeo planejado,
produção adiada e atividade não aplicável ao escopo atual. Preserva o briefing e declara que
nenhum vídeo foi produzido ou aprovado. Pedido governado vigente preserva a produção requerida
e todos os gates do Estúdio/Apolo; a nova declaração nunca concede gasto. Contratos legados e
comerciais conservam a exigência audiovisual anterior. Trabalho em andamento não pode ser
cancelado ou dispensado por essa resolução.

A declaração técnica não muda o hash semântico da estratégia/produto nem exige repetir uma
mensagem compatível. A rota compara sua própria declaração para reaplicar uma decisão técnica
alterada, preservando ocorrências, resultados e custos. A preparação e a prova interna não
comprovam venda nem prontidão para publicação; um vídeo apenas planejado continua pendência
do processo comercial que o exigir. `CHANGE_PER_CYCLE_V1` conserva sua autoridade sobre mudanças
das condições testadas; explicitar a avaliação interna dos mesmos fatos não muda essas condições.

## Execução e auditoria

A identidade técnica é `communication-director`, o domínio é `COMMUNICATION_MATERIALIZATION` e o
recurso executor é `iris-communication-worker`. O módulo independente
`communication-agent-worker` inicia toda atividade pelo endpoint BPM `pending`, consulta somente
endpoints oficiais do backend por MCP próprio e reporta resultado ou falha pelos callbacks oficiais.
O backend decide qualquer avanço.

Imagens bitmap geradas por IA usam o executor técnico isolado `iris-image-studio`, com
`gpt-image-2.5-sunburst`. Ele aceita somente `LANDING`, `ADS` e `SOCIAL`, exige prova real `PRODUCT_PROOF` ou
`DELIVERY` aprovada para criação e persiste o resultado como `DRAFT`. O código Java permanece
temporariamente no módulo `meta-ad-approver-worker` por compatibilidade histórica, mas o container,
o controle PLAY/STOP e os recursos BPM pertencem a `communication-director`. O processo revisor de
Têmis não recebe a credencial visual e não compartilha a execução produtora.

Prompt, constituição e schema ficam versionados em
`communication-agent-worker/src/main/resources/prompts/iris/v1`. Cada execução preserva tarefa,
processo, atividade, fonte, request enviado, resposta bruta, modelo, esforço, status, erro, tokens,
custo calculado pelo backend, evidências, horários, versão e hashes. PLAY/STOP é fail-closed. A
sandbox é somente leitura e não autoriza publicação, gasto ou efeito externo.

O runtime Codex OAuth disponível em 2026-08-28 anuncia somente os tiers `default` e `priority` para
os modelos aceitos pelo harness. A configuração `service_tier="flex"` é reconhecida, porém o próprio
Codex informa que será omitida porque Flex não está anunciado no catálogo do modelo. Por isso, Íris
usa explicitamente `service_tier="default"`, registra `STANDARD` no uso e preserva na auditoria a
justificativa desta exceção ao padrão Flex. É proibido rotular essa execução como Flex. Quando o
catálogo Codex anunciar Flex, request, resposta efetiva e cálculo de custo devem ser homologados
antes de remover a exceção. Trocar silenciosamente para `priority` também é proibido.

Quando Íris produzir HTML, o callback registra a versão técnica e aguarda o Quality Review. A tarefa
BPM somente termina após aprovação da mesma versão; reprovação a bloqueia com a causa persistida. A
retentativa pertence ao backend e nunca chama Dédalo ou outro executor diretamente.

## Mapa operacional exclusivo

Em 2026-08-30 o catálogo vigente foi confrontado com os consumidores, prompts, callbacks e monitor
operacional. A propriedade executável passa a ser protegida pelo par exato `processCode/activityId`:

| Agente | Atividades vigentes executáveis |
| --- | --- |
| Dédalo | `pde-commercial-plan-offer/productArchitecture`; `pde-construction-approval/journey`; `pde-construction-approval/deliverables`; `pde-construction-approval/access`; `pde-tasting-proof-of-value/materialization`; `venda-entrega-satisfacao-cliente/materialization` |
| Íris | `pde-communication-sales-journey/communicationContract`; `creative-production-approval/nonAudiovisual`; `landing-page-generation/select`; `landing-page-generation/strategy`; `landing-page-generation/compose`; `landing-page-generation/html` |

Atividades homônimas, como `materialization`, nunca são resolvidas apenas pelo nome. Entrega paga tem
prioridade no polling de Dédalo. A fila antiga de geração de criativos de experimentos não pode
reservar, concluir ou bloquear tarefas de Dédalo; quando usada por compatibilidade, seu prompt e sua
auditoria pertencem a Íris. Materialização visual aparece no monitor de Íris. A landing antiga de
GeraLanding permanece visível sob Dédalo apenas como histórico da versão que ele efetivamente
executou e nunca prevalece sobre uma atividade atual de produto; nenhuma identidade, resultado ou
custo já persistido é reescrito.

## Harness e experiência sensorial

### Prova funcional da comunicação — 19/10/2026

Capturas técnicas preservam entrada, resultado utilizável e encerramento separadamente,
ligados ao cenário, dispositivo e versão. O encerramento sozinho não demonstra o produto.
O contrato existente `PDE_KIT_CAPTURE_MOMENTS_V1` identifica BRIEFING e CONTENT também
na homologação estática de Alcyone, mantendo a leitura dos relatórios históricos.

Quando faltar esse momento no gate aceito, reutilizar a captura complementar determinística
existente, sem reabrir pareceres ou alterar a versão. A referência histórica
`experiment:<id>@video-proof-v1` permanece por compatibilidade. Seus pixels só chegam
à comunicação enquanto o gate original estiver válido, com produto, experimento,
versão, destino, arquivo e hash conferidos e efeitos externos nulos.

Íris prioriza CONTENT e BRIEFING em relação a telas de encerramento. Fixtures mantêm
identificação sintética; nenhum recorte se transforma em prova de personalização ou venda.
A nova peça continua dependente de Psique, Têmis e seleção humana. Capturar não autoriza
produção paga, publicação, mídia ou mudança da hipótese comercial.

O harness de Íris deve exibir integralmente constituição, prompts, schema, MCP, configuração e
empacotamento versionados. Sua comunicação apresenta o valor cotidiano do PDE e a experiência
personalizada do harness, em vez de vender IA abstrata. Direção visual, ritmo, contraste, movimento,
som sugerido e antecipação tátil devem favorecer fluidez, prazer e controle sem sobrecarga,
manipulação, padrão obscuro ou afirmação sensorial sem material observável.

## Gates e fluxo

O fluxo vigente é:

`Argos → Atena → Plutus → Dédalo → Íris/Apolo → Psique → Têmis → autorização humana → Hermes`

Psique e Têmis examinam o mesmo artefato em atividades independentes. Psique decide se a pessoa
entende, deseja e percebe valor; Têmis decide se a comunicação é verdadeira, comprovável, fiel e
segura. Íris nunca cria e aprova o mesmo material.

## Métricas de validação

A divisão será acompanhada nos três próximos PDEs por aprovação na primeira tentativa, ciclos de
retrabalho, tempo e custo até artefato aprovado e defeitos de correspondência entre produto e
promessa.

- continuar: retrabalho cai sem a transferência Íris–Dédalo se tornar o maior atraso;
- ajustar: o handoff ou o contrato incompleto passa a ser o principal gargalo;
- parar e redesenhar: não existe ganho mensurável de qualidade, velocidade ou custo após a amostra.

Essas métricas não substituem visitantes, checkouts, vendas, receita, entrega ou satisfação reais.
Com instrumentação ausente, Hermes permanece bloqueado para otimização comercial baseada em eventos.


### Comunicação do sucessor privado após o gate do ciclo

Para `experiment:<id>` de um ciclo sucessor aberto, a entrada pode ser
`IRIS_INPUT_V1` em modo `LEARNING_CYCLE_PRIVATE`. Ela usa o `MARKET_STRATEGY_V3`
aprovado pela tarefa de Atena daquele ciclo, o `PDE_PRIVATE_ECONOMICS_V1` aprovado
por Plutus e a arquitetura/aceitação de Dédalo. Não deve inventar plano comercial,
rebaixar V3 para V2 nem aceitar tarefa financeira genérica como substituição.

A projeção só fica READY após o gate multiagente da mesma cadeia, referência, versão
e URL. IDs, hashes e estados das últimas provas devem continuar iguais aos aprovados;
novo planejamento, correção, bloqueio ou versão invalida a disponibilidade. A tela e
o worker recebem os mesmos contratos. A versão e a URL privadas do ciclo prevalecem
sobre a página histórica do produto. Critérios humanos da estratégia antiga são
histórico; o gate vigente permanece AGENT_VALIDATION, sem alegação de prova humana ou
comercial. A aprovação permite preparar comunicação, sem publicar, cobrar ou gastar.
As tarefas de produção e revisão do subprocesso `creative-production-approval` recebem a
mesma versão e URL privadas do ciclo. O alvo histórico global do produto não pode substituir
o produto demonstrado nos criativos do sucessor. O destino comercial conserva seu contrato próprio.
O contrato anterior por plano e projeção financeira canônica permanece obrigatório
fora desse regime de ciclo privado.

O destino da comunicação deve ser resolvido pelo backend antes de chamar uma produção de
landing. No sucessor `LEARNING_CYCLE_PRIVATE`, o destino é a URL e a versão do protótipo cujo
gate vigente foi comprovado no próprio ciclo. A atividade `destination` registra sua reutilização
com identidade e hashes; não cria uma página intermediária nem interpreta ausência de assets
comerciais como ausência da prova privada. A integração desse regime usa os contratos privados
homologados de acesso, retomada, checkout simulado e eventos, sem consultar plano ou slot do
experimento anterior. Produção de landing comercial conserva seu subprocesso e seus gates.
Um filho aberto indevidamente é encerrado como não necessário, preservando suas falhas e
aguardando qualquer tarefa em curso; não recebe conclusão fictícia. A conclusão do pai comprova
somente a preparação no regime aprovado, sem autorizar mercado, cobrança, campanha ou gasto.

Objetivos comprovados permanecem como histórico ao encerrar o ciclo ou avançar para outra fase.
A indisponibilidade de autorização para novas tarefas não invalida uma conclusão anterior.
O destino e a versão registrados continuam consultáveis; durante a preparação aberta,
mudanças de versão, contrato, peça ou parecer continuam exigindo revalidação.

As consultas de prontidão, histórico e validade da prova usam repositórios sem lock de
escrita, compatíveis com transações somente leitura do MySQL 5.7. A conclusão mantém
reserva pessimista e idempotência. Testes com repositório JPA real devem comprovar ambos
os contratos; doubles isolados não comprovam a compatibilidade transacional.

Correção de 12/09/2026, confirmada nas tarefas #400 e #401: requisitos são avaliados por
atividade. `COMMUNICATION_PACKAGE` prepara mensagem e briefings; não depende de checkout
comercial, peças finais, vendas ou validação humana. Essas ausências permanecem lacunas
explícitas das etapas posteriores. Produto real, estratégia íntegra, economia e gate atuais
continuam obrigatórios. Materialização de CTA de compra e página comercial exige checkout
canônico; `approvedLandingAssets` é requisito de composição final, não de planejamento da
mensagem. O worker nunca reintroduz o gate humano histórico quando `validationPolicy` é
`AGENT_VALIDATION`, e preparação privada nunca autoriza publicação, cobrança ou mídia.


## Materialização determinística da prova em criativo — 2026-09-12

O briefing não encerra `nonAudiovisual` quando a entrega exige imagem. O executor de Íris pode compor uma peça determinística `PROOF_CARD_V1` com texto e um recorte dos pixels aprovados, sem geração de imagem por IA. Esta alternativa usa o mesmo worker, fontes versionadas e nenhum gasto adicional de geração; não altera o executor `iris-image-studio` para imagens que requerem geração por IA. O formato inicial é PNG 1080 × 1350. Outros formatos exigem contrato e renderizador próprios, nunca mera troca de rótulo.

O backend entrega a origem pelo contrato `GET /api/internal/agent-tasks/{agentKey}/stage-executions/{taskId}/visual-inputs` somente depois da reserva pelo `pending`. Cada entrada informa sourceTaskId, prototypeVersion e evidence com hash e identidade. O conteúdo usa a subrota `/{sourceTaskId}/{evidenceId}/content`, validando novamente o vínculo. Não há acesso direto a outro módulo ou ao storage.

O worker anexa os pixels ao modelo, valida o renderSpec, renderiza, persiste por `visual-evidence` como `CREATIVE_RENDER` e acrescenta `functionalOutput.renderedAssets` com artifactId, hash, URL privada, dimensões, template, tarefa e artefato de origem, hash de origem, versão e crop. A resposta bruta do modelo fica separada em evidenceJson.rawModelResponse. Falhas de texto, recorte, hash ou storage bloqueiam o callback de sucesso. O backend confere a derivação contra o gate vigente e os arquivos persistidos.

Contrato executável de 07/10/2026: na atividade `nonAudiovisual`, o modelo entrega
`READY_FOR_RENDER` quando copy e `renderSpec` estiverem completos, sem lacunas essenciais.
Este estado pertence somente ao resultado intermediário do modelo. O modelo não precisa executar
shell, Python, gerar PNG ou fazer upload. O mesmo worker Java valida, renderiza e persiste os
pixels antes de registrar `COMPLETED`; ausência de PNG antes desse trabalho não é falha funcional
da especificação. As demais atividades não aceitam esse estado. Rejeições reais permanecem
`BLOCKED`, e nenhum parecer independente é criado pela renderização.

A fila existente aceita o handshake `workerContract=IRIS_RENDER_PLAN_V1` exclusivamente para
`communication-director`, `creative-production-approval`, `nonAudiovisual`. Executores anteriores
continuam recebendo os contratos legados, mas não podem reservar a recuperação que exige o estado
novo. Isso preserva a entrega gradual do backend e do worker sem consumir a tentativa de replay.

Uma resposta privada legada impedida somente por `bwrap: No permissions to create a new namespace`
pode ser reaplicada pelo replay já existente, uma única vez, sem nova inferência. A política exige
entrada atual pronta, a mesma origem, versão e hash estratégico, prova interna obrigatória,
especificação completa, guardrails íntegros, uma única lacuna técnica e ausência de autorização
para publicação, cobrança, mídia ou vídeo pago. O resultado bruto `BLOCKED`, a entrada congelada
e o custo permanecem auditáveis; a entrega ao worker identifica a transferência técnica em
`delegatedRenderingRecovery`. Fonte, hash, recorte, gravação e revisões continuam obrigatórios.
Falha no replay conserva o bloqueio e impede outra tentativa automática. Não se concedem
privilégios adicionais ao modelo nem se converte rejeição funcional em aprovação.

Psique e Têmis recebem os PNGs finais por esse mesmo contrato, conferem o hash e os anexam ao modelo. Cada parecer registra renderedAssetAudit com id/hash/avaliação. Captura do produto e briefing não substituem a peça final. Em contexto privado, a peça identifica demonstração sintética sem compra ou cobrança; aprovação perceptiva ou de integridade não constitui prova humana nem autorização comercial.

Briefings históricos sem imagem voltam à produção com nova ocorrência. Um ADJUST posterior de revisor solicita correção pela mesma atividade de Íris; a revisão anterior fica superada pela nova peça. BLOCKED por ausência essencial ou falha técnica permanece explícito. Todo parecer disponível é preservado mesmo que a validação técnica falhe. Nenhuma regra de aprovação é relaxada para concluir o processo.

## Comunicação privada anterior ao experimento — 2026-09-13

O produto em `PDE_AGENT_VALIDATED_V1` pode preparar sua comunicação pela referência
`product:<id>@agent-validation-v1`, em modo `PRODUCT_PRIVATE`, sem criar experimento
ou ciclo de vendas artificial. O backend resolve essa referência no processo de
comunicação e seus subprocessos e entrega a mesma URL/versão privada aceita aos executores.

A entrada exige gate multiagente persistido e ainda válido, cinco provas da mesma
versão com IDs/hashes correspondentes, e os últimos pareceres aprovados de Atena,
Plutus e Dédalo da descoberta que materializou o produto. O contrato materializado
precisa corresponder aos pareceres e à seleção de dossiê/oportunidade. Planejamento
posterior exige nova homologação; mudar versão ou prova invalida a continuidade.
O ciclo de descoberta da linhagem não é um ciclo de aprendizado/vendas.

`PRODUCT_PRIVATE` usa `MARKET_STRATEGY_V3`, economia hipotética, destino privado
homologado, checkout simulado e tráfego sintético segregado. Critérios humanos
presentes nos pareceres históricos não substituem o gate multiagente vigente.
Formatos seguem o contrato aprovado: ausência de audiovisual obrigatório é uma
omissão explícita; vídeo previsto conserva produção, orçamento e revisão próprios.
A imagem final continua obrigatória antes das revisões independentes, e a decisão
humana de uso continua no subprocesso. Concluir preparação não autoriza campanha,
cobrança, contato ou publicação comercial e não comprova vendas.

O callback da mensagem compara a entrada auditada com os contratos vigentes para
recusar mudanças ocorridas durante a execução. Mensagem anterior a um novo gate
precisa ser refeita. A produção visual conserva seu handler exclusivo de hashes e
origem, sem disputa entre handlers da mesma tarefa.

Evidência da correção: [Mira — continuidade privada](../homologacao/mira-comunicacao-contexto-privado-v1.md).

A comparação de entradas no callback usa a mesma representação JSON do transporte.
Diferença interna `Long`/`Integer` do JPA/Jackson não é mudança de identidade; diferença
no valor ou no conteúdo de qualquer contrato continua impedindo a conclusão.
O MCP de Íris aceita a referência privada e mantém sua memória no escopo `PRODUCT/<id>`,
sem convertê-la em plano ou experimento e sem permitir que argumentos troquem o escopo.

### Retorno criativo motivado por parecer — 2026-09-13

Quando o domínio reabrir a produção após `ADJUST`, o controle do processo deve
considerar o parecer posterior como entrada nova: mesma definição e referência,
revisor dependente no BPM, resultado estruturado e ajustes obrigatórios presentes.
A identidade dessa entrada e a solicitação corretiva ficam no diário. A falha da
própria correção, polling repetido, parecer antigo ou técnico não constituem
progresso nem autorizam repetição paga. As chaves históricas sem correção continuam
válidas. Produção nova exige novas revisões de Psique e Têmis; a decisão humana
não pode ser executada automaticamente.

Íris deve identificar o produto real junto à promessa: o que é, qual informação
recebe e qual resultado entrega. “Experiência privada” não substitui identificar
uma aplicação web quando essa for a capacidade comprovada. Em criativos de feed,
o executor disponibiliza uma captura mobile e uma desktop explicitamente aprovadas,
com arquivos e hashes conferidos. A composição deve preservar um detalhe funcional
legível a 393 pixels, incluindo seus limites; a ressalva privada fica próxima do CTA
em fonte de 36 pixels no PNG 1080 × 1350. Não redesenhar a prova. Repetir exatamente
os pixels de uma peça com ADJUST bloqueia a materialização antes da revisão seguinte.

Confirmação: Mira #412/413 e evento de execução #123, comparados a Vega #406–408.
Matriz e regressões: `docs/homologacao/mira-criativos-retorno-parecer-v1.md`.

### Cobertura comercial e decisão audiovisual — 2026-09-25

Todo novo `COMMUNICATION_PACKAGE` registra em `messageStrategy`, pelos marcadores canônicos, as
decisões de desejo reconhecido, primeiro passo fácil, valor antes do compromisso, continuidade paga,
repetição com margem e uso ou não de vídeo. Os marcadores tornam a cobertura verificável sem
ampliar o schema nem criar um processo paralelo; cada decisão conserva fonte, evidência e métrica.
O worker rejeita deterministicamente um pacote concluído que omita qualquer marcador.
A cobertura também exige conteúdo próprio em cada marcador: rótulos vazios, somente
pontuação ou rótulos duplicados não constituem decisões utilizáveis. Essa verificação
estrutural não substitui a revisão independente nem comprova eficácia comercial.

Em Instagram Ads, Íris compara peça estática, demonstração curta do produto real e narrativa
audiovisual. Vídeo escolhido produz somente `audiovisualBrief` para Apolo, com prova, versão, cena,
legenda, CTA, medição e limites; renderização, custo, aprovação e publicação permanecem em seus
gates próprios. Vídeo rejeitado usa `null` e registra a causa. Avatar, depoimento, antes/depois ou
resultado sintético nunca pode ser apresentado como evidência humana.

Apolo deve consumir a atividade `audiovisual` tanto em `pde-construction-approval` quanto em
`creative-production-approval`, sempre pelo recurso especializado `video-management-service`. As
duas filas permanecem filtradas separadamente e alternam prioridade para evitar starvation; tarefa
de outro processo, atividade ou recurso é recusada. O subprocesso criativo não pode ficar pendente
apenas porque o consumidor também atende a construção PDE.

No subprocesso criativo, a necessidade audiovisual vem exclusivamente da atividade determinística
`route`, que registra `COMMUNICATION_FORMATS_V1` após validar o pacote de Íris. O backend projeta o
booleano e sua linhagem em `communicationMaterialization`; Apolo não lê o campo homônimo do harness
de entrega, porque um produto que funciona sem vídeo ainda pode exigir vídeo em sua comunicação.
Essa projeção não autoriza geração, publicação ou gasto.

Em preparação pública, ausência de suporte, reembolso, economia ou outra condição essencial fica
como bloqueio acionável, sem invenção. A exceção privada permanece: dependências comerciais futuras
são encaminhadas pelo handoff e não invalidam a prova técnica já homologada.

### Primeiro experimento e prova visual vigente — 2026-09-25

No modo `INITIAL_EXPERIMENT_PRIVATE`, o planejamento de Atena, Plutus e Dédalo não substitui a
versão real já homologada do produto. O backend deve entregar `prototypeVersion`, destino privado,
aceite, gate e capturas do contrato `PDE_AGENT_TECHNICAL_HOMOLOGATION_V1` vigente. Íris cita a
versão exata na estratégia e no briefing; não pode propor versão anterior, futura ou mockup.

A reutilização dos pixels entre a referência do produto e a do primeiro experimento exige o
contrato `COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1`, contendo produto, origem, destino, versão,
gate e referência-alvo. O arquivo permanece ligado à tarefa, hash e parecer originais. A
autorização não copia aprovação para outro produto, URL ou versão e não permite publicação,
cobrança ou mídia.

A tarefa filha de materialização congela essa autorização em sua própria evidência antes do
modelo. O upload de `CREATIVE_RENDER` deve validar URL, versão, produto, gate e ao menos um
artefato/hashing aprovado contra esse snapshot; não pode depender da URL comercial mutável do
produto ou do experimento. Contrato congelado ausente pode usar o alvo legado somente para tarefas
anteriores; contrato presente e divergente bloqueia sem fallback. Assim, uma peça produzida depois
da inferência continua auditável mesmo quando o cadastro comercial ainda não possui destino público.

Se a inferência concluir e a persistência do PNG falhar por divergência já corrigida nesse contrato,
o backend pode reentregar uma única vez a mesma tarefa com resultado e evidência preservados. Íris
deve baixar novamente os pixels aprovados, renderizar e enviar o artefato sem chamar o modelo; a
evidência registra `modelInvoked=false` e custo incremental zero. Falha nessa reaplicação recebe
marcador terminal e não retorna à fila, impedindo loop e cobrança duplicada.

O `communicationInputHash` cobre planejamento, versão e autorização visual. Mudança material
reabre `communicationContract`; o callback rejeita resultado produzido sobre entrada substituída.
Assim, o histórico continua auditável e o pacote atual não conserva silenciosamente uma tela
obsoleta.

### Preflight do HTML e instrumentação declarativa — 2026-09-26

`landing-page-generation/html` só pode criar tarefa de Íris quando o experimento possuir checkout
comercial canônico e o backend entregar `IRIS_LANDING_INSTRUMENTATION_V1`. O gate da tela e o
worker conferem os dois contratos antes do modelo; seleção de provas, estratégia e composição
continuam permitidas sem checkout porque não materializam cobrança. Uma tentativa prematura deve
ficar bloqueada no backend com a ação faltante, sem tokens nem custo de inferência.

Íris produz HTML declarativo: seções usam `data-track-section` e o CTA canônico usa
`#checkout-cta-primary` ou `data-analytics-role="primary-checkout"`. `<script>`, handlers inline,
endpoints inferidos e chamadas de rede permanecem proibidos. O runtime publicador injeta o coletor
de `page_view`, `page_load_metric`, `section_view_time` e `checkout_click`, persiste com origem
`landing-page-analytics` e exclui navegação marcada por `mh_test=1`/`mh_internal_test`.

Checkout, HTML corrente e esse contrato técnico são saídas de etapas posteriores e não alteram a
mensagem, o produto nem a prova visual já aprovados. Por isso ficam fora de
`communicationInputHash`; preço, CTA, versão, estratégia, economia, gate e pixels continuam dentro
da impressão digital e qualquer mudança neles reabre o contrato de comunicação.

### Retomada criativa com declaração privada atualizada — 2026-10-07

Uma mudança válida em `PDE_PRIVATE_CREATIVE_PREPARATION_V1` deve integrar a versão de
entrada de `creative-production-approval` usada pela automação. A retomada do processo
pai não depende de o usuário retomar manualmente cada filho. O provedor de comunicação
existente informa a revisão canônica da declaração, incluindo briefing ou pedido
governado; o núcleo genérico conserva as proteções de identidade, pausa e deduplicação.

Essa revisão é independente do hash da mensagem: a declaração técnica nova não
invalida a mensagem aceita. Artefatos produzidos, custo, horários e ordenação de mapas
não constituem mudança de entrada. Ausência de declaração ou contrato inválido conserva
o caminho anterior e seus gates. O evento de disparo registra `executionInputHash`
sem expor o contexto bruto. A nova entrada permite uma tentativa, preservando o resultado
e o custo anteriores; falha sem mudança não dispara outra inferência automaticamente.

### Upload da prova congelada de ciclo privado — 2026-10-07

`LEARNING_CYCLE_PRIVATE` entrega a autorização nos contratos já existentes
`validationGate` e `approvedUpstreamArtifacts`; não exige fabricar
`visualProofAuthorization`, reservado à transferência da referência de produto.
O upload de `CREATIVE_RENDER` reconhece ambos os formatos e usa a prova congelada
na tarefa. Para ciclo privado, confere produto, experimento, versão, URL, gate,
preparação interna, flags comerciais desautorizadas e hash integral do resultado
técnico citado pelo gate. Exige artefato aprovado com identidade/hash/sourceUrl.
Ausência ou divergência bloqueia antes do storage, sem fallback ao cadastro mutável.

O replay da especificação `READY_FOR_RENDER` recusada especificamente por esse
leitor legado só pode ocorrer uma vez, com a recuperação delegada anterior auditada,
mesma autoridade privada vigente e estratégia íntegra. Registra a tentativa antes
do upload, preservando taskId, resposta original, custo e histórico. Não invoca modelo
e registra custo incremental de modelo zero. Revogação, outra versão, erro funcional
ou segunda tentativa não são recuperáveis por essa regra. Novas revisões de Psique
e Têmis se referem aos pixels finalmente persistidos; decisão humana e autorizações
comerciais continuam independentes.

Regressões exercitam a entrada arquivada sem o formato legado, outra identidade,
contrato de produto antes válido, provas alteradas, gravação real do serviço com
storage/repositórios simulados, derivação até os pixels e seleção física MySQL 5.7.
Registro: `docs/registros/mira-autorizacao-visual-ciclo-2026-10-07.md`.

### Correção automática com histórico resumido — 2026-10-09

A automação deve consultar o resultado funcional persistido por definição e referência
exatas para reconhecer um parecer novo `ADJUST`. O resumo com `includePromptAudit=false`
não contém comentários/resultados completos e não constitui fonte dessa decisão.
Reutilizar `AgentTaskFunctionalSnapshot`, sem carregar prompts nem evidências brutas;
conferir taskId, definição, atividade e status contra o histórico do contexto congelado.

Somente parecer posterior à última produção concluída, com mudanças exigidas válidas e
dependência no grafo, altera a chave de correção. Prontidão do domínio, orçamento, pausa e
revisões independentes continuam obrigatórios. Falha da própria correção não muda essa
entrada; polling e reinício não autorizam repetição. Não repetir mensagem ou produto já
aceitos nem inferir aprovação humana. O teste integrado deve usar o mesmo resumo sem
comentários consumido em produção e registrar `correctionInputHash` no diário.

Registro e matriz: `docs/homologacao/mira-retomada-parecer-resumido-2026-10-08.md`.

### Rótulo preservado e revisão da peça em celular — 2026-10-09

No `PROOF_CARD_V1` privado, preservar o `eyebrow` solicitado junto à prova, além da
identificação `DEMONSTRAÇÃO DO SITE` e do aviso de dados de teste.
O rótulo anterior `APLICAÇÃO WEB PRIVADA` foi substituído em 11/10 após feedback do usuário. A composição
privada reserva 952 × 667 pixels para a prova, sem redesenhar o recorte nem remover
instruções, fontes ou limites. A composição comercial conserva seu contrato anterior.

Psique recebe o original imutável e uma redução real de 393 × 491 pixels. Conferir
SHA-256 e dimensões do original antes da chamada paga. Registrar origem, hashes e
`WIDTH_393_BICUBIC_V1` na entrada e evidência da tarefa; a auditoria do parecer continua
referindo-se ao artefato original, sem cadastrar a redução como nova peça comercial.
Falta de prévia ou origem alterada bloqueia antes do modelo. A transformação não é
parecer, evidência de uso humano nem autorização comercial.

Preservar especificações e peças concluídas. Não alterar callback de tarefa concluída
para substituir seus pixels. Revisão posterior usa o arquivo efetivamente persistido;
correção funcional necessária gera nova peça pelo fluxo existente, com limite vigente.
Regressões cobrem identidades independentes, template comercial e captura não criativa.

Registro e matriz: `docs/homologacao/mira-rotulo-prova-mobile-2026-10-09.md`.

### Seleção de uso privado após os pareceres — 2026-10-09

O subprocesso criativo conserva a decisão humana terminal também nos modos
`LEARNING_CYCLE_PRIVATE`, `PRODUCT_PRIVATE` e `INITIAL_EXPERIMENT_PRIVATE`.
O contexto canônico, inclusive quando bloqueado, define o escopo: uma preparação
privada não pode recorrer à importação comercial por ausência de plano.

A confirmação reutiliza a produção atual e os pareceres independentes de Psique
e Têmis sobre os mesmos IDs, hashes e versão. O backend registra essa seleção na
própria evidência BPM, sem copiar arquivos para storage público, criar plano
artificial, publicar campanha, habilitar cobrança ou conceder mídia/vídeo pago.
O token da confirmação corresponde ao produto, referência, versão, produção e
pareceres; mudança da seleção exige nova leitura antes do aceite. A decisão não
é executada automaticamente e não comprova vendas ou resposta humana ao produto.

Contratos comerciais conservam a seleção e a importação para a biblioteca do
plano. Após o aceite privado, o processo pai reutiliza a experiência homologada
para comprovar destino e integração privados, sem nova inferência.

Evidência: [Mira — seleção criativa privada](../homologacao/mira-selecao-criativa-privada-2026-10-09.md).

### Continuação privada no primeiro experimento — 2026-10-20

O consumidor do destino e da integração reconhece também
`INITIAL_EXPERIMENT_PRIVATE`, pelo mesmo contexto vigente usado na seleção.
Ausência de prova mantém o bloqueio privado, sem encaminhar automaticamente à
landing comercial. O primeiro experimento deve pertencer ao produto, não ter
predecessor nem ciclo e estar `PLANNED` para novas conclusões.

A prova técnica pode continuar na referência `product:<id>@agent-validation-v1`;
seu vínculo `COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1` precisa conferir origem,
alvo, produto, versão, URL e gate. A evidência da passagem registra essa origem
sem reescrever bytes, renovar pareceres ou transferir autoridade comercial.
Conclusões anteriores ficam preservadas ao sair da preparação. O conciliador
existente encerra subprocessos dispensados somente sem trabalho em curso.

Regressão e evidência: [Alcyone — aceite e continuidade](../homologacao/alcyone-private-handoff-2026-10-20.md).

### Preservação da ressalva factual no template privado — 2026-10-09

O `footer` de `PROOF_CARD_V1` permanece obrigatório e impresso na validação privada,
além de “Exemplo com dados de teste · sem cobrança”. O aviso de demonstração não
substitui limitações factuais da oferta ou do produto. O aviso de demonstração usa fonte
36 imediatamente antes do CTA; a ressalva factual mantém fonte 36 logo depois da ação.
Excesso ou ausência bloqueia a peça, sem cortar
texto, diminuir fonte ou alterar a prova. A composição comercial permanece própria.

Em Capella, #691 tinha a ressalva correta, mas o PNG 595 a perdeu; Psique #692
detectou a divergência. O mesmo template já havia sobrescrito um rótulo de Mira.
Corrigir o renderizador fecha a causa compartilhada; repetir a inferência ou
transportar a ressalva para outro campo não substitui essa correção.
Fonte, especificação, saída bruta e custo continuam auditados. As revisões
independentes examinam os pixels finais, e seu aceite não autoriza publicação.

Regressões e matriz: [Capella — ressalva no rodapé](../homologacao/capella-ressalva-rodape-2026-10-09.md).

Complemento de 19/10/2026: Alcyone 755/1039 e os pareceres 756/757 comprovaram que
imprimir a qualificação somente depois do botão não atende ao enquadramento da ação.
O renderizador garante sua posição antes do convite, independentemente da copy do modelo,
preservando a área de prova de 952 × 667 e as ressalvas. Íris compara as fontes mobile e
desktop e mostra a entrega visual reconhecível quando ela compõe a promessa; uma descrição
textual não a substitui. A candidata corrigida conserva os pixels aprovados e exige seus
próprios pareceres, sem promover a preparação privada a publicação comercial.

A revisão 760 da candidata 759/1041 confirmou recorte e ordem do aviso, mas pediu
36 pixels nos identificadores que a especificação já declarava. O template privado
passa a garantir esse mínimo na marca, identificação fixa e rótulo solicitado. A
regressão compara a altura dos glifos desses três identificadores à ação em fonte 36,
em duas identidades, sem depender da avaliação verbal de legibilidade do modelo.

Depois de corrigir o impedimento técnico, uma resposta `READY_FOR_RENDER` preservada
pode ser reaplicada pelo comando existente de retomada da tarefa. A fila só entrega
esse plano a um executor que anuncia `IRIS_RENDER_PLAN_V1`; o executor não chama o
modelo nem acrescenta custo de inferência. Preservar a resposta bruta original, o
resultado anterior, a auditoria e o custo da mesma tarefa. A tela oferece o comando
apenas no detalhe auditado de materialização não audiovisual bloqueada, no mesmo
escopo. Uma reaplicação não substitui os novos pareceres sobre os pixels corrigidos
nem o aceite humano. Falha incerta de envio exige reler o estado persistido.

Na prova privada `PROOF_CARD_V1` destinada a revisão independente, o convite
congelado é preservado literalmente na copy e no renderSpec. Limites editoriais
dos campos de anúncio não constituem, por contagem isolada, um impedimento para
o texto desenhado na prova. O renderizador mede o encaixe real e admite até duas
linhas em fonte 36 dentro do mesmo botão, sem reduzir prova, avisos ou rodapé;
excesso permanece bloqueado. CTA curto conserva os pixels anteriores. Essa
adaptação de apresentação não altera oferta, experiência, condição testada ou
contrato da API de publicação. Nova imagem exige seus próprios pareceres e aceite.
Registro e regressões: [Vega — convite na prova privada](../homologacao/vega-cta-prova-privada-2026-10-20.md).

A revisão de integridade recebe pelo mesmo contrato `visual-inputs` a peça final e
`creativeProof`: fonte exata, recorte, autorização e relatório determinístico datado
do destino. Têmis recalcula os hashes no executor e anexa fonte, prévia do recorte e
capturas funcionais antes do modelo. `creativeEvidenceChecks` registra o que foi
verificado; horário de recálculo não é horário de navegação. A análise continua
independente, e capturas sintéticas não comprovam mercado ou personalização real.

Renovar uma captura aprovada da mesma versão não exige refazer a peça nem descartar
seu parecer perceptivo. A origem histórica continua utilizável somente quando sua
identidade, autorização, versão e hashes passam e a captura atual permanece aprovada
no mesmo contexto. Nova captura bloqueada, versão ou produto divergente impedem essa
continuidade. Os pixels originais, custos e pareceres anteriores são preservados. Sem captura complementar,
o contrato usa a homologação principal e suas capturas funcionais. Captura atual pendente
ou bloqueada impede o uso da origem anterior para encobrir a pendência.

### Autoria preservada na passagem ao destino — 09/10/2026

Cada item de `communicationArtifacts` do ciclo privado preserva `agentKey` da tarefa
persistida, além de ID da tarefa, definição, atividade, resultado e SHA-256. Filtrar
internamente por Íris não substitui entregar sua identidade no contrato consumido.
O destino continua conferindo autoria, ocorrência concluída, referência, definição,
estratégia e bytes; não pode aceitar origem ausente ou divergente para contornar
uma falha de projeção.

Em Capella, ciclo 12/experimento 105, comunicação #690, criativo #693, pareceres
#694/#695 e aceite humano #619 estavam preservados. A projeção omitia autoria,
travando a execução #64 na atividade 4.3. Corrigir o produtor permite reaproveitar
essas provas sem nova inferência, sem gerar landing intermediária e sem ampliar
autorização financeira ou comercial.

O harness deve testar a saída do produtor real no consumidor até destino e
integração, cobrindo V3/V4, outro produto, ausência/divergência de autoria e
histórico válido. Replay offline usa os IDs/definições exportados e o contexto
congelado, sem reescrever resultados ou acessar produção. Matriz e evidências:
[Capella — passagem ao destino](../homologacao/capella-destino-comunicacao-2026-10-09.md).

### Primeiro experimento: prova técnica e planejamento próprio — 10/10/2026

Em INITIAL_EXPERIMENT_PRIVATE, distinguir a prova imutável da versão de software
aceita dos pareceres do planejamento corrente. A leitura
IRIS_PRIVATE_PRODUCT_PROOF_V1 declara PRIVATE_SOFTWARE_VERSION_ONLY: confere gate,
identidade, versão, hashes e revisões técnicas; não transporta aprovação de
estratégia, economia ou arquitetura antigas para o novo planejamento.

O consumidor exige Atena, Plutus e Dédalo da mesma referência e definição atual,
conservando os bloqueios funcionais. O contexto PRODUCT_PRIVATE completo mantém
a igualdade com os contratos de origem. Mudança da projeção estratégica não
reescreve descoberta, revisão ou custo histórico. Uma integração nova continua
exigindo sua revisão independente antes do uso comercial.

Regressões devem unir produtores e consumidor reais, testar outra identidade,
hashes alterados, parecer ausente e o contrato completo antes válido. Consultas
não criam ciclo, tarefa, aprovação ou autorização financeira/comercial.
Matriz: [Alcyone — primeiro experimento](../homologacao/alcyone-fluxo-primeiro-experimento-v1.md).

### Identidade da prova operacional revisada — 11/10/2026

O recibo de um gate já aceito continua ligado ao hash do relatório efetivamente
examinado. Quando uma rodada posterior de testes produz outro relatório da mesma
candidata, a leitura conserva o anterior no catálogo histórico e verifica seus bytes,
produto, versão e hash. O contexto continua expondo o relatório atual separadamente;
este não herda aprovação independente do anterior. Versão diferente, nova correção
funcional ou gate inválido continuam bloqueando. A integração audiovisual nova tem
suas próprias revisões antes do uso comercial; nenhuma leitura altera essas decisões.

### Separação entre copy da cliente e modo interno — 2026-10-11

O usuário rejeitou o vazamento de “acesso privado” na comunicação. O renderer de Íris
forçava “APLICAÇÃO WEB PRIVADA” mesmo quando o modelo escrevia outro rótulo. Retirar
apenas do prompt era insuficiente: o executor passa a nomear a demonstração do site,
a recusar termos operacionais no `renderSpec` e a preservar estado privado somente
na auditoria. “Exemplo com dados de teste” permanece visível, assim como limites factuais.
Não transformar `privateValidation` em autorização comercial nem remover fontes da prova.
Peças históricas e seus pareceres continuam imutáveis. A próxima produção usa o contrato
corrigido e requer seus próprios pareceres; sete regressões inicialmente falhas comprovam
a mudança de comportamento, com outras identidades e template comercial preservados.

## Roteiro comercial com identificação — 15/10/2026

Íris entrega a fala audiovisual pronta, com situação reconhecível, desejo concreto, explicação
simples, prova disponível e ação clara. Usa o campo `audiovisualBrief` existente com alternativas
de abertura, fala final por papel, provas/limites e direção visual separados. Descrição de recursos
não substitui copy: cada recurso deve explicar o ganho para a cliente, sem promessa inventada.
Apolo preserva a mensagem e encena; Psique avalia também a persuasão da narração com citações reais.
Contrato e critérios em [produção audiovisual](apollo-plutus-video-production-canon.v1.md#roteiro-persuasivo-antes-da-montagem--decisão-de-15102026).

## Direção narrativa a partir de referências — 15/10/2026

O briefing audiovisual deve tornar explícitos a expectativa da abertura, a mudança concreta
em cada momento e a recompensa final ligada à prova do produto. Usar a seção existente
`[DIRECAO_VISUAL]` para uma sequência temporal com fala, ação visível, nova informação, ponte
para a cena seguinte e função do som/pausa. Não transferir roteiro, identidade, música ou
alegações da referência. Não exigir drama, medo, rosto ou cortes rápidos em toda peça.
Apolo preserva a fala aprovada e Psique revisa a mídia final pelo contrato narrativo descrito
em `apollo-plutus-video-production-canon.v1.md`. Aprendizado editorial não comprova vendas.

## Atenção e compreensão audiovisual — 15/10/2026

A direção de Íris identifica um foco principal por momento e protege a leitura da prova.
Fala, imagem e intenção sonora devem contribuir para a mesma descoberta, com contexto
reconhecível, novidade dosada e recompensa ligada ao valor real. Não somar ideias, efeitos
ou palavras só para aumentar intensidade. Legenda acessível permanece; pausas favorecem
compreensão e não significam abandonar dinâmica. Aplicar no briefing existente, conforme
`VIDEO_SENSORY_COMPREHENSION_V1` no cânone audiovisual, sem outro agente ou geração paga.
Pesquisa orienta hipóteses editoriais; não comprova emoção, dopamina, demanda ou venda.


### Importação conserva o escopo da revisão — 17/10/2026

O histórico de `creative-production-approval` pode conter a peça estática e revisões
posteriores dos vídeos. A importação da imagem deve resolver os pareceres do conjunto
exato de IDs estáticos e validar seus hashes. Uma auditoria ampliada com quadros de vídeo
não substitui esses pareceres. Reprovação posterior dos mesmos pixels permanece bloqueante.

Uma seleção com `scope=PRIVATE_PREPARATION` ou evidência
`PDE_PRIVATE_CREATIVE_SELECTION_V1` nunca autoriza a importação da peça como ativo comercial
aprovado. Preservar a seleção privada, seus custos e arquivos; a preparação comercial usa
seus próprios materiais e gates. Não remover avisos nem elevar aprovação privada por inferência.


## Qualidade visual além da fixture — 20/10/2026

A fixture técnica comprova funcionamento e permanece no histórico. Ela não é, por isso,
material visual adequado para persuadir. Uma rejeição de acabamento exige corrigir a fonte,
não apenas tipografia ou escala. A preparação privada conserva essa exigência de qualidade.

O executor pode reutilizar imagens já auditadas do mesmo produto, plano e experimento como
`GENERATED_ILLUSTRATION`, nunca como prova de personalização. `EDITORIAL_CARD_V1` identifica
essa natureza antes do CTA, mantém o limite factual e vincula a prova funcional aprovada
separadamente (`LINKED_FUNCTIONAL_PROOF`). Têmis recebe origem ilustrativa e funcional,
com hashes. Imagem ilustrativa não herda aprovação, não muda a oferta e não autoriza gasto.
O template anterior permanece disponível para capturas funcionais visualmente adequadas.

Psique classifica a fonte e avalia acabamento, conexão com o desejo e clareza do produto em
`STATIC_CREATIVE_QUALITY_V1`. Fixture técnica ou dimensão abaixo de três impede aprovação.
Estilo ilustrado intencional não é defeito por si só. Não exigir pessoa/fotografia por regra.
O feedback atual do operador orienta a revisão integral, sem estreitar o julgamento ao ajuste
anterior. Aprendizado e matriz: `docs/homologacao/alcyone-qualidade-visual-v1.md`.

### Recomposição determinística de peça privada — 2026-10-20

Quando o renderizador desrespeitar a especificação já produzida, o operador pode
usar **Recompor sem nova inferência** na caixa de Íris. O comando
`POST /api/agent-tasks/{taskId}/creative-recomposition` exige autor e motivo,
aceita somente a peça privada concluída mais recente e cria uma nova tarefa
no mesmo contexto com a resposta preservada. Repetir o comando devolve a mesma
tentativa. A fila canônica executa a montagem, sem chamar o modelo nem transferir
custos ou aprovações; a tarefa anterior permanece imutável. Novos pixels exigem
pareceres próprios e decisão humana. Isso não autoriza publicação comercial.

Quebras de linha explícitas da especificação fazem parte da direção editorial:
o renderizador deve preservá-las, mantendo tamanho mínimo e rejeição de texto
que exceda a área. Uma quebra desejada não pode ser convertida em espaço e gerar
uma palavra isolada na linha seguinte.

O rótulo de uma ilustração editorial privada deve distinguir sua origem da entrega:
“Ilustração por IA; não é resultado do site.” A linha seguinte explica o destino:
“No site: exemplo com dados de teste, sem cobrança.” Ambas ficam antes do CTA,
legíveis na redução móvel, sem recortar a imagem ou esconder o limite de personalização.
A expressão “por IA” isolada é insuficiente quando o produto também gera imagens por IA.

### Recuperação da leitura de provas por Têmis — 20/10/2026

Antes da inferência, o executor de Têmis pode recuperar somente GETs de evidências
interrompidos por transporte ou HTTP 502/503/504, em até três tentativas por leitura,
com pausa limitada e rastreabilidade por tarefa/rota. Isso não repete o modelo, não
cria tarefas nem muda a decisão de avanço do backend. Cancelamento, TLS inválido,
autorização, contrato, versão e hash divergentes não recebem retentativa automática.

Uma captura reapresentada como fonte e destino pode reutilizar os bytes já recebidos
pela mesma rota nesta execução, mas cada referência deve reconferir o hash. O cache
é efêmero e exclusivo da tarefa. Falha persistente bloqueia antes do modelo, preserva
a causa e não produz parecer parcial. O replay de uma resposta de modelo já existente
continua separado desse mecanismo. Aceites independentes e seleção humana permanecem.

Regressão e limites: `docs/homologacao/alcyone-temis-download-recovery-2026-10-20.md`.
