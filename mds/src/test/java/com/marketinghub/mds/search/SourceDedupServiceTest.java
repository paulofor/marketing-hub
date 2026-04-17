package com.marketinghub.mds.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceDedupServiceTest {
    private final SourceDedupService service = new SourceDedupService();

    @Test
    void shouldDeduplicateUsingDoiAndKeepMostCompleteDocument() {
        SourceSearchHit first = new SourceSearchHit(
                "pubmed",
                "pubmed:1",
                "10.1000/xyz",
                "Title",
                "",
                "",
                "2024",
                "",
                false,
                false,
                false,
                List.of()
        );
        SourceSearchHit second = new SourceSearchHit(
                "crossref",
                "crossref:9",
                "https://doi.org/10.1000/xyz",
                "Title",
                "Detailed abstract",
                "https://example.org/paper",
                "2024",
                "CC-BY",
                true,
                true,
                true,
                List.of("Author")
        );

        List<SourceSearchHit> deduped = service.deduplicate(List.of(first, second));

        assertThat(deduped).hasSize(1);
        assertThat(deduped.get(0).source()).isEqualTo("crossref");
        assertThat(deduped.get(0).abstractText()).isEqualTo("Detailed abstract");
    }
}
