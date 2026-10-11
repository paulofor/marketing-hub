# Compatibilidade de Mira após capturas adicionais do kit

A validação de integridade do PR #5592 apontou o hash anterior de
PdeAgentValidationHarnessRunner na atestação v21. A alteração exige quinze capturas
somente quando runtimeKind declara DETERMINISTIC_PRIVATE_KIT_V1 e o relatório
expõe PDE_KIT_CAPTURE_MOMENTS_V1. Mira preserva seus dezoito percursos e artefatos;
sem o contrato novo, os caminhos históricos mantêm sua contagem.

Manter o hash antigo deixaria a prova incoerente; sobrescrever a v21 perderia a
história. A v22 registra a compatibilidade com a v21 intacta e
`automaticDeployOnMerge=false`, sem alteração da experiência ou revisão comercial.
Alcyone v13 foi conferida e não referencia a fonte alterada: nenhuma sucessora
foi necessária para esse produto.

A suíte completa de Psique passou com 182 testes (9 condicionais ignorados), incluindo
as fronteiras de Mira, Vega, Alcyone, identidade, segurança e evidências de cenários.
Os dois perfis de kit passaram em 18 percursos técnicos reais e quinze capturas por
perfil; o consumidor aceitou os relatórios e recusou ausência/dispositivo incorreto.
A mudança não executou tarefas ou chamadas de modelo em Mira.

O empacotador de evidências e o resolvedor são reexecutados localmente: a atestação
nova deve selecionar `none`. As limitações anteriores de produto, provas,
consentimento, conciliação e comercialização continuam válidas.
