# Homologação do runner de processos no Actions — 12/09/2026

## Causa confirmada antes da correção

O [job 103490231388](https://github.com/paulofor/marketing-hub/actions/runs/34670279765/job/103490231388)
do PR #5171 parou em `worker-tests.log` depois de aprovar backend, interface, TypeScript e build.
O erro foi `MODULE_NOT_FOUND` ao executar `node --test process-execution-worker/test` com
Node 22.23.2. Os dois artefatos disponíveis (10290158353 e 10290174254) contêm a mesma falha.
O workflow passou a uma nova tentativa sem mudança do código; repetir o job não corrige o contrato.

A reprodução local confirmou que o comando antigo passa no Node 20.20.2 da sandbox, mas falha
no Node 22.23.2 do Actions. O comando documentado pelo módulo, `npm --prefix
process-execution-worker test`, descobre os seis testes e passa no Node 22.23.2. A homologação
anterior cobria Node 22 dentro da imagem, mas permitia executar o runner externo em outra versão.
A documentação oficial descreve a descoberta de arquivos a partir do diretório corrente quando
`node --test` é executado sem argumentos de arquivo:
[Node 22 — execução dos testes](https://nodejs.org/download/release/v22.17.0/docs/api/test.html#running-tests-from-the-command-line).

Evidência bruta local: `artifacts/actions-process-automation/github/` e `reproduction.json`.
Não se trata da antiga ausência de contratos v4 de Têmis: Backend CI da revisão e suíte backend
deste artefato passaram. Nenhum changelog ou critério funcional precisa ser afrouxado.

## Alternativas consideradas

| Alternativa | Benefício | Risco e esforço | Aderência |
| --- | --- | --- | --- |
| Passar um glob explícito de arquivos ao Node | Correção curta | Baixo esforço, mas duplica a seleção já declarada no módulo | Boa |
| Usar `npm test` do módulo e igualar o runtime da homologação | Uma entrada de testes, descoberta automática e paridade com CI | Baixo esforço; requer Node 22 local | Melhor |
| Executar todos os testes do worker somente no container | Runtime isolado por construção | Esforço médio, nova montagem de suíte e manutenção do runner | Boa, desnecessária para esta causa |

Adotada a segunda alternativa. Os gates agora apresentam progresso e o erro relevante no console,
além dos logs completos. O teste real do runner verifica que uma falha recém-adicionada em
subdiretório interrompe a matriz e que o erro do próximo gate conserva código, diagnóstico e limpeza.
O Actions roda esse contrato antes da preparação mais longa e distingue artefatos por tentativa.

## Matriz definida antes das rodadas completas

| Dimensão | Critério |
| --- | --- |
| Reprodução | Comando antigo passa no Node 20 e falha no Node 22.23.2; entrada canônica passa no Node 22 |
| Regressão | Runner real descobre a suíte, encontra teste novo aninhado e não oculta falha nem avança ao próximo gate |
| Paridade | Node 22 no runner, workflow e imagem; runtime inadequado recusado antes da infraestrutura |
| Backend e interface | Mesmas suítes completas do runner do CI, TypeScript e build |
| Integração | MySQL 5.7, API, concorrência, ciclo de vida, reinício e reaplicação Liquibase |
| Worker | Testes unitários, imagem versionada, autenticação simulada e parada graciosa |
| Navegação | Chromium desktop e emulação de iPhone 15 Pro e Pixel 7; painel, comandos e retomada |
| Observabilidade | Etapa, arquivo e erro visíveis; evidências preservadas por rodada/tentativa |
| Segregação | Banco, IDs, tokens e agentes sintéticos; nenhum efeito sobre IA, campanhas, cobranças ou métricas reais |
| Entrega | Sintaxe, contratos de workflow/deploy, diff revisado e topologia temporária removida |

Após a última correção, executar duas rodadas completas consecutivas sem falhas. Se houver nova
correção, reiniciar a contagem. A integração usa backend e MySQL reais com agentes simulados;
emulação Chromium não representa Safari ou aparelhos físicos.

## Resultados

As duas rodadas completas consecutivas passaram com Node 22.23.2, Java 21 e MySQL 5.7 real,
sem alteração de código entre elas. O runtime Node 20 também foi recusado em verificação
negativa antes de qualquer operação Docker. Nenhum critério local ficou pendente.

| Verificação | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend completo | 2.708 executados, zero falhas; 7 exclusões preexistentes | Mesmo resultado |
| Interface | 605 testes, TypeScript e build aprovados | Mesmo resultado |
| Executor | 6 testes aprovados no Node 22.23.2 | Mesmo resultado |
| Regressão do runner | 2 contratos aprovados com descoberta real e falhas simuladas | Mesmo resultado |
| Entrega | 3 contratos aprovados e verificações transacionais de deploy | Mesmo resultado |
| Integração MySQL | 17 cenários gerais e 6 de ciclo de vida aprovados | Mesmo resultado |
| Persistência | Reinício e reaplicação Liquibase aprovados | Mesmo resultado |
| Imagem do executor | Autenticação local, usuário restrito e parada graciosa aprovados | Mesmo resultado |
| Navegação | Desktop, iPhone 15 Pro e Pixel 7 emulados aprovados | Mesmo resultado |
| Limpeza | Topologia e imagens temporárias da rodada removidas | Mesmo resultado |

Actionlint do workflow, sintaxe Bash e revisão do diff também passaram. A verificação final
confirmou ausência de containers, volumes e redes do projeto Compose exclusivo da sandbox.
As impressões SHA-256 dos arquivos executáveis confirmam código idêntico nas duas rodadas.

Logs e capturas: `artifacts/process-automation/actions-node22-round-1/` e
`artifacts/process-automation/actions-node22-round-2/`. Reprodução, versões, checks estáticos,
hashes e conferência consolidada: `artifacts/actions-process-automation/final-verification.json`
e demais arquivos desse diretório. O relatório foi atualizado após os testes; a implementação
permaneceu idêntica à validada nas duas rodadas.

A tentativa 2 do Actions também terminou com a mesma falha do comando antigo. Nenhum workflow
foi disparado para testar a correção. As mudanças estão apenas na sandbox, sem commit, push,
PR, publicação de imagem ou intervenção em produção; a revisão corrigida deverá ser enviada
pelo fluxo de PR do usuário.
