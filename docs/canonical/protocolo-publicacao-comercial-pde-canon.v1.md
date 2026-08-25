# Protocolo Canonico de Publicacao Comercial de PDE v1

## Objetivo

Todo Produto Digital Experiencial (PDE) publicado pelo Marketing Hub deve passar por validacao comercial antes de receber trafego, campanha ativa, experimento em `RUNNING` ou video de maior custo.

O objetivo e impedir que uma versao publica contamine a percepcao da cliente, misture metricas de experimentos diferentes ou gere custo de midia/video antes de provar que a experiencia esta clara, desejavel, mensuravel e pronta para venda.

Este protocolo vale para qualquer PDE, nao apenas para o Metodo MUSA.

## Principio comercial

A cliente deve ver produto, promessa, microvalor e proximo passo. Ela nunca deve ver linguagem de operacao interna.

Linguagem interna pode existir em contrato, banco, painel, logs, pipeline e versao tecnica. Na experiencia publica, ela deve ser traduzida para linguagem de desejo, simplicidade e beneficio pratico.

Exemplos de traducao obrigatoria:

| Linguagem interna | Linguagem publica preferencial |
|---|---|
| `PDE v7`, `versao cientifica`, nome de container ou imagem | nome comercial do metodo ou jornada |
| `experimento`, `campanha`, `teste` | experiencia, diagnostico, plano, jornada |
| `pipeline`, `contrato`, `schema`, `evento` | etapa, resposta, progresso, orientacao |
| `score`, `metrica`, `validacao` | sinal, clareza, ajuste, resultado percebido |

## Checklist obrigatoria antes de publicar

Antes de colocar qualquer PDE como pronto para trafego, o Marketing Hub deve validar:

1. Primeira dobra mobile.
2. Primeira dobra desktop.
3. Promessa principal.
4. CTA primario.
5. Diagnostico ou microexperiencia gratuita.
6. Paywall ou convite de continuidade, quando existir.
7. Checkout ou rota de compra.
8. Mensagens de erro e estados vazios.
9. Videos, imagens e ativos publicos.
10. Eventos e metricas do funil.
11. Separacao correta por produto, URL publica, versao PDE, campanha, experimento e `experienceVersion`.
12. Imagem e container Docker próprios para a versao publica que recebera trafego.
13. Ausencia de termos tecnicos internos na experiencia publica.

Se qualquer item falhar, o PDE deve permanecer em rascunho, correcao ou validacao tecnica. Nao deve entrar em `RUNNING` nem receber aumento de investimento.

## Regra anti-vazamento de linguagem tecnica

Textos publicos de PDE nao podem expor termos como:

- `PDE`;
- `v1`, `v2`, `v3`, `v4`, `v5`, `v6`, `v7` ou equivalente quando usado como versao operacional;
- `slot`;
- `experimento`;
- `campanha`;
- `pipeline`;
- `worker`;
- `contrato`;
- `schema`;
- `JSON`;
- `evento`;
- `tracking`;
- `CTR`;
- `CPL`;
- `checkout` quando a palavra aparecer como termo tecnico, e nao como acao clara de compra;
- `score`;
- `validacao tecnica`;
- `versao cientifica`;
- nomes internos de etapa, modulo, provider, host, porta, branch, build, container ou deploy.

Excecao: um termo tecnico pode aparecer somente quando for indispensavel para transparencia da cliente e estiver escrito em linguagem comum. Mesmo nesses casos, a preferencia e explicar o beneficio pratico em vez do mecanismo interno.

## Regra de ciencia em PDE

Quando o PDE usar artigos, evidencias externas ou base cientifica, a ciencia deve aparecer como plausibilidade pratica, nao como aula.

Forma correta:

- "estudos mostram que este tema influencia percepcao, decisao ou comportamento";
- "o metodo transforma esse principio em microacoes simples";
- "voce recebe uma orientacao aplicavel para hoje";
- "o objetivo e reduzir esforco e aumentar clareza no proximo passo".

Forma proibida:

- prometer resultado absoluto;
- afirmar que a ciencia garante transformacao individual;
- transformar a primeira dobra em explicacao academica;
- citar artigos de forma fria quando isso reduz desejo;
- usar a existencia de IA, algoritmo ou pesquisa como substituto de beneficio percebido.

