# Radar de Design de Experiências — 2026-09-05

## Síntese executiva

A rodada de hoje reforça uma mudança importante no design de produtos com IA: **a experiência deixa de ser apenas a apresentação de respostas e passa a incluir a gestão dinâmica de iniciativa, estado, fluência cognitiva, modalidade e divisão de trabalho entre humano e agente**.

Os sinais mais fortes desta rodada são:

1. **iniciativa proativa bem temporizada pode superar o chat reativo**;
2. **Generative UI ainda perde confiabilidade quando a interface precisa evoluir durante vários turnos**;
3. **o mesmo agente pode produzir experiências emocionais diferentes dependendo do dispositivo usado**;
4. **antropomorfismo não é uma dimensão única e seus componentes podem se atrapalhar**;
5. **interfaces fáceis demais de processar podem reduzir o escrutínio crítico do usuário**;
6. **em sistemas multiagente, reconfigurar responsabilidades é parte da experiência, não apenas infraestrutura**;
7. **há evidência neuroergonômica de que linguagem natural pode reduzir carga cognitiva ao reconfigurar automação**.

---

## 1. A melhor ajuda pode acontecer antes do usuário pedir

### Descoberta

O artigo **“Preemptive, Buffered, or Guided? Empirical Studies on Human–AI Interaction Strategies for Software Test Case Development”**, publicado no *ACM Transactions on Computer-Human Interaction* em 8 de agosto de 2026, comparou formas diferentes de interação entre pessoas e LLMs em uma tarefa cognitivamente complexa.

Os autores estudaram uma interface convencional de chat e três estratégias alternativas: resposta em buffer, input guiado e **preemptive prompting**, em que o sistema observa sinais contextuais e oferece ajuda antes que o usuário formule uma solicitação explícita.

### Evidência

No segundo estudo, com 24 participantes, a estratégia proativa aumentou em média a qualidade dos testes em cerca de **33%**, a criatividade em cerca de **35%** e reduziu o tempo ocioso em até **49%**. O estudo inicial também mostrou que participantes gastavam até **126% mais tempo interagindo com LLMs** do que com busca tradicional, evidenciando que o chat conversacional puro pode consumir atenção demais.

### Mecanismo psicológico/comportamental

O ganho não parece vir apenas da qualidade do modelo, mas de três mecanismos:

- redução do esforço para formular prompts;
- intervenção no momento em que o usuário provavelmente está travado;
- preservação do fluxo cognitivo, porque a IA assume iniciativa parcial sem exigir troca constante de contexto.

### Implicação para produto

O padrão sugere um **Initiative Controller** em vez de um assistente puramente reativo.

Exemplo:

```text
sinais de interação
   ↓
usuário parece avançando?
   ├── sim → não interromper
   └── não → estimar necessidade
              ↓
        sugestão contextual curta
              ↓
        aceitar / rejeitar
              ↓
        calibrar frequência futura
```

O próprio estudo ajustava a probabilidade de novas sugestões conforme o usuário aceitava ou rejeitava intervenções.

### Hipótese/experimento possível

Comparar três experiências em uma mesma tarefa:

1. chat reativo;
2. botão “me ajude”;
3. intervenção proativa baseada em sinais como pausa longa, repetição, retorno de tela ou baixa progressão.

Métricas: conclusão, tempo, número de prompts, criatividade/diversidade, interrupções rejeitadas e carga mental percebida.

### Riscos/limites

A amostra é pequena e a tarefa é desenvolvimento de testes de software. Ajuda proativa mal calibrada pode produzir o efeito “Clippy”: interrupções irritantes, perda de autonomia e confiança. O principal problema de design passa a ser **quando não intervir**.

Fonte: https://doi.org/10.1145/3817601

---

## 2. Generative UI funciona por turno, mas ainda esquece o que construiu

### Descoberta

