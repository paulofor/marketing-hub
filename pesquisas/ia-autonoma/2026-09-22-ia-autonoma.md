# Radar de IA Autônoma — 2026-09-22

## Rodada 17:48 BRT

Nesta rodada, quatro desenvolvimentos novos passam claramente o filtro do radar: **RRSI**, **Self-Healing Harness**, **MedRSI** e **TimEvolve**. Acrescento **iSDFT** como contraponto de categoria (1): há mudança persistente de pesos, mas o processo continua sendo essencialmente um método de fine-tuning/continual learning desenhado por humanos, não um agente que decide autonomamente como se retreinar.

Não encontrei nesta rodada um novo caso documentado de produção no nível LinkedIn/Tencent/Warp em que o sistema já feche sozinho, em tráfego real, todo o ciclo `experiência → variantes → avaliação → promoção → persistência`. Os avanços novos são, porém, muito fortes nas peças que formam esse ciclo.

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégias | (4) Só memória/contexto | (5) Otimização fortemente conduzida por humanos |
|---|---:|---:|---:|---:|---:|
| **RRSI** | Não | **Sim — principal** | **Sim** | Não | Parcial |
| **Self-Healing Harness** | Não | Harness supervisor fixo; regras evoluem | **Sim — regras comportamentais persistentes** | Não | Parcial |
| **MedRSI** | **Sim, em submodelos especializados; não no backbone LLM** | **Sim — novas tools/código** | **Sim — registry de capacidades** | Não | Parcial |
| **TimEvolve** | Não | Não | **Sim — policy de orquestração** | Memória é apenas auxiliar | Parcial |
| **iSDFT** | **Sim — principal** | Não | Não como artefato externo | Não | **Sim — principal** |

## 1. RRSI — regularizar a evolução do harness para evitar que ele “aprenda o benchmark”

**RRSI: Regularized Recursive Self-Improvement of Agent Harnesses**, submetido em 21 de setembro por pesquisadores de Google Cloud AI Research e colaboradores, ataca uma falha que vem aparecendo repetidamente neste radar: um harness pode melhorar muito no conjunto usado durante a evolução e quase não transferir — ou até piorar — em tarefas realmente novas.

O backbone permanece congelado. O objeto de otimização é o harness inteiro: prompts, control flow, configuração, context management, tools, skills, memória e subagentes podem ser modificados, adicionados ou removidos. O sistema usa feedback das execuções para propor candidatos e escolher qual versão passa a ser o próximo incumbent.

A novidade é tratar a evolução do harness como um problema que também precisa de **regularização**, analogamente ao treinamento de modelos. No lado da proposta, RRSI reduz gradualmente a quantidade de mudanças que podem ser agrupadas numa única candidata, mantém histórico explícito do que já funcionou ou falhou e força exploração de componentes pouco testados quando a busca estagna. No lado da seleção, um critic rejeita leakage/benchmark-specific hacks; uma margem de ruído impede promover vencedores estocásticos; ganhos precisam justificar custo adicional; e componentes que deixam de provar utilidade podem ser podados.

Nos oito benchmarks avaliados, RRSI ganhou até **14,1 pontos** no split usado para evolução e até **4,7 pontos** nos benchmarks OOD. No cenário de workspace agents, o harness base tinha média OOD de aproximadamente 39,7; RRSI chegou a 43,6, enquanto métodos de evolução menos regularizados tiveram ganhos bem menores e alguns terminaram abaixo do harness inicial. O paper também reporta **30% menos policy tokens** que a evolução não regularizada. Em resultados detalhados, SWE-bench Verified ganhou +1,8 ponto mesmo sem ter participado da busca; JobBench/GDPval/APEX-Agents ganharam de +3,5 a +4,7; Frontier-Eng ganhou +4,3 Medal points; nenhum held-out regrediu.

**Classificação:** principalmente **(2)+(3)**. O modelo base fica congelado; o que persiste é o harness modificado.

**Intervenção humana:** relevante, mas não em cada edição. Humanos definem benchmarks, budgets, regras do critic/pruner e critérios de seleção. A geração e seleção iterativa de mudanças de harness é automatizada dentro desse desenho.

