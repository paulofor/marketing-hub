# Revisão sonora e demonstração de Capella — 16/10/2026

Solicitação: continuar Capella até o experimento em execução. Aceite humano dos novos vídeos,
homologação comercial e confirmação externa de mídia continuam obrigatórios; não são substituídos
por testes locais ou por uma tarefa concluída.

## Evidência e causa

Após a conciliação das locuções no ledger, Psique #739 recebeu os vídeos #72/#73. A ferramenta
auditiva descreveu música, embora os dois recibos de produção declarem `VOICE_ONLY`,
`source_audio_preserved=false`, `music=none`. Além disso, distribuiu frases por intervalos
aproximados que contradizem as durações medidas dos segmentos. O parecer visual usou esses
intervalos para inferir defeitos de sincronia. A montagem já sincronizava legendas e cenas pelas
durações físicas; mudar sua sincronização com base nessa hipótese agravaria o problema.

A revisão também encontrou déficits independentes reais: demonstração pequena e incompleta e
arte de inspiração imediatamente antes de legenda de agenda aberta. Sua copy foi considerada
persuasiva. O resultado ADJUST ficou BLOCKED tecnicamente porque um mesmo momento sonoro foi
associado a dois frames diferentes; essa relação é válida para examinar uma transição.

A investigação da retomada mostrou outra causa: backend e worker só reaplicavam resultados
`APPROVED`. Um `ADJUST` já pago poderia cair novamente no modelo ao retomar. A regressão do
consumidor falhou antes da correção. A retomada explícita agora marca o callback preservado;
somente o worker que anuncia `PSIQUE_CALLBACK_DECISIONS_V2` recebe ajustes/bloqueios. O caminho
legado, os gates, as provas e o custo continuam preservados. O consumidor real foi exercitado
com servidor local e as três decisões, sem captura nem modelo.

Foram comparadas: confiar nos tempos estimados; realinhar vídeo às estimativas; ou fornecer os
limites medidos e validar a escuta contra a composição real. A terceira preserva o áudio,
evita ajustes falsos e reduz novas inferências. A demonstração visual será corrigida usando
as capturas e artes existentes, com cenas e textos correspondentes.

## Mudança reutilizável

O backend projeta mix e segmentos completos da mídia exata. A ferramenta recebe contexto
sonoro enxuto, sem o briefing persuasivo. Respostas que inventam trilha ou tempos divergentes
ficam preservadas e bloqueadas, com seu custo registrado. A aprovação final revalida a mesma
regra. Um momento pode sustentar frames distintos; repetir o mesmo par continua inválido.

Metadados não são percepção: a voz ainda precisa ser ouvida. Os limites são por segmento,
não alinhamento por palavra nem comprovação de sincronismo labial. Reproduções antigas,
respostas brutas, custos e rejeições permanecem históricos.

## Matriz local, antes da publicação

| Área | Critério |
| --- | --- |
| Evidência | Mix e segmentos completos; contagens, duração e texto incompatíveis não geram linha do tempo |
| Escuta | Rejeitar música inventada e tempos divergentes no caso original e outra identidade |
| Parecer | Aceitar frames diferentes do mesmo trecho, recusar par duplicado e mídia cruzada |
| Recuperação | Resposta preservada e replay sem nova chamada; custo inválido não vira zero |
| Compatibilidade | Imagens, recibos sem segmentos medidos e revisões anteriores válidas conservadas |
| Vídeo | Locução existente, arte e legenda correspondentes, prova legível e condições da oferta |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7: reprodução e leitura |
| Observabilidade | Arquivo/hash, request/response, custo e motivo da decisão auditáveis |
| Comercial | Dados de QA separados; zero campanha durante homologação; tetos originais preservados |

Métrica técnica: ausência de aprovação com evidência contraditória e aceitação do resultado
funcional sem erro de contrato. Aumento de conversão, compras e contribuição não foi medido.

## Validação executada na sandbox

