# Homologação local da integração comercial do Rigel — v1

Data: 2026-08-28

## Gargalo e decisão

O gargalo real é `INSTRUMENTACAO`: o Rigel permanece com zero eventos comerciais, zero visitantes
humanos e zero vendas. A atividade `Integrar canal, checkout, acesso e eventos` aparecia no processo
4, mas não possuía executor backend; por isso a interface não conseguia validar, persistir a
tentativa nem avançar o produto ao processo 5.

A métrica esperada nesta entrega é concluir ou bloquear a integração com evidência atual de URL,
oferta, checkout, acesso e eventos, sem fabricar tráfego. Continuar quando todos os contratos
responderem na mesma versão; ajustar quando o gate persistir causa concreta; parar antes de
pagamento, publicação, contato, campanha ou gasto, que pertencem a autorização e homologação
posteriores.

Foram comparadas três alternativas:

| Alternativa | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Reiniciar apenas o proxy | restaura a URL rapidamente | não registra o processo nem evita recorrência | baixo | baixa |
| Criar imediatamente um run de homologação | aproxima o teste final | pula a integração e mistura processos 4 e 5 | baixo | baixa |
| Gate oficial pela tela, contratos executáveis e BPM | preserva ordem, auditoria e causa do bloqueio | exige integração cross-módulo | médio | alta |

Escolha: gate oficial pelo backend e pela tela, com recuperação preventiva do proxy existente.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validações e falhas | Evidência obrigatória |
| --- | --- | --- | --- |
| Entrada pela tela | comando `Validar integração` chama o endpoint oficial da atividade | STOP, versão retirada ou pré-requisito pendente bloqueia | request, resposta e refresh da cadeia |
| Ordem BPM | criativos, landing e comunicação de Íris quando vigente estão concluídos | atividade anterior ausente não pode ser ultrapassada | tarefas e instâncias do mesmo produto/processo |
| Entrada de Íris | estratégia atual, projeção econômica da versão vigente, PDE e provas reais liberam o contrato de comunicação | tarefa genérica, parecer antigo ou predecessor ausente bloqueia antes do modelo | contexto congelado e motivo idêntico na tela, endpoint e worker |
| URL pública | health, contrato e aplicação respondem pela URL HTTPS do slot | conexão, status, slug ou URL divergente registra `BLOCKED` | status HTTP, URL resolvida, resumo e data atuais |
| Oferta | produto, experimento, preço, promessa, CTA, marca, registro, suporte, políticas e checkout coincidem | campo ausente, checkout não HTTPS ou identidade divergente bloqueia | contrato público da oferta |
| Integração | contrato da mesma `experienceVersion` declara eventos, analytics, login, workspace e conclusão | versão, rota ou contrato divergente bloqueia | `PDE_COMMERCIAL_JOURNEY_EVENTS_V1` |
| Eventos | landing emite visita, visualização de CTA, valor e checkout; backend registra compra, acesso, entrega, primeiro uso e reembolso | evento declarado mas não emitido ou ingerível falha no teste de contrato | catálogo único, observação do navegador e teste de ingestão |
| Correlação | evento, produto, versão, sessão, visitante e acesso são declarados | chave ausente bloqueia | lista versionada de correlações |
| Segregação | QA usa `INTERNAL_QA` ou `mh_test` | smoke não pode contar como visita ou venda humana | política pública e zero métricas comerciais fabricadas |
| Persistência | tentativa gera instância BPM com estado, horários, custo zero, evidência e causa | falha não pode virar sucesso nem apagar tentativa anterior | ocorrência incremental `COMPLETED` ou `BLOCKED` |
| Retentativa de agente | predecessor concluído oferece `Tentar novamente` | tarefa bloqueada não pode ser reutilizada como se estivesse pendente | nova tentativa na mesma ocorrência e histórico preservado |
| Avanço | sucesso atualiza a URL do experimento e move Rigel para processo 5 | bloqueio mantém processo 4 e permite revalidação | período anterior fechado e novo período aberto pelo backend |
| Prontidão seguinte | processo 5 reconhece a evidência integrada em vez de exigir pipeline legado incompatível | IDs ou slot divergentes não liberam | evidência estruturada do mesmo produto/experimento/slot |
| Recuperação do proxy | deploy conecta, inicia proxy existente parado e recarrega configuração | ausência real de proxy encerra o deploy com diagnóstico | teste de isolamento e porta 443 ativa |
| Observabilidade | erros de parse e integração preservam contexto e stack trace | falha silenciosa é proibida | logs correlacionados e causa persistida na instância |
| Métricas | atividade preparada não altera visitantes, checkouts, vendas, receita ou gasto | qualquer incremento não segregado falha | métricas comerciais permanecem reais |
| Navegadores | comando e resultado funcionam em Chromium desktop, iPhone 15 Pro e Pixel 7 | overflow, clique duplicado ou botão ativo durante request falha | screenshots, foco e estado de carregamento |

## Regra de rodada

Uma rodada completa sem defeito conclui a homologação. Se uma rodada revelar defeito e houver
correção, depois da última correção são exigidas duas rodadas completas e consecutivas sem falhas;
qualquer novo defeito reinicia a contagem. Toda topologia e dado são locais e segregados.
