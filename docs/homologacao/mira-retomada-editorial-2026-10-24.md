# Retomada sonora e editorial de Mira — 24/10/2026

## Evidência e causa

Após confirmação do operador sobre o limite da organização, a retomada pela tela da
execução 55 criou Psique 803. Os recibos sonoros 11/12 concluíram PASS para os arquivos
64/63: US$ 0,035350 estimados; a inferência textual registrou US$ 0,6185492 estimados.
O resultado completo foi ADJUST/FINAL_VIDEO: abertura fragmentada, pouca identificação,
prova pequena e chamada divergente. Não equivale a aprovação nem a venda.

Dois defeitos de passagem foram reproduzidos: o motivo da pausa recuperava a falha antiga
802 em vez do ajuste 803; a remontagem aceitava somente criticalDeficit=true, excluindo
ajustes editoriais explícitos com identidade correta. O compositor também impunha prova
pequena sobre a cena na primeira/última legenda, impedindo atender o parecer de legibilidade.

## Alternativas e decisão

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Repetir a revisão sem alterar a mídia | Menor trabalho inicial | Novo custo e mesmo defeito | Descartado |
| Tratar escuta PASS como aprovação completa | Avanço rápido | Ignora parecer visual e autoridade independente | Descartado |
| Encaminhar o ADJUST vigente e permitir enquadramento explícito | Resolve a passagem e a causa visual | Correção pequena em contrato, editor e compositor | Adotado |

## Matriz de homologação local

| Dimensão | Critério |
| --- | --- |
| Retomada real | H2/MockMvc reproduz falha antiga → retomada → ajuste concluído; razão atual e nenhuma tarefa duplicada |
| Identidade/autoridade | Mesmo vídeo/hash, parecer FINAL_VIDEO; rejeitar contexto alheio e preservar bloqueio de aprovação |
| Composição | FFmpeg real em duas identidades: prova ampliada no início/final e montagem legada preservada |
| Validação | Recusar enquadramento desconhecido, ordem, leitura e referência inválidas antes de consumo externo |
| Tela | Escolha persistida no contrato em desktop, iPhone e Pixel emulados |
| Auditoria/recuperação | Manter hashes, momentos de revisão, recibos e estimativas; não repetir inferência concluída |
| Dados/métricas | Fixtures locais sem API paga; nenhuma compra ou resultado comercial fabricado |

O contrato POST `/api/sales-videos/jobs/{id}/request-post-production` mantém seus campos e
aceita `proofShots[].layout` opcional: FULL_PRODUCT_PROOF_V1 ou MOTION_WITH_PRODUCT_PROOF_V1.
Ausência preserva montagem antiga. A escolha é auditada no resultado. O frontend usa o
endpoint existente do próprio módulo. Nenhuma campanha ativa é alterada por esse controle.

A hipótese reutilizável é reduzir retrabalho e tempo parado por ajustes editoriais, sem
confundir criticidade com necessidade de correção. Adoção depende dos testes e do parecer
independente da mídia nova; reverter se houver divergência de identidade ou regressão legada.
O impacto em conversão e contribuição exige medição de mercado posterior autorizada.

## Resultado local

- Backend: 89 testes relevantes aprovados, incluindo H2/MockMvc, autorização de
  remontagem, aprovação independente e contrato completo de pós-produção.
- Compositor: 24 testes aprovados; quatro renderizações FFmpeg reais cobrem as duas
  identidades com enquadramento explícito e legado.
- Psique: oito testes de contrato/recuperação sonora aprovados antes da retomada.
- Frontend: dois testes de edição aprovados, build de produção concluído e seleção
  conferida em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados.
- Diff e comentários de responsabilidade revisados. Sem SQL ou migração de schema.

A montagem das peças reais e seus novos pareceres serão registrados após a publicação;
esta validação local não representa aprovação dos vídeos nem resultado comercial.

## Reserva sonora baseada na entrada

A conferência prévia da próxima revisão identificou outra dependência: US$ 4,116 eram
reservados como se todo o contexto de 128 mil tokens fosse áudio, mesmo em vídeos curtos.
Recibos 11/12: PCM mono 24 kHz/16 bits, 22/33 segundos, 6.411/6.629 bytes de texto,
220/330 tokens sonoros e 431/544 de saída. Não foram feitas novas chamadas para medir isso.

Preservar a reserva fixa manteria a pausa apesar da folga observada; reservar apenas a média
histórica subestimaria variação. Foi escolhido medir o PCM e usar margem local de 10 vezes
os tokens/segundo observados, texto por bytes, saída máxima e piso US$ 0,25. É estimativa,
não garantia de preço ou limite tarifário; formato incerto mantém o fallback anterior.
Testes adicionais cobrem duração/contexto maiores, fallback, duas identidades, saldo abaixo
da reserva e repetição sem nova cobrança. Autorizações totais não mudam.

