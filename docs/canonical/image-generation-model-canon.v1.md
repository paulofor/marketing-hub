# Modelo canônico para geração de imagens

> STATUS: CANÔNICO
> ÚLTIMA VALIDAÇÃO: 2026-08-24

## Decisão

Toda geração de imagem nova do Marketing Hub deve usar, por padrão, o modelo de imagem de maior qualidade disponível e homologado para produção no provedor adotado. Economia de custo ou velocidade não autoriza silenciosamente o uso de um modelo visual inferior quando a imagem participa de anúncio, página de venda, produto, prova visual ou entrega ao cliente.

Na OpenAI, o padrão homologado na data desta decisão é `gpt-image-2`, com qualidade `high`. Novos fluxos não podem introduzir `gpt-image-1`, `gpt-image-1.5` ou outro modelo anterior como padrão ou fallback silencioso.

Por decisão operacional de 2026-08-09, nenhuma nova execução pode selecionar qualquer variante `gpt-image-1*`. Registros antigos permanecem somente como evidência histórica. Fluxos visuais usam `gpt-image-2`; agentes e tarefas de raciocínio compatíveis podem usar `gpt-5.6-sol`, sem tratá-lo como substituto automático do modelo visual.

## Seleção e atualização do modelo

- O código deve receber o modelo por configuração versionada, com um único padrão canônico por ambiente.
- Antes de criar ou alterar um pipeline visual, deve-se consultar a documentação oficial do provedor para confirmar qual é o modelo de imagem mais avançado disponível para a conta e para o endpoint utilizado.
- Quando surgir um modelo superior, a migração deve comparar um lote representativo com o modelo vigente, usando os mesmos briefings e critérios comerciais.
- O novo modelo torna-se padrão após compatibilidade técnica e aprovação visual humana mínima de 9/10.
- A revisão do modelo padrão deve ocorrer no mínimo a cada 90 dias e também quando o provedor anunciar uma nova geração de modelo de imagem.
- Cada job, lote ou asset deve persistir o provedor, o identificador exato do modelo, qualidade, tamanho, prompt, status, custo e resposta necessária para auditoria.

## Qualidade comercial obrigatória

Usar o modelo de ponta não substitui revisão. Imagens destinadas a produção devem passar por gate proporcional ao uso, avaliando no mínimo:

- aderência ao briefing, público e objetivo comercial;
- nitidez, composição, diversidade e legibilidade;
- ausência de mãos, rostos, objetos ou textos deformados;
- ausência de dados técnicos, prompts, identificadores internos ou dados pessoais indevidos;
- consistência com a oferta e com a experiência prometida ao cliente;
- direito de uso e rastreabilidade da origem.

Para bibliotecas reutilizadas em entregas comerciais, uma revisão visual independente da execução produtora deve acontecer antes da promoção do asset ao acervo de produção. Aprovação humana adicional continua possível quando o risco comercial ou os direitos de uso exigirem.

## Estúdio visual de Têmis

Por decisão de 2026-08-16, imagens que constituem entregáveis do plano comercial são criadas ou editadas por Têmis com `gpt-image-2` e qualidade `high`. A produção roda no container isolado `themis-image-studio`, sem identidade Codex nem responsabilidade de revisão. O backend publica a fila `pending`, entrega referências autorizadas do mesmo plano, recebe o binário e persiste request, response, usage, custo, modelo e linhagem. O AI Worker não materializa imagens nesse fluxo.

O binário retornado em `b64_json` deve ser decodificado e persistido uma única vez como artefato. A auditoria da resposta preserva metadados, usage, tamanho e SHA-256, substituindo o base64 por marcador explícito. É proibido duplicar o mesmo PNG no multipart, no JSON de auditoria e no payload persistido, pois isso amplia custo de memória e pode indisponibilizar o backend durante lotes visuais.

