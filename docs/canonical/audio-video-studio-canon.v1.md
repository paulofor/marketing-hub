# Estudio de Audio e Video Canon v1

## Decisao de produto

O Marketing Hub tera um item de menu separado chamado **Estudio de Audio e Video** para producoes audiovisuais sofisticadas.

Esse estudio nasce para videos com:

- varias cenas;
- roteiro longo;
- narracao;
- trilha sonora;
- desenho de ritmo;
- continuidade visual;
- pos-producao;
- revisao editorial;
- potencial de uso em campanhas, conteudo organico premium, treinamento, apresentacoes comerciais e PDEs de maior valor percebido.

## Separacao dos fluxos atuais

Os fluxos existentes de video devem continuar nos seus lugares atuais:

- criativos de experimentos;
- videos curtos organicos;
- videos especificos de PDEs;
- aprovacao de videos;
- geracao e tratamento de videos usados dentro de funis ja existentes;
- provedores e tratamentos operacionais atuais.

O novo estudio nao substitui esses fluxos. Ele cria uma frente gradual para producoes mais complexas, com mais etapas criativas e maior exigencia de acabamento.

## Direcao comercial

O objetivo do estudio e aumentar o valor percebido dos produtos digitais e campanhas do Marketing Hub, permitindo criar pecas audiovisuais mais sedutoras, narrativas e memoraveis.

Cada evolucao do estudio deve preservar a regra principal do sistema: gerar vendas de produtos digitais que entregam valor real com IA aplicada ao dia a dia.

## Brief Cinematico de Video Comercial PDE

Todo video comercial de PDE deve nascer de um **Brief Cinematico de Video Comercial PDE** antes da geracao de roteiro, storyboard, imagem-base, renderizacao ou publicacao.

Esse brief deve ficar associado ao perfil/playbook comercial do video e conter, no minimo:

- papel no funil: anuncio, landing, amostra, prova, onboarding, retencao ou outro papel comercial explicito;
- promessa que o video precisa tangibilizar;
- dor que precisa aparecer visualmente;
- cena principal;
- sujeito, personagem, produto ou interface principal;
- movimento do sujeito, da interface ou do ambiente;
- camera e enquadramento;
- luz e estetica;
- emocao esperada;
- CTA ou transicao para a proxima etapa;
- restricoes de qualidade, incluindo clareza, audio, aderencia a oferta, uso comercial aprovado e restricoes do provedor;
- prompt cinematografico final quando houver geracao direta por modelo de video.

O brief nao deve ser decorativo. Ele deve deixar claro qual transformacao o consumidor precisa enxergar e como essa cena reduz esforco mental, aumenta desejo, prova o mecanismo ou aproxima o proximo clique.

## Categorias e duracao

O Estudio de Audio e Video deve trabalhar por **categoria de projeto**, para suportar tanto
pecas comerciais curtas quanto producoes longas sem misturar objetivo, roteiro e governanca.

Categorias canonicas:

- **Video Comercial Curto** (`COMMERCIAL_SHORT`): 6 a 60 segundos. Uso: hero de PDE, anuncio,
  Reels, Stories, retargeting, amostra e criativo de aquisicao.
- **Video Longo / VSL** (`LONG_FORM`): 180 segundos ou mais. Uso: pagina de venda, paywall,
  explicacao de oferta, quebra de objecoes e conteudo comercial profundo.
- **Video Institucional / Conteudo** (`INSTITUTIONAL_CONTENT`): duracao definida pelo contexto,
  desde que tenha objetivo comercial explicito, brief, roteiro, uso e criterio de aprovacao.

O Marketing Hub deve bloquear criacao, edicao ou geracao de projeto quando a duracao nao for
compativel com a categoria informada. A tela pode orientar o operador, mas o backend deve ser a
fonte de verdade desse bloqueio para evitar criacao por API, automacao ou dados legados.

Para a MUSA v7, a categoria correta do video hero cinematografico e **Video Comercial Curto**,
com duracao alvo de aproximadamente 30 segundos.

