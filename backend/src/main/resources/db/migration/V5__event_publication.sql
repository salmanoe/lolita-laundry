-- ============================================================
-- Phase 3 — Spring Modulith event publication registry.
-- Backs spring-modulith-starter-jpa: every published domain event
-- (e.g. order → billing OrderDeliveredEvent) is persisted here until
-- its listener completes, so an event survives a crash between
-- publish and completion and is retried on restart.
--
-- This is the canonical Spring Modulith 2.x PostgreSQL schema. In
-- tests Flyway is disabled and Hibernate ddl-auto creates this table
-- from Modulith's JPA entity instead.
-- Managed by Flyway. Never modify this file after deployment.
-- ============================================================

CREATE TABLE event_publication (
    id                     UUID                     NOT NULL,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    -- Resubmission/retry bookkeeping added by Spring Modulith 2.x's JpaEventPublication
    -- entity. Hibernate ddl-auto: validate (dev/prod) requires every mapped column to
    -- exist, so all three must be present here.
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INTEGER                  NOT NULL DEFAULT 0,
    status                 TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx
    ON event_publication USING hash (serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
    ON event_publication (completion_date);
