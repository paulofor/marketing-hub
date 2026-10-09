# Radar de agentes inteligentes — 09/10/2026

**Pesquisa:** preprints submetidos em 08/10 e divulgados no arXiv em 09/10/2026. Destaque: detectar obrigações omitidas, manter hipóteses revisáveis e preservar conflitos até a conclusão.

## ObligationGuard — deveres implícitos

Pesquisa acadêmica: https://arxiv.org/abs/2610.11773

Diferencia ações proibidas de ações necessárias que o agente não fez. Em estudo preliminar, 56,92% das trajetórias GLM-5.3 tinham obrigações não cumpridas, contra 30% com ações proibidas. Benchmark ObligationBench: 240 trajetórias; ObligationGuard: 57,52% de recall e 21,67% de exact match.

**Aplicação AI Hub:** Obligation Discovery Guard com fonte, escopo e verificador para cada obrigação; por exemplo, preservar o login legado e executar regressão ao adicionar OAuth. **Limitação:** recall ainda baixo e benchmark controlado.

## Memento 3 — world model revisável

Pesquisa acadêmica: https://arxiv.org/abs/2610.11794

Mantém rulebook de hipóteses e aspectos desconhecidos, compila regras em predições executáveis, faz replay e revê regras diante de erro. Autores reportam sucesso em todos os níveis de 25 jogos públicos ARC-AGI-3 e 44% da contagem de ações humanas.

**Aplicação:** World Model FACT/HYPOTHESIS/UNKNOWN/REJECTED, evidência por hipótese e probes read-only. **Limitação:** jogos não equivalem a sistemas empresariais.

## Accurate but Not Humble — conflito de evidências

Estudo acadêmico/EMNLP: https://arxiv.org/abs/2610.12360

Avalia Identify/Solve/Escalate em conflitos de conhecimento. Quatro agentes frequentemente detectaram conflitos cedo, mas não os levaram até a conclusão, comunicando certeza indevida. 

**Aplicação:** Conflict Ledger OPEN/RESOLVED/ESCALATED persistente, ligado ao Completion Gate. **Limitação:** diagnóstico, não componente pronto.

## OnTrack — monitoramento de trajetórias

Pesquisa acadêmica: https://arxiv.org/abs/2610.12375

Monitoramento de passos e dependências em cerca de 1 ms por etapa. Em SWE-bench, +0,057 AUROC versus similaridade; política experimental poupou 18% do compute de execuções que falhariam, com 5 de 6 abortos corretos.

**Aplicação:** monitorar loops, stalls, tools repetidas e obligations paradas; preferir replanejamento antes de abortar. **Limitação:** seis abortos é amostra pequena.

## Hippocam — consolidar por intenção

Pesquisa acadêmica: https://arxiv.org/abs/2610.12124

Intenções aninhadas mantêm o contexto ativo concentrado no subobjetivo. Objetivos concluídos tornam-se memória condensada; histórico original continua recuperável.

**Aplicação:** handoff por goal/subgoal com resultados e evidência bruta. **Limitação:** abstract não oferece ganho quantitativo.

## MASS — evoluir a topologia multiagente

Pesquisa acadêmica: https://arxiv.org/abs/2610.12176

Evolução de workflows e treinamento em trajetórias autogeradas. Autores reportam 1,2–1,6 vezes mais desempenho por token em quatro benchmarks abertos com Qwen3.6-27B.

**Aplicação:** comparar single agent, planner-worker-verifier e challenger. **Limitação:** método original depende de treinamento e autoavaliação.

## Engenharia: Retool

Post técnico de fornecedor, 06/10: https://retool.com/blog/introducing-agent-harness

Sandbox, identidade virtual de agente e ferramentas governadas; não é avaliação independente. Aplicação: discovery read-only separado de side effects.

## Prioridade

1. Obligation Discovery Guard avaliado em 20 tarefas anotadas.
2. Conflict Ledger persistente com evidências.
3. World Model versionado com hipóteses testáveis.
4. Streaming Monitor como alerta, sem abortos automáticos inicialmente.
5. Memória por subgoal e acesso ao histórico bruto.
6. Router de topologia avaliado por custo e regressão.

**Conclusão:** inferir requisitos implícitos é descobrir não apenas o que falta saber, mas o que ainda é obrigatório fazer. Métricas dos estudos são resultados reportados pelos autores, não reproduzidos nesta rodada.
