Estamos Trabalhando nesse contexto:

```xml
<nicho>
	<hipotese>
		<pain/>
		<result/>
		<mecanismo/>
		<proof/>
		<oferta/>
		<experimento>
			<campaignAngle/>
			<adCopy/>
			<adImageBriefing/>
			<landingPageWireframe/>
			<landingCopy/>
			<landingPromptImagem/>
				<listaImagem/>
			<landingPromptImagem/>
			<landingPresetDesign/>
			<landingHtml/>
		</experimento>
	</hipotese>
</nicho>

Nicho: {{NICHE_NAME}}

Dor: {{PAIN_JSON}}

Resultado: {{RESULT_JSON}}


{{prompt-regras-globais}}

Ângulo da Campanha que vai ser publicada:
{{dados-campaignAngle}}

Copy do Anuncio:
{{dados-adCopy}}

Briefing das Imagens dos Anuncios:
{{dados-adImageBriefing}}


template_id: landing-wireframe
template_version: v3
artifact_target: landingPageWireframe

Regras fixas da etapa (Gera Landing, contrato v3):
- Entregar somente JSON válido com raiz obrigatória: `definicoes` e `pagina`.
- `definicoes` deve conter exatamente: `estrutura`, `posicao`, `layout`, `mistas`.
- Cada categoria de `definicoes` deve conter `desktop[]` e `mobile[]`.
- Cada item de definição deve conter somente: `nome`, `atributoCss`, `valor`.
- Em `pagina`/`secoes` e elementos internos, toda classe aplicada deve aparecer SOMENTE no campo `estilos[]`.
- É proibido criar campos de classe por categoria (`estrutura`, `posicao`, `layout`, `mistas`) dentro de `pagina`, `pagina.corpo`, `pagina.corpo.secoes[]`, `elementosSeccao[]` ou `elementosInternos[]`; essas categorias existem apenas em `definicoes`.
- Em `estilos[]` (`pagina.corpo`, seção e elementos internos), use exclusivamente nomes existentes em `definicoes.*.desktop[].nome` ou `definicoes.*.mobile[].nome`; qualquer nome fora disso viola contrato.
- É proibido repetir `atributoCss`/`valor` fora de `definicoes`.
- Não invente campos fora do schema.
- Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
- Evite JSON dentro de strings; mantenha cada informação no campo próprio.


- `pagina.corpo.estilos` obrigatório: declarar classes base aplicadas ao elemento HTML `<body>` usando apenas `bgBody`, `fontBase`, `textPrimary`, `marginReset`; não criar o campo `pagina.body`.
- Em TODO elemento interativo (`a`, `button`), declarar `intencaoAcao` e, quando for navegação interna, `targetSectionId` apontando para `id` real de seção (ex.: `#sec-prova`); quando for link externo, declarar `hrefEsperado`.
- Em toda `img`, declarar contrato de asset em `asset`: `src`, `alt`, `width`, `height` (wireframe deve especificar esses campos mesmo com `src` provisório).
- Em todo campo de formulário (`input`), declarar `contratoCampo`: `type`, `name`, `autocomplete`, `required`, `placeholder`.
- Em elementos de ação/entrada, usar componentes semânticos via `componente`: `buttonPrimary`, `buttonSecondary`, `formInput`, `card` (quando aplicável), evitando depender de combinação manual de tokens.
- `texto.conteudo: ""` significa literalmente “não renderizar texto”; é proibido substituir por placeholder nesta etapa.

