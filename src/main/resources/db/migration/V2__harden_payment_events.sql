ALTER TABLE payment_events
    ALTER COLUMN created_at
    TYPE TIMESTAMP(6) WITH TIME ZONE
    USING created_at AT TIME ZONE 'UTC';

ALTER TABLE payment_events
    ALTER COLUMN payment_id SET NOT NULL;

ALTER TABLE payment_events
    ADD CONSTRAINT fk_payment_events_payment
    FOREIGN KEY (payment_id)
    REFERENCES payments(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_payment_events_payment_id_created_at
    ON payment_events (payment_id, created_at, id);

