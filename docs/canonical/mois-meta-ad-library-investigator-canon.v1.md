# Investigador da Biblioteca de Anúncios Meta — cânone v1

## Objetivo

Encontrar padrões comerciais já expostos ao mercado sem afirmar venda, sucesso ou validação a partir de sinais artificiais. O módulo modela princípios e estruturas; nunca copia marca, texto, personagem, mídia ou criativo.

## Fonte e coleta recorrente

- A API oficial `ads_archive` limita anúncios que não alcançaram a União Europeia a temas sociais, eleições ou política. Portanto, ela não é fonte automática válida para o radar de produtos comerciais no Brasil.
- A investigação comercial brasileira criada no Marketing Hub entra em `ACTIVE_SUPERVISED`; a pessoa registra observações reais pela Biblioteca pública e o backend agenda o objetivo de reobservar o mesmo anúncio após 30 dias.
- O executor oficial só pode ser ativado para categoria e território aceitos pelo contrato vigente da Meta, após preflight real da permissão do aplicativo. Uma credencial válida com `ads_read` não comprova acesso à Biblioteca.
- O preflight deve executar uma consulta mínima real em `ads_archive`, sem reservar a fila antes da resposta. Presença do token, introspecção ou `ads_read=granted` isoladamente nunca podem marcar a integração como autorizada. Código e subcódigo devolvidos pela Meta devem aparecer sanitizados na saúde operacional, sem expor a credencial.
- A pessoa também pode abrir a Biblioteca pública, selecionar um anúncio e cadastrar ID, anunciante, URL pública, texto visível e sinais comerciais verificáveis.
- O backend valida, normaliza, deduplica, persiste observações e decide o gate. A tela nunca publica campanha ou consome orçamento.
- Quando elegível, o `mois-meta-ad-library-collector` é o executor canônico e recebe somente token dedicado por variável protegida de deploy; Argos nunca recebe a credencial.
- Cada cadastro bruto recebido deve ser persistido e correlacionado à investigação.
- O mesmo anúncio observado novamente na mesma investigação não aumenta sua contagem temporal.
- Cada observação supervisionada recebe identificador próprio para construir histórico sem inflar retries.
- Toda investigação declara `publisher_platform`; cada ativo preserva `publisher_platforms` exatamente como observado. Instagram e Facebook nunca podem ser presumidos equivalentes para validar um canal de aquisição.

## Uso pela Descoberta PDE

- Argos declara a consulta de categoria no plano, sempre com país, plataforma, termos específicos e limite.
- O Product Discovery Worker chama somente o endpoint interno do próprio domínio com o lease vigente. O backend cria ou reutiliza uma investigação MOIS idempotente e devolve a cobertura persistida; nenhuma credencial Meta deixa o coletor dedicado.
- A resposta separa `sourceStatus`, modo de coleta, investigação, URL oficial, anúncios aderentes, anúncios ativos, anunciantes distintos e data da última observação.
- Para ciclos B2C adquiridos no Instagram, somente evidência atual, ativa e explicitamente marcada como `INSTAGRAM` atende o gate de presença da categoria no canal.
- Anúncio é sinal de presença e investimento. Ele não conta como oferta paga comparável, compra, venda ou receita e nunca substitui checkout e pagamento reconciliados do Marketing Hub.

## Assistência por agente

- Um agente pode analisar e estruturar evidências persistidas pela API oficial ou cadastradas explicitamente no Marketing Hub.
- O agente não pode autenticar-se como usuário, raspar a interface da Meta, contornar limitação regional, CAPTCHA, rate limit ou controle de acesso.
- Toda extração assistida deve preservar a fonte, separar fato observado de inferência e exigir revisão humana antes de `MODELAR`.

## Diagnósticos separados

1. **Anúncio longevo:** mesmo identificador observado pelo menos duas vezes, com intervalo comprovado de 30 dias ou mais.
2. **Produto plausivelmente validado:** anúncio longevo, três ou mais variações, página ativa e pelo menos um sinal comercial externo auditável.
3. **Oportunidade adequada ao Marketing Hub:** produto plausivelmente validado e ficha ética com dor, público, mecanismo, oferta, ângulos e padrões modeláveis.

Tempo declarado pela plataforma ou presença em uma única consulta não comprova longevidade. O tempo é calculado entre `first_observed_at` e `last_observed_at` persistidos pelo Marketing Hub.

## Gate final

- `INVESTIGAR`: existe sinal inicial, mas faltam observações ou sinais independentes.
- `MODELAR`: todos os gates mínimos foram comprovados.
- `DESCARTAR`: a coleta real não encontrou anúncio ou os sinais comprovados contradizem a hipótese.

Toda decisão deve expor evidências e lacunas. O score mínimo de uma consulta é somente filtro e nunca pode elevar a nota calculada.

## Inteligência criativa e briefings

- Somente uma investigação em `MODELAR` pode gerar briefing.
- O briefing deve citar as evidências persistidas, declarar confiança e criar gancho, direção visual, ângulo e CTA originais.
- Longevidade é sinal de mercado, nunca prova de venda ou autorização para chamar um anúncio de vencedor.
- É proibido copiar marca, texto, personagem, mídia ou composição identificável da fonte.
- Todo briefing fica em `READY_FOR_AD_SPECIALIST` e precisa passar pelo Agente Especialista em Aprovação de Anúncios e pela aprovação humana antes de qualquer publicação.
- Resultado próprio do Marketing Hub — CTR, checkout, venda, margem e satisfação — é a única confirmação comercial do padrão modelado.

## Fontes na tela

Fontes sem coletor real devem aparecer desabilitadas e identificadas como `em implantação`. No Brasil, o Radar Meta comercial deve aparecer explicitamente como supervisionado; ele nunca pode prometer coleta automática, raspar a interface pública ou tratar ausência causada pelo contrato da API como ausência de mercado.

A tela deve oferecer o atalho oficial de pesquisa em nova aba e capturar a plataforma realmente
observada. Enquanto o aplicativo não passar no preflight de `ads_archive`, o coletor automático não
deve reservar pendências; a saúde do módulo permanece disponível, mas informa autorização externa
separadamente da saúde do processo.
