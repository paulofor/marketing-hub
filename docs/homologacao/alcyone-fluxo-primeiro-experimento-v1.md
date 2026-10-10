# Alcyone — primeiro planejamento e fila — 10/10/2026

## Estado e causa

O PR #5567 está integrado em d1fbe7a90c2e57142eeb7e7c399e4381b2855866.
O preflight financeiro passou, Plutus #77 aprovou a projeção privada e Atena #20
identificou a revisão independente da integração como dependência. Preservar esses
pareceres; projeção não comprova demanda nem libera cobrança ou aquisição.

Produto 11, plano 34/v4, experimento 97 PLANNED, sem ciclo. Comunicação #44
aguarda predecessores. Ao solicitar pela tela o planejamento nativo #67, o backend
retornou QUEUED e queueBlocker #44: a dependência fica impedida pela própria espera.
O histórico e as regressões mostram que a continuação do Processo 6 para o Processo 5
já possui exceção segura de precedência, sem tarefa em curso; faltava reconhecer a
preparação do primeiro experimento no Processo 2.

O contexto nativo ainda entrega somente os campos antigos do plano comercial.
A revisão LIVE vigente e a prova integrada precisam acompanhar o planejamento,
para que as novas decisões não dependam de copiar pareceres entre telas.

## Alternativas

| Caminho                                                         | Benefício                         | Risco/esforço                                       | Escolha                        |
| --------------------------------------------------------------- | --------------------------------- | --------------------------------------------------- | ------------------------------ |
| Pausar comunicação e retomar manualmente                        | Destrava o caso                   | Mantém repasse e recorrência                        | Não como solução compartilhada |
| Liberar qualquer processo em espera                             | Remove a fila                     | Atravessa pausas, gastos e identidades              | Rejeitado                      |
| Reconhecer a dependência inicial exata e entregar fontes atuais | Reutiliza coordenador e contratos | Validação de identidade, estado e trabalho em curso | Adotado                        |

A exceção limita-se à comunicação WAITING_INPUT, sem falha, aguardando o
planejamento do mesmo produto/cadeia/experimento inicial PLANNED, sem ciclo ou
predecessor. O coordenador continua impedindo ultrapassagem de tarefas e filhos
em curso. Nenhuma pausa é removida, resultado aprovado ou gasto autorizado por
essa precedência. A comunicação só continua quando seus próprios gates aceitarem
a nova entrada. A referência do primeiro experimento é preservada; não criar ciclo
ou copiar aprovação para solucionar uma fila.

## Matriz definida antes da validação

| Área                          | Aceite                                                                                                             |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| Caso original e outro produto | Planejamento recebe vez; comunicação e custos ficam preservados                                                    |
| Antes válido                  | Continuação de preparação comercial do ciclo mantém comportamento                                                  |
| Proteções                     | Outra identidade, ciclo, sucessor, experimento em operação, pausa, falha e tarefa em curso conservam reserva       |
| Persistência e HTTP           | Consulta expõe fila sem escrever, aprovar, pausar ou duplicar tarefas                                              |
| Passagem ponta a ponta        | MySQL e coordenador reais; agentes simulados concluem dependência e permitem reavaliar comunicação                 |
| Contexto                      | Revisão LIVE atual do produto/plano/versão e prova do mesmo experimento; TEST, stale e origem divergente excluídos |
| Custo e autoridade            | Ausência permanece desconhecida; projeção e geração não são aprovação comercial                                    |
| Dispositivos                  | Chromium desktop, iPhone e Pixel no harness existente                                                              |
| Publicação                    | Testes locais e diff primeiro; PR/revisão/merge e workflows/saúde depois                                           |

Fixtures têm identidades sintéticas 92049–92053/95111–95231. Provedores e agentes
são simulados, sem credenciais, consumo real ou dados comerciais de teste.

## Aprendizado

Capacidade: coordenação de dependências antes de Íris e contexto de planejamento.
Hipótese: reconhecer a precedência inicial e transportar as fontes canônicas reduz
tempo parado e repasses, mantendo gates independentes. Critério local: ausência de
fila circular e de contaminação de contexto, com reserva preservada nos negativos.
Adoção condicionada à matriz; reversão se houver ultrapassagem de trabalho real ou
uso de fonte financeira de outra identidade. Testes não comprovam aumento de vendas.

## Resultados locais

- Backend completo: 4.544 testes, zero falhas/erros; 37 condicionais de outras
  topologias ignorados. A fila desta mudança foi validada com MySQL 5.7 real.

- A regra anterior falhou nos dois casos positivos novos; a regra corrigida passou
  com outra identidade, casos negativos e caminhos comerciais já existentes.
- MySQL 5.7 e HTTP: 21 cenários de API e 22 de lifecycle passaram. A passagem
  planejada → comunicação conservou identidade, custos desconhecidos e gates.
- Navegação: passagem completa em Chromium desktop e iPhone/Pixel emulados;
  reserva por decisão humana antes válida permaneceu visível e protegida.
