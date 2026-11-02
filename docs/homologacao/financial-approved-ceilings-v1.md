# Tetos financeiros na coluna Aprovado — 02/11/2026

Escopo: seis grupos da demonstração financeira do ciclo, sem novos gastos ou tarefas.
Capella: produto 7, cadeia 27, ciclo 19, experimento 112, versão capella-beleza-v3.
Tela consultada em 02/11/2026: teto US$ 80, reserva US$ 20, estimativas US$ 67,37689.
O processo continua em planejamento; esta entrega não altera as decisões de Capella.

Alternativas avaliadas antes da implementação:

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Mover o campo orçado para aprovado sem integrar recibos | Mudança pequena | Exibiria aprovação sem efeito nos controles reais | Recusada |
| Criar controle financeiro paralelo para seis grupos | Contrato uniforme | Duplicaria limites e gates; esforço alto | Recusada |
| Editar tetos na tabela usando recibos e gates canônicos | Uma entrada monetária por grupo, com efeito verificável | Esforço moderado; manter restrições de cada etapa | Escolhida |

Matriz definida antes dos testes:

| Critério | Homologação |
| --- | --- |
| Caminho feliz | Editar teto, confirmar escopo, persistir, recarregar e conferir recibo |
| Interface | Somente a coluna Aprovado contém entradas monetárias; sem receita, câmbio, orçamento ou custo manual |
| Agentes | Preservar reserva já autorizada e custos históricos; sem iniciar tarefa |
| Produção/mídia | Reutilizar gates, limites e decisões existentes; recusar edição fora da etapa |
| Operacionais | Aprovação individual de entrega, deduções e operação; sem exigir preenchimento de outros grupos |
| Validações/falhas | Negativo, precisão, campo ausente, confirmação, contexto, versão, revisão e ciclo encerrado |
| Recuperação | Repetição idêntica recupera recibo sem duplicar; chave com outro conteúdo recusada |
| Integração | UI → controller/service reais → banco local; nenhuma API paga |
| Regressão | Contexto original sintético, outra identidade e fluxo de preparação antes válido |
| Observabilidade | Histórico com autor, data, escopo, moeda e valor; nenhuma alteração produtiva de limites |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; sem estouro do documento |
| Métricas | Evidência de persistência/recuperação; fontes desconhecidas não viram zero ou lucro |

Lacuna observada do harness: fixtures e testes aceitavam três entradas monetárias manuais
por grupo, sem provar que o teto editado alimentava o recibo oficial. A regressão deve
conferir localização dos campos, integração da aprovação e preservação da reserva.
Hipótese: menos repasses e erros de interpretação ao aprovar investimento. Não há
medição de impacto comercial nesta entrega. Adoção após matriz; reversão preserva recibos.

## Resultados locais

- 681 testes Java dos ciclos e arquitetura aprovados; 51 testes financeiros da interface
  aprovados. Verificação de tipos, formatação e build de produção concluídos.
- UI → API real → MySQL 5.7 em desktop, iPhone 15 Pro e Pixel 7 emulados: seis campos
  monetários somente em Aprovado; confirmação, recarga, resposta perdida e replay sem
  duplicação. Zero explícito permanece diferente de valor desconhecido.
- Quatro aprovações concorrentes idênticas produziram um único recibo; mesma chave com
  outro conteúdo foi recusada. Outro produto continuou isolado.
- Preparação preserva a reserva anterior; produção usa o recibo oficial; mídia distribui
  o total escolhido pelos dias comerciais restantes sem ativar campanha. As regressões
  incluem duas identidades e o fluxo anterior de autorização de mídia.
- Navegação pelo card, parecer de Plutus, histórico e insuficiência de saldo preservados,
  sem formulários monetários duplicados. A inspeção visual conferiu as capturas locais.
- Limites: simulações não são gastos, receitas ou vendas; emulação não comprova Safari
  nativo. Fontes ausentes continuam desconhecidas. Nenhum valor real de Capella foi alterado.

A validação do pacote detectou mudança no hash de LearningCycleService, fonte compartilhada
atestada de Mira. Após a regressão dos ciclos, a nova atestação de compatibilidade preserva
v68, a experiência e os pareceres. Não altera nem publica o produto comercial de Mira.