Matriz oficial de grupos e atributos CSS (explícita):
- Grupo `posicionamento` (categoria `definicoes.posicao`): `position`, `top`, `right`, `bottom`, `left`, `z-index`.
- Grupo `exibicaoFluxo` (categoria `definicoes.layout`): `display`, `float`, `clear`, `visibility`, `overflow`, `overflow-x`, `overflow-y`.
- Grupo `tamanho` (categoria `definicoes.estrutura`): `width`, `height`, `min-width`, `min-height`, `max-width`, `max-height`, `box-sizing`.
- Grupo `espacamentoExterno` (categoria `definicoes.estrutura`): `margin`, `margin-top`, `margin-right`, `margin-bottom`, `margin-left`.
- Grupo `espacamentoInterno` (categoria `definicoes.estrutura`): `padding`, `padding-top`, `padding-right`, `padding-bottom`, `padding-left`.
- Grupo `flexbox` (categoria `definicoes.layout`): `flex`, `flex-direction`, `flex-wrap`, `flex-flow`, `justify-content`, `align-items`, `align-content`, `align-self`, `gap`, `row-gap`, `column-gap`, `order`, `flex-grow`, `flex-shrink`, `flex-basis`.
- Grupo `grid` (categoria `definicoes.layout`): `grid`, `grid-template`, `grid-template-columns`, `grid-template-rows`, `grid-template-areas`, `grid-column`, `grid-column-start`, `grid-column-end`, `grid-row`, `grid-row-start`, `grid-row-end`, `grid-area`, `justify-items`, `align-items`, `place-items`, `justify-content`, `align-content`, `place-content`, `gap`, `row-gap`, `column-gap`.
- Grupo `transformacoes` (categoria `definicoes.mistas`): `transform`, `translate`, `scale`, `rotate`, `transform-origin`.

Regra de conformidade dos grupos:
- `definicoes.posicao` só pode usar atributos do grupo `posicionamento`.
- `definicoes.layout` só pode usar atributos dos grupos `exibicaoFluxo`, `flexbox` e `grid`.
- `definicoes.estrutura` só pode usar atributos dos grupos `tamanho`, `espacamentoExterno` e `espacamentoInterno`.
- `definicoes.mistas` só pode usar atributos do grupo `transformacoes`.


Trecho obrigatório do contrato de seção/elementos (manter):
- Cada seção deve conter: `nome`, `objetivo`, `oQueQuerProvocarNoUsuario`, `papelComercial`, `fasePersuasao`, `objeçãoQueRemove`, `prioridadeConversao`, `acaoEsperada`, `fonteContexto[]`, `id`, `estilos[]`, `elementosSeccao[]`; não deve conter `estrutura`, `posicao`, `layout` ou `mistas`.
- Cada item de `elementosSeccao[]` deve conter: `id`, `tag`, `texto`, `estilos[]`, `elementosInternos[]`.
- Quando `tag = "img"`, o elemento deve conter também `briefingVisual` com:
  - `ondeEntraNoVisual`;
  - `tipoVisualEsperado`;
  - `funcaoComercial`;
  - `objecaoQueRemove`;
  - `classificacaoVisual` em: `mockup`, `foto`, `ilustração`, `diagrama`, `print conceitual`;
  - `posicaoDesejada`: posição narrativa/layout desejada na seção (ex.: hero lado direito dentro do card, abaixo do bloco de prova, antes do CTA secundário);
  - `aspectRatio`: proporção aproximada esperada (ex.: `1:1`, `4:3`, `16:9`, `3:4`);
  - `maxVisualHeight`: limite de altura recomendado para desktop e mobile, em texto curto com unidade CSS (ex.: `desktop: 420px; mobile: 280px`);
  - `layoutRole`: papel visual no layout, em: `product`, `proof`, `demonstration`, `before-after`, `mechanism`, `objection-reducer`;
  - `relacaoComCta`: explicar se o visual prepara, reforça ou não compete com o CTA da seção.
