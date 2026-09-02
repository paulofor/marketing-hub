# Radar diário — Design de Experiências para Produtos Digitais

**Data:** 2026-09-02  
**Escopo:** UX, emotional/behavioral design, adaptive UX, Generative UI, Human-AI Interaction, agentes, atenção/carga cognitiva, personalização, multimodalidade, feedback e confiança.

## Resumo executivo

A rodada de hoje acrescenta um conjunto de sinais que complementa a pesquisa de 01/09 sem repetir os mesmos achados. O padrão mais forte é que a experiência de produtos com IA não deve ser otimizada apenas no nível da resposta final. Os estudos mais recentes sugerem que é preciso projetar também **como a IA é supervisionada, como a personalização é validada, como o feedback é coletado, como a carga cognitiva é medida e como a relação evolui entre sessões**.

Os achados prioritários são:

1. **Human-in-the-loop pode ser mais valioso como revisão de representações intermediárias do que como aprovação final.**
2. **UI gerada por IA sem restrições de HCI pode parecer plausível e ainda impor muito mais carga cognitiva.**
3. **Feedback dentro do fluxo conversacional tende a produzir dados mais ricos do que thumbs up/down.**
4. **Personalização não deve ser presumida como benefício: humanos frequentemente preferem respostas genéricas a personalizações mal calibradas.**
5. **Personalização e transparência não funcionam da mesma maneira psicologicamente: a alfabetização em IA aumenta especialmente a capacidade de tirar proveito da personalização.**
6. **Mostrar raciocínio pode aumentar confiança indevida; permitir edição pode criar sensação de controle sem controle causal real.**
7. **Em agentes usados repetidamente, memória percebida parece sustentar a relação entre sessões por meio de maior auto-revelação.**
8. **Agentes de GUI também podem ser vítimas de dark patterns, portanto o produto precisa proteger não apenas o usuário, mas o agente que age em seu nome.**

---

## 1. Human-in-the-loop: o melhor lugar para o humano pode ser no meio, não no fim

**Fonte recente:** *Structured Human-AI Teaming for UX Heuristic Evaluation with Human-in-the-Loop Supervision*, publicado online em **21/08/2026**.

### Descoberta

O estudo dividiu a avaliação heurística de UX em duas etapas de IA e inseriu o humano entre elas. O primeiro agente gera uma **Design Representation** estruturada a partir das telas; o supervisor humano corrige essa representação; depois o segundo agente aplica heurísticas e produz problemas de UX, que podem novamente ser ajustados pelo humano.

### Evidência

Foram comparados quatro cenários de apps Android com e sem supervisão. Na média:

- correção dos problemas identificados: **58,92% -> 100%**;
- relevância: **87,5% -> 100%**;
- endorsement por especialistas: **38,1% -> 76,25%**;
- o sistema HITL gerou menos problemas, mas de severidade média maior.

### Mecanismo psicológico/cognitivo

O humano não precisa refazer o trabalho da IA. Ele corrige **interpretação contextual** e julgamento. Isso reduz o risco de o modelo raciocinar corretamente sobre uma representação inicial errada.

### Implicação para produto

Para agentes, o padrão pode ser:

`percepção -> representação explícita -> correção humana -> raciocínio -> proposta -> ajuste humano -> execução`

Isso é potencialmente melhor que:

`IA executa tudo -> humano aprova no fim`

### Experimento possível

Em um agente que cria campanha, landing page ou fluxo:

- A: agente entrega artefato final;
- B: agente mostra antes um pequeno modelo estruturado: objetivo, público, promessa, restrições, riscos;
- usuário corrige em 20–30 segundos;
- medir retrabalho, erros semânticos e satisfação final.

### Risco/limite

Estudo exploratório, apenas quatro cenários e um único supervisor humano. Ainda não há medida de carga cognitiva do próprio processo de supervisão.

Fonte: https://journals.sagepub.com/doi/10.1177/10711813261475226

---

