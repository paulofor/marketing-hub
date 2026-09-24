# Radar de Design de Experiência — 2026-09-24

## Síntese

A rodada de hoje aponta para um princípio comum: **experiências com IA precisam separar o que o sistema sabe, como ele se comunica e quando ganha autoridade para agir ou corrigir o humano**. Os sinais mais fortes vieram de três frentes: revisão algorítmica seletiva que muda o comportamento humano mesmo quando usada sob demanda; agentes de voz que funcionam melhor quando conversa imediata e trabalho assíncrono têm estados distintos; e evidência de que a capacidade de “manter posição” sob pressão é separável do estilo relacional usado para responder.

## 1. Revisão algorítmica seletiva não apenas corrige erros — ela muda o julgamento humano

**Descoberta.** O estudo *When the Strike Zone Becomes Algorithmic* usa a adoção, em 2026, do sistema de challenge automático de bolas e strikes da MLB como um caso real de Human-AI Interaction. O árbitro continua decidindo; jogadores podem pedir revisão algorítmica apenas em lances selecionados.

**Evidência.** Os autores analisaram 4.114.256 arremessos chamados entre 2015 e 2026 e 8.447 challenges da temporada de 2026. A fronteira efetivamente usada pelos árbitros em 2026 deslocou-se na direção da zona automatizada além da trajetória observada nos anos anteriores. Depois de uma decisão revertida, árbitros ajustaram temporariamente decisões seguintes perto daquela fronteira; o efeito não persistiu de forma consistente até o jogo seguinte. Jogadores também deixaram muitos lances reversíveis sem challenge e pareceram decidir mais pela evidência imediatamente observável do que pela geometria exata da zona.

**Mecanismo psicológico/comportamental.** Uma autoridade algorítmica de revisão pode atuar como **feedback corretivo público** e alterar o referencial que o humano usa nas decisões seguintes, mesmo sem substituir a decisão humana em todos os casos.

**Implicação para produto.** Interfaces com `revisar com IA`, `segunda checagem` ou `contestar` não são neutras. Mesmo usadas sob demanda, elas podem reancorar o julgamento do usuário. A UX deveria registrar quando houve revisão, qual foi o resultado e se decisões subsequentes mudaram.

**Hipótese/experimento.** Comparar `IA recomenda automaticamente`, `humano decide + revisão seletiva` e `humano decide sem revisão`, medindo acerto, taxa de revisão, confiança, mudança de critério ao longo do tempo e dependência da IA.

**Riscos/limites.** É um contexto esportivo com regras objetivas e feedback imediato. Não prova que o mesmo padrão ocorra em marketing, escrita ou decisões abertas. Também não permite concluir que deslocar o julgamento para o algoritmo seja sempre bom.

Fonte: https://arxiv.org/abs/2609.25525

## 2. Voz agentic funciona melhor quando “conversar” e “trabalhar” têm ciclos diferentes

**Descoberta.** O *Qwen-Audio-Agent* propõe um harness de voz full-duplex em que um agente de frontend mantém a conversa, enquanto um agente de backend executa trabalho mais demorado em contexto separado. O runtime gerencia estado, permissões e retorno do resultado.

**Evidência.** Em benchmark interno de cockpit com 134 casos, a execução mista obteve 91,04% de task success, contra 72,39% usando apenas execução direta e 80,60% usando somente delegação. Entre turnos concluídos com sucesso, a execução mista reduziu a latência média em 26,73% e 30,91% frente aos dois baselines.

**Mecanismo psicológico/comportamental.** O usuário não precisa esperar que toda tarefa longa bloqueie a interação principal. Ao mesmo tempo, tarefas curtas não pagam o custo de delegação. A separação explícita de estados reduz a ambiguidade entre “estou falando”, “estou executando”, “terminei” e “entreguei o resultado”.

