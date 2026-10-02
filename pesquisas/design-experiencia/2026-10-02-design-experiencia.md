# Radar de Design de Experiência — 2026-10-02

## Síntese

Os achados de hoje convergem em um princípio: UX com IA deve reagir ao estado real da tarefa e do usuário, em vez de aplicar uma política fixa. Fricção, personalização, confirmação de voz, guardrails, memória de preferência e alinhamento de intenção funcionam melhor quando são contingentes, verificáveis e revisáveis.

## 1. Fricção produtiva ligada ao engajamento

**Descoberta e evidência.** *Who Thinks First? Designing Productive Friction with Engage-to-Unlock GenAI* avaliou 398 participantes em Human-Only, Standard Chatbot, Engage-to-Unlock e Time-Matched Unlock. Frente ao chatbot padrão, Engage-to-Unlock aumentou prompts (7,70 vs. 3,35), reduziu cobertura verbatim de texto da IA no ensaio final (27,32% vs. 47,16%) e reduziu o tempo de avaliação posterior (17,41 vs. 22,21 min), sem diferença significativa no tempo total. Entre erros detectados, a classificação correta do tipo de erro foi 90,43% vs. 78,12%.

**Mecanismo.** A intervenção parece deslocar esforço para o início e dar ao usuário um referencial próprio antes da geração. O controle Time-Matched indica que atrelar o desbloqueio a contribuição real importa mais do que simples espera.

**Produto/experimento.** Em tarefas em que julgamento prévio importa, testar um desbloqueio curto após objetivo, hipótese, restrição ou critério de sucesso. Comparar com geração imediata e espera equivalente.

**Riscos/limites.** O domínio foi escrita argumentativa; não demonstra efeito em conversão, receita ou qualidade comercial. Fricção mal calibrada pode virar burocracia.

Fontes:
- https://arxiv.org/abs/2610.01518
- https://arxiv.org/html/2610.01518v1

## 2. Personalização: habitual e exploratório pedem representações diferentes

**Descoberta e evidência.** *When LLM-Inferred User Context Adds Value in Production Streaming Recommendation* avaliou perfis em uma plataforma de streaming. Perfis agregados foram mais fortes sob consumo habitual, cerca de quatro quintos da amostra; perfis gerados por LLM foram melhores para usuários exploratórios. A representação por LLM também aumentou modestamente diversidade dentro da lista, mas reduziu cobertura de catálogo e novidade.

**Mecanismo.** A agregação preserva afinidades específicas; a síntese semântica pode capturar interesses emergentes, mas pode concentrar recomendações em itens populares.

**Produto/experimento.** Testar rota habitual baseada em comportamento agregado e rota exploratória baseada em síntese semântica apenas quando houver sinais confiáveis de mudança; monitorar cobertura e novidade, não só acerto.

**Riscos/limites.** Avaliação offline, uma plataforma e um domínio; o regime exploratório é definido retrospectivamente e não há prova de impacto em retenção ou receita.

Fonte:
- https://arxiv.org/abs/2609.38999

**Card:** não criado; evidência ainda específica demais para uma orientação comercial reutilizável.

## 3. Voz sob ruído precisa de ação verificável

**Descoberta e evidência.** *VAmoS Part Deux* testou 100 chamadas com 2 a 4 pedidos, 16 ferramentas e 14 stacks de voz. A conclusão ficou entre 17,3% e 44,7%; com televisão ao fundo, a conclusão agregada caiu de 38,7% para 8,6%. O estudo também mostra que uma resposta falada plausível pode ser aceita mesmo quando o estado real da ferramenta está errado.

**Mecanismo.** Fluência sonora pode funcionar como sinal de conclusão, mesmo sem evidência de execução correta.

**Produto/experimento.** Para ações relevantes, mostrar estado da tarefa e confirmar proporcionalmente ao impacto. Avaliar áudio + interpretação + tool state + resultado final.

**Riscos/limites.** Benchmark simulado de domínio específico.

Fonte:
- https://arxiv.org/abs/2609.38512

**Card:** não criado; é robustez/arquitetura de voz, não aderente a `prazer-audio-visual`.

## 4. Guardrails de atenção precisam de consequência proporcional

**Descoberta e evidência.** *When Attention Guardrails Become Barriers to Learning* analisou 14.529 flags de câmera em 448 estudantes, 615 registros de emoção de 273 participantes e respostas livres. Entre 51 respondentes qualitativos, 18 mencionaram espontaneamente o monitoramento. As críticas se concentraram em ações comuns interpretadas como distração e na severidade da consequência, como reiniciar conteúdo já visto.

