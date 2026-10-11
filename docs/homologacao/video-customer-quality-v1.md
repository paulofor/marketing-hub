# Qualidade audiovisual para a cliente — v1

## Evidência e causa

Em 11/10/2026, a tela e o MCP confirmaram a rejeição de Mira #53/#54, experimento #102.
A copy persistida continha “Experimentar Mira em acesso privado”. O briefing #634 misturava
instruções internas e texto final. Os jobs #21256/#21258 combinaram captura única,
redução da página inteira e sobreposição estática nos cortes funcionais. Psique #655
aprovou o criativo estático #654; não há parecer dela sobre esses dois vídeos finais.
A consulta de logs respondeu, mas a janela retida não trouxe esses jobs históricos.
A evidência disponível é o arquivo final, metadados persistidos e o código.

## Alternativas

| Caminho | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Corrigir somente a copy do caso | Baixo esforço | Mantém montagem e ausência de prova final | Insuficiente |
| Regenerar todas as mídias | Maior variedade potencial | Custo adicional e reincidência do contrato | Evitar repetição sem causa corrigida |
| Corrigir produção/revisão existentes e reutilizar fontes compatíveis | Fecha causa compartilhada, preserva auditoria | Exige regressões entre módulos | Adotado |

## Matriz local de aceite

| Dimensão | Caso e resultado exigido |
| --- | --- |
| Copy | Bloquear linguagem interna em fala/legenda/CTA antes de TTS; permitir metadados e ressalvas funcionais |
| Visual | Preservar movimento, manter recortes da prova legíveis e impedir tela longa reduzida como demonstração |
| Revisão | Arquivo final e linha do tempo vinculados; ausência não pode ser aprovação sensorial |
| Recuperação | Rejeição pede correção, não aprovação; histórico e callback preservados sem inferência duplicada |
| Integração | Produtor, finalizador e leitura de ciclo usam contratos reais com dependências locais |
| Regressão | Caso Mira, produto sintético com outros IDs e caminho já válido |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; ler quadros no tamanho de exibição |
| Áudio | Integridade/duração/sincronismo técnicos separados de escuta perceptual |
| Finanças | Teto de US$ 10 audiovisual cumulativo, custo desconhecido não zero; sem mídia nesta correção |
| Métricas | Testes e simulações segregados; nenhuma alegação de venda, lucro ou conversão |

## Resultados locais

- 14 frames extraídos dos MP4s rejeitados com o capturador real, SHA-256 validado, abertura,
  progressão e encerramento. O armazenamento real do backend aceitou as provas usando S3
  substituído por test double, sem enviar imagens de teste ao storage produtivo.
- Psique: suíte Java completa passou (181 testes declarados, 10 opcionais de outros harnesses
  não habilitados); Íris: 79 declarados, 3 opcionais não habilitados. Captura real deste caso
  foi executada separadamente, não substituída por esses skips.
- Apolo: testes de copy, storyboard, pós-produção e sobreposição passaram; FFmpeg real
  conferiu vídeos 15/30 s, recorte e preservação dos outros planos. Uma nova montagem local
  de 20 s com fonte auditada e captura original passou sem qualquer chamada de provedor.
  Essa candidata local tem apenas legenda: não comprova locução final nem aceite de Psique.
- Backend: regressões de identidade/tenant, revisão por hash, armazenamento, reabertura e
  orientação passaram com Mira e IDs sintéticos diferentes. Parecer de PNG não libera
  vídeo novo; aprovação histórica não é alterada.
- Node: contrato de bytes/hash, ausência de redirect e amostragem temporal passou. O teste
  integra a suíte existente de navegador, sem framework paralelo.
- Prévia de 20 s reproduzida em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; duração,
  avanço de reprodução e leitura do recorte conferidos. A audição não foi alegada.
- Nenhum gasto, publicação comercial ou métrica humana foi produzido por estes testes.

## Adoção e reversão

Adotar somente após checks do PR, publicação dos módulos e conferência da revisão real.
Se a validação rejeitar uma entrada válida, preservar o job e sua auditoria, corrigir o contrato
com uma fixture de regressão e reproduzir localmente. Não remover o gate nem repetir inferência
para contornar a falha. Rastrear a entrega pelo commit e pela versão do contrato.


