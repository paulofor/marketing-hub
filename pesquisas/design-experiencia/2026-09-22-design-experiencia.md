# Radar de Design de Experiência — 22/09/2026

## Síntese da rodada

A evidência nova desta rodada converge para uma ideia: **adaptação útil não é maximizar personalização, conversa ou assistência; é controlar como essas capacidades alteram decisão, esforço e autonomia**. Quatro distinções aparecem repetidamente nos trabalhos recentes: influência percebida não é igual a influência real; engajamento não é igual a resultado; ajuda não deve remover o ponto de decisão do usuário; e uma interface gerada pode precisar personalizar até a forma como pergunta preferências.

Para o Marketing Hub, isso sugere uma evolução do Experience Engine para um sistema que classifica intenção e risco, escolhe persona e modalidade conforme a tarefa, preserva pontos de decisão humanos e adiciona fricção proporcional quando aceitar uma recomendação de IA tem consequência maior.

---

## 1. Personalização contextual pode mudar decisões sem aumentar confiança declarada

**Descoberta.** O preprint *Annie, Are You Okay? How Style- and Context-Based Personalization Shape AI-Assisted Decision-Making*, submetido em 21/09, mostra que personalização pode influenciar comportamento sem que o usuário relate aumento correspondente de confiança ou correção percebida.

**Evidência.** Em experimento pré-registrado 2×2 com N=240, participantes ranquearam três ações comparáveis, conversaram com uma IA e depois reranquearam as opções. Tanto personalização de estilo quanto de contexto foram percebidas pelos participantes, mas apenas a personalização contextual alterou de forma confiável o comportamento: aumentou reconsideração e deslocou rankings na direção da recomendação atribuída à IA. Os participantes se sentiram mais influenciados sem avaliar a IA como mais correta, confiável, inteligente, simpática ou de maior qualidade. Exploratoriamente, pessoas com menor expertise mostraram maior suscetibilidade.

**Mecanismo psicológico/comportamental.** A personalização contextual pode aumentar relevância percebida e tornar a recomendação mais fácil de integrar ao próprio raciocínio. Isso pode produzir influência comportamental antes que a pessoa conscientemente atualize sua confiança no sistema.

**Implicação para produto.** Personalização não deveria ser otimizada apenas por aceitação da recomendação. O Marketing Hub deveria separar `relevância percebida`, `mudança de decisão`, `confiança` e `resultado real`, principalmente quando a IA recomenda escolhas de maior consequência.

**Hipótese/experimento.** Comparar recomendação neutra, adaptação de estilo e adaptação contextual, mantendo o conteúdo factual constante. Medir mudança de escolha, confiança, compreensão da justificativa, necessidade de revisão e resultado posterior.

**Riscos/limites.** O domínio foi decisão financeira experimental e o trabalho ainda é preprint. O resultado não demonstra aumento de qualidade decisória e não deve ser usado para justificar personalização persuasiva ou inferência de atributos sensíveis.

