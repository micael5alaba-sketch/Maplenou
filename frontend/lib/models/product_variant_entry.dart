/// One variant row for [AddProductScreen]'s "Variantes" section (ex: "S",
/// "M / Rouge", "256 Go"...). Le backend accepte un label libre par variante
/// (`CreateVariantRequest.label`) — pas de structure d'attributs séparée,
/// donc pour une combinaison (Taille + Couleur) le vendeur tape directement
/// "M / Rouge" comme un seul libellé.
///
/// Mutable on purpose: the form edits label/stock/price in place as the
/// seller types, one instance per row in a growable list.
class ProductVariantEntry {
  String label;
  String stockQuantity;
  String priceOverride;

  ProductVariantEntry({this.label = '', this.stockQuantity = '', this.priceOverride = ''});
}
