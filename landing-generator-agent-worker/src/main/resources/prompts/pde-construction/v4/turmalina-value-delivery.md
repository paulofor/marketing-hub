# Turmalina — preservar a proposta até a entrega (v1)

Aplicável somente quando taskTarget.pdeContext.productValueDelivery.baseline.applicable=true.
A base fixada contém a Big Idea própria do produto e a base Turmalina já cadastrada. Não substitua
consultoria contextual, experiência visual ou memória por um chat, formulário ou roteiro fixo só
porque essa implementação já existe. Regras locais podem comprovar um recorte, não a capacidade
completa de um consultor de IA. Não renove preço, autorização ou direitos históricos.

Na atividade journey, entregue valueSpecification no schema:
- copie baselineSha256 do contexto; contractVersion=TURMALINA_VALUE_DELIVERY_V1;
- converta todas as requirements em histórias de uso concretas, com sourceRefs literais;
- explique o resultado para a cliente e desenhe textualmente os estados entry/result/resume/empty/error,
  identificando conteúdo, hierarquia e ações. Os estados orientam a construção, não são screenshots;
- declare BUILD, REUSE com evidência compatível, ou DEFER com motivo e impacto sobre a promessa;
- cada critério given/when/then precisa de testMethod executável identificável, incluindo caminho
  feliz, erro, retomada, isolamento e dispositivos pertinentes. Não chame testes inexistentes de PASS;
- se faltar capacidade prometida, scope=PROTOTYPE_SLICE. PRODUCT exige toda a proposta, sem adiamento.
  A cobertura semântica deve ser conferida pelos revisores; repetir ids não comprova fidelidade.

Nas atividades deliverables/access, use as mesmas histórias e registre o valor literal de
specificationReference em acceptanceCriteria. Detalhe incrementos pequenos de construção, seu
resultado, integração, provas, reutilização e pendências. Não crie sprints burocráticas nem um
aplicativo paralelo. A implementação deve usar os SDKs cadastrados quando aplicáveis.
READY conclui somente esta especificação, nunca software, homologação ou publicação. A construção
precisa produzir código executável e recibos de teste no fluxo versionado do repositório. Se faltar
capacidade executora, descreva o impedimento e a entrega faltante; não delegue desenvolvimento à cliente.

O backend exige prova registrada no catálogo operacional existente, vinculada ao hash da
especificação, produto, referência e versão. Psique revisa telas e jornadas reais; Têmis confronta
promessa, cobertura e limites independentemente. Não fabrique seus pareceres nem evidência de mercado.
