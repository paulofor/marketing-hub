# Radar de IA autoaprendente — 2026-09-10

## Rodada 18:09 (America/Sao_Paulo)

Há **um avanço novo e forte que passa o filtro nesta rodada**: **RobustSGPO**, de pesquisadores de Wuhan University e principalmente **Kuaishou Technology**, submetido em 9 de setembro de 2026. Ele é particularmente relevante porque estende o **AgentX**, um sistema de recomendação industrial já implantado em produção, e mostra como controlar de forma mais confiável o espaço de mudanças do próprio harness.

Dois outros trabalhos novos foram analisados, mas ficam abaixo do limiar principal: **A-JIT** é uma arquitetura muito interessante de software que se reescreve em runtime, porém ainda sem avaliação quantitativa robusta de melhoria acumulativa; **SmartWeatherAgent** demonstra otimização iterativa de prompts, mas a sequência de objetivos é fortemente pré-definida por humanos e não há evidência de aprendizagem aberta ou implantação contínua.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompt/workflow/tools/skills | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **RobustSGPO / Kuaishou AgentX** | Não | **Sim — estrutura multiagente e snapshots do harness** | **Sim — instruções, contratos e routing** | Não | Parcial |
| **A-JIT** | Não | **Sim — código gerado em runtime** | **Sim — workflows/interfaces podem ser especializados** | Não | Parcial; ainda conceitual |
| **SmartWeatherAgent** | Não | Não | **Sim — prompt** | Não | **Sim — objetivos e estágios pré-definidos** |

## 1. RobustSGPO — controlar *onde* o agente pode se modificar é tão importante quanto gerar a modificação

**Fonte principal:** https://arxiv.org/abs/2609.09646

O RobustSGPO foi submetido em **9 de setembro de 2026** e estende o mecanismo SGPO usado no **AgentX**, da Kuaishou. O AgentX já havia demonstrado em produção um loop de recomendação com Brainstorm Agent, Developing Agent, Evaluation Agent e Harness Evolution. O trabalho novo se concentra especificamente no problema de **como o Evolver deve escolher o escopo da próxima mutação do harness**.

O baseline SGPO já usa um loop persistente: coleta traces, diagnostica falhas, produz um gradiente semântico em linguagem natural, gera uma mudança candidata, executa replay pareado entre a versão atual e a candidata e só aceita a nova versão se ela superar o incumbent e passar pelos checks de segurança. Mudanças rejeitadas são revertidas, mas seus patches, scores e diagnósticos continuam disponíveis como experiência para refinamentos posteriores.

A novidade do RobustSGPO é separar explicitamente três decisões que antes ficavam muito livres para o LLM: **qual escopo editar, qual operação executar e de qual versão anterior continuar a busca**. O sistema define três níveis de permissão: `α1` altera um único agente; `α2` permite alterar qualquer agente já existente na etapa de brainstorming; `α3` também permite mudanças estruturais, como adicionar/remover agentes e alterar routing.

Em vez de abrir o espaço inteiro desde o começo, o melhor resultado veio de uma política periódica **estreito → médio → amplo (`1→2→3`)**. No teste, ela terminou em **4,34/5**, contra **4,06** quando o sistema manteve a permissão máxima (`α3`) o tempo todo. O trabalho mostra que “dar mais liberdade” ao Evolver não significa que ele explore melhor o espaço: a geração tende a voltar para reescritas familiares de instruções mesmo quando mudanças estruturais estão permitidas.

O RobustSGPO também adiciona um **Typed Compiler** para alterações estruturais recorrentes. Quando o controller determina, por exemplo, que um agente precisa ser adicionado ou removido, código determinístico constrói a modificação e verifica se exatamente os objetos solicitados foram alterados. Isso elevou a validade das propostas estruturais de **48,9% para 77,8%** em uma das comparações.

