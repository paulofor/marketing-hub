# Modelo canônico para geração de imagens

> STATUS: CANÔNICO
> ÚLTIMA VALIDAÇÃO: 2026-09-14

## Decisão

Toda geração de imagem nova do Marketing Hub deve usar, por padrão, o modelo de imagem de maior qualidade disponível e homologado para produção no provedor adotado. Economia de custo ou velocidade não autoriza silenciosamente o uso de um modelo visual inferior quando a imagem participa de anúncio, página de venda, produto, prova visual ou entrega ao cliente.

Na OpenAI, o padrão homologado na data desta decisão é [`gpt-image-2.5-sunburst`](https://developers.openai.com/api/docs/models/gpt-image-2.5-sunburst), com qualidade `high`. A documentação oficial o classifica como o modelo de maior capacidade para geração e edição de imagens; o [guia oficial de imagens](https://developers.openai.com/api/docs/guides/image-generation) define os parâmetros compatíveis da Images API e da ferramenta `image_generation`. Novos fluxos não podem introduzir DALL-E, `gpt-image-1*`, `gpt-image-2` ou outro modelo anterior como padrão ou fallback silencioso.

Por decisão operacional atualizada em 2026-09-14, nenhuma nova execução pode selecionar um modelo visual anterior ao Sunburst. Registros antigos permanecem somente como evidência histórica. Fluxos visuais usam `gpt-image-2.5-sunburst`; agentes e tarefas de raciocínio compatíveis podem usar `gpt-5.6-sol`, sem tratá-lo como substituto automático do modelo visual.

## Alternativas consideradas em 2026-09-14

- **Sunburst em todas as novas gerações — escolhida:** maior capacidade e precisão de edição segundo a OpenAI, mesma tabela publicada de tokens da variante Flare e contrato único. O risco é maior latência que a opção rápida.
- **Flare em todas as gerações:** menor latência e boa qualidade, porém não atende ao pedido de usar o modelo mais capaz nos ativos que influenciam venda e entrega.
- **Sunburst para finais e Flare para rascunhos:** pode reduzir latência, mas cria dois padrões, duplica baselines e amplia o risco de um rascunho inferior ser promovido sem comparação controlada. Pode ser avaliada futuramente por experimento específico.

A escolha é Sunburst com qualidade `high`. `xhigh` e `max` ficam disponíveis no catálogo para decisão explícita; não são o padrão porque ampliam latência e custo sem resultado comercial comparativo medido no Marketing Hub.

## Seleção e atualização do modelo

- O código deve receber o modelo por configuração versionada, com um único padrão canônico por ambiente.
- Antes de criar ou alterar um pipeline visual, deve-se consultar a documentação oficial do provedor para confirmar qual é o modelo de imagem mais avançado disponível para a conta e para o endpoint utilizado.
- Quando surgir um modelo superior, a migração deve comparar um lote representativo com o modelo vigente, usando os mesmos briefings e critérios comerciais.
- O novo modelo torna-se padrão de execução após compatibilidade técnica. Cada asset comercial continua `DRAFT` até o gate visual e humano já exigido pelo seu fluxo; a troca de modelo não antecipa aprovação de peça.
- A revisão do modelo padrão deve ocorrer no mínimo a cada 90 dias e também quando o provedor anunciar uma nova geração de modelo de imagem.
- Cada job, lote ou asset deve persistir o provedor, o identificador exato do modelo, qualidade, tamanho, prompt, status, custo e resposta necessária para auditoria.
- Variáveis do modelo visual e do modelo textual que orquestra uma ferramenta de imagem devem ter nomes distintos. `OPENAI_IMAGE_MODEL` identifica o modelo da Images API; `OPENAI_IMAGE_TOOL_MODEL`, o modelo da ferramenta; e `OPENAI_IMAGE_ORCHESTRATION_MODEL`, quando necessário, identifica apenas o modelo textual da Responses API.

O caminho nativo de imagem do Codex App Server não expõe no contrato atual o identificador do modelo visual subjacente. Ele segue a capacidade nativa vigente da plataforma, mas não serve como evidência de execução em `gpt-image-2.5-sunburst`. Ativos que exijam comprovação desse modelo devem usar um dos contratos explícitos da Images API ou da ferramenta `image_generation` e persistir o modelo retornado.

## Qualidade comercial obrigatória

Usar o modelo de ponta não substitui revisão. Imagens destinadas a produção devem passar por gate proporcional ao uso, avaliando no mínimo:

- aderência ao briefing, público e objetivo comercial;
- nitidez, composição, diversidade e legibilidade;
- ausência de mãos, rostos, objetos ou textos deformados;
- ausência de dados técnicos, prompts, identificadores internos ou dados pessoais indevidos;
- consistência com a oferta e com a experiência prometida ao cliente;
- direito de uso e rastreabilidade da origem.

Para bibliotecas reutilizadas em entregas comerciais, uma revisão visual independente da execução produtora deve acontecer antes da promoção do asset ao acervo de produção. Aprovação humana adicional continua possível quando o risco comercial ou os direitos de uso exigirem.

## Estúdio comercial de Íris e fronteira com Dédalo

Por decisão revisada em 2026-08-28, o Estúdio de Imagens do plano comercial pertence a Íris e roda
no container isolado `iris-image-studio`, com `gpt-image-2.5-sunburst` e qualidade `high`. Ele não possui
identidade Codex nem responsabilidade de revisão. Têmis recebe somente a versão persistida para o
gate independente de integridade. O backend publica a fila `pending`, entrega referências aprovadas
do mesmo plano, recebe o binário e persiste request, response, usage, custo, modelo e linhagem. O AI
Worker não materializa imagens nesse fluxo.

O estúdio aceita somente `LANDING`, `ADS` e `SOCIAL` e nunca produz `DELIVERY` ou `PRODUCT_PROOF`.
Entregáveis e provas reais do PDE permanecem sob Dédalo e precisam nascer do fluxo versionado do
produto; `PRODUCT_PROOF` é captura ou exportação fiel, nunca geração que simule o que não existe.
Íris seleciona essas provas e pode compor sua apresentação comercial sem redesenhá-las. Copy,
composição estruturada, landing e peças code-native pertencem ao `communication-agent-worker`.

O binário retornado em `b64_json` deve ser decodificado e persistido uma única vez como artefato. A auditoria da resposta preserva metadados, usage, tamanho e SHA-256, substituindo o base64 por marcador explícito. É proibido duplicar o mesmo PNG no multipart, no JSON de auditoria e no payload persistido, pois isso amplia custo de memória e pode indisponibilizar o backend durante lotes visuais.

O executor calcula e reporta `costUsd` pela composição detalhada retornada pelo provedor. Para `gpt-image-2.5-sunburst`, a tabela oficial consultada em 2026-09-14 usa por milhão de tokens: imagem de entrada US$ 8, imagem de entrada em cache US$ 2, texto de entrada US$ 5, texto de entrada em cache US$ 1,25 e imagem de saída US$ 30; texto de saída não é cobrado. O catálogo usa uma estimativa somente dos tokens de imagem de saída para cada tamanho e qualidade, enquanto a execução final deve conciliar o `usage` real. Mudança de preço exige atualização desta fonte canônica e do teste de cálculo antes de nova produção.

Criação comercial nunca usa geração livre: precisa partir de `PRODUCT_PROOF` ou `DELIVERY` aprovado. Edição e composição híbrida usam o endpoint de edições com os arquivos reais da Biblioteca Audiovisual. Uma edição pode evoluir sua própria peça ainda em `DRAFT`, mas referências adicionais de composição precisam estar `APPROVED`, ser imagens ativas e pertencer ao mesmo plano comercial. O backend revalida essas condições ao entregar a fila: referência removida, aposentada ou reprovada falha o job antes do consumo e nunca transforma silenciosamente uma edição em geração livre. Cada edição gera uma nova versão; o arquivo anterior permanece íntegro. Uma segunda execução independente no `meta-ad-approver-worker` revisa o resultado já persistido e a execução produtora é tecnicamente impedida de aprovar o próprio trabalho. Falha, reinício, timeout, falta de credencial ou pressão de memória do Estúdio não pode derrubar o health nem interromper as filas de revisão.

Em criativos que demonstram uma entrega visual, o backend deve selecionar referências complementares por formato, no mínimo um post e um story quando ambos fizerem parte da promessa. O cenário pode ser gerado pelo GPT Image 2.5 Sunburst, mas os entregáveis aprovados são sobrepostos pelo compositor determinístico versionado, sem redesenho. A peça final precisa preservar pixels, proporções e legibilidade dos arquivos reais, registrar seus IDs/versões e voltar aos gates independentes de Psique e Têmis antes de ficar `READY`.

Por decisão de 2026-08-17, cada criação ou edição congela um playbook visual governado. O backend
resolve apenas a baseline canônica ou a versão promovida no mesmo nicho, tipo de produto, finalidade,
placement e formato, persiste versão, contexto e conteúdo no job e entrega até dois exemplos positivos
`APPROVED` do próprio plano. Memória candidata não entra no prompt. A geração seguinte pode aprender
com padrões confirmados, mas nunca altera jobs em curso, redesenha a entrega, elimina a revisão
independente ou amplia autorização de provider, gasto e publicação.

O contrato prioriza as dimensões oficiais `1024x1024`, `1024x1536` e `1536x1024`; tamanhos personalizados somente podem ser usados dentro dos limites publicados pela OpenAI e com teste do consumidor. O parâmetro `input_fidelity` permanece omitido quando a fidelidade padrão atende ao contrato; se uma edição exigir `high`, a decisão deve ser explícita e auditada. Requests, responses e URL externa são auditados; o binário base64 é persistido no registro técnico, mas redigido do log para não ampliar desnecessariamente o volume operacional.

## Exceções e fallback

Um modelo inferior somente pode ser usado quando houver indisponibilidade comprovada, incompatibilidade funcional ou decisão explícita de custo para um ativo não comercial. A exceção deve:

- ser configurada, nunca hardcoded de forma oculta;
- registrar causa, impacto, modelo alternativo e execução afetada;
- bloquear a publicação automática quando a qualidade final puder prejudicar anúncio, conversão ou entrega ao cliente;
- preservar a tentativa posterior com o modelo canônico quando aplicável.

É proibido reduzir o modelo apenas para fazer o pipeline concluir tecnicamente. Em caso de dúvida entre entregar uma imagem inferior e bloquear a entrega, o sistema deve bloquear e expor a causa.

## Regra de prevenção de recorrência

Testes de contrato ou arquitetura devem impedir que modelos visuais obsoletos voltem a ser definidos como padrão. Exemplos históricos e relatórios podem manter o identificador originalmente usado, mas não servem como recomendação operacional.

O workflow `image-model-contract.yml` deve instalar explicitamente `ripgrep` e `python3`
antes de executar o verificador e sua suíte de regressão. A presença dessas ferramentas na
sandbox não comprova disponibilidade no runner do GitHub. O script distingue violação do
contrato (saída 1) de ausência de ferramenta, falha de leitura ou erro de PCRE2 (saída 2);
ambas bloqueiam o CI. Uma busca sem correspondências só comprova ausência de modelos
aposentados quando terminou sem erro. A suíte `scripts/test-canonical-image-model.py`
preserva essa distinção, os defaults e as exclusões de histórico e testes.
