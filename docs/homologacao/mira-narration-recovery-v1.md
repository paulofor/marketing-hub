# Recuperação de narração preservada — Mira

Solicitação #3332, produto 10, ciclo 9, experimento 102. Anúncio do projeto 13:
render 21252 aceito; finalização 21253 bloqueada porque cinco segmentos somam 15,720 s
para um visual de 15 s. O evento 79539 preservou os cinco arquivos, hashes, textos,
durações e custos pendentes de conciliação. O parecer 703 permanece aceito.

Foram comparadas três correções: encurtar copy e sintetizar novamente (perde voz já paga);
aumentar o render e sintetizar novamente (repete custo); recuperar os segmentos e alongar
explicitamente o último quadro visual (preserva texto, fala, prova e custo). Adota-se a
terceira quando a fonte, o texto e os hashes forem íntegros e o operador definir duração
suficiente. Não acelerar, cortar ou sobrepor fala; não recontar consumo antigo como zero.

Matriz local definida antes de testar:

| Critério | Verificação | Resultado |
| --- | --- | --- |
| Caso original | Mesmos cinco textos/áudios, visual 15 s → duração pedida 20 s; sem TTS novo | Passou |
| Outro contexto | IDs de produto, job e ciclo distintos; mesma regra genérica | Passou |
| Caminho antes válido | Pós-produção normal e HLS sem recuperação preservados | Passou |
| Validações | Texto alterado, recibo incompleto, hash divergente ou duração insuficiente bloqueiam antes de consumir | Passou |
| Integração | Backend deriva o contrato de auditoria própria; executor baixa e verifica binários | Passou |
| Observabilidade/custo | Linhagem, duração solicitada, fontes e reutilização; custo original desconhecido continua desconhecido | Passou |
| Dispositivos | Formulário desktop/iPhone/Pixel e reprodução com legendas e áudio | Passou |
| Segregação | Fixtures e rede simulada; áudio real já existente somente para leitura/homologação local | Passou |

A homologação funcional não é aceite independente, autorização comercial nem prova de venda.

A demonstração 21255 apresentou outro defeito antes de produzir voz: a composição
30 s da prova ultrapassou 120 s. O anúncio 15 s chegou à fala, portanto não faltava
captura nem contrato. Consideraram-se aumentar apenas o timeout, omitir a composição
ou limitar filtros/encoder e usar preset rápido preservando hash, pixels e cortes.
Adota-se a terceira: filtro com uma thread, encoder com duas, preset ultrafast,
CRF 20 e prazo original, sem remover homologação ou prova.
A matriz inclui composição real 15/30 s, pixels dentro/fora dos intervalos e prazo.

Resultados locais: 184 testes de backend/arquitetura sem falhas; 255 testes do executor,
sem falhas (duas homologações reais opt-in executadas separadamente); 71 testes do frontend,
typecheck e build passaram. FFmpeg real recuperou os cinco binários com hashes conferidos,
MP4 20,0 s e áudio íntegro de 15,72 s, cinco cues e zero requests de síntese.
Composição real 15/30 s passou; desktop/iPhone/Pixel enviaram duração explícita válida,
bloquearam entradas inválidas e reproduziram MP4 com controles. Todas as consultas de mídia
real foram somente leitura; fixtures e downloads servidos apenas em localhost, sem custo externo.

Custo antigo de TTS permanece pendente de conciliação no metadado. Os dois pareceres
financeiros conhecidos somam US$ 0,107738 estimados; não é fatura conciliada nem total completo.
A entrega dos vídeos e seus aceites em produção serão conferidos depois da publicação,
sem regenerar a voz do anúncio ou reabrir os ciclos financeiros já aceitos.

Compatibilidade adicional definida antes de testar: backend novo com executor antigo
mantém recuperações na fila, sem claim ou TTS; executor compatível declara
`PRESERVED_TTS_NARRATION_V1` na consulta e na reserva. O SQL filtra antes do limite,
preservando jobs normais. Executor novo continua consumindo backend anterior, que
ignora o parâmetro adicional nos jobs sem recuperação. Foram comparados ordenação
de deploy apenas, consulta de health e contrato explícito na fila/reserva; este último
protege a passagem sem depender de ordem de containers ou inferência de READY.
Testes: consulta real em H2 com limite 1, claim incompatível sem mudança de estado,
HTTP do executor e caminhos anteriores. Passou: filtro antes do limite, claim legado sem mudança de estado, declaração HTTP
na fila/reserva e consumo anterior preservado.


## Captura longa sob recursos limitados — recorrência após #5564

A publicação `1871e279153e225e159688b784ba1ef8be9eed9c` entregou o anúncio
21256/ativo 53 sem síntese nova. A demonstração 21257 repetiu o timeout de 120 s
antes de TTS. A limitação de threads foi insuficiente: o teste anterior usava uma
captura pequena. Não declarar a composição resolvida apenas pelo preset ou health.

Foram comparadas três alternativas: ampliar apenas o prazo (conserva desperdício);
reduzir previamente a imagem (ainda repete decode e filtros); cachear o quadro já
transformado (retira trabalho repetido e conserva pixels). A terceira foi validada
com o MP4 original de 30 s/30 fps e PNG 1179×6138, hash
`45ec50db5899063f59b2d788ee24d9f7f326e18bb3c9194dd32fe63184132fa7`.
O processo já redirecionava stderr para arquivo: descartado bloqueio de pipe.

