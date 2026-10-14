-- Identifiant de transaction FedaPay associé à la commande, pour retrouver la bonne commande
-- quand le webhook FedaPay arrive (il ne renvoie que l'id de sa transaction, pas notre UUID).
ALTER TABLE orders ADD COLUMN fedapay_transaction_id BIGINT;

CREATE INDEX idx_orders_fedapay_transaction_id ON orders(fedapay_transaction_id);