O terceiro mecanismo é um **archive de snapshots completos do harness**. Uma candidata pode não ser boa o suficiente para substituir o incumbent agora, mas ainda representar uma direção interessante de busca. O sistema guarda a melhor versão válida por categoria de alteração e posteriormente pode usá-la como ponto de partida para outra mutação. Mesmo assim, qualquer descendente dessa versão arquivada precisa superar o incumbent atual para ser promovido.

Nos 120 tasks, 95 runs e **7.350 tentativas de candidatas**, o RobustSGPO elevou o score final de 3,82 para **4,30** contra o SGPO original. Em 30 tarefas held-out, a conclusão correta subiu de **18/30 (60%) para 24/30 (80%)**. Sob o mesmo orçamento de **20 milhões de tokens**, o score passou de **3,77 para 4,14**.

Há ainda uma observação importante sobre continual learning. Depois de mudar a família de tarefas, manter snapshots por categoria reduziu a perda na família original para **0,15 ponto**, enquanto estratégias alternativas perderam aproximadamente **0,31–0,33**. Em contrapartida, um archive aleatório alcançou um endpoint ligeiramente melhor na nova família. Portanto surge um trade-off explícito entre **adaptar mais agressivamente** e **preservar capacidade anterior**.

### Classificação

O trabalho entra principalmente em **(2) mudança persistente de harness/scaffold** e **(3) evolução persistente de instruções, contratos, estrutura multiagente e routing**. Os pesos do modelo e o evaluator permanecem fixos. Não é categoria (4), porque os resultados anteriores alteram versões executáveis do harness que persistem entre rodadas. Também não é RSI forte: um processo externo continua definindo permissões, verificadores, métricas e regras de promoção.

### Padrão arquitetural reutilizável

O principal padrão que eu extrairia para agentes próprios é:

```text
trace
  ↓
credit attribution
  ↓
escolher escopo de mutação
  ↓
α1: um componente
α2: vários componentes existentes
α3: mudança estrutural / routing
  ↓
gerar patch
  ↓
typed/structural validation
  ↓
paired replay + held-out
  ↓
KEEP | ARCHIVE | ROLLBACK
  ↓
próxima rodada
```

Isso complementa diretamente o resultado do HarnessEvo visto na rodada anterior: **primeiro localizar a região causal; depois controlar o tamanho da liberdade dada ao Evolver**. Para um sistema baseado em MCP, eu usaria algo como `tool description → tool contract → multi-tool workflow → topology/routing` como níveis progressivos de permissão. O agente começaria tentando corrigir o menor componente possível e só ganharia autorização para mudar o workflow ou a topologia se alterações locais não resolvessem o problema.

Outra ideia muito boa é não tratar toda candidata rejeitada como lixo. Um patch pode ser ruim como substituto imediato, mas útil como **ramo de pesquisa**. O archive permite manter diversidade sem deixar versões experimentais contaminarem a linha principal.

### Limitações

O experimento novo é offline e cobre apenas o workflow de brainstorming do AgentX, embora a infraestrutura de origem seja industrial e já tenha sido implantada no Kuaishou. O próprio paper ressalta que não compara RobustSGPO com todos os sistemas recentes de harness evolution em benchmarks compartilhados. O ganho do archive também tem custo: o RobustSGPO consome mais tokens por rodada, e sua vantagem sobre Structured Search cai de 0,10 ponto em igualdade de rounds para apenas 0,04 quando o orçamento total de tokens é igualado.

O antecedente industrial, **AgentX**, continua sendo importante para interpretar o trabalho: em uma implantação anterior de três semanas no Kuaishou App, três workers processaram 374 ideias e produziram 10 rollouts lançáveis, com ganhos online reportados no produto. O RobustSGPO deve ser visto como **uma melhoria do mecanismo de self-evolution dessa linhagem**, não como uma nova implantação comercial independente.

Fonte do AgentX: https://arxiv.org/abs/2606.26859

