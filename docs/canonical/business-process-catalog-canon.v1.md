# Catálogo canônico de processos do Marketing Hub — v1

## Decisão

O Marketing Hub mantém um cadastro próprio e versionado de processos de negócio. Esse catálogo é a
fonte de verdade para propósito, responsáveis, eventos, atividades, gates, entradas, saídas e relação
com contratos técnicos. A tela canônica é `/business-processes` e a API é
`/api/business-processes`.

Processos de negócio e pipelines não são sinônimos. O processo explica e governa o trabalho ponta a
ponta; pipelines e workers executam contratos técnicos referenciados pelo processo. O catálogo não
faz polling, não consome filas e não avança execuções operacionais.

O processo de descoberta PDE deve começar por uma atividade capaz de receber **sinal humano
observado** sem exigir solução pronta. Conversas, pedidos espontâneos, entrevistas, comentários e
reclamações devem ser registrados de forma anonimizada, separando fala observada, desejo, dor,
tentativa frustrada e inferências. O sinal individual inicia pesquisa; somente confirmação
independente pode transformá-lo em dossiê de oportunidade e permitir o avanço da cadeia.

O catálogo também deve representar o fluxo contínuo de soluções prontas de IA para trabalhos reais,
definido em `docs/canonical/solucoes-prontas-ia-trabalho-canon.v1.md`. Esse fluxo separa cinco objetivos
finais: confirmar grupos de tentativas frustradas; selecionar segmento e solução; construir e aprovar
a solução pronta; comunicar e oferecer o resultado; e realimentar a descoberta com venda, uso e
satisfação. Um exemplo profissional isolado não cria automaticamente um processo vertical nem define
que o produto será PDE.

Para Produtos Digitais Experienciais, a organização macro dos processos deve seguir
`docs/canonical/cadeia-produtos-pde-canon.v1.md`. Processos especializados já publicados, como
fabricação de entregáveis, criativos, landing, homologação e venda, são reutilizados dentro dessa
cadeia e não devem ser confundidos isoladamente com o ciclo completo de criação e venda de um PDE.

Processos podem ser organizados em cadeias de criação e entrega de valor. A cadeia é versionada,
preserva as versões exatas dos processos participantes e possui objetivo, resultado e métrica
principal próprios. Ela serve à visão gerencial e não executa, agenda ou avança etapas. A tela
canônica é `/business-process-chains` e seu contrato de leitura é
`/api/business-process-chains`. Quando uma versão de processo pertencer a uma ou mais cadeias, o
detalhe em `/business-processes` deve mostrar cada cadeia e oferecer link direto para sua versão. A
consulta reversa canônica é `GET /api/business-process-chains/by-process/{processDefinitionId}`.

## Governança BPM

### Modelo operacional explícito

O catálogo e a execução adotam três níveis distintos e persistidos:

1. **Atividade:** definição versionada do trabalho dentro de uma versão de processo. Preserva o
   identificador estável no grafo, nome, objetivo, responsável, recurso ou subprocesso delegado e
   critérios de conclusão. O `diagram_json` continua sendo a fonte da topologia e dos fluxos, mas
   cada nó `TASK` também possui identidade relacional própria e não pode ser tratado apenas como
   texto embutido no JSON.
2. **Instância da atividade:** ocorrência da atividade para uma referência operacional, como um
   produto, experimento ou landing. Consolida situação, entrada, saída, objetivo atingido, bloqueio,
   custo conhecido e cobertura financeira. Uma nova tentativa da mesma execução não cria outra
   instância; retorno funcional ou novo ciclo comprovado cria nova ocorrência auditável.
3. **Tarefa/execução:** tentativa individual atribuída a um agente ou executor dentro da instância.
   Preserva request, response, evidências, erro, datas, consumo e custo próprios. Uma instância pode
   possuir zero tarefas antes de ser liberada e uma ou mais tarefas durante execução, revisão,
   correção ou reprocessamento; cada tarefa regular pertence exatamente a uma instância.

