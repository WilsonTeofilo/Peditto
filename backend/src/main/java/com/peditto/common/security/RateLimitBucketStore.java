package com.peditto.common.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Atomically increments a PostgreSQL rate-limit bucket and reports active blocks. */
@Component
public class RateLimitBucketStore {
    private static final String UPSERT_BUCKET = """
            INSERT INTO api_rate_limit_buckets (
                id, created_at, updated_at, bucket_key, window_started_at,
                request_count, blocked_until, version
            )
            VALUES (gen_random_uuid(), now(), now(), ?, now(), 1, NULL, 0)
            ON CONFLICT (bucket_key) DO UPDATE SET
                request_count = CASE
                    WHEN api_rate_limit_buckets.window_started_at <= now() - (? * interval '1 second') THEN 1
                    ELSE api_rate_limit_buckets.request_count + 1
                END,
                window_started_at = CASE
                    WHEN api_rate_limit_buckets.window_started_at <= now() - (? * interval '1 second') THEN now()
                    ELSE api_rate_limit_buckets.window_started_at
                END,
                blocked_until = CASE
                    WHEN api_rate_limit_buckets.blocked_until > now() THEN api_rate_limit_buckets.blocked_until
                    WHEN (CASE
                        WHEN api_rate_limit_buckets.window_started_at <= now() - (? * interval '1 second') THEN 1
                        ELSE api_rate_limit_buckets.request_count + 1
                    END) > ? THEN now() + (? * interval '1 minute')
                    ELSE NULL
                END,
                updated_at = now()
            RETURNING (blocked_until > now())
            """;

    private final JdbcTemplate jdbc;

    public RateLimitBucketStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isBlocked(String bucketKey, int windowSeconds, int maxRequests, int blockMinutes) {
        Boolean blocked = jdbc.queryForObject(UPSERT_BUCKET, Boolean.class, bucketKey,
                windowSeconds, windowSeconds, windowSeconds, maxRequests, blockMinutes);
        return Boolean.TRUE.equals(blocked);
    }
}
