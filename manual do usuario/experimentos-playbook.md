# Manual de uso — Experimentos com Playbook de Dor → Resultado → Mecanismo → Prova → Oferta

Este guia explica como preencher e revisar os novos campos obrigatórios dos experimentos no Marketing Hub:

- **Etapa do experimento (`stage`)**
- **Variável primária testada (`primary_variable`)**
- **Métrica principal de sucesso (`primary_metric`)**

Esses campos fazem parte da Fase 1 do plano do framework e garantem que cada teste tenha uma hipótese clara e mensurável.

---

## 1. Onde acessar

1. Entre no Marketing Hub.
2. Abra **Testes de Nicho (Experiments)** no menu lateral.
3. Use o botão **Novo Teste** ou edite um registro existente.

Ambas as telas (criação e edição) agora exibem um bloco com as sugestões do **Experiment Playbook**.

---

## 2. Preenchendo um novo experimento

1. **Escolha o Nicho** e a **Hipótese** normalmente.
2. **Etapa do experimento**
   - Use o seletor "Etapa do experimento" para definir qual parte do funil está sendo priorizada (Anúncio, Landing, Amostra ou Venda).
   - Ao mudar a etapa, o sistema limpa a variável/métrica para evitar inconsistências.
3. **Variável principal**
   - Descreva com poucas palavras o que será comparado. Ex.: "Dor medo de segurança vs. Resultado visitas".
   - Utilize os botões de sugestão exibidos logo abaixo para aplicar rapidamente uma variável recomendada pelo playbook.
4. **Métrica principal**
   - Informe o indicador usado para validar o teste. Exemplos: "CTR de link (%)", "Taxa de envio do formulário", "Taxa de resposta à amostra".
   - Clique em um dos botões sugeridos para aplicar métricas padrão e guard-rails.
5. Complete os demais campos (KPI, orçamento, jornada, contas etc.) e salve.

Dica: ao selecionar uma etapa, o playbook preenche automaticamente a primeira variável e a métrica sugerida. Ajuste conforme sua hipótese.

---

## 3. Editando um experimento existente

1. Abra o experimento e clique em **Editar**.
2. O topo do formulário exibirá o mesmo bloco de etapa/variável/métrica.
3. Você pode alterar a etapa a qualquer momento. Ao trocar a etapa o sistema limpa os campos para evitar combinações incorretas. Use novamente os botões de sugestão.
4. Salve para registrar o novo contexto — ele passa a aparecer nas listagens e nas telas de detalhe.

---

## 4. Como funciona o Playbook

- O frontend consome `/api/experiment-playbook` e exibe, para cada etapa, as principais variáveis e métricas recomendadas.
- Os botões aplicam automaticamente o texto sugerido, mantendo consistência com o framework Dor → Resultado → Mecanismo → Prova → Oferta.
- As descrições do playbook também lembram qual métrica é primária e quais guard-rails devem ser monitorados.

> **Importante:** as sugestões são um ponto de partida. Sempre ajuste o texto para refletir a hipótese específica do nicho.

---

## 5. Boas práticas

- **Uma etapa por experimento**: mantenha `stage` alinhado ao que realmente será validado. Não use "AD" se a hipótese está na amostra.
- **Variável descritiva**: escreva em linguagem natural. Isso facilita diagnósticos e relatórios.
- **Métrica acionável**: prefira indicadores que o time acompanhe diariamente (CTR, taxa de envio, taxa de resposta, fechamento etc.).
- **Revisão rápida**: ao abrir o detalhe do experimento, confira a seção "Etapa priorizada / Variável / Métrica" para garantir que o contexto ainda faz sentido.
- **Sincronize com o time**: sempre que alterar a etapa ou métrica avise a operação de mídia e o squad responsável pela prova/oferta.

Seguindo este fluxo, cada experimento fica ancorado em uma variável explícita, com métrica clara e rastreável, permitindo decisões mais rápidas e alinhadas ao framework oficial.