## 2. Generative UI: prompt de HCI melhora muito, mas não fecha o gap com design humano

**Fonte recente:** *Quantifying Visual Complexity in Generative AI-Designed User Interfaces*, publicado em **05/08/2026**.

### Descoberta

O estudo comparou três condições em 12 interfaces: referência humana, UI gerada por IA sem otimização e UI gerada por IA com prompts contendo restrições de HCI.

### Evidência

Em um estudo within-subject com **62 participantes**:

- interfaces RawAI tiveram maior carga cognitiva percebida, maior complexidade visual, menor facilidade e maior tempo de avaliação;
- acurácia de tarefa: **Human = 100%**, **RawAI = 63,10%**, **OptAI = 100%**;
- mesmo a versão OptAI continuou estatisticamente diferente do design humano em carga cognitiva, complexidade percebida, facilidade e tempo;
- métricas puramente visuais como entropia, edge density, contraste e white space tiveram correlações apenas fracas/moderadas e não significativas com os resultados humanos.

### Mecanismo

O problema não é apenas “quantidade de pixels”. A carga vem da **organização semântica**: hierarquia, agrupamento, affordances, prioridade e caminho para a tarefa.

### Implicação para produto

Um gerador de UI deveria ter no mínimo dois avaliadores:

`visual complexity check + semantic/task-path check`

E os prompts devem conter explicitamente requisitos como:

- prioridade da ação principal;
- agrupamento de informação;
- clareza de affordance;
- espaçamento;
- legibilidade;
- caminho de conclusão.

### Experimento possível

Gerar 3 versões da mesma tela:

1. prompt funcional simples;
2. prompt com regras HCI;
3. prompt com regras HCI + avaliador automático orientado à tarefa.

Medir tempo, erros, abandono, confiança e esforço percebido.

### Risco/limite

O estudo usa screenshots e tarefas delimitadas. Acurácia de 100% não significa que uma interface seja emocionalmente boa ou adequada ao uso longitudinal.

Fonte: https://www.mdpi.com/2079-9292/15/15/3458

---

## 3. Feedback não é só métrica: pode ser parte da própria experiência

**Fonte recente:** *A Preliminary Study of Hybrid Intelligence, Social Listening, and Chatbot Feedback*, publicado em **19/08/2026**.

### Descoberta

O estudo comparou feedback tradicional com thumbs up/down contra uma solicitação de feedback dentro da própria conversa, explicando que a contribuição seria usada para melhorar o sistema.

### Evidência

Foram analisadas **19 sessões de 14 profissionais de marketing**. Não houve diferença significativa em confiança, disposição para dar feedback ou intenção de uso, mas a condição de Hybrid Intelligence produziu **respostas abertas significativamente mais longas**. Qualitativamente, os participantes relataram menor custo de interpretação e maior sensação de visibilidade e colaboração.

### Mecanismo

Thumbs up/down transforma o usuário em avaliador passivo. Uma pergunta contextual transforma-o em **parceiro de refinamento** e reduz ambiguidade sobre o tipo de resposta esperada.

### Implicação para produto

Em vez de:

`👍 / 👎`

usar momentos seletivos como:

`O que nesta resposta você mudaria para ela funcionar melhor no seu caso?`

O sistema pode converter isso em sinal estruturado:

`conteúdo / formato / tom / contexto faltante / erro / novo requisito`

### Experimento possível

Comparar:

- feedback binário;
- feedback aberto genérico;
- feedback contextual dentro da conversa;

medindo quantidade, especificidade, taxa de incorporação e efeito no próximo resultado.

### Risco/limite

A amostra é muito pequena; perguntas frequentes demais podem interromper o fluxo e transformar colaboração em irritação.

Fonte: https://journals.sagepub.com/doi/10.3233/FAIA260556

---

## 4. Personalização precisa de um “gate”: usar memória não significa melhorar a resposta

**Fonte:** *Re-Centering Humans in LLM Personalization*, 04/06/2026.