## 2. A-JIT — sinal arquitetural importante, mas ainda abaixo do limiar experimental

**Fonte:** https://arxiv.org/abs/2609.10248

O A-JIT, também de 9 de setembro, propõe integrar um agente diretamente ao runtime da aplicação para observar uso e traces e **construir código just-in-time**. O sistema pode preencher implementações ausentes, gerar novas capacidades e especializar workflows ou interfaces de ferramentas com base em padrões observados durante a execução.

A proposta usa o conceito de `code holes` do Bosque: partes explicitamente incompletas do programa ficam disponíveis para síntese posterior. O runtime pode gerar valores concretos, acumular pares input/output, sintetizar uma implementação completa e validá-la com tipos, invariantes e constraints. Na visão mais autônoma, traces recorrentes de usuários seriam agrupados e transformados em novos workflows ou funções, inicialmente em shadow mode.

Isso se encaixa em **(2)** e parcialmente em **(3)**, porque código, workflows e interfaces podem ser persistentemente especializados sem alterar pesos. Porém eu **não promovo A-JIT ao mesmo nível do RobustSGPO** nesta rodada: o paper é principalmente uma proposta de paradigma e demonstrações de mecanismo; não há benchmark mostrando uma curva de melhoria autônoma, taxa de promoção, regressão, held-out ou ganho operacional acumulado. Ele é um sinal arquitetural forte para acompanhar, não ainda evidência robusta de self-improvement em produção.

## 3. SmartWeatherAgent — prompt melhora, mas o loop é fortemente guiado por humanos

**Fonte:** https://arxiv.org/abs/2609.10135

O SmartWeatherAgent implementa um loop `generation → evaluation → optimization` em 12 estágios de refinamento de prompt. O score composto de qualidade de alertas meteorológicos sobe de **4,2 para 8,9 (+112%)**, com semantic depth de 2,0 para 8,5, logical coherence de 7,5 para 9,0 e scientific rigor de 3,0 para 9,2.

Isso é tecnicamente **(3) evolução persistente de prompt**, mas com forte presença da categoria **(5)**. Os estágios de melhoria são previamente definidos: fases iniciais adicionam elementos básicos do alerta, fases intermediárias exigem métricas e rastreabilidade e fases finais introduzem mecanismos físicos, cadeias temporais e incerteza. Os próprios autores reconhecem que a evolução está limitada por dimensões de avaliação e um framework em estágios previamente especificado, além de ainda não estar integrado a streams meteorológicos operacionais em tempo real.

Por isso, o trabalho mostra que um loop de prompt pode acumular melhoria, mas **não é evidência de um agente descobrindo de forma aberta o que deve mudar com base em experiência de produção**.

## Conclusão da rodada

O **RobustSGPO é o avanço realmente importante de hoje**. Ele acrescenta uma camada que estava faltando no desenho dos agentes autoaperfeiçoáveis: não basta ter um Evolver capaz de gerar patches. É preciso controlar **o espaço de mutação**.

A arquitetura geral agora fica mais próxima de:

```text
Execution Trace
  → Credit Attribution
  → Mutation-Scope Controller
  → Candidate Patch
  → Structural/Permission Checks
  → Replay + Held-out
  → KEEP | ARCHIVE | ROLLBACK
  → Lineage / Retention
  → próxima execução
```

A implicação prática é forte para MCP e harnesses próprios: mudanças locais deveriam ser o default; mudanças de workflow, routing, criação de novos agentes ou novas tools deveriam exigir evidência maior. E candidatas promissoras, mas ainda insuficientes para produção, podem viver numa **linha de pesquisa separada** em vez de serem imediatamente descartadas ou promovidas.

Nesta varredura não apareceu um novo caso forte de **(1) pesos sendo atualizados continuamente a partir de experiência de produção**, nem um novo caso convincente de **recursive self-improvement forte** em que a versão melhorada assuma automaticamente o processo que produzirá sua própria sucessora.
