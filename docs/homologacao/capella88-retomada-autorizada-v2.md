# Matriz de homologação — retomada autorizada Capella #88 v2

> Registro histórico da entrega de 22/09/2026. Em 23/09/2026, a Meta informou mínimo de
> `spend_cap` de campanha de R$ 300 e recusou o teto autorizado de R$ 125. A campanha permaneceu
> pausada. A causa, o fallback nativo no único conjunto e os testes preventivos estão na
> [matriz vigente](capella88-preflight-processo58-v1.md); este arquivo preserva a evidência anterior.

Data: 22/09/2026. Escopo: produto Capella #7, experimento #88, processo #82 v8 e
campanha Meta existente. Os dados produtivos só serão registrados pela tela depois do merge e
deploy; os casos locais usam identificadores e servidores simulados.

| Dimensão | Caso | Aceite |
| --- | --- | --- |
| Caminho feliz | R$ 20/dia, teto acumulado R$ 125, 23–29/09, R$ 50 sem compra e cinco compras | autorização única `PENDING`; processo reutiliza a campanha; worker mantém orçamento diário, aplica teto e confirma readback |
| Validação | teto menor que `diária × dias` | plano aceito porque o teto absoluto interrompe antes da capacidade máxima |
| Validação | teto ou parada sem compra não superam o gasto atual; datas invertidas; consentimento ausente | `400`, sem pedido, alteração Meta ou gasto |
| Falha externa | Insights vazio, destino inválido, orçamento/readback divergente ou Meta rejeita | campanha permanece/volta `PAUSED`; callback registra falha e evidência |
| Integração | orçamento diário e vitalício | cada modo permanece inalterado; campanha diária usa `spend_cap` e ad set usa `daily_budget` |
| Agenda | início futuro | endpoint `pending` não entrega antes da data em `America/Sao_Paulo`; nenhum gasto antecipado |
| Concorrência | repetição idêntica, pedido diferente concorrente e lease abandonada | idempotência, conflito no pedido divergente e recuperação exclusiva após expiração |
| Paradas | lead sem compra aos R$ 50; cinco compras; teto R$ 125 | primeira condição aplicável pede pausa; cinco compras concluem como sucesso; teto/zero compra como parada comercial |
| Observabilidade | request, response, URL, IDs, gasto, modo e limites | logs correlacionados sem token; backend persiste histórico e evidência estruturada |
| Métricas | receita, compras, gasto e margem | compra atribuída vem do funil; gasto vem da Meta; projeção nunca é registrada como venda |
| Isolamento | outro experimento sem autorização | política padrão e campos próprios preservados; nenhum ID de Capella codificado no contrato |
| Interface | desktop, iPhone 15 Pro e Pixel 7 | campos, consentimentos, erro e estado pendente legíveis e operáveis |

Critério final: todos os testes locais relevantes, Liquibase MySQL 5.7, PR, workflows e deploys
devem passar; na produção, a Meta não pode ativar antes de 23/09 e só pode ativar depois do
readback dos limites autorizados.

## Resultado local

- Backend: 3.409 testes aprovados, zero falha/erro e 21 cenários condicionais dispensados.
- Facebook Ads Worker: 137 testes aprovados, sem falha, incluindo orçamento diário e vitalício,
  compensação, readback divergente e gasto ausente.
- Frontend: 741 testes aprovados; `typecheck`, build produtivo e Prettier aprovados.
- Liquibase: sete changesets novos aplicados duas vezes no MySQL 5.7, seguidos de validação JPA,
  persistência e concorrência; segunda aplicação permaneceu idempotente.
- Interface segregada: desktop, iPhone 15 Pro e Pixel 7 aprovaram o payload autorizado, estado
  pendente/concluído/falha, gasto anterior preservado e ausência de overflow horizontal.

Esses resultados comprovam o contrato operacional local. Não comprovam ativação na Meta, vendas
ou lucro; esses pontos dependem da entrega publicada e dos eventos reais conciliados.