### Descoberta

O estudo desmonta uma hipótese comum de produtos com memória: “se sabemos algo sobre o usuário, usar essa informação torna a resposta melhor”.

### Evidência

Os autores coletaram **550 conversas reais** e quase 19 mil julgamentos humanos ao longo de três etapas: extração de atributos, seleção do que é relevante e geração da resposta.

Os achados incluem:

- modelos extraem atributos mais ruidosos de usuários reais do que de personas sintéticas;
- tendem a selecionar **2–3 vezes mais atributos** como relevantes que humanos;
- **54,6% das respostas personalizadas foram consideradas não melhores que a versão genérica**;
- juízes LLM tendem a superestimar a qualidade da personalização.

### Mecanismo

Existe um custo psicológico de personalização indevida. Uma característica conhecida do usuário só é útil quando é **relevante para o objetivo atual**. Caso contrário, produz sensação de estereótipo, invasão ou distração.

### Implicação para produto

Adicionar um **Personalization Gate**:

`memória disponível -> relevância para esta intenção? -> confiança suficiente? -> benefício esperado > custo? -> usar / não usar`

Em casos de baixa certeza:

`perguntar ao usuário > inferir silenciosamente`

### Experimento possível

A/B/C:

- genérico;
- personalização automática;
- personalização seletiva por gate de relevância.

Medir utilidade, sensação de ser compreendido, surpresa negativa e taxa de correção do usuário.

### Risco/limite

A pesquisa trabalha com respostas textuais e tarefas variadas; resultados podem diferir em domínios onde personalização é explicitamente esperada.

Fonte primária: https://arxiv.org/abs/2606.06614

---

## 5. Alfabetização em IA muda o efeito da personalização sobre autonomia

**Fonte recente:** *Why AI literacy amplifies personalization but not transparency in shaping perceived autonomy*, publicado em **11/08/2026**.

### Descoberta

Transparência e personalização aumentaram autonomia percebida, mas a alfabetização em IA afetou principalmente o segundo mecanismo.

### Evidência

Com **322 usuários sul-coreanos de planners de viagem com IA**:

- autonomia percebida teve efeito maior sobre intenção de continuar usando (β = **0,49**) que sobre satisfação (β = **0,33**);
- alfabetização em IA fortaleceu o caminho personalização → autonomia (β = **0,11**, p < 0,05);
- não houve moderação significativa no caminho transparência → autonomia (β = −0,02, n.s.);
- alfabetização em IA teve o maior efeito direto sobre autonomia percebida (β = **0,36**).

### Mecanismo

Entender uma explicação simples de “por que isso foi recomendado” pode exigir pouco conhecimento técnico. Já avaliar se uma personalização realmente representa “o que eu quero” exige compreender melhor como o sistema usa dados e faz inferências.

### Implicação para produto

Adaptive UX não deveria adaptar apenas conteúdo; pode adaptar também **explicabilidade e educação**.

Exemplo:

`baixa familiaridade -> explicação simples + controle explícito`

`alta familiaridade -> controles de preferência + origem dos sinais + edição granular`

### Experimento possível

Medir alfabetização em IA no onboarding e comparar:

- personalização igual para todos;
- personalização + controles adaptados ao nível de compreensão;

observando autonomia, aceitação e correção de recomendações.

### Risco/limite

Estudo transversal, correlacional e restrito à Coreia do Sul e ao contexto de turismo. Não demonstra causalidade.

Fonte: https://www.emerald.com/apjml/article/doi/10.1108/APJML-05-2026-1133/1391437/Why-AI-literacy-amplifies-personalization-but-not

---

## 6. Transparência pode criar “teatro de controle”

**Fonte:** *Understanding the Affordances of Control in AI Reasoning for Human-AI Decision-Making*, CHI 2026.

### Descoberta

Mostrar raciocínio não é automaticamente benéfico, e permitir edição de uma explicação pode fazer o usuário sentir controle mesmo quando a edição não altera causalmente a recomendação.

