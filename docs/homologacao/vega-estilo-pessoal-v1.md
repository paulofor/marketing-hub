# Vega — mudança de foco para estilo pessoal

Decisão de 25/10/2026. O pedido muda o posicionamento do produto; não solicita lançamento,
novo consumo ou substituição silenciosa da candidata de um experimento existente.

## Evidência e escolha

A consulta à tela e ao backend confirmou produto #4, Big Idea v1 de Atena #21 e candidata
v14 no ciclo #13/experimento #106, ainda PLANNED. O cadastro global tratava redução de ruído,
feminilidade e elegância como direção universal. A própria Big Idea também mencionava
reconhecer o estilo, gerando um briefing ambíguo para novos contextos.

A superfície v14 usa regras locais. Os antigos prompts do `pde-ai-worker` que dizem
“elegante, feminina” não são a causa executora da candidata v14; alterá-los isoladamente
não mudaria o produto em tela. A mudança pedida deve começar pela fonte estratégica
compartilhada, preservando a versão histórica. Não criar outro agente.

Alternativas consideradas: manter elegância (não atende à decisão), trocar somente o
slogan por estilo (continua contraditório) ou alinhar Big Idea, público, promessa,
mecanismo, jornada e comunicação (escolhida). O cadastro atualizado orienta a construção
da próxima candidata; não comprova que a experiência de estilo já está homologada.

## Aceite antes da aplicação

- Registrar Big Idea v2 pela gestão de produtos, com motivo e histórico v1 preservados.
- Alinhar os campos comerciais editáveis à nova direção, mantendo id, slug, preço R$ 67,
  sete dias, pagamento único, 90 dias de acesso e autorizações nos seus escopos originais.
- Preservar integralmente contrato PDE publicado, ciclos/experimentos e pareceres existentes.
- Confirmar a persistência por API e MCP, sem novas tarefas de IA nem campanha.
- Reutilizar regressões locais da gestão e do contexto: evolução, campos obrigatórios,
  conflito, isolamento entre produtos, versão fixada e recuperação sem reler a ideia atual.
- Conferir leitura do novo conteúdo na tela. A avaliação semântica da próxima experiência
  deve comparar gostos distintos, neutralidade, salvar/retomar e dispositivos relevantes.

## Harness e limites do aprendizado

A correção do briefing é reutilizável: todos os agentes dos novos contextos recebem a
mesma Big Idea versionada. O mecanismo de fixação existente já protege histórico e
isolamento; não precisa de infraestrutura ou outro agente. Para Dédalo e Psique, o
aceite futuro passa a ser correspondência entre preferência e orientação, não adesão
à estética de quem produz ou revisa.

Concorrência é sinal de categoria existente e hipótese de mercado mais amplo, sem
comprovar tamanho, demanda por Vega, disposição de pagar ou superioridade. Medir uso
do primeiro ajuste, continuidade, compras líquidas e contribuição no próximo teste
comercial autorizado. Menos retrabalho e melhor conversão ainda não foram medidos.

Esta entrega não modifica o executável histórico v14. A próxima candidata de estilo
precisa de implementação e pareceres próprios antes de receber tráfego; não pode
reutilizar aprovações da direção anterior como prova de prontidão.

## Correção da apresentação da direção

Após o registro v2, a tela ainda destacava a proposta inicial de Atena acima da versão
vigente. A ordem foi corrigida: direção atual primeiro; pesquisa e fontes em consulta
recolhida. O endpoint existente já distingue revisão atual e pesquisa, sem novo estado
inferido no frontend. A regressão reproduz proposta antiga com decisão posterior e
confirma que consultar a pesquisa não salva, executa IA ou substitui a direção.

## Resultado operacional

A alteração foi aplicada pelo frontend em 25/10/2026: produto #4 agora se apresenta
como **Método MUSA - Seu Estilo em 7 Dias**; Big Idea v2, registro #5, com histórico v1
preservado. Foram alinhados público, dor, hipótese, promessa, mecanismo, jornada,
CTA, linguagem, narrativa e observação vigente. A nota antiga foi preservada como histórico.

A conferência por API e MCP confirmou os dados. Onze campos protegidos, incluindo
id/slug, preço, contrato PDE, validação e checkout, permaneceram iguais; os sete slots
não mudaram. Ciclo #13/experimento #106 permanece com a v14 e nenhuma tarefa PENDING/RUNNING.
Não houve chamada de IA, nova campanha ou gasto nesta alteração de direção.

Os 16 testes locais existentes de Big Idea passaram: evolução, versão fixada, contexto
para executores, isolamento e autoridade histórica. Os testes da gestão incluem
conflito, preservação do rascunho e consulta da proposta anterior sem sobrescrita.

## Resultado local da tela

- 109 testes frontend aprovados (11 da gestão do produto/Big Idea e 98 dos fluxos
  executados pelo CI do painel), além dos 16 testes de contexto backend já descritos.
- TypeScript e build de produção aprovados.
- Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: v2 em destaque, pesquisa
  recolhida, abertura do histórico sem escrita e ausência de overflow horizontal.
- Os testes usam dados sintéticos e interceptam toda a API; não geram métricas de
  mercado ou chamadas de modelo. Uma primeira asserção encontrou duas ocorrências
  legítimas da proposta (resumo e detalhe); o teste foi delimitado ao resumo visível.
- Diff revisado: nenhuma classe Java, migração, contrato publicado ou prompt de
  experiência histórica foi alterado. A tela utiliza os endpoints existentes.
