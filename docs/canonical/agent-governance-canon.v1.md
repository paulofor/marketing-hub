# Governança de agentes v1

## Objetivo

O cadastro de agentes do Marketing Hub é a fonte operacional para identidade, versão, estado,
objetivo, métricas, gatilhos, ferramentas e limites de autoridade. Prompt e schema permanecem
versionados no módulo executor responsável.

## Contrato mínimo

Todo agente deve possuir `agentKey` estável, `nickname` curto e exclusivo para comunicação, versão incremental, status operacional, responsável,
objetivo de negócio, métricas de sucesso, modelo, política de gatilhos, política de autoridade e
caminhos versionados do prompt e schema. Cada gravação cria uma fotografia imutável em
`agent_version`.

O `nickname` é obrigatório, possui no máximo 60 caracteres e identifica o agente para pessoas e
telas. Ele não substitui o nome descritivo nem a `agentKey`, que continua sendo a identidade técnica
estável das integrações. Cada alteração do apelido deve permanecer registrada na versão do contrato.

O contrato operacional também deve registrar separadamente a responsabilidade do agente, as regras
que o Orquestrador usa para coordená-lo, as informações obrigatórias de entrada, o que deve ser
analisado e o que deve ser oferecido como saída funcional. Esses campos não substituem prompt ou
schema do executor e não podem ampliar limites de autoridade.

Status permitidos: `DRAFT`, `TEST`, `ACTIVE`, `PAUSED` e `BLOCKED`.

## Autoridade

O cadastro nunca amplia silenciosamente a autoridade do executor. Ações de gasto, preço,
publicação, comunicação em massa, início ou retomada de campanhas e abertura de PR exigem regra
explícita e aprovação humana. Gates determinísticos podem autorizar apenas ações preventivas já
previstas no contrato canônico do agente.

## Gestão administrativa

A tela `Gestão de agentes` é a entrada canônica para criar e revisar contratos. Cada salvamento
incrementa a versão e preserva uma fotografia auditável. As regras do Orquestrador descrevem
acionamento, pré-condições, prioridade, bloqueios e encaminhamento humano; a execução continua
determinística no backend e os módulos executores apenas consomem pendências e reportam resultados.

## Mesa de trabalho e caixa de entrada

Cada agente cadastrado possui uma mesa própria em `/agents/{id}`. A mesa apresenta sua identidade,
caixa de entrada, remetente, data, prioridade, estado e resultado esperado de cada solicitação.
Pessoas abrem tarefas pela interface administrativa; agentes delegam pelo contrato comum
`POST /api/internal/agent-tasks/v1`, informando obrigatoriamente a própria `agentKey` e a do
destinatário. O backend valida ambas no catálogo e preserva a autoria.

Os estados canônicos são `PENDING`, `IN_PROGRESS`, `BLOCKED`, `COMPLETED` e `CANCELLED`. Uma tarefa
não concede novas permissões ao destinatário: gasto, publicação, preço, campanha, comunicação em
massa e aprovação continuam sujeitos aos gates do agente. A caixa de entrada registra intenção e
andamento; executores continuam consumindo seus endpoints `pending` específicos, coordenados pelo
backend, até que exista um orquestrador canônico que materialize a tarefa na fila operacional.

## Melhorias sugeridas pelos agentes

Todo agente cadastrado pode sugerir uma melhoria do Marketing Hub enquanto realiza uma tarefa. A
sugestão deve usar o contrato comum `POST /api/internal/system-improvements/v1` e informar a
`agentKey`, um título objetivo, a descrição acionável e, quando existir, a referência da tarefa,
job, execução ou experimento que originou a percepção.

O backend valida a identidade no catálogo, registra a data em UTC e preserva o agente solicitante.
As sugestões formam um backlog administrativo próprio, visível no menu `Melhorias do Sistema`;
elas não são memória promovida, não alteram código automaticamente e não ampliam a autoridade do
agente. Implementação, publicação, gasto ou mudança comercial continuam sujeitos aos gates e às
aprovações já definidos neste cânone.

## Migração do Operador de Crescimento

O Operador usa a chave `growth-operator`, versão inicial `1`, modelo `gpt-5.6-sol` e execução
orientada a eventos. Toda nova execução deve apontar para a `agent_version` ativa no momento em
que foi criada, preservando qual contrato fundamentou a decisão.

O prompt e o schema canônicos são:

- `growth-operator-worker/src/main/resources/prompts/growth-operator/v1/diagnosis.md`
- `growth-operator-worker/src/main/resources/prompts/growth-operator/v1/diagnosis-schema.json`

