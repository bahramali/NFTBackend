-- Index covering DISTINCT and ORDER BY columns on actuator_status
CREATE INDEX idx_actuator_status_composite_type_time
ON actuator_status (composite_id, actuator_type, status_time DESC);
