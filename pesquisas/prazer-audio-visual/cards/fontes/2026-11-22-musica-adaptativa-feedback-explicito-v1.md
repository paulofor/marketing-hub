# Fonte revisada v1 — Música adaptativa e feedback explícito

- **Data de revisão:** 2026-11-22
- **Artigo:** Kejing Chen, *Multimodal emotion recognition and feedback adaptation for personalized music therapy*.
- **Revista e data:** Scientific Reports, 2026-11-21 (versão inicial aceita e revisada por pares).
- **DOI:** https://doi.org/10.1038/s41598-026-77374-z
- **Coleção:** prazer-audio-visual
- **CardKey estável:** musica-adaptativa-feedback-explicito

## Achado verificado

O artigo apresenta um sistema computacional que combina reconhecimento multimodal de emoções, atributos de valência/ativação da música, preferências e feedback histórico para selecionar música. Emprega Transformer de fusão de sinais faciais, vocais e fisiológicos; VGGish para atributos musicais; e recomendação híbrida com otimização baseada em DQN. Reporta acurácia de reconhecimento de 90,1%, F1 de 89,6%, HR@10 de 84,2% e NDCG@10 de 80,5%. Em avaliação de curto prazo, a redução média da distância ao estado afetivo-alvo foi de 0,214 no sistema proposto, contra 0,186 no comparador Tensor Fusion, segundo os autores. Tais métricas são de tarefas distintas e não medem prazer, retenção comercial nem eficácia clínica.

## Mecanismo e interpretação crítica

A ideia operacional é um circuito de recomendação que observa a resposta do usuário e reajusta escolhas, em vez de pressupor que uma trilha fixa servirá para todos. O estudo demonstra uma proposta técnica com desempenho reportado em suas avaliações, mas não estabelece causalmente que adaptar a trilha melhore bem-estar, prazer ou tratamento. A identificação automática de emoções a partir de expressão facial, voz e fisiologia é imperfeita e contextual.

## Hipótese de uso a testar, não comprovada

Em experiências digitais não clínicas, comparar aleatoriamente: (A) trilha fixa adequada, (B) escolha manual do usuário e (C) recomendação adaptativa baseada **somente em feedback explícito e opt-in**. Igualar volume, conteúdo e duração; medir agrado autorrelatado, desconforto, interrupções, controle percebido e repetição voluntária; análise com participantes não vistos no treinamento. Um resultado favorável em engajamento não é evidência de vendas.

## Limitações e riscos

A avaliação do artigo não equivale a ensaio clínico independente, e comparações internas de desempenho não garantem generalização. O estudo não testa a variante de menor coleta de dados aqui proposta. Não inferir diagnósticos, estados psicológicos sensíveis ou emoções verdadeiras. Priorizar consentimento, minimização de dados, controles de volume, desligamento e acessibilidade. A evidência para a **aplicação comercial proposta é baixa**.

## Referência e auditoria

Fonte primária: https://www.nature.com/articles/s41598-026-77374-z

Material editorial revisado para geração de card candidato DRAFT. Sem solicitação de revisão editorial, ativação ou arquivamento.
