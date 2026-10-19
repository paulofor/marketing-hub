# Worker IA com Codex App Server — v1

Decisão de 19/10/2026: o Worker IA deve executar as chamadas Responses de texto e visão
com Codex App Server, usando a autenticação ChatGPT dos agentes como padrão de integração.
O transporte produtivo é `OPENAI_TEXT_TRANSPORT=codex-app-server`. O modo
`responses-api` permanece uma opção explícita de compatibilidade, sem fallback automático.

## Responsabilidades e identidade

O filtro interno de WebClient substitui somente o POST no endpoint Responses configurado.
Backend, armazenamento, callbacks, fila `pending`, prompts/schemas e validadores conservam
suas responsabilidades. Não existe serviço proxy externo nem comunicação direta entre workers.
O backend continua sendo a única autoridade para avançar etapas e autorizar custos/publicação.

O App Server roda por stdio, sem porta pública, com sandbox somente leitura, aprovação
`never`, ferramentas locais e pesquisa desabilitadas. Uma solicitação de ferramenta não
suportada interrompe o turno. Imagens de referência são preservadas como entrada multimodal.
Geração especializada de imagens continua usando seu contrato/API próprios; Codex não passa
a produzir imagens ou áudio por consequência desta mudança. O enriquecimento legado também
usa o transporte compartilhado para geração, mantendo Google como pesquisa independente.
Ferramentas e parâmetros Responses sem equivalente são bloqueados antes da inferência.

O Worker tem `CODEX_HOME` exclusivo, persistente no host `191.252.210.83`, em
`/opt/marketinghub/ai-worker/codex-home`. Nenhum `auth.json`, access token ou refresh token
pode ser clonado de outro agente, transportado no repositório ou exibido ao operador.
Execução e reconexão compartilham `.oauth-session.lock`. Credenciais de APIs herdadas são
removidas do subprocesso Codex. Ausência de sessão ChatGPT bloqueia a inferência.

## Modelo, tier e custo

Preservar o modelo solicitado pela etapa e conferir sua disponibilidade por `model/list`.
Ausência não autoriza substituir o modelo silenciosamente. A preferência Flex continua na
entrada; Codex OAuth solicita `default` por compatibilidade, com motivo explícito no recibo.
Tokens permitem estimativa conservadora Standard pelo catálogo do backend, sem representar
fatura da assinatura ChatGPT. Uso desconhecido permanece desconhecido, nunca zero.
Não aumentar limites de preparação, audiovisual, mídia nem conceder aprovações humanas.

## Contrato, auditoria e recuperação

O App Server recebe `outputSchema`; a saída passa também por validação local do schema e
pelos validadores funcionais existentes. O recibo preserva thread, turno, request real,
eventos/resposta bruta, modelo, tier e uso quando disponíveis. Não inserir esses metadados
no conteúdo final entregue à cliente.

Nos clientes do core, metadados vinculam o despacho ao `idJob` original. Os recibos ficam
em `/opt/marketinghub/ai-worker/codex-receipts`, fora do diretório de rsync. Reentrega do mesmo
job/request reaproveita os mesmos bytes. Outro produto, job ou contrato não pode reutilizar
esse recibo. Persistir a saída antes da validação e do callback evita nova inferência após
falha de integração. Um marcador de despacho sem resposta é ambíguo e bloqueia nova inferência
automática; não apagá-lo para retentar sem investigar. Falha anterior ao início de turno,
como autenticação ausente, não cria consumo ou bloqueio financeiro fictício.

## Validação e aprendizado

A regressão deve cobrir Capella sintética, outro produto, caminho antes válido, callback,
despacho incerto, falta de autenticação, schema inválido, imagem e ausência de uso.
O ganho verificável é remover a dependência da API paga nas etapas Responses e prevenir
consumo duplicado na recuperação. Isso não comprova prontidão comercial ou aumento de vendas.
Matriz/evidências: `docs/homologacao/ai-worker-codex-app-server-v1.md`.

Referência oficial: [Codex App Server](https://learn.chatgpt.com/docs/app-server).

## Compatibilidade dos contratos ativos — retomada de 19/10/2026

Autenticação confirma a conta, mas não comprova disponibilidade do modelo pedido por
uma etapa. A primeira retomada de Capella expôs sete contratos do GeraSalesPage ainda
em `gpt-5.5`, ausente de `model/list`. O transporte deve continuar recusando esse caso
antes de inferir, sem alias ou fallback silencioso. A correção versiona os sete
contratos para `gpt-6.1-sol`, disponível na sessão própria e no catálogo de custos,
copiando literalmente prompts/schemas e conservando contratos e execuções anteriores.
Outros pipelines não são alterados por consequência. Migração com fonte incompleta,
colisão ou modelo sem preço falha antes de ativar parcialmente as novas versões.
A regressão MySQL 5.7 aplica as definições históricas reais, confere preservação de
conteúdo e snapshots de duas identidades, idempotência e rollback sem apagar auditoria.
Matriz: `docs/homologacao/capella-worker-retomada-codex-v1.md`.

Instalação nova deve preparar o registro de preços antes dos templates: a migração
semeia somente o modelo ausente, com fonte oficial, e não sobrescreve valores já
cadastrados. O rollback preserva essa referência para a auditoria das execuções.
