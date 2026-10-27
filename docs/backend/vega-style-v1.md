# Vega — jornada contextual privada v1

Versão de produto: `musa-pde-entry-v16-modelagem-internacional`. Estado canônico
no backend principal, agregado na sessão privada existente e serializado pelo
lock da sessão. Não há novo banco, fila, provedor ou promoção comercial.

Base: `/api/pde/vega/style/v1`. O gateway PDE encaminha somente os caminhos
declarados. O navegador utiliza cookie `vega-style-session`, HttpOnly, Secure
em HTTPS e SameSite Strict. O segredo não aparece em URL, armazenamento do
navegador, screenshot ou retorno funcional; somente o hash é persistido.

| Método e caminho | Contrato |
| --- | --- |
| GET `contract` | Capacidades sem sessão, escrita ou aceite |
| POST `internal/sessions` | `cycleId`, `prototypeVersion`; exige token interno, ciclo aberto e versão exata; limite 18 sessões sintéticas por ciclo/versão |
| GET `access/workspace` | Jornada, respostas, progresso e referência ao último resultado da própria sessão |
| POST `start` | Início explícito; carregar a página não registra uso |
| POST `access/missions/{missionId}/ai-guidance` | Versão, chave idempotente de 16–80 caracteres e os três campos; no máximo duas tentativas técnicas por missão/sessão |
| GET `access/ai-guidance/{id}` | Recupera a resposta persistida, inclusive bloqueio; sem nova execução |
| POST `access/missions/{missionId}/interactions` | Mesmas escolhas da geração e reflexão não vazia |
| POST `access/missions/{missionId}/complete` | Reflexão obrigatória; progressão decidida no backend |
| POST `events` | VALUE_MOMENT, READY_RESULT_USED e CHECKOUT_STARTED explícitos; sem pagamento |
| POST `internal/events` | Transporte sintético de PREFERRED_OVER_FREE, sem opinião humana fabricada |
| POST `internal/fixtures/{action}` | Direito sintético integral, expiração de direito/sessão ou revogação; exige token interno |
| GET `internal/report` | Operações, fatos, regra, decisão, horários, custos e sinais persistidos; exige ambas as credenciais |
| GET `materials/{name}` | Três registros pessoais em texto, autenticados e sem dados internos; exige direito integral vigente |

Entrada: `occasion` (160 caracteres), `existingSelection` (300),
`stylePreference` (opções da v15); `reflection` opcional na geração e obrigatória
na conclusão. Campos extras ou vazios são recusados. A chave idempotente com
outra entrada retorna conflito. Chamadas concorrentes iguais retornam o mesmo
resultado. Nenhum resultado de outra sessão ou versão é acessível.

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
