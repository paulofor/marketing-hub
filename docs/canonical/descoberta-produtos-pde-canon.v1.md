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

O gate deve rejeitar B2B disfarçado de B2C, curso generico sem microexperiencia, produto que dependa
de operacao empresarial para gerar valor e ideia cuja aquisicao no Instagram seja apenas uma
suposicao. Temperatura Hotmart, anuncio ativo, audiencia e pontuacao continuam sendo sinais, nao
vendas. Para declarar uma nova oportunidade superior ao benchmark Rigel, a pontuacao auditavel deve
ser **estritamente maior**, com consenso dos agentes e valor percebido minimo preservado.

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
consultas públicas, marketplaces autorizados, limite de produtos e condições de parada.

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

Um ciclo dirigido não pode concluir nem marcar a tarefa do dossiê como concluída com menos
de dez ofertas únicas comparáveis, vindas dos marketplaces autorizados ou de páginas
comerciais públicas aderentes ao problema quando o formato pesquisado não for coberto pelos
marketplaces. A contagem deve deduplicar por referência e domínio, exigir aderência
semântica e preservar ao menos dois caminhos independentes de confirmação. Dados ausentes
devem bloquear o ciclo com a lacuna explícita, sem fabricar evidência e sem transformar
temperatura, score, ranking, anúncio ou página comercial em venda.

O plano deve exigir ao menos dez ofertas comparáveis e bloquear qualquer tentativa de
compra, afiliação, publicação, acesso a credenciais ou ampliação autônoma do limite.
Enquanto a sessão Codex individual estiver desabilitada, o worker pode usar o plano
determinístico seguro, sem alterar os gates comerciais.

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

O modulo deve ter pelo menos cinco gates:

1. Gate de escala: bloqueia dores pequenas demais ou sem evidencias independentes.
2. Gate de desatendimento: bloqueia dores ja bem atendidas por solucoes simples e baratas.
3. Gate de encaixe PDE: bloqueia oportunidades sem microexperiencia clara.
4. Gate de compra: bloqueia oportunidades em que o caminho do desconhecimento ate a compra
   exige salto mental grande demais.
5. Gate de mecanismo: bloqueia aprovacao sem base cientifica candidata verificavel e sem
   limites de promessa coerentes com essa evidencia.

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
entregar [microexperiencia digital] que gera [valor percebido rapido] com baixo esforco.
```

Se essa frase nao puder ser preenchida com evidencia, a oportunidade nao deve avancar.
