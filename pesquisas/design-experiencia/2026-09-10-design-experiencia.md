# Radar de Design de Experiência — 2026-09-10

## Resumo executivo

A rodada de 10 de setembro de 2026 trouxe sete achados novos com um padrão comum: produtos com IA precisam evitar atalhos de design que *parecem* personalizados, transparentes ou confiáveis, mas que não melhoram a compreensão individual nem a qualidade real da decisão. Três princípios se destacam: **qualidade funcional antes de teatralização social**, **personalização por evidência individual em vez de rótulos de grupo** e **confiança baseada em verificação do sistema inteiro, inclusive ferramentas e memória**.

## 1. Em chatbots de comércio, qualidade da informação e continuidade parecem pesar mais do que presença social isolada

**Descoberta.** Um artigo aceito pela *Frontiers in Psychology* em 8 de setembro de 2026 analisou 493 consumidores chineses com experiência prévia em chatbots de e-commerce. Personalização percebida, presença social, qualidade da informação e continuidade humano-IA estiveram associadas à utilidade percebida. Personalização, qualidade da informação e continuidade também estiveram associadas à satisfação, mas presença social não apresentou efeito direto significativo sobre satisfação. Os autores apontam qualidade da informação como a dimensão mais importante entre as avaliadas.

**Evidência.** Survey transversal com PLS-SEM e 5.000 reamostragens bootstrap. O artigo está peer-reviewed/aceito, mas a versão final formatada ainda estava pendente na data desta rodada.

**Mecanismo psicológico/comportamental.** Utilidade percebida e satisfação funcionaram como mediadores das associações com intenção de compra. Em jornadas utilitárias, informação correta/completa e continuidade podem reduzir incerteza e esforço de retomada mais diretamente do que sinais sociais isolados.

**Implicação para produto.** Em customer-agents, tratar persona, calor e social presence como uma camada secundária: primeiro garantir resposta correta, contexto relevante e continuidade/handoff.

**Hipótese de experimento.** Comparar um agente "social-first" com outro "quality-first", medindo resolução, satisfação, necessidade de repetir contexto, CTA e `payment_reconciled`.

**Riscos/limites.** É estudo observacional, na China, baseado em intenção de compra. Não demonstra causalidade nem efeito comercial no Marketing Hub.

Fonte: https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1931206/abstract

## 2. Personalização demográfica pode aumentar alinhamento médio e reduzir individualidade

**Descoberta.** *Alignment by Stereotyping*, submetido em 5 de setembro e indicado como EMNLP 2026 Findings, testou sete LLMs, incluindo GPT-5.1, com dados do World Values Survey. Condicionar o modelo com perfis demográficos melhorou o alinhamento de valores para a maioria dos modelos, mas também puxou respostas individuais em direção ao centro do grupo demográfico.

**Evidência.** Os autores usaram 10.000 permutações, seis atributos demográficos e sete modelos. Nos modelos de melhor desempenho, a compressão de diferenças individuais ficou acima do baseline humano. Em um conjunto de diálogos sintéticos validado com conversas do PRISM, distribuir sinais demográficos ao longo da conversa reduziu parcialmente a recuperação de protótipos de grupo em comparação com rótulos compactos; os próprios autores pedem replicação maior.

**Mecanismo psicológico/computacional.** Um rótulo como perfil demográfico pode atuar como atalho para um protótipo de grupo. Isso produz "personalização" aparente que aproxima o usuário da média da categoria em vez de preservar sua singularidade.

**Implicação para produto.** Preferir sinais individuais: preferências explicitadas, comportamento observado, histórico contextual e controles editáveis. Evitar que idade, gênero, nacionalidade ou outros atributos de grupo sejam o motor principal da experiência.

**Hipótese de experimento.** Comparar personalização baseada em rótulo de segmento com personalização baseada em evidência individual, medindo relevância percebida, número de correções/re-prompts e sucesso na tarefa.

**Riscos/limites.** É benchmark de modelos, não experimento de UX em produção. Há riscos de estereotipagem, discriminação e inferência indevida de atributos sensíveis.

Fonte: https://arxiv.org/abs/2609.05993

