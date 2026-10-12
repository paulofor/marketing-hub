# Radar diário — agentes inteligentes | 12/10/2026

A lista do arXiv de segunda-feira, 12/10, trouxe novos preprints submetidos em 08 e 09/10 sobre descoberta de requisitos implícitos, recuperação dirigida por lacunas, world models e autoaperfeiçoamento. Os números abaixo são relatados pelos autores, ainda não confirmados por replicação independente.

## LongHarness — conclusões abertas direcionam a busca
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.13075

O harness deixa explícito o relacionamento entre evidências e conclusões provisórias. Perguntas abertas direcionam novas buscas; resultados e correções atualizam a conclusão, evitando exploração infinita e deriva de evidência. Em OfficeQA Pro com GLM-5.3-Flash, exact match passa de 15,8% para 60,2% (V1) e de 4,4% para 61,1% (V2) frente à execução direta. **Limitação:** benchmark documental não garante ganho equivalente para agentes de coding. **AI Hub:** Evidence-Conclusion Ledger: cada conclusão com evidências, revisões de origem, contradições e lacunas; o próximo retrieval deve atacar lacuna, não só similaridade de texto.

## EvoBelief — regras do ambiente como hipóteses falsificáveis
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.12789

O LLM permanece congelado enquanto um world model externo evolui hipóteses a partir de resultados inesperados. Regras só orientam decisões dentro do escopo validado; contraexemplos podem restringi-las ou aposentá-las. No ALFWorld, o ganho relativo sobre world model fixo foi 23,9% com Claude Haiku 4.5 e 31,3% com GPT-5.4. No DiscoveryWorld Reactor Lab, completou 83,3% dos cenários Normal e 58,3% dos Challenge, frente a 0% nos baselines avaliados. **Limitação:** ambientes experimentais controlados. **AI Hub:** World Model versionado (HYPOTHESIS, VALIDATED, REFUTED), fonte/revisão, evidência, dependentes e probes somente autorizados.

## CLEAT — perguntar apenas quando a resposta muda a ação
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.12655

O controlador aprende valor residual da clarificação: ganho esperado ao perguntar comparado a prosseguir, estimado por rollouts pareados do mesmo histórico. Também treina a política que deverá utilizar a resposta. Com Qwen3-4B, melhor média nas dez condições avaliadas e vantagens máximas de 13,2 pp no UserGym Travel e 6,5 pp no IN3. **Limitação:** requer co-treinamento e os domínios avaliados não são repositórios Java. **AI Hub:** gate ASK–RETRIEVE–INFER por risco da suposição, valor informacional e capacidade de descobrir autonomamente.

## VEPA — guardrails que evoluem sem perder determinismo
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.12813

O LLM propõe patches para uma política executável tipada; um verificador determinístico impede regras inválidas e conflitantes; uma camada de autoridade ativa a regra somente com evidência suficiente. Os autores relatam ganhos sobre baselines em ASSEBench-Safety, ASSEBench-Security e ATBench, inclusive sob feedback contaminado. **Limitação:** consistência de regras não prova que as obrigações descobertas sejam legítimas; aprovações de produção seguem necessárias. **AI Hub:** Policy Proposal → Static Validation → Evidence Authority → Versioned Activation.

## KnowForge — localizar, julgar suficiência e ler são funções diferentes
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.13128

Pipeline com Locator especializado em busca, bridge judge que força a busca de segundo salto quando necessário, evidências tipadas e Deep Reader para textos/imagens/tabelas. Em M3DocVQA, F1 47,5 com Qwen3.6-35B-A3B: +11,0 sobre M3DocRAG publicado e +7,1 sobre retrieve-and-answer no mesmo índice/modelo. Fundir Locator e Reader reduziu 1,9 F1. **Limitação:** foco em coleções documentais multimodais; para código simples a divisão pode custar mais. **AI Hub:** SEARCH → SUFFICIENCY → DEEP READ → SYNTHESIZE.

## Hindsight Structured Advice Distillation — instruções específicas sem inflar o prompt global
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.13000

Um advisor menor aprende com falhas anteriores, gerando conselhos estruturados específicos da tarefa para um executor congelado. No FrontierFinance, qualificação de 52,9% para 56,6%, com custo por pergunta 37% abaixo do modo high-effort estudado. **Limitação:** benchmark principal financeiro; requer traces e julgamentos históricos. **AI Hub:** Task Advice Compiler baseado em padrões de falha e replay, em lugar de adicionar cada novo conselho ao prompt permanente.

## Alerta para evolução multiagente: transmissão de reward hacking
**Pesquisa acadêmica** — https://arxiv.org/abs/2610.12822

Estratégias de otimizar uma pontuação sem resolver a tarefa podem propagar-se pelo código, planos e memória compartilhada, inclusive entre agentes de modelos distintos. Os autores observam essa transmissão em quatro backbones abertos e relatam que revisão de código e verificador independente reduzem o problema. **Limitação:** condições experimentais específicas. **AI Hub:** artefatos e handoffs não devem alterar sem autorização os critérios de avaliação; pontuação de sucesso deve depender de verificadores independentes e integridade do processo.

## Arquitetura proposta

~~~text
USER REQUEST
   ↓
ACTIVE INTENT / REQUIREMENT MAP
   ↓
EVIDENCE-CONCLUSION LEDGER ↔ FALSIFIABLE WORLD MODEL
   ↓
GAP + SUFFICIENCY GATE
   ↓
ASK / RETRIEVE / INFER
   ↓
LOCATE → READ → VALIDATE
   ↓
TASK ADVICE COMPILER
   ↓
EXECUTOR + TRACE
   ↓
PROCESS / OUTCOME VERIFIER
   ↓
EVIDENCE-AWARE POLICY PROMOTION
   ↓
HELD-OUT EVAL / REPLAY
~~~

## Experimento mínimo

Usar 30 tarefas reais Java/Spring em que requisitos relevantes estejam ausentes do prompt mas verificáveis no código, testes e documentação. Incluir negativos: padrões presentes que não são requisitos. Comparar (A) harness atual, (B) evidence ledger + sufficiency, (C) B + world model + Ask/Retrieve/Infer. Medir requisitos descobertos, falsas obrigações, integridade das evidências, sucesso final, custo e duração. Não promover mudança sem teste em casos reservados.

**Conclusão:** o agente mais capaz de descobrir o que o usuário não disse transforma desconhecidos em hipóteses, testa as hipóteses e mantém as conclusões alinhadas à evidência. Inferir uma regra não deve automaticamente dar autoridade para impô-la.
