import 'delivery_info_model.dart';
import 'product_model.dart';
import 'product_spec_entry.dart';
import 'product_variant_model.dart';
import 'review_model.dart';
import 'variant_group_model.dart';

/// Full data behind [ProductDetailsScreen] — generic enough to represent
/// any product type in the marketplace (mode, électronique, beauté,
/// maison, alimentaire...), whatever fields it does or doesn't use.
class ProductDetailModel {
  final String id;
  final String name;
  final String shopId;
  final String shopName;
  final String shopSlug;
  final List<String> images;
  final List<String> badges;
  final num price;
  final num? oldPrice;
  final double? rating;
  final int? reviewCount;

  /// Variantes réelles du produit (`VariantResponse` — voir
  /// `catalog/dto/VariantResponse.java`) : c'est leur `id` que le panier
  /// référence, pas celui du produit.
  final List<ProductVariantModel> variants;

  final String description;
  final List<ProductSpecEntry> specifications;
  final DeliveryInfoModel delivery;
  final List<ReviewModel> reviews;
  final List<ProductModel> relatedProducts;

  const ProductDetailModel({
    required this.id,
    required this.name,
    required this.shopId,
    required this.shopName,
    required this.shopSlug,
    required this.images,
    this.badges = const [],
    required this.price,
    this.oldPrice,
    this.rating,
    this.reviewCount,
    this.variants = const [],
    required this.description,
    this.specifications = const [],
    required this.delivery,
    this.reviews = const [],
    this.relatedProducts = const [],
  });

  int? get discountPercent {
    final old = oldPrice;
    if (old == null || old <= price) return null;
    return (((old - price) / old) * 100).round();
  }

  /// Un produit sans variante définie par le vendeur reçoit toujours au
  /// moins une variante par défaut côté backend (le stock vit sur
  /// `ProductVariant`, jamais sur `Product`) — cette liste n'est donc vide
  /// que si le produit n'a pas encore été chargé.
  bool get hasVariants => variants.isNotEmpty;

  /// Découpe le libellé d'une variante sur " / " ("M / Rouge" -> ["M", "Rouge"]). Le backend ne
  /// connaît qu'un libellé libre par variante : c'est la convention de saisie vendeur qui permet
  /// à l'acheteur de choisir chaque attribut (taille, couleur...) séparément. Un préfixe
  /// "Nom: valeur" ("Taille: M / Couleur: Rouge") fixe le nom du groupe.
  static List<String> _parts(String label) =>
      label.split('/').map((p) => p.trim()).where((p) => p.isNotEmpty).toList();

  static const _sizeValues = {'XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL', 'UNIQUE'};
  static const _colorValues = {
    'rouge', 'bleu', 'vert', 'jaune', 'noir', 'blanc', 'gris', 'rose', 'orange', 'violet', 'marron', 'beige',
    'red', 'blue', 'green', 'yellow', 'black', 'white', 'gray', 'grey', 'pink', 'purple', 'brown',
  };

  static String _guessGroupName(List<String> values, int index) {
    if (values.every((v) => _sizeValues.contains(v.toUpperCase()) || RegExp(r'^\d{2}(\.\d)?$').hasMatch(v))) {
      return 'Taille';
    }
    if (values.every((v) => _colorValues.contains(v.toLowerCase()))) return 'Couleur';
    return 'Option ${index + 1}';
  }

  /// Parties (nom de groupe, valeur) de chaque variante, ou null si les libellés ne suivent pas
  /// tous la même structure à plusieurs attributs (alors : un seul groupe "Choix" à plat).
  List<List<(String? name, String value)>>? get _structuredParts {
    if (variants.length <= 1) return null;
    final rows = <List<(String?, String)>>[];
    for (final variant in variants) {
      final parts = _parts(variant.label).map((part) {
        final colon = part.indexOf(':');
        if (colon > 0 && colon < part.length - 1) {
          return (part.substring(0, colon).trim() as String?, part.substring(colon + 1).trim());
        }
        return (null as String?, part);
      }).toList();
      rows.add(parts);
    }
    final width = rows.first.length;
    if (width < 2 || rows.any((r) => r.length != width)) return null;
    return rows;
  }

  List<VariantGroupModel> get variantGroups {
    if (variants.length <= 1) return const [];
    final rows = _structuredParts;
    if (rows == null) {
      return [VariantGroupModel(label: 'Choix', options: variants.map((v) => v.label).toList())];
    }
    final groups = <VariantGroupModel>[];
    for (var i = 0; i < rows.first.length; i++) {
      final values = <String>[];
      for (final row in rows) {
        if (!values.contains(row[i].$2)) values.add(row[i].$2);
      }
      final named = rows.first[i].$1;
      groups.add(VariantGroupModel(label: named ?? _guessGroupName(values, i), options: values));
    }
    return groups;
  }

  /// Variante correspondant à la sélection courante (une valeur par groupe de [variantGroups]).
  ProductVariantModel? variantForSelection(Map<String, String> selected) {
    if (variants.length == 1) return variants.first;
    final groups = variantGroups;
    if (groups.length == 1 && groups.first.label == 'Choix') {
      final label = selected['Choix'];
      return label == null ? null : variantByLabel(label);
    }
    final rows = _structuredParts;
    if (rows == null) return null;
    for (var v = 0; v < variants.length; v++) {
      var match = true;
      for (var g = 0; g < groups.length; g++) {
        if (rows[v][g].$2 != selected[groups[g].label]) {
          match = false;
          break;
        }
      }
      if (match) return variants[v];
    }
    return null; // combinaison inexistante (ex. taille M n'existe pas en bleu)
  }

  /// Sélection initiale : la première variante en stock (sinon la première), décomposée par groupe.
  Map<String, String> initialSelection() {
    final groups = variantGroups;
    if (groups.isEmpty) return {};
    final start = variants.firstWhere((v) => v.inStock, orElse: () => variants.first);
    if (groups.length == 1 && groups.first.label == 'Choix') return {'Choix': start.label};
    final row = _structuredParts![variants.indexOf(start)];
    return {for (var g = 0; g < groups.length; g++) groups[g].label: row[g].$2};
  }

  ProductVariantModel? variantByLabel(String label) {
    for (final variant in variants) {
      if (variant.label == label) return variant;
    }
    return null;
  }
}