- `briefingVisual` é exclusivo de `img`: para outras tags, manter `briefingVisual: null` (não preencher objeto).
- `elementosInternos[]` representa hierarquia de filhos e deve suportar recursão (filho pode conter netos e assim por diante), sempre com o mesmo contrato do elemento pai.
- Campo `texto` de cada elemento deve conter exatamente: `tamMaximo`, `tamMinimo`, `conteudo`.
- `tamMinimo` e `tamMaximo` não são números aleatórios: eles são o contrato de espaço textual para a próxima etapa de copy e devem ser definidos pela função do texto no layout, pela intenção comercial do bloco e pelo esforço cognitivo aceitável no mobile.
- Defina `tamMinimo`/`tamMaximo` por tipo de texto e contexto de tela:
  - Títulos principais (`h1`) e chamadas de primeira dobra: curtos, fortes e escaneáveis; use faixa pequena/média para caber bem no hero sem empurrar CTA e imagem para baixo.
  - Subtítulos (`h2`, `h3`) e títulos de cards: ainda menores que blocos explicativos; devem nomear a ideia, a dor, o resultado ou a objeção com clareza, sem virar parágrafo.
  - Botões e links de ação (`button`, `a`): faixas muito curtas; texto direto de ação, sem explicação embutida.
  - Bullets, `li`, badges e microcopy auxiliar: faixas curtas; cada item deve comunicar uma ideia só, facilitando leitura rápida.
  - Parágrafos explicativos (`p`) e descrições de mecanismo/prova/oferta: faixas maiores que títulos, mas controladas; permitir contexto suficiente para explicar por que aquilo importa, sem cansar o usuário.
  - FAQ, prova, como funciona e objeções: podem ter faixa média quando precisam remover dúvida real, mas divida em blocos pequenos em vez de liberar textos longos em um único elemento.
  - Labels/placeholders de formulário: faixas curtas e funcionais; não usar como área de persuasão.
- Em geral, `tamMinimo` deve representar o menor texto ainda útil para cumprir a função comercial do elemento; `tamMaximo` deve representar o maior texto que cabe naquele espaço sem quebrar escaneabilidade, hierarquia visual ou avanço para o CTA.
- Quanto mais alto o elemento estiver na página e quanto mais próximo estiver do CTA principal, menor deve ser o limite de copy; explicações mais longas devem ficar em seções posteriores, onde o usuário já aceitou entender mecanismo, prova ou oferta.
- Preserve a hierarquia: título orienta, subtítulo enquadra, parágrafo explica, bullet facilita decisão, CTA move para ação. Não dê a um título limite de parágrafo nem a um parágrafo limite tão curto que impeça explicar o mecanismo.
- `elementosInternos` pode ser lista vazia, mas sempre deve existir.


- Para cada visual (`img`) planejado no wireframe, explicitar no `objetivo`/metadados da seção:
  - onde o visual entra na narrativa da página (posição e contexto comercial);
  - qual tipo de visual é esperado;
  - qual função comercial o visual cumpre;
  - qual objeção o visual ajuda a remover;
  - classificar o visual como: `mockup`, `foto`, `ilustração`, `diagrama` ou `print conceitual`;
  - posição desejada no layout, proporção aproximada, limite de altura no desktop/mobile e relação com o CTA.

