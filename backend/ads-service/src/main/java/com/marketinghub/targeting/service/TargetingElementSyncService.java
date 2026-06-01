package com.marketinghub.targeting.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TargetingElementSyncService {
    private final TargetingElementRepository repository;

    public TargetingElementSyncService(TargetingElementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void syncManualLists(MarketNiche niche) {
        if (niche == null || niche.getId() == null) {
            return;
        }
        List<TargetingElement> existing = repository.findByNicheId(niche.getId());
        Map<String, TargetingElement> manualByKey = new HashMap<>();
        for (TargetingElement element : existing) {
            if (element.getHypothesis() == null
                    && element.getSource() == TargetingElementSource.MANUAL
                    && isManualType(element.getType())
                    && StringUtils.hasText(element.getTerm())) {
                manualByKey.put(key(element.getType(), element.getTerm()), element);
            }
        }

        Set<String> expectedKeys = new LinkedHashSet<>();
        List<TargetingElement> toPersist = new ArrayList<>();
        addOrUpdate(niche, TargetingElementType.INTEREST, niche.getInterestList(), manualByKey, expectedKeys, toPersist);
        addOrUpdate(niche, TargetingElementType.JOB_TITLE, niche.getRoleList(), manualByKey, expectedKeys, toPersist);
        addOrUpdate(niche, TargetingElementType.BEHAVIOR, niche.getBehaviorList(), manualByKey, expectedKeys, toPersist);

        List<TargetingElement> toDelete = manualByKey.entrySet().stream()
                .filter(entry -> !expectedKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!toPersist.isEmpty()) {
            repository.saveAll(toPersist);
        }
        if (!toDelete.isEmpty()) {
            repository.deleteAll(toDelete);
        }
    }

    private void addOrUpdate(MarketNiche niche,
                             TargetingElementType type,
                             List<String> terms,
                             Map<String, TargetingElement> manualByKey,
                             Set<String> expectedKeys,
                             List<TargetingElement> toPersist) {
        for (String term : normalizeTerms(terms)) {
            String key = key(type, term);
            expectedKeys.add(key);
            TargetingElement existing = manualByKey.get(key);
            if (existing != null) {
                if (!term.equals(existing.getTerm())) {
                    existing.setTerm(term);
                    toPersist.add(existing);
                }
                continue;
            }
            TargetingElement element = TargetingElement.builder()
                    .niche(niche)
                    .type(type)
                    .term(term)
                    .source(TargetingElementSource.MANUAL)
                    .status(TargetingElementStatus.APPROVED)
                    .build();
            toPersist.add(element);
        }
    }

    private boolean isManualType(TargetingElementType type) {
        return type == TargetingElementType.INTEREST
                || type == TargetingElementType.JOB_TITLE
                || type == TargetingElementType.BEHAVIOR;
    }

    private String key(TargetingElementType type, String term) {
        return type.name() + "::" + term.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> normalizeTerms(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            normalized.add(value.trim());
        }
        return List.copyOf(normalized);
    }
}
