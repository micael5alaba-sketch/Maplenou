-- Indexes pour accélérer les requêtes KPI admin
CREATE INDEX IF NOT EXISTS idx_orders_status_created  ON orders(status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_created_at      ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_created_at       ON users(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sub_orders_status      ON sub_orders(status);
