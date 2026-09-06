# Radar diário — Design de Experiências para Produtos Digitais

**Data:** 2026-09-06

## Síntese executiva

A rodada de hoje reforça uma mudança importante no design de experiências com IA: **não basta otimizar a sensação durante a interação**. Alguns trabalhos recentes mostram que uma experiência pode parecer mais empática, confortável ou “inteligente” e ainda produzir efeitos indesejáveis depois — mais dependência percebida, mais tempo de tarefa, mais frustração ou mais confiança do que o sistema merece.

O princípio que emerge é: **projetar para o efeito durante e depois da interação**. Em produtos com IA, convém separar pelo menos cinco objetivos: alívio imediato, autonomia futura, eficiência, confiança calibrada e continuidade de uso.

---

## 1. Adaptação emocional pode aumentar usabilidade e, ao mesmo tempo, aumentar frustração e duração da tarefa

**Descoberta**

Um estudo publicado em 4 de setembro de 2026 avaliou um agente conversacional em mixed reality que usa sinais de prosódia da voz para adaptar o estilo de resposta. O sistema processava conteúdo semântico e sinais afetivos em pipelines paralelos, sem bloquear a resposta principal.

**Evidência**

- 40 participantes, divididos em condição adaptativa e não adaptativa.
- SUS médio: **88,6** com adaptação afetiva vs. **81,4** sem adaptação.
- A condição adaptativa teve mais turnos de conversa e maior tempo de conclusão.
- O workload total não diferiu significativamente.
- Curiosamente, a frustração foi **maior** na condição adaptativa, apesar da melhor usabilidade percebida.

**Mecanismo psicológico/comportamental**

A adaptação emocional pode tornar a interação mais natural e solidária, aumentando envolvimento, mas também prolongando a conversa e criando expectativas maiores sobre a capacidade do agente. Em tarefas procedurais, mais interação nem sempre significa melhor desempenho.

**Implicação para produto**

Não usar “mais adaptação” como default. Introduzir um **Adaptation Budget**: quanto de adaptação emocional é desejável dado o objetivo da tarefa, tempo disponível e risco de interrupção.

**Hipótese/experimento**

A/B/C:
1. estilo neutro;
2. adaptação emocional contínua;
3. adaptação emocional apenas quando sinais de dificuldade/frustração ultrapassarem um limiar.

Medir: tempo de tarefa, taxa de sucesso, SUS, frustração, número de turnos e retenção.

**Riscos/limites**

Amostra pequena, jovem e contexto de mixed reality. Inferência afetiva por voz é probabilística e deve ser tratada como sinal auxiliar, não como diagnóstico.

**Fonte:** https://link.springer.com/article/10.1007/s12652-026-05125-z

---

## 2. Duas respostas podem aliviar igualmente no momento, mas produzir efeitos opostos depois

**Descoberta**

Um experimento randomizado aceito em 24 de agosto de 2026 comparou dois estilos de resposta de companheiros de IA diante de emoções negativas: um orientado a **regulação adaptativa** e outro orientado a **reasseguramento relacional**.

**Evidência**

- 380 adultos.
- Ambos os estilos reduziram o afeto negativo imediato de forma semelhante.
- O estilo orientado a regulação produziu maior autoeficácia de regulação emocional (**b = 0,500; p < 0,001**), melhor qualidade de estratégia e maior recuperação afetiva posterior sem IA.
- O estilo de reasseguramento produziu maior percepção de necessidade emocional da IA (**b = 1,010; p < 0,001**) e maior intenção de reliance de curto prazo (**b = 0,628; p < 0,001**).

**Mecanismo psicológico/comportamental**

Uma resposta pode acalmar o usuário de duas maneiras: aumentando a capacidade da pessoa de lidar com o problema ou tornando a presença do agente mais necessária. O benefício imediato pode mascarar trajetórias psicológicas diferentes.

**Implicação para produto**

Adicionar um **Post-Interaction Outcome** ao design. Não avaliar apenas “o usuário ficou melhor agora?”, mas também “ele ficou mais capaz de agir sozinho depois?”.

**Hipótese/experimento**

Comparar respostas que:
- solucionam/acolhem diretamente;
- ensinam uma estratégia reutilizável;
- combinam acolhimento + transferência gradual de competência.

Medir não apenas satisfação, mas autoeficácia e desempenho em uma tarefa posterior sem IA.

**Riscos/limites**

Os autores enfatizam que os resultados de reliance são de curto prazo e não equivalem a dependência clínica ou patológica.

