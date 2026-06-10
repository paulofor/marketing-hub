# Plano de implementação — Públicos gerais no Marketing Hub sem sobrepor o fluxo CNAE

## 1. Objetivo

Inserir no Marketing Hub um tratamento próprio para **públicos gerais** — como beleza, renda extra, relacionamento e outros agrupamentos comportamentais amplos — sem misturar esse fluxo com o pipeline OPRM NichoCNAE já existente.

O objetivo de negócio é permitir que o Marketing Hub descubra subnichos, dores e primeiras iscas em mercados amplos, mantendo o eixo central:

> **Dor → Resultado → Mecanismo → Prova → Oferta**

A inclusão deve aumentar a capacidade de encontrar oportunidades comerciais, mas sem enfraquecer a rastreabilidade e a especialização que o fluxo CNAE já oferece.

## 2. Princípio de separação

Públicos gerais e públicos derivados de CNAE devem ser tratados como **origens diferentes de descoberta de mercado**.

| Aspecto | Fluxo CNAE atual | Novo fluxo de públicos gerais |
| --- | --- | --- |
| Origem | CNAE, CNPJ, MEI e atividade econômica oficial | Mercado amplo, comportamento, intenção, dor ou categoria de consumo |
| Unidade inicial | CNAE/candidato de nicho técnico | Semente de público geral |
| Pergunta principal | “Qual realidade operacional existe nesta atividade econômica?” | “Quais subnichos e dores comerciais existem dentro deste público amplo?” |
| Saída inicial | Rotina, tarefas, dificuldades e perfil MEI/autônomo | Mapa de subnichos, dores de entrada, linguagem e iscas possíveis |
| Risco principal | Confundir CNAE com pessoa real | Criar campanha genérica sem dor específica |
| Uso ideal | Profissões, MEIs, autônomos, donos-operadores | Mercados amplos, comportamentos, desejos e dores não mapeados por CNAE |

Regra de implementação: **nenhum dado de público geral deve ser gravado nas tabelas OPRM CNAE como se fosse CNAE**. O vínculo com CNAE só pode existir quando o sistema encontrar uma correspondência real e opcional para auditoria, nunca como fonte obrigatória.

## 3. O que não deve ser feito

Para evitar sobreposição e perda de clareza, ficam proibidas as seguintes abordagens:

1. Criar registros falsos de CNAE para representar públicos como “renda extra”, “relacionamento” ou “beleza geral”.
2. Reaproveitar `oprm_niche_candidate` diretamente como origem primária de público geral sem campo de origem e sem rastreabilidade própria.
3. Enviar públicos gerais diretamente para publicação de campanha sem subnicho, dor, isca e pergunta qualificadora.
4. Misturar score CNAE com score de público geral na mesma régua sem distinguir a origem.
5. Tratar público amplo como nicho final. Público amplo é ponto de partida, não destino comercial.
6. Alterar a regra canônica do fluxo NichoCNAE para acomodar mercados gerais.
7. Usar público geral para contornar validações de targeting, compliance ou qualidade de landing.

## 4. Conceito novo: semente de público geral

Criar um conceito específico chamado **Semente de Público Geral**.

Uma semente representa uma área ampla de mercado que ainda precisa ser quebrada em subnichos e dores testáveis.

Exemplos:

- Beleza;
- Renda extra;
- Relacionamento;
- Profissionais autônomos com agenda irregular;
- Pessoas que vendem serviços pelo WhatsApp;
- Pessoas com dificuldade de transformar habilidade em oferta;
- Pequenos prestadores que dependem de indicação.

### 4.1 Campos mínimos da semente

- `id`;
- `name`;
- `description`;
- `marketContext`;
- `country`;
- `language`;
- `seedType`;
- `status`;
- `businessGoal`;
- `riskNotes`;
- `createdAt`;
- `updatedAt`.

### 4.2 Tipos sugeridos de semente

