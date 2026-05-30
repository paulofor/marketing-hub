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
- Em `pagina`/`secoes`, usar somente referências por `nome` já definido em `definicoes`.
- Em `pagina`/`secoes`, usar referências simples por nome de classe (sem separar por `desktop` e `mobile`).
- Em `estilos[]` (seção e elementos internos), use exclusivamente nomes existentes em `definicoes.*.desktop[].nome` ou `definicoes.*.mobile[].nome`; qualquer nome fora disso viola contrato.
- É proibido repetir `atributoCss`/`valor` fora de `definicoes`.
- Não invente campos fora do schema.
- Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
- Evite JSON dentro de strings; mantenha cada informação no campo próprio.


- `pagina.body` obrigatório: declarar classes base aplicadas ao `<body>` usando apenas `bgBody`, `fontBase`, `textPrimary`, `marginReset`.
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
- Cada seção deve conter: `nome`, `objetivo`, `oQueQuerProvocarNoUsuario`, `papelComercial`, `fasePersuasao`, `objeçãoQueRemove`, `prioridadeConversao`, `acaoEsperada`, `fonteContexto[]`, `id`, `estilos[]`, `elementosSeccao[]`.
- Cada item de `elementosSeccao[]` deve conter: `id`, `tag`, `texto`, `estilos[]`, `elementosInternos[]`.
- Quando `tag = "img"`, o elemento deve conter também `briefingVisual` com:
  - `ondeEntraNoVisual`;
  - `tipoVisualEsperado`;
  - `funcaoComercial`;
  - `objecaoQueRemove`;
  - `classificacaoVisual` em: `mockup`, `foto`, `ilustração`, `diagrama`, `print conceitual`.
- `briefingVisual` é exclusivo de `img`: para outras tags, manter `briefingVisual: null` (não preencher objeto).
- `elementosInternos[]` representa hierarquia de filhos e deve suportar recursão (filho pode conter netos e assim por diante), sempre com o mesmo contrato do elemento pai.
- Campo `texto` de cada elemento deve conter exatamente: `tamMaximo`, `tamMinimo`, `conteudo`.
- `elementosInternos` pode ser lista vazia, mas sempre deve existir.


- Para cada visual (`img`) planejado no wireframe, explicitar no `objetivo`/metadados da seção:
  - onde o visual entra na narrativa da página (posição e contexto comercial);
  - qual tipo de visual é esperado;
  - qual função comercial o visual cumpre;
  - qual objeção o visual ajuda a remover;
  - classificar o visual como: `mockup`, `foto`, `ilustração`, `diagrama` ou `print conceitual`.

Regras comerciais e estruturais obrigatórias (mantidas):
- Mobile-first obrigatório: priorize leitura vertical e CTA claro nas primeiras seções.
- Objetivo comercial obrigatório: estruturar a página para venda com foco na coleta de informação para envio de amostra/prova do produto (ex.: formulário/CTA de captura).
- Fase wireframe NÃO preenche copy: em TODOS os elementos, `texto.conteudo` deve ser string vazia (`""`) nesta etapa.
- Para tags de lista (`ul`), sempre declarar os `li` internos explicitamente.
- Formulário obrigatório da landing: incluir seção/formulário de captura contendo somente os campos `nome` e `email` (não incluir telefone, WhatsApp, CPF, empresa ou outros campos).
- Hero obrigatório com âncora primária: na primeira dobra (hero), incluir CTA com link âncora direto para a seção do formulário.
- Âncoras obrigatórias adicionais: incluir mais duas âncoras internas para pontos estratégicos distintos da página (ex.: mecanismo, prova social, oferta), além da âncora do hero para o formulário.
- Quantidade mínima de seções obrigatória: gerar no mínimo 4 seções em `pagina.corpo.secoes`.
- Quantidade mínima de imagens obrigatória: gerar no mínimo 4 elementos `img` no total da página.
- Regra mandatória para cumprir o mínimo de imagens: cada seção em `pagina.corpo.secoes[]` deve conter pelo menos um elemento `img` em `elementosSeccao` (direto ou em `elementosInternos`).
- Imagem de produto obrigatória: garantir que pelo menos uma `img` represente visualmente a ideia do produto/entrega que o cliente está comprando (ex.: mockup da solução, amostra do conteúdo, kit/resultado final esperado).
- Balanceamento visual obrigatório: intercalar blocos de texto e imagem ao longo da página para reduzir paredes de texto e melhorar escaneabilidade.

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
