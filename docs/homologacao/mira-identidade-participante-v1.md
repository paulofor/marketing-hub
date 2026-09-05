# Mira — identidade da participante e continuidade da leitura

Data: 2026-09-05. Produto interno `10`/Mira; primeira leitura privada.

## Evidência antes da alteração

O SSH autenticou no host `163.245.200.7` com a chave já cadastrada pelo usuário, sem troca de
credencial. O MCP confirmou backend principal `aae31df72a42` e PDE `c7e542ba793c`. A página
produtiva apresentava `Mira · validação privada` e não tinha o campo de convite já presente no
repositório. O catálogo mantém o nome descritivo “Orientação digital individualizada de rotina
para pele madura · PDE planejado #36”, inadequado para copiar literalmente na experiência.
Há somente cinco eventos `QA_INTERNAL`; nenhuma leitura humana. A consulta não alterou sessões.
O comando `GET /api/products/10/private-readings/privateReading1` retornou 500. O log do
`PdePrivateReadingClient` e a chamada autenticada ao PDE no próprio host confirmaram 404 em
`/api/pde/mira/private/v1/internal/readings/1`: o backend principal já usa o contrato novo,
mas o PDE publicado ainda não o implementa. O formulário também ocultava a URL aceita quando
essa consulta falhava, acoplando indevidamente acesso e disponibilidade de prova.

## Decisão

| Alternativa | Benefício | Risco | Esforço e aderência |
|---|---|---|---|
| Manter Mira na experiência | Nenhuma migração | Expõe codinome e contraria a identidade do catálogo | Baixo esforço; não atende |
| Criar uma marca comercial nova | Identidade própria | Exige decisão de posicionamento ainda não tomada | Maior esforço; prematuro |
| Usar benefício aprovado e manter identificadores internos | Clareza para a pessoa e histórico íntegro | Marca definitiva permanece por decidir | Baixo esforço; atende a leitura privada |

Escolhida a terceira. Preservar acesso individual, consentimento, respostas negativas,
importação automática de sinais e segregação de QA. Uma rodada automatizada nunca confirma
participação, preferência ou compra de pessoa real.

Para a falha de consulta, compararam-se repetição do comando (não corrige 404), fallback para
declaração manual (perde prova confiável) e acesso aceito independente da prova (preserva
usabilidade com registro bloqueado). Escolhida a terceira, sem dispensar a atualização coordenada
do PDE e sem tolerância a uma prova de produto/versão diferente. A matriz para o container PDE
local, valida o aviso e o acesso em três dispositivos, reinicia o container e percorre a jornada.

## Matriz definida antes dos testes

| Dimensão | Validação | Critério |
|---|---|---|
| Identidade | Entrada, aba, resultado, erros e encerramento | Sem codinome, versão técnica ou instruções do Marketing Hub visíveis à participante |
| Acesso | Convite no fragmento ou campo protegido; ausência e código inválido | Sem segredo em URL HTTP, captura ou log; instruções claras e consentimento obrigatório |
| Jornada feliz | Entrada, resultado, retomada, preferência e intenção simulada | Resultado persistido e cinco sinais com origem correta |
| Falhas | Integração interrompida, resposta negativa, outra participante e QA | Evidência íntegra, tentativa possível e nenhum sucesso humano inferido |
| Consulta indisponível | Container PDE parado de propósito | Acesso aceito permanece visível; nenhum sinal vira positivo ou negativo; comando BPM recusado |
| Integração | PDE e MySQL 5.7 reais locais; API administrativa com doubles de persistência | Importação autenticada e decisão BPM exercitadas; sem dados produtivos |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Jornada legível, sem overflow, controles acessíveis |
| Métricas | Contagem por origem; ausência de cobrança e mídia | Oito eventos privados sintéticos e cinco QA na matriz existente; zero efeito comercial |
| Entrega | Build versionado e revisão do diff | Não usar deploy como teste; publicação de código depende do fluxo de PR solicitado pelo usuário |

