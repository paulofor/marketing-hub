# Retorno causal das atividades — 30/10/2026

## Evidência e decisão

Capella, ciclo 19, experimento 112, processo 126: Dédalo 884 e 885 bloquearam
a mesma divergência. O catálogo não descrevia a entrada `specialtyCode` já existente;
a fotografia da Big Idea resolvia o alvo sem o contexto do processo. Retomar alterava
a tentativa, sem corrigir essas entradas. Os custos conhecidos das duas tarefas foram
US$ 2,666756 e US$ 2,907136 estimados, sem conciliação de fatura.

Alternativas avaliadas: inferir destino de texto livre (ambíguo), reiniciar todo o
processo (repete custos e pareceres) ou retorno estruturado validado pelo BPM.
Escolhida a terceira, reaproveitando prontidão, fila, contexto de correção e auditoria.
Para o caso histórico, o descritor correto permite ao validador existente reabrir Atena.

## Matriz definida antes dos testes

| Caso | Aceite |
|---|---|
| Capella e outra identidade | Entrada do catálogo corresponde a record, validação e UI; Atena → Plutus → Dédalo. |
| Rejeição de predecessor | Origem correta reaberta, motivo e critério visíveis, dependentes aguardam. |
| Retomar sem mudança | Nenhuma nova tarefa paga, mesmo após retomar ou comando direto. |
| Correção aprovada | Saída funcional diferente permite continuar na ordem causal. |
| Correção sem progresso | Nova tarefa ou justificativa não libera revisão repetida. |
| Retornos sucessivos | Máximo de dois retornos por par/ciclo; terceiro bloqueia com causa. |
| Contrato inválido | Destino próprio, posterior, humano ou inexistente não é executado. |
| Isolamento e recuperação | Ciclo fechado, produto distinto, execução ativa e respostas históricas preservados. |
| Big Idea | Nova tarefa fixa a revisão no contexto correto; replay conserva a fotografia. |
| Interface | Desktop, iPhone e Pixel mostram a origem correta e o impedimento; orçamento continua acessível. |
| Comercial e observabilidade | Dados sintéticos isolados; sem mídia/IA externa nos testes; tarefas e custos preservados. |

## Resultados

Validação local: 347 testes de backend aprovados, mais 17 verificações do catálogo do harness, incluindo serviço real de atividades,
persistência do motor em H2, projeção do card bloqueado, isolamento, recuperação e
contexto da Big Idea. Dédalo: 112 testes da suíte aprovados; o teste opcional de replay foi executado
separadamente com o resultado original da tarefa 885, sem repetir a inferência. Desktop, iPhone 15 Pro e Pixel 7
em Chromium passaram, usando o JSON produzido pelo serviço real e dependências locais
simuladas; nenhuma chamada de execução ou de provedor foi feita pelo navegador.

Reprodução: testes `PdeImplementedInputPlanningHandoffTest` com a propriedade
`upstream.fixture-output=../../artifacts/flow-recovery/local-history.json`, frontend
local em 5177 e `node scripts/test-upstream-correction-browser.cjs`. As demais
regressões estão em `UpstreamCorrectionReadinessProviderTest`,
`ProcessRunRecoveredCostTest`, `LearningCycleImplementedInputContextTest`,
`BigIdeaTaskContextTest` e `PdeUpstreamCorrectionContractTest`.

O contrato de Dédalo é publicado como v7, preservando prompt/schema v6 vinculados
a atestações históricas. O CI detectou a dependência ao verificar hashes de Mira;
a compatibilidade foi reproduzida e corrigida localmente antes de atualizar o PR.
As novas atestações de compatibilidade de Mira (v66) e Vega (v9) registram somente
as provas compartilhadas revalidadas; não renovam pareceres nem autorizações. O
pacote de revisão reuniu 770 arquivos e 193 manifestos íntegros.
O schema novo usa objeto anulável compatível com o verificador estrito do executor;
o replay de resultados antigos continua sem exigir campo inexistente na geração original.
A adoção inicial do retorno estruturado é o planejamento de Dédalo; o mecanismo de
prontidão é reutilizável por contratos ADJUST/APPROVE que declarem o mesmo retorno.
Outros contratos não são reinterpretados por heurística de texto.