Os 59 testes adicionais de contrato, reserva e recuperação sonora passaram. A soma das
validações locais relevantes é 182 testes, além do build e das três navegações emuladas.

## Retorno de locução e limite da mensagem

A remontagem autorizada 21295 reutilizou o clipe 21267, mas a primeira locução recebeu
HTTP 401 / invalid_api_key. Nenhum áudio novo foi recebido. A credencial própria de
Apolo e a cópia legada do mesmo host também foram recusadas na consulta sem geração;
a credencial de Psique não foi copiada nem alterada.

O callback `/internal/video/jobs/21295/fail` falhou três vezes: MySQL 1406, mensagem
maior que `sales_video_job_event.message VARCHAR(512)`. Eventos anteriores aceitos
(21289, 21288, 21272) tinham 79–103 caracteres. A falha não era nova geração em curso:
a persistência do estado terminal foi revertida pelo evento extenso.

A correção limita o resumo e preserva a mensagem integral no LONGTEXT existente.
Para mensagens extensas, `details_json` usa `SALES_VIDEO_EVENT_DETAIL_V1`, com
`originalMessage` e `sourceDetailsText` (texto original, inclusive quando contém JSON).
Mensagens dentro do limite mantêm o contrato anterior. `failure_detail` permanece
inalterado. O mapeamento JPA explicita o limite canônico; não há alteração de schema.

Alternativas: ampliar a coluna exigiria migração sem resolver a mistura de resumo e
auditoria; cortar a resposta no executor perderia evidência e não protegeria outros
callbacks; limitar o resumo no backend e preservar o original protege todas as origens.
A última opção foi adotada, com regressão de persistência H2 em modo MySQL para o caso,
outra identidade e o caminho curto antes válido, incluindo corte seguro de Unicode.

A nova locução permanece dependente da recuperação da credencial própria de Apolo;
não repetir a chamada 401 sem mudança comprovada. Não ampliar orçamento, substituir
por voz robótica ou atribuir aprovação aos revisores para contornar essa dependência.

## Fonte operacional recuperável e recarga de Apolo

A conferência adicional do histórico identificou a fonte operacional previamente
fornecida ao Worker IA (`191.252.210.83`, arquivo protegido OpenAI). Ela respondeu HTTP
200 na consulta de autenticação sem geração, enquanto o arquivo próprio e o legado de
Apolo responderam 401. Isso permite preparar a rotação dentro da retomada autorizada,
sem solicitar outra chave nem copiar/modificar a credencial exclusiva de Psique.

Manter a chave inválida preservaria o bloqueio; trocar um arquivo comum poderia afetar
outros produtos; preparar cópia própria validada e recarregar pelo pipeline mantém o
isolamento. Foi escolhido o último caminho, com backup e coordenação operacional.
Nenhuma chave fica no repositório, argumento de comando ou saída de ferramenta.

O publicador de vídeo passa a usar `--force-recreate --no-deps` somente no serviço
`video-management`. A validação executa o script real com Docker simulado: mesma imagem
em duas aplicações, somente vídeo recriado, segredo ausente preservando o container.
Os 14 testes de retirada/isolamento passaram; `bash -n`, ShellCheck e o teste de
recuperação atômica também passaram. Nenhuma nova imagem de produção é criada fora
do fluxo versionado. A rotação e a autenticação no container serão registradas na
evidência operacional após a publicação.

## Recusa da primeira locução e custo incremental

A rotação publicada autenticou e produziu as novas peças 77/78 (38/44 segundos).
As locuções somam US$ 0,084632 estimados por duração/recibos. A tentativa 21295,
recusada com 401 antes do primeiro áudio, continuou com custo desconhecido; a
reserva sonora da tarefa 804 foi corretamente bloqueada antes de nova inferência.

O callback preserva request, resposta `invalid_api_key`, HTTP 401, segmento 1 e
identidade da tentativa. O executor encerra nesse ponto sem retry. Estimar todas
as falhas como zero perderia consumo parcial; exigir extrato manteria uma recusa
sem áudio sem solução; reconhecer apenas essa prova completa permite registrar
custo incremental estimado zero no ledger existente. Foi escolhido o último,
sem tratar a estimativa como fatura confirmada.

A validação exige finalização encerrada, entrada correspondente, JSON integral
sem duplicação, nenhum áudio/recibo/tarefa aceita, sem voz reutilizada e mesmo
produto/experimento/tenant. Segmentos posteriores, timeouts, respostas truncadas,
identidades conflitantes e cobrança confirmada continuam bloqueados. Recibos de
áudio recebidos antes de falha posterior mantêm custo positivo. A regra não
retoma nem aprova tarefas: a conciliação continua pela tela existente.

Validação local desta correção: 68 testes de conciliação, 43 de reserva sonora e
62 de jobs aprovados (173 casos). A rodada adicional repetiu somente a
conciliação após reforçar a rejeição de recibo malformado. Sem novo endpoint,
migração ou chamada a provedor; comentários de responsabilidade revisados.

