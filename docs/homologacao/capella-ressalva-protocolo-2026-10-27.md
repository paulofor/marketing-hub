# Capella — ressalva condicional no protocolo — 27/10/2026

## Evidência, causa e escolha

Na retomada autorizada da execução 87, ciclo 15/experimento 108, Psique 837
parou antes da inferência: faltava identidade no contexto multiagente. Os logs
mostraram que `LearningCycleConstructionContext` descartava o contexto inteiro
porque o detector de comparação encontrava REFERENCE/REDUCED na estratégia 827.
O histórico contradisse a hipótese de estratégia incompatível: Atena escreveu
“18 combinações somente se implementada a comparação REFERENCE/REDUCED” e
“Se houver comparação documental implementada, executar 18 combinações”. Dédalo
829 preservou essa condição e a homologação 834 aprovou a matriz de nove cenários.
A correção de Vega do PR 5715 havia introduzido detecção por palavra, sem separar
ressalva condicional de exigência. O erro não era ausência de orçamento ou login.

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Refazer estratégia, economia e arquitetura | Pode retirar a frase | Repete inferência e invalida provas coerentes | Rejeitada |
| Remover a proteção contra comparação incompatível | Destrava o caso | Reintroduz a falha real de Vega | Rejeitada |
| Interpretar ressalvas explícitas por frase e valor JSON | Preserva contrato e identidade | Pequena mudança no produtor e consumidor | Adotada |

O backend e Atena usam a mesma regra: ressalvas nas duas formas explícitas acima
não habilitam a comparação. Outras obrigações continuam bloqueadas. A avaliação
ocorre por frase e por valor JSON, sem deixar a condição de um campo liberar outro.
Frases com contraste ou obrigação incondicional continuam recusadas. Não é um
interpretador geral de linguagem natural: formulações ambíguas permanecem conservadoras.
A capacidade da versão exata continua sendo a única fonte para a matriz comparativa.

## Matriz de validação local

- Caso original e outra identidade: contexto completo, linhagem preservada,
  três cenários e três dispositivos, sem comparação habilitada por uma frase.
- Caminho válido: Mira mantém capacidade comparativa e 18 combinações.
- Falhas: comparação obrigatória, mistura de frases, ponto e vírgula, condição
  de orçamento e contraste que tenta exigir comparação sem implementação.
- Integração: construção do contexto pelo serviço real e callback de Atena;
  a falha acontece antes de chamadas de modelo ou efeitos comerciais.
- Recuperação: histórico e homologação aceitos não são reescritos; retomada
  permanece pelos comandos oficiais, sem tarefa duplicada.
- Interface: nenhuma alteração de tela; conferir retomada e estado no painel.
- Observabilidade: preservar tarefa 837, erro e ausência de inferência; não
  transformar custo monetário desconhecido em zero conciliado.

Antes da correção, quatro regressões falharam: duas ressalvas foram indevidamente
rejeitadas e dois contextos perderam a identidade. Depois, 43 testes direcionados
do backend passaram, incluindo recuperação e capacidade por versão. Os 89 testes
do executor Atena passaram, sem falhas ou testes ignorados.

A atestação de compatibilidade de Mira preserva as versões anteriores, sem renovar
pareceres ou autorização comercial, nem republicar seu produto.

## Aprendizado e limite comercial

Hipótese verificável: distinguir ressalva de obrigação reduz revisões interrompidas
antes do modelo e repetições do planejamento. Medir falhas por contexto ausente,
retomadas e custo até aceite. Testes locais comprovam comportamento, não vendas.
Reverter se comparação sem capacidade for aceita ou identidade histórica for alterada.

Autorização do usuário #3786 registrada pela tela em Capella: US$30 cumulativos de
preparação, incluindo US$25,5265436 conhecidos estimados antes da retomada; mídia
até R$89,95 totais por cinco dias após homologação. Sem novo audiovisual pago.
Os valores Codex são estimativas e não exigem extrato da API. Autorização não
substitui parecer independente, homologação ou seleção humana dos materiais.
