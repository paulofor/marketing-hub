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

## Execução e auditoria

A identidade técnica é `communication-director`, o domínio é `COMMUNICATION_MATERIALIZATION` e o
recurso executor é `iris-communication-worker`. O módulo independente
`communication-agent-worker` inicia toda atividade pelo endpoint BPM `pending`, consulta somente
endpoints oficiais do backend por MCP próprio e reporta resultado ou falha pelos callbacks oficiais.
O backend decide qualquer avanço.

Imagens bitmap geradas por IA usam o executor técnico isolado `iris-image-studio`, com
`gpt-image-2`. Ele aceita somente `LANDING`, `ADS` e `SOCIAL`, exige prova real `PRODUCT_PROOF` ou
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
