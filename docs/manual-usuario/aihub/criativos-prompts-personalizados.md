# Prompts personalizados para geração de criativos

Use esta funcionalidade para orientar a IA na geração dos anúncios de campanha, definindo tanto o texto quanto a cena das imagens diretamente pela interface do experimento.

## Pré-requisitos

1. O experimento precisa ter **KPI alvo** e **preset de métricas** configurados.
2. Cadastre ao menos **uma conta de Instagram** e selecione a página do Facebook (opcional) na aba **Criativos**.
3. Informe quantos criativos deseja solicitar no card "Biblioteca de criativos" quando quiser disparar uma nova rodada.

## Passo a passo

1. Acesse **Experimentos → (seu experimento) → aba Criativos**.
2. Revise a conta de Instagram e a página onde os anúncios serão publicados; clique em **Salvar página** se fizer alterações.
3. Role até o card **Prompts personalizados**.
4. Preencha:
   - **Prompt dos textos**: descreva o posicionamento, CTA, prova social ou restrições de linguagem que deseja aplicar.
   - **Prompt das imagens**: detalhe o enquadramento, cores e elementos visuais desejados.
5. Utilize variáveis para reutilizar informações do experimento:

   | Variável | Descrição |
   | --- | --- |
   | `{{quantity}}` | Quantidade de criativos solicitada no comando atual.
   | `{{experimentName}}`, `{{experimentId}}` | Identificação do experimento.
   | `{{hypothesisTitle}}`, `{{persona}}`, `{{problem}}`, `{{promise}}`, `{{mechanism}}`, `{{uniqueMechanism}}`, `{{entrega}}`, `{{successRule}}`, `{{offerType}}`, `{{price}}` | Campos da hipótese vinculada.
   | `{{headline}}`, `{{primaryText}}` | (Somente no prompt das imagens) reutiliza o conteúdo gerado para cada anúncio.

6. Clique em **Salvar prompts**. Se ambos os campos estiverem vazios, o sistema retorna ao comportamento padrão do worker.
7. Solicite novos criativos normalmente usando o banner "Biblioteca de criativos". O worker enviará os pedidos para a OpenAI no modo **batch assíncrono**, reduzindo custo e mantendo o processamento em background. Você pode acompanhar os resultados pelo próprio card de criativos.

## Boas práticas

- Indique o **formato de saída** (ex.: "responda em JSON com headline e primaryText") mesmo em prompts personalizados para reforçar o formato esperado.
- Combine variáveis com instruções claras, por exemplo:
  ```text
  Considere {{persona}} que enfrenta {{problem}} e ofereça uma solução baseada em {{uniqueMechanism}}. Cada anúncio deve fechar com um CTA convidando para {{entrega}}.
  ```
- No prompt das imagens, descreva elementos objetivos (ângulo, cores, emoções) e cite `{{headline}}` quando quiser que o visual dialogue com o texto gerado.
- Sempre que fizer ajustes, dispare uma nova solicitação de criativos para validar o resultado antes de aprovar o envio para mídia.
