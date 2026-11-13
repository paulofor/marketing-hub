# Compatibilidade das rotas históricas MUSA — 13/11/2026

A nova rota `musa-consultor-pwa-v1/agent-validation` foi acrescentada antes do roteamento
histórico; não altera os componentes das jornadas anteriores. O gate de evidência
imutável apontou que as referências da entrada App.tsx nas atestações v12/v14 e o
contrato do publicador na v13 precisavam de revalidação. Documentos históricos não
foram modificados; novas revisões declaram apenas compatibilidade, sem deploy desses
produtos, reaprovação comercial ou repetição de agentes.

Validação: build/TypeScript, 232 testes unitários do PDE, 30 testes de publicação
(incluindo v16/v17 e recusa de capacidade antiga), roteamento histórico integrado com
contratos locais e MySQL 5.7. A candidata contínua passou sua matriz própria de nove
combinações; esse resultado não transfere aceite a jornadas anteriores.

As cópias de compatibilidade mantêm product, versões, evidências imutáveis anteriores
e autorizações. Só alteram os hashes das fontes revalidadas, registram vínculo ao
manifesto predecessor e `automaticDeployOnMerge=false`. Não substituem a atestação
v13 da superfície v10 que publica exclusivamente a candidata privada adicionada.

A rodada detectou uma lacuna do runner JVM local existente: ele exigia o token
sintético nos testes sem configurá-lo no backend, e não habilitava a concessão QA.
A configuração agora declara ambos somente na topologia de testes. O teste de
contrato do runner impede regressão e mantém SMTP sandbox-mail sem usar segredos
reais. O produto publicado não recebe esse token nem habilitação QA por esta mudança.

Resultado final: 30 percursos históricos v14/v15 aprovados na rodada inicial; os
18 casos v5/v6/v7/v12 afetados pela configuração do runner passaram após a correção,
sem repetir os outros 30. Todos em desktop/iPhone 15 Pro/Pixel 7 em Chromium,
backend PDE e MySQL 5.7 reais, integrações Pepper/Marketing Hub simuladas localmente.
Os quatro testes de contrato do runner e a montagem completa do pacote de provas
(839 arquivos e 216 manifestos nesta revisão) passaram. Nenhuma compra real,
reembolso real, mensagem externa ou inferência paga foi executada.
