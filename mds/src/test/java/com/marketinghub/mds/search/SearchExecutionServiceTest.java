package com.marketinghub.mds.search;

import com.marketinghub.mds.config.MdsProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchExecutionServiceTest {

    @Test
    void shouldRetryAndSucceedWhenSourceFailsOnce() {
        AtomicInteger attempts = new AtomicInteger();
        EvidenceSearchClient flakyClient = new EvidenceSearchClient() {
            @Override
            public String source() {
                return "pubmed";
            }

            @Override
            public List<SourceSearchHit> search(String query, int limit) {
                if (attempts.getAndIncrement() == 0) {
                    throw new RuntimeException("temporary timeout");
                }
                return List.of(new SourceSearchHit(
                        "pubmed",
                        "pubmed:1",
                        null,
                        "title",
                        null,
                        "https://pubmed.ncbi.nlm.nih.gov/1/",
                        "2024",
                        null,
                        true,
                        true,
                        true,
                        List.of()
                ));
            }
        };

        SearchExecutionService service = new SearchExecutionService(List.of(flakyClient), properties());

        List<SourceSearchHit> hits = service.execute(List.of(new SearchQueryPlan("pubmed", "query", 5)));

        assertThat(hits).hasSize(1);
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void shouldFailPredictablyWhenAllSourcesFail() {
        EvidenceSearchClient alwaysFailingPubmed = failingClient("pubmed");
        EvidenceSearchClient alwaysFailingCrossref = failingClient("crossref");

        SearchExecutionService service = new SearchExecutionService(
                List.of(alwaysFailingPubmed, alwaysFailingCrossref),
                properties()
        );

        assertThatThrownBy(() -> service.execute(List.of(
                new SearchQueryPlan("pubmed", "q1", 3),
                new SearchQueryPlan("crossref", "q2", 3)
        )))
                .isInstanceOf(RecoverableSourceException.class)
                .hasMessageContaining("all configured sources failed");
    }

    private EvidenceSearchClient failingClient(String source) {
        return new EvidenceSearchClient() {
            @Override
            public String source() {
                return source;
            }

            @Override
            public List<SourceSearchHit> search(String query, int limit) {
                throw new RuntimeException("network down");
            }
        };
    }

    private MdsProperties properties() {
        MdsProperties props = new MdsProperties();
        props.getSearch().setRetryMaxAttempts(2);
        props.getSearch().setRetryBackoffMs(0);
        return props;
    }
}
