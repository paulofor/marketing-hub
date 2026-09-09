# Homologação — navegador versionado no CI de Argos e Psique

Data: 2026-09-09. Revisão investigada: `689df350591f84a99354efe55fee761e4e22e6c9`.

## Causa confirmada e histórico

Os runs [34385449770](https://github.com/paulofor/marketing-hub/actions/runs/34385449770)
(Argos) e [34385449771](https://github.com/paulofor/marketing-hub/actions/runs/34385449771)
(Psique) falharam durante `playwright-core install --with-deps chromium`. O APT do runner
consultou o repositório de Google Chrome e recebeu `Hash Sum mismatch`, encerrando com código 100.
Os runs de PR `34385321543` e `34385321564` tiveram a mesma falha. Nenhum desses erros ocorreu no
coordenador de publicação corrigido anteriormente.

O run posterior `34386330587` corresponde a outro caso: a continuação encontrou a origem antiga
`34348645618` realmente concluída com `failure` no SHA `28b3f866`. O gate manteve o bloqueio, como
previsto no contrato; essa conclusão não foi convertida artificialmente em sucesso. As continuações
`34386413910` e `34386413873` terminaram verdes com os jobs de publicação não aplicáveis ignorados.
O deploy central do SHA investigado permanecia na fila durante esta análise.

Na revisão anterior, `ecf8a6b4`, os runs `34382224626` e `34382224462` aprovaram essas mesmas etapas.
Isso confirma a instabilidade externa da instalação, não uma regressão funcional dos agentes.
O problema corresponde à recorrência de `LOOP-ACTIONS-META-PLAYWRIGHT-APT-MUTAVEL`, antes corrigida
no Meta Ad Approver. Os Dockerfiles de Argos e Psique ainda repetiam essa instalação dinâmica.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
|---|---|---|---|
| Repetir a instalação com limite | Pequeno ajuste; recupera falhas transitórias | Mantém dependência dos índices e atrasa o diagnóstico | Não escolhida |
| Restringir o APT aos repositórios necessários | Evita que o índice do Chrome interrompa a instalação | Ainda depende da sincronização Ubuntu e exige manutenção de fontes | Não escolhida |
| Usar imagem Playwright versionada nos testes e no runtime | Browser e bibliotecas já compatíveis; dispensa APT e download do navegador | Exige validar imagem, permissões e versão do pacote | Escolhida |

O contrato oficial exige alinhar a versão da imagem à do pacote do projeto:
[Playwright Docker](https://playwright.dev/docs/docker) e
[Playwright no CI](https://playwright.dev/docs/ci). Ambos os agentes usam `playwright-core` 1.54.2.
A base escolhida é `mcr.microsoft.com/playwright:v1.54.2-noble`, fixada também pelo digest
`sha256:18b4bcff4f8ba0ac8c44b09f09def6a4f6cb8579e5f26381c21f38b50935d5d8`.

## Matriz definida antes dos testes

| Dimensão | Validação local e resultado exigido |
|---|---|
| Caminho feliz | Suíte completa Node de Argos e captura de Psique dentro da mesma base do CI |
| Versão e contrato | Pacote, lock, Dockerfile e workflow concordam; drift e instalação dinâmica são recusados |
| Falhas | Browser ausente ou teste inválido falha; não há `continue-on-error` nem sucesso artificial |
| Integrações | Backend, fontes externas e IA simulados; fixtures locais de coleta e captura |
| Imagem final | Build pelos Dockerfiles versionados, certificados, Codex e Chromium funcionando como usuário do runtime |
| Psique | Testes Java completos e captura real da imagem em filesystem somente leitura |
| Argos | Biblioteca factual materializada, coleta renderizada e browser real da imagem sem caminho global do host |
| Observabilidade | Logs por controle, contagens de testes, versão/digest e códigos de saída preservados |
| Publicação | Coordenação por mesmo SHA, cancelamentos, fila, SSH e proveniência das imagens continuam protegidos |
| Navegadores e dispositivos | Chromium empacotado; captura iPhone 15 Pro e leitura de cards desktop, conforme suítes afetadas |
| Métricas e segregação | Nenhuma chamada a vendas, analytics, SMTP real ou modelo pago; dados sintéticos e projeto Compose exclusivo |
| Higiene | Diff válido, Actionlint e contratos; limpeza da topologia e imagens temporárias da sessão |

Após o último ajuste, foram executadas duas rodadas completas consecutivas sem falhas, conforme
os resultados abaixo. PR, push e deploy não foram usados como teste.

## Ajuste encontrado durante a validação local

A primeira suíte Java de Psique executou 89 testes e revelou uma falha em
`CodexProcessSupervisorTest.keepsProcessAliveWhileJsonlAdvances`: esperava `COMPLETED`, recebeu
`INACTIVITY_TIMEOUT`. A fixture real emitia eventos a cada 50 ms, mas exigia agendamento do shell
abaixo de 130 ms. O arquivo da execução interrompida contém os cinco eventos previstos; a falha
ocorreu na pequena janela final antes da saída do shell. O mesmo teste passou no CI anterior.

Foram comparadas três alternativas: ampliar as janelas (barato, mas mantém flutuação), sincronizar
shell e teste por sinais (melhora o início, mas mantém disputa com o relógio real) e controlar o
relógio monotônico e o progresso (determinístico, pequena extensão no construtor de teste).
Foi escolhida a terceira. A produção mantém `System.nanoTime` e os mesmos limites; apenas os
cenários de progresso e teto usam processo simulado e relógio controlado. O teste de encerramento
da árvore continua usando shell e descendente reais. Os testes confirmam progresso além da janela
inicial de inatividade e interrupção exata no teto absoluto, sem enfraquecer as asserções.

## Integridade da homologação e armazenamento

- A engine isolada não enxerga o caminho do checkout como bind mount: a tentativa inicial recebeu
  `ENOENT` para `package.json`. A topologia local passou a receber o checkout e suas dependências por
  streaming em volume exclusivo, com limpeza ao final; o CI continua usando o checkout do runner.
- O primeiro build de Psique encontrou `No space left on device`. A medição confirmou o filesystem
  de 59 GiB cheio; não era HTTP 429 do Maven. Foram removidos por ID somente 20 registros de cache
  criados pelos nossos builds de 18:07 (698,2 MB). Nenhuma imagem de outro trabalho foi removida.
  Os Dockerfiles deixam de empacotar caches npm e passam a compartilhar a camada do cliente Codex.
  Depois desse ajuste, o build integral de ambos os módulos e as capturas reais passaram.
- A revisão da matriz identificou que o índice factual de Argos estava materializado apenas na
  cópia usada pelos testes. A preparação local agora repete também a materialização do checkout
  antes do build, como já ocorre no workflow, e lê a biblioteca pelo método produtivo na imagem.
  O Dockerfile também recusa índice ausente antes de terminar o build. As rodadas finais incluem
  esses controles adicionais; as rodadas anteriores de 23 controles não são a aprovação final.
- A suíte de CI e as imagens finais executam sem rede externa nas provas de navegador. As imagens
  finais são testadas como usuários `1000` (Argos) e `10001` (Psique), com filesystem somente leitura.
  A leitura de `codex --version` não chama modelo nem inicia autenticação; o aviso de aliases em
  filesystem somente leitura não impede o comando e não foi ocultado nos logs.

## Resultado final

| Rodada final | Controles | Resultado |
|---|---|---|
| `complete-1` | 25/25 | Aprovada, sem falhas e sem testes ignorados |
| `complete-2` | 25/25 | Aprovada, consecutiva e sem mudança funcional entre as rodadas |

Cada rodada aprovou 124 testes Node de Argos, 89 testes Java de Psique, duas provas de captura
na base do CI e duas na imagem final, 19 contratos de versão do navegador, 17 cenários de
coordenação de publicação e 39 contratos de imagens. Também passaram Actionlint, ShellCheck,
arquitetura dos agentes, contratos de SSH, fila de publicação e revisão do diff.

As duas imagens foram construídas pelos Dockerfiles versionados e abriram Chromium `139.0.7258.5`
com Node `22.18.0`, nos usuários produtivos, em desktop, iPhone 15 Pro e Pixel 7. Psique preservou
Java 21 e a captura integral com 12 dobras. A leitura produtiva da biblioteca de Argos encontrou
7 evidências selecionadas e cobertura de 14 documentos Gartner, 23 de IA aplicada e 15 de momentos
de compra B2C. Browser inexistente foi recusado sem download nem fallback silencioso.

Evidências brutas locais: `artifacts/actions-browser-2026-09-09/complete-1/` e
`artifacts/actions-browser-2026-09-09/complete-2/`, com `results.tsv` e um log por controle.
A matriz reproduzível está em `scripts/test-agent-browser-ci-local.sh`; requer
`ACTIONS_TEST_COMPOSE_PROJECT` com o projeto exclusivo do ambiente.

Ao final de cada rodada, Compose removeu containers e volumes do projeto
`aihub-42f79e0e-e7bb-4650-800c-a2d84627440f-fdf513d7a7`, e o wrapper de homologação removeu as tags
temporárias de Argos e Psique da respectiva sessão. As imagens de outros trabalhos foram preservadas.
Não houve commit, push, PR, chamada a modelo pago, acesso a VPS ou publicação.
