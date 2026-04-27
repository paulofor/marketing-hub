package com.marketinghub.mois.service;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "integrations.mois.hotmart-collection")
public class MoisHotmartCollectionProperties {

    private boolean enabled;
    private String workspaceId = "workspace-001";
    private String niche = "marketing-digital";
    private String marketTheme = "ofertas-com-temperatura-alta";
    private List<String> sources = new ArrayList<>(List.of("HOTMART"));
    private String timeWindow = "LAST_7_DAYS";
    private int limitPerSource = 25;
    private String locale = "pt-BR";
    private String country = "BR";
    private int minSuccessScore = 80;
}
