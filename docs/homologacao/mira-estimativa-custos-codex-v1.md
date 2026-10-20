# Estimativa operacional de Mira e consumo Codex — v1

## Evidência e decisão

Em 20/10/2026 o usuário pediu estimativa em lugar do extrato indisponível e confirmou que
texto/visão do Worker IA usa Codex. O cânone do transporte e o executor já calculam
estimativa por tokens reportados. A pendência de Mira não é desse transporte: os recibos
Speech de 10/11 de outubro ainda exigiam extrato administrativo, que retornou 403.
Mira/produto 10, ciclo 9, experimento 102, execução 55, vídeos 64/63: teto audiovisual
acumulado US$ 12; mídia continua R$ 100 totais/5 dias somente após homologação.

## Alternativas e escolha

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Aguardar extrato oficial | Maior precisão | Mantém bloqueio externo sem prazo | Preservar como aperfeiçoamento posterior |
| Tratar migração Codex como custo zero | Implementação pequena | Apaga locuções API e distorce margem | Rejeitada |
| Estimar recibos preservados e separar transportes | Permite controle conservador sem outra inferência | Incerteza explícita; requer duração e identidade auditadas | Adotada no endpoint/ledger existentes |

Estimativa de voz é um envelope interno, **não tarifa oficial por minuto**: para o modelo
`gpt-4o-mini-tts-2025-12-15`, US$ 0,072/minuto de áudio fisicamente medido, mais o envelope
conservador de entrada (bytes UTF-8 do request × US$ 0,60/milhão × 2). O envelope de áudio
corresponde à premissa de planejamento de até 100 unidades de áudio por segundo à tarifa
preservada de US$ 12/milhão; não afirma conhecer a tokenização real. A premissa não é
persistida como tokens medidos. Nenhum preço desconhecido ou outro modelo recebe esse cálculo.
Referência de tarifas: https://developers.openai.com/api/docs/models/gpt-4o-mini-tts

O cálculo inclui falhas depois de produzir voz e exclui reutilizações com origem comprovada.
Contagem de requests e hashes impede cobrança duplicada. Custo faturado permanece desconhecido.
Consumo Codex usa tokens reportados e catálogo Standard como equivalente interno; o extrato
API não é requisito de uma sessão ChatGPT. Quotas/créditos continuam aplicáveis:
https://learn.chatgpt.com/docs/pricing

## Matriz de homologação anterior à execução

| Cobertura | Aceite |
| --- | --- |
| Caminho feliz | Recibos/duração válidos produzem estimativa positiva, eventos e ledger; nenhuma chamada externa |
| Regressão original/outra identidade | Duas identidades sintéticas, inclusive falha após gerar voz e origem recuperada dos eventos |
| Compatibilidade | Extrato oficial e reutilização anteriores continuam válidos |
| Validações | Ausência/duração inválida, modelo/tarifa divergente, recibo duplicado, outro tenant/produto/experimento e lote misto não gravam custos |
| Recuperação | Reenvio idêntico não duplica eventos/custos; custo confirmado não é sobrescrito; extrato posterior substitui estimativa sem somá-los |
| Interface | Desktop Chromium, iPhone e Pixel emulados: opção explícita, fonte, envio ao endpoint existente e recibo rotulado estimado |
| Observabilidade/métricas | Método, versão, fonte, operador, hash e premissas preservados; orçamento/status/aceites imutáveis |
| Segregação | Testes usam identidades e recibos sintéticos, sem IA paga nem dados comerciais fabricados |

Adoção: somente após testes e publicação. Reversão: desmarcar estimativa e preservar lançamentos;
melhor medição substitui o valor no mesmo ledger, sem apagar eventos. Métrica futura: tempo de
espera por custo e diferença estimado/medido; não há prova de aumento de vendas.

## Resultado local

