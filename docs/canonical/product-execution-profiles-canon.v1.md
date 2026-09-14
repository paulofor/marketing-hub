# Fichas de execução e percursos de produto — v1

Decisão de 14/09/2026: manter uma cadeia comum de seis processos e especializar o trabalho
pela entrega contratada. A prioridade é gerar vendas e receita com valor real e margem.

## Escolha

| Alternativa | Benefício | Risco | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Uma cadeia por mineral | Autonomia | Duplicação de controles e correções | Alto | Não adotada |
| Atividades uniformes | Operação simples | Entregas incompatíveis | Baixo | Preservada só para históricos |
| Cadeia comum e ficha versionada | Reúso com especialização | Exige contratos explícitos | Médio | Adotada |

A ficha descreve versão do produto, resultado comprado, entradas, entregáveis, critérios de
qualidade, capacidade principal, modo de entrega, receita e economia. O mineral é contexto,
nunca uma condição suficiente para escolher uma implementação. Imagens personalizadas podem
servir a Quartzo, Safira ou outro tipo. Experiência guiada não implica webapp obrigatório.

O backend fornece os percursos `PERSONALIZED_IMAGES`, `GUIDED_EXPERIENCE`, `AI_TOOL` e
`DIGITAL_PACKAGE`, todos em v1. As atividades especializadas pertencem aos processos comuns;
os subprocessos reutilizados conservam definição e versão exatas na ficha. O executor recebe a
ficha no contexto da tarefa e continua consumindo os endpoints canônicos. Não decide o avanço.

## Histórico e evidências

Cada revisão é imutável. Um vínculo congela a ficha, a cadeia, o ciclo quando houver e a
referência de execução; uma revisão nova não altera o vínculo anterior. Não migrar produtos
ou execuções em curso automaticamente. Mudança de escopo exige nova referência ou revisão
explícita antes de iniciar trabalho. Provas, custos e decisões antigas permanecem consultáveis.

Leitura, comandos de atividade e execução automática devem transportar `sourceReference`
quando a navegação declarar um contexto. Validar propriedade, cadeia, ciclo e composição;
nunca substituir uma referência explícita pelo ciclo ou experimento mais recente. Ficha
ainda sem vínculo não oferece navegação operacional que possa inferir outra execução.

Na passagem de contexto foram comparados: inferir sempre o mais recente (esforço baixo,
risco de trocar a execução), exigir ciclo em todo produto (esforço baixo, impede trabalho
privado anterior ao experimento) e transportar referência explícita com validação canônica
(esforço médio, preserva identidade e reúso). A terceira opção foi adotada na consulta,
no comando manual e na retomada automática das fichas.

O percurso de imagens exige contrato de quantidade e qualidade, entradas autorizadas,
personalização, produção, revisão e acesso/entrega. A jornada guiada exige primeiro resultado
útil e continuidade. Ferramentas com IA exigem validação de entrada, personalização, limites,
falhas e qualidade. Kits exigem completude, usabilidade e entrega.

Uma dispensa deve ter motivo explícito, não comprova objetivo e nunca dispensa aprovação
humana, qualidade, privacidade, homologação ou conciliação comercial. Dados ausentes bloqueiam;
ausência de venda medida não equivale a zero vendas.

## Plutus e consumo

Quatro pontos de controle: oferta, desenho da entrega, homologação e operação. O parecer de
Plutus deve corresponder à ficha e ao plano, com evidência persistida e decisão identificada.
Reutilizar o parecer da mesma revisão quando não houver mudança material, sem nova chamada
paga por imagem. A aprovação de um checkpoint não autoriza mídia, publicação ou cobrança.

A economia usa três cenários (favorável, base e conservador), receita e custos na mesma moeda,
incluindo aquisição, taxas, suporte, infraestrutura, armazenamento, entrega e IA. O custo de IA
inclui o pacote inteiro, falhas cobradas e regenerações. Margem e teto precisam caber no cenário
conservador; desconhecido bloqueia. Mudança de modelo, preço, tarifa, consumo ou contrato exige
nova revisão antes de novos gastos. Limites locais não garantem um teto financeiro no provedor:
uma resposta acima da reserva é auditada e bloqueia novas chamadas, sem apagar o custo real.

Reservar consumo atomicamente antes da chamada externa, com correlação e idempotência. Falha
ou custo ainda desconhecido não devolve automaticamente orçamento. Dados de teste têm escopo
próprio e não alimentam vendas, receita ou satisfação reais. Métricas de resultado só recebem
valores conciliados de fonte identificada; estimativas financeiras não são resultados medidos.

O orçamento privado de produção e teste (`productionBudget`) é separado do custo variável
de cada entrega vendida. Seu teto vale para toda a execução, mesmo com várias chaves de pacote.
Pode usar modelo diferente daquele da entrega. Zero explícito nos três limites desabilita o
consumo privado; campo ausente não é zero. A economia por venda inclui amortização de produção
em `otherCostsBrl` quando aplicável, com premissas de volume conferidas por Plutus.

Uma entrega estática pode declarar `costModel=NO_VARIABLE_AI_COST` e os dois custos de IA
iguais a zero. Isso não elimina aquisição, taxas, suporte, armazenamento ou custo de produção,
nem permite uma chamada paga da entrega. Nos demais modelos, os custos precisam ser positivos.

| Alternativa para os orçamentos | Benefício | Risco | Esforço | Escolha |
| --- | --- | --- | --- | --- |
| Misturar produção e cada venda | Menos campos | Custo errado ou bloqueio de formatos legítimos | Baixo | Não |
| Dispensar controle fora das imagens | Adoção rápida | Consumo sem proteção financeira | Baixo | Não |
| Orçamentos explícitos separados | Margem coerente e controle por escopo | Exige premissas financeiras | Médio | Sim |


## Decisão de controle financeiro

| Alternativa | Benefício | Risco | Esforço | Escolha |
| --- | --- | --- | --- | --- |
| Consultar Plutus em cada imagem | Parecer frequente | Custo e latência repetidos | Alto | Não |
| Confiar apenas em limite do worker | Implantação pequena | Concorrência e retry excedem o pacote | Baixo | Não |
| Parecer reutilizado e reserva no backend | Proteção determinística e auditável | Requer conciliação de custos | Médio | Sim |

Na v1, o gerador administrativo reserva duas variações; o Lead Portal reserva uma chamada
por imagem sem base e até três por imagem-base, conforme contrato testado do AI Worker.
São tentativas **reservadas**, não comprovação do número efetivamente cobrado. A reserva
completa deve caber na ficha antes de o worker assumir o pacote. Resultado incompleto não
promove a entrega. Os demais gates e a revisão de qualidade continuam obrigatórios.
O primeiro usa o orçamento privado; o segundo usa o teto de cada pacote vendido.

Custos ausentes nos callbacks permanecem `COST_PENDING`. A tela oferece conciliação humana
com link do comprovante oficial, responsável, valor e justificativa/câmbio. Sobrecusto fica
`OVER_BUDGET` e bloqueia novas chamadas mesmo quando outro pacote é solicitado.

A adoção é explícita por nova execução. O catálogo e os prompts cobrem os quatro percursos;
o conector financeiro de entrega implementado nesta v1 cobre imagens administrativas e
pacotes do Lead Portal. Outros executores que futuramente gerem consumo de uma capacidade
precisam integrar a mesma reserva antes da chamada, sem declarar cobertura que não executam.

Referência de desenho consultada: [subprocessos reutilizáveis e vínculo de versão](https://docs.camunda.io/docs/components/modeler/bpmn/call-activities/).
