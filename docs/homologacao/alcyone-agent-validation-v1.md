# Homologação de Alcyone sem piloto humano — v1

Data da matriz: 2026-09-30.

## Escopo e limites

Alcyone #11 deve sair do contrato humano legado, concluir a construção e atravessar a homologação
multiagente antes de qualquer comunicação comercial. O fluxo não recruta, convida, entrevista,
solicita opinião, representa participante fictícia, cobra, publica, cria campanha ou autoriza mídia.
Agentes comprovam prontidão; o mercado voluntário continuará sendo a única prova de demanda.

## Matriz definida antes da rodada local

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Contrato | `MARKET_STRATEGY_V4` gera `PDE_AGENT_VALIDATION_V1` | V3 incompleto, cenário/dispositivo extra, flag comercial ou humana bloqueiam | Testes de prompt, schema, consumidor e hook |
| Migração | #11, run #26, instância #414 e tarefa #534 convergem para `product:11@agent-validation-v1` | Claim concorrente antes do deploy conserva estado, mas não a referência humana | MySQL 5.7 físico com #534 `IN_PROGRESS`, JSON válido e precondições |
| Dédalo | polling reserva #534, carrega contexto e produz jornada | timeout libera novo ciclo; troca de container reentrega uma vez somente lease sem saída/custo | teste HTTP local, heartbeat por `taskId`, logs e tarefa auditável |
| Visual estático | harness entrega três fixtures de 1024x1024 sem rede | imagem do resultado não pode acionar Estúdio, vídeo ou orçamento | contrato `PDE_STATIC_RESULT_FIXTURES_V1`, zero provider e #566 retomável |
| Construção | jornada, componentes, audiovisual, acesso e instrumentação pertencem à mesma versão | contrato ausente, artefato incompatível ou efeito externo indevido bloqueiam | resultados/artefatos persistidos pelo backend |
| Homologação | técnica + `ADHERENT`, `RECOVERY`, `SAFETY` + Têmis + gate | erro recuperável, pedido fora de escopo, privacidade, custo ou versão divergente bloqueiam | pareceres separados, causa-raiz e próxima correção |
| Observabilidade | request/response, modelo, tokens, custo, horários e status ficam vinculados à execução | falha não pode desaparecer em thread presa ou apenas em log | telemetria e relatório funcional persistido |
| Métricas | resultado em até 600 s e custo técnico de até R$ 24 por pacote | QA interno não entra em visita, conversão, checkout, venda, receita ou satisfação | `trafficClass=AGENT_VALIDATION`, `mh_internal_test`, flags comerciais falsas |
| Segregação | dados sintéticos ficam no produto #11 e na referência v1 | nenhuma evidência de Mira, Vega ou tráfego real pode ser reutilizada | IDs, fonte, versão e hashes coerentes |
| Navegadores | Chromium desktop 1440, iPhone 15 Pro e Pixel 7 | entrada, resultado, retomada, erro, limite e CTA simulado continuam acessíveis | screenshots e assertions Playwright por perfil |

## Alternativas e escolha

1. Encerrar Alcyone: custo baixo, mas elimina a oportunidade sem testar o mecanismo.
2. Migrar apenas o registro #11: desbloqueia agora, porém o próximo produto volta a nascer humano.
3. Corrigir a autoridade de criação, migrar a execução aberta e limitar o polling: maior esforço
   inicial, melhor aderência ao cânone e menor recorrência. Esta é a opção adotada.

## Resultado

A rodada local foi aprovada em 30/09/2026:

- backend: 3.715 testes, sem erro ou falha; 23 cenários explicitamente ignorados pela própria
  suíte;
- Atena: 44 testes, sem erro, falha ou cenário ignorado;
- Dédalo: 81 testes, sem erro, falha ou cenário ignorado, incluindo servidor local que aceita a
  conexão e retém a resposta até o timeout liberar o polling, auditoria anterior ao modelo e
  heartbeat segregado por tarefa;
- MySQL 5.7 físico: primeira aplicação, idempotência, rollback não regressivo e reaplicação
  aprovados para Processo 2 v10, cadeia v24, produto #11, run #26, instância #414 e tarefa #534 já
  em `IN_PROGRESS`, além de três fontes públicas sem duplicidade;
- validações estáticas Liquibase, `bash -n`, ShellCheck, JSON, Spotless e revisão de whitespace
  aprovadas.

A primeira suíte integral detectou que o catálogo central de Atena ainda não declarava os dois
artefatos v10. O manifesto foi corrigido e o teste de cobertura foi repetido com sucesso; publicar
somente o worker deixaria a auditoria incompleta. A reserva produtiva de #534 e os cenários visuais
da versão gerada dependem do deploy e serão comprovados operacionalmente sem transformar QA em
visita, checkout, venda ou satisfação.

Na primeira publicação, a imagem antiga reservou #534 vinte segundos antes de ser substituída. A
rodada corretiva passou a persistir a auditoria antes do modelo, emitir telemetria `DEDALO_BPM` e
recuperar apenas uma lease sem saída ou consumo após dois minutos. Heartbeat recente permanece
protegido; output observado ou uma segunda interrupção bloqueiam a tarefa para impedir custo
duplicado. A fixture MySQL reproduz #534 e #414 já `IN_PROGRESS`, garantindo que a migração não
dependa da ordem entre claim e deploy.

A retomada produtiva concluiu #534 e #565 e chegou corretamente ao gate audiovisual #566. A tarefa
bloqueou antes de provider, crédito ou custo porque o contrato confundia as três imagens estáticas
do resultado com vídeo/áudio de Apolo. Foram comparados gasto governado no Estúdio, dispensa total do
visual e separação das responsabilidades; a terceira alternativa preserva o valor do produto sem
criar gasto. Alcyone passa a exigir três fixtures estáticas de 1024x1024 no harness determinístico e
declara vídeo/áudio não requerido, sem transformar QA em evidência humana ou comercial.

