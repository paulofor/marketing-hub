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

Para Produtos Digitais Experienciais, a organização macro dos processos deve seguir
`docs/canonical/cadeia-produtos-pde-canon.v1.md`. Processos especializados já publicados, como
fabricação de entregáveis, criativos, landing, homologação e venda, são reutilizados dentro dessa
cadeia e não devem ser confundidos isoladamente com o ciclo completo de criação e venda de um PDE.

Processos podem ser organizados em cadeias de criação e entrega de valor. A cadeia é versionada,
preserva as versões exatas dos processos participantes e possui objetivo, resultado e métrica
principal próprios. Ela serve à visão gerencial e não executa, agenda ou avança etapas. A tela
canônica é `/business-process-chains` e seu contrato de leitura é
`/api/business-process-chains`.

## Governança BPM

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
- O diagrama é persistido como grafo estruturado, não como imagem ou XML livre.
- Todo grafo precisa de exatamente um evento inicial, um final e fluxos entre elementos existentes.
- A tela renderiza o grafo persistido pelo backend e não infere status ou regra de negócio.
- Publicar uma definição não publica landing, campanha, oferta ou conteúdo e não autoriza gasto.

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
