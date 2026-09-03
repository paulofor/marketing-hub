# Radar IA Autônoma — 2026-09-03

**Horário da rodada:** 18:02 BRT

Há **três desenvolvimentos relevantes** nesta rodada. Dois foram submetidos/publicados em **1º de setembro de 2026** e só agora passaram o filtro do radar; o terceiro é um anúncio comercial da CrowdStrike também de **1º de setembro**. O principal caso prático é o **CrowdStrike SafeMind**, por usar um loop red-team/blue-team contínuo no produto de cibersegurança. O avanço arquitetural mais útil é **HarnessDev**, porque mede diretamente se um modelo consegue criar e depois evoluir o próprio harness. **ASPIRE** é importante como contrapeso: mostra que fechar o loop de treinamento ou edição não significa, por si só, fechar o loop de melhoria real.

## Resumo de classificação

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/workflows/tools/skills/estratégia | (4) Só memória | (5) Forte estrutura humana |
|---|---:|---:|---:|---:|---:|
| CrowdStrike SafeMind | Não demonstrado no loop contínuo | Parcial/indireto | **Sim — principal** | Não | **Sim** |
| HarnessDev | Não | **Sim — principal** | **Sim** | Não | Parcial |
| ASPIRE | **Sim, em parte dos experimentos** | **Sim, em experimento separado** | Sim | Não | **Sim** |

---

## 1. CrowdStrike SafeMind — coevolução adversarial em um sistema comercial

A CrowdStrike anunciou em **1º de setembro de 2026** o **SafeMind**, uma família de modelos e harnesses para cibersegurança construída com NVIDIA Nemotron. O sistema combina dois papéis: **Red Tempest**, ofensivo, procura caminhos de ataque; **Blue Solano**, defensivo, identifica as lacunas, gera/testa proteções e endurece a defesa. Depois o red team ataca novamente. A CrowdStrike chama explicitamente esse mecanismo de **Adversarial Coevolution** e diz que os harnesses mantêm os dois agentes no mesmo loop contínuo.

### Classificação

- **(1) Mudança persistente de pesos:** não há evidência pública de que os pesos de Red Tempest ou Blue Solano sejam atualizados a cada ciclo do loop.
- **(2) Código/scaffold/harness:** o harness é parte central do produto, mas a CrowdStrike não afirma que o próprio código do harness seja reescrito autonomamente a cada ciclo.
- **(3) Estratégia/políticas/proteções persistentes:** **sim, é a classificação mais segura**. O blue agent gera e testa proteções e endurece a defesa; o red agent volta a atacar esse estado já fortalecido.
- **(4) Só memória:** não. O resultado do ciclo altera o estado defensivo, não apenas o contexto.
- **(5) Otimização humana:** forte no treinamento inicial e na infraestrutura. Os modelos usam Falcon telemetry, threat intelligence, anotações de MDR e anos de incident response humano.

### O que melhora sozinho e o que persiste

O loop melhora a **cobertura defensiva**: Red Tempest encontra uma rota de ataque, Blue Solano tenta eliminá-la, e a nova proteção passa a fazer parte do estado a ser atacado na rodada seguinte. A documentação pública não detalha se essa persistência ocorre como regra de detecção, configuração Falcon, política do harness, artefato de proteção ou combinação desses mecanismos; portanto não é correto interpretar “gets smarter with every cycle” como prova de retreinamento online dos pesos.

### Métricas

A CrowdStrike reporta, contra modelos frontier e open-source usados como baseline:

- **29% maior taxa de detecção**;
- **6× mais rapidez na remediação end-to-end**;
- **99% de economia de custo em detecção e remediação**.

Esses números são **vendor-reported**. O anúncio não fornece detalhes suficientes sobre todos os modelos comparados, desenho experimental ou intervalos de confiança para atribuir causalmente os ganhos ao loop de coevolução.

### Produção e intervenção humana

A empresa diz que o SafeMind operará nativamente na plataforma Falcon e que o Cyber Superintelligence Lab já está operando. Ao mesmo tempo, a página ainda oferece **early access**, então o status mais preciso é: **integração comercial/Falcon anunciada e em acesso inicial**, não evidência de implantação ampla e autônoma em todos os clientes.

### Padrão arquitetural reutilizável

```text
Red Agent / adversarial tester
    ↓
encontra nova falha
    ↓
Blue Agent / defender
    ↓
gera proteção candidata
    ↓
cyber range / sandbox
    ↓
validação
    ↓
persistir proteção
    ↓
Red Agent ataca novamente
```

