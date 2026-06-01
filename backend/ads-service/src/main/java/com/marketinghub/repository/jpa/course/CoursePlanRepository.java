package com.marketinghub.repository.jpa.course;

import com.marketinghub.course.CoursePlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link CoursePlan} entities.
 */
public interface CoursePlanRepository extends JpaRepository<CoursePlan, Long> {
}
