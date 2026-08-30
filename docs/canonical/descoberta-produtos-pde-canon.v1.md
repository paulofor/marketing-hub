# Descoberta de produtos PDE - canon v1

Data de criacao: 2026-07-26

## Objetivo

Definir a regra canonica para descoberta de novos produtos PDE no Marketing Hub.

O modulo de descoberta de produtos PDE existe para pesquisar a internet, encontrar dores
de mercado com grande quantidade de pessoas afetadas, identificar lacunas de atendimento
e transformar esses sinais em oportunidades auditaveis para criacao de produtos digitais
de experiencia.

O modulo nao deve comecar pela pergunta "qual produto podemos criar?". Ele deve comecar
pela pergunta:

```text
Qual dor recorrente, intensa e mal atendida aparece em grande escala e pode ser aliviada
por uma experiencia digital simples, observavel e vendavel?
```

## Pesquisa sem evidencia real

Uma busca valida pode terminar sem oportunidades quando nao encontrar evidencia publica
suficiente. Nesse caso, o worker deve concluir o ciclo com lista vazia e decisao de
pesquisar mais; o backend deve aceitar o resultado e nunca exigir ou fabricar uma
oportunidade apenas para satisfazer o contrato tecnico.

## Principio central

Toda oportunidade de produto PDE deve nascer de cinco provas minimas:

- dor grande: muitas pessoas vivem a situacao, em frequencia relevante;
- dor mal atendida: as solucoes atuais sao caras, complexas, fragmentadas, demoradas,
  pouco personalizadas ou exigem esforco alto;
- encaixe PDE: existe uma microexperiencia digital capaz de gerar valor percebido rapido,
  antes de pedir uma compra maior.
- mecanismo plausivel: existe base cientifica candidata verificavel para sustentar ou
  limitar a explicacao de como a microexperiencia pode ajudar;
- intencao de compra: existem sinais observaveis em precos, concorrentes, anuncios,
  reviews, cursos, contratacao ou busca por alternativas pagas.

Sem essas cinco provas, o material deve ficar como sinal de pesquisa, nao como oportunidade
de produto.

Falha, indisponibilidade ou ausencia de resultado da busca nunca deve gerar evidencia
artificial. O ciclo deve permanecer em pesquisa, com a lacuna explicitada para nova coleta.

## Relação com a estrada do desejo

A oportunidade PDE deve ser avaliada pela estrada:

```text
Desconhecido
-> Importante
-> Compreensivel
-> Plausivel
-> Valioso para mim
-> Desejavel
-> Compravel
```

O modulo deve rejeitar oportunidades que dependam apenas de uma promessa abstrata. Uma
oportunidade forte precisa permitir:

- uma situacao reconhecivel de dor;
- uma analogia simples para entender a categoria;
- um mecanismo plausivel;
- uma microexperiencia de valor;
- uma reducao clara de risco e esforco;
- uma oferta compravel sem salto mental grande.

## Fontes de sinais

A pesquisa deve priorizar fontes publicas que mostrem comportamento real, nao apenas
conteudo promocional:

- perguntas recorrentes em buscadores, foruns e comunidades publicas;
- comentarios e reclamacoes sobre tentativas de resolver a dor;
- reviews negativos e objecoes de produtos existentes;
- videos, comentarios e conteudos educativos com alta recorrencia de duvida;
- tendencias de busca e termos relacionados;
- paginas de concorrentes e alternativas existentes;
- marketplaces de produtos digitais;
- bibliotecas de anuncios quando disponiveis;
- noticias, relatorios setoriais e dados publicos que indiquem escala.

Conversas espontaneas com pessoas do publico tambem sao fontes validas de **sinal inicial**, sobretudo
quando revelam simultaneamente desejo, dor, tentativa anterior e dificuldade para executar. Esse tipo
de relato deve entrar no Marketing Hub antes de existir uma ideia fechada de produto. Exemplos de
linguagem valiosa incluem "queria um desses", "preciso conseguir...", "tentei fazer, mas me perdi" e
"hoje resolvo manualmente".

Uma conversa individual nao comprova escala, mercado ou disposicao de pagar. Ela abre uma hipotese de
investigacao e nunca pode aprovar sozinha uma oportunidade, um Plano Comercial ou uma campanha.

Quando o relato descreve uma tentativa frustrada de criar ou operar um agente de IA, a profissão da
pessoa é contexto, não definição automática do mercado. O primeiro agrupamento deve considerar o
trabalho que ela queria concluir, a entrada disponível, o resultado esperado e o ponto em que travou.
Somente evidências posteriores podem definir segmento, formato de solução e encaixe PDE. A regra
transversal está em `docs/canonical/solucoes-prontas-ia-trabalho-canon.v1.md`.

Conteudo de solucao deve ser usado para medir saturacao e lacuna, nao como prova direta
da dor. A prova da dor deve vir preferencialmente da linguagem do publico.

