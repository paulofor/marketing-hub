# Radar Design de Experiência — 2026-10-01

Princípio da rodada: **experiências agentic maduras precisam tornar visível a incerteza certa, a proveniência da mudança e o estado real do trabalho — sem confundir fluência, maioria ou resumo com evidência.**

## 1. Incerteza precisa ser bem direcionada para proteger a decisão humana

*Referential Uncertainty in Human--AI Collaboration* estudou uma tarefa em que um humano descrevia uma peça e um agente visual precisava identificá-la e posicioná-la. Uma distribuição de crença elicidada separadamente foi melhor calibrada (ECE 0,15; AUROC 0,65) do que probabilidades brutas dos tokens de ação, que ficaram fortemente superconfiantes (confiança média 0,97; ECE 0,44). Em três modelos, pedidos de esclarecimento apareceram em apenas 3,5% a 16,7% dos turnos.

No estudo humano controlado (N=210), participantes que recebiam só a mensagem padrão aceitaram 78% dos movimentos errados e não distinguiam acertos de erros (AUC 0,50). Descrições precisas e, sobretudo, ressalvas bem direcionadas reduziram a aceitação de movimentos errados para 36%, preservando em grande parte a aceitação dos corretos.

**Mecanismo:** fluência sem ressalva pode funcionar como sinal implícito de certeza. Uma ressalva específica torna a possibilidade de erro saliente e cria um ponto de revisão. O próprio estudo mostra a condição de contorno: um hedge derivado de sinal de incerteza ruim pode piorar o resultado.

**Produto/experimento:** comparar `sem sinal`, `alerta genérico` e `ressalva específica + revisar evidência/confirmar referente`, medindo aceitação de erros e acertos separadamente.

**Riscos/limites:** é uma tarefa colaborativa de puzzle, não uma decisão comercial ou de alto risco; não demonstra efeito sobre conversão ou vendas.

Fonte: https://arxiv.org/abs/2609.39518

## 2. Provenance deve ficar presa ao artefato, não escondida no chat

Dois trabalhos novos convergem. **Reactant / “Who Asked for This?”** transforma anotações inline em transações verificáveis que ligam pedido, skill e transformações ao documento; relata quatro meses de uso autônomo por três colegas. **NarrativeSteward** organiza outline, worldbuilding e grafos narrativos como artefatos ligados, com registro de mudança, recuperação e verificação. Em estudo within-subject com 12 participantes, facilitou formular pedidos de revisão, inspecionar alterações e entender mudanças e estrutura frente a agentes generalistas.

**Mecanismo:** quando um agente altera um artefato grande, o custo cognitivo migra para “o que mudou e por quê?”. Chats separados obrigam o humano a reconstruir essa causalidade.

**Produto/experimento:** tratar `brief`, `claim`, `cena`, `asset`, `hipótese`, `métrica` e `regra` como objetos com lineage; comparar `chat + diff bruto` com `mudança ligada ao objeto + pedido original + agente/skill + evidência`.

**Limites:** Reactant tem amostra de uso muito pequena; NarrativeSteward tem N=12.

Fontes:
- https://arxiv.org/abs/2609.40126
- https://arxiv.org/abs/2609.39333

## 3. Voz full-duplex precisa separar conteúdo, timing e ciclo da tarefa

*DuplexAct-Bench* avaliou 12 sistemas em 1.290 trials em inglês e chinês, cobrindo interrupção, yielding, início proativo, silêncio ativo e backchanneling. Qualidade semântica e timing comportamental frequentemente divergiram: um sistema pode dizer algo correto no momento errado.

*Richard: Voice-First Mobile Interaction for Persistent Tasks* separa sessão de voz, execução da tarefa e entrega do resultado. O usuário pode sair da conversa e depois inspecionar, revisar ou recuperar o trabalho; estados e notificações continuam ligados à solicitação persistente.

**Produto/experimento:** modelar duas máquinas de estado — `escutar/falar/esperar/interromper` e `pedido/confirmação/execução/falha-conclusão/entrega` — e medir erro de timing, interrupções indevidas, abandono, perguntas “já terminou?” e recuperação após desconexão, não apenas WER.

**Limites:** DuplexAct é benchmark técnico; Richard é protótipo com verificação funcional, sem estudo humano amplo.

Fontes:
- https://arxiv.org/abs/2609.39446
- https://arxiv.org/abs/2609.39976

## 4. A conversa pode revelar uma preocupação e o resumo apagá-la

