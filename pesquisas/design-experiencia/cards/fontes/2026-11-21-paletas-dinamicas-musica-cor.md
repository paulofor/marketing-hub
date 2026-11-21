# Fonte revisada — paletas dinâmicas música–cor

**Artigo analisado:** Wei, W.-L.; Yang, K.; Lin, J. *Sonic Palette: Predicting Dynamic Color Themes from Music*, preprint submetido em 19/11/2026. https://arxiv.org/html/2611.25781v1

## Evidência encontrada
O sistema prevê, a partir da música, sequências de cinco cores dominantes por quadro. Base com 265 videoclipes oficiais (aproximadamente 18,5 horas; 1,7 milhão de quadros), divisão de 225 treino, 20 validação e 20 teste. Em teste, ACBD foi 3,5161, ante 3,8853 (Transformer não autorregressivo) e 4,6757 (MLP estática), e ATBD foi 0,8671, ante 1,0068 e 0,9959, respectivamente. Menor é melhor nas duas métricas de reconstrução cromática. Avaliação subjetiva com 26 avaliadores favoreceu transições mais naturais e melhor correspondência percebida entre atmosfera de cor e música. Não foram medidos comportamento de compra ou prazer neural.

## Hipótese interpretativa
Manter continuidade cromática temporal e variar paletas com a estrutura musical pode melhorar congruência percebida entre imagem e som. Mediação psicológica por emoção é sugerida pela literatura anterior citada pelos autores, não testada causalmente neste experimento.

## Aplicação possível
Testar vídeos de marketing com cor/iluminação seguindo alterações musicais, mas preservar cores da marca, legibilidade, conforto visual, acessibilidade e licenças de áudio/vídeo. Usar como critério criativo experimental, não como regra universal.

## Resultado real observado
Melhor previsão de paletas e de variação temporal no conjunto de teste do estudo e melhor avaliação subjetiva de naturalidade e correspondência; **nenhuma demonstração de maior retenção publicitária, desejo, CTA, pagamento ou ROI**.

## Limites e riscos
Preprint, amostra de teste de 20 vídeos e 26 avaliadores; videoclipes possuem direção estética prévia, possivelmente enviesada por gêneros e convenções; recursos originais podem exigir licenças. Alterações de cor automáticas podem prejudicar identidade visual e acessibilidade.
