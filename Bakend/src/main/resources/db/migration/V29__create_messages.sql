-- V29 : Messages d'une conversation

CREATE TABLE messages (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id UUID        NOT NULL,
    sender_id       UUID        NOT NULL,
    content         TEXT        NOT NULL,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_messages_sender       FOREIGN KEY (sender_id)       REFERENCES users(id)
);

CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
