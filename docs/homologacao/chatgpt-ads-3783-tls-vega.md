# Recuperação TLS durante a entrega ChatGPT Ads #3783

## Evidência e causa — 28/10/2026

O PR #5726 foi integrado em `ff92e5b357d31ccb758e78285e539ca4e5a6b04d`,
com painel/backend saudáveis e Hermes publicado. O watchdog #40620478013
detectou expiração real das superfícies de Vega, registrada na issue #5727.
Certificado servido e linhagem em disco: validade encerrada em 28/10/2026
às 01:46:56 UTC. Não existia cron/timer de renovação de Clube MUSA. Os logs
recentes tratavam somente `mkthub.api.br`; os últimos de Clube MUSA eram
de julho. O deploy #40556092887 pulou a emissão opcional, como configurado.
Portanto, republicar a aplicação não renovava esse certificado.

## Decisão e limites

Comparadas três opções: renovar junto com uma publicação completa existente
(rápido, mas recria pagamentos desnecessariamente); acrescentar renovação
TLS ao mesmo pipeline (menor intervenção e prevenção periódica); intervenção
manual no host (fora do fluxo de publicação autorizado). Escolhida a segunda.

O job usa os volumes existentes e a linhagem já emitida, confirma todas as
rotas HTTP de desafio, renova apenas Clube MUSA e reutiliza o instalador
do proxy com `SSL_DOMAINS=clubemusa.com.br`. Faz reload, sem trocar imagens
da aplicação, recriar containers ou tocar certificados dos outros produtos.
Verificação externa exige confiança TLS e pelo menos 14 dias de validade.
Agendamento duas vezes ao dia e execução manual `operation=renew_tls`,
serializados com os demais publicadores do mesmo VPS. Nenhuma campanha,
chamada de IA ou autorização comercial é criada por essa manutenção.

## Matriz local, definida antes da execução

| Caso | Aceite |
| --- | --- |
| Certificado vencido e renovação válida | Todos os nomes preservados; cópia restrita e reload; desafio removido |
| Certificado ainda válido | Mesmo percurso idempotente, sem recriar serviços |
| Autoridade falha ou mantém certificado vencido | Falha explícita, sem copiar nem recarregar |
| Certificado novo perde um nome | Bloqueio antes de alterar o proxy |
| Montagem, proxy ou desafio incorretos | Bloqueio antes da chamada Certbot |
| Falha na cópia/reload | Resultado não pode indicar sucesso |
| Certificado de outro produto | Linhagem alheia preservada e recusada |
| HTTPS local confiável, vencido e de outro nome | Somente confiança/nome/validade corretos são aceitos |
| Workflow e shell | Agendamento/main/lock e separação de deploy; bash -n e ShellCheck |

Fixtures locais usam CA e comandos externos simulados; não são dados
comerciais nem métricas de venda. Navegadores não precisam de nova matriz:
a interface publicada do canal já passou em desktop, iPhone e Pixel.

Rodada local: 15 testes da renovação/HTTPS aprovados; contratos existentes de
transporte, seleção de destino e proxy aprovados; recuperação do proxy executada
em Docker no projeto exclusivo da sandbox e removida ao terminar. Bash -n,
ShellCheck e Actionlint aprovados. Nenhuma alteração em Java ou no schema.

## Adoção e reversão

Adotar somente após testes locais, PR integrado, execução versionada com
HTTPS externo válido e reexecução justificada do watchdog. Preservar logs
Certbot e evidências dos runs. Se a renovação falhar, manter o erro visível;
nunca usar TLS inseguro para tornar o monitor verde. A correção previne
indisponibilidade; não comprova efeito em vendas.
