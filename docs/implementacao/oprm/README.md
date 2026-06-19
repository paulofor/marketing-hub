# Implementação OPRM NichoCNAE

## Documento de entrada

Este arquivo define a ordem canônica para implementar as melhorias do pipeline OPRM NichoCNAE.

Os dois planos existentes não são alternativas concorrentes.

## Documento mestre

Seguir como roadmap principal:

`docs/implementacao/oprm/plano-melhoria-qualidade-pipeline-nichocnae.md`

Esse documento define:

- diagnóstico global do pipeline;
- arquitetura-alvo;
- qualidade das fontes e claims;
- gates de materialização;
- segurança da busca;
- contratos de dados;
- prioridades P0, P1 e P2;
- critérios globais de aceite.

## Documento complementar obrigatório

Usar durante a implementação da máquina de estados, retomada e reaproveitamento:

`docs/implementacao/oprm/plano-reprocessamento-orientado-conhecimento-nichocnae.md`

Esse documento detalha:

- `Knowledge Accumulator`;
- `Reprocess Controller`;
- retry técnico versus reprocessamento cognitivo;
- versionamento de conhecimento e execuções;
- invalidação seletiva;
- grafo de dependência de artefatos;
- memória de queries, fontes e falhas;
- prevenção de loops e controle de orçamento.

O plano complementar implementa e aprofunda principalmente estes itens do plano mestre:

- seção `5.9 — Máquina de estados e idempotência`;
- seção `6.4 — Adaptive Deep Research Planner`;
- item `P0.4 — Correção da máquina de estados`;
- item `P1.2 — Query planner adaptativo`;
- requisitos de retomada, reuso e observabilidade.

---

## Ordem recomendada de implementação

### Fase 0 — Baseline e proteção

Antes de alterar o comportamento:

1. transformar os ciclos 68–75 em fixtures e testes de regressão;
2. registrar métricas atuais de custo, queries, fetches, fontes e materializações;
3. colocar a nova execução sob feature flag;
4. impedir que a versão nova materialize automaticamente durante a calibração.

### Fase 1 — Correções de integridade de baixo acoplamento

Executar a partir do plano mestre:

1. `P0.1 — Source safety filter`;
2. canonicalização e deduplicação básica de URLs;
3. `P0.5 — Validador de nome`;
4. `P0.6 — Isolamento entre CNAEs`;
5. classificação correta de falhas técnicas.

Resultado esperado:

- conteúdo inseguro bloqueado;
- nomes específicos não geram erro 500;
- ciclos não misturam CNAEs;
- SSL, timeout e broken pipe não são tratados como falha de mercado.

### Fase 2 — Integridade da evidência

Executar a partir do plano mestre:

1. `P0.3 — Trecho exato obrigatório`;
2. `P0.2 — Juiz semântico de ator e contexto`;
3. validação de entailment;
4. deduplicação de claims e fontes independentes;
5. síntese restrita a claims aprovados.

Resultado esperado:

- nenhum claim sem trecho exato;
- nenhuma inversão de ator;
- ocupações adjacentes não viram prova;
- sínteses passam a ser auditáveis.

### Fase 3 — Núcleo de execução versionada

Neste ponto, usar o plano complementar como especificação detalhada.

Implementar:

1. execuções de estágio imutáveis e versionadas;
2. separação entre tentativa cognitiva e retry técnico;
3. linhagem mínima dos artefatos;
4. estados epistêmicos `TENTATIVE`, `VALIDATED`, `REJECTED`, `CONTRADICTED`, `SUPERSEDED` e `STALE`;
5. retomada da etapa técnica sem mudar a versão de conhecimento.

Resultado esperado:

- o pipeline deixa de sobrescrever artefatos;
- cada tentativa é auditável;
- falha técnica repete somente a etapa afetada.

### Fase 4 — Reprocessamento orientado por conhecimento

Continuar pelo plano complementar:

