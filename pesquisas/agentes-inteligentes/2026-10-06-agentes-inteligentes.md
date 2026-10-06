# Radar diário — agentes mais inteligentes | 06/10/2026

## Resumo

O lote de cs.AI de 6 de outubro trouxe trabalhos especialmente úteis para o problema deste radar. A conclusão principal é que "contexto ausente" precisa ser decomposto em estados distintos: informação que vale a pena perguntar, intenção que foi substituída, evidência que ficou obsoleta, tarefa inviável e evidência repetida que não é independente.

## Learning to Clarify Underspecified Intents

**Tipo:** pesquisa acadêmica / preprint, 03/10/2026.  
**Fonte:** https://arxiv.org/abs/2610.04719

O trabalho trata clarification como **Value of Information**: o agente deve perguntar pela informação cuja ausência causa maior perda evitável, não simplesmente porque está incerto. Em estudo pré-registrado com **456 sessões e 76 participantes**, o método obteve resultados significativamente melhores com menos perguntas e menor tempo/custo de interação.

**AI Hub:** o Ask-or-Infer Gate deveria estimar impacto de errar, custo de perguntar e se a evidência pode ser obtida em repo, runtime, banco ou documentação. Perguntar ao usuário vira último recurso para lacunas de alto valor.

**Limitação:** o domínio principal é geração de imagens; a função de utilidade precisa ser adaptada para coding e workflows empresariais.

## You Changed Your Mind, The Model Didn't

**Tipo:** pesquisa acadêmica / preprint, 05/10/2026.  
**Fonte:** https://arxiv.org/abs/2610.06496

O paper mostra **mentioned-as-in-effect confusion**: uma proposta rejeitada ou substituída continua influenciando o agente porque permaneceu no histórico.

**AI Hub:** criar um **Active Intent Ledger** com estados ACTIVE, REJECTED e SUPERSEDED. O audit log preserva tudo; o Working State injeta somente a intenção ativa e as transições necessárias.

**Limitação:** o benchmark não resolve sozinho casos em que o usuário nunca deixa claro se uma proposta virou requisito.

## Concord + MemTrace — memória com validade da fonte

**Tipo:** pesquisa acadêmica / preprints, 04/10/2026.  
**Fontes:** https://arxiv.org/abs/2610.05281 e https://arxiv.org/abs/2610.04838

Concord liga observações às fontes, detecta mudanças e atualiza, anota ou suprime contexto stale; nas condições construídas do ConcordBench, manteve respostas consistentes com o workspace restaurado usando **46,4% menos tokens** que o melhor baseline não-oracle. MemTrace ancora memória de coding em arquivos, símbolos e testes e revalida antes do reuso; com o mesmo backbone/harness, reporta **+21,2 pontos no DeepSWE, +4,4 no SWE-EVO e +17,8 no SWE-Milestone**.

**AI Hub:** cada evidence item deve carregar source, revision e validity. Se a fonte mudou, a memória não volta ao contexto antes de revalidação.

**Limitação:** isso funciona melhor para fontes versionáveis; fatos externos exigem políticas próprias de freshness.

## HERA — reconhecer quando não existe solução válida

**Tipo:** pesquisa acadêmica / preprint, 05/10/2026.  
**Fonte:** https://arxiv.org/abs/2610.06563

HERA co-evolui harness e ambientes feasible/infeasible. Em held-out evaluation, aumenta **abstention accuracy de 61,7% para 83,3%** e completion das tarefas viáveis de **68,3% para 76,7%**; o harness transfere para 19 outros LLMs com ganho médio de **15,3 pp em abstention**.

**AI Hub:** criar um Feasibility Gate com SOLVABLE, MISSING_CONTEXT, MISSING_CAPABILITY, BLOCKED, INFEASIBLE e NEEDS_USER. Evals devem remover propositalmente pré-condições para testar se o agente reconhece impossibilidade em vez de inventar um caminho.

