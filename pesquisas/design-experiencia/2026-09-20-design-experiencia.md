# Radar de Design de Experiência — 2026-09-20

## Síntese da rodada

O padrão mais forte de hoje é **diversidade útil sem perder continuidade**. Em interfaces conversacionais, sugestões de próximo passo parecem funcionar melhor quando cobrem intenções realmente diferentes, e não apenas variações de redação. Em personalização longitudinal, um comentário pontual não deveria ter o mesmo peso que uma preferência estável. Em agentes longos, resumir contexto para responder corretamente agora pode apagar distinções necessárias para uma atualização futura. E, em orquestração multi-agent, adicionar agentes só tende a ajudar quando a tarefa tem horizonte longo e dependências esparsas.

---

## 1. Sugestões de continuidade devem cobrir intenções distintas, não sinônimos

### Descoberta

Um sistema de sugestões pós-resposta obteve os melhores resultados quando otimizou simultaneamente a qualidade de cada sugestão e a **cobertura de intenções distintas** do conjunto.

### Evidência

O preprint **Generative Query Suggestion via Intent Coverage and Query-Level Credit Assignment**, submetido em 16/09/2026, foi avaliado em dataset de produção, teste humano e um A/B online de uma semana. Os braços foram alocados em buckets mutuamente exclusivos e randomizados por usuário. A variante proposta teve ganho de CTR de **16,7% sobre o SFT incumbente** (`p=0,003`) e de **3,25% sobre GRPO** (`p=0,02`). Em 150 contextos de avaliação humana pareada, o GSB melhorou em **+0,07** versus GRPO (`p=0,03`). A cobertura de intenção foi de 0,85 para 0,91, embora essa métrica seja parcialmente alinhada ao objetivo de treinamento.

Fonte: https://arxiv.org/abs/2609.19209

### Mecanismo psicológico/comportamental

Opções que representam intenções diferentes reduzem **redundância percebida** e o custo de formular sozinho a próxima pergunta. A interface deixa de perguntar “qual frase você prefere?” e passa a oferecer “qual direção você quer seguir?”.

### Implicação para produto

Depois de uma resposta de Psique/customer-agent, testar poucas sugestões semanticamente ortogonais, por exemplo: **esclarecer**, **comparar**, **verificar evidência**, **executar** ou **aprofundar**. Evitar três chips que são essencialmente a mesma pergunta reescrita.

### Hipótese/experimento

Comparar:
1. nenhuma sugestão;
2. três reformulações semanticamente próximas;
3. três sugestões cobrindo intenções distintas.

Medir clique útil, reformulação manual, continuação da sessão, conclusão da tarefa e abandono. CTR não deve ser o único KPI.

### Riscos e limites

O teste é de um único produto e o paper é preprint. CTR é proxy de engajamento, não satisfação, conversão ou resultado comercial. Sugestões podem virar nudging excessivo se empurrarem o usuário para caminhos convenientes ao produto.

**Card criado:** `sugestoes-follow-up-cobertura-intencao`, coleção `neuromarketing`.

---

## 2. Memória longa precisa distinguir preferência estável de sinal temporário

### Descoberta

O novo benchmark **ReaLMem** mostra que lembrar fatos é mais fácil do que prever escolhas futuras do usuário e propõe dar peso diferente a preferências conforme sua estabilidade temporal.

### Evidência

O trabalho **To Memories and Beyond: From Remembering to Knowing You across Long-Term Multimodal Personal Archives** usa **2.508 sessões multimodais cobrindo mais de seis anos** e 1.629 pares QA construídos a partir de arquivos pessoais reais, com anotações do próprio participante. Os autores organizam a avaliação em três níveis: factual recall, persona inference e predictive personalization. Nos modelos avaliados, o terceiro nível ficou consistentemente em torno de **57,9%–59,2%** no cenário full-context. O módulo ChronoProfiler atribui um score de estabilidade temporal aos atributos para arbitrar preferências conflitantes.

Fonte: https://arxiv.org/abs/2609.19167

### Mecanismo psicológico/comportamental

Uma memória que trata toda menção como igualmente representativa **achata o tempo**. Um interesse pontual, uma preferência antiga e um padrão repetido passam a parecer equivalentes.

### Implicação para produto

O Preference Ledger do Marketing Hub deveria guardar pelo menos:
`valor + contexto + origem + recência + repetição + estabilidade + confiança + revisável`.

Um sinal novo pode superar um antigo, mas não deveria apagar silenciosamente todo o histórico.

### Hipótese/experimento

Comparar personalização baseada em perfil plano versus perfil temporalmente ponderado. Medir correções do usuário, relevância percebida, pedidos de “não era isso” e frequência de preferências revertidas.

### Riscos e limites

É benchmark de memória, não estudo comercial de UX. O dataset é sensível e relativamente concentrado. Inferir estabilidade de preferências pode errar e cristalizar padrões se o usuário não puder revisar.