O **EvoGenUI-Bench**, submetido em 29 de agosto de 2026, muda uma fraqueza importante na avaliação de Generative UI. Em vez de perguntar se um modelo consegue gerar uma interface uma vez, ele verifica se consegue **manter o mesmo artefato funcional enquanto os requisitos mudam durante cinco turnos consecutivos**.

O benchmark contém 150 tarefas de cinco turnos (750 turnos), cobrindo apresentação de informação, interação com estado e interfaces ligadas a ferramentas/estado externo.

### Evidência

Entre oito modelos avaliados, o mais forte chegou a **74,9% de sucesso por turno**, mas completou apenas **37,3% dos episódios inteiros de cinco turnos**. Em tarefas ligadas a ferramentas, a retenção de requisitos previamente satisfeitos caiu para **52,4%**.

Os erros se concentraram em:

- arquitetura da informação;
- propagação de estado derivado;
- ligação entre controles e comportamento;
- sincronização com estado externo;
- decomposição cumulativa de requisitos.

### Mecanismo psicológico/comportamental

Para o usuário, isso produz uma falha particularmente ruim: a interface parece “entender” cada pedido isoladamente, mas perde coerência longitudinal. A pessoa precisa então lembrar ao sistema o que já havia sido acordado, aumentando carga cognitiva e reduzindo confiança.

### Implicação para produto

Generative UI precisa de um **Cross-Turn State Integrity Layer**:

```text
requisito novo
   ↓
estado atual da interface
   ↓
requisitos anteriormente satisfeitos
   ↓
gerar alteração mínima
   ↓
testar comportamento
   ↓
confirmar que nada regressou
```

Ou seja, UI generativa precisa se aproximar de engenharia de software incremental: diff, teste e regressão — não apenas geração visual.

### Hipótese/experimento possível

Criar uma tarefa de cinco ou dez refinamentos sucessivos e comparar:

- regeneração completa;
- edição incremental;
- edição incremental + memória estruturada de requisitos + testes automatizados.

Métrica principal: **retention rate**, isto é, quantos requisitos antigos continuam corretos depois de cada alteração.

### Riscos/limites

É um benchmark recente e ainda não representa toda a variedade de aplicações. Mas o gap entre sucesso por turno e sucesso por episódio é um alerta muito concreto contra demos que avaliam apenas a primeira geração.

Fonte: https://arxiv.org/abs/2608.29387

---

## 3. O dispositivo faz parte da psicologia da experiência

### Descoberta

O artigo **“A Confidant in Your Pocket: How Smartphone Portability Facilitates Emotional Disclosure to AI Chatbots”**, publicado em 18 de agosto de 2026 em *Psychology & Marketing*, mostra que usar o mesmo tipo de chatbot em smartphone versus computador pode alterar o nível de abertura emocional do usuário.

### Evidência

Em três estudos, totalizando **754 participantes**, interações no smartphone produziram maior expressão emocional. Um dos estudos mostrou que o acesso móvel aumentava escolhas orientadas à emoção e essas escolhas estavam associadas a menor emoção negativa após a interação.

O terceiro estudo identificou **portabilidade percebida** como mediador e mostrou que o efeito era apoiado quando o agente respondia com estilo orientado a **warmth**, mas não com um estilo orientado a competência.

### Mecanismo psicológico/comportamental

O smartphone não é apenas uma tela menor. Portabilidade, proximidade corporal, privacidade percebida e disponibilidade contínua podem criar uma sensação de acesso íntimo e imediato, favorecendo disclosure.

Isso significa que a “experiência” é composta por:

```text
agente + interface + dispositivo + contexto físico
```

### Implicação para produto

Um Experience Engine deveria considerar **device context** antes de escolher tom e modalidade.

No celular, por exemplo, uma experiência reflexiva pode ser mais apropriada para microinterações breves, privadas e calorosas. No desktop, a mesma tarefa pode favorecer estrutura, comparação e análise.

### Hipótese/experimento possível

Para o mesmo agente, testar quatro condições:

- mobile + warmth;
- mobile + competence;
- desktop + warmth;
- desktop + competence.

