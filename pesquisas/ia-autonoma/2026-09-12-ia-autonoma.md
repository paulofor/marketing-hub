# Radar IA Autônoma — 2026-09-12

## Rodada — 18:20 BRT

Há **dois desenvolvimentos recentes que passam o filtro nesta rodada**. Nenhum é um novo deployment comercial no nível de LinkedIn/Tencent/Warp publicado hoje, mas ambos acrescentam mecanismos relevantes que ainda não tinham entrado no radar: **Qiushi Engine**, porque demonstra uma forma de “Research RSI” em que descobertas produzidas autonomamente alteram a geração seguinte de experimentos e de modelos; e **NeoHorse-1**, porque transforma telemetria de um routing harness em um flywheel de pós-treinamento, usando as próprias regiões de deficiência observadas para decidir o que entra na próxima mistura de treino.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) prompts/retrieval/workflows/tools/skills/estratégias | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **Qiushi Engine / Research RSI** | **Sim, nos modelos-alvo** | Não demonstrado no próprio research harness | **Sim — estratégia científica muda entre estágios** | Não | **Sim — metas, recursos e estágios** |
| **NeoHorse-1** | **Sim — principal** | Não demonstrado como autoedição do harness | **Sim — routing/evals determinam o currículo seguinte** | Não | **Sim — pipeline de post-training** |

### 1. Qiushi Engine — a descoberta científica passa a modificar a geração seguinte

O paper **“Data-Efficient Language Modeling: From Frontier Advancement to Principle-Guided Model Improvement”**, submetido ao arXiv em **9 de setembro de 2026**, descreve um programa de pesquisa autônoma de longa duração conduzido pelo **Qiushi Engine** sobre o BabyLM 2026 Strict-Small. O sistema executou pesquisa bibliográfica, formulação de métodos, implementação, treinamento, avaliação, análise de mecanismos e síntese. O processo foi dividido em três estágios: primeiro produzir um modelo frontier; depois investigar experimentalmente *por que* certas técnicas funcionavam; por fim usar os princípios encontrados para alterar o método de treinamento da geração seguinte.

O ponto que o diferencia de um simples sweep automático é que **o resultado científico do estágio anterior muda o espaço de decisões do estágio seguinte**. A conclusão sobre dependências contextuais levou a mudanças concretas em como o texto ficava visível, quais posições recebiam supervisão e quais previsões eram preservadas. Duas seeds da nova continuação superaram a continuação comum partindo do mesmo parent model.

A métrica agregada Overall passou de **42,02 para 42,25** entre as duas gerações. A segunda geração obteve o maior Overall no snapshot público do BabyLM Strict-Small de **8 de setembro de 2026**. O paper também relata controles em que o sistema reviu hipóteses anteriores quando experimentos adicionais mostraram shortcuts ou efeitos confundidores, o que é relevante porque indica que o loop não apenas acumula “memórias positivas”: ele também corrige interpretações científicas anteriores.

**O que persiste entre execuções:** checkpoints treinados, registros experimentais, resultados, hipóteses, princípios científicos e métodos derivados desses resultados. **O que não foi demonstrado:** o Qiushi Engine reescrevendo autonomamente o código do próprio research harness e promovendo uma nova versão de si mesmo.

A intervenção humana continua importante. Os pesquisadores forneceram **objetivos, recursos computacionais e requisitos de estágio**; o Qiushi Engine executou o trabalho científico dentro desse enquadramento. Portanto eu classificaria o caso como **(1) mudança persistente de pesos + evolução persistente da estratégia de pesquisa**, com componente forte de **(5)**. Não é RSI forte no sentido em que o modelo melhorado passa a controlar sozinho o processo que criará sua própria sucessora.

O padrão reutilizável é:

```text
goal + resource bounds
        ↓
autonomous literature/problem framing
        ↓
hypothesis → experiment → evaluation
        ↓
mechanism analysis
        ↓
principle memory
        ↓
next-method design
        ↓
new checkpoint
        ↓
repeat
```

A lição arquitetural é importante para agentes próprios: em vez de guardar apenas “essa variante ganhou”, persistir também **a explicação testada de por que ela ganhou** e usar essa explicação para gerar o próximo espaço de hipóteses.