**Limitação:** mutações sintéticas precisam representar falhas reais.

## Judged Useless, Queried Anyway

**Tipo:** pesquisa acadêmica / tool use e stopping, 05/10/2026.  
**Fonte:** https://arxiv.org/abs/2610.06191

Sete agentes classificavam resultados de uma fonte defeituosa como inúteis em **97%–100%** dos casos, mas frequentemente continuavam consultando-a. Só uma regra imposta pelo harness tornou o stopping causalmente dependente desse julgamento; uma replicação pré-registrada em **300 perguntas** confirmou o efeito.

**AI Hub:** criar uma ponte **Evidence Judgment → Control Policy**. Se uma fonte acumula evidência inútil, o runtime muda provider, pergunta, abstém ou encerra aquela linha de busca. Avaliação não pode ser apenas texto adicional no prompt.

**Limitação:** o threshold deve depender de risco, custo e latência.

## Copies or Sources? — consenso precisa contar fontes independentes

**Tipo:** pesquisa acadêmica / multiagente, 05/10/2026.  
**Fonte:** https://arxiv.org/abs/2610.06192

Quando vários agentes repetem a mesma leitura, aggregators tratam as cópias como corroboracão parcial. Em **5%–40%** dos logs com uma leitura repetida três vezes, isso leva a commitment precoce que um oracle de fontes independentes não faria. Exigir referência à leitura original reduz esse commitment de **11,2% para 1,1%**.

**AI Hub:** Shared Mission State e Evidence Graph devem usar evidence IDs e independent_source_count. Três agentes citando ev-17 continuam representando uma fonte.

**Limitação:** independência causal entre fontes reais pode ser difícil de determinar.

## EVISKILL + PrisMem — evolução ancorada em replay e capacidade

**Tipo:** pesquisa acadêmica / preprints, 04–05/10/2026.  
**Fontes:** https://arxiv.org/abs/2610.05030 e https://arxiv.org/abs/2610.06361

EVISKILL liga cada edição de skill a **Replayable Evidence Cards**, permite estado provisório e exige replay/validação antes da promoção. PrisMem evolui memória por capacidade em vez de um score agregado; reporta **+10,54 pp no BEAM-1M e +7,83 pp no LongMemEval-M**.

**AI Hub:** toda mutação de skill ou memory policy deve guardar failures que a motivaram, replay cases, capability alvo, regressões e custo. Só depois vira global.

**Limitação:** aumenta custo de avaliação, mas reduz regressões silenciosas.

## Arquitetura sugerida

USER REQUEST → ACTIVE INTENT LEDGER → UNKNOWN/REQUIREMENT MAP → VALUE-OF-INFORMATION GATE → SOURCE-BOUND CONTEXT → STALE VALIDATION → FEASIBILITY GATE → EXECUTION → EVIDENCE GRAPH → INDEPENDENT-SOURCE CHECK → EVIDENCE-TO-CONTROL POLICY → VERIFIER → REPLAYABLE EVIDENCE CARD → CAPABILITY-SPECIFIC EVOLUTION.

## Prioridade

1. Active Intent Ledger.
2. Ask-or-Infer por Value of Information.
3. Evidence com source + revision + validity.
4. Feasibility Gate.
5. Evidence Judgment ligado ao control flow.
6. Contagem de fontes independentes.
7. Evidence Card + replay para promoção de mudanças.

## Conclusão

O avanço de hoje é perceber que descobrir o que faltou no prompt não significa apenas "buscar mais". O sistema precisa saber **o que vale perguntar, o que ainda está ativo, o que ficou stale, o que é impossível, quantas fontes independentes existem e quando uma avaliação deve realmente mudar o comportamento**.

Não encontrei nas últimas 24 horas um post técnico de laboratório/engenharia que acrescentasse algo materialmente diferente desses preprints; preferi não preencher essa categoria artificialmente.
