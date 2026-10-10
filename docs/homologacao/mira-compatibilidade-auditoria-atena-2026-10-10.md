# Mira — compatibilidade da auditoria de Atena

## Causa confirmada

O PR #5574, integrado na main em `e0306028809665270e4dca90fe3bf5c620a3b268`,
preservou a versão real do prompt na recuperação de Atena. A atestação privada
de Mira v16 ainda apontava o hash anterior de
`PdeMarketStrategyBpmTaskConsumer.java`. O empacotador canônico detectou essa
divergência na validação local da solicitação #3335 de Capella, antes de qualquer
commit. A fonte divergente já estava na main; não foi alterada nesta solicitação.

Manter a divergência impediria o empacotamento do backend. Reescrever a v16 ou
relaxar o validador destruiria a rastreabilidade. Foi escolhida a revalidação
proporcional com atestação sucessora v17, preservando a v16 e o mesmo produto,
experiência privada, pareceres e autorizações.

A mesma sucessora revalida o acesso compartilhado às aprovações no painel:
`LearningCyclesPage.tsx` e seu teste também pertencem às evidências congeladas.
O link agora fica disponível para percursos sem integração automática, sempre
com produto e experimento explícitos. A rota já funcionava para Mira; o seu
destino e a decisão humana permanecem iguais, sem mudança dos arquivos da
experiência privada. Os 27 testes da tela e seis navegações locais (desktop,
iPhone e Pixel, com duas identidades) passaram sem gravação ou autoaprovação.

## Validação local e limite

Os testes `PdeMarketStrategyBpmTaskConsumerTest` e `PdeMarketStrategyDeliveryTest`
passaram: **40 casos, sem falhas ou skips**. Eles preservam processamento e
callback canônicos, versão original do prompt e recuperação sem nova inferência,
incluindo diferentes identidades e caminhos antes válidos.

A candidata continua `mira-private-candidate-v3`; esta é compatibilidade do
harness compartilhado, não uma nova hipótese, experimento ou autorização. A
publicação privada permanece ligada ao manifesto comercial v25 e seu hash de
frontend; o seletor da nova atestação deve retornar `none`. Sem alterações da
experiência de Mira, novas tarefas, revisão comercial fabricada ou novo gasto.

O empacotador e o seletor existentes são executados novamente após a criação
da v17. A sucessora atualiza somente os hashes conferidos, vincula esta evidência e
preserva a v16 como referência atestada. Nenhum manifesto histórico é reescrito.