A conferência de recuperação também exige preservar esta estimativa em callbacks
posteriores sem custo. O teste parametrizado do ledger cobre essa nova evidência
junto das conciliações anteriores; a suíte do ledger e a conciliação passaram.

## Parecer 805 e alinhamento do convite

Psique aprovou as escutas 13/14 (US$ 0,044373 estimados), mas devolveu ADJUST sobre os MP4s 77/78. Pediu identificação do site, convite de entrada correto, captura durante a fala e retenção final menor. A avaliação textual custou US$ 0,592888 estimados. A imagem 659 permanece adequada; não se repete homologação de produto.

Foram comparados: encurtar somente os vídeos (não corrige o atraso durante toda a fala); deslocar a voz para o fim (piora ritmo e cria retrabalho); corrigir a regra compartilhada para acompanhar o início do convite, acrescentar identificação editorial e ajustar a duração. A terceira opção resolve a causa sem trocar a prova ou ampliar a promessa.

Matriz local: CTA completo e último frame, abertura identificada sem alterar captura, dois produtos, ausência de rótulo legada, texto literal com pontuação/percentual, rótulo inválido rejeitado antes de consumo, frontend desktop/iPhone/Pixel sem overflow. Os custos e as autorizações existentes permanecem separados; testes sintéticos não são mercado. A Big Idea de manhã/noite permanece sem prova na v3 e não será introduzida no vídeo como capacidade já entregue.

Validação desta correção: 76 testes backend, 24 testes do compositor/relógio com FFmpeg real e dois do editor passaram; build do frontend aprovado. Desktop, iPhone 15 Pro e Pixel 7 preservaram rótulo, contrato e largura. A fixture de pixels e o rótulo com percentual/pontuação foram inspecionados. Nenhum dado de teste foi gravado como resultado comercial.

## Callback sem confirmação após preservação da voz

A demonstração 21300 recuperou os seis áudios do job 21299, cuja duração medida excedeu o alvo. O anúncio 21298 persistiu os recibos e o progresso de 95%, mas a thread ficou em `BackendVideoClient.postForJob → Mono.block` esperando a resposta. O dump da JVM e o evento persistido confirmam ausência de nova chamada ao fornecedor nesse ponto.

Foram comparados: reiniciar e repetir a produção (risco de duplicação de locução); apenas registrar falha manual (não previne recorrência); limitar cada confirmação e recuperar a mesma voz auditada. A terceira opção foi adotada. O executor limita chamadas a 30 segundos, preserva três tentativas e registra contexto/stack trace. O backend recupera falhas de integração somente com recibos completos, identidade, texto e hash conferidos. Claim de órfão que já recebeu voz fica bloqueado contra consumo repetido.

Matriz: resposta perdida após persistência, payload idêntico no replay, esgotamento finito, chamada válida posterior, job original e outra identidade, recuperação de voz completa, recibo ausente rejeitado e contratos anteriores preservados. Nenhum teste chama fornecedor pago. A conclusão depende dos pareceres sobre os arquivos finais, sem substituir seleção humana.

A inspeção do MP4 21300 também mostrou identificação do site sobreposta ao aviso obrigatório de IA. O compositor reserva agora faixas distintas; uma regressão FFmpeg mede a faixa inteira antes da aplicação do aviso. Esta correção entra no mesmo lote local, e a remontagem reutilizará a voz preservada.

Validação local do lote: 101 testes de jobs/recuperação do backend passaram; o módulo de vídeo concluiu 291 testes sem falhas (nove casos FFmpeg condicionais fora dessa rodada). Após ajustar a faixa de texto, os 17 testes do compositor foram executados com FFmpeg habilitado, sem skips ou falhas. Diff e comentários revisados antes da publicação.

## Correção técnica da candidata ainda não revisada

A leitura do percurso confirmou outra restrição antes da retomada: a demonstração 79/21300, produzida mas ainda sem parecer, não poderia ser remontada para remover a sobreposição sem rejeição humana ou inferência adicional. Não se registrou rejeição em nome do usuário.

Compararam-se: pagar revisão de defeito já conhecido (retrabalho); registrar rejeição humana não fornecida (incorreto); permitir remontagem estrita da candidata com voz preservada. A terceira alternativa mantém seleção humana e revisores independentes. A regra bloqueia parecer vigente, revisão em andamento, voz ausente e campanha em execução. A matriz cobre Mira, outra identidade, caminho anterior com ADJUST e impedimentos de seleção/consumo.

Validação local da passagem: 117 testes dos contratos de jobs, recuperação, evidências e seleção passaram. A rodada incluiu candidata sem recibo (bloqueada), bloqueios históricos sem revisão ativa (não impedem correção), revisão IN_PROGRESS (bloqueia), identidade diferente, parecer vigente e preservação dos cinco áudios.
