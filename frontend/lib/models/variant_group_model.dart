/// A group of selectable variant options for a product — "Format",
/// "Taille", "Couleur", "Pointure"... Generic enough to represent any
/// product's option set, whatever the product type.
class VariantGroupModel {
  final String label;
  final List<String> options;

  const VariantGroupModel({required this.label, required this.options});
}