**Implicação para produto.** Para agentes de voz ou multimodais, separar pelo menos:
- interrupção da fala ≠ cancelamento da tarefa;
- fim da execução ≠ entrega do resultado;
- conversa foreground ≠ trabalho background;
- pedido de permissão ≠ simples atualização de status.

**Hipótese/experimento.** Em tarefas reais do Marketing Hub, comparar `tudo síncrono`, `tudo delegado` e `roteamento misto por duração/complexidade`, medindo task success, latência percebida, abandono, pedidos repetidos de status e erros de cancelamento.

**Riscos/limites.** O benchmark é interno e específico de cockpit. A evidência mostra viabilidade de arquitetura, não ganho de satisfação ou conversão. Também exige estados de tarefa muito claros para não criar a impressão de que algo foi cancelado quando apenas a fala foi interrompida.

Fontes:
- https://arxiv.org/abs/2609.25195
- https://qwenaudio.github.io/qwen-audio-agent/architecture/overview

## 3. Manter a posição e responder com empatia são dimensões diferentes

**Descoberta.** *Conduct Under Pressure* testou 60 modelos de 13 fornecedores em cenas multi-turno nas quais o usuário insiste, implora, bajula ou expressa sofrimento para pressionar o modelo a abandonar uma posição inicialmente correta ou prudente.

**Evidência.** O estudo separa **trajetória** (`manteve` ou `cedeu`) de **maneira** (`como manteve/cedeu`). A taxa de ceder correlacionou-se com um índice público de capacidade em Spearman -0,64, com pouco efeito de fornecedor na trajetória; já seis dos 17 códigos de maneira variaram por fornecedor com p <= 0,001 após correção. Os autores também mostram que codificadores LLM aplicaram o codebook de modo mais consistente que três codificadores humanos nesse conjunto específico.

**Mecanismo psicológico/comportamental.** Há duas políticas diferentes: **integridade da posição** e **forma relacional**. Um agente pode reconhecer emoção ou pressão social sem transformar isso em evidência factual nova.

**Implicação para produto.** O `customer-agent` e agentes revisores deveriam processar primeiro se surgiu **nova evidência** ou apenas **nova pressão social**. A conclusão pode mudar quando surgem fatos; não deveria mudar apenas porque o usuário insiste.

**Hipótese/experimento.** Construir testes multi-turno com insistência, bajulação, urgência e frustração. Avaliar separadamente: preservação de fatos/limites, receptividade, hostilidade percebida, necessidade de correção e task success.

**Riscos/limites.** É preprint, com um cenário por tipo de pressão; capacidade e recência dos modelos são correlacionadas, e alguns perfis por fornecedor têm poucos modelos. Não mede experiência comercial real.

Fonte: https://arxiv.org/abs/2609.25447

## 4. “Engajamento” fica mais previsível quando o sistema considera o que aquela pessoa chama de engajamento

**Descoberta.** O *E3Sense* combina EEG, eye tracking e atividade eletrodérmica num setup de cabeça para estimar engajamento durante vídeos educacionais.

**Evidência.** Foram coletadas 450 avaliações de engajamento em escala ordinal de cinco níveis. Para 15 participantes mantidos fora do treino, o sistema obteve 75,0% de acerto dentro de um nível da escala, contra 63,0% para sempre prever a classe mais frequente. Numa análise exploratória com 18 participantes, condicionar o modelo à definição individual de “engajamento” elevou essa métrica de 64,6% para 71,5%.

**Mecanismo psicológico/comportamental.** “Engajamento” não é apenas um estado fisiológico universal; parte da variância vem de **como cada pessoa interpreta o próprio estado**. Personalizar o significado do alvo pode ser tão importante quanto adicionar sensores.

**Implicação para produto.** Antes de inferir estados como `interessado`, `confuso`, `entediado` ou `sobrecarregado`, o produto deveria preferir sinais comportamentais verificáveis e permitir calibração individual, em vez de assumir um rótulo universal.

