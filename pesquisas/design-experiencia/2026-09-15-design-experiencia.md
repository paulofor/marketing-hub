# Radar de Design de Experiência — 2026-09-15

## Resumo executivo

A rodada de 15/09/2026 aponta uma mudança importante na forma de desenhar experiências com IA: **não basta gerar uma boa resposta ou uma boa interface; é preciso controlar como alternativas são exploradas, como preferências são inferidas e se o usuário realmente incorporou a informação entregue**.

Três sinais se destacam. Primeiro, um novo trabalho de generative UI mostra que explorar direções de design como uma etapa estruturada separada da geração de código produz diversidade mais controlável do que simplesmente aumentar a temperatura do modelo. Segundo, TraceMind mostra que **conteúdo adotado não é necessariamente conteúdo compreendido** e que sinais de interação ao longo do tempo ajudam a estimar uptake. Terceiro, um trabalho aceito na CoRL 2026 mostra que feedback binário escasso pode ser mal interpretado quando o sistema extrapola automaticamente sua implicação para opções que o usuário nunca avaliou.

O padrão comum é **preservar incerteza útil**: a IA deve distinguir entre “o usuário escolheu isto”, “o usuário entendeu isto” e “o usuário prefere isto de forma geral”. Esses três estados não são equivalentes.

---

## 1. Generative UI melhora quando exploração de design é separada da implementação

### Descoberta

O preprint *Enabling Creative Exploration for Vibe Design Agents*, submetido em 14/09/2026, propõe tornar a direção de design uma decisão intermediária explícita. Em vez de aumentar a temperatura do modelo e variar simultaneamente estética e código, o sistema primeiro gera especificações estruturadas de direção visual com scores de tipicidade; um seletor escolhe uma delas, e só depois o gerador implementa a interface mantendo configurações estáveis.

### Evidência

O trabalho avaliou 168 prompts e 1.255 comparações pareadas por temperatura para cada intervenção. A amostragem estruturada de temas ampliou a cobertura de alternativas e a variação visual observada. Em um experimento online com mais de 300 mil tarefas, houve menos eventos de feedback negativo, mas mais interações de correção; o aumento observado em exportação de código permaneceu estatisticamente incerto.

Fonte: https://arxiv.org/abs/2609.15078

### Mecanismo psicológico/comportamental

Exploração criativa e execução técnica têm objetivos diferentes. Misturar as duas dimensões em uma única variável de aleatoriedade pode produzir diversidade superficial ou erros de implementação. Uma representação intermediária torna a exploração **legível, comparável e selecionável** antes de o usuário investir em refinamento.

### Implicação para produto

Para interfaces, criativos, landing pages ou experiências geradas por IA, usar:

`brief → 3–5 direções coerentes → seleção → implementação → refinamento`

em vez de:

`brief → uma interface → regenerar até gostar`.

Isso também permitiria preservar explicitamente dimensões fixas de marca, acessibilidade e categoria enquanto se explora apenas o que realmente pode variar.

### Hipótese/experimento

Comparar três fluxos: geração única; regeneração por temperatura; exploração estruturada de direções antes da implementação. Medir diversidade percebida, tempo até aprovação, número de correções, abandono e exportação/publicação.

### Riscos e limites

O ganho em exportação de código não foi estatisticamente estabelecido e houve aumento de interações corretivas. O artigo é preprint e ainda não demonstra ganho comercial. Por aderência insuficiente às coleções atuais da Biblioteca, **não virou card**.

---

## 2. Conteúdo usado pelo usuário não significa conteúdo compreendido

### Descoberta

*TraceMind: Predicting User Information Uptake from Low-Cost Interaction Traces during Human-LLM Content Co-Generation*, atualizado em 14/09, estuda a diferença entre uma informação aparecer no produto final e o usuário realmente ter processado essa informação.

### Evidência

