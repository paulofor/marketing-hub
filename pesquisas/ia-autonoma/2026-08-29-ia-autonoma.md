# IA Autoaprendente — 2026-08-29

**Horário da rodada:** 18:11 BRT

Nesta rodada há **três avanços novos e relevantes**. O principal é o **JIT-Agent**, que trata o harness como um artefato executável que pode ser gerado e melhorado durante uma sequência de tarefas, enquanto o modelo gerador permanece congelado em tempo de uso. O segundo, **WikiSkill**, separa explicitamente experiência bruta, conhecimento persistente e skills operacionais confiáveis. O terceiro, **HarnessLens**, melhora a evolução do harness ao verificar cada alteração apenas nas tarefas que realmente deveriam revelar o comportamento modificado, reduzindo muito o custo de avaliação.

Não encontrei hoje um novo caso de produção no nível LinkedIn/Tencent/Warp em que o sistema feche sozinho todo o ciclo de tráfego real → experimento → seleção → promoção persistente.

## Resumo de classificação

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/workflows/tools/skills | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| JIT-Agent | Offline, no treino do meta-modelo | **Sim, online** | **Sim, online** | Não | **Sim** |
| WikiSkill | Não | Não no framework | **Sim** | A Wiki isoladamente é memória, mas alimenta otimização real | Parcial |
| HarnessLens | Não | Configuração do harness, não framework-fonte | **Sim** | Não | Parcial |

## 1. JIT-Agent — harness executável gerado e evoluído just-in-time

**Classificação:** online principalmente **(2) + (3)**; para construir o JIT-Agent existe também **(1) + (5)** porque o meta-modelo é treinado offline com exemplos, reparos e RL.

O JIT-Agent é um modelo de 27B treinado para produzir um harness executável específico para cada tarefa a partir de um protocolo fixo de quatro módulos: memória, planejamento, ação e orquestração de capacidades/tools/skills. Em execução, o gerador pode permanecer congelado; o que evolui é um **arquivo/banco de harnesses**. Conforme novas trajetórias e feedback chegam, o sistema recupera harnesses anteriores, gera uma variante melhor e atualiza o arquivo persistente.

Isso é mais forte do que apenas alterar um prompt. Os harnesses gerados podem mudar representação de planejamento, topologia de ações, estado persistente e capacidades expostas ao agente. Em um exemplo, a tarefa é compilada para um DAG explícito.

### Métricas

Em 24 comparações controladas entre backbone e benchmark, o harness gerado pelo JIT-Agent superou ReAct em todas, com ganho médio de **+7,6 pontos percentuais**. Por família de modelo, os ganhos médios reportados foram **+10,2** para DeepSeek, **+4,0** para Qwen3.6 e **+8,6** para Mimo2.5. No DeepSearchQA o ganho médio foi **+15,2 pontos**, incluindo **+22,2** com Mimo-V2.5-Pro e **+19,0** com DeepSeek-V4-Flash. No DeepPlanning-Shopping houve ganho médio de **+7,5**, chegando a **+24,8** com DeepSeek-V4-Flash.

Os autores também comparam uma versão **Static JIT** com **Streaming JIT**. Na versão streaming, os harnesses são recuperados e atualizados ao longo da sequência de tarefas; a vantagem aumenta conforme o feedback se acumula e termina com maior acurácia cumulativa nos benchmarks DeepPlanning-Shopping, DeepPlanning-Travel e OfficeBench.

### Intervenção humana e limitação

O ciclo online não atualiza os pesos do gerador, mas chegar a esse gerador exige uma infraestrutura bastante projetada por humanos: exemplos produzidos por teacher, trajetórias de reparo para erros de compilação/runtime e uma etapa Evo-GDPO que treina o JIT-Agent para propor harnesses que ultrapassem a fronteira do arquivo existente em recompensa, custo e latência. Portanto não é um caso de RSI espontâneo.

### Padrão reutilizável

`Task → recuperar harnesses anteriores → gerar harness candidato → executar → verifier/feedback → comparar com arquivo atual → persistir vencedor → próxima tarefa recupera a versão melhor`