## Sincronização observada na produção — 11/10/2026

A montagem #21268 mostrou outra causa: a frase de exemplo começava em 7,488 s, mas o
storyboard colocava a prova em 24–27 s. No anúncio #21269 a fala começava em 7,776 s e a
prova só entrava em 12 s. Voz/legenda estavam sincronizadas entre si, porém a prova usava
outro relógio. Sete screenshots fixos também podiam omitir um inserto curto.

Foram comparados ajustar somente os cortes do caso, sintetizar novamente a locução para
seguir o storyboard ou sincronizar a prova pelo áudio já auditado. A última opção preserva
custo e identidade e corrige a fonte compartilhada. A finalização passa a mostrar a prova
no trecho explicitamente associado ao exemplo, com seis segundos para leitura e dois
segundos reservados ao encerramento. Contrato ambíguo bloqueia, sem adivinhar associação.

- Regressões incluem Mira, outro produto, entrada ambígua, tempo inválido e composição
  legada. FFmpeg real verifica pixels durante a fala e ausência no antigo intervalo.
- O backend entrega os momentos de cada trecho narrado e da prova à captura de Psique.
  A amostragem mantém abertura/encerramento e passa a incluir esses instantes.
- Nova finalização da mesma fonte rejeitada conserva os recibos e bytes de TTS quando
  texto, fonte, tenant e duração são compatíveis. A autorização de nova síntese permanece
  falsa. O service continua impedindo substituir finalização aprovada ou ativa.
- O teste local usa os arquivos reais recebidos de Mira com servidor HTTP local e recusa
  qualquer chamada de síntese. A conferência operacional posterior é registrada no PR.

Validação desta correção: 67 testes de backend; 270 do módulo audiovisual (um cenário
opcional não aplicável); 181 de Psique (dez opcionais não habilitados). Os 44 testes de
navegador passaram. A execução final foi sequencial para evitar a concorrência de
Chromium com os testes Java na sandbox. Os dois MP4s de 20/30 s foram recompostos localmente usando os
10 áudios recebidos, com zero request de síntese. A captura real gerou 26 frames incluindo
a prova; reprodução foi conferida em desktop, iPhone 15 Pro e Pixel 7 emulados.

A captura também revelou que simular a resposta HTTP sem Range podia devolver outro
instante, e a ausência de viewport mobile reduzia a imagem a uma fração da tela. O
capturador agora usa Blob dos bytes verificados, confere o tempo efetivo e declara o
viewport. Um MP4 determinístico vermelho/azul testa pixels reais de abertura e fim,
impedindo que screenshots incorretos sejam rotulados como evidência do instante pedido.

### Regressão obrigatória na imagem do agente

O CI do PR #5580 executou os testes funcionais, mas pulou a verificação de pixels porque
a imagem genérica de navegador não possuía FFmpeg no caminho esperado. A regressão foi
separada em `final-video-pixel.smoke.mjs` e passou a usar um MP4 sintético versionado
(vermelho por três segundos, azul por três segundos, H.264, 90×160, 5 fps) e Canvas para
conferir os pixels. Não depende mais de FFmpeg nem de download durante o teste.

O workflow executa obrigatoriamente esse teste na imagem Docker do próprio agente, com
o usuário de runtime, filesystem somente leitura e rede desabilitada, além dos 43 testes
Node existentes. Ausência de navegador, codecs ou captura correta falha o job; não há skip.
A fixture foi sintetizada localmente com FFmpeg (`color` + `concat` + `libx264`) e contém
somente cores, sem produto, pessoa, cliente ou dado comercial.

### Revalidação após a conclusão do processo de origem

Após a produção corrigida #59/#60, o comando de revisão da execução #55 foi aceito, mas
aguardou o pai #54, já concluído. O problema foi reproduzido localmente em dois produtos:
os testes esperavam o despacho da revisão e receberam `WAITING_PARENT`.

Foram comparadas uma revisão independente fora da árvore (menor mudança, mas perde a
passagem automática), uma nova execução completa (mais ampla e sujeita a retrabalho) e
recuperar a delegação existente. A terceira alternativa mantém a identidade e as provas:
somente um comando explícito posterior à conclusão, com contexto vigente, reabre a espera
dos ancestrais concluídos. O pai conserva seus aceites e aguarda o retorno validado do filho.

