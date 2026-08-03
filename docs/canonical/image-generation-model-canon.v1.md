# Modelo canônico para geração de imagens

> STATUS: CANÔNICO
> ÚLTIMA VALIDAÇÃO: 2026-08-03

## Decisão

Toda geração de imagem nova do Marketing Hub deve usar, por padrão, o modelo de imagem de maior qualidade disponível e homologado para produção no provedor adotado. Economia de custo ou velocidade não autoriza silenciosamente o uso de um modelo visual inferior quando a imagem participa de anúncio, página de venda, produto, prova visual ou entrega ao cliente.

Na OpenAI, o padrão homologado na data desta decisão é `gpt-image-2`, com qualidade `high`. Novos fluxos não podem introduzir `gpt-image-1`, `gpt-image-1.5` ou outro modelo anterior como padrão ou fallback silencioso.

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

Para bibliotecas reutilizadas em entregas comerciais, a aprovação humana deve acontecer antes da promoção do asset ao acervo de produção.

## Exceções e fallback

Um modelo inferior somente pode ser usado quando houver indisponibilidade comprovada, incompatibilidade funcional ou decisão explícita de custo para um ativo não comercial. A exceção deve:

- ser configurada, nunca hardcoded de forma oculta;
- registrar causa, impacto, modelo alternativo e execução afetada;
- bloquear a publicação automática quando a qualidade final puder prejudicar anúncio, conversão ou entrega ao cliente;
- preservar a tentativa posterior com o modelo canônico quando aplicável.

É proibido reduzir o modelo apenas para fazer o pipeline concluir tecnicamente. Em caso de dúvida entre entregar uma imagem inferior e bloquear a entrega, o sistema deve bloquear e expor a causa.

## Regra de prevenção de recorrência

Testes de contrato ou arquitetura devem impedir que modelos visuais obsoletos voltem a ser definidos como padrão. Exemplos históricos e relatórios podem manter o identificador originalmente usado, mas não servem como recomendação operacional.
