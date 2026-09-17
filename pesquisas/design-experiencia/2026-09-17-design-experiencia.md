# Radar de Design de Experiência — 2026-09-17

## Resumo executivo

A rodada de hoje reforça uma mudança importante no design de produtos com IA: **não basta tornar o agente mais capaz; é preciso projetar como o humano decide quando confiar, quando verificar, quando delegar e como o próprio software expõe estado e ações para leitores humanos e agentes**.

Os achados mais fortes apontam para cinco princípios: (1) competência do modelo não se converte automaticamente em ganho humano; (2) conversacionalidade aumenta confiança mesmo quando a informação está errada, portanto verificação precisa ser acionável; (3) interfaces para agentes precisam de um substrato semântico explícito e persistente; (4) satisfação percebida não substitui conclusão verificável de tarefa; e (5) automação de modo pode reduzir fricção quando o sistema preserva contexto e deixa o usuário manter a palavra final.

## 1. Competência do LLM não vira automaticamente competência da dupla humano–IA

### Descoberta

O preprint *Available but Unclaimed: An Empirical Study of Human-AI Synergy*, submetido em 15/09/2026, estudou 535 participantes em uma bateria de 40 itens de raciocínio, rotação mental, silogismos e analogias. Os participantes trabalharam sem assistência ou com GPT-5.6-Luna, Claude Opus 4.8, Gemini 3.6 Flash ou Kimi K3; cada modelo também respondeu aos mesmos itens sozinho 100 vezes sob elicitação comparável.

A assistência ajudou mais nos itens em que o modelo era competente, mas apenas parte do ganho potencial chegou ao humano. O estudo reporta *synergy capture* de aproximadamente 0,584 no comparador de complementaridade. A confiança pós-conselho separou respostas corretas de incorretas pior do que a confiança de participantes sem assistência.

Fonte: https://arxiv.org/abs/2609.16793

### Mecanismo e implicação

O gargalo deixa de ser apenas “qual é a acurácia do modelo?” e passa a ser **se o usuário sabe quando deferir à IA e quando revisar**. Fluência e segurança verbal são sinais fracos de confiabilidade item a item.

Para o Marketing Hub, decisões relevantes podem ganhar uma camada de *selective deference*: evidência específica da tarefa, alternativa contrastante quando útil, opção de revisar e sinal de confiabilidade somente quando houver base verificável.

### Hipótese de experimento

Comparar recomendação simples versus recomendação com camada curta de calibração. Medir adoção de recomendações corretas, adoção de recomendações erradas, revisões, tempo de decisão e resultado verificável da tarefa.

### Riscos e limites

É preprint e usa tarefas cognitivas experimentais, não funis comerciais. A métrica de *synergy capture* é uma construção analítica do estudo e não deve ser transportada como percentual esperado para outros domínios.

## 2. Conversa natural pode aumentar confiança indevida; um aviso passivo não resolve

### Descoberta

O artigo peer-reviewed *Chat but verify: Combating misinformation in conversational Generative AI with verification affordance*, publicado no *Journal of Computer-Mediated Communication* em 29/07/2026, realizou um experimento pré-registrado 2 × 4 com 477 participantes. Foram variados sinais de conversacionalidade e quatro condições de verificação: nenhuma, apenas cue, ação obrigatória e ação voluntária.

Maior conversacionalidade aumentou confiança ao reduzir a percepção negativa de máquina, mesmo quando o assistente apresentava informação incorreta. Apenas mostrar a possibilidade de verificar teve efeito limitado. **Quando a pessoa efetivamente verificava**, a credibilidade percebida e a confiança excessiva caíam. Na condição voluntária, 61 de 122 participantes — exatamente 50% — acionaram a verificação.

Fonte primária: https://academic.oup.com/jcmc/article/31/3/zmag012/8746865

### Mecanismo e implicação

Conversacionalidade funciona como sinal social e pode favorecer processamento heurístico: “soa natural, portanto parece confiável”. Uma affordance de verificação que exige uma ação real cria uma rota para processamento mais sistemático.