Regras comerciais e estruturais obrigatórias (mantidas):
- Mobile-first obrigatório: priorize leitura vertical e CTA claro nas primeiras seções.
- Primeira dobra forte obrigatória: o hero deve abrir com resultado comercial desejado pelo nicho + dor removida + mecanismo plausível, e não com uma descrição operacional da amostra. A amostra/PDF/mini-kit deve aparecer como prova concreta do mecanismo, não como promessa principal.
- Desktop comercial obrigatório: no desktop, o hero deve ser pensado em duas colunas dentro de um container centralizado; coluna esquerda com headline, subtítulo, 3 bullets de valor e CTA primário; coluna direita com card visual/prova do produto ou card de captura curta. É proibido gerar uma página estreita de coluna única no desktop quando houver espaço para hierarquia visual.
- Formulário com contexto claro: a seção de captura deve conter rótulos ou microcopy textual visível para `nome` e `email`, além de CTA específico; inputs não podem depender apenas de campos vazios/sem placeholder para o usuário entender o que preencher.
- Sequência persuasiva mínima obrigatória: depois do hero, estruturar seções que respondam nesta ordem lógica: (1) por que a situação atual custa venda/renovação; (2) como o mecanismo resolve com pouco esforço; (3) o que a pessoa recebe na amostra e no próximo passo; (4) formulário/ação; (5) dúvidas/objeções essenciais.
- Menos CTAs redundantes: usar CTAs nos pontos de decisão, evitando repetir o mesmo botão após blocos que ainda não agregaram argumento novo.
- Princípio de pouco esforço obrigatório: o usuário não quer fazer esforço para entender a comunicação da página; portanto, cada seção deve reduzir carga cognitiva, deixar a mensagem principal evidente em leitura rápida, usar poucos caminhos de decisão, evitar excesso de informações simultâneas e conduzir naturalmente para o próximo CTA.
- Objetivo comercial obrigatório: estruturar a página para venda com foco na coleta de informação para envio de amostra/prova do produto (ex.: formulário/CTA de captura).
- Fase wireframe NÃO preenche copy: em TODOS os elementos, `texto.conteudo` deve ser string vazia (`""`) nesta etapa.
- Para tags de lista (`ul`), sempre declarar os `li` internos explicitamente.
- Formulário obrigatório da landing: incluir seção/formulário de captura contendo somente os campos `nome` e `email` (não incluir telefone, WhatsApp, CPF, empresa ou outros campos).
- Hero obrigatório com âncora primária: na primeira dobra (hero), incluir CTA com link âncora direto para a seção do formulário.
- Âncoras obrigatórias adicionais: incluir mais duas âncoras internas para pontos estratégicos distintos da página (ex.: mecanismo, prova social, oferta), além da âncora do hero para o formulário.
- Quantidade mínima de seções obrigatória: gerar no mínimo 4 seções em `pagina.corpo.secoes`.
- Quantidade de imagens orientada por função comercial: planejar normalmente entre 2 e 4 elementos `img` úteis no total da página; exceder esse intervalo somente quando o nicho exigir mais prova visual concreta para vender com clareza.
- Regra comercial para uso de imagens: inserir `img` somente quando a imagem cumpre função explícita de prova, demonstração do produto, antes/depois, explicação do mecanismo ou redução de objeção; é proibido adicionar imagem apenas para preencher seção ou decorar a página.
- Imagem de produto obrigatória: garantir que pelo menos uma `img` represente visualmente a ideia do produto/entrega que o cliente está comprando (ex.: mockup da solução, amostra do conteúdo, kit/resultado final esperado).
- Hero com imagem controlada: quando a imagem de produto ou outro visual entrar no hero, ela deve ficar dentro de container controlado, com proporção e altura máximas declaradas em `briefingVisual`, sem bloco full-width desproporcional e sem competir com o CTA principal.
- Balanceamento visual obrigatório: intercalar blocos de texto e imagem quando isso reduzir paredes de texto e melhorar escaneabilidade, sem forçar imagem em toda seção.

Heurísticas de composição (inspiração, não regra rígida):
- Hero bullets: preferir ~3 itens.
- Lista de entregáveis: preferir entre 3 e 5 itens.
- Antes/depois: preferir 3 itens de "antes" e 3 itens de "depois".
- Como funciona: preferir 3 passos.
- FAQ: preferir entre 4 e 6 perguntas.

Heurística para listas longas no mobile (inspiração contextual):
- Evitar listas grandes no início da página.
- Evitar mais de 5 itens visíveis por bloco quando a pessoa ainda não entendeu a oferta.
- Evitar misturar no mesmo bloco: benefícios, recursos e explicações extensas.
- Evitar estruturas que aumentem sensação de esforço, prejudiquem foco no CTA ou alonguem demais o mobile sem avanço de persuasão.

Ajuste de intenção por seção (referência do esboço):
- `papelComercial`: descreva a função comercial da seção no funil da landing (ex.: primeira dobra de conversão, remoção de risco, fechamento).
- `fasePersuasao`: explicite a fase predominante (ex.: dor-promessa-prova-acao).
- `objeçãoQueRemove`: declare a principal objeção que a seção resolve.
- `prioridadeConversao`: inteiro de 1 a 10 (10 = mais crítico para conversão).
- `acaoEsperada`: qual ação concreta o usuário deve tomar após consumir a seção.
- `fonteContexto[]`: liste de onde a seção foi derivada (ex.: `PAIN_JSON.surface`, `campaignAngle.primaryPromise`).
- `objetivo`: declarar a função comercial central da seção.
- `nome` e `id`: manter coerência com a etapa do funil e com a navegação por âncoras.

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao schema `landing-page-wireframe-schema.json` da etapa Gera Landing.
