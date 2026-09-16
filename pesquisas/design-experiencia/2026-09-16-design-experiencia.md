# Radar de Design de Experiência — 2026-09-16

## Resumo executivo

A rodada de 16/09/2026 converge para um princípio que pode ser resumido como **calibrar antes de ajudar**. As novidades mais úteis de hoje não apontam para agentes que simplesmente falam mais, intervêm mais cedo ou parecem mais empáticos. Elas apontam para sistemas que calibram a complexidade da resposta, a confiança do usuário, o momento da intervenção, a estabilidade do papel em voz e o modo como emoções são respondidas.

Os dois achados com aplicação mais direta ao Marketing Hub viraram cards em `neuromarketing`: um sobre **orçamento de complexidade conversacional** e outro sobre **monitoramento metacognitivo para calibrar confiança**. Os demais achados ficaram no relatório porque são principalmente arquitetura de agentes, benchmarks de voz, evidência sensível ou casos de produto ainda sem validação comportamental suficiente.

---

## 1. Complexidade conversacional funciona como um orçamento conjunto

### Descoberta

O preprint *When AI Becomes Hard to Understand: Cognitive Demands in Real-World Human-AI Conversations*, submetido em 15/09/2026, analisou mais de 84 mil conversas reais com ChatGPT e Gemini em contextos financeiros e de saúde.

### Evidência

Os autores usaram repetição de prompts e pedidos de esclarecimento após mal-entendidos como indicadores comportamentais de dificuldade. Comprimento, legibilidade e diversidade lexical não apresentaram relações fixas e independentes com a dificuldade; o efeito dependia da combinação entre essas características. Maior diversidade lexical esteve associada a menos repetição de prompts em respostas curtas, mas essa associação enfraqueceu conforme o comprimento aumentava. O padrão apareceu nos dois domínios.

Fonte: https://arxiv.org/abs/2609.17301

### Mecanismo psicológico/comportamental

Carga cognitiva não parece depender de uma única dimensão. Uma resposta pode tolerar mais vocabulário quando é curta, ou mais profundidade quando sua estrutura reduz esforço de navegação. Assim, a experiência funciona como um **orçamento de complexidade** distribuído entre comprimento, vocabulário, número de conceitos, quantidade de opções e profundidade.

### Implicação para produto

Evitar regras universais como “responda sempre curto”. No customer-agent, a política pode adaptar conjuntamente comprimento, profundidade, vocabulário e número de opções. Repetição, reformulação e pedidos de esclarecimento podem funcionar como sinais para redistribuir a complexidade da próxima resposta.

### Hipótese/experimento

Comparar uma política fixa de resposta curta com uma política adaptativa que reduz ou redistribui complexidade após sinais de dificuldade. Medir esclarecimentos adicionais, reformulações, abandono, avanço para CTA e retrabalho.

### Riscos e limites

É evidência observacional em preprint. Repetição e esclarecimento são proxies, não medidas diretas de carga cognitiva. Os domínios estudados foram finanças e saúde, e não houve medição de conversão ou vendas.

### Card