## Inspiracao atualizada para novos produtos

Por decisao de 2026-08-26, cada novo ciclo de descoberta deve consultar novamente as
colecoes vivas `pesquisas/gartner` e `pesquisas/ia-aplicada`, considerando os artigos
disponiveis no momento da execucao, inclusive os adicionados depois da publicacao deste
canon. O processo nao pode congelar uma lista de arquivos nem copiar o conteudo atual para
o prompt como se fosse uma base permanente.

O ciclo tambem deve consultar, pelo contrato oficial do Marketing Hub, os produtos da
Hotmart que o proprio sistema tenha identificado com sinais de sucesso comercial. Essa
consulta deve usar o catalogo persistido e vigente do backend ou do MOIS, sem acesso direto
ao banco e sem transformar temperatura, posicao, score ou presenca no marketplace em venda
comprovada.

Essas tres fontes sao **inspiracao**, nao validacao automatica. Elas podem orientar temas,
problemas emergentes, mecanismos, formatos de entrega, promessa, prova, oferta, funil e
linguagem, mas nao autorizam copiar produto, marca, texto, criativo ou estrutura proprietaria.
Cada hipotese inspirada ainda deve comprovar dor, escala, desatendimento e intencao de compra
por evidencias independentes do publico.

Para manter rastreabilidade, o dossie deve registrar, para cada inspiracao utilizada:

- colecao e caminho do artigo ou identificador oficial do produto Hotmart;
- data do material e data da consulta;
- padrao comercial ou tecnologico observado;
- hipotese de valor original derivada para o novo produto;
- evidencia independente que confirmou ou descartou a hipotese;
- limite de uso que evita copia e promessa sem prova.

Fonte vazia, indisponivel ou sem item aderente deve ficar registrada como resultado real da
consulta. O processo pode pesquisar mais, mas nunca preencher a lacuna com referencia
inventada nem aprovar uma oportunidade sustentada apenas pela inspiracao.

## Recorte B2C para aquisicao no Instagram

Por decisao de 2026-08-26, quando o canal comercial informado para o ciclo for Instagram, a
Descoberta deve comparar primeiro oportunidades **B2C**, ligadas a uma cena pessoal concreta e a um
desejo que o proprio consumidor reconhece. Crescimento profissional, comunicacao, aprendizagem,
relacionamentos, hobbies, organizacao e experiencias fora da tela sao territorios permitidos; a
categoria nao substitui a comprovacao da dor nem autoriza promessa sensivel.

Nesse recorte, cada candidata deve registrar:

- uma pessoa fisica especifica, o momento em que a necessidade fica urgente e o resultado desejado;
- um gancho compreensivel nos primeiros segundos de um anuncio ou Reel, sem explorar vergonha,
  medo, solidao, rejeicao ou promessa garantida;
- microvalor demonstravel no celular antes da compra e valor principal entregue com baixo esforco;
- uma rota atribuivel entre impressao, clique, inicio da experiencia, momento de valor e checkout;
- alternativas gratuitas e pagas, sinais observaveis de compra e o que torna o mecanismo original;
- dependencia de outra pessoa, conta, integracao ou operacao humana que possa impedir a primeira
  entrega de valor.

## Territorios humanos e entrega pronta

Por decisao de 2026-08-26, a Descoberta PDE deve pesquisar prioritariamente oportunidades que
ajudem a pessoa a experimentar pelo menos um destes territorios de valor: **afeto e pertencimento**,
**reconhecimento** ou **alivio de esforco**. Esses territorios orientam a busca, mas nao constituem
prova universal de demanda. Cada candidata ainda deve vincular o territorio escolhido a pelo menos
duas evidencias independentes do publico e preservar a linguagem observada.

No recorte B2C, a IA deve trabalhar nos bastidores. A proposta nao pode transferir ao cliente o
trabalho de aprender prompting, pesquisar ferramentas, combinar respostas, configurar automacoes ou
montar manualmente o resultado. A candidata deve declarar um contrato `humanValueDelivery` com:

- um ou mais territorios entre `AFFECTION_AND_BELONGING`, `RECOGNITION` e `EFFORT_RELIEF`;
- transformacao percebida e fontes independentes que sustentam sua relevancia para a cena;
- resultado ou artefato pronto que a pessoa consegue usar;
- entrada minima que somente o cliente pode fornecer para personalizar o resultado;
- no maximo cinco passos do inicio ao primeiro resultado utilizavel e valor em ate dez minutos;
- `requiresPromptEngineering: false`, `requiresManualAssembly: false` e
  `usableWithoutAiKnowledge: true`;
- limite de automacao que preserve controle, privacidade, autenticidade e responsabilidade humana.

