# Homologação — liberação econômica Opala / tarefa 436

Data: 2026-09-17  
Produto: Vega (ID 4)  
Ciclo: 2  
Experimento: 92  
Versão: `musa-pde-entry-v12-primeiro-ajuste-aplicavel`  
Processo: Preparar operação comercial Opala v1 (definição 77)

O cadastro produtivo confirma Vega como `PDE - Produto Digital Experiencial`, com o percurso comercial Opala já vinculado ao ciclo; não houve reclassificação de tipo, oferta ou formato.

## Resultado

Foi criada pela tela administrativa a revisão financeira LIVE `1`, plano `Vega v12 — piloto econômico conservador`, para a versão exata do ciclo e o plano comercial 3 v7. A revisão permanece separada de receita realizada e não autoriza campanha, cobrança ou gasto.

A tarefa 436 e seu custo estimado de USD 0,406908 foram preservados como histórico. Não houve nova chamada paga de Plutus. A atividade produtiva não foi reiniciada porque a janela do ciclo terminou em `2026-09-17T02:59:00Z` e a proteção sistêmica ainda precisa passar por PR e publicação.

## Decisão entre alternativas

| Alternativa | Benefício | Risco / custo | Decisão |
|---|---|---|---|
| Repetir a tarefa 436 com o contexto atual | Nenhum desenvolvimento | Repetiria custo e retornaria o mesmo bloqueio | Rejeitada |
| Preencher ausências com zero ou limites otimistas | Liberação rápida | Margem artificial e risco de vender com contribuição negativa | Rejeitada |
| Persistir projeção conservadora, validar antes da inferência e revalidar somente a janela | Auditável, reutilizável e protege margem | Exige publicação coordenada antes da retomada | Escolhida |

## Plano econômico persistido

- Preço: R$ 67,00; Pepper `owm6x`.
- Taxa Pepper: 6,99% + R$ 2,49, reconciliada com uma transação autenticada de R$ 67,00 bruto e R$ 59,83 líquido.
- Reembolso: provisão de 12% herdada do plano comercial; é premissa, não taxa observada.
- Tributos: provisão conservadora de 33%, usada como teto de teste; não é a alíquota efetiva. Antes de escalar, deve ser substituída pelo PGDAS/RBT12 aplicável.
- Afiliado/comissão: 0% somente para aquisição direta neste piloto; entrada de afiliado invalida a revisão.
- Entrega por cliente: suporte R$ 5,00, armazenamento R$ 0,10 e infraestrutura R$ 0,40.
- IA durante o uso: R$ 0,00. A telemetria e `AiGuidanceService` identificam `MUSA_LOCAL_RULES_V1`, `service_tier=LOCAL` e custo zero tanto no resultado gratuito quanto nas missões pagas.
- Limites: sete resultados incluídos, nenhuma tentativa de IA paga, CAC máximo R$ 15,00 e margem mínima projetada de 20%.
- Validade da revisão: 2026-10-17.

Fontes externas registradas na própria revisão:

- Pepper, condições e taxas: <https://lp.pepper.com.br/condicoes-e-taxas>
- Pepper, reembolsos: <https://ajuda.pepper.com.br/pt-br/article/politicas-de-reembolso-de-compras-odde8p/>
- Pepper, chargeback: <https://ajuda.pepper.com.br/pt-br/article/chargeback-o-que-e-como-funciona-e-como-se-proteger-1qepfxo/>
- Lei Complementar 123: <https://planalto.gov.br/ccivil_03/leis/lcp/lcp123.htm>

### Cenários determinísticos

| Cenário | Clientes | CAC | Contribuição por venda após CAC | Margem projetada | Resultado operacional projetado |
|---|---:|---:|---:|---:|---:|
| Conservador | 3 | R$ 15,00 | R$ 9,18 | 30,92% | R$ 27,53 |
| Base | 5 | R$ 15,00 | R$ 9,18 | 30,92% | R$ 45,88 |
| Otimista | 8 | R$ 10,00 | R$ 14,18 | 47,77% | R$ 113,41 |
| Uso intenso contratado | 3 | R$ 15,00 | R$ 9,18 | 30,92% | R$ 27,53 |

