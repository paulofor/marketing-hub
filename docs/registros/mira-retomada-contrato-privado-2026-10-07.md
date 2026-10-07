# Mira — retomada com contrato privado atualizado — 07/10/2026

## Evidência e causa confirmadas

Publicação 1911f5d5260d22f3b9098f90e5a19433fab934f0 confirmada: 14 workflows
bem-sucedidos, backend/frontend/vídeo e nove containers de agentes conferidos. A
retomada do pai 54 pela tela preservou a mensagem 634, mas o filho 55 bloqueou antes
de inferência. Eventos 1619/1621 registram rota e tentativa 635 antigas; 1632 mostra
NO_PROGRESS na rota após a declaração privada mudar. A tela já expunha essa rota
como disponível, sem objetivo comprovado.

ProcessRunContext.inputVersion usa somente versão do ciclo e definição do produto.
A chave de deduplicação não via a mudança comprovada em PDE_PRIVATE_CREATIVE_PREPARATION_V1.
O teste anterior incrementava retryEpoch ao retomar diretamente o filho, encobrindo
a diferença em relação à retomada do pai. O orçamento continua US$8,5320024 estimados
sob teto TOTAL US$10; nenhum task novo foi criado. Pai pausado pela tela para validar
a correção antes de permitir inferência.

| Alternativa | Benefício | Risco/esforço | Decisão |
|---|---|---|---|
| Retomar cada filho manualmente | Recupera a ocorrência | Depende de repasse e não registra a mudança semântica de entrada | Não previne a classe |
| Desabilitar deduplicação ou incrementar todos os filhos | Remove o bloqueio | Pode repetir inferência sem mudança e gastar novamente | Rejeitada |
| Usar o contrato do provedor existente na versão de entrada criativa | Reconhece mudança real e preserva chaves anteriores | Ajuste proporcional no contrato já usado pelo backend | Adotada |

## Matriz local antes da implementação

- Reproduzir rota concluída e produção bloqueada com eventos reais persistidos;
  mudar a declaração sem mudar produto/ciclo e sem retomar o filho explicitamente.
- Provar que declaração nova permite uma rota e uma produção novas, mantendo a
  mensagem e a tentativa anteriores, sem repetir o comando em ticks seguintes.
- Conferir outro produto/referência, ordenação/números equivalentes e pedido governado.
- Preservar bloqueio de entradas iguais, legado válido, mensagem já concluída,
  STOP/pausa/ciclo fechado, autorização e decisão humana de uso.
- Exercer provedor, contexto de execução, controller, diário e persistência reais;
  substituir apenas dependências externas e agentes por doubles, sem cobrança.
- Executar regressões relacionadas de comunicação/automação, recursos do JAR,
  empacotamento/evidências, comentários Java e checks de entrega antes de publicação.

A revisão da entrada será restrita ao subprocesso criativo; artefatos produzidos,
custos e consultas não se tornarão uma nova entrada. Não altera a versão da mensagem,
escolha de formato comercial, autorização de mídia ou limite de IA.

## Implementação e resultados locais

- `CommunicationMaterializationContextProvider` conserva sua interface funcional e
  acrescenta revisão opcional. Íris fornece hash canônico somente da declaração
  privada válida; o contexto genérico incorpora essa revisão exclusivamente onde
  o provedor a oferece. Não há nova infraestrutura, endpoint ou consulta por nome/ID.
- `ProcessRunService` resolve a entrada uma vez por disparo e registra seu hash no
  diário existente. A mensagem conserva seu hash anterior e não é reexecutada.
- Regressão controller/H2 falhou antes do ajuste: esperado WAITING_ACTIVITY, observado
  BLOCKED. Após a correção, uma rota e uma produção novas, retryEpoch do filho zero,
  tarefa anterior preservada, dois hashes de entrada diferentes e nenhuma duplicação
  em ticks repetidos. Psique/Têmis precedem o gate humano; vídeo em briefing não é gerado.
- 447 testes de comunicação, automação e arquitetura sem falha; 14 casos de revisão
  de entrada e cinco de persistência, incluindo o caso novo. Um replay legado depende
  de fixture exportada (`PRIVATE_JOURNEY_REPLAY`), ausente nesta execução; não é gate
  desta retomada, reproduzida integralmente pelo controller e H2 com os contratos reais.
- 16 testes do pacote de evidências e nove de recursos aprovados. Pacote com 414
  arquivos/77 manifestos. JAR recompilado; classes e recursos conferidos contra os
  bytes atuais, catálogo carregado. Spotless limitado aos sete arquivos Java alterados,
  responsabilidade de classes/métodos em português e diff revisados.
- Root 54 PAUSED e filho WAITING_PARENT confirmados na API antes da publicação.
  Candidata V3, ciclo 9/experimento 102, provas anteriores e teto cumulativo preservados.

## Limites

O hash não libera contrato indisponível, revisão independente, decisão humana,
publicação comercial ou gasto. Mudança de declaração permite reparar a tentativa
sem presumir sucesso da peça. A entrega de código deverá ser comprovada pelo PR,
workflows, revisão publicada e saúde antes de retomar o mesmo processo pela tela.
Resultados comerciais permanecem desconhecidos: não há nova prova de venda ou lucro.