- 208 testes backend passaram (incluindo 92 regras de arquitetura) (ledger, endpoint, orçamento e gate auditivo); quatro testes do
  formulário, tipos e build passaram.
- Replay local do service real com os 14 jobs/recibos preservados: **33 sínteses novas**,
  130,632 segundos medidos, **US$ 0,174380 estimados**; seis origens com geração e oito
  reutilizações/falhas anteriores à voz. Reenvio manteve os mesmos valores sem eventos adicionais.
- O valor substitui apenas a parcela desconhecida de locução no planejamento; a reserva histórica
  de US$ 2 não é somada novamente nem tratada como custo faturado.
- O Worker IA já estima texto/visão Codex por tokens reportados; não houve necessidade de alterar
  esse transporte ou fazer outra inferência para registrar esta regra.

Contrato administrativo existente: `POST /api/sales-videos/experiments/{experimentId}/narration-costs/reconcile`.
O campo opcional `estimateFromReceipts: true` é incompatível com `usage` preenchido; o backend
calcula todos os valores a partir da auditoria persistida, sem aceitar valores escolhidos pelo
frontend. Clientes anteriores continuam com `false`. Resposta mantém
`providerInvoiceConfirmed: false`. Lote inválido não grava parcialmente.

A UI local passou em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, com um único POST
simulado por dispositivo, usando a página existente e o contrato HTTP real. Os testes de endpoint
percorreram o cálculo/ledger com doubles; nenhuma escrita produtiva foi usada para testar.
O replay adicional do ledger protege a estimativa contra callbacks posteriores sem custo.
Tentativa de conferir dois binários antigos diretamente no storage retornou HTTP 403; o cálculo
permanece apoiado nas durações físicas preservadas pelo executor, e não em nova medição local.

## Recusa anterior à geração encontrada na conferência integral

O job 21265 também mantinha custo nulo. O histórico confirma `APOLLO_PLANNING_HTTP_ERROR`:
um envio `/v1/responses` Flex seguido de HTTP 429 `flex_unavailable`, sem resposta aceita,
usage, saída, asset, providerJobId ou task aceita no ledger. O código de Apolo executa o
planejamento antes de enviar clipes ao provedor. Esse caso comprova recusa anterior à
inferência, não consumo desaparecido. O valor estimado exibido na lista era uma projeção do
vídeo que não chegou a iniciar, não evidência de custo dessa tentativa.

A opção existente passa a reconhecer essa sequência de forma geral, sem exceção por ID,
para tentativas RENDER encerradas. Exige pareamento de todos os envios e recusas, contexto,
endpoint/modelo/Flex e erro `flex_unavailable`, mais consulta SQL de ausência de task aceita.
Custo incremental estimado zero recebe fonte `PLANNING_REJECTED_BEFORE_GENERATION_AUDITED`,
sem declarar fatura e sem alterar arquivos/status. Resposta ausente/aceita, timeout, outro
erro, request trocado, duplicidade, task aceita, providerJobId ou asset preservam bloqueio.
A tela aceita o ID dessa tentativa no mesmo registro auditado; não há novo endpoint paralelo.

A matriz foi ampliada antes da rodada adicional: duas identidades para recusa comprovada,
doze variantes ambíguas/aceitas, replay e preservação em callback incompleto. Essa correção
não transforma outras falhas ou gerações pelo Codex em custo zero.


Confirmação oficial adicional: o guia de
[Flex Processing](https://developers.openai.com/api/docs/guides/flex-processing#resource-unavailable-errors)
informa que recusas 429 por indisponibilidade de recursos não são cobradas. Isso sustenta
somente a recusa explícita conferida; não permite inferir custo zero de outros erros.
O replay final incluiu 15 tentativas e manteve US$ 0,174380, com a recusa pré-geração em zero.
A presença do diagnóstico legado em texto foi reproduzida; somente eventos estruturados com
`eventType` são lidos como auditoria. JSON declarado truncado segue bloqueante.
