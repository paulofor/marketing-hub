# Conciliação auditável da locução — v1

Solicitação: avançar Capella até o experimento em operação, preservando tetos e seleção humana.
Uma entrega técnica não comprova campanha ativa nem vendas.

## Causa comprovada

Em 16/10/2026, as finalizações #21288/#21289 falharam antes da síntese: a captura homologada
não tinha recorte legível. Os renders #21286/#21287 permaneceram preservados. A recuperação
pelo Estúdio usa artes existentes, recorte e fala revisada, sem gerar novamente as cenas.

A consulta administrativa anterior estava somente em resultsSnapshot dos projetos #11/#12.
O ledger conservava custos desconhecidos, inclusive em reutilizações. O gate auditivo consulta
essa fonte financeira e corretamente impede nova chamada quando sua cobertura está incompleta.

Foram comparadas três alternativas: aguardar fatura por chamada, indisponível no retorno binário;
repetir síntese, que duplica consumo sem conciliar; e confrontar uso oficial e recibos para
persistir estimativa conservadora no ledger. A terceira usa a evidência disponível e preserva
orçamento, independência e a distinção entre estimativa e cobrança.

## Contrato e limites

NARRATION_COST_RECONCILIATION_V1 reutiliza endpoint do módulo de vídeo, tela de vídeos do produto,
ledger e eventos existentes. Exige tenant, produto e experimento coincidentes, tentativas
encerradas, recibos únicos, tarifas preservadas e período/quantidade de chamadas compatíveis.
O operador confere a exclusividade da coorte consultada. O importador não recebe credencial
nem chama a OpenAI. Evidência ausente ou ambígua continua bloqueada.

O rateio por quantidade de requests é estimativa por tentativa, arredondada conservadoramente,
não fatura individual. Coorte, fonte, operador e hash ficam em eventos PROGRESS, compatíveis
com o ENUM real do MySQL. Replay igual não duplica eventos. Valor já conciliado diferente é
recusado; callback incompleto não apaga a conciliação.

Reutilização só recebe custo incremental zero com voz preservada, fonte conhecida e recibos
REUSED. Falhas PDE_PRODUCT_PROOF_INVALID/DELIVERY_EXAMPLES_INVALID sem voz preservada nem
interações são anteriores ao TTS no executor atual: erros posteriores preservam recibos no
callback. Outras falhas sem recibo continuam desconhecidas. Essa distinção depende do contrato
versionado do executor e não se aplica a timeout, erro de rede ou resposta perdida.

## Matriz local antes de publicar

Os provedores são simulados na homologação de código. Produtos 9007/9010 e experimentos
99001–99003 são fixtures; não criam compra, campanha ou evento comercial.

| Área | Aceite verificável |
| --- | --- |
| Caminho feliz e outro produto | Recibos, uso e tarifa geram custo no contexto correto |
| Integração | Endpoint e DTO reais chegam ao ledger, sem declarar fatura |
| Validações | Contexto, contagem, modelo, tarifa, período, recibo, parcial e duplicidade inválidos não escrevem |
| Recuperação | Voz aponta à fonte conhecida sem nova inferência |
| Falha anterior à voz | Prova inválida sem síntese é distinta de custo ausente depois da síntese |
| Auditoria | Fonte, hash, operador e rateio preservados; replay não duplica eventos |
| Callback | Retorno incompleto conserva custo e fonte aceitos |
| Interface | JSON estruturado; erro antes da rede; botão desabilitado durante envio |
| Dispositivos | Desktop, iPhone 15 Pro e Pixel 7 em Chromium: formulário e reprodução sem overflow |
| Qualidade audiovisual | FFmpeg real confere recorte, sincronia, estabilidade e isolamento entre produtos |
| Métrica | Cobertura de custo e passagem aceita; sem inferir ganho de conversão ou margem |

A revisão independente, a seleção humana dos vídeos, a jornada comercial, o preflight com
segregação e a confirmação externa da campanha continuam requisitos para declarar RUNNING.

Fontes: [Usage/completions](https://developers.openai.com/api/reference/resources/admin/subresources/organization/subresources/usage/methods/completions)
e [tarifa TTS](https://developers.openai.com/api/docs/models/gpt-4o-mini-tts).

## Resultado local

Em 16/10, o formulário e o envio estruturado passaram em desktop, iPhone 15 Pro e Pixel 7
no Chromium. JSON inválido não chamou a API, envio pendente bloqueou duplicidade e não houve
rolagem horizontal. Os MP4 #72 (35 s) e #73 (55 s), H.264/AAC 1080×1920, reproduziram com
áudio habilitado nos três perfis. Isso não é avaliação perceptual da interpretação da voz.

Os testes reais de arquitetura exigiram manter o acesso financeiro pela fachada existente
StudioCostLedgerService; SalesVideo usa um record de consulta, sem importar entidade ou
repository financeiro. A borda HTTP admite somente os records da operação, nunca o executor
de conciliação. O teste de integração atravessa controller, DTO, validação e ledger simulado.

A rodada integral do backend e a repetição somente das classes afetadas pela cópia esparsa
terminaram com 4767 testes, 0 falhas e 0 erros (39 ignorados pelas condições existentes).
Isso inclui uma execução local descartável do importador com os recibos reais: 16 chamadas
originais e 15 novas, nove reutilizações e duas falhas pré-síntese. Custos rateados estimados:
US$ 0,016398 anteriores e US$ 0,019828 novos; reutilizações não recebem cobrança duplicada.
Essa fixture operacional não é versionada e nenhum teste escreve em produção.

Os 35 testes do módulo frontend, TypeScript/build, 16 testes das atestações e parse do Swagger
também passaram. A seleção humana e as revisões dos arquivos finais não são simuladas como
aprovação comercial.

## Compatibilidade do histórico completo — 16/10/2026

A aplicação pela tela retornou 409 antes de escrever: `details_json` contém também mensagens
legadas como `Claim automático pelo video-management-service` e diagnósticos de falha em texto.
O teste operacional anterior tinha selecionado somente eventos com recibos e não cobria esse
formato. A hipótese foi confirmada no banco e reproduzida localmente com todos os 212 eventos
das 15 tentativas, incluindo as mensagens sem JSON.

A correção trata como auditoria apenas metadados e eventos que declaram `tts_interactions`.
Não transforma mensagens em recibos, modifica histórico ou afrouxa custo desconhecido.
Metadados inválidos e eventos que declaram recibos malformados continuam bloqueados.
Foi preferida a leitura compatível a reescrever eventos antigos ou ignorar indiscriminadamente
qualquer erro de JSON. A responsabilidade do leitor é distinguir mensagens de andamento dos
recibos financeiros que realmente precisa validar.

A fixture comum agora inclui claim em texto; duas identidades cobrem recibos de falha posterior
à síntese entre mensagens legadas, e uma regressão recusa auditoria declarada porém truncada.
O lote real completo passou localmente, com os mesmos custos e sem acesso de escrita remoto.
A rodada relevante terminou com 188 testes sem falha/erro, incluindo arquitetura, ledger,
reserva auditiva, lifecycle dos jobs e o teste operacional descartável do lote completo.