Criado `orcamento-complexidade-conversacional-ai`, coleção `neuromarketing`.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-16-orcamento-complexidade-conversacional-ai.md`

SHA-256: `fdeb57b4ecfaad96c71dfb35ef3e6afd388a0d960b2a343314370a977c9576ec`

---

## 2. Avisos genéricos sobre erros da IA são mais fracos que cues específicos da tarefa

### Descoberta

*Beyond "ChatGPT Can Make Mistakes": Designing Interventions to Support Metacognitive Monitoring in AI-Assisted Work*, submetido em 15/09, investigou como ajudar usuários a monitorar a própria confiança quando trabalham com IA.

### Evidência

Primeiro, 11 especialistas produziram 30 propostas de intervenção. Depois, um experimento between-subjects com 917 participantes e 12 problemas de planejamento/organização comparou quatro intervenções com um assistente LLM baseline: cartão de confiabilidade por tarefa, respostas contrastantes, pontos de pausa e reflexão pós-problema. Cartões de confiabilidade e respostas contrastantes reduziram erro de estimação e overconfidence e melhoraram a discriminação agregada da confiança. O estudo, porém, não demonstrou melhora de desempenho na tarefa.

Fonte: https://arxiv.org/abs/2609.17065

### Mecanismo psicológico/comportamental

“ChatGPT pode errar” transfere quase todo o trabalho metacognitivo ao usuário. Um cue específico para aquela tarefa ou uma alternativa plausível torna a incerteza mais concreta e comparável, ajudando a pessoa a revisar sua certeza.

### Implicação para produto

Para recomendações de maior consequência, testar um pequeno bloco com confiabilidade/evidência ou uma resposta alternativa contrastante antes da decisão. O objetivo não é aumentar confiança, mas **calibrá-la**.

### Hipótese/experimento

Comparar recomendação normal, aviso genérico e cue específico da tarefa. Medir aceitação acrítica, correções posteriores, necessidade de revisão e abandono.

### Riscos e limites

Um indicador de confiança pode virar um falso selo de autoridade. Melhor calibração subjetiva não significa maior acurácia. O estudo é preprint e não demonstrou impacto comercial.

### Card

Criado `monitoramento-metacognitivo-calibra-confianca-ai`, coleção `neuromarketing`.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-16-monitoramento-metacognitivo-calibra-confianca-ai.md`

SHA-256: `bc14b05de59a8100381fb707ccf917832c0067060cad8d57d8fa65a6cc572c0d`

---

## 3. Assistência just-in-time melhora quando percepção, diagnóstico e intervenção são separados

### Descoberta

*A Scenario-Knowledge-Driven Pipeline for Just-in-Time Assistance*, submetido em 15/09 e aceito em workshop do IROS 2026, propõe um pipeline no qual um documento de cenário humano e versionado configura sensing, restringe o raciocínio do LLM e define uma escada graduada de intervenção.

### Evidência

No proof-of-concept, duas sessões gravadas de uso de quiosque foram reproduzidas offline. Em 95 atualizações, cada frase de narração citou eventos primitivos observáveis. A camada de regras identificou 12 de 13 e 7 de 7 episódios anotados de dificuldade nas duas sessões; o sistema também desescalou quando houve recuperação e só chegou ao nível máximo uma vez, quando evidências convergiram.

Fonte: https://arxiv.org/abs/2609.17132

### Mecanismo psicológico/comportamental

Separar `o que aconteceu` de `o usuário precisa de ajuda?` e de `qual intervenção usar?` reduz a tendência de transformar toda anomalia em interrupção. A ajuda passa a depender de evidência acumulada e de recuperação observada.

### Implicação para produto

Para agentes, vale modelar três etapas independentes: `narrar estado → avaliar necessidade → escolher intervenção`. Isso pode reduzir proatividade excessiva e permitir de-escalada quando o usuário retoma o controle sozinho.

### Hipótese/experimento

Comparar uma política que intervém ao primeiro sinal com uma política que exige convergência de sinais e desescala após recuperação. Medir interrupções desnecessárias, conclusão, tempo e sensação de controle.

### Riscos e limites

A evidência empírica é extremamente pequena: apenas duas sessões. Não virou card porque é principalmente arquitetura de harness e ainda não há base comportamental suficiente.

---

## 4. Agentes de voz mantêm melhor o papel semântico do que a emoção vocal ao longo da conversa

### Descoberta

O novo benchmark *RoleBreak* testa robustez de role-playing em diálogos falados de longa duração.

### Evidência

O benchmark inclui 310 papéis, 6.688 turnos verificados por humanos, 11.743 critérios finos e 1.856 alvos de emoção expressiva, avaliando nove configurações entre modelos full-duplex, omni e pipelines em cascata. Os sistemas atuais foram muito mais robustos em aderência semântica ao papel do que em manter emoção vocal. Mesmo o sistema mais forte apresentou a primeira falha de persona e de segurança, em média, após aproximadamente 10,4 e 11,6 turnos. Aumentar a capacidade do LLM ajudou principalmente a parte semântica, com pouco ganho na dimensão de emoção vocal.

