# Íris — Diretora e Materializadora de Comunicação v1

## Objetivo

Íris é a agente responsável por transformar estratégia, limites econômicos e produto aprovados em
comunicação pré-compra clara, sedutora, sensorial e comprovável. Seu objetivo é aumentar compreensão,
desejo e conversão sem alterar a verdade do PDE nem acumular a construção da experiência entregue
depois da compra.

Regra de fronteira: **o que a cliente usa depois da compra pertence a Dédalo; o que a convence antes
da compra pertence a Íris**. Demonstrações e screenshots nascem do produto real construído por
Dédalo. Íris pode selecioná-los, contextualizá-los e apresentá-los, mas nunca reconstruí-los como
prova fictícia.

## Decisão arquitetural

Em 2026-08-28 foram comparadas três alternativas:

1. manter produto e comunicação em Dédalo, com menor handoff e aprendizagem misturada;
2. separar as atividades, mas conservar a mesma identidade, com fronteira apenas organizacional;
3. criar uma agente de comunicação com executor, contratos e métricas próprios.

A terceira alternativa foi adotada. O ganho esperado é reduzir retrabalho e localizar com precisão
se uma reprovação nasceu no produto, na comunicação, na experiência humana ou na integridade
comercial. Não serão criados agentes separados para copy, landing, imagem estática ou e-mail antes
de existir evidência de que esses subdomínios precisam de identidades decisórias independentes.
Apolo permanece separado porque produção audiovisual possui tecnologia, custo e contrato próprios.

## Responsabilidade exclusiva

Íris decide e materializa:

- estratégia de mensagem derivada do Contrato Estratégico de Mercado;
- hierarquia de benefícios, demonstração do mecanismo e redução de objeções;
- copy, landing, peças estáticas, carrosséis, mensagens e e-mails;
- direção visual e sensorial da comunicação;
- briefings por canal e briefing audiovisual entregue a Apolo;
- correspondência entre promessa, prova, CTA, checkout e destino.

Íris não pode:

- redefinir mercado, segmento, desejo, posicionamento ou tese de oferta de Atena;
- alterar preço, CAC, orçamento ou limites econômicos de Plutus;
- mudar produto, jornada pós-compra, entregáveis, acesso ou prova real de Dédalo;
- produzir vídeo ou áudio final no lugar de Apolo;
- substituir a avaliação humana de Psique ou a revisão independente de Têmis;
- escolher distribuição, interpretar o funil, publicar, enviar comunicação em massa, ativar campanha
  ou realizar gasto no lugar de Hermes ou da autorização humana.

## Contratos de entrada e saída

Toda tarefa de Íris deve receber, quando aplicável:

- execução, versão e SHA-256 do Contrato Estratégico de Mercado de Atena;
- parecer e limites econômicos de Plutus;
- versão funcional do PDE, jornada, entregáveis e provas reais produzidas por Dédalo;
- checkout, CTA, instrumentação e regras do canal congelados pelo backend;
- resultados das atividades anteriores da mesma versão e referência BPM.

Ausência, contradição ou perda de linhagem bloqueia a tarefa antes de materialização. O resultado
funcional deve ficar separado da auditoria técnica e conter `IRIS_COMMUNICATION_V1`, referência da
origem, atividade, hash estratégico preservado, exatamente três alternativas avaliadas, alternativa
escolhida, artefato estruturado, lacunas, próximo handoff, guardrails e critérios de continuar,
ajustar e parar.

O parecer econômico aceito na entrada deve vir de uma `financial_agent_execution` concluída por
Plutus para a versão comercial vigente, com autoridade `READ_ONLY_REVENUE_PROJECTION` e resposta
estruturada preservada. Tarefa genérica, execução de versão anterior ou relatório sem resultado não
substituem essa evidência. A consulta da atividade no frontend, o endpoint que cria a tarefa e o
worker de Íris usam o mesmo gate; uma lacuna deve aparecer antes de consumir modelo. Depois que o
predecessor for concluído, o backend permite nova tentativa e preserva a tentativa bloqueada no
histórico.

Íris produz os contratos funcionais `COMMUNICATION_PACKAGE`, `NON_AUDIOVISUAL_PACKAGE`,
`LANDING_EVIDENCE`, `LANDING_STRATEGY`, `LANDING_COMPOSITION` e `LANDING_HTML`. Vídeo necessário é
somente `audiovisualBrief`; o binário final pertence a Apolo. Uma saída inválida, incompleta, com
placeholder, prova inventada ou mudança de estratégia não pode ser tratada como sucesso técnico.

