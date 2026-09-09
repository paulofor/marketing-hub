# Radar de Design de Experiência — 2026-09-09

## Síntese executiva

A rodada de 9 de setembro aponta para uma evolução importante do design de experiências com IA: **o sistema precisa administrar três orçamentos simultaneamente — atenção, autoridade e segurança psicológica**.

Os achados de hoje mostram que proatividade e adaptação podem ajudar, mas não deveriam ser maximizadas. Conteúdo adicional inesperado aparece associado a burnout digital; em decisões apoiadas por IA, densidade de informação e pressão de tempo aparecem fortemente ligadas à carga cognitiva; interfaces neuroadaptativas já conseguem fechar o loop `sinal cognitivo → adaptação → nova interação`; e um estudo longitudinal curto com prática por voz sugere que IA pode criar um espaço de ensaio psicologicamente mais seguro quando permite repetição e autorregulação.

No lado dos agentes, dois sinais práticos reforçam uma mesma arquitetura: usuários formam modelos mentais diferentes conforme o papel funcional do agente, e o lançamento do Bybit AI mostra uma aplicação real de **autoridade delimitada**, com a IA operando em uma subconta isolada em vez de ganhar acesso irrestrito à conta principal.

O padrão de produto que emerge é: **proatividade com limite, informação em camadas, papel funcional explícito, adaptação baseada em estado e autonomia com blast radius controlado**.

## 1. Informação inesperada com IA tem custo de atenção e aparece associada a burnout digital

**Descoberta.** O artigo peer-reviewed *AI information encounters and university students’ digital burnout: the mediating role of cognitive flexibility and the moderating effect of AI learning trust*, publicado em 7 de setembro de 2026 na *Frontiers in Psychology*, estudou o efeito de encontros informacionais gerados por IA — conteúdo adicional ou inesperado que aparece durante uma interação, mesmo sem busca explícita do usuário.

**Evidência.** O survey transversal reuniu **566 respostas válidas**. Encontros informacionais com IA estiveram positivamente associados a digital burnout. A flexibilidade cognitiva mediou parcialmente essa relação: mais encontros informacionais estiveram associados a menor flexibilidade cognitiva, enquanto maior flexibilidade esteve associada a menor burnout. Maior confiança em IA atenuou parte das associações.

**Mecanismo.** A evidência não prova causalidade, mas sustenta uma hipótese de design importante: **serendipidade também consome atenção**. Uma sugestão adicional pode ser útil isoladamente, mas muitas intervenções inesperadas aumentam o número de coisas que o usuário precisa processar, priorizar ou rejeitar.

**Implicação para produto.** Proatividade precisa de um **Attention Budget**. Um agente pode calcular `relevância × urgência × novidade × custo de interrupção` e mostrar espontaneamente somente o que ultrapassar um limiar. O restante fica disponível em uma área de descobertas ou sob demanda.

**Experimento possível.** Comparar três versões: A) agente com muitas sugestões espontâneas; B) no máximo duas sugestões priorizadas por etapa; C) nenhuma sugestão espontânea, apenas sob demanda. Medir conclusão, abandono, tempo, número de sugestões abertas, satisfação e percepção de sobrecarga.

**Riscos/limites.** Estudo transversal, autorrelatado e educacional. Não permite afirmar que a proatividade causa burnout nem generalizar diretamente para comércio digital.

Fonte: https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1897395/full

## 2. Progressive disclosure aparece como uma das intervenções mais promissoras para carga cognitiva em decisões com IA

**Descoberta.** *Multidimensional Cognitive-State Modeling and an Adaptive Cognitive Load Twin for AI-Supported Managerial Decision-Making*, publicado em 27 de agosto de 2026 na revista *Data*, modelou carga cognitiva, fadiga decisória, explicabilidade, confiança, viés de automação, controle humano, qualidade percebida e acurácia objetiva separadamente.