Template, curso, lista de prompts, tutorial de ferramenta ou conjunto de pecas ainda dependente de
montagem nao atende entrega pronta. Uma ferramenta guiada pode atender quando recebe apenas a entrada
minima, executa o trabalho complexo e devolve uma saida final utilizavel. Afeto nao autoriza promessa
de controlar outra pessoa; reconhecimento nao autoriza vergonha, comparacao humilhante ou status
falso; alivio de esforco nao autoriza esconder trabalho essencial nem prometer resultado garantido.

No prototipo privado, cada leitura deve observar tambem `READY_RESULT_USED`: participante que usou o
resultado pronto sem prompting ou montagem externa. O criterio minimo dessa taxa deve ser maior que
zero, declarado antes do uso, calculado sobre quem iniciou a experiencia e aprovado nas duas
leituras. Interesse, elogio ou entrega gerada sem uso nao substituem esse fato.

O gate deve rejeitar B2B disfarçado de B2C, curso generico sem microexperiencia, produto que dependa
de operacao empresarial para gerar valor e ideia cuja aquisicao no Instagram seja apenas uma
suposicao. Temperatura Hotmart, anuncio ativo, audiencia e pontuacao continuam sendo sinais, nao
vendas. Para declarar uma nova oportunidade superior ao benchmark Rigel, a pontuacao auditavel deve
ser **estritamente maior**, com consenso dos agentes e valor percebido minimo preservado.

## Validacao obrigatoria do momento de compra

Por decisao de 2026-08-26, no recorte B2C para Instagram a priorizacao final deve ser precedida por
uma etapa independente de **Validacao do Momento de Compra**. Artigos, ofertas, anuncios, reviews,
temperatura, ranking, score de agente e intencao declarada podem orientar a hipotese, mas nao
autorizam compara-la com o benchmark nem aprova-la como produto.

Antes dessa etapa, o processo deve qualificar as fontes comerciais. Snapshot Hotmart ou outra fonte
com placeholder, identidade ou URL incompleta, item sem preco e sem qualquer sinal de tracao, data
ausente quando a atualidade for necessaria, vencimento definido pelo plano, erro de coleta ou falta
de aderencia deve ficar com status explicito e bloquear a priorizacao. O ultimo snapshot nominal
pode permanecer como inspiracao historica, mas nao substitui uma leitura atual nem entra como
comportamento recente.

Cada candidata deve registrar uma cena de compra estruturada com:

- pessoa fisica e situacao exata;
- gatilho e prazo;
- consequencia pratica ou financeira de nao agir;
- tentativa frustrada e solucao atual;
- gasto, assinatura, comparacao de preco ou outro comportamento pago observavel;
- orcamento ou limite de gasto quando existir evidencia, sem inventar capacidade de pagamento;
- alternativa gratuita mais forte, incluindo Google, ChatGPT, planilha, amigo ou conteudo;
- vantagem funcional que o prototipo pretende demonstrar sobre essa alternativa.

Depois da pesquisa e antes do score final, Atena congela a estratégia de mercado da candidata;
Dedalo pode materializar um prototipo privado, limitado e sem publicacao; Temis traduz a estratégia
em comunicação; Hermes define somente distribuição, atribuição e mensuração; Psique revisa valor e
esforco. Os criterios de sucesso devem ser declarados antes do primeiro
uso e preservar denominadores para, no minimo, inicio da experiencia, chegada ao microvalor,
preferencia sobre a alternativa gratuita e inicio de checkout. O checkout do prototipo nao realiza
pagamento e seus eventos devem usar marcador explicito de teste ou validacao privada.

Somente duas leituras independentes e consistentes, ambas acima dos criterios predeclarados e sem
bloqueio de Psique ou Temis, liberam a candidata para priorizacao final e comparacao com Rigel. Uma
leitura favoravel isolada, media que esconda uma leitura reprovada ou nova amostragem do modelo nao
atende o gate. O backend persiste entradas, contagens, taxas, decisoes, evidencias e motivos e e a
unica autoridade para liberar a etapa seguinte.

O contrato persistivel `purchaseMomentGate` deve expor, de forma coerente, `required`, `status`,
`sourceQualityPassed`, `finalPrioritizationEligible`, `minimumIndependentReadings`, criterios,
candidatas elegiveis, `humanValueDelivery` e leituras. Em uma aprovacao, o backend nao pode confiar somente nos booleanos
do worker: deve confirmar o vinculo nominal da candidata, a cena e o prototipo privado, recalcular
as taxas de inicio, microvalor, uso do resultado pronto, preferencia e checkout pelas contagens,
exigir IDs distintos, validar a ordem temporal e confirmar as decisoes de Psique e Temis.
Divergencia entre resumo e fatos bloqueia a conclusao.

O resultado deve seguir estas regras:

- `CONTINUAR`: duas leituras aprovadas, preferencia observada e compromisso comercial mensuravel;
- `AJUSTAR`: existe uso ou microvalor, mas algum criterio predeclarado nao foi atingido;
- `PARAR`: a alternativa gratuita vence, a fonte e invalida ou existe risco relevante nao
  controlavel;