- Backend: 237 testes, incluindo contrato auditivo, recuperação e arquitetura; zero falhas.
- Psique: 225 testes Java, zero falhas (11 testes condicionais não aplicáveis nesta rodada);
  sete verificações JavaScript de áudio, frames e reprodução também passaram.
- Executor audiovisual: 290 testes, zero falhas; seis testes condicionais separados da rodada.
  O flag de FFmpeg real cobriu composição, identidade alternativa e caminho legado.
- Duas execuções adicionais do harness de recuperação renderizaram integralmente anúncio
  (35 s, sete segmentos) e demonstração (55 s, oito segmentos), com arquivos reais preservados
  e dependências HTTP locais. Nenhuma chamada de síntese; o mock reprova tentativas pagas.
- O anúncio original tinha pico de -0,9 dBFS, rejeitado pelo gate. A normalização agora conserva
  3 dB de margem antes do AAC; o anúncio renderizado marcou -2,7 dBFS e -17,4 LUFS. O critério
  do gate foi preservado. Não confundir medição técnica com naturalidade ou persuasão.
- As montagens locais usam a mesma arte de agenda aberta antes da legenda correspondente,
  recortes legíveis de copiar/baixar e dos campos de personalização, sem os cabeçalhos internos
  do ambiente de teste. A prova continua identificada como exemplo com dados de teste.
- Navegação local: os dois MP4s reproduziram em Chromium desktop, iPhone 15 Pro e Pixel 7,
  seis combinações sem erro, com áudio habilitado e capturas conferidas.
- Bundle canônico de evidências, Spotless e diff sem erros. Nenhuma imagem de produção
  é construída fora do Dockerfile/pipeline versionado.

A publicação e a revisão dos novos arquivos são etapas seguintes; este registro local não
atribui aprovação a Psique/Têmis nem altera o aceite humano ou o estado comercial do experimento.

## Continuação após recuperação do parecer

O PR #5605 foi integrado em `2867c806b9d6c54fb6f7adc0dbe39de8f59d8c8c`. Os 14 workflows
da main e encadeados passaram; backend e executores publicados foram conferidos. A retomada
suportada concluiu #739 com ADJUST, mantendo US$ 0,2347328 e sem nova inferência. A inbox de
agentes esgotou o tempo em duas tentativas; o callback foi recuperado pelo endpoint oficial.

A solicitação de nova montagem pela tela recusou a fonte com HTTP 400. A guarda só permitia
filho falho ou rejeitado pelo humano; não reconhecia o ajuste independente recém-aceito.
O teste `READY_AGENT_ADJUST` reproduziu a mesma recusa antes da correção. Não havia nova
montagem criada nem cobrança decorrente dessa tentativa.

Alternativas comparadas: pedir uma nova rejeição humana criaria um repasse desnecessário;
usar o MP4 final como nova fonte perderia a recuperação da voz; reconhecer o parecer
independente da mídia exata conserva custo e autoridade. A terceira foi implementada na
guarda compartilhada, sem alterar a seleção humana ou criar outro fluxo.

A matriz complementar cobre ID/hash, decisão, escopo e status; bloqueio em campanha ativa;
preservação de aprovação/rejeição humanas; fonte antiga e tentativa concorrente; e integração
real entre parecer, montagem e recuperação de cinco segmentos em duas identidades. Os renders
integrais e testes de dispositivo da rodada anterior continuam aplicáveis: esta correção muda
somente a autorização de recuperação no backend, sem modificar mídia ou compositor.

Métrica esperada: eliminar o HTTP 400 indevido e permitir correção sem novas sínteses.
Teste técnico não comprova qualidade percebida, vendas ou contribuição. O saldo audiovisual
continua cumulativo; os novos arquivos ainda precisam de parecer e seleção próprios.

Validação complementar: 198 testes Java aprovados, incluindo 92 de arquitetura, 59 do serviço
de montagem, 37 de recuperação e dez de evidência/seleção. As duas integrações recuperaram
cinco vozes e acompanharam o novo job, sem mudar READY/PENDING do arquivo anterior.
Spotless, diff e bundle canônico (523 arquivos, 115 manifestos) também passaram.
