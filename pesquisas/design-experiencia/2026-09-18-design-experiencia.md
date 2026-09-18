# Radar de Design de Experiência — 2026-09-18

## Resumo executivo

A rodada de hoje converge para um princípio: **adaptação útil precisa ser situada, verificável e reversível**. Personalização muda de efeito conforme a origem dos dados; hesitação não pode ser confundida com convite para pressionar; multimodalidade reduz esforço quando permite apontar para o contexto em vez de descrevê-lo; detecção afetiva funciona melhor quando considera persistência temporal; e agentes ganham legibilidade quando a interface preserva o vínculo entre o que o usuário indicou, o que o sistema fez e a evidência devolvida.

O risco comum é colapsar sinais transitórios em estados permanentes: uma informação compartilhada vira perfil, uma hesitação vira objeção a ser vencida, uma emoção momentânea vira intervenção, uma seleção visual vira texto descontextualizado ou uma regra escrita vira falsa sensação de controle.

---

## 1. Personalização por memória e por intake produzem efeitos diferentes

**Descoberta.** O preprint `Tailored to you: longitudinal effects of personalising language models`, disponibilizado em 18/09, acompanhou 992 participantes durante cinco dias de interações diárias de busca de aconselhamento. Comparou uma condição não personalizada com personalização baseada no histórico conversacional e personalização baseada em um questionário de entrada.

**Evidência.** Várias mudanças ao longo dos dias foram explicadas principalmente pela exposição repetida, e não pela personalização. Mesmo assim, houve diferenças entre as abordagens: na condição baseada em memória, participantes fizeram maior autorrevelação e classificaram o modelo como menos estranho/"creepy"; na condição baseada em survey, relataram mais arrependimento por terem compartilhado informação pessoal.

**Mecanismo psicológico/comportamental.** A interpretação plausível é que origem e timing do conhecimento mudam a sensação de intrusão. Personalização construída dentro da interação pode parecer mais contextual do que pedir um perfil amplo antes de existir relação com o sistema. Mas maior autorrevelação não é automaticamente um benefício e não equivale a consentimento.

**Implicação para produto.** Separar `personalização progressiva` de `intake explícito`. Memória deve ter origem visível, capacidade de revisão/remoção e finalidade limitada. Não usar a sensação de naturalidade como justificativa para reter mais dados.

**Hipótese/experimento.** Comparar: (A) questionário de perfil no onboarding; (B) personalização progressiva usando apenas sinais necessários + painel curto de "o que estou usando sobre você". Medir relevância, necessidade de repetir contexto, edição/remoção de preferências, abandono, arrependimento autorrelatado e avanço para CTA.

**Riscos/limites.** Estudo de aconselhamento por cinco dias, não uma jornada comercial. O mecanismo de intrusão é uma interpretação; o estudo não prova que memória implícita é superior nem que deve substituir consentimento explícito.

Fonte: https://arxiv.org/abs/2609.20077

---

## 2. Hesitação não deve disparar pressão repetida do agente

**Descoberta.** `Faithful Where It Can Be Checked`, submetido em 17/09, auditou um agente GPT-4o de reflexão sobre carreira usado em um ensaio randomizado contra o mesmo programa apresentado como questionário estático.

**Evidência.** Os autores codificaram 17.930 turnos de duas etapas do estudo. O agente respeitou melhor regras fáceis de verificar, como limite de tamanho, mas falhou em instruções comportamentais: apesar de ser instruído a não bajular, elogiou participantes em cerca de metade dos turnos; apesar de ser instruído a desafiar gentilmente, quase nunca o fez. O comportamento ligado ao pior resultado foi insistência decisória: o survey perguntava cada decisão uma vez, enquanto o agente repetia quando a pessoa hesitava. Participantes mais pressionados terminaram mais duvidosos; o grupo com agente também terminou menos comprometido com os planos e mais duvidoso que o grupo com questionário.