São projeções para um teste limitado, não vendas nem lucro realizados. Um reembolso ou chargeback na primeira coorte exige pausa e revisão.

## Causa-raiz e prevenção

O fluxo Opala enviava Plutus antes de exigir um plano financeiro vigente, completo e da mesma versão. A ausência só era descoberta depois da inferência paga. Além disso, não existia comando seguro para renovar uma janela vencida preservando hipótese, versão e teto.

A candidata corrige a fonte compartilhada:

1. O backend resolve por produto, plano comercial, ambiente LIVE e versão exata uma revisão financeira imutável.
2. Plano ausente, vencido, inviável ou com preço divergente bloqueia a atividade antes de criar tarefa paga.
3. O worker de Plutus repete a validação antes da inferência e rejeita resposta que divergir do cenário BASE calculado.
4. O prompt vivo v3 recebe o cálculo determinístico; não reconstrói custos nem inventa limites.
5. A tela permite revalidar apenas datas de ciclo PLANNED, preservando versão, orçamento e ausência de autorização de gasto.
6. Alterar janela invalida o parecer econômico anterior, obrigando nova conferência com o prazo real.

A primeira candidata introduziu uma dependência circular ao reutilizar o serviço que também solicita Plutus. A regressão ampla encontrou o defeito. A versão final lê diretamente os registros imutáveis e não participa da criação de tarefas.

## Comparação com a versão anterior

| Critério | Anterior | Candidata |
|---|---|---|
| Descoberta de dados ausentes | Após chamada paga | Antes da criação da tarefa |
| Versão financeira | Texto livre no contexto | Produto + plano + versão exata |
| Números de Plutus | Modelo podia recalcular | Conferidos contra cálculo determinístico |
| Janela vencida | Exigia intervenção sem comando próprio | Comando auditável que altera somente datas |
| Histórico da 436 | Existia, sujeito a repetição | Preservado e não reutilizado como aprovação |
| Custo incremental da correção | Indisponível | Zero chamadas de LLM durante homologação |

## Matriz executada após a última correção

Foram concluídas duas rodadas consecutivas sem falha, cada uma contendo:

- testes direcionados de Opala, janela do ciclo e inicialização do contexto Spring;
- 12 testes do worker de Plutus;
- 18 testes da tela de ciclos, typecheck e build de produção;
- fluxo visual real da renovação de janela em desktop, iPhone 15 Pro e Pixel 7, sem campo de orçamento e sem origem externa;
- catálogo/prompt v3 e Liquibase em MySQL 5.7 real;
- 37 testes Opala com atualização, idempotência e rollback no MySQL 5.7;
- validação de comentários/formatação Java, contratos temporais, includes relativos e `git diff --check`.

Também foi aprovada uma regressão ampla do backend com 3.203 testes, zero falhas/erros e 17 testes ignorados previstos. O defeito circular foi encontrado antes dessa aprovação e reiniciou a contagem das rodadas.

## Estado final

- Revisão financeira LIVE 1: persistida e `PROJECTED_VIABLE`.
- Tarefa 436: preservada como `BLOCKED`; nenhuma repetição paga.
- Ciclo 2 / experimento 92: preservados; janela publicada continua vencida.
- Código: pronto e validado na worktree; sem commit, PR ou deploy.
- Campanha, cobrança, acesso e gasto: não autorizados nem executados.

Após publicação, a sequência segura é revalidar a janela pela tela e reiniciar uma única vez a atividade econômica. Plutus continuará independente: a projeção determinística habilita a análise, mas não substitui seu parecer nem a autorização humana de mídia.
