# Radar de Design de Experiência — 2026-09-19

## Síntese da rodada

O padrão mais forte desta rodada é que **adaptação útil precisa preservar a capacidade humana e separar a possibilidade de intervir do motivo para intervir**. Os trabalhos mais interessantes de hoje mostram três riscos complementares: um agente pode resolver demais e acelerar deskilling; uma interface de voz pode tomar a palavra porque existe uma pausa, mesmo quando semanticamente deveria ficar quieta; e uma personalização pode tratar um traço contextual como se fosse identidade permanente. A direção mais promissora é uma UX que combine assistência gradual, intervenção semanticamente justificada, representação compartilhada entre humano e agente e memória contextualizada de sucessos e falhas.

---

## 1. Feedback metacognitivo pode reduzir delegação completa sem retirar a IA

### Descoberta

Uma intervenção pequena — tornar visível ao usuário a consequência de pedir a solução completa — pode reduzir cognitive offloading e preservar melhor desempenho posterior sem IA.

### Evidência

O preprint **Designing Against Deskilling: Metacognitive Feedback Reduces Cognitive Offloading to LLM Assistants**, submetido em 17 de setembro de 2026, relata um experimento online pré-registrado com **704 participantes**, desenho 2×2 mais controle sem IA e uma tarefa de aritmética de frações. O assistente baseado em LLM fornecia solução completa somente quando solicitada explicitamente. Feedback metacognitivo reduziu as chances de offloading da resposta (**OR=0,47**) e aumentou as chances de acerto em um teste posterior sem IA (**OR=1,51**). Um incentivo por usar menos ajuda não apresentou efeito estabelecido nesses desfechos.

Fonte: https://arxiv.org/abs/2609.20143

### Mecanismo psicológico/comportamental

A interpretação mais plausível é **autorregulação metacognitiva**. Em vez de impedir o usuário de delegar, a interface introduz uma fricção reflexiva: a pessoa percebe que está terceirizando uma oportunidade de prática e pode decidir se ainda quer a solução integral. O experimento não isolou diretamente esse mecanismo.

### Implicação para produto

Em agentes que ajudam a raciocinar, revisar, aprender ou tomar decisões, vale testar uma política de **guidance-first**: primeiro pistas, estrutura, comparação ou próximo passo; solução integral continua disponível quando solicitada. Quando houver delegação repetida, um aviso curto pode tornar explícito o custo de prática sem bloquear o usuário.

### Hipótese/experimento

Comparar três condições: agente padrão; agente guidance-first; guidance-first + feedback metacognitivo. Medir quantidade de pedidos de solução integral, qualidade de uma tarefa seguinte executada sem ajuda, número de correções, tempo total e satisfação.

### Riscos e limites

O estudo é educacional e específico de frações. Não demonstra efeito em produtividade, vendas ou trabalho criativo. Avisos frequentes podem soar moralizantes, aumentar atrito ou negar ajuda legítima. A intervenção deve preservar a escolha de pedir a resposta completa.

**Card criado:** `feedback-metacognitivo-reduz-offloading-ai`, coleção `neuromarketing`.

---

## 2. Adaptação cultural em vídeo parece funcionar melhor quando a barreira é social, não apenas informacional

### Descoberta

Um apresentador culturalmente congruente teve vantagem sobre um apresentador neutro quando a crença-alvo era reforçada pela comunidade; essa vantagem praticamente desapareceu quando o problema era apenas uma lacuna de conhecimento médico.

### Evidência

O preprint **Durably Reducing Belief in Women's Health Misinformation Through Culturally Adaptive AI Videos**, submetido em 16 de setembro de 2026, relata um experimento de campo com **434 mulheres de baixa alfabetização em área suburbana da Índia**. Um vídeo gerado por IA com apresentador culturalmente adaptado reduziu a crença em desinformação de saúde em **30%**, quase o dobro da redução produzida por um apresentador neutro em comparação com a condição sem intervenção. Os ganhos persistiram por três semanas. A vantagem foi maior para crenças sustentadas por normas comunitárias e negligenciável para lacunas de conhecimento como compreensão sobre vacinas.

Fonte: https://arxiv.org/abs/2609.19364

### Mecanismo psicológico/comportamental

Familiaridade, reconhecimento e **relevância social percebida** podem aumentar receptividade quando a mensagem disputa uma norma do grupo. O estudo não isola qual componente — aparência, linguagem, estilo, identificação ou outro — gerou o efeito.

### Implicação para produto