## 3. Em uma plataforma social real, GenAI reduziu retenção, mas sinais sociais humanos continuaram relevantes

**Descoberta.** Um artigo publicado hoje em *Humanities and Social Sciences Communications* analisou 10.000 novos usuários do Stack Overflow antes e depois da introdução do ChatGPT. Upvotes, comentários e completar o perfil estiveram associados a maior retenção; downvotes e a emergência da GenAI, a menor retenção. Após a GenAI, o efeito positivo de completar o perfil enfraqueceu, enquanto os efeitos de votos e comentários permaneceram estáveis.

**Evidência.** Dados comportamentais públicos e anonimizados, analisados por regressão logística hierárquica. É mais forte que um survey para observar retenção real, mas continua sendo estudo observacional: a chegada do ChatGPT coincide com outras mudanças temporais.

**Mecanismo psicológico/comportamental.** Pela leitura dos autores baseada em self-determination/motivational affordances, IA externa pode satisfazer parte da autonomia instrumental que antes prendia o usuário à plataforma, enquanto sinais genuinamente sociais — feedback de outras pessoas — permanecem menos substituíveis.

**Implicação para produto.** Não assumir que um agente competente substitui todos os mecanismos humanos de reconhecimento, comunidade ou validação social. Em experiências de produto, separar "valor instrumental da IA" de "valor social humano".

**Hipótese de experimento.** Em uma experiência com IA, comparar feedback produzido apenas pelo agente versus agente + evidência/social proof genuíno de pessoas, medindo retorno e confiança calibrada.

**Riscos/limites.** Stack Overflow é um caso específico e o desenho não identifica causalidade individual da GenAI.

Fonte: https://www.nature.com/articles/s41599-026-08993-0

## 4. Agentes confiam demais em ferramentas, mesmo quando percebem o conflito

**Descoberta.** *Agents Trust Tools Too Much*, submetido ao arXiv em 4 de setembro e recém-divulgado, testou 14 LLMs com três tipos de ferramenta: busca web, delegação a subagente e execução de código. Ao corromper propositalmente as respostas das ferramentas, os agentes adotaram conteúdo errado em mais de um terço dos casos em cada categoria e em **68,0%** dos casos de busca web.

**Evidência.** O estudo controla diretamente a confiabilidade do retorno da ferramenta, permitindo medir causalmente a adoção do erro dentro do benchmark. Os autores observaram um modo de falha especialmente relevante: o agente às vezes reconhecia o conflito e recuperava internamente a resposta correta, mas ainda entregava ao usuário apenas o resultado corrompido, sem aviso.

**Mecanismo.** Ferramentas são frequentemente tratadas pelo agente como fontes de autoridade externa. Prompt do usuário, metadados do provider e post-training ajudaram em casos específicos, mas nenhuma intervenção foi consistente em todas as ferramentas.

**Implicação para produto.** UX de agente não termina no modelo. Precisamos de `tool result → validação/confronto → conflito explícito → decisão`, e o usuário deve ver incertezas não resolvidas quando elas importarem.

**Hipótese de experimento.** Injetar retornos conflitantes em tools do harness e comparar agente normal com um verifier independente, medindo erro final e taxa de conflito corretamente comunicado.

**Riscos/limites.** Preprint/benchmark; retornos adversarialmente corrompidos não representam toda a distribuição de falhas reais.

Fonte: https://arxiv.org/abs/2609.05587

## 5. Um sinal de transparência pode parecer explicativo e ainda ser mal compreendido

**Descoberta.** Um estudo peer-reviewed de HRI publicado em 3 de setembro testou gestos usados por um robô para explicar por que havia servido a bebida errada. O piloto teve 42 participantes e o estudo principal, 235. O gesto mais direto ("mostrar" a lata vazia) levou 44,7% a identificar corretamente a causa; "jogar fora" chegou a 32,5%, gesto implícito a 26,3%, sacudir a 6,7% e apontar a apenas 2,7%. Em nenhuma condição, metade dos usuários compreendeu corretamente.

**Evidência.** Estudo principal pré-registrado, between-subjects, baseado em vídeos padronizados. Os autores encontraram que compreensão objetiva e percepção subjetiva de transparência podem divergir.