A instância, e não uma tarefa isolada, é a autoridade do estado operacional da atividade. O backend
abre e atualiza a instância ao criar, reservar, concluir, bloquear ou refazer tarefas e deriva dela o
histórico apresentado ao usuário. A conclusão de uma tarefa só encerra a instância quando os
critérios funcionais da atividade forem satisfeitos; tentativas anteriores continuam auditáveis e
compõem custo e retrabalho. Para o mesmo responsável, a tentativa mais recente substitui o estado
operacional da anterior, sem apagar seu custo ou evidência; assim, uma correção bem-sucedida encerra
o bloqueio anterior. Uma tarefa criada depois de a instância estar concluída ou cancelada abre nova
ocorrência, preservando integralmente o ciclo encerrado. Tarefas excepcionais e registros históricos incompletos permanecem
legíveis como legado até poderem ser vinculados sem fabricar dados.

Todo fluxo especializado que persista uma execução em tabela própria deve também materializar e
sincronizar a instância e a tarefa BPM reais no momento em que o backend abre, entrega, conclui ou
bloqueia o trabalho. A referência de origem deve ser estável e vincular a entidade técnica à versão
de processo e à atividade vigentes. A tabela especializada pode preservar detalhes adicionais, mas
não substitui `business_process_activity_instance` e `agent_task`; o frontend não pode reconstruir
nem sintetizar tarefas a partir dela. O histórico por atividade deve representar a execução
persistida, inclusive falhas, modelo, prompt, consumo disponível, datas, resultado e evidências, sem
converter atividade técnica em sucesso funcional.
Se uma versão nova for publicada durante uma execução já aberta, seus callbacks devem continuar na
tarefa da versão original. Renomeações de atividade entre versões só podem ser correlacionadas por
aliases explícitos do fluxo, preservando uma única execução para a mesma referência de origem.

- Cada processo possui código estável e versões imutáveis depois de publicadas.
- Uma versão nasce `DRAFT`; somente publicação explícita a torna `PUBLISHED`.
- Nomes equivalentes, desconsiderando caixa, acentos, espaços e pontuação, não podem criar processos
  com códigos diferentes; o usuário deve criar uma versão do processo canônico já existente.
- Uma versão `DRAFT` pode ser excluída somente quando não possui tarefa operacional vinculada.
  Versões publicadas, aposentadas ou utilizadas nunca podem ser excluídas pelo cadastro.
- Todos os metadados, elementos BPM e fluxos de uma versão `DRAFT` podem ser editados no Marketing
  Hub. Editar uma versão publicada cria uma nova versão em rascunho; versões `PUBLISHED` e `RETIRED`
  nunca são alteradas diretamente.
- Ao publicar uma nova versão, a anterior passa a `RETIRED`, preservando histórico e auditoria.
- O catálogo operacional em `/business-processes` mostra somente versões `DRAFT` e `PUBLISHED`.
  Versões `RETIRED` ficam na tela histórica `/business-processes/retired`, acessível pelo catálogo,
  para não competir visualmente com os processos atuais sem apagar sua rastreabilidade.
- O diagrama é persistido como grafo estruturado, não como imagem ou XML livre.
- Todo grafo precisa de exatamente um evento inicial, um final e fluxos entre elementos existentes.
- A tela renderiza o grafo persistido pelo backend e não infere status ou regra de negócio.
- Publicar uma definição não publica landing, campanha, oferta ou conteúdo e não autoriza gasto.

## Commits por produto e processo

Toda alteração versionada realizada para um produto durante um processo ou subprocesso deve ser
registrada de forma estruturada contra o `product_id` e a versão exata da
`business_process_definition`. O registro deve preservar, no mínimo, repositório, SHA completo do
commit, resumo funcional, responsável e data do registro. O mesmo commit pode atender mais de um
produto ou processo somente quando cada vínculo for declarado separadamente; inferência por nome,
status comercial, branch, PR ou commit atual da build é proibida.

O histórico da cadeia de valor do produto é a superfície administrativa canônica para consultar e
registrar esses vínculos. A inclusão deve ser idempotente para a combinação produto, processo,
repositório e SHA, sem apagar vínculos anteriores quando o produto avançar de processo. Commits são
evidência de implementação, não evidência de venda, qualidade, deploy ou objetivo funcional atingido;
o processo continua sujeito aos próprios gates e critérios de conclusão.

Os contratos canônicos são `GET /api/products/{productId}/process-commits` para consulta,
`GET /api/products/{productId}/process-commits/{commitId}` para detalhe segregado e
`POST /api/products/{productId}/process-commits` para registro. O backend deve rejeitar produto,
processo ou SHA inválidos e impedir que a tela vincule silenciosamente um commit a um processo fora
do histórico conhecido daquele produto.

## Situação das atividades por produto e processo

