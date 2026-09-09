# Vega — retomada auditável de Atena

## Diagnóstico de 09/09/2026

- UI do processo 67, atividade 2.1, ciclo #2: tarefa #358 em `IN_PROGRESS` desde 20:00:49 UTC.
- MCP/MySQL: experimento #92; nenhum resultado, prompt, erro ou término persistido.
- MCP/log de Atena: às 20:03:36–37 o envio de `failure` falhou duas vezes por indisponibilidade
  do backend. A atividade já havia produzido um parecer; o worker apagava os arquivos temporários
  antes da confirmação do callback e não retomava envios. Não há telemetria registrada para #358.
- Comparação histórica: #327 concluiu com resultado e prompt; #326 teve falha de transporte.
- O snapshot de #358 contém o ciclo #2, aprendizado conciliado do #91 e hipótese do sucessor.
  O prompt v7 aplica indevidamente a exigência `DOSSIER_READY` a essa entrada de melhoria.
- Actions 34398921266 e 34398921246: bloqueio anterior à carga das imagens por capacidade.
  Psique exigia 10.211 MiB e havia 8.449 MiB, mesmo preservando somente um rollback.
  Os runs verdes 34405494635/656 pularam o deploy; não comprovam a recuperação desses serviços.
- SSH do backend responde. O helper recusou `root@163.245.202.80` como destino não autorizado.
  A liberação foi solicitada enquanto a implementação local continua. Nenhum salto SSH é permitido.

## Alternativas

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Aumentar timeout e repetir POST em memória | Mudança pequena | Perde resultado em reinício; ocupa executor | Rejeitada |
| Reiniciar sempre a análise | Pode recuperar uma tarefa | Repete consumo e pode produzir outro parecer | Rejeitada |
| Persistir trabalho e callback no executor, reenvio com confirmação idempotente | Conserva resposta, auditoria e custo; resiste à indisponibilidade | Exige armazenamento persistente e contratos de repetição | Escolhida |

Para a estratégia do sucessor, foram comparados: iniciar nova descoberta com Argos (adequado a uma
mudança de nicho, mas acrescenta trabalho sem relação com o ajuste aprovado); preparar outro briefing
manual (permite revisão detalhada, mas duplica dados e responsabilidades); e consumir o aprendizado
aprovado do ciclo (preserva evidência e foco na melhoria, com baixo esforço operacional). Foi escolhida
a terceira alternativa, verificando a identidade do produto/experimento e mantendo os gates próprios
do #92. Lacunas factuais continuam explícitas; não se exige nova descoberta para toda iteração.

Para os Actions, reduzir o tamanho das imagens pode diminuir a necessidade futura de espaço, ampliar
o disco resolve capacidade com custo adicional e revisar a ocupação pode recuperar espaço sem alterar
os serviços. A próxima ação técnica depende de inspecionar o host autorizado: não reduzir a reserva
nem remover recursos fora da política para fazer o gate passar.

## Matriz definida antes da implementação e dos testes

| Dimensão | Critério de aceite |
| --- | --- |
| Caminho feliz | Atena consome a fila BPM, recebe aprendizado do produto/experimento correto, registra auditoria e resultado; atividade seguinte fica disponível |
| Falhas de transporte | Backend indisponível antes/depois da inferência e resposta perdida após commit não perdem parecer, não duplicam consumo e não deixam tarefa esquecida |
| Reinício | Callback pendente sobrevive à recriação do executor; inferência interrompida registra causa e permite nova tentativa |
| Validação | Resposta inválida ou insuficiência funcional bloqueia com diagnóstico; não fabricar aprovação |
| Integração | Worker real com backend HTTP local e modelo simulado; backend testado com persistência local; imagem construída pelo Dockerfile do repositório |
| Observabilidade | Prompt antes da inferência, resposta bruta, modelo, tokens disponíveis, tarefa, fonte e tentativas de envio preservados |
| Isolamento e métricas | #91 é aprendizado; #92 mantém identidade, versão e gates próprios; fixtures locais sem tráfego ou receita de produção |
| Navegação | Revisar UI em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; nova tentativa somente para tarefa elegível, próxima atividade comprovada |
| Actions e capacidade | Reproduzir contratos relevantes; preservar imagens em uso, rollback e reserva; capacidade insuficiente permanece um bloqueio real |
| Entrega operacional | Após testes e diff, publicar somente módulos necessários se houver acesso autorizado; reexecutar pela UI e confirmar resultado no banco |

Como houve defeitos confirmados, foram executadas duas rodadas locais completas após as correções.
Ambas terminaram com código de saída zero, sem falhas. O aceite operacional em produção continua
pendente do acesso descrito adiante.

## Resultados locais

| Validação | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend completo e empacotamento | 2.570 testes; 0 falhas/erros; 5 ignorados preexistentes | 2.570 testes; 0 falhas/erros; 5 ignorados preexistentes |
| Arquitetura backend, incluída no total | 92 aprovados | 92 aprovados |
| Atena: suíte completa e empacotamento | 33 aprovados | 33 aprovados |
| Frontend: atividades, comandos e auditoria de falha | 23 aprovados | 23 aprovados |
| Contratos de imagem, workflows e coordenação de agentes | 56 aprovados | 56 aprovados |
| UI local: retomada e próximo comando no mesmo ciclo | Desktop, iPhone 15 Pro e Pixel 7 aprovados | Desktop, iPhone 15 Pro e Pixel 7 aprovados |
| Imagem final de Atena: HTTP 503 e recriação | Mesmo callback; uma inferência; estado removido só após confirmação | Mesmo callback; uma inferência; estado removido só após confirmação |