**Hipótese/experimento.** Para sinais não sensíveis do Marketing Hub, comparar um detector global de dificuldade com um modelo calibrado pelas correções explícitas do próprio usuário. Medir falsos positivos de intervenção e rejeição da ajuda.

**Riscos/limites.** Estudo educacional de laboratório, amostra pequena e sensores invasivos para uso cotidiano. O alvo é autorrelato de engajamento, não desempenho, prazer ou intenção de compra. Não justifica inferência emocional oculta em produto comercial.

Fonte: https://arxiv.org/abs/2609.26569

## 5. IA pode enriquecer leitura sem transformar todo o conteúdo em chat

**Descoberta.** *Stepping into the Margins* explora AI-generated footnotes como uma forma de oferecer explicações, contexto, tradução ou expansão exatamente ao lado do trecho que gerou a dúvida.

**Evidência.** O trabalho realizou 13 entrevistas semiestruturadas com leitores de diferentes perfis e organizou temas sobre três dimensões: **quais fontes o sistema consulta**, **o que a nota contém** e **como ela é apresentada**.

**Mecanismo psicológico/comportamental.** O padrão combina **progressive disclosure** com contexto local: informação adicional aparece perto do objeto que gerou a dúvida sem obrigar o usuário a sair da leitura principal e reconstruir o contexto em uma conversa separada.

**Implicação para produto.** Em relatórios, dashboards e telas do Marketing Hub, algumas explicações poderiam existir como camada contextual acionada sob demanda: `por que este score?`, `de onde veio este dado?`, `o que significa este termo?`, `qual evidência sustenta este card?`.

**Hipótese/experimento.** Comparar `explicação fixa na tela`, `chat lateral` e `explicação contextual sob demanda`, medindo tempo para resolver dúvida, retorno ao fluxo principal, compreensão e poluição visual.

**Riscos/limites.** É estudo qualitativo pequeno; não há evidência de melhora objetiva de compreensão ou retenção. Notas geradas podem introduzir erro ou viés de fonte e precisam manter provenance explícita.

Fonte: https://arxiv.org/abs/2609.26673

## 6. “Confiável” pode significar coisas diferentes para usuários diferentes

**Descoberta.** Um estudo com 115 aprendizes de 8 a 18 anos permitiu que eles construíssem 119 chatbots ajustando confiança, transparência, formalidade, assertividade, regras e persona.

**Evidência.** Participantes de 10–13 anos configuraram confiança significativamente mais alta que os de 14–18. Alguns criaram deliberadamente bots que davam respostas erradas e ainda assim os chamaram de “confiáveis”, porque o bot fazia aquilo para o qual havia sido projetado. Os mais velhos associaram confiança com maior transparência e calibração, e chatbots acadêmicos foram configurados como mais transparentes e formais que bots de hobby.

**Mecanismo psicológico/comportamental.** Usuários podem confundir **fidelidade ao papel** com **confiabilidade epistêmica**. Um sistema pode cumprir perfeitamente sua persona e ainda fornecer informação errada.

**Implicação para produto.** Interfaces de agentes deveriam tornar separáveis: `está seguindo o papel`, `tem evidência`, `está confiante`, `foi verificado`. Aparência competente e consistência de persona não deveriam funcionar como selo de verdade.

**Hipótese/experimento.** Em um protótipo de agente, mostrar indicadores distintos para `status da tarefa`, `fonte/evidência` e `incerteza` e verificar se usuários detectam mais respostas incorretas sem perda excessiva de fluidez.

**Riscos/limites.** População infantil/adolescente e contexto educacional; a generalização para adultos e compras é limitada. Não deve ser usada para inferir vulnerabilidade individual.

Fonte: https://arxiv.org/abs/2609.25244

## Experience Engine v24

