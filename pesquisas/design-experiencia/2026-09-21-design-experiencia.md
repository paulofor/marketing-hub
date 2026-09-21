# Radar de Design de Experiência — 2026-09-21

## Síntese da rodada

O padrão mais forte desta rodada é **adaptação com guardrails observáveis**. Em Generative UI, a novidade mais útil não é simplesmente gerar telas em tempo real, mas validar objetivo, mecânica, solvabilidade e aparência antes de expor a interface. Em agentes proativos, mais intervenções não significam mais engajamento. Em voz, sinais não verbais imediatamente anteriores à resposta podem melhorar a adequação prosódica. Em personalização, equipes precisam controlar explicitamente quando um sinal vira inferência, se persiste e quem pode usá-lo. E, em sistemas multi-agent, comunicação só compensa o custo de coordenação quando existe feedback de progresso suficientemente claro.

---

## 1. Generative UI começa a exigir um pipeline de validação, não apenas geração

### Descoberta

O novo trabalho do Google Research sobre **learning interactives** mostra uma direção mais madura para Generative UI: a interface gerada passa por objetivos aprovados, estrutura progressiva, scaffolding, autoavaliação funcional/adversarial e revisão humana.

### Evidência

O Google Research publicou em 17/09/2026 um sistema que gera simulações interativas personalizadas para objetivos curriculares. Os objetivos gerados precisam ser aprovados pelo professor. Depois, o pipeline cria níveis progressivos, hints e feedback e entra em loops de autocorreção que verificam critérios pedagógicos, mecânicos e visuais. Uma das avaliações agentic abre um navegador Chrome e interage com a simulação, inclusive levando controles a valores extremos para testar solvabilidade e comportamento. Os interactives liberados publicamente também passam por revisão de professores. Em uma avaliação, 40 interactives STEM foram avaliados por professores; em um estudo inicial nos EUA, 12 professores solicitaram três interactives cada e deram nota média 8/10 de qualidade. O próprio Google informa que estudos de campo sobre ganho de aprendizagem e engajamento ainda serão realizados.

Fonte: https://research.google/blog/the-future-of-practice-enabling-teachers-to-create-learning-interactives-with-generative-ui/

### Mecanismo psicológico/comportamental

A geração deixa de ser uma escolha estética ilimitada e passa a operar dentro de um **espaço de ação restringido pelo objetivo**. Isso pode reduzir inconsistência, excesso de opções e interfaces que parecem plausíveis, mas não permitem concluir a tarefa.

### Implicação para produto

Para Generative UI no Marketing Hub, tratar cada interface gerada como um artefato executável que precisa de:
`objetivo explícito -> componente gerado -> teste funcional -> teste de estados extremos -> validação visual/acessibilidade -> aprovação quando necessário`.

### Hipótese/experimento

Comparar chat-only contra uma interface gerada com componentes conhecidos e validação automática. Medir tempo para decisão, erros, ações inválidas, correções e task success; não apenas preferência visual.

### Riscos e limites

É um caso educacional conduzido pelo próprio Google. A nota dos professores não demonstra melhora de aprendizagem nem efeito comercial. Os loops de validação também aumentam custo e latência.

**Sem card:** achado forte de arquitetura de Generative UI, mas não se encaixa legitimamente nas quatro coleções atuais.

---

## 2. Agente proativo deve otimizar suporte menos intrusão, não frequência de intervenção

### Descoberta

Um novo estudo de HRI reforça que **mais intervenções não geram necessariamente mais engajamento**; uma política seletiva baseada no estado do usuário e contexto da tarefa pode ser mais apropriada do que busca contínua de interação.

### Evidência

*When Should Robots Intervene?*, submetido em 18/09/2026, comparou estratégia contínua de engajamento com estratégia context-aware em 32 participantes numa tarefa de hospital simulado. As políticas usavam sinais como orientação corporal e atenção. Os autores observaram que maior frequência de interação não levou necessariamente a maior engajamento e encontraram um trade-off sistemático entre suporte percebido e intrusividade, influenciado também por proximidade física e esforço do usuário.

Fonte: https://arxiv.org/abs/2609.21734

### Mecanismo psicológico/comportamental

Cada intervenção consome atenção e cria uma obrigação social implícita de responder. O benefício da ajuda precisa superar esse custo naquele momento.