1. `Knowledge Accumulator`;
2. snapshot estruturado de conhecimento;
3. `Artifact Dependency Graph` mínimo;
4. invalidação seletiva;
5. `Reprocess Controller`;
6. matriz de rewind;
7. memória de queries e fontes;
8. assinaturas de falhas anteriores;
9. limites de tentativas e orçamento.

Resultado esperado:

- um gate pode voltar à menor etapa necessária;
- fatos validados são preservados;
- erros e fontes rejeitadas não são repetidos;
- o mesmo `researchCycleId` evolui por versões de conhecimento.

### Fase 5 — Pesquisa eficiente e seleção de candidatos

Retornar ao plano mestre:

1. `P1.1 — Candidate tournament`;
2. `P1.2 — Query planner adaptativo`;
3. `P1.3 — Reranking por objetivo`;
4. `P1.4 — Deduplicação completa`;
5. `P1.5 — Normalização do schema`.

O query planner adaptativo deve consumir o snapshot e os gaps definidos pelo plano complementar.

Resultado esperado:

- menos queries antecipadas;
- pesquisa apenas dos gaps;
- seleção de candidatos baseada em evidência;
- redução de custo e contaminação.

### Fase 6 — Gate comercial e calibração

Executar a partir do plano mestre:

1. níveis de evidência `E0–E5`;
2. confiança calculada a partir das evidências;
3. benchmark humano;
4. revisão humana seletiva;
5. ganho informacional por tentativa;
6. liberação gradual da materialização automática.

Resultado esperado:

- dor, impacto econômico e intenção de compra ficam separados;
- materialização automática exige pelo menos E3;
- a confiança deixa de ser score especulativo do modelo.

---

## Regra prática para o desenvolvedor

Ao implementar uma tarefa, usar a seguinte decisão:

| Tipo de tarefa | Documento que governa |
|---|---|
| Fonte, segurança, claim, trecho, entailment ou gate | Plano mestre |
| Estado, retry, versão, retomada ou reuso | Plano complementar |
| Query adaptativa | Plano mestre para objetivo e plano complementar para memória/reprocessamento |
| Persistência de conhecimento e linhagem | Plano complementar |
| Materialização e níveis E0–E5 | Plano mestre |
| Critérios globais de aceite | Plano mestre |

Em caso de conflito:

1. o plano mestre governa o objetivo e o gate de negócio;
2. o plano complementar governa a mecânica de execução, versão, memória e reprocessamento;
3. nenhuma implementação pode violar os hard gates de evidência do plano mestre para facilitar o reprocessamento.

---

## Primeiro incremento recomendado

Para reduzir risco, o primeiro pull request não deve tentar implementar todo o reprocessamento.

Escopo inicial:

1. criar enum de tipo de falha: `INFRASTRUCTURE`, `VALIDATION`, `QUALITY`, `MARKET_EVIDENCE`;
2. criar execução de estágio imutável com `attemptNumber` e `technicalRetryNumber`;
3. corrigir retry técnico para repetir somente a etapa afetada;
4. corrigir transição `MEI_AUDIENCE_READY + MATERIALIZAR_NICHO → MATERIALIZER`;
5. adicionar testes dos ciclos 69 e 74;
6. manter comportamento cognitivo atual temporariamente.

Segundo incremento:

1. trecho exato obrigatório;
2. juiz semântico de ator/contexto;
3. testes dos ciclos 70, 72 e 75.

Terceiro incremento:

1. snapshot de conhecimento;
2. `Reprocess Controller` mínimo;
3. rewind para query planner quando faltar evidência;
4. preservação de fontes e claims aceitos;
5. testes do ciclo 72.

---

## Definição de concluído

A implementação conjunta dos dois planos estará concluída quando:

- o plano mestre atingir seus critérios globais de aceite;
- o plano complementar atingir seus critérios de reaproveitamento e reprocessamento;
- o README continuar refletindo a ordem real de execução;
- os ciclos históricos 68–75 passarem pelos testes de regressão esperados;
- o sistema puder materializar, reprocessar, encerrar por falta de evidência ou encaminhar para revisão humana sem reiniciar todo o job.
