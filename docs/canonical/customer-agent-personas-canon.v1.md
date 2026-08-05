# Biblioteca de Personas e Agente Cliente v1

## Objetivo

A Biblioteca de Personas mantém hipóteses versionadas e rastreáveis sobre clientes. O Agente
Cliente usa essas hipóteses para revisar ofertas, vídeos e páginas antes de testes humanos.

## Fonte e confiança

Toda persona exige evidências explícitas. Novas personas começam como `HIPOTESE`; somente dados
humanos oficiais podem elevar sua confiança. Texto persuasivo produzido por IA, avaliações
simuladas e estimativas nunca constituem validação.

## Separação obrigatória

`simulated_assessment` registra a previsão do agente, `hypothesis_json` registra o que deve ser
testado e `human_result_json` recebe somente resultados posteriores de sessões, vendas, feedbacks
ou outras fontes humanas oficiais. Esses campos nunca devem ser fundidos ou preenchidos um pelo
outro.

## Autoridade

O Agente Cliente opera em sandbox somente leitura. Ele pode retornar `APROVAR_TESTE`, `AJUSTAR` ou
`REPROVAR`, mas não altera ativos, preços, campanhas, publicações, personas ou resultados humanos.

## Métrica de qualidade

A maturidade é medida pela correspondência posterior entre objeções previstas e comportamento
humano observado, nunca pela quantidade ou eloquência dos relatórios.