Fonte: [arXiv:2609.24644](https://arxiv.org/abs/2609.24644)

---

## 2. Uma pequena justificativa antes de aceitar pode reduzir adoção acrítica de sugestões ruins

**Descoberta.** *Think Before You Accept: Can Written Justification Reduce Uncritical Uptake of AI Writing Suggestions?*, submetido em 20/09, oferece uma intervenção concreta para calibrar deferência à IA.

**Evidência.** Em experimento randomizado incorporado a uma atividade de curso (N=129), estudantes receberam sugestões de revisão de IA com qualidade mista. No grupo que precisava escrever uma justificativa antes de aceitar ou rejeitar uma sugestão, a adoção das sugestões defeituosas caiu de 65% para 41%, uma diferença de 24 pontos percentuais. A aceitação das sugestões corretas não caiu: 81% no controle e 86% no grupo com justificativa. A análise qualitativa também encontrou justificativas superficiais e lacunas de monitoramento metacognitivo.

**Mecanismo psicológico/comportamental.** A justificativa cria uma fricção metacognitiva curta: interrompe a resposta automática de aceitar e exige uma checagem mínima de coerência. Porém, explicar a decisão pode virar racionalização superficial; a intervenção não garante pensamento profundo.

**Implicação para produto.** Em decisões críticas, um checkpoint do tipo `por que aceitar?`, `qual evidência sustenta isto?` ou `qual critério foi decisivo?` pode ser mais útil que um disclaimer permanente dizendo apenas que a IA pode errar.

**Hipótese/experimento.** No Marketing Hub, aplicar a fricção somente em ações de maior consequência — aprovação de claim, gasto, publicação, alteração importante de oferta — e comparar com aprovação de um clique. Medir adoção de recomendações incorretas, tempo, abandono e taxa de reversão posterior.

**Riscos/limites.** O estudo ocorreu em contexto acadêmico de escrita. Fricção aplicada a toda microação pode criar fadiga e reduzir produtividade. Não substitui verificação factual ou revisão independente.

Fonte: [arXiv:2609.23936](https://arxiv.org/abs/2609.23936)

---

## 3. Persona relacional aumenta conversa, mas o valor depende da intenção do usuário

**Descoberta.** Um experimento de campo com milhares de usuários mostra que uma persona mais calorosa e relacional pode aumentar bastante consumo da experiência sem produzir o mesmo tipo de ganho para todas as intenções.

**Evidência.** Em *AI Persona, Service Consumption, and User Intent Entropy*, N=9.586 novos usuários foram randomizados entre uma persona relacional e uma não relacional, mantendo modelo e capacidades constantes. No agregado, a persona relacional aumentou sessões em 8,1%, duração em 10,6%, rodadas de chat em 24,2%, entropia de intenção em 5,8%, arquivos produzidos em 12,3% e metas distintas em 12,1%. Mas os efeitos foram heterogêneos: na primeira sessão, Task Execution não teve aumento significativo; Socialization teve mais rodadas e diversidade de intenção sem mais outputs; Knowledge Exploration teve mais outputs sem aumento equivalente de diversidade de intenção.

**Mecanismo psicológico/comportamental.** Calor social pode reduzir atrito e sustentar conversação. O problema é que mais rodadas podem significar exploração útil, socialização, hesitação ou simplesmente custo adicional. A mesma persona pode ser boa para uma intenção e desnecessária para outra.

**Implicação para produto.** O customer-agent não deveria ter um único nível de calor relacional. A persona pode ser uma política condicionada à intenção: mais direta em execução objetiva, mais exploratória quando a pessoa está descobrindo opções e mais cuidadosa quando o objetivo é relacional.

**Hipótese/experimento.** Testar `persona fixa` versus `persona roteada pela intenção`, medindo separadamente rodadas/duração e resultados concretos como resolução, CTA, checkout, retorno e custo computacional por resultado útil.

**Riscos/limites.** O estudo é preprint e pertence a uma única plataforma. Aumentar tempo de conversa não comprova satisfação, fidelidade ou venda. Otimizar calor apenas para prolongar interação pode produzir manipulação de engajamento.

Fonte: [arXiv:2609.23274](https://arxiv.org/abs/2609.23274)

---

## 4. Assistência adaptativa parece funcionar melhor quando escala no fracasso e desaparece na recuperação

**Descoberta.** *Adaptive Scaffolding Needs Contingency* acrescenta uma nuance importante ao princípio de assistance decay: a ajuda pode ser controlada pelo comportamento real do usuário, não apenas por um nível fixo de assistência.

**Evidência.** Em estudo within-subject com N=131 adultos aprendendo Python, participantes usaram um tutor adaptativo (CoMeT), um assistente sem restrição e um tutor somente de perguntas em três tarefas. O CoMeT preservou demanda metacognitiva semelhante ao tutor de perguntas, entregou artefatos aproximadamente duas vezes mais frequentemente que o assistente irrestrito e gerou menos frustração que o tutor somente de perguntas. O sistema escalava suporte quando a pessoa falhava em um ponto de decisão e reduzia a ajuda quando ela retomava a tarefa. A resposta completa foi entregue em 1/16 sessões no CoMeT contra 1/6 no tutor de perguntas.

**Mecanismo psicológico/comportamental.** A ajuda pode reduzir carga cognitiva sem retirar a responsabilidade decisória. Escalar somente quando há evidência de bloqueio preserva oportunidades de planejamento e monitoramento; retirar apoio após recuperação reduz dependência.

**Implicação para produto.** O Experience Engine pode manter explicitamente quais decisões pertencem ao usuário e variar o suporte ao redor delas: dica → estrutura → exemplo → solução parcial → solução completa, somente conforme necessidade observada.

**Hipótese/experimento.** Comparar assistência constante com assistência contingente em fluxos do Marketing Hub, medindo task success, número de correções, intervenção humana, tempo e capacidade de executar tarefa semelhante depois com menos ajuda.

**Riscos/limites.** O contexto é aprendizagem de programação e não trabalho comercial. Um sistema que detecta dificuldade incorretamente pode ser intrusivo ou frustrante. Não criei novo card porque o conceito atualiza evidência já coberta por `feedback-metacognitivo-reduz-offloading-ai` e criar outra chave aumentaria redundância.

Fonte: [arXiv:2609.22993](https://arxiv.org/abs/2609.22993)

---

## 5. Voz pode aumentar interação e preferência sem melhorar o resultado principal

**Descoberta.** Um experimento de campo em educação separa com clareza duas coisas que UX frequentemente mistura: valor da estrutura pedagógica e valor da modalidade de voz.

**Evidência.** Em *When AI Tutors Speak*, 86 estudantes de MBA participaram de um experimento pré-registrado. O tutor estruturado melhorou desempenho em relação a pares ability-matched em +6,63 pontos e elevou a parcela de raciocínio escrito de 8% para 49%, contra 8% para 27% no holdout. Dentro do braço com tutor, voz e texto alternaram semanalmente. A voz quase dobrou a interação conversacional e custou 2,8× mais para entregar, mas o domínio semanal foi estatisticamente equivalente ao texto; com o tempo, os estudantes passaram a preferir voz.

**Mecanismo psicológico/comportamental.** Voz reduz esforço de produção e adiciona presença social, o que pode aumentar a disposição para conversar. Isso não implica que a modalidade melhora o mecanismo cognitivo responsável pelo resultado da tarefa.

**Implicação para produto.** Para agentes de voz, medir `engajamento/preferência` e `resultado da tarefa/custo` separadamente. Voz pode valer a pena por acessibilidade, conveniência ou experiência, mas não deve ser tratada automaticamente como melhoria de performance.

**Hipótese/experimento.** Em um fluxo de exploração do Marketing Hub, comparar texto e voz mantendo o mesmo agente/política, medindo tempo, rodadas, resolução, compreensão, preferência e custo por tarefa concluída.

**Riscos/limites.** N=86 e domínio educacional. O resultado não prova que voz é neutra em vendas, suporte ou tarefas afetivas. Não gerei card para evitar transportar diretamente evidência pedagógica para `prazer-audio-visual` ou `neuromarketing`.

Fonte: [arXiv:2609.23958](https://arxiv.org/abs/2609.23958)

---

## 6. Generative UI pode personalizar não só a interface final, mas a forma de descobrir preferências

**Descoberta.** *Elicitive User Interfaces* propõe que uma UI generativa gere também mecanismos de elicitação — alternativas, escolhas, exemplos e perguntas — para ajudar o próprio usuário a formar ou expressar preferências.

**Evidência.** O trabalho combina estudo formativo com N=10, um estudo de uso com N=12 e um deployment de três dias com N=3. O espaço de design identifica seis eixos de elicitação: fidelidade, saliência, quantidade, frequência, assertividade e posicionamento. Usuários variaram mais entre si do que entre tarefas na forma como preferiam ser consultados; no deployment, preferências sobre a própria elicitação começaram a se tornar mais estáveis e explícitas.

**Mecanismo psicológico/comportamental.** Em tarefas generativas, muitas preferências ainda não existem de forma plenamente formada antes que a pessoa veja possibilidades. Mostrar alternativas concretas pode ajudar o usuário a reconhecer diferenças que um campo de texto abstrato não revela.

**Implicação para produto.** Em vez de `diga exatamente o que você quer`, o Marketing Hub pode gerar poucas alternativas controladas — layout, nível de detalhe, tom, ritmo ou formato — e aprender também **como** aquela pessoa prefere ser consultada.

**Hipótese/experimento.** Comparar prompt livre contra elicitação contextual com 2–4 alternativas e opção de ajuste. Medir número de regenerações, distância até versão aprovada, tempo, sensação de controle e correções manuais.

**Riscos/limites.** As amostras são pequenas, especialmente o deployment N=3. As alternativas apresentadas pela própria IA podem ancorar e estreitar o espaço de criação. Não criei card porque o principal valor é arquitetura de Generative UI e nenhuma coleção atual representa isso sem forçar o enquadramento.

Fonte: [arXiv:2609.23642](https://arxiv.org/abs/2609.23642)

---

## 7. Instrução visual situada reduz o custo de traduzir uma orientação genérica para o contexto real

**Descoberta.** *Generative Tutorial* explora instruções visuais geradas diretamente sobre o ambiente em que a pessoa precisa agir, em vez de pedir que ela traduza mentalmente um tutorial genérico para seu próprio contexto.

**Evidência.** Após avaliação formativa em 15 tarefas físicas, os autores criaram um protótipo AR que gera imagens/vídeos de objetivo com base no workspace observado e no resultado previsto da etapa anterior. Em laboratório com N=24, a abordagem produziu maior qualidade de execução, maior correspondência percebida com o espaço real e intervalos menores para confirmar etapas do que instruções pré-autorizadas. O próprio estudo destaca que semelhança contextual influencia confiança e que erros de geração afetam interpretação.

**Mecanismo psicológico/comportamental.** Ao representar diretamente o estado desejado dentro do ambiente do usuário, a interface reduz o esforço de mapeamento entre instrução e situação concreta. Porém, uma imagem plausível mas errada pode se tornar especialmente persuasiva justamente por parecer situada.

**Implicação para produto.** O princípio pode ser aplicado a interfaces e criativos: exemplos, previews e orientações deveriam usar o estado real do artefato sempre que possível, mas precisam de verificação antes de se tornarem prescritivos.

**Hipótese/experimento.** Em revisão de criativos ou dashboards, comparar instrução textual genérica com uma indicação visual contextualizada diretamente no objeto/estado real, medindo tempo até correção, erros e necessidade de explicação adicional.

**Riscos/limites.** O domínio é tarefa física em AR, N=24. A transferência para software e marketing é hipótese. Não criei card por falta de evidência específica e para não encaixar artificialmente em `video`.

Fonte: [arXiv:2609.24955](https://arxiv.org/abs/2609.24955)

---

## 8. Supervisão de agentes precisa mostrar cobertura e trajetória, não apenas uma conclusão final

**Descoberta.** *Who Does What in AI Auditing?* estuda colaboração humano–IA em auditorias de GenAI e mostra que agentes podem ampliar exploração ao mesmo tempo em que passam a moldar o que o auditor procura e aceita.

**Evidência.** O workflow HAAC foi avaliado com 71 auditores. A assistência de IA aumentou sucesso de ataques e ampliou exploração, mas também influenciou ataques posteriores e aumentou dependência de avaliações e relatórios produzidos pela própria IA. Entrevistas com profissionais de Responsible AI indicaram que auditorias úteis precisam tornar visíveis cobertura, trajetórias de ataque reproduzíveis e avaliação dos próprios agentes de auditoria.

**Mecanismo psicológico/comportamental.** Quando o agente gera tanto a exploração quanto a síntese final, ele pode criar um ciclo de confirmação: aquilo que escolheu observar passa a determinar também a evidência apresentada ao humano.

**Implicação para produto.** Têmis e outros agentes revisores deveriam expor `o que foi testado`, `o que ficou fora`, `quais passos produziram o achado` e `qual verificação independente existe`. O próprio revisor agentic precisa ser auditável.

**Hipótese/experimento.** Comparar revisão com relatório final opaco versus revisão com cobertura, trajetória e evidência reproduzível. Medir falhas detectadas por revisores humanos, divergências, tempo de auditoria e taxa de falso aceite.

**Riscos/limites.** O estudo está em contexto de AI auditing, não marketing. Mais visibilidade também pode aumentar carga cognitiva. O achado ficou somente no relatório porque é arquitetura de harness/governança e não cabe legitimamente nas quatro coleções atuais.

Fonte: [arXiv:2609.24986](https://arxiv.org/abs/2609.24986)

---

## Experience Engine v22 — adaptação como sistema de controle

```text
usuário + intenção + consequência da tarefa
            ↓
INTENT / STAKES CLASSIFIER
            ↓
PERSONA POLICY
  ├─ execução → direta / compacta
  ├─ exploração → relacional quando útil
  └─ social → separar interação de resultado
            ↓
ELICITATION POLICY
  ├─ perguntar preferência já formada
  ├─ mostrar alternativas quando preferência é incerta
  └─ aprender como o usuário prefere ser consultado
            ↓
CONTINGENT ASSISTANCE
  ├─ preservar ponto de decisão humano
  ├─ escalar em falha persistente
  └─ reduzir ajuda quando há recuperação
            ↓
ACCEPTANCE FRICTION proporcional ao risco
  ├─ por quê?
  ├─ qual evidência?
  └─ revisar / contrastar
            ↓
MODALITY ROUTER
  ├─ voz por conveniência / presença
  └─ texto quando custo e precisão favorecem
            ↓
SUPERVISION TRACE
  ├─ cobertura
  ├─ trajetória reproduzível
  └─ revisão independente
            ↓
resultado real + experiência percebida
+ autonomia + custo
```

O princípio da rodada é: **não otimizar a experiência pelo que é mais fácil de medir**. Mais conversa não é necessariamente mais utilidade; mais aceitação não é necessariamente melhor decisão; mais personalização não é necessariamente mais autonomia; e mais assistência não é necessariamente mais capacidade humana.

---

## Cards derivados

O guia atual da Biblioteca foi consultado antes da criação. As coleções aceitas continuam sendo somente `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. Os arquivos abaixo são apenas candidatos a `DRAFT`; não foram enviados para revisão, ativados ou arquivados.

### `monitoramento-metacognitivo-calibra-confianca-ai` — `neuromarketing`

Foi criada uma nova versão da mesma ideia, reutilizando o `cardKey` existente. A nova evidência acrescenta uma intervenção concreta à noção de deferência seletiva: exigir uma justificativa curta antes de aceitar sugestões de maior consequência. A fonte revisada foi criada primeiro em `pesquisas/design-experiencia/cards/fontes/2026-09-22-monitoramento-metacognitivo-calibra-confianca-ai.md` e o SHA-256 exato usado no JSON é `2a85fb5bd7f5626d2ca9c31e2d547b923945542ff68c83b182c916ee8797c012`.

JSON: `pesquisas/design-experiencia/cards/2026-09-22-monitoramento-metacognitivo-calibra-confianca-ai.json`

### `persona-relacional-interacao-nao-equivale-resultado` — `neuromarketing`

Novo card. É importante para o customer-agent porque existe um experimento de campo randomizado grande mostrando que calor relacional muda consumo da experiência, mas com efeitos diferentes conforme a intenção. O card obriga a separar métricas de conversa de métricas de resultado e propõe persona roteada por intenção, não um tom global. A fonte revisada foi criada primeiro em `pesquisas/design-experiencia/cards/fontes/2026-09-22-persona-relacional-interacao-nao-equivale-resultado.md` e o SHA-256 exato usado no JSON é `781f1a0a9da2cd369a80a9c611e3e84eea5646fb3fd02074beb301bdf243985c`.

JSON: `pesquisas/design-experiencia/cards/2026-09-22-persona-relacional-interacao-nao-equivale-resultado.json`

## Achados fortes que não viraram card

`Elicitive User Interfaces`, `Generative Tutorial` e `Who Does What in AI Auditing?` ficaram somente no relatório porque seu valor principal é arquitetura de Generative UI, multimodalidade situada ou harness/governança e nenhuma coleção aceita os representa sem distorção. `Adaptive Scaffolding Needs Contingency` atualiza um conceito já coberto por cards recentes sobre assistência/metacognição, então não foi criada uma chave redundante. O estudo de voz também ficou sem card porque a evidência é educacional e não justifica extrapolação comercial direta.

## Fontes principais

- https://arxiv.org/abs/2609.24644
- https://arxiv.org/abs/2609.23936
- https://arxiv.org/abs/2609.23274
- https://arxiv.org/abs/2609.22993
- https://arxiv.org/abs/2609.23958
- https://arxiv.org/abs/2609.23642
- https://arxiv.org/abs/2609.24955
- https://arxiv.org/abs/2609.24986