Matriz local adicional: despacho único em dois produtos; retorno confirmado antes de encerrar
o pai; pausa e retomada durante a revisão; nenhuma tarefa sem comando novo, com comando
antigo, sob PAUSED/PAUSING/ERROR/CLOSED, STOP, contexto fechado, produto divergente ou outra
reserva ativa. As delegações antes válidas e os controles de vídeo continuam nas regressões.
A alteração não autoriza gasto novo nem substitui revisão independente ou seleção humana.

Resultado local desta passagem: 222 testes de processo, persistência, fila, revisão audiovisual
e recuperação passaram, sem falhas ou skips. Incluem os dois casos que falhavam antes com
`WAITING_PARENT`. A revisão real de Mira e os workflows de publicação ficam vinculados ao PR.

### Leitura de provas durante a aprovação audiovisual

A retomada criou a tarefa #723, mas o leitor de imagens recebeu HTTP 409 antes de executar
o modelo. O banco mantinha Mira #9 aberta em `VIDEO_APPROVAL`; o log de
`IrisLearningCycleContext` registrou `Ciclo privado não aberto`. O contexto aceito por #654
estava `READY` na preparação anterior. A imagem e sua versão não haviam sido substituídas.

Foram comparados regredir a etapa do ciclo, usar apenas o contexto histórico congelado ou
reconhecer as etapas audiovisuais já definidas entre ajuste e homologação. A terceira opção
preserva o estado real e confere novamente produto, versão, gate e hashes atuais; não ignora
uma prova substituída nem renova autorização financeira ou comercial.

A matriz adicional liga o produtor real do contexto ao serviço real de leitura dos pixels,
com persistência e armazenamento substituídos localmente: mesma prova antes/depois da
transição, identidades de Mira e outro produto, quatro etapas audiovisuais e recusa de ciclo
encerrado, fases comerciais, versão/gate divergentes, tarefa posterior ou resultado alterado.
Antes da correção, os quatro estados falhavam e as duas leituras integradas retornavam o
mesmo HTTP 409 observado. Não houve chamada de IA na tentativa operacional bloqueada.

Validação desta correção: 597 testes declarados, 594 executados sem falhas; três harnesses
opcionais de replay/captura não habilitados nesta rodada já haviam sido executados nas
validações anteriores dos vídeos. A nova passagem produtor → leitor foi executada nos dois
contextos, sem skips. Inclui 92 regras de arquitetura. A atestação sucessora v20 preserva
a experiência privada v3 e atualiza somente a compatibilidade dessa leitura.

### Resolução de relatório histórico — 11/10/2026

A execução 724 revelou uma segunda causa independente da etapa: o gate 545 aponta
para o relatório de controles v1, enquanto o contexto atual entrega v2. A v2 foi
produzida em 10/10 para a integração audiovisual; Têmis 633 havia examinado a v1 em
07/10. Preservar o relatório antigo não significa aprovar a integração nova.

A consulta por hash no catálogo existente permite conferir o recibo histórico sem
substituir a prova atual. Regressão inicial: duas identidades falham com READY
esperado/BLOCKED obtido. Critérios de aceite: ler a mesma imagem aceita em
VIDEO_APPROVAL; manter relatório atual e recibo anterior distintos; recusar bytes
adulterados, produto/versão/hash desconhecidos, gate inválido e ciclo fechado;
nenhuma gravação, inferência ou aprovação implícita. O replay local usa somente
SELECTs exportados de Mira, incluindo alvo 724, gate 545 e histórico exato.

Resultados: 55 testes direcionados passaram; o replay dos dados atuais passou junto
a eles (56 execuções). A regressão ampliada executou 651 testes sem falhas, incluindo
92 regras de arquitetura; duas rotinas de replay opcional ficaram fora da rodada sem
exportação. O replay relevante foi executado separadamente com o arquivo exportado.
Bundle de revisão: 516 arquivos e 111 manifestos válidos. A atestação v21 mantém
a experiência v3 e seus limites privados; relatórios v1/v2 permanecem imutáveis.


