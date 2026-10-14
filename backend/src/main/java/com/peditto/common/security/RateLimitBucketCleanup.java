package com.peditto.common.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimitBucketCleanup {
    private final JdbcTemplate jdbc;

    public RateLimitBucketCleanup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${peditto.security.rate-limit.cleanup-delay-ms:3600000}", initialDelayString = "${peditto.security.rate-limit.cleanup-delay-ms:3600000}")
    public void deleteExpiredBuckets() {
        jdbc.update("DELETE FROM api_rate_limit_buckets WHERE updated_at < now() - interval '1 day'");
    }
}
