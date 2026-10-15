# Narrativa audiovisual a partir das referências — 15/10/2026

## Evidência e escopo

Referências #1 Tik Tok Flavio, #2 Rio Antigo e #3 Madonna, indicadas pelo operador na tela
`/audio-video-studio/videos-analysis`. Os três MP4 foram baixados novamente e seus hashes
coincidem com a inspeção registrada em `../registros/video-reference-examples-2026-08-25.md`.
Foram inspecionados 72 quadros distribuídos pelos arquivos, metadados e reprodução em Chromium
desktop/iPhone/Pixel. As transcrições e análises #4/#5/#6 já persistidas foram recuperadas por MCP,
sem nova inferência. Transcrição não comprova escuta perceptual da voz ou sincronismo labial.

| Referência | Engenharia reversa da estrutura, sem copiar o roteiro |
| --- | --- |
| #1, 131,402 s | 0–11 s: missão e pergunta; 11–56 s: destino e deslocamento; 56–81 s: encontro/objeção; 81–105 s: aproximação; 105–123 s: recompensa em escala maior; final: continuação. Planos gerais, reação e detalhes mantêm a mesma ação. |
| #2, 162,006 s | 0–18 s: presença humana e escala; 18–46 s: contexto e construção; 46–73 s: dificuldade; 73–92 s: mistério; 92–131 s: contraponto; 131–162 s: obra concluída e convite. Não tomar a história narrada como fato verificado. |
| #3, 63,745 s | 0–9 s: performance; 9–18 s: ambiente; 18–29 s: troca de olhares; 29–37 s: aproximação; 37–58 s: dança; final: convite. A relação muda enquanto performance, luz e enquadramento preservam continuidade. |

Os cortes não são uma regra de qualidade: planos longos do terceiro exemplo continuam tendo
ação e progressão. A mecânica transferível é expectativa → desenvolvimento → recompensa.
Não transferir pessoas, falas, música, material político ou alegações históricas para ofertas.
Os exemplos não possuem prova atribuída de vendas. Esta entrega é independente de produto,
ciclo, campanha ou orçamento; não refaz peças nem altera autorizações históricas.

## Lacuna observada e alternativas

O contrato publicado `VIDEO_COPY_REVIEW_V1` separa a qualidade textual, mas `visualProgression`
continua sendo uma nota com evidência livre: não exige que Psique identifique a abertura,
o desenvolvimento e a recompensa em frames realmente recebidos. Os testes existentes aceitam
essa nota sem referências temporais. A análise das referências existe no Estúdio; ela não é,
por si, uma revisão da progressão de cada nova peça.

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Exigir mais cortes ou um provider novo | Movimento fácil de contar | Pode continuar decorativo; aumenta custo sem resolver narrativa | Não adotada |
| Criar um agente narrador separado | Responsabilidade visível | Duplica Íris e Apolo, acrescenta repasse e não corrige o aceite | Não adotada |
| Explicitar a progressão em Íris/Apolo e exigir evidência temporal em Psique | Reutiliza produção e revisão existentes | Pequena ampliação do parecer; não prova qualidade artística nem conversão | Adotada |

## Matriz definida antes da implementação e testes

| Dimensão | Aceite local |
| --- | --- |
| Escrita/direção | Prompts efetivamente compostos incluem expectativa, nova informação, recompensa e ponte ao produto, sem receita obrigatória de cortes ou pessoas |
| Revisão final | PROGRESSION só aprova com abertura, desenvolvimento e recompensa ligados a frames reais da mesma mídia/hash |
| Falhas | Recusar nota alta sem narrativa, modo decorativo, critério insuficiente, frame alheio/inventado, ordem invertida, instante fora do vídeo e evidência repetida |
| Recuperação | ADJUST/BLOCKED preservam diagnóstico, inclusive quando não há prova suficiente; replay usa o mesmo parecer e evidências sem nova inferência |
| Regressão | Caso original, outra identidade e roteiro antes válido; revisão de imagem sem vídeo continua válida |
| Integração | Schema estrito, composição do prompt, consumidor e persistência de evidência existentes; modelo simulado, nenhuma chamada paga |
| Interface/dispositivos | Sem mudança de UI/player; referências reproduzidas em desktop/iPhone/Pixel; consulta de contratos publicados após deploy |
| Métricas e isolamento | Fixtures declaradas como teste; observar rejeições por narrativa e retrabalho; retenção, compra e contribuição só em experimento posterior autorizado |

## Adoção e limites

Adotar com testes relevantes aprovados e recursos publicados iguais aos versionados. Se uma
revisão válida for recusada por identidade/tempo, corrigir a validação com fixture antes de
repetir modelo; preservar parecer/custo. Não reescrever aprovações históricas. Não tratar
presença de campos ou notas sintéticas como demonstração de aprendizado criativo ou venda.

## Resultados locais

- Íris: 87 testes executados; três opcionais desabilitados. O prompt composto entrega a direção
  narrativa junto ao contexto canônico, sem outra etapa ou inferência.
- Psique: 201 testes executados; 11 opcionais desabilitados. Ausência de narrativa, modo decorativo,
  nota insuficiente, frame não recebido/estrangeiro, hash diferente, ordem/duração e repetição
  impedem aprovação. Pareceres de ajuste, imagem estática e recuperação permanecem válidos.
- Apolo: 279 testes executados; nove opcionais desabilitados. O request efetivo e sua auditoria
  preservam a direção narrativa; rotas determinísticas, integração e recuperação permanecem válidas.
- Backend: 21 testes de catálogo/empacotamento e fornecimento das mídias reais passaram. Não há
  endpoint ou coluna nova; o resultado continua no parecer persistido da mesma tarefa.
- Captura: três testes JS de identidade/amostragem e um teste real de pixels/instante passaram
  com Chromium local. A reprodução das referências confirmou duração e ausência de erro no player.
- Schema: 31 objetos conferidos com propriedades fechadas e campos obrigatórios coerentes.
- Evidência comercial existente: 16 testes do empacotador passaram; 521 arquivos/114 manifestos
  preservados. Nenhuma atestação histórica foi renovada.

A primeira execução do teste de replay encontrou somente uma configuração incompleta da fixture:
ObjectMapper sem módulo de Instant. Corrigida para reproduzir a serialização do runtime antes da
suíte completa. A chamada inicial do smoke a partir da raiz foi ajustada para o diretório do módulo;
o capturador real passou. Não houve correção de produto, gasto ou publicação como mecanismo de teste.

São 588 testes Java efetivamente executados, além dos testes JS e de contratos acima. Os 23
opcionais desabilitados pertencem a integrações de outros harnesses. Escores e cenas de teste não
são pareceres reais de Psique. Este trabalho melhora o processo e seus controles; não entrega
novos vídeos nem comprova equivalência artística às referências ou aumento de vendas.