O histórico da cadeia de valor do produto deve oferecer, em cada processo ou subprocesso, acesso a
uma visão gerencial de **Atividades e tarefas**. Essa visão precisa explicar em linguagem de negócio
o estado geral, quantas atividades tiveram o objetivo comprovado, qual atividade exige atenção, a
causa persistida e tudo que ainda falta para concluir a versão selecionada. A lista técnica de
tarefas permanece disponível abaixo desse resumo, mas não pode ser a única forma de reconstruir a
situação.

O contrato canônico é
`GET /api/business-processes/{processDefinitionId}/products/{productId}/activity-executions`. O
backend, e não o frontend, calcula a situação de cada atividade e do processo. A instância BPM é a
autoridade quando existir. Uma tarefa composta somente comprova outra atividade quando houver
vínculo relacional em `agent_task_activity_coverage`; nesse caso a tela identifica explicitamente a
cobertura. Resultado técnico embutido em comentário, prompt ou JSON não conclui outra atividade sem
instância ou cobertura persistida. Ausência de ambos deve aparecer como atividade não iniciada, sem
inferir sucesso a partir do estado comercial, da landing, do experimento ou de outra tarefa.

Quando uma atividade ou um subprocesso atingir o objetivo, a mesma resposta do backend deve expor
o próximo passo oficial da composição publicada. Se o subprocesso concluído retornar ao processo
pai, a próxima atividade do pai deve aparecer como continuação prevista, sem fabricar entrada,
execução ou conclusão. Uma instância BPM concluída prevalece sobre tarefas bloqueadas de tentativas
anteriores preservadas apenas para auditoria; o frontend não pode reconstruir essa precedência.

Atividades de versões históricas continuam auditáveis, mas não entram no denominador de conclusão
da versão selecionada. Retentativas anteriores permanecem visíveis; o resumo usa a ocorrência BPM
mais recente da própria atividade dentro da referência de execução mais recente do produto e nunca
soma novamente custo ou tarefa composta. Uma execução antiga concluída não pode esconder bloqueio,
pendência ou ausência de trabalho do ciclo atual.

Uma atividade que exige parecer conjunto pode declarar `responsibleAgentKeys` no nó `TASK`. O
backend abre idempotente e atomicamente uma tarefa para cada agente, usando a mesma versão de
processo, atividade, referência de execução e ocorrência BPM. A atividade somente conclui quando a
tentativa vigente de todos os coautores conclui; abrir apenas um parecer é inválido e não pode
liberar a sucessora. O comando canônico da tela do produto é
`POST /api/business-processes/{processDefinitionId}/products/{productId}/activities/{activityId}/execution-requests`.
Ele somente aceita versão `PUBLISHED`, produto em `PLAY`, atividade ainda não iniciada e experimento
do próprio produto. Repetir o comando reutiliza as tarefas existentes e não duplica custo nem
execução.

## Recursos especializados por atividade

Uma atividade `TASK` pode declarar opcionalmente `executionResourceCode` quando o agente precisar de
uma capacidade que não pertence ao seu executor comum. O valor não é texto livre: deve apontar para
um recurso ativo do catálogo persistido `business_process_execution_resource`, consultado pela tela
em `GET /api/business-process-execution-resources`.

Cada recurso informa código estável, nome, tipo, agente responsável, referência do executor e
instruções de uso. A primeira capacidade oficial é `themis-image-studio`, executada no container
isolado homônimo para criar e editar imagens. Atividades sem essa necessidade permanecem sem recurso
e seguem o executor normal do agente.

O backend valida o recurso ao salvar e publicar a definição. Quando uma tarefa é vinculada, também
confirma que o agente responsável é o mesmo do recurso. O contrato `pending` entrega ao executor o
objeto `executionResource` completo. Um executor comum consulta a fila sem código de recurso e não
pode reservar atividade especializada; o executor próprio deve informar `executionResourceCode` e
só recebe atividades com correspondência exata. Recurso ausente, inativo ou atribuído a outro agente
bloqueia a execução antes do trabalho e do consumo de modelo.

O recurso não altera a regra de orquestração: o container consome pendência e reporta resultado
somente pelo backend; não chama outro executor nem decide a próxima atividade. Como versões
publicadas são imutáveis, adicionar, trocar ou remover um recurso exige nova versão do processo.

## Documentos gerados por atividade

