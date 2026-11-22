# Continuidade parcial — Vega/MUSA — 22/11/2026

## Evidência e decisão

Base: PR 5864 integrado em f6e2705eadc3cbb0bf356370cddd4711b22207c4.
GET consultivo confirmou produto 4, cadeia 27, ciclo 22, experiment:115, processo 117 → 127,
execução 102 e nenhuma sucessora. MCP db_query confirmou 102 PAUSED. Jornada 859 e componentes
860 expõem metadados, não comprovação dos novos critérios. Nenhuma escrita no produto real.

Alternativas: exigir declarações (mantém o bloqueio e pode fabricar aceite), reconstruir todas
as entregas (perde trabalho e aumenta consumo), preservar artefatos com revalidação seletiva
(escolhida). O recibo separa reuso de aceite e pendências. O executor recebe histórico separado
dos resultados aceitos. A comparação textual é conservadora: não presume equivalência de um
critério reescrito. Provas novas passam pelos responsáveis e gates existentes.

## Matriz local

- Caminho válido: oito contratos idênticos, oito aceites referenciais, origem intacta.
- Reuso parcial: sete contratos idênticos e um alterado; sete aceites e uma pendência.
- Caso com oito alterações: nenhuma aprovação automática; oito artefatos preservados.
- Texto genérico: rejeitado antes de criar execução. Ausência de prova: não herda aceite.
- Estrutura incompatível, contexto encerrado, referência errada e snapshot concorrente: rejeição.
- Repetição/recarga: uma sucessora; recibo persistido e rollback transacional.
- Financeiro: PAUSED e PAUSE_CONTEXT mantidos; produto/ciclo não gravados, sem tarefas nem
  integração paga na preparação. Dispensa 140, dez custos desconhecidos, teto US$200 e reserva
  US$0 não são alterados. Saldo não é recalculado como se custos desconhecidos fossem zero.
- Agentes/gates: tarefas pendentes não entram em reusedTasks; contexto histórico do destino
  disponível somente na referência vinculada, com resultados originais para revalidação.
- Navegadores: Chromium desktop, iPhone 15 Pro e Pixel 7; falha recuperável, recarga e layout.

## Operação

Na [atividade histórica](http://191.252.181.168:5173/products/4/value-chain-history/processes/117/activities?chainId=27&learningCycleId=22&sourceReference=experiment%3A115), abrir
**Continuar no processo publicado**, conferir **Critérios e pendências**, informar responsável
pela conferência e motivo, então **Preparar continuidade sem executar**. O destino nasce pausado.
**Abrir continuidade** conserva cadeia/ciclo/experimento. Nenhuma compatibilidade ou retomada
paga deve ser aplicada automaticamente nesta entrega técnica.

## Limite do aprendizado

Hipótese: reduzir declarações indevidas e reconstrução desnecessária. Métrica local: aceites sem
prova no destino = zero, artefatos históricos preservados, nenhuma chamada paga de preparação.
Não há medição comercial nem alegação de aumento de vendas. Adoção exige testes e publicação;
reversão preserva os recibos e mantém a pausa.

## Resultados locais

590 testes Java em 25 suítes passaram, incluindo 92 regras de arquitetura sem alteração,
quatro cenários de contenção/persistência e as regressões de tarefas, gates e continuidade.
Quatro testes React, typecheck e build completo passaram; 43 testes dos contratos de evidências
passaram. Chromium desktop, iPhone 15 Pro e Pixel 7 confirmaram um POST, recarga e ausência de
overflow. O navegador usa API sintética; persistência e política foram verificadas em Java.

Um replay somente local do snapshot consultivo do Vega confirmou oito entregas preservadas e
zero aceites de destino fabricados. Os requisitos Turmalina acrescentados permaneceram pendentes.
Isso não é homologação funcional do produto nem prova da tarefa 917. Não houve POST no Vega.

A primeira rodada ampliada encontrou compilação desatualizada na verificação do contexto de
tarefas e esgotamento do limite efetivo de 3,5 GiB da sandbox. A recompilação, o limite de heap Maven
e os lotes sequenciais isolados
passaram, sem enfraquecer testes. A atestação Mira v89 conserva v88 e revalida somente as duas
fontes compartilhadas alteradas; não renova homologação ou autorização comercial.

Evidências locais recuperáveis: `artifacts/partial-continuity` e
`artifacts/process-version-continuity`, declaradas no manifesto de entrega. Fontes de teste,
matriz e contrato estão versionados. Publicação ainda depende do PR, checks e deploys.

A recarga após revalidação consulta o aceite vigente da ocorrência de destino, sem alterar o
recibo histórico. Testes Java e React confirmaram que a pendência antiga deixa de ser anunciada
quando existe aceite real. Typecheck e build foram repetidos para essa alteração. Consulta MCP
adicional confirmou 917 COMPLETED e 921 BLOCKED no experiment:115; nenhum desses estados foi alterado.

## Complemento do contexto de correção

Após a integração do PR #5865, a inspeção do consumidor Dédalo confirmou que o contexto completo
é injetado na execução. A exceção antiga de `prototypeCorrection` consultava todas as versões e
poderia anunciar artefatos pendentes como conclusões. A atividade comum, por sua vez, não recebia
os aceites compatíveis da origem. Duas regressões falharam antes da correção.

Foram consideradas três opções: limitar a consulta à versão de destino (simples, mas perderia
reprovações úteis); fornecer apenas uma orientação no prompt (barato, mas manteria dados
contraditórios); aplicar o recibo ao contexto estruturado (pequeno ajuste, auditável e reutilizável).
Foi escolhida a terceira. O contexto conserva reprovações e artefatos históricos; conclusões
incluem somente resultados do destino e tarefas aceitas pelo recibo. O seletor vigente elimina
duplicidade e preserva resultados posteriores. Fluxos sem recibo mantêm seu comportamento.

O complemento passou em 209 testes Java de tarefas, seleção de contexto, continuidade, entrada
de correção, orçamento e autorização de versão. O pacote comercial manteve 857 arquivos e 224
manifestos válidos, sem mudança da candidata. Evidências antes/depois ficam no diretório recuperável
já declarado. Nenhum dado ou agente do Vega foi alterado.
