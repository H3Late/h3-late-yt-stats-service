package com.h3late.stats.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Supports "my history" style lookups across all of an account's clips/votes/reports by
 * user_id (the collapsed identity column — see AccountIdentity). Index creation on
 * already-existing tables is unreliable under ddl-auto=update in this project (no migration tool
 * exists here) — same reasoning as ContestSchedulerService.ensureSingleActiveContestConstraint().
 */
@Service
@RequiredArgsConstructor
public class AccountSchemaService {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureUserIdIndexes() {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS ix_contest_clip_user_id ON contest_clip (user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS ix_clip_vote_user_id ON clip_vote (user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS ix_clip_report_user_id ON clip_report (user_id)");
    }
}