- `CATEGORY`: categoria ampla, como beleza ou fitness;
- `DESIRE`: desejo amplo, como renda extra;
- `LIFE_CONTEXT`: contexto de vida, como pós-término ou recolocação;
- `BEHAVIOR`: comportamento, como vender pelo WhatsApp;
- `CHANNEL`: dependência de canal, como Instagram, WhatsApp ou app de relacionamento;
- `PAIN_CLUSTER`: agrupamento de dores, como agenda vazia ou falta de confiança para cobrar.

## 5. Modelo de dados proposto

### 5.1 `oprm_general_audience_seed`

Tabela para armazenar a semente de público geral.

Campos principais:

- `id` BIGINT PK;
- `name` VARCHAR(191) NOT NULL;
- `description` LONGTEXT NULL;
- `market_context` LONGTEXT NULL;
- `country` VARCHAR(64) NOT NULL DEFAULT 'BR';
- `language` VARCHAR(32) NOT NULL DEFAULT 'pt-BR';
- `seed_type` VARCHAR(32) NOT NULL;
- `status` VARCHAR(32) NOT NULL;
- `business_goal` LONGTEXT NULL;
- `risk_notes` LONGTEXT NULL;
- `created_at` DATETIME NOT NULL;
- `updated_at` DATETIME NOT NULL.

Status sugeridos:

- `DRAFT`;
- `READY_FOR_RESEARCH`;
- `RESEARCHING`;
- `MAPPED`;
- `PAUSED`;
- `ARCHIVED`.

### 5.2 `oprm_general_audience_subniche`

Tabela para subnichos derivados da semente geral.

Campos principais:

- `id` BIGINT PK;
- `seed_id` BIGINT NOT NULL;
- `name` VARCHAR(191) NOT NULL;
- `persona_summary` LONGTEXT NULL;
- `pain_summary` LONGTEXT NULL;
- `desired_outcome_summary` LONGTEXT NULL;
- `language_patterns` LONGTEXT NULL;
- `channels_summary` LONGTEXT NULL;
- `qualification_question` LONGTEXT NULL;
- `status` VARCHAR(32) NOT NULL;
- `opportunity_score` DECIMAL(5,2) NULL;
- `risk_score` DECIMAL(5,2) NULL;
- `market_niche_id` BIGINT NULL;
- `created_at` DATETIME NOT NULL;
- `updated_at` DATETIME NOT NULL.

Status sugeridos:

- `DISCOVERED`;
- `NEEDS_REVIEW`;
- `APPROVED_FOR_EXPERIMENT`;
- `REJECTED`;
- `CONVERTED_TO_NICHE`.

### 5.3 `oprm_general_audience_pain_angle`

Tabela para dores e ângulos testáveis dentro de um subnicho.

Campos principais:

- `id` BIGINT PK;
- `subniche_id` BIGINT NOT NULL;
- `pain` LONGTEXT NOT NULL;
- `desired_result` LONGTEXT NOT NULL;
- `mechanism_direction` LONGTEXT NULL;
- `proof_or_lead_magnet` LONGTEXT NULL;
- `safe_promise` LONGTEXT NULL;
- `first_ad_hook` LONGTEXT NULL;
- `landing_confirmation_question` LONGTEXT NULL;
- `compliance_notes` LONGTEXT NULL;
- `status` VARCHAR(32) NOT NULL;
- `created_at` DATETIME NOT NULL;
- `updated_at` DATETIME NOT NULL.

### 5.4 `oprm_general_audience_source_evidence`

Tabela para evidências agregadas e rastreáveis usadas no mapeamento.

Campos principais:

- `id` BIGINT PK;
- `seed_id` BIGINT NOT NULL;
- `subniche_id` BIGINT NULL;
- `source_url` VARCHAR(1024) NULL;
- `source_domain` VARCHAR(191) NULL;
- `source_type` VARCHAR(64) NULL;
- `evidence_summary` LONGTEXT NOT NULL;
- `captured_at` DATETIME NOT NULL.