- `AGUARDAR_VALIDACAO`: o prototipo ou as duas leituras ainda nao existem.

Intencao, inicio de checkout de teste e parecer de agente continuam separados de venda. Somente
pagamento reconciliado no contrato comercial oficial pode contar como venda ou receita.

## Colecao viva de momentos de compra B2C

Cada ciclo B2C deve consultar novamente `pesquisas/momentos-de-compra-b2c`, incluindo todos os
resumos datados existentes no momento da execucao. A ausencia de resumo diario deve ser registrada
como fonte vazia e manter o gate aberto; `ini.md` define o tema e nao conta como artigo ou evidencia.

O tema canonico da colecao e: **momentos de decisao B2C iminente no Brasil, com situacao pessoal,
prazo, dinheiro ou consequencia material em jogo, tentativa frustrada e solucao paga mal atendida**.
Cada resumo preserva cena, gatilho, prazo, custo do erro, gastos atuais, ofertas pagas, reclamacoes,
alternativa gratuita, linguagem do consumidor, demonstracao em Reel, microvalor em ate dez minutos
e limites de seguranca.

### Cobertura real da categoria na Meta Ads Library

Por decisao de 2026-08-26, Argos deve incluir no plano dirigido de todo ciclo B2C para Instagram uma
consulta de categoria com `country`, `publisherPlatform=INSTAGRAM`, termos especificos e limite de
coleta. O Product Discovery Worker envia essa solicitacao somente ao endpoint interno do proprio
dominio; o backend cria ou reutiliza a investigacao canonica no radar MOIS e devolve evidencias ja
persistidas, sem expor token, cookie ou controller de outro modulo ao executor.

Cada ciclo possui exatamente uma investigacao Meta e uma consulta ampla da categoria. O plano nao
pode fragmentar o mesmo ciclo em investigacoes concorrentes, pois isso perderia a correspondencia
entre consulta, payload bruto e dossie; refinamento posterior deve abrir uma nova tentativa do ciclo.

O dossie deve persistir separadamente:

- status da fonte, modo de coleta e identificador da investigacao;
- quantidade de anuncios aderentes, anuncios ativos e anunciantes distintos;
- plataforma declarada pela fonte, data da observacao mais recente e URL oficial de pesquisa;
- cada anuncio aderente com referencia, anunciante, atividade, longevidade, confianca e ressalva de
  que investimento observado nao comprova venda.

Somente `OBSERVED`, com anuncio atual, ativo e explicitamente distribuido no Instagram, pode atender
o gate de presenca real da categoria no canal. Resultado publico generico, anuncio observado apenas
no Facebook, fonte desatualizada, erro de permissao ou coleta ainda pendente nao substituem essa
prova. Falha da fonte deve aparecer como `UNAVAILABLE` ou estado de espera equivalente; nunca como
ausencia de mercado.

Anuncios Meta nao entram na contagem minima de dez ofertas pagas comparaveis e nao podem elevar
score como se fossem compras. Eles medem presenca, variedade, atualidade e investimento aparente no
canal. A comprovacao final continua dependendo de eventos atribuidos, checkout e pagamento
reconciliado do proprio Marketing Hub.

Por decisao de 2026-08-30, a cobertura comercial brasileira deve tentar primeiro uma observacao
publica deterministica no Chromium efemero do Product Discovery Worker. O backend prepara e vincula
a busca oficial; o navegador do executor apenas observa os fatos publicamente visiveis e os reporta
pelo endpoint interno do proprio dominio. A sessao humana continua obrigatoria como fallback quando
a fonte exigir interacao, bloquear a automacao ou deixar de expor filtros e fatos verificaveis.

O navegador publico deve:

- aceitar somente a URL oficial preparada pelo backend para a investigacao e o lease vigentes;
- fixar pais, status ativo e plataforma Instagram na propria URL e confirmar os tres filtros na
  interface antes de aceitar qualquer card;
- limitar consultas, tempo e quantidade de anuncios, sem rolagem ou repeticao irrestrita;
- registrar status HTTP, titulo, duracao, desfecho, ID do anuncio, anunciante, texto visivel,
  formato, destino, atividade, sinal comercial, instante, URL e payload bruto estruturado;
- usar contexto efemero, sem storage state, senha, token, login, cookie persistente ou download de
  lote de criativos;
- devolver `NO_MATCHING_ACTIVE_ADS` somente quando a propria interface confirmar um resultado
  vazio com os filtros esperados;
- devolver `AWAITING_SUPERVISED_OBSERVATION` diante de CAPTCHA, login obrigatorio, bloqueio,
  timeout, layout desconhecido, filtro ausente ou qualquer ambiguidade;
- manter anuncio ativo como sinal de presenca e investimento, nunca como compra, venda ou receita.

Quando o navegador devolver `AWAITING_SUPERVISED_OBSERVATION`, a propria execucao independente
deve oferecer uma sessao supervisionada da Biblioteca publica da Meta. A sessao deve:

- abrir a busca oficial com os termos, pais e plataforma definidos por Argos;
- aceitar somente URL oficial da Biblioteca e registrar ID do anuncio, anunciante, texto visivel,
  formato, plataforma, destino, atividade da pagina e sinal comercial observado;
- persistir o payload bruto, instante, investigacao e ciclo antes de qualquer normalizacao;
- rejeitar instante futuro e congelar novos registros enquanto a tentativa reaberta estiver na fila
  ou em execucao;
- expor para o operador os anuncios ativos e a linguagem comercial que Argos consumira;
- manter a tentativa concluida como historico e abrir uma nova tentativa auditavel somente por
  comando humano explicito de reanalise;
- reutilizar obrigatoriamente a mesma investigacao na reanalise, mesmo que o novo planejamento
  formule termos diferentes, impedindo perda ou mistura da evidencia supervisionada;
- bloquear a reanalise enquanto nao existir anuncio atual, ativo e explicitamente distribuido no
  Instagram.

A extracao publica automatizada e a sessao humana nao podem automatizar login, armazenar cookie ou
senha, contornar CAPTCHA, rate limit, bloqueio ou controle de acesso, publicar anuncio, alterar
campanha ou declarar venda. A automacao pode estruturar somente os cards publicos carregados pela
interface oficial no limite do ciclo; a observacao humana confirma apenas o que estava visivel na
mesma fonte. Atividade, longevidade e linguagem continuam sendo sinais de mercado, nao receita
comprovada.

## Caixa de sinais humanos observados

O inicio do processo `Descoberta e priorizacao da oportunidade PDE` deve aceitar uma entrada simples
chamada **sinal humano observado**. Ela permite registrar conversa, entrevista, pedido espontaneo,
comentario, reclamacao ou observacao de rotina sem exigir que o operador ja conheca o produto, a
oferta ou o mecanismo.

Cada sinal deve preservar, no minimo:

- tipo de fonte e contexto da conversa;
- data, canal e publico presumido;
- trecho literal relevante ou transcricao fiel;
- versao anonimizada usada pelos agentes;
- desejo ou resultado procurado;
- dor pratica e dor emocional observadas;
- tentativa anterior e por que a pessoa nao conseguiu concluir;
- esforco, risco ou conhecimento que a pessoa quer evitar;
- solucao atual ou comportamento substituto, quando existir;
- indicio de urgencia, pedido explicito ou intencao de pagar, sem inventar o que nao foi dito;
- consentimento, finalidade, restricao de uso e referencia segura ao material bruto;
- identificador de linhagem para acompanhar o sinal ate oportunidade, plano, PDE, comunicacao e
  experimento.

Nomes, telefone, empresa, foto, curriculo, identificadores e dados de terceiros devem ser removidos da
versao entregue aos agentes, salvo necessidade legitima, base adequada e acesso restrito. Uma captura
de tela privada nao deve virar prova publica nem copy atribuida a uma pessoa sem autorizacao.

## Leitura orientada a dor e desejo

O primeiro tratamento do sinal deve separar observacao de inferencia:

- **fala observada:** as palavras realmente usadas pela pessoa;
- **desejo funcional:** o resultado concreto que ela quer obter;
- **desejo emocional e social:** como quer se sentir ou ser percebida;
- **dor pratica:** tarefa, demora, custo, confusao ou retrabalho atual;
- **dor emocional:** frustracao, inseguranca, sobrecarga ou medo percebido;
- **tentativa frustrada:** o que tentou e em qual etapa se perdeu;
- **lacuna candidata:** o que precisaria ser mais simples, seguro ou familiar;
- **hipotese de PDE:** uma experiencia guiada que pode reduzir esforco e gerar microvalor;
- **lacunas de evidencia:** tudo que ainda precisa ser confirmado.

Argos estrutura o sinal, busca recorrencia e preserva as fontes. Psique interpreta desejo, alivio,
esforco percebido, familiaridade, risco emocional e valor para a pessoa sem substituir a fala real por
uma simulacao. Dedalo compara de duas a tres microexperiencias candidatas somente depois da
confirmacao inicial. Plutus avalia viabilidade economica. Temis usa a linguagem validada apenas quando
produto e Plano Comercial ja forem fontes de verdade para a comunicacao.

## Fluxo enxuto do sinal ate a venda

Para evitar complexidade prematura, o processo inicial deve seguir cinco movimentos:

1. **Capturar:** registrar e anonimizar o sinal sem exigir proposta de produto.
2. **Estruturar:** extrair dor, desejo, tentativa frustrada, resultado esperado e lacunas.
3. **Confirmar:** procurar recorrencia, desatendimento e intencao de compra em pelo menos dois
   caminhos independentes.
4. **Conceber:** comparar de duas a tres microexperiencias PDE e aprovar no maximo uma para o Plano
   Comercial.