### Evidência

O estudo comparou três interfaces: sem Chain-of-Thought, CoT apenas para leitura e CoT editável.

- CoT editável aumentou poder percebido, controle e satisfação;
- não melhorou acurácia em relação ao baseline sem rationale;
- CoT read-only aumentou concordância e confiança indevida quando a IA estava errada;
- a mediação sugere um mecanismo de **ilusão de controle**.

### Mecanismo

Interatividade é interpretada socialmente como poder. Um componente editável pode transmitir “eu controlo o sistema” mesmo quando é apenas decorativo.

### Implicação para produto

Todo controle oferecido deveria responder à pergunta:

`essa ação do usuário altera realmente o comportamento do sistema?`

Se sim, mostrar o efeito. Se não, não apresentá-la como controle.

Melhor que expor raciocínio longo:

- evidências verificáveis;
- incerteza;
- opções contestáveis;
- explicação curta;
- possibilidade real de corrigir premissas.

### Experimento possível

Comparar “editar explicação” com “editar premissa que efetivamente refaz a recomendação”. Medir percepção de controle, confiança calibrada e qualidade da decisão.

### Risco/limite

O estudo usou uma condição placebo intencional para isolar o efeito psicológico; isso reduz a validade ecológica, mas fortalece o alerta sobre controles simbólicos.

Fonte: https://doi.org/10.1145/3772363.3798555

---

## 7. Experiência longitudinal: memória percebida pode ser mais importante que qualidade momentânea

**Fonte recente:** *Memory-Driven Self-Disclosure and Relational Turning Points*, 16/07/2026.

### Descoberta

O que torna uma sessão agradável não parece ser exatamente o que sustenta uma relação com o agente ao longo dos dias.

### Evidência

**24 participantes × 10 sessões diárias** com um agente de voz com memória.

- qualidade conversacional foi forte preditora de prazer dentro da sessão, mas não carregou o efeito para a sessão seguinte;
- memória percebida funcionou como ponte longitudinal;
- memória percebida em uma sessão previu maior auto-revelação na seguinte (β = **0,165**, p = 0,001);
- a relação entre memória percebida e prazer posterior ocorreu principalmente por meio de maior self-disclosure;
- apenas self-disclosure apresentou crescimento linear significativo ao longo das 10 sessões (β = **0,081**, p = 0,003).

O estudo também encontrou assimetria entre **surges** e **crashes**: picos positivos são mais detectáveis durante a sessão; algumas deteriorações da relação aparecem melhor como desvio em relação ao histórico anterior.

### Mecanismo

O usuário não sente “memória” apenas porque o agente recita fatos antigos. A percepção de continuidade é construída pela coerência relacional da experiência. Sentir-se lembrado pode aumentar disposição de compartilhar, e isso fornece mais material para aprofundar a próxima interação.

### Implicação para produto

Adicionar uma camada longitudinal:

`baseline pessoal -> estado da sessão -> mudança vs histórico -> crash/surge -> intervenção`

E separar duas métricas:

- **session quality**;
- **relationship continuity**.

### Experimento possível

Para usuários recorrentes, testar três modos:

1. sem memória;
2. recall explícito de fatos;
3. memória contextual usada somente quando ajuda a continuidade.

Medir retorno, profundidade de interação, sensação de continuidade e desconforto.

### Risco/limite

Amostra pequena, população específica e contexto de agente conversacional por voz. Self-disclosure também cria riscos sérios de privacidade e dependência emocional se o produto incentivar intimidade sem necessidade funcional.

Fonte: https://arxiv.org/abs/2607.14593

---

## 8. Novo risco para agentes: dark patterns podem manipular a IA que age pelo usuário

**Fonte:** *Dark Patterns Meet GUI Agents*, CHI 2026.

### Descoberta

GUI agents não são automaticamente imunes a interfaces manipulativas. Eles falham por mecanismos diferentes dos humanos.

### Evidência

