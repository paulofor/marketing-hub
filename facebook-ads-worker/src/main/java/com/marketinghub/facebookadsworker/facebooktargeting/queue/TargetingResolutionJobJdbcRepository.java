package com.marketinghub.facebookadsworker.facebooktargeting.queue;

import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class TargetingResolutionJobJdbcRepository implements TargetingResolutionJobRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetingResolutionJobJdbcRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TargetingResolutionJobJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public List<TargetingResolutionJobRecord> claimPendingJobs(String workerId, int batchSize) {
        List<Long> candidates = jdbcTemplate.queryForList(
            "SELECT id FROM targeting_resolution_job WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit",
            Map.of("limit", Math.max(batchSize, 1)),
            Long.class
        );
        if (CollectionUtils.isEmpty(candidates)) {
            return List.of();
        }
        List<Long> claimed = new ArrayList<>();
        Instant now = Instant.now();
        for (Long jobId : candidates) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("jobId", jobId)
                    .addValue("workerId", workerId)
                    .addValue("startedAt", Timestamp.from(now));
            int updated = jdbcTemplate.update(
                """
                UPDATE targeting_resolution_job
                   SET status = 'PROCESSING',
                       locked_by = :workerId,
                       locked_at = :startedAt,
                       started_at = :startedAt,
                       attempt_count = attempt_count + 1,
                       last_error = NULL
                 WHERE id = :jobId AND status = 'PENDING'
                """,
                params
            );
            if (updated == 1) {
                claimed.add(jobId);
            }
        }
        if (claimed.isEmpty()) {
            return List.of();
        }
        return fetchJobRecords(claimed);
    }

    @Override
    @Transactional
    public void markCompleted(long jobId, int resolvedOptionsCount) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobId", jobId)
                .addValue("finishedAt", Timestamp.from(Instant.now()))
                .addValue("resolvedCount", resolvedOptionsCount);
        jdbcTemplate.update(
            """
            UPDATE targeting_resolution_job
               SET status = 'SUCCEEDED',
                   result_count = :resolvedCount,
                   finished_at = :finishedAt,
                   locked_by = NULL,
                   locked_at = NULL
             WHERE id = :jobId
            """,
            params
        );
    }

    @Override
    @Transactional
    public void markFailed(long jobId, String errorMessage) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobId", jobId)
                .addValue("finishedAt", Timestamp.from(Instant.now()))
                .addValue("error", errorMessage);
        jdbcTemplate.update(
            """
            UPDATE targeting_resolution_job
               SET status = 'FAILED',
                   last_error = :error,
                   finished_at = :finishedAt,
                   locked_by = NULL,
                   locked_at = NULL
             WHERE id = :jobId
            """,
            params
        );
    }

    @Override
    @Transactional
    public int releaseExpiredLocks(Duration lockTtl) {
        if (lockTtl == null || lockTtl.isNegative() || lockTtl.isZero()) {
            return 0;
        }
        Instant threshold = Instant.now().minus(lockTtl);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("threshold", Timestamp.from(threshold));
        return jdbcTemplate.update(
            """
            UPDATE targeting_resolution_job
               SET status = 'PENDING',
                   locked_by = NULL,
                   locked_at = NULL,
                   started_at = NULL
             WHERE status = 'PROCESSING'
               AND locked_at IS NOT NULL
               AND locked_at < :threshold
            """,
            params
        );
    }

    private List<TargetingResolutionJobRecord> fetchJobRecords(List<Long> jobIds) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("jobIds", jobIds);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(
            """
            SELECT j.id             AS job_id,
                   j.request_id     AS request_id,
                   r.locale         AS request_locale,
                   r.country        AS request_country,
                   c.id             AS candidate_id,
                   c.texto_sugerido AS seed,
                   c.type           AS candidate_type,
                   c.idioma         AS locale_hint,
                   c.idioma         AS locale,
                   c.country        AS country,
                   c.origem         AS origin,
                   c.intent_tag     AS intent_tag,
                   c.score          AS score,
                   c.rationale      AS rationale
              FROM targeting_resolution_job j
              JOIN targeting_candidate c ON c.id = j.candidate_id
              JOIN targeting_request r ON r.id = j.request_id
             WHERE j.id IN (:jobIds)
            """,
            params
        );
        Map<Long, TargetingResolutionJobRecord> jobs = new LinkedHashMap<>();
        while (rowSet.next()) {
            long jobId = rowSet.getLong("job_id");
            UUID requestId = toUuid((byte[]) rowSet.getObject("request_id"));
            TargetingCandidateType type = TargetingCandidateType.valueOf(rowSet.getString("candidate_type"));
            BigDecimal score = rowSet.getBigDecimal("score");
            TargetingResolutionJobRecord record = new TargetingResolutionJobRecord(
                jobId,
                requestId,
                rowSet.getString("request_locale"),
                rowSet.getString("request_country"),
                rowSet.getLong("candidate_id"),
                rowSet.getString("seed"),
                new ArrayList<>(),
                type,
                rowSet.getString("locale_hint"),
                rowSet.getString("locale"),
                rowSet.getString("country"),
                rowSet.getString("origin"),
                score,
                rowSet.getString("rationale"),
                rowSet.getString("intent_tag")
            );
            jobs.put(jobId, record);
        }
        if (jobs.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> variants = fetchVariants(
            jobs.values().stream().map(TargetingResolutionJobRecord::candidateId).collect(Collectors.toList())
        );
        return jobs.values().stream()
                .map(record -> new TargetingResolutionJobRecord(
                        record.jobId(),
                        record.requestId(),
                        record.requestLocale(),
                        record.requestCountry(),
                        record.candidateId(),
                        record.seed(),
                        variants.getOrDefault(record.candidateId(), List.of()),
                        record.type(),
                        record.localeHint(),
                        record.locale(),
                        record.country(),
                        record.origin(),
                        record.score(),
                        record.rationale(),
                        record.intentTag()
                ))
                .toList();
    }

    private Map<Long, List<String>> fetchVariants(List<Long> candidateIds) {
        if (CollectionUtils.isEmpty(candidateIds)) {
            return Map.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("candidateIds", candidateIds);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(
            """
            SELECT candidate_id, variant_value
              FROM targeting_candidate_seed_variant
             WHERE candidate_id IN (:candidateIds)
             ORDER BY candidate_id, variant_order
            """,
            params
        );
        Map<Long, List<String>> variants = new HashMap<>();
        while (rowSet.next()) {
            long candidateId = rowSet.getLong("candidate_id");
            variants.computeIfAbsent(candidateId, id -> new ArrayList<>())
                    .add(rowSet.getString("variant_value"));
        }
        return variants;
    }

    private UUID toUuid(byte[] value) {
        if (value == null || value.length != 16) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        long high = buffer.getLong();
        long low = buffer.getLong();
        return new UUID(high, low);
    }
}