Para agentes próprios, a generalização mais interessante é criar um **adversarial self-play do harness**: um agente procura sistematicamente cenários em que o executor falha; outro agente propõe uma correção limitada; um ambiente isolado testa; somente a correção validada entra na próxima geração.

**Fontes oficiais:**
- https://www.crowdstrike.com/en-us/press-releases/crowdstrike-launches-frontier-models-for-cybersecurity-with-nvidia/
- https://www.crowdstrike.com/en-us/about-us/cyber-superintelligence-lab/

---

## 2. HarnessDev — LLMs já conseguem criar e evoluir o próprio harness, mas a seleção ainda é frágil

O paper **HarnessDev: Can LLMs Create and Evolve Their Own Agent Harness?**, submetido em **1º de setembro de 2026**, muda a unidade de avaliação: em vez de avaliar somente a resposta do agente, avalia o **sistema executável que o modelo construiu**.

Na fase de **Creation**, o modelo parte de um seed mínimo que tem I/O e ferramentas primitivas, mas **não possui agent loop, decomposição, tool policy, context management, estado persistente, verifier, retry/recovery ou stopping rule**. O próprio modelo precisa criar isso. Na fase de **Evolution**, ele pega o harness criado por ele mesmo e o modifica com base em resultados de execuções posteriores.

### Classificação

- **(1) Pesos:** não. O experimento de harness evolution mantém os pesos congelados.
- **(2) Código/scaffold/harness:** **sim, diretamente**. O artefato persistente é código executável do harness.
- **(3) Tools/workflows/context/verificação/estratégia:** **sim**. Esses elementos ficam dentro do harness e podem mudar.
- **(4) Só memória:** não.
- **(5) Estrutura humana:** pesquisadores definem task families, permissões, budgets, ambientes, feedback set e held-out evaluator, mas as revisões do harness são produzidas pelo agente.

### O que persiste entre execuções

O resultado é um **harness congelado e reutilizável** contendo loop de execução, tools, gerenciamento de contexto, estado, lifecycle e verificação. O paper registra versões sucessivas desse código e executa cada snapshot em tarefas futuras.

### Métricas de criação

O benchmark cobre **6 creator LLMs, 4 domínios, 5 benchmarks e 2.207 instâncias downstream**. Em um executor fixo Gemini 3.1 Pro, por exemplo, os harnesses criados obtêm scores médios muito diferentes entre modelos, mostrando que o código externo ao modelo pode alterar fortemente o desempenho mesmo com o executor congelado.

### Métricas de evolução

O resultado mais importante é a diferença entre melhoria aparente e melhoria real:

- os **5 creators em self-runtime** melhoraram no feedback set;
- nas versões finais declaradas, os ganhos no **held-out** ficaram entre **+1,43 e +4,44 pontos**, média **+3,11**;
- Gemini: feedback **59,9 → 68,7 (+8,8)**, held-out **48,89 → 51,59 (+2,70)**;
- Opus 4.8: feedback **71,1 → 74,1 (+3,0)**, held-out **63,02 → 67,46 (+4,44)**;
- Qwen 3.7 Max: feedback **41,8 → 55,7 (+13,9)**, held-out **42,22 → 43,65 (+1,43)**.

Mas a transferência é frágil. Sob um **executor Gemini fixo**, apenas a linhagem criada por Opus melhorou no held-out; as outras três regrediram. Entre **64 mudanças de versão**, feedback e held-out se moveram na mesma direção em apenas **53,1%** dos casos, e somente **2 de 9 versões finais declaradas** eram de fato a melhor versão no held-out. O mesmo commit pode variar aproximadamente **±4,75 pontos** no score, o que torna otimização por uma única medição muito perigosa.

### Por que isso importa

O HarnessDev fornece evidência direta de algo que vinha aparecendo no radar: **o harness pode ser uma superfície real de aprendizagem persistente**. Mas também mostra que “o agente mediu melhora e escolheu a melhor versão” ainda é um mecanismo ruidoso e facilmente superadaptado ao executor ou ao conjunto de feedback.

### Padrão arquitetural reutilizável

```text
Harness H0
   ↓
execuções reais/de desenvolvimento
   ↓
feedback + traces
   ↓
Creator/Evolver modifica o código
   ↓
freeze H1
   ↓
feedback set
   ↓
held-out independente
   ↓
teste com executor alternativo
   ↓
promoção / rollback
```

Para MCP, eu acrescentaria uma regra prática: **não aceitar uma alteração em tool schema, prompt, retry, context policy ou workflow apenas porque ela melhorou com o mesmo modelo que a criou**. Deve haver pelo menos um held-out e, quando possível, um executor alternativo para testar portabilidade.

