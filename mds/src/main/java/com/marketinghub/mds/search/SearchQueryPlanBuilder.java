package com.marketinghub.mds.search;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchQueryPlanBuilder {

    public List<SearchQueryPlan> buildPlans(MechanismQuestion question) {
        String q = question.text();
        return List.of(
                new SearchQueryPlan("pubmed", q, 10),
                new SearchQueryPlan("crossref", q, 10)
        );
    }
}
