# Plano financeiro de produto — v1

## Decisão de 15/09/2026

O Marketing Hub passa a oferecer **plano financeiro por produto e versão**, com **modelos
reutilizáveis por tipo de produto**. O mineral não determina preço, consumo ou lucro.

| Alternativa | Benefício | Risco | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Plano somente por tipo | Reúso simples | Oculta diferenças de oferta e consumo entre produtos | Baixo | Rejeitada |
| Plano somente por produto | Economia individual | Repete premissas comuns | Médio | Insuficiente |
| Modelo por tipo e plano por produto/versão | Reúso e responsabilidade individual | Exige adoção explícita | Médio | Adotada |

Modelos são referências, não aprovação financeira. Copiar um modelo congela sua revisão;
revisões posteriores não alteram produtos, contratos vendidos ou execuções em curso.
Planos de produto congelam a versão do plano comercial. Mudança comercial ou vencimento
exige nova revisão. Cada gravação preserva o histórico e registra responsável e premissas.
Dados de homologação usam escopo `TEST`, separado de `LIVE`, sem resultados de vendas.

## Operação pela tela

1. Abra **Plano financeiro** no menu ou no card do produto/tipo.
2. Cadastre um modelo do tipo quando houver premissas reutilizáveis. No produto, selecione
   **Usar modelo** ou crie um plano próprio; confirme versão, plano comercial e período.
3. Informe fontes, preço, custos e três cenários. Campo numérico vazio permanece pendente;
   o backend calcula também o consumo máximo contratado e explica o que precisa ser revisto.
4. Solicite o parecer de Plutus na revisão do produto. Confira premissas, custo da análise
   e limitações; registre a adoção nos checkpoints da ficha de execução correspondente.

Revisar premissas cria histórico, sem sobrescrever a revisão anterior. A atualização de
um modelo de tipo não altera as cópias já adotadas. O ambiente **Homologação** não solicita
análises pagas e nunca comprova venda, margem realizada ou aprovação comercial.

## Integração escolhida

| Caminho | Benefício | Risco/limite | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Ampliar o planejamento EPM mensal | Reutiliza orçamento por nicho | Escopo agregado não representa entrega de um produto/versão | Médio | Preservar seu contrato atual |
| Usar somente a ficha de execução | Já controla consumo e checkpoints | Exige desenho completo da entrega para planejar hipóteses financeiras | Médio | Manter como contrato operacional |
| Plano econômico próprio com fila existente de Plutus | Permite rascunhos, modelos e cenários auditáveis | Exige conferir a adoção na ficha antes da operação | Médio | Adotado |

O novo módulo não substitui o financeiro legado agregado por nicho nem reapresenta seus
valores como resultado confiável por versão. O novo contrato apresenta projeções, suas fontes
e limites; o acompanhamento realizado continua dependendo das conciliações canônicas.

## Economia e fontes

Valores comerciais são em BRL por cliente/pacote no período contratado. IA pode ser orçada
em BRL ou USD; conversão exige câmbio e fonte explícitos. Custo ausente permanece nulo.
Zero é uma declaração explícita que precisa de justificativa nas premissas. Registrar
provedor/modelo, fonte e data da tarifa, unidades incluídas, tentativas e teto por cliente.
Tentativas incluem falhas cobradas e regenerações. O custo por resultado útil divide o custo
do pacote pela quantidade de resultados aproveitáveis prometida, não pelo número de chamadas.
Uma tentativa financeira corresponde à produção de um resultado: somar as chamadas necessárias
e documentar a composição. Em lotes, ratear o custo entre os resultados e contar tentativas
nessa mesma unidade. A quantidade de requests do executor pode ser diferente; conferir a
conversão na ficha operacional. Resolução, qualidade e personalização devem constar nas fontes.

O backend calcula cenários conservador, base e otimista e acrescenta **uso intenso** usando
as premissas conservadoras com todas as tentativas permitidas. A tela apenas apresenta os
resultados. Percentuais de taxas, tributos, comissões e provisão de reembolso incidem sobre
o preço bruto; tarifa fixa incide por pedido. Cada dedução entra uma única vez.

- Receita líquida projetada = preço menos deduções comerciais.
- Contribuição antes de aquisição = receita líquida menos IA e custos variáveis de entrega.
- Contribuição após aquisição = contribuição anterior menos CAC.
- Margem = contribuição após aquisição / receita líquida positiva.
- Resultado operacional do período = contribuição × clientes projetados menos custo fixo.
- Resultado após investimento = resultado operacional menos investimento inicial de IA e demais custos.
- Equilíbrio = clientes necessários para cobrir custo fixo e investimento com contribuição positiva.

O investimento inicial não é amortizado novamente no custo variável. Compra de créditos não
é consumo. O custo da avaliação de Plutus é mostrado separadamente, com cobertura, para evitar
somá-lo automaticamente a premissas que já o incluam. A razão IA/receita exige receita líquida
positiva. Todas essas métricas são **projeções**, nunca vendas ou lucro realizado.

Margem mínima e CAC máximo são premissas explícitas, sem percentual universal. Custo essencial
desconhecido, contribuição não positiva, margem insuficiente, cenário de uso intenso inviável
ou teto incapaz de cobrir as tentativas impedem classificar o plano como viável em projeção.
O plano registra as lacunas e quem deve atuar; não muda preço, cobrança, oferta ou orçamento.

## Plutus e cadeia de valor

Plutus recebe a revisão exata, cenários calculados, premissas e fontes pelo contrato existente
de projeção financeira. Uma solicitação por revisão é reutilizada após concorrência/retomada;
falha ou parecer desfavorável não provoca repetição paga automática. Alterar premissas permite
uma nova revisão, sujeita à mesma avaliação. O backend persiste vínculo, status, relatório e
custo disponível. Um modelo de tipo não transfere parecer ao produto.
O contexto legado de projeção comporta até 64.000 caracteres para preservar fontes e cálculos;
o backend valida esse limite antes da fila, sem truncamento ou remoção silenciosa de evidências.

Preservar os pontos canônicos já existentes: oferta (`economics`), desenho da entrega,
homologação e operação. O plano complementa a ficha e seus gates; não substitui reservas
atômicas, aprovação humana, autorização de mídia ou publicação. Não reversionar cadeias em
curso nem dar tarefas como concluídas por criar um plano. Antes da adoção operacional, conferir
os limites na ficha exata. A v1 não cria uma trava universal de vendas em todos os executores.

Fontes primárias: [agente financeiro](financial-agent-canon.v1.md),
[tipos de produto](product-types-canon.v1.md) e
[fichas e checkpoints](product-execution-profiles-canon.v1.md).
