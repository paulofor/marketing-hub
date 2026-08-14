# Piloto de replay em modo sombra de Apolo

## Objetivo

Comparar o planejador atual e uma versão candidata usando execuções congeladas, sem chamar OpenAI, Runway, Luma ou outro provider e sem autorizar gasto ou publicação.

## Decisão arquitetural

O executor calcula uma comparação pura e reutiliza o mesmo gate determinístico do fluxo real. O relatório identifica explicitamente `providerCalled=false` e `spendingAuthorized=false`. Uma candidata aprovada em replay pode avançar apenas para sombra online; nunca para geração paga automática.

Em 2026-08-14, Apolo foi integrado ao ambiente governado compartilhado do backend. Os conjuntos de
replay e holdout, versões, métricas e decisão passam a ser persistidos pelo mesmo contrato usado
pelos demais agentes homologados. O backend rejeita a avaliação se houver chamada a provider,
autorização de gasto ou publicação.

Ainda em 2026-08-14, o replay foi ligado ao fluxo real de storyboard. Cada job registra no backend
uma observação idempotente com baseline, candidata, nota, custo previsto e resposta bruta. O backend,
e não Apolo, aplica o QA determinístico. Ao acumular uma amostra homogênea de 10 casos de replay e
5 de holdout, cria automaticamente uma memória `CANDIDATE`, congela o experimento e calcula a decisão.
A confirmação da memória continua exigindo promoção explícita posterior; o replay não muda o job,
não chama provider, não autoriza gasto e não publica. O Estúdio expõe baseline, candidata, pontuação,
custo, memória, QA e decisão sem recomputar os dados no frontend. A ferramenta MCP
`apollo_learning_experiments` oferece a mesma leitura auditável para diagnóstico operacional.

Em 2026-08-14, o piloto passou a materializar também uma `SkillCandidate` de roteiro e storyboard.
A candidata preserva procedência das 15 trajetórias, diff, versão e decisão de segurança. Ela somente
pode ser promovida explicitamente depois do replay/holdout, entra em janela monitorada de cinco casos
e sofre rollback diante de incidente, custo fora do teto ou taxa de aprovação inferior a 60%. A
baseline permanece disponível e a skill não recebe autoridade para provider, gasto ou publicação.

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
