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

Conteudo de solucao deve ser usado para medir saturacao e lacuna, nao como prova direta
da dor. A prova da dor deve vir preferencialmente da linguagem do publico.

## Busca dedicada

## Argos híbrido e pesquisa dirigida

Argos atua como investigador Codex, mas não recebe login, senha, cookie ou token de
marketplace. Antes da coleta, ele deve persistir um plano versionado com perguntas,
consultas públicas, marketplaces autorizados, limite de produtos e condições de parada.

Hotmart e ClickBank permanecem como coletores autenticados isolados. O plano de Argos
funciona como solicitação dirigida; os coletores são responsáveis por autenticação e
captura estruturada. Toda conclusão comercial deve preservar fonte, data, snapshot bruto
e correlação com o ciclo. Uma observação isolada ou uma posição momentânea no ranking não
comprova vendas.

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
