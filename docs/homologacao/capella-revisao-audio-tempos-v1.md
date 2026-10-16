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

- Backend: 110 testes, incluindo contrato auditivo, recuperação e arquitetura; zero falhas.
- Psique: 222 testes Java, zero falhas (11 testes condicionais não aplicáveis nesta rodada);
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