Para agentes com MCP, isso sugere manter **o LLM fixo e a configuração operacional versionada**: seleção de tools, sequenciamento, estado, prompts intermediários, política de retries, verificadores e subagentes podem ser tratados como um artefato executável que compete contra versões anteriores.

Fonte: https://arxiv.org/abs/2608.25593

## 2. WikiSkill — separar memória de experiência da skill que está autorizada a governar o agente

**Classificação:** principalmente **(3)**. Não há mudança de pesos. A Wiki é uma camada de conhecimento persistente, mas o sistema não fica em (4) porque esse conhecimento é usado para criar e validar mudanças reais nas skills operacionais.

O WikiSkill, de Google Research e Virginia Tech, mantém três camadas distintas:

1. **experiência bruta**: traces de execução;
2. **Wiki persistente**: padrões, hipóteses, erros e tentativas anteriores;
3. **skills executáveis**: a política procedural efetivamente usada pelo agente.

Depois de uma rodada, um Wiki Maintainer consolida o que foi aprendido. Um Skill Proposer lê a Wiki e traces selecionados e propõe uma mudança **atômica**: criar uma skill nova ou alterar apenas uma skill existente. O candidato é avaliado em um conjunto de validação separado e só substitui a skill atual se superar estritamente o melhor score anterior.

A ideia mais interessante é que, quando uma skill candidata falha, **o patch da skill é revertido, mas a Wiki não é revertida**. A rejeição é registrada, incluindo diff, score e razão, para impedir que futuras rodadas repitam o mesmo caminho ruim. Assim, o sistema pode lembrar uma hipótese rejeitada sem deixar essa hipótese controlar as próximas execuções.

### Métricas

Média em cinco benchmarks:

- Qwen-3.5-4B: **26,2 → 38,5**;
- Qwen-3.5-9B: **29,9 → 47,4**;
- Qwen-3.6-27B: **39,4 → 63,3**;
- Gemma-4-31B: **41,3 → 54,9**;
- Gemini-3.5-Flash: **49,5 → 68,1**.

Contra o concorrente de evolução de skills mais forte usado pelos autores, os ganhos médios adicionais foram de **+3,3, +5,1, +10,0, +5,8 e +12,0 pontos**, respectivamente.

A ablação é forte: retirar a Wiki persistente do Skill Proposer derrubou a média do Gemini-3.5-Flash de **63,7 para 48,7**, queda de 15 pontos. Curiosamente, dar a Wiki diretamente ao agente executor durante a evolução também piorou a qualidade final das skills, de **63,7 para 60,9**: o agente conseguia “se virar” lendo a Wiki, escondendo lacunas que deveriam virar skills melhores.

Também há transferência entre modelos, mas não é universal. Uma skill evoluída por Qwen3.6-27B elevou Qwen3.5-9B no SpreadSheet de **24,3 sem skill para 50,5**, mas uma skill produzida por Qwen3.5-4B reduziu Gemini-3.5-Flash no mesmo benchmark de **50,5 para 18,1**. Isso mostra que conhecimento procedural pode ser bastante dependente da capacidade do modelo que irá executá-lo.

### Intervenção humana e limitação

Humanos definem arquitetura, benchmarks e o gate. As alterações concretas de Wiki e skills são produzidas pelo sistema. O paper ainda injeta skills diretamente no prompt; retrieval/triggering de skills, pruning de Wiki e adaptação dentro de uma única execução longa ficam para trabalhos futuros.

### Padrão reutilizável

`traces imutáveis → Wiki/padrões persistentes → proposer → pequeno diff de skill → validação → aceitar ou rollback da skill → SEMPRE preservar o conhecimento sobre a tentativa`

A lição é importante: **memória e política não deveriam ser a mesma coisa**. O agente pode lembrar que uma estratégia foi tentada e falhou, sem transformá-la em comportamento ativo.

Fonte: https://arxiv.org/abs/2608.27454

## 3. HarnessLens — verificar cada mudança exatamente onde ela deveria fazer diferença

