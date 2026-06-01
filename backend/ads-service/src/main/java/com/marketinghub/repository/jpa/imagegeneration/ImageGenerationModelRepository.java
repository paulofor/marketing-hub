package com.marketinghub.repository.jpa.imagegeneration;

import com.marketinghub.imagegeneration.ImageGenerationModel;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de ImageGenerationModel.
 */
public interface ImageGenerationModelRepository extends JpaRepository<ImageGenerationModel, Long> {

    @Override
    @EntityGraph(attributePaths = {"qualities", "qualities.prices"})
    List<ImageGenerationModel> findAll(Sort sort);
}
