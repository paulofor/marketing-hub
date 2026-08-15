# Catálogo canônico de processos do Marketing Hub — v1

## Decisão

O Marketing Hub mantém um cadastro próprio e versionado de processos de negócio. Esse catálogo é a
fonte de verdade para propósito, responsáveis, eventos, atividades, gates, entradas, saídas e relação
com contratos técnicos. A tela canônica é `/business-processes` e a API é
`/api/business-processes`.

Processos de negócio e pipelines não são sinônimos. O processo explica e governa o trabalho ponta a
ponta; pipelines e workers executam contratos técnicos referenciados pelo processo. O catálogo não
faz polling, não consome filas e não avança execuções operacionais.

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

## Primeiro processo: Geração de landing page

A versão 1 formaliza o ciclo:

`briefing → Dédalo → validação técnica → Psique → Têmis → aprovação humana`.

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

O contrato operacional canônico é `/api/internal/agent-tasks/<agentKey>/stage-executions/pending`. O backend libera somente atividades cujas predecessoras `TASK` da mesma versão de processo e referência de execução estejam concluídas. O executor reporta resultado ou falha pelos callbacks da execução; resultado e evidências ficam persistidos na tarefa. O executor nunca escolhe nem dispara a próxima atividade.

Uma demanda fora do catálogo pode ser registrada como `Atividade excepcional`, sem vínculo regular e com justificativa obrigatória auditável. A exceção não cria nem altera processo automaticamente; recorrências devem orientar revisão ou criação de processo. Tarefas históricas anteriores a esta regra permanecem legíveis como legadas.
