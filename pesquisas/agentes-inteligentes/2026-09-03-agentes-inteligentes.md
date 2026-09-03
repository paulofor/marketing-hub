# Radar de Agentes Inteligentes — 2026-09-03

## Tema do dia

Como fazer um agente de IA perceber requisitos, riscos, dependências e contexto relevante que o usuário não colocou explicitamente no prompt.

## Ideia central

O ganho de inteligência operacional de um agente não vem apenas de usar um LLM mais forte ou de aumentar o tamanho do prompt. Ele pode vir principalmente do **harness** ao redor do modelo: mecanismos que ajudam o agente a interpretar objetivo, buscar contexto, detectar lacunas, gerar hipóteses, planejar, executar e criticar a própria resposta.

Em outras palavras, o prompt do usuário não deve ser tratado como uma especificação completa. Deve ser tratado como uma expressão parcial de intenção.

## Arquitetura sugerida

```text
OBJETIVO DO USUÁRIO
        |
        v
  INTERPRETER
  entender meta
        |
        v
CONTEXT DISCOVERY
repo / MCP / docs
banco / histórico
        |
        v
    PLANNER
cria hipóteses
acha lacunas
        |
        v
 EXECUTOR / LLM
        |
        v
     CRITIC
"o que ficou
 faltando?"
        |
   problema?
   /      \
 sim      não
  |        |
 loop    resultado
```

## Capacidade-chave: Missing Context Analysis

Antes de executar uma tarefa, o agente pode seguir uma etapa permanente como esta:

1. Determinar o objetivo real do usuário.
2. Identificar informações provavelmente relevantes, mesmo que não tenham sido mencionadas.
3. Buscar essas informações nas ferramentas disponíveis.
4. Identificar pressupostos ocultos.
5. Identificar consequências indiretas da mudança.
6. Considerar casos de erro e integração.
7. Só então produzir o plano de execução.

Uma regra útil para o harness:

> Não trate o prompt do usuário como uma especificação completa. Trate-o como uma expressão parcial de intenção. Sua responsabilidade é descobrir os requisitos implícitos necessários para atingir o objetivo real.

## Onde MCP e ferramentas entram

O MCP Server pode funcionar como a camada de descoberta de contexto. Para uma tarefa de engenharia, por exemplo, o agente poderia consultar automaticamente funções como:

```text
get_project_architecture()
get_module_responsibilities()
get_relevant_documentation(task)
get_recent_decisions(module)
search_similar_implementations(task)
get_database_schema()
get_environment_constraints()
```

Isso permite que um pedido curto, como "adicione login do Google", seja enriquecido automaticamente com arquitetura existente, padrões, decisões anteriores, secrets, testes, persistência e impactos em outros módulos.

## Critic / Reflection

Depois de uma primeira solução, um segundo estágio pode perguntar:

> O que um engenheiro experiente perceberia que o autor desta solução não percebeu?

O critic deve procurar especificamente:

- requisitos implícitos;
- efeitos colaterais;
- dependências ignoradas;
- premissas não verificadas;
- inconsistências arquiteturais;
- problemas de segurança;
- regressões;
- oportunidades de simplificação.

## Geração de hipóteses

Um agente mais forte não apenas executa ordens. Ele formula hipóteses sobre o problema.

Exemplo: diante de "quero aumentar a conversão da landing page", o agente pode levantar hipóteses como:

- proposta de valor pouco clara;
- confiança insuficiente;
- esforço percebido alto;
- desalinhamento anúncio → landing page;
- momento de compra inadequado.

Essas hipóteses não estavam no prompt. Elas foram derivadas do objetivo e das evidências disponíveis.

## Sete capacidades para um AI Hub

1. **Goal inference** — entender o objetivo real.
2. **Context retrieval** — MCP, repositório, documentação, banco e logs.
3. **Gap detection** — detectar o que não foi dito, mas importa.
4. **Hypothesis generation** — criar explicações e caminhos alternativos.
5. **Planning** — organizar investigação e execução.
6. **Execution + tools** — agir no sistema.
7. **Critique / reflection** — revisar lacunas antes de concluir.

## Memória de decisões

Além de RAG, o agente pode manter memória estruturada de decisões importantes. Exemplo:

```text
DECISION MEMORY

Google OAuth:
- backend é autoridade da sessão
- frontend não persiste refresh token
- secrets nunca ficam no repo
- callback de produção usa domínio definido
- testes devem preservar o fluxo de login existente
```

Em tarefas futuras, essa memória pode ser recuperada automaticamente para evitar que o usuário precise repetir contexto.

## RAG versus Harness

RAG ajuda o agente a obter informação.

O harness decide:

- quando buscar informação;
- o que buscar;
- como raciocinar sobre o que encontrou;
- quando agir;
- como verificar o resultado;
- quando tentar novamente.

Uma forma útil de pensar:

```text
LLM
+ memória
+ MCP / tools
+ RAG
+ planner
+ critic
+ loops
+ regras de decisão
+ feedback de execução
= agente mais capaz
```

## Implicação prática para o AI Hub

Um avanço importante seria fazer cada tarefa recebida passar automaticamente por três perguntas antes da execução:

1. **O que o usuário está realmente tentando conseguir?**
2. **O que provavelmente importa e ele não mencionou?**
3. **Onde posso descobrir isso sozinho sem perguntar a ele?**

Essa camada tende a aumentar bastante a qualidade aparente e prática do agente, mesmo sem trocar o modelo base.

## O que o radar vai monitorar

A partir de hoje, este radar deve procurar trabalhos sobre:

- agent harnesses e scaffolds;
- context engineering;
- goal inference;
- missing-context detection;
- proactive agents;
- planning;
- self-reflection e critique;
- memory;
- retrieval / RAG;
- tool use e MCP;
- hypothesis generation;
- test-time compute;
- self-improving agents;
- avaliação de agentes.

Para cada achado relevante, o radar deve separar evidência acadêmica de opinião/engenharia e traduzir o resultado para algo que possa ser aplicado ao AI Hub.
