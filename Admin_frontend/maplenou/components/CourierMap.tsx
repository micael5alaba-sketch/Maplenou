"use client";

import { useEffect, useRef } from "react";
import "leaflet/dist/leaflet.css";
import type { Map as LeafletMap, LayerGroup } from "leaflet";

export interface MapPoint {
  id: string;
  lat: number;
  lng: number;
  label: string;
  kind: "shop" | "courier" | "courierStale";
}

const COLORS: Record<MapPoint["kind"], string> = {
  shop: "#f59e0b",
  courier: "#16a34a",
  courierStale: "#9ca3af",
};

// Lomé : centre par défaut quand aucun point n'est encore connu.
const DEFAULT_CENTER: [number, number] = [6.1725, 1.2314];

/**
 * Carte (Leaflet + tuiles OpenStreetMap, sans clé d'API) des boutiques à récupérer et des
 * livreurs. Leaflet manipule `window`, il est donc importé dynamiquement dans l'effet (pas de
 * rendu serveur).
 */
export default function CourierMap({ points, height = 360 }: { points: MapPoint[]; height?: number }) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const layerRef = useRef<LayerGroup | null>(null);
  const fittedRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const L = (await import("leaflet")).default;
      if (cancelled || !containerRef.current) return;

      if (!mapRef.current) {
        mapRef.current = L.map(containerRef.current).setView(DEFAULT_CENTER, 12);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
          maxZoom: 19,
          attribution: "&copy; OpenStreetMap",
        }).addTo(mapRef.current);
        layerRef.current = L.layerGroup().addTo(mapRef.current);
      }

      const layer = layerRef.current!;
      layer.clearLayers();
      for (const p of points) {
        L.circleMarker([p.lat, p.lng], {
          radius: p.kind === "shop" ? 9 : 8,
          color: "#ffffff",
          weight: 2,
          fillColor: COLORS[p.kind],
          fillOpacity: 0.95,
        })
          .bindTooltip(p.label)
          .addTo(layer);
      }

      // Cadre la carte sur les points une seule fois (pas à chaque rafraîchissement, pour ne
      // pas arracher la vue à l'admin qui zoome).
      if (points.length > 0 && !fittedRef.current) {
        fittedRef.current = true;
        mapRef.current.fitBounds(L.latLngBounds(points.map((p) => [p.lat, p.lng] as [number, number])), {
          padding: [40, 40],
          maxZoom: 15,
        });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [points]);

  useEffect(() => {
    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
      layerRef.current = null;
    };
  }, []);

  return <div ref={containerRef} style={{ height }} className="w-full overflow-hidden rounded-xl border border-border" />;
}