**Classificação:** principalmente **(3)**. O framework-base, modelo, provider, permissões, tools de benchmark e evaluator permanecem fixos. O sistema evolui componentes configuráveis do harness, como instruções, skills, descrições de tools e parâmetros, contexto de lifecycle hooks, agent definitions e compactação.

O problema atacado é importante: um sistema que propõe uma alteração de harness normalmente desperdiça muito orçamento testando-a em lotes grandes e aleatórios. HarnessLens tenta primeiro entender **qual comportamento deveria ter mudado** e seleciona:

- tarefas em que o novo comportamento deveria aparecer;
- probes de regressão nos componentes possivelmente afetados.

A versão atual e a candidata são executadas sob condições pareadas. O sistema não aceita uma alteração apenas porque a média subiu: precisa existir evidência atribuível de comportamento melhor e **nenhuma regressão atribuível**. Só então o candidato recebe uma rodada de confirmação majoritariamente em tarefas frescas; se a métrica também melhorar, a alteração é persistida.

Há barreiras informacionais explícitas: o componente que compara comportamento não vê o diff proposto; o editor não tem acesso ao conjunto de teste, credenciais ou estado do evaluator; o controlador externo controla o orçamento.

### Métricas

Médias agregadas:

- OpenCode: **41,83 → 47,53**;
- Codex: **40,94 → 44,06**;
- Pi: **45,49 → 49,67**.

Exemplos: OpenCode no BIRD foi de **37,50 para 45,83**; Codex no BIRD de **37,50 para 47,22**; Pi no Banking de **19,40 para 33,33**.

O destaque é eficiência: HarnessLens usa orçamento máximo de **200 unidades totais** incluindo rollouts e sessões LLM, contra 300 apenas de TRAIN rollouts no HarnessFix, 660 no Meta-Harness e 4.800 no Self-Harness. Isso equivale a cerca de **1/24 do orçamento do Self-Harness**. No TEST, o método nunca ficou abaixo do harness inicial; quando não encontrou evidência suficiente, simplesmente não aceitou edição.

Na ablação com OpenCode, estratégias de batch fixo ou aleatório praticamente não evoluíram o sistema. Um gate baseado apenas na métrica melhorou menos. A versão completa chegou a **85,0 no Retail, 25,37 no Banking e 45,83 no BIRD**, contra **75,0 / 20,90 / 37,50** no harness original.

### Intervenção humana e limitação

A arquitetura de verificação e os componentes editáveis são definidos por humanos; propostas, seleção de verificações e promoção são automatizadas. Foi testado em uma única família de modelos, três harnesses e quatro benchmarks públicos, sem validação ainda em deployment aberto de produção.

### Padrão reutilizável

`patch candidato → inferir comportamento afetado → selecionar testes que exercitam esse comportamento + regressões relacionadas → A/B pareado → atribuir mudança → confirmação em amostra fresca → commit/rollback`

Para MCP, isso é especialmente útil. Se o agente altera o schema ou a descrição de uma tool, não é necessário rerodar todo o sistema: primeiro teste tarefas que realmente utilizam aquela tool, mais um conjunto pequeno de regressões adjacentes.

Fonte: https://arxiv.org/abs/2608.27311

## Síntese arquitetural da rodada

Os três trabalhos convergem para uma arquitetura que parece cada vez mais sólida:

`experiência → camada persistente de conhecimento → gerar alteração pequena e identificável → escolher verifier relevante à alteração → testar contra versão atual → promover apenas se houver melhora atribuível → manter histórico de alterações aceitas e rejeitadas → próxima execução parte da melhor versão conhecida`

A mudança importante é que **autoaperfeiçoamento não precisa significar mexer nos pesos**. O objeto que aprende pode ser um harness executável, uma biblioteca de skills, a configuração de tools/MCP ou a própria lógica de planejamento. Ao mesmo tempo, esses trabalhos reforçam que a parte evolutiva precisa ficar separada de um **evaluator/gate relativamente imutável**, com rollback e holdout.