Antes dessas rodadas, a suíte ampliada identificou dois ajustes necessários: a fixture do teste de
conclusão assíncrona precisava usar a nova consulta com lock e o catálogo de harness precisava incluir
o prompt v8. Foram corrigidos antes da validação final. O teste de persistência confirmou o lock em
duas transações concorrentes. O teste de serviço confirmou resposta perdida após commit, repetição
sem duplicação e recusa de outro agente ou parecer divergente.

A fixture visual foi alinhada ao `executionControl` vigente: é permitido solicitar trabalho futuro,
mas o backend governa quando o executor pode consumi-lo. Os testes do backend protegem essa
predecessão; o navegador verifica a ausência de duplicação da tarefa ativa, a retomada elegível,
o estado concluído e a correlação do próximo comando com produto #4, processo 67 e ciclo #2.

Também passaram o build do frontend, a navegação comum de auditoria em três perfis, a formatação
Java dos arquivos alterados, a sintaxe dos scripts, o catálogo JSON e `git diff --check`.
Os YAML alterados foram analisados pelo parser do Prettier. A imagem do backend também foi
construída pelo seu Dockerfile, usando o JAR aprovado pela suíte completa; o build e a limpeza
terminaram com sucesso (`/tmp/vega358-backend-image.log`). Isso comprova o empacotamento e não
substitui a verificação operacional conjunta após publicar. Nenhuma imagem foi enviada ao host.

Comandos reproduzíveis, a partir dos respectivos diretórios:

```sh
# backend/ads-service e experiment-strategist-worker
mvn -q package

# frontend
npm test -- --run src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/ProductProcessActivityExecutionPanel.test.tsx src/components/AgentTaskFailureAudit.test.tsx

# Raiz, com o frontend local já iniciado em 127.0.0.1:4173
FRONTEND_BASE_URL=http://127.0.0.1:4173 node frontend/e2e/vega-atena-recovery-responsive.mjs
node --test scripts/test-agent-image-bundle.mjs scripts/coordinate-agent-deployment.test.mjs scripts/test-agent-image-workflows.mjs
AIHUB_HOMOLOGATION_SESSION=vega358-validacao ATENA_BPM_COMPOSE_PROJECT=aihub-8f65111b-4b7a-4b6a-ba32-58d94814420a-4388da8046 bash scripts/run-docker-homologation.sh bash experiment-strategist-worker/scripts/validate-bpm-image-local.sh
```

Evidências desta sandbox: `/tmp/vega358-matrix-{1,2}-{backend,worker,frontend,actions,browser,image}.log`
e `-counts.json`; snapshots `/tmp/vega358-recovery-{desktop,iPhone-15-Pro,Pixel-7}.png` e
`/tmp/vega358-production-final.png`. As fixtures, o teste de imagem e os contratos de regressão estão
no repositório; os logs temporários não são dados comerciais nem artefatos produtivos.

## Limites e continuidade operacional

- O teste usa modelo simulado, sem credenciais de agentes e sem inferência externa. Comprova
  contrato, persistência, envio, reenvio, isolamento e navegação; não comprova o mérito de um parecer
  real da Atena nem a conclusão produtiva da atividade.
- O teste de concorrência usa H2 real com transações e `PESSIMISTIC_WRITE`; não houve alteração de
  schema ou changelog. Não declarar homologação MySQL 5.7 desta correção.
- A imagem de Atena é construída pelo Dockerfile versionado. O teste passa por dois containers
  distintos, com rede externa desabilitada e o mesmo volume persistente: o primeiro recebe HTTP 503;
  o segundo confirma o mesmo parecer, mantendo apenas uma chamada ao modelo simulado.
- A engine Docker não compartilha os caminhos da sandbox por bind mount. A fixture é transmitida
  por stdin para volume exclusivo de teste. Containers, volumes e tags temporárias da sessão são
  removidos pelo Compose e pelo wrapper canônico.
- O resultado antigo de #358 foi apagado pelo worker anterior. O armazenamento novo protege
  execuções futuras, mas não recria esse parecer. A recuperação operacional deve preservar #358
  como tentativa interrompida, verificar que o processo antigo não está ativo, cancelar a tentativa
  pelo comando oficial e solicitar nova execução na atividade 2.1 pela UI após atualizar o executor.
- Só uma conclusão real registrada pelo backend libera a passagem produtiva para Plutus. Não
  marcar #358 como concluída, enviar parecer fabricado nem iniciar o #92 em `RUNNING` para simular
  progresso. A autorização atual cobre a recuperação dessa atividade; gates comerciais permanecem.
- Nenhuma publicação foi executada: o host da Atena `root@163.245.202.80` foi recusado pelo helper
  com `destino não autorizado` (saída 77). O host do backend responde, mas uma atualização parcial
  não resolve a falta do executor. A liberação do destino foi solicitada ao usuário; não houve salto
  de rede, alteração da allowlist ou uso alternativo de credenciais.
- A #358 continuava `IN_PROGRESS`, sem resultado ou erro, na consulta MCP feita durante a revisão
  final. O experimento #92 não foi ativado. As falhas de capacidade dos Actions continuam pendentes
  de inspeção do host; as verificações locais dos contratos não equivalem a um deploy aprovado.
