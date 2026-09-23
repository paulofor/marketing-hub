# Radar de Design de Experiência — 2026-09-23

## Síntese da rodada

A principal convergência de hoje é que **boa experiência com IA exige separar forma social, julgamento, autoria e influência**. Um agente pode ser receptivo sem concordar; uma ferramenta generativa pode acelerar um não especialista sem necessariamente acelerar um especialista; um rascunho pode parecer apenas uma conveniência e ainda deslocar o estilo cultural do autor; e até um “thinking partner” que não recomenda uma decisão pode alterar temporariamente os valores mais salientes durante o julgamento.

Para produto, a consequência é clara: não otimizar apenas fluidez, velocidade ou aceitação. Precisamos medir também **preservação de intenção, independência substantiva, estilo autoral, qualidade verificada e efeitos de influência**.

---

## 1. Receptividade e bajulação não são a mesma coisa

**Descoberta.** O preprint *Receptiveness, Not Sycophancy* separa receptividade conversacional de deferência substantiva. O alvo desejável é uma IA capaz de reconhecer a perspectiva do usuário e discordar de forma construtiva sem mudar seu julgamento apenas para agradar.

**Evidência.** O estudo pré-registrado recrutou 200 participantes; 196 permaneceram após as exclusões previstas. Cada pessoa comparou respostas com a mesma conclusão substantiva, mas com uma versão reescrita para ser mais receptiva. Para respostas originalmente geradas por modelos, a versão receptiva ganhou em qualidade (+0,13 em escala de 7 pontos), probabilidade percebida de ser ouvida (+0,24 em escala de -2 a +2) e preferência para buscar conselho (+0,20). Os efeitos foram muito maiores em respostas originalmente humanas. O estudo também mostra que métricas atuais de “social sycophancy” podem penalizar comportamentos que são, na realidade, receptividade.

**Mecanismo psicológico/comportamental.** Reconhecer a perspectiva, explicitar terreno comum legítimo, limitar excesso de certeza e oferecer um caminho construtivo pode reduzir resistência interpessoal sem exigir concordância.

**Implicação para produto.** No `customer-agent`, separar duas camadas: primeiro o julgamento factual/substantivo; depois a forma de comunicá-lo. O agente deveria conseguir dizer “entendo por que isso parece atraente” e, ainda assim, manter “mas os dados não sustentam essa conclusão”.

**Experimento possível.** Comparar uma resposta direta contra uma resposta receptiva com exatamente a mesma conclusão. Medir compreensão, correções do usuário, continuidade útil, conclusão de tarefa e aceitação de informação correta.

**Riscos/limites.** O contexto foi aconselhamento moral, não vendas. Os efeitos para textos de modelos foram modestos. Receptividade sem âncora substantiva pode virar bajulação ou validação de erro.

Fonte: https://arxiv.org/abs/2609.26579

---

## 2. Prompt-to-design economiza tempo, mas o efeito depende muito de quem está usando

**Descoberta.** Um RCT com Figma Make mostra que “IA acelera design” é uma afirmação incompleta: o ganho depende de função e complexidade da tarefa.

**Evidência.** Foram 50 product designers e 50 product managers, randomizados para trabalhar com ou sem Figma Make em três tarefas. No conjunto, o acesso foi associado a cerca de 20% menos tempo entre quem concluiu as tarefas. Product managers tiveram os maiores ganhos: aproximadamente 33% menos tempo acumulado no modelo com interação por função, além de aumento expressivo de conclusão na tarefa mais difícil. Para designers profissionais, o efeito agregado foi bem menor e não foi robusto em todas as especificações; o ganho mais claro apareceu na tarefa mais complexa. A qualidade foi principalmente autoavaliada, e os autores pedem avaliação independente de qualidade visual, acessibilidade e aderência a design system.

**Mecanismo.** Ferramentas prompt-to-design parecem reduzir mais o custo de execução para pessoas cuja barreira é transformar intenção em artefato do que para especialistas que já executam rapidamente.