## Arquitetura obrigatória dos agentes

O contrato detalhado e bloqueante está em
`docs/canonical/premium-ai-agent-architecture-canon.v1.md` e prevalece para qualquer agente novo,
migrado ou reativado.

Os primeiros agentes independentes — Cliente, Financeiro, Operador de Crescimento e Estrategista de
Experimentos — são o modelo arquitetural obrigatório para agentes novos ou migrados. Todo agente
operacional deve possuir:

- módulo executor e container próprios, com CI/CD e identidade operacional independentes;
- Codex ChatGPT executado em sandbox isolada e `read-only`, com timeout, usuário sem privilégios,
  filesystem somente leitura e diretório temporário efêmero;
- servidor MCP próprio, versionado no módulo, com catálogo mínimo de ferramentas tipadas e restritas
  ao domínio do agente; o MCP deve consultar somente endpoints oficiais do backend e registrar cada
  ferramenta, origem, correlação e horário usados;
- prompt e schema versionados no módulo executor;
- consumo iniciado exclusivamente pelo endpoint `pending` canônico e callback oficial de resultado;
- request/contexto, resposta bruta, modelo, status, erro, telemetria e custos persistíveis e
  correlacionados à execução;
- segregação determinística entre experimentos, clientes ou planejamentos e teste preventivo contra
  mistura de dados;
- backend como fonte de verdade dos estados, tentativas, gates e avanço. O agente nunca publica,
  gasta, aprova humanamente ou dispara a próxima etapa por conta própria.

É proibido embutir um agente em worker genérico de geração, chamar OpenAI diretamente no lugar do
Codex, compartilhar MCP irrestrito entre domínios ou tratar apenas um cadastro na tela como agente
operacional completo. Reuso deve ocorrer nos contratos do backend e nos padrões arquiteturais, não
pela mistura de responsabilidades entre executores.

Todo agente novo deve seguir o blueprint e a matriz de homologação definidos em
`premium-ai-agent-architecture-canon.v1.md`. A revisão de arquitetura deve comprovar o gate global
antes de mudar o cadastro de `DRAFT`/`BLOCKED` para `TEST`; somente resultados reais e auditáveis
podem justificar a passagem posterior para `ACTIVE`.

## Métrica de maturidade

A qualidade de um agente é medida por pendências resolvidas e resultados posteriores comprovados,
não por quantidade de ciclos, relatórios, estimativas ou recomendações.

## Coordenação entre agentes

Quando Estrategista, Operador de Crescimento e Especialista em Aprovação de Anúncios participarem
do mesmo experimento, a coordenação deve seguir `orquestrador-agentes-canon.v1.md`. O Orquestrador
é determinístico, persiste evidências e nunca amplia a autoridade individual dos agentes.
# Painel de maturidade e ciclo compartilhado

O Cadastro de Agentes deve consolidar execuções, pendências e resultados confirmados dos executores Operador, Financeiro, Cliente e Aprovador de Anúncios Meta sem criar uma fila paralela. Cada executor permanece responsável por sua execução; o backend normaliza os indicadores. Simulação, hipótese, relatório ou impacto estimado nunca contam como resultado confirmado. A maturidade deve priorizar taxa de conclusão, pendências encerradas com evidência posterior e dez resultados confirmados antes de qualquer ampliação de autonomia.

No Aprovador de Anúncios Meta, cada parecer persistido por versão do criativo representa uma execução. O executor canônico é o módulo independente `meta-ad-approver-worker`, que usa Codex em sandbox somente leitura e MCP próprio. Revisões e correções pendentes formam as pendências do ciclo; decisões encerradas formam resoluções; apenas `APPROVED` conta como resultado técnico confirmado. O custo consolidado inclui a chamada auditada de revisão e as versões visuais geradas por solicitação corretiva do agente, sem atribuir ao aprovador o custo do criativo inicial. Aprovação técnica nunca substitui aprovação humana, publicação ou resultado comercial.

Quando o Aprovador decidir `ADJUST` ou `REJECTED`, a correção seguinte deve usar um contrato visual estruturado, persistido e auditável. O contrato precisa separar requisitos obrigatórios, elementos proibidos e critérios objetivos de aceitação; o worker deve incorporar todos esses itens ao prompt final e bloquear a geração quando requisitos ou critérios estiverem ausentes. Parecer em texto livre ou recomendação vaga não autoriza nova chamada paga. A versão gerada retorna ao mesmo gate multimodal, preservando limite de tentativas, custo e aprovação humana.
