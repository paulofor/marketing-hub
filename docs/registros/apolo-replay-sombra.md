# Piloto de replay em modo sombra de Apolo

## Objetivo

Comparar o planejador atual e uma versão candidata usando execuções congeladas, sem chamar OpenAI, Runway, Luma ou outro provider e sem autorizar gasto ou publicação.

## Decisão arquitetural

O executor calcula uma comparação pura e reutiliza o mesmo gate determinístico do fluxo real. O relatório identifica explicitamente `providerCalled=false` e `spendingAuthorized=false`. Uma candidata aprovada em replay pode avançar apenas para sombra online; nunca para geração paga automática.

## Matriz de homologação

| Cenário | Resultado esperado |
|---|---|
| Histórico com repetição e texto embutido | Pontuação inferior a 70 |
| Candidata diversa, completa e no teto | Elegível somente para sombra online |
| Candidata acima do orçamento | Bloqueada antes do provider |
| Integrações externas | Nenhuma chamada |
| Observabilidade | Pontuação, gate, créditos, custo e decisão no relatório |
| Segregação | Entrada congelada entregue explicitamente por execução |

## Critério comercial

Continuar quando a candidata alcançar pelo menos 70 pontos, superar a versão atual e não elevar créditos. Ajustar quando melhorar qualidade com piora de custo. Parar diante de violação de orçamento, tentativa de provider, gasto ou publicação.