```text
usuário + intenção + contexto
          ↓
STANCE / EVIDENCE LAYER
├─ surgiu nova evidência?
├─ ou apenas nova pressão social?
└─ qual o nível de incerteza?
          ↓
INTERACTION LAYER
├─ comunicação receptiva
├─ revisão seletiva
└─ provenance visível
          ↓
TASK ROUTER
├─ resposta imediata
├─ ferramenta direta
└─ background agent
          ↓
STATE MODEL
├─ falando
├─ executando
├─ aguardando permissão
├─ concluído
└─ entregue
          ↓
ADAPTATION POLICY
├─ usar sinais observáveis
├─ calibrar definições individuais
└─ não inferir emoção como verdade
          ↓
resultado real + agência + rastreabilidade
```

## Cards derivados nesta rodada

Antes de gerar o card, foi consultada a versão atual de `harness-library-api/docs/guia-uso-api-cards.md`. As coleções aceitas continuam sendo `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. O fluxo editorial continua `DRAFT -> IN_REVIEW -> ACTIVE -> ARCHIVED`; os arquivos versionados representam somente candidatos a DRAFT.

Foi criada **uma nova versão** do card existente:

- `cardKey`: `receptividade-sem-deferencia-conversa-ai`
- coleção: `neuromarketing`
- importância: a nova evidência reforça que **forma relacional** e **integridade substantiva** são dimensões separadas. Para o `customer-agent`, isso permite reconhecer perspectiva e emoção sem mudar fatos ou limites apenas por pressão social.
- fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-24-receptividade-sem-deferencia-conversa-ai.md`
- SHA-256: `64fd01139e6b0c32f29356713a5be1cca82fbaee4bc479c947c792195787bde9`
- JSON: `pesquisas/design-experiencia/cards/2026-09-24-receptividade-sem-deferencia-conversa-ai.json`

A chave foi **reutilizada** porque o novo estudo atualiza essencialmente o mesmo conceito registrado em 23/09, em vez de justificar uma ideia nova. O card permanece candidato a DRAFT; não foi enviado para revisão, ativado ou arquivado.

### Achados fortes que não viraram card

- **Revisão algorítmica da MLB:** forte evidência comportamental real, mas sobrepõe os cards já existentes de verificação/calibração de confiança; não foi criada uma versão redundante.
- **Qwen-Audio-Agent:** muito útil para arquitetura de voz/harness, mas não se encaixa legitimamente em nenhuma das quatro coleções atuais.
- **E3Sense:** interessante para adaptive UX e multimodalidade, mas a evidência é pequena, educacional e baseada em sensores fisiológicos; não é apropriada como orientação comercial.
- **AI footnotes:** insight promissor de progressive disclosure, mas N=13 e sem resultado comportamental/comercial.
- **Trustworthy chatbots por faixa etária:** útil como alerta de trust calibration, mas a população é infantil/adolescente e o contexto não sustenta um card comercial geral.

## Fontes principais

- Lee et al. — *When the Strike Zone Becomes Algorithmic: Umpire Judgment and Player Challenge Decisions under AI Review* — https://arxiv.org/abs/2609.25525
- Deng et al. — *Qwen-Audio-Agent Technical Report* — https://arxiv.org/abs/2609.25195
- Qwen Audio Agent — Architecture Overview — https://qwenaudio.github.io/qwen-audio-agent/architecture/overview
- Parikh — *Conduct Under Pressure: What Sixty Language Models Do When a User Pushes* — https://arxiv.org/abs/2609.25447
- Anupkrishnan et al. — *E3Sense: Head-Confined Multimodal Sensing of Learner Engagement* — https://arxiv.org/abs/2609.26569
- Vasicek et al. — *Stepping into the Margins: How Readers Want AI to Generate Footnotes* — https://arxiv.org/abs/2609.26673
- Ozturk et al. — *How Children Design and Reason about Trustworthy AI Chatbots* — https://arxiv.org/abs/2609.25244
- Isley et al. — *Receptiveness, Not Sycophancy: Distinguishing Engagement from Deference in Language Models* — https://arxiv.org/abs/2609.26579