Para o Marketing Hub, recomendações factuais ou de maior consequência deveriam poder oferecer **verificação acionável** — abrir a evidência, comparar fonte ou pedir segunda checagem — em vez de depender apenas de “a IA pode errar”.

### Hipótese de experimento

Comparar `aviso passivo` versus `verificação acionável` em recomendações factuais. Medir aceitação de afirmações incorretas, uso da verificação, abandono e tempo adicional introduzido pela checagem.

### Riscos e limites

O cenário foi controlado e relacionado a informação de saúde. Verificar pode adicionar fricção; uma interface ruim também pode apenas transferir confiança cega para a suposta fonte verificadora.

## 3. Interfaces para agentes precisam de um substrato semântico, não de uma estética especial

### Descoberta

O novo *Affora: A Design System for Agent-Friendly Interfaces*, submetido em 16/09/2026, trata a interface como superfície compartilhada por pessoas e agentes. O artigo separa a camada visual daquilo que chama de **semantic substrate**: controles, nomes, relações, escolhas e estados que podem ser recuperados de maneira operável.

Em um dos estudos, 60 componentes interativos foram implementados com HTML nativo e oito bibliotecas. O HTML semântico teve 91% de sucesso no modelo intermediário, enquanto as bibliotecas variaram de 86,7% a 68,3%. Em uma sequência controlada de correções, ajustar semântica elevou sucesso de 43% para 67%, e tornar escolhas e estado explicitamente recuperáveis elevou a 90%.

Fonte: https://arxiv.org/abs/2609.19125

### Mecanismo e implicação

O agente não “vê” necessariamente a mesma interface que a pessoa. Um botão visualmente óbvio pode ter papel, estado ou opções ambíguos na representação recebida pelo agente. Isso cria um novo objeto de design: **Agent Experience (AX) dentro da mesma UI**.

Para produtos que serão operados por computer-use agents, eu acrescentaria ao design system regras executáveis para nome estável, role correto, estado persistente, alternativas enumeráveis, confirmação observável e resultado recuperável.

### Hipótese de experimento

Executar uma mesma suíte de tarefas sobre UI atual e UI com substrato semântico reforçado. Medir sucesso, retries, passos, tokens e falhas por estado não observável.

### Riscos e limites

O próprio trabalho afirma que os benefícios para usuários humanos ainda precisam de avaliação. Resultados variam conforme a representação oferecida ao agente; não é uma prova de que “HTML simples” seja sempre melhor ou de que estética deva ser reduzida.

## 4. Satisfação pode dizer “deu certo” quando a tarefa objetivamente falhou

### Descoberta

O trabalho *GAUGE: When Not to Trust LLM-as-a-Judge in User-Simulated Evaluation of Task-Oriented Agents* avaliou 25 agentes de seis provedores nos benchmarks τ²-bench e SimulatorArena. Entre conversas classificadas como satisfatórias por um painel humano cego, **57,5% ainda falharam a tarefa objetiva**. A satisfação praticamente não carregou informação suficiente sobre sucesso. Para pares de agentes claramente diferentes, o gate de avaliação ranqueou bem; entre candidatos muito próximos, a taxa de discordância de decisão saltou de menos de 1% para 31%.

Fonte: https://arxiv.org/abs/2609.12191

### Mecanismo e implicação

Uma conversa pode ser clara, educada e convincente sem ter executado o que precisava. Isso separa duas famílias de métrica: **qualidade percebida** e **estado real da tarefa**.

No Marketing Hub, avaliações de agentes deveriam manter ambos: satisfação/clareza/fluidez de um lado e `task_success`, evento concluído, artefato correto ou ação reconciliada do outro. LLM-as-a-judge pode permanecer como filtro barato, mas precisa ser periodicamente calibrado contra outcome verificável.

### Hipótese de experimento

Reavaliar jobs históricos comparando julgamento sintético, avaliação humana e eventos objetivos. Mapear onde há “pareceu ótimo, mas falhou” e usar esses casos como regressão.

### Riscos e limites

O estudo usa benchmarks e usuários simulados. A conclusão correta não é abandonar LLM judges, mas limitar o que eles podem provar e evitar ranking forçado quando diferenças estão próximas.

## 5. Automação de modo: tirar do usuário a decisão de “qual ferramenta usar” pode reduzir fricção

