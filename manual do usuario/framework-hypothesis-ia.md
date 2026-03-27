# Manual do módulo "Framework Dor → Resultado → Mecanismo → Prova → Oferta"

## Objetivo do módulo
Este módulo organiza todo o aprendizado de hipótese dentro do Marketing Hub. A tela de detalhe da hipótese
(`/niches/:nicheId/hypotheses/:hypothesisId`) reúne os cinco blocos do framework e permite que o administrador:

1. Revisar o que já foi aprendido em cada seção (dor, resultado, mecanismo, prova e oferta);
2. Registrar instruções adicionais e pedir para a IA atualizar apenas o bloco selecionado;
3. Validar rapidamente se a hipótese já tem insumos suficientes para seguir para experimento.

## Pré-requisitos
- Nicho e hipótese já cadastrados no Marketing Hub.
- Perfil com permissão para acessar **Niches → Hypotheses**.
- IA Worker em execução com a variável `OPENAI_API_KEY` configurada (caso contrário o botão "Gerar com IA" ficará
  disponível, mas o job falhará no worker).

## Como usar pelo front-end
1. **Acesse o nicho**: vá em **Niches → {nome do nicho} → Hypotheses** e clique na hipótese desejada.
2. **Localize o card “Framework Dor → Resultado → Oferta”**. Ele contém abas para cada seção:
   - **Dor**: superfície, raiz, dor emocional, dor social e custo.
   - **Resultado**: resultado desejado, identidade, impacto de negócio e sinal de sucesso.
   - **Mecanismo**: mecanismo central, diferencial, elemento visível e porquê acreditar.
   - **Prova**: tipo de prova, ativo, mensagem e estágio.
   - **Oferta**: nome, promessa, entregáveis, redução de risco, narrativa de preço, preço e CTA.
3. **Personalize as instruções (opcional)**: ao selecionar uma aba, use o campo de texto “Instruções extras” para
   direcionar o tom, o foco ou restrições que a IA deve respeitar.
4. **Dispare a geração**:
   - Clique em **Gerar com IA**. O botão mostra um spinner “Gerando...” enquanto o backend cria o job.
   - Você receberá um toast “Seção atualizada com IA”. Isso apenas confirma que o job foi enfileirado; a escrita
     efetiva acontece quando o AI Worker finaliza a chamada na OpenAI.
   - Normalmente a resposta chega em até 2 minutos. A própria tela recarrega a hipótese via React Query, então basta
     permanecer na página ou clicar em **Refresh** no topo se precisar forçar a atualização.
5. **Revisão e checklist**: abaixo das abas existe o checklist de aprovação. Marque manualmente cada item quando a
   seção estiver pronta. Isso ajuda a equipe a saber se a hipótese pode entrar em experimento.

## Boas práticas
- Use instruções curtas e muito específicas (“traga dores sobre consistência de aulas on-line” é melhor do que
  “fale mais da dor”).
- Gere uma seção por vez e revise antes de passar para a próxima; isso evita que o worker reescreva algo que já foi
  aprovado.
- Registre aprendizados importantes no campo “Notas” do checklist.
- Quando quiser preservar parte do texto atual, copie o trecho, inclua nas instruções e peça para “manter” ou
  “refinar” em vez de “reescrever tudo”.

## Como acompanhar jobs e resolver falhas
1. **Histórico recente**: ainda não há uma fila específica na UI para o framework. Use o próprio card para confirmar
   se os campos foram atualizados e verifique o horário de atualização da hipótese.
2. **Logs do backend**: acesse `http://191.252.181.168:8000/ops-mh-observability-v2/backend-log-stream-x9k` e filtre
   por `HypothesisFrameworkGenerationService` para encontrar jobs enfileirados.
3. **Logs do AI Worker**: em `http://191.252.120.96:4567/worker-observability/logfile` procure por
   `HypothesisFrameworkGenerationScheduler`. Erros da OpenAI ou de parsing aparecem como
   `Hypothesis framework job {id} failed`.
4. **Banco de dados**: a tabela `hypothesis_framework_generation_job` mostra o status (`PENDING`, `PROCESSING`,
   `FAILED`, `COMPLETED`). Use o endpoint interno `/api/internal/hypothesis-framework/jobs/pending` para validar se o
   worker ainda tem itens na fila.
5. **Falhas comuns**:
   - Credencial da OpenAI não configurada: os jobs ficarão sempre em `PENDING`.
   - Mudanças de API (como o parâmetro `text.format`) geram erros 400; após ajustar o backend, reenvie a seção.
   - Conteúdo inválido retornado pela IA: o job entra em `FAILED`. Revise o log, ajuste as instruções e gere novamente.

Seguindo estes passos o time consegue interagir com todo o framework direto do front-end principal, mantendo
rastreamento completo no backend e no AI Worker.
