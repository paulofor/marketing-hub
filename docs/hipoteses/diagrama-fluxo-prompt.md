
flowchart TB

    subgraph Fluxo_Principal
        direction LR
        I["DOR"] --> R["RESULTADO"] --> M["MECANISMO"] --> P["PRODUTO"] --> O["OFERTA"]
    end

    subgraph Fatores_Transversais
        direction LR
        E["ENVELOPE DO PRODUTO"]
        G["GLOBAIS"]
    end

    subgraph Resumos
        direction LR
        RI["Resumo de Dor"]
        RR["Resumo de Resultado"]
        RM["Resumo de Mecanismo"]
        RP["Resumo de Produto"]
        RO["Resumo de Oferta"]
    end

    E --> I
    E --> R
    E --> M
    E --> P
    E --> O

    G --> R
    G --> M
    G --> P
    G --> O

    I --> RI
    R --> RR
    M --> RM
    P --> RP
    O --> RO