### Caso de produto

Em 16/09/2026, a Anthropic anunciou a unificação de Claude Chat e Cowork. A justificativa declarada é diretamente de UX: usuários se frustravam decidindo onde a tarefa pertencia e o contexto iniciado em uma superfície não carregava naturalmente para outra. A nova experiência recebe a intenção e decide se precisa de resposta rápida ou trabalho mais longo, enquanto Docs, Slides e Design passam a operar dentro da conversa.

Fonte oficial: https://claude.com/blog/cowork-is-now-claude

### Implicação

Esse é um caso concreto de **intent-first routing**: o usuário descreve a tarefa, o sistema roteia capacidades. Para o Marketing Hub, isso sugere esconder routing técnico quando ele não representa uma escolha útil para a pessoa, mas manter visíveis estado, permissões, ações relevantes e checkpoints.

### Hipótese de experimento

Comparar fluxo em que a pessoa escolhe previamente “modo/agente/ferramenta” contra um único ponto de entrada com roteamento automático e override. Medir tempo até iniciar, trocas de modo, retrabalho e correções de roteamento.

### Riscos e limites

É um caso de produto e um relato do próprio fornecedor, não um experimento causal. Automação de routing pode esconder mudanças de autoridade; por isso o sistema deve tornar claro quando passa de responder para agir.

## 6. IA mediadora pode regular emoção, mas ainda não sabemos quanto isso causa melhores acordos

### Descoberta

O preprint *AI Mediators Regulate Emotion and Create Value in Disputes*, submetido em 15/09/2026, comparou disputas com mediador humano novato, mediador de IA ou sem mediador. Os autores reportam que a IA reduziu emoção negativa significativamente mais do que o mediador humano e encontraram sinal de maior realização de ganhos conjuntos em disputas com alto potencial integrativo. A análise das mensagens mostra que a IA sugeria trade-offs com maior frequência.

Fonte: https://arxiv.org/abs/2609.17933

### Mecanismo e implicação

Um agente não precisa apenas responder ao conteúdo; ele pode tentar **regular o processo da interação**: reduzir escalada, reformular posições e procurar trade-offs. Isso pode ser útil em reclamações, negociação de escopo e recuperação de falha.

### Hipótese de experimento

Em conversas conflituosas não sensíveis, comparar customer-agent padrão com uma política que detecta escalada e introduz reformulação + opções de trade-off. Medir resolução, número de turnos, abandono e necessidade de escalonamento humano.

### Riscos e limites

É preprint, e a relação causal entre redução de emoção e melhor acordo não está estabelecida. Mediação em disputas reais pode envolver consequências jurídicas, financeiras e emocionais incompatíveis com automação sem supervisão.

## 7. Em co-criação audiovisual coletiva, a interface pode competir com a relação entre as pessoas

### Descoberta

O preprint *Encypher*, submetido em 16/09/2026 e em revisão para CHI 2027, transforma qualidades do movimento coletivo em prompts para música generativa em tempo real. O trabalho combina cinco semanas de co-design com dançarinos, estudo com participantes que não se conheciam, evento público em museu e performance. Os autores relatam percepção de **shared agency** e maior atenção aos sinais do grupo; iniciantes também demonstraram incerteza.

Fonte: https://arxiv.org/abs/2609.18062

### Mecanismo e implicação

Aqui, a IA não é apenas “geradora de conteúdo”: ela pode funcionar como um **objeto compartilhado que coordena pessoas**. Para experiências sociais, a métrica não deveria ser só qualidade do conteúdo gerado; também importa se a interface faz pessoas olharem umas para as outras ou somente para a tela.

### Hipótese de experimento

Em experiências colaborativas, comparar feedback centralizado em tela contra feedback ambiental/discreto. Medir orientação entre participantes, participação equilibrada, sensação de agência compartilhada e qualidade percebida do resultado.

### Riscos e limites

É estudo pequeno e situado em dança; não demonstra que a mesma dinâmica se transfira para colaboração remota, marketing ou consumo. Por isso ficou apenas no relatório.

## Síntese — Experience Engine v17