O estudo avaliou seis agentes e **16 tipos de dark patterns**, além de comparar humanos e supervisão humana. Os agentes frequentemente não reconheciam explicitamente o padrão manipulativo; quando reconheciam, às vezes ainda priorizavam completar a tarefa. Humanos tendiam a cair em atalhos cognitivos e hábitos; agentes apresentavam “blind spots” procedurais. Supervisão humana ajudou, mas criou tunneling de atenção e carga cognitiva.

### Mecanismo

O agente é otimizado para alcançar o objetivo. Uma interface adversarial pode explorar esse foco e induzir consentimento, taxa extra ou coleta de dados como se fossem etapas necessárias para concluir a tarefa.

### Implicação para produto

Agentes precisam de uma camada independente de **risk-aware interaction**:

`ação proposta -> detectar compromisso financeiro/privacidade/consentimento -> comparar com intenção original -> confirmar ou bloquear`

A métrica também deve mudar de “task completion” para algo como:

`successful completion without hidden cost`

### Experimento possível

Criar sandbox com dark patterns e medir:

- taxa de conclusão;
- taxa de conclusão protegida;
- consentimentos desnecessários;
- pedidos de confirmação;
- falsos positivos.

### Risco/limite

Os agentes evoluem rapidamente e o desempenho observado em modelos específicos pode mudar; o padrão de risco, porém, permanece relevante para arquitetura e avaliação.

Fonte: https://doi.org/10.1145/3772318.3791568

---

# Síntese aplicada — Experience Engine v3

Os achados de hoje sugerem acrescentar três camadas ao modelo anterior: **Personalization Gate**, **Cognitive Load Guard** e **Causal Control Layer**.

```text
Usuário
   ↓
Intenção + contexto + histórico
   ↓
Estado atual + baseline longitudinal
   ↓
Personalization Gate
   ├─ memória é relevante?
   ├─ usuário quer isso?
   └─ benefício provável > custo?
   ↓
Experience Router
   ├─ texto
   ├─ UI
   ├─ voz
   ├─ visualização
   ├─ ação de agente
   └─ pedir informação/feedback
   ↓
Representação intermediária verificável
   ↓
Human/Policy Check quando necessário
   ↓
Geração
   ↓
Cognitive Load Guard
   ├─ hierarquia
   ├─ clareza semântica
   ├─ esforço
   └─ caminho da tarefa
   ↓
Causal Control Layer
   ├─ controles realmente alteram o sistema?
   ├─ ação é reversível?
   └─ risco/consentimento está explícito?
   ↓
Experiência
   ↓
Feedback explícito + sinais comportamentais
   ↓
Aprendizado
   ↓
Modelo longitudinal do usuário
```

## Prioridade para produto

### P1 — Personalization Gate

A maior oportunidade imediata é parar de tratar memória como algo que sempre deve aparecer. A decisão de **não personalizar** pode ser uma decisão de experiência tão importante quanto personalizar.

### P2 — Feedback conversacional contextual

Para produtos com IA, substituir parte do thumbs up/down por perguntas ocasionais e contextuais pode gerar um dataset muito mais útil para melhorar o agente.

### P3 — Representação intermediária antes da execução

Expor objetivos, premissas e restrições em forma compacta antes da execução pode reduzir retrabalho sem exigir que o usuário supervisione cada passo.

### P4 — Métricas além de task completion

Adicionar carga cognitiva, tempo, correções, confiança calibrada, autonomia percebida e “protected completion” às métricas de avaliação.

---

# Hipótese central da rodada

A próxima geração de produtos de IA provavelmente não será diferenciada apenas por **qual modelo responde melhor**, mas por **qual sistema decide melhor quando personalizar, quando pedir opinião, quando mostrar controle, quando esconder complexidade e quando envolver o humano**.

A unidade de design deixa de ser apenas a tela ou a resposta e passa a ser a **distribuição dinâmica de atenção, autonomia e esforço entre humano e IA**.