O estudo envolveu 62 participantes em três tarefas. Os autores extraíram unidades atômicas de informação dos rascunhos finais e criaram perguntas de reconhecimento pós-tarefa, produzindo 1.187 rótulos de uptake. O sistema TraceMind acompanhou essas unidades no chat e no rascunho e combinou sinais espaciais, temporais e de workflow. Segundo os autores, superou todos os baselines aprendidos em AUROC, AUPRC da classe de não-uptake, balanced accuracy e macro-F1. Engajamento ativo sustentado forneceu informação adicional a sinais isolados.

Fonte: https://arxiv.org/abs/2609.12600

### Mecanismo psicológico/comportamental

Copiar, aceitar ou manter uma informação no artefato pode acontecer com processamento superficial. A incorporação cognitiva é um processo temporal: releitura, edição, referência posterior e interação ativa podem indicar maior probabilidade de que a informação tenha sido efetivamente absorvida.

### Implicação para produto

No Marketing Hub, vale separar:

- `exposed_to_information`;
- `information_present_in_output`;
- `probable_uptake`;
- `confirmed_understanding`.

Para informações críticas — preço, restrições, condições, responsabilidade, próxima ação — o sistema não deveria assumir entendimento apenas porque a mensagem foi mostrada ou incorporada ao texto final.

### Hipótese/experimento

Quando sinais de uptake forem baixos, oferecer resumo curto, confirmação ou revisão contextual antes da próxima ação. Medir erro posterior, retrabalho, abandono, necessidade de suporte e conclusão correta.

### Riscos e limites

Os sinais são probabilísticos e não devem ser tratados como leitura mental. O estudo mede reconhecimento, não compreensão profunda ou conversão. Privacidade e minimização de telemetria são obrigatórias.

### Card

Criado o card `adocao-nao-equivale-compreensao-ai`, coleção `neuromarketing`.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-15-adocao-nao-equivale-compreensao-ai.md`

SHA-256: `c35a8766b067fa3de6e04974685bf28462cf8dfe2670bd56bf30f0ebc1c5f170`

---

## 3. Feedback binário não deveria virar preferência global automaticamente

### Descoberta

O trabalho *Rethinking the Implications of Human Feedback for Preference Learning in Human-Robot Collaboration*, aceito na CoRL 2026 e atualizado em 15/09, questiona uma prática comum: receber um feedback positivo/negativo e inferir automaticamente o que esse feedback implica para ações alternativas que o usuário nunca avaliou.

### Evidência

Em dois ambientes colaborativos simulados, os rótulos de implicação fornecidos pelos participantes frequentemente divergiram da regra fixa usada pelo sistema. Usar os rótulos humanos melhorou substancialmente a aprendizagem de preferência no método PIE. O método proposto, IMPLIED, aprende e revisa essas implicações ao longo do tempo; em trajetórias gravadas e em um estudo físico de preparação de pizza com robô, superou regras fixas e baselines baseados em LLM, aproximando-se de um oracle com rótulos humanos.

Fontes:

- https://arxiv.org/abs/2609.13982
- https://www.people-aligned-robots.com/publications

### Mecanismo psicológico/comportamental

Um feedback é localizado: rejeitar uma resposta pode significar problema de tom, conteúdo, momento, formato ou objetivo. Transformá-lo imediatamente em uma regra geral comprime várias explicações possíveis numa única preferência permanente.

### Implicação para produto

O customer-agent deveria registrar preferência como algo parecido com:

`valor + contexto + escopo + confiança + recência + possibilidade de revisão`

em vez de `usuário não gosta de X`.

Uma rejeição de uma mensagem não deveria, sozinha, banir um tema, formato ou abordagem em todas as interações futuras.

### Hipótese/experimento

Comparar uma política que generaliza cada 👍/👎 imediatamente com outra que mantém hipóteses locais e revisáveis. Medir número de correções posteriores, recomendações rejeitadas, satisfação e necessidade de reexplicar preferências.

### Riscos e limites

O domínio original é colaboração humano-robô; transferir para atendimento digital é hipótese. O resumo público acessível não informa tamanho de amostra, o que reduz a precisão da avaliação da força da evidência.

### Card

Criado o card `feedback-binario-nao-implica-preferencia-ampla`, coleção `neuromarketing`.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-15-feedback-binario-nao-implica-preferencia-ampla.md`

