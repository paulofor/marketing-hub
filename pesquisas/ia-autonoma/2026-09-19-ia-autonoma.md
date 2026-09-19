# Radar de IA Autônoma — 2026-09-19

## Rodada 18:10 BRT

Nesta rodada, três avanços entram no radar principal e um quarto entra como infraestrutura habilitadora. O destaque é **AutoData**, porque amplia o auto-research de código/modelo para o próprio **algoritmo de seleção dos dados de pré-treinamento**. **SIFT** mostra como reduzir drasticamente o custo de explorar auto-modificações do harness. **Levels, Ticks and Cascaded Intelligence**, da Salesforce AI Research, é importante justamente pela classificação negativa: há persistência comportamental entre sessões, mas os próprios autores dizem que isso ainda é **acumulação, não aprendizado**. **Chronicle** não é um self-improving agent, mas oferece uma peça operacional muito útil para qualquer loop de evolução: transformar falhas reais em testes reprodutíveis de regressão.

Não encontrei nesta rodada um novo caso de produção no nível LinkedIn/Tencent/Warp em que o sistema, já em tráfego real, feche sozinho o ciclo completo `experiência → variante → avaliação → promoção → persistência`.

## Classificação

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness persistente | (3) Prompt/retrieval/workflow/tools/skills/estratégia persistente | (4) Apenas memória/contexto | (5) Otimização principalmente conduzida por humanos |
|---|---:|---:|---:|---:|---:|
| AutoData | Indiretamente, apenas nos modelos treinados para avaliar receitas | **Sim — algoritmo executável de seleção de dados** | **Sim — estratégia de curadoria** | Não | **Sim, no desenho do outer loop** |
| SIFT | Não | **Sim — principal** | **Sim — estratégia do harness** | Não | Parcial |
| Levels/Ticks/Cascaded Intelligence | Não | Parcial: regras/guards podem ser acumulados | Não como learner automático | **Sim — principal** | Sim |
| Chronicle | Não | Não é evolver; testa alterações | Não | Não | **Sim — infraestrutura humana/CI** |

---

## 1. AutoData — o agente agora otimiza também os dados que moldam o próximo modelo

**Fonte principal:** https://arxiv.org/abs/2609.19754

AutoData trata seleção de dados de pré-treinamento como um problema de **busca sobre programas executáveis**, não apenas como ajuste de pesos sobre domínios fixos. Em cada iteração, um agente propõe uma função de seleção, a função escolhe um subconjunto do corpus, um pequeno modelo proxy é treinado nesse subconjunto, o resultado de validação volta como feedback e o agente refina a próxima função.

Fluxo simplificado:

```text
candidate data-selection program
        ↓
seleciona subconjunto do corpus
        ↓
treina modelo proxy
        ↓
mede val-bpb / CORE
        ↓
feedback para o agente
        ↓
novo programa de seleção
```

### O que melhora sozinho

O objeto mutável é o **algoritmo de seleção de dados**: regras de scoring, estratificação, filtros, combinações de features e amostragem estocástica. O agente descobre interações entre sinais que não foram especificadas manualmente como receita final.

### O que persiste

A melhor receita executável descoberta ao final da busca. Ela pode ser reaplicada em novas execuções e, no experimento, foi transferida sem retuning para modelos maiores.

### Intervenção humana

Alta no meta-nível: humanos definem o corpus, as features disponíveis, o proxy, a métrica, o orçamento e o mecanismo de busca. Dentro desse espaço, o agente gera, executa, mede e refina automaticamente os programas candidatos.

### Métricas

- 200 passos de busca sobre um proxy GPT-2 de 125M.
- 1.200 métodos candidatos gerados no total entre GPT-5.5, Gemini-3-Pro-Preview e Claude-Opus-4.7.
- Os autores reportam melhor `val-bpb` do AutoData entre 125M e 897M, com melhoria estatisticamente significativa sobre os baselines testados.
- No modelo de 1,3B, a receita otimizada para CORE alcançou **0,2727 ± 0,0128**, contra **0,2609 ± 0,0058** do random uniform, **0,2567 ± 0,0081** do RegMix e **0,2470 ± 0,0019** do DCLM-Baseline.
- No proxy de 125M, a receita CORE chegou a **0,1142 ± 0,0023**, contra **0,1041 ± 0,0125** do random uniform.

### Limitações

