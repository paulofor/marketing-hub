# Fonte revisada — validação humana de expressões faciais geradas por IA

Data da revisão: 2026-09-23

## Fonte primária

Hareli, S. & David, S. **Creating and validating photorealistic AI-generated facial expression stimuli for emotion research.** *Behavior Research Methods* (2026). Publicado em 22/09/2026. DOI: 10.3758/s13428-026-03156-0  
https://link.springer.com/article/10.3758/s13428-026-03156-0

## Achado revisado

Três estudos, somando mais de 2.000 participantes, avaliaram rostos fotorealistas gerados por IA e pré-selecionados por ferramentas computacionais para expressar emoções-alvo. Os participantes humanos reconheceram bem as emoções pretendidas e não distinguiram de forma confiável as imagens geradas de fotografias reais. Porém, reconhecimento categórico e fotorealismo não garantiram naturalidade ou autenticidade equivalentes: felicidade e neutralidade foram melhor avaliadas, enquanto raiva recebeu avaliações inferiores; a autenticidade da raiva não ficou acima do ponto médio da escala.

## Mecanismo proposto

Reconhecer uma categoria emocional e perceber uma expressão como natural/autêntica são julgamentos distintos. Pessoas integram a configuração facial com pistas sociais e expectativas que um prompt ou classificador automático pode não capturar. Por isso, validação computacional não substitui avaliação humana quando a experiência depende de credibilidade emocional.

## Força da evidência

**Média-alta para validação de estímulos faciais estáticos.** Há amostras grandes e replicação em múltiplos estudos. A generalização para vídeo dinâmico, confiança em avatares e resultado comercial ainda precisa de teste.

## Limitações

- Estímulos estáticos, não vídeo.
- Expressões e identidades geradas podem carregar vieses demográficos ou sociais.
- Algumas emoções, especialmente raiva e nojo, podem ser mais difíceis de separar.
- Em vídeo, ainda é necessário validar consistência de identidade, transições temporais e onset–apex–offset.
- O estudo não demonstra aumento de confiança, prazer, conversão ou venda.

## Aplicação experimental

Para avatares e personagens gerados por IA, aplicar QA em duas camadas: classificação automática e avaliação humana de reconhecimento, naturalidade e autenticidade. Em vídeo, acrescentar avaliação temporal. Comparar esse pipeline com geração baseada apenas em prompt.

## Relação com o card

O card `emocao-facial-ia-validacao-humana` é uma regra de QA experimental. Ele não afirma que rostos mais autênticos aumentam vendas.