Observação: persistir apenas evidência agregada e rastreável. Não persistir dados pessoais, comentários integrais desnecessários ou material sensível.

## 6. Fluxo funcional proposto

### Etapa 1 — Cadastro da semente

O usuário cadastra ou seleciona uma semente de público geral.

Exemplo:

> Beleza — profissionais autônomas que dependem de agenda, WhatsApp e Instagram para vender serviços.

A tela deve deixar claro que a semente ainda não é nicho nem campanha.

### Etapa 2 — Descoberta de subnichos

O sistema gera ou permite cadastrar subnichos derivados.

Exemplo para beleza:

- manicure autônoma;
- designer de sobrancelha;
- esteticista facial;
- lash designer;
- depiladora;
- cabeleireira autônoma.

Cada subnicho precisa conter:

- quem é a pessoa;
- qual rotina ou contexto de trabalho importa;
- quais dores aparecem;
- quais canais usa;
- como confirmar que o lead pertence ao público.

### Etapa 3 — Mapeamento de dores e linguagem

Para cada subnicho aprovado, o sistema deve mapear dores e linguagem real.

Exemplo para manicure:

- agenda vazia;
- clientes que somem;
- Instagram que recebe curtida, mas não gera WhatsApp;
- medo de cobrar mais;
- dificuldade de organizar horários.

A saída dessa etapa não deve ser uma oferta final. Deve ser um mapa de dores e oportunidades.

### Etapa 4 — Construção de ângulos seguros

Cada dor aprovada vira um ângulo testável.

Exemplo:

- Dor: agenda vazia durante a semana;
- Resultado: reativar clientes antigas;
- Mecanismo: mensagens prontas para WhatsApp;
- Prova/isca: kit com 12 mensagens;
- Pergunta qualificadora: “Você trabalha como manicure hoje?”.

### Etapa 5 — Conversão controlada para `MarketNiche`

Somente após aprovação humana ou regra de qualidade, o subnicho pode virar `MarketNiche`.

A criação de `MarketNiche` deve carregar apenas o que é necessário:

- nome específico do nicho;
- descrição;
- segmentação base;
- interesses/demografia sugeridos;
- vínculo opcional com a origem de público geral.

Essa etapa não deve alterar nem depender das tabelas de CNAE.

### Etapa 6 — Criação de hipótese

A hipótese deve ser específica para uma dor principal.

Modelo:

> Acreditamos que [subnicho] com [dor] responderá melhor a [isca/promessa segura] do que a uma mensagem genérica, porque [mecanismo de dor percebida].

Exemplo:

> Acreditamos que manicures autônomas com agenda irregular responderão melhor a uma promessa de reativação de clientes antigas pelo WhatsApp do que a uma promessa genérica de marketing digital, porque a dor percebida é horário vazio e dinheiro perdido.

### Etapa 7 — Criação de experimento de lead/isca

O primeiro experimento para público geral deve priorizar lead ou WhatsApp, não venda direta.

Campos obrigatórios do pacote experimental:

- público/subnicho;
- dor principal;
- isca;
- promessa segura;
- pergunta qualificadora;
- métrica principal;
- stop-loss de CPL;
- duração curta;
- orçamento pequeno.

### Etapa 8 — Targeting inicial sem depender de CNAE

O targeting deve ser próprio do público geral e pode combinar:

- interesses;
- comportamentos;
- cargos quando existirem;
- gênero/faixa etária quando fizer sentido;
- criativo com frase de triagem;
- landing com confirmação do público.

Enquanto a regra canônica de publicação exigir ao menos um `JOB_TITLE` aprovado no fallback manual, o sistema deve operar no modo conservador:

- gerar ou solicitar cargo/termo aprovado quando existir;
- usar interesses e comportamentos como enriquecimento;
- não publicar ad set amplo puro se o publicador atual bloquear esse cenário.