Medir profundidade de resposta, duração, abandono, autoavaliação de conforto e continuidade de uso.

### Riscos/limites

Maior disclosure não é automaticamente melhor. Produtos podem explorar involuntariamente vulnerabilidade emocional, sobretudo se combinarem disponibilidade constante com antropomorfismo intenso.

Fonte: https://doi.org/10.1002/mar.70252

---

## 4. “Parecer humano” precisa ser dividido em pelo menos três controles

### Descoberta

O artigo **“The Physical, Emotional, and Autonomous Anthropomorphism of Service Chatbots”**, publicado em 14 de agosto de 2026, argumenta que antropomorfismo de chatbot não deve ser tratado como um único conceito.

Os autores distinguem três dimensões:

- **física**: nome, avatar, voz, aparência;
- **emocional**: expressar/interpretar emoções;
- **autônoma**: compreender, planejar e agir de forma flexível.

### Evidência

Em quatro estudos, a dimensão autônoma aumentou satisfação principalmente por elevar percepção de **competência**. A dimensão emocional elevou **warmth**, mas em cenários utilitários isso não se traduziu de maneira equivalente em satisfação.

Mais interessante: quando autonomia alta foi combinada com emocionalidade alta, o ganho de competência percebida foi atenuado. Em um dos experimentos, adicionar sinais emocionais a um agente autônomo reduziu a vantagem que a autonomia produzia.

### Mecanismo psicológico/comportamental

Usuários parecem avaliar sistemas também pelas dimensões sociais de **warmth** e **competence**. Sinais excessivos de calor podem conflitar com pistas de eficiência e domínio técnico em tarefas utilitárias.

### Implicação para produto

Não usar um controle genérico de “humanização”. Usar um **Anthropomorphism Profile**:

```text
físico      0 ───── 100
emocional   0 ───── 100
autonomia   0 ───── 100
```

A configuração deveria depender da tarefa.

Exemplo provável:

- suporte emocional → warmth maior;
- finanças, operações, diagnóstico técnico → competência/autonomia mais fortes e teatralização emocional menor.

### Hipótese/experimento possível

Criar versões de um agente com matriz 2×2:

- autonomia baixa/alta;
- emocionalidade baixa/alta.

Medir competência, warmth, confiança, satisfação e taxa de aceitação da recomendação.

### Riscos/limites

O estudo foi conduzido sobretudo em contextos utilitários de serviço. Em entretenimento ou companionship, o equilíbrio pode ser diferente.

Fonte: https://doi.org/10.1002/mar.70255

---

## 5. “Fácil de entender” pode virar um viés de confiança

### Descoberta

O artigo **“The danger in easy answers”**, publicado na edição de agosto de 2026 de *Computers in Human Behavior: Artificial Humans*, examinou como estilos conversacional e informativo afetam a experiência com chatbots de saúde.

O trabalho introduz o conceito de **subjective processing fluency**: a sensação de que uma informação é fácil de processar.

### Evidência

Após interagir com os chatbots, **44 participantes** fizeram entrevistas semiestruturadas. Em geral, participantes preferiram respostas mais fáceis de processar e agentes mais conversacionais; informação excessiva aumentava sobrecarga e reduzia engajamento.

Mas os autores identificam um paradoxo: maximizar a fluência pode produzir **processing fluency bias** — informação apresentada com enorme facilidade pode ser julgada mais favoravelmente independentemente da qualidade real do conteúdo.

### Mecanismo psicológico/comportamental

O cérebro usa facilidade de processamento como heurística. Quando algo “flui”, pode parecer mais verdadeiro, seguro ou confiável do que realmente é.

### Implicação para produto

O objetivo não deveria ser `maximize fluency`, mas **calibrate fluency**.

Uma boa experiência poderia ter três níveis:

```text
baixo risco → resposta fluida e direta
médio risco → resposta + evidência visível
alto risco → resposta + incerteza + comparação + confirmação
```

### Hipótese/experimento possível

Apresentar a mesma recomendação em três formatos:

