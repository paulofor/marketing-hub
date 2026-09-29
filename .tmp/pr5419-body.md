## Resumo

- aceita `READY_FOR_ANALYSIS` como base financeira agregada no gate Safira
- preserva a exigência de parecer Plutus concluído, aprovado, com cobertura completa e cenário-base positivo
- registra a causa-raiz e a evidência operacional do Mira no histórico de loops e na matriz de homologação

## Causa-raiz

O preparador financeiro agregado classifica corretamente a revisão como `READY_FOR_ANALYSIS`; o gate Safira aceitava apenas o estado detalhado `PROJECTED_VIABLE`, mesmo depois de Plutus concluir `APPROVE` com cobertura `COMPLETE_AGGREGATE`.

## Validação local

- `mvn -Dtest=SafiraCommercialChecksTest test` — 6 testes, zero falhas
- `mvn -Dtest='com.marketinghub.safira.commercial.v1.service.*Test' test` — 18 testes, zero falhas
- `mvn spotless:check` — sucesso
- `mvn test` — 3.682 testes, zero falhas/erros, 23 skips previstos

## Limites preservados

- nenhuma campanha, autorização de mídia ou gasto é criado por esta mudança
- `READY_FOR_ANALYSIS` sem parecer concluído continua bloqueado por regressão
