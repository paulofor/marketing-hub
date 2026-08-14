# Matriz de homologação — aprendizado governado de agentes v1

## Objetivo

Comprovar que uma hipótese só vira estratégia operacional após superar o baseline fora da amostra,
sem regressão, vazamento entre experimentos ou aumento de custo acima do limite.

| Dimensão | Cenário e critério de aprovação |
|---|---|
| Caminho feliz | congelar candidata, executar baseline/candidata, obter ganho no holdout, marcar `READY_FOR_PROMOTION` e promover explicitamente |
| Validações | rejeitar agente desconhecido, conjuntos vazios, menos de 10 replays ou menos de 5 holdouts |
| Falhas | rejeitar regressão, validação local incompleta, custo excessivo e segunda avaliação |
| Integrações | confirmar memória somente na promoção e expor aos MCPs apenas registros `PROMOTED` |
| Observabilidade | preservar versões, conjuntos congelados, resultados, ganho, custo, decisão e horários |
| Métricas | medir ganho de score/aprovação, reincidência, custo e latência; vendas somente por evento real |
| Segregação | impedir leitura de estratégia de outro agente, experimento ou tenant |
| Replay | usar falhas históricas e sucessos para prevenir regressão; holdout não participa da criação da candidata |
| Autoridade | workers não avaliam, promovem, alteram código, publicam ou gastam |
| Apolo | comparar storyboards congelados sem OpenAI, provider de vídeo, gasto ou publicação |
| Efeitos externos | rejeitar a avaliação se ela relatar provider, autorização de gasto ou publicação |
| Dispositivos | replays de Dédalo incluem desktop, iPhone e Android; Têmis inclui placements e landing mobile/desktop |

Uma rodada local integral sem defeito conclui a homologação. Se surgir defeito, após a última correção
serão exigidas duas rodadas completas e consecutivas sem falhas.