## Separacao backend e providers de video

O backend principal do Marketing Hub deve ser responsavel por:

- cadastro do projeto, brief, blueprint, cenas, biblias visuais e regras de continuidade;
- persistencia de jobs, eventos, status, custos, evidencias, assets, HLS e metricas;
- endpoint de pendencias para o executor;
- callbacks de progresso, sucesso, falha, expiracao e registro de artefatos;
- relatorio operacional e comercial para a tela.

Toda tentativa do Estudio deve abrir uma entrada idempotente no ledger financeiro **antes da primeira chamada externa paga**. Progresso, sucesso, falha e expiracao atualizam essa mesma entrada; ausencia de callback nunca pode apagar o consumo. Isso vale para roteiro, storyboard, render de cena, voz, trilha, montagem, pos-producao, imagem auxiliar e rotas legadas de audio ou video enquanto elas existirem.

Cada entrada deve preservar produto, plano e experimento quando conhecidos, tipo de ativo, etapa/origem, provedor, modelo, status e horarios. O fluxo canonico do Estudio exige produto para iniciar consumo novo. Rotas legadas de midia sem esse contexto ainda devem registrar a tentativa como nao atribuida e bloquear a reconciliacao, nunca descartar o custo nem transferi-lo para outro planejamento.

Custo reportado pelo provedor prevalece; estimativa so pode usar tabela versionada e deve ser identificada como estimativa. Quando nao existir preco confiavel, o custo permanece nulo e reduz a cobertura. Nunca registrar zero apenas porque o provider nao retornou valor. Cada retry e nova chamada externa e uma tentativa financeira independente; atualizacoes de status da mesma tentativa nao podem duplicar custo.

O backend principal **nao deve** implementar integracao direta com providers de video, clientes
HTTP/SDKs de renderizacao, adaptadores Luma, Kling, HeyGen, Runway, Veo ou qualquer executor de
provider. Essas responsabilidades pertencem ao modulo executor de video, atualmente tratado como
`video-management-service`, que deve consumir pendencias oficiais do backend e reportar resultados
pelos callbacks canonicos.

Essa separacao evita que o Estudio vire um conjunto caotico de chamadas externas dentro do backend:
o Hub opera como cockpit no-code e fonte de verdade; o modulo de integracao executa a producao.

## Biblia visual obrigatoria

Antes de qualquer renderizacao, producao, revisao ou aprovacao de video no Estudio, o projeto deve ter uma **biblia visual** persistida no backend.

A biblia visual deve conter, no minimo:

- personagens e imagens mestre aprovadas, com angulos, figurino, acessorios e URLs ou IDs de referencia;
- ambientes e imagens mestre, incluindo plano geral, angulo oposto, lateral, entradas, saidas, objetos fixos e mapa simples;
- objetos, produto e marca, incluindo telas do PDE, interface, logotipo, textos e referencias separadas para composicao;
- direcao visual, incluindo estilo, luz, lente, textura, paleta, enquadramento e nivel de realismo;
- plano de geracao de imagem, priorizando solicitar primeiro ao modelo de imagem OpenAI as imagens mestre e frames-chave antes de gerar video;
- regras de continuidade para preservar rosto, cabelo, figurino, escala, objetos, arquitetura, temperatura de cor e identidade visual entre cenas.

O backend deve bloquear status de construcao ou liberacao de video quando qualquer bloco estiver ausente. A tela deve coletar esses dados como etapa de pre-producao premium, mas nao deve ser a unica barreira de qualidade.

## Prompt operacional e imagem inicial por cena

O prompt comercial de cada cena deve ser editavel no Marketing Hub e persistido no projeto antes de qualquer geracao paga. O repositorio preserva templates, regras de seguranca e contratos dos providers; o banco preserva a versao operacional escolhida para o projeto. O Estudio deve bloquear o render quando houver alteracao de prompt ainda nao salva, garantindo que auditoria, job e resultado apontem para a mesma instrucao.