- Só foi demonstrado em uma família de modelos pequenos/médios, de 125M a 1,3B.
- O corpus de base foi um único corpus, NVIDIA ClimbMix.
- Não há evidência de que o próprio agente decida autonomamente quando deve alterar o processo de busca, trocar a métrica ou modificar o evaluator.
- Portanto, isso é melhor descrito como **auto-research do pipeline de dados**, não como RSI forte.

### Padrão reutilizável

```text
artefato executável candidato
        ↓
proxy barato
        ↓
feedback quantitativo
        ↓
iterar programa
        ↓
validar em escala maior
        ↓
promover receita vencedora
```

Para agentes próprios, o mesmo padrão pode ser aplicado a routing, retrieval, políticas de tool-use e workflows: procurar primeiro num proxy barato e só promover depois de validação externa.

---

## 2. SIFT — self-modification fica mais barata quando o evaluator caro deixa de ser chamado em cada candidato

**Fonte principal:** https://arxiv.org/abs/2609.19526

SIFT mantém o padrão de coding agents que modificam seu próprio harness, mas ataca o principal gargalo prático: cada candidato normalmente precisa ser executado em tarefas reais para estimar se ficou melhor.

O sistema adiciona um **LLM judge pairwise**. Cada novo patch é comparado com versões existentes; os wins/losses são agregados por um modelo Bradley–Terry e transformados em um strength score. Esse sinal barato orienta a busca em árvore; avaliações downstream caras ficam reservadas para os candidatos mais promissores.

```text
harness atual
   ↓
self-improver gera patch
   ↓
LLM judge pairwise
   ↓
Bradley–Terry rank
   ↓
priorizar branch
   ↓
avaliação real somente nos candidatos fortes
```

### O que melhora sozinho

O **código do harness do coding agent**. O self-improving model lê código e traces anteriores, identifica fraquezas e escreve um patch que cria o agente-filho.

### O que persiste

Cada nó da árvore representa uma implementação persistente do harness; as versões melhores permanecem no archive e podem gerar descendentes.

### Intervenção humana

O outer loop, benchmark, judge e regras de busca são definidos por humanos. A geração dos patches e a exploração da árvore são automatizadas.

### Métricas

Na versão atual do preprint:

- **35% no Polyglot com o3-mini**.
- **32% no Polyglot com Qwen3-30B**.
- Qwen3: menos de **250 CPU-hours** e cerca de **7 horas de wall-clock**.
- o3-mini: menos de **50 CPU-hours** e cerca de **5 horas de wall-clock**.
- Os autores também reportam transferência dos harnesses descobertos entre diferentes coding models.

### Por que importa

As últimas rodadas vinham apontando o evaluator como gargalo do self-improvement. SIFT reforça uma arquitetura de **duas avaliações**:

```text
muitas candidates
   ↓
evaluator barato / ranking
   ↓
poucas finalists
   ↓
evaluator real caro
```

Isso é diretamente reutilizável para skills, prompts, routing e workflows.

### Limitações

- O judge pode errar; ele não substitui a avaliação real.
- O mecanismo que cria e seleciona o próprio judge não está evoluindo recursivamente.
- Continua sendo evolução de harness, não demonstração de RSI forte em que o melhor agente também melhora persistentemente o mecanismo que produz a próxima geração.

---

## 3. Salesforce — Levels, Ticks and Cascaded Intelligence: persistência comportamental não é automaticamente aprendizado

**Fonte principal:** https://arxiv.org/abs/2609.19519

O trabalho da Salesforce AI Research é especialmente útil para manter a taxonomia do radar limpa. Os autores construíram um agente que operou por **10 dias**, atravessou resets de contexto e sessão, reproduziu um resultado publicado de reinforcement learning e exigiu atenção humana apenas cerca de uma vez por dia.

A arquitetura usa níveis temporais com arquivos bounded, ticks como unidade de ação autônoma e cascaded intelligence, escalando para modelos mais fortes apenas após falhas de review.

### O que persiste

- checkpoints;
- standing decisions;
- directives;
- review findings;
- recipes descobertas;
- regras operacionais;
- em alguns casos, um erro recorrente vira um guard do harness.

Durante a campanha, uma falha de worker virou primeiro uma regra nos briefs e depois um **stop guard no harness**; a falha não voltou a ocorrer.

### Métricas / evidências

- campanha de **10 dias**;
- mais de **200 ticks**;
- cerca de **duas dezenas de escalations humanas**;
- num caso de debugging operacional, step time caiu de **958 s para 180 s** após o driver gerar hipóteses e steering directives;
- o agente atravessou dezenas de resets de driver por compaction, rotation ou watchdog e retomou o trabalho a partir dos artefatos persistentes.

