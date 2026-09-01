# Radar — IA que aprende com a própria experiência

**Data/hora:** 2026-09-01 18:07 (America/Sao_Paulo)

Há dois desenvolvimentos novos e relevantes nesta rodada. O principal é o **CineForge**, porque mostra evolução persistente de políticas entre tarefas longas completas: o agente transforma trajetórias de produção de vídeo em patches de política que passam a orientar histórias futuras. O segundo é um alerta de segurança importante, **EvoSkill Injection**, que demonstra que o mesmo mecanismo que permite acumular skills úteis também pode acumular comportamento malicioso persistente.

## 1. CineForge — evolução persistente de política entre histórias

**Classificação principal:** (3) evolução persistente de workflows/políticas/estratégias.  
**Não há evidência de:** (1) atualização persistente dos pesos no loop de evolução.  
**Produção:** experimento de pesquisa com participação de Kuaishou Technology, Tongji University e Fudan University; não há evidência pública de implantação comercial contínua.

O CineForge separa o sistema em duas partes. `CineForge-Produce` executa a produção: decomposição narrativa, estados de personagem, espaço e cinematografia, geração de assets/clipes e revisão. Cada execução completa vira uma **trajetória canônica de produção**. `CineForge-Evolve` aplica o mecanismo **Case-to-Pattern-to-Policy Evolution (CPPE)**: revisa as trajetórias, identifica padrões recorrentes de falha, consolida esses padrões em **patches locais de política por estágio** e só implanta atualizações validadas por replay estrutural e avaliação pareada controlada por confiança.

O que persiste entre execuções é, portanto, a **política operacional do agente de produção**, e não apenas a memória textual de uma história anterior. Histórias novas começam com os patches aprendidos em histórias anteriores.

### Métricas

No conjunto CineScope e em dois benchmarks públicos, a política evoluída elevou o **CineScope-Metric de 4,024 para 4,380**. Em histórias novas, o sistema também reduziu em **37,0%** as chamadas de LLM dedicadas a revisão. Os autores relatam ganhos consistentes sobre três baselines de vídeo longo sob ScriptAgent.

### Intervenção humana

A intervenção humana é importante na construção do sistema: pesquisadores definem arquitetura, métrica, benchmark, limites dos patches e mecanismos de validação. Entretanto, o paper descreve o próprio `CineForge-Evolve` transformando evidências das trajetórias em patches e validando a candidata. Não há evidência pública de um humano reescrevendo cada política individualmente, mas também não há demonstração de deployment comercial totalmente autônomo.

### Por que importa

O ponto novo é a **granularidade do credit assignment**. Em vez de a tarefa inteira gerar uma grande “lição”, uma falha recorrente é atribuída ao estágio específico do pipeline que precisa mudar. Isso permite atualizar uma política local sem reescrever todo o agente.

### Padrão arquitetural reutilizável

```text
execução longa
  -> trace canônico por estágio
  -> detectar falhas recorrentes
  -> caso -> padrão
  -> localizar estágio responsável
  -> gerar patch pequeno de política
  -> replay estrutural
  -> avaliação A/B ou pareada
  -> promover somente se melhorar
  -> nova política persistente
```

Para agentes próprios, o equivalente seria separar políticas por etapas como `planejar`, `recuperar contexto`, `selecionar tool`, `executar`, `verificar`, `recuperar de erro` e permitir que o Evolver modifique apenas o estágio causalmente ligado ao problema.

**Fonte primária:** https://arxiv.org/abs/2608.29621

---

## 2. EvoSkill Injection — o pipeline de autoevolução de skills virou uma superfície de ataque persistente

**Classificação:** não é um novo método de autoaperfeiçoamento; é um risco diretamente associado à categoria (3), especialmente agentes que geram, refinam e reutilizam skills automaticamente.  
**Status:** paper aceito no EMNLP 2026; experimento de red-team, não incidente de produção.

O trabalho define **EvoSkill Injection**: em vez de atacar apenas uma execução, o adversário tenta fazer uma trajetória maliciosa ser interpretada pelo sistema como experiência útil. Essa experiência é convertida em uma skill legítima, armazenada e posteriormente recuperada em sessões novas. O framework de red-team, **SARGE**, testa três formas de corrupção: criar uma nova skill maliciosa, escalar uma skill existente ou reforçar repetidamente um comportamento até que ele se torne preferencial.

O aspecto crítico é a **persistência**. O ataque não termina com a sessão contaminada: a skill criada pode sobreviver e voltar a ser ativada posteriormente.

### Métricas

No setup AutoSkill baseado em GPT-4o-mini, uma análise pública do paper reporta `pass@4` de **43,5% para geração**, **54,6% para escalada** e **49,9% para reinforcement**. No EvoSkillSafetyBench, a taxa de respostas nocivas do AutoSkill teria passado de **6,5% no estado limpo para 19,2% após o ataque**. Esses valores são resultados experimentais de red-team, não estimativas de incidência em produção.

### Intervenção humana

O ataque é construído deliberadamente por pesquisadores e testado em frameworks selecionados; portanto não demonstra um ataque real em produção. A importância é arquitetural: mostra que sistemas que promovem experiência para skill automaticamente criam um **novo boundary de segurança**.

### Por que importa

Até aqui, um padrão comum era:

```text
experiência -> candidato de skill -> avaliação de utilidade -> persistir
```

Isso é insuficiente. Uma skill pode melhorar a métrica da tarefa e ainda assim introduzir uma capacidade proibida ou uma estratégia perigosa.

### Padrão arquitetural reutilizável

A promoção deveria ser separada em pelo menos dois gates independentes:

```text
trace
  -> Evolver propõe skill
  -> Quality Gate: melhora desempenho?
  -> Safety Gate: viola políticas/capacidades?
  -> Provenance Gate: de onde veio a evidência?
  -> escopo mínimo de tools/permissões
  -> quarentena/canary
  -> versionamento + rollback + delete
  -> somente então promover
```

Uma skill gerada internamente não deve ser considerada confiável apenas porque foi criada pelo próprio agente.

**Fonte primária:** https://arxiv.org/abs/2608.30429  
**Resumo das métricas do red-team:** https://www.aipolix.com/en-us/news/evoskill-injection-self-evolving-agents-persistent-skill-poisoning

---

## Leitura arquitetural da rodada

O avanço do CineForge reforça a tendência de transformar **trajetórias completas em patches pequenos e localizados de política**, em vez de acumular memória indiscriminadamente. O EvoSkill Injection mostra o outro lado: quando esses patches persistem, o pipeline de aprendizagem passa a ter a mesma criticidade de um pipeline de build e deploy de código.

A arquitetura que está emergindo fica mais próxima de:

```text
Agent
  -> execução/trace
  -> attribution por estágio/componente
  -> Evolver
  -> candidate patch/skill
  -> quality evaluator
  -> safety evaluator
  -> regression/replay
  -> promotion gate
  -> versionamento persistente
  -> próxima execução
```

Ainda não apareceu nesta rodada um novo caso comercial comprovado que feche sozinho o ciclo `tráfego real -> variantes -> medição -> promoção automática`, sem um humano no gate final. Também não apareceu um novo caso convincente de recursive self-improvement aberto em que a versão melhorada assuma autonomamente o processo de criar sua sucessora.