# Módulo de Provas e Oferta do Framework

Este guia explica como usar os novos recursos da fase 2 do framework **Dor → Resultado → Mecanismo → Prova → Oferta** diretamente no Marketing Hub.

## 1. Catalogar provas no nível da hipótese

1. Abra uma hipótese em modo de edição.
2. Acesse a aba **Prova** do componente "Framework Dor → Resultado → Oferta".
3. Use o botão **Nova prova** para abrir o modal de cadastro.
   - Defina o **estágio** (Anúncio, Landing, Amostra ou Oferta).
   - Escolha um **tipo** existente da lista de provas visuais ou informe um **tipo personalizado**.
   - Preencha o **plano do ativo**, mensagem, notas de entrega e opcionalmente a URL do ativo.
4. Clique em **Salvar** para registrar o ativo. Todo item criado fica disponível na lista "Provas catalogadas".
5. Utilize o botão **Aplicar** em qualquer cartão para preencher automaticamente os campos da seção Prova do framework. O tipo, mensagem, ativo e estágio são copiados para o formulário, mantendo rastreabilidade.

> Enquanto a hipótese não estiver salva, o módulo mostra um alerta orientando a salvar primeiro — isso evita provas órfãs.

## 2. Atualizar ou reaproveitar provas existentes

- Clique em **Editar** em qualquer cartão para ajustar estágio, status ou narrativa.
- O campo **Status** permite marcar a prova como `Rascunho`, `Aprovada` ou `Arquivada`, ajudando a filtrar o que deve entrar no próximo experimento.
- Cada prova pode receber o prompt/modelo usado para gerar o ativo, garantindo rastreabilidade de IA.

## 3. Selecionar o pacote oficial da oferta

1. Acesse a aba **Oferta** do framework.
2. No bloco **Pacote oficial da oferta**, utilize o seletor para escolher um pacote já existente. Ele lista:
   - Pacotes criados diretamente para a hipótese.
   - Pacotes produzidos em experimentos que referenciam a mesma hipótese.
3. Ao selecionar um item, o cartão abaixo mostra descrição, deliverables e prompt utilizado.
4. Para criar um novo pacote sem sair da hipótese:
   - Clique em **Novo pacote**.
   - Nomeie o pacote, descreva e informe o prompt utilizado pelo worker.
   - Selecione os deliverables aprovados do nicho (checklist dentro do modal).
   - Salve e, se desejar, defina-o como pacote oficial (o seletor já vem posicionado no novo item).

> O pacote oficial fica persistido no campo `offerPackageId` da hipótese, permitindo que a oferta seja reutilizada em experimentos futuros sem reconstruir os entregáveis.

## 4. Recomendações operacionais

- **Provas aprovadas**: mantenha o status como `APPROVED` somente após validação humana. Assim, o time sabe quais assets podem subir direto para anúncios ou landing pages.
- **Pacotes reutilizáveis**: nomeie os pacotes de oferta com um padrão que combine nicho + promessa. Facilita encontrar rapidamente o pacote vencedor em hipóteses irmãs.
- **Sincronização com experimentos**: sempre que um experimento gerar um pacote validado, retorne à hipótese e selecione-o como pacote oficial. Isso mantém o aprendizado centralizado.
- **IA Worker**: ao preencher os campos de prompt/modelo tanto nas provas quanto nos pacotes, você cria um histórico reutilizável para futuras solicitações ao worker de IA.

Seguindo estes passos, o pipeline de Prova e Oferta fica 100% auditável dentro do Marketing Hub, dispensando planilhas paralelas e garantindo que cada experimento saiba exatamente qual ativo e pacote deve ser publicado.