5. **Preservar a voz:** levar linguagem, evidencias e limites aprovados ao produto, oferta, criativos,
   landing, venda e aprendizado, sempre vinculados ao sinal original.

O produto e a comunicacao nao podem avancar diretamente da conversa individual. A saida de
`Capturar` e `Estruturar` e uma hipotese pesquisavel; a saida de `Confirmar` e que pode se tornar um
dossie de oportunidade.

O processo não deve saltar de “uma profissional tentou criar um agente” para “criar um produto para a
profissão dessa pessoa”. Antes, deve procurar outras situações com o mesmo trabalho, a mesma causa de
fracasso ou o mesmo resultado desejado. A oportunidade aprovada pode escolher um segmento inicial,
mas deve preservar a diferença entre evidência observada e decisão de posicionamento.

## Busca dedicada

## Argos híbrido e pesquisa dirigida

Argos atua como investigador Codex, mas não recebe login, senha, cookie ou token de
marketplace. Antes da coleta, ele deve persistir um plano versionado com perguntas,
consultas públicas, marketplaces autorizados, pedidos de cobertura Meta, limites de produtos e
anuncios e condições de parada.

Hotmart e ClickBank permanecem como coletores autenticados isolados. O plano de Argos
funciona como solicitação dirigida; os coletores são responsáveis por autenticação e
captura estruturada. Como a cadeia PDE aceita kits, webapps, agentes, serviços assistidos
e outros formatos digitais, páginas comerciais públicas de concorrentes também podem
compor o conjunto comparável quando preservarem URL, domínio, descrição, preço disponível,
data, confiança e correlação com o ciclo. Conteúdo editorial, página de busca, menção
genérica, anúncio e posição em ranking não são alternativa paga comparável por si sós.
Toda conclusão comercial deve preservar fonte, data, snapshot bruto e correlação com o
ciclo. Uma observação isolada ou uma posição momentânea no ranking não comprova vendas.

O Product Discovery Worker deve executar cada solicitação dirigida pelo contrato interno
do próprio domínio no backend. O backend consulta os snapshots persistidos, filtra por
termos da investigação e devolve um contrato normalizado; o worker nunca acessa banco,
credencial ou controller de outro módulo. As ofertas usadas devem entrar no dossiê com
marketplace, referência, URL, coleta, preço e sinal de tração disponíveis.

Cada pendência deve ser entregue com lease único e prazo limitado. Plano, conclusão e falha
devem repetir o lease vigente; callback de uma tentativa substituída é rejeitado. Ciclo em
`RESEARCHING` cujo lease expirou deve voltar à fila automaticamente com nova tentativa
auditável, evitando bloqueio permanente após queda ou interrupção do worker.

Cada `ProductDiscoveryCycle` deve possuir a referência estável
`product-discovery-cycle:<cycleId>` na atividade inicial de Argos da versão de processo vigente. O
backend abre a tarefa como `PENDING`, registra `IN_PROGRESS` quando entrega o ciclo pelo endpoint
`pending`, persiste plano, resposta bruta, modelo, prompt e tokens efetivamente disponíveis e fecha
a mesma tarefa como `COMPLETED` ou `BLOCKED` junto com o callback funcional. A instância BPM e a
tarefa são obrigatórias para o histórico por atividade; a linha em `product_discovery_cycle`
continua sendo a fonte detalhada do domínio, mas não pode existir como execução invisível ao
processo. Retroativos devem preservar status e erro reais, identificar a origem do backfill e manter
como ausentes horários, prompt, tokens ou custos que não tenham sido registrados na execução.

Um ciclo dirigido não pode concluir com oportunidade `APPROVE` nem liberar o dossiê para a etapa
seguinte com menos de dez ofertas únicas comparáveis, vindas dos marketplaces autorizados ou de
páginas comerciais públicas aderentes ao problema quando o formato pesquisado não for coberto
pelos marketplaces. A pesquisa pode encerrar tecnicamente e preservar candidatas factuais como
`RESEARCH_MORE`, desde que a lacuna permaneça explícita e nenhuma próxima etapa seja liberada. A
contagem deve deduplicar por referência e domínio, exigir aderência semântica e preservar ao menos
dois caminhos independentes de confirmação. Dados ausentes não podem ser fabricados nem transformar
temperatura, score, ranking, anúncio ou página comercial em venda.

O plano deve exigir ao menos dez ofertas comparáveis e bloquear qualquer tentativa de
compra, afiliação, publicação, acesso a credenciais ou ampliação autônoma do limite.
Enquanto a sessão Codex individual estiver desabilitada, o worker pode usar o plano
determinístico seguro, sem alterar os gates comerciais.

### Descoberta orientada a público e canal

Por decisão de 2026-08-30, a mesma atividade de Argos aceita dois modos explícitos:

- `VALIDATE_MARKET`: aprofunda um mercado ou dor já informados;
- `DISCOVER_MARKETS`: parte de uma lente ampla de público, canal, contexto editorial e fontes de
  referência para encontrar situações de compra e mercados candidatos.