1. extremamente simples e conclusivo;
2. simples + evidência;
3. simples + evidência + incerteza explícita.

Medir compreensão real, confiança, lembrança e verificação posterior — não apenas satisfação.

### Riscos/limites

É uma pesquisa qualitativa e em saúde. Ainda não determina causalmente quanto de fricção é necessário. Mas adiciona um fundamento psicológico forte para não usar “clareza percebida” como única métrica de UX.

Fonte: https://doi.org/10.1016/j.chbah.2026.100370

---

## 6. Em sistemas de agentes, trocar responsabilidades também é trabalho

### Descoberta

O artigo **“Envisioning the work of reconfiguring”**, publicado em 27 de agosto de 2026 na *AI Magazine*, propõe que sistemas humano-agente sejam modelados não apenas pelas tarefas que cada ator executa, mas também pelo **trabalho necessário para mudar essa divisão de responsabilidades quando a situação muda**.

### Evidência

É um trabalho conceitual, não um experimento. A contribuição é explicitar que mudar quem faz o quê altera simultaneamente dependências, coordenação, sincronização e necessidades de comunicação dentro do sistema.

### Mecanismo psicológico/comportamental

Quando a automação assume ou devolve uma tarefa, o usuário precisa atualizar seu modelo mental:

- quem está responsável agora?
- o que já foi feito?
- de quem depende a próxima ação?
- quem deve ser avisado se algo mudar?

Se essa transição for invisível, surgem lacunas de responsabilidade.

### Implicação para produto

Agentes deveriam possuir um **Reconfiguration Manager** explícito:

```text
situação mudou
   ↓
redistribuir responsabilidades
   ↓
mostrar mudança relevante ao humano
   ↓
confirmar handoff
   ↓
atualizar dependências / checkpoints
```

Isso se encaixa diretamente em BPM + agentes: além de definir a atividade, o processo deve definir **regras de handoff entre humano e agente**.

### Hipótese/experimento possível

Comparar um fluxo de agente em que responsabilidades mudam silenciosamente contra outro em que cada mudança é apresentada como um pequeno evento de handoff: `agente assumiu`, `aguardando humano`, `agente devolveu`, `bloqueado por X`.

Medir erros de coordenação, retrabalho e tempo para recuperar contexto.

### Riscos/limites

O artigo ainda precisa de validação empírica. Excesso de notificações de reconfiguração também pode virar ruído.

Fonte: https://doi.org/10.1002/aaai.70085

---

## 7. Já é possível medir se uma interface com IA realmente reduz esforço mental

### Descoberta

Um artigo publicado em **27 de agosto de 2026** no *Journal of Intelligent Manufacturing* avaliou um fluxo de colaboração humano-robô em que operadores podiam reprogramar tarefas de cobots usando linguagem natural via LLM.

### Evidência

O sistema gerava código executável em **1–2 prompts**. A avaliação combinou frequência cardíaca, piscadas, taxa de erro e NASA-TLX. A condição assistida por LLM apresentou menor carga mental subjetiva e maior confiabilidade do processo. O trabalho reporta uma redução de cerca de **96% nos erros de montagem**, chegando a média de 1,03% no cenário assistido.

Os autores reconhecem que o baseline obrigava operadores a absorver tarefas cognitivamente mais difíceis quando o robô não podia ser reconfigurado. Isso é relevante: parte do benefício vem de **redistribuir carga para a automação**, e não apenas da linguagem natural em si.

### Mecanismo psicológico/comportamental

A interface natural reduz o custo de tradução entre intenção humana e representação técnica. O operador permanece concentrado no objetivo e transfere a transformação da intenção em comandos para o sistema.

### Implicação para produto

Para avaliar experiências com agentes, métricas de UX podem incluir sinais objetivos de carga:

- erro;
- tempo de hesitação;
- frequência de correção;
- interrupções;
- sinais fisiológicos quando apropriado;
- NASA-TLX ou escalas equivalentes.