Fonte: https://arxiv.org/abs/2609.16614

### Mecanismo psicológico/comportamental

Voz adiciona um canal afetivo que não é automaticamente estabilizado pelo mesmo mecanismo que mantém conteúdo e persona. Prosódia e emoção do próprio usuário também alteram o comportamento do agente mesmo quando o texto permanece igual.

### Implicação para produto

Em agentes de voz, avaliar separadamente `conteúdo correto`, `papel/persona`, `segurança` e `emoção/prosódia`. Testes curtos podem mascarar drift que só aparece depois de vários turnos.

### Hipótese/experimento

Criar testes de 15–20 turnos com mudança controlada de emoção vocal do usuário, mantendo conteúdo textual equivalente. Medir estabilidade de papel, tom, limites e recuperação após drift.

### Riscos e limites

É benchmark/preprint, não estudo de preferência ou resultado do usuário. Não virou card: classificá-lo em `prazer-audio-visual` só porque envolve voz distorceria sua finalidade principal, que é robustez de agentes.

---

## 5. Empatia de voz não deveria apenas espelhar a emoção do usuário

### Descoberta

*ER-EDF: A Psychology-Grounded Emotion Regulation Framework for Speech Empathetic Dialogue Generation in Large Audio-Language Models* propõe separar explicitamente percepção emocional e regulação da emoção da resposta.

### Evidência

O trabalho avaliou o framework em cinco modelos áudio-linguísticos e dois datasets e reportou melhora consistente em qualidade de resposta empática em avaliações automáticas e humanas. A proposta evita mapear diretamente “usuário triste → agente triste” ou “usuário irritado → agente irritado”.

Fonte: https://arxiv.org/abs/2609.15089

### Mecanismo psicológico/comportamental

Empatia útil exige reconhecer o estado do outro sem necessariamente reproduzi-lo. Dependendo da situação, uma resposta mais regulada — calma, estável ou encorajadora — pode ser mais funcional que espelhamento afetivo literal.

### Implicação para produto

Para voz e avatares, separar `emotion recognition` de `response emotion policy`. A política pode decidir quando acompanhar, suavizar, conter ou neutralizar a emoção percebida.

### Hipótese/experimento

Comparar espelhamento direto com uma política regulada em cenários de frustração, dúvida e entusiasmo. Medir percepção de empatia, clareza, escalada emocional e satisfação.

### Riscos e limites

A evidência está principalmente no comportamento dos modelos, não em resultados comerciais ou estudos longitudinais de usuários. Não virou card nesta rodada.

---

## 6. Ligação emocional com IA pode coexistir com benefício e risco; vínculo alto não é diagnóstico suficiente

### Descoberta

*Beyond Benefit or Risk: Perceived Impact Profiles of Human-AI Affective Interaction and Their Associations with Psychological Functioning*, submetido em 15/09, examinou perfis diferentes de impacto de interações afetivas com IA.

### Evidência

O primeiro estudo entrevistou 52 usuários com vínculos emocionais com IA e identificou quatro domínios positivos e quatro negativos. O segundo acompanhou 673 usuários chineses por seis meses e encontrou quatro perfis: minimal, benefit-driven, mixed e risk-driven. Perfis mixed e risk-driven apresentaram vínculo elevado, mas o perfil de risco tinha maior vulnerabilidade. Após ajuste pelo baseline, os perfis iniciais não previram cinco de seis indicadores no follow-up; apenas o perfil mixed mostrou maior flourishing em relação ao minimal.

Fonte: https://arxiv.org/abs/2609.16645

### Mecanismo psicológico/comportamental

A intensidade do vínculo por si só é uma variável pobre para inferir dano ou benefício. O mesmo nível de ligação pode coexistir com padrões funcionais ou disfuncionais diferentes.

### Implicação para produto

Evitar otimizar agentes relacionais simplesmente para maximizar tempo, apego ou dependência. Avaliar padrões mais ricos: autonomia, substituição social, limites, funcionamento cotidiano e possibilidade de interromper a interação sem custo artificial.

### Hipótese/experimento