Em vídeos do Marketing Hub, adaptação cultural deveria ser usada como uma hipótese específica, não como regra estética universal. Ela faz mais sentido quando a objeção é socialmente reforçada e pode ser testada mantendo roteiro, oferta, duração e edição constantes.

### Hipótese/experimento

Comparar apresentador neutro versus apresentador sintético culturalmente congruente, medindo retenção, compreensão, confiança, CTA, qualidade pós-clique e percepção de estereótipo. O segmento cultural usado deve ser validado explicitamente e não inferido de maneira sensível ou caricatural.

### Riscos e limites

O desfecho foi crença em desinformação de saúde, não compra. A população é muito específica. Adaptação cultural pode virar caricatura, estereótipo ou manipulação identitária; não deve simular endosso de uma pessoa ou comunidade real.

**Card criado:** `video-apresentador-culturalmente-adaptado`, coleção `video`.

---

## 3. Em voz full-duplex, “posso falar agora?” e “devo falar agora?” são problemas diferentes

### Descoberta

Modelos full-duplex parecem tomar a palavra principalmente porque foram endereçados ou porque surgiu silêncio, e não porque detectaram que o conteúdo exige correção ou alerta.

### Evidência

O preprint **Full-Duplex Speech Models Take the Floor When Asked, Not When Needed**, submetido em 17 de setembro de 2026, avaliou cinco famílias de modelos. Os gatilhos conversacionais — ser diretamente endereçado e detectar silêncio — foram muito mais confiáveis para iniciar fala do que a presença de fatos falsos ou riscos. Mesmo quando os modelos respondiam a uma afirmação falsa, a taxa de contestação do erro ficou em cerca de **0,14–0,15**; em cenários de risco, respostas que efetivamente alertavam ficaram em aproximadamente **0,04–0,07**.

Fonte: https://arxiv.org/abs/2609.19596

### Mecanismo psicológico/comportamental

O comportamento sugere uma política de turn-taking fortemente guiada por pistas conversacionais superficiais. A disponibilidade do canal para falar é tratada como proxy de relevância semântica.

### Implicação para produto

Uma arquitetura de voz deveria separar pelo menos dois módulos: **Floor Policy** (`há espaço para falar?`) e **Intervention Policy** (`há razão suficiente para falar?`). Um agente pode ter permissão para interromper somente quando há risco, erro crítico, pedido explícito ou limiar semântico claramente definido.

### Hipótese/experimento

Construir diálogos com pausas idênticas, mas diferentes níveis de consequência semântica. Medir interrupção necessária, interrupção indevida, detecção de erro e tempo de recuperação. A política ideal deveria manter baixa taxa de intrusão sem deixar passar situações de alta consequência.

### Riscos e limites

É benchmark de modelos em cenários controlados, não um estudo de preferência humana em ambientes reais. Uma política de intervenção agressiva pode gerar interrupções irritantes ou paternalistas.

**Sem card:** é um achado forte de arquitetura e segurança de agentes de voz, mas classificá-lo como `prazer-audio-visual` ou `neuromarketing` distorceria sua finalidade.

---

## 4. Uma mesma estrutura semântica pode servir ao agente e à pessoa que revisa o resultado

### Descoberta

O **Semantic Action Graph** propõe uma representação única para ligar geração/narração de highlights e inspeção humana, evitando que o agente trabalhe com uma estrutura e a interface mostre outra.

### Evidência

O trabalho **Semantic Action Graph: A Shared Representation for Agent Grounding and Human Interpretation of Sports Highlights**, aceito em workshop do IEEE VIS 2026, representa performer, ação, destinatário, momento e estado, com relações temporais e de resultado. O protótipo SportSAGE usa a mesma estrutura tanto no pipeline agentic quanto na interface de busca e inspeção. Um design probe com **12 fãs de futebol** encontrou boa aceitação e uso do grafo para navegar e interpretar eventos.

Fonte: https://arxiv.org/abs/2609.20768

### Mecanismo psicológico/comportamental

Uma **representação compartilhada** reduz o gap referencial: o usuário consegue apontar para o mesmo objeto, evento e momento que o agente usa internamente. Isso melhora verificabilidade e editabilidade sem exigir reconstruir contexto em texto.

### Implicação para produto

No pipeline de vídeo, cenas, personagens, ações, claims e timecodes poderiam existir como entidades semânticas persistentes. O humano seleciona, bloqueia ou revisa esses mesmos objetos; o agente usa os mesmos IDs para editar, justificar ou regenerar.

### Hipótese/experimento

Comparar uma revisão chat-only com uma revisão apoiada por objetos semânticos compartilhados. Medir tempo de edição, quantidade de correções equivocadas, rastreabilidade e facilidade de desfazer mudanças.