### Implicação para produto

No customer-agent, separar `detectar oportunidade` de `interromper`. Usar sinais observáveis da sessão — repetição de erro, pedido explícito, bloqueio persistente, etapa crítica — antes de oferecer ajuda proativa.

### Hipótese/experimento

Comparar ajuda contínua, ajuda somente sob demanda e ajuda context-aware. Medir aceitação/rejeição da ajuda, tempo de resolução, conclusão, abandono e intrusividade percebida.

### Riscos e limites

É HRI físico, com N=32; proximidade corporal não existe da mesma forma num produto web. Não usar o resultado para inferir personalidade ou atributos sensíveis do usuário.

**Sem novo card:** atualiza materialmente o tema de proatividade, mas o card existente `proatividade-informacional-orcamento-atencao` já contém evidência digital mais forte; criar nova versão com um estudo menor e mais distante não aumentaria o rigor.

---

## 3. Haptics: preferência sensorial e desempenho podem apontar em direções diferentes

### Descoberta

Um estudo novo de VR encontrou preferência dos participantes por feedback háptico, mas efeitos de desempenho pequenos e até piora de precisão em uma combinação específica.

### Evidência

*Comparing Haptic Feedback Across Hand Tracking and Controllers in VR Object Interaction Tasks*, submetido em 18/09 e aceito no ACM VRST 2026, comparou vibração, impulso e ausência de feedback em gestos de grasp, pinch e tap, tanto com hand tracking quanto com controllers. Controllers foram mais rápidos em grasp/pinch, hand tracking foi mais preciso em pinch e preferido no geral. Haptics teve efeitos limitados de desempenho; impulso reduziu a precisão de grasp com controllers. Ainda assim, participantes preferiram vibração e impulso a nenhum feedback, com preferência geral por vibração.

Fonte: https://arxiv.org/abs/2609.21869

### Mecanismo psicológico/comportamental

Feedback sensorial pode aumentar sensação de confirmação e presença sem necessariamente melhorar controle motor. Um sinal mais forte também pode adicionar perturbação ao gesto.

### Implicação para produto

Não usar `mais intensidade = melhor haptic`. Definir semântica por ação: confirmação discreta, erro, mudança de estado e alerta podem requerer padrões diferentes.

### Hipótese/experimento

Em dispositivos compatíveis, testar sem haptic versus vibração curta e discreta em ações específicas. Medir erro, velocidade, confiança na ação, preferência e fadiga separadamente.

### Riscos e limites

O domínio é VR e requer hardware específico; não demonstra efeito em mobile, vídeo ou conversão. Preferência subjetiva não deve ser usada como proxy de desempenho.

**Sem card:** é uma boa atualização do achado de 13/09 sobre custo multimodal, mas já existem cards hápticos mais aderentes à coleção `prazer-audio-visual`; um novo card seria redundante.

---

## 4. Voz adaptativa pode se beneficiar do que o usuário acabou de demonstrar, não só do texto que disse

### Descoberta

Um novo sistema de TTS usa **um segundo de reação facial imediatamente anterior à resposta** para escolher emoção e prosódia antes de sintetizar a fala.

### Evidência

*Listen Before You Speak*, submetido em 18/09/2026, compara condicionamento temporal por reação do ouvinte com uma versão text-only. No protocolo dyadic MELD, o condicionamento temporal obteve melhor macro-F1 médio e maior concordância em valence-arousal-dominance, com acurácia praticamente inalterada. Em avaliação de adequação contextual com 20 pesquisadores de fala, 76% das decisões preferiram a variante temporal, 9% preferiram text-only e 15% não expressaram preferência. Reações corretas também superaram reações temporalmente incompatíveis.

Fonte: https://arxiv.org/abs/2609.21683

### Mecanismo psicológico/comportamental

A mesma frase pode pedir respostas prosódicas diferentes conforme a reação que acabou de provocar. O sinal recente pode reduzir ambiguidade sobre como a fala anterior foi recebida.

### Implicação para produto

Para agentes de voz multimodais, separar `o que responder` de `como soar`. Quando houver consentimento e canal visual disponível, sinais recentes podem informar prosódia; sem esse canal, a política deve degradar para texto/áudio sem inventar emoção.

