CREATE TABLE IF NOT EXISTS monitoring_page (
    id BIGSERIAL PRIMARY KEY,
    rack_id VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    slug VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_monitoring_page_rack_id UNIQUE (rack_id),
    CONSTRAINT ux_monitoring_page_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS ix_monitoring_page_enabled_sort
    ON monitoring_page (enabled, sort_order);