### Riscos e limites

A amostra é muito pequena e o domínio é esportivo. O benefício pode cair quando o conteúdo não tem eventos discretos ou quando o schema fica complexo demais.

**Sem card:** pode eventualmente se encaixar em `video`, mas a evidência humana ainda é pequena demais para transformar em orientação persistente.

---

## 5. Falhas podem virar restrições reutilizáveis, não apenas exemplos negativos esquecidos

### Descoberta

Um novo trabalho de diálogo adaptativo para robôs mostra valor em armazenar explicitamente estratégias que falharam e recuperar essas falhas como restrições contextuais para futuras interações.

### Evidência

O artigo **Learning from Success and Failure: Acquiring Adaptive Dialogue Strategies for Social Robots**, publicado no IEEE Robotics and Automation Letters e disponibilizado no arXiv em 17 de setembro, usa atributos do usuário reconhecidos por VLM e histórico de diálogo para gerar estratégias. Estratégias extraídas de interações de campo incluem tanto sucessos quanto falhas; representar explicitamente as falhas complementou estratégias de sucesso e melhorou o desempenho reportado pelos autores.

Fonte: https://arxiv.org/abs/2609.19570

### Mecanismo psicológico/comportamental

Uma falha contextualizada ajuda a delimitar **quando uma estratégia não deve ser aplicada**. Isso reduz a tendência de transformar um padrão bem-sucedido em regra universal.

### Implicação para produto

O playbook de um agente poderia armazenar não só `faça X quando Y`, mas também `evite X quando Z`, com evidência, contexto, recência e possibilidade de revisão. Falhas repetidas podem ser consolidadas em constraints antes de virarem regras mais fortes.

### Hipótese/experimento

Comparar retrieval de estratégias apenas de sucesso versus sucesso + restrições derivadas de falhas. Medir repetição do mesmo erro, escaladas, taxa de resolução e número de correções humanas.

### Riscos e limites

O contexto é robótica social e o uso de atributos inferidos por visão pode introduzir vieses ou inferências inadequadas. Restrições extraídas de poucos casos podem congelar exceções como regra.

**Sem card:** trata principalmente de memória/harness e não se encaixa legitimamente nas coleções atuais.

---

## 6. UX multi-agent pode precisar de uma superfície de coordenação, não apenas um chat único

### Descoberta

O **Treadstone** propõe uma interface inspirada em feed social na qual humanos e múltiplos agentes publicam hipóteses, evidências, respostas e contestações de forma assíncrona.

### Evidência

O trabalho **Treadstone: A Social-Media-Inspired Platform for Multi-Agent Collaborative Data Analysis**, submetido em 17 de setembro, permite que agentes transmitam hipóteses proativamente e que humanos façam curadoria leve, liguem claims e contestem análises. O estudo qualitativo reporta colaboração percebida com preservação de agência humana em comparação com um chatbot convencional solitário.

Fonte: https://arxiv.org/abs/2609.19774

### Mecanismo psicológico/comportamental

Uma superfície compartilhada cria **group awareness**: torna visível quem propôs o quê, qual evidência sustenta cada claim e onde existem conflitos. Isso reduz a carga de manter toda a atividade de múltiplos agentes dentro de uma conversa linear.

### Implicação para produto

Para um Marketing Hub com vários agentes, uma interface de coordenação poderia mostrar `agente → hipótese → evidência → status → contestação → decisão humana`, mantendo o chat para aprofundamento local, mas não como única representação do trabalho coletivo.

### Hipótese/experimento

Comparar transcript único com feed de hipóteses/evidências. Medir detecção de conflito entre agentes, recall de proveniência, tempo para localizar a origem de uma recomendação e esforço de intervenção humana.

### Riscos e limites

A evidência descrita no paper é qualitativa e ainda inicial. Um feed mal desenhado pode apenas deslocar a sobrecarga do chat para outra superfície.

**Sem card:** achado de coordenação/harness, sem coleção válida correspondente.

---

## 7. Personalização pode precisar ser contextual à relação, e não uma personalidade global

### Descoberta

O novo **Value Faces** sugere que a autoapresentação e os valores salientes de uma pessoa mudam conforme a relação e o interlocutor; portanto, um único perfil global pode apagar diferenças importantes de contexto.

### Evidência

