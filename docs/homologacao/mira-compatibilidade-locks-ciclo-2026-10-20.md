# Compatibilidade privada com a ordem de locks dos ciclos

A publicação de Capella revelou uma promoção concorrente de locks no experimento.
O serviço compartilhado de ciclos passou a bloquear o produto antes do ciclo nos
atalhos de orçamento/homologação e a bloquear o experimento antes de gravar a janela.
Nenhum contrato funcional, preço, experiência ou aceite de Mira foi alterado.

A validação do pacote recusou corretamente o hash antigo de LearningCycleService.
A atestação v32 preserva a v31 imutável e atualiza somente a referência desse serviço.
Não altera a versão privada v3, não publica Mira, não executa tarefas de agentes e
não modifica autorizações financeiras ou pareceres independentes.

Validação local: regressões de ciclos/registro/homologação/seleção e arquitetura
aprovadas, além dos 14 testes de serviço/controller privados de Mira. O MySQL 5.7.44
reproduz o deadlock anterior e comprova a ordem corrigida em duas identidades; o
callback de resultado recupera o mesmo job sem inferência. Os 16 testes do construtor
de evidências preservam bloqueio de hash divergente, história e isolamento.

Limite: compatibilidade técnica das fontes compartilhadas, sem novo parecer do
produto, aprovação comercial ou medição de conversão. AutomaticDeployOnMerge
continua false; não houve retentativa paga de Mira para renovar esta prova.