**Implicação para produto.** Generative UI não deveria ser vendida internamente como substituição uniforme do trabalho de design. Ela pode ser especialmente útil para permitir que PMs, marketing e outras funções produzam um primeiro artefato, enquanto designers concentram energia em qualidade, coerência e casos complexos.

**Experimento possível.** Medir `papel × complexidade × AI assist` e separar tempo, conclusão, qualidade independente, acessibilidade e retrabalho.

**Riscos/limites.** Ganho de velocidade não demonstra ganho de qualidade. A amostra é de 100 profissionais e usa uma ferramenta específica.

Fonte: https://arxiv.org/abs/2609.26725

---

## 3. Rascunhos de IA podem deslocar o estilo cultural do autor

**Descoberta.** Um rascunho não funciona apenas como conteúdo inicial; ele pode atuar como uma âncora de estilo e deslocar a forma como a pessoa se comunica.

**Evidência.** Em experimento pré-registrado com trabalhadores japoneses e americanos, 177 participantes formaram a amostra analítica que efetivamente gerou os rascunhos nas duas condições de IA. Os participantes escreveram emails profissionais sem IA, com um rascunho de baixo contexto e com um rascunho de alto contexto. Os emails finais se moveram na direção do estilo do rascunho. O deslocamento foi particularmente grande quando o rascunho estava culturalmente desalinhado: os autores reportam mudanças 10–16 vezes maiores do que nas condições alinhadas.

**Mecanismo.** O rascunho cria uma estrutura inicial de linguagem, tom e convenções que reduz o custo de edição, mas também cria ancoragem. Ao editar, a pessoa pode preservar grande parte da forma recebida.

**Implicação para produto.** Em ferramentas de escrita, não assumir que “usuário pode editar” preserva autoria. Uma política melhor é partir do estilo individual observado/explicitado, permitir controle de formalidade/direção e mostrar quando uma transformação de tom está sendo aplicada.

**Experimento possível.** Comparar `rascunho genérico`, `rascunho alinhado ao estilo individual` e `estrutura sem redação completa`, medindo distância do estilo baseline, quantidade de edição, satisfação e clareza.

**Riscos/limites.** O estudo compara Japão e EUA em emails hierárquicos e envolve tradução. Não justifica inferir cultura por nacionalidade nem automatizar estereótipos.

Fonte: https://arxiv.org/abs/2609.26403

---

## 4. Uma IA “neutra” pode alterar temporariamente quais valores ficam mais salientes

**Descoberta.** Mesmo quando instruída a apoiar raciocínio sem recomendar uma decisão, a interação com LLMs pode alterar temporariamente prioridades de valor.

**Evidência.** Em estudo pré-registrado com 200 adultos dos EUA, participantes interagiram com ChatGPT, Claude ou Gemini como “thinking partner”, ou receberam considerações fixas geradas por IA. As três condições interativas deslocaram temporariamente prioridades para maior foco pessoal em relação ao controle, com efeitos de `d=0,37–0,51`, principalmente via Self-Enhancement. Uma tarefa depois, as diferenças praticamente desapareceram e não sobreviveram à correção estatística.

**Mecanismo.** A própria seleção de perguntas, enquadramentos, exemplos e linguagem durante a conversa pode tornar alguns valores mais cognitivamente disponíveis, mesmo sem persuasão explícita.

**Implicação para produto.** Em fluxos de decisão, “não dar uma recomendação” não basta para garantir neutralidade. Agentes deveriam registrar quais objetivos/valores estão sendo enfatizados, permitir contraste entre alternativas e evitar enquadramento unilateral invisível.

**Experimento possível.** Para decisões relevantes, comparar uma conversa livre contra uma interface que explicita múltiplos critérios de valor e oferece uma etapa de contraste antes da escolha.