O trabalho **Value Faces: Surfacing How Self-Presentation Shifts Across Relationships**, submetido em 17 de setembro, analisou históricos de chat com base nos dez valores humanos de Schwartz e construiu perfis separados por relação. Em um estudo mixed-methods com **18 participantes**, os perfis distinguiram contextos relacionais acima do acaso; as diferenças inferidas também tiveram correspondência com percepções dos próprios participantes e ajudaram alguns a refletir sobre como se apresentam em relações diferentes.

Fonte: https://arxiv.org/abs/2609.19581

### Mecanismo psicológico/comportamental

A saliência de valores e a autoapresentação são **situacionais e relacionais**. O mesmo usuário pode querer comportamentos diferentes em contexto profissional, familiar, criativo ou comercial sem que isso represente contradição de identidade.

### Implicação para produto

Entradas de preferência deveriam carregar escopo: `com quem`, `objetivo`, `canal`, `papel`, `recência` e `confiança`. Em vez de “o usuário prefere X”, o sistema poderia armazenar “em contexto Y, diante do objetivo Z, X foi preferido”.

### Hipótese/experimento

Comparar personalização baseada em um perfil global com personalização contextual por papel/relação. Medir correções, rejeições, trocas de contexto e sensação de adequação.

### Riscos e limites

A amostra é pequena e a inferência de valores pode tocar atributos íntimos ou sensíveis. O produto não deveria inferir ou persistir esse tipo de informação sem base legítima e controles claros de revisão.

**Sem card:** evidência inicial e sensível; não é apropriado convertê-la em orientação comercial persistente agora.

---

## Experience Engine v19 — Capability-Preserving Adaptation

```text
usuário
   ↓
INTENT + RELATIONAL CONTEXT
   ↓
CAPABILITY-PRESERVING ASSISTANCE
   ├── guidance first
   ├── feedback metacognitivo
   └── delegação total quando explicitamente desejada
   ↓
INTERVENTION ROUTER
   ├── posso falar/agora?
   └── devo falar/agora?
   ↓
SHARED SEMANTIC SUBSTRATE
   ↓
SUCCESS + FAILURE STRATEGY MEMORY
   ↓
MULTI-AGENT COORDINATION SURFACE
   ↓
EXECUÇÃO + PROVENIÊNCIA VISÍVEL
   ↓
resultado + capacidade humana
+ agência calibrada + contexto revisável
```

O princípio desta rodada é: **separar possibilidade de agir de motivo para agir e preservar a capacidade do usuário enquanto o sistema se adapta**. Um agente maduro não precisa apenas saber executar; precisa saber quando orientar, quando falar, quando ficar quieto, quando uma preferência vale apenas naquele contexto e como deixar sua estrutura de trabalho inspecionável pela pessoa.

---

## Cards derivados nesta rodada

Antes de criar os cards foi consultada a versão atual de `harness-library-api/docs/guia-uso-api-cards.md`. As coleções válidas permanecem `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. Os arquivos abaixo representam somente candidatos a **DRAFT**; não houve envio para revisão, ativação ou arquivamento.

### 1. `feedback-metacognitivo-reduz-offloading-ai`

- **Coleção:** `neuromarketing`
- **Por que importa:** introduz uma política concreta para agentes que precisam ajudar sem maximizar dependência: orientação gradual + reflexão antes da delegação integral.
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-09-19-feedback-metacognitivo-reduz-offloading-ai.md`
- **SHA-256:** `c201bacc3895ecdca9b1144d56a66a6dcac300ed3993bae235abf20414848c44`
- **JSON:** `pesquisas/design-experiencia/cards/2026-09-19-feedback-metacognitivo-reduz-offloading-ai.json`

### 2. `video-apresentador-culturalmente-adaptado`

- **Coleção:** `video`
- **Por que importa:** transforma adaptação cultural de um conceito genérico em uma hipótese condicional: tende a fazer mais sentido quando a barreira da mensagem é socialmente reforçada, e não apenas informacional.
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-09-19-video-apresentador-culturalmente-adaptado.md`
- **SHA-256:** `34ec2cca71199c56fa077cf1e60fd0f9711ed73249036e67425821a5250bec09`
- **JSON:** `pesquisas/design-experiencia/cards/2026-09-19-video-apresentador-culturalmente-adaptado.json`

### Achados fortes que não viraram card

- Full-duplex voice: forte para arquitetura e segurança de agentes de voz, sem coleção adequada.
- Semantic Action Graph: promissor para `video`, mas ainda com evidência humana pequena (N=12).
- Estratégias de falha: principalmente memória/harness.
- Treadstone: principalmente coordenação multi-agent/harness.
- Value Faces: amostra pequena e inferências potencialmente sensíveis.

Nenhum card foi enviado para revisão, ativado ou arquivado automaticamente.
