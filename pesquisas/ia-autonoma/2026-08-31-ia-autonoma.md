# Radar — IA autoaprendente / agentes que se aperfeiçoam

**Data:** 2026-08-31 17:55 (America/Sao_Paulo)

Há **dois avanços novos e relevantes** nesta rodada. O principal é **EvoUndo**, porque adiciona uma condição que passa a parecer necessária para agentes que modificam persistentemente o próprio harness: uma melhoria só deveria ser promovida se o sistema também souber desfazê-la com segurança em estados diferentes daquele em que ela foi criada. O segundo é **FedEHR-Agents**, que mostra um padrão novo de evolução de prompts/experiência de forma federada: agentes de organizações diferentes melhoram localmente e compartilham somente experiência agregada, não os dados brutos.

## Classificação resumida

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) prompts/RAG/workflows/tools/skills/estratégias | (4) só memória | (5) forte condução humana |
|---|---:|---:|---:|---:|---:|
| EvoUndo | Não como objeto principal | **Sim — superfície alvo** | **Sim — superfície alvo** | Não | Parcial |
| FedEHR-Agents | Não no mecanismo de evolução do agente | Não como foco | **Sim — principal** | Não | Parcial |

## 1. EvoUndo — evolução persistente precisa ser reversível

**Paper:** *EvoUndo: Recoverability-Constrained Self-Evolution for LLM Agent Harnesses*  
**Submetido:** 28/08/2026  
**Fonte:** https://arxiv.org/abs/2608.28363

EvoUndo parte de um problema muito concreto: agentes capazes de modificar **prompts, tools, middleware, configurações, registries de ferramentas, listeners, arquivos e recursos do harness** podem produzir uma alteração que melhora a tarefa agora, mas deixa efeitos persistentes que depois não conseguem ser revertidos corretamente.

O sistema representa cada mutação como quatro componentes: a modificação para frente, um mecanismo de captura do estado anterior (*witness*), um programa de recuperação e um contrato declarando quais efeitos precisam ser restaurados. A mutação candidata não é aceita apenas porque melhora a métrica de capacidade: ela precisa também passar por **verificação de recuperação em estados contrafactuais**, demonstrando que consegue voltar a um estado observacionalmente equivalente ao anterior.

### O que o sistema melhora sozinho

O foco do trabalho não é descobrir uma nova policy melhor, e sim **reparar automaticamente a semântica de rollback de uma modificação criada pelo modelo**. Diante de falha de recuperação, o sistema diagnostica se o problema é falta de informação sobre o estado original, grounding impreciso ou linguagem de recuperação insuficiente e tenta produzir uma recuperação válida sem enfraquecer a mudança de capacidade que estava sendo testada.

### O que persiste

O objeto persistente é a própria mutação do harness — por exemplo, nova configuração, tool, middleware ou prompt — acompanhada de sua semântica de recuperação. A contribuição principal é tornar **recoverability um gate de promoção**, não um procedimento improvisado somente depois de uma regressão.

### Métricas

Em **600 tarefas inéditas de autoevolução one-shot**, os autores encontraram **197 mutações que melhoravam a capacidade mas falhavam na verificação de recuperação**. Com a representação original, reparos convencionais recuperaram **0/197** dessas falhas. Uma auditoria determinística mostrou que apenas 48/197 eram expressáveis na linguagem original; ao ampliar a linguagem de recuperação, o teto empírico subiu para **191/197**. Quando a linguagem existente já era suficiente, fornecer grounding exato do endereço de estado elevou a recuperação de **0/48 para 38/48 (79,2%)**; para as falhas que exigiam linguagem mais expressiva, o novo cálculo de recuperação chegou a **142/143 (99,3%)**. Os resultados principais usam gpt-oss-120b e há uma replicação com Qwen3.8-27B.

### Intervenção humana

Alta no desenho estrutural: humanos definiram o espaço de mutações, equivalência observacional, linguagem de recuperação e protocolo de avaliação. Porém os reparos são sintetizados pelo modelo a partir do diagnóstico/verificador. Portanto isso não é recursive self-improvement aberto; é uma camada de segurança para **(2) e (3)**.

### Por que importa

Nos sistemas que já estamos acompanhando — StarHarness, Warp Factories, JIT-Agent e outros — a arquitetura comum é `candidate diff -> eval -> commit/rollback`. EvoUndo mostra que **“tem rollback” não é suficiente**. Um rollback estático pode falhar porque o estado no momento futuro já não é o mesmo em que a modificação nasceu.

### Padrão arquitetural reutilizável

```text
trace -> proposta de mutação
      -> medir ganho de capacidade
      -> capturar witness do pré-estado
      -> executar mutação
      -> testar recuperação em vários estados contrafactuais
      -> verificar equivalência observacional
      -> [passou] promover versão + recovery program
      -> [falhou] reparar recuperação ou rejeitar mutação
```

