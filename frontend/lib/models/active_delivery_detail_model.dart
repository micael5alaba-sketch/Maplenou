/// Full detail of one delivery, shown on [CourierDeliveryDetailScreen] and
/// carried into [CourierDeliveryConfirmationScreen]. Assemblé depuis
/// `DeliveryModel` + `DeliveryNavigationModel` (voir `CourierDeliveryService`).
class ActiveDeliveryDetailModel {
  final String id;
  final String orderNumber;
  final String status;
  final String shopName;
  final String pickupAddress;
  final String clientName;
  final String deliveryAddress;
  final double? distanceKm;

  /// Coordonnées du retrait/dépôt — absentes si l'adresse correspondante n'a
  /// pas été géolocalisée (pas de géocodage d'adresse texte côté backend,
  /// seulement la position GPS capturée si l'utilisateur l'a autorisée à la
  /// création de l'adresse). Utilisées pour ouvrir l'itinéraire dans l'app
  /// Google Maps externe — pas de carte intégrée, pas de clé Google Maps
  /// Platform (voir `INVENTAIRE_ABONNEMENTS.md` §3).
  final double? pickupLatitude;
  final double? pickupLongitude;
  final double? dropoffLatitude;
  final double? dropoffLongitude;

  /// Gain net estimé pour le livreur (75% du tarif de la zone de livraison),
  /// visible dès que la livraison lui est assignée — pas seulement une fois
  /// terminée (voir `CourierEarningService` côté backend).
  final double? estimatedEarning;

  const ActiveDeliveryDetailModel({
    required this.id,
    required this.orderNumber,
    required this.status,
    required this.shopName,
    required this.pickupAddress,
    required this.clientName,
    required this.deliveryAddress,
    this.distanceKm,
    this.pickupLatitude,
    this.pickupLongitude,
    this.dropoffLatitude,
    this.dropoffLongitude,
    this.estimatedEarning,
  });

  bool get canStart => status == 'ASSIGNED';
  bool get canConfirm => status == 'IN_TRANSIT';

  /// Destination de l'itinéraire : le point de retrait tant que la course
  /// n'est pas commencée, puis l'adresse du client une fois en route.
  double? get navigationLatitude => status == 'ASSIGNED' ? pickupLatitude : dropoffLatitude;
  double? get navigationLongitude => status == 'ASSIGNED' ? pickupLongitude : dropoffLongitude;
  bool get hasNavigationTarget => navigationLatitude != null && navigationLongitude != null;
}