- Reinício: dez processos sintéticos dos cinco produtos preservaram conclusão e
  referência sem ciclo, além das regressões existentes de tarefa ativa e histórico.
- Worker de processos: seis testes; runner: quatro; contrato de entrega: cinco;
  atestações comerciais: 16 testes/104 manifestos/497 arquivos sem divergência.
- `bash -n`, ShellCheck, formatação e diff passaram. Topologia Compose, volumes e
  processos locais foram removidos após as evidências. Não houve inferência real
  nessa matriz; emulações Chromium não equivalem a aparelhos físicos ou Safari.

O pacote conserva as 4.311 classes testadas, 765 recursos externos e catálogo com
527 cartões. A revisão do diff e dos comentários de responsabilidade passou.
A retomada publicada será acrescentada nas evidências do PR. A preparação real inclui Plutus #77 (APPROVE privado, USD
0,1290348) e Atena #20 (revisão ainda pendente, USD 0,2734852). Total conhecido desta
preparação: USD 1,214161, incluindo USD 0,811641 anteriores. Esses valores são
estimativas, não faturamento reconciliado. Mídia e vídeo pago seguem sem autorização.

## Correção da consulta financeira no claim — 10/10/2026

O segundo PR está integrado em ce4e6e4e3bce64e058adfbf78f4e5e46fcadc582.
Os workflows passaram, e a execução #67 passou da fila para Atena #706.
A conferência posterior mostrou HTTP 500 no endpoint `pending`, antes da inferência.
O stack trace termina em `FinancialPlanService.required`/`findById`: a integração
acrescentada pelo PR chamava `get(..., null)`, embora esse contrato leia uma revisão
exata. Os testes anteriores simulavam esse método e não verificavam sua exigência
real. A reserva transacional foi desfeita; #706 permaneceu PENDING, sem resultado
ou novo custo informado. Não criar outra tarefa nem reexecutar o planejamento.

| Alternativa                                                             | Benefício                                   | Risco/esforço                                                | Decisão     |
| ----------------------------------------------------------------------- | ------------------------------------------- | ------------------------------------------------------------ | ----------- |
| Listar todo o histórico e filtrar em memória                            | Reutiliza consulta pública                  | Mais leitura; seleção menos explícita                        | Não adotada |
| Fazer `get` aceitar ID vazio como revisão atual                         | Corrige a chamada                           | Muda contrato de revisão exata e pode substituir referências | Rejeitada   |
| Selecionar a última revisão do mesmo proprietário/ambiente/plano no SQL | Mantém identidade e leitura exata existente | Query restrita e teste da fronteira real                     | Adotada     |

A regressão adicional usa o endpoint nativo completo e o serviço financeiro/JPA
reais no MySQL 5.7 do harness existente. Somente a fila, o catálogo de referências
e os provedores são sintéticos. Cobrir dois produtos, revisão LIVE versus TEST,
revisão comercial obsoleta, recuperação da lease e preservação do custo desconhecido.
O teste que simulava `get` não é evidência suficiente dessa fronteira.

Capacidade aprimorada: reserva da tarefa com fontes financeiras canônicas.
Critério de adoção: resposta HTTP 200 com revisão correta e sem inferência,
segregação preservada e consulta de revisão explícita antes válida inalterada.
Reverter se houver troca silenciosa de revisão ou contaminação de identidade.
Essa correção não comprova demanda, venda ou margem realizada.

### Resultado da regressão adicional

- A chamada anterior foi recompilada e reproduziu HTTP 500 no claim real. O stack
  local confirmou `findById` sem ID, `FinancialPlanService.required` e o provider.
- A versão corrigida passou nos 4.548 testes do backend: zero falhas/erros,
  37 condicionais de outras topologias ignorados. Os 44 testes focados passaram.
- MySQL 5.7: 65 consultas/comandos da matriz financeira e 22 do claim nativo
  passaram, com dois produtos, TEST mais recente que LIVE, lease preservada,
  exclusão de fonte obsoleta e custo ausente `NOT_REPORTED`. Nenhum modelo chamado.
- Reinício preservou histórico financeiro, parecer e custo; apply/reapply,
  rollback/restore e validação estática Liquibase passaram. Não há novo changelog.
- Diff, formatação e sintaxe do runner passaram; a topologia e os volumes foram
  removidos. Esta rodada foi diagnóstico de persistência/claim: não repete nem
  substitui as provas de interface e coordenação completas do PR anterior.

A entrega só permite recuperar a mesma tarefa #706 pelo executor. O aceite real
de Atena, dos demais predecessores e de Íris ainda precisa ser observado após
publicação, mantendo limites financeiros, revisões e decisões humanas.

## Prova técnica e planejamento atual — 10/10/2026

Após a correção do claim, Atena #706, Plutus #707 e Dédalo #708 terminaram com
APPROVE no planejamento #67. A comunicação #44 continuou aguardando versão e
capturas. O log mostrou a divergência de `marketStrategy` na consulta privada:
a descoberta #491 conserva MARKET_STRATEGY_V3; a projeção atual contém V4.
O gate técnico e os hashes tinham passado antes dessa comparação. Preservar a
origem e os pareceres próprios; não atribuir aprovação nova à descoberta antiga.