**Riscos/limites.** O efeito foi temporário, em adultos dos EUA, e não houve convergência detectável de decisões. É um tema sensível de influência; não deve ser transformado em técnica de persuasão comercial.

Fonte: https://arxiv.org/abs/2609.25586

---

## 5. Especialistas querem delegar execução, não intenção narrativa

**Descoberta.** Em visual data storytelling, especialistas raramente tratam o LLM como “autor autônomo”. Eles preferem delegar produção e manter sob controle humano o significado da história.

**Evidência.** O trabalho aceito no IEEE VIS 2026 entrevistou 12 especialistas em visual data storytelling. A assistência foi considerada mais produtiva depois de o humano definir intenção e restrições. O uso do LLM deslocou trabalho de produção para verificação.

**Mecanismo.** Atividades de execução são mais fáceis de verificar contra uma intenção já definida. Já objetivo narrativo, seleção do que importa e responsabilidade por claims exigem julgamento contextual e autoria.

**Implicação para produto.** No Marketing Hub, agentes de criação deveriam receber `intent locks`: audiência, mensagem central, claims permitidos, claims proibidos, prioridade e critério de sucesso. O modelo pode gerar variações, mas não redefinir silenciosamente esses elementos.

**Experimento possível.** Comparar geração end-to-end contra `human-seeded intent + geração + verification gate`, medindo retrabalho, drift de mensagem, claims incorretos e tempo total.

**Riscos/limites.** É estudo qualitativo com 12 especialistas e específico de visualização de dados. Não demonstra que essa divisão maximiza conversão.

Fonte: https://arxiv.org/abs/2609.25700
Versão IEEE VIS: https://ebay.ieeevis.org/year/2026/program/paper/d8023d9f-4d7e-4873-97c1-de94c9fabc92/

---

## 6. Vídeo personalizado por IA já consegue qualidade de artefato razoável em escala — mas ainda não prova impacto no espectador

**Descoberta.** O sistema Bespoke mostra que uma pipeline estruturada consegue regenerar vídeos completos para públicos de setores diferentes com custo baixo e qualidade avaliada como aceitável por especialistas.

**Evidência.** A partir de 31 aulas, foram gerados 209 vídeos personalizados para healthcare, finance, energy e audiência genérica. Vinte e cinco especialistas avaliaram 92 vídeos: 87% ficaram no nível ou acima do ponto de referência “comparável a uma aula MOOC padrão”; média global 3,42/5 e 48% com nota 4 ou mais. O custo de API foi cerca de US$ 0,22 por minuto. Os principais problemas restantes foram voz, sincronização slide–narração e renderização/layout.

**Mecanismo.** A pipeline define objetivos antes do conteúdo, recupera contexto do domínio, gera narração e visuais, usa validação separada e itera sobre falhas. A personalização é tratada como transformação estruturada, não apenas troca de palavras.

**Implicação para produto.** Para vídeo comercial, o padrão mais útil é `brief/objetivo → adaptação de exemplos → geração → validador independente → revisão de voz/sync/layout`, em vez de um prompt único.

**Experimento possível.** Comparar vídeo genérico e versão adaptada ao contexto do nicho mantendo oferta e duração constantes; medir retenção, compreensão, CTA e rejeição.

**Riscos/limites.** O estudo mede qualidade do artefato por especialistas, não aprendizagem, preferência do espectador ou conversão. É contexto educacional.

Fonte: https://arxiv.org/abs/2609.26540

---

## 7. Em sistemas de recomendação, algoritmo e agência humana precisam ser estudados juntos

**Descoberta.** Uma revisão peer-reviewed na *Nature Computational Science*, publicada em 21/09, argumenta que muitos efeitos atribuídos a algoritmos de ranking/recomendação também dependem fortemente de auto-seleção e preferências do usuário.

**Evidência.** A revisão sintetiza a literatura de plataformas e considera inconclusiva a evidência que atribui, de forma geral, resultados problemáticos exclusivamente à curadoria algorítmica. Os autores defendem desenho causal que trate algoritmo e agência humana como partes de um mesmo sistema.

