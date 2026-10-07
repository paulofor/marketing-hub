# Radar diário — agentes mais inteligentes | 07/10/2026

## Resumo executivo

Na busca de hoje, os trabalhos mais úteis vieram sobretudo de submissões de 4–5 de outubro. A convergência principal é que o harness precisa externalizar estado operacional, contratos entre agentes, descoberta de capacidades e política de memória.

## 1. StateWise — reparar estado persistente antes da ação

**Tipo:** pesquisa acadêmica / preprint  
**Fonte:** https://arxiv.org/abs/2610.05241

StateWise usa counterfactual replanning por registro para identificar quais registros persistidos realmente influenciam a decisão atual. Registros críticos são revalidados por checagens read-only ou clarificação direcionada, com evidência ligada ao registro e ao escopo. Em 150 casos executáveis com estado persistente corrompido, o paper reporta 93,3% de correção geral contra 38,7% do baseline.

**Aplicação ao AI Hub:** tratar memória operacional como records tipados com fonte, revisão, escopo, validade e dependências. Antes de uma ação, revalidar somente records causalmente críticos, reparar o estado e replanejar.

**Limitação:** revalidar muitos records aumenta custo; o filtro de criticidade precisa ser bom.

## 2. AECP — handoffs entre agentes como artefatos estruturados

**Tipo:** pesquisa acadêmica / preprint  
**Fonte:** https://arxiv.org/abs/2610.06481

AECP exige que agentes colaborem por artefatos estruturados, e não apenas por mensagens livres. O harness fornece findings quando código relacionado é acessado, compara implementação com compromissos de interface registrados e obriga agentes afetados a revisitar acordos alterados. Em Doc2Repo, NL2Repo e CodeProjectEval, os autores reportam +28,2% de test pass rate médio e -16,5% de wall time frente à comunicação livre.

**Aplicação ao AI Hub:** findings, contratos e decisões que governam outro agente devem virar artefatos versionados com produtor, consumidores, evidência e regras verificáveis.

**Limitação:** comunicação totalmente estruturada pode perder descobertas exploratórias; notas livres podem existir como rascunho, mas não deveriam governar outro agente sem promoção.

## 3. MemPilot — memória montada sob demanda

**Tipo:** pesquisa acadêmica / preprint  
**Fonte:** https://arxiv.org/abs/2610.06830

MemPilot questiona a construção query-agnostic de memória. A política decide entre recuperar memória pronta ou voltar ao histórico bruto para curadoria específica, controlando quantidade de evidência, modelo usado e acesso multimodal. O objetivo é otimizar desempenho, custo e latência.

**Aplicação ao AI Hub:** criar um Memory Orchestrator que decida se a abstração persistida é suficiente ou se deve descer até raw evidence e produzir um context package específico.

**Limitação:** o trabalho usa policy treinada por RL e benchmarks multimodais; uma política determinística ou LLM-gated seria um primeiro passo mais simples.

## 4. MCPacific — descoberta de tools por capacidade

**Tipo:** pesquisa acadêmica / estudo do ecossistema MCP  
**Fonte:** https://arxiv.org/abs/2610.05319

MCPacific mapeia 124.267 servidores MCP únicos, 1.328.233 especificações de tools e 58.915 capacidades numa taxonomia hierárquica. Apresentar candidatos por capacidade funcional, em vez de lista plana, melhorou task completion nos quatro modelos avaliados, chegando a +12 pontos percentuais de Pass@0.75 em conjuntos congestionados.

**Aplicação ao AI Hub:** o Capability Registry deveria começar pela capacidade necessária e depois escolher provider/tool por autoridade, confiança, manutenção, custo, latência e escopo. Schemas devem ser carregados sob demanda.

**Limitação:** a taxonomia é amplamente automatizada e sinais de qualidade precisam de validação para decisões críticas.

## 5. Agent Skill Evolution — regras concretas mudam comportamento

**Tipo:** pesquisa acadêmica / estudo empírico  
**Fonte:** https://arxiv.org/abs/2610.04832

O estudo analisa 2.608 pares de revisão de 3.159 Skills. Em 16 modelos open-weight, adicionar uma regra elevou compliance em +0,41 em média; nos quatro agentes, a ação requerida subiu +0,23; em três agentes avaliados por blind judges, a correção final subiu +0,10. Os maiores ganhos vieram de regras que nomeavam comandos ou caminhos concretos antes ausentes. Quando skills são carregadas sob demanda, os agentes preservam em média cerca de metade do ganho de ação; carregar o corpo da skill aumentou os tokens do episódio em aproximadamente 50%.

**Aplicação ao AI Hub:** skills devem ter gatilhos, ações verificáveis, comandos/fontes concretos e completion checks. Skill selection é tão importante quanto o conteúdo da skill.

**Limitação:** skills mais detalhadas aumentam contexto; lazy loading só funciona com um Skill Need Detector competente.

## 6. Attention Tax / Handoff Tax — quando multiagente compensa

**Tipo:** pesquisa acadêmica / modelo + experimento  
**Fonte:** https://arxiv.org/abs/2610.06069

O paper separa attention tax, quando um agente único degrada com contexto longo, e handoff tax, quando a decomposição perde informação. A decomposição passa a valer a pena quando a economia de attention tax supera o handoff tax. Em um experimento de reconciliação de ledger, o modelo previu corretamente o regime em que a decomposição venceria.

**Aplicação ao AI Hub:** o router deve escolher single-agent, coordinator+workers ou amostras paralelas considerando pressão de contexto, profundidade, separabilidade de interface, perda de handoff e correlação de falhas.

**Limitação:** é um modelo estilizado; serve como heurística, não como fórmula pronta.

## Engenharia: Pi 1.0 / Codemode

**Tipo:** engenharia / arquitetura de produto  
**Fonte:** https://lucumr.pocoo.org/2026/10/6/what-is-codemode/

Armin Ronacher descreve o Codemode do Pi 1.0: o harness confiável é separado do ambiente de execução, e um sandbox no lado do harness compõe chamadas de tools por código, inclusive com concorrência e processamento intermediário, sem enviar toda a mecânica para o contexto principal do LLM.

**Aplicação ao AI Hub:** uma camada programável no control plane pode fazer discovery, parallel calls, filtering, projection e aggregation, devolvendo ao modelo apenas o resultado relevante. Isso também reduz a necessidade de embutir toda lógica de orquestração na imagem do worker.

**Limitação:** mais poder no harness exige sandbox, limites, auditoria e cuidados com durabilidade/determinismo.

## Prioridade prática

1. State Record Validation antes de ações dependentes de memória persistente.
2. Artifact Contracts para handoffs entre agentes.
3. Capability Registry organizado por capacidade.
4. Schema/tool loading sob demanda.
5. Memory Orchestrator com fallback até raw evidence.
6. Skill Need Detector + skills concretas e verificáveis.
7. Single/Multi-Agent Router baseado em attention tax e handoff tax.
8. Harness-side programmable orchestration para evitar poluir contexto com operações mecânicas.

## Conclusão

Context engineering não é apenas selecionar texto para colocar no prompt. É decidir qual estado ainda é válido, qual capacidade falta, qual agente deve receber qual compromisso e qual parte do trabalho pode ser executada fora do contexto do LLM.

Para descobrir aquilo que o usuário não colocou no prompt, o fluxo fica: identificar a lacuna; verificar se já existe estado persistido; revalidar esse estado; descobrir a capacidade necessária; escolher provider/tool; decidir se é preciso outro agente; transferir contratos estruturados; e devolver ao modelo apenas a evidência relevante.
