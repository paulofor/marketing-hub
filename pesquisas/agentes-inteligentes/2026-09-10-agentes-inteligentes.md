# Radar diário — agentes mais inteligentes | 10/09/2026

## Resumo executivo

A rodada de hoje encontrou uma leva particularmente forte de trabalhos submetidos em 8 de setembro e anunciados em 9 de setembro. O padrão mais importante é que **a inteligência operacional do agente está sendo movida para estruturas explícitas do harness**, em vez de depender de um prompt monolítico: grafos procedurais, verificadores independentes, memória de inconsistências, skills versionadas e otimização localizada por componente.

Para o AI Hub, a principal conclusão é: **não basta armazenar fatos e contexto; é preciso armazenar também “como agir”, “como verificar” e “onde o sistema costuma falhar” em estruturas separadas e evolutivas.**

---

## 1. Procedural Graphs: memória procedural explícita e autoevolutiva

**Tipo:** pesquisa acadêmica / preprint  
**Paper:** *Procedural Graphs: Self-Evolving Execution Structures for LLM Agents*  
**Submetido:** 08/09/2026  
**Fonte:** https://arxiv.org/abs/2609.09153

A ideia central é separar conhecimento factual de conhecimento procedural. Em vez de manter apenas notas, resumos ou trajetórias, o sistema representa procedimentos como um grafo dirigido de transições: cada nó pode representar uma ação, skill, passo de raciocínio ou estado; cada aresta descreve quando a transição é válida, como prosseguir e quais armadilhas evitar.

Durante a execução, o harness localiza o nó correspondente ao estado atual e fornece ao agente apenas o subgrafo próximo, transformado em orientação situacional. Durante a evolução offline, um refinador compara trajetórias bem-sucedidas e malsucedidas, propõe mudanças no grafo e só aceita uma mudança se ela mantiver ou melhorar o desempenho em validação. Mudanças rejeitadas ficam registradas como evidência negativa.

### Evidência

Os autores avaliam sete benchmarks e quatro famílias de LLM. No conjunto principal de seis benchmarks, o Procedural Graph ficou em primeiro ou empatado em primeiro em 21 de 24 combinações modelo–benchmark. Comparado com o melhor baseline de memória em cada combinação, teve 19 vitórias, 2 empates e 3 derrotas. Entre os maiores ganhos relatados estão +9,0 pontos no BFCL v3, +7,41 no GDPval e +6,96 no τ-bench.

### Limitações

O método ainda exige um mecanismo de localização do estado atual e um modelo adicional para gerar a orientação a partir do subgrafo. A autoevolução também depende de um conjunto de validação confiável; se a métrica de validação for ruim, o grafo pode otimizar para o alvo errado. O paper avalia principalmente ambientes benchmark e ainda não demonstra manutenção de grandes grafos procedurais em projetos reais durante meses.

### Aplicação ao AI Hub

Criar uma camada `ProceduralMemory`, separada da memória factual:

```text
FACT MEMORY
- arquitetura
- decisões
- restrições
- evidências

PROCEDURAL MEMORY
- passos válidos
- pré-condições
- transições
- verificações
- pitfalls
```

Exemplo:

```yaml
procedure: integrate_google_oauth

transitions:
  - from: inspect_current_auth
    to: identify_session_authority
    condition: existing authentication found
    pitfalls:
      - do not replace existing login blindly

  - from: implement_provider
    to: run_auth_regression
    condition: provider integration completed
    pitfalls:
      - do not declare success before existing-login regression tests
```

Isso é mais forte do que simplesmente recuperar uma conversa antiga dizendo “preserve o login atual”: o harness passa a conhecer **em que momento essa regra deve entrar na execução**.

---

## 2. ExecCritic: o agente não deveria escrever a solução e também definir sozinho o teste que prova que ela está correta

**Tipo:** pesquisa acadêmica / preprint  
**Paper:** *ExecCritic: Learn to Test, Test to Improve for Coding Agents*  
**Submetido:** 08/09/2026  
**Fonte:** https://arxiv.org/abs/2609.09133

O trabalho mostra um problema importante para qualquer harness com self-reflection: testes gerados pelo próprio agente podem codificar a mesma interpretação errada que gerou o patch. Nesse caso, código e teste “concordam” e criam falsa confiança.

O ExecCritic separa explicitamente dois papéis. Um `Test Agent` cria testes independentes; um harness fail-closed qualifica e congela esses testes; só então um `Repair Agent` pode modificar o código usando a execução como feedback, sem alterar os testes.

### Evidência

No SWE-bench Verified, mantendo o Repair Agent base fixo, testes produzidos por um Test Agent fraco reduziram a taxa de resolução de 61,2% para 57,3%. Já testes produzidos por GPT-5.6-sol elevaram a taxa para 65,3%. Após treinamento específico dos papéis, a composição dos dois agentes Qwen atingiu 72,6%, ganho de 11,4 pontos sobre o baseline sem testes.

O resultado negativo é tão importante quanto o positivo: **feedback de execução ruim pode tornar o agente pior.**

### Limitações