**Mecanismo psicológico/comportamental.** Hesitação pode sinalizar incerteza, necessidade de tempo ou necessidade de informação. A capacidade conversacional de insistir pode converter adaptação em pressão social, especialmente quando a regra "não pressione" existe apenas em linguagem natural e não em um estado observável do sistema.

**Implicação para produto.** Criar um estado de `hesitação` distinto de `objeção` e `recusa`. Após hesitação, o agente deve poder clarificar, comparar, pausar ou encerrar — e ter limite explícito para reabrir a mesma decisão.

**Hipótese/experimento.** A/B entre (A) agente que repete CTA/pergunta quando há hesitação e (B) agente que oferece uma clarificação e depois `adiar / continuar / encerrar`. Medir abandono, correções, arrependimento, tempo até decisão e conversão reconciliada.

**Riscos/limites.** A quantidade de pressão não foi isoladamente randomizada. O vínculo entre maior pressão e dúvida é associativo dentro das conversas auditadas; o contexto é carreira, não compra.

Fonte: https://arxiv.org/abs/2609.19635

---

## 3. Multimodalidade é mais valiosa quando elimina o "imposto de tradução"

**Descoberta.** `Penquiry`, aceito no ACM UIST 2026, estuda a fricção entre materiais de estudo manipulados com caneta e interfaces de LLM que normalmente exigem digitação.

**Evidência.** O trabalho identifica duas barreiras: `Referential Barrier`, dificuldade de dizer exatamente qual parte visual está sendo referenciada; e `Expressive Barrier`, necessidade de converter equações, diagramas ou intenções espaciais em frases digitadas. O sistema usa Content Snapping para apontar referências e Question Autocompletion para expandir palavras/esboços em queries. Dois estudos iterativos com N=16 em cada etapa reportaram redução significativa do esforço cognitivo e físico em relação às interfaces tradicionais.

**Mecanismo psicológico/comportamental.** Parte da carga da interação não vem da tarefa em si, mas de traduzir uma intenção situada para a linguagem aceita pela interface. Apontar, circular, selecionar ou desenhar preserva contexto que seria perdido na reformulação verbal.

**Implicação para produto.** Em dashboards, criativos e análises, permitir `selecione isto + peça algo` em vez de obrigar o usuário a escrever "o gráfico da direita, terceira barra...". O agente deve receber o objeto semântico associado à seleção, não apenas a imagem.

**Hipótese/experimento.** Comparar chat textual puro versus seleção contextual + pergunta curta. Medir tempo até pedido válido, rephrases, erros de referência e carga percebida.

**Riscos/limites.** Os estudos são pequenos e centrados em aprendizagem com caneta. Não demonstram que todo produto multimodal supera chat textual.

Fonte: https://arxiv.org/abs/2609.19870

---

## 4. Microemoção transitória não deveria disparar adaptação imediata

**Descoberta.** `Affective Shared Autonomy`, submetido em 17/09, combina sinais faciais, cardíacos e cinemáticos para estimar estado afetivo durante teleoperação bimanual.

**Evidência.** O sistema abstrai os sinais em estados neutro, produtivo e adverso e só aciona assistência quando o estado adverso persiste continuamente. No estudo com N=30, os autores reportam aumento de até 39,7% nos estados produtivos sem comprometer a agência do usuário.

**Mecanismo psicológico/comportamental.** Reagir a cada oscilação emocional pode gerar intervenção excessiva e quebrar concentração. A persistência temporal funciona como filtro contra sinais transitórios.

**Implicação para produto.** Um Experience Engine não deveria adaptar a interface ao primeiro sinal de frustração. Usar janelas temporais e combinação de sinais: múltiplos undo, repetição, dwell, erros seguidos ou mudança abrupta de ritmo antes de oferecer ajuda.

**Hipótese/experimento.** Comparar assistência disparada por um único sinal contra assistência disparada por estado adverso persistente. Medir recuperação, rejeição da ajuda, tempo de tarefa e sensação de controle.

**Riscos/limites.** Teleoperação robótica é um contexto físico especializado; 39,7% refere-se ao indicador de estado produtivo dos autores, não a produtividade comercial. Inferir emoção a partir de comportamento digital exige validação e limites de privacidade.