| Alternativa visual | Benefício | Risco | Esforço/custo | Aderência |
| --- | --- | --- | --- | --- |
| Produzir no Estúdio | acabamento publicável | gasto e autorização sem necessidade funcional | alto e variável | baixa para QA interno |
| Remover as imagens | desbloqueio imediato | protótipo deixa de demonstrar o mecanismo visual | baixo | baixa para valor do produto |
| Fixture estática no harness | preserva resultado, auditoria e responsividade | exige contrato explícito entre imagem e audiovisual | baixo, determinístico e sem provider | alta; opção adotada |

Após a correção, a regressão local confirmou 3.715 testes de backend e 83 do worker sem erro ou
falha, Spotless, ShellCheck, validação estática de Liquibase e MySQL 5.7 físico em aplicação,
idempotência, rollback e reaplicação. O prompt de arquitetura também elimina o legado de duas
leituras humanas e deixa explícito que sinais sintéticos não representam visita, preferência,
checkout, venda ou validação de mercado.

## Superfície executável e rodada local integral

O avanço de #568 revelou que jornada e componentes documentados não constituíam uma experiência
executável. Foram comparadas três alternativas:

| Alternativa | Benefício | Risco | Esforço/custo | Decisão |
| --- | --- | --- | --- | --- |
| Reutilizar Mira | entrega rápida | mistura produto, versão, evidência e aprendizagem | baixo | rejeitada |
| Aceitar os documentos de Dédalo | não exige software | cria prontidão fictícia e não testa o mecanismo | baixo | rejeitada |
| Criar superfície Alcyone isolada sobre o motor PDE neutro | identidade, retomada, eventos e rollback próprios | exige imagem, proxy, DNS e contrato novos | moderado e determinístico | adotada |

A versão `alcyone-private-v1` possui imagem, container, porta, alvo de deploy e domínio próprios.
O backend persiste sessões e eventos de forma atômica; a interface entrega três visuais gerados por
código, checkout apenas simulado e bloqueio explícito de foto corporal, compra de roupa e julgamento
do corpo. A página sem sessão permanece fechada para uso e informa que não vende nem recruta.

A rodada real do harness em Docker executou a mesma imagem do customer-agent-worker e o mesmo
frontend/backend da topologia local. Resultado:

- decisão `APPROVED`, duração total de 8 segundos;
- 9/9 combinações aprovadas: `ADHERENT`, `RECOVERY` e `SAFETY` em Chromium 1440×900, iPhone 15 Pro e
  Pixel 7;
- 9 screenshots PNG, responsividade, acessibilidade básica, privacidade e retomada aprovadas;
- resultado aderente em 1 segundo, abaixo do teto de 600 segundos;
- zero chamadas de provider, custo externo zero, R$ 0,00 de mídia, pagamento/publicação/campanha
  desativados e nenhuma alegação humana ou comercial;
- saída do harness com SHA-256
  `439ec9bcf83ef545dbdefeeac848545754402076036d775a82cee2ae4809aefa`.

A rodada também encontrou uma incompatibilidade histórica: o gate do backend ainda exigia cinco
registros técnicos, enquanto Alcyone produz a matriz completa de nove. O contrato agora preserva as
provas legadas de cinco itens, mas exige exatamente os nove pares únicos, fixtures estáticas, zero
provider, segregação e efeitos externos nulos sempre que o envelope estendido estiver presente.
Teste unitário recusa contagem correta com par cenário/dispositivo duplicado.

Na repetição limpa da topologia, os dois primeiros cenários receberam `502` porque o E2E aguardava
somente o processo do Nginx iniciar, não a saúde do backend. O Compose agora exige backend saudável,
depois frontend saudável e somente então inicia o harness. Um teste de contrato fixa essa ordem. A
reexecução limpa encerrou com 9/9 casos aprovados em 9,8 segundos e removeu containers, rede e volume
temporários ao final.

## Fechamento da validação local

Antes da publicação, a solução integral foi repetida com os seguintes resultados:

- backend principal: 3.716 testes, zero falha ou erro e 23 cenários ignorados pela própria suíte;
- backend PDE: 198 testes, zero falha ou erro;
- customer-agent-worker: 145 testes Java, zero falha ou erro, 2 cenários ignorados, e 22/22 testes
  JavaScript aprovados;
- topologia Docker de Alcyone: 9/9 percursos Playwright aprovados na mesma imagem publicada, cobrindo
  caminho aderente, retomada e segurança em desktop, iPhone 15 Pro e Pixel 7;
- regressões comerciais: 9 percursos privados de Mira e 18 percursos comerciais de Mira aprovados,
  além dos contratos de isolamento e proxy de Vega;
- Swagger, JSON, hashes das evidências comerciais, Spotless, `bash -n`, ShellCheck, contratos de
  release, roteamento e artefatos aprovados.

O CI passa a executar a topologia local completa antes de construir a imagem publicável. A saúde do
backend e do frontend é uma precondição do harness, e a limpeza de containers, rede e volume acontece
mesmo quando um percurso falha. Isso impede que prontidão de processo seja inferida apenas de build,
documento ou processo Nginx vivo.

A publicação privada também fica vinculada a um manifesto imutável de Alcyone. Backend PDE,
frontend, customer-agent-worker, proxy público e certificado são recuperados pela mesma revisão
integrada. O escopo dedicado `alcyone-proxy` aceita somente o host PDE, desativa os certificados de
outros produtos e exige o job produtivo `deploy`; build verde sem troca do proxy não comprova
entrega. Essa proteção não autoriza tráfego, checkout real, campanha ou mídia.