**Sem card:** reforça cards já existentes sobre escopo/recência de preferência e personalização por memória; criar outra chave seria redundante e a evidência é de benchmark, não de resultado comercial.

---

## 3. Um resumo pode estar correto hoje e quebrar amanhã

### Descoberta

Context compression pode preservar exatamente o que é necessário para responder à pergunta atual e, ao mesmo tempo, eliminar distinções necessárias para uma atualização futura.

### Evidência

O preprint **Correct Now, Insufficient Later: Auditing Update Sufficiency in Context Compression**, submetido em 17/09, cria pares de históricos com a mesma resposta atual, aplica a mesma atualização futura e exige respostas posteriores diferentes. No piloto com 24 pares, seis mecanismos, 12 condições de memória e dois backends, uma estratégia determinística chegou a **96/96** em um backend e **82/96** em outro, enquanto uma memória estruturada obteve **56/96** em uma das condições reportadas. A auditoria encontrou memórias bem formadas, porém semanticamente erradas, e mostrou grande fragilidade a renomeação de identificadores.

Fonte: https://arxiv.org/abs/2609.20045

### Mecanismo psicológico/comportamental

A compressão otimiza relevância para o **presente conhecido**. Como o futuro é desconhecido, apagar aparentemente “detalhes” pode remover justamente a distinção que uma atualização posterior usará.

### Implicação para produto

Para agentes de longa duração, separar:
- resumo operacional;
- fatos/decisões canônicos;
- referências e IDs estáveis;
- fonte bruta recuperável.

Não depender do resumo textual como única memória.

### Hipótese/experimento

Construir testes com histórico comprimido, depois aplicar atualizações que tornam relevantes detalhes antes considerados secundários. Medir resposta correta e capacidade de reconstrução da fonte.

### Riscos e limites

É um piloto sintético e o próprio autor não reivindica generalização para tarefas naturais. Serve como alerta de arquitetura, não como evidência de um método universalmente superior.

**Sem card:** pertence a memória/harness de agentes e não às quatro coleções comerciais atuais.

---

## 4. Harness melhor não é “mais componentes”; é configuração adequada ao modelo e ao orçamento

### Descoberta

Uma grande ablação de harnesses mostra que planejamento, ferramentas e compactação de contexto têm valor diferente conforme a força do modelo e o orçamento de contexto.

### Evidência

**An Empirical Study of Harness Design for Coding Agents**, submetido em 17/09, avaliou **176 configurações** em quatro modelos, SWE-Bench Verified e Terminal-Bench 2.1. O estudo encontrou que context management fica mais valioso com janelas menores; **elision por regra antes de summarization por LLM** foi a combinação mais eficiente entre as estratégias testadas; planejamento ajudou a acurácia de modelos mais fracos, mas em modelos fortes funcionou mais como economia de custo; e ferramentas predefinidas ajudaram modelos menos proficientes em shell, enquanto modelos bons em Bash puderam operar com interface mais simples e barata.

Fonte: https://arxiv.org/abs/2609.20804

### Mecanismo psicológico/comportamental

Do ponto de vista de experiência, confiabilidade percebida emerge do sistema inteiro. Um harness que adiciona componentes indiscriminadamente pode aumentar custo e estado oculto sem elevar qualidade.

### Implicação para produto

No AI Hub, tratar planning, summarization e tool surface como **políticas configuráveis por modelo/tarefa**, não como módulos obrigatórios sempre ativos.

### Hipótese/experimento

Rodar uma matriz pequena por categoria de tarefa:
`modelo × planejamento × compactação × action space`,
medindo sucesso, custo, duração, overflow e número de recuperações.

### Riscos e limites

O domínio é coding agents. Não prova que a mesma combinação é ótima para pesquisa, marketing ou atendimento.

**Sem card:** arquitetura de harness sem coleção adequada.

---

## 5. Multi-agent ajuda principalmente quando a tarefa pode realmente ser dividida

### Descoberta

Mais agentes não significam automaticamente mais inteligência. O benefício aparece sobretudo em tarefas longas com dependências esparsas; em fluxos fortemente sequenciais, um único agente tende a ser melhor.

### Evidência

O preprint **Rethinking Multi-Agent Collaboration: When More Is Less**, submetido em 17/09, compara estruturas single-agent e multi-agent e conclui que os ganhos são condicionados pela **estrutura de dependência da tarefa**. O método SAIGE cria agentes sob demanda em um grafo dinâmico e os autores observam que aumentar indiscriminadamente o pool de agentes ou a profundidade de recursão não melhora consistentemente o resultado.

Fonte: https://arxiv.org/abs/2609.19759

### Mecanismo psicológico/comportamental

Cada novo agente cria custo de coordenação, duplicação de contexto e possibilidade de conflito. Só existe ganho quando a decomposição permite trabalho paralelo ou relativamente independente.

