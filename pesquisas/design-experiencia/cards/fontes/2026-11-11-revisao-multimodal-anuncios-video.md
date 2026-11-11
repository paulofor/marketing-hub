# Fonte revisada — revisão multimodal de vídeos publicitários gerados por IA

**Referência:** Agarwaal, Perla, Mohanraj e Ghose, *Multimodal Judges Flag, Humans Decide: A Review-and-Edit Framework for AI-Generated Product Video Ads*. arXiv:2611.13178v1, submetido em 09/11/2026. https://arxiv.org/html/2611.13178v1

## Evidência encontrada
Em 120 anúncios de vídeo de aproximadamente 15 segundos gerados pelo próprio pipeline dos autores (562 cenas), a revisão organizada em seis eixos — fidelidade do produto, qualidade da cena, pronúncia, roteiro, som e legendas — apontou produto incorreto em 60% das cenas, segundo a maioria dos revisores. Em 60 anúncios, revisores vendo achados de juízes de IA marcaram problemas de produto em 58% das cenas, ante 26% com os achados ocultos. Essa comparação não foi randomizada nem cega para os juízes expostos, e pode conter ancoragem. Em 120 cenas rotuladas sem acesso ao juiz, o detector de produto obteve AUC 0,84, recall 26/29 e precisão 26/56: há falsos alarmes relevantes.

## Hipótese interpretativa
Apontamentos de IA, com evidência e localização temporal, podem chamar atenção humana para defeitos concretos que passariam despercebidos; também podem influenciar indevidamente o revisor. Não se trata de prova de melhora causal da qualidade final dos anúncios.

## Aplicação possível
No Marketing Hub/Apolo, comparar referências reais do produto com cada cena; revisar separadamente roteiro, alegações, voz, áudio e legendas; exigir aceitação ou rejeição humana de alertas; corrigir somente a cena ou trecho confirmado e revisar novamente.

## Resultados reais observados e limites
Houve maior frequência de marcações de defeitos com os alertas visíveis, não medição de maior conversão, vendas ou retenção. Amostra originada de um único pipeline de e-commerce, revisores internos, variação de atenção e risco de viés por sugestões do detector. A precisão de 26/56 impede aprovação/rejeição automática baseada nesse juiz.
