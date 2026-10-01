# Radar diário — agentes mais inteligentes | 2026-10-01

A rodada de hoje traz quatro trabalhos acadêmicos recentes e uma novidade de engenharia especialmente úteis para o AI Hub. A convergência é clara: agentes melhores precisam de um harness que diagnostique o tipo de falha, selecione melhor o contexto, localize onde a trajetória divergiu e separe descoberta proativa de autorização para agir.

## Harness as a Language

**Tipo:** pesquisa acadêmica; revisão de 30/09.  
**Fonte:** https://arxiv.org/abs/2609.26891

O JAZ reduz o núcleo do agente a uma primitiva programável, `invoke`, capaz de chamar o modelo recursivamente e manipular o histórico como estado de programa. No StuLife, supera Letta/MemGPT em 8% na parte mais dependente de recall, com aproximadamente metade do custo; no AppWorld, supera ACE em 4% em continual self-improvement.

**Aplicação:** simplificar o runtime dos workers e manter fora do LLM os componentes determinísticos de verificação, políticas, memória e conclusão. Muitos papéis de agentes podem virar configurações sobre o mesmo substrato em vez de runtimes diferentes.

**Limitação:** o paper demonstra expressividade e eficiência; não prova superioridade em governança ou segurança.

## Harness Evolution as Learning

**Tipo:** pesquisa acadêmica; 29/09.  
**Fonte:** https://arxiv.org/abs/2609.36892

O trabalho modela evolução do harness como aprendizagem e separa três causas de erro: **aproximação** (o harness não consegue representar o comportamento desejado), **generalização** (faltam evidências/interações para aprender a regra correta) e **otimização** (o algoritmo de evolução não encontrou uma boa configuração). Também mostra que aumentar memória não melhora desempenho de forma monotônica.

**Aplicação:** antes de alterar prompt, memória ou skill, classificar a falha. Se uma restrição não pode ser imposta pelo harness, o problema é de aproximação; se o requisito não foi descoberto, é de generalização/contexto; se requisito e mecanismo existem mas a política escolhida é ruim, é de otimização. Isso evita tratar toda falha com “mais prompt”.

**Limitação:** o benchmark é orientado a personalização, embora o modelo conceitual seja mais geral.

## Hindsight-Divergence Localization

**Tipo:** pesquisa acadêmica; 29/09.  
**Fonte:** https://arxiv.org/abs/2609.36864

O HDL usa o resultado final para localizar decisões anteriores onde valeria explorar alternativas. Ele preserva o prefixo da trajetória e gera continuações a partir do ponto de divergência. Os autores reportam até 2,5x menos tokens gerados, 1,8x de speedup no rollout e ganhos de até 12,5 pontos em tarefas de agentes.

**Aplicação:** no self-improvement do AI Hub, localizar a primeira decisão realmente consequente, preservar tudo que já estava correto e fazer replay apenas de alternativas a partir dali. Isso é melhor que reescrever uma skill inteira após uma falha.

**Limitação:** HDL foi proposto para RLVR; o uso em replay de harness é uma extrapolação arquitetural.

## ATTUNER

**Tipo:** pesquisa acadêmica / context engineering; 29/09.  
**Fonte:** https://arxiv.org/abs/2609.36722

ATTUNER estuda agentes que reutilizam skills, documentos, memória e código via caches independentes. O achado mais útil é que esses artefatos continuam bem representados; a degradação aparece principalmente quando o modelo precisa **selecionar entre vários artefatos**. Com adaptação no lado da query, treinando menos de 0,05% dos parâmetros, o método chega à qualidade próxima de full-context prefill e até 3,73x de speedup em sete benchmarks.

**Aplicação:** acrescentar uma camada de seleção de artefatos antes do contexto do modelo, usando provenance, escopo, autoridade, dependência da tarefa e relevância da evidência. O problema de memória não é só guardar/recuperar; é escolher corretamente entre memórias plausíveis.

**Limitação:** o mecanismo neural exige adaptação do modelo; com modelos fechados, o valor imediato é o diagnóstico do gargalo de seleção.

## OpenAI Dots

**Tipo:** engenharia/produto de laboratório; 29/09.  
**Fonte:** https://openai.com/index/introducing-dots/

Os Dots trabalham de forma proativa, mas a pesquisa em background usa ferramentas conectadas restritas à leitura. Essas ferramentas não podem modificar conteúdo ou executar ações externas; ações com impacto passam por regras, revisão e, quando necessário, aprovação.

**Aplicação:** separar no AI Hub um **Discovery Mode** read-only, amplo e proativo, de um **Actuation Mode** para alterações. Isso permite que o agente procure contexto ausente continuamente sem receber automaticamente a mesma autoridade para agir.

**Limitação:** é documentação de produto da própria OpenAI, não um benchmark independente.

## Prioridade prática

1. Criar um **Failure Diagnostic** com aproximação/generalização/otimização.
2. Separar **Discovery Mode** read-only de Actuation Mode.
3. Criar uma **Artifact Selection Layer** antes de montar contexto.
4. Implementar um **Divergence Point Finder** para replay localizado.
5. Simplificar workers em um runtime programável comum, mantendo governança externa.
6. Medir interferência entre memórias, em vez de assumir que mais memória sempre ajuda.
7. Avaliar self-improvement em casos held-out e regressões.

## Conclusão

O agente mais inteligente não é o que acumula mais regras. É o que consegue descobrir contexto com liberdade, selecionar a evidência certa, diagnosticar por que falhou, localizar onde a trajetória desviou e melhorar sem receber autoridade desnecessária.

Para o AI Hub, a direção que emerge é separar um **plano cognitivo** — hipóteses, descoberta, planejamento e exploração — de um **plano de controle** — permissões, evidência, verificação, promoção de memória e conclusão.
