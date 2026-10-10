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