## Parecer independente #727 e sequência demonstrável

Em 11/10, Psique inspecionou 26 frames dos MP4s #59/#60 e solicitou ADJUST por categoria
ambígua, primeira ação pouco concreta e encerramento repetitivo. A prova recebeu 3/5 em
legibilidade; clareza, primeira ação e progressão ficaram em 2/5. A imagem #569 continuou
adequada. O backend criou indevidamente Íris #728 para produção estática; a tarefa terminou
e foi preservada, com custo estimado US$ 0,614282, antes da pausa das próximas passagens.

A mudança reutilizável acrescenta sequência de capturas homologadas por trecho narrado,
com resolução no backend, conferência de tenant/versão/hash no executor e retorno tipado
`correctionScope`. Contratos antigos mantêm o encaminhamento conservador; parecer novo
exclusivamente audiovisual não reabre produção de imagem. Não se altera o parecer histórico.

Matriz definida antes da rodada: entrada → resultado → continuidade em capturas do mesmo
produto; recusa de outro tenant/experimento, hash e recorte inválidos; trechos fora de ordem,
sobrepostos ou sem tempo de leitura; repetição idempotente da mesma finalização; revisão
vinculada ao hash final; retorno exclusivo de vídeo versus imagem/misto/legado; Chromium
em desktop/iPhone/Pixel, integridade audiovisual e separação entre frames e audição.
Provas de teste não são evidência de mercado. Nenhuma meta comercial ou orçamento é ampliado.


A revisão ponta a ponta também confirmou que a proteção de recuperação bloqueava qualquer
nova copy. A finalização distingue agora recuperação idêntica e revisão de locução confirmada:
a segunda exige fonte rejeitada/falha, financeiro aprovado do mesmo ciclo e custo conhecido
inferior ao teto vigente. O monitor bloqueado impede nova síntese; custos pendentes seguem
pendentes. Falhas de enquadramento/relógio após TTS reutilizam os recibos íntegros, evitando
cobrar novamente pelo ajuste visual. Testes incluem texto igual, texto alterado sem confirmação,
financeiro pendente, teto atingido, custo desconhecido e recuperação das duas falhas de prova.


Validação desta iteração: 791 testes backend declarados (788 executados; três replays
opcionais não aplicáveis), incluindo 92 regras de arquitetura; todos os executados passaram.
A primeira rodada detectou a incompatibilidade entre DTO legado e record canônico da operação;
a regra agora admite somente records no subpacote de pós-produção, mantendo a proibição de
serviços executáveis. A rodada direcionada posterior passou integralmente.
Psique: 182 declarados, 172 executados e dez opcionais de outros harnesses. Apolo: suíte
completa sem falhas; FFmpeg real e integração da sequência foram executados adicionalmente
com fontes recebidas e TTS simulado em servidor local. Frontend: 26 testes, typecheck e build;
editor e reprodução conferidos em desktop/iPhone/Pixel, sem transbordamento horizontal.
A voz simulada valida sincronização e recuperação, não naturalidade ou efeito comercial.

A fila exige `PDE_VIDEO_POST_PRODUCTION_V2` para a sequência: filtro SQL antes da paginação
e checagem de claim impedem que um executor antigo ignore os trechos durante o rollout.
O contrato dos recibos de voz permanece V1; a capacidade nova continua aceitando-o.


Na inspeção da peça incidental #728/658, o cabeçalho continuava vazando “APLICAÇÃO WEB
PRIVADA”: era imposto pelo renderizador e por seu contrato versionado. A correção remove
o rótulo interno, valida os seis campos visíveis e preserva fontes, limites e identificação
de dados de teste. Antes da mudança, sete regressões falharam: duas sobre o cabeçalho e
cinco sobre textos operacionais, em diferentes identidades. Não se reescreve #728 nem se
aprova essa imagem; qualquer produção sucessora terá hash e revisão próprios.

Íris: 84 testes declarados; 82 executados sem falhas após a correção, incluindo replay real dos pixels de #523. Dois harnesses opcionais sem entrada permaneceram fora da rodada. A referência de teste não foi enviada como artefato de produção.