**Fonte:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1950163/abstract

---

## 3. “Engajamento”, “confiança”, “reliance”, “apego” e “dependência” não devem ser tratados como a mesma métrica

**Descoberta**

Uma síntese narrativa publicada em 4 de setembro de 2026 mostra que a literatura sobre IA conversacional mistura construtos diferentes como se fossem um único continuum.

**Evidência**

O trabalho revisou 51 estudos peer-reviewed e diferencia explicitamente:

- **trust**: expectativa de competência/confiabilidade;
- **reliance**: delegar decisão ou tarefa;
- **over-reliance**: delegar sem escrutínio adequado;
- **attachment/companionship**: significado relacional e proximidade emocional;
- **dysregulated dependence/problematic use**: perda de controle e prejuízo funcional.

A revisão também mostra que escalas recentes frequentemente medem fenômenos diferentes e ainda carecem de validação transversal, estabilidade temporal e replicação.

**Mecanismo psicológico/comportamental**

Alta frequência de uso pode refletir utilidade, hábito, vínculo ou perda de controle — mecanismos totalmente distintos.

**Implicação para produto**

Criar um **Experience Metrics Model** separado por construto. Não usar retenção ou tempo de uso como proxy de “boa relação” nem de dependência.

**Hipótese/experimento**

Dashboard com quatro eixos independentes:
`confiança calibrada | reliance | vínculo | autonomia funcional`.

Observar se aumento de retenção vem acompanhado de aumento ou redução de autonomia.

**Riscos/limites**

É uma síntese narrativa, não uma meta-análise causal; os próprios autores destacam heterogeneidade metodológica e de medidas.

**Fonte:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1827795/full

---

## 4. Confiança em IA deve ser projetada como sistema multifatorial, não como sensação única

**Descoberta**

Uma revisão sistemática aceita em 27 de agosto de 2026 analisou a relação entre design de interação e confiança em sistemas com IA.

**Evidência**

- 1.565 artigos inicialmente analisados.
- 33 estudos atenderam aos critérios finais.
- Nove fatores de design apareceram de forma recorrente: **explainability, anthropomorphism, confidence, interface, errors, security, transparency, competence e perceived control**.

**Mecanismo psicológico/comportamental**

A confiança emerge de múltiplas pistas: competência percebida, transparência, controle, tratamento de erros e aparência/socialidade. Alterar apenas um componente pode gerar confiança mal calibrada.

**Implicação para produto**

Substituir a métrica “trust score” por um **Trust Vector**, por exemplo:
`competência percebida | previsibilidade | transparência | controle | segurança | recuperação de erro`.

**Hipótese/experimento**

Testar se mostrar níveis de confiança + evidências + ação de correção melhora calibração mais do que apenas mostrar explicações textuais.

**Riscos/limites**

A revisão aponta fragmentação conceitual e metodológica, então não há ainda uma receita universal de interface para confiança.

**Fonte:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1932655/abstract

---

## 5. Persona do agente pode alterar como emoção se transforma em intenção de uso

**Descoberta**

Um estudo publicado em 3 de setembro de 2026 comparou três estilos de persona para agentes de apoio: **Listener, Guide e Motivator**.

**Evidência**

- 266 respostas válidas.
- O engajamento afetivo foi um mecanismo central de aceitação.
- O efeito de engajamento afetivo sobre intenção comportamental foi significativamente mais forte no **Guide** do que no Listener (diferença de coeficiente **0,353; p = 0,024**) e mais forte no **Motivator** do que no Listener (**0,414; p = 0,006**).
- Guide e Motivator não diferiram significativamente entre si nesse mecanismo.

**Mecanismo psicológico/comportamental**

Escuta/validação pode gerar conexão, mas orientação e motivação parecem transformar essa conexão em intenção de continuidade de forma mais forte em determinados contextos.

**Implicação para produto**

Persona não deveria ser só branding. Pode ser uma variável adaptativa de comportamento: `escutar`, `orientar`, `motivar`, escolhida conforme estágio da jornada.

**Hipótese/experimento**

Criar um **Persona Router**:
- alta carga emocional → Listener;
- necessidade de decisão → Guide;
- baixa energia/procrastinação → Motivator.

Comparar contra uma persona fixa, medindo intenção de continuar, sensação de compreensão, ação efetiva e abandono.

**Riscos/limites**

Amostra majoritariamente universitária e contexto de apoio em saúde mental. Não generalizar automaticamente para comércio, produtividade ou educação.

**Fonte:** https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1863595/full

---