Quando o objetivo de uma atividade `TASK` produzir um documento, a definição pode declarar
`documentOutput.label` com o nome funcional desse documento. A tela BPM apresenta no próprio
objetivo um link para os **dez documentos concluídos mais recentes** daquela atividade.
Quando o processo já possuir qualquer saída documental, seu objetivo principal também oferece uma
visão consolidada dos dez documentos mais recentes da versão inteira.

O histórico documental não cria uma persistência paralela: cada documento corresponde ao resultado
e às evidências já auditados na tarefa da atividade. A consulta preserva tarefa, origem, agente,
horário, tokens e custo estimado, sempre segregados pela versão exata do processo e pelo identificador
da atividade. Atividades legadas que já possuem resultados concluídos também recebem o link, mesmo
antes de uma nova versão declarar o rótulo explícito.

Somente atividades `TASK` podem declarar saída documental. Gates e eventos não geram tarefas e não
podem assumir autoria de documentos. A API canônica é
`GET /api/business-processes/{processDefinitionId}/activities/{activityId}/documents`; ela limita a
resposta a dez itens ordenados do mais recente para o mais antigo. Conteúdo de outro processo,
atividade, tarefa bloqueada ou execução incompleta não pode aparecer nesse histórico.
O consolidado do objetivo principal usa
`GET /api/business-processes/{processDefinitionId}/documents` e aplica o mesmo limite e segregação.

## Histórico recente de execução por atividade

Todo nó `TASK` do diagrama BPM oferece acesso às **dez tarefas mais recentes** da atividade. A
consulta usa o `process_code` canônico e o identificador estável da atividade para preservar o
histórico válido mesmo quando o usuário abriu uma versão aposentada ou quando uma nova versão do
mesmo processo foi publicada. Cada item informa obrigatoriamente a versão exata que originou a
tarefa; processos com código diferente nunca podem ser misturados.

O histórico inclui tarefas pendentes, em andamento, concluídas, bloqueadas e canceladas. A tela
apresenta somente dados persistidos pelo backend: criação, início, término, duração derivável desses
marcos, responsável, status, origem, nome interno do produto quando houver vínculo canônico, prompt
enviado, resultado/comentários, evidências, falha, modelo, esforço de raciocínio, tokens e custo
estimado. Ausência de medição ou de auditoria legada deve aparecer como não registrada, nunca como
zero, valor padrão ou inferência do frontend. JSON persistido pode ser explorado em árvore sem
alterar seu conteúdo original.

A API canônica é
`GET /api/business-processes/{processDefinitionId}/activities/{activityId}/executions`. O backend
valida que a versão selecionada existe e que `activityId` corresponde a um nó `TASK`, limita a dez
itens no banco e ordena da tarefa mais recente para a mais antiga.

Quando uma única tarefa técnica realmente cobrir mais de uma atividade do mesmo processo, essa
cobertura deve ser persistida de forma relacional em `agent_task_activity_coverage`. A mesma tarefa
real pode então aparecer no histórico de cada atividade coberta, preservando um único prompt,
resultado, intervalo, consumo e custo. É proibido ao frontend inferir a cobertura por título, copiar
a tarefa para fabricar execuções ou somar novamente seu custo. No GeraLanding, a homologação
composta de Dédalo cobre seleção de provas, estratégia, composição e HTML; a chamada técnica
correlacionada por `agent-task:<id>` é a fonte do prompt, parecer, modelo e duração exibidos.

## Primeiro processo: Geração de landing page

A versão 1 formaliza o ciclo:

`briefing → Dédalo → validação técnica → Psique → Têmis → aprovação humana`.

Na execução operacional, o worker de Dédalo deve reservar primeiro a atividade liberada pelo
endpoint BPM canônico e materializá-la, de forma idempotente, na fila técnica do GeraLanding. Uma
lease `IN_PROGRESS` sem resultado deve ser reoferecida ao mesmo executor após reinício. A conclusão
ou falha técnica atualiza a própria tarefa BPM antes de qualquer sucessora ficar elegível; é proibido
liberar Psique ou Têmis apenas pelo recebimento da tarefa de Dédalo.

Os executores de Psique e Têmis devem consumir as atividades liberadas pela fila BPM canônica,
produzir parecer estruturado e reportar resultado e evidências na própria tarefa. Psique não pode
liberar Têmis, e Têmis não pode alterar o experimento ou publicar ativos; somente o backend calcula
as predecessoras concluídas e libera a próxima atividade.

