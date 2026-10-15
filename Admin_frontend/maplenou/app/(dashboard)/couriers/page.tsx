"use client";

import { useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { UserResponse } from "@/lib/types";
import { Button, Card, ErrorState, PageHeader } from "@/components/ui";

export default function CreateCourierPage() {
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<UserResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await apiClient.post<UserResponse>("/api/admin/delivery-agents", {
        fullName,
        phoneNumber,
        password,
      });
      setCreated(user);
      setFullName("");
      setPhoneNumber("");
      setPassword("");
    } catch (err) {
      setError(err instanceof ApiException ? err.message : "Erreur lors de la création.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Créer un livreur"
        subtitle="Compte livreur interne à l'entreprise — communique le mot de passe toi-même au livreur."
      />

      <Card className="max-w-lg">
        {error && (
          <div className="mb-4">
            <ErrorState message={error} />
          </div>
        )}
        {created && (
          <div className="mb-4 rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
            Compte créé pour <strong>{created.fullName}</strong> ({created.phoneNumber}). Il devra soumettre
            ses 4 documents avant de pouvoir recevoir une livraison.
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-foreground">Nom complet</label>
            <input
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-foreground">Numéro de téléphone</label>
            <input
              required
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              placeholder="+228..."
              className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-foreground">Mot de passe initial</label>
            <input
              required
              type="text"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Au moins 8 caractères, 1 majuscule, 1 chiffre"
              className="w-full rounded-xl border border-border bg-input-fill px-4 py-2.5 text-sm outline-none focus:border-primary"
            />
          </div>
          <Button type="submit" disabled={submitting}>
            {submitting ? "..." : "Créer le compte livreur"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