Cada clipe deve perseguir uma unica conclusao visual. Para cenas com personagem, figurino, produto ou ambiente recorrente, o fluxo deve priorizar `image_to_video` com uma imagem inicial aprovada e registrar no job o asset efetivamente enviado. A imagem fixa governa identidade, enquadramento e composicao inicial; o prompt governa principalmente movimento, camera, microacao e restricoes. Texto, legenda e CTA continuam pertencendo a pos-producao deterministica.

Toda cena aceita por um provedor pago deve aparecer no modulo financeiro transversal, deduplicada por job e ordem da cena, com ciclo, horario e identificador externo quando disponivel. A contagem deve representar solicitacoes aceitas pelo provedor, nunca pollings, heartbeats ou jobs recusados antes da criacao. O clipe gerado deve permanecer sem texto embutido; copy, legenda e CTA sao aplicados somente na pos-producao deterministica para permitir correcao sem nova geracao paga.

## Primeiro escopo

A primeira versao deve ser um cockpit de construcao no frontend, deixando clara a fronteira entre:

- videos rapidos e tratamentos existentes;
- producoes sofisticadas do Estudio de Audio e Video.

As proximas evolucoes devem ser incrementais e orientadas por etapas auditaveis: briefing, roteiro, cenas, referencias visuais, voz, trilha, montagem, revisao e publicacao.

## Videos de referencia para analise

O subitem **Videos para analise** do Estudio de Audio e Video deve ser uma fila de envio de
videos externos/de sucesso escolhidos pelo usuario para aprendizado do sistema, e nao uma
listagem passiva de videos ja gerados pelos experimentos.

Cada video de referencia deve registrar, no minimo:

- URL publica ou origem acessivel do video;
- titulo operacional;
- plataforma ou fonte;
- nicho, produto ou contexto comercial;
- papel no funil quando conhecido;
- objetivo de aprendizado, como gancho, ritmo, prova, objecao, CTA, edicao, promessa ou retencao;
- evidencia de sucesso percebida ou medida, como views, comentarios, criativo vencedor, vendas,
  retencao, compartilhamentos ou observacao comercial do operador;
- status da analise, preservando fila, analise em andamento, analisado ou rejeitado.

Essa fila deve alimentar aprendizados reutilizaveis para novos roteiros, criativos, ofertas,
storyboards, cortes e criterios de revisao comercial. Videos de referencia nao devem ser
misturados com assets produzidos em experimentos, porque a finalidade e diferente: referencia
externa para aprendizado versus ativo interno de campanha.

Cada video de referencia deve possuir tela propria de **resultado da analise**, acessivel pela
fila, organizada em etapas semelhantes ao Studio principal:

- evidencias usadas;
- diagnostico comercial;
- analise por sequencia/frame-chave;
- aprendizados que o sistema deve reaproveitar;
- melhorias acionaveis para vendas;
- decisao operacional e proximo movimento.

O resultado deve ser ligado ao video de referencia e exposto pelo backend, para que o frontend
mostre relatorio auditavel sem depender de logs tecnicos ou recomputacao local.

## Etapas premium de producao com IA

A tela do Estudio de Audio e Video deve organizar projetos premium pelas seguintes etapas operacionais:

1. **Estrategia / oferta e funil:** objetivo comercial, publico, dor, promessa, mecanismo, canal, duracao e metrica primaria.
2. **Roteiro / narrativa:** gancho, historia, demonstracao, prova, objecoes e CTA falado ou visual.
3. **Biblia visual / pre-producao:** personagem, ambiente, objetos, marca, direcao visual, imagens mestre e regras de continuidade.
4. **Storyboard / plano de cenas:** cenas curtas com enquadramento, movimento, acao, emocao e transicao.
5. **Audio / voz e trilha:** narracao, ritmo, pausas, trilha, efeitos e legibilidade para consumo sem som.
6. **Geracao IA / provider:** escolha de Luma, Kling, HeyGen ou outro motor conforme tipo de cena, custo, duracao e consistencia.
7. **Montagem / pos-producao:** corte de falhas, ritmo, uniao de cenas, legenda, audio, capa, HLS e fallback MP4.
8. **Revisao / gate comercial:** promessa permitida, clareza, continuidade, audio, prova, CTA, HLS e aderencia ao PDE.
9. **Aprendizado / metricas:** play, retencao, clique, diagnostico, paywall, checkout e compra para decidir novos cortes.