Fonte principal: [arXiv:2609.10702](https://arxiv.org/abs/2609.10702)

### 2. NeoHorse-1 — o routing harness vira fonte contínua de currículo

O **NeoHorse-1**, submetido em **8 de setembro de 2026**, propõe uma arquitetura em que um routing harness não serve apenas para escolher qual modelo atende cada requisição: ele vira também uma **camada de observabilidade que produz os sinais para a próxima rodada de treinamento**.

Para cada interação, o sistema registra a demanda de capacidade prevista pelo router, o tier selecionado, o modelo efetivamente servido, a trajetória completa, tool calls, observações do ambiente e o resultado. O corpus principal contém da ordem de **10^5 a 10^6 trajetórias geradas pelo harness**. Antes de entrar no treinamento, cada trajetória passa por validação estrutural determinística e avaliação em seis dimensões semânticas: goal attainment, instruction adherence, tool use, evidence consistency, error recovery e termination.

O detalhe mais relevante para self-improvement é a **capability-guided data allocation**. O checkpoint corrente é avaliado numa suite estratificada e disjunta do treino. A partir daí o sistema cria um perfil de deficiência por região de capacidade, tarefa, outcome e tier de routing. A mistura de dados da próxima rodada é então deslocada para regiões em que o modelo está abaixo do esperado, preservando cobertura ampla. O checkpoint treinado volta para o harness; suas novas forças e fraquezas geram as próximas trajetórias e o próximo perfil de deficiência.

O loop fica aproximadamente:

```text
deployed routing harness
        ↓
trajectory + predicted demand + served tier + outcome
        ↓
structural gate + semantic evaluation
        ↓
deficiency profile
        ↓
rebalance next training mixture
        ↓
SFT / on-policy distillation
        ↓
new checkpoint
        ↓
return to harness
        ↓
new capability gaps
```

Os resultados reportados são relevantes: no modelo 4B, a macro-média em onze benchmarks sobe de **58,94 para 64,87**; no 9B, de **65,60 para 69,04**. O paper também mostra que dados vindos do routing harness superam dados agentes sintéticos sob treinamento pareado, com ganho médio de aproximadamente **+6,26 pontos**, reforçando a ideia de que erros, recovery e decisões de ferramentas observados em uso real carregam informação difícil de reproduzir em datasets artificiais.

**O que persiste:** pesos dos novos checkpoints e a distribuição de treino reorganizada a partir da telemetria histórica. **O que não foi demonstrado:** o routing harness alterando autonomamente seu próprio código, sua policy de routing ou seu evaluator. O mecanismo de treino, a teacher policy e os critérios foram projetados por humanos. Portanto a classificação correta é **(1) + (3) + forte (5)**, e não RSI forte.

Os próprios autores chamam o NeoHorse-1 de **“initial prototype”** e dizem que o passo seguinte é estender o processo por sucessivas iterações. Isso é importante: a arquitetura para um flywheel recursivo está presente, mas o paper ainda não demonstra várias gerações consecutivas em que cada sucessora fecha o ciclo automaticamente.

Para agentes com MCP, a ideia mais reutilizável é transformar o próprio **router/dispatcher em sensor de aprendizagem**. Em vez de registrar apenas “tool usada / sucesso”, guardar separadamente:

```text
capacidade estimada
policy/routing escolhido
modelo/tool realmente servido
resultado
custo de recovery
```

Isso permite distinguir “o sistema achou que a tarefa era fácil e falhou” de “o sistema sabia que era difícil, mas foi obrigado a usar uma rota mais fraca por custo/policy”. Essa separação melhora muito o credit assignment da próxima otimização.

Fonte principal: [arXiv:2609.08183](https://arxiv.org/abs/2609.08183)

### Síntese da rodada

A novidade mais útil desta rodada é que os dois trabalhos tratam **a experiência não apenas como memória, mas como variável que muda a próxima distribuição de pesquisa ou treinamento**.

O Qiushi Engine trabalha num nível mais alto: **resultado experimental → princípio científico → nova hipótese/método → próximo modelo**. O NeoHorse trabalha num nível operacional: **tráfego/trajectories → perfil de deficiência → nova mistura de treino → próximo checkpoint**.

Isso sugere uma arquitetura em camadas para agentes próprios:

```text
Execution / Research Trace
        ↓
Attribution + structured evaluation
        ↓
Deficiency / principle extraction
        ↓
choose what should change
        ↓
fast layer: skill / prompt / routing / workflow
        ↓
slow layer: training / distillation / weights
        ↓
held-out evaluation
        ↓
versioned promotion
        ↓
new traces
```

Ainda **não encontrei nesta rodada um novo caso comercial comprovado** fechando automaticamente todo o ciclo `produção → variantes → avaliação → promoção → rollback` sem gate externo, nem um novo exemplo convincente de **recursive self-improvement forte** em que a versão melhorada assuma automaticamente o papel do Evolver que criará a própria sucessora.
