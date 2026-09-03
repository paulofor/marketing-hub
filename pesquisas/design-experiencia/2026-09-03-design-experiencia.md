# Radar de Design de Experiência — 2026-09-03

## Síntese da rodada

O sinal mais forte de hoje é uma mudança de foco: **a melhor experiência com IA não é necessariamente a mais humana, a mais rica ou a mais personalizada; é a que escolhe a modalidade, o nível de controle e o grau de adaptação adequados ao estado do usuário e à tarefa**.

Nesta rodada, priorizei trabalhos ainda não cobertos nos dias anteriores, incluindo um resultado divulgado publicamente em 2 de setembro, estudos e versões recentes de agosto, além de pesquisas de 2026 com aplicação direta em produtos digitais.

---

## 1. Em situações de frustração, texto pode funcionar melhor que voz

**Descoberta.** Um estudo publicado no *European Journal of Marketing* comparou chatbots de texto e voicebots após falhas de serviço. Em um estudo de campo com 138 avaliações reais, o chatbot recebeu média 4,12/5 contra 3,03/5 do voicebot. Em experimento controlado com 160 participantes, texto também produziu maior alívio percebido e melhores avaliações; um terceiro experimento com 320 participantes mostrou que o efeito depende da gravidade da falha. O trabalho voltou a ganhar destaque em 2 de setembro de 2026 em divulgação da University of Queensland.

**Mecanismo psicológico/comportamental.** O texto cria mais distância emocional e ritmo deliberado. Digitar desacelera a interação, oferecendo tempo para a emoção negativa diminuir. Voz, embora mais humana e fluida, pode manter o usuário preso à intensidade emocional daquele momento.

**Implicação para produto.** A modalidade não deveria ser escolhida apenas pela disponibilidade técnica. Um sistema poderia usar um `Modality Router`: se detectar irritação/frustração moderada, prioriza texto e respostas curtas; em outros estados, voz pode ser vantajosa.

**Hipótese/experimento.** A/B/C: voz imediata vs. texto vs. escolha adaptativa baseada em estado emocional. Medir resolução, abandono, satisfação, tempo até redução de linguagem negativa e necessidade de escalonamento humano.

**Riscos/limites.** O estudo é centrado em falhas de serviço e não prova que texto seja superior em todos os contextos. Severidade alta reduziu a diferença entre modalidades.

Fontes: https://doi.org/10.1108/EJM-08-2024-0693 ; https://phys.org/news/2026-09-human-ai-customer.html

---

## 2. Personalização de longo prazo falha quando as preferências são implícitas e distantes

**Descoberta.** O benchmark RealPref, com 100 perfis, 1.300 preferências e históricos longos, testa preferências expressas de quatro formas, desde declaração direta até sinais implícitos distribuídos por várias sessões. O desempenho dos LLMs cai significativamente quando a preferência é mais implícita e quando o contexto cresce. O estudo também mostra que a distância entre a informação relevante e a consulta final importa. Em contextos moderados, um simples lembrete para considerar preferências passadas pode melhorar bastante o resultado; em contextos extremos, RAG tende a ser mais eficaz.

**Mecanismo psicológico/comportamental.** Para o usuário, uma preferência pode ser sentida como algo contínuo (“ele já deveria saber disso”), enquanto o sistema trata o histórico como sinais esparsos competindo por atenção contextual. Isso cria uma assimetria entre memória percebida e memória real.

**Implicação para produto.** Não basta armazenar conversa. Preferências precisam virar objetos estruturados com origem, confiança, recência, força e contexto de aplicação.

**Hipótese/experimento.** Comparar quatro estratégias: histórico bruto; resumo do usuário; RAG de preferências; `Preference Ledger` estruturado. Medir violações de preferência, acertos em situações novas e número de correções do usuário.

**Riscos/limites.** O dataset é sintético e benchmark não substitui comportamento longitudinal real. Inferir preferências implícitas demais pode gerar personalização invasiva ou incorreta.

Fonte: https://arxiv.org/abs/2603.04191

---

## 3. Talvez seja melhor permitir que o usuário “gire um controle” do que pedir que explique sua preferência

**Descoberta.** *Steerable Chatbots* explora personalização por activation steering: em vez de exigir prompts como “seja mais direto, mas não frio, com um pouco mais de detalhe”, o sistema expõe fatores ajustáveis de preferência. Em estudo within-subjects com 14 participantes, os autores observaram potencial para alinhar melhor respostas em cold start que prompting puro, além de diferenças importantes entre usuários quanto a controle, persistência e transparência.

