package com.marketinghub.hypothesis.service;

import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class HypothesisKanbanFacade {
    private final HypothesisRepository repository;
    private final HypothesisMapper mapper;

    public HypothesisKanbanFacade(HypothesisRepository repository, HypothesisMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Map<HypothesisStatus, List<HypothesisDto>> board(Long marketNicheId) {
        Iterable<com.marketinghub.hypothesis.Hypothesis> it = repository.findByMarketNicheId(marketNicheId);
        return StreamSupport.stream(it.spliterator(), false)
                .map(mapper::toDto)
                .collect(Collectors.groupingBy(HypothesisDto::getStatus,
                        () -> new EnumMap<>(HypothesisStatus.class), Collectors.toList()));
    }
}
