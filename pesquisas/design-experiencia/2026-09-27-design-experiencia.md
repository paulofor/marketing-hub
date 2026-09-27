# Radar de Design de Experiência — 2026-09-27

## Síntese

A rodada reforça que confiança, supervisão e personalização precisam ser tratadas como estados calibráveis. Um score de confiança pode decidir quando escalar uma avaliação; a presença de um humano não assegura por si só boa supervisão; interfaces inclusivas precisam controlar carga cognitiva; e até o rótulo “recomendado por IA” pode mudar a percepção de personalização e privacidade.

## 1. Confiança como sinal de roteamento

*JEV-as-a-Judge*, submetido em 22 de setembro, testa um avaliador econômico que encaminha casos de baixa confiança para um modelo mais forte. Em tarefas comuns, o primeiro avaliador ficou próximo do comparator forte; em tarefas de derivação e respostas erradas escritas de forma convincente, o gap aumentou. Em um teste congelado, o cascade preservou aproximadamente 99% da acurácia do comparator no workload testado com custo de comparação menor.

**Mecanismo:** incerteza pode alocar recursos, mas não certifica correção.

**Aplicação/experimento:** comparar revisão única forte, revisão econômica e cascade calibrado por workload. Medir falso aceite, falso bloqueio, custo, latência e qualidade final.

**Limites:** thresholds são específicos do workload e a latência sequencial ao vivo não foi medida.

Fonte: https://arxiv.org/abs/2609.26550

## 2. “Human in the loop” também exige capacidade humana mantida

*The Unmonitored Dependency*, submetido em 25 de setembro, analisou cinco frameworks públicos de segurança de IA — Anthropic, OpenAI, Google DeepMind, Meta e xAI — nas versões vigentes em 8 de julho de 2026. O paper conclui que os cinco atribuem decisões relevantes a julgamento humano, mas nenhum contém, no texto público analisado, uma provisão operacional para assegurar que essa capacidade seja mantida ao longo do tempo.

**Mecanismo:** supervisão pode virar controle nominal quando a capacidade humana é presumida como estável.

**Aplicação/experimento:** acompanhar amostras assistidas e não assistidas de revisores ao longo do tempo, medindo detecção de erro e capacidade de override.

**Limites:** a conclusão é sobre documentos públicos, não sobre práticas internas nem sobre incompetência real de revisores.

Fonte: https://papers.ssrn.com/abstract=7248205

## 3. Adaptive UX precisa controlar carga cognitiva

Uma revisão sistemática publicada online em 24 de setembro no *Human Factors* sintetizou 93 estudos com uso direto de IA por adultos mais velhos e comparou ChatGPT e Amazon Alexa com as recomendações derivadas. Os temas recorrentes incluem usabilidade, privacidade, explicabilidade, autonomia, personalização e letramento digital; aparecem também sobrecarga de informação e complexidade de interface.

**Mecanismo:** quando o custo cognitivo da interface supera o benefício da adaptação, mais capacidade de IA não se converte automaticamente em experiência melhor.

**Aplicação/experimento:** comparar UI fixa com modo adaptativo de densidade, tamanho das etapas e profundidade das explicações. Medir task success, erros, tempo, pedidos de ajuda e controle percebido.

**Limites:** adultos mais velhos são heterogêneos e a revisão não prova que uma configuração específica causará melhor resultado.

Fonte: https://journals.sagepub.com/doi/full/10.1177/00187208261490714

## 4. “Recomendado por IA” pode aumentar personalização percebida e também saliência de privacidade

O artigo peer-reviewed *How artificial intelligence recommendation tags shape advertising attitude*, publicado em 2 de setembro, relata três experimentos. Segundo o resumo, rótulos explícitos de recomendação por IA elevaram a personalização percebida, associada no modelo dos autores a atitude publicitária mais positiva. O efeito variou conforme o fit da recomendação e preocupações de privacidade; com alta preocupação de privacidade, a relação positiva entre personalização percebida e atitude ficou mais fraca.

**Mecanismo:** o disclosure pode tornar a personalização mais saliente, mas também tornar privacidade mais saliente.

**Aplicação/experimento:** manter oferta e recomendação constantes e variar rótulo explícito, implícito e ausente. Medir personalização percebida, atitude, CTR, opt-out, checkout e pagamento reconciliado.

**Limites:** o material público revisado expõe o resumo; não há evidência de aumento de conversão ou vendas.

Fonte: https://www.sbp-journal.com/index.php/sbp/article/view/16641

## Cards da rodada

Foi consultado o guia atual em `harness-library-api/docs/guia-uso-api-cards.md`. As coleções válidas continuam sendo `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

Foi criado o candidato `rotulo-recomendacao-ai-personalizacao-privacidade`, coleção `neuromarketing`. Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-27-rotulo-recomendacao-ai-personalizacao-privacidade.md`. SHA-256: `e4af7db5c893654b43a4900c44ae7a1398cf9f8cea61515bec19895df3c13913`. JSON: `pesquisas/design-experiencia/cards/2026-09-27-rotulo-recomendacao-ai-personalizacao-privacidade.json`.

Nenhum card foi enviado para revisão, ativado ou arquivado. Os achados sobre avaliação adaptativa, capacidade supervisora e inclusive UX ficaram sem card porque nenhuma coleção atual os representa sem distorção.