**Padrão reutilizável:** não basta `candidate → score → promote`. A busca precisa carregar explicitamente uma memória de hipóteses aceitas/rejeitadas, limitar a complexidade incremental e exigir que cada mecanismo continue “pagando aluguel” em ganho real. Para agentes próprios, isso sugere uma regra: quanto mais longa a história de autoevolução, **menores, mais atribuíveis e mais transferíveis devem ser as mudanças aceitas**.

Fonte: https://arxiv.org/abs/2609.24972  
Código: https://github.com/google-research/rrsi

## 2. Self-Healing Harness — persistência como “admission control” de auto-modificações

**Self-Healing Harness for Runtime Oversight of Agent Self-Modification**, também de 21 de setembro, é talvez o paper mais diretamente aplicável a um agente rodando continuamente. A proposta é separar duas autoridades: o agente pode **inventar sua própria correção**, mas um runtime externo decide se essa correção adquire autoridade persistente entre execuções.

O loop é `Detect → Notice → Heal → Validate`. O sistema detecta stall/regressão a partir da trajetória de scores, escreve a evidência num workspace externo, deixa o próprio agente formular uma regra candidata e testa essa regra contra o caso que motivou a mudança **e contra casos protegidos que antes funcionavam**. A regra só passa de provisória para persistente se melhorar a falha-alvo sem regressão acima da margem aceita. Quando replay não é possível, o sistema usa forward trials, explicitamente tratados como evidência mais fraca. Um guard adicional reavalia o conjunto acumulado de regras, porque interações entre regras individualmente boas podem criar regressões novas.

O resultado mais importante é quantitativo: o gate rejeitou **383 propostas decididas por replay**; em **211 delas (55%)**, a regra realmente corrigia a falha que a havia motivado, mas degradava pelo menos um caso que já funcionava. Ou seja, um loop baseado apenas em “consertou o erro original?” teria promovido centenas de mudanças ruins. Em 16 pares Baseline/Harness sobre AppWorld, Terminal-Bench e τ²-Bench, o task-completion score foi maior com Harness em todos os pares; repeated-trial reliability foi maior em 12 e empatou em 4. Só dois intervalos do score primário excluíram zero, portanto boa parte do ganho é direcional e não deve ser superinterpretada. No maior caso, AppWorld com gpt-terra subiu de 0,701 para 0,723 e pass@1 de 0,018 para 0,054.

O modelo permanece congelado. As mudanças persistentes são regras comportamentais explícitas num **external rule workspace**. O paper usa um diretório compacto de rule files e mantém notices, rules, traces, replay cases e audit journal fora do contexto até serem explicitamente recuperados.

**Classificação:** principalmente **(3)**; o harness supervisor é fixo, enquanto as regras que induzem comportamento evoluem persistentemente. Não é mera memória porque existe uma função explícita de otimização/admissão: candidatas são testadas e promovidas ou rejeitadas.

**Intervenção humana:** humanos fixam o gate, métricas, margens, protected corpus e ferramentas disponíveis. O agente escreve as mudanças; o runtime decide persistência automaticamente.

**Padrão reutilizável:** tratar qualquer mudança persistente de skill/policy/routing como uma transação com estados `CANDIDATE → TRIAL → ACTIVE | REJECTED | RETIRED`, mantendo a decisão de promoção fora do alcance do próprio agente adaptativo.

Fonte: https://arxiv.org/abs/2609.24130

## 3. MedRSI — experiência gera novas tools, código e até submodelos treinados

**MedRSI: Recursive Self-Improvement for Medical Agents via Clinically Aligned Self-Evolution** é um avanço importante porque o objeto que evolui não é apenas texto. O reasoning model principal e o workflow geral ficam fixos, mas o sistema transforma falhas em **novas capacidades executáveis**: pode gerar código determinístico, compor tools existentes ou treinar novos modelos especializados. Cada candidata é empacotada com código, pesos quando aplicável, schemas de entrada/saída, descrição de chamada, dependências, histórico de desenvolvimento e hash de versão.

