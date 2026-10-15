# Prazer audiovisual a serviço da compreensão — 15/10/2026

## Evidência, hipótese e limite

Leitura do acervo `pesquisas/prazer-audio-visual`: triagem dos 39 relatórios de
28/08 a 11/10 e leitura dirigida dos mecanismos de expectativa, congruência,
atenção e carga concorrente. Fontes primárias conferidas em 15/10/2026:

| Acervo | Fonte primária e achado | Aplicação proposta e limite |
| --- | --- | --- |
| 28/08, seção 3 | [Mas-Herrero e Marco-Pallarés, PNAS 2025](https://pmc.ncbi.nlm.nih.gov/articles/PMC12304940/): preferência depende da interação entre surpresa e previsibilidade, com diferenças individuais | Situação familiar, pequena descoberta e recompensa compreensível; sem fórmula universal de cortes ou inferência de dopamina |
| 14/09, seção 1 | [Liu et al., Music Perception 2026](https://online.ucpress.edu/mp/article-abstract/43/5/501/215184/Seeing-the-MusicHow-Audiovisual-Dynamic-Congruence): dois experimentos com piano e espectrogramas, 60 e 40 participantes; congruência percebida favoreceu julgamento estético | Relacionar fala, ação visual e ênfase; resultado restrito ao paradigma, não prova publicitária |
| 19/09, seção 2 | [Cruz Heredia et al., Cognitive Science 2026](https://pubmed.ncbi.nlm.nih.gov/42725572/): interferência estrutural entre linguagem e música em tarefa dupla | Proteger a frase principal de competição musical; hipótese editorial, não proibição de música |
| 01/10, seção 1 | [Park et al., Psychophysiology 2026](https://pubmed.ncbi.nlm.nih.gov/42693580/): carga concorrente altera processamento e reconhecimento posterior de imagens | Evitar que prova, leitura e novidades simultâneas disputem atenção; experimento laboratorial, não efeito comercial medido |

Não ativar automaticamente cards DRAFT nem tratar a biblioteca como autorização de gasto.
Não implementar biometria, personalização por idade ou inferências neuroquímicas. O pedido
é uma melhoria compartilhada de capacidade, sem produto/ciclo ou nova produção paga.

A cadeia #26 e os agentes foram consultados na UI. O backend estava na main
`f2715a44181b0eb4dada1bc3bd07e450e8c8e9ca`, já com a melhoria narrativa #5598.
MCP confirmou Psique #731 (ADJUST, congruência 2) e #736 (APPROVED, fluidez 3,
congruência 3, sobrecarga 2), ambas sem audição. O histórico **não comprova** aprovação
com sobrecarga alta. A lacuna é preventiva, confirmada no código: a aprovação de vídeo
confere composição, copy e narrativa, mas não as notas sensoriais já emitidas.

Hipótese: coordenação de atenção, fala e prova reduz retrabalho por incompreensão e melhora
a chance de uma comunicação útil. A validação local só prova o contrato e sua integração.
Retenção, compra líquida, CAC, reembolso e contribuição dependem de mercado autorizado.

## Alternativas

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Acrescentar estudos sem mudar o aceite | Baixo esforço | Mantém contradição entre notas e decisão | Insuficiente |
| Criar agente/gate ou otimização biométrica | Especialização aparente | Custo, repasses e dados novos sem evidência de necessidade | Não adotado |
| Orientar os três agentes e conferir coerência dos campos existentes no vídeo | Corrige a lacuna e preserva o percurso | Mudança pequena, sem novo schema ou chamada | Adotado |

## Matriz definida antes dos testes

| Dimensão | Aceite local |
| --- | --- |
| Produção | Prompt composto de Íris/Apolo descreve foco de atenção, coerência, pausa de leitura e recompensa; não exige mais cortes nem trilha paga |
| Revisão | APPROVED exige evidência visual, fluidez e congruência >=3 e sobrecarga <=2 nos campos existentes |
| Falhas | Recusar aprovação contraditória, ausência de evidência e modalidades inventadas; notas de prazer não compensam falha de compreensão |
| Limites | Os números são política editorial interna, não limiares científicos; ADJUST/BLOCKED conservam diagnóstico e custos |
| Regressão | Identidade original, outra identidade, notas históricas válidas #736, imagem sem vídeo e recuperação serializada |
| Integração | Validadores e prompts reais com modelo simulado, schema existente, callback/outbox sem nova inferência |
| Interface | Sem mudança de player/UI; preservar captura mobile e contrato publicado, sem alegar audição por frames |
| Observabilidade | Motivo identifica campo contraditório; parecer preserva instante, fonte, limitação e correção |
| Isolamento e métrica | Fixtures sintéticas sem eventos comerciais; observar rejeições contraditórias/retrabalho, nunca contabilizar como vendas |

## Adoção e reversão

Aplicar somente a novas revisões de vídeos, sem reescrever pareceres históricos ou ampliar
os gates de imagem/produto. Adotar após testes locais e publicação dos mesmos recursos.
Se uma avaliação válida for recusada, preservar resultado e corrigir/revalidar a regra
antes de replay; não repetir modelo para satisfazer o validador. Não declarar aprendizado
criativo, voz natural ou aumento de vendas a partir de testes determinísticos.

## Resultados locais

- Reprodução antes da mudança: quatro falhas esperadas confirmaram que o aceite não
  recusava fluidez/congruência insuficientes, sobrecarga ou ausência sensorial.
- Psique: 206 testes executados, 11 integrações opcionais desabilitadas. Regressões
  exercitam o validador final real, duas identidades, limites históricos válidos,
  ADJUST/BLOCKED e serialização do diagnóstico para recuperação.
- Íris: 87 testes executados, três opcionais desabilitados. Apolo: 279 executados,
  nove opcionais desabilitados. Requests compostos incluem a direção sensorial;
  integração, limites e callbacks existentes permanecem válidos, sem modelo externo.
- Backend: 36 testes de catálogo, conteúdo e pesquisa aprovados. JAR inicializou os
  catálogos; 4.324 classes e 770 recursos empacotados coincidem com as fontes locais.
- Captura audiovisual: três testes JS e um smoke de pixels/instante com Chromium real
  passaram. Pacote comercial: 16 testes e 521 arquivos/114 manifestos preservados.
  Os 12 testes do verificador de recursos também passaram.
- Spotless passou nos arquivos Java alterados dos módulos que o configuram;
  `git diff --check` passou. O check amplo de Íris apontou somente formatação prévia em
  `PrivateCreativePreparationInputTest`, fora desta entrega; o arquivo alterado passou
  na conferência direcionada. Apolo preserva a formatação existente do módulo.

São 608 testes Java executados com sucesso, além dos testes de captura e empacotamento.
A primeira rodada concorrente esgotou o limite compartilhado de 512 processos da sandbox;
as suítes completas passaram sequencialmente com CPUs/threads limitadas por comando.
As dependências Node do módulo foram instaladas localmente, sem alterar os executáveis
globais. O empacotador foi reexecutado no diretório interno exigido pelo contrato após a
recuperação dos recursos. Nenhuma publicação foi usada como mecanismo de teste.