### Hipótese/experimento

Comparar prosódia baseada apenas no texto com prosódia condicionada por sinais imediatamente anteriores. Medir adequação percebida, conforto, interrupções e correções.

### Riscos e limites

É benchmark e avaliação com 20 especialistas, não uso real longitudinal. Inferência facial pode errar, carregar vieses e envolver dados sensíveis; não deve virar requisito padrão de câmera.

**Sem card:** aplicação muito específica e sensível para o Marketing Hub atual; não há justificativa para forçar em `prazer-audio-visual`.

---

## 5. Personalização precisa de controles separados para inferência, persistência, acesso e ação

### Descoberta

Educadores configurando sistemas GenAI não quiseram apenas escolher “quanto personalizar”; eles precisaram decidir **quando atividade vira inferência, se essa inferência persiste, quem pode acessá-la e o que ela autoriza depois**.

### Evidência

*Understanding How Educators Configure GenAI Support for Open-Ended Learning*, submetido em 17/09/2026, realizou entrevistas e atividades de design com 15 educadores dos EUA. Os participantes configuraram geração, personalização e learner modeling e encontraram dificuldades para decompor uma IA genérica em funções e responsabilidades compreensíveis. O paper propõe limites explícitos em torno de personalização, inferência, persistência, disclosure e ação.

Fonte: https://arxiv.org/abs/2609.21019

### Mecanismo psicológico/comportamental

Um único toggle de “personalização” esconde decisões qualitativamente diferentes. Tornar o ciclo visível pode aumentar compreensão e reduzir a transformação silenciosa de sinais situacionais em perfil persistente.

### Implicação para produto

No Preference Ledger, representar separadamente:
`observação -> inferência -> persistência -> compartilhamento -> ação autorizada`.
Uma inferência pode existir apenas para a sessão sem ser gravada; um dado gravado pode não autorizar uma ação automática.

### Hipótese/experimento

Comparar painel de personalização genérico contra controles por estágio. Medir correções, exclusões, entendimento do que está sendo lembrado e arrependimento posterior.

### Riscos e limites

Estudo qualitativo pequeno e educacional, sem métrica de resultado. Controles demais também podem aumentar carga cognitiva.

**Sem card:** reforça memória/proveniência já cobertas em cards recentes; é principalmente arquitetura de governança e não exige nova chave.

---

## 6. “Soar cooperativo” não é o mesmo que cooperar; agência do usuário aparece como sinal mais consistente

### Descoberta

Uma análise grande de diálogos sugere que mecanismos de polidez humana não transferem de forma simples para conversas com IA. O sinal mais consistente associado a alinhamento foi **dar ao usuário espaço para moldar a troca**.

### Evidência

*Talking Past the Machine*, submetido em 18/09/2026, analisou 15.881 diálogos humano-ChatGPT e 10.784 humano-humano usando modelos de efeitos mistos. Os autores observam que a IA reproduz vários sinais superficiais de cooperação, mas com dinâmica diferente: hedging e softening associados a acomodação entre humanos aparecem associados a menor alinhamento em conversas com IA. Agência foi o preditor mais consistente de alinhamento nos dois contextos.

Fonte: https://arxiv.org/abs/2609.21401

### Mecanismo psicológico/comportamental

Hipótese: confirmação, escolha e possibilidade de redirecionamento preservam adaptação mútua; calor e polidez sem participação real podem apenas simular cooperação.

### Implicação para produto

No customer-agent, não otimizar somente persona “amigável”. Permitir corrigir premissas, escolher próximos passos, recusar rotas e redirecionar a interação com baixo esforço.

### Hipótese/experimento

Comparar uma versão fluida/assertiva com outra que contenha pontos explícitos de agência. Medir correções, task success, abandono, satisfação e CTA separadamente.

### Riscos e limites

O estudo é observacional e alinhamento linguístico não é conversão, qualidade nem satisfação. Muitos checkpoints também podem aumentar fricção.

**Card criado:** `agencia-usuario-alinhamento-conversa-ai`, coleção `neuromarketing`.

---

## 7. Multi-agent: comunicação compensa quando existe progresso compartilhável e feedback claro

### Descoberta

Um novo estudo acrescenta uma nuance ao achado de ontem sobre decomposição: **agentes comunicantes podem superar tentativas independentes, mas o ganho depende de compute suficiente e de uma medida clara de progresso**.

