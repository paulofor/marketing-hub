# Radar de IA autônoma — 2026-08-27

**Horário da rodada:** 21:41 (America/Sao_Paulo)

## Resumo executivo

Há dois avanços novos que passam o filtro nesta rodada. O principal é prático: a Warp publicou em 27/08/2026 uma arquitetura de **Factories as Code** em que o próprio harness/factory é versionado e pode receber diffs sugeridos por agentes a partir de traces, scorers e benchmarks. O segundo é acadêmico: **HypoForge** mostra que diferentes etapas de um agente devem aprender com tipos diferentes de feedback, persistindo novas skills sem alterar os pesos do modelo.

| Caso | (1) Pesos | (2) Código/scaffold | (3) Skills/workflow/RAG | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| Warp Factories | Não | **Sim** | **Sim** | Não | **Sim, gate final** |
| HypoForge | Não | Não | **Sim** | Não | Parcial |
| KOPE | Não | Não | Fronteiriço | **Principalmente sim** | Parcial |

## 1. Warp Factories — harness como objeto explicitamente auto-otimizável

A Warp publicou em **27 de agosto de 2026** a arquitetura de suas novas **Warp Factories**. O ponto central é que toda a fábrica de agentes fica definida em arquivos versionados: `factory.yaml`, definições de agentes, skills, MCP servers, regras de model routing, automações, benchmarks e scorers. A própria Warp afirma que essas definições devem ser **editáveis por agentes**, formando a base do self-improvement.

### Classificação

- **(2) mudança persistente de código/scaffold/harness:** sim.
- **(3) mudança persistente de skills, contexto, routing, configuração de ferramentas/MCP:** sim.
- **(1) atualização de pesos:** não descrita nesse loop.
- **(5) condução humana:** presente no gate final de revisão/merge.

### O que o sistema melhora sozinho

As execuções geram traces e telemetria. **Scorer agents** avaliam essas execuções em dimensões como correção, custo, eficiência e qualidade. Um **self-improvement agent** recebe lotes de execuções avaliadas, procura padrões em sucessos e falhas e gera um **diff contra a própria definição da factory**.

Fluxo:

`agentes executam → traces + interação humana → scorers atribuem notas → improvement agent encontra padrões → altera modelo/skill/context/configuração → gera PR → humano revisa → merge → próximas execuções usam a nova configuração`

A Warp também introduz benchmarks de configurações concorrentes. É possível executar tarefas representativas em paralelo usando configurações diferentes — por exemplo, modelos ou regras de routing distintos — e usar os mesmos scorers para comparar os resultados. Um agente pode então sintetizar o benchmark e gerar o diff correspondente à configuração vencedora.

### O que persiste entre execuções

Persistem os próprios arquivos da factory versionados em Git: skills, definições dos agentes, configurações, routing, MCPs, automações e outros componentes do harness.

### Intervenção humana

A promoção final ainda passa por humano: o improvement agent sugere a alteração, mas alguém revisa e faz merge do PR. Portanto isso ainda não é recursive self-improvement autônomo completo.

### Métricas

A Warp apresenta métricas operacionais agregadas de factories, mas **não publicou ainda uma ablação causal isolando quanto da melhoria vem especificamente do self-improvement loop**. Por isso, nesta rodada, o valor principal é a evidência arquitetural e operacional, não uma métrica causal de ganho acumulado.

### Padrão arquitetural reutilizável

`Factory-as-Code → traces → scorer → observer/improver → candidate diff → benchmark → regression check → PR → approval → nova versão`

Isso se parece com um **GitOps para inteligência do agente**.

### Relação com MCP

MCP faz parte da superfície configurável: a factory pode declarar MCP servers e manter essas configurações no mesmo conjunto versionado que contém skills, modelos e regras do agente. Portanto, no futuro, o sistema pode não apenas aprender **qual ferramenta usar**, mas também melhorar a forma como suas ferramentas ficam organizadas e disponibilizadas ao agente.

**Fonte:** https://www.warp.dev/blog/agent-self-improving-software-factories

---