**Evidência.** Foram **420 profissionais de logística e supply chain**, cada um realizando quatro cenários de decisão apoiada por IA, totalizando **1.680 observações**. Densidade de informação e pressão de tempo apareceram entre os correlatos de design mais consistentes de carga cognitiva e fadiga. Maior carga cognitiva esteve associada a menor acurácia objetiva. Maior explicabilidade percebida e maior controle humano estiveram associados a melhores avaliações e menor tendência a viés de automação. Nas simulações baseadas no modelo, reduzir densidade e usar **progressive disclosure** apareceram como intervenções promissoras.

**Mecanismo.** Em vez de tentar tornar toda a interface simples, o sistema pode tornar **simples o próximo passo**. Detalhes ficam disponíveis, mas não competem simultaneamente pela atenção.

**Implicação para produto.** Em recomendações de agentes: `decisão sugerida → evidência principal → próxima ação`, com métricas secundárias, histórico e explicações profundas expansíveis.

**Experimento possível.** Comparar painel completo versus apresentação em camadas. Medir tempo até decisão, taxa de inspeção de evidências, reversões, erros e carga percebida.

**Riscos/limites.** Amostra purposive em um país e cenários controlados. A recomendação de progressive disclosure é apoiada por modelagem/simulação e precisa de teste causal direto no produto.

Fonte: https://www.mdpi.com/2306-5729/11/9/215

## 3. IA por voz pode funcionar como um espaço de ensaio psicologicamente seguro — desde que o usuário consiga negociar e corrigir o papel do agente

**Descoberta.** Um artigo da *Frontiers in Psychology*, aceito em **7 de setembro**, comparou oito semanas de prática de interpretação com um agente GenAI por voz contra role-play tradicional entre pares.

**Evidência.** Foram **70 universitários**, divididos em duas turmas intactas de 35. O grupo com IA teve desempenho significativamente melhor em interpretação, especialmente em precisão informacional e gerenciamento da interação, além de ansiedade significativamente menor com grande tamanho de efeito. Engajamento não diferiu significativamente. As entrevistas apontaram **segurança psicológica** e possibilidade de repetir tentativas até dominar a atividade como benefícios, mas também identificaram instabilidade do papel do agente e feedback corretivo limitado.

**Mecanismo.** O ganho potencial não parece vir apenas da voz. A experiência permite `tentar → errar → ajustar → repetir` sem parte do julgamento social que acompanha prática com outra pessoa. Ao mesmo tempo, a qualidade cai quando o agente muda de papel ou deixa de dar feedback suficientemente preciso.

**Implicação para produto.** Criar um **Rehearsal Mode** para tarefas que envolvem preparo antes de uma situação real: entrevista, apresentação, negociação, atendimento, demonstração, pitch ou treinamento de agentes. O usuário deveria poder interromper e dizer “volte ao papel X”, “seja mais difícil”, “avalie apenas Y”.

**Experimento possível.** Para uma tarefa de preparo, comparar tutorial, simulação com agente e simulação com agente + repetição orientada. Medir autoeficácia, conclusão real da tarefa posterior e quantidade de correções necessárias.

**Riscos/limites.** Apenas uma turma por condição, portanto comparações entre grupos são exploratórias. Contexto educacional especializado; mecanismos psicológicos ainda não foram isolados experimentalmente.

Fonte: https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1914690/abstract

## 4. O papel funcional do agente molda o modelo mental do usuário

**Descoberta.** *Bug Detective and Quality Coach: Developers’ Mental Models of AI-Assisted IDE Tools*, publicado em *Computers in Human Behavior Reports* em 2026, realizou seis workshops de co-design com **58 desenvolvedores**.

**Evidência.** Para detecção de bugs, participantes imaginaram o agente como um **“bug detective”**: deve interromper principalmente em problemas críticos, mostrar confiança/evidência e oferecer feedback acionável. Para legibilidade de código, imaginaram um **“quality coach”**: contextual, personalizado e progressivo. Nos dois casos, confiança dependia da clareza da explicação, do momento da intervenção e do controle do usuário.