## Regra de metricas limpas

O cockpit nao deve misturar metricas de PDE de produtos, campanhas, experimentos, URLs publicas, versoes PDE, imagens/containers ou `experienceVersion` diferentes em uma mesma leitura comercial.

Para decisao de campanha ou experimento, o resumo deve separar obrigatoriamente:

- produto;
- experimento;
- campanha;
- URL publica;
- versao PDE publica;
- imagem/container Docker publicado;
- `experienceVersion`;
- origem/UTM;
- qualidade de trafego (`HUMAN`, `BOT_SUSPECTED`, `PLATFORM_CRAWLER`, `INTERNAL_QA` ou `UNKNOWN`);
- janela de publicacao;
- modo de execucao quando existir (`TEST` ou `PRODUCTION`).

Quando nao houver atribuicao suficiente para separar a origem, o dado deve aparecer como historico, diagnostico ou leitura incompleta. Ele nao pode alimentar decisao de vencedor, escala, pausa comercial, comparacao entre versoes ou conclusao sobre performance do experimento.

O painel deve diferenciar claramente:

- validacao interna ou QA;
- validacao pre-campanha;
- trafego organico;
- trafego pago atribuido a campanha atual;
- historico de versao anterior;
- resultado comercial limpo.

## Gate antes de `RUNNING`

Um PDE so pode liberar experimento/campanha para `RUNNING` quando todos os pontos abaixo estiverem verdadeiros:

- a URL publica final responde no dominio que recebera trafego;
- a versao publicada entrega a `experienceVersion` esperada;
- a versao possui imagem e container Docker próprios, sem reaproveitar imagem genérica de outra versão pública;
- `GET /version-diagnostics.json` confirma `version`, `imageVersionId`, `image`, `commitSha` e `experienceVersion`;
- a primeira dobra esta comercialmente limpa em mobile e desktop;
- a microexperiencia inicial funciona sem erro visivel;
- o CTA principal esta claro e coerente com a promessa;
- o funil de login, paywall, checkout e acesso esta configurado conforme a estrategia do produto;
- eventos minimos do funil aparecem persistidos e segmentados pela versao correta;
- metricas exibidas no cockpit nao misturam campanha atual com historico de outra campanha;
- imagens e videos publicos estao aprovados e vinculados a ativos rastreaveis;
- nao ha termos internos visiveis para a cliente.

Se o PDE depender de video caro, avatar, HLS ou render externo, a geracao de custo alto deve ocorrer somente depois da validacao da experiencia sem o ativo caro, salvo decisao comercial explicita registrada.

Para PDE em canal `DIRECT_ONE_TO_ONE`, o material comercial não precisa fingir ser um criativo de
mídia paga. O run produtivo mais recente em `READY_TO_PUBLISH`, com todos os gates da experiência,
checkout, entrega, distribuição individual e dados aprovados, é a autoridade para a ativação. O
comando administrativo deve alterar atomicamente o experimento para `RUNNING`, o run para `RUNNING`,
abrir sua janela comercial e mover o produto para Venda, Entrega e Aprendizado. Se qualquer uma
dessas alterações não puder ser persistida, nenhuma delas deve permanecer aplicada.

## Registro de decisao

Cada publicacao comercial de PDE deve deixar evidencia operacional no Marketing Hub ou na documentacao do experimento:

- data/hora da publicacao;
- produto;
- URL;
- versao PDE publica;
- imagem/container Docker publicado;
- `experienceVersion`;
- experimento/campanha vinculados;
- checklist executada;
- bloqueios encontrados;
- decisao tomada: publicar, manter rascunho, corrigir ou liberar para trafego;
- responsavel ou origem da decisao.

Logs tecnicos podem apoiar diagnostico, mas nao substituem evidencia persistida ou registrada de decisao comercial.

## Criterio de melhoria continua

Sempre que um PDE publico expuser linguagem interna, medir versao errada, misturar metricas ou entrar em campanha sem validacao visual/eventos, a correcao deve atacar a causa-raiz:

- ajustar contrato, cockpit, backend, worker ou frontend que permitiu o erro;
- adicionar teste, checklist ou bloqueio de publicacao quando aplicavel;
- registrar o aprendizado no documento do tema;
- evitar depender apenas de revisao manual futura.