### Curadoria aprendível do agente Videomaker

O agente Videomaker deve avaliar continuamente os modelos disponíveis e candidatos, sem tratar o
provider mais novo ou a maior resolução anunciada como vencedor automático. A comparação deve usar
o mesmo brief e referências, considerando duração nativa, continuidade, áudio, controle temporal,
modalidades de entrada, resolução efetivamente inspecionada com `ffprobe`, custo por ativo aprovado,
tempo de geração, licença comercial, falhas, revisão independente e eventos reais de retenção, CTA,
checkout e venda.

Wan3.0 permanece candidato `DRAFT` enquanto estiver em preview e sem adapter internacional
homologado. Seu diferencial a testar é transformar documentos, páginas públicas, imagens, vídeo e
áudio em uma narrativa de até 30 segundos.

Seedance 2.5 e Hailuo 3 são modelos do provider operacional `RUNWAY`: o executor deve usar o
adapter Runway e exclusivamente o segredo `RUNWAY_API_KEY` já governado pelo Marketing Hub. É
proibido criar, solicitar ou persistir token Pippit, ByteDance ou MiniMax para esses modelos. Cada
modelo deve preservar seu identificador externo próprio (`seedance2_5` e `hailuo3`), modelo, custo,
request, response e resultado de QA no job. Compartilhar adapter e credencial não autoriza mapear
Seedance 2.5 para `seedance2`, nem ativar ou consumir créditos antes de o contrato vigente da Runway
aceitar o identificador e os gates de preço, licença e qualidade estarem homologados. A exportação
anunciada até 4K não deve ser registrada como geração nativa 4K sem inspeção do arquivo e confirmação
do contrato.

Para produção assistida, Apolo deve conhecer o fluxo disponível na plataforma Runway: briefing,
Runway Agent, escolha do Seedance 2.5, referências de personagem, cenário e áudio, geração de
narrativa de até 30 segundos e posterior edição ou extensão. Esse fluxo é especialmente relevante
quando continuidade narrativa e aderência multimodal forem mais importantes que gerar cenas
isoladas. A descrição da ByteDance sobre narrativas de 30 segundos, referências multimodais mais
precisas e edição mais confiável deve orientar a hipótese de uso, mas não substitui homologação.
Apolo deve distinguir explicitamente a capacidade da plataforma assistida do contrato da API
automatizada vigente; nenhuma capacidade da interface autoriza inventar endpoint, duração ou
modalidade no adapter. Toda execução continua sujeita a teto financeiro, licença, persistência das
referências, custo, request/response, inspeção do arquivo e QA independente.

O Videomaker pode propor uma homologação comparativa, mas não pode ativar provider, consumir
créditos, publicar ou promover uma peça sem os gates técnicos, financeiros, jurídicos e comerciais
persistidos no backend.

### Estrategia e aprendizado por experimento

Cada video comercial deve preservar a hipotese, funcao no funil, framework, evidencias cientificas, limites da promessa, plano de medicao, resultados reais, decisao e proxima versao. Pecas complementares usam o mesmo `strategyGroupKey`: o video de campanha qualifica pela dor e o hero do PDE aprofunda mecanismo e jornada, mantendo `message match`. Resultados estimados nunca podem ser registrados como venda; a decisao permitida e `COLLECTING`, `CONTINUE`, `ADJUST` ou `STOP`.