```text
usuário
   ↓
INTENT-FIRST ROUTER
   ↓
SEMANTIC SUBSTRATE
   ├── ações explícitas
   ├── estado persistente
   └── resultado verificável
   ↓
SELECTIVE DEFERENCE
   ├── evidência específica
   ├── alternativa / contraste
   └── revisão independente
   ↓
VERIFICATION AFFORDANCE
   ↓
EXECUÇÃO / MEDIAÇÃO
   ↓
DUAL OUTCOME
   ├── experiência percebida
   └── tarefa realmente concluída
   ↓
calibração contínua
```

O princípio central da rodada é: **não confundir uma experiência convincente com uma experiência correta**. Conversa natural, satisfação, confiança e estética são importantes, mas precisam coexistir com estado recuperável, evidência, verificação e sucesso objetivo.

## Cards gerados

### `monitoramento-metacognitivo-calibra-confianca-ai`

- Coleção: `neuromarketing`
- Tipo: nova versão de uma ideia já existente.
- Motivo: o novo estudo com 535 participantes acrescenta evidência de que competência do modelo não se converte automaticamente em ganho humano e que confiança pós-conselho perde capacidade discriminativa. Isso atualiza diretamente o card de 16/09 sobre calibração metacognitiva.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-17-monitoramento-metacognitivo-calibra-confianca-ai.md`
- SHA-256: `d68d988e0ba10ede32c0f7154d6ffe927ac5568b38a681dc5265fe72a46cdf55`
- JSON: `pesquisas/design-experiencia/cards/2026-09-17-monitoramento-metacognitivo-calibra-confianca-ai.json`

### `conversacionalidade-verificacao-calibra-confianca`

- Coleção: `neuromarketing`
- Tipo: novo card.
- Motivo: há evidência peer-reviewed e pré-registrada de que sinais conversacionais podem elevar confiança mesmo diante de informação incorreta e de que uma ação real de verificação é mais útil do que um cue passivo.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-17-conversacionalidade-verificacao-calibra-confianca.md`
- SHA-256: `07fe4ebe23c4dd6e6a3242a4fd34633823bb9123cae4c17413c85dfd212ec68f`
- JSON: `pesquisas/design-experiencia/cards/2026-09-17-conversacionalidade-verificacao-calibra-confianca.json`

Os arquivos representam somente candidatos a `DRAFT`. Nenhuma versão foi enviada para revisão, ativada ou arquivada.

## Achados fortes que não viraram card

- **Affora**: altamente relevante para arquitetura de interfaces operadas por agentes, mas não cabe legitimamente nas quatro coleções atuais.
- **GAUGE**: muito útil para harness/evaluation, porém seria distorção classificá-lo como `neuromarketing`.
- **Unificação Claude/Cowork**: prática de produto interessante, mas sem evidência causal suficiente para card persistente.
- **AI Mediators**: preprint promissor, ainda com limitações para extrapolar a mediação experimental para fluxos comerciais reais.
- **Encypher**: interessante para experiência audiovisual e agência coletiva, mas o contexto pequeno e situado ainda não justifica card em `prazer-audio-visual`.

## Referências

- Welsch et al. — *Available but Unclaimed: An Empirical Study of Human-AI Synergy*: https://arxiv.org/abs/2609.16793
- *Chat but verify: Combating misinformation in conversational Generative AI with verification affordance*: https://academic.oup.com/jcmc/article/31/3/zmag012/8746865
- Gao — *Affora: A Design System for Agent-Friendly Interfaces*: https://arxiv.org/abs/2609.19125
- Bodhwani, Tran & Wei — *GAUGE: When Not to Trust LLM-as-a-Judge in User-Simulated Evaluation of Task-Oriented Agents*: https://arxiv.org/abs/2609.12191
- Anthropic — *Claude Cowork and chat are now one Claude*: https://claude.com/blog/cowork-is-now-claude
- Hale & Gratch — *AI Mediators Regulate Emotion and Create Value in Disputes*: https://arxiv.org/abs/2609.17933
- Chen & Huang — *Encypher: Shared Agency and Social Presence in Collaborative Music Generation for Dance Cyphers*: https://arxiv.org/abs/2609.18062
