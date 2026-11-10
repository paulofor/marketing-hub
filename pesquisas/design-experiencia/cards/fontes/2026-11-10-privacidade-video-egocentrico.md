# Fonte revisada — privacidade individual em vídeo egocêntrico

Revisão: 2026-11-10

## Referência
Wang, Xue; Zhang, Yang. **EgoVeil: Personalized Privacy Reasoning for Egocentric Video**. Preprint divulgado em novembro de 2026 (registro de 07/11/2026); pesquisa incluída na listagem cs.HC consultada em 10/11.
https://arxiv.org/abs/2611.10578

## Evidência encontrada
O sistema EgoVeil transforma regiões de vídeo egocêntrico em observações semânticas estruturadas, usa um raciocinador de privacidade e uma memória explícita de preferências de cada usuário para decidir o que ocultar. Avaliado em dados de uso natural de 10 pessoas com óculos inteligentes, em protocolo cronológico entre usuários. Resultado reportado: balanced accuracy de 91,93% e Mask recall de 84,42%, superiores aos baselines avaliados. Os números medem previsão/recuperação de regiões a mascarar segundo o protocolo do artigo, não eliminação universal de risco de identificação.

## Hipótese interpretativa
Uma regra geral de ocultação pode falhar porque a sensibilidade de uma cena varia com a pessoa e o contexto. Preferências explícitas e correção recorrente podem tornar a seleção de elementos sensíveis mais aderente; isso não prova proteção completa da privacidade.

## Aplicação possível
No Marketing Hub, aplicar o conceito à revisão de vídeos UGC/primeira pessoa, quando houver autorização de captação: checklist e controle explícito sobre rostos, telas, endereços e terceiros; comparação de mascaramento genérico versus personalizado com inspeção humana. Manter minimização de dados e rastreabilidade de autorizações.

## Resultado real observado
Desempenho técnico de identificação das áreas de máscara no conjunto de 10 usuários. Nenhum teste de criativos publicitários, conversão, consentimento real de terceiros ou segurança de publicação foi demonstrado.

## Riscos e limites
Dados sensíveis podem ser inferidos mesmo após desfoque; falsos negativos deixam informação visível; memória de privacidade pode ela própria expor dados. Amostra pequena e contexto de smart glasses, não de vídeo publicitário convencional. Exigir autorização, auditoria humana e verificação antes de publicar; não prometer anonimização garantida.