Para MCP, isso significa que uma melhoria que altere schema/defaults de uma tool, registre nova tool, troque middleware ou política de retry deveria carregar também uma operação de **undo verificável**. O gate de promoção ficaria então com pelo menos três dimensões: `melhora`, `não regride` e `é reversível`.

## 2. FedEHR-Agents — evolução federada da experiência do agente

**Paper:** *FedEHR-Agents: Federated Agentic Optimization for Automated EHR Modeling*  
**Submetido:** 28/08/2026  
**Fonte:** https://arxiv.org/abs/2608.27856

Este trabalho, de pesquisadores ligados a McGill/Mila, propõe um objeto diferente para colaboração federada: em vez de compartilhar apenas gradientes ou checkpoints, cada hospital mantém um agente que aprende **experiência de modelagem** e o servidor agrega essa experiência em **meta-prompts globais**.

Cada agente local executa pré-processamento e desenvolvimento de modelos, mantém histórico das execuções, recebe feedback de um evaluator específico da tarefa e usa **TextGrad para refinar seus prompts**. Periodicamente, o servidor recebe evidências estruturadas desses agentes, seleciona experiências confiáveis e complementares e destila um meta-prompt global. Esse meta-prompt retorna aos agentes para a rodada seguinte de refinamento local.

### O que o sistema melhora sozinho

Principalmente **prompts, estratégia de pré-processamento/feature engineering e experiência operacional**. A otimização depende do desempenho real dos modelos que cada agente constrói. O sistema usa esse resultado como feedback para refinar a orientação da próxima rodada.

### O que persiste

Persistem a memória histórica local, os prompts refinados e os **meta-prompts globais agregados**. Portanto é claramente **(3)**, e não apenas (4): a experiência altera o procedimento usado nas rodadas futuras.

### Métricas

Na tarefa de mortalidade em 48h, remover a memória derruba AUPRC de **0,236 para 0,223**, remover o evaluator para **0,226**, e substituir a agregação de experiência por simples média de prompts para **0,221**. Ao aumentar a federação de 3 para 20 hospitais, a AUPRC do FedEHR-Agents sobe de **0,236 para 0,252**, enquanto PromptAvg vai de **0,221 para 0,241**.

Em outras tarefas, com 20 hospitais, o sistema atinge AUPRC de **0,236 em ARF-4h**, **0,553 em LOS>7d** e **0,341 em Sepsis**, superando PromptAvg por **0,022, 0,020 e 0,023** respectivamente. Os autores também testam diferentes backbones de LLM.

### Intervenção humana

Humanos definiram tarefas clínicas, protocolo federado, evaluator e mecanismo de TextGrad/agregação. Não é um agente em produção decidindo sozinho modificar todo o seu sistema; é um pipeline de evolução externa bem delimitado.

### Por que importa

A ideia generalizável não é clínica: ela resolve o problema de **como vários agentes podem aprender coletivamente sem compartilhar dados sensíveis ou traces brutos**. O artefato federado passa a ser a experiência destilada.

### Padrão arquitetural reutilizável

```text
Agent A -> execução -> métrica -> refinement local -> experiência A
Agent B -> execução -> métrica -> refinement local -> experiência B
Agent C -> execução -> métrica -> refinement local -> experiência C
                           |
                           v
                agregador com evidência
                           |
                           v
                    meta-skill/meta-prompt
                           |
             ------------------------------
             |             |              |
             v             v              v
          Agent A        Agent B        Agent C
```

Em uma plataforma de agentes com MCP, isso poderia permitir que agentes de diferentes clientes compartilhassem **padrões abstratos de uso de tools, retries, validações e workflows**, sem enviar dados particulares dos clientes. Antes de agregar uma experiência, o servidor precisaria exigir evidência objetiva de que ela melhorou a tarefa local.

## Leitura desta rodada

O **EvoUndo é o alerta principal**. Até agora o padrão dominante era `propor -> testar -> promover -> rollback se der problema`. Este trabalho adiciona uma quarta propriedade importante: **antes de promover, prove que o rollback continuará funcionando fora do estado exato em que a mudança foi criada**.

O **FedEHR-Agents** acrescenta outra direção nova: não apenas um agente aprendendo sozinho, mas **uma população de agentes compartilhando experiência otimizada como artefato federado**, sem precisar compartilhar os dados que geraram a experiência.

Nesta varredura não encontrei um novo deployment comercial comprovado, posterior aos já registrados, que feche de forma autônoma o ciclo `tráfego real -> variante -> A/B -> promoção persistente` sem gate humano. Também não surgiu um novo caso convincente de recursive self-improvement aberto em que o sucessor melhorado passe automaticamente a conduzir a próxima geração de melhoria.