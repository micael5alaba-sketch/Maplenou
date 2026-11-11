-- Idempotence des demandes de retrait partenaire : une meme cle client (double clic, nouvel envoi apres
-- coupure reseau) ne cree jamais deux retraits ni deux reservations.
ALTER TABLE reseller_withdrawals ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uq_reseller_withdrawals_idem ON reseller_withdrawals(partner_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
