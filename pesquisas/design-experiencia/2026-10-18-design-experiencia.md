# Radar de Design de Experiência — 18/10/2026

**Recorte:** artigos submetidos em 14 e 15/10, disponibilizados no lote de 15–16/10. Consulta em domingo, 18/10. Sem lote regular mais recente em cs.HC após 16/10. Seleção de achados não apresentados nas rodadas 08–17/10.

## Síntese

O padrão da rodada é a diferença entre executar uma tarefa, preservar seu resultado verificável e entregar ao humano um relatório utilizável. Para vídeos, um novo estudo sugere registrar mudanças de cenas e rever evidências originais em vez de confiar apenas em resumos.

## 1. TaskVM — controlar estado desejado, preservado e observado

**Fonte:** Yang et al., *Task-State Governance: A Virtual Machine for Verifiable and Recoverable GUI Agent Execution*, arXiv:2610.20100, 14/10/2026 — https://arxiv.org/abs/2610.20100

**Evidência:** protótipo com verificador independente e estados desejado/preservado/observado. Em 36 tarefas, 21/36 (58,3%) satisfizeram estritamente o contrato contra 18/36 (50,0%) no agente GUI direto; estudo cruzado com 37 participantes encontrou controle percebido de recuperação 5,56/7 contra 4,63/7 (p=0,005). **Custo:** 19.960 segundos agregados no protótipo versus 9.010 segundos no comparador direto.

**Mecanismo interpretativo:** o usuário governa efeitos externos e invariantes, não só o plano interno do agente. **Aplicação:** verificar orçamento, publicação e integridade dos ativos do Marketing Hub por evidência observada. **Experimento:** comparação com supervisão por logs, mensurando efeitos indevidos, custo e reversões. **Limites:** o que aparece na tela não prova todo o estado do backend; compensação não equivale a rollback transacional; maior custo computacional. Sem card, por falta de coleção adequada.

## 2. Reply Usability — resultado correto também precisa ser compreensível

**Fonte:** Jiang et al., *Measuring and Reducing Unusable Replies of LLM Agents*, arXiv:2610.19659, 14/10/2026 — https://arxiv.org/abs/2610.19659

**Evidência:** 54% de 480 respostas de oito modelos não eram utilizáveis pelo leitor que via apenas pedido e resposta; erros frequentes: referências privadas e nomes internos. Exigir requisitos de entrega reduziu falhas de 52% a 43%; reescrita por segundo modelo reduziu a 36%, nas condições avaliadas.

**Mecanismo:** falha de contexto compartilhado; agentes supõem que usuários sabem o que aconteceu na execução interna. **Aplicação:** um delivery gate independente que confira respostas diretas, termos definidos, links navegáveis e próximos passos. **Experimento:** medir número de pedidos de esclarecimento e tempo até ação. **Limites:** avaliação automatizada e risco de introduzir erros ao reescrever. Sem card: UX de agentes não possui coleção.

## 3. DeltaStreamer — memória de mudanças e replay de vídeo

**Fonte:** Sun et al., *DeltaStreamer: Towards Human-Like Agentic Streaming Video Understanding*, arXiv:2610.19849, 14/10/2026 — https://arxiv.org/abs/2610.19849

**Evidência:** arquitetura que registra mudanças visuais entre observações, organiza episódios em memória hierárquica e revisita trechos de vídeo sob demanda. Pontuação 66,07 no OVO-Bench e 59,53 no StreamingBench; remover replay reduziu OVO-Bench de 66,07 para 63,28; usar histórico completo em lugar da memória hierárquica resultou em 59,18.

**Mecanismo interpretativo:** preservar *o que mudou e quando* facilita recuperar evidências que resumos estáticos apagariam. **Aplicação:** revisão de vídeos por Apolo/Têmis com timeline rastreável de textos, preços, objetos, falas e CTAs, acompanhada de replay. **Experimento:** comparar com revisão por frames fixos; medir precisão de alertas, falsos positivos, custo e latência. **Limites:** benchmarks técnicos não representam espectadores ou conversão; privacidade e direitos sobre os vídeos. **Card candidato:** `video-revisao-memoria-deltas-replay`, coleção `video`.

## 4. Affective Personal Informatics — predominância de interfaces textuais

**Fonte:** Liu et al., *Scoping the Landscape of Large Language Model-based Affective Personal Informatics*, arXiv:2610.20084, 14/10/2026 — https://arxiv.org/abs/2610.20084

**Evidência:** revisão de 45 artigos de 2020 a junho/2026 aponta predomínio de pipelines centrados em texto e amostras de jovens universitários. **Mecanismo proposto:** exigir que pessoas verbalizem emoções impõe carga de interpretação e exclui modos diferentes de expressão. **Aplicação:** testar experiências acessíveis e multimodais com consentimento e avaliação humana. **Limites:** a revisão não prova que voz, face ou sinais fisiológicos sejam indicadores confiáveis de emoções ou de consumo. Sem card.

## 5. ShadAR — efeitos visuais por voz

**Fonte:** Mei et al., *ShadAR: Real-time Prompting On-demand Visual Augmentation in Augmented Reality*, arXiv:2610.19998, 14/10/2026 — https://arxiv.org/abs/2610.19998

**Evidência:** comandos de voz geram shaders aplicados à realidade aumentada. Em 30 testes, 26 shaders foram corretos sintaticamente na primeira tentativa com raciocínio baixo, e 28 com raciocínio alto. A implantação demorou em média 32,67 s ou 42,66 s; workshop com 21 pesquisadores diferenciou comandos procedimentais de objetivos vagos. **Mecanismo:** falar intenção elimina programação, mas ambiguidade amplia interpretação. **Aplicação:** editor de criativos com comandos específicos e prévias. **Experimento:** avaliar tempo para primeira prévia útil e correções. **Limites:** protótipo XR com latência alta; não há prova de impacto comercial. Sem card.

## 6. Speculative Sampling — a responsividade da geração de vídeo

**Fonte:** Huang et al., *Speculative Sampling in Interactive Video Generation*, arXiv:2610.20556, 14/10/2026 — https://arxiv.org/abs/2610.20556

**Evidência:** proposta técnica que prevê quadros futuros substituíveis e reaproveita amostragens quando comandos não mudam. **Mecanismo:** o usuário percebe a espera até sua intervenção modificar a tela, não só o tempo total de renderização. **Aplicação:** em sistemas de vídeo interativo, medir latência entre comando e resposta visual, continuidade e custo GPU. **Experimento:** comparar estratégias de amostragem numa implementação futura. **Limites:** avaliação técnica sem estudo de espectadores ou conversão. Sem card.

## Experience Engine v48

`Pedido → contrato desejado/preservado → execução → verificador independente → memória de mudanças com fontes → retorno compreensível → avaliação humana/comercial`.

Prioridades: (1) usabilidade do retorno dos agentes; (2) verificabilidade e preservação de estado nas operações; (3) revisão audiovisual temporal com replay. Resultados comerciais permanecem hipóteses; devem ser medidos por retenção, compreensão, custo, CTA e pagamentos reais.

## Cards e decisão editorial

Conferido `harness-library-api/docs/guia-uso-api-cards.md` na `main`: coleções aceitas `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`. Selecionado um único candidato a DRAFT: `video-revisao-memoria-deltas-replay` em `video`. TaskVM e Reply Usability são achados fortes que **não viraram cards por falta de coleção válida**; os demais permanecem no relatório. A geração dos arquivos não autoriza revisão, ativação nem arquivamento.
