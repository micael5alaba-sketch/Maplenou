"use client";

import dynamic from "next/dynamic";
import { useEffect, useMemo, useState } from "react";
import { apiClient, ApiException } from "@/lib/apiClient";
import type { AdminUserResponse, CourierLocationResponse, DeliveryResponse, Page } from "@/lib/types";
import type { MapPoint } from "@/components/CourierMap";
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  StatusBadge,
  Table,
  formatDate,
} from "@/components/ui";

// Leaflet touche `window` : chargé côté client uniquement.
const CourierMap = dynamic(() => import("@/components/CourierMap"), { ssr: false });

/** Un livreur qui n'a pas "pingé" depuis plus de 10 min est considéré hors ligne. */
const ONLINE_WINDOW_MS = 10 * 60 * 1000;

function distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const rad = (d: number) => (d * Math.PI) / 180;
  const a =
    Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lng2 - lng1) / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export default function DeliveriesPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<DeliveryResponse> | null>(null);
  const [couriers, setCouriers] = useState<AdminUserResponse[]>([]);
  const [locations, setLocations] = useState<CourierLocationResponse[]>([]);
  // Horloge d'affichage (en ligne / hors ligne), avancée à chaque rafraîchissement des positions.
  const [now, setNow] = useState(() => Date.now());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [assigningId, setAssigningId] = useState<string | null>(null);
  const [selectedCourier, setSelectedCourier] = useState<Record<string, string>>({});

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([
      apiClient.get<Page<DeliveryResponse>>("/api/deliveries/pending", { page, size: 20 }),
      apiClient.get<Page<AdminUserResponse>>("/api/admin/users", { role: "DELIVERY_AGENT", active: true, size: 100 }),
      apiClient.get<Page<CourierLocationResponse>>("/api/admin/delivery-agents/locations", { size: 100 }),
    ])
      .then(([deliveries, agents, agentLocations]) => {
        setData(deliveries);
        setCouriers(agents.content);
        setLocations(agentLocations.content);
      })
      .catch((e) => setError(e instanceof ApiException ? e.message : "Erreur de chargement."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [page]);

  // Les positions bougent : rafraîchissement discret toutes les 30 s, sans toucher à la liste
  // des livraisons (ni à la sélection de livreur en cours).
  useEffect(() => {
    const timer = setInterval(() => {
      apiClient
        .get<Page<CourierLocationResponse>>("/api/admin/delivery-agents/locations", { size: 100 })
        .then((p) => {
          setLocations(p.content);
          setNow(Date.now());
        })
        .catch(() => {});
    }, 30_000);
    return () => clearInterval(timer);
  }, []);

  const locationByAgent = useMemo(() => new Map(locations.map((l) => [l.agentId, l])), [locations]);

  const mapPoints = useMemo<MapPoint[]>(() => {
    const points: MapPoint[] = [];
    const seenShops = new Set<string>();
    for (const d of data?.content ?? []) {
      if (d.shopLatitude == null || d.shopLongitude == null || seenShops.has(d.shopId)) continue;
      seenShops.add(d.shopId);
      points.push({ id: `shop-${d.shopId}`, lat: d.shopLatitude, lng: d.shopLongitude, label: `Boutique : ${d.shopName}`, kind: "shop" });
    }
    for (const l of locations) {
      const online = now - new Date(l.updatedAt).getTime() < ONLINE_WINDOW_MS;
      points.push({
        id: `agent-${l.agentId}`,
        lat: l.latitude,
        lng: l.longitude,
        label: `${l.fullName} (${online ? "en ligne" : "hors ligne"})`,
        kind: online ? "courier" : "courierStale",
      });
    }
    return points;
  }, [data, locations, now]);

  /** Livreurs triés du plus proche au plus éloigné de la boutique de la livraison. */
  function couriersFor(d: DeliveryResponse) {
    return couriers
      .map((c) => {
        const loc = locationByAgent.get(c.id);
        const online = !!loc && now - new Date(loc.updatedAt).getTime() < ONLINE_WINDOW_MS;
        const km =
          loc && d.shopLatitude != null && d.shopLongitude != null
            ? distanceKm(loc.latitude, loc.longitude, d.shopLatitude, d.shopLongitude)
            : null;
        return { courier: c, online, km };
      })
      .sort((a, b) => Number(b.online) - Number(a.online) || (a.km ?? Infinity) - (b.km ?? Infinity));
  }

  async function assign(deliveryId: string) {
    const agentId = selectedCourier[deliveryId];
    if (!agentId) {
      alert("Choisis un livreur d'abord.");
      return;
    }
    setAssigningId(deliveryId);
    try {
      await apiClient.patch(`/api/deliveries/${deliveryId}/assign`, { agentId });
      load();
    } catch (e) {
      alert(e instanceof ApiException ? e.message : "Erreur — le livreur n'a peut-être pas tous ses documents validés.");
    } finally {
      setAssigningId(null);
    }
  }

  return (
    <div>
      <PageHeader title="Livraisons en attente" subtitle="Affecter un livreur aux colis prêts à être récupérés" />

      <Card className="mb-4">
        <div className="mb-2 flex flex-wrap items-center gap-4 text-xs text-muted">
          <span className="font-medium text-foreground">Carte des livreurs</span>
          <span><span style={{ color: "#f59e0b" }}>●</span> Boutique à récupérer</span>
          <span><span style={{ color: "#16a34a" }}>●</span> Livreur en ligne</span>
          <span><span style={{ color: "#9ca3af" }}>●</span> Livreur hors ligne (dernière position)</span>
        </div>
        <CourierMap points={mapPoints} />
        {locations.length === 0 && (
          <p className="mt-2 text-xs text-muted">
            Aucune position de livreur reçue pour l&apos;instant : un livreur apparaît ici dès qu&apos;il passe « En ligne » dans son app.
          </p>
        )}
      </Card>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}

      {data && (
        <Card>
          {data.content.length === 0 ? (
            <EmptyState message="Aucune livraison en attente d'affectation." />
          ) : (
            <Table headers={["Commande", "Boutique", "Statut", "Créée le", "Affecter"]}>
              {data.content.map((d) => (
                <tr key={d.id}>
                  <td className="px-4 py-3 font-medium text-foreground">#{d.orderNumber}</td>
                  <td className="px-4 py-3 text-muted">{d.shopName}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={d.status} />
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDate(d.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <select
                        value={selectedCourier[d.id] ?? ""}
                        onChange={(e) => setSelectedCourier((prev) => ({ ...prev, [d.id]: e.target.value }))}
                        className="rounded-lg border border-border bg-input-fill px-2 py-1.5 text-sm"
                      >
                        <option value="">Choisir un livreur</option>
                        {couriersFor(d).map(({ courier: c, online, km }) => (
                          <option key={c.id} value={c.id}>
                            {c.fullName}
                            {km != null ? ` — ${km.toFixed(1)} km de la boutique` : " — position inconnue"}
                            {online ? "" : " (hors ligne)"}
                          </option>
                        ))}
                      </select>
                      <Button onClick={() => assign(d.id)} disabled={assigningId === d.id}>
                        Affecter
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
