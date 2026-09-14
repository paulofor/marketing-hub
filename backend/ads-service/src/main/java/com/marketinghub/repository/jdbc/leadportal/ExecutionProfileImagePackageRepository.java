package com.marketinghub.repository.jdbc.leadportal;

import com.marketinghub.leadportal.service.executionprofile.ProfilePackageSource;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Responsabilidade: consultar a correlação persistida entre pacote, experimento e produto. */
@Repository
@RequiredArgsConstructor
public class ExecutionProfileImagePackageRepository {
  private final JdbcTemplate jdbc;

  /** Expõe todas as origens candidatas para o serviço bloquear correlação ambígua. */
  public List<ProfilePackageSource> findSources(long packageId) {
    return jdbc.query(
        """
        SELECT DISTINCT exp.id AS experiment_id, exp.product_id,
          COALESCE(pack.planned_outputs, exp.images_per_package, 20) AS outputs,
          sub.stored_file_name, pack.prompt
        FROM flow_submission_image_package pack
        JOIN flow_submissions sub ON sub.id = pack.submission_id
        JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
        JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
        WHERE pack.id = ? AND exp.product_id IS NOT NULL
        """,
        (rs, n) ->
            new ProfilePackageSource(
                rs.getLong("product_id"),
                "experiment:" + rs.getLong("experiment_id"),
                rs.getInt("outputs"),
                rs.getString("stored_file_name") != null
                    && !rs.getString("stored_file_name").isBlank(),
                Objects.toString(rs.getString("prompt"), "")
                    + "|"
                    + Objects.toString(rs.getString("stored_file_name"), "")),
        packageId);
  }
}