Matriz complementar definida antes da correção:

| Critério | Verificação | Resultado |
| --- | --- | --- |
| Reprodução da causa | Mesmos bytes, prazo 120 s, 0,25 CPU/768 MiB, rede bloqueada | Anterior excedeu 120,19 s |
| Composição corrigida | Quadro preparado uma vez; mesmo prazo e limites | Passou em 36,74 s |
| Integridade temporal/visual | Comparação dos 900 frames, duração e cortes | SSIM Y/U/V/total = 1,0 |
| Custo operacional | Decode/CPU/memória no mesmo host local | 752 → 1 decodes; 57,93 → 8,03 CPU-s; 411488 → 193096 KiB |
| Executor Java real | 15/30 s, outro tenant/projeto e captura 1179×6138, 0,25 CPU | Passou; composição 13,71/26,79 s |
| Fluxo completo da demonstração | Bytes locais, sete textos, TTS simulado, duração explícita 35 s, MP4/VTT/HLS, callback e replay | Passou |
| Recuperação antes válida | Cinco áudios preservados, custo original pendente, zero TTS novo | Passou |
| Observabilidade | Composição distinta de download; reutilização distinta de síntese; uso ainda pendente | Passou |
| Prevenção no publicador | Teste real habilitado no job que já instala FFmpeg | Configurado; validação local YAML |

A medida nativa de CPU/memória é distinta do ensaio com quota: o timeout anterior
consumiu 29,86 CPU-s até ser interrompido; o corrigido consumiu 8,66 CPU-s até concluir.
A quota é um teste determinístico de pressão, não uma equivalência de hardware com VPS.
`VerifyPrivateProof` aceita MP4/PNG locais opcionais, preserva os textos e conta o
número real de segmentos; não depende de cinco cortes nem de IDs específicos.
O tom sintetizado da fixture não avalia naturalidade da voz da demonstração final.
A adoção mantém gates, bytes auditados, tempo e limites financeiros. Impacto comercial
continua sem medida; a melhoria comprovada é conclusão com menor custo operacional.

Validação final local do publicador: 256 testes do executor/arquitetura, zero falhas;
a recuperação opt-in com os cinco áudios reais passou separadamente sem TTS novo.
Os dois casos Java/FFmpeg sob quota passaram, com pixels, dimensões e duração conferidos.
YAML do workflow válido. Diff e comentários de responsabilidade Java revisados.


## Integração de Mira no percurso existente

A conferência da próxima passagem mostrou que `LearningCycleVideoBinding` reconhecia
apenas `/vega-private`; a candidata real `/mira-candidate` não recebia o vídeo e seu
harness não media reprodução/recuperação. Compararam-se concluir o vídeo isolado,
criar outro pipeline e estender contrato/player/harness existentes. Adota-se a terceira,
com decisão humana preservada, vínculo ao mesmo ciclo/versão e rehomologação obrigatória.

Matriz complementar definida antes dos testes:

| Critério | Verificação | Resultado |
| --- | --- | --- |
| Caso Mira | Gate de aprovações e sessão do mesmo produto/ciclo/experimento/versão | Passou local; aceite humano produtivo continua pendente |
| Outro contexto | Segunda identidade isolada e mídia divergente omitida | Passou, ciclos sintéticos 7006/7017 e regressões de contexto |
| Caminho Vega | Mesmo player opcional, seletores e contrato antes válidos | Passou em desktop/iPhone/Pixel |
| Falhas | Mídia indisponível/revogada, hash divergente, voz ausente, erro de reprodução | Passou em testes unitários e falha de mídia nos navegadores |
| Passagem | Recibo único, fingerprint atual, homologação medida antes das revisões | Passou no contrato e relatório local |
| Dispositivos | Desktop/iPhone/Pixel, ação principal sem assistir, fallback e legendas | Passou em 18 percursos e três cenários adicionais |
| Segregação | HTTP/SQL locais, fontes e decisões de fixture, zero chamadas pagas | Passou, MySQL 5.7 e mídia sintética com áudio |

Sessão expõe `videoIntegration` opcional pela API já existente. Sem recibo ou se
contexto divergir, a organização/retomada anterior continua funcionando. O player
não inicia sozinho nem exige assistir. O harness compara hashes e mede reprodução,
áudio decodificado e recuperação, sem afirmar aceite humano ou evidência comercial.

Resultados: 146 testes relevantes de backend/arquitetura (um opt-in sem relatório
na rodada unitária; a matriz com relatório foi executada depois), 173 testes do
Customer Agent Worker (cinco opt-ins de outros fluxos não ativados), 39 testes Node,
build/typecheck de Mira e Vega. O runner existente aceita `MIRA_TEST_VIDEO_BINDING_FILE`
opcional; o mesmo recibo entra no backend local e no harness, sem alterar produção.
O teste forneceu certificado local válido e pinagem específica ao Chromium, sem
desabilitar a verificação TLS de Node. A primeira tentativa detectou certificado
histórico expirado; o ensaio corrigido passou e a topologia temporária foi removida.