Reutilizar a matriz e o runner de `mira-primeira-leitura-assistida-v1.md`. Se aparecer um defeito,
corrigir a causa e executar duas rodadas completas consecutivas após a última correção.

## Ajuste encontrado na revisão visual

A rodada `identidade-final-1` passou nos testes e na integração, mas a inspeção da captura mobile
mostrou o asterisco obrigatório separado do rótulo do convite. O `label` usa grid e os dois itens
eram filhos independentes. O texto e seu asterisco passam a formar uma única linha. Após esse
ajuste, a contagem das duas rodadas completas recomeça; aquela rodada não encerra a homologação.

## Resultado após a última correção

`identidade-verificada-1` e `identidade-verificada-2` concluíram consecutivamente com o mesmo diff,
sem falhas ou erros. Cada rodada aprovou:

| Verificação | Resultado por rodada |
|---|---|
| Backend principal | 2.328 testes aprovados e três ignorados preexistentes fora do fluxo Mira |
| Backend PDE | 160 testes aprovados, sem ignorados |
| Interface administrativa | 482 testes aprovados em 141 arquivos |
| Builds e contratos | Dois frontends, JAR PDE, duas imagens temporárias, fronteira de API, Actionlint, Spotless e ShellCheck |
| Interrupção real local | PDE parado e retomado; link aceito disponível e registro bloqueado em desktop, iPhone 15 Pro e Pixel 7 |
| Jornadas | Consentimento, retomada, resposta positiva e negativa, troca de convite, QA e decisão BPM nos três dispositivos |
| Persistência MySQL 5.7 | Oito eventos privados sintéticos e cinco QA, sem duplicação ou efeito comercial |
| Revisão visual | Identidade da participante preservada e campo obrigatório legível no celular |
| Limpeza | Containers, rede e volumes exclusivos removidos; duas referências de imagem temporária removidas em cada rodada |

Os três testes ignorados preexistentes são a comparação literal de HTML GeraLanding, o cenário
optativo de MySQL para descoberta Meta e o arquivo externo de pacote criativo. Nenhum teste de
Mira foi ignorado. Evidências locais em `tmp/mira-reading-identidade-verificada-1/` e
`tmp/mira-reading-identidade-verificada-2/`, fora do Git. A rodada preparatória anterior foi
interrompida pela ausência de dependências npm, resolvida com `npm ci` nos dois frontends antes
das rodadas completas.

O runner exige `MIRA_DOCKER_PROJECT`; foi usado exclusivamente
`aihub-be2a2a1c-cc44-4768-bde6-c078d2d314b0-e803968038`, sem reutilizar o projeto de outra sessão.

Foi preparado um arquivo de convite individual para entregar ao usuário, fora do repositório,
com permissão `0600`, sem imprimir o código. O arquivo não inicia sessão automaticamente e foi
conferido nos três dispositivos sem chamadas externas. Orienta aguardar a publicação da atualização
e reservar o acesso à primeira participante real. Os convites humanos não foram usados pelo QA.

## Estado produtivo e próximo marco

A reconsulta final pelo MCP e pelo backend confirmou `5/10`, `privateReading1=NOT_STARTED` e
somente cinco eventos `QA_INTERNAL`. Não houve leitura humana, venda, cobrança, mídia, commit,
push, PR ou deploy nesta execução. A publicação do código segue a regra vigente de PR pelo usuário;
o SSH disponível foi utilizado para diagnóstico e preparação privada do acesso.

A causa operacional do 404 continua exigindo atualizar o PDE junto ao contrato administrativo:
o workflow PDE só publica por `workflow_dispatch`, com `frontend_version=v7` para esta superfície.
O novo estado de indisponibilidade melhora a interface, mas não substitui esse alinhamento.
Depois da publicação, a participante deve consentir, usar seus produtos e responder por conta
própria. O operador importa a prova e confirma sua observação pela tela. Continuar com prova real
e íntegra; ajustar diante de dificuldade ou resposta negativa; interromper diante de falha de
privacidade, integridade ou efeito financeiro inesperado. QA nunca conclui a atividade humana.