O executor calcula e reporta `costUsd` pela composição detalhada de tokens de imagem e texto, entrada e saída, retornada pelo provedor. Para `gpt-image-2`, a tabela vigente em 2026-08-16 usa por milhão de tokens: imagem de entrada US$ 8, texto de entrada US$ 5, imagem de saída US$ 30 e texto de saída US$ 10. Mudança de preço exige atualização desta fonte canônica e do teste de cálculo antes de nova produção.

Criação de um entregável visual pode usar a Image API sem referência quando o contrato do produto permitir. Edição e composição híbrida usam o endpoint de edições com os arquivos reais da Biblioteca Audiovisual. Peça comercial de produto não visual nunca usa geração livre: precisa partir de `PRODUCT_PROOF` ou `DELIVERY` aprovado. Uma edição pode evoluir seu arquivo de origem ainda em `DRAFT`, mas referências adicionais de composição precisam estar `APPROVED`, ser imagens ativas e pertencer ao mesmo plano comercial. O backend revalida essas condições ao entregar a fila: referência removida, aposentada ou reprovada falha o job antes do consumo e nunca transforma silenciosamente uma edição em geração livre. Cada edição gera uma nova versão; o arquivo anterior permanece íntegro. Uma segunda execução independente no `meta-ad-approver-worker` revisa o resultado já persistido e a execução produtora é tecnicamente impedida de aprovar o próprio trabalho. Falha, reinício, timeout, falta de credencial ou pressão de memória do Estúdio não pode derrubar o health nem interromper as filas de revisão.

Em criativos que demonstram uma entrega visual, o backend deve selecionar referências complementares por formato, no mínimo um post e um story quando ambos fizerem parte da promessa. O cenário pode ser gerado pelo GPT Image 2, mas os entregáveis aprovados são sobrepostos pelo compositor determinístico versionado, sem redesenho. A peça final precisa preservar pixels, proporções e legibilidade dos arquivos reais, registrar seus IDs/versões e voltar aos gates independentes de Psique e Têmis antes de ficar `READY`.

Por decisão de 2026-08-17, cada criação ou edição congela um playbook visual governado. O backend
resolve apenas a baseline canônica ou a versão promovida no mesmo nicho, tipo de produto, finalidade,
placement e formato, persiste versão, contexto e conteúdo no job e entrega até dois exemplos positivos
`APPROVED` do próprio plano. Memória candidata não entra no prompt. A geração seguinte pode aprender
com padrões confirmados, mas nunca altera jobs em curso, redesenha a entrega, elimina a revisão
independente ou amplia autorização de provider, gasto e publicação.

O contrato permite as dimensões homologadas `1024x1024`, `1024x1536`, `1536x1024`, `2048x2048` e `2048x1152`. O parâmetro `input_fidelity` deve ser omitido com `gpt-image-2`, pois o modelo já trata todas as referências em alta fidelidade. Requests, responses e URL externa são auditados; o binário base64 é persistido no registro técnico, mas redigido do log para não ampliar desnecessariamente o volume operacional.

## Exceções e fallback

Um modelo inferior somente pode ser usado quando houver indisponibilidade comprovada, incompatibilidade funcional ou decisão explícita de custo para um ativo não comercial. A exceção deve:

- ser configurada, nunca hardcoded de forma oculta;
- registrar causa, impacto, modelo alternativo e execução afetada;
- bloquear a publicação automática quando a qualidade final puder prejudicar anúncio, conversão ou entrega ao cliente;
- preservar a tentativa posterior com o modelo canônico quando aplicável.

É proibido reduzir o modelo apenas para fazer o pipeline concluir tecnicamente. Em caso de dúvida entre entregar uma imagem inferior e bloquear a entrega, o sistema deve bloquear e expor a causa.

## Regra de prevenção de recorrência

Testes de contrato ou arquitetura devem impedir que modelos visuais obsoletos voltem a ser definidos como padrão. Exemplos históricos e relatórios podem manter o identificador originalmente usado, mas não servem como recomendação operacional.