### Evidência

*Scaling Discovery through Test-Time Communication*, submetido em 17/09/2026, faz agentes sem papéis pré-definidos compartilharem descobertas por um diretório comum. Em ARC-AGI-3, um time de `k` agentes comunicantes igualou o success rate de `4k` agentes independentes, e houve tarefas que nenhum agente isolado resolveu, mas times resolveram de forma confiável. Os ganhos também apareceram em tarefas de pesquisa; quatro agentes produziram um classificador MNIST de 1.957 bytes com 99,4% de acurácia. Os próprios autores mostram o limite: quando compute é restrito ou não existe métrica clara de progresso, agentes independentes podem superar comunicação.

Fonte: https://arxiv.org/abs/2609.21032

### Mecanismo psicológico/comportamental

Comunicação só cria valor quando uma descoberta parcial pode ser reconhecida, persistida e reutilizada pelo restante do time. Sem feedback de progresso, coordenação vira overhead.

### Implicação para produto

O router multi-agent do AI Hub deveria considerar pelo menos:
`decomponibilidade + feedback de progresso + orçamento de compute + artefato compartilhado`.
O workspace comum precisa armazenar breakthroughs verificáveis, não apenas mensagens entre agentes.

### Hipótese/experimento

Comparar `best-of-k independente` versus `k agentes com workspace compartilhado`, mantendo orçamento equivalente. Medir sucesso, custo, tempo, redundância e qualidade das descobertas reutilizadas.

### Riscos e limites

Benchmarks e tarefas de pesquisa não são equivalentes a UX de atendimento ou marketing. Resultados com mais compute não provam eficiência econômica.

**Sem card:** forte para o AI Hub, mas pertence a harness/orquestração e não cabe nas quatro coleções aceitas.

---

## Experience Engine v21

A rodada sugere acrescentar uma camada explícita de **validação e autorização da adaptação**:

```text
usuário
   ↓
INTENÇÃO + ESTADO OBSERVÁVEL
   ↓
INTERVENTION POLICY
   ├── existe necessidade?
   ├── benefício > intrusão?
   └── momento apropriado?
   ↓
PERSONALIZATION PIPELINE
   ├── observação
   ├── inferência
   ├── persistência
   ├── acesso
   └── ação autorizada
   ↓
GENERATIVE UI COMPILER
   ├── objetivo
   ├── componente
   ├── testes funcionais/adversariais
   └── validação quando necessária
   ↓
AGENCY CHECKPOINTS
   ↓
MULTI-AGENT ROUTER
   ├── progresso mensurável?
   └── descoberta compartilhável?
   ↓
resultado + task success
+ agência + rastreabilidade
```

O princípio da rodada é: **adaptar não é apenas gerar uma resposta diferente; é decidir o que pode ser inferido, quando agir, como validar o que foi gerado e onde o usuário continua no controle**.

---

## Cards derivados nesta rodada

Antes de criar cards, foi consultada a versão atual de `harness-library-api/docs/guia-uso-api-cards.md`. As coleções válidas permanecem `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`; os arquivos no repositório representam apenas candidatos a `DRAFT`.

### `agencia-usuario-alinhamento-conversa-ai`

- **Coleção:** `neuromarketing`
- **Por que importa:** converte um achado de Human-AI Interaction em uma hipótese operacional para Psique/customer-agent: preservar escolha e possibilidade de redirecionamento, em vez de depender apenas de uma persona polida e fluida.
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-09-21-agencia-usuario-alinhamento-conversa-ai.md`
- **SHA-256:** `ebceb04c55e8b7b562312ae4794c210e44e78cb58a6046cfc132cee64166221a`
- **JSON:** `pesquisas/design-experiencia/cards/2026-09-21-agencia-usuario-alinhamento-conversa-ai.json`

Nenhum card foi criado para Generative UI, multi-agent, voz ou governança de personalização porque os achados são predominantemente arquitetura/harness ou ainda não têm encaixe comercial legítimo nas coleções aceitas. Haptics e proatividade ficaram sem nova versão por já existirem cards mais fortes e aderentes sobre esses conceitos.

Nenhum candidato foi enviado para revisão, ativado ou arquivado.