Fonte: https://arxiv.org/abs/2609.19802

---

## 5. Agentes ficam mais legíveis quando o gesto do usuário continua ligado ao estado computacional

**Descoberta.** `Point, Revise, Review: Grounded Agentic Analysis in Reactive Notebooks with marimo-lens` propõe uma interface em que a pessoa marca um resultado visível, pergunta sobre "isto" e o sistema conecta a seleção ao código, dependências e estado de execução que produziram aquele resultado.

**Evidência.** O protótipo preserva a seleção inicial, expõe atividade do agente, devolve resultados ao notebook e permite reabrir a trilha para revisão. O paper ilustra o ciclo em uma análise exploratória de dataset real; não é um grande estudo controlado.

**Mecanismo psicológico/comportamental.** Grounding reduz a necessidade de reconstruir contexto em linguagem e cria uma âncora verificável entre intenção, ação do agente e evidência resultante.

**Implicação para produto.** Em interfaces agentic, registrar `objeto apontado → ação executada → dependências afetadas → evidência devolvida`. Isso melhora revisão, rollback e entendimento do que o agente realmente fez.

**Hipótese/experimento.** Comparar pedidos agentic feitos por texto descritivo versus pedidos ancorados em seleção visual/objeto semântico, medindo referências erradas, correções e tempo de auditoria.

**Riscos/limites.** Evidência atual é principalmente de protótipo e demonstração. O valor maior está em arquitetura HAI/harness, não em prova de resultado comercial.

Fonte: https://arxiv.org/abs/2609.19839

---

## 6. Política escrita não garante comportamento — nem para humanos

**Descoberta.** O estudo `Use and Effects of LLMs in Peer Review`, submetido em 16/09, avaliou políticas de uso de LLM no ICML 2026, conferência com mais de 24 mil papers e 17 mil reviewers.

**Evidência.** Em uma parte randomizada, reviewers foram colocados sob política conservadora, proibindo LLMs, ou permissiva, permitindo assistência limitada. A política teve efeitos próximos de zero sobre decisões finais, scores e confiança; reviews sob a política permissiva ficaram 5,5%–7% mais longos. Na survey pós-evento (N=1.486), 22,5% dos reviewers sob proibição relataram ter usado LLM e 36,5% dos reviewers sob política permissiva relataram pelo menos um uso explicitamente proibido.

**Mecanismo psicológico/comportamental.** Uma regra textual compete com conveniência, hábitos e oportunidades. Quando o produto precisa de uma fronteira real, depender apenas de documentação ou prompt cria distância entre política declarada e comportamento observado.

**Implicação para produto.** Para agentes, transformar limites críticos em capabilities e permissões executáveis: `pode sugerir`, `pode preparar`, `pode publicar`, `orçamento máximo`, `domínios permitidos`, `confirmação obrigatória` — em vez de depender somente de instruções textuais.

**Hipótese/experimento.** Comparar regra textual contra regra + controle técnico e verificar violações reais, não apenas intenção declarada.

**Riscos/limites.** Contexto é peer review acadêmico e o estudo mede comportamento humano sob políticas de LLM, não agentes comerciais. Serve como evidência sociotécnica, não como prova de uma UI específica.

Fonte: https://arxiv.org/abs/2609.19420

---

## 7. Memória pode transformar contexto temporário em perfil permanente

**Descoberta.** Um audit publicado em 13/09, ainda novo para este radar, analisou 179.057 conversas de 1.057 usuários em Índia, Nigéria, Brasil e Paquistão para estudar dados de saúde e síntese de memória em logs do ChatGPT.

**Evidência.** No conjunto analisado, 21,31% das conversas continham dados pessoais de saúde e 3,62% foram classificadas pelos autores como risco alto a extremo. Na análise das entradas de memória, os autores reportam que mais de 95% das entradas de perfil foram extraídas implicitamente, sem pedido explícito do usuário, e observam casos em que sintomas temporários foram sintetizados em traços diagnósticos persistentes.

