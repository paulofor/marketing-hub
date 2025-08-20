package com.marketinghub.worker;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;

import java.util.List;

public interface HypothesisChatGptClient {
    List<Hypothesis> generate(MarketNiche niche, int quantity);
}