SHA-256: `717a7ec21203f1163263588d7e75982c74ca92420908acf8a8640c4da2941b91`

---

## 4. Generative co-design democratiza personalização, mas os defaults do modelo moldam o que o usuário imagina

### Descoberta

*Personalizing Personal Health Interfaces: Co-Design with Generative AI*, submetido em 14/09 e atualizado em 15/09, estudou 14 participantes redesenhando interfaces do Google Health e Apple Health com Figma Make.

### Evidência

Os participantes criaram interfaces com mais contexto pessoal, planejamento futuro e elementos interativos. Entretanto, designs conversacionais convergiram frequentemente para a convenção de “janela de chat”. O trabalho relata que IA ajudou a materializar ideias ainda pouco articuladas, mas defaults do modelo e latência de geração influenciaram o processo. Interpretabilidade e accountability foram mais facilmente operacionalizadas do que privacidade, confiança e segurança emocional.

Fonte: https://arxiv.org/abs/2609.15046

### Mecanismo psicológico/comportamental

Ferramentas generativas diminuem o custo de expressar uma ideia, mas seus defaults também funcionam como **âncoras de design**. Se o primeiro resultado é um chat, o usuário pode explorar variações do chat em vez de imaginar modalidades inteiramente diferentes.

### Implicação para produto

Um gerador de experiências deveria oferecer deliberadamente primitivas alternativas — painel, fluxo guiado, cards, timeline, voz, formulário progressivo, canvas — antes de fixar a interação numa metáfora conhecida.

### Hipótese/experimento

Comparar co-design livre com uma condição em que o sistema apresenta quatro paradigmas de interação distintos antes da geração. Medir diversidade estrutural das soluções, sensação de autoria e número de decisões revertidas.

### Riscos e limites

Amostra pequena, contexto de saúde e ferramenta específica. Não virou card: a evidência é útil para arquitetura de generative UI, mas ainda é fraca para uma regra persistente do Marketing Hub.

---

## 5. Automação precisa permanecer legível e contestável

### Descoberta

O paper conceitual *Bring Buttons Back: Physical Interfaces for the Age of Automation*, atualizado em 15/09, propõe Physically Stateful Interfaces (PSIs): controles físicos que não apenas enviam comandos, mas também expressam o estado da automação e permitem intervenção direta.

### Evidência

Os autores descrevem três comportamentos fundamentais — Self/Reset, Resist/Hide e Assert/Unhide — para combinar affordance, feedforward e feedback no próprio ponto de controle. O trabalho argumenta que automação invisível enfraquece a ligação entre ação humana e estado do sistema.

Fonte: https://arxiv.org/abs/2609.14684

### Mecanismo psicológico/comportamental

Quando estado e controle ficam separados, o usuário precisa manter um modelo mental abstrato da automação. Controles stateful tornam a autoridade atual e as possibilidades de intervenção perceptíveis no próprio objeto de interação.

### Implicação para produto

O equivalente digital seria evitar automação “fantasma”. Modos como `auto`, `assistido`, `somente sugestão` e `pausado` devem permanecer visíveis, com reversão e override próximos à ação — não escondidos em configurações profundas.

### Hipótese/experimento

Comparar um agente com automação implícita contra outro com estado de autonomia persistente e controles de override visíveis. Medir compreensão do estado, erros de expectativa, reversões e sensação de controle.

### Riscos e limites

É uma proposta conceitual, não uma validação comportamental robusta. Não virou card por falta de evidência empírica suficiente e porque a aplicação é principalmente arquitetura de interação.

---