*Positive Ratings, Hidden Concerns* analisou entrevistas adaptativas por voz após um pré-survey. Em 44 primeiras entrevistas (132 observações tema-a-tema), 20% a 41% das sessões combinaram avaliação favorável com uma preocupação substantiva verbalizada depois.

*Whose Voice Survives the Summary?* analisou 2.586 respostas livres bilíngues e 45 resumos. Uma preocupação mencionada apenas uma vez era eliminada em 86% dos casos; conteúdos curtos e somente em alemão também tiveram retenção menor. Controlando frequência, o problema apareceu como viés de prevalência, não apenas de sentimento.

**Mecanismo:** há dois filtros: `disclosure` e `summarization`. A pessoa pode revelar algo só depois de ganhar segurança, e a síntese pode apagá-lo por ser raro.

**Produto/experimento:** separar `temas dominantes` de `sinais raros porém potencialmente críticos`, mantendo acesso ao trecho de origem. Comparar resumo por frequência com resumo que preserve minorias/contradições, medindo recall e falsos alarmes.

**Limite:** os dados são organizacionais; temas raros também podem ser ruído.

Fontes:
- https://arxiv.org/abs/2609.38788
- https://arxiv.org/abs/2609.38818

## 5. Desacordo entre avaliadores pode rotear atenção humana

*JuryFlow* decompõe respostas em claims, coleta veredictos de vários judges e constrói um grafo de desacordo. Em vez de encerrar pela maioria, usa entropia para localizar conflitos que merecem revisão. Uma intervenção focal pode propagar a correção e gerar rubrica reutilizável. Em MT-Bench e LLMBar, a configuração automática melhorou concordância com rótulos gold frente a single-judge e majority vote.

**Produto/experimento:** para Têmis, comparar `revisão total`, `maioria simples` e `revisão focal por desacordo`, medindo falso aceite, custo humano e estabilidade da rubrica.

**Limite:** o benchmark principal não inclui estudo humano; é evidência de engenharia.

Fonte: https://arxiv.org/abs/2609.40103

## 6. Caso de produto: personalização criativa grounded no histórico real

Em 30/09, o Instagram começou a liberar nos EUA um assistente conversacional dentro do Edits. Segundo a cobertura do lançamento, ele usa métricas do próprio criador — views, retenção, likes, shares e follows — além de comentários, tendências e interesses da audiência para sugerir conceitos, hooks, captions e scripts.

**Implicação:** o padrão interessante é `personalização + provenance`, não apenas “chat no editor”. Para o Marketing Hub, uma sugestão deveria poder responder “por que isto?” apontando os criativos, eventos e métricas que a sustentam.

**Hipótese:** sugestões ancoradas no histórico reconciliado reduzem regenerações e aumentam a proporção de experimentos aproveitáveis frente a sugestões genéricas.

**Limite:** é caso de produto recém-lançado, sem evidência pública controlada de melhora.

Fonte: https://techcrunch.com/2026/09/30/instagram-rolls-out-an-ai-video-assistant-for-creators/

## Alerta de design para públicos vulneráveis

Um experimento de campo randomizado com agentes de voz em uma hotline infantil encontrou forte efeito do framing sobre a resposta das crianças, enquanto a autoridade nominal da persona teve efeito próximo de zero. Esse achado deve ser tratado como alerta de vulnerabilidade e necessidade de guardrails mais fortes, não como técnica comercial.

Fonte: https://arxiv.org/abs/2609.38782

## Experience Engine v31

```text
INTENT / REFERENT
  ↓
UNCERTAINTY GATE
  ↓
ARTIFACT + PROVENANCE
  ↓
PARTICIPATION POLICY
  ↓
PERSISTENT TASK LIFECYCLE
  ↓
DISAGREEMENT ROUTER
  ↓
SUMMARY WITH MINORITY RETENTION
```

Direção: **externalizar os estados que realmente mudam a decisão humana, em vez de apenas produzir uma resposta final convincente.**

## Cards

O guia atual continua aceitando somente `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

Foi criado um único candidato a DRAFT: `incerteza-direcionada-reduz-aceitacao-erro-ai`, coleção `neuromarketing`. A fonte revisada tem SHA-256 `769dd5c9c11029e9f8cbd6609ef543ac5127192aa882a0da43be6c44ad54f565`.

Os achados fortes sobre provenance de autoria, NarrativeSteward, voz full-duplex/persistência, JuryFlow e retenção de vozes raras ficaram sem card porque são predominantemente arquitetura/harness/escuta organizacional e não se encaixam legitimamente nas quatro coleções atuais. O estudo com crianças ficou apenas como alerta.

Nenhum card foi enviado para revisão, ativado ou arquivado.
