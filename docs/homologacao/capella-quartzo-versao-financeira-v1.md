# Capella — versão financeira no percurso Quartzo

Em 20/10/2026, o anúncio #540 foi aprovado por Têmis após comparação das fontes reais.
A seleção foi registrada pela tela; o Processo 5 #71 abriu automaticamente Quartzo #72.
Página, anúncio e checkout tiveram suas conclusões persistidas. A projeção das revisões
comerciais, entretanto, dizia faltar plano financeiro LIVE.

## Evidência e causa

O banco confirmou contrato do produto #7 em `v1`, implementação do ciclo #12 em
`capella-private-v3` e revisão financeira #22 vinculada ao plano comercial #2 v7 em `v1`.
O parecer de Plutus #88 já existe. O histórico #88/ciclo #4 funcionava porque ciclo e
contrato coincidiam em `v1`; ciclos posteriores passaram a nomear a implementação.

O seletor comparava a revisão financeira à versão executável, enquanto o serviço
financeiro a grava e valida contra `Product.validationDefinitionVersion`. Não há
evidência de ausência de cadastro ou de mudança da economia por essa diferença nominal.

## Alternativas comparadas

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Renomear/recriar a revisão com o nome da implementação | Aparente correspondência local | O serviço financeiro a marca desatualizada; perde o parecer e provoca repetição paga | Rejeitada |
| Aceitar qualquer revisão do produto | Implementação curta | Mistura contratos e planos diferentes, enfraquecendo o gate | Rejeitada |
| Selecionar pelo contrato canônico e preservar a implementação | Reutiliza a revisão correta sem nova inferência | Ajuste pequeno com regressão da fronteira entre contratos | Adotada |

## Matriz local e aceite

Antes da correção, seis casos executados: quatro falharam por seleção financeira
incorreta; os dois caminhos com nomes coincidentes/sem ciclo passaram. Após o ajuste,
os seis passaram, usando o contexto e o gate econômico reais, com portas de persistência
substituídas por test doubles.

- Caso original: implementação `capella-private-v3`, contrato `v1`, mesma revisão e
  parecer concluído; o gate econômico aceita a descoberta limitada já autorizada.
- Outra identidade: produto/experimento/ciclo/plano distintos e nomes de versões
  diferentes, sem regra por nome ou ID.
- Caminhos antes válidos: versões coincidentes e referência sem ciclo.
- Negativos: outro contrato, outro plano e contrato canônico ausente não são aceitos.
- Leitura não solicita análise, altera revisão, muda preço ou autoriza mídia.
- A versão executável permanece no snapshot. Fingerprints por atividade preservam
  página, anúncio e checkout quando somente o vínculo econômico muda.

A suíte relacionada de Quartzo, prontidão, automação e arquitetura executou 499 testes:
496 aprovados, sem falhas ou erros; três migrações antigas permanecem condicionadas ao
runner MySQL específico, sem alteração de changelog neste lote. O MySQL 5.7.44 local
validou a consulta e a disputa descritas abaixo. Spotless e revisão do diff passaram.
A primeira passagem do CI exigiu o nome completo da classe no banco H2 da nova fixture;
o identificador foi ajustado. Os quatro casos da fixture, os 28 contratos Python do CI,
os 16 testes do bundle e a integridade dos 123 manifestos/540 arquivos passaram localmente.
A conferência publicada deve localizar a revisão #22 sem reabrir Plutus e manter as provas
já gravadas. Pareceres comerciais continuam independentes; publicação do código não os aprova.

## Conciliação do pai e do filho

A instância de público #706 também ficou concluída, mas o controle #72 entrou em espera
técnica. O log completo confirmou SQL 1205 no `SELECT ... product ... FOR UPDATE`;
não era ausência de público. O pending anterior entregava #71 e #72 no mesmo lote ao
executor paralelo. A consulta de locks posterior estava vazia; não se identifica por
inferência o dono exato da reserva naquela ocorrência.

| Alternativa | Benefício | Custo/limite | Decisão |
| --- | --- | --- | --- |
| Executor global com concorrência um | Evita disputa entre conciliações do lote | Atrasa todos os produtos por uma execução demorada | Preservar paralelismo |
| Contrato novo com chave de produto e agrupamento no executor | Mantém paralelismo entre produtos | Amplia API/worker e ainda exige proteção contra outros consumidores | Não necessário |
| Pending existente com uma vez por produto e tratamento transitório | Conserva contrato, locks e responsabilidade do backend | Exige testar justiça e retomada sem mudar a pausa | Adotada |

O pending seleciona a execução menos observada de cada produto; depois da conciliação,
pai e filho alternam a vez. A classificação de erro de lock preserva o estado já persistido,
sem outra reserva do produto, alteração de custo, nova inferência ou falsa decisão humana.
O próximo polling continua no executor existente. Erros não transitórios mantêm o evento e
a política de falha. Pausa concorrente prevalece; não se apagam os eventos históricos.

A regressão anterior teve duas falhas de seleção e três erros de lock em oito testes;
os oito passaram após a correção. No MySQL 5.7.44 isolado, a consulta extraída do repository
retornou pai + outro produto; após observação do pai, outro produto + filho, em duas
identidades. Dois clientes reproduziram 1205 e a reserva funcionou depois da liberação.
Os testes JPA usam transações independentes para verificar a leitura posterior ao rollback,
inclusive uma pausa gravada durante a disputa. Nenhum teste acessa o ambiente produtivo
ou executa compra real.

## Limites operacionais

Os tetos permanecem US$ 15 de preparação, US$ 10 de audiovisual e R$ 100 de mídia por cinco
dias após homologação, com 30% para continuidade/escala e descoberta limitada já aceita.
Custos Codex são estimados, sem exigir relatório de faturamento da API. O token expirado
da Meta continua impedindo ativação. Testes e preparo não comprovam compras ou lucro.