Um modo futuro de público amplo guiado por criativo deve ser tratado como evolução canônica separada, não como exceção escondida.

### Etapa 9 — Landing/formulário como confirmação

A landing ou formulário deve confirmar:

- para quem é;
- qual dor resolve;
- o que a pessoa recebe;
- por que faz sentido;
- qual próximo passo;
- se o lead pertence ao público.

Perguntas qualificadoras são obrigatórias.

Exemplo para manicure:

> Você trabalha como manicure hoje?

Opções:

- sim, atendo em casa;
- sim, tenho espaço próprio;
- sim, trabalho em salão;
- estou começando;
- não sou manicure.

### Etapa 10 — Leitura de qualidade do público

Além de CTR e CPL, o sistema deve medir qualidade do público.

Sinais bons:

- lead informa profissão correta;
- resposta do formulário traz dor real;
- pessoa pede o material;
- pessoa responde no WhatsApp;
- há perguntas de preço ou próximo passo.

Sinais ruins:

- muita gente fora do perfil;
- curiosos sem profissão;
- baixo preenchimento;
- promessa confusa;
- lead baixa a isca, mas não responde.

## 7. Telas propostas

### 7.1 OPRM → Públicos Gerais

Nova tela principal para sementes.

Cards/colunas:

- nome da semente;
- tipo;
- status;
- subnichos descobertos;
- subnichos aprovados;
- experimentos criados;
- último processamento;
- risco/compliance.

Ações:

- criar semente;
- pausar;
- abrir detalhes;
- gerar subnichos;
- revisar subnichos.

### 7.2 Detalhe da semente

Deve mostrar:

- descrição da semente;
- contexto de mercado;
- subnichos encontrados;
- dores recorrentes;
- fontes/evidências;
- riscos;
- recomendações de próximos testes.

### 7.3 Detalhe do subnicho geral

Deve mostrar:

- persona resumida;
- dores;
- resultados desejados;
- canais;
- linguagem real;
- ângulos testáveis;
- perguntas qualificadoras;
- botão para converter em nicho;
- botão para criar experimento de lead.

### 7.4 Experimentos vinculados ao público geral

Na tela do experimento, mostrar a origem:

> Origem: Público Geral → Beleza → Manicure autônoma

Isso evita confusão com:

> Origem: OPRM NichoCNAE → CNAE X

## 8. Endpoints propostos

Todos devem ficar no escopo OPRM do backend principal.

### 8.1 Sementes

- `GET /api/oprm/general-audiences/seeds`
- `POST /api/oprm/general-audiences/seeds`
- `GET /api/oprm/general-audiences/seeds/{seedId}`
- `PATCH /api/oprm/general-audiences/seeds/{seedId}`
- `POST /api/oprm/general-audiences/seeds/{seedId}/archive`

### 8.2 Subnichos

- `GET /api/oprm/general-audiences/seeds/{seedId}/subniches`
- `POST /api/oprm/general-audiences/seeds/{seedId}/subniches`
- `GET /api/oprm/general-audiences/subniches/{subnicheId}`
- `PATCH /api/oprm/general-audiences/subniches/{subnicheId}`
- `POST /api/oprm/general-audiences/subniches/{subnicheId}/approve`
- `POST /api/oprm/general-audiences/subniches/{subnicheId}/reject`

### 8.3 Ângulos

- `GET /api/oprm/general-audiences/subniches/{subnicheId}/pain-angles`
- `POST /api/oprm/general-audiences/subniches/{subnicheId}/pain-angles`
- `PATCH /api/oprm/general-audiences/pain-angles/{angleId}`
- `POST /api/oprm/general-audiences/pain-angles/{angleId}/approve`

### 8.4 Conversões operacionais