Essa ordem evita desperdicio de geracao: nenhum video premium deve avancar para renderizacao antes de existir clareza comercial, roteiro estruturado e referencias visuais suficientes para preservar consistencia e valor percebido.

A pagina de edicao do projeto deve apresentar os blocos operacionais nessa mesma ordem, tanto visualmente quanto no documento HTML. A navegacao resumida pode permanecer no topo, mas roteiro, storyboard, audio, geracao, montagem, revisao e aprendizado nao podem ser separados por listas de projetos, escopo institucional ou outros paineis auxiliares. Em telas pequenas, a navegacao das etapas pode ter rolagem horizontal, enquanto o conteudo principal permanece em uma unica sequencia vertical.

Os cabecalhos das nove etapas devem usar o mesmo componente visual, com tipografia, borda, fundo e espacamento consistentes. Paineis internos podem variar conforme o tipo de conteudo, mas nao podem competir visualmente com o inicio de uma nova etapa nem fazer etapas equivalentes parecerem componentes sem relacao.

Para vídeos montados por cenas, cada item do plano deve declarar localização, ação visível e direção de câmera próprias, além da função comercial. Prompts genéricos que repetem personagem caminhando, pose ou enquadramento não comprovam mecanismo e devem ser bloqueados na revisão. Quando o roteiro exigir espelho, a direção visual pode usá-lo como objeto narrativo, mas deve proibir câmera/equipe refletida, duplicação impossível da personagem e reflexos incoerentes. O provider escolhido também deve respeitar sua duração direta máxima; suporte declarado a montagem por cenas só pode ser oferecido na tela quando o executor realmente gerar e unir os clipes.

Quando uma cena curta precisar demonstrar varias microacoes, o prompt operacional deve ordenar cada plano em intervalos de tempo, descrever o resultado visual verificavel de cada gesto e proibir explicitamente repeticao ou acoes ambiguas observadas nas tentativas reprovadas. Enumerar conceitos em uma unica frase nao comprova que o provider mostrara todos eles; o gate humano deve reprovar a cena quando uma microacao planejada nao for compreensivel sem audio.

O plano de cenas persistido deve ser editavel na tela e deve ser a unica fonte usada para montar os prompts dos renders. A interface nao pode substituir silenciosamente o storyboard salvo por prompts fixos. Quando uma cena falhar por excesso de acoes, o usuario deve poder reduzi-la a uma unica conclusao visual, salvar e gerar novas variacoes auditaveis sem alterar as outras funcoes narrativas.

O Estudio deve apresentar uma visao auditavel do storyboard por cena e por tentativa, composta no backend a partir do plano persistido, jobs, assets, montagens e ledger financeiro. Cada linha deve mostrar funcao comercial, duracao solicitada, creditos previstos, creditos efetivamente consumidos quando liquidados, arquivo produzido e percentual de aproveitamento editorial. Arquivo produzido nao equivale a material aproveitado: o percentual so pode ser positivo quando houver evidencia persistida de uso em uma montagem pronta; ausencia de liquidacao, arquivo ou montagem deve aparecer como pendencia explicita, nunca como zero inferido ou sucesso.

Quando a estratégia exigir montagem narrativa, o Estúdio deve criar um job independente por cena, vinculando projeto, ordem e função comercial no metadata auditável. O operador deve revisar e selecionar exatamente um clipe pronto por função antes de solicitar a montagem. Uma cena reprovada deve poder ser regenerada isoladamente, e a montagem não pode avançar com cenas ausentes ou ainda em processamento. A publicação continua bloqueada até a revisão humana do vídeo final.

Cada plano aprovado deve gerar e persistir um quadro final normalizado. A partir do segundo plano, o backend deve exigir um job anterior aprovado do mesmo perfil e substituir a imagem inicial genérica por esse quadro, registrando a estratégia `LAST_FRAME_TO_FIRST_FRAME`, job e asset de origem. A tela deve orientar a geração sequencial e impedir que um plano intermediário seja gerado sem a ponte anterior. A montagem deve usar microtransições curtas, auditar duração e média por plano e, quando houver seis ou mais planos, reprovar média superior a quatro segundos. Após a montagem aprovada, o backend deve enfileirar automaticamente a finalização contínua de voz e legenda; atividade técnica não substitui revisão humana nem resultado comercial.

