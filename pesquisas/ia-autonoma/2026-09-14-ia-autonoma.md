# Radar IA Autônoma — 2026-09-14

## Rodada 18:18 (America/Sao_Paulo)

Nesta rodada, **um desenvolvimento novo e relevante passou o filtro**. Não apareceu um novo caso convincente de recursive self-improvement forte nem um novo deployment comercial fechando sozinho todo o ciclo produção → variante → promoção → rollback. O avanço mais útil veio de um paper de indústria da **Manulife** sobre um **skill router de produção com 34.396 skills**: ao tentar melhorar o routing via fine-tuning com dados sintéticos, o sistema sofreu **catastrophic forgetting** em tarefas reais e OOD.

## Caso: When Synthetic Data Hurts — Manulife

**Fonte primária:** https://arxiv.org/abs/2609.10750

**Classificação:**

- **(1) mudança persistente de pesos:** sim — retriever/reranker Qwen3-Embedding-0.6B ajustados via LoRA.
- **(2) mudança persistente de código/scaffold/harness:** não é o foco.
- **(3) evolução de prompts/retrieval/workflows/tools/skills:** impacto direto sobre a camada de retrieval/routing de skills, porém a adaptação estudada é realizada principalmente via mudança de pesos do router.
- **(4) mera memória/contexto:** não.
- **(5) fine-tuning ou otimização conduzida por humanos:** sim — fortemente. O pipeline, dados sintéticos, regularização e gates são definidos pelos pesquisadores; o agente não decide sozinho quando ou como retreinar o router.

### O que foi estudado

O trabalho usa um catálogo de **34.396 skills** e executa tarefas de SkillsBench e Terminal-Bench 2 em ambiente Harbor. O estudo produziu **1.423 trials em 173 tarefas**, dos quais 1.116 tiveram score; a parte supervisionada final inclui 75 tarefas com pelo menos uma skill positiva e 273 skills positivas.

A pergunta prática foi: conforme a biblioteca de skills cresce, podemos usar dados sintéticos para treinar um router melhor sem destruir capacidades que ele já tinha?

A resposta foi: **não automaticamente**.

No regime agressivo de fine-tuning, a melhora no conjunto sintético veio acompanhada de uma queda de **recall OOD de 0,850 para 0,650**. Ou seja, o router ficou melhor exatamente no tipo de exemplo usado para treiná-lo e pior em casos reais ou fora da nova distribuição.

### Mitigação

Os autores testaram técnicas de continual learning, incluindo:

- embedding-anchor regularization;
- Learning without Forgetting (LwF);
- Elastic Weight Consolidation (EWC);
- L2-initialization.

Esses métodos conseguiram preservar melhor o comportamento anterior em dados reais/OOD e, ao mesmo tempo, melhorar o retrieval sintético in-distribution. Para o retriever/reranker Qwen de 0,6B, o ganho in-distribution reportado chegou a **+13,98%** em relação ao ajuste agressivo sem preservação adequada.

### Por que isso importa para agentes autoaperfeiçoáveis

Até agora, boa parte do radar estava concentrada em **criar ou editar skills**. Este trabalho mostra que existe outro componente que também precisa evoluir cuidadosamente: **o skill router**.

Não adianta o agente acumular centenas ou milhares de boas skills se, depois de aprender novas rotas, ele deixa de recuperar corretamente as antigas.

Isso cria um problema equivalente ao catastrophic forgetting de modelos, só que na camada operacional do harness:

```text
nova experiência
      ↓
novas skills / novos exemplos
      ↓
router é otimizado
      ↓
melhora na distribuição nova
      ↓
regressão silenciosa em skills antigas
```

### Padrão arquitetural reutilizável

Para agentes próprios, eu passaria a versionar e avaliar **skill bank e skill router separadamente**.

```text
Execution Traces
      ↓
Real Skill Outcomes
      ↓
Synthetic Augmentation
      ↓
Candidate Router
      ↓
┌─────────────────────────────┐
│ Gate A: tarefas novas / ID  │
│ Gate B: tarefas reais antigas│
│ Gate C: OOD / long tail     │
└─────────────────────────────┘
      ↓
shadow / replay
      ↓
promote ou rollback
```

Para MCP, isso significa que, se o sistema aprender que certas tools/skills devem ser selecionadas com mais frequência, a promoção dessa nova política de routing deve provar que **não perdeu rotas antigas**.

Eu adicionaria um requisito explícito ao pipeline de autoevolução:

> **nenhuma melhoria no router é promovida apenas por ganho na distribuição que gerou a alteração; ela precisa preservar um conjunto congelado de capacidades anteriores e um conjunto OOD.**

### Limitações

Este não é um caso de self-improvement autônomo completo. O agente não coleta sozinho seus traces, decide que precisa retreinar, escolhe a técnica de continual learning e promove o novo router. O trabalho é principalmente **categoria (1) + (5)**: atualização persistente de pesos dentro de um processo desenhado por humanos.

Ainda assim, a evidência é relevante porque vem de um cenário de **skill routing em escala de produção**, e revela um problema que tende a surgir diretamente em agentes que acumulam skills, tools e playbooks ao longo do tempo.

## Principal conclusão da rodada

A nova lição é que **o problema de continual learning não está apenas no LLM principal**. Ele também aparece em componentes menores do harness, como o router de skills.

Portanto, a arquitetura de um agente que aprende continuamente precisa proteger pelo menos três coisas contra regressão:

1. capacidades do modelo;
2. conteúdo das skills/playbooks;
3. **a política que decide qual skill usar**.

O terceiro item é fácil de ignorar e, em um sistema com milhares de skills, pode se tornar um dos componentes mais críticos.

## Fontes

- Murtaza, S. S. et al. **When Synthetic Data Hurts: On Catastrophic Forgetting in Skill Retrieval for LLM Agents**. arXiv:2609.10750, 9 Sep 2026. https://arxiv.org/abs/2609.10750