**Mecanismo.** Usuários não formam apenas uma opinião sobre “a IA”; eles constroem um modelo de **qual função aquela IA está exercendo**. O comportamento esperado muda conforme o papel. Um detector deveria ser seletivo; um coach pode interagir mais frequentemente.

**Implicação para produto.** Em vez de uma persona fixa para todo o sistema, definir papéis funcionais explícitos: `detector`, `coach`, `executor`, `reviewer`, `planner`. Cada papel recebe políticas próprias de iniciativa, explicação, frequência e autonomia.

**Experimento possível.** Mesmo agente, mesma capacidade, mas duas condições: persona genérica versus papel funcional explícito com política de comportamento correspondente. Medir correções, expectativa quebrada, confiança e aceitação das sugestões.

**Riscos/limites.** Pesquisa qualitativa com desenvolvedores; não prova impacto causal em adoção nem generaliza automaticamente para consumidores.

Fonte: https://doi.org/10.1016/j.chbr.2026.101157

## 5. Caso real: Bybit lança agente conversacional com autoridade isolada em uma subconta

**Descoberta.** Em **9 de setembro de 2026**, a Bybit anunciou o Bybit AI, uma camada conversacional integrada ao app que interpreta intenção e pode executar ações. A decisão de design mais interessante não é o chat: é o modelo de autorização.

**Evidência/caso.** Usuários elegíveis ativam uma **AI Subaccount** dedicada, isolada do saldo principal. A documentação oficial anterior da subconta descreve controles de permissão, exposição financeira delimitada, autenticação específica e arquitetura em que ações do agente permanecem dentro da conta isolada; material oficial também descreve limite padrão de ativos e retiradas desabilitadas por padrão até alteração explícita do usuário.

**Mecanismo.** Isso implementa **bounded agency**: o usuário não precisa escolher entre “IA só sugere” e “IA controla tudo”. Ele concede um espaço de ação com blast radius conhecido.

**Implicação para produto.** Agentes do Marketing Hub podem ter scopes concretos: orçamento máximo por experimento, campanhas permitidas, capacidade de rascunhar versus publicar, domínios acessíveis, janela temporal e ações que exigem confirmação. A experiência de autonomia passa a incluir visualização clara do perímetro de autoridade.

**Experimento possível.** Comparar agente somente consultivo versus agente com autoridade limitada e reversível. Medir conclusão de tarefas, número de confirmações, confiança calibrada, erros e necessidade de intervenção humana.

**Riscos/limites.** Trata-se de caso de produto e documentação do fornecedor, não estudo independente de UX. O domínio é financeiro e de alto risco; não deve ser usado como evidência de que a arquitetura aumenta conversão ou confiança em outros contextos.

Fontes:
- https://www.prnewswire.com/news-releases/bybit-ai-is-live-one-intelligent-conversational-layer-to-redefine-financial-experience-302873309.html
- https://www.bybit.com/en/help-center/article/Introduction-to-the-AI-Subaccount
- https://www.bybit.com/en/learn/ai-subaccount/first-trade-ai-subaccount

## 6. Interfaces adaptativas já conseguem fechar o loop entre carga cognitiva e mudança visual

**Descoberta.** *Context-Aware Adaptive Visualizations for Critical Decision Making*, publicado online em **25 de agosto de 2026**, apresenta o Symbiotik: um sistema que estima mental workload com sinais neurofisiológicos e usa reinforcement learning para alterar dashboards em tempo real.

**Evidência.** O estudo reuniu **120 participantes**, distribuídos entre três tipos de visualização. EEG e eye tracking foram registrados enquanto usuários respondiam perguntas com diferentes níveis de complexidade visual e de tarefa. O sistema opera em ciclos de aproximadamente dois segundos, estima workload e seleciona entre ausência de adaptação, adaptação parcial ou completa de atributos visuais. Os autores reportam melhora de desempenho e engajamento na abordagem adaptativa.

**Mecanismo.** É um closed loop real: `sinal do usuário → estimativa de estado → decisão de interface → alteração visual → novo comportamento/sinal`. Isso aproxima bastante o conceito de Experience Engine de um sistema tecnicamente implementável.

