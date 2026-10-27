# Vega — memória factual da pesquisa no sucessor

## Evidência observada

O ciclo 18/experimento 111 preservou o briefing do ciclo 16/experimento 109 e a
referência à modelagem 2/pesquisa 73. Sua memória, porém, não continha os objetos
`factualSources`. O contrato de adoção exata funciona intencionalmente somente
no ciclo original. Reutilizar esse mesmo bloco como uma adoção nova seria incorreto.

Comparadas três alternativas: repetir a pesquisa, transferir a adoção e oferecer
as fontes como memória histórica separada. Escolhida a terceira: preserva o
estudo e seus limites sem repetir consumo nem transferir autoridade comercial.

`LearningCycleTaskContext` recebe `inheritedReferenceResearch` de um componente
somente de leitura. Ele percorre até 32 ancestrais explícitos, exige o mesmo
produto e uma adoção anterior ao sucessor, preserva as fontes congeladas e remove
a decisão estratégica anterior. Não modifica `referenceModeling`, registros de
adoção, tarefas, ciclos ou orçamentos. Não interpreta concorrência como venda.

## Matriz de verificação

| Caso | Aceite |
| --- | --- |
| Vega 16→18 e outro produto | Fontes completas na memória entregue à tarefa, IDs próprios |
| Adoção própria | Comportamento anterior preservado, sem contexto substituto |
| Produto diferente ou fonte futura | Memória recusada |
| Tarefa anterior ao ciclo | Contexto indisponível, sem alteração histórica |
| Sucessor intermediário | Origem rastreável sem copiar aprovação |
| Ciclo inválido ou fontes vazias | Sem loop ou evidência inventada |
| Escritas e consumo | Nenhum `save`, pesquisa, geração ou gasto |

Os testes usam serviços reais com repositórios simulados; não há SQL ou schema
alterado. O teste da passagem resolve o contexto canônico da tarefa, além da
consulta do componente. Os caminhos de adoção e planejamento existentes são
executados como regressão. Não se trata de uma nova inferência por Atena nem de
aceite comercial do ciclo 18. O ganho esperado é evitar reconsulta desnecessária
e decisões sem fonte; economia e vendas devem ser medidas em execução futura.

## Resultado local

38 verificações de contexto, adoção e recuperação e as 92 regras ArchUnit
passaram. Depois, a regressão adicional de corrupção de conteúdo passou junto
aos seis casos do novo componente (7 no total). São 131 verificações relevantes,
sem falhas. A primeira execução conjunta foi interrompida por memória na JVM
configurada para 3 GiB; a mesma suíte passou em processo único com heap de
1.400 MiB, sem mudar regras ou reduzir cobertura. A compilação intermediária
anterior aos marcadores finais foi repetida com o código completo.

O manifesto de compatibilidade de Mira é revalidado porque compartilha o
contrato de contexto. Ele não publica outra experiência nem renova revisões.