### Classificação correta

Os próprios autores são explícitos: isso é **“accumulation, not learning”**. Pesos não mudam e não existe um learner automático que, com base na métrica de desempenho, faça otimização persistente do harness de forma geral.

Portanto, para este radar, o caso fica principalmente em **(4) memória/contexto persistente**, com alguns patches manuais/operacionais entrando no harness. É uma infraestrutura excelente para continual learning futuro, mas ainda não é continual learning propriamente dito.

### Padrão reutilizável

O ponto importante é arquitetural:

```text
every tick leaves a record
        ↓
every gate leaves a verdict
        ↓
every failure can become a regression case
        ↓
learner futuro entra exatamente nesses gates
```

Isso ajuda a separar duas necessidades:

1. **sobreviver a context reset/process restart**;
2. **otimizar comportamento com base no histórico**.

A primeira é pré-requisito da segunda, mas não deve ser confundida com ela.

---

## 4. Chronicle — replay determinístico como regression gate para agentes que evoluem

**Fonte principal:** https://arxiv.org/abs/2609.20625
**Código:** https://github.com/theagentplane/chronicle

Chronicle não é um self-improving agent. Ele entra aqui porque resolve uma das maiores dificuldades operacionais dos sistemas acima: **como provar que um patch do harness não reintroduziu uma falha observada em produção quando o agente e as tools são não determinísticos?**

Ele grava boundaries não determinísticos como envelopes imutáveis. No `cut-point replay`, algumas boundaries são servidas do registro antigo e outras são executadas com o código novo. Assim, um incidente vira um teste de regressão de CI.

### Métricas

- benchmark com 6 falhas gravadas;
- overhead de gravação: **23 μs por crossing**, equivalente a **0,008%** de uma chamada de modelo assumida em 300 ms;
- full replay: **zero chamadas de modelo** e resultado bit-stable em 20 repetições;
- os cut-point tests falharam no código defeituoso e passaram nas versões corrigidas/benignas nos 6 casos;
- no mutation study, capturaram todos os mutants que deixavam a ação insegura passar, enquanto o baseline que stubava todas as boundaries não capturou nenhum.

### Padrão reutilizável

Para um sistema autoevolutivo:

```text
falha real
  ↓
record trace/boundaries
  ↓
candidate skill/harness/policy
  ↓
cut-point replay
  ↓
regression gate
  ↓
promote / discard
```

Isso torna traces antigos muito mais valiosos: deixam de ser apenas logs e viram **testes executáveis para futuras versões do agente**.

---

## Síntese da rodada

A novidade conceitual mais forte desta rodada é que o espaço de auto-otimização está se ampliando em três direções diferentes:

1. **AutoData:** não apenas código e harness — o próprio algoritmo que escolhe os dados do próximo treinamento entra no loop de busca.
2. **SIFT:** a evolução de harness precisa de um evaluator barato intermediário para ser economicamente escalável.
3. **Salesforce long-horizon:** antes de aprender continuamente, o sistema precisa conseguir operar continuamente sem esquecer; persistência não deve ser confundida com aprendizagem.
4. **Chronicle:** traces históricos podem virar uma suíte de regressão reproduzível, aproximando self-improvement de CI/CD real.

Uma arquitetura resultante ficaria:

```text
Live Execution
    ↓
Trace / Incident Record
    ↓
Credit Attribution
    ↓
Candidate program / skill / routing / harness / data recipe
    ↓
Cheap surrogate / judge / proxy
    ↓
Shortlist
    ↓
Replay + regression gates
    ↓
Real expensive evaluation
    ↓
PROMOTE | DISCARD
    ↓
Persistent version
```

Ainda não há, nesta rodada, evidência nova de **RSI forte**: nenhuma versão melhorada passa a controlar autonomamente e melhorar persistentemente o próprio Evolver, evaluator, política de promoção e processo que produz a geração seguinte.

## Fontes públicas

- AutoData — https://arxiv.org/abs/2609.19754
- SIFT — https://arxiv.org/abs/2609.19526
- Levels, Ticks and Cascaded Intelligence — https://arxiv.org/abs/2609.19519
- Chronicle — https://arxiv.org/abs/2609.20625
- Chronicle GitHub — https://github.com/theagentplane/chronicle
