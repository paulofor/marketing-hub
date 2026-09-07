# Fonte revisada — Consentimento elevado pode esconder desalinhamento com a preferência real

## Evidência revisada

Um estudo de 2026 em *Computers in Human Behavior Reports* auditou 624 sites de apostas licenciados no Reino Unido e depois realizou um experimento online randomizado com 615 participantes em uma plataforma simulada.

Na auditoria, dark patterns eram frequentes nos banners de consentimento. No experimento, um formato comum de banner aumentou significativamente a aceitação de rastreamento, mas reduziu a correspondência entre a decisão registrada e a preferência que o usuário havia declarado.

O achado comportamental usado neste card é que uma taxa maior de consentimento não demonstra, por si só, maior preferência ou vontade real. Saliência, assimetria e fricção podem deslocar a escolha sem alterar a preferência subjacente.

## Hipótese interpretativa

O Marketing Hub pode evitar otimizações locais enganosas se avaliar não apenas `consent_rate`, mas também simetria da escolha e, quando viável em pesquisa, alinhamento entre a ação registrada e a preferência declarada.

## Aplicação possível no Marketing Hub

Criar `ConsentSymmetryScore` e `PreferenceAlignmentAudit`. Aceitar e rejeitar devem ter clareza e saliência adequadas, e testes devem comparar alternativas igualmente éticas e compatíveis, por exemplo uma versão compacta contra uma versão `purpose-first` que explique de forma curta por que o dado é solicitado.

## Limites

O estudo foi realizado no contexto britânico de apostas e GDPR. Ele não determina requisitos jurídicos aplicáveis no Brasil, que precisam ser avaliados sob LGPD e demais regras pertinentes. Também não prova impacto comercial positivo de interfaces simétricas; mostra um risco metodológico e comportamental de interpretar consentimento como preferência.

## Fonte original

https://cronfa.swansea.ac.uk/Record/cronfa72625