Critério de adoção: origem correta, saída funcional alterada, sem tarefa duplicada,
com STOP/pausa e aceites mantidos. Critério de revisão: retorno indevido, perda de
histórico ou nova tarefa sem mudança. Medir recorrência do bloqueio, tempo parado e
custo por passagem aceita; os dois bloqueios originais somam US$ 5,573892 estimados.
Não há redução comercial medida nem aumento de vendas comprovado.

A consulta financeira de 30/10 registra US$ 64,289364 conhecidos estimados e três
tarefas sem custo, com ausência de autorização estruturada própria no ciclo 19.
A primeira validação não exercitou o polling de um processo `BLOCKED` contra
o financeiro real. Na publicação, a origem foi corretamente reaberta, mas o
reconciliador iniciou Atena #886 sem conferir o teto anterior de US$ 50. A execução
#95 foi pausada pela UI, sem cancelar a tarefa iniciada. Esse consumo não recebeu
autorização retroativa; seu resultado e custo precisam permanecer auditados.
A demonstração está em `/financial/execution?productId=7&chainId=27&cycleId=19`.
Histórico financeiro e novas decisões pertencem ao fluxo administrativo existente.

## Complemento de homologação — 31/10/2026

Entre congelar todos os bloqueios, interpretar limites em notas livres e consultar
o recibo financeiro existente antes do retorno, foi escolhida a terceira opção.
Ela preserva continuação automática autorizada e pausa apenas o consumo sem cobertura.

| Caso adicional | Aceite |
|---|---|
| Bloqueado observado pelo polling | Origem reconhecida, mas nenhum novo trabalho sem recibo. |
| Retomar repetido sem limite | Pausa persistida, diário com causa e zero tarefas adicionais. |
| Custo acumulado acima do teto | Pausa antes da criação de tarefa. |
| Reserva e custo anterior da atividade | Saldo disponível deve cobrir a referência histórica; sem garantir custo futuro. |
| Recibo de outro produto/ciclo ou custo totalmente desconhecido | Nenhuma autorização presumida. |
| Recibo suficiente e retomada explícita | Uma tarefa, com deduplicação e histórico preservados. |
| Tarefa já iniciada e callback | Receber o resultado; impedir a próxima atividade durante a pausa. |

A proteção limita novas tarefas de correção/revalidação em ciclos de aprendizado;
não é um mecanismo de faturamento do provedor nem uma migração de autorizações antigas.

### Falso bloqueio e recuperação da resposta

Atena 886 encerrou com decisão `APPROVE`, mas o validador recusou o texto
“uma matriz de nove combinações, sem REFERENCE/REDUCED” como se exigisse a comparação.
Foi escolhida uma exclusão explícita e restrita ao fim da cláusula, em vez de retirar
esse controle ou interpretar livremente qualquer negação. As regressões preservam
bloqueios para obrigação em outra frase/campo, contraste e “não homologar sem”.
Backend e executor aplicam a mesma regra, mantendo a capacidade ligada à candidata.

A resposta e o transporte originais de 886 foram revalidados localmente pelo
validador completo do executor. O replay usa os endpoints oficiais de estado e
callback, sem enviar consumo novamente, conservando resposta, evidência, prompt,
modelo e custo. A execução permanece pausada durante essa recuperação. O custo
registrado de 886 é US$ 3,087526 estimados; não é autorização nem cobrança conciliada.

Resultado da rodada complementar: 367 testes relevantes de backend e 100 testes
da suíte completa de Atena aprovados, incluindo replay local da resposta 886. Mais 152 testes do serviço de tarefas passaram. A
regressão do callback também reproduz outra identidade e exige custo e tokens
inalterados após reaplicação e repetição idempotente. A prova compartilhada de Mira recebe atestação v67 de compatibilidade;
a atestação de Vega permanece compatível e preservada.
nenhum parecer comercial é renovado.
