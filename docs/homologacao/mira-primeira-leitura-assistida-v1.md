# Mira — primeira leitura privada assistida

Data: 2026-09-05.

## Evidência e decisão

A tela de `product:10`, processo `68`, mostra `privateReading1` disponível e 5/10 atividades
concluídas. O MCP confirmou somente cinco eventos `QA_INTERNAL` em `pde_funnel_event` e nenhum
`PRIVATE_READING`. A aceitação do protótipo aponta para
`https://v7.clubemusa.com.br/mira-private`, versão `mira-private-v1`.

O formulário exigia responsável, código manual, cinco respostas, observação, referência de evidência
e duas confirmações; não mostrava o acesso aceito. O handler confiava nos booleanos enviados pela
tela. A sessão real do protótipo já conhece o participante e os eventos, mas não havia contrato para
importá-los na atividade.

Alternativas: (1) guia e preenchimento manual, baixo esforço mas mantém transcrição; (2) assistente
com acesso visível e importação de evidência, esforço moderado e menor risco de registro incorreto;
(3) callback que conclui a atividade sem revisão, maior acoplamento e perda da confirmação humana.
Escolhida a segunda: manter a confirmação humana, buscar os sinais no backend PDE e revalidá-los
antes da gravação BPM. Não alterar critérios comerciais nem transformar QA em cliente.

## Matriz definida antes da execução

| Dimensão | Cenário | Critério |
|---|---|---|
| Acesso | Link aceito visível; convite individual por fragmento ou campo protegido | Nenhum convite é exposto pelo endpoint administrativo; sem segredo em URL HTTP, logs ou relatório |
| Consentimento | Convite válido com/sem consentimento | Sem consentimento não cria sessão/evento; confirmação humana nunca pré-marcada |
| Identidade | Primeira, segunda e QA; troca de convite no mesmo navegador | Sessões segregadas; QA e outra versão/produto recusados como prova |
| Jornada feliz | Entrada, resultado, retomada, uso, preferência e intenção simulada | Evidência real importada; cinco sinais; atividade concluída somente após confirmação humana |
| Resposta negativa | Preferência ou intenção negativa | Leitura encerrada e preservada; gate bloqueado sem transformar negativa em positiva |
| Integridade | Sinais forjados, evidência desatualizada, integração indisponível | Backend reconsulta prova e recusa gravação; sem fallback para declaração manual de Mira |
| Continuidade | Reinício e atualização de entrada após leitura encerrada | Mesmo resultado e prova; sem aproveitar sinais antigos para entrada nova |
| Observabilidade | Eventos, consentimento, término e referência auditável | Relatório sem segredo; teste não conta como venda, receita, entrega ou aquisição |
| Integração local | PDE, API administrativa, interface e MySQL 5.7 com dados sintéticos | Tráfego local; sem SMTP real, IA paga, checkout real ou banco produtivo |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Sem overflow; botões assíncronos com estado ocupado; erros legíveis e tentativa possível |
| Regressão | Testes Java/React afetados, builds e contratos HTTP | Testes relevantes passam e diff revisado |

Se a primeira rodada revelar defeito, corrigir a causa e executar duas rodadas completas consecutivas
sem falhas após a última correção. A execução automatizada valida software com dados sintéticos;
não substitui a leitura humana produtiva.

## Limites operacionais constatados

Atualização em 2026-09-05: o usuário cadastrou a chave e o SSH passou a autenticar no host
`163.245.200.7`. A ausência de acesso descrita abaixo pertence à execução anterior e foi resolvida.
A investigação seguinte encontrou versões incompatíveis na consulta de leitura, sem consumo dos
convites humanos. Ver [identidade e continuidade](mira-identidade-participante-v1.md).

SSH para `root@163.245.200.7` respondeu `Permission denied (publickey,password)`. A sandbox não
possui chave em `~/.ssh` nem agente SSH autenticado. O MCP permite inventário, logs e operações
restritas, sem transferência de imagem. A publicação autorizada depende de credencial operacional
disponibilizada com segurança. O convite humano permanece no cofre existente; não é exposto por
endpoint administrativo sem autenticação.

## Ajustes comprovados na homologação

- A navegação por fragmento mantinha o React e a sessão anterior ao trocar o convite. O novo convite
  agora reinicializa a página e mantém o segredo fora das URLs HTTP. A troca é testada na mesma aba.
- Após interrupção entre salvar a entrada e gerar o resultado, a retomada precisava repor os campos
  persistidos. A jornada agora simula essa falha, recarrega e conclui sem redigitar os produtos.
- O endpoint interno precisava declarar o nome do parâmetro de rota explicitamente, pois o build
  PDE não preserva nomes de parâmetros. Há teste MockMvc e chamada HTTP real.