O estudo é específico para reparo de software e usa uma arquitetura treinada para os papéis. Mesmo testes “congelados” ainda podem representar uma especificação errada se o Test Agent interpretou mal o requisito. Portanto, o problema não desaparece; apenas fica mais auditável.

### Aplicação ao AI Hub

O `Requirement Compiler` deveria produzir critérios de aceitação, mas o agente que implementa não deveria ter autoridade para modificá-los livremente.

```text
Requirement Compiler
       ↓
Acceptance Criteria
       ↓
Independent Test/Verifier Agent
       ↓
FAIL-CLOSED QUALIFICATION
       ↓
freeze verifier artifacts
       ↓
Executor / Repair Agent
```

Para requisitos inferidos, isso é especialmente útil: a inferência só vira um requisito operacional forte depois de passar por validação/evidência.

---

## 3. AgentGrad: melhorar o harness exige descobrir QUAL componente causou a falha

**Tipo:** pesquisa acadêmica / preprint  
**Paper:** *AgentGrad: Intervention-guided Prompt Optimization for Multi Agent Systems*  
**Submetido:** 08/09/2026  
**Fonte:** https://arxiv.org/abs/2609.08572

O AgentGrad critica uma prática comum em otimização de prompts multiagente: quando uma execução falha, alterar o prompt de algum agente baseado em feedback textual sem provar que aquele agente era realmente o responsável pela falha.

A proposta realiza intervenções sequenciais: modifica o comportamento de um agente por vez e observa se a falha desaparece. Só então gera o “gradiente textual” correspondente. Depois agrupa falhas semanticamente semelhantes antes de generalizar uma atualização.

### Evidência

Os autores relatam melhor desempenho em cinco benchmarks multiagente e redução média de 2,5× no tempo de otimização em relação ao baseline mais rápido comparado.

### Limitações

É otimização de prompts em sistemas multiagente, não uma prova direta de melhoria em inferência de requisitos implícitos. Além disso, intervenções controladas ficam mais caras conforme cresce o número de agentes e componentes do harness.

### Aplicação ao AI Hub

Não usar um único `improve_harness()` que reescreve tudo. Registrar falhas e localizar causalmente o módulo:

```text
failure
  ↓
intervention tests
  ↓
which component changes the outcome?
  ↓
Requirement Compiler?
Context Router?
Planner?
Verifier?
Skill?
Memory retrieval?
  ↓
only then propose an update
```

Isso evita que um erro de retrieval seja “corrigido” alterando o planner ou que um erro de verificação gere uma nova regra no prompt global.

---

## 4. Closing the Consistency Gap: acurácia média esconde instabilidade entre execuções

**Tipo:** pesquisa acadêmica / preprint  
**Paper:** *Closing the Consistency Gap: Self-Evolving Agents That Learn to Stay on Course*  
**Submetido:** 08/09/2026  
**Fonte:** https://arxiv.org/abs/2609.08832

O paper mostra que avaliar um agente apenas por taxa média de sucesso é insuficiente. No AppWorld, um ReAct Agent com GPT-4.1 tinha pass rate médio de 77%, mas apenas 53% das tarefas eram resolvidas com sucesso em todas as cinco repetições — um gap de consistência de 24 pontos.

O framework identifica passos instáveis, transforma esses padrões em memória episódica e injeta guidelines específicas em futuras execuções semelhantes.

### Evidência

A fração de tarefas bem-sucedidas em todas as cinco execuções aumentou 16 pontos em tarefas iguais e 13 pontos em tarefas semelhantes.

### Limitações

A abordagem depende de executar tarefas repetidamente para descobrir instabilidade, o que aumenta custo. Também existe risco de memorizar particularidades do benchmark em vez de aprender uma regra geral.

### Aplicação ao AI Hub

Adicionar uma classe especial de memória:

```text
InstabilityMemory

- task_pattern
- unstable_step
- observed_variants
- failure_frequency
- stabilizing_guideline
- supporting_runs
```

E mudar as métricas de avaliação:

```text
não medir somente:
pass@1

medir também:
pass@N consistency
implicit-requirement consistency
retrieval consistency
verification consistency
```

Isso é muito relevante para “deduzir aquilo que não foi dito”: não basta o agente perceber o requisito implícito em uma execução e esquecê-lo na próxima.

---

## 5. SkillAdam: skills autoevolutivas precisam de memória da própria otimização e limite de mudança

**Tipo:** pesquisa acadêmica / preprint  
**Paper:** *SkillAdam: Stable and Efficient Skill Evolution for Agents*  
**Submetido:** 08/09/2026  
**Fonte:** https://arxiv.org/abs/2609.08944

O trabalho tenta resolver um problema que provavelmente aparecerá quando o AI Hub começar a evoluir suas próprias skills: uma correção recente pode sobrescrever uma regra boa aprendida anteriormente.

O SkillAdam usa duas ideias inspiradas no Adam optimizer. Uma `Optimization Memory` registra problemas, tentativas de solução e seus resultados para manter direção estável. Um `Volatility-driven Edit Budget` reduz o tamanho das alterações quando os efeitos recentes são inconsistentes e permite mudanças maiores quando a evidência é consistente.

### Evidência