## 6. Preservar histórico não garante preservar continuidade relacional

### Descoberta

*When AI Companions Disappear*, submetido em 14/09 e atualizado em 15/09, analisou reações de usuários durante a transição regulatória chinesa de serviços antropomórficos de IA.

### Evidência

A análise qualitativa cobriu 89 posts, 1.425 comentários e 2.005 respostas no RedNote. Usuários tentaram reter, migrar e reconstruir seus companions. Um achado importante foi que **preservar registros de conversa não necessariamente restaurava memórias compartilhadas ou a interação familiar**.

Fonte: https://arxiv.org/abs/2609.15482

### Mecanismo psicológico/comportamental

Continuidade percebida depende não apenas de fatos armazenados, mas de padrões de resposta, estilo, convenções compartilhadas e expectativas construídas ao longo do tempo. Um histórico textual pode permanecer intacto enquanto a experiência subjetiva muda profundamente.

### Implicação para produto

Em upgrades de modelo ou migrações de agentes, separar:

- persistência dos fatos;
- compatibilidade semântica da memória;
- estilo/interação;
- convenções aprendidas;
- mudanças deliberadas de comportamento.

Mudanças materiais deveriam ter comunicação explícita, possibilidade de comparar comportamentos e, quando apropriado, rollback ou transição gradual.

### Riscos e limites

Contexto de AI companions e estudo qualitativo em uma plataforma/regulação específicas. A extrapolação para agentes utilitários deve ser cuidadosa. Não virou card porque a coleção atual não representa bem continuidade arquitetural e porque o contexto relacional exige cautela especial.

---

## Experience Engine v15

A rodada sugere acrescentar quatro controles ao modelo acumulado:

```text
usuário
   ↓
intenção + contexto + histórico
   ↓
EXPLORATION POLICY
   ├── gerar direções
   ├── comparar
   └── só depois implementar
   ↓
UPTAKE ESTIMATOR
   ├── exposto
   ├── adotado
   ├── provavelmente processado
   └── confirmado
   ↓
PREFERENCE INFERENCE POLICY
   ├── evidência local
   ├── escopo
   ├── confiança
   └── revisão
   ↓
AUTOMATION STATE / OVERRIDE
   ↓
execução
   ↓
CONTINUITY CHECK
   ├── fatos
   ├── estilo
   ├── convenções
   └── comportamento
   ↓
resultado + compreensão + agência
```

O princípio da rodada é: **não colapsar estados diferentes em uma única conclusão conveniente**. Escolher não é compreender; rejeitar não é definir uma preferência global; preservar um histórico não é preservar a experiência; e gerar variedade não é explorar design de forma útil.

---

## Cards gerados

### `adocao-nao-equivale-compreensao-ai`

- Coleção: `neuromarketing`
- Por que importa: cria uma separação operacional entre exposição, adoção e compreensão, útil para customer-agent e jornadas em que o usuário precisa realmente incorporar uma informação antes de agir.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-15-adocao-nao-equivale-compreensao-ai.md`
- SHA-256: `c35a8766b067fa3de6e04974685bf28462cf8dfe2670bd56bf30f0ebc1c5f170`
- JSON: `pesquisas/design-experiencia/cards/2026-09-15-adocao-nao-equivale-compreensao-ai.json`

### `feedback-binario-nao-implica-preferencia-ampla`

- Coleção: `neuromarketing`
- Por que importa: reduz o risco de personalização rígida e incorreta a partir de um único 👍/👎, correção ou recusa.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-15-feedback-binario-nao-implica-preferencia-ampla.md`
- SHA-256: `717a7ec21203f1163263588d7e75982c74ca92420908acf8a8640c4da2941b91`
- JSON: `pesquisas/design-experiencia/cards/2026-09-15-feedback-binario-nao-implica-preferencia-ampla.json`

Os dois arquivos representam candidatos a `DRAFT`. Não foram enviados para revisão, ativados ou arquivados.
