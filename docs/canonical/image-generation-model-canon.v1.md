# Modelo canônico para geração de imagens

> STATUS: CANÔNICO
> ÚLTIMA VALIDAÇÃO: 2026-08-16

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

Por decisão de 2026-08-16, imagens que constituem entregáveis do plano comercial são criadas ou editadas por Têmis com `gpt-image-2` e qualidade `high`. O backend publica a fila `pending`, entrega referências autorizadas do mesmo plano, recebe o binário e persiste request, response, usage, custo, modelo e linhagem. O AI Worker não materializa imagens nesse fluxo.

Criação sem referência usa a Image API. Edição e composição híbrida usam o endpoint de edições com os arquivos reais da Biblioteca Audiovisual. Uma edição pode evoluir seu arquivo de origem ainda em `DRAFT`, mas referências adicionais de composição precisam estar `APPROVED`, ser imagens ativas e pertencer ao mesmo plano comercial. O backend revalida essas condições ao entregar a fila: referência removida, aposentada ou reprovada falha o job antes do consumo e nunca transforma silenciosamente uma edição em geração livre. Cada edição gera uma nova versão; o arquivo anterior permanece íntegro. Uma segunda execução independente de Têmis revisa o resultado já persistido e a execução produtora é tecnicamente impedida de aprovar o próprio trabalho.

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