- `POST /api/oprm/general-audiences/subniches/{subnicheId}/convert-to-market-niche`
- `POST /api/oprm/general-audiences/pain-angles/{angleId}/create-hypothesis`
- `POST /api/oprm/general-audiences/pain-angles/{angleId}/create-lead-experiment`

## 9. Pipeline operacional separado

Criar um pipeline novo, separado do NichoCNAE:

`OPRM_GENERAL_AUDIENCE_DISCOVERY`

Etapas sugeridas:

1. `GENERAL_AUDIENCE_SEED_REVIEW` — valida se a semente é ampla, útil e não duplicada;
2. `GENERAL_AUDIENCE_SUBNICHE_DISCOVERY` — propõe subnichos específicos;
3. `GENERAL_AUDIENCE_PAIN_MAPPING` — mapeia dores, linguagem e contexto;
4. `GENERAL_AUDIENCE_ANGLE_BUILDER` — transforma dores em ângulos de lead/isca;
5. `GENERAL_AUDIENCE_QUALITY_GATE` — bloqueia saídas genéricas, promessas arriscadas ou público indefinido;
6. `GENERAL_AUDIENCE_EXPERIMENT_BRIEF` — prepara pacote para criação de nicho, hipótese e experimento.

Esse pipeline não substitui o pipeline NichoCNAE. Ele é uma origem paralela de descoberta.

## 10. Critérios de qualidade

Um subnicho de público geral só pode avançar quando tiver:

1. nome específico;
2. dor principal clara;
3. frase de triagem que afasta público errado;
4. isca concreta;
5. pergunta qualificadora de formulário;
6. promessa segura;
7. risco/compliance avaliado;
8. métrica inicial definida;
9. explicação do motivo pelo qual esse público pode comprar algo depois.

Bloquear quando:

- o texto servir para qualquer público;
- a dor for genérica demais;
- a promessa parecer garantia de renda, relacionamento ou resultado sensível;
- não houver forma de confirmar se o lead pertence ao público;
- não houver próximo passo comercial plausível.

## 11. Métricas

### 11.1 Métricas de aquisição

- CTR;
- CPC;
- CPL;
- taxa de conversão da landing;
- taxa de resposta no WhatsApp/e-mail.

### 11.2 Métricas de qualidade de público

- percentual de leads dentro do subnicho;
- percentual de respostas com dor real;
- percentual de leads que pedem próximo passo;
- intenção declarada de compra;
- taxa de curiosos fora do perfil.

### 11.3 Métricas de aprendizado

- dor vencedora;
- subnicho vencedor;
- isca vencedora;
- linguagem vencedora;
- canal com melhor resposta;
- oferta futura mais promissora.

## 12. Primeiros casos recomendados

### 12.1 Beleza — manicure autônoma

- Dor: agenda vazia;
- Resultado: preencher horários ociosos;
- Isca: mensagens prontas para reativar clientes;
- Pergunta qualificadora: “Você trabalha como manicure hoje?”;
- Objetivo: lead ou WhatsApp.

### 12.2 Beleza — designer de sobrancelha

- Dor: Instagram não passa confiança;
- Resultado: parecer mais profissional antes da cliente chamar;
- Isca: checklist de perfil profissional;
- Pergunta qualificadora: “Você atende clientes de sobrancelha hoje?”;
- Objetivo: lead.

### 12.3 Beleza — esteticista facial

- Dor: cliente tem medo ou não entende o procedimento;
- Resultado: gerar confiança antes do atendimento;
- Isca: modelo de conteúdo educativo;
- Pergunta qualificadora: “Você trabalha com estética facial hoje?”;
- Objetivo: lead.

### 12.4 Renda extra — habilidade em oferta

- Dor: não saber o que vender;
- Resultado: transformar habilidade em primeira oferta simples;
- Isca: roteiro de primeira oferta;
- Pergunta qualificadora: “Qual habilidade você poderia transformar em serviço?”;
- Objetivo: lead.