Reprovações técnicas, de percepção da cliente ou comerciais retornam a Dédalo com causa persistida e
geram nova versão. O backend do experimento continua sendo a autoridade das transições operacionais.
A aprovação humana continua obrigatória antes da publicação. A referência técnica vigente é o bloco
GeraLanding do `experiment-pipeline`, definido em
`docs/canonical/procedimento-experimento-canon.v1.md`.

## Métricas

O catálogo deve permitir medir cobertura de responsáveis, quantidade de gates e versões. Quando o
cadastro for vinculado a execuções reais, deverá medir também tempo por etapa, retrabalho, custo por
landing aprovada, reincidência de causa, CTA, checkout e vendas, sem confundir publicação da definição
com resultado comercial.

# Vínculo operacional com a Mesa de Entrada

Toda nova tarefa humana enviada pela Mesa de Entrada deve apontar para uma atividade `TASK` de uma versão `PUBLISHED` e para o agente responsável definido nessa atividade. O backend valida a versão, o tipo do elemento e o responsável; rascunhos não podem orientar trabalho operacional.

Quando `responsibleAgentKeys` estiver presente, a validação de responsabilidade aceita somente os
agentes listados e a orquestração considera todos os coautores obrigatórios. O texto de `owner`
continua legível para o usuário, mas não substitui a identidade técnica versionada dos executores.

Cada tarefa deve preservar dois marcos temporais canônicos: `received_at`, registrado somente quando o executor reserva a atividade pelo endpoint `pending`, e `delivered_at`, registrado uma única vez quando o callback entrega resultado e evidências. Criação, atualizações, bloqueios e cancelamentos não podem fabricar ou sobrescrever esses marcos.

O contrato operacional canônico é `/api/internal/agent-tasks/<agentKey>/stage-executions/pending`. O backend libera somente atividades cujas predecessoras `TASK` da mesma versão de processo e referência de execução estejam concluídas e cujo recurso opcional corresponda ao executor solicitante. O executor reporta resultado ou falha pelos callbacks da execução; resultado e evidências ficam persistidos na tarefa. O executor nunca escolhe nem dispara a próxima atividade.

Toda tarefa que consumir modelo de IA deve persistir, no callback de resultado ou falha, o consumo
real informado pelo provedor: tokens totais de entrada, parcela da entrada atendida por cache e
tokens de saída. O executor informa modelo, tier e contadores; o backend é a autoridade do preço e
calcula o custo estimado em USD com o catálogo canônico vigente naquele instante. Como tokens de
cache fazem parte da entrada total, somente a parcela `entrada - cache` usa a tarifa de entrada
normal e a parcela em cache usa a tarifa própria. A tarefa preserva o custo calculado como histórico,
sem recalculá-lo retroativamente quando o catálogo mudar.

A ausência de preço para um modelo não pode apagar os tokens nem transformar falha de
instrumentação em sucesso econômico: a tarefa deve ficar com o consumo persistido, custo
indisponível e status explícito de preço ausente. Tarefas sem modelo podem registrar custo zero;
tarefas legadas que não reportaram consumo permanecem identificadas como não informadas. A tela da
tarefa deve mostrar entrada, saída, cache e custo estimado vindos do backend, sem estimativa local.

Uma tarefa excepcional pode ser vinculada posteriormente a uma atividade regular somente enquanto estiver `PENDING`, ainda não tiver sido recebida e a definição estiver `PUBLISHED`. O vínculo preserva o mesmo identificador e histórico da tarefa, remove a excepcionalidade e valida se a atividade pertence ao agente responsável. Tarefas recebidas, concluídas ou já vinculadas não podem ser migradas por esse contrato.

Uma demanda fora do catálogo pode ser registrada como `Atividade excepcional`, sem vínculo regular e com justificativa obrigatória auditável. A exceção não cria nem altera processo automaticamente; recorrências devem orientar revisão ou criação de processo. Tarefas históricas anteriores a esta regra permanecem legíveis como legadas.

A tela do experimento deve expor a instância do processo vinculada à referência da entidade. A situação
de cada atividade é calculada pelo backend com o mesmo grafo usado pelo endpoint `pending`, distinguindo
atividade liberada, atividade em execução, bloqueio por predecessora, falha e conclusão. Quando já existe
uma instância BPM para a entidade, tarefas sem vínculo de processo da mesma referência são apresentadas
como legado substituído e não competem visualmente com o trabalho canônico.