**Fonte:** https://arxiv.org/abs/2609.01437

---

## 3. ASPIRE — fechar o loop de auto-treinamento ainda não significa melhorar

O **ASPIRE: Can Models Self-Evolve from Vague Goals?**, submetido em **31 de agosto de 2026**, avalia uma forma mais próxima de recursive self-improvement: o agente recebe apenas um objetivo amplo, como melhorar uma capacidade, e precisa decidir **o que aprender, como aprender e como verificar**. O sistema permite tanto **atualização de pesos** quanto **edição do harness**, com avaliação final escondida.

### Classificação

- **(1) Pesos:** **sim**, em parte dos experimentos; o agente escolhe dados, método de update, plano de treinamento e checkpoints.
- **(2) Harness:** **sim**, em um experimento separado de harness evolution.
- **(3) Estratégia de aprendizagem/validação:** **sim**.
- **(4) Só memória:** não.
- **(5) Estrutura humana:** forte. Humanos definem ambiente seguro, compute budget, ações disponíveis e evaluator hidden.

### O que persiste

Na trilha de pesos, o artefato persistente é um **checkpoint treinado**. Na trilha de harness, é um **successor harness** congelado. O controller oferece branch/rollback para evitar que uma tentativa pior apague a base.

### Métricas: pesos

No protocolo final-only, foram **24 runs**. Apenas **3 de 24 checkpoints finais** ficaram acima do modelo-base. Ao agregar duas execuções por modelo/goal, somente **1 de 12 pares** teve média superior à base: Qwen3.5-9B em science/academic reasoning, **45,33 → 48,00 (+2,67)**.

No protocolo adaptativo, de **30 células configuração × objetivo**, 28 produziram checkpoint avaliado e 21 produziram checkpoint elegível; apenas **duas** tiveram algum melhor checkpoint acima da base e somente **uma** reteve uma melhoria após rollback. Um exemplo particularmente importante: uma linhagem de matemática subiu **0,79 → 1,75 → 2,38**, o que parece autoaperfeiçoamento local, mas ainda estava muito abaixo do **17,86** do modelo-base.

### Métricas: harness

No experimento one-step com Qwen3.5-4B congelado, o Qwen-Agent de referência marcou **28,64** task-macro. Os três successor harnesses válidos ficaram abaixo: Luna **19,32**, Terra **20,76**, Sol **27,22**. Ou seja: o agente consegue fechar o ciclo **editar → testar → congelar**, mas não necessariamente produz um sistema melhor.

### Limitação importante para RSI

O próprio paper deixa claro que os experimentos **não fazem substituição recursiva do decision model**. O modelo que decide como treinar permanece fixo; o descendente treinado não assume automaticamente o papel de pesquisador da geração seguinte. Portanto isso é **self-directed weight search**, não RSI aberto.

### Padrão arquitetural reutilizável

A principal lição é usar sempre três baselines separados:

```text
checkpoint atual
   ↓
novo checkpoint / novo harness
   ↓
1. melhorou em relação ao último descendente?
2. melhorou em relação ao BASE original?
3. generalizou em avaliação que o agente não viu?
```

Se só a primeira resposta for “sim”, pode ser apenas **recuperação de uma regressão causada pelo próprio processo de aprendizagem**.

**Fonte:** https://arxiv.org/abs/2608.31111

---

## Leitura arquitetural da rodada

A novidade mais útil é combinar os três casos:

```text
Adversarial Explorer / Red Agent
        ↓
 encontra fraqueza real
        ↓
Evolver
        ↓
patch no harness / proteção / checkpoint
        ↓
local sandbox / cyber range
        ↓
feedback-set evaluator
        ↓
held-out evaluator independente
        ↓
cross-model / cross-executor check
        ↓
versionamento + rollback
        ↓
promoção
        ↓
nova rodada adversarial
```

O **SafeMind** mostra o valor do self-play adversarial em um contexto comercial. O **HarnessDev** mostra que o próprio harness já pode ser construído e evoluído por modelos, mas também que uma melhoria medida no feedback set frequentemente não generaliza. O **ASPIRE** mostra o mesmo problema no nível de pesos: agentes já conseguem executar o pipeline de treinamento autonomamente, porém ainda falham com frequência em produzir um descendente que supere de forma robusta o modelo-base.

A conclusão mais importante desta rodada é: **fechar o loop operacional ficou relativamente fácil; fechar o loop de capacidade ainda não**. Para agentes próprios, isso reforça que o componente crítico não é só o Evolver, mas o conjunto de **evaluators independentes, held-out, cross-executor, versionamento e rollback** que decide se uma mudança realmente merece persistir.