- A sessão automática do wrapper Docker continha maiúsculas e era recusada como nome de imagem.
  Geração e validação agora usam minúsculas; o teste cobre o caminho padrão sem variável explícita.
- O Actionlint global não reconhecia `queue: max` já presente no `HEAD`. Comparar com o arquivo
  anterior confirmou que não era erro introduzido pelo workflow. O runner usa o validador canônico
  `scripts/run-actionlint.sh`, com a revisão e política ShellCheck versionadas do projeto.

As imagens da homologação são construídas pelos Dockerfiles em
`infra/testing/mira-private-reading`. O PDE e seu MySQL são reais; o backend principal exercita
controller, cliente HTTP autenticado, handler e executor BPM reais com repositories/predecessor
simulados. Nenhum banco, convite humano ou endpoint de escrita produtivo participa da matriz.

## Resultados

Duas rodadas locais completas e consecutivas, `final-1` e `final-2`, passaram sem falhas após a
última correção. Cada rodada executou:

| Verificação | Resultado por rodada |
|---|---|
| Backend principal | 2.349 testes catalogados: 2.345 aprovados e quatro condicionais ignorados; sem falhas ou erros |
| Backend PDE | 160 testes aprovados, sem ignorados |
| Frontend administrativo | 485 testes aprovados em 142 arquivos |
| Builds | TypeScript/Vite dos dois frontends, JAR PDE e duas imagens Docker PDE construídas localmente |
| Jornada existente | Três testes de regressão do protótipo, em desktop, iPhone e Pixel |
| Nova integração | Nos três dispositivos: acesso, consentimento, retomada após falha, leitura positiva, negativa, troca de convite, QA e decisão BPM |
| Persistência | MySQL 5.7: oito eventos `PRIVATE_READING` sintéticos e cinco `QA_INTERNAL`, sem duplicação após retomadas |
| Contratos | Actionlint canônico, Spotless dos arquivos alterados no backend principal, ShellCheck, fronteira de API PDE e contratos de deploy/limpeza |
| Encerramento | Compose removido com volumes e órfãos; as duas imagens temporárias removidas pelo wrapper |

Os quatro testes ignorados do backend principal são condicionais preexistentes e não foram
desabilitados por esta alteração. As rotas e os critérios de Mira foram exercitados sem skips.

Logs, contagens e capturas ficam em `tmp/mira-reading-final-1/` e `tmp/mira-reading-final-2/`, fora
do Git. A revisão visual confirmou legibilidade e ausência de overflow no celular. O diff foi
revisado e passou em `git diff --check`. Não houve commit, push, PR ou deploy.

Na reconsulta produtiva, `prototypeAcceptance` permanece `COMPLETED`; `privateReading1` e
`privateReading2` permanecem `NOT_STARTED`. O MCP continua retornando somente os cinco eventos QA.
A primeira leitura produtiva depende de uso e opinião de uma pessoa real, aderente ao público e
consentida; nenhuma aprovação humana foi registrada pelo teste automatizado.

## Continuação de usabilidade — 05/09/2026

A produção já exibe o assistente e não pede os campos manuais antigos, mas o botão principal abre
somente a URL base. O texto “Abrir protótipo de Mira” fazia parecer que esse link continha o acesso,
embora o segredo permaneça corretamente fora da API administrativa.

Foram comparados: expor o token estático no Hub, o que reduz passos mas quebra privacidade; criar uma
nova emissão autenticada de convites, solução melhor para escala mas sem autenticação administrativa
segura no escopo atual; e tornar explícita a entrega do arquivo já existente, sem transcrever código.
A terceira opção foi escolhida agora. A atividade começa pelo arquivo individual, explica que ele
preenche o acesso localmente e chama a URL base de verificação, não de convite.

O convite da primeira participante foi conferido apenas como arquivo `0600`, destino HTTPS e
fragmento presente, sem abrir a sessão humana. O token exclusivo de QA abriu a versão publicada,
removeu o fragmento, manteve `noindex/no-referrer` e mostrou a identidade pública “Sua rotina,
organizada com calma”, sem erro de console. A produção continua com exatamente cinco eventos
`QA_INTERNAL`; `privateReading1` permanece `NOT_STARTED`. Somente a participante pode consentir,
usar seus próprios produtos e declarar preferência/intenção; QA ou o modelo não podem fazê-lo.

A continuação foi revalidada nas rodadas `final-1` e `final-2` junto do contrato de Vega. Em cada
rodada, o backend teve 2.345 testes aprovados e quatro condicionais ignorados; também passaram
160 testes do PDE e 485 do frontend,
MySQL 5.7, builds, imagens temporárias e as 12 jornadas locais. Os containers, volumes e três
imagens temporárias foram removidos ao final.