### Implicação para produto

Antes de escolher multi-agent, classificar o workflow:
- **sequencial e fortemente acoplado** → single-agent com tools;
- **longo, decomponível, dependências esparsas** → agentes especializados coordenados;
- **incerto** → começar single-agent e abrir subagentes apenas quando surgir um ramo independente.

### Hipótese/experimento

Comparar single-agent e multi-agent usando as mesmas tarefas e o mesmo orçamento total de tokens. Medir sucesso, custo, tempo, conflitos e retrabalho humano.

### Riscos e limites

É benchmark de arquitetura, não estudo de percepção do usuário. A fronteira pode variar conforme modelos, toolset e tarefa.

**Sem card:** forte para o AI Hub, mas não se encaixa legitimamente nas coleções válidas.

---

## 6. Caso de produto: Generative UI está migrando de “tela fixa” para resposta visual contextual

### Descoberta

No evento **The Next Interface**, realizado em Nova York em 17/09, a equipe de UX do Gemini apresentou o design language Neural Expressive e uma direção de interface na qual a resposta pode assumir imagens, timelines interativas, vídeo e gráficos dinâmicos, em vez de ficar restrita ao texto/chat.

Fonte: https://www.itsnicethat.com/features/the-next-interface-google-gemini-event-partnership-010926

### Evidência

É um **caso de prática de produto**, não um estudo causal. A publicação descreve o sistema e o objetivo de comunicar “thinking, intelligence and dynamism” por animação, tipografia, cor e componentes gerados conforme o contexto.

### Mecanismo psicológico/comportamental

A hipótese de design é reduzir o esforço de traduzir uma intenção complexa para um formato fixo: o sistema escolhe uma representação mais adequada ao conteúdo.

### Implicação para produto

No Marketing Hub, Generative UI deveria escolher entre:
`texto | cards | tabela | timeline | comparação | gráfico | preview visual | ação`,
mas sempre dentro de componentes auditáveis e conhecidos pelo frontend.

### Hipótese/experimento

Para uma mesma tarefa, comparar chat-only contra resposta que seleciona um componente visual adequado. Medir tempo para localizar informação, erros de interpretação, próxima ação e abandono.

### Riscos e limites

Não há evidência apresentada de que a direção visual melhore tarefa, compreensão ou satisfação. Generative UI sem limites pode piorar previsibilidade, acessibilidade e consistência.

**Sem card:** caso de produto promissor, mas ainda sem evidência suficiente para virar regra persistente.

---

## Experience Engine v20

A rodada de hoje sugere acrescentar uma camada de **diversidade controlada e memória temporal**:

```text
usuário
   ↓
INTENT MODEL
   ↓
NEXT-STEP SLATE
   ├── poucas opções
   ├── intenções ortogonais
   └── sem obrigar escolha
   ↓
TEMPORAL PREFERENCE LEDGER
   ├── recência
   ├── repetição
   ├── estabilidade
   └── contexto
   ↓
CONTEXT RETENTION POLICY
   ├── resumo operacional
   ├── estado canônico
   └── fonte recuperável
   ↓
ORCHESTRATION ROUTER
   ├── single-agent se sequencial
   └── multi-agent se decomponível
   ↓
GENERATIVE UI ROUTER
   ↓
resultado + continuidade
+ compreensão + agência
```

O princípio da rodada é: **não confundir variedade com diversidade útil, nem memória com um perfil plano**. A boa adaptação oferece caminhos realmente diferentes, preserva o que pode voltar a importar e escolhe a complexidade de coordenação de acordo com a estrutura real da tarefa.

---

## Cards derivados nesta rodada

Antes de criar cards, foi consultada a versão atual de `harness-library-api/docs/guia-uso-api-cards.md`. As coleções válidas permanecem:

- `video`
- `prazer-audio-visual`
- `neuromarketing`
- `momentos-de-compra-b2c`

### `sugestoes-follow-up-cobertura-intencao`

- **Coleção:** `neuromarketing`
- **Por que importa:** traduz um A/B real de produção em uma decisão diretamente testável no customer-agent: oferecer poucas direções semanticamente distintas, em vez de reformulações redundantes.
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-09-20-sugestoes-follow-up-cobertura-intencao.md`
- **SHA-256:** `f173fe910a81c1dd7f51277766fffbb47094d50156a17c3824422af634642a8a`
- **JSON:** `pesquisas/design-experiencia/cards/2026-09-20-sugestoes-follow-up-cobertura-intencao.json`
- **Estado pretendido:** candidato a `DRAFT`; sem submit-review, ativação ou arquivamento automático.

Nenhum outro card foi criado. ReaLMem reforça ideias já registradas sobre preferência contextual/temporal; os achados de context compression, harness e multi-agent são de arquitetura; e o caso do Gemini ainda não traz evidência comportamental suficiente.
