# Fonte revisada do card — surprisal acústico e saliência temporal

## Origem

Derivado da rodada científica de 07/09/2026:
`pesquisas/prazer-audio-visual/2026-09-07-prazer-audiovisual.md`

Artigo principal:
Anikin A. *Measuring surprisal in sound sequences*. Behavior Research Methods. 2026;58(10):278.
DOI: 10.3758/s13428-026-03153-3

## Evidência revisada

Cento e noventa e cinco ouvintes avaliaram a previsibilidade de 300 sequências acústicas sintéticas. O estudo comparou diferentes medidas computacionais de surpresa. Shannon surprisal e novidade por matriz de auto-similaridade acompanharam melhor a imprevisibilidade percebida causada por variabilidade espectral, enquanto uma medida baseada em autocorrelação capturou melhor a irregularidade rítmica.

Vários algoritmos convergiram para uma janela de análise próxima de 1 segundo como particularmente relevante para reproduzir julgamentos humanos de imprevisibilidade espectral. O resultado sugere que surpresa auditiva não é uma variável única: irregularidade espectral e irregularidade temporal podem precisar de representações separadas.

## Limites

O estudo mede imprevisibilidade percebida e saliência potencial, não prazer, dopamina, retenção ou conversão. Os estímulos eram sequências sintéticas de vocalizações animais e sons ambientais, e os parâmetros foram otimizados no próprio conjunto de dados, com risco de overfitting destacado pelo autor.

## Uso como hipótese

Em edição e geração de áudio por IA, calcular curvas separadas de surprisal espectral e rítmico e testar a colocação de eventos visuais, cortes ou mudanças sonoras em picos de surpresa. A janela de aproximadamente 1 segundo deve ser tratada como ponto inicial de engenharia a validar por gênero, público e formato, não como constante universal.
