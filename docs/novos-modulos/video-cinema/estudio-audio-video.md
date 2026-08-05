# Estudio de Audio e Video

- **Data:** 2026-07-28
- **Status:** inicio de construcao
- **Modulo:** frontend administrativo do Marketing Hub

## Decisao

Criar um item de menu novo chamado **Estudio de Audio e Video**.

Esse item sera desenvolvido aos poucos para videos realmente sofisticados, com varias cenas, script longo, trilha sonora, narracao, continuidade visual e pos-producao.

## O que permanece onde esta

Os muitos pontos atuais de tratamento de videos serao mantidos:

- criativos de experimentos;
- videos para PDEs;
- videos organicos curtos;
- aprovacao de videos;
- tratamentos operacionais de renderizacao, provedores e pos-producao ja existentes.

Esses fluxos continuam com foco em velocidade, validacao, criativos e operacao de funil.

## Papel do novo estudio

O Estudio de Audio e Video deve ser a area para producoes de maior valor percebido:

- video manifesto de produto;
- video narrativo de oferta;
- aula ou apresentacao comercial com cenas;
- storytelling cinematografico;
- demonstracao guiada de PDE;
- conteudo premium para aumentar confianca, desejo e conversao.

## Regra de pre-producao premium

Antes de construir ou renderizar video no Estudio, o projeto deve ter uma
**biblia visual** definida e persistida. Isso evita gerar cada cena apenas por
texto, que tende a mudar rosto, figurino, objetos e ambientes entre takes.

Blocos obrigatorios da biblia visual:

- personagens: imagens mestre aprovadas, angulos, figurino, acessorios e URLs
  ou IDs de referencia;
- ambientes: imagem mestra, angulo oposto, lateral, entradas, saidas, objetos
  fixos e mapa simples do cenario;
- objetos/produto/marca: interface do PDE, telas, simbolos, logotipo, textos e
  referencias separadas para composicao;
- direcao visual: estilo, luz, lente, textura, paleta, enquadramento e nivel de
  realismo;
- plano de geracao de imagem: solicitar primeiro ao modelo de imagem OpenAI as
  imagens mestre e frames-chave, aprovar esses ativos e so depois gerar video;
- regras de continuidade: preservar rosto, cabelo, figurino, escala, objetos,
  arquitetura, temperatura de cor e identidade visual entre cenas.

O backend deve bloquear avanço para renderizacao, producao, revisao ou aprovacao
quando qualquer bloco da biblia visual estiver ausente. O objetivo nao e burocracia:
e proteger consistencia premium e reduzir desperdicio com cenas bonitas, mas
incoerentes entre si.

## Alternativas avaliadas

### Alternativa 1: somente documentar

Beneficio: baixo custo e sem risco tecnico.

Risco: a decisao nao aparece no produto e tende a ficar invisivel para a operacao.

### Alternativa 2: usar o menu atual de Videos

Beneficio: reaproveita a area existente.

Risco: mistura videos rapidos, criativos e producoes sofisticadas, criando confusao operacional.

### Alternativa 3: criar area separada e incremental

Beneficio: preserva os fluxos atuais e abre uma trilha propria para videos complexos.

Risco: exige evolucao gradual de contratos, persistencia e automacoes.

Decisao: seguir com a alternativa 3.

## Primeira entrega

A primeira entrega deve criar:

- item de menu principal;
- rota dedicada;
- tela inicial de cockpit;
- separacao conceitual entre videos atuais e producoes sofisticadas;
- proximas etapas visiveis para orientar a construcao.

## Proximas etapas recomendadas

1. Criar cadastro persistido de projeto audiovisual.
2. Criar etapas de briefing, roteiro, cenas, audio, trilha e montagem.
3. Conectar cada etapa a jobs auditaveis no backend.
4. Expor status, custos, artefatos e revisoes na tela.
5. Permitir reaproveitar ativos vindos de produtos, experimentos, PDEs e biblioteca de midia.

## Aprendizado da referencia Tik Tok Flavio — 2026-08-05

O MP4 de referencia possui 131,4 segundos e 39 mudancas visuais fortes detectadas,
equivalentes a aproximadamente um novo plano a cada 3,4 segundos. A continuidade
percebida nao vem de um clipe longo: vem da repeticao coerente de personagem,
objeto central, paleta, direcao de movimento e progressao narrativa entre muitos
planos curtos.

### Causa-raiz da diferenca de qualidade

O Estudio limitava todo projeto a quatro cenas, mesmo quando o roteiro persistido
possuia seis ou mais planos. Cada clipe era gerado isoladamente e a montagem apenas
os concatenava. Isso produzia poucos enquadramentos longos e transferia para um
prompt textual uma continuidade que precisa ser resolvida no storyboard e nas
referencias visuais.

### Regra operacional adotada

- permitir de 2 a 12 planos consecutivos por montagem cinematografica;
- manter DOR no primeiro plano e CTA no ultimo, com RESULTADO, MECANISMO e PROVA
  distribuídos no desenvolvimento;
- gerar e aprovar cada plano separadamente, preservando a possibilidade de refazer
  somente o trecho inconsistente;
- usar 6 a 10 planos para criativos de 25 a 40 segundos, buscando ritmo medio de
  2,5 a 4 segundos por plano;
- a proxima evolucao deve extrair o quadro final aprovado de um plano para usa-lo
  como ponte visual do plano seguinte e adicionar narracao, trilha e legendas como
  camadas continuas na pos-producao.