| Alternativa                                             | Benefício                           | Risco/esforço                                                    | Escolha   |
| ------------------------------------------------------- | ----------------------------------- | ---------------------------------------------------------------- | --------- |
| Reescrever ou repetir a descoberta antiga               | Alinha os textos                    | Altera história e exige nova inferência sem corrigir a fronteira | Rejeitada |
| Remover a comparação de origem de todo contexto privado | Destrava a consulta                 | Enfraquece o contrato completo antes válido                      | Rejeitada |
| Separar prova do software aceito e planejamento atual   | Reutiliza gate e revisões imutáveis | Exige escopo explícito e pareceres próprios no consumidor        | Adotada   |

O leitor parcial declara PRIVATE_SOFTWARE_VERSION_ONLY e não entrega estratégia,
economia ou arquitetura de origem como aprovação atual. O consumidor do primeiro
experimento exige Atena, Plutus e Dédalo do planejamento corrente. O contexto
privado completo mantém suas verificações anteriores. Integração nova, preços,
checkout, mídia e prontidão comercial conservam seus próprios gates.

### Matriz antes da rodada

| Área                            | Aceite                                                                                                         |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Antes/depois e outra identidade | Duas identidades sintéticas permitem a passagem sem reescrever V3 antiga                                       |
| Histórico antes válido          | Contexto completo V3 continua íntegro; divergência de origem permanece bloqueada                               |
| Integridade técnica             | Gate, status, identidade, hashes e revisões posteriores continuam obrigatórios nos dois leitores               |
| Planejamento atual              | Prova técnica não substitui parecer ausente/reprovado de Plutus ou Dédalo                                      |
| Fronteira real                  | Controller e serviço de tarefa recebem os dois provedores produtivos; executor valida entrada exportada        |
| Observabilidade e custos        | Contexto explicita escopo, ausência não vira custo zero, consultas não escrevem nem repetem inferência         |
| Interface                       | Reutilizar navegação completa já aprovada; conferir passagem publicada em desktop e celular sem alterar layout |

Capacidade aprimorada: encaminhar prova da versão utilizável sem confundi-la com
estratégia ou demanda. Hipótese verificável: remover essa confusão reduz espera
antes de Íris. Adotar se a matriz preservar os negativos; reverter se parecer ou
versão divergente receber aprovação implícita. Impacto em vendas depende do
experimento real, ainda não iniciado. O novo fluxo de personalização continua
pendente de revisão independente; a prova histórica não o aprova retroativamente.


### Resultado da separação de prova

- Antes: os dois casos sintéticos falharam com o consumidor anterior; zero erros
  de preparação do teste. Depois: backend completo com 4.551 testes, zero falhas
  e erros, 37 condicionais de outras topologias ignorados.
- Controller HTTP e serviço de tarefa reais transportaram a estratégia V4 atual,
  prova técnica com escopo explícito e a mesma versão. Os repositories do cenário
  são doubles; não houve escrita, cobrança ou inferência nesse ensaio.
- Executor: 79 testes, zero falhas/erros, dois condicionais de outras provas
  ignorados; a integração exportada do primeiro planejamento foi executada.
- Atestações: 16 testes e pacote de 104 manifestos/497 arquivos íntegros. O teste
  de ciclo já vinculado aos manifestos foi preservado; a regressão do primeiro
  planejamento é própria, sem substituir homologações de outros produtos.
- O planejamento real #67 terminou 3/3. Consumo conhecido da preparação:
  USD 2,130497 de estimativas, incluindo as etapas anteriores; teto USD 10.
  Não há autorização audiovisual ou de mídia. A comunicação ainda depende da
  publicação desta correção e de seus próprios pareceres e seleção humana.

O pacote final passou: 4.312 classes idênticas às compiladas, 765 recursos íntegros
e catálogo executável com 527 cartões. Diff e comentários de responsabilidade
foram revisados. Nenhum changelog, autorização ou execução histórica foi alterado.


### Destino preservado antes do merge

A conferência do consumidor visual real identificou que a nova projeção técnica
precisa preservar PRIVATE_PDE_DESTINATION_V1, tipo e indicação de não gerar
landing, além de URL e versão. Um helper compartilhado conserva exatamente o
contrato anterior nos leitores completo e parcial. A regressão de duas identidades
agora passa pelo FrozenCreativeVisualAuthorization, antes da leitura HTTP.

A projeção incompleta falhou nos dois casos, sem erros do ensaio. O ajuste passou
nos 109 testes relacionados a contexto, capturas, callback, prontidão e formatos;
a entrada atualizada foi validada novamente no executor. Não repetir a matriz
inteira após essa projeção: a rodada completa de 4.551 precede o ajuste, e as
regressões afetadas foram repetidas. CI do HEAD final continuará sendo obrigatório.
As nove capturas persistidas de #583 conservam URL de origem e hashes válidos;
enhum arquivo, parecer ou resultado histórico foi substituído.