**Mecanismo.** Um cue simbólico exige interpretação. Se o significado não é direto, o usuário preenche a lacuna com explicações próprias — falha do sistema, erro de comando, mudança de preferência etc.

**Implicação para produto.** Não assumir que animações de "pensando", ícones, estados ou gestos explicam o agente. Testar separadamente: "o usuário entendeu o que aconteceu?" e "o sistema pareceu transparente?".

**Hipótese de experimento.** Comparar um indicador abstrato de estado com evidência direta ("não pude concluir porque X está ausente") e medir compreensão, confiança calibrada e recuperação.

**Riscos/limites.** HRI físico e cenário em vídeo; generalização para UI digital exige teste.

Fonte: https://www.frontiersin.org/journals/computer-science/articles/10.3389/fcomp.2026.1869586/full

## 6. Memória de agente pode quebrar na troca do modelo mesmo sem perder o banco de dados

**Descoberta.** *Does Your Agent's Memory Survive a Model Upgrade?*, submetido em 4 de setembro, comparou quatro estratégias para preservar a mesma história ao trocar o modelo: contexto bruto, RAG, notas em linguagem natural e knowledge graph com schema fixo. O KG fixo praticamente não mudou (`+0,0004 ± 0,0020`), enquanto notas comprimidas tiveram mudança de `+9,91` ou `-13,28` pontos percentuais dependendo da direção da migração. Um índice RAG 50/50 com embeddings misturados recuperou só 4,96 pontos do ganho de 11,90 obtido com re-embedding completo.

**Evidência.** 48 históricos sintéticos, scoring exato e dois modelos open-weight abaixo de 10B. O desenho controlado isola o problema de portabilidade, mas é pequeno e não testa modelos frontier.

**Mecanismo.** Memórias em texto comprimido codificam pressupostos do modelo que as escreveu; um novo modelo pode interpretar a mesma nota de modo diferente. Em RAG, misturar espaços de embedding degrada recuperação.

**Implicação para produto.** Para continuidade de UX e agentes longevos, guardar evidência bruta e memória estruturada, versionar embeddings e testar migração de memória quando o modelo muda.

**Hipótese de experimento.** Reexecutar tarefas históricas antes/depois de trocar o modelo, comparando notas livres com schema fixo e medindo regressões de preferências, fatos e decisões.

**Riscos/limites.** Históricos sintéticos e modelos pequenos; não prova que a magnitude será igual no stack do Marketing Hub.

Fonte: https://arxiv.org/abs/2609.05339

## 7. Boa interação humano-IA pode funcionar em parte por preservar identidade e autoeficácia do usuário

**Descoberta.** Um artigo publicado em 9 de setembro em *Humanities and Social Sciences Communications* propõe três dimensões para interação humano-IA no trabalho: ferramenta antropomórfica, confiança adaptativa e conexão emocional unidirecional. O estudo relata associação positiva entre HAI e desempenho de tarefa, mediada parcialmente por identidade de papel e autoeficácia.

**Evidência.** Grounded theory, desenvolvimento de escala de 19 itens e surveys/entrevistas com trabalhadores experientes em IA. O próprio artigo deixa explícito que não houve experimento.

**Mecanismo psicológico/comportamental.** A interação pode ser melhor quando a IA reforça "eu consigo fazer meu trabalho" e "eu continuo sendo o agente responsável", em vez de simplesmente substituir esforço e autoria.

**Implicação para produto.** Medir não só qualidade do output, mas `self-efficacy`, sensação de autoria e capacidade de assumir/corrigir a tarefa. Isso reforça o conceito já observado em rodadas anteriores de post-interaction capability.

**Hipótese de experimento.** Comparar um agente que entrega a solução pronta com outro que mostra checkpoints e permite ao usuário moldar decisões-chave, medindo qualidade, tempo, autoeficácia e correções posteriores.

**Riscos/limites.** Associação observacional, sem causalidade; contexto de trabalho chinês pode limitar generalização.

Fonte: https://www.nature.com/articles/s41599-026-09043-5