**Mecanismo.** Baixa precisão percebida combinada a consequência alta e pouca recuperação reduz senso de controle.

**Produto/experimento.** Usar escada de severidade: sinal → explicação → confirmação → recuperação. Comparar isso com bloqueio automático.

**Riscos/limites.** Estudo observacional e educacional; não estabelece causalidade geral.

Fonte:
- https://arxiv.org/abs/2609.39023

**Card:** não criado; evidência/contexto ainda estreitos para generalização.

## 5. Adaptive UX pode aprender preferências pelas correções

**Descoberta e evidência.** *SPHERE* usa fala e edições em VR para aprender preferências espaciais. Em estudo com 42 participantes, o sistema adaptativo reduziu significativamente o número de correções e reduziu demanda física e esforço; a queda global do NASA-TLX não foi significativa.

**Mecanismo.** Correções feitas pelo usuário funcionam como demonstrações de preferência contextual; restrições hierárquicas ajudam a separar padrão persistente de ajuste local.

**Produto/experimento.** Comparar perfil inicial estático com preferências aprendidas de correções confirmadas, medindo ajustes, tempo até aceitação e reversões.

**Riscos/limites.** VR/layout espacial não equivale a marketing digital; uma correção isolada não deve virar preferência permanente.

Fonte:
- https://arxiv.org/abs/2610.02023

**Card:** não criado; não há coleção atual adequada.

## 6. Intenção deve ser um estado revisável

**Descoberta e evidência.** *Beyond Oracle Communication* propõe Drift-Bench++, com falhas de comunicação, mudanças de objetivo e paciência limitada. Modelos mais fortes se beneficiam de interação, mas permanecem distantes do cenário oracle; os autores também validam a presença desses tipos de falha em sessões do ProdAgent.

**Mecanismo.** Um agente que congela o briefing inicial pode continuar otimizando um objetivo que já mudou.

**Produto/experimento.** Manter `intent state` revisável e re-ground quando houver correção, contradição ou mudança material. Comparar intenção fixa, revisão periódica e revisão acionada por sinais.

**Riscos/limites.** Não há política universal demonstrada para detectar mudança; perguntas demais também geram atrito.

Fonte:
- https://arxiv.org/abs/2609.38604

**Card:** não criado; trata de harness/agent interaction.

## 7. Emotional design: limites relacionais precisam continuar visíveis

**Descoberta e evidência.** *Afterglow* estudou um memorial com IA por meio de pesquisa formativa (N=57) e seis walkthroughs com 20 participantes. O trabalho propõe **Legible Restraint**: limites de autoridade relacional precisam permanecer claros mesmo quando mudam voz, contexto, gatilho ou fonte de dados; e **Designing for Goodbye**, para que a experiência continue disponível sem tornar o uso contínuo uma obrigação.

**Mecanismo.** Quanto mais responsivo e relacional o agente, maior o risco de a presença sugerir uma autoridade ou reciprocidade que o sistema não possui.

**Produto/experimento.** Em personas emocionais, comparar limites mostrados só no onboarding com limites reforçados nos momentos em que o agente muda de papel ou toma iniciativa.

**Riscos/limites.** Estudo qualitativo e de domínio específico; não há prova de efeito comercial.

Fonte:
- https://arxiv.org/abs/2609.38729

**Card:** não criado; não há encaixe legítimo nas quatro coleções atuais.

## Experience Engine v32

`estado da tarefa → intenção/regime do usuário → política adaptativa → ação → evidência → correção → atualização de estado`

Regras práticas: fricção ligada ao trabalho; personalização com fallback; ação por voz verificável; preferência e intenção revisáveis; guardrails recuperáveis; limites relacionais persistentes.

## Cards

Foi consultada a versão atual de `harness-library-api/docs/guia-uso-api-cards.md`. As coleções válidas permanecem `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

Foi preparada uma atualização de `feedback-metacognitivo-reduz-offloading-ai`, coleção `neuromarketing`. A nova evidência diferencia fricção ligada ao engajamento de mera espera.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-10-02-friccao-produtiva-engage-to-unlock.md`

SHA-256: `017a2acc29d5936a05d569d716c873263d3f0564d2bdfc8b468d8cbfc64b8c0d`

JSON: `pesquisas/design-experiencia/cards/2026-10-02-friccao-produtiva-engage-to-unlock.json`

O arquivo é candidato a `DRAFT`; esta rodada não envia para revisão, não ativa e não arquiva versões.