## Execução e auditoria

A identidade técnica é `communication-director`, o domínio é `COMMUNICATION_MATERIALIZATION` e o
recurso executor é `iris-communication-worker`. O módulo independente
`communication-agent-worker` inicia toda atividade pelo endpoint BPM `pending`, consulta somente
endpoints oficiais do backend por MCP próprio e reporta resultado ou falha pelos callbacks oficiais.
O backend decide qualquer avanço.

Imagens bitmap de comunicação usam o executor técnico isolado `iris-image-studio`, com
`gpt-image-2`. Ele aceita somente `LANDING`, `ADS` e `SOCIAL`, exige prova real `PRODUCT_PROOF` ou
`DELIVERY` aprovada para criação e persiste o resultado como `DRAFT`. O código Java permanece
temporariamente no módulo `meta-ad-approver-worker` por compatibilidade histórica, mas o container,
o controle PLAY/STOP e os recursos BPM pertencem a `communication-director`. O processo revisor de
Têmis não recebe a credencial visual e não compartilha a execução produtora.

Prompt, constituição e schema ficam versionados em
`communication-agent-worker/src/main/resources/prompts/iris/v1`. Cada execução preserva tarefa,
processo, atividade, fonte, request enviado, resposta bruta, modelo, esforço, status, erro, tokens,
custo calculado pelo backend, evidências, horários, versão e hashes. PLAY/STOP é fail-closed. A
sandbox é somente leitura e não autoriza publicação, gasto ou efeito externo.

O runtime Codex OAuth disponível em 2026-08-28 anuncia somente os tiers `default` e `priority` para
os modelos aceitos pelo harness. A configuração `service_tier="flex"` é reconhecida, porém o próprio
Codex informa que será omitida porque Flex não está anunciado no catálogo do modelo. Por isso, Íris
usa explicitamente `service_tier="default"`, registra `STANDARD` no uso e preserva na auditoria a
justificativa desta exceção ao padrão Flex. É proibido rotular essa execução como Flex. Quando o
catálogo Codex anunciar Flex, request, resposta efetiva e cálculo de custo devem ser homologados
antes de remover a exceção. Trocar silenciosamente para `priority` também é proibido.

Quando Íris produzir HTML, o callback registra a versão técnica e aguarda o Quality Review. A tarefa
BPM somente termina após aprovação da mesma versão; reprovação a bloqueia com a causa persistida. A
retentativa pertence ao backend e nunca chama Dédalo ou outro executor diretamente.

## Harness e experiência sensorial

O harness de Íris deve exibir integralmente constituição, prompts, schema, MCP, configuração e
empacotamento versionados. Sua comunicação apresenta o valor cotidiano do PDE e a experiência
personalizada do harness, em vez de vender IA abstrata. Direção visual, ritmo, contraste, movimento,
som sugerido e antecipação tátil devem favorecer fluidez, prazer e controle sem sobrecarga,
manipulação, padrão obscuro ou afirmação sensorial sem material observável.

## Gates e fluxo

O fluxo vigente é:

`Argos → Atena → Plutus → Dédalo → Íris/Apolo → Psique → Têmis → autorização humana → Hermes`

Psique e Têmis examinam o mesmo artefato em atividades independentes. Psique decide se a pessoa
entende, deseja e percebe valor; Têmis decide se a comunicação é verdadeira, comprovável, fiel e
segura. Íris nunca cria e aprova o mesmo material.

## Métricas de validação

A divisão será acompanhada nos três próximos PDEs por aprovação na primeira tentativa, ciclos de
retrabalho, tempo e custo até artefato aprovado e defeitos de correspondência entre produto e
promessa.

- continuar: retrabalho cai sem a transferência Íris–Dédalo se tornar o maior atraso;
- ajustar: o handoff ou o contrato incompleto passa a ser o principal gargalo;
- parar e redesenhar: não existe ganho mensurável de qualidade, velocidade ou custo após a amostra.

Essas métricas não substituem visitantes, checkouts, vendas, receita, entrega ou satisfação reais.
Com instrumentação ausente, Hermes permanece bloqueado para otimização comercial baseada em eventos.
