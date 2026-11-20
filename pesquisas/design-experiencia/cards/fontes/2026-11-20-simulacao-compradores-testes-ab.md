# Fonte revisada — simulação de compradores e priorização de A/B

**Fonte primária:** Pasula et al. (Squoosh AI), *Squoosh Bench: Evaluating UX-1 on 318 Historical Website A/B Tests*, preprint submetido em 18/11/2026. https://arxiv.org/html/2611.25200v1

## Evidência encontrada
Os autores da empresa desenvolvedora de UX-1 avaliaram retrospectivamente 318 testes A/B de sites, selecionados entre 412 registros por duração superior a 21 dias. UX-1 simulou mil compradores por versão. A classificação original vencedora, variante vencedora ou inconclusivo coincidiu com 267/318 resultados históricos (84,0%; acurácia balanceada 83,4%), contra 196/318 (61,6%) para Qwen3 Next 80B na configuração testada. Entre 126 previsões favoráveis à variante, 118 corresponderam a vitórias históricas; 19 variantes vencedoras ficaram fora dessa seleção.

## Resultado real observado
As vitórias ou derrotas eram resultados de testes A/B humanos **já realizados**; a IA tentou prevê-los retrospectivamente. **Não houve implantação prospectiva, aumento comercial causado pelo simulador nem validação independente reportados.**

## Hipótese interpretativa
Modelar percursos de navegação, abandono e conversão com dados humanos pode ajudar a filtrar candidatos a testes futuros; isso não substitui experimentos reais.

## Aplicação possível
Usar previsão simulada como triagem para ordenar hipóteses de melhoria em páginas de produto, comparação e checkout. Registrar previsões antes do tráfego real e testar também uma amostra de candidatos reprovados pelo simulador.

## Limites e riscos
Preprint produzido pela própria empresa, dados privados não auditáveis publicamente, possível sobreposição desconhecida entre sites de treino e teste, seleção que excluiu testes curtos, heterogeneidade dos sites e dependência do protocolo. Acurácia de resultados históricos não comprova acurácia prospectiva, aumento de taxa de conversão, receita ou ROI.
