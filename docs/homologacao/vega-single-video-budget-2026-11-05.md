# Vega — autorização incremental de um vídeo — 05/11/2026

## Evidência e limite da entrega

O pedido autoriza USD 15 para **uma única produção nova**, incluindo agentes,
revisões, imagens, clipes, voz, trilha, montagem, taxas e tentativas. Não autoriza
USD 91,46 históricos estimados, nem transforma o teto em USD 106,46.
Contexto consultado: produto 4, cadeia 27, ciclo 21, experimento 114, projeto 14,
perfil 69, versão `musa-pde-entry-v18-video-cinematico`.

O MCP confirmou cinco consultas isoladas (#42–46) e nenhuma tarefa ou job
vinculado a elas. O preflight #46 cotou somente clipes Gen-4.5: estimativa USD 3,84
e reserva máxima USD 4,80. A cotação venceu e não autoriza execução posterior.
Não existe vídeo entregue ou revisão de uma candidata audiovisual deste pedido.

A preparação atual registra `PREPARATION_CUMULATIVE` e a autorização audiovisual
registra `TWO_VIDEOS_PRODUCTION_AND_REVIEW`. Nenhuma expressa o escopo solicitado.
Notas livres no projeto não são recibo financeiro consumível ou prova de saldo.

## Alternativas

| Caminho | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Somar novo teto ao histórico | Reutiliza o formulário acumulado | Autoriza escopo recusado pelo cliente | Recusado |
| Usar contrato de duas peças | Reutiliza fluxo audiovisual da cadeia | Troca quantidade e cobertura da autorização | Recusado |
| Recibo incremental no ledger existente | Preserva origem e vínculo de uma produção | Exige suporte explícito de consulta e registro | Implementação local |

O recibo novo não cria tabela, projeto, perfil, experimento, tarefa ou gate.
Registra `AUTHORIZE_SINGLE_VIDEO` no ledger de decisões existente, com escopo
`SINGLE_VIDEO_INCREMENTAL_ALL_IN_V1`. Leitura e gravação respeitam tenant,
produto, cadeia, ciclo, experimento, versão, projeto e perfil. Replay não soma
teto. Outra chave para o mesmo projeto é recusada, inclusive após tentativa.
O evento administrativo não substitui a última transição funcional da cadeia.
O snapshot existente de Plutus recebe a mesma referência e a origem do recibo,
com instrução explícita de custo integral e sem apresentar o valor como saldo.

**Este recurso registra a autorização; não implementa a produção inteira.**
Não transforma um recibo em parecer de Plutus, reserva, cobertura dos custos
futuros, roteiro aprovado, prova homologada ou aceite da candidata. A resposta
`startsProduction=false` descreve o efeito do comando. A continuidade paga
permanece dependente dos contratos e das validações operacionais do fluxo.

## Matriz local definida antes dos testes

| Dimensão | Aceite |
| --- | --- |
| Caminho feliz | Tela → controller → service → evento → leitura preserva USD 15 e a origem |
| Isolamento | Outro produto, experimento, versão, perfil ou tenant não recebe o recibo |
| Histórico | Preparação acumulada e duas peças não são lidas nem alteradas pelo comando |
| Repetição | Replay preserva evento e revisão; outra chave não renova teto do mesmo projeto |
| Validações | Confirmação, revisão atual, valor positivo, contexto exato e origem obrigatórios |
| Falhas | Erro permanece visível; repetição sem edição mantém chave idempotente |
| Integração | O evento não avança BPM, abre tarefa, reserva ou job |
| Interface | Desktop e Chromium emulado como iPhone/Pixel; formulário e recibo legíveis |
| Observabilidade | Evento, referência, operador real, origem, contexto, valor, horário e escopo |
| Métricas | Testes sintéticos separados; nenhum teste prova conversão ou venda |
| Vídeo solicitado | MP4, áudio, legendas, prévia e revisão por hash permanecem pendentes |

## Aprendizado reutilizável

A lacuna observada é a confusão entre teto acumulado e autorização incremental
integral. Hipótese: preservar explicitamente a unidade de autorização reduz
repedidos de aprovação e duplicação de orçamento. O comportamento a validar é
registro exato, isolamento e idempotência; impacto comercial não foi medido.
Adotar somente com os testes aprovados e revisão do PR; reverter se o comando
mudar histórico, aceitar outro contexto ou iniciar consumo. Publicação depende
da confirmação específica exigida nesta solicitação. Não há execução automática
prometida após encerrar a conversa.

## Resultado observado na sandbox

- 109 testes dos contratos financeiros, projeção de atividade, tenant e ciclo
  de Apolo/Plutus passaram, incluindo 16 casos do novo recibo.
- 92 testes de arquitetura passaram.
- 11 testes de interface passaram. A simulação de resposta de outro contexto
  foi corrigida para preservar o contrato de lista do endpoint de projetos.
- Seis cenários de navegador passaram: dois produtos sintéticos em desktop,
  iPhone 15 Pro e Pixel 7, incluindo erro 503, replay idêntico, confirmação e
  recibo. Nenhuma chamada paga ou chamada a produção nesses testes.
- Build do frontend concluído; somente avisos existentes de tamanho dos chunks.
- O teste da projeção precisou incluir o novo evento administrativo na lista
  excluída das transições; o bloqueio anterior continua visível após o recibo.

No projeto real #14, pelo frontend, o campo Legendas foi corrigido para oito
blocos de fala separados por `|`. As instruções visuais anteriores foram
preservadas nas notas de montagem. O perfil continua #69, projeto DRAFT e roteiro
sem aprovação. Essa preparação evita narrar instruções editoriais, mas não
representa validação de Íris, voz gerada ou vídeo pronto.

Nova consulta MCP em 05/11 confirmou zero jobs do perfil #69, zero tarefas com
referência `experiment:114`, zero lançamentos no ledger desse experimento e zero
reservas dos ciclos do projeto #14. O teto novo permanece sem consumo registrado;
nenhuma conciliação do histórico foi alterada. A autorização nova ainda precisa
ser registrada em produção após publicação autorizada da funcionalidade.

## Correções confirmadas na conferência do PR

A primeira execução de CI encontrou dois contratos de teste que precisavam
acompanhar a nova dependência: a fixture local não importava o registrador e o
manifesto imutável de Mira ainda fixava o hash anterior do serviço compartilhado.
A fixture passou a importar o serviço real; uma sucessora de compatibilidade
preserva os pareceres e a experiência anterior, sem autorizar outra publicação.
Os três testes de composição da fixture passaram. A compatibilidade de Mira foi
verificada separadamente, sem alterar o manifesto anterior.

Também foi demonstrado que a consulta de Íris selecionava o último ciclo de
vídeo mesmo quando ele era apenas um preflight isolado. A consulta agora exclui
somente os três estados de preflight antes de selecionar o pedido mais recente.
Um pedido real anterior continua visível; STOP/CANCELLED mais recentes continuam
impedindo a produção. Foram aprovados 22 testes do contexto e 13 da consulta,
incluindo outra identidade, histórico válido, preflight posterior e parada.
Esse ajuste melhora o contexto executável de Íris, sem iniciar tarefa ou
confundir estimativa com autorização. A hipótese é reduzir pedidos e retrabalho
indevidos; não há resultado comercial medido.

A primeira tentativa local com MySQL 5.7 falhou com `ENOSPC` ao criar `ibdata1`.
Uma topologia temporária com dados em memória permitiu repetir a validação sem
mexer em produção. A aplicação local iniciou com a dependência corrigida, e o
roteiro existente de decisão e passagem entre processos passou para dois
produtos sintéticos, incluindo rollback, replay, concorrência e pausa. O banco
era MySQL 5.7 real; os executores externos permaneceram simulados e sem gasto.