## 2. HypoForge — skills diferentes aprendem com tipos diferentes de feedback

O **HypoForge**, submetido em **26 de agosto de 2026**, é um framework multiagente para pesquisa científica que mantém o modelo-base congelado. O que muda persistentemente são skills procedurais para geração de hipóteses e testes científicos.

### Classificação

- **(3) evolução persistente de skills/estratégias:** sim.
- **(1) mudança de pesos:** não.
- **(4) mera memória/contexto:** não; as trajetórias são transformadas em novos procedimentos persistentes.
- **(5) condução humana:** parcial, porque datasets, objetivos, ground truth e estrutura de avaliação foram definidos por pesquisadores.

### O que o sistema melhora sozinho

O ponto novo é usar **tipos diferentes de feedback para etapas diferentes**.

Na geração de hipóteses, onde não há naturalmente uma resposta binária de certo/errado, um gerador produz alternativas e um discriminator fornece crítica comparativa. Esse feedback é destilado em uma nova versão da skill de geração.

Na etapa de teste científico, há sinais objetivos: código executou ou não, resultados batem com ground truth, testes estatísticos funcionam etc. As trajetórias são analisadas e as skills de desenho/execução experimental são atualizadas separadamente.

As versões novas só são mantidas quando melhoram o objetivo; caso contrário, a versão anterior continua sendo usada.

### O que persiste

Persistem as novas versões das skills procedurais. O modelo-base continua congelado.

### Métricas

Segundo o paper, no conjunto de teste separado, o HypoForge obteve **Hit@K 0,648**, contra **0,409 sem skill** e **0,497 com skill projetada por humanos**. Na etapa de teste científico, chegou a **0,659**, contra **0,562 sem skill** e **0,612 com skill humana**.

### Padrão arquitetural reutilizável

Não usar um único evaluator para tudo. Separar feedback por natureza da atividade:

- **atividade objetiva** → testes, métricas, resposta da API, estado do ambiente;
- **atividade subjetiva** → critic, preferência, comparação de alternativas;
- **atividade econômica** → custo, conversão, latência;
- **atividade de segurança** → policy checker independente.

Cada tipo de feedback pode alimentar **uma skill distinta**, em vez de pedir ao modelo para “refletir sobre a tarefa inteira e melhorar tudo”.

**Fonte:** https://arxiv.org/abs/2608.25770

---

## 3. KOPE — interessante, mas ainda principalmente memória persistente

O **KOPE**, também de 26 de agosto de 2026, registra decisões, compilação, correção, performance, falhas e alternativas num **Experience Graph Memory**. Depois recupera experiências relevantes sob orçamento fixo de contexto.

### Classificação rigorosa

- **(4) memória/contexto persistente:** principal classificação.
- **(3) evolução de workflow/estratégia:** ainda não suficientemente demonstrada.
- **(1) pesos:** congelados.

O ganho é relevante, mas o sistema continua principalmente sabendo mais porque **recorda evidências anteriores**, não porque reescreve persistentemente o mecanismo pelo qual trabalha.

### Métricas

Na ablação completa de 53 operadores, o Experience Graph Memory elevou a taxa de sucesso de **55,2% para 84,6%** e produziu **1,43× de speedup geométrico** nas comparações de timing válidas.

**Fonte:** https://arxiv.org/abs/2608.25570

---

## Conclusão da rodada

O principal alerta é **Warp Factories**, porque transforma o harness em um artefato explicitamente **versionável, mensurável e modificável por outros agentes** — incluindo skills, modelos, contexto, routing, automações e MCP — e adiciona infraestrutura para comparar variantes antes de gerar a alteração persistente.

Ainda não apareceu nesta rodada um novo caso convincente de **(1) atualização autônoma e persistente dos pesos** nem um sistema comercial que feche sozinho todo o ciclo `produção → experimentação → seleção → promoção` sem humano no último gate.

O padrão arquitetural mais promissor continua sendo:

`traces reais → atribuição de falha → proposta de mudança pequena → evaluator externo → benchmark/regressão → versionamento → promoção controlada → próxima execução usa a melhoria`
