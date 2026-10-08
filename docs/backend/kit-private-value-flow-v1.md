# Kit privado e fluidez do produto — contrato v1

O backend principal governa sessões, reservas, filas, aceite e decisões. O compositor
em `lead-portal-payments-service` consome a fila; Psique consome sua tarefa e usa o
harness privado. Nenhum executor decide a próxima atividade ou consulta o banco.

| Contrato | Acesso e resultado |
| --- | --- |
| `/api/pde/kit/private/v1/admin/sessions` POST | Administração existente; emite acesso de QA após conferir arquitetura e contratos da mesma versão |
| `/api/pde/kit/private/v1/internal/sessions` POST | `X-PDE-Internal-Token`; sessão sintética para homologação |
| `/session` GET, `/input` PUT, `/events` POST, `/download` GET, `/assets/{name}` GET | Relativos ao prefixo de kits; `X-Kit-Session` opaco, sem segredo em URL |
| `/stage-executions/pending` GET | `X-Payments-Auth`; IDs previamente reservados pelo backend |
| `/stage-executions/{id}/claim`, `/result`, `/failure` POST | Mesma credencial; claim idempotente e callback vinculado ao arquivo/hash |
| `/internal/sessions/{id}` DELETE, `/internal/cycles/{id}/report` GET | Credencial PDE; revogação e relatório sem segredos nem bytes do ZIP |
| `/api/business-process-chains/learning-cycles/v1/products/{productId}/{cycleId}/contribution-target` POST | Escolha explícita, revisão esperada e chave idempotente; meta disponibilizada no contexto das próximas tarefas |

A interface privada versionada fica em `/api/pde/kit/private/v1/prototype` e utiliza
as rotas do próprio backend, também pelo proxy público existente `/mh-api`. O frontend
administrativo passa o acesso por `postMessage`, conferindo origem e janela. Todas as
respostas privadas são `no-store`. O banco persiste somente hash do acesso.

Entradas de QA aceitam sete campos e consentimento simulado, e-mail `@sandbox.local`
e telefone fictício `00000000000`. Reserva: até seis composições por ciclo, duas por
fixture, sem renovação por dispositivo. Sessões expiram em 30 dias; ZIP aceito admite
dez transferências. O contrato privado `PDE_PRIVATE_KIT_PACKAGE_V2` exige exatamente
36 arquivos funcionais: `posts/post-01.png` a `post-10.png` (1080×1080),
`stories/story-01.png` a `story-10.png` (1080×1920), dez
`legendas/legenda-NN.txt`, cinco `mensagens/mensagem-NN.txt` e
`calendario/calendario-7-dias.txt`. Textos usam UTF-8; cada um dos sete dias referencia
post, story, legenda e mensagem existentes. Auditoria, manifesto e instruções técnicas
ficam fora do ZIP final. O backend valida nomes, dimensões, textos, referências,
limites e hashes. `providerCalls` deve ser zero.

O registro de prova do runtime `DETERMINISTIC_PRIVATE_KIT_V1` exige URL/perfil suportados
e pacote `READY` da identidade exata, com os 36 arquivos do formato atual. Essa conferência não substitui a homologação técnica,
as três revisões independentes de Psique ou Têmis. Contratos legados preservam seus gates.
O compositor conserva claim e arquivo para replay após falha de callback, sem nova composição.

Pacotes comerciais legados de 24 arquivos e provas anteriores permanecem imutáveis e
legíveis. Antes da exposição comercial, uma prova privada do formato antigo pode receber
um novo evento `REGISTER_PROTOTYPE`, após conferir o pacote corrigido da mesma identidade.
Isso cumpre os contratos de entrega já aprovados, sem mudar a hipótese, versão comercial,
janela ou autorização. O digest de novas composições inclui a versão do formato; a reserva
continua limitada a duas por fixture e seis por ciclo. Não ampliar cotas nem reutilizar
uma prova do formato anterior para aprovar a correção.

O harness confere o hash do ZIP realmente baixado e os nomes dos arquivos do manifesto.
Cada cenário associa `screenshotEvidenceKeys` aos recibos de upload; antes do parecer,
o consumidor os transforma em `screenshotEvidenceIds` persistidos. Ausência do vínculo,
pacote incompatível ou identidade divergente bloqueia antes do modelo. A homologação
técnica corrigida não substitui as três revisões de Psique nem a revisão de Têmis.

`valueFlow`, exposto nas consultas existentes de ciclos e contexto do processo, contém
situação, impedimento, responsáveis, execução ativa, decisão, critério de aceite, tempo,
pacotes aceitos e entregas das tarefas. Cenários financeiros permanecem hipóteses com fonte;
contribuição e compras somente aparecem com medição conciliada, válida e segregada. Ausência
de fonte é desconhecida. Sessões e eventos de QA nunca são prova comercial.

Validação e limites: [matriz e evidências locais](../homologacao/fluidez-produtos-capella-v1.md).
Regressão da passagem completa: [conclusão do Processo 3](../homologacao/capella-conclusao-processo3-v1.md).