O objetivo deixa de ser apenas “o usuário gostou?” e passa a incluir **quanto trabalho cognitivo o sistema retirou ou adicionou**.

### Hipótese/experimento possível

Em um workflow digital comum, comparar:

- formulário/manual;
- chat;
- agente com interpretação de intenção + execução.

Além de tempo e satisfação, medir quantidade de decisões intermediárias que o usuário precisou manter mentalmente.

### Riscos/limites

O domínio é industrial e a comparação envolve tarefas com dificuldades diferentes. Não se deve generalizar os números diretamente para produtos digitais comuns.

Fonte: https://link.springer.com/article/10.1007/s10845-026-02958-5

---

# Síntese de produto: Experience Engine v5

As rodadas anteriores enfatizaram personalização, modalidade, confiança, memória, Generative UI e estado emocional. Hoje aparecem três camadas novas e muito importantes: **iniciativa, integridade longitudinal e coordenação**.

```text
Usuário
   ↓
INTENT + CONTEXT + DEVICE
   ↓
STATE / COGNITIVE LOAD ESTIMATOR
   ↓
PREFERENCE + RELATIONSHIP STATE
   ↓
INITIATIVE CONTROLLER
   ├── esperar
   ├── perguntar
   └── agir / sugerir proativamente
   ↓
ANTHROPOMORPHISM PROFILE
   ↓
MODALITY / EXPERIENCE ROUTER
   ↓
PROCESSING FLUENCY CONTROLLER
   ↓
GENERATION / GENERATIVE UI
   ↓
CROSS-TURN STATE INTEGRITY
   ↓
RECONFIGURATION / HANDOFF MANAGER
   ↓
experiência
   ↓
comportamento + feedback + performance
   ↓
calibração da próxima interação
```

## Insight central da rodada

**A próxima geração de UX adaptativa não será apenas “personalizar o que mostrar”, mas decidir quando intervir, quanto simplificar, que personalidade funcional assumir e quem deve estar no controle em cada etapa.**

O ponto mais promissor para um produto experimental é começar pelo **Initiative Controller**, porque ele pode ser implementado sem sensores sofisticados. Pausas, repetições, retornos, tentativas frustradas, rejeições e progressão da tarefa já fornecem sinais suficientes para testar quando um agente deve ficar silencioso ou tomar iniciativa.

---

## Fontes

- Shi, B.; Kristensson, P. O. *Preemptive, Buffered, or Guided? Empirical Studies on Human–AI Interaction Strategies for Software Test Case Development*. ACM TOCHI, 8 ago. 2026. https://doi.org/10.1145/3817601
- Peng, Y. et al. *EvoGenUI-Bench: Evaluating LLMs as Multi-Turn Generative UI Assistants*. arXiv, 29 ago. 2026. https://arxiv.org/abs/2608.29387
- Li, B.; Huang, Y.; Zhang, R. *A Confidant in Your Pocket: How Smartphone Portability Facilitates Emotional Disclosure to AI Chatbots*. Psychology & Marketing, 18 ago. 2026. https://doi.org/10.1002/mar.70252
- Hu, Y.; Acikgoz, F.; Yu, S. *The Physical, Emotional, and Autonomous Anthropomorphism of Service Chatbots*. Psychology & Marketing, 14 ago. 2026. https://doi.org/10.1002/mar.70255
- *The danger in easy answers: Conceptualizing subjective processing fluency for healthcare chatbot information presentation*. Computers in Human Behavior: Artificial Humans, ago. 2026. https://doi.org/10.1016/j.chbah.2026.100370
- IJtsma, M. *Envisioning the work of reconfiguring: A modeling perspective for designing adaptive human-agent systems*. AI Magazine, 27 ago. 2026. https://doi.org/10.1002/aaai.70085
- Trapero, J. A. et al. *Neuroergonomic signatures of improved human–robot collaboration in LLM-supported industrial workflows*. Journal of Intelligent Manufacturing, 27 ago. 2026. https://link.springer.com/article/10.1007/s10845-026-02958-5
