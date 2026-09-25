# Capella #88 — retomada com hierarquia substituta v1

Data: 25/09/2026

Escopo: experimento #88, autorização acumulada de R$ 125,00, ritmo máximo de R$ 20,00/dia e término em 29/09/2026.

## Causa confirmada e decisão

A conta Meta exige `min_campaign_group_spend_cap=30000`, acima do teto humano de `12500` centavos. As tentativas auditadas provaram que a Meta rejeita `spend_cap` abaixo do mínimo (`2446307`), orçamento diário com limite vitalício (`1885624`/`1885652`), campos zero (`1885099`/`1885272`) e a conversão da campanha existente de diário para vitalício (`1885630`).

Foram comparadas três alternativas:

1. Elevar o teto para R$ 300: simples, mas viola a autorização e amplia risco financeiro.
2. Ativar sem trava nativa e depender da pausa posterior: baixo esforço, mas admite ultrapassagem entre sincronizações.
3. Preservar a origem pausada e criar uma substituta determinística com orçamento vitalício somente no ad set: maior esforço, porém mantém teto, ritmo, criativos e auditoria.

A alternativa 3 é a adotada. A substituta não representa novo experimento nem nova hipótese comercial; é apenas a continuação física auditada do mesmo #88.

## Matriz local de homologação

| Dimensão | Cenário | Critério de aceite |
|---|---|---|
| Caminho feliz | Origem diária, conta BRL e mínimo acima do teto | Origem pausada; substituta sem CBO; único ad set vitalício limitado ao saldo; anúncios com os mesmos criativos; callback completo |
| Limites | Gasto anterior conciliado | `gasto anterior + lifetime_budget <= R$ 125`; média restante arredondada para cima `<= R$ 20/dia` |
| Validação | Teto anterior, múltiplos conjuntos, moeda divergente ou gasto ausente | Falha fechada antes da ativação e sem inferir gasto zero |
| Falha Meta | Criação ou mutação retorna erro HTTP | Corpo/código/subcódigo auditáveis; origem e objeto substituto conhecido permanecem pausados |
| Readback | Orçamento, prazo, filiação, criativo ou estado divergente | Callback de sucesso recusado e compensação confirmada |
| Idempotência | Worker reinicia depois de criação parcial | Recupera por nome determinístico e não duplica campanha, conjunto ou anúncio |
| Callback | Resposta HTTP perdida antes ou depois do commit | Consulta `GET /api/facebook-campaign-resumptions/{id}`; preserva ativação apenas se `COMPLETED` |
| Observabilidade | Request/response Meta e callback | Logs incluem requestId, URL, payload seguro, resposta e stack trace; nenhum token é exposto |
| Métricas | Primeiro sync da substituta | Soma retrato congelado da origem ao insight corrente; somente a campanha vigente entra na fila de sync |
| Isolamento | Testes automatizados | IDs, destino e credencial são sintéticos; nenhuma chamada alcança Meta, produção ou analytics reais |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 | Formulário mantém os valores autorizados e exibe resultado persistido sem overflow ou erro de console |

## Evidência local

- Backend: 3.521 testes executados, sem falhas ou erros; 22 casos condicionais não aplicáveis foram ignorados.
- Facebook Ads Worker: 149 testes executados, sem falhas ou erros, cobrindo caminho feliz, retry, divergência, erro da Meta, falha depois da ativação e perda de resposta do callback.
- MySQL 5.7: sete changesets incrementais aplicados; a segunda execução aplicou zero mudanças, sem `TIMESTAMP NOT NULL` e com include relativo explícito.

## Evidência publicada

A evidência produtiva deve ser anexada ao comentário consolidado do Pull Request após o merge e a execução autorizada, porque seus identificadores só existem depois da publicação. O registro deve vincular PR e SHA, workflows e deploys, pedido de retomada, campanha e conjunto substitutos, gasto anterior, orçamento vitalício confirmado, estado da execução #20 e da atividade 5.5 e verificação em desktop e celular.

Gates técnicos não comprovam vendas, receita ou lucro. O resultado comercial deve continuar sendo medido por compras líquidas, reembolsos, custo integral e margem.