### 12.5 Relacionamento — perfil em app

- Dor: perfil não vira conversa;
- Resultado: melhorar apresentação e clareza;
- Isca: checklist de perfil;
- Pergunta qualificadora: “Você usa app de relacionamento hoje?”;
- Objetivo: lead.

Observação: renda extra e relacionamento exigem cuidado maior de compliance. Evitar promessa de renda garantida, conquista garantida, manipulação emocional ou resultado absoluto.

## 13. Fases de implementação

### Fase 1 — Documentação e contrato

1. Registrar este plano.
2. Validar com o usuário os conceitos de semente, subnicho e ângulo.
3. Definir se será necessário criar cânone específico para públicos gerais ou seção nova no cânone OPRM.
4. Definir os status finais das entidades.

### Fase 2 — Banco e backend base

1. Criar changelog Liquibase com as tabelas novas.
2. Criar entidades Java com comentários de responsabilidade.
3. Criar repositories.
4. Criar services separados do fluxo CNAE.
5. Criar controllers em pacote OPRM próprio.
6. Documentar endpoints no Swagger.
7. Criar testes unitários dos services.

### Fase 3 — Frontend operacional

1. Adicionar item “Públicos Gerais” na navegação interna do OPRM.
2. Criar tela de listagem de sementes.
3. Criar tela de detalhe da semente.
4. Criar tela de detalhe do subnicho.
5. Adicionar ações de aprovação/conversão.
6. Mostrar origem separada nos experimentos criados.

### Fase 4 — Pipeline de descoberta

1. Criar pipeline `OPRM_GENERAL_AUDIENCE_DISCOVERY`.
2. Implementar etapa de descoberta de subnichos.
3. Implementar etapa de mapeamento de dores.
4. Implementar quality gate.
5. Persistir evidências agregadas.
6. Impedir saída genérica ou contaminada por promessa arriscada.

### Fase 5 — Conversão para experimento

1. Criar conversão de subnicho para `MarketNiche`.
2. Criar conversão de ângulo para hipótese.
3. Criar criação assistida de experimento de lead.
4. Gerar targeting inicial dentro das regras atuais.
5. Garantir pergunta qualificadora na landing/formulário.

### Fase 6 — Métricas e aprendizado

1. Marcar experimentos com origem `GENERAL_AUDIENCE`.
2. Medir qualidade do público no formulário.
3. Comparar subnichos e dores.
4. Sugerir próximo experimento com base em resposta real.
5. Identificar quando um público geral deve virar linha permanente de produtos.

## 14. Ordem recomendada de execução

1. Implementar somente cadastro e revisão manual de sementes/subnichos.
2. Rodar os três primeiros casos de beleza sem automação completa.
3. Validar se os campos são suficientes para decisão comercial.
4. Automatizar geração de subnichos.
5. Automatizar mapeamento de dores.
6. Só depois automatizar criação de hipótese/experimento.

Essa ordem reduz risco e evita que o Marketing Hub gere campanhas genéricas sem aprendizado real.

## 15. Decisão pendente

Antes de iniciar código, decidir:

1. O novo fluxo terá cânone próprio ou seção dentro do cânone OPRM?
2. O primeiro MVP será manual-assistido ou já terá geração por IA?
3. A publicação continuará usando targeting conservador ou será criado futuramente um modo canônico de público amplo guiado por criativo?
4. A origem `GENERAL_AUDIENCE` deve ser adicionada ao experimento desde o MVP ou somente quando houver criação automática?

## 16. Recomendação final

Começar com um MVP simples e separado:

> **OPRM → Públicos Gerais → Sementes → Subnichos → Ângulos → Experimento de lead**

Sem tocar no fluxo CNAE atual.

O primeiro lote deve ser de beleza, porque possui subnichos claros, dores práticas, iscas simples e menor risco de promessa sensível do que renda extra ou relacionamento.
