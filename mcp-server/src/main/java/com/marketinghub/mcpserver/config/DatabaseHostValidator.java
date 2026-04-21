package com.marketinghub.mcpserver.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHostValidator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHostValidator.class);
    static final String INVALID_HOST = "interface.vps-kinghost.net";
    static final String EXPECTED_HOST = "d555d.vps-kinghost.net";

    private final String datasourceUrl;

    public DatabaseHostValidator(@Value("${spring.datasource.url:}") String datasourceUrl) {
        this.datasourceUrl = datasourceUrl;
    }

    @PostConstruct
    void validate() {
        if (usesInvalidHost(datasourceUrl)) {
            throw new IllegalStateException(
                    "spring.datasource.url está apontando para host inválido '"
                            + INVALID_HOST
                            + "'. Use '"
                            + EXPECTED_HOST
                            + "'."
            );
        }

        if (datasourceUrl != null && datasourceUrl.contains(EXPECTED_HOST)) {
            log.info("Datasource configurado com host correto: {}", EXPECTED_HOST);
        }
    }

    static boolean usesInvalidHost(String url) {
        return url != null && url.contains(INVALID_HOST);
    }
}
