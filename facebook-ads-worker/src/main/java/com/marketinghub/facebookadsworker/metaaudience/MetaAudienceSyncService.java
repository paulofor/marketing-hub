package com.marketinghub.facebookadsworker.metaaudience;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Serviço responsável por criar audiências de email na Meta e reportar o resultado ao backend. */
@Service
public class MetaAudienceSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaAudienceSyncService.class);
    private static final int BATCH_SIZE = 10000;
    private final MetaAudienceBackendClient backendClient;
    private final FacebookAdsService facebookAdsService;

    /** Inicializa o serviço com cliente do backend e gateway da Graph API. */
    public MetaAudienceSyncService(MetaAudienceBackendClient backendClient, FacebookAdsService facebookAdsService) {
        this.backendClient = backendClient;
        this.facebookAdsService = facebookAdsService;
    }

    /** Processa audiências pendentes respeitando criação, hash SHA-256 e upload em lotes. */
    public void processPendingAudiences() {
        for (MetaAudienceBackendClient.PendingAudience audience : backendClient.listPending(5)) {
            processAudience(audience);
        }
    }

    /** Processa uma audiência isolada e mantém o backend como fonte de verdade do status. */
    private void processAudience(MetaAudienceBackendClient.PendingAudience audience) {
        try {
            List<String> hashes = normalizeAndHash(audience.emails());
            if (hashes.isEmpty()) {
                backendClient.reportSync(audience.id(), new MetaAudienceBackendClient.SyncResult(null, 0, "FAILED", "Audiência sem emails válidos para sincronização."));
                return;
            }
            String facebookAudienceId = facebookAdsService.createCustomerListAudience(audience.facebookAdAccountId(), audience.audienceName());
            for (int start = 0; start < hashes.size(); start += BATCH_SIZE) {
                int end = Math.min(start + BATCH_SIZE, hashes.size());
                facebookAdsService.addEmailHashesToCustomAudience(facebookAudienceId, hashes.subList(start, end));
            }
            backendClient.reportSync(audience.id(), new MetaAudienceBackendClient.SyncResult(facebookAudienceId, hashes.size(), "SYNCED", null));
            LOGGER.info("Meta audience synced successfully. audienceId={} facebookAudienceId={} syncedContacts={}", audience.id(), facebookAudienceId, hashes.size());
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao sincronizar audiência Meta. audienceId={} marketNicheId={} cnae={} adAccountId={}",
                    audience.id(), audience.marketNicheId(), audience.sourceCnaeCode(), audience.facebookAdAccountId(), ex);
            backendClient.reportSync(audience.id(), new MetaAudienceBackendClient.SyncResult(null, 0, "FAILED", ex.getMessage()));
        }
    }

    /** Normaliza emails, remove duplicados e gera SHA-256 hexadecimal conforme exigido pela Meta. */
    private List<String> normalizeAndHash(List<String> emails) {
        if (CollectionUtils.isEmpty(emails)) {
            return List.of();
        }
        LinkedHashSet<String> hashes = new LinkedHashSet<>();
        for (String email : emails) {
            if (StringUtils.hasText(email)) {
                hashes.add(sha256(email.trim().toLowerCase(Locale.ROOT)));
            }
        }
        return new ArrayList<>(hashes);
    }

    /** Calcula SHA-256 em hexadecimal para o valor informado. */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", ex);
        }
    }
}
