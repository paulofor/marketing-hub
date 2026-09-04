# Matriz de homologação — atividade audiovisual PDE de Apolo v1

Data: 2026-09-04

## Objetivo e decisão

Comprovar que Apolo consome a atividade `audiovisual` do processo
`pde-construction-approval` pelo recurso `video-management-service`, respeita o contrato versionado
do produto e devolve uma decisão auditável ao backend sem avançar a cadeia por conta própria.

Foram comparadas três alternativas:

1. o backend pular a atividade opcional: custo baixo, mas acumula decisão funcional no backend e
   elimina a autoria auditável de Apolo;
2. chamar um modelo de IA para interpretar todo produto: aceita ambiguidade, porém adiciona custo e
   pode contrariar o booleano canônico;
3. Apolo consumir a fila especializada e avaliar deterministicamente o contrato: preserva as
   responsabilidades, custa zero quando o vídeo não é necessário e deixa o backend liberar a etapa
   seguinte.

A terceira alternativa foi escolhida por resolver a causa-raiz com menor risco comercial e técnico.

## Gargalo, métrica e regra de decisão

- Gargalo real: tarefa #336 pendente porque nenhum runtime de Apolo consulta a fila BPM atribuída ao
  serviço de vídeo.
- Evidência decisiva: o contrato de Mira declara
  `pdeContext.harness.audiovisualRequired=false`; logs não mostram nenhuma consulta da fila.
- Métrica esperada: tarefa #336 concluída uma única vez, etapa seguinte liberada pelo backend, zero
  tokens, zero custo, zero artefato e zero chamada a provider.
- Continuar: contrato booleano íntegro e callback aceito pelo backend.
- Ajustar: falha de transporte ou divergência entre a fila e o recurso especializado.
- Parar: vídeo obrigatório sem autorização/orçamento, contrato ausente ou qualquer tentativa de
  consumir créditos sem preflight governado.

## Segregação dos testes

- Testes locais usam tarefas e servidores HTTP efêmeros, sem consultar a conta Runway.
- Nenhuma chave externa, crédito, mídia, campanha, publicação ou venda participa da homologação.
- O callback da tarefa produtiva #336 só pode ocorrer depois da suíte local e usa o endpoint interno
  oficial do backend; não substitui imagens ou containers no host.
- Dados de navegador usam automação identificada e não contam como visitante ou evidência comercial.

## Casos obrigatórios

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Contrato contém `audiovisualRequired=false` | Tarefa concluída com decisão `NOT_REQUIRED`, custo zero e nenhum artefato |
| Autoridade | Callback de sucesso é aceito | Somente o backend libera `access`; executor não declara próxima etapa |
| Vídeo obrigatório | Contrato contém `audiovisualRequired=true` | Tarefa bloqueada com autorização/orçamento como ação, sem provider |
| Contrato ausente | Campo canônico não existe ou não é booleano | Tarefa bloqueada como evidência incompleta, sem inferência textual |
| Dado concorrente | Um componente técnico possui formato `AUDIO`, mas o booleano é `false` | Booleano prevalece; nenhuma geração é iniciada |
| Fila | Polling consulta agente, processo, atividade e recurso exatos | No máximo uma tarefa elegível é reservada |
| Idempotência | Poller executa novamente após callback | Não cria tarefa nem callback duplicado |
| STOP | Controle automático do backend está desligado | Fila não é consultada |
| Concorrência | Duas chamadas locais do scheduler se sobrepõem | Guard local permite somente uma reserva |
| Transporte | Backend falha ao consultar ou receber callback | Exceção completa e identificadores são registrados; provider não é chamado |
| Auditoria | Decisão determinística termina ou bloqueia | Entrada integral, regra, fonte, custo, tokens e efeitos externos são persistidos |
| Observabilidade | Endpoint `/api/status` é consultado | Estado do consumidor BPM aparece sem expor segredo |
| Scheduler | Integração longa ocupa outra rotina | Pool mínimo mantém BPM, produção, reconexão e health independentes |
| Arquitetura | Fonte do executor é inspecionada | Contrato `pending/result/failure` e recurso especializado ficam protegidos |
| Desktop | Cadeia de Mira aberta em Chromium | #336 concluída e próxima atividade visível sem erro de console |
| Mobile | Cadeia aberta em iPhone 15 Pro e Pixel 7 | Progresso, decisão e próxima atividade legíveis e utilizáveis por toque |

## Critério de encerramento

A primeira rodada local completa sem defeito encerra a homologação. Se qualquer rodada revelar
defeito e houver correção, a contagem é reiniciada e duas rodadas locais completas e consecutivas
precisam passar após a última correção. A conclusão produtiva da #336 deve ser verificada novamente
na API, no banco e na tela, sem criar uma tarefa sucessora automaticamente.

## Resultado executado

Um defeito de segurança de ambiente foi encontrado durante a primeira validação: o consumidor
especializado ficaria habilitado por padrão também em execuções locais. A configuração foi
corrigida para permanecer desligada por padrão e ser ligada somente pelos Compose versionados de
produção. Como houve correção, a homologação foi reiniciada.

Duas rodadas locais completas e consecutivas passaram depois da correção. Cada rodada confirmou:

- 138 testes do `video-management-service`, sem falhas ou erros;
- 82 testes dos contratos reais de tarefas e atividades BPM no backend, sem falhas ou erros;
- contrato de deploy isolado, incluindo habilitação explícita apenas no Compose de produção;
- imagem Docker do executor construída e iniciada localmente;
- health `UP` e `/api/status` com processo, atividade e recurso especializados corretos;
- consumidor desligado por padrão e habilitável de modo explícito, sem acesso a provider externo.

Na validação operacional, o endpoint especializado reservou exclusivamente a tarefa #336 e o
callback oficial foi aceito com HTTP 204. Banco e API registraram `COMPLETED`, decisão `READY`,
regra `apollo-pde-audiovisual-contract-v1`, custo USD 0, zero chamadas a provider, zero créditos,
zero artefatos e nenhum efeito externo. O backend liberou `access` como quarta atividade, sem o
executor declarar ou disparar a próxima etapa.

A tela publicada foi validada em desktop, iPhone 15 Pro e Pixel 7: mostra 3 de 10 atividades
concluídas, audiovisual concluído e `Implementar acesso privado e continuidade` como atividade
atual, sem erros de página ou console. Nenhuma imagem foi substituída manualmente no host e nenhuma
tarefa duplicada foi criada.
