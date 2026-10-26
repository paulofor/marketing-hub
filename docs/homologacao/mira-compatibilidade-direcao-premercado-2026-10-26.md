# Compatibilidade de Mira com mudanças antes da autorização comercial

O CI do PR #5704 recusou o hash antigo de `LearningCycleSuccessorPreparation`
na atestação v48. O serviço compartilhado passou a permitir a preparação de uma
mudança encerrada em AUTHORIZATION, preservando as proteções de recibo, identidade,
experimento PLANNED sem publicação solicitada, revisão e versão nova.

Foram comparadas a manutenção do bloqueio, a alteração do manifesto histórico e
uma nova atestação de compatibilidade. A terceira preserva o histórico e registra
as fontes efetivamente revalidadas. A v49 mantém produto, experiência privada v3,
publicação, artefatos e pareceres de Mira. `automaticDeployOnMerge` permanece false;
não existe nova aprovação comercial, inferência ou campanha.

Validação local: 115 testes de preparação, comandos, regras e modelagem; 14 testes
do service/controller privado de Mira; 92 regras de arquitetura. A regressão do
preparo mantém o caso de Mira em ADJUSTMENT e VALIDATION, inclui Vega e outra
identidade em AUTHORIZATION e recusa mercado, publicação solicitada e recibo ausente.
Os 43 testes de frontend e a navegação em três dispositivos preservam o fluxo anterior.

A atestação atualiza somente os dois hashes afetados, vincula a v48 por SHA-256 e
documenta este escopo. O empacotador canônico e o seletor de publicação devem ser
validados localmente antes de atualizar o mesmo PR. A compatibilidade técnica
não comprova experiência humana, receita ou margem.
