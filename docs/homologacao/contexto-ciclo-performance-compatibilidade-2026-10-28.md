# Compatibilidade da leitura compacta por ciclo — 28/10/2026

A navegação publicada confirmou que o contexto do ciclo ainda carregava tarefas
integrais para indicar o próximo trabalho. `LearningCycleWorkResolver` passa a pedir
o resumo; `BusinessProcessActivityExecutionService` resolve a referência canônica
do ciclo para consultar a mesma projeção leve usada pelas referências explícitas.

## Prova local

Os 85 testes relevantes passaram: 12 do resolvedor (incluindo três identidades),
35 de execução de atividades, cinco de contexto, dois de cadeia publicada,
19 de continuação, dez de revalidação e dois cenários HTTP integrados de homologação.
O teste integrado reproduziu a leitura indevida antes do ajuste e passou depois,
exigindo ausência de consulta integral de tarefas e do histórico de experimentos.

Os caminhos bloqueado, concluído, recuperação, decisão humana, contexto divergente
e leitura histórica permanecem cobertos. Não há chamadas pagas nem alteração de
produto, experimento, autorização ou parecer independente.

## Limites da atestação

`mira-private-candidate.v59` preserva integralmente a v58, registra seu hash e
atualiza apenas as fontes compartilhadas revalidadas. Mantém
`EVIDENCE_COMPATIBILITY_ONLY`, a experiência `mira-private-candidate-v3`, o mesmo
fingerprint do frontend e `automaticDeployOnMerge: false`. Esta compatibilidade
não renova a homologação comercial nem publica outra versão do produto.

A publicação da aplicação e a medição desta passagem devem ser verificadas pelo
SHA integrado; testes locais não comprovam latência de produção ou aumento de vendas.
