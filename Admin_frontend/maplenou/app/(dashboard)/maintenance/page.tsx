"use client";

import { useEffect, useState, type FormEvent } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { MaintenanceStatusResponse } from "@/lib/types";
import { Button, Card, ErrorState, LoadingState, PageHeader } from "@/components/ui";

export default function MaintenancePage() {
  const [status, setStatus] = useState<MaintenanceStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    apiClient
      .get<MaintenanceStatusResponse>("/api/admin/maintenance")
      .then((s) => {
        setStatus(s);
        if (s.message) setMessage(s.message);
      })
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function enable(e: FormEvent) {
    e.preventDefault();
    if (
      !confirm(
        "Activer le mode maintenance ?\n\nTous les appels API seront coupés (503) pour les clients Acheteur / Vendeur / Livreur — seuls l'espace admin, la connexion et les webhooks de paiement resteront accessibles. À utiliser uniquement pour une intervention planifiée ou un incident.",
      )
    )
      return;
    setBusy(true);
    setError(null);
    try {
      const s = await apiClient.post<MaintenanceStatusResponse>("/api/admin/maintenance/enable", { message });
      setStatus(s);
    } catch (e) {
      setError(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  async function disable() {
    setBusy(true);
    setError(null);
    try {
      const s = await apiClient.post<MaintenanceStatusResponse>("/api/admin/maintenance/disable");
      setStatus(s);
    } catch (e) {
      setError(e instanceof ApiException ? e.message : "Erreur.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Maintenance"
        subtitle="Coupe tout le trafic API (acheteurs, vendeurs, livreurs) en un clic — l'espace admin reste accessible"
      />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {status && !loading && (
        <>
          <Card
            className={`mb-6 max-w-xl ${status.enabled ? "ring-2 ring-error" : ""}`}
          >
            <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">Statut actuel</p>
            <p className={`text-2xl font-bold ${status.enabled ? "text-error" : "text-primary"}`}>
              {status.enabled ? "Maintenance ACTIVE" : "Plateforme en ligne"}
            </p>
            {status.enabled && status.message && (
              <p className="mt-2 text-sm text-muted">Message affiché : « {status.message} »</p>
            )}
          </Card>

          {status.enabled ? (
            <Button variant="danger" onClick={disable} disabled={busy}>
              Désactiver la maintenance — remettre la plateforme en ligne
            </Button>
          ) : (
            <Card className="max-w-xl">
              <form onSubmit={enable} className="flex flex-col gap-3">
                <div>
                  <label className="mb-1 block text-xs font-medium text-muted">
                    Message affiché aux utilisateurs (optionnel)
                  </label>
                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    rows={3}
                    maxLength={500}
                    placeholder="Maplenou est actuellement en maintenance. Merci de réessayer dans quelques instants."
                    className="w-full rounded-xl border border-border bg-input-fill px-3 py-2 text-sm outline-none focus:border-primary"
                  />
                </div>
                <Button type="submit" variant="danger" disabled={busy}>
                  Activer la maintenance — couper tous les services
                </Button>
              </form>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