Nos cinco benchmarks curtos, SkillAdam obteve quatro vitórias estritas e um empate. Em DeepPlanning, aumentou a média de 21,7% do SkillOpt para 28,3%. Na fase de otimização, usou 67,3% menos tokens e 68,8% menos chamadas de API que SkillOpt, ao mesmo tempo em que obteve desempenho final superior.

Na ablação de DeepPlanning, retirar tanto memória de otimização quanto orçamento adaptativo derrubou a média de 28,3% para 19,2%.

### Limitações

Ainda exige ciclos repetidos de avaliação e um avaliador suficientemente confiável. O ganho vem de benchmarks e não prova automaticamente estabilidade em projetos de software reais durante longos períodos.

### Aplicação ao AI Hub

Cada skill deveria possuir histórico de evolução, e não apenas `skill.md` atual:

```yaml
skill: google-oauth-spring
version: 7

optimization_memory:
  - issue: forgot password-login regression
    attempted_fix: add regression step
    outcome: improved

  - issue: excessive mandatory checks
    attempted_fix: remove all preflight checks
    outcome: rejected

edit_budget: small
volatility: high
```

A implicação prática é importante: **não deixar o último erro observado reescrever uma skill inteira.**

---

## 6. Q2D-Web: benchmark de RAG para agentes precisa testar consultas escritas pelo agente, não pelo humano

**Tipo:** pesquisa acadêmica / benchmark  
**Paper:** *Q2D-Web: A Large-Scale Benchmark for Retrieval in Agentic RAG Systems*  
**Submetido:** 08/09/2026  
**Fonte:** https://arxiv.org/abs/2609.08887

O Q2D-Web observa que retrievers usados por agentes recebem consultas reformuladas por máquinas, cuja distribuição é diferente de consultas escritas por humanos. O benchmark usa 190 milhões de documentos e 70 mil consultas agentic em dez idiomas, derivadas de queries reais de usuários e seus threads.

### Evidência

Treze retrievers foram comparados. A ordem relativa dos métodos variou bastante conforme domínio, idioma e tipo de consulta. Os autores também mostram que uma amostra de um terço do corpus, construída por reciprocal-rank fusion, preserva o ranking dos modelos no benchmark completo, com diferenças absolutas de Recall@1000 de 3–7 pontos.

### Limitações

O benchmark mede principalmente o primeiro estágio de retrieval, não a taxa final de sucesso do agente. Parte dos relevance judgments vem de sinais de produção, citações e julgamentos por LLM, portanto ainda há imperfeição nos rótulos.

### Aplicação ao AI Hub

O benchmark do seu MCP/retrieval não deveria usar apenas buscas manuais como:

```text
"Google OAuth architecture"
```

Ele deveria registrar e avaliar **as consultas que o próprio agente realmente gera**, por exemplo:

```text
"where is session ownership decided"
"previous auth decisions refresh token"
"existing password login regression tests"
```

Métrica importante:

```text
Agent Retrieval Recall
= informação necessária recuperada
  pelas queries que o próprio agente decidiu formular
```

---

## Síntese para o AI Hub

A arquitetura que emerge da rodada de hoje é menos parecida com um “prompt enorme” e mais com um runtime cognitivo composto por artefatos independentes:

```text
USER INTENT
    │
    ▼
Requirement Compiler
    │
    ├── facts / inferred / unknown
    │
    ▼
Context Router ───────────────► Evidence / MCP / Repo
    │
    ▼
Procedural Graph
    │
    ▼
Planner
    │
    ▼
Independent Verifier
    │
    ▼
Executor
    │
    ▼
Execution Evidence
    │
    ├── instability detection
    ├── skill evolution
    ├── procedural-graph evolution
    └── harness component diagnosis
```

### Prioridade prática sugerida

1. **Procedural Graph / Procedural Memory** — representa como agir e em qual ordem.
2. **Independent Verifier / Acceptance Contract** — impede o executor de validar a própria interpretação errada.
3. **Instability Memory** — aprende especialmente com comportamentos que variam entre execuções.
4. **Skill Evolution com histórico e edit budget** — evita que novas correções destruam conhecimento antigo.
5. **Harness Component Attribution** — só altera um componente quando há evidência de que ele causou a falha.
6. **Agent-generated Retrieval Benchmark** — mede o MCP usando as queries que o próprio agente formula.

## Conclusão

A melhor ideia nova de hoje é a separação entre **memória factual** e **memória procedural**. Para fazer um agente deduzir algo que não estava no prompt, recuperar uma decisão antiga é apenas metade do problema. O agente precisa saber **quando aquela decisão se aplica, qual próximo passo ela implica e qual ação seria inválida se a ignorasse**.

A direção mais promissora para o AI Hub passa a ser:

```text
LLM
+ belief/requirement state
+ evidence retrieval
+ procedural graph
+ independent verifier
+ instability memory
+ versioned skills
+ causal harness evolution
──────────────────────────────
agent intelligence at system level
```

Não encontrei hoje um post técnico de laboratório com evidência nova suficientemente forte para entrar acima desses papers; preferi manter a rodada centrada nos preprints recém-publicados.