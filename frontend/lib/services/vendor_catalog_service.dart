import '../models/vendor_product_model.dart';

/// Provides the seller's product catalog shown on [VendorCatalogScreen].
///
/// Mocked for now — no network call. Replace [getProducts] with a real
/// `GET /api/shops/mine/products` call once wired up; the return shape
/// stays the same.
class VendorCatalogService {
  List<VendorProductModel> getProducts() {
    return const [
      VendorProductModel(
        id: 'p1',
        name: 'Sac Cabas Tressé',
        description: 'Fait main en fibres naturelles locales. Résistant et élégant pour le quotidien.',
        price: 24500,
        stock: 12,
        imageUrl: 'https://picsum.photos/seed/sac-cabas-tresse/600/600',
      ),
      VendorProductModel(
        id: 'p2',
        name: 'Robe Wax Imprimée',
        description: 'Tissu wax authentique, coupe ajustée, doublure intérieure confortable.',
        price: 38000,
        stock: 3,
        imageUrl: 'https://picsum.photos/seed/robe-wax/600/600',
      ),
      VendorProductModel(
        id: 'p3',
        name: 'Sandales Cuir Véritable',
        description: 'Semelle en cuir tannage végétal, cousues main, disponibles en plusieurs tailles.',
        price: 32000,
        stock: 0,
        imageUrl: 'https://picsum.photos/seed/sandales-cuir/600/600',
      ),
      VendorProductModel(
        id: 'p4',
        name: 'Smartphone Reconditionné X10',
        description: 'Grade A, batterie neuve, garantie 6 mois, livré avec chargeur.',
        price: 145000,
        stock: 7,
        imageUrl: 'https://picsum.photos/seed/smartphone-x10/600/600',
      ),
      VendorProductModel(
        id: 'p5',
        name: 'Collier Perles Dorées',
        description: 'Bijou artisanal plaqué or, idéal pour les grandes occasions.',
        price: 18500,
        stock: 15,
      ),
      VendorProductModel(
        id: 'p6',
        name: 'Crème Karité Bio',
        description: '100% naturelle, hydratation intense, sans parabène.',
        price: 6500,
        stock: 2,
        imageUrl: 'https://picsum.photos/seed/creme-karite/600/600',
      ),
      VendorProductModel(
        id: 'p7',
        name: 'Ceinture Cuir Homme',
        description: 'Cuir pleine fleur, boucle métal brossé, taille ajustable.',
        price: 15000,
        stock: 20,
        imageUrl: 'https://picsum.photos/seed/ceinture-cuir/600/600',
      ),
      VendorProductModel(
        id: 'p8',
        name: 'Panier de Rangement Tressé',
        description: "Fibres végétales tressées à la main, plusieurs tailles disponibles.",
        price: 12000,
        stock: 0,
      ),
      VendorProductModel(
        id: 'p9',
        name: 'Écouteurs Bluetooth Sport',
        description: 'Autonomie 20h, résistants à la transpiration, son stéréo haute qualité.',
        price: 22000,
        stock: 9,
        imageUrl: 'https://picsum.photos/seed/ecouteurs-sport/600/600',
      ),
      VendorProductModel(
        id: 'p10',
        name: 'Miel Pur des Savanes',
        description: 'Récolté localement, non pasteurisé, pot de 500g.',
        price: 5000,
        stock: 4,
        imageUrl: 'https://picsum.photos/seed/miel-savanes/600/600',
      ),
    ];
  }
}