O loop tem cinco etapas: diagnóstico, priorização de falhas, reflexão sobre a capacidade ausente, desenvolvimento da candidata e trial/registration. O mecanismo mais interessante é **fast discovery with slow registration**: uma melhoria aparente entra primeiro num experimental pool e só vira parte do stable tool registry depois de mostrar benefício em **três coortes subsequentes** que não participaram da criação da candidata. O ganho médio precisa ultrapassar uma margem e o clinical cost não pode aumentar.

Nos benchmarks públicos, o agente de glaucoma saiu de **56,2% para 94,1% de balanced accuracy** em 20 rodadas; heart disease passou de **56,5% para 82,3%**. No glaucoma, o clinical cost caiu de **12,4 para 2,5 por 100 casos** e erros classificados como sérios/críticos caíram de **9,8% para 1,5%**. Em 20 rodadas, MedRSI propôs **118 tools candidatas**; 41 chegaram ao experimental pool e apenas **16** sobreviveram aos trials e se tornaram capacidades persistentes. O custo, porém, é enorme: **212 accelerator-hours e 1,9 bilhão de reasoning tokens por trajetória**, com 68% do compute dedicado ao treinamento de modelos candidatos.

O sistema ainda demonstrou construção de capacidades não previstas originalmente. Em glaucoma multimodal, a balanced accuracy foi de 79,2% na rodada 0 para **92,1% na rodada 12**. Na previsão de ejection fraction, o nMAE caiu de 0,85 para **0,55**, superando inclusive um regressor task-specific desenhado pela equipe (0,66).

**Classificação:** combina **(2)+(3)** e, em alguns candidatos, também **(1)** — mas é importante distinguir: **os pesos do backbone LLM não mudam**; o que pode ganhar pesos novos são submodelos especializados criados como tools.

**Intervenção humana:** moderada/forte no desenho. Rubricas clínicas, datasets permitidos, evaluator, critérios de registro e limites de segurança são definidos e congelados por humanos. Dentro deles, o agente identifica gaps, especifica, constrói e experimenta novas capacidades.

**Padrão reutilizável:** separar `experimental registry` de `stable registry`. Uma tool nova não deve entrar no agente estável porque venceu um benchmark local; deve provar utilidade em janelas futuras independentes. Esse padrão é diretamente aplicável a MCP: `candidate tool → sandbox → forward cohorts/replay → stable registry`.

Fonte: https://arxiv.org/abs/2609.24838  
Código: https://github.com/ImprintLab/MedRSI

## 4. TimEvolve — resultados futuros reescrevem a policy de orquestração

**When Tomorrow Becomes Today: Self-Evolving Policies for Agentic Time-Series Forecasting**, com autores da Ant International, Tsinghua e CUHK, é um caso muito limpo de **aprendizado persistente fora dos pesos**. O LLM, os modelos numéricos, tools e prompts ficam congelados. Quando o resultado futuro finalmente se torna observável, ele atualiza três partes da policy: **confiança nos experts**, **seleção de agent paths** e **força da intervenção** da rota escolhida sobre o prior numérico.

A estrutura é `predict → commit alternatives → reveal outcome → update policy → next forecast`. Como todas as previsões de experts e caminhos candidatos são registradas antes de o alvo ser conhecido, quando o futuro chega o sistema consegue avaliar não só o caminho selecionado, mas também alternativas não escolhidas. Isso produz um sinal de aprendizado muito mais rico sem anotação humana adicional.

Em oito domínios do Time-MMD, TimEvolve obteve o melhor average rank entre **15 métodos**: **1,250 em MSE e 1,375 em MAE**, liderando ambos os erros em sete dos oito domínios. Comparado ao regime que aprende somente com alternativas escolhidas, usar feedback de todas as alternativas previamente comprometidas reduziu em média **5,0% o MSE e 6,3% o MAE**. O próprio paper mostra que memória episódica existe, mas é auxiliar; a parte que realmente aprende é o estado explícito da policy.

**Classificação:** **(3)**. O que persiste são coeficientes/estatísticas da policy de orquestração; não muda o LLM nem as tools.

**Intervenção humana:** o update rule, features e arquitetura do learner são desenhados por humanos; depois disso, os outcomes observados atualizam automaticamente a policy durante a sequência temporal.

