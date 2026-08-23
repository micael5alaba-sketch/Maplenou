-- V31 : Champs transporteur tiers (bordereau d'expédition, tracking) sur les sous-commandes

ALTER TABLE sub_orders ADD COLUMN carrier_code VARCHAR(30);
ALTER TABLE sub_orders ADD COLUMN tracking_number VARCHAR(100);
ALTER TABLE sub_orders ADD COLUMN carrier_label_url VARCHAR(500);

CREATE INDEX idx_sub_orders_tracking_number ON sub_orders(tracking_number);
