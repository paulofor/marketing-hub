# Vega — jornada contextual privada v1

Versões de produto: `musa-pde-entry-v16-modelagem-internacional` e
`musa-pde-entry-v17-continuidade-clara`. Estado canônico
no backend principal, agregado na sessão privada existente e serializado pelo
lock da sessão. Não há novo banco, fila, provedor ou promoção comercial.

Base: `/api/pde/vega/style/v1`. O gateway PDE encaminha somente os caminhos
declarados. O navegador utiliza cookie `vega-style-session`, HttpOnly, Secure
em HTTPS e SameSite Strict. O segredo não aparece em URL, armazenamento do
navegador, screenshot ou retorno funcional; somente o hash é persistido.

| Método e caminho | Contrato |
| --- | --- |
| GET `contract` | Capacidades sem sessão, escrita ou aceite; `supportedPrototypeVersions` informa versões aceitas, `optionalReflectionPrototypeVersions` identifica a conclusão sem comentário e `prototypeVersion` preserva a referência v16 |
| POST `internal/sessions` | `cycleId`, `prototypeVersion`; exige token interno, ciclo aberto e versão exata; limite 18 sessões sintéticas por ciclo/versão |
| GET `access/workspace` | Jornada, respostas, progresso, último resultado e `reflectionRequiredForCompletion` da versão da própria sessão |
| POST `start` | Início explícito; carregar a página não registra uso |
| POST `access/missions/{missionId}/ai-guidance` | Versão, chave idempotente de 16–80 caracteres e os três campos; no máximo duas tentativas técnicas por missão/sessão |
| GET `access/ai-guidance/{id}` | Recupera a resposta persistida, inclusive bloqueio; sem nova execução |
| POST `access/missions/{missionId}/interactions` | Mesmas escolhas da orientação válida; comentário opcional na v17, obrigatório na v16 |
| POST `access/missions/{missionId}/complete` | Orientação válida e ação explícita; respeita `reflectionRequiredForCompletion`; progressão decidida no backend |
| POST `events` | VALUE_MOMENT, READY_RESULT_USED e CHECKOUT_STARTED explícitos; sem pagamento |
| POST `internal/events` | Transporte sintético de PREFERRED_OVER_FREE, sem opinião humana fabricada |
| POST `internal/fixtures/{action}` | Direito sintético integral, expiração de direito/sessão ou revogação; exige token interno |
| GET `internal/report` | Operações, fatos, regra, decisão, horários, custos e sinais persistidos; exige ambas as credenciais |
| GET `materials/{name}` | Três registros pessoais em texto, autenticados e sem dados internos; exige direito integral vigente |

Entrada: `occasion` (160 caracteres), `existingSelection` (300),
`stylePreference` (opções da v15); `reflection` opcional na geração. Na v17,
também é opcional na conclusão; a v16 preserva a exigência histórica.
Campos obrigatórios vazios e campos extras são recusados. A chave idempotente com
outra entrada retorna conflito. Chamadas concorrentes iguais retornam o mesmo
resultado. Nenhum resultado de outra sessão ou versão é acessível.

A v17 ajusta a hierarquia das ações e explica a continuidade na interface. Ela
exige ciclo próprio: criar uma sessão não migra a v16 nem herda autorização,
parecer ou aceite. O workspace e as operações usam a versão da sessão, enquanto
as regras de orientação permanecem compartilhadas e auditáveis. O publicador
aguarda a capacidade explícita da versão antes de promover a imagem.

Correção de 28/10/2026: concluir um dia na v17 não exige comentário, download,
confirmação de aplicação ou compra. O backend publica
`reflectionRequiredForCompletion=false`; a interface usa esse valor e só oferece
conclusão após orientação válida. O comentário omitido permanece ausente/vazio,
sem texto fabricado. Concluir não registra VALUE_MOMENT, READY_RESULT_USED,
PREFERRED_OVER_FREE ou CHECKOUT_STARTED. Direitos dos Dias 2 a 7 são preservados.
Contrato HTTP: [Swagger da jornada contextual](../swagger/vega-contextual-style-v1.yaml).

O catálogo aceita apenas relações e recursos declarados; ausências, negações,
contradições e situações não suportadas produzem BLOCKED sem cartão pronto.
O catálogo e os testes versionados congelam esse limite; uma regra não equivale
a avaliação por modelo ou eficácia de moda comprovada.

Todos os dados possuem origem AGENT_VALIDATION e marcador mh_internal_test.
Compra simulada não concede direitos. A fixture de 90 dias é independente da
sessão privada de sete dias e não altera oferta, cobrança ou quotas comerciais.
Os metadados de AUDIO das especificações anteriores não geram áudio: esta
implementação entrega texto/HTML, sem produção audiovisual autorizada.

Provas e matriz: [homologação contextual v16](../homologacao/vega-modelagem-internacional-v16.md).
