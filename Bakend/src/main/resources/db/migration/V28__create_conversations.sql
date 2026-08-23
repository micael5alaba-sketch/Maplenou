-- V28 : Messagerie — conversations vendeur↔client et utilisateur↔support (admin)

CREATE TABLE conversations (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    type            VARCHAR(20) NOT NULL,                 -- BUYER_SELLER ou SUPPORT
    shop_id         UUID,                                 -- NULL pour SUPPORT
    buyer_id        UUID        NOT NULL,                 -- client (BUYER_SELLER) ou demandeur (SUPPORT)
    seller_id       UUID,                                 -- propriétaire de la boutique, NULL pour SUPPORT
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_conversations_shop   FOREIGN KEY (shop_id)   REFERENCES shops(id),
    CONSTRAINT fk_conversations_buyer  FOREIGN KEY (buyer_id)  REFERENCES users(id),
    CONSTRAINT fk_conversations_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Un seul fil par (boutique, client) et un seul fil support par utilisateur
CREATE UNIQUE INDEX uk_conversations_buyer_seller ON conversations(shop_id, buyer_id) WHERE type = 'BUYER_SELLER';
CREATE UNIQUE INDEX uk_conversations_support ON conversations(buyer_id) WHERE type = 'SUPPORT';

CREATE INDEX idx_conversations_seller_id ON conversations(seller_id);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_at DESC);
