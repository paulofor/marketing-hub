# Radar de Design de Experiência — 2026-09-26

## Síntese

A rodada aponta uma regra prática: fricção, iniciativa e confiança devem ser calibradas, não maximizadas. Ajudar o usuário a pensar não exige bloquear ajuda; vários agentes podem funcionar melhor quando trabalham sobre o artefato real; e experiências audiovisuais precisam preservar imersão sem esconder incerteza e proveniência.

## 1. Guardrails podem virar roadblocks

O preprint *Guardrails or Roadblocks?* relata um RCT 2x2 com 132 estudantes de programação. A condição Socrática + Contexto Completo recebeu a menor avaliação de suporte à conclusão da tarefa: 3,53/5, contra 4,12–4,27 nas demais, com p=0,007. O uso de outro LLM e a compreensão pós-tarefa também tiveram sinais descritivos menos favoráveis nessa condição, embora essas diferenças não tenham sido estabelecidas como significativas.

**Mecanismo:** fricção informativa pode promover autorregulação; fricção rígida pode apenas elevar o custo da interação.

**Aplicação/experimento:** comparar ajuda direta, feedback metacognitivo curto com rota de ajuda direta, e orientação Socrática rígida. Medir task success, turnos, retrabalho, procura de outra ferramenta e satisfação.

**Limite:** contexto educacional; não há evidência de impacto comercial.

Fontes: https://arxiv.org/abs/2609.29995 e https://arxiv.org/abs/2609.20143

## 2. Multi-agent UX pode funcionar melhor em torno do artefato

*DocuTeam* usa o documento como superfície compartilhada entre humano e agentes. Em estudo within-subjects com 20 participantes, os resultados foram avaliados como mais novos, relevantes e específicos; houve 46% menos mensagens de steering, 39% mais modificações no documento e nenhuma diferença significativa de carga global pelo NASA-TLX.

**Mecanismo:** o usuário deixa de reconstruir contexto no chat e mantém atenção no trabalho em evolução.

**Aplicação/experimento:** comparar um painel de chat multi-agent com agentes ancorados a blocos, claims, cenas ou tarefas do artefato. Medir coordenação, qualidade externa, alterações úteis e sobrecarga.

**Limite:** N=20 e domínio de planejamento; atividade simultânea de vários agentes pode sobrecarregar.

Fonte: https://arxiv.org/abs/2609.29309

## 3. Em avatares, imersão e verificabilidade são dimensões diferentes

*Signals of AI Hallucination* comparou gestos/postura, ícones e texto com citações em um agente incorporado em VR. No estudo within-subjects com 24 participantes, as três formas de sinal melhoraram a identificação de conteúdo potencialmente incorreto frente ao baseline. Gestos favoreceram confiança e imersão; texto favoreceu interpretabilidade e menor demanda mental; ícones funcionaram como meio-termo.

**Mecanismo:** sinais sociais preservam fluxo, mas são menos explícitos; sinais persistentes tornam risco e proveniência mais fáceis de inspecionar.

**Aplicação/experimento:** em agentes audiovisuais, combinar sinal social leve com ícone persistente e acesso à fonte. Comparar gesto isolado, ícone, cue multimodal e ausência de cue.

**Limite:** N=24, VR e exposição curta; a própria sinalização pode elevar confiança.

Fonte: https://arxiv.org/abs/2609.28812

## 4. Autonomia pode ser um privilégio conquistado

*Working with Agentic Teammates* analisou um agente persistente e proativo implantado em mais de 20 equipes por cinco meses, com mais de 41 mil turnos. O estudo qualitativo entrevistou 17 participantes de 11 equipes. Um problema recorrente foi o agente tratar arquivos antigos ou especulativos como atuais; os autores descrevem a autonomia como algo que deveria ser progressivamente conquistado.

**Mecanismo:** capacidade técnica não cria legitimidade social; confiança depende de desempenho observado, consentimento e controle percebido.

**Aplicação/experimento:** evoluir permissões por níveis — observar, sugerir, preparar, executar com confirmação, autonomia delimitada — e comparar com autonomia ampla desde o início.

**Limite:** estudo qualitativo em uma empresa; não prova causalmente o melhor esquema de níveis.

Fonte: https://arxiv.org/abs/2609.29901

## 5. Voz deve ser avaliada pela ação e pela recuperação

*Voice Agents under Acoustic Stress* propõe TRACE para testar a mesma tarefa com áudio original e degradado por ruído, reverberação ou fala concorrente, avaliando conclusão, ações erradas, recuperação e esforço do usuário.

**Mecanismo:** um erro de reconhecimento só importa quando se propaga para interpretação, ação ou custo de recuperação.

**Aplicação/experimento:** combinar confiança acústica e consequência da ação; confirmar comandos de maior impacto quando a entrada estiver incerta e medir erros, turnos de recuperação e abandono.

**Limite:** framework/overview, não RCT de UX.

Fonte: https://arxiv.org/abs/2609.29452

## Cards da rodada

Foi consultado o guia atual em harness-library-api/docs/guia-uso-api-cards.md. As coleções válidas continuam sendo video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c. Os arquivos abaixo permanecem candidatos a DRAFT.

**Atualização:** feedback-metacognitivo-reduz-offloading-ai, coleção neuromarketing. A nova evidência diferencia fricção reflexiva de fricção obstrutiva. Fonte: pesquisas/design-experiencia/cards/fontes/2026-09-26-feedback-metacognitivo-friccao-calibrada.md. SHA-256: 31a0087223818c79c659a04918f020d7aefc25feb7ce53bbb1c6f53b67d2c196. JSON: pesquisas/design-experiencia/cards/2026-09-26-feedback-metacognitivo-reduz-offloading-ai.json.

**Novo:** sinais-incerteza-avatar-modalidade-evidencia, coleção prazer-audio-visual. É útil por separar presença audiovisual, interpretabilidade e confiança em agentes com avatar. Fonte: pesquisas/design-experiencia/cards/fontes/2026-09-26-sinais-incerteza-avatar-modalidade-evidencia.md. SHA-256: 8d35b22a44d58a47d86452e4904db73e0ada97b5fa197f00e12d5a6f61459fe0. JSON: pesquisas/design-experiencia/cards/2026-09-26-sinais-incerteza-avatar-modalidade-evidencia.json.

Nenhum card foi enviado para revisão, ativado ou arquivado.

## Achados fortes sem card

DocuTeam ficou sem card por ser principalmente arquitetura de colaboração multi-agent. Agentic Teammates ficou sem card por tratar governança e autonomia. Voice Agents under Acoustic Stress ficou sem card por ser arquitetura e avaliação de voz. Forçar esses achados nas coleções atuais distorceria o conteúdo.