No modo de descoberta, Argos deve pesquisar em camadas independentes: linguagem espontânea da dor,
comportamento no canal, alternativas gratuitas e pagas, ofertas e preços, reclamações, mecanismo
científico candidato e inspiração das coleções vivas do repositório. As consultas devem ser curtas,
atômicas e variadas; copiar o briefing inteiro para todas as fontes não constitui plano dirigido.

Argos pode sugerir de duas a três **candidatas factuais de mercado** quando conseguir vincular cada
uma a evidências coletadas. Isso não é priorização estratégica: Argos registra pessoa, cena, dor,
linguagem, alternativas, esforço residual, sinais comerciais, aderência observável ao Instagram,
fontes e lacunas. Atena continua sendo a única autoridade para escolher mercado prioritário,
posicionamento, tese de oferta, formato do PDE e canal.

Candidatas com evidência insuficiente podem permanecer visíveis como `RESEARCH_MORE`, para orientar
a próxima coleta, sem se tornarem oportunidade aprovada. O mínimo de dez ofertas comparáveis, a
cobertura Meta/Instagram e os demais gates continuam bloqueando `APPROVE`, mas não devem apagar uma
candidata factual nem transformar falta de maturidade comercial em falha técnica.

O planejamento e a síntese devem usar prompts e schemas versionados. O ciclo deve preservar as duas
interações: request, resposta bruta, modelo, modo, tokens disponíveis, fontes acessadas e relatório
estruturado. Toda referência devolvida pelo modelo deve apontar para um identificador de evidência
realmente coletado; identificador inexistente bloqueia a execução em vez de ser corrigido ou
ignorado silenciosamente.

O worker de descoberta deve usar API de busca dedicada quando houver chave operacional
configurada. A ordem preferencial inicial e:

1. Brave Search API para busca web estruturada com sinal bruto de mercado.
2. Tavily quando a etapa exigir busca otimizada para agentes e conteudo sintetizavel.
3. SerpAPI quando a validacao depender especificamente da SERP do Google.
4. DuckDuckGo apenas como fallback sem chave, sem forca suficiente para aprovar sozinho
   oportunidades de alto impacto.

O payload bruto recebido da API de busca deve ser registrado com mascara de segredos e
correlacao por consulta/ciclo, preservando auditoria sem expor chaves.

## Sinais obrigatorios da dor

Cada dor candidata deve registrar:

- publico afetado;
- cena concreta da rotina em que a dor aparece;
- frequencia percebida;
- intensidade pratica;
- intensidade emocional;
- custo de nao resolver;
- esforco atual para resolver;
- linguagem usada pelo publico;
- canais onde a dor aparece;
- evidencias publicas usadas;
- sinais de que existe quantidade relevante de pessoas afetadas.

## Criterios de dor grande

Uma dor so deve virar oportunidade quando houver sinais de escala por pelo menos dois
caminhos independentes:

- volume ou tendencia de busca;
- recorrencia em comunidades, comentarios ou perguntas;
- presenca de multiplas solucoes concorrentes tentando capturar a demanda;
- investimento publicitario observavel no tema;
- dados setoriais ou populacionais que sustentem o tamanho;
- repeticao da dor em mais de um canal.

O modulo deve bloquear oportunidades baseadas em uma unica fonte, uma unica historia ou
uma inferencia sem evidencia.

## Criterios de desatendimento

Uma dor e considerada mal atendida quando as solucoes atuais mostram pelo menos um destes
sinais:

- exigem muito tempo, configuracao ou conhecimento tecnico;
- resolvem apenas parte do problema;
- sao caras demais para o publico;
- entregam informacao, mas nao uma experiencia aplicavel;
- dependem de consultoria manual;
- criam confusao, excesso de opcoes ou risco de erro;
- recebem objecoes recorrentes em reviews, comentarios ou reclamacoes;
- atendem empresas estruturadas, mas deixam consumidores, MEIs, autonomos ou pequenos
  operadores sem caminho simples.

## Criterios de encaixe PDE

Uma oportunidade tem encaixe PDE quando pode ser materializada como experiencia digital
com estas caracteristicas:

- entrada simples do usuario: frase, resposta curta, imagem, arquivo, URL ou escolha guiada;
- mecanismo observavel: o usuario entende como a experiencia transforma a entrada;
- microresultado rapido: o usuario sai com clareza, diagnostico, plano, recomendacao,
  simulacao, roteiro, visualizacao, organizacao ou decisao melhor;
- baixo esforco percebido;
- possibilidade de antes/depois concreto;
- continuidade natural para produto pago;
- entrega digital escalavel com custo marginal baixo;
- limite claro do que o produto faz e nao faz.

Produtos que exigem resultado medico, juridico, financeiro garantido, renda garantida,
tratamento sensivel ou promessa individual inevitavel devem ser bloqueados ou enviados
para revisao humana antes de qualquer experimento.

