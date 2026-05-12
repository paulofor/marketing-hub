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


{prompt-regras-globais}

Ângulo da Campanha que vai ser publicada: 
{dados-campaignAngle}

Copy do Anuncio:
{dados-adCopy}

Briefing das Imagens dos Anuncios:
{dados-adImageBriefing}


template_id: landing-wireframe
template_version: v2
artifact_target: landingPageWireframe

Regras fixas da etapa (formato simplificado):
- Entregar somente JSON válido no formato raiz `pagina`.
- Estrutura obrigatória: `pagina.head.texto`, `pagina.corpo.estilos[]`, `pagina.corpo.secoes[]`.
- Cada item de `estilos[]` deve ter apenas `nome` e `valor`.
- Cada seção deve conter: `nome`, `objetivo`, `oQueQuerProvocarNoUsuario`, `papelComercial`, `fasePersuasao`, `objeçãoQueRemove`, `prioridadeConversao`, `acaoEsperada`, `fonteContexto[]`, `id`, `estilos[]`, `elementosSeccao[]`.
- Cada item de `elementosSeccao[]` deve conter: `id`, `tag`, `texto`, `estilos[]`, `elementosInternos[]`.
- Quando `tag = "img"`, o elemento deve conter também `briefingVisual` com:
  - `ondeEntraNoVisual`;
  - `tipoVisualEsperado`;
  - `funcaoComercial`;
  - `objecaoQueRemove`;
  - `classificacaoVisual` em: `mockup`, `foto`, `ilustração`, `diagrama`, `print conceitual`.
- `elementosInternos[]` representa hierarquia de filhos e deve suportar recursão (filho pode conter netos e assim por diante), sempre com o mesmo contrato do elemento pai.
- Campo `texto` de cada elemento deve conter exatamente: `tamMaximo`, `tamMinimo`, `conteudo`.
- `elementosInternos` pode ser lista vazia, mas sempre deve existir.
- Não invente campos fora do schema.
- Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
- Evite JSON dentro de strings; mantenha cada informação no seu campo próprio.
- Mobile-first obrigatório: priorize leitura vertical e CTA claro nas primeiras seções.
- Objetivo comercial obrigatório: estruturar a página para venda com foco na coleta de informação para envio de amostra/prova do produto (ex.: formulário/CTA de captura).
- Fase wireframe NÃO preenche copy: em TODOS os elementos, `texto.conteudo` deve ser string vazia (`""`) nesta etapa.
- Para tags de lista (`ul`), sempre declarar os `li` internos explicitamente.
- Formulário obrigatório da landing: incluir seção/formulário de captura contendo somente os campos `nome` e `email` (não incluir telefone, WhatsApp, CPF, empresa ou outros campos).
- Hero obrigatório com âncora primária: na primeira dobra (hero), incluir CTA com link âncora direto para a seção do formulário.
- Âncoras obrigatórias adicionais: incluir mais duas âncoras internas para pontos estratégicos distintos da página (ex.: mecanismo, prova social, oferta), além da âncora do hero para o formulário.
- Balanceamento visual obrigatório: intercalar blocos de texto e imagem ao longo da página, inserindo elementos `img` em seções relevantes para reduzir paredes de texto e melhorar escaneabilidade.
- Imagem de produto obrigatória: garantir que pelo menos uma `img` represente visualmente a ideia do produto/entrega que o cliente está comprando (ex.: mockup da solução, amostra do conteúdo, kit/resultado final esperado).
- Para cada visual (`img`) planejado no wireframe, explicitar no `objetivo`/metadados da seção:
  - onde o visual entra na narrativa da página (posição e contexto comercial);
  - qual tipo de visual é esperado;
  - qual função comercial o visual cumpre;
  - qual objeção o visual ajuda a remover;
  - classificar o visual como: `mockup`, `foto`, `ilustração`, `diagrama` ou `print conceitual`.
- Heurística prática de composição (usar como inspiração, NÃO como regra rígida; adapte ao contexto do nicho/oferta):
  - Hero bullets: preferir ~3 itens.
  - Lista de entregáveis: preferir entre 3 e 5 itens.
  - Antes/depois: preferir 3 itens de "antes" e 3 itens de "depois".
  - Como funciona: preferir 3 passos.
  - FAQ: preferir entre 4 e 6 perguntas.
  - Formulário inicial: em cenários gerais, pode variar entre 3 e 4 campos; quando houver diretriz explícita desta execução, ela prevalece.
- Heurística para listas longas no mobile (inspiração contextual, não regra absoluta):
  - Evitar listas grandes no início da página.
  - Evitar mais de 5 itens visíveis por bloco quando a pessoa ainda não entendeu a oferta.
  - Evitar misturar no mesmo bloco: benefícios, recursos e explicações extensas.
  - Evitar estruturas que aumentem sensação de esforço, prejudiquem foco no CTA ou alonguem demais o mobile sem avanço de persuasão.
  - Listas grandes podem ser aceitáveis quando estiverem em FAQ recolhido, quebradas em cards, com hierarquia clara, posicionadas após entendimento da oferta ou quando necessárias para provar entrega concreta (ex.: mini-kit com 5 itens).

- Ajuste de intenção por seção (referência do esboço):
  - `papelComercial`: descreva a função comercial da seção no funil da landing (ex.: primeira dobra de conversão, remoção de risco, fechamento).
  - `fasePersuasao`: explicite a fase predominante (ex.: dor-promessa-prova-acao).
  - `objeçãoQueRemove`: declare a principal objeção que a seção resolve.
  - `prioridadeConversao`: inteiro de 1 a 10 (10 = mais crítico para conversão).
  - `acaoEsperada`: qual ação concreta o usuário deve tomar após consumir a seção.
  - `fonteContexto[]`: liste de onde a seção foi derivada (ex.: `PAIN_JSON.surface`, `campaignAngle.primaryPromise`).

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageWireframe` simplificado.

Campos obrigatórios:
- pagina
- pagina.head.texto
- pagina.corpo.estilos[] com nome, valor
- pagina.corpo.secoes[] com nome, objetivo, oQueQuerProvocarNoUsuario, papelComercial, fasePersuasao, objeçãoQueRemove, prioridadeConversao, acaoEsperada, fonteContexto, id, estilos, elementosSeccao
- pagina.corpo.secoes[].elementosSeccao[] com id, tag, texto, estilos, elementosInternos
- Para `tag = "img"`, `pagina.corpo.secoes[].elementosSeccao[].briefingVisual` é obrigatório no contrato.
- pagina.corpo.secoes[].elementosSeccao[].texto com tamMaximo, tamMinimo, conteudo
- Em wireframe, `conteudo` deve ser sempre `""` (sem texto final).

Formato de resposta:
- Precisamos da resposta em Json-Schema.