**Implicação para produto.** Não é necessário começar por EEG. O princípio pode ser aplicado com proxies comportamentais mais baratos: tempo parado, voltas de tela, número de correções, rejeições, expansão frequente de ajuda e ritmo de interação. A adaptação deve ser pequena e reversível.

**Experimento possível.** Criar um dashboard com duas densidades. Se o sistema detectar sinais de dificuldade, reduz informação secundária e aumenta destaque do próximo passo. Comparar com dashboard estático.

**Riscos/limites.** A inferência de workload ainda usa construct preliminar e sensores; o próprio artigo aponta necessidade de melhorar personalização e generalização entre layouts. Neuroadaptação também traz questões de privacidade e consentimento.

Fonte: https://journals.sagepub.com/doi/10.3233/FAIA251433

## Experience Engine v9

```text
Usuário
   ↓
intenção + contexto + histórico
   ↓
ATTENTION BUDGET
   ├── quantas sugestões cabem agora?
   └── qual interrupção realmente merece aparecer?
   ↓
COGNITIVE STATE ESTIMATOR
   ├── comportamento
   └── sinais multimodais opcionais
   ↓
ROLE CONTRACT
   ├── detector
   ├── coach
   ├── executor
   ├── reviewer
   └── planner
   ↓
AUTHORITY BOUNDARY
   ├── escopo
   ├── orçamento
   ├── reversibilidade
   └── confirmação por risco
   ↓
PRESENTATION POLICY
   ├── progressive disclosure
   ├── proactive suggestion
   ├── rehearsal mode
   └── adaptive visualization
   ↓
experiência
   ↓
resultado + carga + controle + confiança calibrada
   ↓
recalibração
```

## Cards gerados nesta rodada

Foram gerados **dois candidatos a DRAFT**, ambos na coleção válida `neuromarketing`.

### 1. `progressive-disclosure-carga-cognitiva-ai`

Importância: traduz evidência recente sobre densidade de informação e carga cognitiva em uma hipótese de UX diretamente testável em recomendações, aprovações e painéis de agentes do Marketing Hub.

Arquivos:
- `pesquisas/design-experiencia/cards/2026-09-09-progressive-disclosure-carga-cognitiva-ai.json`
- `pesquisas/design-experiencia/cards/fontes/2026-09-09-progressive-disclosure-carga-cognitiva-ai.md`
- SHA-256 da fonte: `3398b5a0f0e13f61fcccbb74bc2ec552901bad7ae44f5c6808176b440785c351`

### 2. `proatividade-informacional-orcamento-atencao`

Importância: adiciona uma restrição útil ao conceito de agentes proativos. A hipótese operacional é que poucas sugestões altamente priorizadas produzam melhor experiência que um fluxo constantemente interrompido por descobertas adicionais.

Arquivos:
- `pesquisas/design-experiencia/cards/2026-09-09-proatividade-informacional-orcamento-atencao.json`
- `pesquisas/design-experiencia/cards/fontes/2026-09-09-proatividade-informacional-orcamento-atencao.md`
- SHA-256 da fonte: `3908c0bf23cf03774ad30e67c9b47cd7136dab3c9c6a7a124cb2185a382b26c5`

## Achados fortes que não viraram card

**Bounded agency / AI Subaccount:** muito útil para arquitetura de agentes, mas nenhuma das quatro coleções atuais representa adequadamente governança/autoridade de agente sem distorção.

**Rehearsal Mode com voz:** achado interessante para produtos digitais, mas ainda não possui encaixe forte o suficiente em `video`, `prazer-audio-visual`, `neuromarketing` ou `momentos-de-compra-b2c` para justificar card nesta rodada.

**Role Contract para agentes:** relevante para Human-AI Interaction, mas a evidência é qualitativa e o valor principal é arquitetural; mantido no relatório.

**Neuroadaptive UI:** tem aderência temática a neuromarketing, porém a implementação depende de sinais neurofisiológicos e ainda é distante do uso atual do Marketing Hub; não foi criado card apenas para aumentar quantidade.
