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

### Preparação simplificada — decisão de 20/09/2026

O formulário padrão do produto solicita somente **período de suporte em dias**, com
sugestão editável, e **geração personalizada com IA**, inicialmente **Sim**. Para Quartzo,
a sugestão inicial é sete dias; para outros tipos, trinta dias. São propostas operacionais,
nunca alteração automática da oferta ou do acesso já vendido. Escolhas já registradas
prevalecem sobre os valores iniciais.

O backend identifica produto, versão, plano comercial e revisão, reaproveita as premissas
da revisão correspondente e registra as duas escolhas em uma nova revisão imutável.
Preço e CAC podem vir do cadastro/plano comercial com origem explícita. Custos ausentes
continuam desconhecidos: custos realizados agregados não substituem uma projeção. O campo
planejado `variableCostPerSaleBrl` da mesma versão comercial pode ser preservado como envelope
de todos os custos variáveis, exceto CAC, sem declarar seus componentes como zero. Nesse modo,
taxas, reembolso, suporte, entrega e IA não são deduzidos novamente; Plutus deve confirmar a
cobertura antes de aprovar. Escolher IA não confirma provedor, tarifa nem consumo. Escolher sem
IA declara somente ausência de geração personalizada, preservando os custos iniciais de produção.

Suporte e período da projeção são conceitos separados. A sugestão de suporte não modifica
silenciosamente custos, prazo de acesso ou período econômico já cadastrado. Quando ainda
não há período econômico, a preparação propõe trinta dias, com a premissa registrada.
Os detalhes financeiros e a edição completa ficam em uma opção avançada, sem exigir seu
preenchimento na preparação simplificada. As lacunas e gates continuam sendo verdade do
backend; salvar as duas escolhas não autoriza gasto, publicação nem aprovação financeira.

Antes de salvar, o backend reconfere revisão e versão comercial. Concorrência exige
recarregar; produtos, ambientes e versões não compartilham premissas automaticamente.

Quando preço, CAC, envelope variável, custo fixo, fonte e validade são suficientes, mas margem
mínima e cenários ainda dependem do parecer, a revisão fica `READY_FOR_ANALYSIS`. Esse estado só
libera Plutus: não significa `PROJECTED_VIABLE`. O parecer deve registrar decisão e cobertura;
somente `APPROVE` com cobertura agregada ou detalhada completa pode atender ao gate econômico.
`ADJUST`, `BLOCKED`, contribuição não positiva ou fonte essencial ausente preservam o bloqueio.

### Edição avançada e modelos por tipo

1. Abra **Plano financeiro** no menu ou no card do produto/tipo. No produto, a opção
   **Edição financeira avançada** mantém o formulário completo para revisar fontes e custos.
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
O contrato de projeção v2 exige `decision`, justificativa e avaliação de cobertura. Envelope
agregado completo é uma base auditável, não uma decomposição; custos ausentes fora de sua
cobertura continuam bloqueantes.

Preservar os pontos canônicos já existentes: oferta (`economics`), desenho da entrega,
homologação e operação. O plano complementa a ficha e seus gates; não substitui reservas
atômicas, aprovação humana, autorização de mídia ou publicação. Não reversionar cadeias em
curso nem dar tarefas como concluídas por criar um plano. Antes da adoção operacional, conferir
os limites na ficha exata. A v1 não cria uma trava universal de vendas em todos os executores.

### Adoção no subprocesso Opala

Decisão de 17/09/2026: a atividade econômica do subprocesso
`opala-commercial-preparation-v1` só pode reservar Plutus depois que o backend localizar uma
revisão `LIVE` do mesmo produto, plano comercial e versão do ciclo. A revisão precisa estar
vigente e classificada como `PROJECTED_VIABLE`; preço, fontes e os cenários determinísticos
seguem no contexto auditável. Janela vencida bloqueia antes da inferência.

Plutus revisa a recomendação, mas não recria os números: o parecer copia preço, CAC máximo,
provisão de reembolso e contribuição do cenário-base calculado pelo backend. Divergência no
callback é recusada, preservando a resposta bruta e o custo. Revisão nova ou alteração material
invalida somente o parecer econômico afetado. O histórico anterior, inclusive parecer bloqueado,
nunca é sobrescrito. Essa integração é específica do Opala; outros percursos adotam o plano por
seus próprios contratos e gates.

Fontes primárias: [agente financeiro](financial-agent-canon.v1.md),
[tipos de produto](product-types-canon.v1.md) e
[fichas e checkpoints](product-execution-profiles-canon.v1.md).