A duração validada pelo provider deve ser a do clipe isolado solicitado, e não a duração total planejada para o vídeo montado. O backend deve rejeitar montagens do Estúdio que não contenham exatamente uma cena de cada função `DOR`, `RESULTADO`, `MECANISMO` e `CTA`, todas vinculadas ao mesmo projeto; a tela deve substituir a variação selecionada quando outra opção da mesma cena for aprovada.

A seleção automática do provider deve interpretar somente uma escolha explícita da tela ou a marcação textual `como principal`. A simples presença do nome de um provider no plano não constitui seleção, pois ele pode estar documentado como alternativa ou reprovado; nesse caso, o Estúdio deve manter o provider padrão aprovado e nunca iniciar geração com a opção proibida.

## HLS para PDEs

Quando um projeto do Estudio de Audio e Video gerar material para PDE, a entrega publicavel deve incluir HLS (`.m3u8`) gerenciado pelo Marketing Hub. O arquivo MP4 pode ser preservado como master/origem, mas a URL usada pelo PDE deve ser a playlist HLS registrada no ativo comercial ou no job correspondente.

A tela do Marketing Hub deve permitir identificar se o video possui HLS pronto para PDE. Video sem HLS nao deve ser tratado como pronto para publicacao em PDE, mesmo que possua MP4 aprovado.

O gate comercial final deve ser calculado no backend a partir de evidencias persistidas. Uma peca so pode aparecer como `READY` quando derivar de montagem narrativa auditavel com exatamente `DOR`, `RESULTADO`, `MECANISMO` e `CTA`, estiver finalizada com narracao pt-BR, legenda queimada legivel em mobile e VTT, preservar CTA funcional, possuir HLS `.m3u8` e tiver revisao humana final registrada. `VIDEO_READY` isoladamente significa apenas conclusao tecnica e nunca autoriza publicacao comercial.

Quando existir uma playlist HLS ja publicada fora do fluxo completo de render, ela deve ser cadastrada ou corrigida no ativo comercial do Marketing Hub antes de ser usada por PDE. O operador deve conseguir salvar a URL `.m3u8` no Hub, associada ao experimento/projeto/job, para preservar governanca de custo, revisao, aprovacao e rastreabilidade.

# Monitoramento governado de agentes

- Projetos legados de vídeo podem iniciar ciclo sem `commercialPlanId`; nesse caso, Plutus recebe somente o snapshot segregado do ledger sem plano e o teto explícito do ciclo.
- Ausência de plano nunca autoriza atribuir custos a outro planejamento nem transformar o teto em meta de gasto.
- O monitor administrativo deve expor, por agente, trabalho atual, estado, última atividade, dificuldade e decisão externa pendente usando dados persistidos das tarefas e pipelines.
- Apolo permanece aguardando enquanto o gate financeiro estiver pendente; Plutus aparece como responsável pela decisão do mesmo ciclo.
- Todo novo ciclo pago deve persistir antes do gate um objetivo de aprendizado verificável e um criterio de sucesso observavel. O ledger deve ser segregado por `videoProductionCycleId`, iniciar vazio e registrar cada tentativa nova sem misturar custos historicos. Apolo deve primeiro montar com ativos existentes; somente a cena causal que continuar impedindo a qualidade pode originar novo ciclo pago.
- Falha não recuperável do provider, saldo insuficiente, custo desconhecido ou job que já possua asset bloqueia a reconciliação automática de Apolo. Uma substituição automática pode ocorrer apenas uma vez para migrar job legado; nova tentativa exige avaliação explícita do material existente e novo gate financeiro. Montagens devem registrar duração e custo do conjunto completo, nunca apenas da última cena.
