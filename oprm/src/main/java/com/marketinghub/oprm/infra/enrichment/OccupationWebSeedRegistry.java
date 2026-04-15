package com.marketinghub.oprm.infra.enrichment;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OccupationWebSeedRegistry {

    private static final Map<String, List<String>> SEEDS = Map.of(
            "personal trainer", List.of(
                    "https://en.wikipedia.org/wiki/Personal_trainer",
                    "https://www.bls.gov/ooh/personal-care-and-service/fitness-trainers-and-instructors.htm"
            ),
            "pastor", List.of(
                    "https://en.wikipedia.org/wiki/Pastor",
                    "https://www.indeed.com/career-advice/finding-a-job/how-to-become-a-pastor"
            ),
            "agricultor", List.of(
                    "https://en.wikipedia.org/wiki/Farmer",
                    "https://www.bls.gov/ooh/farming-fishing-and-forestry/farmers-ranchers-and-other-agricultural-managers.htm"
            ),
            "manicure", List.of(
                    "https://en.wikipedia.org/wiki/Manicure",
                    "https://www.coursera.org/articles/nail-technician"
            ),
            "cabeleireiro", List.of(
                    "https://en.wikipedia.org/wiki/Hairdresser",
                    "https://www.coursera.org/articles/hairdresser"
            ),
            "dono de loja de celulares", List.of(
                    "https://en.wikipedia.org/wiki/Mobile_phone_retailer",
                    "https://www.sebrae.com.br/sites/PortalSebrae/artigos/como-montar-uma-loja-de-celular"
            )
    );

    public List<String> seedsFor(String occupationName) {
        return SEEDS.getOrDefault(occupationName, List.of());
    }
}
