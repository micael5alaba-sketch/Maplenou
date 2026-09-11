/// One row of the "Spécifications" section: a label/value pair generic
/// enough to describe any product type (Marque, Matière, Origine,
/// Pointure, Garantie...).
class ProductSpecEntry {
  final String label;
  final String value;

  const ProductSpecEntry({required this.label, required this.value});
}