Em experiências relacionais de baixo risco, medir não apenas frequência/retorno, mas indicadores de autonomia e substituição de atividades. Não aplicar isso como scoring psicológico individual sem validação e consentimento.

### Riscos e limites

Domínio sensível, dados autorrelatados e contexto cultural específico. Não virou card por prudência científica e porque converter esse resultado em orientação comercial poderia incentivar uso inadequado de sinais emocionais.

---

## 7. Voz em tempo real começa a precisar de estados explícitos de “ainda pensando”

### Descoberta

Em 15/09/2026, o Google documentou novos recursos do Gemini Live em que o sistema pode continuar raciocinando em background durante uma sessão de voz e emitir falas intermediárias enquanto uma operação ainda não terminou.

### Evidência/caso de produto

A documentação do Gemini Live descreve `interaction_status` como `IN_PROGRESS` versus `IDLE` e suporte a fillers durante chamadas assíncronas de ferramentas. Isso é relevante porque um modelo pode falar mais de uma vez dentro da mesma solicitação sem que a tarefa já esteja concluída.

Fontes:

- https://ai.google.dev/gemini-api/docs/live
- https://ai.google.dev/gemini-api/docs/live-guide

### Mecanismo psicológico/comportamental

Em voz, silêncio prolongado é facilmente interpretado como travamento ou encerramento. Por outro lado, uma fala intermediária pode ser interpretada erroneamente como resposta final. O usuário precisa perceber o **estado de ciclo da interação**, não apenas ouvir linguagem natural.

### Implicação para produto

Agentes de voz deveriam diferenciar verbal e visualmente `ouvindo`, `pensando`, `executando ferramenta`, `falando parcial` e `concluído`. Filler pode reduzir silêncio, mas não deve mascarar que o sistema ainda não terminou.

### Hipótese/experimento

Comparar voz com silêncio, fillers neutros e status explícito de progresso em tarefas com tools lentas. Medir interrupções, repetição de comando, abandono e entendimento de que a tarefa ainda estava em andamento.

### Riscos e limites

É uma prática de produto/documentação de fornecedor, não evidência independente de melhoria de UX. Não virou card.

---

## Experience Engine v16 — calibração antes de assistência

```text
usuário
   ↓
intenção + contexto + histórico
   ↓
COMPLEXITY BUDGET
   ├── comprimento
   ├── vocabulário
   ├── profundidade
   └── número de opções
   ↓
METACOGNITIVE CALIBRATION
   ├── confiança específica da tarefa
   ├── alternativa contrastante
   └── incerteza explícita
   ↓
ASSISTANCE-NEED ASSESSMENT
   ├── observar
   ├── esperar evidência
   ├── intervir
   └── desescalar
   ↓
MODALITY POLICY
   ├── semântica
   ├── voz/prosódia
   └── emoção regulada
   ↓
INTERACTION STATE
   ├── pensando
   ├── executando
   ├── parcial
   └── concluído
   ↓
resultado + compreensão
+ autonomia + confiança calibrada
```

O princípio central desta rodada é: **não maximizar ajuda; calibrar a ajuda**. Uma resposta mais curta não é automaticamente melhor, mais confiança não é automaticamente melhor, mais empatia não significa espelhar mais emoção e mais proatividade não significa intervir mais cedo.

## Cards criados

Foram criados apenas dois candidatos porque são os achados com melhor combinação de evidência e aplicação direta ao Marketing Hub:

1. `orcamento-complexidade-conversacional-ai` — coleção `neuromarketing`.
2. `monitoramento-metacognitivo-calibra-confianca-ai` — coleção `neuromarketing`.

As fontes revisadas foram criadas antes dos JSONs e seus SHA-256 foram calculados sobre os bytes efetivamente versionados. Nenhum card foi enviado para revisão, ativado ou arquivado; permanecem candidatos a `DRAFT`.

Os achados sobre assistência just-in-time, robustez de voz, regulação emocional, impacto afetivo e estado de execução em voz ficaram somente no relatório por falta de encaixe limpo nas coleções atuais ou por evidência ainda insuficiente/sensível.