**Padrão reutilizável:** antes de executar, registrar as alternativas candidatas. Quando o outcome chega, pontuar também as alternativas que não foram usadas. Isso converte cada execução real em uma espécie de **full-information feedback** para routing e orchestration. Em agentes com vários MCPs/subagentes, pode significar registrar `candidates considered + selected route + eventual outcome` e usar o resultado para recalibrar `tool trust`, `route score` e `intervention strength`.

Fonte: https://arxiv.org/abs/2609.24862

## 5. iSDFT — avanço de continual learning nos pesos, mas não RSI autônomo

**iSDFT: Information-Proximal Self-Distillation for Continual Learning in LLMs** entra nesta rodada para manter clara a categoria (1). Aqui há realmente **mudança persistente dos pesos do próprio LLM**. O método aprende novas habilidades a partir de demonstrações usando self-distillation on-policy, mas limita a quantidade de informação que o teacher injeta a cada token e mantém uma âncora KL para uma cópia congelada do modelo base, reduzindo drift acumulado.

Em quatro backbones e duas tarefas de especialização, iSDFT superou SDFT convencional em **7 de 8** combinações e empatou na restante. **73%** das avaliações de retenção ficaram dentro de 0,5 ponto do modelo base, contra **52%** no baseline mais forte. Em benchmarks adicionais, os ganhos médios incluíram +11,8 em MATH-500, +9,6 em AIME24 e +9,4 em AMC23.

**Classificação:** **(1)+(5)**. Há aprendizado nos pesos, mas a técnica, schedule de informação, demonstrações e procedimento de treino são fornecidos por humanos. O sistema não observa sua própria operação e decide autonomamente que precisa se retreinar ou como deve modificar seu algoritmo de aprendizado.

**Padrão reutilizável:** se um sistema vier a consolidar experiência do harness nos pesos, não deve apenas treinar no comportamento novo; precisa de um mecanismo explícito de retenção da policy antiga. Em outras palavras, `plasticidade local` e `preservação global` devem ser controles separados.

Fonte: https://arxiv.org/abs/2609.24646

## Síntese da rodada

O avanço conceitual mais importante de hoje é que **self-improvement de harness está começando a adquirir os mesmos mecanismos de disciplina que engenharia de software e machine learning já usam**: regularização, admission control, experimental/stable registries, delayed feedback, replay, protected cases, rollback e retenção explícita.

A arquitetura geral que emerge desta rodada é:

```text
live execution / outcome
        ↓
trace + candidate alternatives
        ↓
credit / failure attribution
        ↓
small candidate change
  ├─ rule / skill / routing
  ├─ harness code
  ├─ new tool
  └─ optional specialized model
        ↓
EXPERIMENTAL / CANDIDATE
        ↓
matched replay + protected cases
        ↓
forward evidence on future cohorts
        ↓
complexity / cost / leakage checks
        ↓
ACTIVE | REJECTED | RETIRED
        ↓
next execution starts from validated state
```

Para uma arquitetura própria, eu extrairia três regras desta rodada. Primeiro, **o agente pode propor a mudança, mas não deve controlar sozinho a decisão de persistência**. Segundo, **mudança local boa não significa mudança global boa**: o dado mais forte de hoje é justamente os 55% de propostas do Self-Healing Harness que corrigiam o caso-alvo e quebravam algo que antes funcionava. Terceiro, **estado comportamental evolutivo deve poder mudar sem rebuild do runtime** sempre que possível: regras, skills, routing, policy state e registry de tools podem ser versionados externamente; nova imagem fica reservada a mudanças reais no core executável.

Ainda não há evidência nova nesta rodada de **RSI forte**, isto é, de uma versão melhorada que também se torne melhor no próprio ato de construir, avaliar e promover sua sucessora e passe a controlar autonomamente o mecanismo de melhoria. RRSI regulariza o evolver; Self-Healing Harness mantém o gate externo; MedRSI mantém evaluator/rubricas e workflow de evolução fixos; TimEvolve mantém o update rule fixo; iSDFT é treinamento humano-orquestrado.