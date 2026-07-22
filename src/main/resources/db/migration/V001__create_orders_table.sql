CREATE TABLE orders (
                        id              UUID PRIMARY KEY,
                        product_id      VARCHAR(64)  NOT NULL,
                        customer_id     VARCHAR(64)  NOT NULL,
                        idempotency_key VARCHAR(128) NOT NULL,
                        quantity        INTEGER      NOT NULL CHECK (quantity > 0),
                        status          VARCHAR(16)  NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'FAILED')),
                        created_at      TIMESTAMPTZ  NOT NULL,

                        CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key)
);

-- Every status lookup from the API (GetOrderStatusUseCase) hits this by PK
-- already (orders.id), so no extra index needed there.
CREATE INDEX idx_orders_product_id ON orders (product_id);
CREATE INDEX idx_orders_status ON orders (status);