## Saida canonica da oportunidade

Toda oportunidade aprovada deve gerar um dossie curto e auditavel com:

- nome da oportunidade;
- publico primario;
- dor raiz;
- dor pratica;
- dor emocional;
- escala estimada;
- nivel de desatendimento;
- concorrentes e alternativas atuais;
- lacuna exploravel;
- formato PDE recomendado;
- primeira microexperiencia de valor;
- mecanismo plausivel;
- primeiro angulo de campanha;
- principal risco comercial;
- evidencias usadas;
- score final;
- decisao: aprovar, pesquisar mais, rejeitar ou pedir revisao humana.

Cada ciclo com evidencia real deve comparar de duas a tres oportunidades distintas por
dor raiz, mecanismo e formato de microexperiencia. Uma unica formulacao generica de
"alivio" nao representa comparacao suficiente para decisao comercial.

O ranking gerencial deve ser derivado das oportunidades e evidencias persistidas nos
ciclos mais recentes. Trilhas editoriais fixas podem orientar novas pesquisas quando nao
houver dados, mas nao podem se sobrepor ao ranking produzido por resultados reais.

## Separacao entre historico legado e maturidade PDE

Historico de campanhas, paginas, criativos ou produtos antigos nao deve ser tratado como
validacao positiva automatica para priorizar um novo PDE. Esse historico deve ser usado
como aprendizado de mercado, linguagem, objecoes, canais, dores percebidas e motivos de
fracasso ou sucesso, mas nao como prova de que a oportunidade PDE atual esta pronta para
venda.

Toda classificacao comercial deve separar explicitamente tres tipos de maturidade:

- maturidade de pesquisa: qualidade, quantidade e independencia das evidencias sobre a
  dor, escala, desatendimento e linguagem do publico;
- maturidade de ativo legado: existencia de campanha, landing page, produto, criativo,
  publico salvo, experimento ou material antigo relacionado ao nicho;
- maturidade PDE: existencia de mecanismo plausivel, microexperiencia personalizada,
  valor percebido rapido, prova de aplicabilidade, caminho de compra e criterios de
  entrega compativeis com Produto Digital de Experiencia.

A prioridade comercial deve ser sustentada pela maturidade PDE, nao pela maturidade de
ativo legado. Quando houver varias tentativas anteriores sem sucesso em um nicho, esse
historico deve aumentar o risco comercial do candidato ate que a nova proposta demonstre
uma diferenca real de mecanismo, experiencia, promessa, prova de valor e distribuicao.

Um nicho com muitos ativos antigos pode ser marcado como "candidato com aprendizado
acumulado", mas so deve ser tratado como benchmark ou prioridade de execucao quando a
validacao PDE atual superar os motivos historicos de baixa conversao.

## Gates de negocio

O modulo deve ter pelo menos sete gates:

1. Gate de escala: bloqueia dores pequenas demais ou sem evidencias independentes.
2. Gate de desatendimento: bloqueia dores ja bem atendidas por solucoes simples e baratas.
3. Gate de encaixe PDE: bloqueia oportunidades sem microexperiencia clara.
4. Gate de compra: bloqueia oportunidades em que o caminho do desconhecimento ate a compra
   exige salto mental grande demais.
5. Gate de mecanismo: bloqueia aprovacao sem base cientifica candidata verificavel e sem
   limites de promessa coerentes com essa evidencia.
6. Gate de momento de compra: no recorte B2C/Instagram, bloqueia priorizacao final sem fontes
   atuais, prototipo privado, criterios predeclarados, duas leituras consistentes, vantagem
   observada sobre o gratuito e ausencia de bloqueio de Psique ou Temis.
7. Gate de valor humano e entrega pronta: bloqueia candidata sem territorio sustentado por evidencia,
   resultado final utilizavel, entrada minima, baixo numero de passos e uso observado sem prompting
   ou montagem manual.

## Relacao com outros modulos

- OPRM pode fornecer publico, rotina e linguagem operacional.
- MOIS pode fornecer comparacao com produtos e paginas de venda existentes.
- Pipeline de hipotese deve receber apenas oportunidades aprovadas ou sinalizadas para
  revisao humana.
- GeraLanding, campanhas e criativos so devem entrar depois de existir dor, mecanismo,
  microexperiencia e decisao de oportunidade.

O modulo de descoberta nao deve publicar campanha, criar oferta final, criar landing page
ou iniciar gasto de midia automaticamente.

## Regra de qualidade

Uma oportunidade de produto PDE so e boa se puder ser resumida assim:

```text
Muitas pessoas vivem [dor concreta], as solucoes atuais deixam [lacuna clara], e podemos
entregar [resultado pronto e utilizavel] que gera [afeto, reconhecimento ou alivio de esforco]
com entrada minima e baixo esforco.
```

Se essa frase nao puder ser preenchida com evidencia, a oportunidade nao deve avancar.
