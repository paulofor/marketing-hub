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
dez transferências. A resposta deve conter vinte PNGs, textos e calendário associados;
o backend valida nomes, dimensões, limites e hash. `providerCalls` deve ser zero.

O registro de prova do runtime `DETERMINISTIC_PRIVATE_KIT_V1` exige URL/perfil suportados
e pacote `READY` da identidade exata. Essa conferência não substitui a homologação técnica,
as três revisões independentes de Psique ou Têmis. Contratos legados preservam seus gates.
O compositor conserva claim e arquivo para replay após falha de callback, sem nova composição.

`valueFlow`, exposto nas consultas existentes de ciclos e contexto do processo, contém
situação, impedimento, responsáveis, execução ativa, decisão, critério de aceite, tempo,
pacotes aceitos e entregas das tarefas. Cenários financeiros permanecem hipóteses com fonte;
contribuição e compras somente aparecem com medição conciliada, válida e segregada. Ausência
de fonte é desconhecida. Sessões e eventos de QA nunca são prova comercial.

Validação e limites: [matriz e evidências locais](../homologacao/fluidez-produtos-capella-v1.md).