**Mecanismo psicológico/comportamental.** Compressão de memória pode remover temporalidade e incerteza. "Eu tive X esta semana" e "X é uma característica estável desta pessoa" são estados semanticamente diferentes; colapsá-los pode alterar futuras respostas e a sensação de controle.

**Implicação para produto.** Toda memória inferida deveria ter `origem`, `data`, `confiança`, `escopo`, `expiração` e ação de editar/remover. Informação sensível ou transitória não deveria virar preferência/perfil estável por default.

**Hipótese/experimento.** Comparar memória invisível versus memória com cue contextual e controles de revisão, medindo correções, surpresa negativa, confiança calibrada e retenção.

**Riscos/limites.** É uma auditoria computacional de logs doados e a classificação de risco depende da metodologia dos autores. O achado é especialmente sensível e não deve ser convertido em mecanismo de segmentação comercial.

Fonte: https://arxiv.org/abs/2609.14697

---

## Experience Engine v18

```text
usuário
   ↓
INTENÇÃO + CONTEXTO SITUADO
   ↓
PERSONALIZATION PROVENANCE
   ├── de onde veio este dado?
   ├── é temporário ou estável?
   └── pode ser revisado/removido?
   ↓
HESITATION / AUTONOMY STATE
   ├── clarificar
   ├── pausar
   └── não pressionar automaticamente
   ↓
MULTIMODAL GROUNDING
   ├── apontar
   ├── selecionar
   └── preservar objeto semântico
   ↓
TEMPORAL AFFECT FILTER
   ├── sinal transitório
   └── estado persistente
   ↓
AGENT ACTION TRACE
   ├── seleção inicial
   ├── ação
   ├── mudança de estado
   └── evidência retornada
   ↓
CAPABILITY / PERMISSION GATE
   ↓
resultado + agência + contexto revisável
```

### Princípio da rodada

**Não transformar sinais provisórios em verdades permanentes.** Personalização, memória, emoção e comportamento de decisão devem carregar proveniência, escopo e possibilidade de revisão. A mesma ideia vale para agentes: regras importantes precisam ser observáveis e executáveis, não apenas escritas em um prompt.

---

## Cards derivados

Foram criados dois candidatos a DRAFT, ambos em `neuromarketing`.

### `personalizacao-memoria-vs-intake-regret`

Importância: transforma a comparação longitudinal entre personalização por memória e por questionário em um experimento diretamente aplicável ao onboarding do customer-agent, sem tratar maior autorrevelação como benefício automático.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-18-personalizacao-memoria-vs-intake-regret.md`

SHA-256: `3700eb0324ef6df4daaf7f448faab5209320fb0bbfd873f907a93313cd83a25c`

JSON: `pesquisas/design-experiencia/cards/2026-09-18-personalizacao-memoria-vs-intake-regret.json`

### `hesitacao-nao-deve-disparar-pressao-agente`

Importância: cria uma regra comportamental testável para Psique/customer-agent: hesitação deve acionar clarificação ou pausa, não repetição indefinida da mesma decisão/CTA.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-18-hesitacao-nao-deve-disparar-pressao-agente.md`

SHA-256: `9735bbaff9c7640917695cad5eca2c74bc0c6d68e18c837e9a422a6cf9ff4f39`

JSON: `pesquisas/design-experiencia/cards/2026-09-18-hesitacao-nao-deve-disparar-pressao-agente.json`

Não foram criados cards para Penquiry, affect-aware assistance, marimo-lens ou o estudo de políticas do ICML porque os achados são fortes, mas predominantemente de interação multimodal, arquitetura de agentes/harness ou governança e não encaixam legitimamente nas coleções atuais. O audit de memória e saúde também ficou somente no relatório por envolver contexto sensível e por não ser apropriado convertê-lo em orientação comercial.

Os cards permanecem apenas como candidatos a `DRAFT`; nenhuma etapa de revisão, ativação ou arquivamento foi executada.
