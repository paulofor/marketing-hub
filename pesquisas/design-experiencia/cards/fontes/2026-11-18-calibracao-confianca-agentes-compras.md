# Fonte revisada — confiança declarada e acurácia de agentes de compra

**Referência:** Adi Krishnamoorthy e X.Y. Han, *Thinking Harder Doesn't Lead to Better Choices: Reasoning Effort Increases the Confidence but Not the Correctness of LLM Shopping Agents*. arXiv:2611.22033v1, submetido em **17/11/2026**. https://arxiv.org/html/2611.22033v1

## Evidência encontrada

O preprint compara **dez modelos** em três benchmarks de seleção de produtos (WebShop, Amazon SQID e Wayfair WANDS). Cada questão apresenta **dez produtos candidatos**, um rotulado como correto; a busca e a navegação são removidas para isolar a seleção a partir de descrições e imagens disponíveis. São 496 tarefas WebShop, 500 SQID e 330 WANDS. As configurações de esforço de raciocínio são comparadas dentro das mesmas tarefas.

Para a maior parte dos pares modelo–benchmark, elevar o esforço de raciocínio **não produz ganho estatisticamente significativo de acerto**. Em contraste, eleva significativamente a **confiança verbalizada** em nove de dez modelos em WebShop, cinco de dez em SQID e seis de dez em WANDS (comparações low→high, segundo os autores). Há exceções pontuais de melhoria de acurácia, e nem todos os aumentos de confiança sobrevivem às correções. A calibração varia por modelo. No WebShop, em esforço baixo, **47 de 493 tarefas** foram erradas por todos os dez modelos; a concentração de erros foi muito maior que a prevista sob uma hipótese de independência. Produtos errados frequentemente deixam de satisfazer um requisito secundário.

**Resultado realmente observado:** qualidade das escolhas, confiança autorrelatada pelos modelos e correlação entre erros **em benchmarks controlados**. Não foi medida compra real por consumidores, nem checkout, receita, satisfação ou redução de prejuízo.

## Hipótese interpretativa

O aumento de deliberação pode elevar a sensação verbalizada de certeza sem reparar erros de leitura de atributos como tamanho, compatibilidade, preço e versão. Essa é uma interpretação plausível dos achados, **não um mecanismo psicológico humano comprovado**. Concordância entre vários modelos também não garante independência das verificações.

## Aplicação possível

Em agente de recomendação e comparação B2C, tratar a confiança verbalizada como **sinal não calibrado**. Antes de recomendar ou executar uma compra, confrontar requisitos explícitos do comprador com campos verificáveis do catálogo, apresentar incompatibilidades e pedir autorização para compromissos irreversíveis. Medir correção da correspondência produto–requisito, custo, latência e eventos do funil separadamente.

## Experimento proposto (não realizado)

Comparar: (A) agente com esforço alto e justificativa livre versus (B) agente com esforço convencional e checklist determinístico de restrições + fontes da ficha do produto. Manter catálogo e consultas idênticos, cegar os revisores e medir escolhas corretas, restrições violadas, falsa confiança, custo e latência. Em piloto real, medir correções, cancelamentos, checkout e pagamentos reconciliados, com autorização humana.

## Riscos e limites

Preprint sem evidência de revisão por pares; tarefas com dez alternativas pré-selecionadas e uma resposta rotulada como correta, diferentemente de lojas reais; rótulos de SQID/WANDS incluem julgamento humano e possíveis ambiguidades; resultados dependem de modelos e versões avaliadas; não se deve generalizar que raciocínio adicional nunca ajuda. Não apresentar percentuais de benchmark como taxas de venda, nem recomendar automatizar compras sem controle do consumidor.
