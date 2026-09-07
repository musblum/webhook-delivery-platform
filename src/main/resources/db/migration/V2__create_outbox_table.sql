CREATE TABLE outbox_messages (
     id UUID PRIMARY KEY,
     delivery_id UUID NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     published_at TIMESTAMPTZ,

     CONSTRAINT fk_outbox_delivery
         FOREIGN KEY (delivery_id)
             REFERENCES deliveries(id)
);