## 6. Haptics pode ser usado como canal de atenção, não apenas como “efeito”

**Descoberta**

Um estudo de 23 de agosto comparou feedback visual periférico, háptico e combinado para direcionar atenção a regiões fora do campo visual.

**Evidência**

- 12 participantes, 105 trials por participante.
- O feedback háptico foi significativamente mais rápido que o visual periférico para localizar alvos atrás do usuário.
- A combinação visual + háptica não melhorou objetivamente o desempenho sobre haptics sozinho, mas reduziu demanda mental/física percebida em relação ao visual isolado e teve alta preferência.

**Mecanismo psicológico/comportamental**

O tato usa um canal perceptual diferente e pode redirecionar atenção sem competir diretamente com recursos visuais já ocupados.

**Implicação para produto**

Adicionar um **Attention Channel Router**. Se a visão já está ocupada ou o elemento relevante está fora do foco, usar vibração/áudio em vez de adicionar mais elementos na tela.

**Hipótese/experimento**

Em smartphone/wearable:
- alerta visual;
- vibração direcional/semântica;
- visual + vibração.

Medir tempo até perceber, erro, interrupção da tarefa e carga cognitiva.

**Riscos/limites**

Amostra pequena e cenário de laboratório. Haptics excessivo pode gerar fadiga, urgência artificial e habituação.

**Fonte:** https://www.mdpi.com/2078-2489/17/9/814

---

## 7. O comportamento motor pode virar sensor implícito de UX

**Descoberta**

Um estudo de agosto investigou se dados cinemáticos de teleoperação poderiam prever dimensões subjetivas de UX e workload sem interromper o usuário para aplicar questionários.

**Evidência**

- 16 participantes e 144 gravações de movimento.
- Modelos específicos por dimensão alcançaram **R² ≥ 0,50 em 7 de 10 métricas**, com pico de **R² = 0,787 para usabilidade**.
- Classificação high/low atingiu **AUC ≥ 0,75 em 9 de 10 métricas**.
- Frustração, desempenho e usabilidade chegaram a AUC próxima/perfeita no dataset analisado.
- Os sinais mais úteis envolveram irregularidade temporal, entropia, variância e forma da distribuição dos movimentos.

**Mecanismo psicológico/comportamental**

Carga cognitiva e frustração alteram padrões de movimento. Portanto, o próprio fluxo de interação pode conter sinais indiretos do estado do usuário.

**Implicação para produto**

Em software comum, o equivalente pode ser explorar com muito cuidado sinais como:
`tempo entre ações | correções | backtracking | hesitação | movimento de cursor | velocidade de digitação | repetição de tentativas`.

Esses sinais poderiam alimentar um **Implicit UX Sensor** e acionar ajuda apenas quando necessário.

**Hipótese/experimento**

Construir um classificador simples de “fluindo vs. encontrando dificuldade” com telemetria de interação, validado sempre contra auto-relato real do usuário.

**Riscos/limites**

O estudo é um proof of concept offline, com apenas 16 participantes e todos homens. Não usar telemetria comportamental para inferir estados psicológicos sensíveis sem validação, transparência e controles de privacidade.

**Fonte:** https://www.mdpi.com/1424-8220/26/15/5002

---

## Padrão emergente da rodada

O principal avanço desta rodada é separar **experiência imediata** de **efeito pós-interação**.

```text
Usuário
   ↓
intenção + contexto + histórico + sinais atuais
   ↓
STATE / DIFFICULTY ESTIMATOR
   ↓
OUTCOME HORIZON
   ├── aliviar agora
   ├── ensinar / aumentar autonomia
   ├── acelerar tarefa
   └── preservar segurança
   ↓
ADAPTATION BUDGET
   ↓
PERSONA ROUTER
   ├── Listener
   ├── Guide
   └── Motivator
   ↓
ATTENTION / MODALITY ROUTER
   ├── texto
   ├── voz
   ├── visual
   └── haptics
   ↓
experiência
   ↓
IMPLICIT UX SENSOR + feedback explícito
   ↓
TRUST VECTOR + AUTONOMY METRICS
   ↓
resultado imediato + efeito posterior
```

### Insight central

**Uma experiência de IA não deveria ser otimizada apenas para “como o usuário se sente durante a sessão”.** Ela deveria considerar também o que sobra depois: mais capacidade, mais clareza, melhor decisão e autonomia — ou, ao contrário, mais necessidade de voltar ao sistema.

Essa diferença parece estar se tornando um dos eixos mais importantes do design de produtos digitais com agentes de IA.