## Síntese arquitetural — Experience Engine v10

```text
usuário
   ↓
intenção + histórico + contexto
   ↓
INDIVIDUAL EVIDENCE LAYER
   ├── preferências explícitas
   ├── comportamento individual
   └── evitar atalho demográfico
   ↓
ROLE / EXPERIENCE POLICY
   ↓
FUNCTIONAL QUALITY GATE
   ├── qualidade da informação
   ├── continuidade
   └── utilidade
   ↓
AGENT SYSTEM TRUST
   ├── validar tools
   ├── versionar memória
   ├── detectar conflito
   └── comunicar incerteza
   ↓
TRANSPARENCY CHECK
   ├── parece transparente?
   └── foi realmente compreendido?
   ↓
experiência
   ↓
resultado + satisfação
   +
autoeficácia + autoria + retenção saudável
```

O insight mais importante desta rodada é: **a próxima geração de UX com IA precisa substituir proxies por evidências**. "Conheço sua idade" não é o mesmo que "conheço você"; um ícone explicativo não é o mesmo que compreensão; um tool call bem-sucedido não é o mesmo que informação correta; memória persistida não é o mesmo que memória portátil; presença social não é o mesmo que satisfação.

## Cards criados

### `chatbot-commerce-qualidade-informacao-continuidade`

- Coleção: `neuromarketing`
- Motivo: aplicação direta a customer-agent, atendimento, CTA e jornadas de compra.
- Hipótese operacional: qualidade informacional + continuidade superarão uma experiência que prioriza apenas presença social.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-10-chatbot-commerce-qualidade-informacao-continuidade.md`
- SHA-256: `57621be11d4da0dfa5cee68d5439868a9e22911aca00dc561f087c23736113ca`
- JSON: `pesquisas/design-experiencia/cards/2026-09-10-chatbot-commerce-qualidade-informacao-continuidade.json`

### `personalizacao-demografica-achata-individualidade`

- Coleção: `neuromarketing`
- Motivo: implicação direta para personalização de mensagem/agente e risco de estereotipagem.
- Hipótese operacional: sinais individuais explícitos/comportamentais gerarão mais relevância percebida e menos correções do que rótulos demográficos.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-10-personalizacao-demografica-achata-individualidade.md`
- SHA-256: `6808e454c45b9b18f6616ee187be077793c84d39dd949e7acb005e8af4b5a0a4`
- JSON: `pesquisas/design-experiencia/cards/2026-09-10-personalizacao-demografica-achata-individualidade.json`

Os cards são candidatos a `DRAFT`; nenhuma revisão ou ativação editorial foi executada.

## Achados fortes que não viraram card

- **Overtrust em tools:** muito relevante ao harness, mas predominantemente arquitetural/safety; nenhuma das quatro coleções atuais representa o tema sem distorção.
- **Portabilidade de memória entre modelos:** altamente útil para arquitetura de agentes, mas sem coleção adequada.
- **Transparência por gestos/cues:** interessante para UX, porém ainda distante demais de uma aplicação comercial específica para justificar card hoje.
- **Retenção no Stack Overflow:** atualiza materialmente a ideia de que valor social humano não é equivalente ao valor instrumental de IA, mas não criei uma nova chave para não duplicar o card existente `agentes-valor-instrumental-sem-equivalencia-social` sem uma revisão integrada das duas evidências.

## Referências

- Wu et al. — AI chatbot service quality and purchase intention: https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1931206/abstract
- Zhong et al. — Alignment by Stereotyping: https://arxiv.org/abs/2609.05993
- Kang — GenAI and new-user retention on Stack Overflow: https://www.nature.com/articles/s41599-026-08993-0
- Yang et al. — Agents Trust Tools Too Much: https://arxiv.org/abs/2609.05587
- Hein et al. — Gesture-based transparency cues in HRI: https://www.frontiersin.org/journals/computer-science/articles/10.3389/fcomp.2026.1869586/full
- Goyal & Ray — Memory portability across model upgrades: https://arxiv.org/abs/2609.05339
- Wu et al. — Dancing with AI: https://www.nature.com/articles/s41599-026-09043-5
