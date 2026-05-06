package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartProductSnapshot;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotmartCollectorService {

    public HotmartCollectionResponse collect(HotmartCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);
        List<HotmartProductSnapshot> samples = List.of(
                new HotmartProductSnapshot("Produto quente (placeholder)", "4.7", "R$ 350,00", "https://app.hotmart.com/market/search", Instant.now())
        );

        return new HotmartCollectionResponse(
                "READY_FOR_AUTOMATION",
                "Submódulo separado criado. Próximo passo: integrar Playwright com sessão persistida para coletar até " + boundedMax + " produtos.",
                samples
        );
    }
}
