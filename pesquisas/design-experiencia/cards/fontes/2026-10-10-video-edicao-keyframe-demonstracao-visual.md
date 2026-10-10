# Fonte revisada — edição de vídeo com keyframe visual como referência

## Evidência encontrada

Qu et al., *VINCIE-NExT: Unlocking Video Editing from Images via In-Context Modeling* (arXiv:2610.12104, submetido em 08/10/2026; aceito no NeurIPS 2026): o método decompõe edição de vídeo em vídeo → imagem original → imagem editada → vídeo e usa o quadro editado como âncora visual para propagação temporal. O benchmark OpenVE-Bench inclui 431 clipes. Em comparação cega entre a configuração com treinamento completo e uma versão reduzida (ambas com Chain-of-Editing), dez avaliadores humanos julgaram 431 pares: aderência à instrução favoreceu a configuração completa em 38% dos casos versus 24% para a reduzida, com 39% de empates (por arredondamento, soma 101%). Para consistência, 28% versus 9%, com 63% de empates; para qualidade visual, 15% versus 14%, com 71% de empates. Os resultados automáticos usam modelos avaliadores e precisam ser distinguidos dessa comparação humana.

Fonte primária: https://arxiv.org/abs/2610.12104 e https://arxiv.org/html/2610.12104v1

## Hipótese interpretativa

Uma referência visual explícita pode reduzir ambiguidade sobre a aparência desejada e facilitar a consistência entre quadros em edições de clipes curtos. O estudo não isola o efeito de exigir aprovação humana do quadro antes da propagação.

## Aplicação possível

No fluxo do Apolo, testar vídeo-fonte → keyframe editado revisável → propagação → checagem de continuidade e preservação de regiões não editadas, em comparação com edição de vídeo direta sob o mesmo briefing, orçamento e ferramenta. Registrar aprovações e efeitos visuais como artefatos auditáveis.

## Resultado real observado e não observado

Observado: desempenho em benchmark de edição e preferência humana entre duas configurações do método, sobretudo em aderência à instrução. Não observado: melhor conversão, economia de custo real de produção do Marketing Hub ou superioridade em vídeos longos/cortes rápidos. Risco: erro no keyframe se propaga; latência e custos adicionais; potencial de manipulação enganosa de imagens de pessoas.
