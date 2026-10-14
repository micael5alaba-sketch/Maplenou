import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart' as ll;

/// Un point à afficher sur [LiveTrackingMap] : position de retrait, de
/// livraison, ou position en direct du livreur.
class TrackedPoint {
  final ll.LatLng position;
  final IconData icon;
  final Color color;

  const TrackedPoint({required this.position, required this.icon, required this.color});
}

/// Carte en direct réutilisable (suivi livreur côté acheteur, itinéraire
/// côté livreur) avec des tuiles OpenStreetMap — gratuit, sans clé ni compte
/// Google Maps Platform (voir `INVENTAIRE_ABONNEMENTS.md` §3). Cadre
/// automatiquement la vue sur l'ensemble des [points] fournis.
class LiveTrackingMap extends StatelessWidget {
  final List<TrackedPoint> points;
  final double height;

  const LiveTrackingMap({super.key, required this.points, this.height = 200});

  @override
  Widget build(BuildContext context) {
    if (points.isEmpty) {
      return SizedBox(
        height: height,
        child: const Center(child: Icon(Icons.map_outlined, size: 48, color: Colors.grey)),
      );
    }

    final positions = points.map((p) => p.position).toList();
    final single = positions.length == 1;

    return SizedBox(
      height: height,
      child: FlutterMap(
        options: MapOptions(
          initialCenter: positions.first,
          initialZoom: single ? 15 : 13,
          initialCameraFit: single
              ? null
              : CameraFit.bounds(
                  bounds: LatLngBounds.fromPoints(positions),
                  padding: const EdgeInsets.all(48),
                ),
          interactionOptions: const InteractionOptions(flags: InteractiveFlag.all),
        ),
        children: [
          TileLayer(
            urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
            userAgentPackageName: 'com.maplenou.app',
          ),
          MarkerLayer(
            markers: points
                .map((p) => Marker(
                      point: p.position,
                      width: 40,
                      height: 40,
                      child: Icon(p.icon, color: p.color, size: 36),
                    ))
                .toList(),
          ),
          const RichAttributionWidget(
            attributions: [TextSourceAttribution('OpenStreetMap contributors')],
          ),
        ],
      ),
    );
  }
}
