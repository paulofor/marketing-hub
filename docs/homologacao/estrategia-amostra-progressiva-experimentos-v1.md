# Estratégia de amostra progressiva de experimentos — matriz de homologação v1

## Objetivo

Validar que o cockpit transforma `sampleSize`, `targetCvr`, visitantes humanos distintos, compras
líquidas atribuídas e teto financeiro em uma decisão progressiva, sem concluir rejeição ou escala
por volume precoce.

## Alternativas consideradas

| Alternativa | Benefício | Risco | Esforço | Aderência ao objetivo |
| --- | --- | --- | --- | --- |
| Manter amostra apenas como configuração/texto | Mudança mínima | O cockpit continua recomendando alterações com poucos acessos | Baixo | Baixa |
| Automatizar parada e escala em 100/500 | Execução rígida | Pode ampliar gasto ou encerrar oportunidade sem autorização e sem margem/entrega | Alto | Média |
| Decisão progressiva calculada pelo backend | Separa evidência, precisão e caixa; mantém a tela como projeção da verdade do backend | Exige contrato, testes e interface | Médio | Alta |

A terceira alternativa foi escolhida porque corrige a causa da decisão prematura sem ampliar gasto,
alterar campanha ou transformar estimativa em autorização.

## Matriz ponta a ponta

| Dimensão | Cenário | Aceite |
| --- | --- | --- |
| Caminho feliz inicial | 4 visitantes humanos, zero compra líquida, `sampleSize=100`, `targetCvr=5` | `INSUFFICIENT_DATA`, 96 visitantes restantes, meta de 5 compras e alvo de precisão de 500 |
| Caminho feliz positivo | 100 visitantes e 5 compras líquidas | sinal inicial; recomenda preservar versão e validar margem/entrega antes da rodada de precisão |
| Caminho feliz de precisão | 500 visitantes e 25 compras líquidas | leitura de precisão pronta; escala permanece condicionada a contribuição, entrega, satisfação e autorização |
| Validação negativa | 100 visitantes e zero compra | limite superior unilateral de 95% abaixo de 5% e recomendação de revisar a versão, sem reprovar o produto inteiro |
| Validação intermediária | 100 visitantes e 1–4 compras | meta inicial não atingida, mas intervalo ainda explícito; nenhuma escala automática |
| Exatidão estatística | 5/100 e 25/500 compras líquidas | intervalo bilateral de Clopper-Pearson, sem substituir por aproximação normal |
| Integração PDE | visitantes humanos distintos, versão, códigos Meta e compra financeira | exclui sessões repetidas, tráfego não humano, acessos sem atribuição, outras versões e vendas reembolsadas |
| Escopo seguro | experimento de vendas sem coorte financeira PDE conciliada | mantém o contrato próprio e não aplica a estratégia até existir numerador líquido confiável |
| Observabilidade | mensuração PDE indisponível ou compras maiores que visitantes | cockpit bloqueia conclusão e orienta corrigir mensuração |
| Proteção financeira | projeção da amostra excede `mediaSpendLimit` | informa incompatibilidade; não altera orçamento nem status da campanha |
| Estimativa precoce | menos de 20 visitantes | custo projetado identificado como preliminar |
| Segregação | experimento fake ou abordagem direta | não aplica o plano de visitantes pagos; preserva contratos próprios |
| Integração com o ciclo | 100 sessões de apenas 40 visitantes humanos distintos | mantém a escala bloqueada até 100 visitantes; sessões repetidas ficam apenas na auditoria |
| Regressão comercial | quatro visitantes sem compra | gargalo principal deixa de afirmar que a página não converteu |
| Desktop | Chromium em viewport desktop | card, progresso, intervalo e teto legíveis sem overflow |
| Mobile iOS | Chromium emulado como iPhone 15 Pro | conteúdo empilha, métricas permanecem legíveis e ações acessíveis |
| Mobile Android | Chromium emulado como Pixel 7 | conteúdo empilha, métricas permanecem legíveis e ações acessíveis |

## Limites

- A funcionalidade não aumenta orçamento, não pausa campanha e não publica mídia; apenas expõe a
  parada automática canônica de R$ 25 e o teto já aplicados pelo backend.
- A projeção usa o custo observado por visitante distinto e pode mudar com a aquisição.
- Pesquisa qualitativa com cinco pessoas continua sendo evidência complementar; sessão ou evento
  técnico não deve ser convertido em observação humana sem registro próprio.
- O ciclo de aprendizagem recebe `humanVisitors` no contrato automático v2 e não pode liberar
  escala usando sessões repetidas como substituto da amostra humana distinta.
