# Monitoramento de preços de modelos por Plutus — v1

## Decisão

Plutus é responsável por pesquisar e manter evidências de preços de modelos de IA por plataforma de acesso. Fabricante, modelo e provedor de acesso são dimensões diferentes: o mesmo modelo pode possuir preço, unidade, resolução, áudio, plano e taxa distintos conforme a plataforma.

## Contrato comercial

- preço verificado exige URL oficial, data da observação, moeda, valor, unidade, quantidade coberta, modalidade, resolução e áudio quando aplicáveis;
- a comparação deve usar custo normalizado somente quando as unidades forem equivalentes;
- evidência com mais de 30 dias é vencida e não pode fundamentar recomendação financeira;
- ausência de conversão oficial entre créditos e USD torna a oferta não comparável;
- menor preço não substitui gates de qualidade, licença, confiabilidade e desempenho comercial;
- pesquisa e recomendação não autorizam compra, consumo, geração, troca de provider ou publicação;
- o backend persiste e expõe a verdade; a rotina periódica pertence ao Financial Agent Worker.

## Resultado esperado

O Estúdio deve mostrar preço original, custo normalizado por segundo quando possível, vigência, fonte oficial e bloqueios. Plutus usa esse catálogo para estimar e supervisionar orçamento antes de Apolo consumir providers.
