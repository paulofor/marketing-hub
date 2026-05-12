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
- Cada seção deve conter: `nome`, `objetivo`, `oQueQuerProvocarNoUsuario`, `id`, `estilos[]`, `elementosSeccao[]`.
- Cada item de `elementosSeccao[]` deve conter: `id`, `tag`, `texto`, `estilos[]`, `elementosInternos[]`.
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

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageWireframe` simplificado.

Campos obrigatórios:
- pagina
- pagina.head.texto
- pagina.corpo.estilos[] com nome, valor
- pagina.corpo.secoes[] com nome, objetivo, oQueQuerProvocarNoUsuario, id, estilos, elementosSeccao
- pagina.corpo.secoes[].elementosSeccao[] com id, tag, texto, estilos, elementosInternos
- pagina.corpo.secoes[].elementosSeccao[].texto com tamMaximo, tamMinimo, conteudo
- Em wireframe, `conteudo` deve ser sempre `""` (sem texto final).

Formato de resposta:
- Precisamos da resposta em Json-Schema.
