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
| Lance CBO → ABO | Campanha informa `LOWEST_COST_WITHOUT_CAP`, mas ad set de origem omite `bid_strategy` | Novo ad set recebe a estratégia da campanha, não envia `bid_amount` e a releitura coincide |
| Limites | Gasto anterior conciliado | `gasto anterior + lifetime_budget <= R$ 125`; média restante arredondada para cima `<= R$ 20/dia` |
| Validação | Teto anterior, múltiplos conjuntos, moeda divergente ou gasto ausente | Falha fechada antes da ativação e sem inferir gasto zero |
| Falha Meta | Criação ou mutação retorna erro HTTP | Corpo/código/subcódigo auditáveis; origem e objeto substituto conhecido permanecem pausados |
| Readback | Orçamento, prazo, filiação, criativo ou estado divergente | Callback de sucesso recusado e compensação confirmada |
| Idempotência | Worker reinicia depois de criação parcial | Recupera por nome determinístico e não duplica campanha, conjunto ou anúncio |
| Callback | Resposta HTTP perdida antes ou depois do commit | Consulta `GET /api/facebook-campaign-resumptions/{id}`; preserva ativação apenas se `COMPLETED` |
| Observabilidade | Request/response Meta e callback | Logs incluem requestId, URL, payload seguro, resposta e stack trace; nenhum token é exposto |
| Métricas | Primeiro sync da substituta | Soma retrato congelado da origem ao insight corrente; somente a campanha vigente entra na fila de sync |
| Conciliação pós-ativação | Campanha já opera quando o Processo 5 relê suas atividades | Preparação concluída continua legível; nova mutação comercial permanece bloqueada; execução avança para 5/5 |
| Isolamento | Testes automatizados | IDs, destino e credencial são sintéticos; nenhuma chamada alcança Meta, produção ou analytics reais |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 | Formulário mantém os valores autorizados e exibe resultado persistido sem overflow ou erro de console |

## Evidência local

- Backend: 3.524 testes executados, sem falhas ou erros; 22 casos condicionais não aplicáveis foram ignorados.
- Facebook Ads Worker: 151 testes executados, sem falhas ou erros, cobrindo caminho feliz, estratégia de lance CBO → ABO, retry, divergência, erro da Meta, falha depois da ativação e perda de resposta do callback.
- MySQL 5.7: sete changesets incrementais aplicados; a segunda execução aplicou zero mudanças, sem `TIMESTAMP NOT NULL` e com include relativo explícito.

## Evidência da primeira execução publicada

O pedido de retomada #9 preservou os limites humanos e chegou à criação da hierarquia
substituta. A Meta criou a campanha `120251812602130326`, mas recusou o novo ad set com
HTTP 400, código `100` e subcódigo `2490487`, porque o payload não informou uma estratégia
de lance. O executor confirmou a compensação: campanha original e substituta permaneceram
pausadas, a substituta não recebeu ad set, anúncio ou gasto e o pedido terminou `FAILED`.

A causa foi confirmada na resposta real da Graph API: a campanha CBO de origem expõe
`bid_strategy=LOWEST_COST_WITHOUT_CAP`, enquanto seu ad set não repete o campo. Foram
comparadas três alternativas: inventar um lance numérico, confiar no default que a Meta já
rejeitou ou copiar a estratégia observada no nível da campanha e validá-la no readback. A
terceira preserva o comportamento aprovado sem acrescentar teto ou custo e foi adotada.
Estratégias limitadas continuam bloqueadas quando o `bid_amount` da origem não puder ser
confirmado.

## Evidência publicada

O pedido de retomada #10 terminou `COMPLETED`. A origem `120251282333490326` e a substituta
incompleta do pedido #9, `120251812602130326`, permaneceram pausadas. A campanha vigente
`120251812866290326` foi ativada com o conjunto `120251812866410326`, estratégia
`LOWEST_COST_WITHOUT_CAP`, dois anúncios e os mesmos criativos aprovados. O gasto anterior
conciliado foi R$ 25,26 e o orçamento vitalício restante ficou em R$ 99,74: a soma é exatamente
o teto acumulado de R$ 125,00. A média de R$ 19,95 por dia entre 25 e 29/09 permanece abaixo do
ritmo máximo de R$ 20,00. O primeiro sync registrou exposição real; isso não equivale a compra.

O comentário consolidado do Pull Request deve vincular esses objetos ao SHA publicado, aos
workflows, ao estado final da execução #20 e à verificação em desktop e celular.

Na primeira conciliação posterior à retomada #10, a campanha já ativa fez a projeção da atividade
5.1 reaplicar indevidamente a trava de mutação Quartzo. A execução #20 registrou `ERROR` em 4/5,
apesar de todas as provas anteriores permanecerem válidas. A projeção do roteador passou a resolver
o contexto somente para leitura; o caminho que realmente inicia nova preparação continua aplicando
a trava mutável. Assim, ativar uma campanha não invalida retroativamente sua preparação e também não
abre permissão para alterá-la em operação.

Gates técnicos não comprovam vendas, receita ou lucro. O resultado comercial deve continuar sendo medido por compras líquidas, reembolsos, custo integral e margem.