**Mecanismo psicológico/comportamental.** Preferências latentes são difíceis de verbalizar. Controles contínuos reduzem o custo de transformar sensação subjetiva em linguagem precisa.

**Implicação para produto.** A personalização pode ganhar uma camada de “controles sensoriais/semânticos”: conciso ↔ detalhado, exploratório ↔ objetivo, formal ↔ casual, iniciativa baixa ↔ alta. Esses controles não precisam necessariamente aparecer o tempo todo; podem surgir quando o sistema detecta repetidas correções semelhantes.

**Hipótese/experimento.** Comparar prompt livre, presets e sliders adaptativos. Medir tempo até chegar à resposta desejada, número de re-prompts e sensação de controle.

**Riscos/limites.** Amostra pequena. Expor parâmetros demais cria carga cognitiva e pode transformar uma experiência fluida em painel de configuração.

Fonte: https://arxiv.org/abs/2505.04260

---

## 4. Design emocional responsável inclui saber quando inserir fricção

**Descoberta.** Um estudo qualitativo com 45 usuários de IA, publicado em julho de 2026 no *Journal of Consumer Affairs*, identificou três paradoxos. Primeiro, maior intimidade antropomórfica pode reduzir vigilância ética. Segundo, certa frustração/fricção pode aumentar comportamentos de verificação. Terceiro, transparência mal desenhada pode provocar ansiedade e até reduzir conformidade em vez de aumentá-la.

**Mecanismo psicológico/comportamental.** Fluidez aumenta confiança e reduz esforço de checagem; uma interrupção pequena pode reativar processamento deliberativo. Porém fricção excessiva vira irritação e abandono.

**Implicação para produto.** Surge a ideia de `Deliberate Friction`: não colocar confirmação em tudo, mas introduzir uma pequena mudança de ritmo quando risco, irreversibilidade ou incerteza ultrapassarem um limiar.

**Hipótese/experimento.** Em decisões de maior consequência, comparar: fluxo sem interrupção; alerta genérico; pausa curta com evidência/alternativas. Medir taxa de verificação, reversões posteriores e confiança calibrada.

**Riscos/limites.** Trabalho qualitativo; não fornece ainda um limiar causal de “fricção ótima”. Fricção pode virar dark pattern se usada para dificultar ações que beneficiam o usuário.

Fonte: https://doi.org/10.1111/joca.70070

---

## 5. Haptics está começando a virar mídia generativa

**Descoberta.** *Prompt-to-Touch*, publicado em 8 de agosto de 2026 no *ACM Transactions on Computer-Human Interaction*, transforma uma descrição textual de sensação tátil em efeito háptico. O pipeline traduz o texto para uma descrição sonora, usa text-to-audio, converte o áudio em vibração e compensa as características do atuador. Houve dois estudos humanos (n=20 e n=10). No primeiro, os efeitos melhoraram a experiência em 10 de 12 cenários; no segundo, 5 de 6 efeitos foram mais adequados ao cenário para o qual haviam sido gerados.

**Mecanismo psicológico/comportamental.** Toque pode carregar significado por metáfora e congruência multisensorial: frequência, ritmo e intensidade complementam imagem, som e narrativa.

**Implicação para produto.** O designer pode deixar de escolher vibrações de uma biblioteca fixa. Um agente poderia receber algo como “confirmação leve, calorosa e discreta” e gerar/adaptar o padrão para relógio, celular ou controle.

**Hipótese/experimento.** Para uma mesma ação, comparar vibração padrão, efeito gerado por texto e efeito gerado + calibrado por preferência individual. Medir reconhecimento, agradabilidade, lembrança e intrusão.

**Riscos/limites.** Ainda não é geração em tempo real; interpretação do toque varia cultural e individualmente; padrões inadequados podem causar desconforto. O próprio artigo recomenda filtros e limites físicos.

Fonte: https://doi.org/10.1145/3819584

---

## 6. Em feedback tátil, significado pode valer mais que fidelidade

**Descoberta.** *Semantic Haptic Feedback Enhances Dexterous Robotic Teleoperation*, de agosto de 2026, compara haptics sensorial de alta fidelidade com padrões abstratos que representam estados como confirmação e exceção. Em três estudos, a abordagem semântica teve desempenho semelhante em tarefas unimanual e foi superior em tarefas bimanuais, com menor carga de trabalho, maior consciência situacional e maior preferência dos participantes.

