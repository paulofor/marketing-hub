# Compatibilidade de Mira após correção do pacote privado — 08/10/2026

As fontes compartilhadas de ciclo, homologação e captura receberam a correção
do contrato privado de kits. A atestação `mira-private-candidate.v11` preserva
a v10 como história e registra somente a compatibilidade das fontes alteradas.
Não altera a experiência `mira-private-candidate-v3`, pixels, preço, ficha,
experimento, parecer, autorização ou publicação comercial de Mira.

As novas exigências de pacote e matriz aplicam-se somente aos contratos
`DETERMINISTIC_PRIVATE_KIT_V1` e `PDE_PRIVATE_KIT_FIXTURES_V1`. Cenários de Mira
permanecem nos seus contratos documentais, inclusive sanitização e IDs de
captura. Prova de outro ciclo/versão é recusada; o registro não reabre uma
candidata vigente. A revisão de integridade com evidência operacional nova
continua reutilizando a técnica e Psique quando compatíveis.

Evidência executável: suíte completa do backend sem falhas/erros, regressões
de `LearningCyclePrototypeRegistrationTest`, `LearningCyclePrototypeContextTest`,
`PdeAgentValidationReworkReadinessProviderTest`, `PdeAgentValidationGateActivityExecutorTest`,
`PdeTechnicalHomologationReadinessProviderTest` e suíte de Psique. O empacotador
canônico verifica os hashes atuais e preserva as fontes de produto e manifests
históricos, sem enfraquecer a conferência de evidência.

A revalidação não chama modelos nem homologa Mira comercialmente de novo.
Mantém as limitações existentes: emulação Chromium não comprova Safari nativo;
testes e versões compatíveis não comprovam utilidade humana, compra ou margem.