**Mecanismo.** O comportamento observado emerge de feedback loops: o sistema altera o que é exposto, a pessoa escolhe o que consumir, e essas escolhas realimentam o sistema.

**Implicação para produto.** Em adaptive UX, evitar concluir “a recomendação causou o comportamento” apenas porque comportamento e recomendação se correlacionam. Guardar exposições, escolhas, recusas e alternativas disponíveis permite distinguir melhor seleção do usuário de influência do algoritmo.

**Experimento possível.** Em recomendações de conteúdo/oferta, registrar controle ou randomização parcial de exposição e comparar efeito incremental contra preferências basais.

**Riscos/limites.** É uma revisão, não um novo experimento, e foca plataformas de conteúdo. O achado deve melhorar desenho de medição, não ser usado para minimizar riscos algorítmicos.

Fonte: https://www.nature.com/articles/s43588-026-01038-1

---

## Atualização do Experience Engine — v23

```text
usuário + intenção + estado
          ↓
SUBSTANTIVE JUDGMENT
├─ fatos / evidência
├─ recomendação independente
└─ limites
          ↓
RECEPTIVE COMMUNICATION
├─ reconhecer perspectiva
├─ terreno comum real
└─ discordar construtivamente
          ↓
AUTHORSHIP / STYLE GUARD
├─ preservar intenção
├─ preservar estilo quando desejado
└─ não inferir cultura por estereótipo
          ↓
DELEGATION BOUNDARY
├─ intenção humana protegida
├─ execução delegável
└─ verificação obrigatória
          ↓
GENERATIVE ARTIFACT VALIDATION
├─ qualidade
├─ acessibilidade / coerência
├─ voz / sync / layout
└─ resultado observável
          ↓
INFLUENCE TRACE
├─ o que foi sugerido?
├─ que valores/frames foram salientados?
└─ que escolha o usuário fez?
          ↓
resultado real + agência + autoria + rastreabilidade
```

## Cards derivados

Foi criado **um único card**, por ser o achado com melhor combinação de evidência, aplicabilidade e aderência a uma coleção aceita:

- `receptividade-sem-deferencia-conversa-ai`
- coleção: `neuromarketing`
- importância: permite ao `customer-agent` ser mais fácil de ouvir sem transformar calor ou validação social em concordância automática.
- fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-23-receptividade-sem-deferencia-conversa-ai.md`
- SHA-256: `6244305ab7f9dac2bfce1ba409e5c67be40d0296b3e453a71a45d03a8d5cf299`
- JSON: `pesquisas/design-experiencia/cards/2026-09-23-receptividade-sem-deferencia-conversa-ai.json`
- estado pretendido: candidato a `DRAFT`; não enviado para revisão, não ativado e não arquivado.

### Achados fortes sem card

- **Prompt-to-design / Figma Make:** forte, mas pertence principalmente a arquitetura de Generative UI e produtividade de design; nenhuma coleção atual representa isso sem distorção.
- **Delegation boundaries em visual storytelling:** forte para harness/autoria, mas sem coleção de agentes/harness.
- **Value Compass:** forte, porém trata influência sobre valores; mantido apenas como alerta de design, não como orientação comercial.
- **Emails e estilo cultural:** poderia ser aproximado de `neuromarketing`, mas a generalização é estreita e há risco de transformar adaptação em estereótipo; mantido no relatório.
- **Bespoke:** encaixa em `video`, mas ainda mede qualidade do artefato, não efeito humano/comercial; mantido no relatório até evidência de resultado no espectador.

## Guia de cards consultado

A versão atual de `harness-library-api/docs/guia-uso-api-cards.md` continua aceitando somente as coleções `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. O fluxo editorial permanece `DRAFT -> IN_REVIEW -> ACTIVE -> ARCHIVED`, e commit de candidato não autoriza revisão ou ativação automática.
