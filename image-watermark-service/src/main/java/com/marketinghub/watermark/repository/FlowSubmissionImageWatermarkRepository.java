package com.marketinghub.watermark.repository;

import com.marketinghub.watermark.entity.FlowSubmissionImageWatermarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowSubmissionImageWatermarkRepository
        extends JpaRepository<FlowSubmissionImageWatermarkEntity, Long> {}
