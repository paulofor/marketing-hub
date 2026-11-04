# Recuperação da homologação de Capella — 04/11/2026

A tarefa técnica 894 terminou no limite do executor e foi classificada como falha do
produto. O fluxo abriu Dédalo 895, que não tinha uma rejeição funcional para corrigir.
A correção permite homologar a mesma candidata após recuperação comprovada, preservando
tarefas, custos, entradas e exigências de revisão independente.

## Matriz e resultados locais

| Verificação | Resultado |
| --- | --- |
| Prontidão, gates, grafo, predecessores, projeção e correção do backend | 158 testes aprovados |
| Worker de Psique, incluindo contratos de Mira e separação de falha técnica | 242 aprovados; 13 condicionais não executados |
| Navegador real: caminho válido, retomada, prazo, HTTP 401/504, crash e identidades distintas | 69 testes aprovados |
| Compatibilidade adicional de Mira: service, controller e comparação de entradas | 20 aprovados; 1 teste condicional não executado |
| Compatibilidade adicional de Vega: estilo, versões privadas e segurança de execução | 32 testes aprovados |
| Empacotador de provas imutáveis | 17 testes aprovados |
| Seleção de publicação, incluindo atestações sem deploy automático | 22 testes aprovados |

Capella e outro produto/experimento foram exercitados com ids distintos. Rejeição funcional
continua exigindo correção. Downloads recusados não provocam nova transferência; crash aborta
o cenário; timeout conserva a categoria `EXECUTOR_FAILURE` e as últimas observações disponíveis.

Na investigação, uma leitura de imagem recebeu HTTP 504 no proxy após 60,057 segundos.
Uma reprodução posterior dos nove cenários, no runtime existente, terminou em 193,841 segundos;
a sandbox com o mesmo Chrome terminou em 206,335 segundos. Essas execuções foram diagnósticas,
sem callback de aprovação de tarefa. A causa exata do timeout 894 não foi preservada pelo
executor antigo; ela não pode ser convertida em reprovação funcional por inferência.

## Compatibilidade com as provas de Mira e Vega

O manifesto v70 fixa os hashes do runner, do provedor de retrabalho e dos respectivos testes.
As quatro fontes mudaram nesta correção. A v71 conserva o manifesto v70 integralmente e registra
o delta depois das regressões acima. A experiência, as fontes visuais, os contratos de entrada
e saída e o harness específico de Mira não mudaram. Os testes preservam seus gates e vínculos.

Vega fixa o runner compartilhado e seu teste na atestação de estilo v9. A v10 registra
somente esses dois deltas, mantendo a v9 intacta. As 32 regressões adicionais preservam
entradas, versões e segurança da entrega de Vega; o worker completo preserva seus contratos.
Nenhuma fonte visual ou implementação específica desses produtos foi alterada.

A atestação é `EVIDENCE_COMPATIBILITY_ONLY`, com `automaticDeployOnMerge=false`. Não altera
experimento de Mira ou Vega, não renova parecer de Psique/Têmis e não autoriza divulgação ou gasto.
Os testes locais não comprovam vendas. O critério de adoção é liberar somente a nova
homologação técnica necessária, sem ignorar reprovação funcional ou gerar repetição infinita.

A engine Docker local respondeu ao ping, mas expirou nas consultas de versão e containers.
A validação usou navegador real e dependências locais controladas; não houve mudança de schema.
O CI revelou que a conferência das atestações faltava no checklist local inicial. O contrato
operacional agora inclui o empacotador antes do commit para prevenir essa recorrência.
