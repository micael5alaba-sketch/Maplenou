"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { ConversationResponse, MessageResponse, Page } from "@/lib/types";
import { Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Pagination, formatDate } from "@/components/ui";

export default function SupportConversationsPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<ConversationResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeId, setActiveId] = useState<string | null>(null);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<ConversationResponse>>("/api/admin/conversations/support", { page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [page]);

  return (
    <div>
      <PageHeader title="Conversations support" subtitle="Échanges entre les utilisateurs et l'équipe Maplenou" />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <div className="flex gap-6">
          <Card className="w-80 flex-shrink-0 !p-0">
            {data.content.length === 0 ? (
              <EmptyState message="Aucune conversation support." />
            ) : (
              <div className="divide-y divide-border">
                {data.content.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => setActiveId(c.id)}
                    className={`block w-full px-4 py-3 text-left transition hover:bg-input-fill ${
                      activeId === c.id ? "bg-input-fill" : ""
                    }`}
                  >
                    <p className="text-sm font-semibold text-foreground">{c.buyerName ?? "Utilisateur"}</p>
                    <p className="text-xs text-muted">{formatDate(c.lastMessageAt ?? c.createdAt)}</p>
                  </button>
                ))}
              </div>
            )}
            <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
          </Card>

          <div className="flex-1">
            {activeId ? (
              <Thread conversationId={activeId} />
            ) : (
              <Card>
                <EmptyState message="Sélectionne une conversation à gauche." />
              </Card>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Thread({ conversationId }: { conversationId: string }) {
  const [messages, setMessages] = useState<MessageResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [content, setContent] = useState("");
  const [sending, setSending] = useState(false);

  function load() {
    setError(null);
    apiClient
      .get<Page<MessageResponse>>(`/api/conversations/${conversationId}/messages`, { size: 50 })
      .then((page) => setMessages([...page.content].reverse()))
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."));
  }

  useEffect(load, [conversationId]);

  async function send(e: FormEvent) {
    e.preventDefault();
    if (!content.trim()) return;
    setSending(true);
    try {
      await apiClient.post(`/api/conversations/${conversationId}/messages`, { content });
      setContent("");
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur lors de l'envoi.");
    } finally {
      setSending(false);
    }
  }

  return (
    <Card className="flex h-[70vh] flex-col">
      {error && <ErrorState message={error} />}
      <div className="flex-1 space-y-3 overflow-y-auto">
        {messages === null && <LoadingState />}
        {messages?.length === 0 && <EmptyState message="Aucun message." />}
        {messages?.map((m) => (
          <div key={m.id} className="max-w-[80%] rounded-xl bg-input-fill px-3 py-2">
            <p className="text-xs font-semibold text-muted">{m.senderName}</p>
            <p className="text-sm text-foreground">{m.content}</p>
            <p className="mt-1 text-[11px] text-muted">{formatDate(m.createdAt)}</p>
          </div>
        ))}
      </div>
      <form onSubmit={send} className="mt-4 flex gap-2 border-t border-border pt-4">
        <input
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Répondre..."
          className="flex-1 rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
        />
        <Button type="submit" disabled={sending}>
          Envoyer
        </Button>
      </form>
    </Card>
  );
}