**Mecanismo psicológico/comportamental.** O sistema não tenta reproduzir toda a realidade sensorial; ele transmite somente o significado necessário para a decisão. Isso reduz carga perceptiva e libera atenção.

**Implicação para produto digital.** O princípio vale além de robótica: uma vibração não precisa imitar algo real. Pode codificar “feito”, “atenção”, “mudança importante” ou “ação precisa de você”. É uma espécie de linguagem tátil compacta.

**Hipótese/experimento.** Em app móvel/wearable, comparar vibrações decorativas com 3–4 padrões semânticos consistentes. Medir identificação sem olhar a tela, tempo de resposta e carga percebida.

**Riscos/limites.** Evidência vem de teleoperação robótica; transferência para produtos de consumo precisa de validação. Vocabulários hápticos grandes podem ficar difíceis de aprender.

Fonte: https://arxiv.org/abs/2608.02780

---

## 7. O “estado emocional do agente” pode fazer parte do loop de experiência

**Descoberta.** *AffectLoop*, publicado em 17 de agosto de 2026, implementa em um robô social um loop que acompanha dinâmica emocional verbal e facial do usuário, mantém também um estado afetivo do próprio agente e usa ambos para condicionar fala e comportamento. Em um piloto within-subjects com cinco participantes, a versão com loop afetivo teve avaliações maiores de empatia e satisfação e mostrou melhor alinhamento afetivo e recuperação de valência negativa.

**Mecanismo psicológico/comportamental.** A conversa emocional deixa de ser `emoção do usuário → resposta` e passa a ser uma dinâmica temporal de co-regulação: o sistema acompanha como o estado muda após cada intervenção.

**Implicação para produto.** Um agente digital pode manter um `Affective State Machine` simples: tensão, energia, valência, confiança e tendência. O objetivo não seria “diagnosticar emoções”, mas observar direção: a interação está melhorando ou piorando o estado aparente?

**Hipótese/experimento.** Comparar respostas baseadas apenas no conteúdo textual com respostas condicionadas pela trajetória de sinais ao longo de 5–10 turnos. Medir satisfação, abandonos e velocidade de recuperação após frustração.

**Riscos/limites.** Amostra extremamente pequena (n=5). Inferência emocional é probabilística, culturalmente variável e sensível do ponto de vista de privacidade.

Fonte: https://arxiv.org/abs/2608.16686

---

## Insight integrado: Experience Engine v4 — “State → Modality → Meaning → Friction”

Os trabalhos desta rodada sugerem uma camada nova sobre o Experience Engine já discutido nos dias anteriores:

```text
Usuário
  ↓
intenção + histórico + sinais atuais
  ↓
STATE ESTIMATOR
  ↓
PREFERENCE LEDGER / PERSONALIZATION GATE
  ↓
MODALITY ROUTER
  ├─ texto
  ├─ voz
  ├─ UI
  ├─ visual
  └─ haptics
  ↓
SEMANTIC ENCODING
  ↓
RISK / DELIBERATION CHECK
  ├─ fluxo contínuo
  └─ deliberate friction
  ↓
experiência
  ↓
reação explícita + implícita
  ↓
atualização longitudinal
```

A ideia central é: **o produto não deveria perguntar apenas “qual conteúdo gerar?”, mas “qual experiência ajuda este usuário, neste estado, nesta tarefa, com o menor custo cognitivo e o grau correto de agência?”**

Uma regra de design que emerge desta rodada é:

> Mais humano, mais multimodal, mais personalizado e mais fluido não significa automaticamente melhor. Cada uma dessas dimensões deve ser tratada como uma variável adaptativa.

## Experimento prioritário sugerido

Construir um protótipo de agente com apenas três estados operacionais — `calmo`, `incerto`, `frustrado` — e três respostas de experiência — `texto direto`, `explicação estruturada`, `pausa + confirmação`. O estado pode começar sendo inferido somente de sinais explícitos e linguagem da conversa, sem dados biométricos. Comparar com uma versão fixa e medir resolução, satisfação, correções, abandono e tempo de tarefa. Isso testa a hipótese central do radar sem exigir inicialmente uma infraestrutura multimodal complexa.
